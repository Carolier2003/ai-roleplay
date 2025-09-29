#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
AI角色扮演系统 - 知识库清空工具

功能:
1. 清空MySQL中的知识库数据
2. 清空Redis中的向量数据
3. 支持全部清空或按角色清空
4. 提供安全确认机制

Author: Assistant
Date: 2025-09-25
"""

import os
import sys
import json
import time
import logging
import argparse
import requests
from typing import Dict, List, Any, Optional
from pathlib import Path

# 添加项目根目录到Python路径
project_root = Path(__file__).parent.parent.parent
sys.path.insert(0, str(project_root))

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler('logs/clear_knowledge.log', encoding='utf-8')
    ]
)
logger = logging.getLogger(__name__)

class KnowledgeBaseCleaner:
    """知识库清空工具"""
    
    def __init__(self, base_url: str = "http://localhost:18080"):
        self.base_url = base_url
        self.mysql_config = {
            'host': 'localhost',
            'port': 3306,
            'user': 'root',
            'password': 'roleplay123',
            'database': 'ai_roleplay'
        }
        self.redis_config = {
            'host': 'localhost',
            'port': 6379,
            'db': 0
        }
        
        # 角色配置
        self.characters = {
            1: "哈利·波特",
            2: "苏格拉底", 
            3: "爱因斯坦",
            4: "江户川柯南",
            5: "泰拉瑞亚向导"
        }
    
    def check_environment(self) -> bool:
        """检查运行环境"""
        logger.info("🔍 检查运行环境...")
        
        # 检查必要的依赖
        try:
            import mysql.connector
            import redis
            logger.info("✅ Python依赖已满足")
        except ImportError as e:
            logger.error(f"❌ 缺少必要依赖: {e}")
            logger.info("💡 请运行: pip install mysql-connector-python redis")
            return False
        
        # 检查MySQL连接
        try:
            import mysql.connector
            conn = mysql.connector.connect(**self.mysql_config)
            conn.close()
            logger.info("✅ MySQL连接正常")
        except Exception as e:
            logger.error(f"❌ MySQL连接失败: {e}")
            return False
        
        # 检查Redis连接
        try:
            import redis
            r = redis.Redis(**self.redis_config)
            r.ping()
            logger.info("✅ Redis连接正常")
        except Exception as e:
            logger.error(f"❌ Redis连接失败: {e}")
            return False
        
        # 检查后端服务（可选）
        try:
            response = requests.get(f"{self.base_url}/api/health", timeout=5)
            if response.status_code == 200:
                logger.info("✅ 后端服务运行正常")
            else:
                logger.warning("⚠️ 后端服务异常，但可以继续清空操作")
        except Exception:
            logger.warning("⚠️ 无法连接后端服务，但可以继续清空操作")
        
        return True
    
    def get_knowledge_stats(self, character_id: Optional[int] = None) -> Dict[str, Any]:
        """获取知识库统计信息"""
        try:
            import mysql.connector
            conn = mysql.connector.connect(**self.mysql_config)
            cursor = conn.cursor()
            
            if character_id:
                # 单个角色统计
                cursor.execute("""
                    SELECT COUNT(*) as total, 
                           SUM(CASE WHEN deleted = 0 THEN 1 ELSE 0 END) as active,
                           SUM(CASE WHEN deleted = 1 THEN 1 ELSE 0 END) as deleted,
                           SUM(CASE WHEN vector_id IS NOT NULL AND deleted = 0 THEN 1 ELSE 0 END) as vectorized
                    FROM character_knowledge 
                    WHERE character_id = %s
                """, (character_id,))
                result = cursor.fetchone()
                stats = {
                    'character_id': character_id,
                    'character_name': self.characters.get(character_id, f"角色{character_id}"),
                    'total': result[0] or 0,
                    'active': result[1] or 0,
                    'deleted': result[2] or 0,
                    'vectorized': result[3] or 0
                }
            else:
                # 全部统计
                cursor.execute("""
                    SELECT character_id,
                           COUNT(*) as total,
                           SUM(CASE WHEN deleted = 0 THEN 1 ELSE 0 END) as active,
                           SUM(CASE WHEN deleted = 1 THEN 1 ELSE 0 END) as deleted,
                           SUM(CASE WHEN vector_id IS NOT NULL AND deleted = 0 THEN 1 ELSE 0 END) as vectorized
                    FROM character_knowledge 
                    GROUP BY character_id
                """)
                results = cursor.fetchall()
                stats = {
                    'characters': [],
                    'total_all': 0,
                    'active_all': 0,
                    'deleted_all': 0,
                    'vectorized_all': 0
                }
                
                for row in results:
                    char_stats = {
                        'character_id': row[0],
                        'character_name': self.characters.get(row[0], f"角色{row[0]}"),
                        'total': row[1] or 0,
                        'active': row[2] or 0,
                        'deleted': row[3] or 0,
                        'vectorized': row[4] or 0
                    }
                    stats['characters'].append(char_stats)
                    stats['total_all'] += char_stats['total']
                    stats['active_all'] += char_stats['active']
                    stats['deleted_all'] += char_stats['deleted']
                    stats['vectorized_all'] += char_stats['vectorized']
            
            cursor.close()
            conn.close()
            return stats
            
        except Exception as e:
            logger.error(f"获取统计信息失败: {e}")
            return {}
    
    def clear_mysql_data(self, character_id: Optional[int] = None) -> bool:
        """清空MySQL知识库数据"""
        try:
            import mysql.connector
            conn = mysql.connector.connect(**self.mysql_config)
            cursor = conn.cursor()
            
            if character_id:
                logger.info(f"🗑️ 清空MySQL中角色 {self.characters.get(character_id, character_id)} 的知识库数据...")
                cursor.execute("DELETE FROM character_knowledge WHERE character_id = %s", (character_id,))
            else:
                logger.info("🗑️ 清空MySQL中所有知识库数据...")
                cursor.execute("DELETE FROM character_knowledge")
            
            deleted_count = cursor.rowcount
            conn.commit()
            cursor.close()
            conn.close()
            
            logger.info(f"✅ MySQL清空完成，删除了 {deleted_count} 条记录")
            return True
            
        except Exception as e:
            logger.error(f"❌ MySQL清空失败: {e}")
            return False
    
    def clear_redis_data(self, character_id: Optional[int] = None) -> bool:
        """清空Redis向量数据"""
        try:
            import redis
            r = redis.Redis(**self.redis_config)
            
            if character_id:
                logger.info(f"🗑️ 清空Redis中角色 {self.characters.get(character_id, character_id)} 的向量数据...")
                # 由于Redis中的向量数据通常以特定前缀存储，这里使用通配符删除
                pattern = f"*character_{character_id}_*"
                keys = r.keys(pattern)
                if keys:
                    deleted_count = r.delete(*keys)
                    logger.info(f"✅ Redis清空完成，删除了 {deleted_count} 个键")
                else:
                    logger.info("ℹ️ Redis中没有找到相关的向量数据")
            else:
                logger.info("🗑️ 清空Redis中所有向量数据...")
                # 清空整个数据库（谨慎操作）
                r.flushdb()
                logger.info("✅ Redis清空完成，已清空整个数据库")
            
            return True
            
        except Exception as e:
            logger.error(f"❌ Redis清空失败: {e}")
            return False
    
    def clear_via_api(self, character_id: Optional[int] = None) -> bool:
        """通过API清空知识库（如果后端支持）"""
        try:
            if character_id:
                url = f"{self.base_url}/api/knowledge/clear/{character_id}"
            else:
                url = f"{self.base_url}/api/knowledge/clear"
            
            response = requests.post(url, timeout=30)
            if response.status_code == 200:
                logger.info("✅ 通过API清空成功")
                return True
            elif response.status_code == 404:
                logger.info("ℹ️ 后端不支持API清空功能")
                return False
            else:
                logger.warning(f"⚠️ API清空响应异常: {response.status_code}")
                return False
                
        except Exception as e:
            logger.info(f"ℹ️ API清空不可用: {e}")
            return False
    
    def confirm_operation(self, operation_desc: str) -> bool:
        """确认操作"""
        print(f"\n⚠️ 危险操作确认:")
        print(f"   {operation_desc}")
        print(f"   这个操作是不可逆的！")
        
        while True:
            confirm = input("\n是否确认执行？请输入 'YES' 确认，或 'no' 取消: ").strip()
            if confirm == 'YES':
                return True
            elif confirm.lower() in ['no', 'n', '']:
                return False
            else:
                print("❌ 请输入 'YES' 确认或 'no' 取消")
    
    def clear_knowledge_base(self, character_id: Optional[int] = None, force: bool = False) -> bool:
        """清空知识库"""
        logger.info("🚀 开始知识库清空操作...")
        
        # 获取当前统计信息
        stats = self.get_knowledge_stats(character_id)
        if not stats:
            logger.error("❌ 无法获取知识库统计信息")
            return False
        
        # 显示当前状态
        if character_id:
            char_name = self.characters.get(character_id, f"角色{character_id}")
            print(f"\n📊 当前状态 - {char_name}:")
            print(f"   总记录数: {stats.get('total', 0)}")
            print(f"   活跃记录: {stats.get('active', 0)}")
            print(f"   已删除记录: {stats.get('deleted', 0)}")
            print(f"   已向量化: {stats.get('vectorized', 0)}")
            operation_desc = f"清空角色 {char_name} 的所有知识库数据"
        else:
            print(f"\n📊 当前状态 - 全部角色:")
            for char_stats in stats.get('characters', []):
                print(f"   {char_stats['character_name']}: {char_stats['active']} 条活跃记录")
            print(f"   总计: {stats.get('active_all', 0)} 条活跃记录")
            operation_desc = "清空所有角色的知识库数据"
        
        # 确认操作
        if not force and not self.confirm_operation(operation_desc):
            logger.info("❌ 操作已取消")
            return False
        
        logger.info(f"🗑️ 开始执行清空操作...")
        success = True
        
        # 尝试通过API清空
        if self.clear_via_api(character_id):
            logger.info("✅ 通过API清空成功")
        else:
            # API不可用，直接操作数据库
            logger.info("ℹ️ API不可用，直接操作数据库...")
            
            # 清空MySQL
            if not self.clear_mysql_data(character_id):
                success = False
            
            # 清空Redis
            if not self.clear_redis_data(character_id):
                success = False
        
        # 验证清空结果
        logger.info("🔍 验证清空结果...")
        time.sleep(1)  # 等待操作完成
        
        final_stats = self.get_knowledge_stats(character_id)
        if character_id:
            remaining = final_stats.get('active', 0)
            if remaining == 0:
                logger.info(f"✅ 清空验证成功：角色 {self.characters.get(character_id)} 的知识库已清空")
            else:
                logger.warning(f"⚠️ 清空不完整：还剩余 {remaining} 条记录")
                success = False
        else:
            remaining = final_stats.get('active_all', 0)
            if remaining == 0:
                logger.info(f"✅ 清空验证成功：所有角色的知识库已清空")
            else:
                logger.warning(f"⚠️ 清空不完整：还剩余 {remaining} 条记录")
                success = False
        
        return success

def main():
    parser = argparse.ArgumentParser(description="AI角色扮演系统知识库清空工具")
    parser.add_argument("--url", default="http://localhost:18080", help="后端服务地址")
    parser.add_argument("--character", type=int, help="指定要清空的角色ID (1-5)")
    parser.add_argument("--force", action="store_true", help="强制执行，跳过确认")
    parser.add_argument("--stats", action="store_true", help="只显示统计信息，不执行清空")
    
    args = parser.parse_args()
    
    cleaner = KnowledgeBaseCleaner(args.url)
    
    # 检查环境
    if not cleaner.check_environment():
        logger.error("❌ 环境检查失败")
        sys.exit(1)
    
    # 如果只是查看统计信息
    if args.stats:
        stats = cleaner.get_knowledge_stats(args.character)
        if args.character:
            char_name = cleaner.characters.get(args.character, f"角色{args.character}")
            print(f"\n📊 {char_name} 知识库统计:")
            print(f"   总记录数: {stats.get('total', 0)}")
            print(f"   活跃记录: {stats.get('active', 0)}")
            print(f"   已删除记录: {stats.get('deleted', 0)}")
            print(f"   已向量化: {stats.get('vectorized', 0)}")
        else:
            print(f"\n📊 全部知识库统计:")
            for char_stats in stats.get('characters', []):
                print(f"   {char_stats['character_name']}: {char_stats['active']} 条活跃记录")
            print(f"   总计: {stats.get('active_all', 0)} 条活跃记录")
        sys.exit(0)
    
    # 执行清空操作
    success = cleaner.clear_knowledge_base(args.character, args.force)
    
    if success:
        logger.info("🎉 知识库清空成功！")
        sys.exit(0)
    else:
        logger.error("❌ 知识库清空失败！")
        sys.exit(1)

if __name__ == "__main__":
    main()
