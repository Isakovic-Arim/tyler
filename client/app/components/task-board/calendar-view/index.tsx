import {
    DndContext,
    closestCenter,
    type DragEndEvent,
} from "@dnd-kit/core";
import {format} from "date-fns";
import type {Task} from "~/models";
import DroppableColumn from "./draggable/DroppableColumn";

export default function Component({
                                      daysOfWeek,
                                      groupedTasks,
                                      onDragEnd,
                                  }: {
    daysOfWeek: Date[];
    groupedTasks: Record<string, Task[]>;
    onDragEnd: (event: DragEndEvent) => void;
}) {
    return (
        <DndContext collisionDetection={closestCenter} onDragEnd={onDragEnd}>
            <div className="grid grid-cols-7 gap-4">
                {daysOfWeek.map((date) => {
                    const dayKey = format(date, "yyyy-MM-dd");
                    const dayName = format(date, "EEEE");
                    return (
                        <DroppableColumn
                            key={dayKey}
                            dayKey={dayKey}
                            dayName={dayName}
                            tasks={groupedTasks[dayKey] || []}
                        />
                    );
                })}
            </div>
        </DndContext>
    );
}
