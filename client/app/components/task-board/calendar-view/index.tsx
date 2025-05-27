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
                                      onToggleDayOff,
                                      isCurrentWeek,
                                  }: {
    daysOfWeek: Date[]
    daysOff: string[]
    groupedTasks: Record<string, TaskResponseDto[]>
    onTaskClick: (task: TaskResponseDto) => void
    onDone: (task: TaskResponseDto) => void
    onDelete: (task: TaskResponseDto) => void
    onAddTask: (date: string) => void
    onToggleDayOff: (date: string) => void
    isCurrentWeek: boolean
}) {
    return (
        <div className="grid grid-cols-7 gap-4">
            {daysOfWeek.map((date) => {
                const dayKey = format(date, "yyyy-MM-dd")
                const dayName = format(date, "EEEE")
                const dayNumber = format(date, "d")
                const isDayOff = daysOff.includes(dayKey)

                return (
                    <DayColumn
                        key={dayKey}
                        date={dayKey}
                        dayName={dayName}
                        dayNumber={dayNumber}
                        isDayOff={isDayOff}
                        tasks={groupedTasks[dayKey] || []}
                        onTaskClick={onTaskClick}
                        onDone={onDone}
                        onDelete={onDelete}
                        onAddTask={onAddTask}
                        onToggleDayOff={onToggleDayOff}
                        isCurrentWeek={isCurrentWeek}
                    />
                )
            })}
        </div>
    )
}
