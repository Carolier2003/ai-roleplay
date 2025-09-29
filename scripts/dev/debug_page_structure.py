#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
调试页面结构脚本
检查哈利·波特维基的实际页面结构
"""

import requests
from bs4 import BeautifulSoup

def debug_page_structure():
    """调试页面结构"""
    print("🔍 调试哈利·波特维基页面结构...")
    
    try:
        url = "https://harrypotter.fandom.com/zh/wiki/%E5%88%86%E9%99%A2%E5%B8%BD"
        
        headers = {
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
        }
        
        response = requests.get(url, headers=headers, timeout=10)
        response.raise_for_status()
        
        soup = BeautifulSoup(response.content, 'html.parser')
        
        print(f"📄 页面标题: {soup.title.string if soup.title else 'N/A'}")
        
        # 查找可能的内容区域
        print("\n🔍 查找内容区域...")
        
        # 尝试不同的内容选择器
        content_selectors = [
            ('div.mw-content-text', 'MediaWiki内容区域'),
            ('div#content', 'ID为content的区域'),
            ('div.page-content', '页面内容区域'),
            ('main', '主要内容区域'),
            ('article', '文章区域'),
            ('div.WikiaPageContentWrapper', 'Wikia页面包装器'),
            ('div.page-header__title', '页面标题'),
            ('div.portable-infobox', '信息框'),
            ('div#mw-content-text', 'MW内容文本')
        ]
        
        for selector, description in content_selectors:
            elements = soup.select(selector)
            if elements:
                print(f"  ✅ 找到 {description}: {len(elements)} 个元素")
                # 显示第一个元素的前100个字符
                first_element = elements[0]
                text = first_element.get_text().strip()[:100]
                print(f"     预览: {text}...")
            else:
                print(f"  ❌ 未找到 {description}")
        
        # 查找所有段落
        paragraphs = soup.find_all('p')
        print(f"\n📝 找到 {len(paragraphs)} 个段落")
        
        if paragraphs:
            for i, p in enumerate(paragraphs[:5]):  # 只看前5个
                text = p.get_text().strip()
                if len(text) > 20:
                    print(f"  段落 {i+1}: {text[:80]}...")
        
        # 查找所有div class属性
        print(f"\n🏷️  页面中的主要div类名:")
        divs_with_class = soup.find_all('div', class_=True)
        class_names = set()
        for div in divs_with_class:
            for class_name in div.get('class', []):
                if 'content' in class_name.lower() or 'text' in class_name.lower() or 'body' in class_name.lower():
                    class_names.add(class_name)
        
        for class_name in sorted(class_names)[:10]:  # 只显示前10个
            print(f"  - {class_name}")
        
        return True
        
    except Exception as e:
        print(f"❌ 调试失败: {e}")
        return False

if __name__ == "__main__":
    debug_page_structure()
