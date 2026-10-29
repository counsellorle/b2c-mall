package com.lxs.b2cmall.shop.support;

import com.lxs.b2cmall.shop.entity.Order;
import com.lxs.b2cmall.shop.entity.Product;
import com.lxs.b2cmall.shop.mapper.OrderMapper;
import com.lxs.b2cmall.shop.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * B 端店铺数据归属校验：跨店操作必须被拒绝，同店放行，未经网关（无 shopId）保持兼容。
 */
class ShopGuardTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private OrderMapper orderMapper;

    private ShopGuard guard;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        guard = new ShopGuard();
        LoginFixtures.inject(guard, "productMapper", productMapper);
        LoginFixtures.inject(guard, "orderMapper", orderMapper);
    }

    private Product product(int id, int shopId) {
        Product p = new Product();
        p.setId(id);
        p.setShopId(shopId);
        return p;
    }

    private Order order(int id, int shopId) {
        Order o = new Order();
        o.setId(id);
        o.setShopId(shopId);
        return o;
    }

    @Test
    @DisplayName("本店铺商品：允许操作")
    void sameShopProductAllowed() {
        when(productMapper.findById(10)).thenReturn(product(10, 1));

        assertThatCode(() -> guard.checkProduct(10, 1)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("他店商品：拒绝操作，避免越权改价/下架")
    void otherShopProductRejected() {
        when(productMapper.findById(10)).thenReturn(product(10, 2));

        assertThatThrownBy(() -> guard.checkProduct(10, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("无权操作该商品");
    }

    @Test
    @DisplayName("商品不存在：提示不存在")
    void missingProductRejected() {
        when(productMapper.findById(99)).thenReturn(null);

        assertThatThrownBy(() -> guard.checkProduct(99, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("商品不存在");
    }

    @Test
    @DisplayName("批量操作中任一商品不属于本店即整体拒绝")
    void batchRejectedWhenAnyProductNotOwned() {
        when(productMapper.findById(10)).thenReturn(product(10, 1));
        when(productMapper.findById(11)).thenReturn(product(11, 3));

        assertThatThrownBy(() -> guard.checkProducts(Arrays.asList(10, 11), 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("无权操作该商品");
    }

    @Test
    @DisplayName("他店订单：拒绝发货/查看详情")
    void otherShopOrderRejected() {
        when(orderMapper.findById(20)).thenReturn(order(20, 2));

        assertThatThrownBy(() -> guard.checkOrder(20, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("无权操作该订单");
    }

    @Test
    @DisplayName("未经网关（shopId 为空）时跳过校验，保持本地联调兼容")
    void skipWhenNoShopId() {
        assertThatCode(() -> {
            guard.checkProduct(10, null);
            guard.checkOrder(20, null);
        }).doesNotThrowAnyException();
    }
}
