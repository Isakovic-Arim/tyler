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
            className={`border border-gray-200 dark:border-gray-700 rounded-lg bg-white dark:bg-gray-800 ${
                isDayOff ? "bg-purple-50 dark:bg-purple-900/20 border-purple-200 dark:border-purple-700" : ""
            } min-h-[250px] sm:min-h-[300px] flex flex-col`}
        >
            {/* Day Header */}
            <div
                className={`p-2 sm:p-3 border-b border-gray-100 dark:border-gray-700 ${isDayOff ? "bg-purple-100 dark:bg-purple-900/30" : "bg-gray-50 dark:bg-gray-700"}`}
            >
                <div className="flex items-center justify-between">
                    <div
                        className={`flex-1 ${
                            isCurrentWeek
                                ? "cursor-pointer hover:bg-white dark:hover:bg-gray-600 hover:bg-opacity-50 rounded p-1 -m-1"
                                : ""
                        }`}
                        onClick={handleDayOffClick}
                        title={isCurrentWeek ? (isDayOff ? "Click to remove day off" : "Click to set as day off") : ""}
                    >
                        <div className="flex items-center gap-2">
                            <div>
                                <p className="text-xs sm:text-sm font-medium text-gray-600 dark:text-gray-400">{dayName}</p>
                                <p className="text-base sm:text-lg font-bold text-gray-900 dark:text-white">{dayNumber}</p>
                            </div>
                            {isDayOff && <Calendar size={14} className="text-purple-600 dark:text-purple-400 sm:w-4 sm:h-4" />}
                        </div>
                    </div>

                    {!isDayOff && (
                        <button
                            onClick={() => onAddTask(date)}
                            className="p-1 hover:bg-white dark:hover:bg-gray-600 hover:shadow-sm rounded-md transition-all duration-150 text-gray-400 dark:text-gray-500 hover:text-blue-600 dark:hover:text-blue-400"
                            title="Add task"
                        >
                            <Plus size={14} className="sm:w-4 sm:h-4" />
                        </button>
                    )}
                </div>

                {isDayOff && (
                    <div className="mt-2">
            <span className="text-xs bg-purple-200 dark:bg-purple-800 text-purple-700 dark:text-purple-300 px-2 py-1 rounded-full">
              Day Off
            </span>
                    </div>
                )}
            </div>

            {/* Tasks */}
            <div className="p-1 sm:p-2 flex-1 space-y-1 sm:space-y-2">
                {!isDayOff &&
                    tasks.map((task) => (
                        <TaskCard key={task.id} task={task} onClick={onTaskClick} onDone={onDone} onDelete={onDelete} />
                    ))}

                {!isDayOff && tasks.length === 0 && (
                    <div className="text-center py-4 sm:py-8">
                        <p className="text-gray-400 dark:text-gray-500 text-xs sm:text-sm">No tasks</p>
                        <button
                            onClick={() => onAddTask(date)}
                            className="mt-2 text-blue-600 dark:text-blue-400 hover:text-blue-700 dark:hover:text-blue-300 text-xs sm:text-sm font-medium"
                        >
                            Add a task
                        </button>
                    </div>
                )}

                {isDayOff && (
                    <div className="text-center py-4 sm:py-8">
                        <Calendar size={24} className="text-purple-400 dark:text-purple-500 mx-auto mb-2 sm:w-8 sm:h-8" />
                        <p className="text-purple-600 dark:text-purple-400 text-xs sm:text-sm font-medium">Day Off</p>
                        <p className="text-purple-500 dark:text-purple-400 text-xs mt-1">Enjoy your rest day!</p>
                        {isCurrentWeek && (
                            <button
                                onClick={handleDayOffClick}
                                className="mt-2 text-purple-600 dark:text-purple-400 hover:text-purple-700 dark:hover:text-purple-300 text-xs underline"
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
