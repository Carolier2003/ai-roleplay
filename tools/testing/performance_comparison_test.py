#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
性能优化效果对比测试
重点测试泰拉瑞亚向导（1205条知识）的响应时间
"""

import requests
import json
import time
import statistics

class PerformanceComparisonTest:
    def __init__(self):
        self.base_url = "http://localhost:18080"
        
        # 测试用例 - 针对泰拉瑞亚向导
        self.test_cases = [
            {
                "character_id": 5,
                "character_name": "泰拉瑞亚向导",
                "questions": [
                    "天顶剑需要什么材料制作？",
                    "推荐一些强力的魔法武器",
                    "新手应该用什么武器？",
                    "泰拉刃怎么获得？",
                    "最强的近战武器是什么？"
                ]
            },
            {
                "character_id": 4,
                "character_name": "江户川柯南",
                "questions": [
                    "你的真实身份是什么？",
                    "黑衣组织有哪些成员？",
                    "你破解过哪些著名案件？"
                ]
            },
            {
                "character_id": 1,
                "character_name": "哈利·波特",
                "questions": [
                    "霍格沃茨有哪些学院？",
                    "魁地奇比赛规则是什么？",
                    "伏地魔的真名是什么？"
                ]
            }
        ]

    def test_single_request(self, character_id, question):
        """测试单个请求的响应时间"""
        start_time = time.time()
        
        try:
            payload = {
                "message": question,
                "characterId": character_id,
                "userId": "performance_test"
            }
            
            response = requests.post(
                f"{self.base_url}/api/chat/message",
                json=payload,
                headers={"Content-Type": "application/json"},
                timeout=30  # 30秒超时
            )
            
            end_time = time.time()
            response_time = end_time - start_time
            
            if response.status_code == 200:
                data = response.json()
                return {
                    "success": True,
                    "response_time": response_time,
                    "content_length": len(data.get("content", "")),
                    "status": "success"
                }
            else:
                return {
                    "success": False,
                    "response_time": response_time,
                    "error": f"HTTP {response.status_code}",
                    "status": "http_error"
                }
                
        except requests.exceptions.Timeout:
            end_time = time.time()
            return {
                "success": False,
                "response_time": end_time - start_time,
                "error": "Timeout (>30s)",
                "status": "timeout"
            }
        except Exception as e:
            end_time = time.time()
            return {
                "success": False,
                "response_time": end_time - start_time,
                "error": str(e),
                "status": "error"
            }

    def run_performance_test(self):
        """运行性能测试"""
        print("🚀 性能优化效果测试开始")
        print(f"测试时间: {time.strftime('%Y-%m-%d %H:%M:%S')}")
        print("=" * 60)
        
        all_results = {}
        
        for test_case in self.test_cases:
            character_id = test_case["character_id"]
            character_name = test_case["character_name"]
            questions = test_case["questions"]
            
            print(f"\n🎭 测试角色: {character_name} (ID: {character_id})")
            print("-" * 50)
            
            response_times = []
            success_count = 0
            timeout_count = 0
            
            for i, question in enumerate(questions, 1):
                print(f"\n❓ 问题 {i}: {question}")
                
                result = self.test_single_request(character_id, question)
                response_time = result["response_time"]
                
                if result["success"]:
                    success_count += 1
                    response_times.append(response_time)
                    content_length = result.get("content_length", 0)
                    print(f"   ✅ 成功: {response_time:.2f}s, 内容长度: {content_length}")
                else:
                    if result["status"] == "timeout":
                        timeout_count += 1
                    print(f"   ❌ 失败: {response_time:.2f}s, 错误: {result['error']}")
                
                # 请求间隔，避免过载
                time.sleep(1)
            
            # 统计结果
            if response_times:
                avg_time = statistics.mean(response_times)
                min_time = min(response_times)
                max_time = max(response_times)
                
                print(f"\n📊 {character_name} 性能统计:")
                print(f"   成功率: {success_count}/{len(questions)} ({success_count/len(questions)*100:.1f}%)")
                print(f"   超时次数: {timeout_count}")
                print(f"   平均响应时间: {avg_time:.2f}s")
                print(f"   最快响应: {min_time:.2f}s")
                print(f"   最慢响应: {max_time:.2f}s")
                
                # 性能评级
                if avg_time < 3:
                    grade = "优秀 (< 3s)"
                elif avg_time < 8:
                    grade = "良好 (< 8s)"
                elif avg_time < 15:
                    grade = "可接受 (< 15s)"
                else:
                    grade = "需要优化 (≥ 15s)"
                
                print(f"   性能评级: {grade}")
                
                all_results[character_name] = {
                    "character_id": character_id,
                    "success_rate": success_count / len(questions),
                    "timeout_count": timeout_count,
                    "avg_response_time": avg_time,
                    "min_response_time": min_time,
                    "max_response_time": max_time,
                    "grade": grade
                }
            else:
                print(f"\n❌ {character_name}: 没有成功的请求")
                all_results[character_name] = {
                    "character_id": character_id,
                    "success_rate": 0,
                    "timeout_count": timeout_count,
                    "grade": "测试失败"
                }
        
        # 生成总结报告
        self.generate_performance_report(all_results)
        
        return all_results

    def generate_performance_report(self, results):
        """生成性能优化总结报告"""
        print(f"\n{'='*60}")
        print("📈 性能优化效果总结报告")
        print(f"{'='*60}")
        
        print("\n🎯 关键优化效果:")
        print("1. 批量数据库查询优化 - 解决N+1查询问题")
        print("2. 智能参数调整 - 根据知识库大小优化topK和阈值")
        print("3. 超时控制机制 - 10秒向量搜索超时")
        print("4. 性能监控日志 - 详细的耗时统计")
        
        # 按角色展示结果
        print(f"\n📊 各角色性能表现:")
        print("-" * 50)
        
        for character_name, stats in results.items():
            if "avg_response_time" in stats:
                print(f"{character_name:12s} | "
                      f"成功率: {stats['success_rate']*100:5.1f}% | "
                      f"平均响应: {stats['avg_response_time']:5.2f}s | "
                      f"评级: {stats['grade']}")
            else:
                print(f"{character_name:12s} | 测试失败")
        
        # 重点关注泰拉瑞亚向导
        terraria_stats = results.get("泰拉瑞亚向导")
        if terraria_stats and "avg_response_time" in terraria_stats:
            avg_time = terraria_stats["avg_response_time"]
            timeout_count = terraria_stats["timeout_count"]
            
            print(f"\n🏆 泰拉瑞亚向导优化效果分析:")
            print(f"   知识库规模: 1205条 (最大)")
            print(f"   平均响应时间: {avg_time:.2f}s")
            print(f"   超时次数: {timeout_count}")
            
            if avg_time < 8 and timeout_count == 0:
                print(f"   🎉 优化效果: 优秀! 从30s+降至{avg_time:.1f}s")
            elif avg_time < 15:
                print(f"   ✅ 优化效果: 良好! 显著改善性能")
            else:
                print(f"   ⚠️ 优化效果: 仍需进一步优化")
        
        # 整体评估
        successful_tests = sum(1 for stats in results.values() if stats.get("success_rate", 0) > 0)
        total_tests = len(results)
        
        print(f"\n🏅 整体系统评估:")
        print(f"   测试角色: {successful_tests}/{total_tests} 成功")
        
        if successful_tests == total_tests:
            avg_times = [stats["avg_response_time"] for stats in results.values() 
                        if "avg_response_time" in stats]
            if avg_times:
                overall_avg = statistics.mean(avg_times)
                if overall_avg < 5:
                    overall_grade = "A+ (优秀)"
                elif overall_avg < 10:
                    overall_grade = "A (良好)"
                else:
                    overall_grade = "B (合格)"
                
                print(f"   系统平均响应时间: {overall_avg:.2f}s")
                print(f"   系统性能评级: {overall_grade}")
            else:
                print(f"   系统性能评级: 数据不足")
        else:
            print(f"   系统性能评级: C (需要修复失败的角色)")

if __name__ == "__main__":
    tester = PerformanceComparisonTest()
    results = tester.run_performance_test()
    
    # 保存详细结果
    with open("performance_test_results.json", "w", encoding="utf-8") as f:
        json.dump(results, f, ensure_ascii=False, indent=2)
    print(f"\n💾 详细测试结果已保存到: performance_test_results.json")
