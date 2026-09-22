package com.lxs.b2cmall.shop.mapper;

import com.lxs.b2cmall.shop.entity.OrderStatusLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderStatusLogMapper {

    @Insert("INSERT INTO tb_order_status_log (order_id, from_status, to_status, operator, remark, create_time) " +
            "VALUES (#{orderId}, #{fromStatus}, #{toStatus}, #{operator}, #{remark}, #{createTime})")
    void insert(OrderStatusLog log);
}