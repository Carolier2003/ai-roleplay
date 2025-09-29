import { computed } from 'vue'
import { darkTheme, lightTheme } from 'naive-ui'
import type { GlobalTheme } from 'naive-ui'
import themeConfig from '@/assets/theme.json'

export function useTheme() {
  // 腾讯元宝主题配置 - 只覆盖common部分避免类型问题
  const yuanbaoTheme = computed(() => ({
    ...lightTheme,
    common: {
      ...lightTheme.common,
      primaryColor: themeConfig.colors.primary[500],
      primaryColorHover: themeConfig.colors.primary[400],
      primaryColorPressed: themeConfig.colors.primary[600],
      primaryColorSuppl: themeConfig.colors.primary[500],
      borderRadius: themeConfig.borderRadius.md,
      fontSize: themeConfig.fontSize.base,
      lineHeight: '1.6',
      boxShadow1: themeConfig.shadows.sm,
      boxShadow2: themeConfig.shadows.base,
      boxShadow3: themeConfig.shadows.lg,
    }
  }))

  // 注入CSS变量到根元素
  const injectCSSVariables = () => {
    const root = document.documentElement
    
    // 注入颜色变量
    Object.entries(themeConfig.colors.primary).forEach(([key, value]) => {
      root.style.setProperty(`--primary-${key}`, value)
    })
    
    Object.entries(themeConfig.colors.gray).forEach(([key, value]) => {
      root.style.setProperty(`--gray-${key}`, value)
    })
    
    // 注入其他设计token
    Object.entries(themeConfig.spacing).forEach(([key, value]) => {
      root.style.setProperty(`--spacing-${key}`, value)
    })
    
    Object.entries(themeConfig.borderRadius).forEach(([key, value]) => {
      root.style.setProperty(`--radius-${key}`, value)
    })
    
    Object.entries(themeConfig.fontSize).forEach(([key, value]) => {
      root.style.setProperty(`--font-${key}`, value)
    })
    
    Object.entries(themeConfig.shadows).forEach(([key, value]) => {
      root.style.setProperty(`--shadow-${key}`, value)
    })
  }

  return {
    yuanbaoTheme,
    injectCSSVariables,
    themeConfig
  }
}
