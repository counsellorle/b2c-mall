package com.lxs.b2cmall.shop.controller;

import com.lxs.b2cmall.shop.dto.BatchShipDTO;
import com.lxs.b2cmall.shop.dto.OrderQueryDTO;
import com.lxs.b2cmall.shop.entity.Order;
import com.lxs.b2cmall.shop.service.OrderService;
import com.lxs.b2cmall.shop.vo.BatchShipResult;
import com.lxs.b2cmall.shop.vo.OrderDetailVO;
import com.lxs.b2cmall.shop.vo.OrderGroupVO;
import com.lxs.b2cmall.shop.vo.PageVO;
import com.lxs.b2cmall.shop.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/list")
    public Result<List<Order>> list() {
        return Result.success(orderService.listOrders());
    }

    // ==================== P3 店铺订单查询 ====================

    /** 多条件分页查询（11 个筛选项），返回订单组（订单头 + 商品行） */
    @GetMapping("/page")
    public Result<PageVO<OrderGroupVO>> page(OrderQueryDTO query) {
        try {
            return Result.success(orderService.page(query));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 状态 Tab 计数：「全部 / 待付款 / 待发货 / 待收货 / 交易成功 / 交易失败 / 待退款」 */
    @GetMapping("/statusCount")
    public Result<Map<String, Long>> statusCount(@RequestParam(required = false) Integer shopId) {
        try {
            return Result.success(orderService.statusCount(shopId));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 批量发货：逐单校验状态，返回成功数与失败明细 */
    @PostMapping("/batchShip")
    public Result<BatchShipResult> batchShip(@RequestBody BatchShipDTO dto) {
        try {
            return Result.success(orderService.batchShip(dto));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 按当前筛选条件导出订单 Excel（xlsx） */
    @GetMapping("/export")
    public void export(OrderQueryDTO query, HttpServletResponse response) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        try {
            String fileName = URLEncoder.encode("订单列表.xlsx", "UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
            orderService.export(query, response.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException("导出失败：" + e.getMessage(), e);
        }
    }

    /** 订单详情：订单组（含商品行/物流/支付）+ 状态流转日志 */
    @GetMapping("/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Integer id) {
        try {
            return Result.success(orderService.detail(id));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 单笔发货（保留原有接口） */
    @PostMapping("/ship")
    public Result<Order> ship(@RequestParam Integer orderId,
                              @RequestParam(defaultValue = "admin") String operator) {
        try {
            Order order = orderService.ship(orderId, operator);
            return Result.success(order);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
