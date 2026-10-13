package com.diy.nail.controller;

import com.diy.nail.common.Result;
import com.diy.nail.dto.AddressRequest;
import com.diy.nail.entity.Address;
import com.diy.nail.service.AddressService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 顾客端接口：地址簿（需登录，仅本人）
 */
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    /** 当前登录用户 ID */
    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    /** 地址列表 */
    @GetMapping
    public Result<List<Address>> list(Authentication authentication) {
        return Result.ok(addressService.list(currentUserId(authentication)));
    }

    /** 新增地址（最多 10 条） */
    @PostMapping
    public Result<Long> create(Authentication authentication, @Valid @RequestBody AddressRequest request) {
        return Result.ok(addressService.create(currentUserId(authentication), request));
    }

    /** 编辑地址 */
    @PutMapping("/{id}")
    public Result<Void> update(Authentication authentication, @PathVariable Long id,
                               @Valid @RequestBody AddressRequest request) {
        addressService.update(currentUserId(authentication), id, request);
        return Result.ok();
    }

    /** 设为默认地址 */
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(Authentication authentication, @PathVariable Long id) {
        addressService.setDefault(currentUserId(authentication), id);
        return Result.ok();
    }

    /** 删除地址 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(Authentication authentication, @PathVariable Long id) {
        addressService.delete(currentUserId(authentication), id);
        return Result.ok();
    }
}
