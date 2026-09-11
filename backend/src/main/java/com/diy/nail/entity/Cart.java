package com.diy.nail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 购物车实体（表 cart，同一用户同一设计唯一）
 */
@Data
@TableName("cart")
public class Cart {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 */
    private Long userId;

    /** 设计 */
    private Long designId;

    /** 数量 */
    private Integer quantity;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
