package com.lxs.b2cmall.shop.service;

import com.lxs.b2cmall.shop.dto.BatchShipDTO;
import com.lxs.b2cmall.shop.dto.OrderQueryDTO;
import com.lxs.b2cmall.shop.entity.Order;
import com.lxs.b2cmall.shop.vo.BatchShipResult;
import com.lxs.b2cmall.shop.vo.OrderDetailVO;
import com.lxs.b2cmall.shop.vo.OrderGroupVO;
import com.lxs.b2cmall.shop.vo.PageVO;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface OrderService {

    List<Order> listOrders();

    Order getOrder(Integer id);

    Order ship(Integer orderId, String operator);

    // ==================== P3 店铺订单查询 ====================

    /** 多条件分页查询，返回订单组（订单头 + 商品行） */
    PageVO<OrderGroupVO> page(OrderQueryDTO query);

    /** 状态 Tab 计数：key 为状态名，「全部」在首位 */
    Map<String, Long> statusCount(Integer shopId);

    /** 批量发货：逐单过状态机，失败的记录原因而不中断整批 */
    BatchShipResult batchShip(BatchShipDTO dto);

    /** 按当前筛选条件导出 Excel */
    void export(OrderQueryDTO query, OutputStream out);

    /** 订单详情：订单组 + 状态流转日志 */
    OrderDetailVO detail(Integer id);
}
