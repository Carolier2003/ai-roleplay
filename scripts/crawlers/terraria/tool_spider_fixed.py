#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
泰拉瑞亚工具信息爬虫 - 修复版
完全复制独立测试脚本的成功逻辑
"""

import requests
import time
import random
import re
import json
from urllib.parse import urljoin, unquote
from bs4 import BeautifulSoup
from cf_bypass import cf_make_request

class TerrariaToolSpiderFixed:
    """泰拉瑞亚工具信息爬虫 - 修复版"""
    
    def __init__(self):
        # 完全复制独立测试脚本的成功配置
        self.base_url = "https://terraria.wiki.gg"
        self.main_url = "https://terraria.wiki.gg/zh/wiki/工具"
        self.session = requests.Session()
        self.headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
            'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
            'Accept-Encoding': 'gzip, deflate, br',
            'Connection': 'keep-alive',
            'Upgrade-Insecure-Requests': '1',
        }
        self.tool_links = []
        self.tools_data = []
        self.failed_urls = []
        
    def make_request(self, url, max_retries=5):
        """改进的请求方法，确保获取完整页面"""
        print(f"🌐 请求页面: {url}")
        
        # 针对工具页面，期望的完整页面大小（根据调试结果调整）
        min_expected_size = 80000  # 降低期望，大多数工具页面可能没有300K
        
        for attempt in range(max_retries):
            try:
                # 每次重试使用新的session
                if attempt > 0:
                    self.session = requests.Session()
                    print(f"🔄 第{attempt + 1}次尝试（新session）...")
                    time.sleep(random.uniform(2, 4))  # 增加延时
                
                response = cf_make_request(url, self.headers, self.session, 30, 3)
                
                if response and response.status_code == 200:
                    print(f"✅ CF绕过成功，大小: {len(response.content)} bytes")
                    
                    try:
                        html_content = response.text
                        print(f"📝 HTML内容大小: {len(html_content)} 字符")
                        
                        # 多种验证方法
                        is_complete = False
                        
                        # 方法1：大小验证（主要工具页面应该较大）
                        if len(html_content) > min_expected_size:
                            is_complete = True
                            print(f"✅ 大小验证通过")
                        
                        # 方法2：关键标签验证
                        if '<h1' in html_content and '</html>' in html_content:
                            if not is_complete and len(html_content) > 20000:
                                is_complete = True
                                print(f"✅ 结构验证通过（较小但完整）")
                        
                        # 方法3：内容质量验证
                        if 'firstHeading' in html_content or 'mw-page-title-main' in html_content:
                            if not is_complete and len(html_content) > 15000:
                                is_complete = True
                                print(f"✅ 内容验证通过（包含标题）")
                        
                        if is_complete:
                            return html_content
                        else:
                            print(f"⚠️ 页面内容不完整，重试...")
                            
                    except Exception as e:
                        print(f"❌ HTML处理失败: {e}")
                else:
                    print(f"❌ 请求失败，状态码: {response.status_code if response else 'None'}")
                    
            except Exception as e:
                print(f"❌ 请求异常: {e}")
        
        print(f"❌ {max_retries}次尝试均失败")
        return None
    
    def extract_tool_links(self, html_content):
        """提取工具链接"""
        print("🔍 提取工具链接...")
        
        soup = BeautifulSoup(html_content, 'html.parser')
        
        # 查找所有 itemlist terraria 区域
        itemlist_divs = soup.find_all('div', class_='itemlist terraria')
        print(f"📋 找到 {len(itemlist_divs)} 个工具列表区域")
        
        tool_links = set()
        for itemlist_div in itemlist_divs:
            li_elements = itemlist_div.find_all('li')
            print(f"  📊 当前区域有 {len(li_elements)} 个工具项")
            
            for li in li_elements:
                # 取第一个a标签
                a_tag = li.find('a', href=re.compile(r'/zh/wiki/'))
                if a_tag and a_tag.get('href'):
                    href = a_tag.get('href')
                    title = a_tag.get('title', '').strip()
                    
                    # 补全绝对路径并解码URL
                    full_url = urljoin(self.base_url, href)
                    # 对URL进行解码以处理中文字符
                    decoded_url = unquote(full_url)
                    tool_links.add(decoded_url)
                    
                    if title:
                        print(f"    ✅ 发现工具: {title}")
        
        self.tool_links = list(tool_links)
        print(f"📊 总计发现 {len(self.tool_links)} 个不重复的工具链接")
        return self.tool_links
    
    def run_crawler(self):
        """运行爬虫主流程"""
        print("🚀 泰拉瑞亚工具信息爬虫启动（修复版）")
        print("=" * 50)
        
        # 1. 获取主页面
        main_html = self.make_request(self.main_url)
        if not main_html:
            print("❌ 无法获取主页面，爬虫终止")
            self.failed_urls.append(self.main_url)
            self.save_failed_urls()
            return False
        
        # 2. 提取工具链接
        if not self.extract_tool_links(main_html):
            print("❌ 无法提取工具链接，爬虫终止")
            self.failed_urls.append(self.main_url)
            self.save_failed_urls()
            return False
        
        # 3. 爬取所有工具
        print(f"\n🚀 开始爬取全部 {len(self.tool_links)} 个工具...")
        
        for i, tool_url in enumerate(self.tool_links):
            tool_data = self.crawl_single_tool(tool_url, i+1, len(self.tool_links))
            if tool_data:
                self.tools_data.append(tool_data)
                print(f"    ✅ 已采集 {len(self.tools_data)} 个工具：{tool_data.get('工具名', '未知')}")
            else:
                print(f"    ❌ 工具 {i+1} 爬取失败")
            
            # 延时（避免请求过快）
            time.sleep(random.uniform(0.3, 0.8))
            
            # 每10个工具显示进度
            if (i + 1) % 10 == 0:
                progress = (i + 1) / len(self.tool_links) * 100
                print(f"📊 进度: {i+1}/{len(self.tool_links)} ({progress:.1f}%)")
                # 保存中间结果
                self.save_results()
        
        # 4. 保存测试结果
        self.save_results()
        self.save_failed_urls()
        
        print(f"\n🎉 测试完成！")
        print(f"✅ 成功爬取: {len(self.tools_data)} 个工具")
        print(f"❌ 失败: {len(self.failed_urls)} 个")
        
        return True
    
    def crawl_single_tool(self, tool_url, current, total):
        """爬取单个工具信息"""
        print(f"🔧 [{current}/{total}] 爬取工具: {tool_url}")
        
        # 使用改进的请求方法（内置重试机制）
        html_content = self.make_request(tool_url, max_retries=5)
        
        if not html_content:
            print(f"    ❌ 页面获取失败")
            self.failed_urls.append(tool_url)
            return None
        
        soup = BeautifulSoup(html_content, 'html.parser')
        
        # 多种方法提取工具名
        tool_name = "未知工具"
        
        # 方法1：h1.firstHeading
        title_tag = soup.find('h1', class_='firstHeading')
        if title_tag:
            tool_name = title_tag.get_text().strip()
        else:
            # 方法2：任何h1标签
            title_tag = soup.find('h1')
            if title_tag:
                tool_name = title_tag.get_text().strip()
            else:
                # 方法3：从页面title提取
                title_tag = soup.find('title')
                if title_tag:
                    page_title = title_tag.get_text().strip()
                    if " - " in page_title:
                        tool_name = page_title.split(" - ")[0].strip()
                    else:
                        # 方法4：从URL提取
                        url_parts = tool_url.split('/')
                        if url_parts:
                            from urllib.parse import unquote
                            tool_name = unquote(url_parts[-1]).replace('_', ' ')
        
        print(f"    📝 工具名: {tool_name}")
        
        # 提取基本属性
        attributes = self.extract_tool_attributes(soup)
        
        # 提取工具能力
        tool_power = self.extract_tool_power(soup)
        
        # 提取配方
        recipes = self.extract_tool_recipes(soup)
        
        tool_data = {
            "工具名": tool_name,
            "属性": attributes,
            "工具能力": tool_power,
            "配方表": recipes,
            "url": tool_url
        }
        
        return tool_data
    
    def extract_tool_attributes(self, soup):
        """提取工具属性"""
        attributes = {}
        
        # 查找stat表格
        stat_tables = soup.find_all('table', class_='stat')
        for table in stat_tables:
            rows = table.find_all('tr')
            for row in rows:
                th = row.find('th')
                td = row.find('td')
                if th and td:
                    key = th.get_text(strip=True)
                    value = td.get_text(strip=True)
                    if key and value:
                        attributes[key] = value
        
        return attributes
    
    def extract_tool_power(self, soup):
        """提取工具能力"""
        tool_power = {}
        
        # 查找toolpower相关信息
        toolpower_sections = soup.find_all('div', class_='toolpower')
        for section in toolpower_sections:
            # 这里可以根据实际页面结构调整
            pass
        
        return tool_power
    
    def extract_tool_recipes(self, soup):
        """提取制作配方"""
        recipes = []
        
        # 查找配方表格
        recipe_tables = soup.find_all('table', class_=lambda x: x and 'recipes' in ' '.join(x) if x else False)
        for table in recipe_tables:
            tbody = table.find('tbody')
            if tbody:
                rows = tbody.find_all('tr')
                for row in rows:
                    tds = row.find_all('td')
                    if len(tds) >= 3:
                        recipe = {
                            "产物": tds[0].get_text(strip=True),
                            "材料": [li.get_text(strip=True) for li in tds[1].find_all('li')] if tds[1].find_all('li') else [tds[1].get_text(strip=True)],
                            "制作站": tds[2].get_text(strip=True)
                        }
                        recipes.append(recipe)
        
        return recipes
    
    def save_results(self):
        """保存结果"""
        output_file = "../../data/terraria/terraria_tools_fixed.json"
        
        with open(output_file, 'w', encoding='utf-8') as f:
            for tool_data in self.tools_data:
                f.write(json.dumps(tool_data, ensure_ascii=False, separators=(',', ':')) + '\n')
        
        print(f"📝 结果已保存到: {output_file}")
    
    def save_failed_urls(self):
        """保存失败的URL"""
        if self.failed_urls:
            failed_file = "../../data/terraria/failed_fixed.txt"
            with open(failed_file, 'w', encoding='utf-8') as f:
                for url in self.failed_urls:
                    f.write(url + '\n')
            print(f"⚠️ 保存 {len(self.failed_urls)} 个失败URL到: {failed_file}")

def main():
    """主函数"""
    spider = TerrariaToolSpiderFixed()
    spider.run_crawler()

if __name__ == "__main__":
    main()
