import { useUIStore, type ConfirmOptions } from '@/stores/ui'

export function useConfirm() {
    const uiStore = useUIStore()

    const confirm = (message: string, title = '提示'): Promise<boolean> => {
        return new Promise((resolve) => {
            uiStore.showConfirm({
                title,
                message,
                type: 'info',
                onConfirm: () => resolve(true),
                onCancel: () => resolve(false)
            })
        })
    }

    const warning = (message: string, title = '警告'): Promise<boolean> => {
        return new Promise((resolve) => {
            uiStore.showConfirm({
                title,
                message,
                type: 'warning',
                confirmText: '确定',
                cancelText: '取消',
                onConfirm: () => resolve(true),
                onCancel: () => resolve(false)
            })
        })
    }

    const danger = (message: string, title = '危险操作'): Promise<boolean> => {
        return new Promise((resolve) => {
            uiStore.showConfirm({
                title,
                message,
                type: 'danger',
                confirmText: '删除',
                cancelText: '取消',
                onConfirm: () => resolve(true),
                onCancel: () => resolve(false)
            })
        })
    }

    const show = (options: ConfirmOptions) => {
        uiStore.showConfirm(options)
    }

    return {
        confirm,
        warning,
        danger,
        show
    }
}
