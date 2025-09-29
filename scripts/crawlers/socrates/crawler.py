#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
苏格拉底维基百科知识爬虫
从中文维基百科爬取苏格拉底相关知识
"""

import requests
from bs4 import BeautifulSoup
import json
import os
import time
from typing import List, Dict
import re

class SocratesCrawler:
    """苏格拉底知识爬虫"""
    
    def __init__(self):
        self.headers = {
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
        }
        self.base_url = "https://zh.wikipedia.org"
        self.output_dir = "../../data/socrates"
        os.makedirs(self.output_dir, exist_ok=True)
        
        # 苏格拉底相关页面
        self.pages_to_crawl = [
            {
                "url": "/wiki/%E8%8B%8F%E6%A0%BC%E6%8B%89%E5%BA%95",
                "title": "苏格拉底",
                "category": "BIOGRAPHY",
                "importance": 10
            },
            {
                "url": "/wiki/%E8%8B%8F%E6%A0%BC%E6%8B%89%E5%BA%95%E6%95%99%E5%AD%A6%E6%B3%95",
                "title": "苏格拉底教学法",
                "category": "PHILOSOPHY",
                "importance": 9
            },
            {
                "url": "/wiki/%E5%8F%A4%E5%B8%8C%E8%85%8A%E5%93%B2%E5%AD%A6",
                "title": "古希腊哲学",
                "category": "PHILOSOPHY",
                "importance": 8
            },
            {
                "url": "/wiki/%E6%9F%8F%E6%8B%89%E5%9B%BE",
                "title": "柏拉图",
                "category": "STUDENT",
                "importance": 9
            },
            {
                "url": "/wiki/%E9%9B%85%E5%85%B8",
                "title": "雅典",
                "category": "LOCATION",
                "importance": 8
            },
            {
                "url": "/wiki/%E7%9F%A5%E8%AF%86%E8%AE%BA",
                "title": "知识论",
                "category": "PHILOSOPHY",
                "importance": 7
            },
            {
                "url": "/wiki/%E4%BC%A6%E7%90%86%E5%AD%A6",
                "title": "伦理学",
                "category": "PHILOSOPHY",
                "importance": 7
            },
            {
                "url": "/wiki/%E5%BE%B7%E6%80%A7%E4%BC%A6%E7%90%86%E5%AD%A6",
                "title": "德性伦理学",
                "category": "PHILOSOPHY",
                "importance": 8
            }
        ]
    
    def get_page_content(self, url: str) -> BeautifulSoup:
        """获取页面内容"""
        try:
            full_url = self.base_url + url
            print(f"🔍 正在获取: {full_url}")
            
            response = requests.get(full_url, headers=self.headers, timeout=10)
            response.raise_for_status()
            response.encoding = 'utf-8'
            
            soup = BeautifulSoup(response.text, 'html.parser')
            return soup
            
        except requests.RequestException as e:
            print(f"❌ 获取页面失败: {url}, 错误: {e}")
            return None
    
    def extract_page_data(self, soup: BeautifulSoup, page_info: Dict) -> Dict:
        """从页面提取数据"""
        if not soup:
            return None
            
        try:
            # 提取标题
            title = page_info["title"]
            h1 = soup.find('h1', {'class': 'firstHeading'})
            if h1:
                title = h1.get_text().strip()
            
            # 维基百科的主要内容区域
            content_div = None
            content_selectors = [
                'div#mw-content-text',
                'div.mw-parser-output', 
                'div#content',
                'div.mw-body-content'
            ]
            
            for selector in content_selectors:
                content_div = soup.select_one(selector)
                if content_div:
                    break
            
            if not content_div:
                print(f"❌ 未找到内容区域")
                return None
            
            # 清理和提取内容
            content = self.clean_wikipedia_content(content_div)
            
            if len(content) < 100:
                print(f"❌ 内容太短，可能提取失败")
                return None
            
            # 提取信息框数据
            infobox_data = self.extract_infobox(soup)
            
            # 提取分类信息
            categories = self.extract_categories(soup)
            
            return {
                "title": title,
                "content": content,
                "category": page_info["category"],
                "importance": page_info["importance"],
                "url": page_info["url"],
                "infobox": infobox_data,
                "categories": categories,
                "character_id": 2,  # 苏格拉底的角色ID
                "source": "中文维基百科"
            }
            
        except Exception as e:
            print(f"❌ 数据提取失败: {e}")
            return None
    
    def clean_wikipedia_content(self, content_div: BeautifulSoup) -> str:
        """清理维基百科内容"""
        # 移除不需要的元素
        for tag in content_div.find_all(['script', 'style', 'table.navbox', 'div.navbox', 
                                       'div.reflist', 'div.printfooter', 'div.catlinks']):
            tag.decompose()
        
        # 移除引用标记
        for tag in content_div.find_all('sup', class_='reference'):
            tag.decompose()
        
        # 移除图片说明等
        for tag in content_div.find_all('div', class_=['thumbcaption', 'magnify']):
            tag.decompose()
        
        # 提取段落文本
        paragraphs = []
        for p in content_div.find_all('p'):
            text = p.get_text().strip()
            if text and len(text) > 20:
                # 清理文本
                text = re.sub(r'\[\d+\]', '', text)  # 移除引用标记
                text = re.sub(r'\s+', ' ', text)     # 标准化空白符
                paragraphs.append(text)
        
        return '\n\n'.join(paragraphs)
    
    def extract_infobox(self, soup: BeautifulSoup) -> Dict:
        """提取信息框数据"""
        infobox = {}
        infobox_table = soup.find('table', class_='infobox')
        
        if infobox_table:
            for row in infobox_table.find_all('tr'):
                th = row.find('th')
                td = row.find('td')
                if th and td:
                    key = th.get_text().strip()
                    value = td.get_text().strip()
                    if key and value:
                        infobox[key] = value
        
        return infobox
    
    def extract_categories(self, soup: BeautifulSoup) -> List[str]:
        """提取页面分类"""
        categories = []
        catlinks = soup.find('div', id='catlinks')
        if catlinks:
            for link in catlinks.find_all('a'):
                if '/wiki/Category:' in link.get('href', ''):
                    categories.append(link.get_text().strip())
        
        return categories
    
    def make_safe_filename(self, filename: str) -> str:
        """创建安全的文件名"""
        # 移除或替换不安全的字符
        filename = re.sub(r'[<>:"/\\|?*]', '_', filename)
        filename = filename.replace(' ', '_')
        return filename[:100]  # 限制长度
    
    def save_data(self, data: Dict, filename: str):
        """保存数据到JSON文件"""
        filepath = os.path.join(self.output_dir, filename)
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print(f"💾 已保存: {filepath}")
    
    def crawl_all(self):
        """爬取所有页面"""
        print("🚀 开始爬取苏格拉底相关知识...")
        print(f"📊 总页面数: {len(self.pages_to_crawl)}")
        
        all_data = []
        success_count = 0
        
        for i, page_info in enumerate(self.pages_to_crawl, 1):
            print(f"\n📖 处理第 {i}/{len(self.pages_to_crawl)} 页: {page_info['title']}")
            
            try:
                # 获取页面内容
                soup = self.get_page_content(page_info["url"])
                if not soup:
                    continue
                
                # 提取数据
                data = self.extract_page_data(soup, page_info)
                if not data:
                    continue
                
                # 保存单个文件
                safe_filename = self.make_safe_filename(page_info["title"])
                self.save_data(data, f"{safe_filename}.json")
                
                all_data.append(data)
                success_count += 1
                
                print(f"✅ 成功爬取: {page_info['title']}")
                print(f"📝 内容长度: {len(data['content'])} 字符")
                
                # 延迟避免过于频繁的请求
                time.sleep(1)
                
            except Exception as e:
                print(f"❌ 处理失败: {page_info['title']}, 错误: {e}")
                continue
        
        # 保存合并数据
        if all_data:
            self.save_data(all_data, "socrates_knowledge_base.json")
            
            # 生成统计报告
            self.generate_report(all_data, success_count)
    
    def generate_report(self, all_data: List[Dict], success_count: int):
        """生成爬取报告"""
        total_pages = len(self.pages_to_crawl)
        total_content = sum(len(data['content']) for data in all_data)
        
        report = {
            "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
            "character": "苏格拉底",
            "total_pages": total_pages,
            "successful_pages": success_count,
            "success_rate": f"{success_count/total_pages*100:.1f}%",
            "total_content_length": total_content,
            "avg_content_length": int(total_content/success_count) if success_count > 0 else 0,
            "categories": list(set(data['category'] for data in all_data)),
            "pages": [{"title": data['title'], "length": len(data['content'])} for data in all_data]
        }
        
        self.save_data(report, "crawl_report.json")
        
        print(f"\n🎉 爬取完成！")
        print(f"📊 统计信息:")
        print(f"   - 总页面数: {total_pages}")
        print(f"   - 成功爬取: {success_count}")
        print(f"   - 成功率: {report['success_rate']}")
        print(f"   - 总内容: {total_content:,} 字符")
        print(f"   - 平均长度: {report['avg_content_length']:,} 字符")

if __name__ == "__main__":
    crawler = SocratesCrawler()
    crawler.crawl_all()