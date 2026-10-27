package com.lxs.b2cmall.shop.mapper;

import com.lxs.b2cmall.shop.entity.OrderStatusLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderStatusLogMapper {

    @Insert("INSERT INTO tb_order_status_log (order_id, from_status, to_status, operator, remark, create_time) " +
            "VALUES (#{orderId}, #{fromStatus}, #{toStatus}, #{operator}, #{remark}, #{createTime})")
    void insert(OrderStatusLog log);

    @Select("SELECT id, order_id, from_status, to_status, operator, remark, create_time " +
            "FROM tb_order_status_log WHERE order_id = #{orderId} ORDER BY id ASC")
    @Results({
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "fromStatus", column = "from_status"),
            @Result(property = "toStatus", column = "to_status"),
            @Result(property = "createTime", column = "create_time")
    })
    List<OrderStatusLog> findByOrderId(@Param("orderId") Integer orderId);
}
