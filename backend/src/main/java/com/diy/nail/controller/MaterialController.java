package com.diy.nail.controller;

import com.diy.nail.common.Result;
import com.diy.nail.entity.Material;
import com.diy.nail.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 顾客端接口：浏览上架素材（需登录）
 */
@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    /** 按类型获取上架素材（不传 type 返回全部；图案可按 category 分类），按排序号升序 */
    @GetMapping
    public Result<List<Material>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category) {
        return Result.ok(materialService.listOnSale(type, category));
    }
}
