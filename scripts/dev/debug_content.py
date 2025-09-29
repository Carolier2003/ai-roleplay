#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
调试页面内容脚本
检查页面的原始HTML内容
"""

import requests
from bs4 import BeautifulSoup

def debug_page_content():
    """调试页面内容"""
    url = "https://harrypotter.fandom.com/zh/wiki/%E5%88%86%E9%99%A2%E5%B8%BD"
    
    headers = {
        'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
        'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
    }
    
    try:
        response = requests.get(url, headers=headers, timeout=10)
        response.encoding = 'utf-8'
        
        # 保存原始HTML用于调试
        with open('debug_page.html', 'w', encoding='utf-8') as f:
            f.write(response.text)
        
        print(f"✅ 原始HTML已保存到 debug_page.html")
        print(f"📄 内容长度: {len(response.text)} 字符")
        
        # 检查是否包含某些关键词
        content = response.text.lower()
        keywords = ['分院帽', 'sorting hat', '霍格沃茨', 'hogwarts', 'javascript', 'loading']
        
        print("\n🔍 关键词检查:")
        for keyword in keywords:
            if keyword in content:
                print(f"  ✅ 找到: {keyword}")
            else:
                print(f"  ❌ 未找到: {keyword}")
        
        # 检查页面结构
        soup = BeautifulSoup(response.text, 'html.parser')
        
        print(f"\n📊 页面结构:")
        print(f"  标题标签数量: {len(soup.find_all('title'))}")
        print(f"  H1标签数量: {len(soup.find_all('h1'))}")
        print(f"  段落标签数量: {len(soup.find_all('p'))}")
        print(f"  Div标签数量: {len(soup.find_all('div'))}")
        
        # 查找所有H1标签的内容
        h1_tags = soup.find_all('h1')
        print(f"\n📝 H1标签内容:")
        for i, h1 in enumerate(h1_tags):
            print(f"  H1-{i+1}: {h1.get_text().strip()}")
        
        # 查找所有有class属性的div
        divs = soup.find_all('div', class_=True)
        print(f"\n🏷️  前10个div的class属性:")
        for i, div in enumerate(divs[:10]):
            classes = ' '.join(div.get('class', []))
            print(f"  Div-{i+1}: {classes}")
        
        # 检查是否有特定的内容加载指示器
        loading_indicators = [
            'loading',
            'spinner',
            'skeleton',
            'placeholder',
            'lazy'
        ]
        
        print(f"\n⏳ 加载指示器检查:")
        for indicator in loading_indicators:
            elements = soup.find_all(True, {'class': lambda x: x and indicator in ' '.join(x)})
            if elements:
                print(f"  ⚠️  找到 {indicator} 相关元素: {len(elements)} 个")
        
        return True
        
    except Exception as e:
        print(f"❌ 调试失败: {e}")
        return False

if __name__ == "__main__":
    print("🔍 页面内容调试器")
    print("=" * 40)
    debug_page_content()
