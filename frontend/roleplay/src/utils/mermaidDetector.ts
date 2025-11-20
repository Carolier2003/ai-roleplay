/**
 * Mermaid 语法检测和转换工具
 * 用于识别 AI 输出中的 Mermaid 图表语法并进行转换
 */

// Mermaid 关键词列表
const MERMAID_KEYWORDS = [
  'graph TD',
  'graph LR',
  'graph TB',
  'graph BT',
  'graph RL',
  'flowchart',
  'sequenceDiagram',
  'classDiagram',
  'stateDiagram',
  'erDiagram',
  'journey',
  'gantt',
  'pie',
  'mindmap'
]

// Mermaid 语法特征（用于辅助判断）
const MERMAID_PATTERNS = [
  /-->/g,        // 箭头
  /->/g,         // 简单箭头
  /\[.*?\]/g,    // 方括号节点
  /\{.*?\}/g,    // 花括号节点
  /\(.*?\)/g,    // 圆括号节点
  /\|\|/g,       // 条件分支
]

/**
 * 检测文本块是否包含 Mermaid 语法
 * @param content 要检测的文本内容
 * @returns 是否为 Mermaid 块
 */
export function detectMermaidBlock(content: string): boolean {
  if (!content || content.trim().length === 0) {
    return false
  }

  const lines = content.split('\n')
  
  // 1. 检查是否包含明确的 Mermaid 关键词
  const hasKeyword = MERMAID_KEYWORDS.some(keyword => 
    content.includes(keyword)
  )
  
  if (hasKeyword) {
    return true
  }

  // 2. 检查是否有连续多行包含 Mermaid 语法特征
  let patternMatchCount = 0
  
  for (const line of lines) {
    const trimmedLine = line.trim()
    
    // 跳过空行和纯文本行
    if (!trimmedLine || trimmedLine.length < 3) {
      continue
    }
    
    // 检查是否包含箭头语法
    if (trimmedLine.includes('->') || trimmedLine.includes('-->')) {
      patternMatchCount++
    }
    
    // 检查是否包含节点定义
    if (/[A-Z]\d*\[.+?\]/.test(trimmedLine) || /[A-Z]\d*\{.+?\}/.test(trimmedLine)) {
      patternMatchCount++
    }
  }

  // 如果有3行以上包含 Mermaid 特征，认为是 Mermaid 块
  return patternMatchCount >= 3
}

/**
 * 将普通文本转换为标准 Mermaid 语法
 * @param content 原始文本内容
 * @returns 转换后的 Mermaid 语法
 */
export function convertToMermaid(content: string): string {
  if (!content) {
    return content
  }

  let lines = content.split('\n')
  let result: string[] = []
  let inMermaidBlock = false
  let mermaidLines: string[] = []
  
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    const trimmedLine = line.trim()
    
    // 检测 Mermaid 块的开始
    if (!inMermaidBlock && isMermaidLineStart(trimmedLine)) {
      inMermaidBlock = true
      mermaidLines = []
      
      // 如果第一行不是 graph 声明，添加默认声明
      if (!MERMAID_KEYWORDS.some(kw => trimmedLine.includes(kw))) {
        mermaidLines.push('graph TD')
      }
    }
    
    // 在 Mermaid 块内
    if (inMermaidBlock) {
      // 检测块的结束（空行或明显的非 Mermaid 语法）
      if (trimmedLine === '' || (!isMermaidLine(trimmedLine) && mermaidLines.length > 2)) {
        // 输出 Mermaid 块
        if (mermaidLines.length > 0) {
          result.push('<pre class="mermaid">')
          result.push(mermaidLines.join('\n'))
          result.push('</pre>')
          mermaidLines = []
        }
        inMermaidBlock = false
        
        // 保留当前行（如果不是空行）
        if (trimmedLine !== '') {
          result.push(line)
        }
      } else {
        // 处理并添加到 Mermaid 块
        mermaidLines.push(processMermaidLine(trimmedLine))
      }
    } else {
      // 普通文本行
      result.push(line)
    }
  }
  
  // 处理结尾的 Mermaid 块
  if (mermaidLines.length > 0) {
    result.push('<pre class="mermaid">')
    result.push(mermaidLines.join('\n'))
    result.push('</pre>')
  }
  
  return result.join('\n')
}

/**
 * 判断是否为 Mermaid 行的开始
 */
function isMermaidLineStart(line: string): boolean {
  // 包含关键词
  if (MERMAID_KEYWORDS.some(kw => line.includes(kw))) {
    return true
  }
  
  // 包含节点定义或箭头
  if (line.includes('->') || line.includes('-->')) {
    return true
  }
  
  // 包含节点语法
  if (/[A-Z]\d*[\[\{\(]/.test(line)) {
    return true
  }
  
  return false
}

/**
 * 判断是否为 Mermaid 语法行
 */
function isMermaidLine(line: string): boolean {
  if (!line || line.length === 0) {
    return false
  }
  
  // 包含箭头
  if (line.includes('->') || line.includes('-->') || line.includes('---')) {
    return true
  }
  
  // 包含节点定义
  if (/[A-Z]\d*[\[\{\(]/.test(line)) {
    return true
  }
  
  // Mermaid 注释
  if (line.startsWith('%%')) {
    return true
  }
  
  // Mermaid 子图
  if (line.includes('subgraph') || line.includes('end')) {
    return true
  }
  
  return false
}

/**
 * 处理单行 Mermaid 语法
 */
function processMermaidLine(line: string): string {
  let processed = line
  
  // 清理多余的符号
  processed = processed.replace(/^[-•]\s+/, '    ')  // 列表符号转缩进
  
  // 确保中文和 emoji 在节点内
  // 如果包含中文但没有用 [] 包裹，自动添加
  if (/[\u4e00-\u9fa5]/.test(processed) && !processed.includes('[') && !processed.includes('(')) {
    // 简单处理：如果是纯文本描述，转为注释
    if (!processed.includes('->') && !processed.includes('-->')) {
      processed = '%% ' + processed
    }
  }
  
  return processed
}

/**
 * 提取文本中的所有 Mermaid 块
 * @param content 文本内容
 * @returns Mermaid 块数组
 */
export function extractMermaidBlocks(content: string): Array<{ start: number; end: number; content: string }> {
  const blocks: Array<{ start: number; end: number; content: string }> = []
  const lines = content.split('\n')
  
  let inBlock = false
  let startLine = -1
  let blockLines: string[] = []
  
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim()
    
    if (!inBlock && isMermaidLineStart(line)) {
      inBlock = true
      startLine = i
      blockLines = [line]
    } else if (inBlock) {
      if (line === '' || (!isMermaidLine(line) && blockLines.length > 2)) {
        // 块结束
        blocks.push({
          start: startLine,
          end: i - 1,
          content: blockLines.join('\n')
        })
        inBlock = false
        blockLines = []
      } else {
        blockLines.push(line)
      }
    }
  }
  
  // 处理结尾的块
  if (inBlock && blockLines.length > 0) {
    blocks.push({
      start: startLine,
      end: lines.length - 1,
      content: blockLines.join('\n')
    })
  }
  
  return blocks
}

