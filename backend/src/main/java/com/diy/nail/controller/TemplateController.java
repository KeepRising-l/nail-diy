package com.diy.nail.controller;

import com.diy.nail.common.PageResult;
import com.diy.nail.common.Result;
import com.diy.nail.entity.Template;
import com.diy.nail.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 顾客端接口：浏览上架款式模板（需登录）
 */
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    /** 分页浏览上架模板（风格标签可选筛选），按排序号升序 */
    @GetMapping
    public Result<PageResult<Template>> page(
            @RequestParam(required = false) String styleTag,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(templateService.pageOnSale(styleTag, page, size));
    }

    /** 模板详情（仅上架可见，供"一键套用"取设计参数） */
    @GetMapping("/{id}")
    public Result<Template> detail(@PathVariable Long id) {
        return Result.ok(templateService.detail(id));
    }
}
