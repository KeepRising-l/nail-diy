package com.diy.nail.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车条目视图：购物车条目 + 设计快照（名称/预览图/单价）
 */
@Data
public class CartVO {

    /** 购物车条目 id */
    private Long id;

    /** 设计 id */
    private Long designId;

    /** 数量 */
    private Integer quantity;

    /** 设计名称快照 */
    private String designName;

    /** 预览图快照 */
    private String previewUrl;

    /** 单价快照 */
    private BigDecimal price;
}
