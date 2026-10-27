package com.lxs.b2cmall.shop.mapper;

import com.lxs.b2cmall.shop.dto.OrderQueryDTO;
import com.lxs.b2cmall.shop.entity.Order;
import com.lxs.b2cmall.shop.vo.HourAmountVO;
import com.lxs.b2cmall.shop.vo.OrderTrendVO;
import com.lxs.b2cmall.shop.vo.StatusCountVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderMapper {

    // Dashboard 用（pay_time 由 Java 写入本地时间，统计口径统一加 localtime 修正）
    @Select("SELECT COUNT(*) FROM tb_order WHERE status = #{status}")
    long countByStatus(@Param("status") String status);

    @Select("SELECT COALESCE(SUM(pay_amount), 0) FROM tb_order WHERE pay_time IS NOT NULL AND DATE(pay_time) = DATE('now', 'localtime')")
    double todayPaymentAmount();

    @Select("SELECT COALESCE(SUM(pay_amount), 0) FROM tb_order WHERE pay_time IS NOT NULL AND DATE(pay_time) = DATE('now', 'localtime', '-1 day')")
    double yesterdayPaymentAmount();

    @Select("SELECT COALESCE(SUM(pay_amount), 0) FROM tb_order WHERE pay_time IS NOT NULL AND strftime('%Y-%m', pay_time) = strftime('%Y-%m', 'now', 'localtime')")
    double monthlyPaymentAmount();

    @Select("SELECT COUNT(*) FROM tb_order")
    long totalOrders();

    @Select("SELECT COALESCE(strftime('%H', pay_time), '') as hour, COALESCE(SUM(pay_amount), 0) as amount " +
            "FROM tb_order WHERE pay_time IS NOT NULL AND DATE(pay_time) = DATE('now', 'localtime') " +
            "GROUP BY strftime('%H', pay_time)")
    List<HourAmountVO> todayHourlyAmount();

    @Select("SELECT COALESCE(strftime('%H', pay_time), '') as hour, COALESCE(SUM(pay_amount), 0) as amount " +
            "FROM tb_order WHERE pay_time IS NOT NULL AND DATE(pay_time) = DATE('now', 'localtime', '-1 day') " +
            "GROUP BY strftime('%H', pay_time)")
    List<HourAmountVO> yesterdayHourlyAmount();

    // KPI：支付客户数（独立用户去重）
    @Select("SELECT COUNT(DISTINCT user_id) FROM tb_order WHERE pay_time IS NOT NULL AND DATE(pay_time) = DATE('now', 'localtime')")
    long todayPayUserCount();

    @Select("SELECT COUNT(DISTINCT user_id) FROM tb_order WHERE pay_time IS NOT NULL AND DATE(pay_time) = DATE('now', 'localtime', '-1 day')")
    long yesterdayPayUserCount();

    // KPI：支付订单数
    @Select("SELECT COUNT(*) FROM tb_order WHERE pay_time IS NOT NULL AND DATE(pay_time) = DATE('now', 'localtime')")
    long todayPayOrderCount();

    @Select("SELECT COUNT(*) FROM tb_order WHERE pay_time IS NOT NULL AND DATE(pay_time) = DATE('now', 'localtime', '-1 day')")
    long yesterdayPayOrderCount();

    // KPI：退款金额（以状态流转日志中当日转入"待退款"的订单实付金额合计计）
    @Select("SELECT COALESCE(SUM(o.pay_amount), 0) FROM tb_order o " +
            "JOIN tb_order_status_log l ON l.order_id = o.id " +
            "WHERE l.to_status = '待退款' AND DATE(l.create_time) = DATE('now', 'localtime')")
    double todayRefundAmount();

    @Select("SELECT COALESCE(SUM(o.pay_amount), 0) FROM tb_order o " +
            "JOIN tb_order_status_log l ON l.order_id = o.id " +
            "WHERE l.to_status = '待退款' AND DATE(l.create_time) = DATE('now', 'localtime', '-1 day')")
    double yesterdayRefundAmount();

    @Select("SELECT DATE(create_time) as date, COUNT(*) as order_count, COALESCE(SUM(pay_amount), 0) as amount FROM tb_order GROUP BY DATE(create_time) ORDER BY date")
    List<OrderTrendVO> trend();

    // 订单管理用
    @Select("SELECT id, order_no, user_id, shop_id, total_amount, pay_amount, status, pay_type, " +
            "receiver_name, receiver_phone, receiver_address, shipping_status, after_sale_status, " +
            "is_deleted, delivery_type, create_time, pay_time, ship_time FROM tb_order ORDER BY create_time DESC")
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
            @Result(property = "isDeleted", column = "is_deleted"),
            @Result(property = "deliveryType", column = "delivery_type"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "payTime", column = "pay_time"),
            @Result(property = "shipTime", column = "ship_time")
    })
    List<Order> findAll();

    @Select("SELECT o.id, o.order_no, o.user_id, o.shop_id, o.total_amount, o.pay_amount, o.status, o.pay_type, " +
            "o.receiver_name, o.receiver_phone, o.receiver_address, o.shipping_status, o.after_sale_status, " +
            "o.is_deleted, o.delivery_type, o.create_time, o.pay_time, o.ship_time, s.shop_name " +
            "FROM tb_order o LEFT JOIN tb_shop s ON s.id = o.shop_id WHERE o.id = #{id}")
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
            @Result(property = "isDeleted", column = "is_deleted"),
            @Result(property = "deliveryType", column = "delivery_type"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "payTime", column = "pay_time"),
            @Result(property = "shipTime", column = "ship_time"),
            @Result(property = "shopName", column = "shop_name")
    })
    Order findById(@Param("id") Integer id);

    @Update("UPDATE tb_order SET status = #{status}, ship_time = #{shipTime} WHERE id = #{id}")
    void updateStatus(@Param("id") Integer id, @Param("status") String status, @Param("shipTime") String shipTime);
    @Insert("INSERT INTO tb_order (order_no, user_id, shop_id, total_amount, pay_amount, status, " +
            "pay_type, receiver_name, receiver_phone, receiver_address, shipping_status, after_sale_status, " +
            "delivery_type, is_deleted, create_time) " +
            "VALUES (#{orderNo}, #{userId}, #{shopId}, #{totalAmount}, #{payAmount}, #{status}, " +
            "#{payType}, #{receiverName}, #{receiverPhone}, #{receiverAddress}, " +
            "#{shippingStatus,jdbcType=VARCHAR}, #{afterSaleStatus,jdbcType=VARCHAR}, " +
            "#{deliveryType,jdbcType=VARCHAR}, COALESCE(#{isDeleted,jdbcType=INTEGER}, 0), #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Order order);
    @Update("UPDATE tb_order SET pay_time = #{payTime} WHERE id = #{id}")
    void updatePayTime(@Param("id") Integer id, @Param("payTime") String payTime);

    // ==================== P3 店铺订单查询 ====================

    String ORDER_COLUMNS = "o.id, o.order_no, o.user_id, o.shop_id, o.total_amount, o.pay_amount, o.status, " +
            "o.pay_type, o.receiver_name, o.receiver_phone, o.receiver_address, o.shipping_status, " +
            "o.after_sale_status, o.is_deleted, o.delivery_type, o.create_time, o.pay_time, o.ship_time, " +
            "s.shop_name";

    /**
     * 11 个筛选项。B 端可见全部订单（C 端「删除」仅是用户侧隐藏），故不按 is_deleted 过滤，
     * 与 Dashboard 各状态计数口径保持一致。
     */
    String ORDER_QUERY_CONDITION =
            "<if test='q.orderNo != null and q.orderNo != \"\"'> AND o.order_no LIKE '%' || #{q.orderNo} || '%'</if>" +
            "<if test='q.status != null and q.status != \"\"'> AND o.status = #{q.status}</if>" +
            "<if test='q.receiverName != null and q.receiverName != \"\"'> AND o.receiver_name LIKE '%' || #{q.receiverName} || '%'</if>" +
            "<if test='q.receiverPhone != null and q.receiverPhone != \"\"'> AND o.receiver_phone LIKE '%' || #{q.receiverPhone} || '%'</if>" +
            "<if test='q.payType != null and q.payType != \"\"'> AND o.pay_type = #{q.payType}</if>" +
            "<if test='q.afterSaleStatus != null and q.afterSaleStatus != \"\"'> AND o.after_sale_status = #{q.afterSaleStatus}</if>" +
            "<if test='q.deliveryType != null and q.deliveryType != \"\"'> AND o.delivery_type = #{q.deliveryType}</if>" +
            "<if test='q.shopId != null'> AND o.shop_id = #{q.shopId}</if>" +
            "<if test='q.startTime != null and q.startTime != \"\"'> AND o.create_time &gt;= #{q.startTime}</if>" +
            "<if test='q.endTime != null and q.endTime != \"\"'> AND o.create_time &lt;= #{q.endTime}</if>" +
            "<if test='q.orderType != null and q.orderType != \"\"'> AND EXISTS (SELECT 1 FROM tb_order_item oi " +
            "JOIN tb_product p ON p.id = oi.product_id WHERE oi.order_id = o.id AND p.product_type = #{q.orderType})</if>" +
            "<if test='q.orderCategory != null and q.orderCategory != \"\"'> AND EXISTS (SELECT 1 FROM tb_order_item oi " +
            "JOIN tb_product p ON p.id = oi.product_id WHERE oi.order_id = o.id AND p.product_category = #{q.orderCategory})</if>";

    @Select("<script>SELECT COUNT(*) FROM tb_order o WHERE 1=1" + ORDER_QUERY_CONDITION + "</script>")
    long countByQuery(@Param("q") OrderQueryDTO q);

    @Select("<script>SELECT " + ORDER_COLUMNS + " FROM tb_order o LEFT JOIN tb_shop s ON s.id = o.shop_id " +
            "WHERE 1=1" + ORDER_QUERY_CONDITION +
            " ORDER BY o.create_time DESC, o.id DESC LIMIT #{q.limit} OFFSET #{q.offset}</script>")
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
            @Result(property = "isDeleted", column = "is_deleted"),
            @Result(property = "deliveryType", column = "delivery_type"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "payTime", column = "pay_time"),
            @Result(property = "shipTime", column = "ship_time"),
            @Result(property = "shopName", column = "shop_name")
    })
    List<Order> selectPageByQuery(@Param("q") OrderQueryDTO q);

    @Select("<script>SELECT " + ORDER_COLUMNS + " FROM tb_order o LEFT JOIN tb_shop s ON s.id = o.shop_id " +
            "WHERE 1=1" + ORDER_QUERY_CONDITION +
            " ORDER BY o.create_time DESC, o.id DESC</script>")
    List<Order> selectAllByQuery(@Param("q") OrderQueryDTO q);

    @Select("<script>SELECT status, COUNT(*) AS cnt FROM tb_order " +
            "<where><if test='shopId != null'> shop_id = #{shopId}</if></where> GROUP BY status</script>")
    List<StatusCountVO> countGroupByStatus(@Param("shopId") Integer shopId);

    @Update("<script>UPDATE tb_order SET status = #{status}, ship_time = #{shipTime}" +
            "<if test='deliveryType != null and deliveryType != \"\"'>, delivery_type = #{deliveryType}</if>" +
            " WHERE id = #{id}</script>")
    int updateShip(@Param("id") Integer id, @Param("status") String status,
                   @Param("shipTime") String shipTime, @Param("deliveryType") String deliveryType);
}