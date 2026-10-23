package com.lxs.b2cmall.shop.service;

import com.lxs.b2cmall.shop.entity.Product;
import com.lxs.b2cmall.shop.mapper.ProductMapper;
import com.lxs.b2cmall.shop.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * P2 删除校验逻辑：已产生订单的商品禁止删除。
 */
class ProductServiceImplDeleteTest {

    private ProductMapper productMapper;
    private ProductServiceImpl service;

    @BeforeEach
    void setUp() {
        productMapper = Mockito.mock(ProductMapper.class);
        service = new ProductServiceImpl();
        // 注入 mock（publish 用的 factory 置 null 不影响本测试）
        org.springframework.test.util.ReflectionTestUtils.setField(service, "productMapper", productMapper);
    }

    private Product exist(int id) {
        Product p = new Product();
        p.setId(id);
        p.setName("测试商品");
        return p;
    }

    @Test
    void deleteShouldThrowWhenOrderExists() {
        when(productMapper.findById(1)).thenReturn(exist(1));
        when(productMapper.countOrderItems(1)).thenReturn(3L);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.delete(1));
        assertTrue(ex.getMessage().contains("禁止删除"));
        verify(productMapper, never()).deleteById(any());
    }

    @Test
    void deleteShouldPassWhenNoOrder() {
        when(productMapper.findById(2)).thenReturn(exist(2));
        when(productMapper.countOrderItems(2)).thenReturn(0L);

        assertDoesNotThrow(() -> service.delete(2));
        verify(productMapper).deleteById(2);
    }

    @Test
    void deleteShouldThrowWhenProductMissing() {
        when(productMapper.findById(404)).thenReturn(null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.delete(404));
        assertTrue(ex.getMessage().contains("不存在"));
    }

    @Test
    void updateStatusShouldRejectIllegalValue() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.updateStatus(1, "paused"));
        assertTrue(ex.getMessage().contains("非法状态"));
        verify(productMapper, never()).updateStatus(any(), any(), any());
    }

    @Test
    void batchShouldRejectEmptyIds() {
        com.lxs.b2cmall.shop.dto.ProductBatchDTO batch = new com.lxs.b2cmall.shop.dto.ProductBatchDTO();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.batch(batch));
        assertTrue(ex.getMessage().contains("id 列表"));
    }
}
