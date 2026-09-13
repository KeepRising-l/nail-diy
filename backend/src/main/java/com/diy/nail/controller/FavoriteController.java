package com.diy.nail.controller;

import com.diy.nail.common.PageResult;
import com.diy.nail.common.Result;
import com.diy.nail.entity.Template;
import com.diy.nail.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 顾客端接口：款式收藏（需登录）
 */
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    /** 收藏/取消收藏（切换），返回切换后是否已收藏 */
    @PostMapping("/{templateId}/toggle")
    public Result<Boolean> toggle(Authentication authentication, @PathVariable Long templateId) {
        return Result.ok(favoriteService.toggle(currentUserId(authentication), templateId));
    }

    /** 当前用户收藏的模板 ID 列表 */
    @GetMapping("/ids")
    public Result<List<Long>> ids(Authentication authentication) {
        return Result.ok(favoriteService.listIds(currentUserId(authentication)));
    }

    /** 收藏的模板分页（按收藏时间倒序） */
    @GetMapping
    public Result<PageResult<Template>> page(Authentication authentication,
                                             @RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "12") long size) {
        return Result.ok(favoriteService.page(currentUserId(authentication), page, size));
    }
}
