#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试扩展爬虫
先测试少量页面确保工作正常
"""

from expanded_batch_crawler import ExpandedBatchCrawler

class TestExpandedCrawler(ExpandedBatchCrawler):
    """测试版扩展爬虫"""
    
    def __init__(self):
        super().__init__()
        self.output_dir = "test_expanded_data"
        
        # 只测试前10个页面
        self.pages_to_crawl = self.pages_to_crawl[:10]

def main():
    """测试主函数"""
    print("🧪 测试扩展批量爬虫 - 前10页")
    print("=" * 50)
    
    crawler = TestExpandedCrawler()
    
    try:
        print(f"🚀 测试爬取 {len(crawler.pages_to_crawl)} 个页面...")
        
        # 爬取数据
        result = crawler.crawl_all()
        
        # 显示结果
        print(f"\n📊 测试结果:")
        print(f"总页面数: {result['total']}")
        print(f"成功爬取: {result['success']}")
        print(f"爬取失败: {result['failed']}")
        print(f"成功率: {result['success']/result['total']*100:.1f}%")
        
        if result['success'] > 0:
            # 生成导入数据
            import_file = crawler.generate_import_data()
            print(f"✅ 测试成功！生成文件: {import_file}")
            print(f"🚀 可以运行完整版: python3 expanded_batch_crawler.py")
        else:
            print(f"❌ 测试失败")
    
    except Exception as e:
        print(f"❌ 测试失败: {e}")

if __name__ == "__main__":
    main()
