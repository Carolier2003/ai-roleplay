import { useUIStore, type ToastType } from '@/stores/ui'

export function useToast() {
    const uiStore = useUIStore()

    const show = (message: string, type: ToastType = 'info', duration = 3000) => {
        uiStore.addToast(message, type, duration)
    }

    const success = (message: string, duration = 3000) => show(message, 'success', duration)
    const error = (message: string, duration = 3000) => show(message, 'error', duration)
    const warning = (message: string, duration = 3000) => show(message, 'warning', duration)
    const info = (message: string, duration = 3000) => show(message, 'info', duration)

    return {
        show,
        success,
        error,
        warning,
        info
    }
}
