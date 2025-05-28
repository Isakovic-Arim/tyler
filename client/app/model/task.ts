export interface TaskResponseDto {
    id: number;
    parentId: number;
    subtasks: number;
    name: string;
    description: string;
    dueDate: string;
    deadline: string;
    remainingXp: number;
    done: boolean;
}

export interface TaskRequestDto {
    parentId?: number;
    name: string;
    description?: string;
    dueDate: string;
    deadline: string;
    priorityId: number;
}
