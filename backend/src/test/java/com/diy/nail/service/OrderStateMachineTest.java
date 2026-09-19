package com.diy.nail.service;

import com.diy.nail.common.BusinessException;
import com.diy.nail.entity.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 订单状态机单元测试：覆盖全部合法流转、三个终态封口、越阶段非法跳转与异常输入。
 * 合法流转表按论文中的状态流转图独立硬编码，实现规则表被误改时测试即失败。
 */
class OrderStateMachineTest {

    /** 全部 9 条合法流转，与状态流转图一一对应 */
    @ParameterizedTest(name = "[{index}] {0} → {1} 应放行")
    @CsvSource({
            "PENDING_PAY, PAID",        // 模拟支付
            "PENDING_PAY, CANCELLED",   // 顾客取消
            "PAID, PRODUCING",          // 商家接单
            "PAID, CANCELLED",          // 顾客/商家取消
            "PRODUCING, WAIT_PICKUP",   // 自提单制作完成
            "PRODUCING, WAIT_SHIP",     // 邮寄单制作完成
            "WAIT_PICKUP, VERIFIED",    // 自提码核销
            "WAIT_SHIP, SHIPPED",       // 商家发货
            "SHIPPED, RECEIVED"         // 顾客确认收货
    })
    void legalTransitionPasses(String from, String to) {
        assertDoesNotThrow(() -> OrderStateMachine.assertTransition(from, to));
    }

    /** 三个终态（已核销/已签收/已取消）无出边，任何操作都应被拦截 */
    @ParameterizedTest(name = "[{index}] 终态 {0} 不允许任何流转")
    @ValueSource(strings = {Order.STATUS_VERIFIED, Order.STATUS_RECEIVED, Order.STATUS_CANCELLED})
    void terminalStateRejectsAnyTransition(String terminal) {
        String[] targets = {
                Order.STATUS_PENDING_PAY, Order.STATUS_PAID, Order.STATUS_PRODUCING,
                Order.STATUS_WAIT_PICKUP, Order.STATUS_WAIT_SHIP, Order.STATUS_SHIPPED,
                Order.STATUS_RECEIVED, Order.STATUS_VERIFIED, Order.STATUS_CANCELLED};
        for (String to : targets) {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> OrderStateMachine.assertTransition(terminal, to));
            assertEquals(OrderStateMachine.ILLEGAL_TRANSITION, ex.getMessage());
        }
    }

    /** 越阶段跳转与串履约方式的非法流转，应统一拦截并给出固定提示 */
    @ParameterizedTest(name = "[{index}] {0} → {1} 应拦截")
    @CsvSource({
            "PENDING_PAY, PRODUCING",   // 未支付不能接单
            "PENDING_PAY, WAIT_SHIP",   // 未支付不能发货
            "PAID, SHIPPED",            // 未制作不能发货
            "PAID, RECEIVED",           // 未发货不能签收
            "PRODUCING, RECEIVED",      // 未交付不能签收
            "WAIT_PICKUP, SHIPPED",     // 自提单不能走邮寄发货
            "WAIT_SHIP, VERIFIED",      // 邮寄单不能走自提核销
            "SHIPPED, VERIFIED",        // 已发货不能自提核销
            "CANCELLED, PAID"           // 已取消订单不能复活
    })
    void illegalTransitionIsRejected(String from, String to) {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> OrderStateMachine.assertTransition(from, to));
        assertEquals(OrderStateMachine.ILLEGAL_TRANSITION, ex.getMessage());
    }

    @Test
    @DisplayName("未知或空的来源状态一律拦截")
    void unknownFromStateIsRejected() {
        assertThrows(BusinessException.class,
                () -> OrderStateMachine.assertTransition("NOT_A_STATUS", Order.STATUS_PAID));
        assertThrows(BusinessException.class,
                () -> OrderStateMachine.assertTransition(null, Order.STATUS_PAID));
    }
}
