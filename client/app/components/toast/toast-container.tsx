import { useToast } from "./toast-context"
import ToastItem from "./toast-item"

export default function ToastContainer() {
    const { toasts } = useToast()

    if (toasts.length === 0) return null

    return (
        <div className="fixed bottom-6 left-1/2 transform -translate-x-1/2 z-[9999] flex flex-col gap-2 pointer-events-none">
            {toasts.map((toast) => (
                <ToastItem key={toast.id} toast={toast} />
            ))}
        </div>
    )
}
