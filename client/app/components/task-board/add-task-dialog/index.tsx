import { format } from "date-fns";
import { useState, type ChangeEvent, type FormEvent } from "react";
import { httpClient } from "~/service";
import type {TaskRequestDto} from "~/model/task";

export default function Component({parentId}: {parentId?: number}) {
    const [formData, setFormData] = useState<TaskRequestDto>({
        name: "",
        description: "",
        dueDate: format(new Date(), "yyyy-MM-dd"),
        deadline: format(new Date(), "yyyy-MM-dd"),
        parentId: parentId,
        priorityId: 1,
    });

    const handleFormChange = (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        const { name, value } = e.target;
        setFormData((prev) => ({ ...prev, [name]: value }));
    };

    const handleFormSubmit = async (e: FormEvent) => {
        e.preventDefault();
        try {
            await httpClient.post("/tasks", {
                ...formData,
                priorityId: formData.priorityId,
            });
            setFormData({
                name: "",
                description: "",
                dueDate: format(new Date(), "yyyy-MM-dd"),
                deadline: format(new Date(), "yyyy-MM-dd"),
                priorityId: 1,
            });
        } catch (error) {
            console.error("Failed to add task:", error);
        }
    };

    return <form onSubmit={handleFormSubmit} className="max-w-xl space-y-4 bg-white p-4 rounded shadow">
        <h3 className="text-lg font-semibold">Add New Task</h3>
        <div>
            <label className="block mb-1 font-medium">Task Name</label>
            <input
                name="name"
                value={formData.name}
                onChange={handleFormChange}
                required
                className="w-full border px-2 py-1 rounded"
            />
        </div>
        <div>
            <label className="block mb-1 font-medium">Description</label>
            <textarea
                name="description"
                value={formData.description}
                onChange={handleFormChange}
                className="w-full border px-2 py-1 rounded"
            />
        </div>
        <div>
            <label className="block mb-1 font-medium">Due Date</label>
            <input
                type="date"
                name="dueDate"
                value={formData.dueDate}
                onChange={handleFormChange}
                required
                className="w-full border px-2 py-1 rounded"
            />
        </div>
        <div>
            <label className="block mb-1 font-medium">Deadline</label>
            <input
                type="date"
                name="deadline"
                value={formData.deadline}
                onChange={handleFormChange}
                required
                className="w-full border px-2 py-1 rounded"
            />
        </div>
        <div>
            <label className="block mb-1 font-medium">Priority ID</label>
            <input
                type="number"
                name="priorityId"
                value={formData.priorityId}
                onChange={handleFormChange}
                required
                className="w-full border px-2 py-1 rounded"
            />
        </div>
        <button
            type="submit"
            className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700"
        >
            Add Task
        </button>
    </form>
}