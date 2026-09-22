package com.lxs.b2cmall.shop.mapper;

import com.lxs.b2cmall.shop.entity.Category;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CategoryMapper {

    @Select("SELECT id, parent_id, name, level, type, sort, created_at FROM tb_category WHERE id = #{id}")
    @Results({
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "createdAt", column = "created_at")
    })
    Category findById(@Param("id") Integer id);

    @Select("SELECT id, parent_id, name, level, type, sort, created_at FROM tb_category ORDER BY sort ASC")
    @Results({
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "createdAt", column = "created_at")
    })
    List<Category> findAll();
}