package com.lxs.b2cmall.shop.support;

import com.lxs.b2cmall.shop.entity.Order;
import com.lxs.b2cmall.shop.entity.Product;
import com.lxs.b2cmall.shop.mapper.OrderMapper;
import com.lxs.b2cmall.shop.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * B 端店铺数据归属校验。
 * <p>
 * 网关校验员工 token 后会注入 X-Shop-Id；此处校验被操作的商品/订单是否属于当前店铺，
 * 防止通过构造 id 越权操作其他店铺的数据。
 * <p>
 * shopId 为 null 表示请求未经网关（本地直连 8082 调试），此时跳过校验以保持兼容。
 */
@Component
public class ShopGuard {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderMapper orderMapper;

    public void checkProduct(Integer productId, Integer shopId) {
        if (shopId == null) {
            return;
        }
        if (productId == null) {
            throw new RuntimeException("商品ID不能为空");
        }
        Product product = productMapper.findById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        assertBelongs(product.getShopId(), shopId, "商品");
    }

    public void checkProducts(List<Integer> productIds, Integer shopId) {
        if (shopId == null || productIds == null) {
            return;
        }
        for (Integer id : productIds) {
            checkProduct(id, shopId);
        }
    }

    public void checkOrder(Integer orderId, Integer shopId) {
        if (shopId == null) {
            return;
        }
        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        assertBelongs(order.getShopId(), shopId, "订单");
    }

    private void assertBelongs(Integer ownerShopId, Integer currentShopId, String target) {
        if (ownerShopId != null && !currentShopId.equals(ownerShopId)) {
            throw new RuntimeException("无权操作该" + target);
        }
    }
}
