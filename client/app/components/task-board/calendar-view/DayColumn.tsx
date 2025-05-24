import type {TaskResponseDto} from "~/model/task";
import TaskCard from "./TaskCard";

export default function DayColumn({dayName, isDayOff, tasks, onTaskClick, onDone, onDelete}: {
    dayName: string;
    tasks: TaskResponseDto[];
    isDayOff: boolean;
    onTaskClick: (task: TaskResponseDto) => void;
    onDone: (task: TaskResponseDto) => void;
    onDelete: (task: TaskResponseDto) => void;
}) {
    return (
        <>
            <div className={`border rounded p-2 ${isDayOff && 'bg-gray-500'} min-h-[200px]`}>
                <p className="text-lg font-bold mb-2">{dayName}</p>
                {tasks.map((task) => (
                    <TaskCard key={task.id} task={task} onClick={onTaskClick} onDone={onDone} onDelete={onDelete}/>
                ))}
            </div>
        </>
    );
}
