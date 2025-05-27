import type { TaskResponseDto } from "~/model/task"
import TaskCard from "./TaskCard"
import { Plus, Calendar } from "lucide-react"

export default function DayColumn({
                                      date,
                                      dayName,
                                      dayNumber,
                                      isDayOff,
                                      tasks,
                                      onTaskClick,
                                      onDone,
                                      onDelete,
                                      onAddTask,
                                      onToggleDayOff,
                                      isCurrentWeek,
                                  }: {
    date: string
    dayName: string
    dayNumber: string
    tasks: TaskResponseDto[]
    isDayOff: boolean
    onTaskClick: (task: TaskResponseDto) => void
    onDone: (task: TaskResponseDto) => void
    onDelete: (task: TaskResponseDto) => void
    onAddTask: (date: string) => void
    onToggleDayOff: (date: string) => void
    isCurrentWeek: boolean
}) {
    const handleDayOffClick = () => {
        if (isCurrentWeek) {
            onToggleDayOff(date)
        }
    }

    return (
        <div
            className={`border border-gray-200 rounded-lg bg-white ${
                isDayOff ? "bg-purple-50 border-purple-200" : ""
            } min-h-[300px] flex flex-col`}
        >
            {/* Day Header */}
            <div className={`p-3 border-b border-gray-100 ${isDayOff ? "bg-purple-100" : "bg-gray-50"}`}>
                <div className="flex items-center justify-between">
                    <div
                        className={`flex-1 ${isCurrentWeek ? "cursor-pointer hover:bg-white hover:bg-opacity-50 rounded p-1 -m-1" : ""}`}
                        onClick={handleDayOffClick}
                        title={isCurrentWeek ? (isDayOff ? "Click to remove day off" : "Click to set as day off") : ""}
                    >
                        <div className="flex items-center gap-2">
                            <div>
                                <p className="text-sm font-medium text-gray-600">{dayName}</p>
                                <p className="text-lg font-bold text-gray-900">{dayNumber}</p>
                            </div>
                            {isDayOff && <Calendar size={16} className="text-purple-600" />}
                        </div>
                    </div>

                    {!isDayOff && (
                        <button
                            onClick={() => onAddTask(date)}
                            className="p-1 hover:bg-white hover:shadow-sm rounded-md transition-all duration-150 text-gray-400 hover:text-blue-600"
                            title="Add task"
                        >
                            <Plus size={16} />
                        </button>
                    )}
                </div>

                {isDayOff && (
                    <div className="mt-2">
                        <span className="text-xs bg-purple-200 text-purple-700 px-2 py-1 rounded-full">Day Off</span>
                    </div>
                )}
            </div>

            {/* Tasks */}
            <div className="p-2 flex-1 space-y-2">
                {!isDayOff &&
                    tasks.map((task) => (
                        <TaskCard key={task.id} task={task} onClick={onTaskClick} onDone={onDone} onDelete={onDelete} />
                    ))}

                {!isDayOff && tasks.length === 0 && (
                    <div className="text-center py-8">
                        <p className="text-gray-400 text-sm">No tasks</p>
                        <button
                            onClick={() => onAddTask(date)}
                            className="mt-2 text-blue-600 hover:text-blue-700 text-sm font-medium"
                        >
                            Add a task
                        </button>
                    </div>
                )}

                {isDayOff && (
                    <div className="text-center py-8">
                        <Calendar size={32} className="text-purple-400 mx-auto mb-2" />
                        <p className="text-purple-600 text-sm font-medium">Day Off</p>
                        <p className="text-purple-500 text-xs mt-1">Enjoy your rest day!</p>
                        {isCurrentWeek && (
                            <button
                                onClick={handleDayOffClick}
                                className="mt-2 text-purple-600 hover:text-purple-700 text-xs underline"
                            >
                                Remove day off
                            </button>
                        )}
                    </div>
                )}
            </div>
        </div>
    )
}
