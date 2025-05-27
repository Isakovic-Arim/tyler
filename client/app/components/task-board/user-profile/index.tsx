import type {UserProfile} from "~/model/user"
import {User, Target, Flame, Calendar} from "lucide-react"

interface Props {
    user: UserProfile
}

export default function UserProfileSidebar({user}: Props) {
    // Calculate progress percentage (0-100)
    const progressPercentage = user.dailyQuota > 0 ? Math.min((user.currentXp / user.dailyQuota) * 100, 100) : 0

    // Determine progress bar color based on completion
    const getProgressColor = () => {
        if (progressPercentage >= 100) return "bg-green-500"
        if (progressPercentage >= 75) return "bg-blue-500"
        if (progressPercentage >= 50) return "bg-yellow-500"
        return "bg-orange-500"
    }

    const getProgressTextColor = () => {
        if (progressPercentage >= 100) return "text-green-700"
        if (progressPercentage >= 75) return "text-blue-700"
        if (progressPercentage >= 50) return "text-yellow-700"
        return "text-orange-700"
    }

    return (
        <div className="w-80 bg-white border-r border-gray-200 p-6 flex flex-col">
            {/* Header */}
            <div className="flex items-center gap-3 mb-8">
                <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center">
                    <User size={24} className="text-blue-600"/>
                </div>
                <div>
                    <h2 className="text-xl font-bold text-gray-900">{user.username}</h2>
                </div>
            </div>

            {/* Daily Progress */}
            <div className="mb-8">
                <div className="flex items-center justify-between mb-3">
                    <h3 className="text-lg font-semibold text-gray-900 flex items-center gap-2">
                        <Target size={20} className="text-blue-600"/>
                        Daily Progress
                    </h3>
                    <span
                        className={`text-sm font-medium ${getProgressTextColor()}`}>{Math.round(progressPercentage)}%</span>
                </div>

                {/* Custom Progress Bar */}
                <div className="mb-4">
                    <div className="w-full bg-gray-200 rounded-full h-3 overflow-hidden">
                        <div
                            className={`h-full ${getProgressColor()} transition-all duration-500 ease-out rounded-full relative`}
                            style={{width: `${progressPercentage}%`}}
                        >
                            {/* Shine effect */}
                            <div
                                className="absolute inset-0 bg-gradient-to-r from-transparent via-white to-transparent opacity-20 animate-pulse"></div>
                        </div>
                    </div>
                    <div className="flex justify-between text-sm text-gray-600 mt-2">
                        <span>{user.currentXp} XP</span>
                        <span>{user.dailyQuota} XP Goal</span>
                    </div>
                </div>

                {/* Progress Status */}
                <div className="bg-gray-50 rounded-lg p-3">
                    {progressPercentage >= 100 ? (
                        <p className="text-green-700 text-sm font-medium">🎉 Daily goal achieved!</p>
                    ) : (
                        <p className="text-gray-600 text-sm">
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
                <div className="bg-gradient-to-r from-orange-50 to-red-50 rounded-lg p-4 border border-orange-100">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <div className="w-10 h-10 bg-orange-100 rounded-lg flex items-center justify-center">
                                <Flame size={20} className="text-orange-600"/>
                            </div>
                            <div>
                                <p className="text-sm text-gray-600">Current Streak</p>
                                <p className="text-2xl font-bold text-orange-700">{user.currentStreak}</p>
                            </div>
                        </div>
                        <div className="text-right">
                            <p className="text-xs text-orange-600">days</p>
                        </div>
                    </div>
                </div>

                {/* Total XP */}
                <div className="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-lg p-4 border border-blue-100">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                                <Target size={20} className="text-blue-600"/>
                            </div>
                            <div>
                                <p className="text-sm text-gray-600">Total XP</p>
                                <p className="text-2xl font-bold text-blue-700">{user.currentXp}</p>
                            </div>
                        </div>
                        <div className="text-right">
                            <p className="text-xs text-blue-600">points</p>
                        </div>
                    </div>
                </div>

                {/* Days Off */}
                <div className="bg-gradient-to-r from-purple-50 to-pink-50 rounded-lg p-4 border border-purple-100">
                    <div className="flex items-center gap-3 mb-2">
                        <div className="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
                            <Calendar size={20} className="text-purple-600"/>
                        </div>
                        <div>
                            <p className="text-sm text-gray-600">Days Off</p>
                            <p className="text-lg font-bold text-purple-700">{user.daysOffPerWeek} days</p>
                        </div>
                    </div>
                    {user.daysOff.length > 0 && (
                        <div className="flex flex-wrap gap-1 mt-2">
                            {user.daysOff.map((day) => (
                                <span key={day}
                                      className="text-xs bg-purple-100 text-purple-700 px-2 py-1 rounded-full">
                  {day.charAt(0) + day.slice(1).toLowerCase()}
                </span>
                            ))}
                        </div>
                    )}
                </div>
            </div>

            {/* Motivational Quote */}
            <div className="mt-auto">
                <div className="bg-gradient-to-r from-gray-50 to-gray-100 rounded-lg p-4 border border-gray-200">
                    <p className="text-sm text-gray-600 italic text-center">
                        "Success is the sum of small efforts repeated day in and day out."
                    </p>
                    <p className="text-xs text-gray-500 text-center mt-2">- Robert Collier</p>
                </div>
            </div>
        </div>
    )
}
