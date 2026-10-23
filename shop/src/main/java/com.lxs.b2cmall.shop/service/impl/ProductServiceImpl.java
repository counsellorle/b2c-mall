package com.lxs.b2cmall.shop.service.impl;

import com.lxs.b2cmall.shop.dto.ProductBatchDTO;
import com.lxs.b2cmall.shop.dto.ProductDTO;
import com.lxs.b2cmall.shop.dto.ProductImportResult;
import com.lxs.b2cmall.shop.dto.ProductQueryDTO;
import com.lxs.b2cmall.shop.entity.Product;
import com.lxs.b2cmall.shop.mapper.ProductMapper;
import com.lxs.b2cmall.shop.service.ProductService;
import com.lxs.b2cmall.shop.template.AbstractProductPublishTemplate;
import com.lxs.b2cmall.shop.template.ProductPublishFactory;
import com.lxs.b2cmall.shop.vo.PageVO;
import com.lxs.b2cmall.shop.vo.PublishResult;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductPublishFactory productPublishFactory;

    @Autowired
    private ProductMapper productMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Excel 导出/导入模板列头（与 importExcel 解析顺序一一对应） */
    private static final List<String> EXCEL_HEADERS = Arrays.asList(
            "商品名称", "商品类型", "商品类别", "分类ID", "市场价", "销售价",
            "库存", "仓库库存", "门店库存", "真实销量", "注水销量",
            "配送方式", "商品编码", "排序", "状态");

    private static final List<String> VALID_STATUS = Arrays.asList("on_sale", "off_sale");

    @Override
    public PublishResult publish(ProductDTO dto, Integer shopId) {
        AbstractProductPublishTemplate template = productPublishFactory.getStrategy(dto.getProductType());
        return template.publish(dto, shopId);
    }

    @Override
    public PageVO<Product> page(ProductQueryDTO query) {
        long total = productMapper.countByCondition(query);
        List<Product> list = total == 0 ? Collections.emptyList() : productMapper.selectPageByCondition(query);
        return PageVO.of(total, query.getPage() == null ? 1 : query.getPage(),
                query.getPageSize() == null ? 10 : query.getPageSize(), list);
    }

    @Override
    public void update(Product product) {
        if (product.getId() == null) {
            throw new RuntimeException("商品 id 不能为空");
        }
        Product exist = productMapper.findById(product.getId());
        if (exist == null) {
            throw new RuntimeException("商品不存在");
        }
        product.setUpdateTime(LocalDateTime.now().format(FMT));
        productMapper.updateById(product);
    }

    @Override
    public void updateStatus(Integer productId, String status) {
        if (!VALID_STATUS.contains(status)) {
            throw new RuntimeException("非法状态值，仅支持 on_sale / off_sale");
        }
        Product exist = productMapper.findById(productId);
        if (exist == null) {
            throw new RuntimeException("商品不存在");
        }
        productMapper.updateStatus(productId, status, LocalDateTime.now().format(FMT));
    }

    @Override
    public void delete(Integer productId) {
        Product exist = productMapper.findById(productId);
        if (exist == null) {
            throw new RuntimeException("商品不存在");
        }
        // 已产生订单的商品禁止删除
        long orderCount = productMapper.countOrderItems(productId);
        if (orderCount > 0) {
            throw new RuntimeException("该商品已产生 " + orderCount + " 条订单记录，禁止删除，可改为下架处理");
        }
        productMapper.deleteById(productId);
    }

    @Override
    public int batch(ProductBatchDTO batch) {
        if (batch.getIds() == null || batch.getIds().isEmpty()) {
            throw new RuntimeException("批量操作必须指定商品 id 列表");
        }
        if (batch.getStatus() != null && !VALID_STATUS.contains(batch.getStatus())) {
            throw new RuntimeException("非法状态值，仅支持 on_sale / off_sale");
        }
        // 纯状态批量走 batchUpdateStatus，其余字段合并走 batchUpdateFields
        if (batch.getStatus() != null && batch.getDeliveryMethods() == null && batch.getCategoryId() == null
                && batch.getSort() == null && batch.getStock() == null
                && batch.getWarehouseStock() == null && batch.getStoreStock() == null) {
            return productMapper.batchUpdateStatus(batch.getIds(), batch.getStatus(), LocalDateTime.now().format(FMT));
        }
        batch.setUpdateTime(LocalDateTime.now().format(FMT));
        return productMapper.batchUpdateFields(batch);
    }

    @Override
    public void export(ProductQueryDTO query, OutputStream out) {
        List<Product> list = productMapper.selectAllByCondition(query);
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("商品列表");
            Row header = sheet.createRow(0);
            for (int i = 0; i < EXCEL_HEADERS.size(); i++) {
                header.createCell(i).setCellValue(EXCEL_HEADERS.get(i));
            }
            int r = 1;
            for (Product p : list) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(nvl(p.getName()));
                row.createCell(1).setCellValue(nvl(p.getProductType()));
                row.createCell(2).setCellValue(nvl(p.getProductCategory()));
                row.createCell(3).setCellValue(p.getCategoryId() == null ? 0 : p.getCategoryId());
                row.createCell(4).setCellValue(p.getMarketPrice() == null ? 0 : p.getMarketPrice());
                row.createCell(5).setCellValue(p.getPrice() == null ? 0 : p.getPrice());
                row.createCell(6).setCellValue(p.getStock() == null ? 0 : p.getStock());
                row.createCell(7).setCellValue(p.getWarehouseStock() == null ? 0 : p.getWarehouseStock());
                row.createCell(8).setCellValue(p.getStoreStock() == null ? 0 : p.getStoreStock());
                row.createCell(9).setCellValue(p.getRealSales() == null ? 0 : p.getRealSales());
                row.createCell(10).setCellValue(p.getFakeSales() == null ? 0 : p.getFakeSales());
                row.createCell(11).setCellValue(nvl(p.getDeliveryMethods()));
                row.createCell(12).setCellValue(nvl(p.getProductCode()));
                row.createCell(13).setCellValue(p.getSort() == null ? 0 : p.getSort());
                row.createCell(14).setCellValue(nvl(p.getStatus()));
            }
            wb.write(out);
        } catch (Exception e) {
            throw new RuntimeException("导出 Excel 失败：" + e.getMessage(), e);
        }
    }

    @Override
    public ProductImportResult importExcel(InputStream in, Integer shopId) {
        ProductImportResult result = new ProductImportResult();
        List<Product> rows = new ArrayList<>();
        try (XSSFWorkbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            // 第 0 行为表头，数据从第 1 行开始
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                try {
                    Product p = parseRow(row, shopId);
                    validate(p, i);
                    rows.add(p);
                    result.setTotal(result.getTotal() + 1);
                } catch (Exception e) {
                    result.getFailures().add("第 " + (i + 1) + " 行：" + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("解析 Excel 失败：" + e.getMessage(), e);
        }

        String now = LocalDateTime.now().format(FMT);
        for (Product p : rows) {
            if (StringUtils.hasText(p.getProductCode())) {
                Product exist = productMapper.findByProductCode(p.getProductCode());
                if (exist != null) {
                    p.setId(exist.getId());
                    p.setUpdateTime(now);
                    productMapper.updateById(p);
                    result.setUpdateCount(result.getUpdateCount() + 1);
                    continue;
                }
            }
            p.setShopId(shopId == null ? 1 : shopId);
            p.setCreateTime(now);
            p.setUpdateTime(now);
            productMapper.insert(p);
            result.setInsertCount(result.getInsertCount() + 1);
        }
        return result;
    }

    private Product parseRow(Row row, Integer shopId) {
        Product p = new Product();
        p.setShopId(shopId == null ? 1 : shopId);
        p.setName(cellStr(row, 0));
        p.setProductType(cellStr(row, 1));
        p.setProductCategory(cellStr(row, 2));
        p.setCategoryId(cellInt(row, 3));
        p.setMarketPrice(cellDbl(row, 4));
        p.setPrice(cellDbl(row, 5));
        p.setStock(cellInt(row, 6));
        p.setWarehouseStock(cellInt(row, 7));
        p.setStoreStock(cellInt(row, 8));
        p.setRealSales(cellInt(row, 9));
        p.setFakeSales(cellInt(row, 10));
        p.setDeliveryMethods(cellStr(row, 11));
        p.setProductCode(cellStr(row, 12));
        p.setSort(cellInt(row, 13));
        String status = cellStr(row, 14);
        p.setStatus(StringUtils.hasText(status) ? status : "on_sale");
        return p;
    }

    private void validate(Product p, int rowIndex) {
        if (!StringUtils.hasText(p.getName())) {
            throw new RuntimeException("商品名称不能为空");
        }
        if (!StringUtils.hasText(p.getProductType())) {
            throw new RuntimeException("商品类型不能为空（physical/virtual 等）");
        }
        if (p.getPrice() == null || p.getPrice() <= 0) {
            throw new RuntimeException("销售价必须大于 0");
        }
        if (!VALID_STATUS.contains(p.getStatus())) {
            throw new RuntimeException("状态仅支持 on_sale / off_sale");
        }
    }

    private boolean isBlankRow(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK
                    && !(cell.getCellType() == CellType.STRING && cell.getStringCellValue().trim().isEmpty())) {
                return false;
            }
        }
        return true;
    }

    private String cellStr(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        DataFormatter fmt = new DataFormatter();
        String v = fmt.formatCellValue(cell).trim();
        return v.isEmpty() ? null : v;
    }

    private Integer cellInt(Row row, int idx) {
        String v = cellStr(row, idx);
        if (v == null) return null;
        try {
            return (int) Double.parseDouble(v);
        } catch (NumberFormatException e) {
            throw new RuntimeException("数字列格式不正确: " + v);
        }
    }

    private Double cellDbl(Row row, int idx) {
        String v = cellStr(row, idx);
        if (v == null) return null;
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            throw new RuntimeException("金额列格式不正确: " + v);
        }
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}
