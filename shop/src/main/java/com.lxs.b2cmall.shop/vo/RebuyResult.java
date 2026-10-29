package com.lxs.b2cmall.shop.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 复购结果：成功加购的商品件数，以及因下架/删除被跳过的商品。
 */
@Data
public class RebuyResult {

    private int addedCount;
    private int skippedCount;
    /** 被跳过的商品名（用于前端提示） */
    private List<String> skippedProducts = new ArrayList<>();
}
