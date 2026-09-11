package com.diy.nail.controller;

import com.diy.nail.common.PageResult;
import com.diy.nail.common.Result;
import com.diy.nail.dto.DesignRequest;
import com.diy.nail.entity.Design;
import com.diy.nail.service.DesignService;
import com.diy.nail.service.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 顾客端接口：我的设计（保存/列表/编辑/删除）+ 预览快照上传（需登录，仅本人可操作）
 */
@RestController
@RequestMapping("/api/designs")
@RequiredArgsConstructor
public class DesignController {

    private final DesignService designService;
    private final FileService fileService;

    /** 当前登录用户 ID（由 JWT 过滤器写入 SecurityContext） */
    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    /** 我的设计分页列表 */
    @GetMapping
    public Result<PageResult<Design>> page(Authentication authentication,
                                           @RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size) {
        return Result.ok(designService.page(currentUserId(authentication), page, size));
    }

    /** 保存设计，返回新设计 id */
    @PostMapping
    public Result<Long> create(Authentication authentication, @Valid @RequestBody DesignRequest request) {
        return Result.ok(designService.create(currentUserId(authentication), request));
    }

    /** 设计详情（供编辑回填） */
    @GetMapping("/{id}")
    public Result<Design> detail(Authentication authentication, @PathVariable Long id) {
        return Result.ok(designService.detail(currentUserId(authentication), id));
    }

    /** 编辑设计 */
    @PutMapping("/{id}")
    public Result<Void> update(Authentication authentication, @PathVariable Long id,
                               @Valid @RequestBody DesignRequest request) {
        designService.update(currentUserId(authentication), id, request);
        return Result.ok();
    }

    /** 删除设计 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(Authentication authentication, @PathVariable Long id) {
        designService.delete(currentUserId(authentication), id);
        return Result.ok();
    }

    /** 预览快照上传：前端截图后上传，规则同素材图片（jpg/png/webp ≤5MB） */
    @PostMapping("/upload/preview")
    public Result<String> uploadPreview(Authentication authentication, @RequestParam("file") MultipartFile file) {
        return Result.ok(fileService.uploadImage(file));
    }
}
