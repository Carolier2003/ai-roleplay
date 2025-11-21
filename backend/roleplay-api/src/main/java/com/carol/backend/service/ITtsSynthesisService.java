package com.carol.backend.service;

import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.carol.backend.dto.TtsSynthesisRequest;
import com.carol.backend.dto.TtsSynthesisResponse;

/**
 * TTS语音合成服务接口
 * 
 * @author jianjl
 * @version 1.0
 * @description TTS语音合成服务
 * @date 2025-01-15
 */
public interface ITtsSynthesisService {
    
    /**
     * 同步语音合成
     * 
     * @param request TTS合成请求
     * @return TTS合成响应
     * @throws ApiException API异常
     * @throws NoApiKeyException API密钥异常
     * @throws UploadFileException 文件上传异常
     */
    TtsSynthesisResponse synthesizeText(TtsSynthesisRequest request) 
            throws ApiException, NoApiKeyException, UploadFileException;
    
    /**
     * 根据角色ID获取推荐音色
     * 
     * @param characterId 角色ID
     * @return 推荐音色
     */
    String getRecommendedVoiceForCharacter(Long characterId);
    
    /**
     * 为角色合成语音（自动选择音色）
     * 
     * @param text 文本内容
     * @param characterId 角色ID
     * @param languageType 语言类型
     * @return TTS合成响应
     * @throws ApiException API异常
     * @throws NoApiKeyException API密钥异常
     * @throws UploadFileException 文件上传异常
     */
    TtsSynthesisResponse synthesizeForCharacter(String text, Long characterId, String languageType) 
            throws ApiException, NoApiKeyException, UploadFileException;
}
