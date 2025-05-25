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
            className={`bg-white border rounded-lg p-3 shadow-sm hover:shadow-md transition-all duration-150 cursor-pointer group ${
                task.done ? "opacity-60 bg-gray-50" : ""
            } ${isOverdue ? "border-red-200 bg-red-50" : isDueToday ? "border-orange-200 bg-orange-50" : "border-gray-200"}`}
        >
            <div onClick={() => onClick(task)} className="mb-2">
                <h4 className={`font-medium text-sm ${task.done ? "line-through text-gray-500" : "text-gray-900"}`}>
                    {task.name}
                </h4>
                {task.description && <p className="text-xs text-gray-500 mt-1 line-clamp-2">{task.description}</p>}
            </div>

            <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                    <input
                        type="checkbox"
                        checked={task.done}
                        onChange={() => onDone(task)}
                        className="w-4 h-4 text-blue-600 rounded focus:ring-blue-500"
                        onClick={(e) => e.stopPropagation()}
                    />
                    <div className="flex items-center gap-1 text-xs text-gray-500">
                        <Calendar size={12} />
                        <span>{format(dueDate, "MMM d")}</span>
                    </div>
                </div>

                <div className="flex items-center gap-1">
                    {task.subtasks > 0 && (
                        <span className="text-xs bg-blue-100 text-blue-700 px-2 py-1 rounded-full">{task.subtasks}</span>
                    )}
                    <span className="text-xs bg-green-100 text-green-700 px-2 py-1 rounded-full">{task.xp} XP</span>
                    <button
                        onClick={(e) => {
                            e.stopPropagation()
                            onDelete(task)
                        }}
                        className="opacity-0 group-hover:opacity-100 p-1 hover:bg-red-100 rounded text-red-500 transition-all duration-150"
                    >
                        <Trash2 size={12} />
                    </button>
                </div>
            </div>
        </div>
    )
}
