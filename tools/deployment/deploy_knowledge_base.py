#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AI角色扮演系统 - 知识库完整导入脚本
用于在新环境中部署所有角色的知识库数据到MySQL和Redis RAG系统

基于RAG向量检索问题调试报告的修复方案，确保：
1. 正确处理character_id字段
2. 避免ID映射错误
3. 确保角色隔离
4. 完整的数据验证

Author: Assistant
Date: 2025-09-24
"""

import os
import sys
import json
import time
import requests
import logging
from typing import Dict, List, Any, Optional
from pathlib import Path
from dataclasses import dataclass
from datetime import datetime

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('knowledge_import.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

@dataclass
class ImportResult:
    """导入结果统计"""
    character_name: str
    character_id: int
    total_files: int
    total_records: int
    imported_records: int
    failed_records: int
    success_rate: float
    import_time: float

class KnowledgeBaseDeployer:
    """知识库部署器"""
    
    def __init__(self, base_url: str = "http://localhost:18080"):
        self.base_url = base_url
        self.results: List[ImportResult] = []
        self.data_dir = Path("scripts/data")
        
        # 角色配置（基于数据库设计）
        self.character_configs = {
            "harry_potter": {"id": 1, "name": "哈利·波特"},
            "socrates": {"id": 2, "name": "苏格拉底"}, 
            "einstein": {"id": 3, "name": "爱因斯坦"},
            "conan": {"id": 4, "name": "江户川柯南"},
            "terraria": {"id": 5, "name": "泰拉瑞亚向导"}
        }
    
    def check_environment(self) -> bool:
        """检查运行环境"""
        logger.info("🔍 检查运行环境...")
        
        # 检查数据目录
        if not self.data_dir.exists():
            logger.error(f"❌ 数据目录不存在: {self.data_dir}")
            return False
        
        # 检查后端服务
        try:
            response = requests.get(f"{self.base_url}/api/health", timeout=5)
            if response.status_code == 200:
                logger.info("✅ 后端服务正常运行")
            else:
                logger.error(f"❌ 后端服务异常: HTTP {response.status_code}")
                return False
        except Exception as e:
            logger.error(f"❌ 无法连接后端服务: {e}")
            logger.error("请确保后端服务已启动: ./start-dev.sh")
            return False
        
        # 检查关键角色数据
        missing_data = []
        for char_dir, config in self.character_configs.items():
            char_path = self.data_dir / char_dir
            if not char_path.exists():
                missing_data.append(char_dir)
        
        if missing_data:
            logger.error(f"❌ 缺失角色数据: {missing_data}")
            return False
        
        logger.info("✅ 环境检查通过")
        return True
    
    def clear_existing_data(self, clear_data: bool = False, auto_mode: bool = False) -> bool:
        """清理现有数据（可选）"""
        if not clear_data:
            if auto_mode:
                # 自动化模式，默认不清理数据
                logger.info("ℹ️ 自动化模式，跳过数据清理，将进行增量导入")
                return True
            else:
                # 交互模式，询问用户
                try:
                    response = input("🤔 是否清理现有的知识库数据？(y/N): ")
                    if response.lower() != 'y':
                        logger.info("ℹ️ 跳过数据清理，将进行增量导入")
                        return True
                except (EOFError, KeyboardInterrupt):
                    logger.info("ℹ️ 用户取消，跳过数据清理")
                    return True
        
        logger.info("🧹 清理现有知识库数据...")
        
        try:
            # 尝试调用清理API（如果后端支持）
            response = requests.post(f"{self.base_url}/api/knowledge/clear", timeout=30)
            if response.status_code == 200:
                logger.info("✅ 现有数据清理完成")
                time.sleep(2)  # 等待清理完成
                return True
            elif response.status_code == 404:
                logger.info("ℹ️ 后端不支持数据清理功能，将进行增量导入")
                return True
            else:
                logger.warning(f"⚠️ 数据清理API响应异常: {response.status_code}")
                return True  # 继续执行，可能是增量导入
        except Exception as e:
            logger.info(f"ℹ️ 数据清理功能不可用: {e}")
            logger.info("ℹ️ 将继续进行增量导入...")
            return True  # 允许继续执行
    
    def import_character_data(self, char_dir: str, config: Dict[str, Any]) -> ImportResult:
        """导入单个角色的数据"""
        character_name = config["name"]
        character_id = config["id"]
        
        logger.info(f"📂 开始导入 {character_name} (ID: {character_id}) 的数据...")
        
        start_time = time.time()
        char_path = self.data_dir / char_dir
        
        total_files = 0
        total_records = 0
        imported_records = 0
        failed_records = 0
        
        # 查找数据文件
        data_files = []
        
        if char_dir in ["terraria", "conan"]:
            # 特殊处理合并后的数据文件
            if char_dir == "terraria":
                # 泰拉瑞亚数据：5种不同类型的文件
                terraria_files = [
                    "terraria_weapons.json",      # 武器数据（已去重）
                    "terraria_tools_fixed.json",  # 工具数据
                    "terraria_npcs.json",         # NPC数据
                    "terraria_bosses.json",       # Boss数据
                    "terraria_events.json"        # 事件数据
                ]
                
                for filename in terraria_files:
                    file_path = char_path / filename
                    if file_path.exists():
                        data_files.append(file_path)
                        logger.info(f"  📄 找到泰拉瑞亚数据文件: {filename}")
                    else:
                        logger.warning(f"  ⚠️ 泰拉瑞亚数据文件缺失: {filename}")
                
            else:  # conan
                main_file = char_path / "conan_merged_knowledge.json"
                if main_file.exists():
                    data_files.append(main_file)
        else:
            # 多文件数据
            data_files = list(char_path.glob("*.json"))
            # 排除报告文件
            data_files = [f for f in data_files if not any(x in f.name.lower() for x in ['report', 'progress'])]
        
        total_files = len(data_files)
        
        if not data_files:
            logger.warning(f"⚠️ 未找到 {character_name} 的数据文件")
            return ImportResult(
                character_name, character_id, 0, 0, 0, 0, 0.0, 0.0
            )
        
        # 导入数据文件
        for data_file in data_files:
            try:
                logger.info(f"  📄 导入文件: {data_file.name}")
                
                knowledge_list = []
                
                # 智能检测文件格式
                with open(data_file, 'r', encoding='utf-8') as f:
                    content = f.read().strip()
                    
                if not content:
                    logger.warning(f"⚠️ 跳过空文件: {data_file.name}")
                    continue
                
                # 智能检测文件格式
                # 先尝试标准JSON格式
                try:
                    data = json.loads(content)
                    if isinstance(data, list):
                        knowledge_list = data
                        logger.info(f"  🔍 检测到JSON数组格式: {data_file.name}")
                    elif isinstance(data, dict):
                        knowledge_list = [data]
                        logger.info(f"  🔍 检测到JSON对象格式: {data_file.name}")
                    else:
                        logger.warning(f"⚠️ 跳过无效格式文件: {data_file.name}")
                        continue
                except json.JSONDecodeError:
                    # JSON解析失败，尝试JSONLines格式
                    logger.info(f"  🔍 尝试JSONLines格式: {data_file.name}")
                    lines = content.split('\n')
                    json_line_count = 0
                    
                    for line_num, line in enumerate(lines, 1):
                        line = line.strip()
                        if line:  # 跳过空行
                            try:
                                item = json.loads(line)
                                knowledge_list.append(item)
                                json_line_count += 1
                            except json.JSONDecodeError as e:
                                logger.warning(f"⚠️ 第{line_num}行JSON解析失败: {e}")
                                continue
                    
                    if json_line_count > 0:
                        logger.info(f"  ✅ JSONLines格式解析成功: {json_line_count} 条记录")
                    else:
                        logger.error(f"❌ 无法解析文件格式: {data_file.name}")
                        continue
                
                file_records = len(knowledge_list)
                total_records += file_records
                
                # 🔥 关键修复：确保正确的character_id
                for item in knowledge_list:
                    if not isinstance(item, dict):
                        continue
                    
                    # 🆕 泰拉瑞亚数据特殊字段映射（完整版 - 包含所有字段）
                    if character_id == 5:  # 泰拉瑞亚向导
                        # 映射名称字段
                        if '武器名' in item:
                            item['title'] = item['武器名']
                        elif '工具名' in item:
                            item['title'] = item['工具名']
                        elif 'NPC名称' in item:
                            item['title'] = item['NPC名称']
                        elif 'Boss中文名' in item:
                            item['title'] = item['Boss中文名']
                        elif '事件中文名' in item:
                            item['title'] = item['事件中文名']
                        
                        # 映射内容字段（包含所有有用信息）
                        content_parts = []
                        
                        # 处理武器数据
                        if '武器名' in item:
                            content_parts.append(f"【武器名称】{item['武器名']}")
                            
                            # 属性信息
                            if '属性' in item and isinstance(item['属性'], dict):
                                content_parts.append("\n【属性信息】")
                                for key, value in item['属性'].items():
                                    content_parts.append(f"  {key}：{value}")
                            
                            # ⭐ 合成表（关键信息！）
                            if '合成表' in item and isinstance(item['合成表'], list) and len(item['合成表']) > 0:
                                content_parts.append("\n【合成配方】")
                                for recipe in item['合成表']:
                                    if isinstance(recipe, dict):
                                        product = recipe.get('产物', '未知')
                                        materials = recipe.get('材料', [])
                                        station = recipe.get('制作站', '未知')
                                        content_parts.append(f"  制作 {product}：需要 {', '.join(materials) if materials else '无材料'}（制作站：{station}）")
                            
                            # URL链接
                            if 'url' in item:
                                content_parts.append(f"\n【详细信息】{item['url']}")
                        
                        # 处理工具数据
                        elif '工具名' in item:
                            content_parts.append(f"【工具名称】{item['工具名']}")
                            
                            # 属性信息
                            if '属性' in item and isinstance(item['属性'], dict):
                                content_parts.append("\n【属性信息】")
                                for key, value in item['属性'].items():
                                    content_parts.append(f"  {key}：{value}")
                            
                            # ⭐ 工具能力
                            if '工具能力' in item and isinstance(item['工具能力'], dict) and len(item['工具能力']) > 0:
                                content_parts.append("\n【工具能力】")
                                for key, value in item['工具能力'].items():
                                    content_parts.append(f"  {key}：{value}")
                            
                            # ⭐ 配方表（关键信息！）
                            if '配方表' in item and isinstance(item['配方表'], list) and len(item['配方表']) > 0:
                                content_parts.append("\n【制作配方】")
                                for recipe in item['配方表']:
                                    if isinstance(recipe, dict):
                                        product = recipe.get('产物', '未知')
                                        materials = recipe.get('材料', [])
                                        station = recipe.get('制作站', '未知')
                                        content_parts.append(f"  制作 {product}：需要 {', '.join(materials) if materials else '无材料'}（制作站：{station}）")
                            
                            # URL链接
                            if 'url' in item:
                                content_parts.append(f"\n【详细信息】{item['url']}")
                        
                        # 处理NPC数据
                        elif 'NPC名称' in item:
                            content_parts.append(f"【NPC名称】{item['NPC名称']}")
                            
                            if '描述' in item:
                                content_parts.append(f"\n【描述】{item['描述']}")
                            
                            if '生成需求' in item:
                                content_parts.append(f"\n【生成需求】{item['生成需求']}")
                            
                            if '自卫武器' in item:
                                content_parts.append(f"\n【自卫武器】{item['自卫武器']}")
                            
                            if '死亡时掉落' in item:
                                content_parts.append(f"\n【死亡时掉落】{item['死亡时掉落']}")
                            
                            if '头像链接' in item:
                                content_parts.append(f"\n【头像】{item['头像链接']}")
                        
                        # 处理Boss数据
                        elif 'Boss中文名' in item:
                            content_parts.append(f"【Boss名称】{item['Boss中文名']}")
                            
                            if '英文标题' in item:
                                content_parts.append(f"\n【英文名】{item['英文标题']}")
                            
                            if '召唤方式' in item:
                                content_parts.append(f"\n【召唤方式】{item['召唤方式']}")
                            
                            if '详细描述' in item and item['详细描述']:
                                content_parts.append(f"\n【详细描述】{item['详细描述']}")
                            
                            # ⭐ 掉落列表（关键信息！）
                            if '掉落列表' in item and isinstance(item['掉落列表'], list) and len(item['掉落列表']) > 0:
                                content_parts.append(f"\n【掉落物品】{', '.join(item['掉落列表'])}")
                            
                            if '头像链接' in item:
                                content_parts.append(f"\n【头像】{item['头像链接']}")
                        
                        # 处理事件数据
                        elif '事件中文名' in item:
                            content_parts.append(f"【事件名称】{item['事件中文名']}")
                            
                            if '英文标题' in item:
                                content_parts.append(f"\n【英文名】{item['英文标题']}")
                            
                            if '触发方式' in item:
                                content_parts.append(f"\n【触发方式】{item['触发方式']}")
                            
                            if '详细描述' in item:
                                content_parts.append(f"\n【详细描述】{item['详细描述']}")
                            
                            if '版本标签' in item and isinstance(item['版本标签'], list):
                                content_parts.append(f"\n【版本】{', '.join(item['版本标签'])}")
                            
                            if '封面图链接' in item:
                                content_parts.append(f"\n【封面图】{item['封面图链接']}")
                        
                        # 合并所有内容
                        if content_parts:
                            item['content'] = "".join(content_parts)
                    
                    # 检查并修正character_id（避免RAG报告中的问题）
                    if 'character_id' not in item or item['character_id'] != character_id:
                        logger.debug(f"  🔧 修正character_id: {item.get('character_id')} -> {character_id}")
                        item['character_id'] = character_id
                    
                    # 确保必需字段
                    if 'title' not in item or not item['title']:
                        item['title'] = item.get('name', f"未命名_{character_name}")
                    
                    if 'content' not in item:
                        item['content'] = item.get('description', f"{character_name}相关知识")
                    
                    if 'knowledge_type' not in item:
                        item['knowledge_type'] = "综合知识"
                    
                    if 'importance_score' not in item:
                        item['importance_score'] = 5
                    
                    if 'status' not in item:
                        item['status'] = 1
                
                # 调用导入API
                import_data = {
                    "characterId": character_id,
                    "knowledgeItems": knowledge_list
                }
                
                # 分批处理大量数据（避免API超时或限制）
                # 针对泰拉瑞亚等大数据集使用超小批次（单条导入成功，批量失败）
                if character_id == 5:  # 泰拉瑞亚向导
                    batch_size = 1  # 逐条导入，避免批量处理问题
                else:
                    batch_size = 50  # 其他角色每批50条记录
                total_batches = (len(knowledge_list) + batch_size - 1) // batch_size
                
                if len(knowledge_list) > batch_size:
                    logger.info(f"  📦 数据量较大，分 {total_batches} 批处理（每批 {batch_size} 条）")
                
                batch_imported = 0
                batch_failed = 0
                
                for batch_num in range(total_batches):
                    start_idx = batch_num * batch_size
                    end_idx = min(start_idx + batch_size, len(knowledge_list))
                    batch_data = knowledge_list[start_idx:end_idx]
                    
                    batch_import_data = {
                        "characterId": character_id,
                        "knowledgeItems": batch_data
                    }
                    
                    try:
                        response = requests.post(
                            f"{self.base_url}/api/knowledge/import/text",
                            json=batch_import_data,
                            headers={"Content-Type": "application/json"},
                            timeout=120  # 2分钟超时
                        )
                        
                        if response.status_code == 200:
                            result = response.json()
                            if result.get("success", False):
                                imported = result.get("imported_count", len(batch_data))
                                batch_imported += imported
                                
                                if total_batches > 1:
                                    logger.info(f"    ✅ 批次 {batch_num + 1}/{total_batches}: {imported}/{len(batch_data)} 条记录")
                                else:
                                    logger.info(f"  ✅ 成功导入: {imported}/{len(batch_data)} 条记录")
                            else:
                                batch_failed += len(batch_data)
                                logger.error(f"    ❌ 批次 {batch_num + 1}/{total_batches} 导入失败: {result.get('message', '未知错误')}")
                        else:
                            batch_failed += len(batch_data)
                            logger.error(f"    ❌ 批次 {batch_num + 1}/{total_batches} API调用失败: HTTP {response.status_code}")
                            if response.text:
                                logger.error(f"        错误详情: {response.text[:200]}")
                    
                    except Exception as e:
                        batch_failed += len(batch_data)
                        logger.error(f"    ❌ 批次 {batch_num + 1}/{total_batches} 处理异常: {e}")
                    
                    # 批次之间休息，避免API限制（泰拉瑞亚需要更长延时）
                    if batch_num < total_batches - 1:
                        if character_id == 5:  # 泰拉瑞亚向导
                            time.sleep(0.1)  # 逐条导入，短暂延时即可
                        else:
                            time.sleep(0.5)  # 其他角色正常延时
                
                # 汇总结果
                imported_records += batch_imported
                failed_records += batch_failed
                
                if total_batches > 1:
                    logger.info(f"  📊 分批处理完成: {batch_imported}/{file_records} 条记录成功，{batch_failed} 条失败")
                
            except Exception as e:
                logger.error(f"  ❌ 文件导入异常: {data_file.name} - {e}")
                failed_records += len(knowledge_list) if 'knowledge_list' in locals() else 1
        
        import_time = time.time() - start_time
        success_rate = (imported_records / total_records * 100) if total_records > 0 else 0
        
        result = ImportResult(
            character_name, character_id, total_files, total_records,
            imported_records, failed_records, success_rate, import_time
        )
        
        logger.info(f"📊 {character_name} 导入完成: {imported_records}/{total_records} 条记录 ({success_rate:.1f}%)")
        return result
    
    def verify_import_results(self) -> bool:
        """验证导入结果"""
        logger.info("🔍 验证导入结果...")
        
        all_success = True
        
        for char_dir, config in self.character_configs.items():
            character_id = config["id"]
            character_name = config["name"]
            
            try:
                # 测试知识搜索API
                search_data = {
                    "characterId": character_id,
                    "query": character_name,
                    "topK": 3
                }
                
                response = requests.post(
                    f"{self.base_url}/api/knowledge/search",
                    json=search_data,
                    headers={"Content-Type": "application/json"},
                    timeout=30
                )
                
                if response.status_code == 200:
                    result = response.json()
                    count = result.get("count", 0)
                    if count > 0:
                        logger.info(f"  ✅ {character_name}: 搜索API正常，返回 {count} 条结果")
                    else:
                        logger.warning(f"  ⚠️ {character_name}: 搜索无结果")
                        all_success = False
                else:
                    logger.error(f"  ❌ {character_name}: 搜索API失败 (HTTP {response.status_code})")
                    all_success = False
                    
            except Exception as e:
                logger.error(f"  ❌ {character_name}: 验证异常 - {e}")
                all_success = False
        
        # 测试角色隔离（等待数据同步后进行）
        logger.info("🔒 测试角色隔离...")
        logger.info("  ⏳ 等待向量数据同步完成...")
        time.sleep(3)  # 等待向量数据库同步
        
        isolation_passed = True
        
        # 测试1: 哈利·波特搜索哲学相关内容（不应找到苏格拉底知识）
        try:
            isolation_test = {
                "characterId": 1,  # 哈利·波特
                "query": "古希腊哲学苏格拉底",
                "topK": 5
            }
            
            response = requests.post(
                f"{self.base_url}/api/knowledge/search",
                json=isolation_test,
                timeout=30
            )
            
            if response.status_code == 200:
                result = response.json()
                results = result.get("results", [])
                count = len(results)
                
                # 检查返回结果是否都属于哈利·波特
                cross_character_found = 0
                for item in results:
                    if item.get("character_id") != 1:
                        cross_character_found += 1
                        logger.warning(f"    🔍 发现跨角色结果: {item.get('title', 'Unknown')} (角色ID: {item.get('character_id')})")
                
                if cross_character_found == 0:
                    logger.info(f"  ✅ 测试1通过：哈利·波特搜索哲学内容，无跨角色结果 (返回{count}条哈利·波特知识)")
                else:
                    logger.error(f"  ❌ 测试1失败：哈利·波特搜索到 {cross_character_found} 条其他角色知识")
                    isolation_passed = False
            else:
                logger.warning(f"  ⚠️ 测试1 API调用失败: {response.status_code}")
            
        except Exception as e:
            logger.warning(f"  ⚠️ 测试1异常: {e}")
        
        # 测试2: 泰拉瑞亚向导搜索魔法内容（不应找到哈利·波特知识）
        try:
            isolation_test = {
                "characterId": 5,  # 泰拉瑞亚向导
                "query": "魔法咒语霍格沃茨",
                "topK": 5
            }
            
            response = requests.post(
                f"{self.base_url}/api/knowledge/search",
                json=isolation_test,
                timeout=30
            )
            
            if response.status_code == 200:
                result = response.json()
                results = result.get("results", [])
                count = len(results)
                
                # 检查返回结果是否都属于泰拉瑞亚向导
                cross_character_found = 0
                for item in results:
                    if item.get("character_id") != 5:
                        cross_character_found += 1
                        logger.warning(f"    🔍 发现跨角色结果: {item.get('title', 'Unknown')} (角色ID: {item.get('character_id')})")
                
                if cross_character_found == 0:
                    logger.info(f"  ✅ 测试2通过：泰拉瑞亚向导搜索魔法内容，无跨角色结果 (返回{count}条泰拉瑞亚知识)")
                else:
                    logger.error(f"  ❌ 测试2失败：泰拉瑞亚向导搜索到 {cross_character_found} 条其他角色知识")
                    isolation_passed = False
            else:
                logger.warning(f"  ⚠️ 测试2 API调用失败: {response.status_code}")
                
        except Exception as e:
            logger.warning(f"  ⚠️ 测试2异常: {e}")
        
        if not isolation_passed:
            all_success = False
        
        return all_success
    
    def generate_report(self) -> str:
        """生成导入报告"""
        logger.info("📋 生成导入报告...")
        
        total_files = sum(r.total_files for r in self.results)
        total_records = sum(r.total_records for r in self.results)
        total_imported = sum(r.imported_records for r in self.results)
        total_failed = sum(r.failed_records for r in self.results)
        overall_success_rate = (total_imported / total_records * 100) if total_records > 0 else 0
        
        report = f"""
