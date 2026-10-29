package com.lxs.b2cmall.shop.mapper;

import com.lxs.b2cmall.shop.entity.Cart;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CartMapper {

    @Insert("INSERT INTO tb_cart (user_id, product_id, quantity, create_time) " +
            "VALUES (#{userId}, #{productId}, #{quantity}, #{createTime})")
    void insert(Cart cart);

    @Select("SELECT id, user_id, product_id, quantity, create_time FROM tb_cart WHERE user_id = #{userId}")
    @Results({
            @Result(property = "userId", column = "user_id"),
            @Result(property = "productId", column = "product_id"),
            @Result(property = "createTime", column = "create_time")
    })
    List<Cart> findByUserId(@Param("userId") Integer userId);

    /** 同一用户同一商品只保留一行（复购时用于合并数量） */
    @Select("SELECT id, user_id, product_id, quantity, create_time FROM tb_cart " +
            "WHERE user_id = #{userId} AND product_id = #{productId} LIMIT 1")
    @Results({
            @Result(property = "userId", column = "user_id"),
            @Result(property = "productId", column = "product_id"),
            @Result(property = "createTime", column = "create_time")
    })
    Cart findByUserAndProduct(@Param("userId") Integer userId, @Param("productId") Integer productId);

    @Update("UPDATE tb_cart SET quantity = #{quantity} WHERE id = #{id}")
    void updateQuantity(@Param("id") Integer id, @Param("quantity") Integer quantity);

    @Delete("DELETE FROM tb_cart WHERE id = #{id} AND user_id = #{userId}")
    void delete(@Param("id") Integer id, @Param("userId") Integer userId);

    @Delete("DELETE FROM tb_cart WHERE user_id = #{userId}")
    void clearByUserId(@Param("userId") Integer userId);
}