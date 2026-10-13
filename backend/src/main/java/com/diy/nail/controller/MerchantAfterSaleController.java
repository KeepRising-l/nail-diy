package com.diy.nail.controller;

import com.diy.nail.common.PageResult;
import com.diy.nail.common.Result;
import com.diy.nail.dto.AfterSaleHandleRequest;
import com.diy.nail.service.AfterSaleService;
import com.diy.nail.vo.AfterSaleVO;
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
 * 商家端接口：售后处理。
 * 路径在 /api/merchant/** 下，已由 SecurityConfig 限制为 MERCHANT / ADMIN 角色。
 */
@RestController
@RequestMapping("/api/merchant/after-sales")
@RequiredArgsConstructor
public class MerchantAfterSaleController {

    private final AfterSaleService afterSaleService;

    /** 售后单分页（可按状态筛选：PENDING / COMPLETED / REJECTED） */
    @GetMapping
    public Result<PageResult<AfterSaleVO>> page(@RequestParam(required = false) String status,
                                                @RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size) {
        return Result.ok(afterSaleService.merchantPage(status, page, size));
    }

    /** 待处理数量（看板角标用） */
    @GetMapping("/pending-count")
    public Result<Long> pendingCount() {
        return Result.ok(afterSaleService.pendingCount());
    }

    /** 处理售后：同意退款 / 拒绝（拒绝需填原因） */
    @PostMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @Valid @RequestBody AfterSaleHandleRequest request) {
        afterSaleService.handle(id, request);
        return Result.ok();
    }
}
