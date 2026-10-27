package com.lxs.b2cmall.shop.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量发货结果：成功的计数，失败的给出逐单原因（不因单笔失败中断整批）
 */
@Data
public class BatchShipResult {
    private int total;
    private int successCount;
    private List<String> failures = new ArrayList<>();
}
