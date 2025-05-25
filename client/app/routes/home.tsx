import type { Route } from "./+types/home"
import { httpClient } from "~/service"
import type { UserProfile } from "~/model/user"
import TaskBoard from "~/components/task-board"
import { redirect, useNavigate } from "react-router"

export function meta({}: Route.MetaArgs) {
    return [{ title: "Tyler" }, { name: "description", content: "Task management for losers like you." }]
}

export default function Home({ loaderData }: { loaderData: UserProfile }) {
    const user = loaderData

    return (
        <div className="h-screen">
            <TaskBoard daysOff={user.daysOff} user={user} />
        </div>
    )
}

export async function clientLoader() {
    try {
        const response = await httpClient.get<UserProfile>("users/me")
        return response.data
    } catch (error) {
        console.error(error)
        return redirect("/login")
    }
}
