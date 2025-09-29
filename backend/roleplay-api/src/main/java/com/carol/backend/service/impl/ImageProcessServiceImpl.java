package com.carol.backend.service.impl;

import cn.hutool.core.util.ArrayUtil;
import com.carol.backend.dto.avatar.CropData;
import com.carol.backend.enums.ErrorCode;
import com.carol.backend.exception.BusinessException;
import com.carol.backend.service.IImageProcessService;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 图片处理服务实现
 */
@Slf4j
@Service
public class ImageProcessServiceImpl implements IImageProcessService {
    
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList("jpg", "jpeg", "png", "webp");
    
    @Override
    public byte[] cropImage(byte[] imageBytes, CropData cropData) {
        log.info("[cropImage] 开始裁剪图片, 裁剪参数: x={}, y={}, width={}, height={}, scale={}, rotate={}", 
            cropData.getX(), cropData.getY(), cropData.getWidth(), cropData.getHeight(), 
            cropData.getScale(), cropData.getRotate());
        
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            // 先读取为BufferedImage，然后使用Thumbnails处理
            BufferedImage sourceImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (sourceImage == null) {
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "无法读取图片数据");
            }
            
            var builder = Thumbnails.of(sourceImage);
            
            // 应用缩放
            if (cropData.getScale() != null && cropData.getScale() != 1.0) {
                builder = builder.scale(cropData.getScale());
            }
            
            // 应用旋转
            if (cropData.getRotate() != null && cropData.getRotate() != 0) {
                builder = builder.rotate(cropData.getRotate());
            }
            
            // 应用裁剪
            builder = builder.sourceRegion(
                cropData.getX(), 
                cropData.getY(), 
                cropData.getWidth(), 
                cropData.getHeight()
            );
            
            // 输出格式
            builder.outputFormat("jpg")
                   .outputQuality(0.9)
                   .toOutputStream(outputStream);
            
            byte[] result = outputStream.toByteArray();
            log.info("[cropImage] 图片裁剪成功, 原始大小: {} bytes, 裁剪后大小: {} bytes", 
                imageBytes.length, result.length);
            
            return result;
        } catch (IOException e) {
            log.error("[cropImage] 图片裁剪失败: {}", e.getMessage(), e);
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "图片裁剪失败");
        }
    }
    
    @Override
    public byte[] generateThumbnail(byte[] imageBytes, int size) {
        log.info("[generateThumbnail] 生成缩略图, 目标尺寸: {}x{}", size, size);
        
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            Thumbnails.of(new ByteArrayInputStream(imageBytes))
                     .size(size, size)
                     .outputFormat("jpg")
                     .outputQuality(0.8)
                     .toOutputStream(outputStream);
            
            byte[] result = outputStream.toByteArray();
            log.info("[generateThumbnail] 缩略图生成成功, 原始大小: {} bytes, 缩略图大小: {} bytes", 
                imageBytes.length, result.length);
            
            return result;
        } catch (IOException e) {
            log.error("[generateThumbnail] 生成缩略图失败: {}", e.getMessage(), e);
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "生成缩略图失败");
        }
    }
    
    @Override
    public byte[] compressImage(byte[] imageBytes, int maxSize, float quality) {
        log.info("[compressImage] 压缩图片, 最大尺寸: {}, 质量: {}", maxSize, quality);
        
        try {
            // 获取原始图片尺寸
            int[] dimensions = getImageDimensions(imageBytes);
            int originalWidth = dimensions[0];
            int originalHeight = dimensions[1];
            
            // 计算压缩后的尺寸
            int targetWidth = originalWidth;
            int targetHeight = originalHeight;
            
            if (originalWidth > maxSize || originalHeight > maxSize) {
                double ratio = Math.min((double) maxSize / originalWidth, (double) maxSize / originalHeight);
                targetWidth = (int) (originalWidth * ratio);
                targetHeight = (int) (originalHeight * ratio);
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            Thumbnails.of(new ByteArrayInputStream(imageBytes))
                     .size(targetWidth, targetHeight)
                     .outputFormat("jpg")
                     .outputQuality(quality)
                     .toOutputStream(outputStream);
            
            byte[] result = outputStream.toByteArray();
            log.info("[compressImage] 图片压缩成功, 原始: {}x{} ({} bytes), 压缩后: {}x{} ({} bytes)", 
                originalWidth, originalHeight, imageBytes.length, 
                targetWidth, targetHeight, result.length);
            
            return result;
        } catch (IOException e) {
            log.error("[compressImage] 图片压缩失败: {}", e.getMessage(), e);
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "图片压缩失败");
        }
    }
    
    @Override
    public String validateImageFormat(byte[] imageBytes) {
        log.info("[validateImageFormat] 验证图片格式, 文件大小: {} bytes", imageBytes.length);
        
        if (ArrayUtil.isEmpty(imageBytes)) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "图片数据为空");
        }
        
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "无效的图片格式");
            }
            
            // 通过文件头判断格式
            String format = detectImageFormat(imageBytes);
            if (!SUPPORTED_FORMATS.contains(format.toLowerCase())) {
                throw BusinessException.of(ErrorCode.PARAM_ERROR, 
                    "不支持的图片格式: " + format + "，支持的格式: " + String.join(", ", SUPPORTED_FORMATS));
            }
            
            log.info("[validateImageFormat] 图片格式验证成功: {}", format);
            return format;
        } catch (IOException e) {
            log.error("[validateImageFormat] 图片格式验证失败: {}", e.getMessage(), e);
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "无效的图片格式");
        }
    }
    
    @Override
    public int[] getImageDimensions(byte[] imageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "无法读取图片尺寸");
            }
            
            int width = image.getWidth();
            int height = image.getHeight();
            
            log.info("[getImageDimensions] 获取图片尺寸: {}x{}", width, height);
            return new int[]{width, height};
        } catch (IOException e) {
            log.error("[getImageDimensions] 获取图片尺寸失败: {}", e.getMessage(), e);
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "获取图片尺寸失败");
        }
    }
    
    /**
     * 通过文件头检测图片格式
     */
    private String detectImageFormat(byte[] imageBytes) {
        if (imageBytes.length < 4) {
            return "unknown";
        }
        
        // JPEG格式检测
        if (imageBytes[0] == (byte) 0xFF && imageBytes[1] == (byte) 0xD8) {
            return "jpg";
        }
        
        // PNG格式检测
        if (imageBytes[0] == (byte) 0x89 && imageBytes[1] == (byte) 0x50 && 
            imageBytes[2] == (byte) 0x4E && imageBytes[3] == (byte) 0x47) {
            return "png";
        }
        
        // WebP格式检测
        if (imageBytes.length >= 12 && 
            imageBytes[0] == (byte) 0x52 && imageBytes[1] == (byte) 0x49 && 
            imageBytes[2] == (byte) 0x46 && imageBytes[3] == (byte) 0x46 &&
            imageBytes[8] == (byte) 0x57 && imageBytes[9] == (byte) 0x45 && 
            imageBytes[10] == (byte) 0x42 && imageBytes[11] == (byte) 0x50) {
            return "webp";
        }
        
        return "unknown";
    }
}
