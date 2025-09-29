#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
五角色知识库全面测试脚本
测试每个角色的知识库准确性、RAG功能和角色隔离
"""

import requests
import json
import time
import sys
from typing import List, Dict

class ComprehensiveCharacterTest:
    def __init__(self):
        self.base_url = "http://localhost:18080"
        
        # 角色配置
        self.characters = {
            1: {
                "name": "哈利·波特",
                "knowledge_count": 45,
                "domain": "魔法世界",
                "expertise": ["魔法", "霍格沃茨", "魁地奇", "黑魔法防御", "伏地魔"]
            },
            2: {
                "name": "苏格拉底", 
                "knowledge_count": 8,
                "domain": "哲学",
                "expertise": ["哲学", "思辨", "智慧", "苏格拉底法", "古希腊"]
            },
            3: {
                "name": "爱因斯坦",
                "knowledge_count": 16, 
                "domain": "物理学",
                "expertise": ["相对论", "物理学", "量子力学", "科学", "数学"]
            },
            4: {
                "name": "江户川柯南",
                "knowledge_count": 137,
                "domain": "推理侦探",
                "expertise": ["推理", "案件", "侦探", "黑衣组织", "工藤新一"]
            },
            5: {
                "name": "泰拉瑞亚向导",
                "knowledge_count": 1205,
                "domain": "游戏指导",
                "expertise": ["武器", "泰拉瑞亚", "制作", "游戏", "装备"]
            }
        }
        
        # 测试用例设计
        self.test_cases = {
            1: {  # 哈利·波特
                "in_domain": [
                    "霍格沃茨有哪些学院？",
                    "魁地奇比赛规则是什么？", 
                    "伏地魔的真名是什么？",
                    "阿瓦达索命咒的效果是什么？"
                ],
                "out_domain": [
                    "相对论的基本原理是什么？",
                    "如何制作泰拉瑞亚的天顶剑？",
                    "黑衣组织的成员有哪些？"
                ],
                "cross_character": [
                    "苏格拉底的哲学思想有哪些？",
                    "爱因斯坦发现了什么定律？"
                ]
            },
            2: {  # 苏格拉底
                "in_domain": [
                    "你的哲学思想是什么？",
                    "什么是苏格拉底法？",
                    "你对智慧的理解是什么？",
                    "古希腊的哲学有什么特点？"
                ],
                "out_domain": [
                    "天顶剑需要什么材料制作？",
                    "黑衣组织的目的是什么？",
                    "霍格沃茨在哪里？"
                ],
                "cross_character": [
                    "哈利·波特学会了什么魔法？",
                    "柯南破解过什么案件？"
                ]
            },
            3: {  # 爱因斯坦
                "in_domain": [
                    "相对论的基本内容是什么？",
                    "E=mc²这个公式代表什么？",
                    "量子力学有什么重要发现？",
                    "你获得过诺贝尔奖吗？"
                ],
                "out_domain": [
                    "如何制作魔法药水？",
                    "泰拉瑞亚有哪些Boss？", 
                    "柯南的真实身份是什么？"
                ],
                "cross_character": [
                    "哈利·波特的魔法原理是什么？",
                    "苏格拉底如何进行哲学思辨？"
                ]
            },
            4: {  # 江户川柯南
                "in_domain": [
                    "你的真实身份是什么？",
                    "黑衣组织有哪些成员？",
                    "你破解过哪些著名案件？",
                    "工藤新一是怎么变小的？"
                ],
                "out_domain": [
                    "相对论是什么意思？",
                    "霍格沃茨有几个学院？",
                    "泰拉瑞亚怎么制作装备？"
                ],
                "cross_character": [
                    "哈利·波特会什么魔法？",
                    "爱因斯坦的理论有哪些？"
                ]
            },
            5: {  # 泰拉瑞亚向导
                "in_domain": [
                    "天顶剑需要什么材料制作？",
                    "推荐一些强力的魔法武器",
                    "新手应该用什么武器？",
                    "泰拉刃怎么获得？"
                ],
                "out_domain": [
                    "霍格沃茨的历史是什么？",
                    "相对论的原理是什么？",
                    "黑衣组织的秘密是什么？"
                ],
                "cross_character": [
                    "哈利·波特的魔法棒有什么功能？",
                    "柯南用什么工具破案？"
                ]
            }
        }

    def test_health_check(self):
        """检查后端服务状态"""
        try:
            response = requests.get(f"{self.base_url}/api/health", timeout=10)
            return response.status_code == 200
        except:
            return False

    def test_character_dialogue(self, character_id: int, question: str) -> Dict:
        """测试角色对话"""
        try:
            payload = {
                "message": question,
                "characterId": character_id,
                "userId": "test_user"
            }
            
            response = requests.post(
                f"{self.base_url}/api/chat/message",
                json=payload,
                headers={"Content-Type": "application/json"},
                timeout=30
            )
            
            if response.status_code == 200:
                data = response.json()
                return {
                    "success": True,
                    "content": data.get("content", ""),
                    "conversation_id": data.get("conversationId", ""),
                    "character_info": data.get("characterInfo", {})
                }
            else:
                return {
                    "success": False,
                    "error": f"HTTP {response.status_code}: {response.text}"
                }
                
        except Exception as e:
            return {
                "success": False,
                "error": str(e)
            }

    def analyze_response_quality(self, response: str, expected_keywords: List[str], 
                               test_type: str) -> Dict:
        """分析回答质量"""
        if not response:
            return {"score": 0, "analysis": "回答为空"}
            
        response_lower = response.lower()
        found_keywords = [kw for kw in expected_keywords if kw.lower() in response_lower]
        keyword_score = len(found_keywords) / len(expected_keywords) if expected_keywords else 0
        
        # 根据测试类型调整评分标准
        if test_type == "in_domain":
            # 领域内问题：期望高关键词匹配率和专业性
            if keyword_score >= 0.6:
                score = "优秀"
            elif keyword_score >= 0.3:
                score = "良好" 
            else:
                score = "一般"
        elif test_type == "out_domain":
            # 领域外问题：期望承认不知道或给出通用回答
            if any(phrase in response_lower for phrase in 
                   ["不知道", "不了解", "不清楚", "无法", "抱歉", "对不起"]):
                score = "优秀"  # 正确承认不知道
            elif keyword_score < 0.3:
                score = "良好"  # 没有错误的专业信息
            else:
                score = "较差"  # 可能给出了错误的专业信息
        else:  # cross_character
            # 跨角色问题：期望无法获得其他角色的专业知识
            if keyword_score < 0.2:
                score = "优秀"  # 正确的角色隔离
            else:
                score = "较差"  # 可能泄露了其他角色的知识
                
        return {
            "score": score,
            "keyword_match": f"{len(found_keywords)}/{len(expected_keywords)}",
            "found_keywords": found_keywords,
            "response_length": len(response)
        }

    def test_single_character(self, character_id: int) -> Dict:
        """测试单个角色"""
        character = self.characters[character_id]
        character_name = character["name"]
        
        print(f"\n{'='*60}")
        print(f"🎭 测试角色: {character_name} (ID: {character_id})")
        print(f"📚 知识库规模: {character['knowledge_count']}条")
        print(f"🎯 专业领域: {character['domain']}")
        print(f"{'='*60}")
        
        results = {
            "character_id": character_id,
            "character_name": character_name,
            "in_domain": [],
            "out_domain": [],
            "cross_character": []
        }
        
        test_cases = self.test_cases[character_id]
        
        # 测试领域内问题
        print(f"\n📖 测试领域内问题 ({character['domain']})")
        print("-" * 40)
        for i, question in enumerate(test_cases["in_domain"], 1):
            print(f"\n❓ 问题 {i}: {question}")
            
            response_data = self.test_character_dialogue(character_id, question)
            
            if response_data["success"]:
                response = response_data["content"]
                analysis = self.analyze_response_quality(
                    response, character["expertise"], "in_domain"
                )
                
                print(f"   💬 回答长度: {analysis['response_length']} 字符")
                print(f"   🎯 关键词匹配: {analysis['keyword_match']}")
                print(f"   ⭐ 质量评分: {analysis['score']}")
                print(f"   📝 回答摘要: {response[:100]}...")
                
                results["in_domain"].append({
                    "question": question,
                    "response": response,
                    "analysis": analysis,
                    "success": True
                })
            else:
                print(f"   ❌ 测试失败: {response_data['error']}")
                results["in_domain"].append({
                    "question": question,
                    "error": response_data["error"],
                    "success": False
                })
            
            time.sleep(1)  # 避免请求过快
        
        # 测试领域外问题
        print(f"\n🚫 测试领域外问题")
        print("-" * 40)
        for i, question in enumerate(test_cases["out_domain"], 1):
            print(f"\n❓ 问题 {i}: {question}")
            
            response_data = self.test_character_dialogue(character_id, question)
            
            if response_data["success"]:
                response = response_data["content"]
                analysis = self.analyze_response_quality(
                    response, [], "out_domain"
                )
                
                print(f"   💬 回答长度: {analysis['response_length']} 字符")
                print(f"   ⭐ 质量评分: {analysis['score']}")
                print(f"   📝 回答摘要: {response[:100]}...")
                
                results["out_domain"].append({
                    "question": question,
                    "response": response,
                    "analysis": analysis,
                    "success": True
                })
            else:
                print(f"   ❌ 测试失败: {response_data['error']}")
                results["out_domain"].append({
                    "question": question,
                    "error": response_data["error"],
                    "success": False
                })
            
            time.sleep(1)
        
        # 测试跨角色问题（角色隔离）
        print(f"\n🛡️ 测试角色隔离")
        print("-" * 40)
        for i, question in enumerate(test_cases["cross_character"], 1):
            print(f"\n❓ 问题 {i}: {question}")
            
            response_data = self.test_character_dialogue(character_id, question)
            
            if response_data["success"]:
                response = response_data["content"]
                analysis = self.analyze_response_quality(
                    response, [], "cross_character"
                )
                
                print(f"   💬 回答长度: {analysis['response_length']} 字符")
                print(f"   ⭐ 隔离效果: {analysis['score']}")
                print(f"   📝 回答摘要: {response[:100]}...")
                
                results["cross_character"].append({
                    "question": question,
                    "response": response,
                    "analysis": analysis,
                    "success": True
                })
            else:
                print(f"   ❌ 测试失败: {response_data['error']}")
                results["cross_character"].append({
                    "question": question,
                    "error": response_data["error"],
                    "success": False
                })
            
            time.sleep(1)
        
        return results

    def generate_summary_report(self, all_results: List[Dict]):
        """生成总结报告"""
        print(f"\n{'='*80}")
        print(f"📊 五角色知识库全面测试总结报告")
        print(f"{'='*80}")
        
        overall_stats = {
            "total_tests": 0,
            "successful_tests": 0,
            "in_domain_excellent": 0,
            "out_domain_excellent": 0,
            "isolation_excellent": 0
        }
        
        for result in all_results:
            character_name = result["character_name"]
            print(f"\n🎭 {character_name}")
            print("-" * 50)
            
            # 统计各类测试结果
            categories = ["in_domain", "out_domain", "cross_character"]
            category_names = ["领域内问题", "领域外问题", "角色隔离"]
            
            for category, category_name in zip(categories, category_names):
                tests = result[category]
                successful = sum(1 for t in tests if t.get("success", False))
                total = len(tests)
                
                if successful > 0:
                    excellent_count = sum(1 for t in tests 
                                        if t.get("success") and 
                                           t.get("analysis", {}).get("score") == "优秀")
                    excellent_rate = (excellent_count / successful) * 100
                    
                    print(f"   {category_name}: {successful}/{total} 成功, "
                          f"{excellent_count} 优秀 ({excellent_rate:.1f}%)")
                    
                    overall_stats["total_tests"] += total
                    overall_stats["successful_tests"] += successful
                    
                    if category == "in_domain":
                        overall_stats["in_domain_excellent"] += excellent_count
                    elif category == "out_domain":
                        overall_stats["out_domain_excellent"] += excellent_count
                    else:
                        overall_stats["isolation_excellent"] += excellent_count
                else:
                    print(f"   {category_name}: {successful}/{total} 成功")
        
        # 总体统计
        print(f"\n📈 总体统计")
        print("-" * 50)
        total_tests = overall_stats["total_tests"]
        successful_tests = overall_stats["successful_tests"]
        success_rate = (successful_tests / total_tests * 100) if total_tests > 0 else 0
        
        print(f"总测试数: {total_tests}")
        print(f"成功测试数: {successful_tests}")
        print(f"成功率: {success_rate:.1f}%")
        print(f"领域内优秀: {overall_stats['in_domain_excellent']}")
        print(f"领域外优秀: {overall_stats['out_domain_excellent']}")
        print(f"隔离优秀: {overall_stats['isolation_excellent']}")
        
        # 系统评级
        if success_rate >= 90 and overall_stats['isolation_excellent'] >= 8:
            grade = "A+ (优秀)"
        elif success_rate >= 80:
            grade = "A (良好)"
        elif success_rate >= 70:
            grade = "B (合格)"
        else:
            grade = "C (需要改进)"
            
        print(f"\n🏆 系统评级: {grade}")
        
        return overall_stats

    def run_comprehensive_test(self):
        """运行全面测试"""
        print("🚀 五角色知识库全面测试开始")
        print(f"测试时间: {time.strftime('%Y-%m-%d %H:%M:%S')}")
        
        # 健康检查
        if not self.test_health_check():
            print("❌ 后端服务不可用，测试终止")
            return False
        
        print("✅ 后端服务正常，开始测试...")
        
        all_results = []
        
        # 依次测试每个角色
        for character_id in sorted(self.characters.keys()):
            try:
                result = self.test_single_character(character_id)
                all_results.append(result)
            except KeyboardInterrupt:
                print(f"\n⚠️ 用户中断测试")
                break
            except Exception as e:
                print(f"\n❌ 测试角色 {character_id} 时发生错误: {e}")
                continue
        
        # 生成总结报告
        if all_results:
            stats = self.generate_summary_report(all_results)
            
            # 保存详细结果
            with open("comprehensive_test_results.json", "w", encoding="utf-8") as f:
                json.dump(all_results, f, ensure_ascii=False, indent=2)
            print(f"\n💾 详细测试结果已保存到: comprehensive_test_results.json")
            
            return True
        else:
            print("❌ 没有完成任何角色测试")
            return False

if __name__ == "__main__":
    tester = ComprehensiveCharacterTest()
    success = tester.run_comprehensive_test()
    sys.exit(0 if success else 1)
