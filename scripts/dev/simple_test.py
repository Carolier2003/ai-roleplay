#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
最简单的爬虫测试
直接复制成功的调试代码
"""

import requests
from bs4 import BeautifulSoup
import json
import os

def simple_crawl_test():
    """最简单的爬取测试"""
    url = "https://harrypotter.fandom.com/zh/wiki/%E5%88%86%E9%99%A2%E5%B8%BD"
    
    # 使用相同的请求头
    headers = {
        'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
    }
    
    try:
        print(f"🔍 爬取: {url}")
        response = requests.get(url, headers=headers, timeout=10)
        response.raise_for_status()
        response.encoding = 'utf-8'
        
        soup = BeautifulSoup(response.text, 'html.parser')
        
        # 提取标题
        title = soup.find('h1', {'class': 'page-header__title'}) or soup.find('h1')
        title_text = title.get_text().strip() if title else "未知标题"
        
        # 提取内容
        content_div = soup.find('div', id='mw-content-text')
        
        if content_div:
            print(f"✅ 找到内容区域")
            paragraphs = content_div.find_all('p')
            
            # 提取有效段落
            valid_paragraphs = []
            for p in paragraphs:
                text = p.get_text().strip()
                if len(text) > 20:
                    valid_paragraphs.append(text)
            
            if valid_paragraphs:
                # 合并所有段落
                content = '\n\n'.join(valid_paragraphs)
                
                # 创建数据
                data = {
                    "title": title_text,
                    "url": "/wiki/%E5%88%86%E9%99%A2%E5%B8%BD",
                    "content": content,
                    "category": "TEST_DATA",
                    "tags": ["分院帽", "霍格沃茨", "魔法"],
                    "importance": 9,
                    "source": "哈利·波特维基",
                    "source_url": url
                }
                
                # 保存数据
                os.makedirs("simple_test_data", exist_ok=True)
                with open("simple_test_data/sorting_hat.json", 'w', encoding='utf-8') as f:
                    json.dump(data, f, ensure_ascii=False, indent=2)
                
                print(f"✅ 成功爬取并保存数据")
                print(f"📝 标题: {title_text}")
                print(f"📄 段落数: {len(valid_paragraphs)}")
                print(f"📝 内容长度: {len(content)} 字符")
                print(f"📄 内容预览: {content[:100]}...")
                
                return True
            else:
                print(f"❌ 没有有效段落")
                return False
        else:
            print(f"❌ 未找到内容区域")
            return False
            
    except Exception as e:
        print(f"❌ 爬取失败: {e}")
        return False

if __name__ == "__main__":
    print("🧪 最简单的爬虫测试")
    print("=" * 40)
    if simple_crawl_test():
        print("\n🎉 测试成功！爬虫逻辑没有问题")
        print("问题可能在于复杂的爬虫框架中的某个环节")
    else:
        print("\n❌ 基础爬取也失败了")
