package com.diy.nail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单状态变更日志实体（表 order_status_log，状态时间线）
 */
@Data
@TableName("order_status_log")
public class OrderStatusLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单 */
    private Long orderId;

    /** 原状态 */
    private String fromStatus;

    /** 新状态 */
    private String toStatus;

    /** 操作人（用户/商家） */
    private String operator;

    /** 备注 */
    private String remark;

    private LocalDateTime createTime;
}
