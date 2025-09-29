#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
柯南百科智能批量爬虫
基于提取的链接进行大规模知识库构建
"""

import requests
from bs4 import BeautifulSoup
import json
import os
import time
import random
from typing import List, Dict, Set
import re
from urllib.parse import unquote

class SmartConanCrawler:
    """智能柯南批量爬虫"""
    
    def __init__(self):
        self.headers = {
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
        }
        self.base_url = "https://www.conanpedia.com"
        self.character_id = 4  # 柯南的角色ID
        self.output_dir = "../../data/conan_expanded"
        os.makedirs(self.output_dir, exist_ok=True)
        
        # 设置爬取参数
        self.delay_range = (2, 5)  # 请求间隔2-5秒
        self.max_pages = 200  # 最大爬取页面数
        self.min_content_length = 50  # 最小内容长度
        
        # 爬取统计
        self.stats = {
            'total_attempted': 0,
            'success': 0,
            'failed': 0,
            'skipped': 0,
            'duplicate': 0,
            'too_short': 0
        }
        
        # 已爬取的URL集合（去重）
        self.crawled_urls: Set[str] = set()
        self.knowledge_items: List[Dict] = []
        
    def load_links(self, filename: str = "extracted_links.json") -> List[Dict]:
        """加载提取的链接"""
        try:
            with open(filename, 'r', encoding='utf-8') as f:
                links = json.load(f)
            print(f"✅ 加载了 {len(links)} 个链接")
            return links
        except Exception as e:
            print(f"❌ 加载链接失败: {e}")
            return []
    
    def prioritize_links(self, links: List[Dict]) -> List[Dict]:
        """对链接进行优先级排序"""
        print("🎯 对链接进行优先级排序...")
        
        # 定义优先级关键词（越重要权重越高）
        priority_keywords = {
            # 主要角色 - 最高优先级
            '江户川柯南': 100, '工藤新一': 100, '毛利兰': 95, '毛利小五郎': 90,
            '灰原哀': 95, '宫野志保': 90, '阿笠博士': 85, '服部平次': 85,
            '怪盗基德': 85, '黑衣组织': 95, '少年侦探团': 80,
            
            # 重要配角 - 高优先级  
            '目暮': 70, '高木': 70, '佐藤': 70, '白鸟': 65, '千叶': 65,
            '琴酒': 80, '伏特加': 75, '贝尔摩德': 80, '基尔': 75, '波本': 75,
            '赤井': 80, 'FBI': 70, 'CIA': 65,
            
            # 重要剧集 - 中等优先级
            '黑衣组织': 70, '重要': 60, '特别篇': 55, '剧场版': 50,
            
            # 普通内容 - 较低优先级
            'TV': 30, 'File': 25, '集': 20
        }
        
        def calculate_priority(link: Dict) -> int:
            """计算链接优先级"""
            text = link.get('text', '')
            url = link.get('url', '')
            
            # 基础分数
            score = 10
            
            # 根据关键词加分
            for keyword, weight in priority_keywords.items():
                if keyword in text or keyword in unquote(url):
                    score += weight
            
            # 角色页面加分
            if link.get('category') == 'CHARACTERS':
                score += 20
                
            # 长度合理的标题加分
            if 3 <= len(text) <= 50:
                score += 10
            
            return score
        
        # 排序并限制数量
        sorted_links = sorted(links, key=calculate_priority, reverse=True)
        prioritized = sorted_links[:self.max_pages]
        
        print(f"📊 优先级排序完成，选择前 {len(prioritized)} 个高优先级链接")
        
        # 显示前10个高优先级链接
        print("\n🔝 前10个高优先级链接:")
        for i, link in enumerate(prioritized[:10], 1):
            priority = calculate_priority(link)
            print(f"  {i}. {link['text']} (优先级: {priority})")
        
        return prioritized
    
    def fetch_page_content(self, url: str) -> str:
        """获取页面内容"""
        try:
            # 随机延迟
            delay = random.uniform(*self.delay_range)
            time.sleep(delay)
            
            response = requests.get(url, headers=self.headers, timeout=15)
            response.raise_for_status()
            response.encoding = 'utf-8'
            
            return response.text
            
        except Exception as e:
            print(f"❌ 获取页面失败 {url}: {e}")
            return ""
    
    def extract_knowledge(self, html: str, title: str, url: str, category: str) -> Dict:
        """提取知识内容"""
        if not html:
            return None
            
        try:
            soup = BeautifulSoup(html, 'html.parser')
            
            # 使用多种选择器
            content_selectors = [
                'div#mw-content-text',
                'div.mw-content-ltr', 
                'div#content',
                'div.mw-parser-output'
            ]
            
            content_div = None
            for selector in content_selectors:
                content_div = soup.select_one(selector)
                if content_div and len(content_div.get_text().strip()) > 100:
                    break
            
            if not content_div:
                return None
            
            # 提取段落
            paragraphs = content_div.find_all('p')
            content_parts = []
            
            for p in paragraphs:
                text = p.get_text().strip()
                if text and len(text) > 15:
                    # 清理文本
                    text = re.sub(r'\[\d+\]', '', text)  # 移除引用
                    text = re.sub(r'\s+', ' ', text)  # 标准化空格
                    text = re.sub(r'^[。，、；：？！\s]+', '', text)  # 移除开头标点
                    if text and len(text) > 10:
                        content_parts.append(text)
            
            # 如果段落不够，提取其他元素
            if len(content_parts) < 3:
                other_elements = content_div.find_all(['div', 'li', 'td', 'th', 'dd'])
                for elem in other_elements:
                    text = elem.get_text().strip()
                    if text and len(text) > 20:
                        # 避免重复
                        if not any(text in part for part in content_parts):
                            text = re.sub(r'\s+', ' ', text)
                            content_parts.append(text)
                            if len(content_parts) >= 10:
                                break
            
            if not content_parts:
                return None
                
            # 合并内容
            content = '\n\n'.join(content_parts[:20])  # 取前20段
            
            if len(content) < self.min_content_length:
                return None
            
            # 限制长度避免token超限
            if len(content) > 3000:
                content = content[:3000] + "..."
            
            # 确定知识类型
            knowledge_type = self.determine_knowledge_type(title, category, content)
            
            # 计算重要性分数
            importance = self.calculate_importance(title, content)
            
            return {
                "character_id": self.character_id,
                "title": title,
                "content": content,
                "knowledge_type": knowledge_type,
                "importance_score": importance,
                "source": "conanpedia",
                "source_url": url,
                "language": "zh",
                "status": 1,
                "tags": [category.lower(), "detective", "conan", "anime", knowledge_type]
            }
            
        except Exception as e:
            print(f"❌ 内容提取失败 {title}: {e}")
            return None
    
    def determine_knowledge_type(self, title: str, category: str, content: str) -> str:
        """确定知识类型"""
        # 基于标题和内容判断知识类型
        if any(keyword in title for keyword in ['TV', 'File', '集', '话']):
            return "episode"
        elif any(keyword in title for keyword in ['组织', '黑衣', 'FBI', 'CIA']):
            return "organization"
        elif any(keyword in title for keyword in ['侦探', '警察', '博士', '医生']):
            return "character"
        elif any(keyword in title for keyword in ['案件', '事件', '杀人', '绑架']):
            return "case"
        elif any(keyword in title for keyword in ['道具', '发明', '药']):
            return "item"
        elif category == "CHARACTERS":
            return "character"
        elif category == "TV_ANIME":
            return "episode"
        else:
            return "knowledge"
    
    def calculate_importance(self, title: str, content: str) -> int:
        """计算重要性分数"""
        score = 5  # 基础分数
        
        # 主要角色加分
        main_characters = ['江户川柯南', '工藤新一', '毛利兰', '毛利小五郎', '灰原哀', '黑衣组织']
        for char in main_characters:
            if char in title:
                score += 3
                break
            elif char in content:
                score += 1
                break
        
        # 内容长度加分
        if len(content) > 1000:
            score += 2
        elif len(content) > 500:
            score += 1
        
        # 限制分数范围
        return min(max(score, 1), 10)
    
    def crawl_batch(self, links: List[Dict]) -> List[Dict]:
        """批量爬取"""
        print(f"\n🚀 开始批量爬取 {len(links)} 个页面")
        print("=" * 50)
        
        for i, link in enumerate(links, 1):
            url = link['full_url']
            title = link['text']
            category = link['category']
            
            print(f"\n📄 [{i}/{len(links)}] 处理: {title}")
            
            self.stats['total_attempted'] += 1
            
            # 检查是否已爬取
            if url in self.crawled_urls:
                print(f"⏭️  已爬取，跳过")
                self.stats['skipped'] += 1
                continue
            
            # 获取页面内容
            html = self.fetch_page_content(url)
            if not html:
                self.stats['failed'] += 1
                continue
            
            # 提取知识
            knowledge = self.extract_knowledge(html, title, url, category)
            if not knowledge:
                print(f"⚠️  内容提取失败或太短")
                self.stats['too_short'] += 1
                continue
            
            # 检查重复内容
            if self.is_duplicate_content(knowledge):
                print(f"🔄 重复内容，跳过")
                self.stats['duplicate'] += 1
                continue
            
            # 保存成功
            self.knowledge_items.append(knowledge)
            self.crawled_urls.add(url)
            self.stats['success'] += 1
            
            print(f"✅ 成功提取: {len(knowledge['content'])} 字符，重要性: {knowledge['importance_score']}")
            
            # 保存单个文件
            safe_filename = re.sub(r'[^\w\s-]', '', title).strip()[:50]
            if safe_filename:
                output_file = os.path.join(self.output_dir, f"{safe_filename}.json")
                with open(output_file, 'w', encoding='utf-8') as f:
                    json.dump(knowledge, f, ensure_ascii=False, indent=2)
            
            # 显示进度
            success_rate = (self.stats['success'] / self.stats['total_attempted']) * 100
            print(f"📊 进度: {success_rate:.1f}% 成功率 ({self.stats['success']}/{self.stats['total_attempted']})")
            
            # 每50个页面保存一次进度
            if i % 50 == 0:
                self.save_progress()
        
        return self.knowledge_items
    
    def is_duplicate_content(self, knowledge: Dict) -> bool:
        """检查是否重复内容"""
        new_content = knowledge['content']
        new_title = knowledge['title']
        
        for existing in self.knowledge_items:
            # 标题相似度检查
            if new_title == existing['title']:
                return True
            
            # 内容相似度检查（简单版）
            if len(new_content) > 100 and len(existing['content']) > 100:
                # 检查前100字符的重复率
                if new_content[:100] == existing['content'][:100]:
                    return True
        
        return False
    
    def save_progress(self):
        """保存爬取进度"""
        if self.knowledge_items:
            progress_file = os.path.join(self.output_dir, "crawl_progress.json")
            progress_data = {
                'stats': self.stats,
                'knowledge_count': len(self.knowledge_items),
                'crawled_urls': list(self.crawled_urls),
                'latest_items': self.knowledge_items[-10:]  # 最新10个项目
            }
            
            with open(progress_file, 'w', encoding='utf-8') as f:
                json.dump(progress_data, f, ensure_ascii=False, indent=2)
            
            print(f"💾 进度已保存: {len(self.knowledge_items)} 个知识条目")
    
    def save_final_results(self):
        """保存最终结果"""
        if not self.knowledge_items:
            print("❌ 没有成功爬取任何内容")
            return
        
        # 保存完整知识库
        output_file = os.path.join(self.output_dir, "conan_expanded_knowledge.json")
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(self.knowledge_items, f, ensure_ascii=False, indent=2)
        
        # 生成统计报告
        self.generate_report()
        
        print(f"\n🎉 爬取完成！")
        print(f"📁 输出目录: {self.output_dir}")
        print(f"📄 主文件: {output_file}")
        print(f"📊 知识条目: {len(self.knowledge_items)} 个")
    
    def generate_report(self):
        """生成爬取报告"""
        report = {
            'crawl_summary': self.stats,
            'knowledge_stats': {
                'total_items': len(self.knowledge_items),
                'avg_content_length': sum(len(k['content']) for k in self.knowledge_items) // len(self.knowledge_items) if self.knowledge_items else 0,
                'knowledge_types': {},
                'importance_distribution': {}
            }
        }
        
        # 统计知识类型
        for item in self.knowledge_items:
            ktype = item['knowledge_type']
            importance = item['importance_score']
            
            report['knowledge_stats']['knowledge_types'][ktype] = report['knowledge_stats']['knowledge_types'].get(ktype, 0) + 1
            report['knowledge_stats']['importance_distribution'][str(importance)] = report['knowledge_stats']['importance_distribution'].get(str(importance), 0) + 1
        
        # 保存报告
        report_file = os.path.join(self.output_dir, "crawl_report.json")
        with open(report_file, 'w', encoding='utf-8') as f:
            json.dump(report, f, ensure_ascii=False, indent=2)
        
        print(f"📋 爬取报告已保存: {report_file}")

def main():
    """主函数"""
    print("🕵️ 柯南百科智能批量爬虫")
    print("=" * 50)
    
    crawler = SmartConanCrawler()
    
    # 加载链接
    links = crawler.load_links()
    if not links:
        print("❌ 没有可用的链接")
        return
    
    # 优先级排序
    prioritized_links = crawler.prioritize_links(links)
    
    print(f"\n⚠️  准备爬取 {len(prioritized_links)} 个页面")
    print(f"预计用时: {len(prioritized_links) * 3.5 / 60:.1f} 分钟")
    
    # 开始爬取
    results = crawler.crawl_batch(prioritized_links)
    
    # 保存结果
    crawler.save_final_results()
    
    # 显示最终统计
    print(f"\n📊 最终统计:")
    print(f"  尝试爬取: {crawler.stats['total_attempted']} 页")
    print(f"  成功: {crawler.stats['success']} 页")
    print(f"  失败: {crawler.stats['failed']} 页")
    print(f"  跳过: {crawler.stats['skipped']} 页")
    print(f"  重复: {crawler.stats['duplicate']} 页")
    print(f"  内容过短: {crawler.stats['too_short']} 页")
    print(f"  成功率: {(crawler.stats['success'] / crawler.stats['total_attempted'] * 100):.1f}%")

if __name__ == "__main__":
    main()
