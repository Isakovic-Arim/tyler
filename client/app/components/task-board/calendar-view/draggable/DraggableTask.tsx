import {useDraggable} from "@dnd-kit/core";
import {format, parseISO} from "date-fns";
import type {Task} from "~/models";

export default function DraggableTask({task}: { task: Task }) {
    const {attributes, listeners, setNodeRef, transform, isDragging} = useDraggable({
        id: task.id.toString(),
        data: {task},
    });

    const style = {
        transform: transform ? `translate(${transform.x}px, ${transform.y}px)` : undefined,
        opacity: isDragging ? 0.5 : 1,
    };

    return (
        <div
            ref={setNodeRef}
            {...listeners}
            {...attributes}
            style={style}
            className="bg-white p-2 rounded shadow mb-2 cursor-move"
        >
            <p className="font-medium">{task.name}</p>
            <p className="text-sm text-gray-500">{format(parseISO(task.dueDate), "MMM d")}</p>
        </div>
    );
}
