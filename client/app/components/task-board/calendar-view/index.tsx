import {format} from "date-fns";
import type {TaskResponseDto} from "~/model/task";
import DayColumn from "./DayColumn";

export default function Component({
                                      daysOfWeek,
                                      groupedTasks,
                                      onTaskClick,
                                      onDone,
                                      onDelete
                                  }: {
    daysOfWeek: Date[];
    groupedTasks: Record<string, TaskResponseDto[]>;
    onTaskClick: (task: TaskResponseDto) => void;
    onDone: (task: TaskResponseDto) => void;
    onDelete: (task: TaskResponseDto) => void;
}) {
    return (
        <div className="grid grid-cols-7 gap-4">
            {daysOfWeek.map((date) => {
                const dayKey = format(date, "yyyy-MM-dd");
                const dayName = format(date, "EEEE");
                return (
                    <DayColumn
                        key={dayKey}
                        dayName={dayName}
                        tasks={groupedTasks[dayKey] || []}
                        onTaskClick={onTaskClick}
                        onDone={onDone}
                        onDelete={onDelete}
                    />
                );
            })}
        </div>
    );
}
