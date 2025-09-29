#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
快速测试脚本
直接测试一个URL是否能正常爬取内容
"""

import requests
from bs4 import BeautifulSoup

def test_single_url():
    """测试单个URL"""
    # 使用之前测试成功的URL
    url = "https://harrypotter.fandom.com/zh/wiki/%E5%88%86%E9%99%A2%E5%B8%BD"
    
    headers = {
        'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
        'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
        'Accept-Encoding': 'gzip, deflate, br',
        'Connection': 'keep-alive',
    }
    
    try:
        print(f"🔍 测试URL: {url}")
        response = requests.get(url, headers=headers, timeout=10)
        print(f"📊 状态码: {response.status_code}")
        print(f"📄 内容长度: {len(response.content)}")
        print(f"🔣 编码: {response.encoding}")
        
        # 强制设置UTF-8编码
        response.encoding = 'utf-8'
        
        soup = BeautifulSoup(response.content, 'html.parser')
        
        # 测试标题提取
        title = soup.find('h1', {'class': 'page-header__title'}) or soup.find('h1')
        print(f"📝 标题: {title.get_text().strip() if title else '未找到'}")
        
        # 测试内容区域
        selectors = [
            ('div#mw-content-text', 'MW内容文本'),
            ('div.page-content', '页面内容'),
            ('div#content', '内容区域'),
            ('div.mw-body-content', 'MW主体内容'),
        ]
        
        for selector, desc in selectors:
            elem = soup.select_one(selector)
            if elem:
                print(f"✅ 找到 {desc}: {len(elem.get_text())} 字符")
            else:
                print(f"❌ 未找到 {desc}")
        
        # 测试段落提取
        paragraphs = soup.find_all('p')
        print(f"📝 段落数量: {len(paragraphs)}")
        
        valid_paragraphs = []
        for p in paragraphs:
            text = p.get_text().strip()
            if len(text) > 20:
                valid_paragraphs.append(text)
        
        print(f"📝 有效段落: {len(valid_paragraphs)}")
        
        if valid_paragraphs:
            print(f"📄 第一个段落: {valid_paragraphs[0][:100]}...")
            return True
        else:
            print("❌ 没有找到有效段落")
            return False
        
    except Exception as e:
        print(f"❌ 测试失败: {e}")
        return False

if __name__ == "__main__":
    print("🧪 快速URL测试")
    print("=" * 40)
    test_single_url()
