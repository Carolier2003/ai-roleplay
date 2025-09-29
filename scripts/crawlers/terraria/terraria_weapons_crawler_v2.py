#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
泰拉瑞亚武器爬虫 v2.0
按照用户指定的HTML结构和流程重新编写

入口: https://terraria.wiki.gg/zh/wiki/武器
输出: terraria_weapons.json (jsonlines格式)
     failed.txt (失败的URL列表)

Author: Assistant
Date: 2025-09-25
"""

import requests
from bs4 import BeautifulSoup
import json
import time
import random
import re
import os
from urllib.parse import urljoin
from typing import List, Dict, Optional
from cf_bypass import cf_make_request


class TerrariaWeaponsCrawler:
    """泰拉瑞亚武器爬虫"""
    
    def __init__(self):
        # 基础配置
        self.base_url = "https://terraria.wiki.gg"
        self.entry_url = "https://terraria.wiki.gg/zh/wiki/武器"
        self.output_dir = "../../data/terraria"
        
        # 确保输出目录存在
        os.makedirs(self.output_dir, exist_ok=True)
        
        # 输出文件路径
        self.weapons_file = os.path.join(self.output_dir, "terraria_weapons.json")
        self.failed_file = os.path.join(self.output_dir, "failed.txt")
        
        # 请求配置
        self.headers = {
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8',
            'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
        }
        
        self.session = requests.Session()
        self.session.headers.update(self.headers)
        
        # 统计信息
        self.total_weapons = 0
        self.success_count = 0
        self.failed_urls = []
    
    def sleep_random(self):
        """随机休眠 0.3~0.8 秒"""
        sleep_time = random.uniform(0.3, 0.8)
        time.sleep(sleep_time)
    
    def make_request(self, url: str) -> Optional[requests.Response]:
        """发送请求，优先使用CF绕过"""
        try:
            # 使用CF绕过功能
            response = cf_make_request(
                url=url,
                headers=self.headers,
                session=self.session,
                timeout=30,
                max_retries=2
            )
            
            if response is not None:
                return response
            
            # 备用：传统请求
            print(f"    ⚠️ CF绕过失败，使用传统方式: {url}")
            response = self.session.get(url, timeout=30)
            response.raise_for_status()
            return response
            
        except Exception as e:
            print(f"    ❌ 请求失败: {e}")
            return None
    
    def get_weapon_urls_from_main_page(self) -> List[str]:
        """
        第一步：解析主页面，获取所有武器子页面URL
        2.1 定位所有 class 包含 infocard 的 div
        2.2 在每个 infocard 里找到所有 <a href="/zh/wiki/…… 且 title 存在的链接
        2.3 过滤掉图片本身（src 结尾为 .png/.jpg/.gif）的 <a>，仅保留武器词条链接
        2.4 将相对路径补全为绝对路径
        2.5 去重后得到「武器子页面 URL 列表」
        """
        print("🔍 第一步：解析主页面，获取武器链接...")
        print(f"    访问入口: {self.entry_url}")
        
        response = self.make_request(self.entry_url)
        if not response:
            print("❌ 无法访问主页面")
            return []
        
        print(f"    ✅ 主页面获取成功，大小: {len(response.text)} 字符")
        
        soup = BeautifulSoup(response.text, 'html.parser')
        weapon_urls = set()  # 使用set自动去重
        
        # 2.1 定位所有 class 包含 infocard 的 div
        infocard_divs = soup.find_all('div', class_=lambda x: x and 'infocard' in x if x else False)
        print(f"    🎯 找到 {len(infocard_divs)} 个 infocard 容器")
        
        for i, div in enumerate(infocard_divs, 1):
            print(f"    📋 处理 infocard {i}/{len(infocard_divs)}")
            
            # 2.2 在每个 infocard 里找到 itemlist 中的武器链接
            itemlists = div.find_all('div', class_='itemlist')
            
            for itemlist in itemlists:
                # 查找 ul > li 结构中的武器链接
                li_items = itemlist.find_all('li')
                
                for li in li_items:
                    # 在每个 li 中查找 <a href="/zh/wiki/…… 且 title 存在的链接
                    links = li.find_all('a', href=True, title=True)
                    
                    for link in links:
                        href = link.get('href', '')
                        title = link.get('title', '')
                        
                        # 检查是否是wiki链接
                        if href.startswith('/zh/wiki/'):
                            # 2.3 过滤掉图片本身（检查是否指向图片文件）
                            if not self.is_image_link(link):
                                # 2.4 将相对路径补全为绝对路径
                                absolute_url = urljoin(self.base_url, href)
                                weapon_urls.add(absolute_url)
                                print(f"        ✅ 武器: {title}")
            
            # 备用方法：如果itemlist方法失败，直接在infocard中查找所有武器链接
            if not weapon_urls:
                print(f"    ⚠️ itemlist方法未找到武器，使用备用方法")
                links = div.find_all('a', href=True, title=True)
                
                for link in links:
                    href = link.get('href', '')
                    title = link.get('title', '')
                    
                    # 检查是否是wiki链接
                    if href.startswith('/zh/wiki/'):
                        # 2.3 过滤掉图片本身
                        if not self.is_image_link(link):
                            # 2.4 将相对路径补全为绝对路径
                            absolute_url = urljoin(self.base_url, href)
                            weapon_urls.add(absolute_url)
                            print(f"        ✅ 武器: {title}")
        
        # 2.5 去重后得到「武器子页面 URL 列表」
        weapon_url_list = list(weapon_urls)
        self.total_weapons = len(weapon_url_list)
        
        print(f"🎉 主页面解析完成，找到 {self.total_weapons} 个武器链接")
        return weapon_url_list
    
    def is_image_link(self, link) -> bool:
        """判断链接是否指向图片"""
        href = link.get('href', '')
        
        # 检查href是否以图片扩展名结尾
        image_extensions = ['.png', '.jpg', '.jpeg', '.gif', '.webp']
        if any(href.lower().endswith(ext) for ext in image_extensions):
            return True
        
        # 检查链接内是否只有img标签（图片链接）
        img_tags = link.find_all('img')
        text_content = link.get_text(strip=True)
        
        # 如果只有图片没有文字，可能是图片链接
        if img_tags and not text_content:
            return True
        
        return False
    
    def parse_weapon_page(self, url: str) -> Optional[Dict]:
        """
        第二步：解析单个武器页面
        3.1 访问武器页面
        3.2 解析两个区域：
           A. <div class="infobox item"> 里的属性表格
           B. <table class="terraria cellborder recipes"> 里的合成表
        """
        response = self.make_request(url)
        if not response:
            return None
        
        soup = BeautifulSoup(response.text, 'html.parser')
        
        # 获取武器名（页面标题）
        weapon_name = self.extract_weapon_name(soup)
        if not weapon_name:
            print(f"    ⚠️ 无法获取武器名称")
            return None
        
        # A. 解析属性表格
        attributes = self.extract_attributes(soup)
        
        # B. 解析合成表
        recipes = self.extract_recipes(soup)
        
        # 3.5 合并数据
        weapon_data = {
            "武器名": weapon_name,
            "属性": attributes,
            "合成表": recipes,
            "url": url
        }
        
        return weapon_data
    
    def extract_weapon_name(self, soup: BeautifulSoup) -> Optional[str]:
        """提取武器名称（页面标题）"""
        try:
            # 尝试从页面标题获取
            title_tag = soup.find('title')
            if title_tag:
                title_text = title_tag.get_text()
                # 移除" - 官方中文 Terraria Wiki"等后缀
                weapon_name = title_text.split(' - ')[0].strip()
                return weapon_name
            
            # 备用：从h1标签获取
            h1_tag = soup.find('h1')
            if h1_tag:
                return h1_tag.get_text(strip=True)
                
        except Exception as e:
            print(f"    提取武器名失败: {e}")
        
        return None
    
    def extract_attributes(self, soup: BeautifulSoup) -> Dict:
        """
        3.3 解析 <div class="infobox item"> 里的属性表格
        将每个 <tr> 提取为 dict：
        {"属性名": "属性值"}
        """
        attributes = {}
        
        try:
            # 查找 <div class="infobox item">
            infobox = soup.find('div', class_=['infobox', 'item'])
            if not infobox:
                # 备用查找方式
                infobox = soup.find('div', class_=lambda x: x and 'infobox' in ' '.join(x) if x else False)
            
            if not infobox:
                print(f"    ⚠️ 未找到 infobox 属性区域")
                return attributes
            
            # 查找表格行
            rows = infobox.find_all('tr')
            
            for row in rows:
                # 查找属性名和属性值
                cells = row.find_all(['th', 'td'])
                
                if len(cells) >= 2:
                    # 第一个单元格作为属性名
                    attr_name = self.clean_text(cells[0].get_text())
                    # 第二个单元格作为属性值
                    attr_value = self.clean_text(cells[1].get_text())
                    
                    if attr_name and attr_value:
                        attributes[attr_name] = attr_value
                elif len(cells) == 1:
                    # 单个单元格的情况，可能是标题行
                    cell_text = self.clean_text(cells[0].get_text())
                    if cell_text and '：' in cell_text:
                        # 处理 "属性名：属性值" 格式
                        parts = cell_text.split('：', 1)
                        if len(parts) == 2:
                            attributes[parts[0].strip()] = parts[1].strip()
            
            print(f"    📊 提取到 {len(attributes)} 个属性")
            
        except Exception as e:
            print(f"    ❌ 属性提取失败: {e}")
        
        return attributes
    
    def extract_recipes(self, soup: BeautifulSoup) -> List[Dict]:
        """
        3.4 解析所有 <table class="terraria cellborder recipes"> 里的合成表
        将每行提取为 list[dict]：
        {"产物": 产物名, "材料": [材料1, 材料2, ...], "制作站": 制作站名}
        """
        recipes = []
        
        try:
            # 查找合成表格
            recipe_tables = soup.find_all('table', class_=lambda x: x and all(cls in x for cls in ['terraria', 'cellborder', 'recipes']) if x else False)
            
            if not recipe_tables:
                print(f"    ⚠️ 未找到合成表格")
                return recipes
            
            print(f"    🔨 找到 {len(recipe_tables)} 个合成表格")
            
            for table_idx, table in enumerate(recipe_tables, 1):
                print(f"    处理表格 {table_idx}/{len(recipe_tables)}")
                
                # 查找表格主体
                tbody = table.find('tbody') or table
                rows = tbody.find_all('tr')
                
                # 跳过表头行
                data_rows = []
                for row in rows:
                    # 如果行中包含th标签，通常是表头
                    if not row.find('th'):
                        data_rows.append(row)
                
                for row in data_rows:
                    cells = row.find_all('td')
                    
                    if len(cells) >= 3:
                        # 标准格式：产物 | 材料 | 制作站
                        product = self.extract_item_name(cells[0])
                        materials = self.extract_materials(cells[1])
                        station = self.extract_station_name(cells[2])
                        
                        if product:  # 至少要有产物名
                            recipe = {
                                "产物": product,
                                "材料": materials,
                                "制作站": station
                            }
                            recipes.append(recipe)
                            print(f"      配方: {product} <- {materials} @ {station}")
            
            print(f"    📝 提取到 {len(recipes)} 个配方")
            
        except Exception as e:
            print(f"    ❌ 配方提取失败: {e}")
        
        return recipes
    
    def extract_item_name(self, cell) -> str:
        """从单元格提取物品名称"""
        try:
            # 优先从title属性获取
            link = cell.find('a', title=True)
            if link:
                return link.get('title', '').strip()
            
            # 备用：从链接文本获取
            link = cell.find('a')
            if link:
                return link.get_text(strip=True)
            
            # 最后：直接从单元格文本获取
            return self.clean_text(cell.get_text())
            
        except:
            return ""
    
    def extract_materials(self, cell) -> List[str]:
        """从材料单元格提取材料列表"""
        materials = []
        
        try:
            # 查找所有材料项（通常在li或链接中）
            items = cell.find_all('li')
            if items:
                for item in items:
                    material_name = self.extract_item_name(item)
                    if material_name:
                        materials.append(material_name)
            else:
                # 备用：查找所有链接
                links = cell.find_all('a', title=True)
                for link in links:
                    material_name = link.get('title', '').strip()
                    if material_name:
                        materials.append(material_name)
            
            # 如果没找到结构化数据，尝试文本解析
            if not materials:
                text = self.clean_text(cell.get_text())
                if text:
                    materials.append(text)
                    
        except:
            pass
        
        return materials
    
    def extract_station_name(self, cell) -> str:
        """从制作站单元格提取制作站名称"""
        try:
            # 优先从链接获取
            link = cell.find('a', title=True)
            if link:
                return link.get('title', '').strip()
            
            # 备用：从文本获取
            return self.clean_text(cell.get_text())
            
        except:
            return ""
    
    def clean_text(self, text: str) -> str:
        """清理文本，去掉HTML标签和多余空白"""
        if not text:
            return ""
        
        # 去掉多余的空白字符
        text = re.sub(r'\s+', ' ', text).strip()
        
        # 去掉一些wiki特有的标记
        text = re.sub(r'\([^)]*版[^)]*\)', '', text)  # 移除版本标记
        text = re.sub(r'\[[^\]]*\]', '', text)       # 移除方括号内容
        
        return text.strip()
    
    def save_weapon_data(self, weapon_data: Dict):
        """保存单个武器数据到文件（jsonlines格式）"""
        try:
            with open(self.weapons_file, 'a', encoding='utf-8') as f:
                json.dump(weapon_data, f, ensure_ascii=False)
                f.write('\n')
        except Exception as e:
            print(f"    ❌ 保存数据失败: {e}")
    
    def save_failed_url(self, url: str, error: str = ""):
        """保存失败的URL到文件"""
        try:
            with open(self.failed_file, 'a', encoding='utf-8') as f:
                f.write(f"{url}\t{error}\n")
        except Exception as e:
            print(f"    ❌ 保存失败URL失败: {e}")
    
    def run(self):
        """主执行流程"""
        print("🚀 泰拉瑞亚武器爬虫 v2.0 启动")
        print("=" * 80)
        
        # 清理之前的输出文件
        for file_path in [self.weapons_file, self.failed_file]:
            if os.path.exists(file_path):
                os.remove(file_path)
        
        # 第一步：获取所有武器链接
        weapon_urls = self.get_weapon_urls_from_main_page()
        if not weapon_urls:
            print("❌ 未能获取武器链接，退出")
            return
        
        print(f"\n🎯 第二步：开始爬取 {len(weapon_urls)} 个武器详情")
        print("=" * 80)
        
        # 第二步：逐个爬取武器详情
        for i, url in enumerate(weapon_urls, 1):
            try:
                print(f"\n🔧 [{i}/{len(weapon_urls)}] 爬取武器: {url.split('/')[-1]}")
                
                # 解析武器页面
                weapon_data = self.parse_weapon_page(url)
                
                if weapon_data:
                    # 保存数据
                    self.save_weapon_data(weapon_data)
                    self.success_count += 1
                    
                    print(f"    ✅ 已采集 {self.success_count}/{self.total_weapons} 件武器：{weapon_data['武器名']}")
                else:
                    # 记录失败
                    self.save_failed_url(url, "解析失败")
                    print(f"    ❌ 武器解析失败")
                
                # 随机休眠
                self.sleep_random()
                
            except Exception as e:
                # 5.3 捕获所有异常，记录到failed.txt，继续执行
                error_msg = str(e)
                self.save_failed_url(url, error_msg)
                print(f"    ❌ 处理异常: {error_msg}")
                continue
        
        # 第三步：输出统计信息
        print("\n" + "=" * 80)
        print("🎉 爬取完成！")
        print(f"📊 统计信息:")
        print(f"    总武器数量: {self.total_weapons}")
        print(f"    成功采集: {self.success_count}")
        print(f"    失败数量: {self.total_weapons - self.success_count}")
        print(f"📁 输出文件:")
        print(f"    武器数据: {self.weapons_file}")
        print(f"    失败记录: {self.failed_file}")


def main():
    """主函数入口"""
    crawler = TerrariaWeaponsCrawler()
    crawler.run()


if __name__ == "__main__":
    main()
