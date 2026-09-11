package com.diy.nail.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设计保存/编辑请求
 */
@Data
public class DesignRequest {

    /** 设计名称 */
    @NotBlank(message = "设计名称不能为空")
    @Size(max = 50, message = "设计名称不能超过50字")
    private String name;

    /** 设计参数 JSON（{shapeId, colorId, patternIds, remark}） */
    @NotBlank(message = "设计参数不能为空")
    private String params;

    /** 预览快照图路径（前端截图上传后获得，可空） */
    private String previewUrl;
}
