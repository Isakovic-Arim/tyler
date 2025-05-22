import {useDroppable} from "@dnd-kit/core";
import type {Task} from "~/models";
import DraggableTask from "./DraggableTask";

export default function DroppableColumn({
                                            dayKey,
                                            dayName,
                                            tasks,
                                        }: {
    dayKey: string;
    dayName: string;
    tasks: Task[];
}) {
    const {setNodeRef} = useDroppable({id: dayKey});

    return (
        <div ref={setNodeRef} className="border rounded p-2 bg-gray-50 min-h-[200px]">
            <p className="text-lg font-bold mb-2">{dayName}</p>
            {tasks.map((task) => (
                <DraggableTask key={task.id} task={task}/>
            ))}
        </div>
    );
}
