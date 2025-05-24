import type {UserProfile} from '~/model/user'

export default function Component({user}: {user: UserProfile}) {
    return <>
        <p>Streak: {user.currentStreak}</p>
        <p>Quota: {user.dailyQuota}</p>
        <p>Xp: {user.currentXp}</p>
    </>
}