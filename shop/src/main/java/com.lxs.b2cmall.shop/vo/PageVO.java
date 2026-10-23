package com.lxs.b2cmall.shop.vo;

import lombok.Data;

import java.util.List;

/**
 * 通用分页返回
 */
@Data
public class PageVO<T> {
    private long total;
    private int page;
    private int pageSize;
    private List<T> list;

    public static <T> PageVO<T> of(long total, int page, int pageSize, List<T> list) {
        PageVO<T> vo = new PageVO<>();
        vo.setTotal(total);
        vo.setPage(page);
        vo.setPageSize(pageSize);
        vo.setList(list);
        return vo;
    }
}
