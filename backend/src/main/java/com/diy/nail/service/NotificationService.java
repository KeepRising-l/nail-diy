package com.diy.nail.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diy.nail.common.PageResult;
import com.diy.nail.entity.Notification;
import com.diy.nail.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息通知业务。
 *
 * <p>设计取舍：不做 WebSocket 实时推送，采用「登录后拉取 + 未读红点」的轻量方案 ——
 * 实现成本低、对现有架构零侵入，也匹配毕设规模。若后续要实时化，把 send 改为
 * 「写库 + 推送」即可，调用方无需改动。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    /**
     * 发送一条通知（供各业务节点调用）。
     *
     * <p>通知属于辅助功能，写入失败只记日志、不抛异常 —— 避免因为一条通知写不进去
     * 而把订单状态变更等主业务事务一起回滚。
     */
    public void send(Long userId, String type, String title, String content, Long bizId, String bizType) {
        if (userId == null || title == null) {
            return;
        }
        try {
            Notification n = new Notification();
            n.setUserId(userId);
            n.setType(type);
            n.setTitle(title);
            n.setContent(content == null ? "" : content);
            n.setBizId(bizId);
            n.setBizType(bizType == null ? "" : bizType);
            n.setIsRead(0);
            notificationMapper.insert(n);
        } catch (Exception e) {
            log.warn("通知写入失败 userId={} title={}", userId, title, e);
        }
    }

    /** 我的通知分页（倒序） */
    public PageResult<Notification> page(Long userId, long page, long size) {
        Page<Notification> result = notificationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /** 未读数量（顶栏红点用） */
    public long unreadCount(Long userId) {
        Long count = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
        return count == null ? 0 : count;
    }

    /** 标记单条已读（仅本人） */
    public void markRead(Long userId, Long id) {
        Notification n = notificationMapper.selectById(id);
        if (n == null || !userId.equals(n.getUserId())) {
            return;
        }
        if (Integer.valueOf(0).equals(n.getIsRead())) {
            n.setIsRead(1);
            n.setReadTime(LocalDateTime.now());
            notificationMapper.updateById(n);
        }
    }

    /** 全部标记已读 */
    public int markAllRead(Long userId) {
        return notificationMapper.markAllRead(userId);
    }

    /** 最新若干条（顶栏下拉面板用，避免拉全量） */
    public List<Notification> latest(Long userId, int limit) {
        return notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getId)
                .last("LIMIT " + Math.max(1, Math.min(limit, 50))));
    }
}
