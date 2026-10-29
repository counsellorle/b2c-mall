package com.lxs.b2cmall.shop.controller;

import com.lxs.b2cmall.shop.auth.CurrentUser;
import com.lxs.b2cmall.shop.dto.OrderCreateDTO;
import com.lxs.b2cmall.shop.entity.Order;
import com.lxs.b2cmall.shop.service.PayService;
import com.lxs.b2cmall.shop.vo.PayResult;
import com.lxs.b2cmall.shop.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/pay")
public class PayController {

    @Autowired
    private PayService payService;

    @Autowired
    private CurrentUser currentUser;

    @PostMapping("/createOrder")
    public Result<Order> createOrder(@Valid @RequestBody OrderCreateDTO dto, HttpServletRequest request) {
        try {
            // 下单人身份以登录态为准，忽略前端传入的 userId，防止替他人下单
            dto.setUserId(currentUser.requireUserId(request));
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