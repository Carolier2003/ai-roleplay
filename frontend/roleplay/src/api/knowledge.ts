import axios from './axios'

/**
 * 知识库管理API
 * @author jianjl
 * @version 1.0
 * @description 提供知识库导入、查询、统计等API
 * @date 2025-01-15
 */

export interface KnowledgeImportResult {
  success: boolean
  message: string
  imported_count?: number
  character_id?: number
  filename?: string
}

export interface KnowledgeStats {
  total_count: number
  knowledge_type_count: Record<string, number>
  source_count: Record<string, number>
}

/**
 * 上传文件导入知识库
 */
export async function importKnowledgeFromFile(
  file: File,
  characterId: number
): Promise<KnowledgeImportResult> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('characterId', characterId.toString())

  const response = await axios.post('/api/knowledge/import/file', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 120000 // 2分钟超时，因为导入可能需要较长时间
  })

  return response.data
}

/**
 * 获取角色知识统计
 */
export async function getKnowledgeStats(characterId: number): Promise<KnowledgeStats> {
  const response = await axios.get(`/api/knowledge/stats/${characterId}`)
  return response.data.stats
}

/**
 * 搜索相关知识
 */
export async function searchKnowledge(
  characterId: number,
  query: string,
  topK: number = 5
) {
  const response = await axios.post('/api/knowledge/search', {
    characterId,
    query,
    topK
  })
  return response.data
}

/**
 * 重新同步向量数据库
 */
export async function resyncVectorStore(characterId: number) {
  const response = await axios.post(`/api/knowledge/resync/${characterId}`)
  return response.data
}
