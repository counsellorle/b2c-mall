package com.lxs.b2cmall.shop.mapper;

import com.lxs.b2cmall.shop.entity.Product;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductMapper {

    @Insert("INSERT INTO tb_product (shop_id, name, keyword, selling_point, category_id, " +
            "shop_category_id, product_type, main_image, video, brand, price, market_price, " +
            "stock, stock_warn, product_code, status, sort, compliance_info, create_time, update_time) " +
            "VALUES (#{shopId}, #{name}, #{keyword}, #{sellingPoint}, #{categoryId}, " +
            "#{shopCategoryId}, #{productType}, #{mainImage}, #{video}, #{brand}, #{price}, #{marketPrice}, " +
            "#{stock}, #{stockWarn}, #{productCode}, #{status}, #{sort}, #{complianceInfo}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Product product);

    @Update("UPDATE tb_product SET status = #{status}, update_time = #{updateTime} WHERE id = #{id}")
    void updateStatus(@Param("id") Integer id, @Param("status") String status, @Param("updateTime") String updateTime);

    @Select("<script>" +
            "SELECT id, shop_id, name, keyword, selling_point, category_id, shop_category_id, " +
            "product_type, main_image, video, brand, price, market_price, stock, stock_warn, " +
            "product_code, status, sort, compliance_info, create_time, update_time " +
            "FROM tb_product WHERE status = 'on_sale'" +
            "<if test='categoryId != null'> AND category_id = #{categoryId}</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (name LIKE '%' || #{keyword} || '%' OR keyword LIKE '%' || #{keyword} || '%')</if>" +
            "<choose>" +
            "  <when test='sort == \"price_asc\"'> ORDER BY price ASC</when>" +
            "  <when test='sort == \"price_desc\"'> ORDER BY price DESC</when>" +
            "  <when test='sort == \"sales\"'> ORDER BY stock DESC</when>" +
            "  <otherwise> ORDER BY sort ASC, create_time DESC</otherwise>" +
            "</choose>" +
            "</script>")
    @Results({
            @Result(property = "shopId", column = "shop_id"),
            @Result(property = "sellingPoint", column = "selling_point"),
            @Result(property = "categoryId", column = "category_id"),
            @Result(property = "shopCategoryId", column = "shop_category_id"),
            @Result(property = "productType", column = "product_type"),
            @Result(property = "mainImage", column = "main_image"),
            @Result(property = "marketPrice", column = "market_price"),
            @Result(property = "stockWarn", column = "stock_warn"),
            @Result(property = "productCode", column = "product_code"),
            @Result(property = "complianceInfo", column = "compliance_info"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "updateTime", column = "update_time")
    })
    List<Product> findByCondition(@Param("categoryId") Integer categoryId,
                                  @Param("keyword") String keyword,
                                  @Param("sort") String sort);

    @Select("SELECT id, shop_id, name, keyword, selling_point, category_id, shop_category_id, " +
            "product_type, main_image, video, brand, price, market_price, stock, stock_warn, " +
            "product_code, status, sort, compliance_info, create_time, update_time " +
            "FROM tb_product WHERE id = #{id}")
    @Results({
            @Result(property = "shopId", column = "shop_id"),
            @Result(property = "sellingPoint", column = "selling_point"),
            @Result(property = "categoryId", column = "category_id"),
            @Result(property = "shopCategoryId", column = "shop_category_id"),
            @Result(property = "productType", column = "product_type"),
            @Result(property = "mainImage", column = "main_image"),
            @Result(property = "marketPrice", column = "market_price"),
            @Result(property = "stockWarn", column = "stock_warn"),
            @Result(property = "productCode", column = "product_code"),
            @Result(property = "complianceInfo", column = "compliance_info"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "updateTime", column = "update_time")
    })
    Product findById(@Param("id") Integer id);
    @Update("UPDATE tb_product SET stock = #{stock}, update_time = #{updateTime} WHERE id = #{id}")
    void updateStock(@Param("id") Integer id, @Param("stock") Integer stock, @Param("updateTime") String updateTime);
}