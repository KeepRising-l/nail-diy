package com.diy.nail.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diy.nail.common.BusinessException;
import com.diy.nail.common.PageResult;
import com.diy.nail.common.ResultCode;
import com.diy.nail.dto.ReviewRequest;
import com.diy.nail.entity.Order;
import com.diy.nail.entity.OrderItem;
import com.diy.nail.entity.Review;
import com.diy.nail.entity.User;
import com.diy.nail.mapper.OrderItemMapper;
import com.diy.nail.mapper.OrderMapper;
import com.diy.nail.mapper.ReviewMapper;
import com.diy.nail.mapper.UserMapper;
import com.diy.nail.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评价业务：完成订单一次性评价晒图（昵称脱敏展示）、商家评价管理（列表/回复/删除）
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;

    /** 提交评价：仅本人已完成（已核销/已签收）订单，每单限一次 */
    public void create(Long userId, ReviewRequest request) {
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "不能评价其他人的订单");
        }
        if (!Order.STATUS_VERIFIED.equals(order.getStatus())
                && !Order.STATUS_RECEIVED.equals(order.getStatus())) {
            throw new BusinessException("未完成的订单不可评价");
        }
        Long exists = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, request.getOrderId()));
        if (exists != null && exists > 0) {
            throw new BusinessException("该订单已评价");
        }
        Review review = new Review();
        review.setOrderId(order.getId());
        review.setUserId(userId);
        review.setRating(request.getRating());
        review.setContent(StrUtil.nullToEmpty(request.getContent()));
        review.setImages(request.getImages() == null ? "[]" : JSONUtil.toJsonStr(request.getImages()));
        review.setReply("");
        reviewMapper.insert(review);
    }

    /** 查某订单的评价（公开展示），无评价返回 null */
    public ReviewVO getByOrderId(Long orderId) {
        Review review = reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, orderId)
                .orderByDesc(Review::getId)
                .last("LIMIT 1"));
        return review == null ? null : toVO(review);
    }

    /** 商家评价分页（评分可选筛选，时间倒序） */
    public PageResult<ReviewVO> merchantPage(Integer rating, long page, long size) {
        Page<Review> result = reviewMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<Review>()
                .eq(rating != null, Review::getRating, rating)
                .orderByDesc(Review::getId));
        List<ReviewVO> vos = new ArrayList<>();
        for (Review review : result.getRecords()) {
            vos.add(toVO(review));
        }
        return PageResult.of(result.getTotal(), vos);
    }

    /** 商家回复评价（展示在评价下方） */
    public void reply(Long id, String reply) {
        Review review = getOrThrow(id);
        review.setReply(reply);
        reviewMapper.updateById(review);
    }

    /** 商家删除违规评价（逻辑删除） */
    public void delete(Long id) {
        getOrThrow(id);
        reviewMapper.deleteById(id);
    }

    /** 评价实体转视图：附订单号、商品名快照、脱敏昵称、晒图列表 */
    public ReviewVO toVO(Review review) {
        ReviewVO vo = new ReviewVO();
        vo.setId(review.getId());
        vo.setOrderId(review.getOrderId());
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setReply(review.getReply());
        vo.setCreateTime(review.getCreateTime());
        vo.setImages(StrUtil.isBlank(review.getImages())
                ? new ArrayList<>() : JSONUtil.toList(review.getImages(), String.class));
        Order order = orderMapper.selectById(review.getOrderId());
        if (order != null) {
            vo.setOrderNo(order.getOrderNo());
            List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, order.getId()));
            vo.setDesignNames(items.stream().map(OrderItem::getDesignName).collect(Collectors.joining("、")));
        }
        User user = userMapper.selectById(review.getUserId());
        vo.setNickname(maskNickname(user == null ? "" : user.getNickname()));
        return vo;
    }

    /** 昵称脱敏：首字符 + ****（保护隐私，REV 备注约定） */
    private String maskNickname(String nickname) {
        if (StrUtil.isBlank(nickname)) {
            return "用户****";
        }
        return nickname.charAt(0) + "****";
    }

    /** 按 id 查评价 */
    private Review getOrThrow(Long id) {
        Review review = reviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return review;
    }
}
