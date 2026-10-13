package com.diy.nail.controller;

import com.diy.nail.common.PageResult;
import com.diy.nail.common.Result;
import com.diy.nail.entity.Store;
import com.diy.nail.service.StoreService;
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
 * 商家端接口：自提门店管理（仅 MERCHANT/ADMIN 可访问）
 */
@RestController
@RequestMapping("/api/merchant/stores")
@RequiredArgsConstructor
public class MerchantStoreController {

    private final StoreService storeService;

    /** 门店分页（关键字/状态可选筛选） */
    @GetMapping
    public Result<PageResult<Store>> page(@RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Integer status,
                                          @RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size) {
        return Result.ok(storeService.page(keyword, status, page, size));
    }

    /** 新增门店 */
    @PostMapping
    public Result<Long> create(@RequestBody Store store) {
        return Result.ok(storeService.create(store));
    }

    /** 编辑门店 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Store store) {
        storeService.update(id, store);
        return Result.ok();
    }

    /** 删除门店（逻辑删除） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        storeService.delete(id);
        return Result.ok();
    }
}
