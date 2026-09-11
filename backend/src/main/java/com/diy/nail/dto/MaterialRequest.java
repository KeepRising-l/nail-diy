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

    /** 素材类型：SHAPE甲型 / COLOR色卡 / PATTERN_IMG贴图 / PATTERN_STYLE参数化款式 / DECOR装饰件 / STICKER贴纸 */
    @NotBlank(message = "素材类型不能为空")
    @Pattern(regexp = "SHAPE|COLOR|PATTERN_IMG|PATTERN_STYLE|DECOR|STICKER", message = "素材类型不合法")
    private String type;

    /** 代码标识（SHAPE/PATTERN_STYLE/矢量DECOR 必填，绑定前端渲染器） */
    @Size(max = 50, message = "代码标识不能超过50字")
    private String code;

    /** 素材名称 */
    @NotBlank(message = "素材名称不能为空")
    @Size(max = 50, message = "素材名称不能超过50字")
    private String name;

    /** 图片路径（贴图/贴纸/立体装饰必填） */
    private String imageUrl;

    /** 颜色代码（色卡必填，格式 #RRGGBB） */
    private String colorCode;

    /** 分类（贴图：BASIC/NATURE/GEO/CUTE/SHINE；色卡：色系；甲型：长度系列） */
    private String category;

    /** 检索标签，逗号分隔 */
    @Size(max = 255, message = "标签过长")
    private String tags;

    /** 类型专属属性 JSON 字符串（如参数化款式的 paramsSchema） */
    private String attrs;

    /** 排序号（越小越靠前） */
    private Integer sort;

    /** 状态：1上架 0下架 */
    private Integer status;
}
