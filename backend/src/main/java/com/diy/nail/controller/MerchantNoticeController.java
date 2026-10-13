package com.diy.nail.controller;

import com.diy.nail.common.PageResult;
import com.diy.nail.common.Result;
import com.diy.nail.dto.NoticeRequest;
import com.diy.nail.entity.Notice;
import com.diy.nail.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家端接口：公告管理（仅 MERCHANT/ADMIN 可访问）
 */
@RestController
@RequestMapping("/api/merchant/notices")
@RequiredArgsConstructor
public class MerchantNoticeController {

    private final NoticeService noticeService;

    /** 公告分页（标题关键字/状态可选筛选） */
    @GetMapping
    public Result<PageResult<Notice>> page(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size) {
        return Result.ok(noticeService.page(keyword, status, page, size));
    }

    /** 新增公告 */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody NoticeRequest request) {
        return Result.ok(noticeService.create(request));
    }

    /** 编辑公告 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody NoticeRequest request) {
        noticeService.update(id, request);
        return Result.ok();
    }

    /** 删除公告（逻辑删除） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return Result.ok();
    }
}
