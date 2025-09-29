#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
RAG知识库导入脚本
将处理后的哈利·波特数据导入到RAG系统
"""

import json
import requests
import time
from typing import List, Dict, Any
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class RAGKnowledgeImporter:
    """RAG知识库导入器"""
    
    def __init__(self, backend_url: str = "http://localhost:18080"):
        self.backend_url = backend_url
        self.api_base = f"{backend_url}/api/knowledge"
        
        # 请求会话
        self.session = requests.Session()
        self.session.headers.update({
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        })
    
    def check_backend_health(self) -> bool:
        """检查后端服务状态"""
        try:
            response = self.session.get(f"{self.backend_url}/api/health", timeout=5)
            return response.status_code == 200
        except Exception as e:
            logger.error(f"后端服务检查失败: {e}")
            return False
    
    def import_knowledge_batch(self, knowledge_list: List[Dict[str, Any]], character_id: int = 1) -> bool:
        """批量导入知识条目（使用新的API格式）"""
        try:
            # 转换为API所需的格式
            knowledge_items = []
            for knowledge in knowledge_list:
                item = {
                    "title": knowledge.get("title", ""),
                    "content": knowledge.get("content", ""),
                    "knowledgeType": knowledge.get("knowledgeType", "KNOWLEDGE"),
                    "importanceScore": knowledge.get("importance", 5),
                    "source": knowledge.get("source", "harry_potter_wiki"),
                    "sourceUrl": knowledge.get("sourceUrl", ""),
                    "tags": knowledge.get("tags", [])
                }
                knowledge_items.append(item)
            
            # 构建批量导入请求
            import_request = {
                "characterId": character_id,
                "knowledgeItems": knowledge_items
            }
            
            # 发送请求
            response = self.session.post(
                f"{self.api_base}/import/text",
                json=import_request,
                timeout=30
            )
            
            if response.status_code == 200:
                result = response.json()
                if result.get("success", False):
                    imported_count = result.get("imported_count", 0)
                    logger.info(f"✅ 批量导入成功: {imported_count} 个知识条目")
                    return True
                else:
                    logger.error(f"❌ 批量导入失败: {result.get('message', '未知错误')}")
                    return False
            else:
                logger.error(f"❌ 批量导入失败: status={response.status_code}, response={response.text}")
                return False
                
        except Exception as e:
            logger.error(f"批量导入知识失败: {e}")
            return False

    def import_single_knowledge(self, knowledge_data: Dict[str, Any]) -> bool:
        """导入单个知识条目（保留兼容性）"""
        return self.import_knowledge_batch([knowledge_data])
    
    def batch_import(self, knowledge_list: List[Dict[str, Any]], batch_size: int = 10, character_id: int = 1) -> Dict[str, int]:
        """批量导入知识（优化为真正的批量处理）"""
        total = len(knowledge_list)
        success_count = 0
        failed_count = 0
        
        logger.info(f"开始批量导入 {total} 个知识条目（每批 {batch_size} 个）...")
        
        # 按批次处理
        for i in range(0, total, batch_size):
            try:
                # 获取当前批次
                batch = knowledge_list[i:i + batch_size]
                batch_num = i // batch_size + 1
                total_batches = (total + batch_size - 1) // batch_size
                
                logger.info(f"📦 处理第 {batch_num}/{total_batches} 批，包含 {len(batch)} 个条目...")
                
                # 批量导入
                if self.import_knowledge_batch(batch, character_id):
                    success_count += len(batch)
                    logger.info(f"✅ 第 {batch_num} 批导入成功")
                else:
                    failed_count += len(batch)
                    logger.error(f"❌ 第 {batch_num} 批导入失败")
                
                # 进度显示
                processed = min(i + batch_size, total)
                progress = processed / total * 100
                logger.info(f"📊 总体进度: {processed}/{total} ({progress:.1f}%)")
                
                # 批次间延迟，避免压垮服务器
                if i + batch_size < total:
                    time.sleep(2)
                    
            except KeyboardInterrupt:
                logger.info("⏹️ 用户中断导入")
                break
            except Exception as e:
                logger.error(f"处理第 {batch_num} 批时出错: {e}")
                failed_count += len(batch)
                continue
        
        result = {
            "total": total,
            "success": success_count,
            "failed": failed_count,
            "success_rate": success_count / total * 100 if total > 0 else 0
        }
        
        return result
    
    def import_from_file(self, json_file: str) -> Dict[str, int]:
        """从JSON文件导入知识"""
        try:
            with open(json_file, 'r', encoding='utf-8') as f:
                knowledge_list = json.load(f)
            
            logger.info(f"从文件加载了 {len(knowledge_list)} 个知识条目")
            
            # 检测知识条目中的character_id
            character_id = 1  # 默认值
            if knowledge_list and isinstance(knowledge_list[0], dict):
                # 优先检查 character_id 字段
                detected_id = knowledge_list[0].get('character_id')
                if detected_id is not None:
                    character_id = int(detected_id)
                    logger.info(f"检测到角色ID (character_id): {character_id}")
                else:
                    # 如果 character_id 不存在，检查 characterId 字段
                    detected_id = knowledge_list[0].get('characterId')
                    if detected_id is not None:
                        character_id = int(detected_id)
                        logger.info(f"检测到角色ID (characterId): {character_id}")
            
            return self.batch_import(knowledge_list, character_id=character_id)
            
        except Exception as e:
            logger.error(f"从文件导入失败: {e}")
            return {"total": 0, "success": 0, "failed": 0, "success_rate": 0}

def main():
    """主函数"""
    print("🚀 RAG知识库导入工具")
    print("=" * 50)
    
    # 检查参数
    import sys
    if len(sys.argv) < 2:
        print("❌ 用法: python import_to_rag.py <knowledge_file.json>")
        print("   例如: python import_to_rag.py processed_harry_potter_knowledge.json")
        return
    
    knowledge_file = sys.argv[1]
    backend_url = sys.argv[2] if len(sys.argv) > 2 else "http://localhost:18080"
    
    # 创建导入器
    importer = RAGKnowledgeImporter(backend_url)
    
    # 检查后端服务
    print(f"🔍 检查后端服务: {backend_url}")
    if not importer.check_backend_health():
        print(f"❌ 后端服务不可用，请确保服务正在运行")
        print(f"   启动命令: cd backend && mvn spring-boot:run -pl roleplay-api")
        return
    
    print("✅ 后端服务正常")
    
    # 导入数据
    try:
        result = importer.import_from_file(knowledge_file)
        
        # 显示结果
        print("\n📊 导入结果统计")
        print("=" * 30)
        print(f"总条目数: {result['total']}")
        print(f"成功导入: {result['success']}")
        print(f"导入失败: {result['failed']}")
        print(f"成功率: {result['success_rate']:.1f}%")
        
        if result['success'] > 0:
            print(f"\n✅ 导入完成！成功导入 {result['success']} 个知识条目")
            print("🧙‍♂️ 现在可以测试哈利·波特的RAG增强对话了！")
        else:
            print("\n❌ 导入失败，请检查日志信息")
            
    except KeyboardInterrupt:
        print("\n⏹️ 用户中断导入")
    except Exception as e:
        print(f"\n❌ 导入失败: {e}")

if __name__ == "__main__":
    main()
