package com.carol.backend.service;

import com.carol.backend.dto.avatar.CropData;

/**
 * 图片处理服务接口
 */
public interface IImageProcessService {
    
    /**
     * 裁剪图片
     * 
     * @param imageBytes 原始图片字节数组
     * @param cropData 裁剪参数
     * @return 裁剪后的图片字节数组
     */
    byte[] cropImage(byte[] imageBytes, CropData cropData);
    
    /**
     * 生成缩略图
     * 
     * @param imageBytes 原始图片字节数组
     * @param size 缩略图尺寸
     * @return 缩略图字节数组
     */
    byte[] generateThumbnail(byte[] imageBytes, int size);
    
    /**
     * 压缩图片
     * 
     * @param imageBytes 原始图片字节数组
     * @param maxSize 最大尺寸
     * @param quality 压缩质量（0.0-1.0）
     * @return 压缩后的图片字节数组
     */
    byte[] compressImage(byte[] imageBytes, int maxSize, float quality);
    
    /**
     * 验证图片格式
     * 
     * @param imageBytes 图片字节数组
     * @return 图片格式（如：jpg, png, webp）
     */
    String validateImageFormat(byte[] imageBytes);
    
    /**
     * 获取图片尺寸
     * 
     * @param imageBytes 图片字节数组
     * @return 图片尺寸数组 [width, height]
     */
    int[] getImageDimensions(byte[] imageBytes);
}
