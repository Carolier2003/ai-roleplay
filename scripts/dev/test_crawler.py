#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
爬虫测试脚本
快速测试爬虫的基本功能
"""

import requests
from bs4 import BeautifulSoup
import time

def test_wiki_access():
    """测试维基网站访问"""
    print("🔍 测试哈利·波特维基访问...")
    
    try:
        url = "https://harrypotter.fandom.com/zh/wiki/%E5%93%88%E5%88%A9%C2%B7%E6%B3%A2%E7%89%B9"
        
        headers = {
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
        }
        
        response = requests.get(url, headers=headers, timeout=10)
        response.raise_for_status()
        
        soup = BeautifulSoup(response.content, 'html.parser')
        title = soup.find('h1', {'class': 'page-header__title'})
        
        if title:
            print(f"✅ 访问成功！页面标题: {title.get_text().strip()}")
            return True
        else:
            print("⚠️ 页面访问成功，但标题解析失败")
            return False
            
    except Exception as e:
        print(f"❌ 访问失败: {e}")
        return False

def test_content_extraction():
    """测试内容提取"""
    print("\n📄 测试内容提取...")
    
    try:
        url = "https://harrypotter.fandom.com/zh/wiki/%E5%88%86%E9%99%A2%E5%B8%BD"
        
        headers = {
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
        }
        
        response = requests.get(url, headers=headers, timeout=10)
        response.raise_for_status()
        
        soup = BeautifulSoup(response.content, 'html.parser')
        
        # 提取标题
        title = soup.find('h1', {'class': 'page-header__title'}) or soup.find('h1')
        title_text = title.get_text().strip() if title else "未知标题"
        
        # 提取内容 (修正选择器)
        content_div = (soup.find('div', {'id': 'mw-content-text'}) or 
                      soup.find('div', {'class': 'page-content'}) or 
                      soup.find('div', {'id': 'content'}))
        if content_div:
            paragraphs = content_div.find_all('p')
            content_preview = ""
            for p in paragraphs[:3]:  # 只取前3段
                text = p.get_text().strip()
                if len(text) > 20:
                    content_preview += text + "\n\n"
            
            print(f"✅ 内容提取成功！")
            print(f"📝 标题: {title_text}")
            print(f"📄 内容预览 (前3段):")
            print("-" * 50)
            print(content_preview[:300] + "..." if len(content_preview) > 300 else content_preview)
            print("-" * 50)
            return True
        else:
            print("❌ 内容区域未找到")
            return False
            
    except Exception as e:
        print(f"❌ 内容提取失败: {e}")
        return False

def test_multiple_pages():
    """测试多个页面访问"""
    print("\n🔗 测试多个页面访问...")
    
    test_urls = [
        "/wiki/%E5%93%88%E5%88%A9%C2%B7%E6%B3%A2%E7%89%B9",  # 哈利·波特
        "/wiki/%E8%B5%AB%E6%95%8F%C2%B7%E6%A0%BC%E5%85%B0%E6%9D%B0",  # 赫敏·格兰杰
        "/wiki/%E9%9C%8D%E6%A0%BC%E6%B2%83%E8%8C%A8%E9%AD%94%E6%B3%95%E5%AD%A6%E6%A0%A1"  # 霍格沃茨魔法学校
    ]
    
    base_url = "https://harrypotter.fandom.com/zh"
    headers = {
        'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36'
    }
    
    success_count = 0
    
    for url_path in test_urls:
        try:
            full_url = base_url + url_path
            print(f"  🔍 测试: {url_path}")
            
            response = requests.get(full_url, headers=headers, timeout=10)
            response.raise_for_status()
            
            soup = BeautifulSoup(response.content, 'html.parser')
            title = soup.find('h1', {'class': 'page-header__title'}) or soup.find('h1')
            
            if title:
                title_text = title.get_text().strip()
                print(f"    ✅ 成功: {title_text}")
                success_count += 1
            else:
                print(f"    ⚠️ 标题解析失败")
            
            # 礼貌性延迟
            time.sleep(1)
            
        except Exception as e:
            print(f"    ❌ 失败: {e}")
    
    print(f"\n📊 多页面测试结果: {success_count}/{len(test_urls)} 成功")
    return success_count == len(test_urls)

def main():
    """主测试函数"""
    print("🧙‍♂️ 哈利·波特维基爬虫测试")
    print("=" * 50)
    
    all_passed = True
    
    # 测试1: 基础访问
    if not test_wiki_access():
        all_passed = False
    
    # 测试2: 内容提取
    if not test_content_extraction():
        all_passed = False
    
    # 测试3: 多页面访问
    if not test_multiple_pages():
        all_passed = False
    
    # 总结
    print("\n" + "=" * 50)
    if all_passed:
        print("🎉 所有测试通过！爬虫准备就绪")
        print("👉 运行命令: ./run_crawler.sh all")
    else:
        print("❌ 部分测试失败，请检查网络连接或等待重试")
    
    return all_passed

if __name__ == "__main__":
    main()
