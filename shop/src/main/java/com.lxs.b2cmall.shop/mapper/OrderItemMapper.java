package com.lxs.b2cmall.shop.mapper;

import com.lxs.b2cmall.shop.entity.OrderItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderItemMapper {

    @Insert("INSERT INTO tb_order_item (order_id, product_id, product_name, product_image, " +
            "price, quantity, subtotal) " +
            "VALUES (#{orderId}, #{productId}, #{productName}, #{productImage}, " +
            "#{price}, #{quantity}, #{subtotal})")
    void insert(OrderItem item);

    @Select("SELECT id, order_id, product_id, product_name, product_image, price, quantity, subtotal " +
            "FROM tb_order_item WHERE order_id = #{orderId}")
    @Results({
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "productId", column = "product_id"),
            @Result(property = "productName", column = "product_name"),
            @Result(property = "productImage", column = "product_image")
    })
    List<OrderItem> findByOrderId(@Param("orderId") Integer orderId);
}