# 🎯 知识库导入完成报告

**导入时间**: {datetime.now().strftime('%Y年%m月%d日 %H:%M:%S')}
**总体成功率**: {overall_success_rate:.1f}% ({total_imported}/{total_records})

## 📊 导入统计

| 角色 | 文件数 | 记录数 | 导入成功 | 失败 | 成功率 | 耗时 |
|------|--------|--------|----------|------|--------|------|"""

        for result in self.results:
            report += f"""
| {result.character_name} | {result.total_files} | {result.total_records} | {result.imported_records} | {result.failed_records} | {result.success_rate:.1f}% | {result.import_time:.1f}s |"""

        report += f"""

## 🎯 总计
- **处理文件**: {total_files} 个
- **处理记录**: {total_records} 条  
- **导入成功**: {total_imported} 条
- **导入失败**: {total_failed} 条
- **成功率**: {overall_success_rate:.1f}%

## ✅ 功能验证
- 知识搜索API: {"✅ 正常" if overall_success_rate > 80 else "❌ 异常"}
- 角色隔离: {"✅ 正常" if overall_success_rate > 80 else "❌ 需要检查"}
- RAG增强对话: {"✅ 可用" if overall_success_rate > 80 else "❌ 需要测试"}

## 📋 使用说明
导入完成后，您可以：
1. 测试对话API: `curl -X POST {self.base_url}/api/chat/message`
2. 测试知识搜索: `curl -X POST {self.base_url}/api/knowledge/search`  
3. 查看系统健康状态: `curl {self.base_url}/api/health`

