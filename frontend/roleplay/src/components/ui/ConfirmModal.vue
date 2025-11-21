<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="fixed inset-0 z-[100] flex items-center justify-center p-4">
        <!-- Backdrop -->
        <div class="absolute inset-0 bg-black/30 backdrop-blur-sm" @click="handleCancel"></div>

        <!-- Modal Content -->
        <div class="relative bg-white/90 backdrop-blur-xl rounded-2xl shadow-2xl border border-white/50 w-full max-w-sm overflow-hidden transform transition-all">
          <div class="p-6 text-center">
            <!-- Icon -->
            <div class="mx-auto mb-4 w-12 h-12 rounded-full flex items-center justify-center" :class="getIconBgClass(options.type)">
              <svg v-if="options.type === 'danger'" class="w-6 h-6 text-red-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              <svg v-else-if="options.type === 'warning'" class="w-6 h-6 text-amber-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              <svg v-else class="w-6 h-6 text-indigo-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10" />
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 16v-4m0-4h.01" />
              </svg>
            </div>

            <!-- Title & Message -->
            <h3 v-if="options.title" class="text-lg font-bold text-gray-900 mb-2">{{ options.title }}</h3>
            <p class="text-sm text-gray-600 leading-relaxed">{{ options.message }}</p>
          </div>

          <!-- Actions -->
          <div class="flex border-t border-gray-100">
            <button
              v-if="options.showCancel !== false"
              @click="handleCancel"
              class="flex-1 px-4 py-3 text-sm font-medium text-gray-600 hover:bg-gray-50 transition-colors border-r border-gray-100"
            >
              {{ options.cancelText || '取消' }}
            </button>
            <button
              @click="handleConfirm"
              class="flex-1 px-4 py-3 text-sm font-bold hover:bg-gray-50 transition-colors"
              :class="getConfirmTextClass(options.type)"
            >
              {{ options.confirmText || '确定' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useUIStore } from '@/stores/ui'

const uiStore = useUIStore()
const { confirmModalVisible: visible, confirmOptions: options } = storeToRefs(uiStore)
const { handleConfirm, handleCancel } = uiStore

const getIconBgClass = (type: string | undefined) => {
  switch (type) {
    case 'danger': return 'bg-red-100'
    case 'warning': return 'bg-amber-100'
    default: return 'bg-indigo-100'
  }
}

const getConfirmTextClass = (type: string | undefined) => {
  switch (type) {
    case 'danger': return 'text-red-600'
    case 'warning': return 'text-amber-600'
    default: return 'text-indigo-600'
  }
}
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-active .transform,
.modal-leave-active .transform {
  transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.modal-enter-from .transform,
.modal-leave-to .transform {
  transform: scale(0.95);
}
</style>
