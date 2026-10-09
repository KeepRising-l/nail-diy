package com.diy.nail.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diy.nail.common.BusinessException;
import com.diy.nail.common.ResultCode;
import com.diy.nail.dto.CartRequest;
import com.diy.nail.entity.Cart;
import com.diy.nail.entity.Design;
import com.diy.nail.mapper.CartMapper;
import com.diy.nail.mapper.DesignMapper;
import com.diy.nail.vo.CartVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 购物车业务：加入/改数量/删除/清空（仅本人）
 */
@Service
@RequiredArgsConstructor
public class CartService {

    public static final int MAX_QUANTITY = 99;

    private final CartMapper cartMapper;
    private final DesignMapper designMapper;

    /** 购物车列表（附设计快照；设计已删除的条目自动忽略） */
    public List<CartVO> list(Long userId) {
        List<Cart> carts = cartMapper.selectList(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .orderByDesc(Cart::getId));
        List<CartVO> result = new ArrayList<>();
        for (Cart cart : carts) {
            Design design = designMapper.selectById(cart.getDesignId());
            if (design == null || !userId.equals(design.getUserId())) {
                continue;
            }
            CartVO vo = new CartVO();
            vo.setId(cart.getId());
            vo.setDesignId(design.getId());
            vo.setQuantity(cart.getQuantity());
            vo.setDesignName(design.getName());
            vo.setPreviewUrl(design.getPreviewUrl());
            vo.setPrice(design.getPrice());
            result.add(vo);
        }
        return result;
    }

    /** 加入购物车：已存在则数量累加 */
    public void add(Long userId, CartRequest request) {
        Design design = designMapper.selectById(request.getDesignId());
        if (design == null) {
            throw new BusinessException("设计不存在或已删除");
        }
        if (!userId.equals(design.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "不能使用其他人的设计");
        }
        int quantity = request.getQuantity() == null ? 1 : request.getQuantity();
        validateQuantity(quantity);
        if (cartMapper.incrementQuantity(userId, request.getDesignId(), quantity) == 1) {
            return;
        }
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setDesignId(request.getDesignId());
        cart.setQuantity(quantity);
        try {
            cartMapper.insert(cart);
        } catch (DuplicateKeyException e) {
            // 另一请求可能刚插入同一设计；再次用条件更新完成累加。
            if (cartMapper.incrementQuantity(userId, request.getDesignId(), quantity) != 1) {
                throw new BusinessException("数量不能大于 " + MAX_QUANTITY);
            }
        }
    }

    /** 修改数量（条目必须属于本人） */
    public void updateQuantity(Long userId, Long id, Integer quantity) {
        Cart cart = getOwned(userId, id);
        validateQuantity(quantity);
        cart.setQuantity(quantity);
        cartMapper.updateById(cart);
    }

    /** 删除条目 */
    public void delete(Long userId, Long id) {
        getOwned(userId, id);
        cartMapper.deleteById(id);
    }

    /** 清空购物车 */
    public void clear(Long userId) {
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
    }

    /** 按 id 查本人购物车条目 */
    private Cart getOwned(Long userId, Long id) {
        Cart cart = cartMapper.selectById(id);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return cart;
    }

    static void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 1 || quantity > MAX_QUANTITY) {
            throw new BusinessException("数量必须在 1 到 " + MAX_QUANTITY + " 之间");
        }
    }
}
