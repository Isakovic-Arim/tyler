import type {Route} from "./+types/home";
import {httpClient} from "~/service";
import Kanban from '~/components/task-board'
import {redirect, useLoaderData, useNavigate} from 'react-router';

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
    const tasks = useLoaderData()
    const navigate = useNavigate()

    return <>
        <h1>Tyler</h1>
        <button onClick={async () => {
            await handleLogout()
                .then(async () => await navigate('/login'))
        }}>Logout
        </button>
        <Kanban/>
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