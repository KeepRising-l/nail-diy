package com.diy.nail.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diy.nail.common.BusinessException;
import com.diy.nail.common.PageResult;
import com.diy.nail.common.ResultCode;
import com.diy.nail.dto.OrderCreateRequest;
import com.diy.nail.dto.ShipRequest;
import com.diy.nail.entity.Address;
import com.diy.nail.entity.AfterSale;
import com.diy.nail.entity.Cart;
import com.diy.nail.entity.Design;
import com.diy.nail.entity.Order;
import com.diy.nail.entity.OrderItem;
import com.diy.nail.entity.Notification;
import com.diy.nail.entity.OrderStatusLog;
import com.diy.nail.entity.Store;
import com.diy.nail.entity.User;
import com.diy.nail.mapper.AddressMapper;
import com.diy.nail.mapper.AfterSaleMapper;
import com.diy.nail.mapper.CartMapper;
import com.diy.nail.mapper.DesignMapper;
import com.diy.nail.mapper.OrderItemMapper;
import com.diy.nail.mapper.OrderMapper;
import com.diy.nail.mapper.OrderStatusLogMapper;
import com.diy.nail.mapper.StoreMapper;
import com.diy.nail.mapper.UserMapper;
import com.diy.nail.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/**
 * 订单业务：整购物车结算下单、模拟支付、取消、我的订单查询
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final BigDecimal MAX_ORDER_AMOUNT = new BigDecimal("99999999.99");

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final CartMapper cartMapper;
    private final DesignMapper designMapper;
    private final StoreMapper storeMapper;
    private final AddressMapper addressMapper;
    private final AfterSaleMapper afterSaleMapper;
    private final UserMapper userMapper;
    private final ReviewService reviewService;
    private final NotificationService notificationService;

    /** 邮寄固定运费（配置化，后续可挪到商家后台） */
    @Value("${nail.freight.amount}")
    private BigDecimal freightAmount;

    /** 满额包邮门槛 */
    @Value("${nail.freight.free-over}")
    private BigDecimal freightFreeOver;

    /** 我的订单分页（状态可选筛选，倒序，含明细与门店） */
    public PageResult<OrderVO> page(Long userId, String status, long page, long size) {
        Page<Order> result = orderMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(StrUtil.isNotBlank(status), Order::getStatus, status)
                .orderByDesc(Order::getId));
        List<OrderVO> vos = new ArrayList<>();
        for (Order order : result.getRecords()) {
            vos.add(toVO(order));
        }
        return PageResult.of(result.getTotal(), vos);
    }

    /** 订单详情（仅本人） */
    public OrderVO detail(Long userId, Long id) {
        return toVO(getOwned(userId, id));
    }

    /** 商家端订单详情（不校验归属） */
    public OrderVO merchantDetail(Long id) {
        return toVO(getOrThrow(id));
    }

    /** 创建订单：整购物车结算，事务内写订单/明细/日志并清空购物车 */
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, OrderCreateRequest request) {
        List<Cart> carts = cartMapper.selectList(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .last("FOR UPDATE"));
        if (carts.isEmpty()) {
            throw new BusinessException("购物车为空，请先将设计加入购物车");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNo("DD" + DateUtil.format(new Date(), "yyyyMMddHHmmssSSS") + RandomUtil.randomNumbers(3));
        order.setDeliveryType(request.getDeliveryType());
        order.setRemark(StrUtil.nullToEmpty(request.getRemark()));

        // 履约方式校验与收货信息快照
        BigDecimal goodsAmount = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        for (Cart cart : carts) {
            Design design = designMapper.selectById(cart.getDesignId());
            if (design == null) {
                throw new BusinessException("购物车中存在已删除的设计，请刷新后重试");
            }
            if (!userId.equals(design.getUserId())) {
                throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "购物车中存在不属于你的设计");
            }
            CartService.validateQuantity(cart.getQuantity());
            if (design.getPrice() == null || design.getPrice().signum() <= 0) {
                throw new BusinessException("设计价格异常，请联系客服处理");
            }
            OrderItem item = new OrderItem();
            item.setDesignId(design.getId());
            item.setDesignName(design.getName());
            item.setDesignParams(design.getParams());
            item.setPreviewUrl(design.getPreviewUrl());
            item.setPrice(design.getPrice());
            item.setQuantity(cart.getQuantity());
            items.add(item);
            goodsAmount = goodsAmount.add(design.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
            if (goodsAmount.compareTo(MAX_ORDER_AMOUNT) > 0) {
                throw new BusinessException("订单金额超出允许范围");
            }
        }

        if (Order.DELIVERY_SELF_PICKUP.equals(request.getDeliveryType())) {
            if (request.getStoreId() == null) {
                throw new BusinessException("请选择自提门店");
            }
            Store store = storeMapper.selectById(request.getStoreId());
            if (store == null || store.getStatus() == null || store.getStatus() != 1) {
                throw new BusinessException("请选择有效的自提门店");
            }
            order.setStoreId(store.getId());
            order.setFreight(BigDecimal.ZERO); // 自提无运费
        } else if (Order.DELIVERY_EXPRESS.equals(request.getDeliveryType())) {
            if (request.getAddressId() == null) {
                throw new BusinessException("请选择收货地址");
            }
            Address address = addressMapper.selectById(request.getAddressId());
            if (address == null || !address.getUserId().equals(userId)) {
                throw new BusinessException("请选择有效的收货地址");
            }
            order.setReceiverName(address.getReceiver());
            order.setReceiverPhone(address.getPhone());
            order.setReceiverAddr(address.getProvince() + address.getCity() + address.getDistrict() + address.getDetail());
            // 运费规则：满额包邮，否则固定运费
            order.setFreight(calcFreight(goodsAmount, freightFreeOver, freightAmount));
        } else {
            throw new BusinessException("履约方式不合法");
        }

        order.setGoodsAmount(goodsAmount);
        order.setTotalAmount(goodsAmount.add(order.getFreight()));
        order.setStatus(Order.STATUS_PENDING_PAY);
        orderMapper.insert(order);

        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }
        writeLog(order.getId(), "", Order.STATUS_PENDING_PAY, "顾客", "提交订单");
        // 只移除本次结算快照；保留结算期间可能新增的购物车条目。
        cartMapper.deleteCheckedOut(userId, carts.stream().map(Cart::getId).toList());
        return order.getId();
    }

    // ------------------- 消息通知（关键节点写入；通知失败不影响主流程） -------------------

    /** 给订单所属顾客发通知 */
    private void notifyCustomer(Order order, String title, String content) {
        notificationService.send(order.getUserId(), Notification.TYPE_ORDER, title, content,
                order.getId(), Notification.BIZ_ORDER);
    }

    /** 给所有商家发通知（单商家模式下遍历 MERCHANT 角色账号） */
    private void notifyMerchants(Order order, String title, String content) {
        List<User> merchants = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, User.ROLE_MERCHANT));
        for (User merchant : merchants) {
            notificationService.send(merchant.getId(), Notification.TYPE_ORDER, title, content,
                    order.getId(), Notification.BIZ_ORDER);
        }
    }

    /** 模拟支付：待支付 → 待接单 */
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long userId, Long id) {
        Order order = getOwned(userId, id);
        if (!Order.STATUS_PENDING_PAY.equals(order.getStatus())) {
            throw new BusinessException("该订单无需支付");
        }
        transition(order, Order.STATUS_PAID,
                changes -> changes.setPayTime(LocalDateTime.now()));
        writeLog(order.getId(), Order.STATUS_PENDING_PAY, Order.STATUS_PAID, "顾客", "模拟支付成功");
        notifyMerchants(order, "有新订单待接单",
                "订单 " + order.getOrderNo() + " 已支付 ￥" + order.getTotalAmount() + "，请及时接单");
    }

    /** 取消订单：仅待支付/待接单可取消 */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long id) {
        Order order = getOwned(userId, id);
        if (!Order.STATUS_PENDING_PAY.equals(order.getStatus())
                && !Order.STATUS_PAID.equals(order.getStatus())) {
            throw new BusinessException("当前订单状态不可取消");
        }
        String from = order.getStatus();
        transition(order, Order.STATUS_CANCELLED, update -> {});
        writeLog(order.getId(), from, Order.STATUS_CANCELLED, "顾客", "取消订单");
        notifyMerchants(order, "顾客取消了订单", "订单 " + order.getOrderNo() + " 已被顾客取消");
    }

    // ------------------- 商家端履约（change-07，状态变更均过状态机） -------------------

    /** 商家订单中心分页（状态可选筛选，含明细/门店/日志） */
    public PageResult<OrderVO> merchantPage(String status, long page, long size) {
        Page<Order> result = orderMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<Order>()
                .eq(StrUtil.isNotBlank(status), Order::getStatus, status)
                .orderByDesc(Order::getId));
        List<OrderVO> vos = new ArrayList<>();
        for (Order order : result.getRecords()) {
            vos.add(toVO(order));
        }
        return PageResult.of(result.getTotal(), vos);
    }

    /** 商家接单：待接单 → 制作中 */
    @Transactional(rollbackFor = Exception.class)
    public void accept(Long id) {
        Order order = getOrThrow(id);
        String from = order.getStatus();
        transition(order, Order.STATUS_PRODUCING, update -> {});
        writeLog(order.getId(), from, Order.STATUS_PRODUCING, "商家", "接单");
        notifyCustomer(order, "商家已接单", "订单 " + order.getOrderNo() + " 已接单，正在制作中");
    }

    /** 制作完成：自提 → 待自提（生成 6 位自提码）；邮寄 → 待发货 */
    @Transactional(rollbackFor = Exception.class)
    public void produce(Long id) {
        Order order = getOrThrow(id);
        String from = order.getStatus();
        String to;
        if (Order.DELIVERY_SELF_PICKUP.equals(order.getDeliveryType())) {
            to = Order.STATUS_WAIT_PICKUP;
        } else if (Order.DELIVERY_EXPRESS.equals(order.getDeliveryType())) {
            to = Order.STATUS_WAIT_SHIP;
        } else {
            throw new BusinessException("订单履约方式异常");
        }
        String pickupCode = null;
        if (Order.STATUS_WAIT_PICKUP.equals(to)) {
            pickupCode = generatePickupCode();
        }
        String generatedPickupCode = pickupCode;
        transition(order, to, changes -> {
            if (generatedPickupCode != null) {
                changes.setPickupCode(generatedPickupCode);
            }
        });
        writeLog(order.getId(), from, to, "商家",
                Order.STATUS_WAIT_PICKUP.equals(to) ? "制作完成，自提码 " + generatedPickupCode : "制作完成，待发货");
        if (Order.STATUS_WAIT_PICKUP.equals(to)) {
            notifyCustomer(order, "制作完成，可到店自提",
                    "订单 " + order.getOrderNo() + " 已制作完成，自提码 " + generatedPickupCode + "，请到店出示核销");
        } else {
            notifyCustomer(order, "制作完成，待发货",
                    "订单 " + order.getOrderNo() + " 已制作完成，商家将尽快发货");
        }
    }

    /** 自提核销：自提码正确 → 已核销（终态） */
    @Transactional(rollbackFor = Exception.class)
    public void verify(Long id, String pickupCode) {
        Order order = getOrThrow(id);
        String from = order.getStatus();
        if (!pickupCode.equals(order.getPickupCode())) {
            throw new BusinessException("自提码不正确");
        }
        transition(order, Order.STATUS_VERIFIED,
                changes -> changes.setFinishTime(LocalDateTime.now()));
        writeLog(order.getId(), from, Order.STATUS_VERIFIED, "商家", "自提核销");
        notifyCustomer(order, "订单已完成", "订单 " + order.getOrderNo() + " 已核销完成，感谢惠顾");
    }

    /** 邮寄发货：待发货 → 已发货，记录物流信息 */
    @Transactional(rollbackFor = Exception.class)
    public void ship(Long id, ShipRequest request) {
        Order order = getOrThrow(id);
        String from = order.getStatus();
        transition(order, Order.STATUS_SHIPPED, changes -> {
            changes.setExpressCompany(request.getExpressCompany());
            changes.setExpressNo(request.getExpressNo());
        });
        writeLog(order.getId(), from, Order.STATUS_SHIPPED, "商家",
                "已发货：" + request.getExpressCompany() + " " + request.getExpressNo());
        notifyCustomer(order, "订单已发货",
                "订单 " + order.getOrderNo() + " 已发货：" + request.getExpressCompany() + " " + request.getExpressNo());
    }

    /** 商家取消订单（仅待支付/待接单阶段，对齐 REQ-FUL-7） */
    @Transactional(rollbackFor = Exception.class)
    public void merchantCancel(Long id) {
        Order order = getOrThrow(id);
        String from = order.getStatus();
        transition(order, Order.STATUS_CANCELLED, update -> {});
        writeLog(order.getId(), from, Order.STATUS_CANCELLED, "商家", "商家取消订单");
        notifyCustomer(order, "订单已取消", "订单 " + order.getOrderNo() + " 已被商家取消");
    }

    /** 顾客确认收货：已发货 → 已签收（终态） */
    @Transactional(rollbackFor = Exception.class)
    public void receive(Long userId, Long id) {
        Order order = getOwned(userId, id);
        String from = order.getStatus();
        transition(order, Order.STATUS_RECEIVED,
                changes -> changes.setFinishTime(LocalDateTime.now()));
        writeLog(order.getId(), from, Order.STATUS_RECEIVED, "顾客", "确认收货");
        notifyMerchants(order, "顾客已签收", "订单 " + order.getOrderNo() + " 已被顾客确认收货");
    }

    /** 运费规则：满额包邮，否则固定运费（纯函数，与状态机同风格集中管理，便于单元测试） */
    static BigDecimal calcFreight(BigDecimal goodsAmount, BigDecimal freeOver, BigDecimal fixedAmount) {
        return goodsAmount.compareTo(freeOver) >= 0 ? BigDecimal.ZERO : fixedAmount;
    }

    /** 生成全局唯一的 6 位数字自提码 */
    private String generatePickupCode() {
        for (int i = 0; i < 10; i++) {
            String code = RandomUtil.randomNumbers(6);
            Long used = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                    .eq(Order::getPickupCode, code)
                    .eq(Order::getStatus, Order.STATUS_WAIT_PICKUP));
            if (used == null || used == 0) {
                return code;
            }
        }
        throw new BusinessException("自提码生成失败，请重试");
    }

    /** 按 id 查订单（商家端用，不校验归属） */
    private Order getOrThrow(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return order;
    }

    /** 组装订单视图：订单 + 明细 + 自提门店 + 状态日志 */
    private OrderVO toVO(Order order) {
        OrderVO vo = new OrderVO();
        vo.setOrder(order);
        vo.setItems(orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .orderByAsc(OrderItem::getId)));
        if (order.getStoreId() != null) {
            vo.setStore(storeMapper.selectById(order.getStoreId()));
        }
        vo.setLogs(orderStatusLogMapper.selectList(new LambdaQueryWrapper<OrderStatusLog>()
                .eq(OrderStatusLog::getOrderId, order.getId())
                .orderByAsc(OrderStatusLog::getId)));
        vo.setReview(reviewService.getByOrderId(order.getId()));
        // 售后状态：只取状态列，避免把整条售后单塞进订单列表（前端据此显示按钮与标签）
        AfterSale sale = afterSaleMapper.selectOne(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getOrderId, order.getId())
                .select(AfterSale::getStatus)
                .orderByDesc(AfterSale::getId)
                .last("LIMIT 1"));
        vo.setAfterSaleStatus(sale == null ? null : sale.getStatus());
        return vo;
    }

    /** 按 id 查本人订单 */
    private Order getOwned(Long userId, Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "不能操作其他人的订单");
        }
        return order;
    }

    /** 状态迁移使用旧状态作为更新条件，避免并发请求互相覆盖。 */
    private void transition(Order order, String to,
                            Consumer<Order> customizer) {
        String from = order.getStatus();
        OrderStateMachine.assertTransition(from, to);
        Order changes = new Order();
        customizer.accept(changes);
        if (orderMapper.transitionStatus(order.getId(), from, to,
                changes.getPayTime(), changes.getFinishTime(), changes.getPickupCode(),
                changes.getExpressCompany(), changes.getExpressNo()) != 1) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
    }

    /** 记录一条状态变更日志 */
    private void writeLog(Long orderId, String from, String to, String operator, String remark) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setOperator(operator);
        log.setRemark(remark);
        orderStatusLogMapper.insert(log);
    }
}
