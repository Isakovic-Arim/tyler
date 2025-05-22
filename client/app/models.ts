export interface Task {
    id: number;
    subtasks: number;
    name: string;
    description: string;
    dueDate: string;
    deadline: string;
    xp: number;
    done: boolean;
}