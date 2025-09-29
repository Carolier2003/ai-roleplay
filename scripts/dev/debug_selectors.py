#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
调试选择器脚本
测试各种选择器的工作情况
"""

import requests
from bs4 import BeautifulSoup

def debug_selectors():
    """调试选择器"""
    url = "https://harrypotter.fandom.com/zh/wiki/%E5%88%86%E9%99%A2%E5%B8%BD"
    
    headers = {
        'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
    }
    
    response = requests.get(url, headers=headers, timeout=10)
    response.encoding = 'utf-8'
    
    soup = BeautifulSoup(response.text, 'html.parser')
    
    print("🔍 选择器调试")
    print("=" * 40)
    
    # 测试各种选择器方式
    selectors = [
        # 使用 select_one (CSS选择器)
        ('soup.select_one("div[id=\\"mw-content-text\\"]")', lambda: soup.select_one('div[id="mw-content-text"]')),
        ('soup.select_one("div#mw-content-text")', lambda: soup.select_one('div#mw-content-text')),
        ('soup.select_one("div.main-container")', lambda: soup.select_one('div.main-container')),
        
        # 使用 find (BeautifulSoup方式)
        ('soup.find("div", id="mw-content-text")', lambda: soup.find('div', id='mw-content-text')),
        ('soup.find("div", {"id": "mw-content-text"})', lambda: soup.find('div', {'id': 'mw-content-text'})),
        ('soup.find("div", class_="main-container")', lambda: soup.find('div', class_='main-container')),
        
        # 使用 find_all 然后取第一个
        ('soup.find_all("div", id="mw-content-text")[0]', lambda: soup.find_all('div', id='mw-content-text')[0] if soup.find_all('div', id='mw-content-text') else None),
    ]
    
    for desc, selector_func in selectors:
        try:
            result = selector_func()
            if result:
                paragraphs = result.find_all('p')
                valid_content = any('分院帽' in p.get_text() for p in paragraphs[:10])
                print(f"✅ {desc}: 找到元素，{len(paragraphs)}个段落，内容有效: {valid_content}")
                
                if valid_content:
                    # 显示第一个相关段落
                    for p in paragraphs:
                        text = p.get_text().strip()
                        if '分院帽' in text and len(text) > 50:
                            print(f"   📄 样例内容: {text[:100]}...")
                            break
            else:
                print(f"❌ {desc}: 未找到元素")
        except Exception as e:
            print(f"❌ {desc}: 异常 - {e}")
    
    # 查找所有有id的div
    print(f"\n🏷️  页面中所有有ID的div:")
    divs_with_id = soup.find_all('div', id=True)
    for div in divs_with_id[:10]:  # 只显示前10个
        div_id = div.get('id')
        print(f"   ID: '{div_id}'")

if __name__ == "__main__":
    debug_selectors()
