package com.lxs.b2cmall.shop.mapper;

import com.lxs.b2cmall.shop.entity.Message;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MessageMapper {

    @Insert("INSERT INTO tb_messages (shop_id, sender_id, title, content, " +
            "msg_type, is_read, created_at) " +
            "VALUES (#{shopId}, #{senderId}, #{title}, #{content}, " +
            "#{msgType}, #{isRead}, #{createdAt})")
    void insert(Message message);

    @Select("SELECT id, shop_id, sender_id, title, content, msg_type, is_read, created_at FROM tb_messages ORDER BY created_at DESC")
    @Results({
            @Result(property = "shopId", column = "shop_id"),
            @Result(property = "senderId", column = "sender_id"),
            @Result(property = "msgType", column = "msg_type"),
            @Result(property = "isRead", column = "is_read"),
            @Result(property = "createdAt", column = "created_at")
    })
    List<Message> findAll();
}