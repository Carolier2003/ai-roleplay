import request from './axios'

export interface User {
    userId: number
    userAccount: string
    displayName: string
    email: string
    avatarUrl: string
    role: string
    status: number
    createdAt: string
    lastLoginAt: string
}

export interface PageResult<T> {
    records: T[]
    total: number
    size: number
    current: number
    pages: number
}

export interface ApiResponse<T> {
    code: number
    message: string
    data: T
    success?: boolean // 兼容部分接口返回 success 字段
}

export interface CharacterKnowledgeStat {
    characterName: string
    count: number
}

export interface AdminStatsResponse {
    userCount: number
    characterCount: number
    knowledgeCount: number
    knowledgeDistribution: CharacterKnowledgeStat[]
}

export const adminApi = {
    // 获取统计数据
    getStats() {
        return request.get<ApiResponse<AdminStatsResponse>>('/api/admin/stats')
    },

    // 获取用户列表
    getUsers(params: { page: number; size: number; keyword?: string }) {
        return request.get<ApiResponse<PageResult<User>>>('/api/admin/users', { params })
    },

    // 更新用户角色
    updateUserRole(userId: number, role: string) {
        return request.put<ApiResponse<string>>(`/api/admin/users/${userId}/role`, { role })
    },

    // 更新用户状态
    updateUserStatus(userId: number, status: number) {
        return request.put<ApiResponse<string>>(`/api/admin/users/${userId}/status`, { status })
    },

    // 删除角色
    deleteCharacter(characterId: number) {
        return request.delete<void>(`/api/characters/${characterId}`)
    },

    // 获取知识列表
    getKnowledgeList(params: { characterId?: number; page: number; size: number; keyword?: string }) {
        return request.get<ApiResponse<any>>('/api/knowledge/list', { params })
    },

    // 添加知识
    addKnowledge(data: { characterId: number; knowledge: any }) {
        return request.post<ApiResponse<any>>('/api/knowledge/import/single', data)
    },

    // 更新知识
    updateKnowledge(id: number, data: any) {
        return request.put<ApiResponse<any>>(`/api/knowledge/${id}`, data)
    },

    // 删除知识
    deleteKnowledge(id: number) {
        return request.delete<ApiResponse<any>>(`/api/knowledge/${id}`)
    }
}
