import { computed } from 'vue'
import themeConfig from '@/assets/theme.json'

export function useTheme() {
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
    injectCSSVariables,
    themeConfig
  }
}
