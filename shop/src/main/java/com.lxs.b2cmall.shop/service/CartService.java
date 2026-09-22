package com.lxs.b2cmall.shop.service;

import com.lxs.b2cmall.shop.dto.CartDTO;
import com.lxs.b2cmall.shop.entity.Cart;

import java.util.List;

public interface CartService {
    void addToCart(CartDTO dto);
    List<Cart> listCart(Integer userId);
    void removeFromCart(Integer cartId, Integer userId);
}