#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
哈利·波特维基数据处理器
将爬取的JSON数据处理成适合RAG系统的格式
"""

import json
import os
import re
from typing import List, Dict, Any
from dataclasses import dataclass
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

@dataclass
class ProcessedKnowledge:
    """处理后的知识数据"""
    title: str
    content: str
    knowledge_type: str
    importance: int
    source: str
    source_url: str
    tags: List[str]
    character_id: int = 1  # 默认为哈利·波特

class HarryPotterDataProcessor:
    """哈利·波特数据处理器"""
    
    def __init__(self, data_dir: str = "data/harry_potter_wiki"):
        self.data_dir = data_dir
        self.processed_data = []
        
        # 知识类型映射
        self.knowledge_type_mapping = {
            "MAIN_CHARACTER": "PERSONALITY",     # 主要角色 -> 性格特征
            "HOGWARTS": "BASIC_INFO",           # 霍格沃茨 -> 基本信息
            "MAGIC_WORLD": "KNOWLEDGE",         # 魔法世界 -> 专业知识
            "EVENTS": "EVENTS",                 # 事件 -> 重要事件
            "RELATIONSHIPS": "RELATIONSHIPS",    # 关系 -> 人际关系
            "ABILITIES": "ABILITIES",           # 能力 -> 能力技能
            "QUOTES": "QUOTES"                  # 语录 -> 经典语录
        }
    
    def load_crawled_data(self) -> List[Dict[str, Any]]:
        """加载爬取的数据"""
        all_data = []
        
        for root, dirs, files in os.walk(self.data_dir):
            for file in files:
                if file.endswith('.json') and file != 'crawl_report.json':
                    filepath = os.path.join(root, file)
                    try:
                        with open(filepath, 'r', encoding='utf-8') as f:
                            data = json.load(f)
                            all_data.append(data)
                    except Exception as e:
                        logger.error(f"读取文件失败: {filepath}, 错误: {e}")
        
        logger.info(f"加载了 {len(all_data)} 个数据文件")
        return all_data
    
    def split_content(self, content: str, max_chunk_size: int = 500) -> List[str]:
        """将长内容拆分为多个块"""
        if len(content) <= max_chunk_size:
            return [content]
        
        # 按段落拆分
        paragraphs = content.split('\n\n')
        chunks = []
        current_chunk = ""
        
        for paragraph in paragraphs:
            if len(current_chunk) + len(paragraph) > max_chunk_size:
                if current_chunk:
                    chunks.append(current_chunk.strip())
                    current_chunk = paragraph
                else:
                    # 单个段落太长，强制拆分
                    words = paragraph.split('。')
                    for word in words:
                        if len(current_chunk) + len(word) > max_chunk_size:
                            if current_chunk:
                                chunks.append(current_chunk.strip())
                            current_chunk = word
                        else:
                            current_chunk += word + "。"
            else:
                current_chunk += "\n\n" + paragraph if current_chunk else paragraph
        
        if current_chunk:
            chunks.append(current_chunk.strip())
        
        return chunks
    
    def enhance_content_with_context(self, data: Dict[str, Any]) -> str:
        """为内容添加上下文信息"""
        title = data.get('title', '')
        content = data.get('content', '')
        category = data.get('category', '')
        tags = data.get('tags', [])
        
        # 添加标题作为上下文
        enhanced_content = f"【{title}】\n\n{content}"
        
        # 如果有标签，添加关键词说明
        if tags:
            relevant_tags = [tag for tag in tags if len(tag) < 20][:5]
            if relevant_tags:
                enhanced_content += f"\n\n关键词: {', '.join(relevant_tags)}"
        
        return enhanced_content
    
    def determine_importance(self, data: Dict[str, Any]) -> int:
        """智能确定重要性等级"""
        base_importance = data.get('importance', 5)
        title = data.get('title', '').lower()
        content = data.get('content', '')
        
        # 重要角色提升重要性
        important_characters = ['哈利·波特', '赫敏', '罗恩', '邓布利多', '伏地魔', '斯内普']
        for char in important_characters:
            if char in title:
                base_importance = min(10, base_importance + 2)
                break
        
        # 核心概念提升重要性
        core_concepts = ['霍格沃茨', '分院帽', '魁地奇', '魂器', '守护神']
        for concept in core_concepts:
            if concept in title:
                base_importance = min(10, base_importance + 1)
                break
        
        # 内容长度影响重要性
        if len(content) > 1000:
            base_importance = min(10, base_importance + 1)
        elif len(content) < 200:
            base_importance = max(1, base_importance - 1)
        
        return base_importance
    
    def process_data(self) -> List[ProcessedKnowledge]:
        """处理数据"""
        raw_data = self.load_crawled_data()
        processed_knowledge = []
        
        for data in raw_data:
            try:
                # 获取基本信息
                title = data.get('title', '未知标题')
                content = data.get('content', '')
                category = data.get('category', 'BASIC_INFO')
                tags = data.get('tags', [])
                source_url = data.get('source_url', '')
                
                # 跳过空内容
                if len(content.strip()) < 50:
                    logger.warning(f"内容太短，跳过: {title}")
                    continue
                
                # 增强内容
                enhanced_content = self.enhance_content_with_context(data)
                
                # 拆分长内容
                content_chunks = self.split_content(enhanced_content)
                
                # 映射知识类型
                knowledge_type = self.knowledge_type_mapping.get(category, 'BASIC_INFO')
                
                # 确定重要性
                importance = self.determine_importance(data)
                
                # 为每个块创建知识条目
                for i, chunk in enumerate(content_chunks):
                    chunk_title = f"{title}" if len(content_chunks) == 1 else f"{title} (第{i+1}部分)"
                    
                    processed = ProcessedKnowledge(
                        title=chunk_title,
                        content=chunk,
                        knowledge_type=knowledge_type,
                        importance=importance,
                        source="哈利·波特中文维基",
                        source_url=source_url,
                        tags=tags[:5]  # 限制标签数量
                    )
                    
                    processed_knowledge.append(processed)
                
                logger.info(f"处理完成: {title} -> {len(content_chunks)} 个知识块")
                
            except Exception as e:
                logger.error(f"处理数据失败: {data.get('title', 'unknown')}, 错误: {e}")
                continue
        
        logger.info(f"数据处理完成，共生成 {len(processed_knowledge)} 个知识条目")
        return processed_knowledge
    
    def save_processed_data(self, processed_data: List[ProcessedKnowledge], output_file: str = "processed_harry_potter_knowledge.json"):
        """保存处理后的数据"""
        output_path = os.path.join(os.path.dirname(self.data_dir), output_file)
        
        # 转换为字典列表
        data_list = []
        for item in processed_data:
            data_dict = {
                "characterId": item.character_id,
                "title": item.title,
                "content": item.content,
                "knowledgeType": item.knowledge_type,
                "importance": item.importance,
                "source": item.source,
                "sourceUrl": item.source_url,
                "tags": item.tags
            }
            data_list.append(data_dict)
        
        # 保存为JSON
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(data_list, f, ensure_ascii=False, indent=2)
        
        logger.info(f"处理后的数据已保存到: {output_path}")
        
        # 生成统计报告
        self.generate_processing_report(processed_data, output_path)
        
        return output_path
    
    def generate_processing_report(self, processed_data: List[ProcessedKnowledge], output_path: str):
        """生成处理报告"""
        # 统计信息
        stats = {
            "总知识条目数": len(processed_data),
            "按类型统计": {},
            "按重要性统计": {},
            "平均内容长度": 0,
            "输出文件": output_path
        }
        
        # 按类型统计
        for item in processed_data:
            knowledge_type = item.knowledge_type
            stats["按类型统计"][knowledge_type] = stats["按类型统计"].get(knowledge_type, 0) + 1
        
        # 按重要性统计
        for item in processed_data:
            importance = item.importance
            stats["按重要性统计"][f"重要性{importance}"] = stats["按重要性统计"].get(f"重要性{importance}", 0) + 1
        
        # 平均长度
        total_length = sum(len(item.content) for item in processed_data)
        stats["平均内容长度"] = total_length // len(processed_data) if processed_data else 0
        
        # 保存报告
        report_path = output_path.replace('.json', '_report.json')
        with open(report_path, 'w', encoding='utf-8') as f:
            json.dump(stats, f, ensure_ascii=False, indent=2)
        
        logger.info(f"处理报告已生成: {report_path}")
        
        # 打印简要统计
        print("\n📊 数据处理统计报告")
        print("=" * 40)
        print(f"总知识条目数: {stats['总知识条目数']}")
        print(f"平均内容长度: {stats['平均内容长度']} 字符")
        print("\n按类型分布:")
        for k, v in stats["按类型统计"].items():
            print(f"  {k}: {v} 条")
        print("\n按重要性分布:")
        for k, v in sorted(stats["按重要性统计"].items()):
            print(f"  {k}: {v} 条")

def main():
    """主函数"""
    print("📚 哈利·波特维基数据处理器")
    print("=" * 50)
    
    processor = HarryPotterDataProcessor()
    
    try:
        # 处理数据
        processed_data = processor.process_data()
        
        if not processed_data:
            print("❌ 没有找到可处理的数据，请先运行爬虫脚本")
            return
        
        # 保存处理后的数据
        output_file = processor.save_processed_data(processed_data)
        
        print(f"\n✅ 数据处理完成！")
        print(f"📁 输出文件: {output_file}")
        print(f"📝 共处理 {len(processed_data)} 个知识条目")
        
    except Exception as e:
        print(f"\n❌ 处理失败: {e}")
        logger.error(f"处理失败: {e}")

if __name__ == "__main__":
    main()
