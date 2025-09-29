#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
环境检查脚本 - 验证部署环境是否就绪
"""

import os
import sys
import requests
import json
from pathlib import Path

def check_python_version():
    """检查Python版本"""
    version = sys.version_info
    if version.major < 3 or (version.major == 3 and version.minor < 8):
        print(f"❌ Python版本过低: {version.major}.{version.minor}, 需要3.8+")
        return False
    print(f"✅ Python版本: {version.major}.{version.minor}.{version.micro}")
    return True

def check_dependencies():
    """检查依赖包"""
    required = ['requests']
    missing = []
    
    for package in required:
        try:
            __import__(package)
            print(f"✅ {package}: 已安装")
        except ImportError:
            missing.append(package)
            print(f"❌ {package}: 缺失")
    
    if missing:
        print(f"\n📦 安装缺失依赖: pip3 install {' '.join(missing)}")
        return False
    return True

def check_project_structure():
    """检查项目结构"""
    required_paths = [
        "scripts/data",
        "backend/roleplay-api",
        "deploy_knowledge_base.py"
    ]
    
    for path in required_paths:
        if Path(path).exists():
            print(f"✅ {path}: 存在")
        else:
            print(f"❌ {path}: 缺失")
            return False
    return True

def check_data_integrity():
    """检查数据完整性"""
    data_dir = Path("scripts/data")
    expected_characters = ["harry_potter", "socrates", "einstein", "conan", "terraria"]
    
    for char in expected_characters:
        char_path = data_dir / char
        if char_path.exists():
            if char in ["terraria", "conan"]:
                # 检查合并后的数据文件
                if char == "terraria":
                    data_file = char_path / "terraria_merged_weapons.json"
                else:
                    data_file = char_path / "conan_merged_knowledge.json"
                
                if data_file.exists():
                    try:
                        with open(data_file, 'r', encoding='utf-8') as f:
                            data = json.load(f)
                        count = len(data) if isinstance(data, list) else 1
                        print(f"✅ {char}: {count}条记录")
                    except Exception as e:
                        print(f"❌ {char}: 数据文件损坏 - {e}")
                        return False
                else:
                    print(f"❌ {char}: 缺少主数据文件")
                    return False
            else:
                # 检查多文件数据
                json_files = list(char_path.glob("*.json"))
                json_files = [f for f in json_files if not any(x in f.name.lower() for x in ['report', 'progress'])]
                if json_files:
                    print(f"✅ {char}: {len(json_files)}个数据文件")
                else:
                    print(f"❌ {char}: 无数据文件")
                    return False
        else:
            print(f"❌ {char}: 目录缺失")
            return False
    
    return True

def check_backend_service():
    """检查后端服务"""
    try:
        response = requests.get("http://localhost:18080/api/health", timeout=5)
        if response.status_code == 200:
            health_data = response.json()
            print(f"✅ 后端服务: {health_data.get('status', 'UP')}")
            return True
        else:
            print(f"❌ 后端服务: HTTP {response.status_code}")
            return False
    except Exception as e:
        print(f"❌ 后端服务: 无法连接 - {e}")
        print("💡 启动命令:")
        print("   cd backend/roleplay-api")
        print("   java -jar target/roleplay-api-0.0.1-SNAPSHOT.jar --server.port=18080")
        return False

def check_database_connection():
    """检查数据库连接"""
    try:
        # 通过后端API检查数据库
        response = requests.get("http://localhost:18080/api/debug/database", timeout=5)
        if response.status_code == 200:
            print("✅ 数据库连接: 正常")
            return True
        else:
            print("⚠️ 数据库连接: 无法验证（API不存在）")
            return True  # 不强制要求
    except Exception:
        print("⚠️ 数据库连接: 无法验证")
        return True  # 不强制要求

def main():
    print("🔍 AI角色扮演系统 - 环境检查")
    print("=" * 40)
    
    checks = [
        ("Python版本", check_python_version),
        ("依赖包", check_dependencies),
        ("项目结构", check_project_structure),
        ("数据完整性", check_data_integrity),
        ("后端服务", check_backend_service),
        ("数据库连接", check_database_connection),
    ]
    
    results = []
    for name, check_func in checks:
        print(f"\n📋 检查{name}...")
        result = check_func()
        results.append((name, result))
    
    print("\n" + "=" * 40)
    print("📊 检查结果汇总:")
    
    all_passed = True
    for name, passed in results:
        status = "✅ 通过" if passed else "❌ 失败"
        print(f"   {name}: {status}")
        if not passed:
            all_passed = False
    
    print("\n" + "=" * 40)
    if all_passed:
        print("🎉 环境检查全部通过，可以开始部署！")
        print("💡 运行部署: ./quick_deploy.sh")
        return True
    else:
        print("❌ 环境检查失败，请解决上述问题后重试")
        return False

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)
