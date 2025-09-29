package com.carol.backend.dto.avatar;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 图片裁剪参数
 */
@Data
public class CropData {
    
    /**
     * 裁剪起始X坐标
     */
    @NotNull(message = "裁剪X坐标不能为空")
    @Min(value = 0, message = "裁剪X坐标不能小于0")
    private Integer x;
    
    /**
     * 裁剪起始Y坐标
     */
    @NotNull(message = "裁剪Y坐标不能为空")
    @Min(value = 0, message = "裁剪Y坐标不能小于0")
    private Integer y;
    
    /**
     * 裁剪宽度
     */
    @NotNull(message = "裁剪宽度不能为空")
    @Min(value = 1, message = "裁剪宽度必须大于0")
    private Integer width;
    
    /**
     * 裁剪高度
     */
    @NotNull(message = "裁剪高度不能为空")
    @Min(value = 1, message = "裁剪高度必须大于0")
    private Integer height;
    
    /**
     * 缩放比例
     */
    private Double scale = 1.0;
    
    /**
     * 旋转角度
     */
    private Integer rotate = 0;
}
