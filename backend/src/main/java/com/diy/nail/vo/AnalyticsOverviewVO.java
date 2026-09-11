package com.diy.nail.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 统计看板总览（ANA-REQ-1/4/5）：销售口径 = 已支付及之后、不含已取消
 */
@Data
public class AnalyticsOverviewVO {

    /** 累计有效订单数 */
    private Long totalOrders;

    /** 累计销售额（订单总额口径） */
    private BigDecimal totalSales;

    /** 待履约订单数（PAID/PRODUCING/WAIT_PICKUP/WAIT_SHIP） */
    private Long pendingFulfillment;

    /** 平均评分（无评价为 0） */
    private BigDecimal avgRating;

    /** 自提订单数 */
    private Long selfPickupCount;

    /** 邮寄订单数 */
    private Long expressCount;

    /** 1-5 星评价分布（name 为星数，count 为数量，恒定 5 项） */
    private List<NameCountVO> ratingDist;
}
