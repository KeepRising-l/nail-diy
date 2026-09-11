package com.diy.nail.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 销售趋势聚合点（按支付日期分桶）
 */
@Data
public class TrendPointVO {

    /** 日期 yyyy-MM-dd */
    private String date;

    /** 当日销售额 */
    private BigDecimal amount;

    /** 当日订单量 */
    private Long orderCount;
}
