#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
调试知识库数据的脚本
检查数据库中的实际知识条目
"""

import requests
import json

def test_backend_health():
    """测试后端健康状态"""
    try:
        response = requests.get("http://localhost:18080/api/health")
        print(f"✅ 后端服务状态: {response.status_code}")
        print(f"📊 响应: {response.json()}")
        return True
    except Exception as e:
        print(f"❌ 后端服务不可用: {e}")
        return False

def check_characters():
    """检查角色列表"""
    try:
        response = requests.get("http://localhost:18080/api/characters")
        characters = response.json()
        print(f"\n🧙‍♂️ 系统中的角色:")
        for char in characters:
            print(f"   ID: {char['id']}, 名称: {char['name']}")
        return characters
    except Exception as e:
        print(f"❌ 获取角色失败: {e}")
        return []

def test_chat_with_character(character_id, message):
    """测试角色对话"""
    try:
        payload = {
            "message": message,
            "userId": "debug_user",
            "characterId": character_id
        }
        response = requests.post(
            "http://localhost:18080/api/chat/message",
            json=payload,
            headers={'Content-Type': 'application/json'}
        )
        
        if response.status_code == 200:
            result = response.json()
            print(f"✅ 角色 {character_id} 对话成功")
            print(f"📝 回复长度: {len(result['content'])} 字符")
            print(f"🗣️ 回复片段: {result['content'][:100]}...")
            return True
        else:
            print(f"❌ 角色 {character_id} 对话失败: {response.status_code}")
            return False
    except Exception as e:
        print(f"❌ 角色 {character_id} 对话异常: {e}")
        return False

def test_knowledge_search(character_id, query):
    """测试知识搜索"""
    try:
        payload = {
            "characterId": character_id,
            "query": query,
            "topK": 5
        }
        response = requests.post(
            "http://localhost:18080/api/knowledge/search",
            json=payload,
            headers={'Content-Type': 'application/json'}
        )
        
        if response.status_code == 200:
            result = response.json()
            print(f"🔍 角色 {character_id} 知识搜索: {result['count']} 个结果")
            return result
        else:
            print(f"❌ 角色 {character_id} 知识搜索失败: {response.status_code}")
            return None
    except Exception as e:
        print(f"❌ 角色 {character_id} 知识搜索异常: {e}")
        return None

def test_knowledge_stats(character_id):
    """测试知识统计"""
    try:
        response = requests.get(f"http://localhost:18080/api/knowledge/stats/{character_id}")
        
        if response.status_code == 200:
            result = response.json()
            stats = result.get('stats', {})
            print(f"📊 角色 {character_id} 知识统计:")
            print(f"   总数: {stats.get('total_count', 0)}")
            print(f"   向量化: {stats.get('vectorized_count', 0)}")
            print(f"   向量化率: {stats.get('vectorization_rate', 0):.1%}")
            return result
        else:
            print(f"❌ 角色 {character_id} 知识统计失败: {response.status_code}")
            return None
    except Exception as e:
        print(f"❌ 角色 {character_id} 知识统计异常: {e}")
        return None

def main():
    print("🔍 RAG知识库调试工具")
    print("=" * 50)
    
    # 1. 检查后端健康状态
    if not test_backend_health():
        return
    
    # 2. 检查角色列表
    characters = check_characters()
    
    # 3. 测试每个角色
    for char in characters:
        char_id = char['id']
        char_name = char['name']
        
        print(f"\n🧪 测试角色: {char_name} (ID: {char_id})")
        print("-" * 30)
        
        # 测试知识统计
        test_knowledge_stats(char_id)
        
        # 测试知识搜索
        if char_id == 1:  # 哈利·波特
            test_knowledge_search(char_id, "霍格沃茨")
        elif char_id == 2:  # 苏格拉底
            test_knowledge_search(char_id, "哲学")
        elif char_id == 3:  # 爱因斯坦
            test_knowledge_search(char_id, "物理")
        
        # 测试对话
        if char_id == 1:  # 哈利·波特
            test_chat_with_character(char_id, "请简单介绍一下霍格沃茨")
        elif char_id == 2:  # 苏格拉底
            test_chat_with_character(char_id, "请谈谈你的哲学思想")
        elif char_id == 3:  # 爱因斯坦
            test_chat_with_character(char_id, "请解释一下相对论")

if __name__ == "__main__":
    main()
