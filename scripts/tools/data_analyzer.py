"""
数据分析工具
分析爬取的知识库数据
"""

import json
from pathlib import Path

def analyze_knowledge_base(character_name: str):
    """分析角色知识库"""
    data_path = Path(f"../data/{character_name}/knowledge_base.json")
    
    if not data_path.exists():
        print(f"❌ 未找到 {character_name} 的知识库文件")
        return
    
    with open(data_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    print(f"📊 {character_name} 知识库分析:")
    print(f"  总条目数: {len(data)}")
    
    # 分析内容统计
    total_chars = sum(len(item['content']) for item in data)
    print(f"  总字符数: {total_chars:,}")
    
    # 分类统计
    categories = {}
    for item in data:
        cat = item.get('category', 'UNKNOWN')
        categories[cat] = categories.get(cat, 0) + 1
    
    print(f"  分类分布:")
    for cat, count in sorted(categories.items()):
        print(f"    {cat}: {count} 条")

if __name__ == "__main__":
    import sys
    if len(sys.argv) > 1:
        analyze_knowledge_base(sys.argv[1])
    else:
        print("用法: python data_analyzer.py <character_name>")
