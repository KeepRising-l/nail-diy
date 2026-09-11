package com.diy.nail.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diy.nail.common.Result;
import com.diy.nail.entity.Store;
import com.diy.nail.mapper.StoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 顾客端接口：自提门店浏览（需登录）
 */
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreMapper storeMapper;

    /** 营业中门店列表（结算选自提点用） */
    @GetMapping
    public Result<List<Store>> list() {
        return Result.ok(storeMapper.selectList(new LambdaQueryWrapper<Store>()
                .eq(Store::getStatus, 1)
                .orderByAsc(Store::getSort)
                .orderByAsc(Store::getId)));
    }
}
