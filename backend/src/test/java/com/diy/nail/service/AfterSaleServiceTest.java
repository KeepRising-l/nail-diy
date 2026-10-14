package com.diy.nail.service;

import com.diy.nail.common.BusinessException;
import com.diy.nail.common.ResultCode;
import com.diy.nail.dto.AfterSaleHandleRequest;
import com.diy.nail.dto.AfterSaleRequest;
import com.diy.nail.entity.AfterSale;
import com.diy.nail.entity.Order;
import com.diy.nail.mapper.AfterSaleMapper;
import com.diy.nail.mapper.OrderMapper;
import com.diy.nail.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 售后业务测试。
 *
 * <p>覆盖四类风险点：
 * ① 可申请范围（仅已完成订单、仅本人订单、一单一售后）
 * ② 退款金额由后端按订单实付计算（不接受前端传值）
 * ③ 处理分支（同意 / 拒绝必填原因 / 重复处理）
 * ④ 并发防护（条件更新影响行数不为 1 时拒绝）
 */
class AfterSaleServiceTest {

    private static final Long USER = 7L;
    private static final Long ORDER_ID = 11L;

    private final AfterSaleMapper afterSaleMapper = mock(AfterSaleMapper.class);
    private final OrderMapper orderMapper = mock(OrderMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final NotificationService notificationService = mock(NotificationService.class);

    private final AfterSaleService service =
            new AfterSaleService(afterSaleMapper, orderMapper, userMapper, notificationService);

    // ---------- 辅助 ----------

    private Order order(Long userId, String status, String amount) {
        Order o = new Order();
        o.setId(ORDER_ID);
        o.setOrderNo("DD20260913122134301578");
        o.setUserId(userId);
        o.setStatus(status);
        o.setTotalAmount(new BigDecimal(amount));
        return o;
    }

    private AfterSaleRequest request(String type, List<String> images) {
        AfterSaleRequest r = new AfterSaleRequest();
        r.setOrderId(ORDER_ID);
        r.setType(type);
        r.setReason("尺寸不合适");
        r.setDescription("戴上去偏小");
        r.setImages(images);
        return r;
    }

    private AfterSale pendingSale(String amount) {
        AfterSale s = new AfterSale();
        s.setId(1L);
        s.setOrderId(ORDER_ID);
        s.setUserId(USER);
        s.setStatus(AfterSale.STATUS_PENDING);
        s.setAmount(new BigDecimal(amount));
        return s;
    }

    // ---------- 申请 ----------

    @Test
    void appliesForCompletedOrder() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order(USER, Order.STATUS_RECEIVED, "39.00"));
        when(afterSaleMapper.selectCount(any())).thenReturn(0L);

        Long id = service.apply(USER, request(AfterSale.TYPE_REFUND_ONLY, List.of()));

