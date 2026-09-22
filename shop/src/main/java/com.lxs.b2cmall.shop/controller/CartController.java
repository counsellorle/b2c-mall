package com.lxs.b2cmall.shop.controller;

import com.lxs.b2cmall.shop.dto.CartDTO;
import com.lxs.b2cmall.shop.entity.Cart;
import com.lxs.b2cmall.shop.service.CartService;
import com.lxs.b2cmall.shop.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @PostMapping("/add")
    public Result<String> add(@Valid @RequestBody CartDTO dto) {
        cartService.addToCart(dto);
        return Result.success("加入购物车成功");
    }

    @GetMapping("/list")
    public Result<List<Cart>> list(@RequestParam Integer userId) {
        return Result.success(cartService.listCart(userId));
    }

    @DeleteMapping("/remove")
    public Result<String> remove(@RequestParam Integer cartId, @RequestParam Integer userId) {
        cartService.removeFromCart(cartId, userId);
        return Result.success("删除成功");
    }
}