package com.diy.nail.dto;

import jakarta.validation.constraints.NotNull;
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
    private Integer quantity;
}
