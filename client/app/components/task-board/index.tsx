import {useEffect, useState} from "react";
import {httpClient} from "~/service";
import {
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
import TaskCalendar from './calendar-view'
import AddTaskDialog from "./add-task-dialog";

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

            <TaskCalendar
                daysOfWeek={daysOfWeek}
                groupedTasks={groupedTasks}
                onDragEnd={handleDragEnd}
            />

            <AddTaskDialog />
        </div>
    );
}
