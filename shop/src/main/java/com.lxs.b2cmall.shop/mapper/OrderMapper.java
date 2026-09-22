package com.lxs.b2cmall.shop.mapper;

import com.lxs.b2cmall.shop.entity.Order;
import com.lxs.b2cmall.shop.vo.OrderTrendVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderMapper {

    // Dashboard 用
    @Select("SELECT COUNT(*) FROM tb_order WHERE status = #{status}")
    long countByStatus(@Param("status") String status);

    @Select("SELECT COALESCE(SUM(pay_amount), 0) FROM tb_order WHERE pay_time IS NOT NULL AND DATE(pay_time) = DATE('now')")
    double todayPaymentAmount();

    @Select("SELECT COALESCE(SUM(pay_amount), 0) FROM tb_order WHERE pay_time IS NOT NULL AND strftime('%Y-%m', pay_time) = strftime('%Y-%m', 'now')")
    double monthlyPaymentAmount();

    @Select("SELECT COUNT(*) FROM tb_order")
    long totalOrders();

    @Select("SELECT DATE(create_time) as date, COUNT(*) as order_count, COALESCE(SUM(pay_amount), 0) as amount FROM tb_order GROUP BY DATE(create_time) ORDER BY date")
    List<OrderTrendVO> trend();

    // 订单管理用
    @Select("SELECT id, order_no, user_id, shop_id, total_amount, pay_amount, status, pay_type, " +
            "receiver_name, receiver_phone, receiver_address, shipping_status, after_sale_status, " +
            "create_time, pay_time, ship_time FROM tb_order ORDER BY create_time DESC")
    @Results({
            @Result(property = "orderNo", column = "order_no"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "shopId", column = "shop_id"),
            @Result(property = "totalAmount", column = "total_amount"),
            @Result(property = "payAmount", column = "pay_amount"),
            @Result(property = "payType", column = "pay_type"),
            @Result(property = "receiverName", column = "receiver_name"),
            @Result(property = "receiverPhone", column = "receiver_phone"),
            @Result(property = "receiverAddress", column = "receiver_address"),
            @Result(property = "shippingStatus", column = "shipping_status"),
            @Result(property = "afterSaleStatus", column = "after_sale_status"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "payTime", column = "pay_time"),
            @Result(property = "shipTime", column = "ship_time")
    })
    List<Order> findAll();

    @Select("SELECT id, order_no, user_id, shop_id, total_amount, pay_amount, status, pay_type, " +
            "receiver_name, receiver_phone, receiver_address, shipping_status, after_sale_status, " +
            "create_time, pay_time, ship_time FROM tb_order WHERE id = #{id}")
    @Results({
            @Result(property = "orderNo", column = "order_no"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "shopId", column = "shop_id"),
            @Result(property = "totalAmount", column = "total_amount"),
            @Result(property = "payAmount", column = "pay_amount"),
            @Result(property = "payType", column = "pay_type"),
            @Result(property = "receiverName", column = "receiver_name"),
            @Result(property = "receiverPhone", column = "receiver_phone"),
            @Result(property = "receiverAddress", column = "receiver_address"),
            @Result(property = "shippingStatus", column = "shipping_status"),
            @Result(property = "afterSaleStatus", column = "after_sale_status"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "payTime", column = "pay_time"),
            @Result(property = "shipTime", column = "ship_time")
    })
    Order findById(@Param("id") Integer id);

    @Update("UPDATE tb_order SET status = #{status}, ship_time = #{shipTime} WHERE id = #{id}")
    void updateStatus(@Param("id") Integer id, @Param("status") String status, @Param("shipTime") String shipTime);
    @Insert("INSERT INTO tb_order (order_no, user_id, shop_id, total_amount, pay_amount, status, " +
            "pay_type, receiver_name, receiver_phone, receiver_address, create_time) " +
            "VALUES (#{orderNo}, #{userId}, #{shopId}, #{totalAmount}, #{payAmount}, #{status}, " +
            "#{payType}, #{receiverName}, #{receiverPhone}, #{receiverAddress}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Order order);
    @Update("UPDATE tb_order SET pay_time = #{payTime} WHERE id = #{id}")
    void updatePayTime(@Param("id") Integer id, @Param("payTime") String payTime);
}