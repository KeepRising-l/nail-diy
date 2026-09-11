package com.diy.nail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细实体（表 order_item，设计快照，商家按 designParams 制作）
 */
@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单 */
    private Long orderId;

    /** 关联设计（可空） */
    private Long designId;

    /** 设计名称快照 */
    private String designName;

    /** 设计参数快照（商家按此制作） */
    private String designParams;

    /** 预览图快照 */
    private String previewUrl;

    /** 单价快照 */
    private BigDecimal price;

    /** 数量 */
    private Integer quantity;

    private LocalDateTime createTime;
}
