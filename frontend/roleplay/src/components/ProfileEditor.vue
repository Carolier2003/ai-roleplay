<template>
  <Teleport to="body">
    <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto overflow-x-hidden bg-black/50 backdrop-blur-sm p-4" @click.self="handleCancel">
      <div class="relative w-full max-w-xl transform rounded-2xl bg-white p-6 text-left shadow-xl transition-all">
        <!-- Header -->
        <div class="mb-6">
          <h3 class="text-xl font-bold text-gray-900">编辑个人资料</h3>
        </div>

        <!-- Form -->
        <div class="space-y-6">
          <!-- 头像 -->
          <div class="space-y-2">
            <label class="block text-sm font-medium text-gray-700">头像</label>
            <div class="flex flex-col sm:flex-row gap-4 items-start">
              <div class="relative w-20 h-20 flex-shrink-0 rounded-full overflow-hidden border-2 border-gray-100 group">
                <img 
                  v-if="formData.avatarUrl" 
                  :src="formData.avatarUrl" 
                  class="w-full h-full object-cover"
                  alt="Avatar"
                />
                <div v-else class="w-full h-full flex flex-col items-center justify-center bg-gradient-to-br from-indigo-500 to-purple-600 text-white font-mono">
                  <div class="text-xl mb-1 tracking-widest">◉ ◉</div>
                  <div class="text-base">◡</div>
                </div>
              </div>
              <div class="flex-1 w-full space-y-2">
                <input
                  v-model="formData.avatarUrl"
                  type="text"
                  placeholder="请输入头像URL"
                  class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-shadow"
                />
                <p class="text-xs text-gray-500">支持 http:// 或 https:// 开头的图片链接</p>
              </div>
            </div>
          </div>

          <!-- 显示名称 -->
          <div class="space-y-2">
            <label class="block text-sm font-medium text-gray-700">
              显示名称 <span class="text-red-500">*</span>
            </label>
            <div class="relative">
              <input
                v-model="formData.displayName"
                type="text"
                placeholder="请输入显示名称"
                maxlength="50"
                class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-shadow"
                :class="{ 'border-red-500 focus:ring-red-500': errors.displayName }"
              />
              <span class="absolute right-3 top-1/2 transform -translate-y-1/2 text-xs text-gray-400">
                {{ formData.displayName.length }}/50
              </span>
            </div>
            <p v-if="errors.displayName" class="text-xs text-red-500">{{ errors.displayName }}</p>
          </div>

          <!-- 邮箱 -->
          <div class="space-y-2">
            <label class="block text-sm font-medium text-gray-700">邮箱</label>
            <div class="relative">
              <input
                v-model="formData.email"
                type="email"
                placeholder="请输入邮箱地址"
                maxlength="100"
                class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-shadow"
                :class="{ 'border-red-500 focus:ring-red-500': errors.email }"
              />
              <div v-if="emailChecking" class="absolute right-3 top-1/2 transform -translate-y-1/2">
                <div class="w-4 h-4 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin"></div>
              </div>
            </div>
            <p v-if="errors.email" class="text-xs text-red-500">{{ errors.email }}</p>
          </div>

          <!-- 性别 -->
          <div class="space-y-2">
            <label class="block text-sm font-medium text-gray-700">性别</label>
            <div class="flex gap-2">
              <button
                v-for="option in genderOptions"
                :key="option.value"
                type="button"
                @click="formData.gender = option.value"
                class="px-4 py-2 text-sm font-medium rounded-md border transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
                :class="[
                  formData.gender === option.value
                    ? 'bg-indigo-600 text-white border-indigo-600'
                    : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
                ]"
              >
                {{ option.label }}
              </button>
            </div>
          </div>

          <!-- 生日 -->
          <div class="space-y-2">
            <label class="block text-sm font-medium text-gray-700">生日</label>
            <input
              v-model="formData.birthday"
              type="date"
              class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-shadow"
              :max="new Date().toISOString().split('T')[0]"
            />
            <p v-if="errors.birthday" class="text-xs text-red-500">{{ errors.birthday }}</p>
          </div>

          <!-- 手机号 -->
          <div class="space-y-2">
            <label class="block text-sm font-medium text-gray-700">手机号</label>
            <input
              v-model="formData.phoneNumber"
              type="tel"
              placeholder="请输入手机号码"
              maxlength="11"
              class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-shadow"
              :class="{ 'border-red-500 focus:ring-red-500': errors.phoneNumber }"
            />
            <p v-if="errors.phoneNumber" class="text-xs text-red-500">{{ errors.phoneNumber }}</p>
          </div>

          <!-- 个人简介 -->
          <div class="space-y-2">
            <label class="block text-sm font-medium text-gray-700">个人简介</label>
            <div class="relative">
              <textarea
                v-model="formData.bio"
                placeholder="介绍一下自己吧..."
                maxlength="200"
                rows="3"
                class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-shadow resize-none"
              ></textarea>
              <span class="absolute right-3 bottom-2 text-xs text-gray-400">
                {{ formData.bio.length }}/200
              </span>
            </div>
          </div>
        </div>

        <!-- Actions -->
        <div class="mt-8 flex justify-end gap-3">
          <button
            @click="handleCancel"
            class="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition-colors"
          >
            取消
          </button>
          <button
            @click="handleSave"
            :disabled="saving || emailChecking"
            class="px-4 py-2 text-sm font-medium text-white bg-indigo-600 border border-transparent rounded-md hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-2"
          >
            <div v-if="saving" class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
            保存
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { GENDER_OPTIONS, ProfileUtils } from '@/api/profile'
import type { UpdateProfileRequest } from '@/api/profile'

