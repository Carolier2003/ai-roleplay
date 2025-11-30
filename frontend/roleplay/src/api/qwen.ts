import request from './axios'

export interface QwenConversationInfo {
    conversationId: string
    title: string
    lastMessage: string
    lastActiveTime: number
    createdAt: number
    messageCount: number
}

export interface QwenConversationResponse {
    conversationId: string
    createdAt: number
    title?: string
}

export const qwenAPI = {
    // 创建新会话
    createConversation() {
        return request.post<QwenConversationResponse>('/api/chat/qwen/conversations')
    },

    // 列出所有会话
    listConversations() {
        return request.get<QwenConversationInfo[]>('/api/chat/qwen/conversations')
    },

    // 获取会话详情
    getConversation(conversationId: string) {
        return request.get<QwenConversationInfo>(`/api/chat/qwen/conversations/${conversationId}`)
    },

    // 删除会话
    deleteConversation(conversationId: string) {
        return request.delete(`/api/chat/qwen/conversations/${conversationId}`)
    },

    // 重命名会话
    renameConversation(conversationId: string, title: string) {
        return request.patch(`/api/chat/qwen/conversations/${conversationId}`, { title })
    }
}
