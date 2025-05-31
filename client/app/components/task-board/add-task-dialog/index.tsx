import { format } from "date-fns"
import { useState, useEffect, type ChangeEvent, type FormEvent } from "react"
import { httpClient } from "~/service"
import type { TaskRequestDto } from "~/model/task"
import type { PriorityResponseDto } from "~/model/priority"
import { X } from "lucide-react"

interface Props {
    isOpen: boolean
    onClose: () => void
    onSave: () => void
    defaultDate?: string
    parentId?: number
}

export default function AddTaskPopover({ isOpen, onClose, onSave, defaultDate, parentId }: Props) {
    const [priorities, setPriorities] = useState<PriorityResponseDto[]>([])
    const [formData, setFormData] = useState<TaskRequestDto>({
        name: "",
        description: "",
        dueDate: defaultDate || format(new Date(), "yyyy-MM-dd"),
        deadline: defaultDate || format(new Date(), "yyyy-MM-dd"),
        parentId: parentId,
        priorityId: 1,
    })

    useEffect(() => {
        if (isOpen) {
            httpClient
                .get<PriorityResponseDto[]>("/priorities")
                .then((res) => setPriorities(res.data))
                .catch(console.error)
        }
    }, [isOpen])

    useEffect(() => {
        if (defaultDate) {
            setFormData((prev) => ({
                ...prev,
                dueDate: defaultDate,
                deadline: defaultDate,
            }))
        }
    }, [defaultDate])

    const handleFormChange = (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
        const { name, value } = e.target
        setFormData((prev) => ({ ...prev, [name]: name === "priorityId" ? Number.parseInt(value) : value }))
    }

    const handleFormSubmit = async (e: FormEvent) => {
        e.preventDefault()
        try {
            await httpClient.post("/tasks", formData)
            setFormData({
                name: "",
                description: "",
                dueDate: defaultDate || format(new Date(), "yyyy-MM-dd"),
                deadline: defaultDate || format(new Date(), "yyyy-MM-dd"),
                parentId: parentId,
                priorityId: 1,
            })
            onSave()
            onClose()
        } catch (error) {
            console.error("Failed to add task:", error)
        }
    }

    if (!isOpen) return null

    return (
        <>
            {/* Backdrop */}
            <div className="fixed inset-0 bg-black/20 dark:bg-black/40 z-40" onClick={onClose} />

            {/* Popover */}
            <div className="fixed top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 bg-white dark:bg-gray-800 rounded-xl shadow-2xl border border-gray-200 dark:border-gray-700 w-full max-w-md z-50">
                <div className="flex items-center justify-between p-6 border-b border-gray-100 dark:border-gray-700">
                    <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Add New Task</h3>
                    <button
                        onClick={onClose}
                        className="p-1 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
                    >
                        <X size={20} className="text-gray-500 dark:text-gray-400" />
                    </button>
                </div>

                <form onSubmit={handleFormSubmit} className="p-6 space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Task Name</label>
                        <input
                            name="name"
                            value={formData.name}
                            onChange={handleFormChange}
                            required
                            className="w-full border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                            placeholder="Enter task name..."
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Description</label>
                        <textarea
                            name="description"
                            value={formData.description}
                            onChange={handleFormChange}
                            rows={3}
                            className="w-full border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                            placeholder="Add description..."
                        />
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Due Date</label>
                            <input
                                type="date"
                                name="dueDate"
                                value={formData.dueDate}
                                onChange={handleFormChange}
                                required
                                className="w-full border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Deadline</label>
                            <input
                                type="date"
                                name="deadline"
                                value={formData.deadline}
                                onChange={handleFormChange}
                                required
                                className="w-full border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                            />
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Priority</label>
                        <select
                            name="priorityId"
                            value={formData.priorityId}
                            onChange={handleFormChange}
                            required
                            className="w-full border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                        >
                            {priorities.map((priority) => (
                                <option key={priority.id} value={priority.id}>
                                    {priority.name} ({priority.xp} XP)
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="flex justify-end space-x-3 pt-4">
                        <button
                            type="button"
                            onClick={onClose}
                            className="px-4 py-2 text-gray-700 dark:text-gray-300 bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 rounded-lg transition-colors"
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors"
                        >
                            Add Task
                        </button>
                    </div>
                </form>
            </div>
        </>
    )
}
