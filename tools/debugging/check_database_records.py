#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
检查数据库中泰拉瑞亚知识记录的实际情况
"""

import mysql.connector
from datetime import datetime

def check_terraria_records():
    try:
        # 数据库连接配置
        config = {
            'host': 'localhost',
            'user': 'roleplay',
            'password': 'roleplay123',
            'database': 'ai_roleplay',
            'charset': 'utf8mb4'
        }
        
        conn = mysql.connector.connect(**config)
        cursor = conn.cursor()
        
        print("🔍 查询数据库中泰拉瑞亚向导的知识记录...")
        
        # 查询总记录数
        cursor.execute("""
            SELECT COUNT(*) FROM character_knowledge 
            WHERE character_id = 5 AND status = 1 AND deleted = 0
        """)
        total_count = cursor.fetchone()[0]
        print(f"📊 总记录数: {total_count} 条")
        
        # 查询创建时间分布
        cursor.execute("""
            SELECT DATE(create_time) as date, COUNT(*) as count
            FROM character_knowledge 
            WHERE character_id = 5 AND status = 1 AND deleted = 0
            GROUP BY DATE(create_time)
            ORDER BY date DESC
        """)
        
        date_counts = cursor.fetchall()
        print(f"\n📅 按日期分布的导入记录:")
        for date, count in date_counts:
            print(f"   {date}: {count} 条记录")
        
        # 检查重复标题
        cursor.execute("""
            SELECT title, COUNT(*) as count
            FROM character_knowledge 
            WHERE character_id = 5 AND status = 1 AND deleted = 0
            GROUP BY title
            HAVING COUNT(*) > 1
            ORDER BY count DESC
            LIMIT 10
        """)
        
        duplicates = cursor.fetchall()
        if duplicates:
            print(f"\n⚠️  发现重复标题的记录:")
            for title, count in duplicates:
                print(f"   「{title}」: {count} 次")
        else:
            print(f"\n✅ 未发现重复标题的记录")
        
        # 查询最近的几条记录示例
        cursor.execute("""
            SELECT title, knowledge_type, LENGTH(content) as content_length, create_time
            FROM character_knowledge 
            WHERE character_id = 5 AND status = 1 AND deleted = 0
            ORDER BY create_time DESC
            LIMIT 5
        """)
        
        recent_records = cursor.fetchall()
        print(f"\n📋 最近导入的记录示例:")
        for title, k_type, length, create_time in recent_records:
            print(f"   📄 {title} ({k_type}, {length}字) - {create_time}")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"❌ 数据库查询失败: {e}")

if __name__ == "__main__":
    check_terraria_records()
