package com.diy.nail.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 模板新增/编辑请求
 */
@Data
public class TemplateRequest {

    /** 模板名称 */
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 50, message = "模板名称不能超过50字")
    private String name;

    /** 风格标签：可爱/轻奢/法式等 */
    @Size(max = 50, message = "风格标签不能超过50字")
    private String styleTag;

    /** 封面图路径（必填，先走图片上传接口获得） */
    private String coverUrl;

    /** 设计参数 JSON（必填，{shapeId, colorId, patternIds, remark}） */
    private String designParams;

    /** 参考价格 */
    @NotNull(message = "参考价格不能为空")
    private BigDecimal price;

    /** 排序号（越小越靠前） */
    private Integer sort;

    /** 状态：1上架 0下架 */
    private Integer status;
}
