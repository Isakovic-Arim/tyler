import {useState, useEffect} from "react";
import type {TaskResponseDto, TaskRequestDto} from "~/model/task";
import type {PriorityResponseDto} from "~/model/priority";
import {httpClient} from '~/service'
import AddTaskDialog from '~/components/task-board/add-task-dialog'

interface Props {
    id?: number;
    onClose: () => void;
    onSave: () => void;
}

export default function TaskUpdateDialog({id, onClose, onSave}: Props) {
    const [priorities, setPriorities] = useState<PriorityResponseDto[]>([])

    const [form, setForm] = useState<TaskRequestDto>({
        name: "",
        description: "",
        dueDate: "",
        deadline: "",
        priorityId: 0
    });

    useEffect(() => {
        if (!id) return;

        const loadData = async () => {
            const prioritiesRes = await httpClient.get<PriorityResponseDto[]>('/priorities');
            const prioritiesData = prioritiesRes.data;
            setPriorities(prioritiesData);

            const taskRes = await httpClient.get<TaskResponseDto>(`/tasks/${id}`);
            const taskData = taskRes.data;

            setForm({
                name: taskData.name,
                description: taskData.description,
                deadline: taskData.deadline,
                dueDate: taskData.dueDate,
                priorityId: prioritiesData.find(p => p.xp === taskData.xp)?.id ?? 0,
            });
        };

        loadData();
    }, [id]);


    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
        const {name, value} = e.target;
        setForm(prev => ({...prev, [name]: value}));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        await httpClient.put(`/tasks/${id}`, form);
        onSave();
        onClose();
    };

    if (!id) return null;

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
            <div className="bg-white p-6 rounded shadow-md max-w-md w-full space-y-4">
                <h2 className="text-lg font-bold">Edit Task</h2>

                <input type="text" name="name" value={form.name} onChange={handleChange} required minLength={3}
                       maxLength={255}
                       placeholder="Task name" className="w-full border p-2 rounded"/>

                <textarea name="description" value={form.description} onChange={handleChange} maxLength={1024}
                          placeholder="Description" className="w-full border p-2 rounded"/>

                <input type="date" name="dueDate" value={form.dueDate} onChange={handleChange} required
                       className="w-full border p-2 rounded"/>

                <input type="date" name="deadline" value={form.deadline} onChange={handleChange} required
                       className="w-full border p-2 rounded"/>

                <select name="priorityId" value={form.priorityId} onChange={handleChange} required
                        className="w-full border p-2 rounded">
                    {priorities.map(priority => <option value={priority.id}>{priority.name}</option>)}
                </select>

                <details>
                    <summary>Add subtask</summary>
                    <AddTaskDialog parentId={id} />
                </details>

                <div className="flex justify-end space-x-2">
                    <button type="button" onClick={onClose} className="px-4 py-2 bg-gray-300 rounded">Cancel</button>
                    <button onClick={handleSubmit} className="px-4 py-2 bg-blue-600 text-white rounded">Save</button>
                </div>
            </div>
        </div>
    );
}
