#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
爱因斯坦维基百科爬虫
爬取爱因斯坦及其相关科学概念的知识
"""

import requests
from bs4 import BeautifulSoup
import json
import os
import time
from typing import List, Dict, Any
import logging
import urllib.parse

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class EinsteinCrawler:
    """爱因斯坦维基百科爬虫"""
    
    def __init__(self):
        self.headers = {
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
        }
        self.base_url = "https://zh.wikipedia.org"
        self.output_dir = "../../data/einstein"  # 统一数据存储位置
        os.makedirs(self.output_dir, exist_ok=True)
        self.character_name = "爱因斯坦"

        # 要爬取的页面列表 - 基于爱因斯坦的科学贡献和相关概念
        self.pages_to_crawl = [
            # 核心传记
            {"url": "/wiki/%E9%98%BF%E5%B0%94%E4%BC%AF%E7%89%B9%C2%B7%E7%88%B1%E5%9B%A0%E6%96%AF%E5%9D%A6", "title": "阿尔伯特·爱因斯坦", "category": "BIOGRAPHY", "importance": 10},
            
            # 相对论理论
            {"url": "/wiki/%E7%9B%B8%E5%AF%B9%E8%AE%BA", "title": "相对论", "category": "PHYSICS_THEORY", "importance": 10},
            {"url": "/wiki/%E7%8B%AD%E4%B9%89%E7%9B%B8%E5%AF%B9%E8%AE%BA", "title": "狭义相对论", "category": "PHYSICS_THEORY", "importance": 9},
            {"url": "/wiki/%E5%B9%BF%E4%B9%89%E7%9B%B8%E5%AF%B9%E8%AE%BA", "title": "广义相对论", "category": "PHYSICS_THEORY", "importance": 9},
            
            # 质能方程和重要发现
            {"url": "/wiki/%E8%B4%A8%E8%83%BD%E6%96%B9%E7%A8%8B", "title": "质能方程", "category": "PHYSICS_FORMULA", "importance": 9},
            {"url": "/wiki/%E5%85%89%E7%94%B5%E6%95%88%E5%BA%94", "title": "光电效应", "category": "PHYSICS_PHENOMENON", "importance": 8},
            {"url": "/wiki/%E5%B8%83%E6%9C%97%E8%BF%90%E5%8A%A8", "title": "布朗运动", "category": "PHYSICS_PHENOMENON", "importance": 7},
            
            # 量子力学相关
            {"url": "/wiki/%E9%87%8F%E5%AD%90%E5%8A%9B%E5%AD%A6", "title": "量子力学", "category": "PHYSICS_THEORY", "importance": 8},
            {"url": "/wiki/%E5%85%89%E5%AD%90", "title": "光子", "category": "PHYSICS_CONCEPT", "importance": 7},
            {"url": "/wiki/%E6%B3%A2%E7%B2%92%E4%BA%8C%E8%B1%A1%E6%80%A7", "title": "波粒二象性", "category": "PHYSICS_CONCEPT", "importance": 7},
            
            # 科学机构和历史背景
            {"url": "/wiki/%E6%99%AE%E6%9E%97%E6%96%AF%E9%A1%BF%E9%AB%98%E7%AD%89%E7%A0%94%E7%A9%B6%E9%99%A2", "title": "普林斯顿高等研究院", "category": "INSTITUTION", "importance": 6},
            {"url": "/wiki/%E7%91%9E%E5%A3%AB%E8%81%94%E9%82%A6%E7%90%86%E5%B7%A5%E5%AD%A6%E9%99%A2", "title": "瑞士联邦理工学院", "category": "INSTITUTION", "importance": 6},
            
            # 诺贝尔奖和荣誉
            {"url": "/wiki/1921%E5%B9%B4%E8%AF%BA%E8%B4%9D%E5%B0%94%E7%89%A9%E7%90%86%E5%AD%A6%E5%A5%96", "title": "1921年诺贝尔物理学奖", "category": "AWARD", "importance": 8},
            
            # 同时代的科学家
            {"url": "/wiki/%E5%B0%BC%E5%B0%94%E6%96%AF%C2%B7%E7%8E%BB%E5%B0%94", "title": "尼尔斯·玻尔", "category": "SCIENTIST", "importance": 7},
            {"url": "/wiki/%E9%A9%AC%E5%85%8B%E6%96%AF%C2%B7%E6%99%AE%E6%9C%97%E5%85%8B", "title": "马克斯·普朗克", "category": "SCIENTIST", "importance": 7},
            
            # 科学哲学和思想
            {"url": "/wiki/%E7%A7%91%E5%AD%A6%E5%93%B2%E5%AD%A6", "title": "科学哲学", "category": "PHILOSOPHY", "importance": 6},
            {"url": "/wiki/%E7%BB%9F%E4%B8%80%E5%9C%BA%E8%AE%BA", "title": "统一场论", "category": "PHYSICS_THEORY", "importance": 7},
        ]
        
        self.crawled_data = []
        self.successful_pages = 0
        self.failed_pages = 0
        self.total_content_length = 0

    def get_page_content(self, url: str) -> BeautifulSoup:
        """获取页面内容并解析为BeautifulSoup对象"""
        full_url = f"{self.base_url}{url}"
        try:
            response = requests.get(full_url, headers=self.headers, timeout=15)
            response.raise_for_status()
            response.encoding = 'utf-8'
            return BeautifulSoup(response.text, 'html.parser')
        except requests.exceptions.RequestException as e:
            logger.error(f"❌ 无法获取页面 {full_url}: {e}")
            return None

    def extract_page_data(self, soup: BeautifulSoup, url: str, title: str, category: str, importance: int) -> Dict[str, Any]:
        """从BeautifulSoup对象中提取页面数据"""
        # 尝试多种选择器来找到主要内容区域
        content_div = None
        selectors = [
            'div.mw-content-ltr.mw-parser-output',  # 最常见的维基百科结构
            'div.mw-parser-output',
            'div#mw-content-text .mw-parser-output',
            'div#mw-content-text'
        ]
        
        for selector in selectors:
            content_div = soup.select_one(selector)
            if content_div:
                paragraphs = content_div.find_all('p')
                if paragraphs:
                    logger.info(f"使用选择器 '{selector}' 找到 {len(paragraphs)} 个段落")
                    break
        
        if not content_div:
            logger.warning(f"❌ 内容区域未找到: {title} ({url})")
            return None

        # 移除不相关元素
        for selector in ['table.infobox', 'div.toc', 'div.reflist', 'div.navbox', 'div.mw-references-wrap',
                         'div.printfooter', 'div.mw-indicator', 'div.catlinks', 'div.sister-project',
                         'div.ambox', 'div.metadata', 'span.mw-editsection', 'sup.reference', 'ol.references',
                         'table.wikitable', 'div.thumb', 'div.gallery']:
            for item in content_div.find_all(selector):
                item.decompose()

        # 提取主要段落文本
        paragraphs = content_div.find_all('p', recursive=False)
        content_text = "\n".join([p.get_text(separator=" ", strip=True) for p in paragraphs if p.get_text(strip=True)])

        # 如果内容太短，尝试更广范围的提取
        if len(content_text) < 150:
            all_text_elements = content_div.find_all(['p', 'h2', 'h3', 'h4', 'li'])
            content_text = "\n".join([elem.get_text(separator=" ", strip=True) for elem in all_text_elements if elem.get_text(strip=True)])

        content_text = self.clean_content(content_text)

        if not content_text or len(content_text) < 100:
            logger.warning(f"❌ 提取内容为空或过短: {title} ({url})")
            return None

        # 提取信息框数据
        infobox = {}
        info_table = soup.find('table', class_='infobox')
        if info_table:
            for row in info_table.find_all('tr'):
                header = row.find('th')
                data = row.find('td')
                if header and data:
                    key = header.get_text(strip=True)
                    value = data.get_text(strip=True)
                    if key and value:
                        infobox[key] = value

        # 提取标签
        tags = []
        # 根据分类添加标签
        category_tags = {
            "BIOGRAPHY": ["传记", "科学家", "物理学家"],
            "PHYSICS_THEORY": ["物理理论", "理论物理", "科学"],
            "PHYSICS_FORMULA": ["物理公式", "数学", "科学"],
            "PHYSICS_PHENOMENON": ["物理现象", "实验物理", "科学"],
            "PHYSICS_CONCEPT": ["物理概念", "理论", "科学"],
            "INSTITUTION": ["科学机构", "教育", "学术"],
            "AWARD": ["科学奖项", "荣誉", "诺贝尔奖"],
            "SCIENTIST": ["科学家", "物理学家", "同时代人"],
            "PHILOSOPHY": ["科学哲学", "哲学", "思想"]
        }
        
        tags.extend(category_tags.get(category, [category]))
        tags.extend([self.character_name, "20世纪科学", "现代物理"])

        return {
            "characterId": 3,  # 爱因斯坦的角色ID
            "title": title,
            "content": content_text,
            "knowledgeType": category,
            "importanceScore": importance,
            "source": "中文维基百科",
            "sourceUrl": f"{self.base_url}{url}",
            "tags": tags,
            "language": "zh"
        }

    def clean_content(self, content: str) -> str:
        """清理和格式化内容"""
        # 移除多余的换行和空格
        content = ' '.join(content.split())
        # 移除维基百科特有的编辑提示
        content = content.replace('[编辑]', '').replace('[查]', '').replace('[论]', '').replace('[阅]', '')
        # 移除引用标记
        content = self.remove_citations(content)
        return content.strip()

    def remove_citations(self, text: str) -> str:
        """移除文本中的引用标记，如 [1], [2]"""
        import re
        return re.sub(r'\[\d+\]', '', text)

    def make_safe_filename(self, title: str) -> str:
        """将标题转换为安全的文件名"""
        safe_title = "".join([c for c in title if c.isalnum() or c in (' ', '.', '_', '-')]).rstrip()
        return safe_title.replace(' ', '_')

    def crawl_single_page(self, page_info: Dict[str, Any]):
        """爬取单个页面"""
        url = page_info["url"]
        title = page_info["title"]
        category = page_info["category"]
        importance = page_info["importance"]

        logger.info(f"📖 处理第 {self.successful_pages + self.failed_pages + 1}/{len(self.pages_to_crawl)} 页: {title}")
        logger.info(f"🔍 正在获取: {self.base_url}{url}")

        soup = self.get_page_content(url)
        if soup:
            data = self.extract_page_data(soup, url, title, category, importance)
            if data and len(data["content"]) > 100:
                filename = f"{self.make_safe_filename(title)}.json"
                filepath = os.path.join(self.output_dir, filename)
                with open(filepath, 'w', encoding='utf-8') as f:
                    json.dump(data, f, ensure_ascii=False, indent=2)
                self.crawled_data.append(data)
                self.successful_pages += 1
                self.total_content_length += len(data["content"])
                logger.info(f"💾 已保存: {filepath}")
                logger.info(f"✅ 成功爬取: {title}")
                logger.info(f"📝 内容长度: {len(data['content'])} 字符")
            else:
                self.failed_pages += 1
                logger.warning(f"❌ 爬取失败或内容过短: {title}")
        else:
            self.failed_pages += 1
            logger.error(f"❌ 无法获取或解析页面: {title}")
        
        # 随机延迟，避免请求过快
        time.sleep(1.5)

    def crawl_all(self):
        """协调整个爬取过程"""
        logger.info("🚀 开始爬取爱因斯坦相关知识...")
        logger.info(f"📊 总页面数: {len(self.pages_to_crawl)}")

        for page_info in self.pages_to_crawl:
            self.crawl_single_page(page_info)

        # 保存所有爬取到的数据到一个总文件
        consolidated_filename = os.path.join(self.output_dir, f"{self.character_name.lower().replace(' ', '_')}_knowledge_base.json")
        with open(consolidated_filename, 'w', encoding='utf-8') as f:
            json.dump(self.crawled_data, f, ensure_ascii=False, indent=2)
        logger.info(f"💾 已保存: {consolidated_filename}")

        # 生成爬取报告
        report = {
            "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
            "character": self.character_name,
            "total_pages": len(self.pages_to_crawl),
            "successful_pages": self.successful_pages,
            "failed_pages": self.failed_pages,
            "success_rate": f"{self.successful_pages / len(self.pages_to_crawl) * 100:.1f}%",
            "total_content_length": self.total_content_length,
            "avg_content_length": self.total_content_length / self.successful_pages if self.successful_pages > 0 else 0,
            "categories": list(set([p["category"] for p in self.pages_to_crawl])),
            "pages": [{"title": d["title"], "length": len(d["content"]), "category": d["knowledgeType"]} for d in self.crawled_data]
        }
        report_filename = os.path.join(self.output_dir, "crawl_report.json")
        with open(report_filename, 'w', encoding='utf-8') as f:
            json.dump(report, f, ensure_ascii=False, indent=2)
        logger.info(f"💾 已保存: {report_filename}")

        logger.info("\n🎉 爬取完成！")
        logger.info("📊 统计信息:")
        logger.info(f"   - 总页面数: {len(self.pages_to_crawl)}")
        logger.info(f"   - 成功爬取: {self.successful_pages}")
        logger.info(f"   - 失败页面: {self.failed_pages}")
        logger.info(f"   - 成功率: {report['success_rate']}")
        logger.info(f"   - 总内容: {self.total_content_length} 字符")
        logger.info(f"   - 平均长度: {report['avg_content_length']:.0f} 字符")
        logger.info(f"   - 知识分类: {', '.join(report['categories'])}")

if __name__ == "__main__":
    crawler = EinsteinCrawler()
    crawler.crawl_all()