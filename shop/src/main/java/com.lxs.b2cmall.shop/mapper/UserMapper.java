package com.lxs.b2cmall.shop.mapper;

import com.lxs.b2cmall.shop.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {

    @Select("SELECT id, username, password, nickname, phone, status, created_at " +
            "FROM tb_user WHERE username = #{username}")
    @Results({
            @Result(property = "createdAt", column = "created_at")
    })
    User findByUsername(@Param("username") String username);

    @Select("SELECT id, username, password, nickname, phone, status, created_at " +
            "FROM tb_user WHERE id = #{id}")
    @Results({
            @Result(property = "createdAt", column = "created_at")
    })
    User findById(@Param("id") Integer id);

    @Insert("INSERT INTO tb_user (username, password, nickname, phone, status, created_at) " +
            "VALUES (#{username}, #{password}, #{nickname}, #{phone}, COALESCE(#{status}, 1), #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);
}
