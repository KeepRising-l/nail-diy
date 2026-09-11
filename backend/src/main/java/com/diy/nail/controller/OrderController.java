package com.diy.nail.controller;

import com.diy.nail.common.PageResult;
import com.diy.nail.common.Result;
import com.diy.nail.dto.OrderCreateRequest;
import com.diy.nail.service.OrderService;
import com.diy.nail.vo.OrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 顾客端接口：我的订单（需登录，仅本人）
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 当前登录用户 ID */
    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    /** 我的订单分页（状态可选筛选，含明细） */
    @GetMapping
    public Result<PageResult<OrderVO>> page(Authentication authentication,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(orderService.page(currentUserId(authentication), status, page, size));
    }

    /** 订单详情（含明细与门店） */
    @GetMapping("/{id}")
    public Result<OrderVO> detail(Authentication authentication, @PathVariable Long id) {
        return Result.ok(orderService.detail(currentUserId(authentication), id));
    }

    /** 提交订单（整购物车结算），返回订单 id */
    @PostMapping
    public Result<Long> create(Authentication authentication, @Valid @RequestBody OrderCreateRequest request) {
        return Result.ok(orderService.create(currentUserId(authentication), request));
    }

    /** 模拟支付（待支付 → 待接单） */
    @PostMapping("/{id}/pay")
    public Result<Void> pay(Authentication authentication, @PathVariable Long id) {
        orderService.pay(currentUserId(authentication), id);
        return Result.ok();
    }

    /** 取消订单（仅待支付/待接单） */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(Authentication authentication, @PathVariable Long id) {
        orderService.cancel(currentUserId(authentication), id);
        return Result.ok();
    }

    /** 确认收货（已发货 → 已签收） */
    @PostMapping("/{id}/receive")
    public Result<Void> receive(Authentication authentication, @PathVariable Long id) {
        orderService.receive(currentUserId(authentication), id);
        return Result.ok();
    }
}
