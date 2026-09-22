package com.lxs.b2cmall.shop.controller;

import com.lxs.b2cmall.shop.entity.Order;
import com.lxs.b2cmall.shop.service.OrderService;
import com.lxs.b2cmall.shop.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/list")
    public Result<List<Order>> list() {
        return Result.success(orderService.listOrders());
    }

    @GetMapping("/{id}")
    public Result<Order> detail(@PathVariable Integer id) {
        Order order = orderService.getOrder(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        return Result.success(order);
    }

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