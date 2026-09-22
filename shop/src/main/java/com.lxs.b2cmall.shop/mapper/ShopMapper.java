package com.lxs.b2cmall.shop.mapper;

import com.lxs.b2cmall.shop.entity.Shop;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ShopMapper {

    @Insert("INSERT INTO tb_shop (shop_name, admin_account, admin_password, logo_url, " +
            "status, created_at, updated_at) " +
            "VALUES (#{shopName}, #{adminAccount}, #{adminPassword}, #{logoUrl}, " +
            "#{status}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Shop shop);
}