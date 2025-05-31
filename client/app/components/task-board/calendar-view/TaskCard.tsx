import { format, parseISO, isToday, isPast } from "date-fns"
import type { TaskResponseDto } from "~/model/task"
import { Trash2, Calendar } from "lucide-react"

export default function TaskCard({
                                     task,
                                     onClick,
                                     onDone,
                                     onDelete,
                                 }: {
    task: TaskResponseDto
    onClick: (task: TaskResponseDto) => void
    onDone: (task: TaskResponseDto) => void
    onDelete: (task: TaskResponseDto) => void
}) {
    const dueDate = parseISO(task.dueDate)
    const isOverdue = isPast(dueDate) && !isToday(dueDate) && !task.done
    const isDueToday = isToday(dueDate)

    return (
        <div
            className={`bg-white dark:bg-gray-800 border rounded-lg p-2 sm:p-3 shadow-sm hover:shadow-md transition-all duration-150 cursor-pointer group ${
                task.done ? "opacity-60 bg-gray-50 dark:bg-gray-700" : ""
            } ${
                isOverdue
                    ? "border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-900/20"
                    : isDueToday
                        ? "border-orange-200 dark:border-orange-800 bg-orange-50 dark:bg-orange-900/20"
                        : "border-gray-200 dark:border-gray-700"
            }`}
        >
            <div onClick={() => onClick(task)} className="mb-2">
                <h4
                    className={`font-medium text-xs sm:text-sm ${task.done ? "line-through text-gray-500 dark:text-gray-400" : "text-gray-900 dark:text-white"}`}
                >
                    {task.name}
                </h4>
                {task.description && (
                    <p className="text-xs text-gray-500 dark:text-gray-400 mt-1 line-clamp-2 hidden sm:block">
                        {task.description}
                    </p>
                )}
            </div>

            <div className="flex items-center justify-between">
                <div className="flex items-center gap-1 sm:gap-2">
                    <input
                        type="checkbox"
                        checked={task.done}
                        onChange={() => onDone(task)}
                        className="w-3 h-3 sm:w-4 sm:h-4 text-blue-600 rounded focus:ring-blue-500 dark:focus:ring-blue-400 dark:bg-gray-700 dark:border-gray-600"
                        onClick={(e) => e.stopPropagation()}
                    />
                    <div className="flex items-center gap-1 text-xs text-gray-500 dark:text-gray-400">
                        <Calendar size={10} className="sm:w-3 sm:h-3" />
                        <span className="hidden sm:inline">{format(dueDate, "MMM d")}</span>
                        <span className="sm:hidden">{format(dueDate, "M/d")}</span>
                    </div>
                </div>

                <div className="flex items-center gap-1">
                    {task.subtasks > 0 && (
                        <span className="text-xs bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-300 px-1 sm:px-2 py-1 rounded-full">
              {task.subtasks}
            </span>
                    )}
                    <span className="text-xs bg-green-100 dark:bg-green-900 text-green-700 dark:text-green-300 px-1 sm:px-2 py-1 rounded-full">
            {task.remainingXp}
          </span>
                    <button
                        onClick={(e) => {
                            e.stopPropagation()
                            onDelete(task)
                        }}
                        className="opacity-0 group-hover:opacity-100 p-1 hover:bg-red-100 dark:hover:bg-red-900/30 rounded text-red-500 dark:text-red-400 transition-all duration-150"
                    >
                        <Trash2 size={10} className="sm:w-3 sm:h-3" />
                    </button>
                </div>
            </div>
        </div>
    )
}
