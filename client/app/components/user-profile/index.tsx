import type {UserProfile} from "~/model/user"
import {User, Target, Flame, Calendar, LogOut, Settings} from "lucide-react"
import {format, parseISO} from "date-fns"
import {httpClient} from "~/service"
import {useNavigate} from "react-router"
import {useState, useEffect} from "react"
import {ThemeToggle} from "~/components/theme"

interface Props {
    user: UserProfile
    currentWeekDates: Date[]
}

export default function UserProfileSidebar({user, currentWeekDates}: Props) {
    const navigate = useNavigate()
    const [isLoggingOut, setIsLoggingOut] = useState(false)

    // Calculate progress percentage (0-100)
    const isToday = (date: Date): boolean => {
        const today = new Date();
        return date.getDate() === today.getDate() &&
            date.getMonth() === today.getMonth() &&
            date.getFullYear() === today.getFullYear();
    };

    const progressPercentage = isToday(new Date(user.lastAchievedDate))
        ? 100
        : user.dailyQuota > 0
            ? Math.min((user.currentXp / user.dailyQuota) * 100, 100)
            : 0;

    // Determine progress bar color based on completion
    const getProgressColor = () => {
        if (progressPercentage >= 100) return "bg-green-500"
        if (progressPercentage >= 75) return "bg-blue-500"
        if (progressPercentage >= 50) return "bg-yellow-500"
        return "bg-orange-500"
    }

    const getProgressTextColor = () => {
        if (progressPercentage >= 100) return "text-green-700 dark:text-green-400"
        if (progressPercentage >= 75) return "text-blue-700 dark:text-blue-400"
        if (progressPercentage >= 50) return "text-yellow-700 dark:text-yellow-400"
        return "text-orange-700 dark:text-orange-400"
    }

    // Get current week days off
    const currentWeekDaysOff = user.daysOff.filter((dayOff) => {
        const dayOffDate = parseISO(dayOff)
        return currentWeekDates.some((weekDate) => format(weekDate, "yyyy-MM-dd") === format(dayOffDate, "yyyy-MM-dd"))
    })

    const handleLogout = async () => {
        setIsLoggingOut(true)
        try {
            await httpClient.post("/auth/logout")
            navigate("/login")
        } catch (error) {
            console.error("Logout failed:", error)
            // Still navigate to login even if logout fails
            navigate("/login")
        } finally {
            setIsLoggingOut(false)
        }
    }

    return (
        <div
            className="w-80 bg-white dark:bg-gray-900 border-r border-gray-200 dark:border-gray-700 p-6 flex flex-col min-h-full">
            {/* Header */}
            <div className="flex items-center gap-3 mb-8">
                <div className="w-12 h-12 bg-blue-100 dark:bg-blue-900 rounded-full flex items-center justify-center">
                    <User size={24} className="text-blue-600 dark:text-blue-400"/>
                </div>
                <div className="flex-1">
                    <h1 className="text-xl font-bold text-gray-900 dark:text-white">{user.username}</h1>
                </div>
                <div className="flex gap-1">
                    <ThemeToggle/>
                    <button
                        onClick={handleLogout}
                        disabled={isLoggingOut}
                        className="p-2 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-colors text-gray-500 dark:text-gray-400 hover:text-red-600 dark:hover:text-red-400 disabled:opacity-50 disabled:cursor-not-allowed"
                        title="Logout"
                    >
                        {isLoggingOut ? (
                            <div
                                className="w-[18px] h-[18px] border-2 border-gray-300 dark:border-gray-600 border-t-red-600 dark:border-t-red-400 rounded-full animate-spin"></div>
                        ) : (
                            <LogOut size={18}/>
                        )}
                    </button>
                </div>
            </div>

            {/* Daily Progress */}
            <div className="mb-8">
                <div className="flex items-center justify-between mb-3">
                    <h3 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
                        <Target size={20} className="text-blue-600 dark:text-blue-400"/>
                        Daily Progress
                    </h3>
                    <span
                        className={`text-sm font-medium ${getProgressTextColor()}`}>{Math.round(progressPercentage)}%</span>
                </div>

                {/* Custom Progress Bar */}
                <div className="mb-4">
                    <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-3 overflow-hidden">
                        <div
                            className={`h-full ${getProgressColor()} transition-all duration-500 ease-out rounded-full relative`}
                            style={{width: `${progressPercentage}%`}}
                        >
                            {/* Shine effect */}
                            <div
                                className="absolute inset-0 bg-gradient-to-r from-transparent via-white to-transparent opacity-20 animate-pulse"></div>
                        </div>
                    </div>
                    <div className="flex justify-between text-sm text-gray-600 dark:text-gray-400 mt-2">
                        <span>{user.currentXp} XP</span>
                        <span>{user.dailyQuota} XP Goal</span>
                    </div>
                </div>

                {/* Progress Status */}
                <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-3">
                    {progressPercentage >= 100 ? (
                        <p className="text-green-700 dark:text-green-400 text-sm font-medium">🎉 Daily goal achieved!</p>
                    ) : (
                        <p className="text-gray-600 dark:text-gray-400 text-sm">
                            <span className="font-medium">{user.dailyQuota - user.currentXp} XP</span> remaining to
                            reach your daily
                            goal
                        </p>
                    )}
                </div>
            </div>

            {/* Stats Grid */}
            <div className="space-y-4 mb-8">
                {/* Current Streak */}
                <div
                    className="bg-gradient-to-r from-orange-50 to-red-50 dark:from-orange-900/20 dark:to-red-900/20 rounded-lg p-4 border border-orange-100 dark:border-orange-800">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <div
                                className="w-10 h-10 bg-orange-100 dark:bg-orange-900 rounded-lg flex items-center justify-center">
                                <Flame size={20} className="text-orange-600 dark:text-orange-400"/>
                            </div>
                            <div>
                                <p className="text-sm text-gray-600 dark:text-gray-400">Current Streak</p>
                                <p className="text-2xl font-bold text-orange-700 dark:text-orange-400">{user.currentStreak}</p>
                            </div>
                        </div>
                        <div className="text-right">
                            <p className="text-xs text-orange-600 dark:text-orange-400">days</p>
                        </div>
                    </div>
                </div>

                {/* Total XP */}
                <div
                    className="bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-blue-900/20 dark:to-indigo-900/20 rounded-lg p-4 border border-blue-100 dark:border-blue-800">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <div
                                className="w-10 h-10 bg-blue-100 dark:bg-blue-900 rounded-lg flex items-center justify-center">
                                <Target size={20} className="text-blue-600 dark:text-blue-400"/>
                            </div>
                            <div>
                                <p className="text-sm text-gray-600 dark:text-gray-400">Total XP</p>
                                <p className="text-2xl font-bold text-blue-700 dark:text-blue-400">{user.currentXp}</p>
                            </div>
                        </div>
                        <div className="text-right">
                            <p className="text-xs text-blue-600 dark:text-blue-400">points</p>
                        </div>
                    </div>
                </div>

                {/* Days Off This Week */}
                <div
                    className="bg-gradient-to-r from-purple-50 to-pink-50 dark:from-purple-900/20 dark:to-pink-900/20 rounded-lg p-4 border border-purple-100 dark:border-purple-800">
                    <div className="flex items-center gap-3 mb-2">
                        <div
                            className="w-10 h-10 bg-purple-100 dark:bg-purple-900 rounded-lg flex items-center justify-center">
                            <Calendar size={20} className="text-purple-600 dark:text-purple-400"/>
                        </div>
                        <div>
                            <p className="text-sm text-gray-600 dark:text-gray-400">Days Off This Week</p>
                            <p className="text-lg font-bold text-purple-700 dark:text-purple-400">
                                {currentWeekDaysOff.length}/2 days
                            </p>
                        </div>
                    </div>
                    {currentWeekDaysOff.length > 0 && (
                        <div className="flex flex-wrap gap-1 mt-2">
                            {currentWeekDaysOff.map((dayOff) => (
                                <span
                                    key={dayOff}
                                    className="text-xs bg-purple-100 dark:bg-purple-900 text-purple-700 dark:text-purple-300 px-2 py-1 rounded-full"
                                >
                  {format(parseISO(dayOff), "EEE, MMM d")}
                </span>
                            ))}
                        </div>
                    )}
                    {currentWeekDaysOff.length === 0 && (
                        <p className="text-xs text-purple-600 dark:text-purple-400 mt-1">
                            Click on days in the calendar to mark them as days off
                        </p>
                    )}
                </div>
            </div>

            {/* Motivational Quote */}
            <div className="mt-auto">
                <div
                    className="bg-gradient-to-r from-gray-50 to-gray-100 dark:from-gray-800 dark:to-gray-700 rounded-lg p-4 border border-gray-200 dark:border-gray-600">
                    <p className="text-sm text-gray-600 dark:text-gray-400 italic text-center">
                        "Success is the sum of small efforts repeated day in and day out."
                    </p>
                    <p className="text-xs text-gray-500 dark:text-gray-500 text-center mt-2">- Robert Collier</p>
                </div>
            </div>
        </div>
    )
}
