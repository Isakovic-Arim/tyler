import { useEffect, useState } from "react"
import { X, AlertCircle, CheckCircle, AlertTriangle, Info } from "lucide-react"
import { useToast, type Toast } from "./toast-context"

interface ToastItemProps {
    toast: Toast
}

export default function ToastItem({ toast }: ToastItemProps) {
    const { removeToast } = useToast()
    const [isVisible, setIsVisible] = useState(false)
    const [isLeaving, setIsLeaving] = useState(false)

    useEffect(() => {
        // Trigger entrance animation
        const timer = setTimeout(() => setIsVisible(true), 10)
        return () => clearTimeout(timer)
    }, [])

    const handleClose = () => {
        setIsLeaving(true)
        setTimeout(() => {
            removeToast(toast.id)
        }, 300)
    }

    const getToastStyles = () => {
        const baseStyles = "pointer-events-auto"

        switch (toast.type) {
            case "error":
                return `${baseStyles} bg-red-50/90 dark:bg-red-900/90 border-red-200 dark:border-red-800 text-red-800 dark:text-red-200`
            case "success":
                return `${baseStyles} bg-green-50/90 dark:bg-green-900/90 border-green-200 dark:border-green-800 text-green-800 dark:text-green-200`
            case "warning":
                return `${baseStyles} bg-yellow-50/90 dark:bg-yellow-900/90 border-yellow-200 dark:border-yellow-800 text-yellow-800 dark:text-yellow-200`
            case "info":
                return `${baseStyles} bg-blue-50/90 dark:bg-blue-900/90 border-blue-200 dark:border-blue-800 text-blue-800 dark:text-blue-200`
            default:
                return `${baseStyles} bg-gray-50/90 dark:bg-gray-800/90 border-gray-200 dark:border-gray-700 text-gray-800 dark:text-gray-200`
        }
    }

    const getIcon = () => {
        const iconProps = { size: 20, className: "flex-shrink-0" }

        switch (toast.type) {
            case "error":
                return <AlertCircle {...iconProps} className="flex-shrink-0 text-red-600 dark:text-red-400" />
            case "success":
                return <CheckCircle {...iconProps} className="flex-shrink-0 text-green-600 dark:text-green-400" />
            case "warning":
                return <AlertTriangle {...iconProps} className="flex-shrink-0 text-yellow-600 dark:text-yellow-400" />
            case "info":
                return <Info {...iconProps} className="flex-shrink-0 text-blue-600 dark:text-blue-400" />
            default:
                return <Info {...iconProps} className="flex-shrink-0 text-gray-600 dark:text-gray-400" />
        }
    }

    return (
        <div
            className={`
        transform transition-all duration-300 ease-out
        ${isVisible && !isLeaving ? "translate-y-0 opacity-100 scale-100" : "translate-y-4 opacity-0 scale-95"}
        ${isLeaving ? "translate-y-2 opacity-0 scale-95" : ""}
      `}
        >
            <div
                className={`
          ${getToastStyles()}
          backdrop-blur-sm border rounded-lg shadow-lg
          px-4 py-3 max-w-md min-w-[300px]
          flex items-center gap-3
        `}
            >
                {getIcon()}

                <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium break-words">{toast.message}</p>
                </div>

                <button
                    onClick={handleClose}
                    className="flex-shrink-0 p-1 hover:bg-black/10 dark:hover:bg-white/10 rounded-md transition-colors duration-150"
                    aria-label="Close notification"
                >
                    <X size={16} className="text-current opacity-70 hover:opacity-100" />
                </button>
            </div>
        </div>
    )
}
