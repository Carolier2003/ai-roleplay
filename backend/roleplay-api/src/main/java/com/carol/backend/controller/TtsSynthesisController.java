package com.carol.backend.controller;

import com.carol.backend.dto.ApiResponse;
import com.carol.backend.dto.TtsSynthesisRequest;
import com.carol.backend.dto.TtsSynthesisResponse;
import com.carol.backend.service.ITtsSynthesisService;
import com.carol.backend.service.IStreamingTtsSynthesisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.HashMap;

/**
 * TTS语音合成控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/tts")
@RequiredArgsConstructor
@Validated
public class TtsSynthesisController {
    
    private final ITtsSynthesisService ttsSynthesisService;
    private final IStreamingTtsSynthesisService streamingTtsService;
    
    /**
     * 同步语音合成 - 返回音频URL
     */
    @PostMapping("/synthesize")
    public ResponseEntity<ApiResponse<TtsSynthesisResponse>> synthesizeText(
            @Valid @RequestBody TtsSynthesisRequest request) {
        
        try {
            log.info("收到语音合成请求: 文本长度={}, 音色={}, 用户={}", 
                    request.getText().length(), request.getVoice(), request.getUserId());
            
            TtsSynthesisResponse response = ttsSynthesisService.synthesizeText(request);
            
            if (response.getSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(response, "语音合成成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("语音合成失败: " + response.getErrorMessage()));
            }
            
        } catch (Exception e) {
            log.error("语音合成API调用失败: error={}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("语音合成服务异常: " + e.getMessage()));
        }
    }
    
    /**
     * 流式语音合成 - 返回SSE流
     */
    @PostMapping(value = "/synthesize/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamingSynthesize(@Valid @RequestBody TtsSynthesisRequest request) {
        
        try {
            log.info("收到流式语音合成请求: 文本长度={}, 音色={}, 用户={}", 
                    request.getText().length(), request.getVoice(), request.getUserId());
            
            return streamingTtsService.createStreamingSynthesis(request);
            
        } catch (Exception e) {
            log.error("创建流式语音合成失败: error={}", e.getMessage(), e);
            
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of("error", e.getMessage())));
                emitter.complete();
            } catch (Exception sendError) {
                emitter.completeWithError(sendError);
            }
            return emitter;
        }
    }
    
    /**
     * 角色语音合成 - 自动选择角色音色
     */
    @PostMapping("/synthesize/character/{characterId}")
    public ResponseEntity<ApiResponse<TtsSynthesisResponse>> synthesizeForCharacter(
            @PathVariable Long characterId,
            @RequestParam String text,
            @RequestParam(required = false, defaultValue = "Chinese") String languageType,
            @RequestParam(required = false) String userId) {
        
        try {
            log.info("收到角色语音合成请求: characterId={}, 文本长度={}, 语言={}", 
                    characterId, text.length(), languageType);
            
            TtsSynthesisResponse response = ttsSynthesisService.synthesizeForCharacter(
                    text, characterId, languageType);
            
            if (response.getSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(response, "角色语音合成成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("角色语音合成失败: " + response.getErrorMessage()));
            }
            
        } catch (Exception e) {
            log.error("角色语音合成API调用失败: characterId={}, error={}", characterId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("角色语音合成服务异常: " + e.getMessage()));
        }
    }
    
    /**
     * 获取角色推荐音色
     */
    @GetMapping("/character/{characterId}/recommended-voice")
    public ResponseEntity<ApiResponse<String>> getRecommendedVoice(@PathVariable Long characterId) {
        
        try {
            String recommendedVoice = ttsSynthesisService.getRecommendedVoiceForCharacter(characterId);
            
            return ResponseEntity.ok(ApiResponse.success(recommendedVoice, "获取推荐音色成功"));
            
        } catch (Exception e) {
            log.error("获取角色推荐音色失败: characterId={}, error={}", characterId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取推荐音色失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取支持的音色列表
     */
    @GetMapping("/voices")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSupportedVoices() {
        
        try {
            Map<String, Object> voicesInfo = new HashMap<>();
            
            // Qwen3-TTS音色（支持多语言）
            Map<String, Object> qwen3Voices = new HashMap<>();
            qwen3Voices.put("Cherry", Map.of("name", "芊悦", "description", "阳光积极、亲切自然小姐姐", 
                    "languages", new String[]{"Chinese", "English", "French", "German", "Russian", "Italian", "Spanish", "Portuguese", "Japanese", "Korean"}));
            qwen3Voices.put("Ethan", Map.of("name", "晨煦", "description", "阳光、温暖、活力、朝气", 
                    "languages", new String[]{"Chinese", "English", "French", "German", "Russian", "Italian", "Spanish", "Portuguese", "Japanese", "Korean"}));
            qwen3Voices.put("Nofish", Map.of("name", "不吃鱼", "description", "不会翘舌音的设计师", 
                    "languages", new String[]{"Chinese", "English", "French", "German", "Russian", "Italian", "Spanish", "Portuguese", "Japanese", "Korean"}));
            qwen3Voices.put("Jennifer", Map.of("name", "詹妮弗", "description", "品牌级、电影质感般美语女声", 
                    "languages", new String[]{"Chinese", "English", "French", "German", "Russian", "Italian", "Spanish", "Portuguese", "Japanese", "Korean"}));
            qwen3Voices.put("Ryan", Map.of("name", "甜茶", "description", "节奏拉满，戏感炸裂，真实与张力共舞", 
                    "languages", new String[]{"Chinese", "English", "French", "German", "Russian", "Italian", "Spanish", "Portuguese", "Japanese", "Korean"}));
            qwen3Voices.put("Katerina", Map.of("name", "卡捷琳娜", "description", "御姐音色，韵律回味十足", 
                    "languages", new String[]{"Chinese", "English", "French", "German", "Russian", "Italian", "Spanish", "Portuguese", "Japanese", "Korean"}));
            qwen3Voices.put("Elias", Map.of("name", "墨讲师", "description", "学科严谨性与叙事技巧的完美结合", 
                    "languages", new String[]{"Chinese", "English", "French", "German", "Russian", "Italian", "Spanish", "Portuguese", "Japanese", "Korean"}));
            
            // 方言音色
            qwen3Voices.put("Jada", Map.of("name", "上海-阿珍", "description", "风风火火的沪上阿姐", 
                    "languages", new String[]{"Chinese", "English", "French", "German", "Russian", "Italian", "Spanish", "Portuguese", "Japanese", "Korean"}));
            qwen3Voices.put("Dylan", Map.of("name", "北京-晓东", "description", "北京胡同里长大的少年", 
                    "languages", new String[]{"Chinese", "English", "French", "German", "Russian", "Italian", "Spanish", "Portuguese", "Japanese", "Korean"}));
            qwen3Voices.put("Sunny", Map.of("name", "四川-晴儿", "description", "甜到你心里的川妹子", 
                    "languages", new String[]{"Chinese", "English", "French", "German", "Russian", "Italian", "Spanish", "Portuguese", "Japanese", "Korean"}));
            qwen3Voices.put("Rocky", Map.of("name", "粤语-阿强", "description", "幽默风趣的阿强，在线陪聊", 
                    "languages", new String[]{"Chinese", "English", "French", "German", "Russian", "Italian", "Spanish", "Portuguese", "Japanese", "Korean"}));
            qwen3Voices.put("Kiki", Map.of("name", "粤语-阿清", "description", "甜美的港妹闺蜜", 
                    "languages", new String[]{"Chinese", "English", "French", "German", "Russian", "Italian", "Spanish", "Portuguese", "Japanese", "Korean"}));
            
            // Qwen-TTS音色（仅中英文）
            Map<String, Object> qwenVoices = new HashMap<>();
            qwenVoices.put("Serena", Map.of("name", "苏瑶", "description", "温柔小姐姐", 
                    "languages", new String[]{"Chinese", "English"}));
            qwenVoices.put("Chelsie", Map.of("name", "千雪", "description", "二次元虚拟女友", 
                    "languages", new String[]{"Chinese", "English"}));
            
            voicesInfo.put("qwen3-tts", qwen3Voices);
            voicesInfo.put("qwen-tts", qwenVoices);
            voicesInfo.put("defaultVoice", "Cherry");
            voicesInfo.put("supportedLanguages", new String[]{"Chinese", "English", "French", "German", "Russian", "Italian", "Spanish", "Portuguese", "Japanese", "Korean"});
            
            return ResponseEntity.ok(ApiResponse.success(voicesInfo, "获取音色列表成功"));
            
        } catch (Exception e) {
            log.error("获取音色列表失败: error={}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取音色列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 停止流式合成
     */
    @PostMapping("/synthesize/stream/{sessionId}/stop")
    public ResponseEntity<ApiResponse<Boolean>> stopStreamingSynthesis(@PathVariable String sessionId) {
        
        try {
            boolean stopped = streamingTtsService.stopStreamingSynthesis(sessionId);
            
            if (stopped) {
                return ResponseEntity.ok(ApiResponse.success(true, "流式合成已停止"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("会话不存在或已结束"));
            }
            
        } catch (Exception e) {
            log.error("停止流式合成失败: sessionId={}, error={}", sessionId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("停止流式合成失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取服务状态
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getServiceStatus() {
        
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("activeStreamingSessions", streamingTtsService.getActiveSessionCount());
            status.put("serviceAvailable", true);
            status.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(ApiResponse.success(status, "获取服务状态成功"));
            
        } catch (Exception e) {
            log.error("获取服务状态失败: error={}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取服务状态失败: " + e.getMessage()));
        }
    }
}
