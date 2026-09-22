package com.lxs.b2cmall.shop.mapper;

import com.lxs.b2cmall.shop.entity.Employee;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.type.JdbcType;

@Mapper
public interface EmployeeMapper {

    @Select("SELECT id, shop_id, username, password, avatar_url, " +
            "last_login_time, login_count, status, created_at, updated_at " +
            "FROM tb_employee WHERE username = #{username}")
    @Results({
            @Result(property = "shopId", column = "shop_id"),
            @Result(property = "avatarUrl", column = "avatar_url"),
            @Result(property = "lastLoginTime", column = "last_login_time", jdbcType = JdbcType.TIMESTAMP),
            @Result(property = "loginCount", column = "login_count"),
            @Result(property = "createdAt", column = "created_at", jdbcType = JdbcType.TIMESTAMP),
            @Result(property = "updatedAt", column = "updated_at", jdbcType = JdbcType.TIMESTAMP)
    })
    Employee findByUsername(@Param("username") String username);

    @Update("UPDATE tb_employee SET last_login_time = CURRENT_TIMESTAMP, " +
            "login_count = login_count + 1, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id}")
    void updateLoginInfo(@Param("id") Integer id);

    @Insert("INSERT INTO tb_employee (shop_id, username, password, avatar_url, " +
            "login_count, status, created_at, updated_at) " +
            "VALUES (#{shopId}, #{username}, #{password}, #{avatarUrl}, " +
            "#{loginCount}, #{status}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Employee employee);
}