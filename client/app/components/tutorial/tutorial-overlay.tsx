import { useEffect, useState, useRef } from "react"
import { useTutorial } from "./tutorial-context"
import { ChevronRight, ChevronLeft, X } from "lucide-react"

interface ElementPosition {
    top: number
    left: number
    width: number
    height: number
}

export default function TutorialOverlay() {
    const { isActive, currentStepIndex, steps, nextStep, prevStep, skipTutorial } = useTutorial()
    const [targetPosition, setTargetPosition] = useState<ElementPosition | null>(null)
    const [tooltipPosition, setTooltipPosition] = useState({ top: 0, left: 0 })
    const tooltipRef = useRef<HTMLDivElement>(null)

    const currentStep = steps[currentStepIndex]

    // Find and measure the target element
    useEffect(() => {
        if (!isActive || !currentStep.target) return

        const updatePosition = () => {
            const targetElement = document.querySelector(currentStep.target || "")
            if (targetElement) {
                const rect = targetElement.getBoundingClientRect()
                setTargetPosition({
                    top: rect.top + window.scrollY,
                    left: rect.left + window.scrollX,
                    width: rect.width,
                    height: rect.height,
                })
            } else {
                setTargetPosition(null)
            }
        }

        updatePosition()

        // Update position on resize
        window.addEventListener("resize", updatePosition)
        return () => window.removeEventListener("resize", updatePosition)
    }, [isActive, currentStep])

    // Position the tooltip
    useEffect(() => {
        if (!isActive || !tooltipRef.current) return
        if (!targetPosition && currentStep.placement !== "center") return

        const tooltipRect = tooltipRef.current.getBoundingClientRect()
        const padding = 20

        if (currentStep.placement === "center") {
            setTooltipPosition({
                top: window.innerHeight / 2 - tooltipRect.height / 2,
                left: window.innerWidth / 2 - tooltipRect.width / 2,
            })
            return
        }

        let top = 0
        let left = 0

        switch (currentStep.placement) {
            case "top":
                top = targetPosition!.top - tooltipRect.height - padding
                left = targetPosition!.left + targetPosition!.width / 2 - tooltipRect.width / 2
                break
            case "bottom":
                top = targetPosition!.top + targetPosition!.height + padding
                left = targetPosition!.left + targetPosition!.width / 2 - tooltipRect.width / 2
                break
            case "left":
                top = targetPosition!.top + targetPosition!.height / 2 - tooltipRect.height / 2
                left = targetPosition!.left - tooltipRect.width - padding
                break
            case "right":
                top = targetPosition!.top + targetPosition!.height / 2 - tooltipRect.height / 2
                left = targetPosition!.left + targetPosition!.width + padding
                break
        }

        // Ensure tooltip stays within viewport
        if (left < padding) left = padding
        if (left + tooltipRect.width > window.innerWidth - padding) {
            left = window.innerWidth - tooltipRect.width - padding
        }
        if (top < padding) top = padding
        if (top + tooltipRect.height > window.innerHeight - padding) {
            top = window.innerHeight - tooltipRect.height - padding
        }

        setTooltipPosition({ top, left })
    }, [isActive, targetPosition, currentStep, tooltipRef])

    // Scroll target into view
    useEffect(() => {
        if (!isActive || !targetPosition) return

        // @ts-ignore
        const targetElement = document.querySelector(currentStep.target || null)
        if (targetElement) {
            targetElement.scrollIntoView({
                behavior: "smooth",
                block: "center",
            })
        }
    }, [isActive, targetPosition, currentStep])

    if (!isActive) return null

    // Calculate spotlight mask path
    const getMaskPath = () => {
        if (!targetPosition || currentStep.disableOverlay) return "M0,0 L0,100% L100%,100% L100%,0 Z"

        const radius = currentStep.spotlightRadius || 0
        const { top, left, width, height } = targetPosition
        const centerX = left + width / 2
        const centerY = top + height / 2

        if (radius > 0) {
            // Circular spotlight
            return `
        M0,0 L0,100% L100%,100% L100%,0 Z
        M${centerX},${centerY} 
        m-${radius},0 
        a${radius},${radius} 0 1,0 ${radius * 2},0 
        a${radius},${radius} 0 1,0 -${radius * 2},0 
        Z
      `
        }

        // Rectangular spotlight
        const padding = 10
        return `
      M0,0 L0,100% L100%,100% L100%,0 Z
      M${left - padding},${top - padding} 
      L${left - padding},${top + height + padding} 
      L${left + width + padding},${top + height + padding} 
      L${left + width + padding},${top - padding} 
      Z
    `
    }

    return (
        <div className="fixed inset-0 z-[9999] pointer-events-none">
            {/* Overlay with spotlight */}
            {!currentStep.disableOverlay && (
                <svg className="absolute inset-0 w-full h-full pointer-events-auto" style={{ zIndex: 1 }}>
                    <defs>
                        <mask id="spotlight-mask">
                            <rect width="100%" height="100%" fill="white" />
                            <path d={getMaskPath()} fill="black" />
                        </mask>
                    </defs>
                    <rect
                        width="100%"
                        height="100%"
                        fill="rgba(0, 0, 0, 0.7)"
                        mask="url(#spotlight-mask)"
                        onClick={(e) => e.stopPropagation()}
                    />
                </svg>
            )}

            {/* Tooltip */}
            <div
                ref={tooltipRef}
                className="absolute bg-white rounded-xl shadow-xl border border-gray-200 p-6 w-80 pointer-events-auto"
                style={{
                    top: tooltipPosition.top,
                    left: tooltipPosition.left,
                    zIndex: 2,
                }}
            >
                <div className="flex justify-between items-center mb-3">
                    <h3 className="text-lg font-bold text-gray-900">{currentStep.title}</h3>
                    <button
                        onClick={skipTutorial}
                        className="p-1 hover:bg-gray-100 rounded-full transition-colors"
                        aria-label="Close tutorial"
                    >
                        <X size={18} className="text-gray-500" />
                    </button>
                </div>

                <p className="text-gray-600 mb-6">{currentStep.content}</p>

                <div className="flex justify-between items-center">
                    <div className="flex gap-1">
                        {steps.map((_, index) => (
                            <div
                                key={index}
                                className={`w-2 h-2 rounded-full ${
                                    index === currentStepIndex ? "bg-blue-600" : "bg-gray-300"
                                } transition-colors`}
                            />
                        ))}
                    </div>

                    <div className="flex gap-2">
                        {currentStepIndex > 0 && (
                            <button
                                onClick={prevStep}
                                className="px-3 py-1.5 text-gray-700 hover:bg-gray-100 rounded-lg transition-colors flex items-center gap-1 text-sm"
                            >
                                <ChevronLeft size={16} />
                                Back
                            </button>
                        )}
                        <button
                            onClick={nextStep}
                            className="px-4 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors flex items-center gap-1 text-sm"
                            disabled={currentStep.disableNext}
                        >
                            {currentStepIndex === steps.length - 1 ? "Finish" : "Next"}
                            {currentStepIndex !== steps.length - 1 && <ChevronRight size={16} />}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    )
}
