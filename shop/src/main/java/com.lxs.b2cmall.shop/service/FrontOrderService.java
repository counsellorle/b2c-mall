package com.lxs.b2cmall.shop.service;

import com.lxs.b2cmall.shop.dto.FrontOrderQueryDTO;
import com.lxs.b2cmall.shop.vo.FrontOrderVO;
import com.lxs.b2cmall.shop.vo.PageVO;
import com.lxs.b2cmall.shop.vo.RebuyResult;

import java.util.Map;

/**
 * C 端（买家）订单管理：所有方法第一个参数为当前登录用户 id，由 token 解析得到，
 * 服务层再次以 userId 作为查询条件，确保用户只能操作自己的订单。
 */
public interface FrontOrderService {

    PageVO<FrontOrderVO> page(Integer userId, FrontOrderQueryDTO query);

    /** 状态 Tab 计数，固定顺序且缺失状态补 0 */
    Map<String, Long> statusCount(Integer userId);

    FrontOrderVO detail(Integer userId, Integer orderId);

    /** 逻辑删除：只隐藏用户侧记录，商家端仍可见 */
    void delete(Integer userId, Integer orderId);

    /** 复购：把订单内仍在售的商品重新加入购物车 */
    RebuyResult rebuy(Integer userId, Integer orderId);
}
