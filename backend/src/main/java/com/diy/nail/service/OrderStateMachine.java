package com.diy.nail.service;

import com.diy.nail.common.BusinessException;
import com.diy.nail.entity.Order;

import java.util.Map;
import java.util.Set;

/**
 * 订单状态机：全系统唯一流转规则（REQ-FUL-1），所有状态变更必须经过此处校验，
 * 杜绝散落的魔法流转判断
 */
public final class OrderStateMachine {

    /** 非法流转统一提示 */
    public static final String ILLEGAL_TRANSITION = "订单当前状态不允许该操作";

    /** 唯一流转规则表：from -> 允许的 to 集合（终态：VERIFIED/RECEIVED/CANCELLED 无出边） */
    private static final Map<String, Set<String>> RULES = Map.of(
            Order.STATUS_PENDING_PAY, Set.of(Order.STATUS_PAID, Order.STATUS_CANCELLED),
            Order.STATUS_PAID, Set.of(Order.STATUS_PRODUCING, Order.STATUS_CANCELLED),
            Order.STATUS_PRODUCING, Set.of(Order.STATUS_WAIT_PICKUP, Order.STATUS_WAIT_SHIP),
            Order.STATUS_WAIT_PICKUP, Set.of(Order.STATUS_VERIFIED),
            Order.STATUS_WAIT_SHIP, Set.of(Order.STATUS_SHIPPED),
            Order.STATUS_SHIPPED, Set.of(Order.STATUS_RECEIVED)
    );

    private OrderStateMachine() {
    }

    /** 校验流转是否合法，非法（含未知/空状态等脏数据）则抛业务异常（中文提示） */
    public static void assertTransition(String from, String to) {
        Set<String> allowed = from == null ? null : RULES.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new BusinessException(ILLEGAL_TRANSITION);
        }
    }
}
