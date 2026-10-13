package com.diy.nail.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diy.nail.common.BusinessException;
import com.diy.nail.common.PageResult;
import com.diy.nail.common.ResultCode;
import com.diy.nail.entity.Store;
import com.diy.nail.mapper.StoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 门店业务：商家端自提门店增删改查
 */
@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreMapper storeMapper;

    /** 门店分页（名称/地址关键字、状态可选筛选），排序号小的在前 */
    public PageResult<Store> page(String keyword, Integer status, long page, long size) {
        LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<Store>()
                .and(StrUtil.isNotBlank(keyword),
                        w -> w.like(Store::getName, keyword).or().like(Store::getAddress, keyword))
                .eq(status != null, Store::getStatus, status)
                .orderByAsc(Store::getSort)
                .orderByAsc(Store::getId);
        Page<Store> result = storeMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /** 新增门店（默认营业中） */
    public Long create(Store store) {
        validate(store);
        store.setStatus(store.getStatus() == null ? 1 : store.getStatus());
        store.setSort(store.getSort() == null ? 0 : store.getSort());
        storeMapper.insert(store);
        return store.getId();
    }

    /** 编辑门店（含营业状态切换） */
    public void update(Long id, Store store) {
        Store existing = getById(id);
        validate(store);
        existing.setName(store.getName());
        existing.setAddress(store.getAddress());
        existing.setPhone(StrUtil.nullToEmpty(store.getPhone()));
        existing.setBusinessHours(StrUtil.nullToEmpty(store.getBusinessHours()));
        existing.setStatus(store.getStatus() == null ? existing.getStatus() : store.getStatus());
        existing.setSort(store.getSort() == null ? existing.getSort() : store.getSort());
        storeMapper.updateById(existing);
    }

    /** 删除门店（逻辑删除；历史自提订单引用的是订单快照，不受影响） */
    public void delete(Long id) {
        getById(id);
        storeMapper.deleteById(id);
    }

    /** 按 ID 查门店（不存在则抛业务异常） */
    public Store getById(Long id) {
        Store store = storeMapper.selectById(id);
        if (store == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return store;
    }

    private void validate(Store store) {
        if (StrUtil.isBlank(store.getName())) {
            throw new BusinessException("门店名称不能为空");
        }
        if (StrUtil.isBlank(store.getAddress())) {
            throw new BusinessException("门店地址不能为空");
        }
    }
}
