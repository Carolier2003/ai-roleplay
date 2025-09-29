<template>
  <n-modal
    :show="visible"
    @update:show="(value: boolean) => $emit('update:visible', value)"
    :mask-closable="false"
    preset="dialog"
    title="编辑个人资料"
    class="profile-editor-modal"
    style="width: 600px"
  >
    <div class="profile-editor">
      <n-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-placement="left"
        label-width="80px"
        require-mark-placement="right-hanging"
      >
        <!-- 头像 -->
        <n-form-item label="头像" path="avatarUrl">
          <div class="avatar-section">
            <n-avatar
              :size="80"
              :src="formData.avatarUrl"
              class="profile-avatar"
            >
              <template #fallback>
                <div class="avatar-fallback">
                  <div class="avatar-eyes">◉ ◉</div>
                  <div class="avatar-mouth">◡</div>
                </div>
              </template>
            </n-avatar>
            <div class="avatar-actions">
              <n-input
                v-model:value="formData.avatarUrl"
                placeholder="请输入头像URL"
                clearable
              />
              <n-text depth="3" style="font-size: 12px; margin-top: 4px;">
                支持 http:// 或 https:// 开头的图片链接
              </n-text>
            </div>
          </div>
        </n-form-item>

        <!-- 显示名称 -->
        <n-form-item label="显示名称" path="displayName">
          <n-input
            v-model:value="formData.displayName"
            placeholder="请输入显示名称"
            maxlength="50"
            show-count
            clearable
          />
        </n-form-item>

        <!-- 邮箱 -->
        <n-form-item label="邮箱" path="email">
          <n-input
            v-model:value="formData.email"
            placeholder="请输入邮箱地址"
            maxlength="100"
            clearable
            :loading="emailChecking"
          />
        </n-form-item>

        <!-- 性别 -->
        <n-form-item label="性别" path="gender">
          <n-radio-group v-model:value="formData.gender">
            <n-radio-button
              v-for="option in genderOptions"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </n-radio-button>
          </n-radio-group>
        </n-form-item>

        <!-- 生日 -->
        <n-form-item label="生日" path="birthday">
          <n-date-picker
            v-model:formatted-value="formData.birthday"
            value-format="yyyy-MM-dd"
            type="date"
            placeholder="请选择生日"
            clearable
            :is-date-disabled="(ts: number) => ts > Date.now()"
          />
        </n-form-item>

        <!-- 手机号 -->
        <n-form-item label="手机号" path="phoneNumber">
          <n-input
            v-model:value="formData.phoneNumber"
            placeholder="请输入手机号码"
            maxlength="11"
            clearable
          />
        </n-form-item>

        <!-- 个人简介 -->
        <n-form-item label="个人简介" path="bio">
          <n-input
            v-model:value="formData.bio"
            type="textarea"
            placeholder="介绍一下自己吧..."
            maxlength="200"
            show-count
            :autosize="{ minRows: 3, maxRows: 5 }"
            clearable
          />
        </n-form-item>
      </n-form>
    </div>

    <template #action>
      <div class="modal-actions">
        <n-button @click="handleCancel">取消</n-button>
        <n-button
          type="primary"
          :loading="saving"
          @click="handleSave"
        >
          保存
        </n-button>
      </div>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch, nextTick } from 'vue'
import {
  NModal,
  NForm,
  NFormItem,
  NInput,
  NButton,
  NAvatar,
  NRadioGroup,
  NRadioButton,
  NDatePicker,
  NText,
  useMessage,
  type FormInst,
  type FormRules
} from 'naive-ui'
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

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const authStore = useAuthStore()
const message = useMessage()

// 表单引用
const formRef = ref<FormInst>()

// 状态
const saving = ref(false)
const emailChecking = ref(false)
const genderOptions = GENDER_OPTIONS

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

// 表单验证规则
const formRules: FormRules = {
  displayName: [
    { required: true, message: '请输入显示名称', trigger: 'blur' },
    { min: 1, max: 50, message: '显示名称长度必须在1-50个字符之间', trigger: 'blur' },
    {
      pattern: /^[\u4e00-\u9fa5a-zA-Z0-9_\-\s]+$/,
      message: '显示名称只能包含中文、英文、数字、下划线、连字符和空格',
      trigger: 'blur'
    }
  ],
  email: [
    {
      validator: (rule: any, value: string) => {
        if (!value) return true // 邮箱是可选的
        return ProfileUtils.validateEmail(value)
      },
      message: '邮箱格式不正确',
      trigger: 'blur'
    },
    {
      asyncValidator: async (rule: any, value: string): Promise<void> => {
        if (!value || value === originalData.email) return
        
        emailChecking.value = true
        try {
          const isAvailable = await authStore.checkEmailAvailability(value)
          if (!isAvailable) {
            throw new Error('该邮箱已被其他用户使用')
          }
        } finally {
          emailChecking.value = false
        }
      },
      trigger: 'blur'
    }
  ],
  avatarUrl: [
    {
      validator: (rule: any, value: string) => {
        if (!value) return true // 头像URL是可选的
        return value.startsWith('http://') || value.startsWith('https://')
      },
      message: '头像URL必须以 http:// 或 https:// 开头',
      trigger: 'blur'
    }
  ],
  phoneNumber: [
    {
      validator: (rule: any, value: string) => {
        if (!value) return true // 手机号是可选的
        return ProfileUtils.validatePhoneNumber(value)
      },
      message: '手机号格式不正确',
      trigger: 'blur'
    }
  ],
  birthday: [
    {
      validator: (rule: any, value: string) => {
        if (!value) return true // 生日是可选的
        return ProfileUtils.validateBirthday(value)
      },
      message: '生日格式不正确或日期无效',
      trigger: 'blur'
    }
  ],
  bio: [
    { max: 200, message: '个人简介不能超过200个字符', trigger: 'blur' }
  ]
}

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

// 处理保存
const handleSave = async () => {
  if (!formRef.value) return
  
  try {
    // 验证表单
    await formRef.value.validate()
    
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
  
  // 清除验证状态
  nextTick(() => {
    formRef.value?.restoreValidation()
  })
  
  emit('update:visible', false)
}
</script>

<style scoped>
.profile-editor {
  padding: 20px 0;
}

.avatar-section {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  width: 100%;
}

.profile-avatar {
  flex-shrink: 0;
  border: 2px solid #f0f0f0;
  transition: border-color 0.3s ease;
}

.profile-avatar:hover {
  border-color: #18a058;
}

.avatar-fallback {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-family: monospace;
  width: 100%;
  height: 100%;
}

.avatar-eyes {
  font-size: 16px;
  margin-bottom: 2px;
  letter-spacing: 2px;
}

.avatar-mouth {
  font-size: 14px;
}

.avatar-actions {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 表单样式优化 */
:deep(.n-form-item-label) {
  font-weight: 500;
}

:deep(.n-input) {
  transition: all 0.3s ease;
}

:deep(.n-input:hover) {
  border-color: #18a058;
}

:deep(.n-radio-button) {
  margin-right: 8px;
}

:deep(.n-date-picker) {
  width: 100%;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .profile-editor-modal {
    width: 90vw !important;
    max-width: 500px !important;
  }
  
  .avatar-section {
    flex-direction: column;
    align-items: center;
  }
  
  .profile-avatar {
    margin-bottom: 12px;
  }
}
</style>
