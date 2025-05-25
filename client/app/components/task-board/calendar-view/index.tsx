import { format } from "date-fns"
import type { TaskResponseDto } from "~/model/task"
import DayColumn from "./DayColumn"

export default function Component({
                                      daysOfWeek,
                                      daysOff,
                                      groupedTasks,
                                      onTaskClick,
                                      onDone,
                                      onDelete,
                                      onAddTask,
                                  }: {
    daysOfWeek: Date[]
    daysOff: string[]
    groupedTasks: Record<string, TaskResponseDto[]>
    onTaskClick: (task: TaskResponseDto) => void
    onDone: (task: TaskResponseDto) => void
    onDelete: (task: TaskResponseDto) => void
    onAddTask: (date: string) => void
}) {
    return (
        <div className="grid grid-cols-7 gap-4">
            {daysOfWeek.map((date) => {
                const dayKey = format(date, "yyyy-MM-dd")
                const dayName = format(date, "EEEE")
                const dayNumber = format(date, "d")
                return (
                    <DayColumn
                        key={dayKey}
                        date={dayKey}
                        dayName={dayName}
                        dayNumber={dayNumber}
                        isDayOff={daysOff.includes(dayName.toUpperCase())}
                        tasks={groupedTasks[dayKey] || []}
                        onTaskClick={onTaskClick}
                        onDone={onDone}
                        onDelete={onDelete}
                        onAddTask={onAddTask}
                    />
                )
            })}
        </div>
    )
}