interface Props {
  visible: boolean
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}

import { useToast } from '@/composables/useToast'

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const authStore = useAuthStore()
const toast = useToast()

// Simple replacement for useMessage
const message = {
  success: (msg: string) => {
    console.log('Success:', msg)
    toast.success(msg)
  },
  error: (msg: string) => {
    console.error('Error:', msg)
    toast.error(msg)
  }
}

// 状态
const saving = ref(false)
const emailChecking = ref(false)
const genderOptions = GENDER_OPTIONS
const errors = reactive<Record<string, string>>({})

// 表单数据
const formData = reactive<UpdateProfileRequest>({
  displayName: '',
  email: '',
  avatarUrl: '',
  bio: '',
  gender: 'U',
  birthday: '',
  phoneNumber: ''
})

// 原始数据（用于重置）
const originalData = reactive<UpdateProfileRequest>({
  displayName: '',
  email: '',
  avatarUrl: '',
  bio: '',
  gender: 'U',
  birthday: '',
  phoneNumber: ''
})

// 监听弹窗显示状态
watch(() => props.visible, (newVisible) => {
  if (newVisible) {
    loadUserProfile()
  }
})

// 加载用户资料
const loadUserProfile = async () => {
  try {
    console.log('[ProfileEditor] 加载用户资料')
    
    // 先从store获取基本信息
    const userInfo = authStore.userInfo
    if (userInfo) {
      Object.assign(formData, {
        displayName: userInfo.displayName || '',
        email: userInfo.email || '',
        avatarUrl: userInfo.avatarUrl || '',
        bio: userInfo.bio || '',
        gender: userInfo.gender || 'U',
        birthday: userInfo.birthday || '',
        phoneNumber: userInfo.phoneNumber || ''
      })
    }
    
    // 尝试获取完整的用户资料
    const profile = await authStore.fetchUserProfile()
    if (profile) {
      Object.assign(formData, {
        displayName: profile.displayName || '',
        email: profile.email || '',
        avatarUrl: profile.avatarUrl || '',
        bio: profile.bio || '',
        gender: profile.gender || 'U',
        birthday: profile.birthday || '',
        phoneNumber: profile.phoneNumber || ''
      })
    }
    
    // 保存原始数据
    Object.assign(originalData, formData)
    
    console.log('[ProfileEditor] 用户资料加载完成:', formData)
  } catch (error) {
    console.error('[ProfileEditor] 加载用户资料失败:', error)
    message.error('加载用户资料失败')
  }
}

// 验证表单
const validateForm = async (): Promise<boolean> => {
  // 重置错误
  Object.keys(errors).forEach(key => delete errors[key])
  let isValid = true

  // 验证显示名称
  if (!formData.displayName) {
    errors.displayName = '请输入显示名称'
    isValid = false
  } else if (formData.displayName.length < 1 || formData.displayName.length > 50) {
    errors.displayName = '显示名称长度必须在1-50个字符之间'
    isValid = false
  } else if (!/^[\u4e00-\u9fa5a-zA-Z0-9_\-\s]+$/.test(formData.displayName)) {
    errors.displayName = '显示名称只能包含中文、英文、数字、下划线、连字符和空格'
    isValid = false
  }

  // 验证邮箱
  if (formData.email) {
    if (!ProfileUtils.validateEmail(formData.email)) {
      errors.email = '邮箱格式不正确'
      isValid = false
    } else if (formData.email !== originalData.email) {
      emailChecking.value = true
      try {
        const isAvailable = await authStore.checkEmailAvailability(formData.email)
        if (!isAvailable) {
          errors.email = '该邮箱已被其他用户使用'
          isValid = false
        }
      } catch (error) {
        console.error('Check email failed:', error)
      } finally {
        emailChecking.value = false
      }
    }
  }

  // 验证手机号
  if (formData.phoneNumber && !ProfileUtils.validatePhoneNumber(formData.phoneNumber)) {
    errors.phoneNumber = '手机号格式不正确'
    isValid = false
  }

  // 验证生日
  if (formData.birthday && !ProfileUtils.validateBirthday(formData.birthday)) {
    errors.birthday = '生日格式不正确或日期无效'
    isValid = false
  }

  return isValid
}

// 处理保存
const handleSave = async () => {
  try {
    // 验证表单
    if (!await validateForm()) return
    
    saving.value = true
    console.log('[ProfileEditor] 开始保存用户资料:', formData)
    
    // 调用更新接口
    await authStore.updateUserProfile(formData)
    
    message.success('个人资料更新成功')
    emit('success')
    emit('update:visible', false)
    
    console.log('[ProfileEditor] 用户资料保存成功')
  } catch (error: any) {
    console.error('[ProfileEditor] 保存用户资料失败:', error)
    
    if (error.message) {
      message.error(error.message)
    } else {
      message.error('保存失败，请检查输入信息')
    }
  } finally {
    saving.value = false
  }
}

// 处理取消
const handleCancel = () => {
  // 重置表单数据
  Object.assign(formData, originalData)
  // 清除错误
  Object.keys(errors).forEach(key => delete errors[key])
  
  emit('update:visible', false)
}
</script>
