package com.lxs.b2cmall.shop.mapper;

import com.lxs.b2cmall.shop.dto.ProductBatchDTO;
import com.lxs.b2cmall.shop.dto.ProductQueryDTO;
import com.lxs.b2cmall.shop.entity.Product;
import com.lxs.b2cmall.shop.support.InMemorySqlite;
import com.lxs.b2cmall.shop.support.SqliteMapperSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P2 商品管理 Mapper 层真库测试：验证分页筛选、动态更新、批量操作、删除校验 SQL。
 */
class ProductMapperP2Test {

    private Connection conn;
    private SqlSessionFactory factory;
    private ProductMapper mapper;
    private OrderItemMapper orderItemMapper;

    @BeforeEach
    void setUp() {
        conn = InMemorySqlite.open();
        factory = SqliteMapperSupport.factory(conn, ProductMapper.class, OrderItemMapper.class);
        SqlSession session = factory.openSession(true);
        mapper = session.getMapper(ProductMapper.class);
        orderItemMapper = session.getMapper(OrderItemMapper.class);
    }

    private Product sample(String name, String code, String type, String category,
                           String status, String deliveryMethods) {
        Product p = new Product();
        p.setShopId(1);
        p.setName(name);
        p.setProductType(type);
        p.setProductCategory(category);
        p.setCategoryId(1);
        p.setPrice(99.9);
        p.setMarketPrice(199.9);
        p.setStock(5);
        p.setStockWarn(10);
        p.setRealSales(100);
        p.setFakeSales(50);
        p.setWarehouseStock(20);
        p.setStoreStock(8);
        p.setDeliveryMethods(deliveryMethods);
        p.setProductCode(code);
        p.setStatus(status);
        p.setSort(1);
        p.setCreateTime("2026-10-23 10:00:00");
        p.setUpdateTime("2026-10-23 10:00:00");
        return p;
    }

    @Test
    void insertAndPageQueryWithFilters() {
        mapper.insert(sample("苹果手机", "SKU001", "physical", "实物商品", "on_sale", "快递配送"));
        mapper.insert(sample("苹果耳机", "SKU002", "physical", "实物商品", "on_sale", "同城配送,快递配送"));
        mapper.insert(sample("华为手表", "SKU003", "seckill", "卡券", "off_sale", "用户自提"));

        // 名称模糊 + 状态
        ProductQueryDTO q = new ProductQueryDTO();
        q.setName("苹果");
        q.setStatus("on_sale");
        assertEquals(2, mapper.countByCondition(q));

        // 商品类型 + 类别
        q = new ProductQueryDTO();
        q.setProductType("seckill");
        q.setProductCategory("卡券");
        assertEquals(1, mapper.countByCondition(q));

        // 配送方式多选（命中任一）
        q = new ProductQueryDTO();
        q.setDeliveryMethods(Arrays.asList("同城配送", "用户自提"));
        assertEquals(2, mapper.countByCondition(q));
        List<Product> page = mapper.selectPageByCondition(q);
        assertEquals(2, page.size());

        // 分页：pageSize=1 第 1 页
        q = new ProductQueryDTO();
        q.setPageSize(1);
        q.setPage(1);
        assertEquals(3, mapper.countByCondition(q));
        assertEquals(1, mapper.selectPageByCondition(q).size());
        // 第 2、3 页各 1 条，第 4 页为空
        q.setPage(2);
        assertEquals(1, mapper.selectPageByCondition(q).size());
        q.setPage(3);
        assertEquals(1, mapper.selectPageByCondition(q).size());
        q.setPage(4);
        assertTrue(mapper.selectPageByCondition(q).isEmpty());
    }

    @Test
    void partialUpdateOnlyChangesProvidedFields() {
        mapper.insert(sample("原名称", "SKU100", "physical", "实物商品", "on_sale", "快递配送"));
        Product p = mapper.findByProductCode("SKU100");
        assertNotNull(p);
        assertEquals(100, p.getRealSales());
        assertEquals(50, p.getFakeSales());

        Product patch = new Product();
        patch.setId(p.getId());
        patch.setName("新名称");
        patch.setPrice(66.6);
        mapper.updateById(patch);

        Product after = mapper.findById(p.getId());
        assertEquals("新名称", after.getName());
        assertEquals(66.6, after.getPrice());
        // 未传字段保持原值
        assertEquals("SKU100", after.getProductCode());
        assertEquals("physical", after.getProductType());
        assertEquals(100, after.getRealSales());
    }

    @Test
    void batchStatusAndBatchFields() {
        Product a = sample("A", "SKU201", "physical", "实物商品", "on_sale", "快递配送");
        Product b = sample("B", "SKU202", "physical", "实物商品", "on_sale", "快递配送");
        mapper.insert(a);
        mapper.insert(b);

        List<Integer> ids = Arrays.asList(a.getId(), b.getId());

        int n = mapper.batchUpdateStatus(ids, "off_sale", "2026-10-23 11:00:00");
        assertEquals(2, n);
        assertEquals("off_sale", mapper.findById(a.getId()).getStatus());
        assertEquals("off_sale", mapper.findById(b.getId()).getStatus());

        ProductBatchDTO f = new ProductBatchDTO();
        f.setIds(ids);
        f.setSort(9);
        f.setWarehouseStock(66);
        f.setUpdateTime("2026-10-23 11:01:00");
        assertEquals(2, mapper.batchUpdateFields(f));
        Product after = mapper.findById(a.getId());
        assertEquals(9, after.getSort());
        assertEquals(66, after.getWarehouseStock());
        assertEquals("off_sale", after.getStatus()); // 未传 status 不被覆盖
    }

    @Test
    void deleteForbiddenWhenOrderExists() {
        mapper.insert(sample("有订单商品", "SKU301", "physical", "实物商品", "on_sale", "快递配送"));
        Product p = mapper.findByProductCode("SKU301");

        assertEquals(0, mapper.countOrderItems(p.getId()));

        // 造一条订单商品行（走同一 MyBatis 通道，保证事务可见）
        com.lxs.b2cmall.shop.entity.OrderItem item = new com.lxs.b2cmall.shop.entity.OrderItem();
        item.setOrderId(1);
        item.setProductId(p.getId());
        item.setProductName("有订单商品");
        item.setPrice(99.9);
        item.setQuantity(1);
        item.setSubtotal(99.9);
        orderItemMapper.insert(item);

        assertEquals(1, mapper.countOrderItems(p.getId()));
        // 无订单商品可直接删
        mapper.insert(sample("无订单商品", "SKU302", "physical", "实物商品", "on_sale", "快递配送"));
        Product q = mapper.findByProductCode("SKU302");
        mapper.deleteById(q.getId());
        assertNull(mapper.findById(q.getId()));
    }

    @Test
    void exportQueryAndFindByCode() {
        mapper.insert(sample("导出A", "SKU401", "physical", "实物商品", "on_sale", "快递配送"));
        assertNotNull(mapper.findByProductCode("SKU401"));
        assertNull(mapper.findByProductCode("NOT_EXIST"));

        ProductQueryDTO q = new ProductQueryDTO();
        q.setStatus("on_sale");
        assertTrue(mapper.selectAllByCondition(q).size() >= 1);
    }
}
