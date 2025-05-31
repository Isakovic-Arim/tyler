import { createContext, useContext, useEffect, useState, type ReactNode } from "react"

type Theme = "light" | "dark" | "system"

interface ThemeContextType {
    theme: Theme
    actualTheme: "light" | "dark"
    setTheme: (theme: Theme) => void
    toggleTheme: () => void
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined)

export function ThemeProvider({ children }: { children: ReactNode }) {
    const [theme, setTheme] = useState<Theme>("system")
    const [actualTheme, setActualTheme] = useState<"light" | "dark">("light")

    // Get system preference
    const getSystemTheme = (): "light" | "dark" => {
        if (typeof window !== "undefined") {
            return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light"
        }
        return "light"
    }

    // Initialize theme from localStorage or system preference
    useEffect(() => {
        const savedTheme = localStorage.getItem("theme") as Theme
        if (savedTheme && ["light", "dark", "system"].includes(savedTheme)) {
            setTheme(savedTheme)
        }
    }, [])

    // Update actual theme based on theme setting
    useEffect(() => {
        const updateActualTheme = () => {
            const newActualTheme = theme === "system" ? getSystemTheme() : theme
            setActualTheme(newActualTheme)

            // Apply theme to document using Tailwind v4 approach
            const root = document.documentElement
            if (newActualTheme === "dark") {
                root.classList.add("dark")
            } else {
                root.classList.remove("dark")
            }
        }

        updateActualTheme()

        // Listen for system theme changes
        if (theme === "system") {
            const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)")
            const handleChange = () => updateActualTheme()
            mediaQuery.addEventListener("change", handleChange)
            return () => mediaQuery.removeEventListener("change", handleChange)
        }
    }, [theme])

    const handleSetTheme = (newTheme: Theme) => {
        setTheme(newTheme)
        localStorage.setItem("theme", newTheme)
    }

    const toggleTheme = () => {
        const newTheme = actualTheme === "light" ? "dark" : "light"
        handleSetTheme(newTheme)
    }

    return (
        <ThemeContext.Provider
            value={{
                theme,
                actualTheme,
                setTheme: handleSetTheme,
                toggleTheme,
            }}
        >
            {children}
        </ThemeContext.Provider>
    )
}

export function useTheme() {
    const context = useContext(ThemeContext)
    if (context === undefined) {
        throw new Error("useTheme must be used within a ThemeProvider")
    }
    return context
}
