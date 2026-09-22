package com.lxs.b2cmall.shop.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
public class ProductDTO {
    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String keyword;
    private String sellingPoint;

    @NotNull(message = "类目不能为空")
    private Integer categoryId;

    private Integer shopCategoryId;

    @NotBlank(message = "商品类型不能为空")
    private String productType;  // physical / virtual / combo / card

    private String mainImage;
    private String video;
    private String brand;

    @NotNull(message = "价格不能为空")
    @Positive(message = "价格必须大于0")
    private Double price;

    private Double marketPrice;

    @NotNull(message = "库存不能为空")
    private Integer stock;

    private Integer stockWarn = 10;
    private String productCode;
    private Integer sort;

    private String complianceInfo;  // 虚拟商品合规信息
}