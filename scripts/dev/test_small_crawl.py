#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
小规模爬虫测试
测试完整的爬取 -> 处理 -> 导入流程
"""

import sys
import os

# 添加当前目录到路径
sys.path.append(os.path.dirname(__file__))

from harry_potter_wiki_crawler import HarryPotterWikiCrawler
from data_processor import HarryPotterDataProcessor
import json
import time

class SmallCrawlTest:
    """小规模爬虫测试"""
    
    def __init__(self):
        self.output_dir = "test_data"
        os.makedirs(self.output_dir, exist_ok=True)
    
    def run_test_crawl(self):
        """运行测试爬取"""
        print("🧪 开始小规模爬虫测试...")
        
        # 创建爬虫实例
        crawler = HarryPotterWikiCrawler()
        
        # 修改输出目录
        crawler.output_dir = f"{self.output_dir}/harry_potter_wiki"
        
        # 设置测试配置 (只爬取3个页面，使用URL编码)
        test_config = {
            "test_pages": {
                "urls": [
                    "/wiki/%E5%88%86%E9%99%A2%E5%B8%BD",              # 分院帽
                    "/wiki/%E5%93%88%E5%88%A9%C2%B7%E6%B3%A2%E7%89%B9",  # 哈利·波特  
                    "/wiki/%E9%9C%8D%E6%A0%BC%E6%B2%83%E8%8C%A8%E9%AD%94%E6%B3%95%E5%AD%A6%E6%A0%A1"  # 霍格沃茨
                ],
                "importance": 9,
                "category": "TEST_DATA"
            }
        }
        
        # 替换爬取配置
        crawler.crawl_config = test_config
        
        try:
            crawler.run()
            print("✅ 测试爬取完成")
            return True
        except Exception as e:
            print(f"❌ 测试爬取失败: {e}")
            return False
    
    def run_test_processing(self):
        """运行测试数据处理"""
        print("\n📚 开始测试数据处理...")
        
        try:
            processor = HarryPotterDataProcessor(f"{self.output_dir}/harry_potter_wiki")
            processed_data = processor.process_data()
            
            if not processed_data:
                print("❌ 没有处理到数据")
                return False
            
            # 保存处理后的数据
            output_file = processor.save_processed_data(
                processed_data, 
                f"{self.output_dir}/test_knowledge.json"
            )
            
            print(f"✅ 测试处理完成，生成 {len(processed_data)} 个知识条目")
            return True
            
        except Exception as e:
            print(f"❌ 测试处理失败: {e}")
            return False
    
    def show_processed_data_preview(self):
        """显示处理后数据的预览"""
        print("\n📋 处理后数据预览:")
        
        try:
            with open(f"{self.output_dir}/test_knowledge.json", 'r', encoding='utf-8') as f:
                data = json.load(f)
            
            for i, item in enumerate(data[:3]):  # 只显示前3个
                print(f"\n📖 知识条目 {i+1}:")
                print(f"   标题: {item['title']}")
                print(f"   类型: {item['knowledgeType']}")
                print(f"   重要性: {item['importance']}")
                print(f"   内容预览: {item['content'][:100]}...")
                print(f"   标签: {', '.join(item['tags'][:3])}")
            
            if len(data) > 3:
                print(f"\n... 还有 {len(data) - 3} 个知识条目")
        
        except Exception as e:
            print(f"❌ 预览失败: {e}")
    
    def cleanup(self):
        """清理测试数据"""
        print(f"\n🧹 清理测试数据: {self.output_dir}")
        import shutil
        if os.path.exists(self.output_dir):
            shutil.rmtree(self.output_dir)
            print("✅ 清理完成")

def main():
    """主函数"""
    print("🧪 哈利·波特维基小规模测试")
    print("=" * 50)
    
    test = SmallCrawlTest()
    
    try:
        # 步骤1: 测试爬取
        if not test.run_test_crawl():
            return
        
        # 步骤2: 测试处理
        if not test.run_test_processing():
            return
        
        # 步骤3: 显示预览
        test.show_processed_data_preview()
        
        print("\n🎉 小规模测试完成！")
        print("✅ 爬取、处理流程都正常工作")
        print("👉 现在可以运行完整爬虫: ./run_crawler.sh all")
        
        # 询问是否清理
        response = input("\n🗑️  是否清理测试数据？(y/N): ").strip().lower()
        if response == 'y':
            test.cleanup()
        else:
            print(f"📁 测试数据保留在: {test.output_dir}")
        
    except KeyboardInterrupt:
        print("\n⏹️ 测试被中断")
        test.cleanup()
    except Exception as e:
        print(f"\n❌ 测试失败: {e}")

if __name__ == "__main__":
    main()
