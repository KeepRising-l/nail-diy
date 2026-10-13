package com.diy.nail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后申请实体（表 after_sale）。
 *
 * <p>状态流转：
 * <pre>
 * PENDING（顾客提交，待商家处理）
 *   ├─ 商家同意 → COMPLETED（已退款，终态）
 *   └─ 商家拒绝 → REJECTED（终态）
 * </pre>
 * 一个订单最多一条售后单（被拒后不可重复申请，避免反复提交）。
 */
@Data
@TableName("after_sale")
public class AfterSale {

    /** 状态：待处理 */
    public static final String STATUS_PENDING = "PENDING";
    /** 状态：已退款（终态） */
    public static final String STATUS_COMPLETED = "COMPLETED";
    /** 状态：已拒绝（终态） */
    public static final String STATUS_REJECTED = "REJECTED";

    /** 类型：仅退款（不退货） */
    public static final String TYPE_REFUND_ONLY = "REFUND_ONLY";
    /** 类型：退货退款 */
    public static final String TYPE_RETURN_REFUND = "RETURN_REFUND";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单 ID */
    private Long orderId;

    /** 申请顾客 ID */
    private Long userId;

    /** 类型：REFUND_ONLY 仅退款 / RETURN_REFUND 退货退款 */
    private String type;

    /** 申请原因分类 */
    private String reason;

    /** 补充说明 */
    private String description;

    /** 凭证图 JSON 数组字符串（最多 3 张 URL） */
    private String images;

    /** 申请退款金额 */
    private BigDecimal amount;

    /** 状态：PENDING / COMPLETED / REJECTED */
    private String status;

    /** 商家拒绝原因 */
    private String rejectReason;

    /** 处理时间 */
    private LocalDateTime handleTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除：0正常 1删除 */
    @TableLogic
    private Integer deleted;
}
