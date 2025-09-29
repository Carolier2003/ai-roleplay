#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
批量简化爬虫
基于验证可工作的简单爬虫逻辑，批量爬取多个页面
"""

import requests
from bs4 import BeautifulSoup
import json
import os
import time
from typing import List, Dict

class BatchSimpleCrawler:
    """批量简化爬虫"""
    
    def __init__(self):
        self.headers = {
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
        }
        self.base_url = "https://harrypotter.fandom.com/zh"
        self.output_dir = "batch_crawled_data"
        os.makedirs(self.output_dir, exist_ok=True)
        
        # 要爬取的页面列表
        self.pages_to_crawl = [
            # 主要角色
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
            # 霍格沃茨相关
            {
                "url": "/wiki/%E5%88%86%E9%99%A2%E5%B8%BD",
                "title": "分院帽",
                "category": "HOGWARTS",
                "importance": 9
            },
            {
                "url": "/wiki/%E9%9C%8D%E6%A0%BC%E6%B2%83%E8%8C%A8%E9%AD%94%E6%B3%95%E5%AD%A6%E6%A0%A1",
                "title": "霍格沃茨魔法学校",
                "category": "HOGWARTS",
                "importance": 9
            },
            {
                "url": "/wiki/%E6%A0%BC%E5%85%B0%E8%8A%AC%E5%A4%9A%E5%AD%A6%E9%99%A2",
                "title": "格兰芬多学院",
                "category": "HOGWARTS",
                "importance": 8
            },
            # 魔法世界
            {
                "url": "/wiki/%E9%AD%81%E5%9C%B0%E5%A5%87",
                "title": "魁地奇",
                "category": "MAGIC_WORLD",
                "importance": 8
            },
            {
                "url": "/wiki/%E9%98%BF%E7%93%A6%E8%BE%BE%E7%B4%A2%E5%91%BD%E5%92%92",
                "title": "阿瓦达索命咒",
                "category": "MAGIC_WORLD",
                "importance": 8
            }
        ]
    
    def crawl_single_page(self, page_info: Dict) -> bool:
        """爬取单个页面"""
        url = page_info["url"]
        full_url = self.base_url + url
        
        try:
            print(f"🔍 爬取: {page_info['title']} ({url})")
            
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
            tags = self.extract_tags(content, page_info["category"])
            
            # 创建数据
            data = {
                "characterId": 1,  # 默认哈利·波特
                "title": title,
                "content": content,
                "knowledgeType": self.map_category_to_knowledge_type(page_info["category"]),
                "importance": page_info["importance"],
                "source": "哈利·波特中文维基",
                "sourceUrl": full_url,
                "tags": tags
            }
            
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
    
    def extract_tags(self, content: str, category: str) -> List[str]:
        """提取标签"""
        # 哈利·波特相关关键词
        keywords = [
            "霍格沃茨", "格兰芬多", "斯莱特林", "拉文克劳", "赫奇帕奇",
            "魁地奇", "分院帽", "魔法", "巫师", "魔杖", 
            "守护神", "伏地魔", "食死徒", "凤凰社", "魔法部"
        ]
        
        found_tags = []
        for keyword in keywords:
            if keyword in content:
                found_tags.append(keyword)
        
        # 添加分类标签
        if category == "MAIN_CHARACTER":
            found_tags.append("主要角色")
        elif category == "HOGWARTS":
            found_tags.append("霍格沃茨")
        elif category == "MAGIC_WORLD":
            found_tags.append("魔法世界")
        
        return found_tags[:8]  # 最多8个标签
    
    def map_category_to_knowledge_type(self, category: str) -> str:
        """映射分类到知识类型"""
        mapping = {
            "MAIN_CHARACTER": "PERSONALITY",
            "HOGWARTS": "BASIC_INFO", 
            "MAGIC_WORLD": "KNOWLEDGE",
            "EVENTS": "EVENTS"
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
        print(f"🚀 开始批量爬取 {len(self.pages_to_crawl)} 个页面...")
        
        success_count = 0
        failed_count = 0
        
        for i, page_info in enumerate(self.pages_to_crawl):
            try:
                if self.crawl_single_page(page_info):
                    success_count += 1
                else:
                    failed_count += 1
                
                # 进度显示
                progress = (i + 1) / len(self.pages_to_crawl) * 100
                print(f"📊 进度: {i + 1}/{len(self.pages_to_crawl)} ({progress:.1f}%)")
                
                # 礼貌性延迟
                time.sleep(1)
                
            except KeyboardInterrupt:
                print("\n⏹️ 用户中断爬取")
                break
            except Exception as e:
                print(f"❌ 处理页面时出错: {e}")
                failed_count += 1
                continue
        
        return {
            "total": len(self.pages_to_crawl),
            "success": success_count,
            "failed": failed_count
        }
    
    def generate_import_data(self) -> str:
        """生成导入数据"""
        print("\n📦 生成批量导入数据...")
        
        all_data = []
        
        # 读取所有JSON文件
        for filename in os.listdir(self.output_dir):
            if filename.endswith('.json'):
                filepath = os.path.join(self.output_dir, filename)
                with open(filepath, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                    all_data.append(data)
        
        # 保存合并数据
        output_file = "harry_potter_knowledge_batch.json"
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(all_data, f, ensure_ascii=False, indent=2)
        
        print(f"✅ 合并数据已保存: {output_file}")
        print(f"📊 总计 {len(all_data)} 个知识条目")
        
        return output_file

def main():
    """主函数"""
    print("🧙‍♂️ 哈利·波特维基批量爬虫")
    print("=" * 50)
    
    crawler = BatchSimpleCrawler()
    
    try:
        # 爬取数据
        result = crawler.crawl_all()
        
        # 显示结果
        print(f"\n📊 爬取结果:")
        print(f"总页面数: {result['total']}")
        print(f"成功爬取: {result['success']}")
        print(f"爬取失败: {result['failed']}")
        print(f"成功率: {result['success']/result['total']*100:.1f}%")
        
        if result['success'] > 0:
            # 生成导入数据
            import_file = crawler.generate_import_data()
            
            print(f"\n🎉 爬取完成！")
            print(f"📁 数据保存在: {crawler.output_dir}")
            print(f"📄 导入文件: {import_file}")
            print(f"\n🚀 下一步: 将数据导入到RAG系统")
            print(f"命令: python3 import_to_rag.py {import_file}")
        else:
            print(f"\n❌ 没有成功爬取任何数据")
    
    except KeyboardInterrupt:
        print(f"\n⏹️ 用户中断爬取")
    except Exception as e:
        print(f"\n❌ 爬取失败: {e}")

if __name__ == "__main__":
    main()