        assertEquals(null, id);   // mock 未回填主键，此处只验证不抛异常且走到了 insert
        verify(afterSaleMapper).insert(any(AfterSale.class));
    }

    @Test
    void acceptsVerifiedOrderToo() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order(USER, Order.STATUS_VERIFIED, "39.00"));
        when(afterSaleMapper.selectCount(any())).thenReturn(0L);

        service.apply(USER, request(AfterSale.TYPE_REFUND_ONLY, List.of()));

        verify(afterSaleMapper).insert(any(AfterSale.class));
    }

    @Test
    void refundAmountComesFromOrderNotClient() {
        // 关键安全点：退款金额必须取自订单实付金额，而非客户端传入
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order(USER, Order.STATUS_RECEIVED, "128.50"));
        when(afterSaleMapper.selectCount(any())).thenReturn(0L);

        service.apply(USER, request(AfterSale.TYPE_REFUND_ONLY, List.of()));

        ArgumentCaptor<AfterSale> captor = ArgumentCaptor.forClass(AfterSale.class);
        verify(afterSaleMapper).insert(captor.capture());
        assertEquals(0, new BigDecimal("128.50").compareTo(captor.getValue().getAmount()));
    }

    @Test
    void rejectsUnfinishedOrder() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order(USER, Order.STATUS_PRODUCING, "39.00"));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.apply(USER, request(AfterSale.TYPE_REFUND_ONLY, List.of())));

        assertEquals("订单完成后才能申请售后", e.getMessage());
        verify(afterSaleMapper, never()).insert(any(AfterSale.class));
    }

    @Test
    void rejectsOtherUsersOrder() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order(99L, Order.STATUS_RECEIVED, "39.00"));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.apply(USER, request(AfterSale.TYPE_REFUND_ONLY, List.of())));

        assertEquals(ResultCode.NOT_FOUND.getCode(), e.getCode());
    }

    @Test
    void rejectsDuplicateApplication() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order(USER, Order.STATUS_RECEIVED, "39.00"));
        when(afterSaleMapper.selectCount(any())).thenReturn(1L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.apply(USER, request(AfterSale.TYPE_REFUND_ONLY, List.of())));

        assertEquals("该订单已提交过售后申请", e.getMessage());
    }

    @Test
    void rejectsIllegalType() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order(USER, Order.STATUS_RECEIVED, "39.00"));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.apply(USER, request("FREE_MONEY", List.of())));

        assertEquals("售后类型不合法", e.getMessage());
    }

    @Test
    void rejectsTooManyImages() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order(USER, Order.STATUS_RECEIVED, "39.00"));

        BusinessException e = assertThrows(BusinessException.class, () -> service.apply(USER,
                request(AfterSale.TYPE_REFUND_ONLY, List.of("a.png", "b.png", "c.png", "d.png"))));

        assertEquals("凭证图最多 3 张", e.getMessage());
    }

    // ---------- 处理 ----------

    @Test
    void approveCompletesAndNotifiesCustomer() {
        when(afterSaleMapper.selectById(1L)).thenReturn(pendingSale("39.00"));
        when(afterSaleMapper.handlePending(eq(1L), eq(AfterSale.STATUS_COMPLETED), eq(""))).thenReturn(1);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order(USER, Order.STATUS_RECEIVED, "39.00"));

        AfterSaleHandleRequest req = new AfterSaleHandleRequest();
        req.setApproved(true);
        service.handle(1L, req);

        verify(afterSaleMapper).handlePending(1L, AfterSale.STATUS_COMPLETED, "");
        verify(notificationService).send(eq(USER), any(), eq("售后申请已通过"), any(), eq(1L), any());
    }

    @Test
    void rejectRequiresReason() {
        when(afterSaleMapper.selectById(1L)).thenReturn(pendingSale("39.00"));

        AfterSaleHandleRequest req = new AfterSaleHandleRequest();
        req.setApproved(false);
        req.setRejectReason("   ");

        BusinessException e = assertThrows(BusinessException.class, () -> service.handle(1L, req));

        assertEquals("请填写拒绝原因", e.getMessage());
        verify(afterSaleMapper, never()).handlePending(anyLong(), any(), any());
    }

    @Test
    void rejectWithReasonMarksRejected() {
        when(afterSaleMapper.selectById(1L)).thenReturn(pendingSale("39.00"));
        when(afterSaleMapper.handlePending(eq(1L), eq(AfterSale.STATUS_REJECTED), eq("已按设计制作完成")))
                .thenReturn(1);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order(USER, Order.STATUS_RECEIVED, "39.00"));

        AfterSaleHandleRequest req = new AfterSaleHandleRequest();
        req.setApproved(false);
        req.setRejectReason("已按设计制作完成");
        service.handle(1L, req);

        verify(afterSaleMapper).handlePending(1L, AfterSale.STATUS_REJECTED, "已按设计制作完成");
        verify(notificationService).send(eq(USER), any(), eq("售后申请被拒绝"), any(), eq(1L), any());
    }

    @Test
    void rejectsHandlingAlreadyHandledSale() {
        AfterSale done = pendingSale("39.00");
        done.setStatus(AfterSale.STATUS_COMPLETED);
        when(afterSaleMapper.selectById(1L)).thenReturn(done);

        AfterSaleHandleRequest req = new AfterSaleHandleRequest();
        req.setApproved(true);

        BusinessException e = assertThrows(BusinessException.class, () -> service.handle(1L, req));

        assertEquals("该售后单已处理", e.getMessage());
    }

    @Test
    void rejectsWhenConcurrentUpdateLosesRace() {
        // 另一个请求已抢先处理：条件更新影响行数为 0
        when(afterSaleMapper.selectById(1L)).thenReturn(pendingSale("39.00"));
        when(afterSaleMapper.handlePending(anyLong(), any(), any())).thenReturn(0);

        AfterSaleHandleRequest req = new AfterSaleHandleRequest();
        req.setApproved(true);

        BusinessException e = assertThrows(BusinessException.class, () -> service.handle(1L, req));

        assertEquals("该售后单已被处理，请刷新后重试", e.getMessage());
        verify(notificationService, never()).send(any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsHandlingMissingSale() {
        when(afterSaleMapper.selectById(404L)).thenReturn(null);

        AfterSaleHandleRequest req = new AfterSaleHandleRequest();
        req.setApproved(true);

        BusinessException e = assertThrows(BusinessException.class, () -> service.handle(404L, req));

        assertEquals(ResultCode.NOT_FOUND.getCode(), e.getCode());
    }
}
