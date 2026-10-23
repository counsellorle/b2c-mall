package com.lxs.b2cmall.shop.service;

import com.lxs.b2cmall.shop.dto.ProductBatchDTO;
import com.lxs.b2cmall.shop.dto.ProductDTO;
import com.lxs.b2cmall.shop.dto.ProductImportResult;
import com.lxs.b2cmall.shop.dto.ProductQueryDTO;
import com.lxs.b2cmall.shop.entity.Product;
import com.lxs.b2cmall.shop.vo.PageVO;
import com.lxs.b2cmall.shop.vo.PublishResult;

import java.io.InputStream;
import java.io.OutputStream;

public interface ProductService {
    PublishResult publish(ProductDTO dto, Integer shopId);

    /** 分页多条件查询商品列表 */
    PageVO<Product> page(ProductQueryDTO query);

    /** 编辑商品（仅更新传入的非空字段） */
    void update(Product product);

    /** 上架/下架，status 取值 on_sale / off_sale */
    void updateStatus(Integer productId, String status);

    /** 删除商品；已产生订单的商品禁止删除 */
    void delete(Integer productId);

    /** 批量修改状态/配送方式/分类/排序/库存，返回受影响行数 */
    int batch(ProductBatchDTO batch);

    /** 按筛选条件导出商品 Excel（xlsx） */
    void export(ProductQueryDTO query, OutputStream out);

    /** Excel 批量导入：有商品编码则按编码更新，否则新增 */
    ProductImportResult importExcel(InputStream in, Integer shopId);
}
