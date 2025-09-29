<template>
  <div class="max-w-none break-words text-base leading-7 text-gray-800 dark:text-gray-100" style="font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">
    <div v-html="renderedContent"></div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'

interface Props {
  content: string
}

const props = defineProps<Props>()

// 配置 marked 选项
marked.setOptions({
  breaks: true,
  gfm: true,
  headerIds: false,
  mangle: false
})

// 自定义渲染器
const renderer = new marked.Renderer()

// 段落渲染 - 段落间空行
renderer.paragraph = (text) => {
  return `<p class="mb-4 last:mb-0">${text}</p>`
}

// 强调文本渲染 - **bold**
renderer.strong = (text) => {
  return `<strong class="font-semibold text-gray-800 dark:text-gray-100">${text}</strong>`
}

// 斜体文本渲染 - *italic*
renderer.em = (text) => {
  return `<em class="italic text-gray-700 dark:text-gray-300">${text}</em>`
}

// 行内代码渲染 - `inline code`
renderer.codespan = (code) => {
  return `<code class="px-1 py-0.5 rounded bg-gray-100 dark:bg-gray-800 text-sm font-mono text-indigo-600 dark:text-indigo-400">${code}</code>`
}

// 代码块渲染（带复制按钮）- ```code```
renderer.code = (code, language) => {
  const escapedCode = code
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
  
  const codeId = `code-${Math.random().toString(36).substr(2, 9)}`
  
  return `
    <div class="relative group my-4">
      <pre class="language-${language || 'text'} block p-4 rounded-lg bg-gray-900 text-gray-100 overflow-x-auto"><code id="${codeId}">${escapedCode}</code></pre>
      <button 
        onclick="copyCode('${codeId}')" 
        class="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200 bg-gray-700 hover:bg-gray-600 text-gray-200 px-2 py-1 rounded text-xs font-medium"
      >
        Copy
      </button>
    </div>
  `
}

// 列表渲染 - - item 和 1. item
renderer.list = (body, ordered) => {
  const tag = ordered ? 'ol' : 'ul'
  const listClass = ordered 
    ? 'list-decimal pl-6 space-y-2 my-4' 
    : 'list-disc pl-6 space-y-2 my-4'
  
  return `<${tag} class="${listClass}">${body}</${tag}>`
}

// 列表项渲染
renderer.listitem = (text) => {
  return `<li class="leading-7">${text}</li>`
}

// 引用块渲染 - > quote
renderer.blockquote = (quote) => {
  return `<blockquote class="border-l-4 border-gray-300 dark:border-gray-600 pl-4 italic text-gray-600 dark:text-gray-400 my-4">${quote}</blockquote>`
}

// 链接渲染 - [text](url)
renderer.link = (href, title, text) => {
  const titleAttr = title ? ` title="${title}"` : ''
  return `<a href="${href}"${titleAttr} class="text-indigo-600 dark:text-indigo-400 hover:underline" target="_blank" rel="noopener">${text}</a>`
}

// 水平分割线渲染 - ---
renderer.hr = () => {
  return `<hr class="my-6 border-gray-200 dark:border-gray-700">`
}

// 标题渲染
renderer.heading = (text, level) => {
  const headingClasses = {
    1: 'text-2xl font-bold text-gray-900 dark:text-gray-100 mt-6 mb-4 first:mt-0',
    2: 'text-xl font-semibold text-gray-900 dark:text-gray-100 mt-5 mb-3 first:mt-0',
    3: 'text-lg font-semibold text-gray-900 dark:text-gray-100 mt-4 mb-2 first:mt-0',
    4: 'text-base font-semibold text-gray-900 dark:text-gray-100 mt-3 mb-2 first:mt-0',
    5: 'text-sm font-semibold text-gray-900 dark:text-gray-100 mt-3 mb-2 first:mt-0',
    6: 'text-sm font-medium text-gray-900 dark:text-gray-100 mt-3 mb-2 first:mt-0'
  }
  
  const className = headingClasses[level as keyof typeof headingClasses] || headingClasses[6]
  return `<h${level} class="${className}">${text}</h${level}>`
}

// 表格渲染
renderer.table = (header, body) => {
  return `
    <div class="overflow-x-auto my-4">
      <table class="min-w-full border border-gray-200 dark:border-gray-700">
        <thead class="bg-gray-50 dark:bg-gray-800">
          ${header}
        </thead>
        <tbody class="bg-white dark:bg-gray-900 divide-y divide-gray-200 dark:divide-gray-700">
          ${body}
        </tbody>
      </table>
    </div>
  `
}

renderer.tablerow = (content) => {
  return `<tr>${content}</tr>`
}

renderer.tablecell = (content, flags) => {
  const tag = flags.header ? 'th' : 'td'
  const className = flags.header 
    ? 'px-4 py-2 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider'
    : 'px-4 py-2 text-sm text-gray-900 dark:text-gray-100'
  
  return `<${tag} class="${className}">${content}</${tag}>`
}

// 设置自定义渲染器
marked.setOptions({ renderer })

// Markdown预处理函数
const preprocessMarkdown = (content: string): string => {
  return content
    // 将中文破折号转换为标准连字符（用于列表）
    .replace(/^([\s]*)—(\s+)/gm, '$1- $2')
    // 将行中的中文破折号也转换（如果前面有空格的话）
    .replace(/(\n[\s]*)—(\s+)/g, '$1- $2')
    // 处理其他可能的列表符号
    .replace(/^([\s]*)•(\s+)/gm, '$1- $2')
    .replace(/(\n[\s]*)•(\s+)/g, '$1- $2')
}

const renderedContent = computed(() => {
  try {
    // 预处理Markdown内容
    const processedContent = preprocessMarkdown(props.content)
    let html = String(marked.parse(processedContent))
    
    // 基本的XSS防护
    html = html
      .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
      .replace(/<iframe\b[^<]*(?:(?!<\/iframe>)<[^<]*)*<\/iframe>/gi, '')
      .replace(/javascript:/gi, '')
      .replace(/on\w+\s*=/gi, '')
    
    return html
  } catch (error) {
    console.error('[AiMessage] Markdown解析失败:', error)
    return props.content
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;')
      .replace(/\n/g, '<br>')
  }
})

// 全局复制函数（需要在window上注册）
if (typeof window !== 'undefined') {
  (window as any).copyCode = (codeId: string) => {
    const codeElement = document.getElementById(codeId)
    if (codeElement) {
      const text = codeElement.textContent || ''
      navigator.clipboard.writeText(text).then(() => {
        // 临时显示复制成功提示
        const button = codeElement.parentElement?.nextElementSibling as HTMLButtonElement
        if (button) {
          const originalText = button.textContent
          button.textContent = 'Copied!'
          button.classList.add('bg-green-600', 'hover:bg-green-500')
          button.classList.remove('bg-gray-700', 'hover:bg-gray-600')
          
          setTimeout(() => {
            button.textContent = originalText
            button.classList.remove('bg-green-600', 'hover:bg-green-500')
            button.classList.add('bg-gray-700', 'hover:bg-gray-600')
          }, 2000)
        }
      }).catch(err => {
        console.error('复制失败:', err)
      })
    }
  }
}
</script>
