import {format, parseISO} from "date-fns";
import type {TaskResponseDto} from "~/model/task";

export default function TaskCard({task, onClick, onDone, onDelete}: {
    task: TaskResponseDto,
    onClick: (task: TaskResponseDto) => void,
    onDone: (task: TaskResponseDto) => void,
    onDelete: (task: TaskResponseDto) => void,
}) {
    return (
        <div
            className="bg-white p-2 rounded shadow mb-2"
        >
            <p onClick={() => onClick(task)} className="font-medium">{task.name}</p>
            <p className="text-sm text-gray-500">{format(parseISO(task.dueDate), "MMM d")}</p>
            <input type='checkbox' checked={task.done} onClick={() => onDone(task)}/>
            <button onClick={() => onDelete(task)}>Delete</button>
        </div>
    );
}
