package com.diy.nail.vo;

import com.diy.nail.entity.Order;
import com.diy.nail.entity.OrderItem;
import com.diy.nail.entity.OrderStatusLog;
import com.diy.nail.entity.Store;
import lombok.Data;

import java.util.List;

/**
 * 订单视图：订单主体 + 商品明细 + 自提门店信息 + 状态时间线日志（自提单展示用）
 */
@Data
public class OrderVO {

    /** 订单主体 */
    private Order order;

    /** 商品明细（设计快照） */
    private List<OrderItem> items;

    /** 自提门店（仅自提订单有值） */
    private Store store;

    /** 状态变更日志（时间线） */
    private List<OrderStatusLog> logs;

    /** 该订单的评价（未评价为 null，列表/详情直接可判断已评价） */
    private ReviewVO review;
}
