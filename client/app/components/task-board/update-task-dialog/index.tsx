"use client"

import type React from "react"

import {useState, useEffect} from "react"
import type {TaskResponseDto, TaskRequestDto} from "~/model/task"
import type {PriorityResponseDto} from "~/model/priority"
import {httpClient} from "~/service"
import AddTaskPopover from "~/components/task-board/add-task-dialog"
import {X, Plus} from "lucide-react"

interface Props {
    task: TaskResponseDto | null
    onClose: () => void
    onSave: () => void
}

export default function TaskUpdatePopover({task, onClose, onSave}: Props) {
    const [priorities, setPriorities] = useState<PriorityResponseDto[]>([])
    const [showAddSubtask, setShowAddSubtask] = useState(false)
    const [form, setForm] = useState<TaskRequestDto>({
        name: "",
        description: "",
        dueDate: "",
        deadline: "",
        priorityId: 0,
    })

    useEffect(() => {
        if (!task) return

        const loadData = async () => {
            const prioritiesRes = await httpClient.get<PriorityResponseDto[]>("/priorities")
            const prioritiesData = prioritiesRes.data
            setPriorities(prioritiesData)

            setForm({
                name: task.name,
                description: task.description,
                deadline: task.deadline,
                dueDate: task.dueDate,
                priorityId: prioritiesData.find((p) => p.xp === task.remainingXp)?.id ?? 0,
            })
        }

        loadData()
    }, [task])

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
        const {name, value} = e.target
        setForm((prev) => ({...prev, [name]: name === "priorityId" ? Number.parseInt(value) : value}))
    }

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        await httpClient.put(`/tasks/${task?.id}`, form)
        onSave()
        onClose()
    }

    if (!task) return null

    return (
        <>
            {/* Backdrop */}
            <div className="fixed inset-0 bg-black/20 z-40" onClick={onClose}/>

            {/* Popover */}
            <div
                className="fixed top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 bg-white rounded-xl shadow-2xl border border-gray-200 w-full max-w-md z-50">
                <div className="flex items-center justify-between p-6 border-b border-gray-100">
                    <h3 className="text-lg font-semibold text-gray-900">Edit Task</h3>
                    <button onClick={onClose} className="p-1 hover:bg-gray-100 rounded-lg transition-colors">
                        <X size={20} className="text-gray-500"/>
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="p-6 space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">Task Name</label>
                        <input
                            type="text"
                            name="name"
                            value={form.name}
                            onChange={handleChange}
                            required
                            minLength={3}
                            maxLength={255}
                            className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">Description</label>
                        <textarea
                            name="description"
                            value={form.description}
                            onChange={handleChange}
                            maxLength={1024}
                            rows={3}
                            className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                        />
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Due Date</label>
                            <input
                                type="date"
                                name="dueDate"
                                value={form.dueDate}
                                onChange={handleChange}
                                required
                                className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Deadline</label>
                            <input
                                type="date"
                                name="deadline"
                                value={form.deadline}
                                onChange={handleChange}
                                required
                                className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                            />
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">Priority</label>
                        <select
                            name="priorityId"
                            value={form.priorityId}
                            onChange={handleChange}
                            required
                            className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                        >
                            {priorities.map((priority) => (
                                <option key={priority.id} value={priority.id}>
                                    {priority.name} ({priority.xp} XP)
                                </option>
                            ))}
                        </select>
                    </div>

                    {/*{task.subtasks > 0 && (*/}
                    <div className="bg-gray-50 rounded-lg p-3">
                        <div className="flex items-center justify-between">
                            <span className="text-sm text-gray-600">Subtasks: {task.subtasks}</span>
                            <button
                                type="button"
                                onClick={() => setShowAddSubtask(true)}
                                className="flex items-center gap-1 text-sm text-blue-600 hover:text-blue-700"
                            >
                                <Plus size={16}/>
                                Add Subtask
                            </button>
                        </div>
                    </div>
                    {/*)}*/}

                    <div className="flex justify-end space-x-3 pt-4">
                        <button
                            type="button"
                            onClick={onClose}
                            className="px-4 py-2 text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors"
                        >
                            Save Changes
                        </button>
                    </div>
                </form>
            </div>

            {/* Add Subtask Popover */}
            <AddTaskPopover
                isOpen={showAddSubtask}
                onClose={() => setShowAddSubtask(false)}
                onSave={() => {
                    setShowAddSubtask(false)
                    onSave()
                }}
                parentId={task.id}
                defaultDate={task.dueDate}
            />
        </>
    )
}
