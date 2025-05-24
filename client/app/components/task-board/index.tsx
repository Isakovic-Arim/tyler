import {useEffect, useState} from "react";
import {httpClient} from "~/service";
import {
    format,
    parseISO,
    startOfWeek,
    addDays,
    addWeeks,
    isSameDay,
} from "date-fns";
import type {TaskResponseDto} from "~/model/task";
import TaskCalendar from './calendar-view'
import AddTaskDialog from "./add-task-dialog";
import TaskUpdateDialog from './update-task-dialog'

export default function TaskBoard({daysOff}: { daysOff: string[] }) {
    const [tasks, setTasks] = useState<TaskResponseDto[]>([]);
    const [selectedTask, setSelectedTask] = useState<TaskResponseDto | null>(null);
    const [currentWeekOffset, setCurrentWeekOffset] = useState(0);

    const startOfCurrentWeek = addWeeks(startOfWeek(new Date(), {weekStartsOn: 0}), currentWeekOffset);
    const daysOfWeek = Array.from({length: 7}, (_, i) => addDays(startOfCurrentWeek, i));

    useEffect(() => {
        httpClient.get<TaskResponseDto[]>("tasks")
            .then((res) => {
                setTasks(res.data);
            });
    }, []);

    const groupedTasks = daysOfWeek.reduce<Record<string, TaskResponseDto[]>>((acc, date) => {
        const dayKey = format(date, "yyyy-MM-dd");
        acc[dayKey] = tasks.filter((task) => isSameDay(parseISO(task.dueDate), date));
        return acc;
    }, {});

    const handleTaskClick = (task: TaskResponseDto) => {
        setSelectedTask(task);
    };

    const handleDone = async (task: TaskResponseDto) => {
        await httpClient.patch(`tasks/${task.id}/done`)
    }

    const handleDelete = async (task: TaskResponseDto) => {
        await httpClient.delete(`tasks/${task.id}`);
    }

    const handleRefresh = () => {
        httpClient.get<TaskResponseDto[]>("tasks")
            .then((res) => setTasks(res.data));
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
                daysOff={daysOff}
                groupedTasks={groupedTasks}
                onTaskClick={handleTaskClick}
                onDone={handleDone}
                onDelete={handleDelete}
            />
            <AddTaskDialog/>
            <TaskUpdateDialog id={selectedTask?.id} onClose={() => setSelectedTask(null)} onSave={handleRefresh}/>
        </div>
    );
}
