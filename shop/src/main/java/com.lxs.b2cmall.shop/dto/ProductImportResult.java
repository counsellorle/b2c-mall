package com.lxs.b2cmall.shop.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品 Excel 导入结果汇总
 */
@Data
public class ProductImportResult {
    /** 解析到的总行数 */
    private int total;
    /** 新增条数 */
    private int insertCount;
    /** 按商品编码更新条数 */
    private int updateCount;
    /** 失败明细（行号 + 原因） */
    private List<String> failures = new ArrayList<>();
}
