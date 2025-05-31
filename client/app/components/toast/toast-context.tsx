import type React from "react"
import { createContext, useContext, useState, useCallback } from "react"

export interface Toast {
    id: string
    message: string
    type: "error" | "success" | "warning" | "info"
    duration?: number
}

interface ToastContextType {
    toasts: Toast[]
    addToast: (toast: Omit<Toast, "id">) => void
    removeToast: (id: string) => void
    showError: (message: string, duration?: number) => void
    showSuccess: (message: string, duration?: number) => void
    showWarning: (message: string, duration?: number) => void
    showInfo: (message: string, duration?: number) => void
}

const ToastContext = createContext<ToastContextType | undefined>(undefined)

export function ToastProvider({ children }: { children: React.ReactNode }) {
    const [toasts, setToasts] = useState<Toast[]>([])

    const addToast = useCallback((toast: Omit<Toast, "id">) => {
        const id = Math.random().toString(36).substr(2, 9)
        const newToast = { ...toast, id }

        setToasts((prev) => [...prev, newToast])

        // Auto remove toast after duration
        const duration = toast.duration || 5000
        setTimeout(() => {
            removeToast(id)
        }, duration)
    }, [])

    const removeToast = useCallback((id: string) => {
        setToasts((prev) => prev.filter((toast) => toast.id !== id))
    }, [])

    const showError = useCallback(
        (message: string, duration?: number) => {
            addToast({ message, type: "error", duration })
        },
        [addToast],
    )

    const showSuccess = useCallback(
        (message: string, duration?: number) => {
            addToast({ message, type: "success", duration })
        },
        [addToast],
    )

    const showWarning = useCallback(
        (message: string, duration?: number) => {
            addToast({ message, type: "warning", duration })
        },
        [addToast],
    )

    const showInfo = useCallback(
        (message: string, duration?: number) => {
            addToast({ message, type: "info", duration })
        },
        [addToast],
    )

    return (
        <ToastContext.Provider
            value={{
                toasts,
                addToast,
                removeToast,
                showError,
                showSuccess,
                showWarning,
                showInfo,
            }}
        >
            {children}
        </ToastContext.Provider>
    )
}

export function useToast() {
    const context = useContext(ToastContext)
    if (context === undefined) {
        throw new Error("useToast must be used within a ToastProvider")
    }
    return context
}
