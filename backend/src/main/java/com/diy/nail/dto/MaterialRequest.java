package com.diy.nail.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 素材新增/编辑请求
 */
@Data
public class MaterialRequest {

    /** 素材类型：SHAPE甲型 / COLOR色卡 / PATTERN图案 */
    @NotBlank(message = "素材类型不能为空")
    @Pattern(regexp = "SHAPE|COLOR|PATTERN", message = "素材类型不合法")
    private String type;

    /** 素材名称 */
    @NotBlank(message = "素材名称不能为空")
    @Size(max = 50, message = "素材名称不能超过50字")
    private String name;

    /** 图片路径（甲型/图案必填，色卡不需要） */
    private String imageUrl;

    /** 颜色代码（色卡必填，格式 #RRGGBB） */
    private String colorCode;

    /** 图案分类（BASIC/FRENCH/GRADIENT/NATURE/GEO/SHINE，甲型/色卡可不填） */
    private String category;

    /** 排序号（越小越靠前） */
    private Integer sort;

    /** 状态：1上架 0下架 */
    private Integer status;
}