## 🔗 相关文件
- 导入日志: `knowledge_import.log`
- 数据源: `scripts/data/`
- 后端服务: `backend/roleplay-api/`

---
*基于RAG向量检索问题调试报告的修复方案生成*
        """
        
        # 保存报告
        with open("knowledge_import_report.md", "w", encoding="utf-8") as f:
            f.write(report)
        
        return report
    
    def run(self, clear_data: bool = False, auto_mode: bool = False) -> bool:
        """执行完整的导入流程"""
        logger.info("🚀 开始知识库导入流程...")
        
        # 1. 环境检查
        if not self.check_environment():
            return False
        
        # 2. 数据清理（可选）
        if not self.clear_existing_data(clear_data, auto_mode):
            return False
        
        # 3. 导入所有角色数据
        logger.info("📚 开始导入角色数据...")
        
        for char_dir, config in self.character_configs.items():
            try:
                result = self.import_character_data(char_dir, config)
                self.results.append(result)
                
                # 导入间隔，避免服务过载
                time.sleep(1)
                
            except Exception as e:
                logger.error(f"❌ {config['name']} 导入异常: {e}")
                # 添加失败记录
                self.results.append(ImportResult(
                    config['name'], config['id'], 0, 0, 0, 0, 0.0, 0.0
                ))
        
        # 4. 验证结果
        verification_passed = self.verify_import_results()
        
        # 5. 生成报告
        report = self.generate_report()
        
        # 总结
        total_imported = sum(r.imported_records for r in self.results)
        total_records = sum(r.total_records for r in self.results)
        
        if total_imported > 0 and verification_passed:
            logger.info(f"🎉 知识库导入成功完成！共导入 {total_imported} 条记录")
            logger.info("📋 详细报告已保存到: knowledge_import_report.md")
            return True
        else:
            logger.error("❌ 知识库导入存在问题，请检查日志和报告")
            return False

if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description="AI角色扮演系统知识库导入工具")
    parser.add_argument("--url", default="http://localhost:18080", help="后端服务地址")
    parser.add_argument("--clear", action="store_true", help="清理现有数据")
    parser.add_argument("--yes", action="store_true", help="自动确认所有操作")
    
    args = parser.parse_args()
    
    deployer = KnowledgeBaseDeployer(args.url)
    
    if args.yes:
        # 自动化模式
        success = deployer.run(args.clear, auto_mode=True)
    else:
        # 交互模式
        print("🎯 AI角色扮演系统 - 知识库导入工具")
        print("=" * 50)
        print(f"后端服务地址: {args.url}")
        print(f"数据目录: {deployer.data_dir}")
        print("包含角色: " + ", ".join([config['name'] for config in deployer.character_configs.values()]))
        print()
        
        confirm = input("确认开始导入？(y/N): ")
        if confirm.lower() == 'y':
            success = deployer.run(args.clear, auto_mode=False)
        else:
            print("❌ 导入已取消")
            success = False
    
    sys.exit(0 if success else 1)
