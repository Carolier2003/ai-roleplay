<template>
  <div class="fixed top-4 left-1/2 -translate-x-1/2 z-[100] flex flex-col gap-2 pointer-events-none">
    <TransitionGroup name="toast">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        class="pointer-events-auto min-w-[300px] max-w-md px-4 py-3 rounded-xl shadow-lg border backdrop-blur-md flex items-center gap-3"
        :class="getToastClasses(toast.type)"
      >
        <!-- Icon -->
        <div class="flex-shrink-0">
          <svg v-if="toast.type === 'success'" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
          </svg>
          <svg v-else-if="toast.type === 'error'" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 9l-6 6M9 9l6 6" />
          </svg>
          <svg v-else-if="toast.type === 'warning'" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <svg v-else class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 16v-4m0-4h.01" />
          </svg>
        </div>

        <!-- Message -->
        <div class="text-sm font-medium">{{ toast.message }}</div>

        <!-- Close Button -->
        <!-- <button @click="removeToast(toast.id)" class="ml-auto text-current opacity-50 hover:opacity-100">
          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button> -->
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useUIStore, type ToastType } from '@/stores/ui'

const uiStore = useUIStore()
const { toasts } = storeToRefs(uiStore)
const { removeToast } = uiStore

const getToastClasses = (type: ToastType) => {
  switch (type) {
    case 'success':
      return 'bg-green-50/90 border-green-200 text-green-700'
    case 'error':
      return 'bg-red-50/90 border-red-200 text-red-700'
    case 'warning':
      return 'bg-amber-50/90 border-amber-200 text-amber-700'
    default:
      return 'bg-white/90 border-gray-200 text-gray-700'
  }
}
</script>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from {
  opacity: 0;
  transform: translateY(-20px);
}

.toast-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}
</style>
