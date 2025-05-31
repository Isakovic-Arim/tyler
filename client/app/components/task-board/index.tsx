import {useEffect, useState} from "react"
import {httpClient} from "~/service"
import {format, parseISO, startOfWeek, addDays, addWeeks, isSameDay} from "date-fns"
import type {TaskResponseDto} from "~/model/task"
import type {UserProfile} from "~/model/user"
import TaskCalendar from "./calendar-view"
import AddTaskPopover from "./add-task-dialog"
import TaskUpdatePopover from "./update-task-dialog"
import {Plus, Menu, HelpCircle} from "lucide-react"
import UserProfileSidebar from "../user-profile"
import {useTutorial} from "~/components/tutorial";

export default function TaskBoard({daysOff, user: initialUser}: { daysOff: string[]; user: UserProfile }) {
    const [tasks, setTasks] = useState<TaskResponseDto[]>([])
    const [user, setUser] = useState<UserProfile>(initialUser)
    const [selectedTask, setSelectedTask] = useState<TaskResponseDto | null>(null)
    const [currentWeekOffset, setCurrentWeekOffset] = useState(0)
    const [showAddTask, setShowAddTask] = useState(false)
    const [addTaskDate, setAddTaskDate] = useState<string>("")
    const [sidebarOpen, setSidebarOpen] = useState(false)
    const {startTutorial, isTutorialCompleted} = useTutorial()

    const startOfCurrentWeek = addWeeks(startOfWeek(new Date(), {weekStartsOn: 0}), currentWeekOffset)
    const daysOfWeek = Array.from({length: 7}, (_, i) => addDays(startOfCurrentWeek, i))

    useEffect(() => {
        httpClient.get<TaskResponseDto[]>("tasks").then((res) => {
            setTasks(res.data)
        })
    }, [])

    useEffect(() => {
        if (currentWeekOffset === 0) {
            httpClient.get<UserProfile>("users/me").then((res) => {
                setUser(res.data)
            })
        }
    }, [currentWeekOffset])

    const groupedTasks = daysOfWeek.reduce<Record<string, TaskResponseDto[]>>((acc, date) => {
        const dayKey = format(date, "yyyy-MM-dd")
        acc[dayKey] = tasks.filter((task) => isSameDay(parseISO(task.dueDate), date))
        return acc
    }, {})

    const handleTaskClick = (task: TaskResponseDto) => {
        setSelectedTask(task)
    }

    const handleDone = async (task: TaskResponseDto) => {
        await httpClient.patch(`tasks/${task.id}/done`)
        handleRefresh()
    }

    const handleDelete = async (task: TaskResponseDto) => {
        await httpClient.delete(`tasks/${task.id}`)
        handleRefresh()
    }

    const handleRefresh = () => {
        httpClient.get<TaskResponseDto[]>("tasks").then((res) => setTasks(res.data))
        httpClient.get<UserProfile>("users/me").then((res) => setUser(res.data))
    }

    const handleAddTask = (date?: string) => {
        setAddTaskDate(date || format(new Date(), "yyyy-MM-dd"))
        setShowAddTask(true)
    }

    const handleToggleDayOff = async (date: string) => {
        if (currentWeekOffset !== 0) return

        try {
            const isDayOff = user.daysOff.includes(date)

            if (isDayOff) {
                await httpClient.delete("/users/me/day-off", {
                    params: {'date': date}
                })
            } else {
                const currentWeekDaysOff = user.daysOff.filter((dayOff) => {
                    const dayOffDate = parseISO(dayOff)
                    return daysOfWeek.some((weekDate) => format(weekDate, "yyyy-MM-dd") === format(dayOffDate, "yyyy-MM-dd"))
                })

                if (currentWeekDaysOff.length >= 2) {
                    alert("You can only set 2 days off per week")
                    return
                }

                await httpClient.post("/users/me/day-off", JSON.stringify(date), {
                    headers: {
                        "Content-Type": "application/json",
                    }
                })
            }

            // Refresh user data
            const response = await httpClient.get<UserProfile>("users/me")
            setUser(response.data)
        } catch (error) {
            console.error("Failed to toggle day off:", error)
        }
    }

    return (
        <div className="flex h-screen bg-gray-50">
            {/* Mobile Sidebar Overlay */}
            {sidebarOpen && (
                <div className="fixed inset-0 bg-black bg-opacity-50 z-40 lg:hidden"
                     onClick={() => setSidebarOpen(false)}/>
            )}

            {/* Profile Sidebar */}
            <div
                className={`fixed lg:static inset-y-0 left-0 z-50 lg:z-auto transform ${
                    sidebarOpen ? "translate-x-0" : "-translate-x-full"
                } lg:translate-x-0 transition-transform duration-300 ease-in-out lg:transition-none`}
            >
                <UserProfileSidebar user={user} currentWeekDates={daysOfWeek}/>
            </div>

            {/* Main Content */}
            <div className="flex-1 flex flex-col min-w-0">
                {/* Mobile Header */}
                <div className="lg:hidden bg-white border-b border-gray-200 p-4 flex items-center justify-between">
                    <button onClick={() => setSidebarOpen(true)}
                            className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
                        <Menu size={24} className="text-gray-600"/>
                    </button>
                    <h1 className="text-lg font-semibold text-gray-900">Tyler</h1>
                    <button
                        onClick={startTutorial}
                        className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                        aria-label="Start tutorial"
                    >
                        <HelpCircle size={20} className="text-gray-600"/>
                    </button>
                </div>

                {/* Desktop Content */}
                <div className="flex-1 p-4 lg:p-6 overflow-auto">
                    {/* Header */}
                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
                        <button
                            onClick={() => setCurrentWeekOffset((prev) => prev - 1)}
                            className="px-4 py-2 bg-white hover:bg-gray-50 text-gray-700 rounded-lg transition-colors shadow-sm border border-gray-200 text-sm sm:text-base"
                        >
                            ← Previous
                        </button>
                        <div className="text-center">
                            <h1 className="text-xl sm:text-2xl font-bold text-gray-900">
                                Week of {format(startOfCurrentWeek, "MMM d, yyyy")}
                            </h1>
                            {currentWeekOffset === 0 && (
                                <p className="text-xs sm:text-sm text-gray-500 mt-1">
                                    Click on day headers to set days off (max 2 per week)
                                </p>
                            )}
                        </div>
                        <div className="flex items-center gap-2">
                            <button
                                onClick={startTutorial}
                                className="hidden sm:flex items-center gap-1 px-3 py-2 bg-white hover:bg-gray-50 text-gray-700 rounded-lg transition-colors shadow-sm border border-gray-200 text-sm"
                                aria-label="Start tutorial"
                            >
                                <HelpCircle size={16} className="text-gray-600"/>
                                <span>{isTutorialCompleted ? "Restart Tutorial" : "Start Tutorial"}</span>
                            </button>
                            <button
                                onClick={() => setCurrentWeekOffset((prev) => prev + 1)}
                                className="px-4 py-2 bg-white hover:bg-gray-50 text-gray-700 rounded-lg transition-colors shadow-sm border border-gray-200 text-sm sm:text-base"
                            >
                                Next →
                            </button>
                        </div>
                    </div>

                    {/* Calendar View */}
                    <TaskCalendar
                        daysOfWeek={daysOfWeek}
                        daysOff={user.daysOff}
                        groupedTasks={groupedTasks}
                        onTaskClick={handleTaskClick}
                        onDone={handleDone}
                        onDelete={handleDelete}
                        onAddTask={handleAddTask}
                        onToggleDayOff={handleToggleDayOff}
                        isCurrentWeek={currentWeekOffset === 0}
                    />

                    {/* Floating Add Button */}
                    <button
                        onClick={() => handleAddTask()}
                        className="fixed bottom-6 right-6 w-12 h-12 sm:w-14 sm:h-14 bg-blue-600 hover:bg-blue-700 text-white rounded-full shadow-lg hover:shadow-xl transition-all duration-200 flex items-center justify-center z-30"
                    >
                        <Plus size={20} className="sm:hidden"/>
                        <Plus size={24} className="hidden sm:block"/>
                    </button>

                    {/* Popovers */}
                    <AddTaskPopover
                        isOpen={showAddTask}
                        onClose={() => setShowAddTask(false)}
                        onSave={handleRefresh}
                        defaultDate={addTaskDate}
                    />

                    <TaskUpdatePopover task={selectedTask} onClose={() => setSelectedTask(null)}
                                       onSave={handleRefresh}/>
                </div>
            </div>
        </div>
    )
}
