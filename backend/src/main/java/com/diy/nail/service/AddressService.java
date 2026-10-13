package com.diy.nail.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.diy.nail.common.BusinessException;
import com.diy.nail.common.ResultCode;
import com.diy.nail.dto.AddressRequest;
import com.diy.nail.entity.Address;
import com.diy.nail.mapper.AddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 地址簿业务：增删改查 + 设默认（仅本人，最多 10 条）
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    /** 每人最多保存的地址数 */
    private static final long MAX_ADDRESS_COUNT = 10;

    private final AddressMapper addressMapper;

    /** 地址列表（默认地址排最前） */
    public List<Address> list(Long userId) {
        return addressMapper.selectList(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getId));
    }

    /** 新增地址：超上限拦截；设默认则清除其他默认；首条自动默认 */
    public Long create(Long userId, AddressRequest request) {
        Long count = addressMapper.selectCount(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId));
        if (count != null && count >= MAX_ADDRESS_COUNT) {
            throw new BusinessException("最多保存 10 条收货地址");
        }
        Address address = new Address();
        address.setUserId(userId);
        copyFromRequest(address, request);
        boolean wantDefault = request.getIsDefault() != null && request.getIsDefault() == 1;
        address.setIsDefault(wantDefault || count == 0 ? 1 : 0);
        if (wantDefault) {
            clearDefault(userId);
        }
        addressMapper.insert(address);
        return address.getId();
    }

    /** 编辑地址（仅本人） */
    public void update(Long userId, Long id, AddressRequest request) {
        Address address = getOwned(userId, id);
        boolean wantDefault = request.getIsDefault() != null && request.getIsDefault() == 1;
        if (wantDefault) {
            clearDefault(userId);
        }
        copyFromRequest(address, request);
        address.setIsDefault(wantDefault ? 1 : 0);
        addressMapper.updateById(address);
    }

    /** 删除地址（仅本人） */
    public void delete(Long userId, Long id) {
        getOwned(userId, id);
        addressMapper.deleteById(id);
    }

    /** 设为默认地址（仅本人，自动取消其他默认） */
    public void setDefault(Long userId, Long id) {
        getOwned(userId, id);
        clearDefault(userId);
        addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                .eq(Address::getId, id)
                .set(Address::getIsDefault, 1));
    }

    /** 按 id 查本人地址 */
    private Address getOwned(Long userId, Long id) {
        Address address = addressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return address;
    }

    /** 清除该用户所有默认标记 */
    private void clearDefault(Long userId) {
        addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                .eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, 1)
                .set(Address::getIsDefault, 0));
    }

    /** 请求字段拷贝到实体 */
    private void copyFromRequest(Address address, AddressRequest request) {
        address.setReceiver(request.getReceiver());
        address.setPhone(request.getPhone());
        address.setProvince(request.getProvince());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setDetail(request.getDetail());
    }
}
