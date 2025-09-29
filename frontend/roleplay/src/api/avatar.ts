import axios from './axios'

/**
 * 裁剪参数接口
 */
export interface CropData {
  x: number
  y: number
  width: number
  height: number
  scale?: number
  rotate?: number
}

/**
 * 头像上传响应接口
 */
export interface AvatarUploadResponse {
  avatarUrl: string
  thumbnailUrl: string
  originalUrl: string
  fileSize: number
  dimensions: string
}

/**
 * 头像API类
 */
export class AvatarAPI {
  
  /**
   * 上传头像
   */
  static async uploadAvatar(file: File, cropData?: CropData): Promise<AvatarUploadResponse> {
    console.log('[AvatarAPI] 上传头像, fileName:', file.name, 'size:', file.size, 'cropData:', cropData)
    
    const formData = new FormData()
    formData.append('file', file)
    
    if (cropData) {
      formData.append('cropData', JSON.stringify(cropData))
    }
    
    const response = await axios.post('/api/avatar/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: 60000 // 60秒超时
    })
    
    console.log('[AvatarAPI] 头像上传成功:', response.data)
    return response.data
  }
  
  /**
   * 删除头像
   */
  static async deleteAvatar(): Promise<boolean> {
    console.log('[AvatarAPI] 删除头像')
    
    const response = await axios.delete('/api/avatar')
    
    console.log('[AvatarAPI] 头像删除结果:', response.data)
    return response.data
  }
  
  /**
   * 获取当前用户头像URL
   */
  static async getAvatarUrl(): Promise<string | null> {
    console.log('[AvatarAPI] 获取头像URL')
    
    try {
      const response = await axios.get('/api/avatar')
      console.log('[AvatarAPI] 获取头像URL成功:', response.data)
      return response.data || null
    } catch (error) {
      console.log('[AvatarAPI] 获取头像URL失败:', error)
      return null
    }
  }
}

/**
 * 头像工具类
 */
export class AvatarUtils {
  
  /**
   * 验证图片文件
   */
  static validateImageFile(file: File): { valid: boolean; message?: string } {
    // 检查文件类型
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp']
    if (!allowedTypes.includes(file.type)) {
      return {
        valid: false,
        message: '不支持的文件格式，请上传 JPG、PNG 或 WebP 格式的图片'
      }
    }
    
    // 检查文件大小 (10MB)
    const maxSize = 10 * 1024 * 1024
    if (file.size > maxSize) {
      return {
        valid: false,
        message: '图片文件大小不能超过 10MB'
      }
    }
    
    return { valid: true }
  }
  
  /**
   * 读取图片文件为DataURL
   */
  static readFileAsDataURL(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = (e) => {
        resolve(e.target?.result as string)
      }
      reader.onerror = reject
      reader.readAsDataURL(file)
    })
  }
  
  /**
   * 获取图片尺寸
   */
  static getImageDimensions(file: File): Promise<{ width: number; height: number }> {
    return new Promise((resolve, reject) => {
      const img = new Image()
      img.onload = () => {
        resolve({
          width: img.naturalWidth,
          height: img.naturalHeight
        })
      }
      img.onerror = reject
      img.src = URL.createObjectURL(file)
    })
  }
  
  /**
   * 格式化文件大小
   */
  static formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B'
    
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }
}
