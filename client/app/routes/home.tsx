import type { Route } from "./+types/home";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "Tyler" },
    { name: "description", content: "Task management for losers like you." },
  ];
}

export default function Home() {
  return <h1>Tyler</h1>;
}
