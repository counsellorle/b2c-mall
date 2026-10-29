package com.lxs.b2cmall.shop.dto;

import lombok.Data;

/**
 * C 端订单列表查询：只按状态筛选（订单号查询走 keyword，命中订单号模糊匹配）。
 */
@Data
public class FrontOrderQueryDTO {

    /** 状态 Tab；为空表示「全部」 */
    private String status;

    /** 订单号模糊查询 */
    private String keyword;

    private Integer page;
    private Integer pageSize;

    public int getPage() {
        return page == null || page < 1 ? 1 : page;
    }

    public int getLimit() {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }

    public int getOffset() {
        return (getPage() - 1) * getLimit();
    }
}
