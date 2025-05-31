import { Sun, Moon, Monitor } from "lucide-react"
import { useTheme } from "./theme-context"
import { useState, useRef, useEffect } from "react"

export default function ThemeToggle() {
    const { theme, setTheme } = useTheme()
    const [isOpen, setIsOpen] = useState(false)
    const dropdownRef = useRef<HTMLDivElement>(null)

    // Close dropdown when clicking outside
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
                setIsOpen(false)
            }
        }

        document.addEventListener("mousedown", handleClickOutside)
        return () => document.removeEventListener("mousedown", handleClickOutside)
    }, [])

    const getIcon = () => {
        switch (theme) {
            case "light":
                return <Sun size={18} />
            case "dark":
                return <Moon size={18} />
            case "system":
                return <Monitor size={18} />
        }
    }

    const options = [
        { value: "light" as const, label: "Light", icon: <Sun size={16} /> },
        { value: "dark" as const, label: "Dark", icon: <Moon size={16} /> },
        { value: "system" as const, label: "System", icon: <Monitor size={16} /> },
    ]

    return (
        <div className="relative" ref={dropdownRef}>
            <button
                onClick={() => setIsOpen(!isOpen)}
                className="p-2 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-lg transition-colors text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-100"
                aria-label="Toggle theme"
            >
                {getIcon()}
            </button>

            {isOpen && (
                <div className="absolute right-0 top-full mt-2 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg py-1 min-w-[120px] z-50">
                    {options.map((option) => (
                        <button
                            key={option.value}
                            onClick={() => {
                                setTheme(option.value)
                                setIsOpen(false)
                            }}
                            className={`w-full px-3 py-2 text-left flex items-center gap-2 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors text-sm ${
                                theme === option.value
                                    ? "text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/20"
                                    : "text-gray-700 dark:text-gray-300"
                            }`}
                        >
                            {option.icon}
                            {option.label}
                        </button>
                    ))}
                </div>
            )}
        </div>
    )
}
