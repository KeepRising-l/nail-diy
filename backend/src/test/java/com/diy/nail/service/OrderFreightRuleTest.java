package com.diy.nail.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 运费规则单元测试：邮寄订单"满额包邮、否则固定运费"。
 * 测试口径与 application.yml 的 nail.freight.amount=8 / free-over=99 对齐；
 * 到店自提固定免运费，不经过该规则。
 */
class OrderFreightRuleTest {

    private static final BigDecimal FIXED_FREIGHT = new BigDecimal("8");
    private static final BigDecimal FREE_OVER = new BigDecimal("99");

    private static BigDecimal freightOf(String goodsAmount) {
        return OrderService.calcFreight(new BigDecimal(goodsAmount), FREE_OVER, FIXED_FREIGHT);
    }

    private static void assertFreightIs(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "运费应为 " + expected + "，实际为 " + actual);
    }

    @Test
    @DisplayName("商品金额恰好等于包邮门槛（99 元）时免运费——边界含等于")
    void freeExactlyAtThreshold() {
        assertFreightIs("0", freightOf("99"));
    }

    @Test
    @DisplayName("商品金额超过包邮门槛时免运费")
    void freeAboveThreshold() {
        assertFreightIs("0", freightOf("99.01"));
        assertFreightIs("0", freightOf("120"));
    }

    @Test
    @DisplayName("商品金额未达包邮门槛时收取固定运费 8 元")
    void chargedBelowThreshold() {
        assertFreightIs("8", freightOf("98.99"));
        assertFreightIs("8", freightOf("50"));
    }

    @Test
    @DisplayName("0 元商品金额同样按未达门槛收取运费")
    void chargedOnZeroAmount() {
        assertFreightIs("8", freightOf("0"));
    }
}
