package com.lxs.b2cmall.shop.mapper;

import com.lxs.b2cmall.shop.entity.LoginLog;
import org.apache.ibatis.annotations.*;

@Mapper
public interface LoginLogMapper {

    @Insert("INSERT INTO tb_login_logs (user_id, login_ip, login_device, login_location, " +
            "login_time, user_agent, status) " +
            "VALUES (#{userId}, #{loginIp}, #{loginDevice}, #{loginLocation}, " +
            "#{loginTime}, #{userAgent}, #{status})")
    void insert(LoginLog log);
}