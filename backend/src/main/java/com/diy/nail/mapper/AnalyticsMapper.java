package com.diy.nail.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 统计看板聚合查询（change-09）：只读聚合，不走 BaseMapper；
 * 销售口径统一：已支付及之后、不含 PENDING_PAY/CANCELLED
 */
@Mapper
public interface AnalyticsMapper {

    /** 有效订单数 */
    @Select("SELECT COUNT(*) FROM orders WHERE deleted = 0 AND status NOT IN ('PENDING_PAY', 'CANCELLED')")
    Long countValidOrders();

    /** 有效订单销售额（订单总额求和） */
    @Select("SELECT IFNULL(SUM(total_amount), 0) FROM orders WHERE deleted = 0 AND status NOT IN ('PENDING_PAY', 'CANCELLED')")
    BigDecimal sumSales();

    /** 待履约订单数 */
    @Select("SELECT COUNT(*) FROM orders WHERE deleted = 0 AND status IN ('PAID', 'PRODUCING', 'WAIT_PICKUP', 'WAIT_SHIP')")
    Long countPendingFulfillment();

    /** 平均评分（无评价返回 0） */
    @Select("SELECT IFNULL(AVG(rating), 0) FROM review WHERE deleted = 0")
    BigDecimal avgRating();

    /** 履约方式分布：delivery_type -> count */
    @Select("SELECT delivery_type AS k, COUNT(*) AS c FROM orders"
            + " WHERE deleted = 0 AND status NOT IN ('PENDING_PAY', 'CANCELLED')"
            + " GROUP BY delivery_type")
    List<Map<String, Object>> deliveryDist();

    /** 评分分布：rating -> count */
    @Select("SELECT rating AS k, COUNT(*) AS c FROM review WHERE deleted = 0 GROUP BY rating")
    List<Map<String, Object>> ratingDist();

    /** 按支付日期聚合销售额与订单量；days=0 表示全部 */
    @Select("SELECT DATE_FORMAT(pay_time, '%Y-%m-%d') AS d, COUNT(*) AS c, IFNULL(SUM(total_amount), 0) AS s"
            + " FROM orders WHERE deleted = 0 AND status NOT IN ('PENDING_PAY', 'CANCELLED') AND pay_time IS NOT NULL"
            + " AND (#{days} = 0 OR pay_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY))"
            + " GROUP BY d ORDER BY d")
    List<Map<String, Object>> trendByDay(@Param("days") int days);

    /** 有效订单明细的设计参数快照与数量（设计偏好统计原料，内存聚合） */
    @Select("SELECT oi.design_params AS p, oi.quantity AS q FROM order_item oi"
            + " JOIN orders o ON oi.order_id = o.id"
            + " WHERE o.deleted = 0 AND o.status NOT IN ('PENDING_PAY', 'CANCELLED')")
    List<Map<String, Object>> validOrderItemParams();
}
