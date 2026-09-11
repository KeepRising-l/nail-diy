package com.diy.nail.controller;

import com.diy.nail.common.PageResult;
import com.diy.nail.common.Result;
import com.diy.nail.dto.ShipRequest;
import com.diy.nail.dto.VerifyRequest;
import com.diy.nail.service.OrderService;
import com.diy.nail.vo.OrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家端接口：订单中心与履约流转（仅 MERCHANT/ADMIN 可访问）
 */
@RestController
@RequestMapping("/api/merchant/orders")
@RequiredArgsConstructor
public class MerchantOrderController {

    private final OrderService orderService;

    /** 订单分页（状态 Tab 筛选） */
    @GetMapping
    public Result<PageResult<OrderVO>> page(@RequestParam(required = false) String status,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(orderService.merchantPage(status, page, size));
    }

    /** 订单详情（含明细/门店/状态日志，商家可见设计参数） */
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.merchantDetail(id));
    }

    /** 接单：待接单 → 制作中 */
    @PostMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable Long id) {
        orderService.accept(id);
        return Result.ok();
    }

    /** 制作完成：自提 → 待自提（生成自提码）；邮寄 → 待发货 */
    @PostMapping("/{id}/produce")
    public Result<Void> produce(@PathVariable Long id) {
        orderService.produce(id);
        return Result.ok();
    }

    /** 自提核销（校验顾客出示的自提码） */
    @PostMapping("/{id}/verify")
    public Result<Void> verify(@PathVariable Long id, @Valid @RequestBody VerifyRequest request) {
        orderService.verify(id, request.getPickupCode());
        return Result.ok();
    }

    /** 邮寄发货（填写快递公司+单号） */
    @PostMapping("/{id}/ship")
    public Result<Void> ship(@PathVariable Long id, @Valid @RequestBody ShipRequest request) {
        orderService.ship(id, request);
        return Result.ok();
    }

    /** 商家取消订单（仅待支付/待接单阶段） */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.merchantCancel(id);
        return Result.ok();
    }
}
