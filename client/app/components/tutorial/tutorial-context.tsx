import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from "react"

export interface TutorialStep {
    id: string
    title: string
    content: string
    target?: string // CSS selector for the element to highlight
    placement?: "top" | "bottom" | "left" | "right" | "center"
    spotlightRadius?: number
    disableOverlay?: boolean
    disableNext?: boolean
    onNext?: () => void
}

interface TutorialContextType {
    isActive: boolean
    currentStepIndex: number
    steps: TutorialStep[]
    startTutorial: () => void
    endTutorial: () => void
    nextStep: () => void
    prevStep: () => void
    skipTutorial: () => void
    setTutorialCompleted: (completed: boolean) => void
    isTutorialCompleted: boolean
}

const TutorialContext = createContext<TutorialContextType | undefined>(undefined)

const defaultSteps: TutorialStep[] = [
    {
        id: "welcome",
        title: "Welcome to Tyler!",
        content: "Let's take a quick tour of your new task management app. You can skip this tutorial at any time.",
        placement: "center",
        disableOverlay: true,
    },
    {
        id: "calendar-view",
        title: "Weekly Calendar",
        content: "This is your weekly view. You can see all your tasks organized by day.",
        target: ".grid-cols-1.sm\\:grid-cols-2.lg\\:grid-cols-7",
        placement: "top",
    },
    {
        id: "profile-sidebar",
        title: "Your Profile",
        content: "Here you can see your progress, XP, and current streak.",
        target: ".w-80.bg-white.border-r",
        placement: "right",
    },
    {
        id: "progress-bar",
        title: "Daily Progress",
        content: "Track your daily XP progress toward your quota here.",
        target: ".w-full.bg-gray-200.rounded-full.h-3",
        placement: "right",
    },
    {
        id: "days-off",
        title: "Days Off",
        content: "You can set up to 2 days off per week. Click on a day header to mark it as a day off.",
        target: ".bg-gradient-to-r.from-purple-50.to-pink-50",
        placement: "right",
    },
    {
        id: "add-task",
        title: "Add Tasks",
        content: "Click this button to add a new task to your week.",
        target: ".fixed.bottom-6.right-6",
        placement: "left",
        spotlightRadius: 50,
    },
    {
        id: "day-column",
        title: "Day View",
        content: "Each column represents a day. You can add tasks specific to a day by clicking the + icon.",
        target: ".grid-cols-1.sm\\:grid-cols-2.lg\\:grid-cols-7 > div:first-child",
        placement: "top",
    },
    {
        id: "week-navigation",
        title: "Navigate Weeks",
        content: "Use these buttons to navigate between different weeks.",
        target: ".flex.flex-col.sm\\:flex-row.sm\\:items-center.sm\\:justify-between",
        placement: "bottom",
    },
    {
        id: "complete",
        title: "You're All Set!",
        content:
            "That's it! You're ready to start using Tyler. You can restart this tutorial anytime from the help button.",
        placement: "center",
        disableOverlay: true,
    },
]

export const TutorialProvider = ({ children }: { children: ReactNode }) => {
    const [isActive, setIsActive] = useState(false)
    const [currentStepIndex, setCurrentStepIndex] = useState(0)
    const [steps, setSteps] = useState<TutorialStep[]>(defaultSteps)
    const [isTutorialCompleted, setIsTutorialCompleted] = useState(false)

    // Check if tutorial has been completed before
    useEffect(() => {
        const tutorialCompleted = localStorage.getItem("tutorialCompleted")
        if (tutorialCompleted === "true") {
            setIsTutorialCompleted(true)
        }
    }, [])

    const startTutorial = useCallback(() => {
        setIsActive(true)
        setCurrentStepIndex(0)
    }, [])

    const endTutorial = useCallback(() => {
        setIsActive(false)
        setCurrentStepIndex(0)
    }, [])

    const nextStep = useCallback(() => {
        const currentStep = steps[currentStepIndex]
        if (currentStep.onNext) {
            currentStep.onNext()
        }

        if (currentStepIndex < steps.length - 1) {
            setCurrentStepIndex((prev) => prev + 1)
        } else {
            endTutorial()
            setTutorialCompleted(true)
        }
    }, [currentStepIndex, steps, endTutorial])

    const prevStep = useCallback(() => {
        if (currentStepIndex > 0) {
            setCurrentStepIndex((prev) => prev - 1)
        }
    }, [currentStepIndex])

    const skipTutorial = useCallback(() => {
        endTutorial()
        setTutorialCompleted(true)
    }, [endTutorial])

    const setTutorialCompleted = useCallback((completed: boolean) => {
        setIsTutorialCompleted(completed)
        localStorage.setItem("tutorialCompleted", completed.toString())
    }, [])

    return (
        <TutorialContext.Provider
            value={{
                isActive,
                currentStepIndex,
                steps,
                startTutorial,
                endTutorial,
                nextStep,
                prevStep,
                skipTutorial,
                setTutorialCompleted,
                isTutorialCompleted,
            }}
        >
            {children}
        </TutorialContext.Provider>
    )
}

export const useTutorial = () => {
    const context = useContext(TutorialContext)
    if (context === undefined) {
        throw new Error("useTutorial must be used within a TutorialProvider")
    }
    return context
}
