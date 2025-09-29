#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
扩展版批量爬虫
大幅扩展哈利·波特维基爬取内容，包含更多角色、地点、魔法道具等
"""

import requests
from bs4 import BeautifulSoup
import json
import os
import time
from typing import List, Dict

class ExpandedBatchCrawler:
    """扩展版批量爬虫"""
    
    def __init__(self):
        self.headers = {
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
        }
        self.base_url = "https://harrypotter.fandom.com/zh"
        self.output_dir = "expanded_crawled_data"
        os.makedirs(self.output_dir, exist_ok=True)
        
        # 要爬取的页面列表 - 大幅扩展
        self.pages_to_crawl = [
            # ===== 主要角色 =====
            {
                "url": "/wiki/%E5%93%88%E5%88%A9%C2%B7%E6%B3%A2%E7%89%B9",
                "title": "哈利·波特",
                "category": "MAIN_CHARACTER",
                "importance": 10
            },
            {
                "url": "/wiki/%E8%B5%AB%E6%95%8F%C2%B7%E6%A0%BC%E5%85%B0%E6%9D%B0",
                "title": "赫敏·格兰杰", 
                "category": "MAIN_CHARACTER",
                "importance": 10
            },
            {
                "url": "/wiki/%E7%BD%97%E6%81%A9%C2%B7%E9%9F%A6%E6%96%AF%E8%8E%B1",
                "title": "罗恩·韦斯莱",
                "category": "MAIN_CHARACTER", 
                "importance": 10
            },
            {
                "url": "/wiki/%E9%98%BF%E4%B8%8D%E6%80%9D%C2%B7%E9%82%93%E5%B8%83%E5%88%A9%E5%A4%9A",
                "title": "阿不思·邓布利多",
                "category": "MAIN_CHARACTER",
                "importance": 10
            },
            {
                "url": "/wiki/%E4%BC%8F%E5%9C%B0%E9%AD%94",
                "title": "伏地魔",
                "category": "MAIN_CHARACTER",
                "importance": 10
            },
            {
                "url": "/wiki/%E8%A5%BF%E5%BC%97%E5%8B%92%E6%96%AF%C2%B7%E6%96%AF%E5%86%85%E6%99%AE",
                "title": "西弗勒斯·斯内普",
                "category": "MAIN_CHARACTER",
                "importance": 9
            },
            {
                "url": "/wiki/%E5%B0%8F%E5%A4%A9%E7%8B%BC%E6%98%9F%C2%B7%E5%B8%83%E8%8E%B1%E5%85%8B",
                "title": "小天狼星·布莱克",
                "category": "MAIN_CHARACTER",
                "importance": 9
            },
            {
                "url": "/wiki/%E8%8E%B1%E5%A7%86%E6%96%AF%C2%B7%E5%8D%A2%E5%B9%B3",
                "title": "莱姆斯·卢平",
                "category": "MAIN_CHARACTER",
                "importance": 8
            },
            {
                "url": "/wiki/%E9%87%91%E5%A6%AE%C2%B7%E9%9F%A6%E6%96%AF%E8%8E%B1",
                "title": "金妮·韦斯莱",
                "category": "MAIN_CHARACTER",
                "importance": 8
            },
            {
                "url": "/wiki/%E7%BA%B3%E5%A8%81%C2%B7%E9%9A%86%E5%B7%B4%E9%A1%BF",
                "title": "纳威·隆巴顿",
                "category": "MAIN_CHARACTER",
                "importance": 8
            },
            {
                "url": "/wiki/%E5%8D%A2%E5%A8%9C%C2%B7%E6%B4%9B%E5%A4%AB%E5%8F%A4%E5%BE%B7",
                "title": "卢娜·洛夫古德",
                "category": "MAIN_CHARACTER",
                "importance": 8
            },
            {
                "url": "/wiki/%E9%B2%81%E4%BC%AF%C2%B7%E6%B5%B7%E6%A0%BC",
                "title": "鲁伯·海格",
                "category": "MAIN_CHARACTER",
                "importance": 8
            },

            # ===== 霍格沃茨相关 =====
            {
                "url": "/wiki/%E9%9C%8D%E6%A0%BC%E6%B2%83%E8%8C%A8%E9%AD%94%E6%B3%95%E5%AD%A6%E6%A0%A1",
                "title": "霍格沃茨魔法学校",
                "category": "HOGWARTS",
                "importance": 10
            },
            {
                "url": "/wiki/%E5%88%86%E9%99%A2%E5%B8%BD",
                "title": "分院帽",
                "category": "HOGWARTS",
                "importance": 9
            },
            {
                "url": "/wiki/%E6%A0%BC%E5%85%B0%E8%8A%AC%E5%A4%9A%E5%AD%A6%E9%99%A2",
                "title": "格兰芬多学院",
                "category": "HOGWARTS",
                "importance": 9
            },
            {
                "url": "/wiki/%E6%96%AF%E8%8E%B1%E7%89%B9%E6%9E%97%E5%AD%A6%E9%99%A2",
                "title": "斯莱特林学院",
                "category": "HOGWARTS",
                "importance": 9
            },
            {
                "url": "/wiki/%E6%8B%89%E6%96%87%E5%85%8B%E5%8A%B3%E5%AD%A6%E9%99%A2",
                "title": "拉文克劳学院",
                "category": "HOGWARTS",
                "importance": 8
            },
            {
                "url": "/wiki/%E8%B5%AB%E5%A5%87%E5%B8%95%E5%A5%87%E5%AD%A6%E9%99%A2",
                "title": "赫奇帕奇学院",
                "category": "HOGWARTS",
                "importance": 8
            },
            {
                "url": "/wiki/%E9%9C%8D%E6%A0%BC%E6%B2%83%E8%8C%A8%E7%89%B9%E5%BF%AB%E5%88%97%E8%BD%A6",
                "title": "霍格沃茨特快列车",
                "category": "HOGWARTS",
                "importance": 8
            },
            {
                "url": "/wiki/%E5%A4%A7%E7%A4%BC%E5%A0%82",
                "title": "大礼堂",
                "category": "HOGWARTS",
                "importance": 8
            },

            # ===== 重要地点 =====
            {
                "url": "/wiki/%E5%AF%B9%E8%A7%92%E5%B7%B7",
                "title": "对角巷",
                "category": "LOCATIONS",
                "importance": 9
            },
            {
                "url": "/wiki/%E4%B9%9D%E5%8F%88%E5%9B%9B%E5%88%86%E4%B9%8B%E4%B8%89%E7%AB%99%E5%8F%B0",
                "title": "九又四分之三站台",
                "category": "LOCATIONS",
                "importance": 8
            },
            {
                "url": "/wiki/%E9%9C%8D%E6%A0%BC%E8%8E%AB%E5%BE%B7%E6%9D%91",
                "title": "霍格莫德村",
                "category": "LOCATIONS",
                "importance": 8
            },
            {
                "url": "/wiki/%E7%A6%81%E6%9E%97",
                "title": "禁林",
                "category": "LOCATIONS",
                "importance": 8
            },
            {
                "url": "/wiki/%E9%98%BF%E5%85%B9%E5%8D%A1%E7%8F%AD",
                "title": "阿兹卡班",
                "category": "LOCATIONS",
                "importance": 8
            },

            # ===== 魔法道具 =====
            {
                "url": "/wiki/%E9%AD%94%E6%B3%95%E7%9F%B3",
                "title": "魔法石",
                "category": "MAGIC_ITEMS",
                "importance": 9
            },
            {
                "url": "/wiki/%E9%AD%82%E5%99%A8",
                "title": "魂器",
                "category": "MAGIC_ITEMS",
                "importance": 9
            },
            {
                "url": "/wiki/%E9%AD%94%E6%9D%96",
                "title": "魔杖",
                "category": "MAGIC_ITEMS",
                "importance": 9
            },
            {
                "url": "/wiki/%E9%9A%90%E5%BD%A2%E8%A1%A3",
                "title": "隐形衣",
                "category": "MAGIC_ITEMS",
                "importance": 8
            },
            {
                "url": "/wiki/%E6%B4%BB%E7%82%B9%E5%9C%B0%E5%9B%BE",
                "title": "活点地图",
                "category": "MAGIC_ITEMS",
                "importance": 8
            },
            {
                "url": "/wiki/%E6%97%B6%E9%97%B4%E8%BD%AC%E6%8D%A2%E5%99%A8",
                "title": "时间转换器",
                "category": "MAGIC_ITEMS",
                "importance": 8
            },

            # ===== 魔法和咒语 =====
            {
                "url": "/wiki/%E9%98%BF%E7%93%A6%E8%BE%BE%E7%B4%A2%E5%91%BD%E5%92%92",
                "title": "阿瓦达索命咒",
                "category": "MAGIC_SPELLS",
                "importance": 9
            },
            {
                "url": "/wiki/%E5%AE%88%E6%8A%A4%E7%A5%9E%E5%92%92",
                "title": "守护神咒",
                "category": "MAGIC_SPELLS",
                "importance": 9
            },
            {
                "url": "/wiki/%E9%92%BB%E5%BF%83%E5%89%9C%E9%AA%A8",
                "title": "钻心剜骨",
                "category": "MAGIC_SPELLS",
                "importance": 8
            },
            {
                "url": "/wiki/%E5%A4%BA%E9%AD%82%E5%92%92",
                "title": "夺魂咒",
                "category": "MAGIC_SPELLS",
                "importance": 8
            },
            {
                "url": "/wiki/%E4%B8%89%E5%A4%A7%E4%B8%8D%E5%8F%AF%E5%AE%BD%E6%81%95%E5%92%92",
                "title": "三大不可宽恕咒",
                "category": "MAGIC_SPELLS",
                "importance": 9
            },

            # ===== 运动和活动 =====
            {
                "url": "/wiki/%E9%AD%81%E5%9C%B0%E5%A5%87",
                "title": "魁地奇",
                "category": "SPORTS",
                "importance": 9
            },
            {
                "url": "/wiki/%E4%B8%89%E5%BC%BA%E4%BA%89%E9%9C%B8%E8%B5%9B",
                "title": "三强争霸赛",
                "category": "SPORTS",
                "importance": 8
            },

            # ===== 重要事件 =====
            {
                "url": "/wiki/%E9%9C%8D%E6%A0%BC%E6%B2%83%E8%8C%A8%E5%A4%A7%E6%88%98",
                "title": "霍格沃茨大战",
                "category": "EVENTS",
                "importance": 10
            },
            {
                "url": "/wiki/%E7%AC%AC%E4%B8%80%E6%AC%A1%E5%B7%AB%E5%B8%88%E6%88%98%E4%BA%89",
                "title": "第一次巫师战争",
                "category": "EVENTS",
                "importance": 9
            },
            {
                "url": "/wiki/%E7%AC%AC%E4%BA%8C%E6%AC%A1%E5%B7%AB%E5%B8%88%E6%88%98%E4%BA%89",
                "title": "第二次巫师战争",
                "category": "EVENTS",
                "importance": 9
            },

            # ===== 组织和团体 =====
            {
                "url": "/wiki/%E5%87%A4%E5%87%B0%E7%A4%BE",
                "title": "凤凰社",
                "category": "ORGANIZATIONS",
                "importance": 9
            },
            {
                "url": "/wiki/%E9%A3%9F%E6%AD%BB%E5%BE%92",
                "title": "食死徒",
                "category": "ORGANIZATIONS",
                "importance": 9
            },
            {
                "url": "/wiki/%E9%AD%94%E6%B3%95%E9%83%A8",
                "title": "魔法部",
                "category": "ORGANIZATIONS",
                "importance": 8
            },
            {
                "url": "/wiki/%E9%82%93%E5%B8%83%E5%88%A9%E5%A4%9A%E5%86%9B",
                "title": "邓布利多军",
                "category": "ORGANIZATIONS",
                "importance": 8
            },

            # ===== 神奇动物 =====
            {
                "url": "/wiki/%E9%BE%99",
                "title": "龙",
                "category": "CREATURES",
                "importance": 8
            },
            {
                "url": "/wiki/%E5%87%A4%E5%87%B0",
                "title": "凤凰",
                "category": "CREATURES",
                "importance": 8
            },
            {
                "url": "/wiki/%E9%A9%AC%E4%BA%BA",
                "title": "马人",
                "category": "CREATURES",
                "importance": 7
            },
            {
                "url": "/wiki/%E5%AE%B6%E5%85%BB%E5%B0%8F%E7%B2%BE%E7%81%B5",
                "title": "家养小精灵",
                "category": "CREATURES",
                "importance": 7
            },

            # ===== 其他重要概念 =====
            {
                "url": "/wiki/%E9%BA%BB%E7%93%9C",
                "title": "麻瓜",
                "category": "CONCEPTS",
                "importance": 8
            },
            {
                "url": "/wiki/%E7%BA%AF%E8%A1%80%E7%BB%9F",
                "title": "纯血统",
                "category": "CONCEPTS",
                "importance": 7
            },
            {
                "url": "/wiki/%E6%B7%B7%E8%A1%80",
                "title": "混血",
                "category": "CONCEPTS",
                "importance": 7
            },
            {
                "url": "/wiki/%E9%BA%BB%E7%93%9C%E5%87%BA%E8%BA%AB",
                "title": "麻瓜出身",
                "category": "CONCEPTS",
                "importance": 7
            }
        ]
    
    def crawl_single_page(self, page_info: Dict) -> bool:
        """爬取单个页面"""
        url = page_info["url"]
        full_url = self.base_url + url
        
        try:
            print(f"🔍 爬取: {page_info['title']} ({page_info['category']}) - 重要性: {page_info['importance']}")
            
            response = requests.get(full_url, headers=self.headers, timeout=10)
            response.raise_for_status()
            response.encoding = 'utf-8'
            
            soup = BeautifulSoup(response.text, 'html.parser')
            
            # 提取标题
            title_elem = soup.find('h1', {'class': 'page-header__title'}) or soup.find('h1')
            title = title_elem.get_text().strip() if title_elem else page_info["title"]
            
            # 提取内容
            content_div = soup.find('div', id='mw-content-text')
            
            if not content_div:
                print(f"  ❌ 未找到内容区域")
                return False
            
            paragraphs = content_div.find_all('p')
            
            # 提取有效段落
            valid_paragraphs = []
            for p in paragraphs:
                text = p.get_text().strip()
                if len(text) > 20:
                    valid_paragraphs.append(text)
            
            if not valid_paragraphs:
                print(f"  ❌ 没有有效段落")
                return False
            
            # 合并所有段落
            content = '\n\n'.join(valid_paragraphs)
            
            # 提取关键词标签
            tags = self.extract_tags(content, page_info["category"], page_info["title"])
            
            # 创建数据
            data = {
                "characterId": 1,  # 默认哈利·波特
                "title": title,
                "content": content,
                "knowledgeType": self.map_category_to_knowledge_type(page_info["category"]),
                "importance": page_info["importance"],
                "source": "哈利·波特中文维基",
                "sourceUrl": full_url,
                "tags": tags,
                "category": page_info["category"]
            }
            
            # 确保输出目录存在
            os.makedirs(self.output_dir, exist_ok=True)
            
            # 保存数据
            safe_filename = self.make_safe_filename(title)
            output_file = os.path.join(self.output_dir, f"{safe_filename}.json")
            
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
            
            print(f"  ✅ 成功保存: {len(valid_paragraphs)} 段落, {len(content)} 字符")
            return True
            
        except Exception as e:
            print(f"  ❌ 爬取失败: {e}")
            return False
    
    def extract_tags(self, content: str, category: str, title: str) -> List[str]:
        """提取标签"""
        # 哈利·波特相关关键词
        keywords = [
            "霍格沃茨", "格兰芬多", "斯莱特林", "拉文克劳", "赫奇帕奇",
            "魁地奇", "分院帽", "魔法", "巫师", "魔杖", "守护神", 
            "伏地魔", "食死徒", "凤凰社", "魔法部", "对角巷", "禁林",
            "魂器", "魔法石", "隐形衣", "活点地图", "时间转换器",
            "邓布利多", "斯内普", "海格", "麻瓜", "纯血统", "混血",
            "阿瓦达索命", "钻心剜骨", "夺魂咒", "不可宽恕咒",
            "霍格莫德", "阿兹卡班", "三强争霸赛", "大战", "战争"
        ]
        
        found_tags = []
        for keyword in keywords:
            if keyword in content:
                found_tags.append(keyword)
        
        # 添加标题作为标签
        if title and title not in found_tags:
            found_tags.append(title)
        
        # 添加分类标签
        category_tags = {
            "MAIN_CHARACTER": "主要角色",
            "HOGWARTS": "霍格沃茨",
            "LOCATIONS": "重要地点",
            "MAGIC_ITEMS": "魔法道具",
            "MAGIC_SPELLS": "魔法咒语",
            "SPORTS": "魔法运动",
            "EVENTS": "重要事件",
            "ORGANIZATIONS": "组织团体",
            "CREATURES": "神奇动物",
            "CONCEPTS": "魔法概念"
        }
        
        if category in category_tags:
            category_tag = category_tags[category]
            if category_tag not in found_tags:
                found_tags.append(category_tag)
        
        return found_tags[:10]  # 最多10个标签
    
    def map_category_to_knowledge_type(self, category: str) -> str:
        """映射分类到知识类型"""
        mapping = {
            "MAIN_CHARACTER": "PERSONALITY",
            "HOGWARTS": "BASIC_INFO",
            "LOCATIONS": "BASIC_INFO",
            "MAGIC_ITEMS": "KNOWLEDGE",
            "MAGIC_SPELLS": "KNOWLEDGE",
            "SPORTS": "KNOWLEDGE",
            "EVENTS": "EVENTS",
            "ORGANIZATIONS": "BASIC_INFO",
            "CREATURES": "KNOWLEDGE",
            "CONCEPTS": "KNOWLEDGE"
        }
        return mapping.get(category, "BASIC_INFO")
    
    def make_safe_filename(self, title: str) -> str:
        """生成安全的文件名"""
        import re
        # 移除特殊字符
        safe_name = re.sub(r'[^\w\s-]', '', title)
        safe_name = re.sub(r'\s+', '_', safe_name)
        return safe_name[:50]  # 限制长度
    
    def crawl_all(self) -> Dict[str, int]:
        """批量爬取所有页面"""
        print(f"🚀 开始扩展批量爬取 {len(self.pages_to_crawl)} 个页面...")
        print(f"📊 分类统计:")
        
        # 统计各分类数量
        category_counts = {}
        for page in self.pages_to_crawl:
            category = page["category"]
            category_counts[category] = category_counts.get(category, 0) + 1
        
        for category, count in category_counts.items():
            print(f"  {category}: {count} 页")
        
        print("\n" + "=" * 60)
        
        success_count = 0
        failed_count = 0
        category_results = {}
        
        for i, page_info in enumerate(self.pages_to_crawl):
            try:
                category = page_info["category"]
                if category not in category_results:
                    category_results[category] = {"success": 0, "failed": 0}
                
                if self.crawl_single_page(page_info):
                    success_count += 1
                    category_results[category]["success"] += 1
                else:
                    failed_count += 1
                    category_results[category]["failed"] += 1
                
                # 进度显示
                progress = (i + 1) / len(self.pages_to_crawl) * 100
                print(f"📊 总进度: {i + 1}/{len(self.pages_to_crawl)} ({progress:.1f}%)")
                
                # 礼貌性延迟
                time.sleep(1.5)  # 稍微长一点的延迟，避免给服务器造成压力
                
            except KeyboardInterrupt:
                print("\n⏹️ 用户中断爬取")
                break
            except Exception as e:
                print(f"❌ 处理页面时出错: {e}")
                failed_count += 1
                if page_info["category"] in category_results:
                    category_results[page_info["category"]]["failed"] += 1
                continue
        
        # 显示分类结果
        print(f"\n📊 分类爬取结果:")
        for category, results in category_results.items():
            total = results["success"] + results["failed"]
            success_rate = (results["success"] / total * 100) if total > 0 else 0
            print(f"  {category}: {results['success']}/{total} ({success_rate:.1f}%)")
        
        return {
            "total": len(self.pages_to_crawl),
            "success": success_count,
            "failed": failed_count,
            "category_results": category_results
        }
    
    def generate_import_data(self) -> str:
        """生成导入数据"""
        print("\n📦 生成扩展批量导入数据...")
        
        all_data = []
        category_stats = {}
        
        # 读取所有JSON文件
        for filename in os.listdir(self.output_dir):
            if filename.endswith('.json'):
                filepath = os.path.join(self.output_dir, filename)
                with open(filepath, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                    all_data.append(data)
                    
                    # 统计分类
                    category = data.get("category", "UNKNOWN")
                    if category not in category_stats:
                        category_stats[category] = 0
                    category_stats[category] += 1
        
        # 保存合并数据
        output_file = "harry_potter_expanded_knowledge.json"
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(all_data, f, ensure_ascii=False, indent=2)
        
        print(f"✅ 扩展知识库已生成: {output_file}")
        print(f"📊 总计 {len(all_data)} 个知识条目")
        
        print(f"\n📋 分类分布:")
        for category, count in sorted(category_stats.items()):
            print(f"  {category}: {count} 条")
        
        return output_file

def main():
    """主函数"""
    print("🧙‍♂️ 哈利·波特维基扩展批量爬虫")
    print("=" * 60)
    print("🎯 本次爬取将获取大量哈利·波特世界的详细知识")
    print("📚 包含角色、地点、魔法道具、咒语、事件等各个方面")
    print("=" * 60)
    
    crawler = ExpandedBatchCrawler()
    
    try:
        # 直接开始爬取
        print(f"\n🚀 开始爬取 {len(crawler.pages_to_crawl)} 个页面，预计需要 {len(crawler.pages_to_crawl) * 1.5 / 60:.1f} 分钟")
        
        # 爬取数据
        result = crawler.crawl_all()
        
        # 显示结果
        print(f"\n🎉 扩展爬取完成！")
        print("=" * 40)
        print(f"📊 总体结果:")
        print(f"  总页面数: {result['total']}")
        print(f"  成功爬取: {result['success']}")
        print(f"  爬取失败: {result['failed']}")
        print(f"  成功率: {result['success']/result['total']*100:.1f}%")
        
        if result['success'] > 0:
            # 生成导入数据
            import_file = crawler.generate_import_data()
            
            print(f"\n🎯 下一步操作:")
            print(f"📁 数据保存在: {crawler.output_dir}/")
            print(f"📄 导入文件: {import_file}")
            print(f"🚀 导入命令: python3 import_to_rag.py {import_file}")
            print(f"💡 这将为哈利·波特AI提供丰富的背景知识！")
        else:
            print(f"\n❌ 没有成功爬取任何数据")
    
    except KeyboardInterrupt:
        print(f"\n⏹️ 用户中断爬取")
    except Exception as e:
        print(f"\n❌ 爬取失败: {e}")

if __name__ == "__main__":
    main()
