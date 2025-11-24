# 角色头像资源

这个目录用于存放角色头像图片。

## 文件命名规范

- `yuanbao.webp` - 腾讯元宝头像
- `harry.webp` - 哈利波特头像  
- `hermione.webp` - 赫敏头像
- `default.webp` - 默认头像（当其他头像加载失败时使用）
- `user.webp` - 用户头像

## 推荐规格

- 格式：WebP（优先）或 PNG
- 尺寸：256x256 像素
- 文件大小：< 50KB
- 背景：透明或纯色

## 获取头像

你可以：
1. 使用AI生成工具创建角色头像
2. 从免费图库下载合适的头像
3. 使用占位符服务（如 https://picsum.photos/256/256）

## 临时占位符

在开发阶段，可以使用以下占位符URL：
- `https://picsum.photos/256/256?random=1` （腾讯元宝）
- `https://picsum.photos/256/256?random=2` （哈利波特）
- `https://picsum.photos/256/256?random=3` （赫敏）
- `https://picsum.photos/256/256?random=4` （默认头像）
- `https://picsum.photos/256/256?random=5` （用户头像）
