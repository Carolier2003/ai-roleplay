#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试BeautifulSoup解析
"""

import requests
from bs4 import BeautifulSoup

def test_soup_parsing():
    """测试BeautifulSoup解析"""
    url = "https://harrypotter.fandom.com/zh/wiki/%E5%88%86%E9%99%A2%E5%B8%BD"
    
    headers = {
        'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
    }
    
    response = requests.get(url, headers=headers, timeout=10)
    response.encoding = 'utf-8'
    
    soup = BeautifulSoup(response.text, 'html.parser')
    
    print("🔍 BeautifulSoup解析测试")
    print("=" * 40)
    
    # 测试段落提取
    paragraphs = soup.find_all('p')
    print(f"📝 总段落数: {len(paragraphs)}")
    
    valid_paragraphs = []
    for i, p in enumerate(paragraphs):
        text = p.get_text().strip()
        if len(text) > 20 and '分院帽' in text:
            valid_paragraphs.append((i, text))
            print(f"✅ 段落 {i}: {text[:100]}...")
            if len(valid_paragraphs) >= 3:  # 只显示前3个相关段落
                break
    
    print(f"\n📊 找到 {len(valid_paragraphs)} 个包含'分院帽'的有效段落")
    
    # 测试不同的内容选择器
    selectors_to_test = [
        ('div[id="mw-content-text"]', 'ID选择器'),
        ('div#mw-content-text', 'CSS选择器'),
        ('[id="mw-content-text"]', '属性选择器'),
    ]
    
    print(f"\n🔍 测试不同选择器:")
    for selector, desc in selectors_to_test:
        elements = soup.select(selector)
        if elements:
            elem = elements[0]
            inner_paragraphs = elem.find_all('p')
            print(f"  ✅ {desc}: 找到元素，包含 {len(inner_paragraphs)} 个段落")
        else:
            print(f"  ❌ {desc}: 未找到")
    
    # 手动查找包含分院帽内容的div
    print(f"\n🔍 查找包含内容的div:")
    all_divs = soup.find_all('div')
    content_divs = []
    
    for div in all_divs:
        text = div.get_text()
        if '分院帽' in text and 'Sorting Hat' in text and len(text) > 500:
            content_divs.append(div)
    
    print(f"📦 找到 {len(content_divs)} 个包含相关内容的div")
    
    if content_divs:
        for i, div in enumerate(content_divs[:2]):  # 只检查前2个
            div_id = div.get('id', '无ID')
            div_class = ' '.join(div.get('class', []))
            inner_ps = div.find_all('p')
            print(f"  Div {i+1}: ID='{div_id}', Class='{div_class}', 段落数={len(inner_ps)}")

if __name__ == "__main__":
    test_soup_parsing()
