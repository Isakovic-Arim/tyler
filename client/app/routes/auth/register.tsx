import type React from "react"
import { useState, useEffect } from "react"
import { httpClient, setGlobalToast } from "~/service"
import { useNavigate, Link } from "react-router"
import { Eye, EyeOff, User, Lock, UserPlus, AlertCircle, CheckCircle } from "lucide-react"
import { useToast } from "~/components/toast"
import { ThemeToggle } from "~/components/theme"

export default function RegisterPage() {
    const navigate = useNavigate()
    const { showError, showSuccess } = useToast()

    const [formData, setFormData] = useState({
        username: "",
        password: "",
        confirmPassword: "",
    })
    const [showPassword, setShowPassword] = useState(false)
    const [showConfirmPassword, setShowConfirmPassword] = useState(false)
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState("")
    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

    // Set up global toast for HTTP client
    useEffect(() => {
        setGlobalToast(showError)
    }, [showError])

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target
        setFormData((prev) => ({ ...prev, [name]: value }))

        // Clear errors when user starts typing
        if (error) setError("")
        if (fieldErrors[name]) {
            setFieldErrors((prev) => ({ ...prev, [name]: "" }))
        }
    }

    const validateForm = () => {
        const errors: Record<string, string> = {}

        // Username validation
        if (!formData.username.trim()) {
            errors.username = "Username is required"
        } else if (formData.username.trim().length < 3) {
            errors.username = "Username must be at least 3 characters"
        } else if (formData.username.trim().length > 20) {
            errors.username = "Username must be less than 20 characters"
        } else if (!/^[a-zA-Z0-9_-]+$/.test(formData.username.trim())) {
            errors.username = "Username can only contain letters, numbers, hyphens, and underscores"
        }

        // Password validation
        if (!formData.password) {
            errors.password = "Password is required"
        } else if (formData.password.length < 6) {
            errors.password = "Password must be at least 6 characters"
        } else if (formData.password.length > 128) {
            errors.password = "Password must be less than 128 characters"
        }

        // Confirm password validation
        if (!formData.confirmPassword) {
            errors.confirmPassword = "Please confirm your password"
        } else if (formData.password !== formData.confirmPassword) {
            errors.confirmPassword = "Passwords do not match"
        }

        setFieldErrors(errors)
        return Object.keys(errors).length === 0
    }

    const getPasswordStrength = () => {
        const password = formData.password
        if (!password) return { strength: 0, label: "", color: "" }

        let score = 0
        if (password.length >= 8) score++
        if (/[a-z]/.test(password)) score++
        if (/[A-Z]/.test(password)) score++
        if (/[0-9]/.test(password)) score++
        if (/[^a-zA-Z0-9]/.test(password)) score++

        if (score <= 2) return { strength: score * 20, label: "Weak", color: "bg-red-500" }
        if (score === 3) return { strength: 60, label: "Fair", color: "bg-yellow-500" }
        if (score === 4) return { strength: 80, label: "Good", color: "bg-blue-500" }
        return { strength: 100, label: "Strong", color: "bg-green-500" }
    }

    const passwordStrength = getPasswordStrength()

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        setIsLoading(true)
        setError("")

        if (!validateForm()) {
            setIsLoading(false)
            return
        }

        try {
            const response = await httpClient.post("/auth/register", {
                username: formData.username.trim(),
                password: formData.password,
            })

            if (response.status === 200) {
                showSuccess("Account created successfully! Please sign in.")
                navigate("/login")
            }
        } catch (error: any) {
            console.error("Error during registration:", error)

            if (error.response?.status === 409) {
                setFieldErrors({ username: "Username already exists" })
            } else if (error.response?.status === 400) {
                setError("Invalid registration data. Please check your inputs.")
            } else {
                setError("Registration failed. Please try again.")
            }
        } finally {
            setIsLoading(false)
        }
    }

    return (
        <div className="min-h-screen bg-gradient-to-br from-purple-50 via-white to-blue-50 dark:from-gray-900 dark:via-gray-800 dark:to-gray-900 flex items-center justify-center p-4">
            <div className="w-full max-w-md">
                {/* Theme Toggle */}
                <div className="flex justify-end mb-4">
                    <ThemeToggle />
                </div>

                {/* Header */}
                <div className="text-center mb-8">
                    <div className="w-16 h-16 bg-purple-600 dark:bg-purple-500 rounded-full flex items-center justify-center mx-auto mb-4">
                        <UserPlus size={32} className="text-white" />
                    </div>
                    <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">Create Account</h1>
                    <p className="text-gray-600 dark:text-gray-400">Join Tyler and start managing your tasks</p>
                </div>

                {/* Register Form */}
                <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-xl border border-gray-100 dark:border-gray-700 p-8">
                    <form onSubmit={handleSubmit} className="space-y-6">
                        {/* General Error Message */}
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
                                    className={`w-full pl-10 pr-4 py-3 border rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all duration-200 bg-gray-50 dark:bg-gray-700 focus:bg-white dark:focus:bg-gray-600 text-gray-900 dark:text-white ${
                                        fieldErrors.username ? "border-red-300 dark:border-red-600" : "border-gray-300 dark:border-gray-600"
                                    }`}
                                    placeholder="Choose a username"
                                    autoComplete="username"
                                    disabled={isLoading}
                                />
                            </div>
                            {fieldErrors.username && (
                                <p className="mt-1 text-sm text-red-600 dark:text-red-400">{fieldErrors.username}</p>
                            )}
                            <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                                3-20 characters, letters, numbers, hyphens, and underscores only
                            </p>
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
                                    className={`w-full pl-10 pr-12 py-3 border rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all duration-200 bg-gray-50 dark:bg-gray-700 focus:bg-white dark:focus:bg-gray-600 text-gray-900 dark:text-white ${
                                        fieldErrors.password ? "border-red-300 dark:border-red-600" : "border-gray-300 dark:border-gray-600"
                                    }`}
                                    placeholder="Create a password"
                                    autoComplete="new-password"
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
                            {fieldErrors.password && (
                                <p className="mt-1 text-sm text-red-600 dark:text-red-400">{fieldErrors.password}</p>
                            )}

                            {/* Password Strength Indicator */}
                            {formData.password && (
                                <div className="mt-2">
                                    <div className="flex items-center gap-2 mb-1">
                                        <div className="flex-1 bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                                            <div
                                                className={`h-2 rounded-full transition-all duration-300 ${passwordStrength.color}`}
                                                style={{ width: `${passwordStrength.strength}%` }}
                                            ></div>
                                        </div>
                                        <span className="text-xs font-medium text-gray-600 dark:text-gray-400">
                      {passwordStrength.label}
                    </span>
                                    </div>
                                    <p className="text-xs text-gray-500 dark:text-gray-400">
                                        Use 8+ characters with a mix of letters, numbers & symbols
                                    </p>
                                </div>
                            )}
                        </div>

                        {/* Confirm Password Field */}
                        <div>
                            <label
                                htmlFor="confirmPassword"
                                className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
                            >
                                Confirm Password
                            </label>
                            <div className="relative">
                                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                    <Lock size={20} className="text-gray-400 dark:text-gray-500" />
                                </div>
                                <input
                                    id="confirmPassword"
                                    name="confirmPassword"
                                    type={showConfirmPassword ? "text" : "password"}
                                    value={formData.confirmPassword}
                                    onChange={handleChange}
                                    className={`w-full pl-10 pr-12 py-3 border rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all duration-200 bg-gray-50 dark:bg-gray-700 focus:bg-white dark:focus:bg-gray-600 text-gray-900 dark:text-white ${
                                        fieldErrors.confirmPassword
                                            ? "border-red-300 dark:border-red-600"
                                            : "border-gray-300 dark:border-gray-600"
                                    }`}
                                    placeholder="Confirm your password"
                                    autoComplete="new-password"
                                    disabled={isLoading}
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-400 transition-colors"
                                    disabled={isLoading}
                                >
                                    {showConfirmPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                                </button>
                            </div>
                            {fieldErrors.confirmPassword && (
                                <p className="mt-1 text-sm text-red-600 dark:text-red-400">{fieldErrors.confirmPassword}</p>
                            )}
                            {formData.confirmPassword && formData.password === formData.confirmPassword && (
                                <div className="mt-1 flex items-center gap-1 text-green-600 dark:text-green-400">
                                    <CheckCircle size={16} />
                                    <span className="text-sm">Passwords match</span>
                                </div>
                            )}
                        </div>

                        {/* Submit Button */}
                        <button
                            type="submit"
                            disabled={isLoading}
                            className="w-full bg-purple-600 hover:bg-purple-700 disabled:bg-purple-400 text-white font-medium py-3 px-4 rounded-lg transition-all duration-200 flex items-center justify-center gap-2 disabled:cursor-not-allowed"
                        >
                            {isLoading ? (
                                <>
                                    <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                                    Creating account...
                                </>
                            ) : (
                                <>
                                    <UserPlus size={20} />
                                    Create Account
                                </>
                            )}
                        </button>
                    </form>

                    {/* Login Link */}
                    <div className="mt-6 text-center">
                        <p className="text-gray-600 dark:text-gray-400">
                            Already have an account?{" "}
                            <Link
                                to="/login"
                                className="text-purple-600 dark:text-purple-400 hover:text-purple-700 dark:hover:text-purple-300 font-medium transition-colors"
                            >
                                Sign in here
                            </Link>
                        </p>
                    </div>
                </div>

                {/* Footer */}
                <div className="text-center mt-8">
                    <p className="text-gray-500 dark:text-gray-400 text-sm">
                        By creating an account, you agree to our terms of service and privacy policy.
                    </p>
                </div>
            </div>
        </div>
    )
}
