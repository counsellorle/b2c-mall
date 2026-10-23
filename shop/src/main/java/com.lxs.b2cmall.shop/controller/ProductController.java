package com.lxs.b2cmall.shop.controller;

import com.lxs.b2cmall.shop.dto.ProductBatchDTO;
import com.lxs.b2cmall.shop.dto.ProductDTO;
import com.lxs.b2cmall.shop.dto.ProductImportResult;
import com.lxs.b2cmall.shop.dto.ProductQueryDTO;
import com.lxs.b2cmall.shop.entity.Product;
import com.lxs.b2cmall.shop.service.ProductService;
import com.lxs.b2cmall.shop.vo.PageVO;
import com.lxs.b2cmall.shop.vo.PublishResult;
import com.lxs.b2cmall.shop.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    /** 演示用：登录后应从会话取当前商家店铺 id，此处提供默认值 */
    private static final Integer DEFAULT_SHOP_ID = 1;

    @PostMapping("/publish")
    public Result<PublishResult> publish(@Valid @RequestBody ProductDTO dto,
                                         @RequestParam Integer shopId) {
        try {
            PublishResult result = productService.publish(dto, shopId);
            if (result.isSuccess()) {
                return Result.success(result);
            } else {
                return Result.error(result.getMessage());
            }
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 分页多条件查询：名称模糊 / 商品类型 / 商品类别 / 状态 / 配送方式多选 / 分类 */
    @GetMapping("/page")
    public Result<PageVO<Product>> page(ProductQueryDTO query) {
        try {
            return Result.success(productService.page(query));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 编辑商品（仅更新传入的非空字段） */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody Product product) {
        try {
            productService.update(product);
            return Result.success(null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 单商品上架/下架：status = on_sale / off_sale */
    @PostMapping("/status")
    public Result<Void> updateStatus(@RequestParam Integer productId, @RequestParam String status) {
        try {
            productService.updateStatus(productId, status);
            return Result.success(null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 删除商品：已产生订单的商品禁止删除 */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestParam Integer productId) {
        try {
            productService.delete(productId);
            return Result.success(null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 批量修改：状态 / 配送方式 / 分类 / 排序 / 库存（仅非 null 字段生效） */
    @PostMapping("/batch")
    public Result<Integer> batch(@RequestBody ProductBatchDTO batch) {
        try {
            return Result.success(productService.batch(batch));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 按当前筛选条件导出商品 Excel（xlsx） */
    @GetMapping("/export")
    public void export(ProductQueryDTO query, HttpServletResponse response) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        try {
            String fileName = URLEncoder.encode("商品列表.xlsx", "UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
            productService.export(query, response.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException("导出失败：" + e.getMessage(), e);
        }
    }

    /** Excel 模板批量导入：有商品编码按编码更新，否则新增 */
    @PostMapping("/import")
    public Result<ProductImportResult> importExcel(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(required = false) Integer shopId) {
        try {
            return Result.success(productService.importExcel(file.getInputStream(),
                    shopId == null ? DEFAULT_SHOP_ID : shopId));
        } catch (IOException e) {
            return Result.error("读取上传文件失败：" + e.getMessage());
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
