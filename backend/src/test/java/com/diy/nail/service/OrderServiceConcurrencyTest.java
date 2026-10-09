package com.diy.nail.service;

import com.diy.nail.common.BusinessException;
import com.diy.nail.dto.OrderCreateRequest;
import com.diy.nail.entity.Cart;
import com.diy.nail.entity.Design;
import com.diy.nail.entity.Order;
import com.diy.nail.entity.Store;
import com.diy.nail.mapper.AddressMapper;
import com.diy.nail.mapper.CartMapper;
import com.diy.nail.mapper.DesignMapper;
import com.diy.nail.mapper.OrderItemMapper;
import com.diy.nail.mapper.OrderMapper;
import com.diy.nail.mapper.OrderStatusLogMapper;
import com.diy.nail.mapper.StoreMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderServiceConcurrencyTest {

    @Test
    void staleStatusUpdateDoesNotWriteMisleadingLog() {
        OrderMapper orderMapper = mock(OrderMapper.class);
        OrderStatusLogMapper logMapper = mock(OrderStatusLogMapper.class);
        OrderService service = new OrderService(
                orderMapper,
                mock(OrderItemMapper.class),
                logMapper,
                mock(CartMapper.class),
                mock(DesignMapper.class),
                mock(StoreMapper.class),
                mock(AddressMapper.class),
                mock(ReviewService.class));
        Order order = new Order();
        order.setId(11L);
        order.setUserId(7L);
        order.setStatus(Order.STATUS_PENDING_PAY);
        when(orderMapper.selectById(11L)).thenReturn(order);
        when(orderMapper.transitionStatus(eq(11L), eq(Order.STATUS_PENDING_PAY), eq(Order.STATUS_PAID),
                any(), isNull(), isNull(), isNull(), isNull())).thenReturn(0);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.pay(7L, 11L));

        assertEquals("订单状态已变化，请刷新后重试", error.getMessage());
        verifyNoInteractions(logMapper);
    }

    @Test
    void checkoutDeletesOnlyTheLockedCartSnapshot() {
        OrderMapper orderMapper = mock(OrderMapper.class);
        OrderItemMapper orderItemMapper = mock(OrderItemMapper.class);
        OrderStatusLogMapper logMapper = mock(OrderStatusLogMapper.class);
        CartMapper cartMapper = mock(CartMapper.class);
        DesignMapper designMapper = mock(DesignMapper.class);
        StoreMapper storeMapper = mock(StoreMapper.class);
        OrderService service = new OrderService(orderMapper, orderItemMapper, logMapper, cartMapper,
                designMapper, storeMapper, mock(AddressMapper.class), mock(ReviewService.class));
        Cart first = cart(21L, 31L);
        Cart second = cart(22L, 32L);
        when(cartMapper.selectList(any())).thenReturn(List.of(first, second));
        when(designMapper.selectById(31L)).thenReturn(design(31L));
        when(designMapper.selectById(32L)).thenReturn(design(32L));
        Store store = new Store();
        store.setId(9L);
        store.setStatus(1);
        when(storeMapper.selectById(9L)).thenReturn(store);
        OrderCreateRequest request = new OrderCreateRequest();
        request.setDeliveryType(Order.DELIVERY_SELF_PICKUP);
        request.setStoreId(9L);

        service.create(7L, request);

        verify(cartMapper).deleteCheckedOut(7L, List.of(21L, 22L));
    }

    private Cart cart(Long id, Long designId) {
        Cart cart = new Cart();
        cart.setId(id);
        cart.setUserId(7L);
        cart.setDesignId(designId);
        cart.setQuantity(1);
        return cart;
    }

    private Design design(Long id) {
        Design design = new Design();
        design.setId(id);
        design.setUserId(7L);
        design.setName("test");
        design.setPrice(BigDecimal.TEN);
        return design;
    }
}
