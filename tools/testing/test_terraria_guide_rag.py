#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
泰拉瑞亚向导RAG功能综合验证测试
验证799个武器数据导入后的功能效果
"""

import requests
import json
import time
import sys

class TerrariaGuideRAGTest:
    def __init__(self):
        self.base_url = "http://localhost:18080"
        self.character_id = 5  # 泰拉瑞亚向导
        
        # 测试用例
        self.test_cases = [
            {
                "name": "天顶剑制作配方测试",
                "query": "天顶剑是怎么制作的？需要哪些材料？",
                "expected_keywords": ["10", "剑", "制作", "材料", "工作台"],
                "description": "测试之前失败的天顶剑相关知识"
            },
            {
                "name": "魔法武器推荐测试", 
                "query": "推荐一些强力的魔法武器",
                "expected_keywords": ["魔法", "法杖", "魔棒", "伤害", "魔力"],
                "description": "测试魔法武器推荐功能"
            },
            {
                "name": "近战武器推荐测试",
                "query": "新手应该用什么近战武器？",
                "expected_keywords": ["剑", "近战", "伤害", "推荐", "新手"],
                "description": "测试近战武器推荐功能"
            },
            {
                "name": "远程武器测试",
                "query": "有什么好的弓箭类武器？",
                "expected_keywords": ["弓", "箭", "远程", "伤害", "射程"],
                "description": "测试远程武器相关知识"
            },
            {
                "name": "召唤武器测试",
                "query": "召唤师职业应该用什么武器？",
                "expected_keywords": ["召唤", "法杖", "召唤物", "宠物", "随从"],
                "description": "测试召唤武器相关知识"
            },
            {
                "name": "Boss武器掉落测试",
                "query": "哪些Boss会掉落强力武器？",
                "expected_keywords": ["Boss", "掉落", "武器", "获得", "强力"],
                "description": "测试Boss掉落武器相关知识"
            }
        ]
        
        # 角色隔离测试
        self.isolation_tests = [
            {"character_id": 1, "name": "哈利·波特"},
            {"character_id": 2, "name": "苏格拉底"},
            {"character_id": 3, "name": "爱因斯坦"},
            {"character_id": 4, "name": "江户川柯南"}
        ]
    
    def test_health_check(self):
        """检查后端服务状态"""
        print("🏥 检查后端服务状态...")
        try:
            response = requests.get(f"{self.base_url}/api/health", timeout=10)
            if response.status_code == 200:
                print("✅ 后端服务正常")
                return True
            else:
                print(f"❌ 后端服务异常: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ 无法连接后端服务: {e}")
            return False
    
    def test_rag_search_api(self):
        """测试RAG搜索API"""
        print("\n🔍 测试RAG搜索API...")
        
        search_queries = [
            "天顶剑",
            "魔法武器", 
            "近战武器",
            "弓箭"
        ]
        
        success_count = 0
        
        for query in search_queries:
            try:
                payload = {
                    "characterId": self.character_id,
                    "query": query,
                    "topK": 5
                }
                
                response = requests.post(
                    f"{self.base_url}/api/knowledge/search",
                    json=payload,
                    headers={"Content-Type": "application/json"},
                    timeout=15
                )
                
                if response.status_code == 200:
                    data = response.json()
                    if data.get("success") and len(data.get("knowledge", [])) > 0:
                        print(f"✅ '{query}' 搜索成功: 找到 {len(data['knowledge'])} 条知识")
                        success_count += 1
                    else:
                        print(f"⚠️ '{query}' 搜索结果为空")
                else:
                    print(f"❌ '{query}' 搜索失败: {response.status_code}")
                    print(f"   响应: {response.text}")
                    
            except Exception as e:
                print(f"❌ '{query}' 搜索异常: {e}")
        
        print(f"📊 RAG搜索API测试结果: {success_count}/{len(search_queries)} 成功")
        return success_count == len(search_queries)
    
    def test_character_dialogue(self):
        """测试泰拉瑞亚向导的RAG增强对话"""
        print("\n💬 测试泰拉瑞亚向导RAG增强对话...")
        
        success_count = 0
        
        for i, test_case in enumerate(self.test_cases, 1):
            print(f"\n📝 测试用例 {i}: {test_case['name']}")
            print(f"   描述: {test_case['description']}")
            print(f"   问题: {test_case['query']}")
            
            try:
                payload = {
                    "message": test_case["query"],
                    "characterId": self.character_id
                }
                
                response = requests.post(
                    f"{self.base_url}/api/chat/message",
                    json=payload,
                    headers={"Content-Type": "application/json"},
                    timeout=30
                )
                
                if response.status_code == 200:
                    data = response.json()
                    ai_response = data.get("content", "")
                    
                    if ai_response:
                        print(f"   回答长度: {len(ai_response)} 字符")
                        
                        # 检查关键词
                        found_keywords = []
                        for keyword in test_case["expected_keywords"]:
                            if keyword.lower() in ai_response.lower():
                                found_keywords.append(keyword)
                        
                        keyword_match_rate = len(found_keywords) / len(test_case["expected_keywords"])
                        
                        if keyword_match_rate >= 0.3:  # 至少30%的关键词匹配
                            print(f"✅ 测试通过 (关键词匹配: {len(found_keywords)}/{len(test_case['expected_keywords'])})")
                            print(f"   匹配的关键词: {found_keywords}")
                            print(f"   回答摘要: {ai_response[:100]}...")
                            success_count += 1
                        else:
                            print(f"⚠️ 关键词匹配度不足: {len(found_keywords)}/{len(test_case['expected_keywords'])}")
                            print(f"   回答: {ai_response[:200]}...")
                    else:
                        print("❌ 回答为空")
                else:
                    print(f"❌ 请求失败: {response.status_code}")
                    print(f"   错误: {response.text}")
                    
            except Exception as e:
                print(f"❌ 测试异常: {e}")
            
            time.sleep(2)  # 避免请求过快
        
        print(f"\n📊 对话测试结果: {success_count}/{len(self.test_cases)} 成功")
        return success_count >= len(self.test_cases) * 0.7  # 70%通过率
    
    def test_character_isolation(self):
        """测试角色隔离 - 其他角色不应该能访问泰拉瑞亚知识"""
        print("\n🚧 测试角色隔离功能...")
        
        terraria_query = "天顶剑是什么？"
        isolation_success = 0
        
        for character in self.isolation_tests:
            character_id = character["character_id"]
            character_name = character["name"]
            
            print(f"\n👤 测试 {character_name} (ID: {character_id}) 访问泰拉瑞亚知识...")
            
            try:
                # 测试RAG搜索API
                search_payload = {
                    "characterId": character_id,
                    "query": terraria_query,
                    "topK": 5
                }
                
                response = requests.post(
                    f"{self.base_url}/api/knowledge/search",
                    json=search_payload,
                    headers={"Content-Type": "application/json"},
                    timeout=15
                )
                
                if response.status_code == 200:
                    data = response.json()
                    knowledge_count = len(data.get("knowledge", []))
                    
                    if knowledge_count == 0:
                        print(f"✅ {character_name} 无法访问泰拉瑞亚知识 (隔离成功)")
                        isolation_success += 1
                    else:
                        print(f"❌ {character_name} 可以访问泰拉瑞亚知识 (发现 {knowledge_count} 条，隔离失败)")
                else:
                    print(f"⚠️ {character_name} 搜索请求失败: {response.status_code}")
                    
            except Exception as e:
                print(f"❌ {character_name} 测试异常: {e}")
        
        print(f"\n📊 角色隔离测试结果: {isolation_success}/{len(self.isolation_tests)} 成功隔离")
        return isolation_success == len(self.isolation_tests)
    
    def test_knowledge_base_stats(self):
        """测试知识库统计信息"""
        print("\n📈 检查知识库统计信息...")
        
        try:
            # 测试泰拉瑞亚向导的知识数量
            search_payload = {
                "characterId": self.character_id,
                "query": "武器",
                "topK": 50
            }
            
            response = requests.post(
                f"{self.base_url}/api/knowledge/search",
                json=search_payload,
                headers={"Content-Type": "application/json"},
                timeout=15
            )
            
            if response.status_code == 200:
                data = response.json()
                knowledge_count = len(data.get("knowledge", []))
                print(f"✅ 泰拉瑞亚向导知识库: 搜索返回 {knowledge_count} 条相关知识")
                
                if knowledge_count >= 20:  # 期望有足够的知识
                    print("✅ 知识库规模充足")
                    return True
                else:
                    print("⚠️ 知识库规模可能不足")
                    return False
            else:
                print(f"❌ 知识库统计失败: {response.status_code}")
                return False
                
        except Exception as e:
            print(f"❌ 知识库统计异常: {e}")
            return False
    
    def run_comprehensive_test(self):
        """运行综合测试"""
        print("🚀 泰拉瑞亚向导RAG功能综合验证测试")
        print("=" * 50)
        
        test_results = []
        
        # 1. 健康检查
        health_ok = self.test_health_check()
        test_results.append(("后端服务健康检查", health_ok))
        
        if not health_ok:
            print("❌ 后端服务不可用，终止测试")
            return False
        
        # 2. 知识库统计
        stats_ok = self.test_knowledge_base_stats()
        test_results.append(("知识库统计检查", stats_ok))
        
        # 3. RAG搜索API测试
        search_ok = self.test_rag_search_api()
        test_results.append(("RAG搜索API测试", search_ok))
        
        # 4. 对话功能测试
        dialogue_ok = self.test_character_dialogue()
        test_results.append(("RAG增强对话测试", dialogue_ok))
        
        # 5. 角色隔离测试
        isolation_ok = self.test_character_isolation()
        test_results.append(("角色隔离测试", isolation_ok))
        
        # 输出总结报告
        print("\n" + "=" * 50)
        print("🎯 综合测试结果总结")
        print("=" * 50)
        
        passed_tests = 0
        total_tests = len(test_results)
        
        for test_name, result in test_results:
            status = "✅ 通过" if result else "❌ 失败"
            print(f"{test_name:<20} {status}")
            if result:
                passed_tests += 1
        
        success_rate = (passed_tests / total_tests) * 100
        
        print(f"\n📊 总体测试结果: {passed_tests}/{total_tests} 通过 ({success_rate:.1f}%)")
        
        if success_rate >= 80:
            print("🎉 泰拉瑞亚向导RAG功能验证成功！")
            return True
        else:
            print("⚠️ 部分功能存在问题，需要进一步调试")
            return False

if __name__ == "__main__":
    tester = TerrariaGuideRAGTest()
    success = tester.run_comprehensive_test()
    sys.exit(0 if success else 1)
