package com.diy.nail.controller;

import com.diy.nail.common.PageResult;
import com.diy.nail.common.Result;
import com.diy.nail.dto.TemplateRequest;
import com.diy.nail.entity.Template;
import com.diy.nail.service.TemplateService;
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
 * 商家端接口：款式模板增删改查（仅 MERCHANT/ADMIN 可访问）
 */
@RestController
@RequestMapping("/api/merchant/templates")
@RequiredArgsConstructor
public class MerchantTemplateController {

    private final TemplateService templateService;

    /** 分页列表：按风格标签/状态/名称关键字筛选 */
    @GetMapping
    public Result<PageResult<Template>> page(
            @RequestParam(required = false) String styleTag,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(templateService.page(styleTag, status, keyword, page, size));
    }

    /** 新增模板 */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody TemplateRequest request) {
        return Result.ok(templateService.create(request));
    }

    /** 编辑模板（含上下架切换） */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody TemplateRequest request) {
        templateService.update(id, request);
        return Result.ok();
    }

    /** 删除模板（逻辑删除） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return Result.ok();
    }
}
