import type React from "react"
import { useState, useEffect } from "react"
import { httpClient, setGlobalToast } from "~/service"
import { useNavigate, Link } from "react-router"
import { Eye, EyeOff, User, Lock, LogIn, AlertCircle } from "lucide-react"
import { useToast } from "~/components/toast"
import { ThemeToggle } from "~/components/theme"

export default function LoginPage() {
    const navigate = useNavigate()
    const { showError, showSuccess } = useToast()

    const [formData, setFormData] = useState({
        username: "",
        password: "",
    })
    const [showPassword, setShowPassword] = useState(false)
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState("")

    // Set up global toast for HTTP client
    useEffect(() => {
        setGlobalToast(showError)
    }, [showError])

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target
        setFormData((prev) => ({ ...prev, [name]: value }))
        // Clear error when user starts typing
        if (error) setError("")
    }

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        setIsLoading(true)
        setError("")

        // Basic validation
        if (!formData.username.trim()) {
            setError("Username is required")
            setIsLoading(false)
            return
        }

        if (!formData.password) {
            setError("Password is required")
            setIsLoading(false)
            return
        }

        try {
            const response = await httpClient.post("/auth/login", {
                username: formData.username.trim(),
                password: formData.password,
            })

            if (response.status === 200) {
                showSuccess("Welcome back! Login successful.")
                navigate("/")
            }
        } catch (error: any) {
            console.error("Error during login:", error)

            if (error.response?.status === 401) {
                setError("Invalid username or password")
            } else if (error.response?.status === 429) {
                setError("Too many login attempts. Please try again later.")
            } else {
                setError("Login failed. Please try again.")
            }
        } finally {
            setIsLoading(false)
        }
    }

    return (
        <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-purple-50 dark:from-gray-900 dark:via-gray-800 dark:to-gray-900 flex items-center justify-center p-4">
            <div className="w-full max-w-md">
                {/* Theme Toggle */}
                <div className="flex justify-end mb-4">
                    <ThemeToggle />
                </div>

                {/* Header */}
                <div className="text-center mb-8">
                    <div className="w-16 h-16 bg-blue-600 dark:bg-blue-500 rounded-full flex items-center justify-center mx-auto mb-4">
                        <LogIn size={32} className="text-white" />
                    </div>
                    <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">Welcome Back</h1>
                    <p className="text-gray-600 dark:text-gray-400">Sign in to your Tyler account</p>
                </div>

                {/* Login Form */}
                <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-xl border border-gray-100 dark:border-gray-700 p-8">
                    <form onSubmit={handleSubmit} className="space-y-6">
                        {/* Error Message */}
                        {error && (
                            <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4 flex items-center gap-3">
                                <AlertCircle size={20} className="text-red-600 dark:text-red-400 flex-shrink-0" />
                                <p className="text-red-700 dark:text-red-400 text-sm">{error}</p>
                            </div>
                        )}

                        {/* Username Field */}
                        <div>
                            <label htmlFor="username" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                                Username
                            </label>
                            <div className="relative">
                                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                    <User size={20} className="text-gray-400 dark:text-gray-500" />
                                </div>
                                <input
                                    id="username"
                                    name="username"
                                    type="text"
                                    value={formData.username}
                                    onChange={handleChange}
                                    className="w-full pl-10 pr-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 bg-gray-50 dark:bg-gray-700 focus:bg-white dark:focus:bg-gray-600 text-gray-900 dark:text-white"
                                    placeholder="Enter your username"
                                    autoComplete="username"
                                    disabled={isLoading}
                                />
                            </div>
                        </div>

                        {/* Password Field */}
                        <div>
                            <label htmlFor="password" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                                Password
                            </label>
                            <div className="relative">
                                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                    <Lock size={20} className="text-gray-400 dark:text-gray-500" />
                                </div>
                                <input
                                    id="password"
                                    name="password"
                                    type={showPassword ? "text" : "password"}
                                    value={formData.password}
                                    onChange={handleChange}
                                    className="w-full pl-10 pr-12 py-3 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 bg-gray-50 dark:bg-gray-700 focus:bg-white dark:focus:bg-gray-600 text-gray-900 dark:text-white"
                                    placeholder="Enter your password"
                                    autoComplete="current-password"
                                    disabled={isLoading}
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowPassword(!showPassword)}
                                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-400 transition-colors"
                                    disabled={isLoading}
                                >
                                    {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                                </button>
                            </div>
                        </div>

                        {/* Submit Button */}
                        <button
                            type="submit"
                            disabled={isLoading}
                            className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-medium py-3 px-4 rounded-lg transition-all duration-200 flex items-center justify-center gap-2 disabled:cursor-not-allowed"
                        >
                            {isLoading ? (
                                <>
                                    <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                                    Signing in...
                                </>
                            ) : (
                                <>
                                    <LogIn size={20} />
                                    Sign In
                                </>
                            )}
                        </button>
                    </form>

                    {/* Register Link */}
                    <div className="mt-6 text-center">
                        <p className="text-gray-600 dark:text-gray-400">
                            Don't have an account?{" "}
                            <Link
                                to="/register"
                                className="text-blue-600 dark:text-blue-400 hover:text-blue-700 dark:hover:text-blue-300 font-medium transition-colors"
                            >
                                Create one here
                            </Link>
                        </p>
                    </div>
                </div>

                {/* Footer */}
                <div className="text-center mt-8">
                    <p className="text-gray-500 dark:text-gray-400 text-sm">
                        By signing in, you agree to our terms of service and privacy policy.
                    </p>
                </div>
            </div>
        </div>
    )
}
