package com.lxs.b2cmall.shop.service;

import com.lxs.b2cmall.shop.dto.ProductImportResult;
import com.lxs.b2cmall.shop.dto.ProductQueryDTO;
import com.lxs.b2cmall.shop.entity.Product;
import com.lxs.b2cmall.shop.mapper.ProductMapper;
import com.lxs.b2cmall.shop.service.impl.ProductServiceImpl;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * P2 Excel 导入/导出：真实 POI 往返 + mock 落库行为。
 */
class ProductServiceImplExcelTest {

    private ProductMapper productMapper;
    private ProductServiceImpl service;

    @BeforeEach
    void setUp() {
        productMapper = Mockito.mock(ProductMapper.class);
        service = new ProductServiceImpl();
        ReflectionTestUtils.setField(service, "productMapper", productMapper);
    }

    private Product sample() {
        Product p = new Product();
        p.setId(1);
        p.setShopId(1);
        p.setName("测试手机");
        p.setProductType("physical");
        p.setProductCategory("实物商品");
        p.setCategoryId(2);
        p.setPrice(1999.0);
        p.setMarketPrice(2599.0);
        p.setStock(30);
        p.setWarehouseStock(50);
        p.setStoreStock(10);
        p.setRealSales(120);
        p.setFakeSales(80);
        p.setDeliveryMethods("快递配送,同城配送");
        p.setProductCode("SKU-EX-001");
        p.setSort(3);
        p.setStatus("on_sale");
        return p;
    }

    @Test
    void exportShouldContainHeadersAndData() throws Exception {
        when(productMapper.selectAllByCondition(any(ProductQueryDTO.class)))
                .thenReturn(Collections.singletonList(sample()));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.export(new ProductQueryDTO(), out);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(0);
            assertEquals("商品名称", header.getCell(0).getStringCellValue());
            assertEquals("状态", header.getCell(14).getStringCellValue());

            Row data = sheet.getRow(1);
            assertEquals("测试手机", data.getCell(0).getStringCellValue());
            assertEquals(1999.0, data.getCell(5).getNumericCellValue(), 0.001);
            assertEquals("SKU-EX-001", data.getCell(12).getStringCellValue());
        }
    }

    @Test
    void importShouldInsertNewAndUpdateByCode() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("商品列表");
            Row header = sheet.createRow(0);
            String[] headers = {"商品名称", "商品类型", "商品类别", "分类ID", "市场价", "销售价",
                    "库存", "仓库库存", "门店库存", "真实销量", "注水销量",
                    "配送方式", "商品编码", "排序", "状态"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            // 第 1 行：新商品（无编码）
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("新商品A");
            r1.createCell(1).setCellValue("physical");
            r1.createCell(2).setCellValue("实物商品");
            r1.createCell(3).setCellValue(1);
            r1.createCell(4).setCellValue(100);
            r1.createCell(5).setCellValue(59.9);
            r1.createCell(6).setCellValue(10);
            r1.createCell(7).setCellValue(100);
            r1.createCell(8).setCellValue(5);
            r1.createCell(9).setCellValue(0);
            r1.createCell(10).setCellValue(0);
            r1.createCell(11).setCellValue("快递配送");
            r1.createCell(12).setCellValue("");
            r1.createCell(13).setCellValue(1);
            r1.createCell(14).setCellValue("on_sale");
            // 第 2 行：已有编码 → 走更新
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("老商品B");
            r2.createCell(1).setCellValue("physical");
            r2.createCell(2).setCellValue("实物商品");
            r2.createCell(3).setCellValue(1);
            r2.createCell(4).setCellValue(200);
            r2.createCell(5).setCellValue(99.9);
            r2.createCell(6).setCellValue(20);
            r2.createCell(7).setCellValue(200);
            r2.createCell(8).setCellValue(8);
            r2.createCell(9).setCellValue(5);
            r2.createCell(10).setCellValue(3);
            r2.createCell(11).setCellValue("同城配送");
            r2.createCell(12).setCellValue("SKU-OLD-1");
            r2.createCell(13).setCellValue(2);
            r2.createCell(14).setCellValue("off_sale");
            // 第 3 行：非法行（无名称）→ 计入 failures
            Row r3 = sheet.createRow(3);
            r3.createCell(1).setCellValue("physical");
            r3.createCell(5).setCellValue(10);
            r3.createCell(14).setCellValue("on_sale");
            wb.write(out);
        }

        Product existing = new Product();
        existing.setId(9);
        existing.setProductCode("SKU-OLD-1");
        when(productMapper.findByProductCode("SKU-OLD-1")).thenReturn(existing);

        ProductImportResult result = service.importExcel(
                new ByteArrayInputStream(out.toByteArray()), 1);

        assertEquals(2, result.getTotal());
        assertEquals(1, result.getInsertCount());
        assertEquals(1, result.getUpdateCount());
        assertEquals(1, result.getFailures().size());
        assertTrue(result.getFailures().get(0).contains("商品名称不能为空"));

        verify(productMapper).insert(any(Product.class));
        verify(productMapper).updateById(argThat(p -> p != null && Integer.valueOf(9).equals(p.getId())));
    }

    @Test
    void importShouldRejectBadPriceRow() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("商品列表");
            Row header = sheet.createRow(0);
            for (int i = 0; i < 15; i++) {
                header.createCell(i).setCellValue("列" + i);
            }
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("零价商品");
            r1.createCell(1).setCellValue("physical");
            r1.createCell(5).setCellValue(0); // 销售价 0，非法
            r1.createCell(14).setCellValue("on_sale");
            wb.write(out);
        }
        ProductImportResult result = service.importExcel(
                new ByteArrayInputStream(out.toByteArray()), 1);
        assertEquals(0, result.getInsertCount());
        assertEquals(1, result.getFailures().size());
        assertTrue(result.getFailures().get(0).contains("销售价"));
    }
}
