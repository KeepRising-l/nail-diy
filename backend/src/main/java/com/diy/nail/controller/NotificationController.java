package com.diy.nail.controller;

import com.diy.nail.common.PageResult;
import com.diy.nail.common.Result;
import com.diy.nail.entity.Notification;
import com.diy.nail.service.NotificationService;
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
 * 消息通知接口（需登录，只能读写本人的通知）。
 *
 * <p>前端在顶栏放铃铛入口：登录后拉未读数与最新几条，点击标记已读。
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** 当前登录用户 ID */
    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    /** 我的通知分页 */
    @GetMapping
    public Result<PageResult<Notification>> page(Authentication authentication,
                                                 @RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size) {
        return Result.ok(notificationService.page(currentUserId(authentication), page, size));
    }

    /** 最新若干条（顶栏下拉面板，避免拉全量） */
    @GetMapping("/latest")
    public Result<List<Notification>> latest(Authentication authentication,
                                             @RequestParam(defaultValue = "8") int limit) {
        return Result.ok(notificationService.latest(currentUserId(authentication), limit));
    }

    /** 未读数量（红点） */
    @GetMapping("/unread-count")
    public Result<Long> unreadCount(Authentication authentication) {
        return Result.ok(notificationService.unreadCount(currentUserId(authentication)));
    }

    /** 标记单条已读 */
    @PostMapping("/{id}/read")
    public Result<Void> markRead(Authentication authentication, @PathVariable Long id) {
        notificationService.markRead(currentUserId(authentication), id);
        return Result.ok();
    }

    /** 全部标记已读，返回影响条数 */
    @PostMapping("/read-all")
    public Result<Integer> markAllRead(Authentication authentication) {
        return Result.ok(notificationService.markAllRead(currentUserId(authentication)));
    }
}
