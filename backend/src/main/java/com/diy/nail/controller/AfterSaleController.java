package com.diy.nail.controller;

import com.diy.nail.common.PageResult;
import com.diy.nail.common.Result;
import com.diy.nail.dto.AfterSaleRequest;
import com.diy.nail.service.AfterSaleService;
import com.diy.nail.vo.AfterSaleVO;
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
 * 顾客端接口：售后申请（需登录，只能操作本人的订单）。
 */
@RestController
@RequestMapping("/api/after-sales")
@RequiredArgsConstructor
public class AfterSaleController {

    private final AfterSaleService afterSaleService;

    /** 当前登录用户 ID（由 JWT 过滤器写入 SecurityContext） */
    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    /** 提交售后申请，返回新售后单 id */
    @PostMapping
    public Result<Long> apply(Authentication authentication, @Valid @RequestBody AfterSaleRequest request) {
        return Result.ok(afterSaleService.apply(currentUserId(authentication), request));
    }

    /** 我的售后单分页 */
    @GetMapping
    public Result<PageResult<AfterSaleVO>> page(Authentication authentication,
                                                @RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size) {
        return Result.ok(afterSaleService.myPage(currentUserId(authentication), page, size));
    }

    /** 查某订单的售后单（订单详情展示用，无售后时 data 为 null） */
    @GetMapping("/by-order/{orderId}")
    public Result<AfterSaleVO> byOrder(Authentication authentication, @PathVariable Long orderId) {
        return Result.ok(afterSaleService.byOrder(currentUserId(authentication), orderId));
    }
}
