package com.diy.nail.controller;

import com.diy.nail.common.Result;
import com.diy.nail.dto.CartRequest;
import com.diy.nail.service.CartService;
import com.diy.nail.vo.CartVO;
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
import java.util.Map;

/**
 * 顾客端接口：购物车（需登录，仅本人）
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /** 当前登录用户 ID */
    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    /** 购物车列表（含设计快照） */
    @GetMapping
    public Result<List<CartVO>> list(Authentication authentication) {
        return Result.ok(cartService.list(currentUserId(authentication)));
    }

    /** 加入购物车（已存在则数量累加） */
    @PostMapping
    public Result<Void> add(Authentication authentication, @Valid @RequestBody CartRequest request) {
        cartService.add(currentUserId(authentication), request);
        return Result.ok();
    }

    /** 修改数量 */
    @PutMapping("/{id}")
    public Result<Void> updateQuantity(Authentication authentication, @PathVariable Long id,
                                       @RequestBody Map<String, Integer> body) {
        cartService.updateQuantity(currentUserId(authentication), id, body.get("quantity"));
        return Result.ok();
    }

    /** 删除条目 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(Authentication authentication, @PathVariable Long id) {
        cartService.delete(currentUserId(authentication), id);
        return Result.ok();
    }

    /** 清空购物车 */
    @DeleteMapping
    public Result<Void> clear(Authentication authentication) {
        cartService.clear(currentUserId(authentication));
        return Result.ok();
    }
}
