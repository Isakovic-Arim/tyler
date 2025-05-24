export interface TaskResponseDto {
    id: number;
    subtasks: number;
    name: string;
    description: string;
    dueDate: string;
    deadline: string;
    xp: number;
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
