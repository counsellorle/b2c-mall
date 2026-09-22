package com.lxs.b2cmall.shop.service.impl;

import com.lxs.b2cmall.shop.dto.CartDTO;
import com.lxs.b2cmall.shop.entity.Cart;
import com.lxs.b2cmall.shop.mapper.CartMapper;
import com.lxs.b2cmall.shop.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartMapper cartMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void addToCart(CartDTO dto) {
        Cart cart = new Cart();
        cart.setUserId(dto.getUserId());
        cart.setProductId(dto.getProductId());
        cart.setQuantity(dto.getQuantity());
        cart.setCreateTime(LocalDateTime.now().format(FMT));
        cartMapper.insert(cart);
    }

    @Override
    public List<Cart> listCart(Integer userId) {
        return cartMapper.findByUserId(userId);
    }

    @Override
    public void removeFromCart(Integer cartId, Integer userId) {
        cartMapper.delete(cartId, userId);
    }
}