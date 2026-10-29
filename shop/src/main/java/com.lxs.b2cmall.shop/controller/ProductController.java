package com.lxs.b2cmall.shop.controller;

import com.lxs.b2cmall.shop.auth.CurrentShop;
import com.lxs.b2cmall.shop.dto.ProductBatchDTO;
import com.lxs.b2cmall.shop.dto.ProductDTO;
import com.lxs.b2cmall.shop.dto.ProductImportResult;
import com.lxs.b2cmall.shop.dto.ProductQueryDTO;
import com.lxs.b2cmall.shop.entity.Product;
import com.lxs.b2cmall.shop.service.ProductService;
import com.lxs.b2cmall.shop.support.ShopGuard;
import com.lxs.b2cmall.shop.vo.PageVO;
import com.lxs.b2cmall.shop.vo.PublishResult;
import com.lxs.b2cmall.shop.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;

/**
 * B 端商品管理。
 * <p>
 * 店铺身份由网关校验员工 token 后注入 X-Shop-Id：查询自动限定本店铺，写操作校验数据归属。
 * 本地直连 8082（无网关头）时退回请求参数 / 默认店铺，便于联调。
 */
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CurrentShop currentShop;

    @Autowired
    private ShopGuard shopGuard;

    /** 演示用：未经网关时（本地直连）的默认店铺 id */
    private static final Integer DEFAULT_SHOP_ID = 1;

    @PostMapping("/publish")
    public Result<PublishResult> publish(@Valid @RequestBody ProductDTO dto,
                                         @RequestParam(required = false) Integer shopId,
                                         HttpServletRequest request) {
        try {
            PublishResult result = productService.publish(dto, resolveShopId(shopId, request));
            if (result.isSuccess()) {
                return Result.success(result);
            } else {
                return Result.error(result.getMessage());
            }
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 分页多条件查询：名称模糊 / 商品类型 / 商品类别 / 状态 / 配送方式多选 / 分类（自动限定本店铺） */
    @GetMapping("/page")
    public Result<PageVO<Product>> page(ProductQueryDTO query, HttpServletRequest request) {
        try {
            Integer shopId = currentShop.shopId(request);
            if (shopId != null) {
                query.setShopId(shopId);
            }
            return Result.success(productService.page(query));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 编辑商品（仅更新传入的非空字段） */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody Product product, HttpServletRequest request) {
        try {
            shopGuard.checkProduct(product.getId(), currentShop.shopId(request));
            productService.update(product);
            return Result.success(null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 单商品上架/下架：status = on_sale / off_sale */
    @PostMapping("/status")
    public Result<Void> updateStatus(@RequestParam Integer productId, @RequestParam String status,
                                     HttpServletRequest request) {
        try {
            shopGuard.checkProduct(productId, currentShop.shopId(request));
            productService.updateStatus(productId, status);
            return Result.success(null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 删除商品：已产生订单的商品禁止删除 */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestParam Integer productId, HttpServletRequest request) {
        try {
            shopGuard.checkProduct(productId, currentShop.shopId(request));
            productService.delete(productId);
            return Result.success(null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 批量修改：状态 / 配送方式 / 分类 / 排序 / 库存（仅非 null 字段生效） */
    @PostMapping("/batch")
    public Result<Integer> batch(@RequestBody ProductBatchDTO batch, HttpServletRequest request) {
        try {
            shopGuard.checkProducts(batch.getIds(), currentShop.shopId(request));
            return Result.success(productService.batch(batch));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 按当前筛选条件导出商品 Excel（xlsx） */
    @GetMapping("/export")
    public void export(ProductQueryDTO query, HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        try {
            Integer shopId = currentShop.shopId(request);
            if (shopId != null) {
                query.setShopId(shopId);
            }
            String fileName = URLEncoder.encode("商品列表.xlsx", "UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
            productService.export(query, response.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException("导出失败：" + e.getMessage(), e);
        }
    }

    /** Excel 模板批量导入：有商品编码按编码更新，否则新增（新增归入当前店铺） */
    @PostMapping("/import")
    public Result<ProductImportResult> importExcel(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(required = false) Integer shopId,
                                                   HttpServletRequest request) {
        try {
            return Result.success(productService.importExcel(file.getInputStream(),
                    resolveShopId(shopId, request)));
        } catch (IOException e) {
            return Result.error("读取上传文件失败：" + e.getMessage());
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 店铺 id 优先级：网关注入 > 请求参数 > 默认值 */
    private Integer resolveShopId(Integer paramShopId, HttpServletRequest request) {
        Integer injected = currentShop.shopId(request);
        if (injected != null) {
            return injected;
        }
        return paramShopId == null ? DEFAULT_SHOP_ID : paramShopId;
    }
}
