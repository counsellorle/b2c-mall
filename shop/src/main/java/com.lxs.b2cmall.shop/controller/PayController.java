package com.lxs.b2cmall.shop.controller;

import com.lxs.b2cmall.shop.dto.OrderCreateDTO;
import com.lxs.b2cmall.shop.entity.Order;
import com.lxs.b2cmall.shop.service.PayService;
import com.lxs.b2cmall.shop.vo.PayResult;
import com.lxs.b2cmall.shop.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/pay")
public class PayController {

    @Autowired
    private PayService payService;

    @PostMapping("/createOrder")
    public Result<Order> createOrder(@Valid @RequestBody OrderCreateDTO dto) {
        try {
            Order order = payService.createOrder(dto);
            return Result.success(order);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/pay")
    public Result<PayResult> pay(@RequestParam Integer orderId) {
        try {
            PayResult result = payService.pay(orderId);
            if (result.isSuccess()) {
                return Result.success(result);
            } else {
                return Result.error(result.getMessage());
            }
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}