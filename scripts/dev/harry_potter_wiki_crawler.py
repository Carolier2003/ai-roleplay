#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
哈利·波特维基爬虫
从 https://harrypotter.fandom.com/zh 爬取角色和世界观数据

注意: 该维基使用 CC-BY-SA 协议，爬取的数据需要遵循相同协议
"""

import requests
import time
import json
import re
import os
from urllib.parse import urljoin, urlparse, quote
from bs4 import BeautifulSoup
from dataclasses import dataclass
from typing import List, Dict, Optional
import logging

# 设置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

@dataclass
class WikiPage:
    """维基页面数据结构"""
    title: str
    url: str
    content: str
    category: str
    tags: List[str]
    importance: int = 5

class HarryPotterWikiCrawler:
    """哈利·波特维基爬虫"""
    
    def __init__(self):
        self.base_url = "https://harrypotter.fandom.com"
        self.zh_base = f"{self.base_url}/zh"
        self.session = requests.Session()
        
        # 设置请求头，模拟浏览器访问
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
            'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
            'Accept-Encoding': 'gzip, deflate, br',
            'Connection': 'keep-alive',
            'Upgrade-Insecure-Requests': '1',
        })
        
        # 创建输出目录
        self.output_dir = "data/harry_potter_wiki"
        os.makedirs(self.output_dir, exist_ok=True)
        
        # 已爬取页面记录 (避免重复)
        self.crawled_urls = set()
        
        # 爬取策略配置
        self.crawl_config = {
            # 主要角色 (高优先级)
            "main_characters": {
                "urls": [
                    "/wiki/哈利·波特",
                    "/wiki/赫敏·格兰杰", 
                    "/wiki/罗恩·韦斯莱",
                    "/wiki/阿不思·邓布利多",
                    "/wiki/西弗勒斯·斯内普",
                    "/wiki/伏地魔",
                    "/wiki/小天狼星·布莱克",
                    "/wiki/卢平",
                    "/wiki/金妮·韦斯莱",
                    "/wiki/纳威·隆巴顿",
                    "/wiki/卢娜·洛夫古德"
                ],
                "importance": 10,
                "category": "MAIN_CHARACTER"
            },
            
            # 霍格沃茨相关 (高优先级)
            "hogwarts": {
                "urls": [
                    "/wiki/霍格沃茨魔法学校",
                    "/wiki/格兰芬多",
                    "/wiki/斯莱特林", 
                    "/wiki/拉文克劳",
                    "/wiki/赫奇帕奇",
                    "/wiki/分院帽",
                    "/wiki/霍格沃茨特快列车",
                    "/wiki/大礼堂",
                    "/wiki/格兰芬多塔",
                    "/wiki/地下室"
                ],
                "importance": 9,
                "category": "HOGWARTS"
            },
            
            # 魔法世界观 (中优先级)
            "magic_world": {
                "urls": [
                    "/wiki/魔法",
                    "/wiki/魁地奇",
                    "/wiki/守护神咒",
                    "/wiki/阿瓦达索命咒",
                    "/wiki/钻心剜骨",
                    "/wiki/魂器", 
                    "/wiki/凤凰社",
                    "/wiki/食死徒",
                    "/wiki/魔法部",
                    "/wiki/对角巷",
                    "/wiki/霍格莫德"
                ],
                "importance": 8,
                "category": "MAGIC_WORLD"
            },
            
            # 重要事件 (中优先级)  
            "events": {
                "urls": [
                    "/wiki/霍格沃茨大战",
                    "/wiki/三强争霸赛",
                    "/wiki/魁地奇世界杯",
                    "/wiki/神秘事务司之战"
                ],
                "importance": 7,
                "category": "EVENTS"
            }
        }
    
    def get_page_content(self, url: str) -> Optional[BeautifulSoup]:
        """获取页面内容"""
        # 确保URL以/开头进行正确的路径拼接
        if not url.startswith('/'):
            url = '/' + url
        full_url = self.zh_base + url
        
        if full_url in self.crawled_urls:
            logger.info(f"页面已爬取，跳过: {url}")
            return None
            
        try:
            logger.info(f"正在爬取: {url}")
            response = self.session.get(full_url, timeout=10)
            response.raise_for_status()
            
            # 强制设置编码为UTF-8
            response.encoding = 'utf-8'
            
            # 添加到已爬取记录
            self.crawled_urls.add(full_url)
            
            # 使用response.text而不是response.content
            soup = BeautifulSoup(response.text, 'html.parser')
            return soup
            
        except requests.RequestException as e:
            logger.error(f"爬取失败: {url}, 错误: {e}")
            return None
    
    def extract_page_data(self, soup: BeautifulSoup, url: str, category: str, importance: int) -> Optional[WikiPage]:
        """提取页面数据"""
        try:
            # 获取页面标题
            title_elem = soup.find('h1', {'class': 'page-header__title'}) or soup.find('h1')
            title = title_elem.get_text().strip() if title_elem else "未知标题"
            
            # 获取主要内容区域 (使用正确的选择器)
            selectors = [
                ('div[id="mw-content-text"]', 'MW内容区域'),
                ('div.main-container', '主容器'),
                ('div#content', '内容区域'),
                ('div.page-content', '页面内容'),
                ('div.mw-body-content', 'MW主体内容')
            ]
            
            content_div = None
            for selector, desc in selectors:
                content_div = soup.select_one(selector)
                if content_div:
                    logger.info(f"使用选择器 '{selector}' ({desc}) 找到内容区域")
                    break
            
            if not content_div:
                # 尝试find方法
                content_div = soup.find('div', id='mw-content-text')
                if content_div:
                    logger.info(f"使用find方法找到mw-content-text")
                else:
                    logger.warning(f"所有选择器都未找到内容区域: {url}")
                    
                    # 尝试直接获取段落
                    paragraphs = soup.find_all('p')
                    if paragraphs and len(paragraphs) > 10:
                        logger.info(f"找到 {len(paragraphs)} 个段落，尝试直接提取")
                        # 创建一个虚拟的内容div
                        content_div = soup.new_tag('div')
                        for p in paragraphs:
                            content_div.append(p)
                    else:
                        logger.warning(f"也未找到足够的段落内容 ({len(paragraphs) if paragraphs else 0} 个)")
                        return None
            
            # 清理内容
            content = self.clean_content(content_div)
            
            # 提取标签
            tags = self.extract_tags(soup, content)
            
            return WikiPage(
                title=title,
                url=url,
                content=content,
                category=category,
                tags=tags,
                importance=importance
            )
            
        except Exception as e:
            logger.error(f"数据提取失败: {url}, 错误: {e}")
            return None
    
    def clean_content(self, content_div) -> str:
        """清理和格式化内容"""
        # 移除不需要的元素
        for elem in content_div.find_all(['script', 'style', 'nav', 'footer']):
            elem.decompose()
        
        # 移除引用标记 [1], [2] 等
        for elem in content_div.find_all('sup', {'class': 'reference'}):
            elem.decompose()
        
        # 移除编辑链接
        for elem in content_div.find_all('span', {'class': 'mw-editsection'}):
            elem.decompose()
            
        # 提取纯文本，保留段落结构
        paragraphs = []
        for p in content_div.find_all(['p', 'h2', 'h3', 'h4', 'li']):
            text = p.get_text().strip()
            if text and len(text) > 10:  # 过滤太短的内容
                paragraphs.append(text)
        
        # 合并段落
        content = '\n\n'.join(paragraphs)
        
        # 清理多余的空白字符
        content = re.sub(r'\n{3,}', '\n\n', content)
        content = re.sub(r' {2,}', ' ', content)
        
        return content.strip()
    
    def extract_tags(self, soup: BeautifulSoup, content: str) -> List[str]:
        """提取标签"""
        tags = []
        
        # 从分类中提取标签
        category_links = soup.find_all('a', href=re.compile(r'/wiki/Category:'))
        for link in category_links:
            tag = link.get_text().strip()
            if tag and len(tag) < 50:  # 避免太长的标签
                tags.append(tag)
        
        # 从内容中提取关键词
        keywords = self.extract_keywords(content)
        tags.extend(keywords)
        
        # 去重并限制数量
        tags = list(set(tags))[:10]
        
        return tags
    
    def extract_keywords(self, content: str) -> List[str]:
        """从内容中提取关键词"""
        # 哈利·波特世界的关键词
        hp_keywords = [
            "格兰芬多", "斯莱特林", "拉文克劳", "赫奇帕奇",
            "霍格沃茨", "魁地奇", "魔法", "巫师", "麻瓜",
            "伏地魔", "食死徒", "凤凰社", "魔法部",
            "魂器", "守护神", "阿瓦达索命", "钻心剜骨",
            "对角巷", "霍格莫德", "阿兹卡班"
        ]
        
        found_keywords = []
        content_lower = content.lower()
        
        for keyword in hp_keywords:
            if keyword in content:
                found_keywords.append(keyword)
        
        return found_keywords[:5]  # 最多5个关键词
    
    def save_page_data(self, page_data: WikiPage):
        """保存页面数据到文件"""
        # 创建分类目录
        category_dir = os.path.join(self.output_dir, page_data.category.lower())
        os.makedirs(category_dir, exist_ok=True)
        
        # 生成文件名 (处理特殊字符)
        safe_title = re.sub(r'[^\w\s-]', '', page_data.title)
        safe_title = re.sub(r'[\s]+', '_', safe_title)
        filename = f"{safe_title}.json"
        
        filepath = os.path.join(category_dir, filename)
        
        # 转换为字典
        data = {
            "title": page_data.title,
            "url": page_data.url,
            "content": page_data.content,
            "category": page_data.category,
            "tags": page_data.tags,
            "importance": page_data.importance,
            "source": "哈利·波特维基",
            "source_url": f"https://harrypotter.fandom.com/zh{page_data.url}",
            "crawl_time": time.strftime("%Y-%m-%d %H:%M:%S")
        }
        
        # 保存为JSON文件
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        
        logger.info(f"已保存: {filepath}")
    
    def crawl_by_category(self, category_name: str, config: Dict):
        """按分类爬取"""
        logger.info(f"开始爬取分类: {category_name}")
        
        urls = config["urls"]
        importance = config["importance"]
        category = config["category"]
        
        crawled_count = 0
        
        for url in urls:
            try:
                # 获取页面内容
                soup = self.get_page_content(url)
                if not soup:
                    continue
                
                # 提取数据
                page_data = self.extract_page_data(soup, url, category, importance)
                if not page_data:
                    continue
                
                # 保存数据
                self.save_page_data(page_data)
                crawled_count += 1
                
                # 礼貌性延迟
                time.sleep(1)
                
            except Exception as e:
                logger.error(f"处理页面失败: {url}, 错误: {e}")
                continue
        
        logger.info(f"分类 {category_name} 爬取完成，成功爬取 {crawled_count} 页")
    
    def run(self):
        """运行爬虫"""
        logger.info("开始爬取哈利·波特维基数据...")
        
        total_crawled = 0
        
        # 按优先级爬取
        for category_name, config in self.crawl_config.items():
            self.crawl_by_category(category_name, config)
            
            # 统计
            total_crawled += len(config["urls"])
            
            # 分类间延迟
            time.sleep(2)
        
        logger.info(f"爬取完成！总共处理 {total_crawled} 个页面")
        logger.info(f"数据保存在: {self.output_dir}")
        
        # 生成统计报告
        self.generate_report()
    
    def generate_report(self):
        """生成爬取报告"""
        report = {
            "crawl_time": time.strftime("%Y-%m-%d %H:%M:%S"),
            "total_pages": len(self.crawled_urls),
            "categories": {},
            "output_directory": self.output_dir
        }
        
        # 统计各分类
        for category_name, config in self.crawl_config.items():
            report["categories"][category_name] = {
                "count": len(config["urls"]),
                "importance": config["importance"],
                "category": config["category"]
            }
        
        # 确保输出目录存在
        os.makedirs(self.output_dir, exist_ok=True)
        
        # 保存报告
        report_file = os.path.join(self.output_dir, "crawl_report.json")
        with open(report_file, 'w', encoding='utf-8') as f:
            json.dump(report, f, ensure_ascii=False, indent=2)
        
        logger.info(f"报告已生成: {report_file}")

def main():
    """主函数"""
    print("🧙‍♂️ 哈利·波特维基爬虫")
    print("=" * 50)
    
    crawler = HarryPotterWikiCrawler()
    
    try:
        crawler.run()
        print("\n✅ 爬取完成！")
        print(f"📁 数据保存在: {crawler.output_dir}")
        
    except KeyboardInterrupt:
        print("\n⏹️ 用户中断爬取")
    except Exception as e:
        print(f"\n❌ 爬取失败: {e}")
        logger.error(f"爬取失败: {e}")

if __name__ == "__main__":
    main()
