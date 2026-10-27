package com.lxs.b2cmall.shop.mapper;

import com.lxs.b2cmall.shop.entity.Product;
import com.lxs.b2cmall.shop.support.InMemorySqlite;
import com.lxs.b2cmall.shop.support.SqliteMapperSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P4 C 端商品列表：
 * 1. 列表返回对外展示销量 sales = real_sales + fake_sales（计算列）
 * 2. sort=sales / price_asc / price_desc / default 排序
 * 3. 仅返回上架商品；详情接口下架商品不可见
 */
class ProductMapperP4Test {

    private Connection conn;
    private SqlSession session;
    private ProductMapper mapper;

    private Product p1; // 销量 15，价格 9.9
    private Product p2; // 销量 3，价格 19.9
    private Product p3; // 销量 100 但已下架

    @BeforeEach
    void setUp() {
        conn = InMemorySqlite.open();
        SqlSessionFactory factory = SqliteMapperSupport.factory(conn, ProductMapper.class);
        session = factory.openSession(true);
        mapper = session.getMapper(ProductMapper.class);

        p1 = insert("P4-A", 9.9, 10, 5, "on_sale");
        p2 = insert("P4-B", 19.9, 3, 0, "on_sale");
        p3 = insert("P4-C", 29.9, 100, 0, "off_sale");
    }

    @AfterEach
    void tearDown() throws Exception {
        session.close();
        conn.close();
    }

    private Product insert(String name, double price, int realSales, int fakeSales, String status) {
        Product p = new Product();
        p.setShopId(1);
        p.setName(name);
        p.setProductType("physical");
        p.setProductCategory("实物商品");
        p.setPrice(price);
        p.setStock(100);
        p.setRealSales(realSales);
        p.setFakeSales(fakeSales);
        p.setStatus(status);
        p.setCreateTime("2026-10-27 10:00:00");
        p.setUpdateTime("2026-10-27 10:00:00");
        mapper.insert(p);
        return p;
    }

    @Test
    void listReturnsDisplaySales() {
        List<Product> list = mapper.findByCondition(null, null, "default");
        assertEquals(2, list.size());
        for (Product p : list) {
            if (p.getId().equals(p1.getId())) {
                assertEquals(15, p.getSales());
            } else if (p.getId().equals(p2.getId())) {
                assertEquals(3, p.getSales());
            } else {
                fail("不应出现下架商品: " + p.getId());
            }
        }
    }

    @Test
    void sortSalesDesc() {
        List<Product> list = mapper.findByCondition(null, null, "sales");
        assertEquals(2, list.size());
        assertEquals(p1.getId(), list.get(0).getId());
        assertEquals(p2.getId(), list.get(1).getId());
    }

    @Test
    void sortPriceAscAndDesc() {
        List<Product> asc = mapper.findByCondition(null, null, "price_asc");
        assertEquals(p1.getId(), asc.get(0).getId());
        assertEquals(p2.getId(), asc.get(1).getId());

        List<Product> desc = mapper.findByCondition(null, null, "price_desc");
        assertEquals(p2.getId(), desc.get(0).getId());
        assertEquals(p1.getId(), desc.get(1).getId());
    }

    @Test
    void filterByCategory() {
        List<Product> list = mapper.findByCondition(999, null, "default");
        assertTrue(list.isEmpty());

        list = mapper.findByCondition(p1.getCategoryId(), null, "default");
        // 测试数据未设置 categoryId，两种情况都允许；只断言不抛异常且结果都为上架
        for (Product p : list) {
            assertEquals("on_sale", p.getStatus());
        }
    }

    @Test
    void onSaleDetailVisible() {
        Product detail = mapper.findOnSaleById(p1.getId());
        assertNotNull(detail);
        assertEquals(15, detail.getSales());
        assertEquals("on_sale", detail.getStatus());
    }

    @Test
    void offSaleDetailInvisible() {
        assertNull(mapper.findOnSaleById(p3.getId()));
    }

    @Test
    void nullSalesTreatedAsZero() {
        // P0 之前的存量数据 real_sales/fake_sales 可能为 NULL，展示销量应为 0
        Product legacy = insert("P4-legacy", 5.0, 0, 0, "on_sale");
        try (java.sql.PreparedStatement ps = conn.prepareStatement(
                "UPDATE tb_product SET real_sales = NULL, fake_sales = NULL WHERE id = ?")) {
            ps.setInt(1, legacy.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            fail(e.getMessage());
        }
        Product detail = mapper.findOnSaleById(legacy.getId());
        assertNotNull(detail);
        assertEquals(0, detail.getSales());
    }
}
