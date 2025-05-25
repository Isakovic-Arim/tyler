import type {TaskResponseDto} from "~/model/task"
import TaskCard from "./TaskCard"
import {Plus} from "lucide-react"

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
}) {
    return (
        <div
            className={`border border-gray-200 rounded-lg bg-white ${isDayOff ? "bg-gray-50" : ""} min-h-[300px] flex flex-col`}
        >
            {/* Day Header */}
            <div className={`p-3 border-b border-gray-100 ${isDayOff ? "bg-gray-100" : "bg-gray-50"}`}>
                <div className="flex items-center justify-between">
                    <div>
                        <p className="text-sm font-medium text-gray-600">{dayName}</p>
                        <p className="text-lg font-bold text-gray-900">{dayNumber}</p>
                    </div>
                    <button
                        onClick={() => onAddTask(date)}
                        className="p-1 hover:bg-white hover:shadow-sm rounded-md transition-all duration-150 text-gray-400 hover:text-blue-600"
                        title="Add task"
                    >
                        <Plus size={16}/>
                    </button>
                </div>
            </div>

            {/* Tasks */}
            <div className="p-2 flex-1 space-y-2">
                {tasks.map((task) => (
                    <TaskCard key={task.id} task={task} onClick={onTaskClick} onDone={onDone} onDelete={onDelete}/>
                ))}

                {tasks.length === 0 && !isDayOff && (
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
            </div>
        </div>
    )
}
