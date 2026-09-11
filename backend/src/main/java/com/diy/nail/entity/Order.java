package com.diy.nail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体（表 orders）
 * 状态常量全系统共用（change-07 履约续用），杜绝魔法字符串
 */
@Data
@TableName("orders")
public class Order {

    /** 订单状态常量 */
    public static final String STATUS_PENDING_PAY = "PENDING_PAY"; // 待支付
    public static final String STATUS_PAID = "PAID";               // 已支付（待接单）
    public static final String STATUS_PRODUCING = "PRODUCING";     // 制作中
    public static final String STATUS_WAIT_PICKUP = "WAIT_PICKUP"; // 待自提
    public static final String STATUS_VERIFIED = "VERIFIED";       // 已核销（终态）
    public static final String STATUS_WAIT_SHIP = "WAIT_SHIP";     // 待发货
    public static final String STATUS_SHIPPED = "SHIPPED";         // 已发货
    public static final String STATUS_RECEIVED = "RECEIVED";       // 已签收（终态）
    public static final String STATUS_CANCELLED = "CANCELLED";     // 已取消（终态）

    /** 履约方式常量 */
    public static final String DELIVERY_SELF_PICKUP = "SELF_PICKUP"; // 到店自提
    public static final String DELIVERY_EXPRESS = "EXPRESS";         // 商家邮寄

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号（唯一） */
    private String orderNo;

    /** 下单用户 */
    private Long userId;

    /** 履约方式：SELF_PICKUP自提/EXPRESS邮寄 */
    private String deliveryType;

    /** 自提门店（自提时有值） */
    private Long storeId;

    /** 收货人（邮寄时有值） */
    private String receiverName;

    /** 收货电话 */
    private String receiverPhone;

    /** 收货地址全文（邮寄时有值） */
    private String receiverAddr;

    /** 商品金额 */
    private BigDecimal goodsAmount;

    /** 运费 */
    private BigDecimal freight;

    /** 订单总额 */
    private BigDecimal totalAmount;

    /** 订单状态 */
    private String status;

    /** 自提码（进入待自提时生成） */
    private String pickupCode;

    /** 快递公司 */
    private String expressCompany;

    /** 物流单号 */
    private String expressNo;

    /** 顾客备注 */
    private String remark;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 完成时间 */
    private LocalDateTime finishTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
