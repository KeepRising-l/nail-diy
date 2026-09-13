package com.diy.nail.controller;

import com.diy.nail.common.Result;
import com.diy.nail.entity.Notice;
import com.diy.nail.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 顾客端接口：已发布公告浏览（首页公告栏，游客可访问）
 */
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /** 已发布公告（置顶优先、新的在前，取前 5 条） */
    @GetMapping
    public Result<List<Notice>> published() {
        return Result.ok(noticeService.listPublished());
    }
}
