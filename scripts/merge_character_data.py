#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
角色数据合并脚本
合并不同版本的数据，去除重复，保留所有唯一内容
"""

import json
import os
from collections import defaultdict
from datetime import datetime

class CharacterDataMerger:
    def __init__(self, backup_dir="data_original_backup"):
        self.backup_dir = backup_dir
        self.merged_data = {}
        
    def merge_terraria_data(self):
        """合并泰拉瑞亚所有版本数据"""
        print("🎯 合并泰拉瑞亚数据...")
        
        # 定义要合并的文件
        terraria_files = [
            ("terraria_comprehensive/terraria_comprehensive_weapons.json", "comprehensive"),
            ("terraria_fandom_comprehensive/terraria_comprehensive_weapons.json", "fandom_comprehensive"), 
            ("terraria_fandom_systematic/terraria_fandom_systematic_weapons.json", "fandom_systematic"),
            ("terraria_fixed/terraria_fixed_weapons.json", "fixed"),
            ("terraria_precise/terraria_precise_weapons.json", "precise")
        ]
        
        all_weapons = []
        seen_titles = set()
        source_stats = defaultdict(int)
        
        for file_path, source_label in terraria_files:
            full_path = os.path.join(self.backup_dir, file_path)
            if os.path.exists(full_path):
                try:
                    with open(full_path, 'r', encoding='utf-8') as f:
                        data = json.load(f)
                        
                    for item in data:
                        title = item.get('title', item.get('name', ''))
                        if title and title not in seen_titles:
                            # 添加合并来源信息
                            item['merged_from'] = source_label
                            item['merged_at'] = datetime.now().isoformat()
                            all_weapons.append(item)
                            seen_titles.add(title)
                            source_stats[source_label] += 1
                        
                    print(f"   ✅ {source_label}: {len(data)}条原始 -> {source_stats[source_label]}条唯一")
                    
                except Exception as e:
                    print(f"   ❌ {source_label}: 读取失败 - {e}")
        
        print(f"🎯 泰拉瑞亚合并结果: {len(all_weapons)}条唯一武器数据")
        return all_weapons, source_stats
    
    def merge_conan_data(self):
        """合并柯南所有版本数据"""
        print("\n🎯 合并柯南数据...")
        
        all_knowledge = []
        seen_titles = set()
        
        # 合并主要知识文件
        main_files = [
            ("conan/conan_comprehensive_knowledge.json", "comprehensive"),
            ("conan_expanded/conan_expanded_knowledge.json", "expanded")
        ]
        
        source_stats = defaultdict(int)
        
        for file_path, source_label in main_files:
            full_path = os.path.join(self.backup_dir, file_path)
            if os.path.exists(full_path):
                try:
                    with open(full_path, 'r', encoding='utf-8') as f:
                        data = json.load(f)
                        
                    for item in data:
                        title = item.get('title', item.get('name', ''))
                        if title and title not in seen_titles:
                            item['merged_from'] = source_label
                            item['merged_at'] = datetime.now().isoformat()
                            all_knowledge.append(item)
                            seen_titles.add(title)
                            source_stats[source_label] += 1
                            
                    print(f"   ✅ {source_label}: {len(data)}条原始 -> {source_stats[source_label]}条唯一")
                    
                except Exception as e:
                    print(f"   ❌ {source_label}: 读取失败 - {e}")
        
        # 添加单独的JSON文件 (conan_expanded中的额外文件)
        expanded_dir = os.path.join(self.backup_dir, "conan_expanded")
        if os.path.exists(expanded_dir):
            individual_files = [f for f in os.listdir(expanded_dir) 
                              if f.endswith('.json') and f not in ['conan_expanded_knowledge.json', 'crawl_progress.json', 'crawl_report.json']]
            
            for filename in individual_files:
                file_path = os.path.join(expanded_dir, filename)
                try:
                    with open(file_path, 'r', encoding='utf-8') as f:
                        data = json.load(f)
                        
                    # 处理单个文件的数据
                    if isinstance(data, dict):
                        title = data.get('title', data.get('name', filename.replace('.json', '')))
                        if title not in seen_titles:
                            data['merged_from'] = 'expanded_individual'
                            data['merged_at'] = datetime.now().isoformat()
                            all_knowledge.append(data)
                            seen_titles.add(title)
                            source_stats['expanded_individual'] += 1
                            
                except Exception as e:
                    continue
            
            print(f"   ✅ 额外文件: {source_stats['expanded_individual']}条唯一数据")
        
        print(f"🎯 柯南合并结果: {len(all_knowledge)}条唯一知识数据")
        return all_knowledge, source_stats
    
    def save_merged_data(self, output_dir="data_merged"):
        """保存合并后的数据"""
        os.makedirs(output_dir, exist_ok=True)
        
        # 合并泰拉瑞亚数据
        terraria_data, terraria_stats = self.merge_terraria_data()
        terraria_output = os.path.join(output_dir, "terraria")
        os.makedirs(terraria_output, exist_ok=True)
        
        with open(os.path.join(terraria_output, "terraria_merged_weapons.json"), 'w', encoding='utf-8') as f:
            json.dump(terraria_data, f, ensure_ascii=False, indent=2)
            
        # 保存合并报告
        terraria_report = {
            "merge_date": datetime.now().isoformat(),
            "total_records": len(terraria_data),
            "source_statistics": dict(terraria_stats),
            "merged_files": [
                "terraria_comprehensive", "terraria_fandom_comprehensive", 
                "terraria_fandom_systematic", "terraria_fixed", "terraria_precise"
            ]
        }
        
        with open(os.path.join(terraria_output, "merge_report.json"), 'w', encoding='utf-8') as f:
            json.dump(terraria_report, f, ensure_ascii=False, indent=2)
        
        # 合并柯南数据
        conan_data, conan_stats = self.merge_conan_data()
        conan_output = os.path.join(output_dir, "conan")
        os.makedirs(conan_output, exist_ok=True)
        
        with open(os.path.join(conan_output, "conan_merged_knowledge.json"), 'w', encoding='utf-8') as f:
            json.dump(conan_data, f, ensure_ascii=False, indent=2)
            
        # 保存合并报告
        conan_report = {
            "merge_date": datetime.now().isoformat(),
            "total_records": len(conan_data),
            "source_statistics": dict(conan_stats),
            "merged_files": ["conan_comprehensive", "conan_expanded"]
        }
        
        with open(os.path.join(conan_output, "merge_report.json"), 'w', encoding='utf-8') as f:
            json.dump(conan_report, f, ensure_ascii=False, indent=2)
        
        # 复制其他角色数据 
        other_characters = ["einstein", "socrates", "harry_potter"]
        for character in other_characters:
            src_dir = os.path.join(self.backup_dir, character)
            if os.path.exists(src_dir):
                import shutil
                dst_dir = os.path.join(output_dir, character)
                shutil.copytree(src_dir, dst_dir, dirs_exist_ok=True)
                print(f"✅ 复制 {character} 数据")
        
        return {
            "terraria": {"count": len(terraria_data), "stats": terraria_stats},
            "conan": {"count": len(conan_data), "stats": conan_stats}
        }

if __name__ == "__main__":
    print("🚀 开始数据合并处理...")
    
    merger = CharacterDataMerger()
    results = merger.save_merged_data()
    
    print(f"\n🎉 数据合并完成！")
    print(f"📊 泰拉瑞亚: {results['terraria']['count']}条唯一数据")
    print(f"📊 柯南: {results['conan']['count']}条唯一数据")
    print(f"📁 合并数据保存在: data_merged/")
