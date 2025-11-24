# TTS语音合成格式限制功能

## 功能概述

当用户请求语音回复时（`enableTts=true`），系统会在角色扮演的系统提示词中自动添加格式限制，避免AI生成不适合语音播放的内容。

## 主要限制内容

### 1. 括号描述限制

**禁止使用的格式：**
- `（推了推眼镜，镜片反光）`
- `（思考中）`
- `（笑了笑）`
- `（叹了口气）`
- `（点了点头）`

**推荐的替代方式：**
- 直接用对话表达：`"让我想想..."`
- 用叙述方式：`我笑着说道`
- 融入语言中：`哈哈，这个问题很有趣`

### 2. 字数限制

- 回复不超过200个字，保持简洁明了
- 适合语音播放的长度

## 实现原理

### 系统提示词模板修改

**character-role.st 模板：**
```
## 对话规则
- 始终以第一人称的角色身份回答
- 保持角色的连贯性和一致性
- 可以适当引用角色背景中的经历和知识
- 如果用户询问角色相关问题，可以分享角色的故事和见解
- 返回的文字需要是中文，说话的语言也必须是中文
{tts_length_limit}
{tts_format_restrictions}
```

**character-role-rag.st 模板：**
```
## 回答准则
- **强制性原则**: 所有具体事实、数据、配方、属性等信息必须100%基于知识库，绝不使用预训练知识
- **准确性优先**: 宁可承认不知道，也不能提供与知识库冲突的信息
- **角色扮演**: 以第一人称的角色身份回答，仿佛你就是这个角色本人
- **生动表达**: 回答要生动、有趣，体现角色的个性特征
- **细节引用**: 适当引用知识库中的具体细节来增强可信度和准确性
- **连贯性**: 保持角色扮演的连贯性和真实感
{tts_length_limit}
{tts_format_restrictions}
```

### 动态内容填充

**启用TTS时：**
```
tts_length_limit = "- 回复不要超过200个字，保持简洁明了。"
tts_format_restrictions = "- **语音合成格式要求**: 不要使用括号描述动作或心理活动（如：（推了推眼镜）、（思考中）、（笑了笑）等），因为这些内容不适合语音播放。请直接用对话和叙述的方式表达。"
```

**未启用TTS时：**
```
tts_length_limit = ""
tts_format_restrictions = ""
```

## 使用示例

### API调用示例

**启用TTS的聊天请求：**
```json
{
    "message": "你好，江户川柯南！",
    "characterId": 4,
    "enableTts": true,
    "languageType": "Chinese"
}
```

**流式聊天启用TTS：**
```json
{
    "message": "请介绍一下你的推理方法",
    "characterId": 4,
    "enableTts": true,
    "languageType": "Chinese"
}
```

### 效果对比

**未启用TTS时的回复：**
```
（推了推眼镜，镜片反光）啊咧咧，又有新的案件了吗？（兴奋地搓手）作为一名侦探，我最喜欢解决各种谜题了！（认真地点头）让我来分析一下现场的线索吧...
```

**启用TTS时的回复：**
```
啊咧咧，又有新的案件了吗？作为一名侦探，我最喜欢解决各种谜题了！让我来分析一下现场的线索吧，首先要观察现场的每一个细节...
```

## 代码实现位置

### 1. PromptTemplateService.java

**createCharacterSystemMessage方法：**
```java
// 如果启用TTS，添加字数限制和格式限制要求
if (enableTts) {
    templateVariables.put("tts_length_limit", "- 回复不要超过200个字，保持简洁明了。");
    templateVariables.put("tts_format_restrictions", 
        "- **语音合成格式要求**: 不要使用括号描述动作或心理活动（如：（推了推眼镜）、（思考中）、（笑了笑）等），" +
        "因为这些内容不适合语音播放。请直接用对话和叙述的方式表达。");
    log.info("为角色 {} 添加TTS字数限制和格式限制要求", character.getName());
} else {
    templateVariables.put("tts_length_limit", "");
    templateVariables.put("tts_format_restrictions", "");
}
```

**createCharacterSystemMessageWithRAG方法：**
- 同样的逻辑应用于RAG增强的提示词

### 2. ChatController.java

**调用位置：**
```java
// 生成包含RAG知识的增强系统提示词
Message systemMessage = promptTemplateService.createCharacterSystemMessageWithRAG(
    character, 
    relevantKnowledge,
    Boolean.TRUE.equals(request.getEnableTts())  // 传递TTS启用状态
);
```

## 测试验证

### 单元测试

运行 `PromptTemplateServiceTtsTest` 可以验证：
1. TTS启用时包含格式限制
2. TTS未启用时不包含格式限制
3. RAG模式下的TTS限制正常工作

### 集成测试

1. 发送启用TTS的聊天请求
2. 检查生成的系统提示词是否包含格式限制
3. 验证AI回复是否遵循格式要求
4. 确认语音合成质量提升

## 注意事项

1. **重启生效**：修改提示词模板后需要重启应用
2. **角色一致性**：格式限制不应影响角色的个性表达
3. **语音质量**：避免括号内容可以显著提升TTS语音的自然度
4. **用户体验**：在保持角色特色的同时，确保语音播放流畅

## 扩展建议

1. **可配置化**：将TTS限制内容配置化，便于调整
2. **智能检测**：在TTS处理前自动过滤括号内容
3. **多语言支持**：为不同语言定制相应的格式限制
4. **用户反馈**：收集用户对语音质量的反馈，持续优化限制规则
