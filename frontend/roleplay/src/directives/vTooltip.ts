import { type Directive, type DirectiveBinding } from 'vue'

const createTooltip = (text: string) => {
    const div = document.createElement('div')
    div.className = 'fixed z-[9999] px-2 py-1 text-xs font-medium text-white bg-gray-900 rounded shadow-lg pointer-events-none transition-opacity duration-200 opacity-0'
    div.textContent = text
    div.id = 'custom-tooltip'
    return div
}

const updateTooltipPosition = (el: HTMLElement, tooltip: HTMLElement) => {
    const rect = el.getBoundingClientRect()
    const tooltipRect = tooltip.getBoundingClientRect()

    const top = rect.top - tooltipRect.height - 8 // 8px gap
    const left = rect.left + (rect.width - tooltipRect.width) / 2

    tooltip.style.top = `${top}px`
    tooltip.style.left = `${left}px`
}

export const vTooltip: Directive = {
    mounted(el: HTMLElement, binding: DirectiveBinding) {
        let tooltip: HTMLElement | null = null

        const show = () => {
            if (!binding.value) return

            tooltip = createTooltip(binding.value)
            document.body.appendChild(tooltip)

            // Force reflow to get dimensions
            updateTooltipPosition(el, tooltip)

            // Show
            requestAnimationFrame(() => {
                if (tooltip) tooltip.style.opacity = '1'
            })
        }

        const hide = () => {
            if (tooltip) {
                tooltip.style.opacity = '0'
                setTimeout(() => {
                    if (tooltip && tooltip.parentNode) {
                        tooltip.parentNode.removeChild(tooltip)
                    }
                    tooltip = null
                }, 200)
            }
        }

        el.addEventListener('mouseenter', show)
        el.addEventListener('mouseleave', hide)
        el.addEventListener('click', hide) // Hide on click too

            // Cleanup
            ; (el as any)._tooltipCleanup = () => {
                el.removeEventListener('mouseenter', show)
                el.removeEventListener('mouseleave', hide)
                el.removeEventListener('click', hide)
                hide()
            }
    },

    updated(el: HTMLElement, binding: DirectiveBinding) {
        // Update text if changed
        if (binding.value !== binding.oldValue) {
            // If tooltip is currently shown, update it? 
            // For simplicity, we just rely on next hover.
        }
    },

    unmounted(el: HTMLElement) {
        if ((el as any)._tooltipCleanup) {
            (el as any)._tooltipCleanup()
        }
    }
}
