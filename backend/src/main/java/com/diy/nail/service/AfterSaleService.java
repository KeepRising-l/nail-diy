package com.diy.nail.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diy.nail.common.BusinessException;
import com.diy.nail.common.PageResult;
import com.diy.nail.common.ResultCode;
import com.diy.nail.dto.AfterSaleHandleRequest;
import com.diy.nail.dto.AfterSaleRequest;
import com.diy.nail.entity.AfterSale;
import com.diy.nail.entity.Notification;
import com.diy.nail.entity.Order;
import com.diy.nail.entity.User;
import com.diy.nail.mapper.AfterSaleMapper;
import com.diy.nail.mapper.OrderMapper;
import com.diy.nail.mapper.UserMapper;
import com.diy.nail.vo.AfterSaleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 售后业务：顾客对已完成订单发起退款/退货申请，商家审核。
 *
 * <p>规则：
 * <ul>
 *   <li>仅「已签收 / 已核销」的订单可申请（未完成的订单走取消流程）</li>
 *   <li>一单一售后：已提交过申请（无论结果）不可重复提交，避免反复骚扰</li>
 *   <li>退款金额由后端按订单实付金额计算，不接受前端传值，防止伪造金额</li>
 *   <li>处理时用条件更新（仅 PENDING 可被处理），防并发重复处理</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AfterSaleService {

    /** 凭证图上限 */
    private static final int MAX_IMAGES = 3;
    /** 拒绝原因长度上限 */
    private static final int MAX_REJECT_REASON = 200;

    private final AfterSaleMapper afterSaleMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    /** 顾客提交售后申请 */
    @Transactional(rollbackFor = Exception.class)
    public Long apply(Long userId, AfterSaleRequest request) {
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!Order.STATUS_RECEIVED.equals(order.getStatus())
                && !Order.STATUS_VERIFIED.equals(order.getStatus())) {
            throw new BusinessException("订单完成后才能申请售后");
        }
        if (!AfterSale.TYPE_REFUND_ONLY.equals(request.getType())
                && !AfterSale.TYPE_RETURN_REFUND.equals(request.getType())) {
            throw new BusinessException("售后类型不合法");
        }
        List<String> images = request.getImages();
        if (images != null && images.size() > MAX_IMAGES) {
            throw new BusinessException("凭证图最多 " + MAX_IMAGES + " 张");
        }
        Long exists = afterSaleMapper.selectCount(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getOrderId, order.getId()));
        if (exists != null && exists > 0) {
            throw new BusinessException("该订单已提交过售后申请");
        }

        AfterSale sale = new AfterSale();
        sale.setOrderId(order.getId());
        sale.setUserId(userId);
        sale.setType(request.getType());
        sale.setReason(request.getReason());
        sale.setDescription(StrUtil.nullToEmpty(request.getDescription()));
        sale.setImages(images == null || images.isEmpty() ? null : JSONUtil.toJsonStr(images));
        // 退款金额由后端按订单实付金额计算，不接受前端传值
        sale.setAmount(order.getTotalAmount());
        sale.setStatus(AfterSale.STATUS_PENDING);
        sale.setRejectReason("");
        afterSaleMapper.insert(sale);

        notifyMerchants(order, sale);
        return sale.getId();
    }

    /** 商家处理售后（同意退款 / 拒绝） */
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long id, AfterSaleHandleRequest request) {
        AfterSale sale = afterSaleMapper.selectById(id);
        if (sale == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!AfterSale.STATUS_PENDING.equals(sale.getStatus())) {
            throw new BusinessException("该售后单已处理");
        }
        boolean approved = Boolean.TRUE.equals(request.getApproved());
        String rejectReason = "";
        if (!approved) {
            rejectReason = StrUtil.nullToEmpty(request.getRejectReason()).trim();
            if (rejectReason.isEmpty()) {
                throw new BusinessException("请填写拒绝原因");
            }
            if (rejectReason.length() > MAX_REJECT_REASON) {
                throw new BusinessException("拒绝原因不能超过 " + MAX_REJECT_REASON + " 字");
            }
        }
        String status = approved ? AfterSale.STATUS_COMPLETED : AfterSale.STATUS_REJECTED;
        // 条件更新：仅 PENDING 可被处理，防止并发下重复处理
        int updated = afterSaleMapper.handlePending(id, status, rejectReason);
        if (updated != 1) {
            throw new BusinessException("该售后单已被处理，请刷新后重试");
        }

        Order order = orderMapper.selectById(sale.getOrderId());
        String orderNo = order == null ? "" : order.getOrderNo();
        notificationService.send(sale.getUserId(), Notification.TYPE_AFTER_SALE,
                approved ? "售后申请已通过" : "售后申请被拒绝",
                approved
                        ? "订单 " + orderNo + " 的退款 ￥" + sale.getAmount() + " 已处理完成"
                        : "订单 " + orderNo + " 的售后申请被拒绝：" + rejectReason,
                sale.getId(), Notification.BIZ_AFTER_SALE);
    }

    /** 顾客：我的售后单分页 */
    public PageResult<AfterSaleVO> myPage(Long userId, long page, long size) {
        Page<AfterSale> result = afterSaleMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<AfterSale>()
                        .eq(AfterSale::getUserId, userId)
                        .orderByDesc(AfterSale::getId));
        return PageResult.of(result.getTotal(), result.getRecords().stream().map(this::toVO).toList());
    }

    /** 顾客：查某订单的售后单（订单详情展示用，无则返回 null） */
    public AfterSaleVO byOrder(Long userId, Long orderId) {
        AfterSale sale = afterSaleMapper.selectOne(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getOrderId, orderId)
                .eq(AfterSale::getUserId, userId)
                .orderByDesc(AfterSale::getId)
                .last("LIMIT 1"));
        return sale == null ? null : toVO(sale);
    }

    /** 商家：售后单分页（可按状态筛选，待处理优先） */
    public PageResult<AfterSaleVO> merchantPage(String status, long page, long size) {
        Page<AfterSale> result = afterSaleMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<AfterSale>()
                        .eq(StrUtil.isNotBlank(status), AfterSale::getStatus, status)
                        .orderByAsc(AfterSale::getStatus)
                        .orderByDesc(AfterSale::getId));
        return PageResult.of(result.getTotal(), result.getRecords().stream().map(this::toVO).toList());
    }

    /** 商家：待处理售后数量（看板用） */
    public long pendingCount() {
        Long count = afterSaleMapper.selectCount(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getStatus, AfterSale.STATUS_PENDING));
        return count == null ? 0 : count;
    }

    /** 通知所有商家有新售后申请 */
    private void notifyMerchants(Order order, AfterSale sale) {
        List<User> merchants = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, User.ROLE_MERCHANT));
        for (User merchant : merchants) {
            notificationService.send(merchant.getId(), Notification.TYPE_AFTER_SALE,
                    "新的售后申请",
                    "订单 " + order.getOrderNo() + " 提交了售后申请（￥" + sale.getAmount() + "），请及时处理",
                    sale.getId(), Notification.BIZ_AFTER_SALE);
        }
    }

    /** 组装视图：售后单 + 订单概要 + 申请顾客信息 */
    private AfterSaleVO toVO(AfterSale sale) {
        AfterSaleVO vo = new AfterSaleVO();
        vo.setAfterSale(sale);
        vo.setOrder(orderMapper.selectById(sale.getOrderId()));
        User user = userMapper.selectById(sale.getUserId());
        if (user != null) {
            vo.setUserNickname(user.getNickname());
            vo.setUserPhone(user.getPhone());
        }
        return vo;
    }
}
