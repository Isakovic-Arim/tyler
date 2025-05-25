import { useEffect, useState } from "react"
import { httpClient } from "~/service"
import { format, parseISO, startOfWeek, addDays, addWeeks, isSameDay } from "date-fns"
import type { TaskResponseDto } from "~/model/task"
import TaskCalendar from "./calendar-view"
import AddTaskPopover from "./add-task-dialog"
import TaskUpdatePopover from "./update-task-dialog"
import { Plus } from "lucide-react"
import UserProfileSidebar from "./user-profile"

export default function TaskBoard({ daysOff, user }: { daysOff: string[]; user: any }) {
    const [tasks, setTasks] = useState<TaskResponseDto[]>([])
    const [selectedTask, setSelectedTask] = useState<TaskResponseDto | null>(null)
    const [currentWeekOffset, setCurrentWeekOffset] = useState(0)
    const [showAddTask, setShowAddTask] = useState(false)
    const [addTaskDate, setAddTaskDate] = useState<string>("")

    const startOfCurrentWeek = addWeeks(startOfWeek(new Date(), { weekStartsOn: 0 }), currentWeekOffset)
    const daysOfWeek = Array.from({ length: 7 }, (_, i) => addDays(startOfCurrentWeek, i))

    useEffect(() => {
        httpClient.get<TaskResponseDto[]>("tasks").then((res) => {
            setTasks(res.data)
        })
    }, [])

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
    }

    const handleAddTask = (date?: string) => {
        setAddTaskDate(date || format(new Date(), "yyyy-MM-dd"))
        setShowAddTask(true)
    }

    return (
        <div className="flex h-screen bg-gray-50">
            {/* Profile Sidebar */}
            <UserProfileSidebar user={user} />

            {/* Main Content */}
            <div className="flex-1 p-6 overflow-auto">
                {/* Header */}
                <div className="flex justify-between items-center mb-6">
                    <button
                        onClick={() => setCurrentWeekOffset((prev) => prev - 1)}
                        className="px-4 py-2 bg-white hover:bg-gray-50 text-gray-700 rounded-lg transition-colors shadow-sm border border-gray-200"
                    >
                        ← Previous
                    </button>
                    <h1 className="text-2xl font-bold text-gray-900">Week of {format(startOfCurrentWeek, "MMM d, yyyy")}</h1>
                    <button
                        onClick={() => setCurrentWeekOffset((prev) => prev + 1)}
                        className="px-4 py-2 bg-white hover:bg-gray-50 text-gray-700 rounded-lg transition-colors shadow-sm border border-gray-200"
                    >
                        Next →
                    </button>
                </div>

                {/* Calendar View */}
                <TaskCalendar
                    daysOfWeek={daysOfWeek}
                    daysOff={daysOff}
                    groupedTasks={groupedTasks}
                    onTaskClick={handleTaskClick}
                    onDone={handleDone}
                    onDelete={handleDelete}
                    onAddTask={handleAddTask}
                />

                {/* Floating Add Button */}
                <button
                    onClick={() => handleAddTask()}
                    className="fixed bottom-6 right-6 w-14 h-14 bg-blue-600 hover:bg-blue-700 text-white rounded-full shadow-lg hover:shadow-xl transition-all duration-200 flex items-center justify-center z-40"
                >
                    <Plus size={24} />
                </button>

                {/* Popovers */}
                <AddTaskPopover
                    isOpen={showAddTask}
                    onClose={() => setShowAddTask(false)}
                    onSave={handleRefresh}
                    defaultDate={addTaskDate}
                />

                <TaskUpdatePopover task={selectedTask} onClose={() => setSelectedTask(null)} onSave={handleRefresh} />
            </div>
        </div>
    )
}
