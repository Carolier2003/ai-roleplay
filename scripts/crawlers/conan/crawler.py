#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
江户川柯南（名侦探柯南）知识爬虫
从柯南百科爬取相关知识
"""

import requests
from bs4 import BeautifulSoup
import json
import os
import time
from typing import List, Dict
import re
import urllib.parse

class ConanCrawler:
    """柯南知识爬虫"""
    
    def __init__(self):
        self.headers = {
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
        }
        self.base_url = "https://www.conanpedia.com"
        self.output_dir = "../../data/conan"
        os.makedirs(self.output_dir, exist_ok=True)
        
        # ⚠️ 重要：确保角色ID正确设置为4（江户川柯南）
        self.character_id = 4
        
        # 柯南相关页面
        self.pages_to_crawl = [
            {
                "url": "/江户川柯南",
                "title": "江户川柯南",
                "category": "MAIN_CHARACTER",
                "importance": 10
            },
            {
                "url": "/工藤新一",
                "title": "工藤新一", 
                "category": "MAIN_CHARACTER",
                "importance": 10
            },
            {
                "url": "/毛利兰",
                "title": "毛利兰",
                "category": "MAIN_CHARACTER", 
                "importance": 9
            },
            {
                "url": "/毛利小五郎",
                "title": "毛利小五郎",
                "category": "MAIN_CHARACTER",
                "importance": 8
            },
            {
                "url": "/阿笠博士",
                "title": "阿笠博士",
                "category": "SUPPORTING_CHARACTER",
                "importance": 8
            },
            {
                "url": "/黑暗组织",
                "title": "黑暗组织",
                "category": "ORGANIZATION",
                "importance": 9
            },
            {
                "url": "/少年侦探团",
                "title": "少年侦探团",
                "category": "ORGANIZATION",
                "importance": 7
            },
            {
                "url": "/APTX4869",
                "title": "APTX4869",
                "category": "ITEM",
                "importance": 9
            },
            {
                "url": "/帝丹小学",
                "title": "帝丹小学",
                "category": "LOCATION",
                "importance": 6
            },
            {
                "url": "/帝丹高中",
                "title": "帝丹高中", 
                "category": "LOCATION",
                "importance": 6
            },
            {
                "url": "/服部平次",
                "title": "服部平次",
                "category": "DETECTIVE",
                "importance": 7
            },
            {
                "url": "/怪盗基德",
                "title": "怪盗基德",
                "category": "THIEF",
                "importance": 8
            },
            {
                "url": "/步美",
                "title": "吉田步美",
                "category": "DETECTIVE_BOYS",
                "importance": 6
            },
            {
                "url": "/光彦",
                "title": "圆谷光彦",
                "category": "DETECTIVE_BOYS", 
                "importance": 6
            },
            {
                "url": "/元太",
                "title": "小嶋元太",
                "category": "DETECTIVE_BOYS",
                "importance": 6
            },
            {
                "url": "/灰原哀",
                "title": "灰原哀",
                "category": "MAIN_CHARACTER",
                "importance": 9
            },
            {
                "url": "/宫野志保",
                "title": "宫野志保",
                "category": "MAIN_CHARACTER",
                "importance": 8
            },
            {
                "url": "/FBI",
                "title": "FBI",
                "category": "ORGANIZATION",
                "importance": 7
            }
        ]
        
    def fetch_page(self, url: str) -> str:
        """获取页面内容"""
        try:
            time.sleep(1)  # 避免请求过快
            full_url = self.base_url + url
            print(f"正在获取: {full_url}")
            
            response = requests.get(full_url, headers=self.headers, timeout=10)
            response.raise_for_status()
            
            # 处理编码
            response.encoding = 'utf-8'
            return response.text
            
        except Exception as e:
            print(f"❌ 获取页面失败 {url}: {e}")
            return ""
    
    def extract_page_data(self, html: str, title: str, category: str, importance: int) -> Dict:
        """提取页面数据"""
        if not html:
            return None
            
        try:
            soup = BeautifulSoup(html, 'html.parser')
            
            # 根据调试结果，使用有效的选择器
            content_selectors = [
                'div#mw-content-text',  # 这个包含最多内容
                'div.mw-content-ltr',
                'div#content',
                'div.mw-parser-output'
            ]
            
            content_div = None
            for selector in content_selectors:
                content_div = soup.select_one(selector)
                if content_div:
                    text_length = len(content_div.get_text().strip())
                    print(f"✅ 使用选择器: {selector} (包含 {text_length} 字符)")
                    if text_length > 100:  # 确保有足够内容
                        break
            
            if not content_div:
                print("⚠️ 未找到内容容器，尝试直接提取段落")
                paragraphs = soup.find_all('p')
            else:
                paragraphs = content_div.find_all('p')
            
            # 提取文本内容 - 降低长度要求
            content_parts = []
            for p in paragraphs:
                text = p.get_text().strip()
                if text and len(text) > 10:  # 降低要求从20到10
                    # 清理文本
                    text = re.sub(r'\[\d+\]', '', text)  # 移除引用标记
                    text = re.sub(r'\s+', ' ', text)  # 标准化空白字符
                    text = re.sub(r'^[。，、；：？！]+', '', text)  # 移除开头的标点
                    if text:  # 确保清理后仍有内容
                        content_parts.append(text)
            
            # 如果段落不够，尝试其他元素
            if len(content_parts) < 3:
                print("⚠️ 段落较少，尝试提取其他文本元素")
                # 尝试提取表格、列表等其他内容
                other_elements = content_div.find_all(['div', 'li', 'td', 'th']) if content_div else []
                for elem in other_elements:
                    text = elem.get_text().strip()
                    if text and len(text) > 15 and text not in [part for part in content_parts]:
                        # 避免重复内容
                        content_parts.append(text)
                        if len(content_parts) >= 10:  # 足够了就停止
                            break
            
            if not content_parts:
                print(f"⚠️ 页面 {title} 没有提取到有效内容")
                return None
                
            content = '\n\n'.join(content_parts[:15])  # 增加到15段
            
            if len(content) < 30:  # 降低最小要求
                print(f"⚠️ 提取内容过短: {len(content)} 字符")
                print(f"内容预览: {content[:100]}")
                return None
            
            # ⚠️ 关键：确保character_id正确设置
            knowledge_item = {
                "character_id": self.character_id,  # 确保是4，不是默认的1
                "title": title,
                "content": content[:2000],  # 限制长度避免token超限
                "knowledge_type": category.lower(),
                "importance_score": importance,
                "source": "conanpedia",
                "source_url": self.base_url + "/" + urllib.parse.quote(title),
                "language": "zh",
                "status": 1,
                "tags": [category, "detective", "conan", "anime"]
            }
            
            print(f"✅ 成功提取: {title} ({len(content)} 字符)")
            return knowledge_item
            
        except Exception as e:
            print(f"❌ 解析失败 {title}: {e}")
            return None
    
    def crawl_all(self):
        """爬取所有页面"""
        print(f"🚀 开始爬取柯南知识库，角色ID: {self.character_id}")
        print(f"📋 计划爬取 {len(self.pages_to_crawl)} 个页面")
        
        knowledge_list = []
        success_count = 0
        
        for i, page_info in enumerate(self.pages_to_crawl, 1):
            print(f"\n📄 [{i}/{len(self.pages_to_crawl)}] 处理: {page_info['title']}")
            
            html = self.fetch_page(page_info['url'])
            if html:
                knowledge_item = self.extract_page_data(
                    html, 
                    page_info['title'], 
                    page_info['category'], 
                    page_info['importance']
                )
                
                if knowledge_item:
                    knowledge_list.append(knowledge_item)
                    success_count += 1
                    
                    # 保存单个文件
                    safe_filename = re.sub(r'[^\w\s-]', '', page_info['title']).strip()
                    output_file = os.path.join(self.output_dir, f"{safe_filename}.json")
                    with open(output_file, 'w', encoding='utf-8') as f:
                        json.dump(knowledge_item, f, ensure_ascii=False, indent=2)
        
        # 保存汇总文件
        if knowledge_list:
            output_file = os.path.join(self.output_dir, "conan_knowledge.json")
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(knowledge_list, f, ensure_ascii=False, indent=2)
            
            print(f"\n🎉 爬取完成！")
            print(f"✅ 成功: {success_count}/{len(self.pages_to_crawl)} 个页面")
            print(f"📁 输出文件: {output_file}")
            print(f"📊 总数据量: {len(json.dumps(knowledge_list, ensure_ascii=False))} 字符")
            print(f"⚠️ 角色ID确认: {self.character_id} (江户川柯南)")
        else:
            print("❌ 没有成功爬取任何内容")

if __name__ == "__main__":
    crawler = ConanCrawler()
    crawler.crawl_all()
