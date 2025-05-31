export interface UserProfile {
    username: string;
    currentXp: number;
    dailyQuota: number;
    currentStreak: number;
    daysOffPerWeek: number;
    daysOff: string[];
    lastAchievedDate: string;
}