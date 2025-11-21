import { defineStore } from 'pinia'
import { ref } from 'vue'

export type ToastType = 'success' | 'error' | 'warning' | 'info'

export interface Toast {
    id: string
    message: string
    type: ToastType
    duration?: number
}

export interface ConfirmOptions {
    title?: string
    message: string
    confirmText?: string
    cancelText?: string
    type?: 'info' | 'warning' | 'danger'
    showCancel?: boolean
    onConfirm?: () => void
    onCancel?: () => void
}

export const useUIStore = defineStore('ui', () => {
    // Toast State
    const toasts = ref<Toast[]>([])

    const addToast = (message: string, type: ToastType = 'info', duration = 3000) => {
        const id = Date.now().toString() + Math.random().toString(36).substring(2, 9)
        const toast: Toast = { id, message, type, duration }
        toasts.value.push(toast)

        if (duration > 0) {
            setTimeout(() => {
                removeToast(id)
            }, duration)
        }
    }

    const removeToast = (id: string) => {
        const index = toasts.value.findIndex(t => t.id === id)
        if (index !== -1) {
            toasts.value.splice(index, 1)
        }
    }

    // Confirm Modal State
    const confirmModalVisible = ref(false)
    const confirmOptions = ref<ConfirmOptions>({
        message: '',
        type: 'info',
        showCancel: true
    })

    const showConfirm = (options: ConfirmOptions) => {
        confirmOptions.value = {
            title: '提示',
            confirmText: '确定',
            cancelText: '取消',
            type: 'info',
            showCancel: true,
            ...options
        }
        confirmModalVisible.value = true
    }

    const closeConfirm = () => {
        confirmModalVisible.value = false
    }

    const handleConfirm = () => {
        if (confirmOptions.value.onConfirm) {
            confirmOptions.value.onConfirm()
        }
        closeConfirm()
    }

    const handleCancel = () => {
        if (confirmOptions.value.onCancel) {
            confirmOptions.value.onCancel()
        }
        closeConfirm()
    }

    return {
        toasts,
        addToast,
        removeToast,

        confirmModalVisible,
        confirmOptions,
        showConfirm,
        closeConfirm,
        handleConfirm,
        handleCancel
    }
})
