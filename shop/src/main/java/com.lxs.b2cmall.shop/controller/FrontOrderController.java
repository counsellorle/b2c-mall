package com.lxs.b2cmall.shop.controller;

import com.lxs.b2cmall.shop.auth.CurrentUser;
import com.lxs.b2cmall.shop.dto.FrontOrderQueryDTO;
import com.lxs.b2cmall.shop.service.FrontOrderService;
import com.lxs.b2cmall.shop.vo.FrontOrderVO;
import com.lxs.b2cmall.shop.vo.PageVO;
import com.lxs.b2cmall.shop.vo.RebuyResult;
import com.lxs.b2cmall.shop.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * C 端订单管理：所有接口都需要登录态，userId 从 token 解析，不接受前端传入，
 * 避免越权查看/操作他人订单。
 */
@RestController
@RequestMapping("/front/order")
public class FrontOrderController {

    @Autowired
    private FrontOrderService frontOrderService;

    @Autowired
    private CurrentUser currentUser;

    @GetMapping("/page")
    public Result<PageVO<FrontOrderVO>> page(FrontOrderQueryDTO query, HttpServletRequest request) {
        try {
            Integer userId = currentUser.requireUserId(request);
            return Result.success(frontOrderService.page(userId, query));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/statusCount")
    public Result<Map<String, Long>> statusCount(HttpServletRequest request) {
        try {
            Integer userId = currentUser.requireUserId(request);
            return Result.success(frontOrderService.statusCount(userId));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<FrontOrderVO> detail(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer userId = currentUser.requireUserId(request);
            return Result.success(frontOrderService.detail(userId, id));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer userId = currentUser.requireUserId(request);
            frontOrderService.delete(userId, id);
            return Result.success("删除成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/rebuy")
    public Result<RebuyResult> rebuy(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer userId = currentUser.requireUserId(request);
            return Result.success(frontOrderService.rebuy(userId, id));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
