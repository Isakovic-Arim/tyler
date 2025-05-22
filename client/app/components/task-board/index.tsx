import {useEffect, useState} from "react";
import {httpClient} from "~/service";
import {
    DndContext,
    closestCenter,
    useDraggable,
    useDroppable,
    type DragEndEvent,
} from "@dnd-kit/core";
import {
    format,
    parseISO,
    startOfWeek,
    addDays,
    addWeeks,
    isSameDay,
} from "date-fns";
import type {Task} from "~/models";

function DraggableTask({task}: { task: Task }) {
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

function DroppableColumn({
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
        <div
            ref={setNodeRef}
            className="border rounded p-2 bg-gray-50 min-h-[200px]"
        >
            <h2 className="text-lg font-bold mb-2">{dayName}</h2>
            {tasks.map((task) => (
                <DraggableTask key={task.id} task={task}/>
            ))}
        </div>
    );
}

export default function TaskBoard() {
    const [tasks, setTasks] = useState<Task[]>([]);
    const [currentWeekOffset, setCurrentWeekOffset] = useState(0);

    const startOfCurrentWeek = addWeeks(startOfWeek(new Date(), {weekStartsOn: 0}), currentWeekOffset);
    const daysOfWeek = Array.from({length: 7}, (_, i) => addDays(startOfCurrentWeek, i));

    useEffect(() => {
        httpClient.get<Task[]>("tasks").then((res) => {
            setTasks(res.data);
        });
    }, []);

    const groupedTasks = daysOfWeek.reduce<Record<string, Task[]>>((acc, date) => {
        const dayKey = format(date, "yyyy-MM-dd");
        acc[dayKey] = tasks.filter((task) => isSameDay(parseISO(task.dueDate), date));
        return acc;
    }, {});

    const handleDragEnd = (event: DragEndEvent) => {
        const {over, active} = event;
        if (!over) return;

        const taskId = parseInt(active.id as string);
        const newDateKey = over.id as string;

        setTasks((prevTasks) =>
            prevTasks.map((task) =>
                task.id === taskId ? {...task, dueDate: newDateKey} : task
            )
        );
    };

    return (
        <div className="p-4">
            <div className="flex justify-between mb-4">
                <button
                    onClick={() => setCurrentWeekOffset((prev) => prev - 1)}
                    className="px-4 py-2 bg-blue-500 text-white rounded shadow"
                >
                    Previous Week
                </button>
                <h2 className="text-xl font-bold">Week of {format(startOfCurrentWeek, "MMM d, yyyy")}</h2>
                <button
                    onClick={() => setCurrentWeekOffset((prev) => prev + 1)}
                    className="px-4 py-2 bg-blue-500 text-white rounded shadow"
                >
                    Next Week
                </button>
            </div>

            <DndContext collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
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
        </div>
    );
}
