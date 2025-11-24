# 角色音色映射使用示例

## 配置文件方式（推荐）

在 `application.yaml` 中配置角色音色映射：

```yaml
speech:
  tts:
    character-voice-mapping:
      1: Ethan        # 哈利·波特 - 年轻男性，阳光活力
      2: Elias        # 苏格拉底 - 智慧长者，学者风范
      3: Marcus       # 爱因斯坦 - 科学家，陕西话特色
      4: Dylan        # 江户川柯南 - 推理侦探，北京话
      5: Ryan         # 泰拉瑞亚向导 - 游戏向导，戏感炸裂
      6: Cherry       # 新角色示例 - 阳光积极小姐姐
      7: Jennifer     # 新角色示例 - 品牌级美语女声
      8: Katerina     # 新角色示例 - 御姐音色
      9: Nofish       # 新角色示例 - 设计师音色
      10: Sunny       # 新角色示例 - 四川妹子
      11: Jada        # 新角色示例 - 上海阿姐
      12: Rocky       # 新角色示例 - 粤语阿强
      13: Kiki        # 新角色示例 - 港妹闺蜜
      14: Serena      # 新角色示例 - 温柔小姐姐
      15: Chelsie     # 新角色示例 - 二次元虚拟女友
```

## API使用示例

### 1. 角色语音合成（自动选择音色）

```http
POST /api/tts/synthesize/character/1
Content-Type: application/x-www-form-urlencoded

text=你好，我是哈利·波特！&languageType=Chinese
```

系统会自动为角色ID=1的哈利·波特选择Ethan音色。

### 2. 获取角色推荐音色

```http
GET /api/tts/character/1/recommended-voice
```

响应：
```json
{
  "success": true,
  "data": "Ethan",
  "message": "获取推荐音色成功"
}
```

### 3. 手动指定音色合成

```http
POST /api/tts/synthesize
Content-Type: application/json

{
    "text": "你好，我是苏格拉底！",
    "voice": "Elias",
    "languageType": "Chinese",
    "characterId": 2
}
```

### 4. 流式语音合成

```http
POST /api/tts/synthesize/stream
Content-Type: application/json

{
    "text": "相对论是我最重要的发现之一...",
    "voice": "Marcus",
    "languageType": "Chinese",
    "characterId": 3
}
```

## 音色特色说明

| 音色 | 中文名 | 特色描述 | 适合角色类型 |
|------|--------|----------|--------------|
| Cherry | 芊悦 | 阳光积极、亲切自然小姐姐 | 活泼开朗的女性角色 |
| Ethan | 晨煦 | 阳光、温暖、活力、朝气 | 年轻男性、英雄角色 |
| Elias | 墨讲师 | 学科严谨性与叙事技巧的完美结合 | 学者、导师、智者 |
| Jennifer | 詹妮弗 | 品牌级、电影质感般美语女声 | 优雅女性、国际化角色 |
| Katerina | 卡捷琳娜 | 御姐音色，韵律回味十足 | 成熟女性、权威角色 |
| Ryan | 甜茶 | 节奏拉满，戏感炸裂，真实与张力共舞 | 戏剧性角色、游戏角色 |
| Marcus | 陕西话 | 陕西话特色 | 西北地区角色、朴实角色 |
| Dylan | 北京话 | 北京胡同里长大的少年 | 北京地区角色、推理角色 |
| Sunny | 四川话 | 甜到你心里的川妹子 | 四川地区角色、可爱女性 |
| Jada | 上海话 | 风风火火的沪上阿姐 | 上海地区角色、干练女性 |
| Rocky | 粤语 | 幽默风趣的阿强，在线陪聊 | 广东地区角色、幽默角色 |
| Kiki | 粤语 | 甜美的港妹闺蜜 | 香港地区角色、甜美女性 |
| Serena | 苏瑶 | 温柔小姐姐 | 温柔体贴的女性角色 |
| Chelsie | 千雪 | 二次元虚拟女友 | 动漫角色、虚拟角色 |

## 动态修改音色

### Java代码示例

```java
@Autowired
private TtsSynthesisConfig ttsSynthesisConfig;

// 动态设置角色音色
ttsSynthesisConfig.setCharacterVoice(16L, "Jennifer");

// 获取角色音色
String voice = ttsSynthesisConfig.getCharacterVoice(16L);
```

## 注意事项

1. **语言支持**：Qwen3-TTS音色支持多语言，Qwen-TTS音色仅支持中英文
2. **配置优先级**：配置文件中的映射优先于代码中的动态设置
3. **默认音色**：未配置的角色会使用默认音色（Cherry）
4. **重启生效**：修改配置文件后需要重启应用才能生效
5. **动态设置**：通过代码动态设置的音色映射会在应用重启后丢失
