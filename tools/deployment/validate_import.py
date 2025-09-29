#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
导入验证脚本 - 验证知识库导入的正确性
基于RAG向量检索问题调试报告的验证要求
"""

import requests
import json
import time
from typing import Dict, List

class ImportValidator:
    def __init__(self, base_url: str = "http://localhost:18080"):
        self.base_url = base_url
        self.characters = {
            1: "哈利·波特",
            2: "苏格拉底", 
            3: "爱因斯坦",
            4: "江户川柯南",
            5: "泰拉瑞亚向导"
        }
    
    def test_knowledge_search(self) -> bool:
        """测试知识搜索API"""
        print("🔍 测试知识搜索API...")
        
        test_cases = [
            (1, "哈利波特", "应该找到哈利·波特相关知识"),
            (2, "苏格拉底哲学", "应该找到苏格拉底哲学相关知识"),
            (3, "相对论", "应该找到爱因斯坦相对论相关知识"),
            (4, "柯南推理", "应该找到柯南推理相关知识"),
            (5, "武器制作", "应该找到泰拉瑞亚武器相关知识"),
        ]
        
        all_passed = True
        
        for character_id, query, description in test_cases:
            try:
                response = requests.post(
                    f"{self.base_url}/api/knowledge/search",
                    json={
                        "characterId": character_id,
                        "query": query,
                        "topK": 3
                    },
                    timeout=10
                )
                
                if response.status_code == 200:
                    result = response.json()
                    count = result.get("count", 0)
                    character_name = self.characters[character_id]
                    
                    if count > 0:
                        print(f"  ✅ {character_name}: 查询'{query}' -> {count}条结果")
                    else:
                        print(f"  ⚠️ {character_name}: 查询'{query}' -> 无结果")
                        all_passed = False
                else:
                    print(f"  ❌ {self.characters[character_id]}: API错误 (HTTP {response.status_code})")
                    all_passed = False
                    
            except Exception as e:
                print(f"  ❌ {self.characters[character_id]}: 请求异常 - {e}")
                all_passed = False
        
        return all_passed
    
    def test_character_isolation(self) -> bool:
        """测试角色隔离（关键测试，基于RAG报告）"""
        print("\n🔒 测试角色隔离...")
        
        # 核心隔离测试：哈利·波特不应该搜索到苏格拉底的知识
        isolation_tests = [
            (1, "苏格拉底", "哈利·波特不应搜索到苏格拉底知识"),
            (1, "相对论", "哈利·波特不应搜索到爱因斯坦知识"),
            (2, "霍格沃茨", "苏格拉底不应搜索到哈利·波特知识"),
            (3, "魔法", "爱因斯坦不应搜索到哈利·波特知识"),
        ]
        
        all_passed = True
        
        for character_id, query, description in isolation_tests:
            try:
                response = requests.post(
                    f"{self.base_url}/api/knowledge/search",
                    json={
                        "characterId": character_id,
                        "query": query,
                        "topK": 5
                    },
                    timeout=10
                )
                
                if response.status_code == 200:
                    result = response.json()
                    count = result.get("count", 0)
                    character_name = self.characters[character_id]
                    
                    if count == 0:
                        print(f"  ✅ {character_name}: 查询'{query}' -> 正确隔离（0结果）")
                    else:
                        print(f"  ❌ {character_name}: 查询'{query}' -> 隔离失败（{count}结果）")
                        # 显示错误的结果
                        knowledge_list = result.get("knowledge_list", [])
                        for item in knowledge_list[:2]:
                            title = item.get("title", "未知")
                            print(f"     错误结果: {title}")
                        all_passed = False
                else:
                    print(f"  ❌ {character_name}: API错误 (HTTP {response.status_code})")
                    all_passed = False
                    
            except Exception as e:
                print(f"  ❌ {self.characters[character_id]}: 请求异常 - {e}")
                all_passed = False
        
        return all_passed
    
    def test_rag_dialogue(self) -> bool:
        """测试RAG增强对话"""
        print("\n💬 测试RAG增强对话...")
        
        dialogue_tests = [
            (2, "请介绍一下你的哲学思想", "苏格拉底对话测试"),
            (1, "告诉我关于霍格沃茨的事情", "哈利·波特对话测试"),
        ]
        
        all_passed = True
        
        for character_id, message, description in dialogue_tests:
            try:
                response = requests.post(
                    f"{self.base_url}/api/chat/message",
                    json={
                        "characterId": character_id,
                        "message": message,
                        "userId": "validator"
                    },
                    timeout=30
                )
                
                if response.status_code == 200:
                    result = response.json()
                    content = result.get("content", "")
                    character_name = self.characters[character_id]
                    
                    if content and len(content) > 50:
                        print(f"  ✅ {character_name}: 对话正常 ({len(content)}字符)")
                        # 显示部分回复内容
                        preview = content[:100] + "..." if len(content) > 100 else content
                        print(f"     预览: {preview}")
                    else:
                        print(f"  ⚠️ {character_name}: 回复过短或为空")
                        all_passed = False
                else:
                    print(f"  ❌ {character_name}: 对话API错误 (HTTP {response.status_code})")
                    all_passed = False
                    
            except Exception as e:
                print(f"  ❌ {self.characters[character_id]}: 对话异常 - {e}")
                all_passed = False
        
        return all_passed
    
    def run_validation(self) -> bool:
        """运行完整验证"""
        print("🧪 AI角色扮演系统 - 导入验证")
        print("=" * 40)
        
        tests = [
            ("知识搜索API", self.test_knowledge_search),
            ("角色隔离", self.test_character_isolation),
            ("RAG增强对话", self.test_rag_dialogue),
        ]
        
        results = []
        for name, test_func in tests:
            print(f"\n📋 {name}测试...")
            result = test_func()
            results.append((name, result))
            
            if result:
                print(f"✅ {name}测试通过")
            else:
                print(f"❌ {name}测试失败")
        
        print("\n" + "=" * 40)
        print("📊 验证结果汇总:")
        
        all_passed = True
        for name, passed in results:
            status = "✅ 通过" if passed else "❌ 失败"
            print(f"   {name}: {status}")
            if not passed:
                all_passed = False
        
        print("\n" + "=" * 40)
        if all_passed:
            print("🎉 所有验证测试通过！知识库导入成功！")
        else:
            print("❌ 部分验证失败，请检查导入过程或后端服务")
        
        return all_passed

if __name__ == "__main__":
    validator = ImportValidator()
    success = validator.run_validation()
    
    import sys
    sys.exit(0 if success else 1)
