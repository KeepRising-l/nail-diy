package com.diy.nail.controller;

import com.diy.nail.common.PageResult;
import com.diy.nail.common.Result;
import com.diy.nail.dto.ReviewReplyRequest;
import com.diy.nail.service.ReviewService;
import com.diy.nail.vo.ReviewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家端接口：评价管理（仅 MERCHANT/ADMIN 可访问）
 */
@RestController
@RequestMapping("/api/merchant/reviews")
@RequiredArgsConstructor
public class MerchantReviewController {

    private final ReviewService reviewService;

    /** 评价分页（评分可选筛选） */
    @GetMapping
    public Result<PageResult<ReviewVO>> page(@RequestParam(required = false) Integer rating,
                                             @RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size) {
        return Result.ok(reviewService.merchantPage(rating, page, size));
    }

    /** 回复评价 */
    @PostMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable Long id, @Valid @RequestBody ReviewReplyRequest request) {
        reviewService.reply(id, request.getReply());
        return Result.ok();
    }

    /** 删除违规评价（逻辑删除） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reviewService.delete(id);
        return Result.ok();
    }
}
