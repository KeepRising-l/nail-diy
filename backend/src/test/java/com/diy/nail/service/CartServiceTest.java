package com.diy.nail.service;

import com.diy.nail.common.BusinessException;
import com.diy.nail.dto.CartRequest;
import com.diy.nail.entity.Cart;
import com.diy.nail.entity.Design;
import com.diy.nail.mapper.CartMapper;
import com.diy.nail.mapper.DesignMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class CartServiceTest {

    private final CartMapper cartMapper = mock(CartMapper.class);
    private final DesignMapper designMapper = mock(DesignMapper.class);
    private final CartService service = new CartService(cartMapper, designMapper);

    @Test
    void quantityMustStayWithinBusinessLimit() {
        assertDoesNotThrow(() -> CartService.validateQuantity(1));
        assertDoesNotThrow(() -> CartService.validateQuantity(99));
        assertThrows(BusinessException.class, () -> CartService.validateQuantity(null));
        assertThrows(BusinessException.class, () -> CartService.validateQuantity(0));
        assertThrows(BusinessException.class, () -> CartService.validateQuantity(100));
    }

    @Test
    void anotherUsersDesignCannotBeAdded() {
        Design design = new Design();
        design.setId(8L);
        design.setUserId(2L);
        when(designMapper.selectById(8L)).thenReturn(design);
        CartRequest request = request(8L, 1);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.add(1L, request));

        assertEquals(403, error.getCode());
        verify(cartMapper, never()).incrementQuantity(any(), any(), any(Integer.class));
    }

    @Test
    void accumulatedQuantityCannotExceedLimit() {
        Design design = new Design();
        design.setId(8L);
        design.setUserId(1L);
        when(designMapper.selectById(8L)).thenReturn(design);
        when(cartMapper.incrementQuantity(1L, 8L, 5)).thenReturn(0);
        doThrow(new DuplicateKeyException("duplicate")).when(cartMapper).insert(any(Cart.class));

        assertThrows(BusinessException.class, () -> service.add(1L, request(8L, 5)));
        verify(cartMapper).insert(any(Cart.class));
    }

    @Test
    void concurrentFirstInsertRetriesAtomicIncrement() {
        Design design = new Design();
        design.setId(8L);
        design.setUserId(1L);
        when(designMapper.selectById(8L)).thenReturn(design);
        when(cartMapper.incrementQuantity(1L, 8L, 5)).thenReturn(0, 1);
        doThrow(new DuplicateKeyException("duplicate")).when(cartMapper).insert(any(Cart.class));

        assertDoesNotThrow(() -> service.add(1L, request(8L, 5)));

        verify(cartMapper, times(2)).incrementQuantity(1L, 8L, 5);
    }

    private CartRequest request(Long designId, Integer quantity) {
        CartRequest request = new CartRequest();
        request.setDesignId(designId);
        request.setQuantity(quantity);
        return request;
    }
}
