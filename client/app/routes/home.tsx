import type {Route} from "./+types/home";
import {httpClient} from "~/service";
import TaskBoard from '~/components/task-board'
import {redirect, useNavigate} from 'react-router';

export function meta({}: Route.MetaArgs) {
    return [
        {title: "Tyler"},
        {name: "description", content: "Task management for losers like you."},
    ];
}

const handleLogout = async () => {
    try {
        await httpClient.post('/auth/logout');
    } catch (e) {
        console.error('Logout failed', e);
    }
};

export default function Home() {
    const navigate = useNavigate()

    return <>
        <h1>Tyler</h1>
        <button onClick={async () => {
            await handleLogout()
                .then(async () => await navigate('/login'))
        }}>Logout
        </button>
        <TaskBoard />
    </>;
}

export async function clientLoader() {
    try {
        const response = await httpClient.get('tasks');
        return response.data;
    } catch (error) {
        console.error(error);
        return redirect('/login');
    }
}