package com.lxs.b2cmall.shop.controller;

import com.lxs.b2cmall.shop.auth.CurrentUser;
import com.lxs.b2cmall.shop.dto.CartDTO;
import com.lxs.b2cmall.shop.entity.Cart;
import com.lxs.b2cmall.shop.service.CartService;
import com.lxs.b2cmall.shop.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 购物车。userId 一律由登录态解析（网关校验 token 后注入 X-User-Id），不再接受前端传入，避免越权操作他人购物车。
 */
@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private CurrentUser currentUser;

    @PostMapping("/add")
    public Result<String> add(@Valid @RequestBody CartDTO dto, HttpServletRequest request) {
        try {
            dto.setUserId(currentUser.requireUserId(request));
            cartService.addToCart(dto);
            return Result.success("加入购物车成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<List<Cart>> list(HttpServletRequest request) {
        try {
            return Result.success(cartService.listCart(currentUser.requireUserId(request)));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/remove")
    public Result<String> remove(@RequestParam Integer cartId, HttpServletRequest request) {
        try {
            cartService.removeFromCart(cartId, currentUser.requireUserId(request));
            return Result.success("删除成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
