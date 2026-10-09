package com.diy.nail.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 加入购物车请求
 */
@Data
public class CartRequest {

    /** 设计 id */
    @NotNull(message = "设计不能为空")
    private Long designId;

    /** 数量（不传默认 1） */
    @Min(value = 1, message = "数量不能小于 1")
    @Max(value = 99, message = "数量不能大于 99")
    private Integer quantity;
}
