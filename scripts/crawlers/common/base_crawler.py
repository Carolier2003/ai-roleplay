"""
通用爬虫基类
为不同角色的爬虫提供统一的接口和公共功能
"""

from abc import ABC, abstractmethod
from typing import List, Dict, Any
import requests
from bs4 import BeautifulSoup
import json
import os
import time

class BaseCrawler(ABC):
    """爬虫基类"""
    
    def __init__(self, character_name: str):
        self.character_name = character_name
        self.headers = {
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36'
        }
        self.output_dir = f"../../data/{character_name}/raw"
        os.makedirs(self.output_dir, exist_ok=True)
    
    @abstractmethod
    def get_pages_to_crawl(self) -> List[Dict[str, Any]]:
        """返回要爬取的页面列表"""
        pass
    
    @abstractmethod
    def extract_content(self, soup: BeautifulSoup, page_info: Dict) -> str:
        """从页面提取内容"""
        pass
    
    def crawl_all(self):
        """爬取所有页面"""
        pages = self.get_pages_to_crawl()
        # 实现通用爬取逻辑
        pass
