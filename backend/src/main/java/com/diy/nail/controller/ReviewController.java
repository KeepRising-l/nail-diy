package com.diy.nail.controller;

import com.diy.nail.common.Result;
import com.diy.nail.dto.ReviewRequest;
import com.diy.nail.service.FileService;
import com.diy.nail.service.ReviewService;
import com.diy.nail.vo.ReviewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 顾客端接口：评价（需登录；提交仅限本人已完成订单，查看公开）
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final FileService fileService;

    /** 当前登录用户 ID */
    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    /** 提交评价（每单限一次） */
    @PostMapping
    public Result<Void> create(Authentication authentication, @Valid @RequestBody ReviewRequest request) {
        reviewService.create(currentUserId(authentication), request);
        return Result.ok();
    }

    /** 查某订单的评价（公开展示，昵称脱敏；无评价 data 为 null） */
    @GetMapping
    public Result<ReviewVO> byOrder(@RequestParam Long orderId) {
        return Result.ok(reviewService.getByOrderId(orderId));
    }

    /** 晒图上传：规则同素材图片（jpg/png/webp ≤5MB） */
    @PostMapping("/images")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileService.uploadImage(file));
    }
}
