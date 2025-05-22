import {httpClient} from "~/service";

export default function TaskBoard() {
    return <>

    </>
}

export async function clientLoader() {
    await httpClient.get('tasks')
        .then(console.log)
}