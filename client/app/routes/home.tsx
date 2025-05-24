import type {Route} from "./+types/home";
import {httpClient} from "~/service";
import type {UserProfile} from '~/model/user'
import TaskBoard from '~/components/task-board'
import Profile from '~/components/user-profile'
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

export default function Home({loaderData}: { loaderData: UserProfile }) {
    const user = loaderData;
    const navigate = useNavigate()

    return <>
        <h1>Tyler</h1>
        <button onClick={async () => {
            await handleLogout()
                .then(async () => await navigate('/login'))
        }}>Logout
        </button>
        <TaskBoard daysOff={user.daysOff} />
        <Profile user={user} />
    </>
}

export async function clientLoader() {
    try {
        const response = await httpClient.get<UserProfile>('users/me');
        return response.data;
    } catch (error) {
        console.error(error);
        return redirect('/login');
    }
}