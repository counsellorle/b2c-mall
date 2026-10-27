package com.lxs.b2cmall.shop.mapper;

import com.lxs.b2cmall.shop.entity.OrderItem;
import com.lxs.b2cmall.shop.vo.OrderItemVO;
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

    /**
     * 批量取多个订单的商品行，并联出商品类型/类别（订单组展示需要类型标签）。
     * 商品已被删除时联表为空，行仍然保留（订单快照不能丢）。
     */
    @Select("<script>SELECT oi.id, oi.order_id, oi.product_id, oi.product_name, oi.product_image, " +
            "oi.price, oi.quantity, oi.subtotal, p.product_type, p.product_category " +
            "FROM tb_order_item oi LEFT JOIN tb_product p ON p.id = oi.product_id " +
            "WHERE oi.order_id IN " +
            "<foreach item='oid' collection='orderIds' open='(' separator=',' close=')'>#{oid}</foreach> " +
            "ORDER BY oi.order_id, oi.id</script>")
    @Results({
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "productId", column = "product_id"),
            @Result(property = "productName", column = "product_name"),
            @Result(property = "productImage", column = "product_image"),
            @Result(property = "productType", column = "product_type"),
            @Result(property = "productCategory", column = "product_category")
    })
    List<OrderItemVO> selectVoByOrderIds(@Param("orderIds") List<Integer> orderIds);
}
