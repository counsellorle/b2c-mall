package com.lxs.b2cmall.shop.template;

import com.lxs.b2cmall.shop.dto.ProductDTO;
import com.lxs.b2cmall.shop.entity.Category;
import com.lxs.b2cmall.shop.entity.Product;
import com.lxs.b2cmall.shop.mapper.CategoryMapper;
import com.lxs.b2cmall.shop.mapper.ProductMapper;
import com.lxs.b2cmall.shop.vo.PublishResult;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class AbstractProductPublishTemplate {

    @Autowired
    protected CategoryMapper categoryMapper;

    @Autowired
    protected ProductMapper productMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 模板方法：定义固定流程骨架，禁止子类覆盖
     */
    public final PublishResult publish(ProductDTO dto, Integer shopId) {
        // 1. 参数校验（通用）
        validateParams(dto);

        // 2. 类目归属校验（通用）
        validateCategory(dto);

        // 3. 价格库存校验（通用）
        validatePriceAndStock(dto);

        // 4. 内容合规审核（钩子，子类实现）
        auditContent(dto);

        // 5. 保存商品信息（通用）
        Product product = save(dto, shopId);

        // 6. 上架（通用）
        onSale(product);

        // 7. 后置处理（默认空，子类可覆盖）
        afterProcess(product);

        return PublishResult.ok(product);
    }

    /**
     * 1. 参数校验
     */
    protected void validateParams(ProductDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new RuntimeException("商品名称不能为空");
        }
        if (dto.getProductType() == null || dto.getProductType().trim().isEmpty()) {
            throw new RuntimeException("商品类型不能为空");
        }
        if (dto.getCategoryId() == null) {
            throw new RuntimeException("类目不能为空");
        }
    }

    /**
     * 2. 类目归属校验
     */
    protected void validateCategory(ProductDTO dto) {
        Category category = categoryMapper.findById(dto.getCategoryId());
        if (category == null) {
            throw new RuntimeException("类目不存在");
        }
    }

    /**
     * 3. 价格库存校验
     */
    protected void validatePriceAndStock(ProductDTO dto) {
        if (dto.getPrice() == null || dto.getPrice() <= 0) {
            throw new RuntimeException("价格必须大于0");
        }
        if (dto.getStock() == null || dto.getStock() < 0) {
            throw new RuntimeException("库存不能为负数");
        }
        if (dto.getMarketPrice() != null && dto.getMarketPrice() < dto.getPrice()) {
            throw new RuntimeException("市场价不能低于销售价");
        }
    }

    /**
     * 4. 内容合规审核（钩子方法，子类必须实现）
     */
    protected abstract void auditContent(ProductDTO dto);

    /**
     * 5. 保存商品信息
     */
    protected Product save(ProductDTO dto, Integer shopId) {
        String now = LocalDateTime.now().format(FMT);

        Product product = new Product();
        product.setShopId(shopId);
        product.setName(dto.getName());
        product.setKeyword(dto.getKeyword());
        product.setSellingPoint(dto.getSellingPoint());
        product.setCategoryId(dto.getCategoryId());
        product.setShopCategoryId(dto.getShopCategoryId());
        product.setProductType(dto.getProductType());
        product.setMainImage(dto.getMainImage());
        product.setVideo(dto.getVideo());
        product.setBrand(dto.getBrand());
        product.setPrice(dto.getPrice());
        product.setMarketPrice(dto.getMarketPrice());
        product.setStock(dto.getStock());
        product.setStockWarn(dto.getStockWarn());
        product.setProductCode(dto.getProductCode());
        product.setSort(dto.getSort());
        product.setComplianceInfo(dto.getComplianceInfo());
        product.setCreateTime(now);
        product.setUpdateTime(now);

        productMapper.insert(product);
        return product;
    }

    /**
     * 6. 上架
     */
    protected void onSale(Product product) {
        String now = LocalDateTime.now().format(FMT);
        productMapper.updateStatus(product.getId(), "on_sale", now);
        product.setStatus("on_sale");
    }

    /**
     * 7. 后置处理（钩子方法，子类可覆盖）
     */
    protected void afterProcess(Product product) {
        // 默认空实现
    }
}