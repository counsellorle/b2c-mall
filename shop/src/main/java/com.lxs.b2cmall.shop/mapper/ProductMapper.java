package com.lxs.b2cmall.shop.mapper;

import com.lxs.b2cmall.shop.dto.ProductBatchDTO;
import com.lxs.b2cmall.shop.dto.ProductQueryDTO;
import com.lxs.b2cmall.shop.entity.Product;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductMapper {

    @Insert("INSERT INTO tb_product (shop_id, name, keyword, selling_point, category_id, " +
            "shop_category_id, product_type, main_image, video, brand, price, market_price, " +
            "stock, stock_warn, real_sales, fake_sales, warehouse_stock, store_stock, " +
            "delivery_methods, product_category, product_code, status, sort, compliance_info, create_time, update_time) " +
            "VALUES (#{shopId}, #{name}, #{keyword}, #{sellingPoint}, #{categoryId}, " +
            "#{shopCategoryId}, #{productType}, #{mainImage}, #{video}, #{brand}, #{price}, #{marketPrice}, " +
            "#{stock}, #{stockWarn}, #{realSales}, #{fakeSales}, #{warehouseStock}, #{storeStock}, " +
            "#{deliveryMethods}, #{productCategory}, #{productCode}, #{status}, #{sort}, #{complianceInfo}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Product product);

    @Update("UPDATE tb_product SET status = #{status}, update_time = #{updateTime} WHERE id = #{id}")
    void updateStatus(@Param("id") Integer id, @Param("status") String status, @Param("updateTime") String updateTime);

    @Select("SELECT COUNT(*) FROM tb_product " +
            "WHERE status = 'on_sale' AND COALESCE(warehouse_stock, stock, 0) < COALESCE(stock_warn, 0)")
    long countStockWarn();

    // ==================== P4 C 端商品列表 ====================

    /** C 端列清单：末尾追加对外展示销量（真实 + 注水）计算列 */
    String FRONT_COLUMNS = "id, shop_id, name, keyword, selling_point, category_id, shop_category_id, " +
            "product_type, main_image, video, brand, price, market_price, stock, stock_warn, " +
            "real_sales, fake_sales, warehouse_stock, store_stock, delivery_methods, product_category, " +
            "product_code, status, sort, compliance_info, create_time, update_time, " +
            "(COALESCE(real_sales,0) + COALESCE(fake_sales,0)) AS sales";

    @Select("<script>" +
            "SELECT " + FRONT_COLUMNS + " FROM tb_product WHERE status = 'on_sale'" +
            "<if test='categoryId != null'> AND category_id = #{categoryId}</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (name LIKE '%' || #{keyword} || '%' OR keyword LIKE '%' || #{keyword} || '%')</if>" +
            "<choose>" +
            "  <when test='sort == \"price_asc\"'> ORDER BY price ASC</when>" +
            "  <when test='sort == \"price_desc\"'> ORDER BY price DESC</when>" +
            "  <when test='sort == \"sales\"'> ORDER BY sales DESC</when>" +
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
            @Result(property = "updateTime", column = "update_time"),
            @Result(property = "sales", column = "sales")
    })
    List<Product> findByCondition(@Param("categoryId") Integer categoryId,
                                  @Param("keyword") String keyword,
                                  @Param("sort") String sort);

    /** C 端详情：仅返回上架商品（下架/删除的商品对外不可见） */
    @Select("SELECT " + FRONT_COLUMNS + " FROM tb_product WHERE id = #{id} AND status = 'on_sale'")
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
            @Result(property = "updateTime", column = "update_time"),
            @Result(property = "sales", column = "sales")
    })
    Product findOnSaleById(@Param("id") Integer id);

    @Select("SELECT id, shop_id, name, keyword, selling_point, category_id, shop_category_id, " +
            "product_type, main_image, video, brand, price, market_price, stock, stock_warn, " +
            "real_sales, fake_sales, warehouse_stock, store_stock, delivery_methods, product_category, " +
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

    // ==================== P2 店铺商品管理 ====================

    String ALL_COLUMNS = "id, shop_id, name, keyword, selling_point, category_id, shop_category_id, " +
            "product_type, main_image, video, brand, price, market_price, stock, stock_warn, " +
            "real_sales, fake_sales, warehouse_stock, store_stock, delivery_methods, product_category, " +
            "product_code, status, sort, compliance_info, create_time, update_time";

    String QUERY_CONDITION = "<if test='q.name != null and q.name != \"\"'> AND name LIKE '%' || #{q.name} || '%'</if>" +
            "<if test='q.productType != null and q.productType != \"\"'> AND product_type = #{q.productType}</if>" +
            "<if test='q.productCategory != null and q.productCategory != \"\"'> AND product_category = #{q.productCategory}</if>" +
            "<if test='q.status != null and q.status != \"\"'> AND status = #{q.status}</if>" +
            "<if test='q.categoryId != null'> AND category_id = #{q.categoryId}</if>" +
            "<if test='q.deliveryMethods != null and q.deliveryMethods.size() > 0'> AND (" +
            "<foreach item='dm' collection='q.deliveryMethods' separator=' OR '>" +
            "delivery_methods LIKE '%' || #{dm} || '%'</foreach>)</if>";

    @Select("<script>SELECT COUNT(*) FROM tb_product WHERE 1=1" + QUERY_CONDITION + "</script>")
    long countByCondition(@Param("q") ProductQueryDTO q);

    @Select("<script>SELECT " + ALL_COLUMNS + " FROM tb_product WHERE 1=1" + QUERY_CONDITION +
            " ORDER BY sort ASC, id DESC LIMIT #{q.limit} OFFSET #{q.offset}</script>")
    List<Product> selectPageByCondition(@Param("q") ProductQueryDTO q);

    @Select("<script>SELECT " + ALL_COLUMNS + " FROM tb_product WHERE 1=1" + QUERY_CONDITION +
            " ORDER BY sort ASC, id DESC</script>")
    List<Product> selectAllByCondition(@Param("q") ProductQueryDTO q);

    @Select("SELECT " + ALL_COLUMNS + " FROM tb_product WHERE product_code = #{code} LIMIT 1")
    Product findByProductCode(@Param("code") String code);

    @Select("SELECT COUNT(*) FROM tb_order_item WHERE product_id = #{productId}")
    long countOrderItems(@Param("productId") Integer productId);

    @Update("<script>UPDATE tb_product SET update_time = #{p.updateTime}" +
            "<if test='p.name != null'>, name = #{p.name}</if>" +
            "<if test='p.keyword != null'>, keyword = #{p.keyword}</if>" +
            "<if test='p.sellingPoint != null'>, selling_point = #{p.sellingPoint}</if>" +
            "<if test='p.categoryId != null'>, category_id = #{p.categoryId}</if>" +
            "<if test='p.shopCategoryId != null'>, shop_category_id = #{p.shopCategoryId}</if>" +
            "<if test='p.productType != null'>, product_type = #{p.productType}</if>" +
            "<if test='p.mainImage != null'>, main_image = #{p.mainImage}</if>" +
            "<if test='p.video != null'>, video = #{p.video}</if>" +
            "<if test='p.brand != null'>, brand = #{p.brand}</if>" +
            "<if test='p.price != null'>, price = #{p.price}</if>" +
            "<if test='p.marketPrice != null'>, market_price = #{p.marketPrice}</if>" +
            "<if test='p.stock != null'>, stock = #{p.stock}</if>" +
            "<if test='p.stockWarn != null'>, stock_warn = #{p.stockWarn}</if>" +
            "<if test='p.realSales != null'>, real_sales = #{p.realSales}</if>" +
            "<if test='p.fakeSales != null'>, fake_sales = #{p.fakeSales}</if>" +
            "<if test='p.warehouseStock != null'>, warehouse_stock = #{p.warehouseStock}</if>" +
            "<if test='p.storeStock != null'>, store_stock = #{p.storeStock}</if>" +
            "<if test='p.deliveryMethods != null'>, delivery_methods = #{p.deliveryMethods}</if>" +
            "<if test='p.productCategory != null'>, product_category = #{p.productCategory}</if>" +
            "<if test='p.productCode != null'>, product_code = #{p.productCode}</if>" +
            "<if test='p.sort != null'>, sort = #{p.sort}</if>" +
            "<if test='p.complianceInfo != null'>, compliance_info = #{p.complianceInfo}</if>" +
            " WHERE id = #{p.id}</script>")
    void updateById(@Param("p") Product p);

    @Update("<script>UPDATE tb_product SET status = #{status}, update_time = #{updateTime} " +
            "WHERE id IN <foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int batchUpdateStatus(@Param("ids") List<Integer> ids, @Param("status") String status, @Param("updateTime") String updateTime);

    @Update("<script>UPDATE tb_product SET update_time = #{f.updateTime}" +
            "<if test='f.status != null'>, status = #{f.status}</if>" +
            "<if test='f.deliveryMethods != null'>, delivery_methods = #{f.deliveryMethods}</if>" +
            "<if test='f.categoryId != null'>, category_id = #{f.categoryId}</if>" +
            "<if test='f.sort != null'>, sort = #{f.sort}</if>" +
            "<if test='f.stock != null'>, stock = #{f.stock}</if>" +
            "<if test='f.warehouseStock != null'>, warehouse_stock = #{f.warehouseStock}</if>" +
            "<if test='f.storeStock != null'>, store_stock = #{f.storeStock}</if>" +
            " WHERE id IN <foreach item='id' collection='f.ids' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int batchUpdateFields(@Param("f") ProductBatchDTO f);

    @Delete("DELETE FROM tb_product WHERE id = #{id}")
    void deleteById(@Param("id") Integer id);
}