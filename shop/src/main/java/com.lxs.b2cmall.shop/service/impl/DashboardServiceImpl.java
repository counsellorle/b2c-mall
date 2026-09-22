package com.lxs.b2cmall.shop.service.impl;

import com.lxs.b2cmall.shop.dto.DashboardSummaryDTO;
import com.lxs.b2cmall.shop.dto.DashboardTrendDTO;
import com.lxs.b2cmall.shop.dto.MessageDTO;
import com.lxs.b2cmall.shop.entity.Message;
import com.lxs.b2cmall.shop.mapper.MessageMapper;
import com.lxs.b2cmall.shop.mapper.OrderMapper;
import com.lxs.b2cmall.shop.service.DashboardService;
import com.lxs.b2cmall.shop.vo.OrderTrendVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String SUMMARY_KEY = "dashboard:summary";
    private static final String TREND_KEY = "dashboard:trend";
    private static final long CACHE_EXPIRE_SECONDS = 60;

    @Override
    public DashboardSummaryDTO getSummary() {
        DashboardSummaryDTO cached = (DashboardSummaryDTO) redisTemplate.opsForValue().get(SUMMARY_KEY);
        if (cached != null) {
            return cached;
        }

        DashboardSummaryDTO summary = new DashboardSummaryDTO();
        summary.setPendingPayment(orderMapper.countByStatus("待付款"));
        summary.setPendingShipment(orderMapper.countByStatus("待发货"));
        summary.setShipped(orderMapper.countByStatus("待收货"));
        summary.setCompleted(orderMapper.countByStatus("交易成功"));
        summary.setFailed(orderMapper.countByStatus("交易失败"));
        summary.setPendingRefund(orderMapper.countByStatus("待退款"));
        summary.setTodayPaymentAmount(orderMapper.todayPaymentAmount());
        summary.setMonthlyPaymentAmount(orderMapper.monthlyPaymentAmount());
        summary.setTotalOrders(orderMapper.totalOrders());

        redisTemplate.opsForValue().set(SUMMARY_KEY, summary, CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        return summary;
    }

    @Override
    public DashboardTrendDTO getTrend() {
        DashboardTrendDTO cached = (DashboardTrendDTO) redisTemplate.opsForValue().get(TREND_KEY);
        if (cached != null) {
            return cached;
        }

        List<OrderTrendVO> trendData = orderMapper.trend();
        DashboardTrendDTO trendDTO = new DashboardTrendDTO();
        List<DashboardTrendDTO.TrendItem> items = trendData.stream().map(vo -> {
            DashboardTrendDTO.TrendItem item = new DashboardTrendDTO.TrendItem();
            item.setDate(vo.getDate());
            item.setOrderCount(vo.getOrderCount());
            item.setAmount(vo.getAmount());
            return item;
        }).collect(Collectors.toList());
        trendDTO.setItems(items);

        redisTemplate.opsForValue().set(TREND_KEY, trendDTO, CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        return trendDTO;
    }

    @Override
    public List<MessageDTO> getMessages() {
        List<Message> messages = messageMapper.findAll();
        return messages.stream().map(msg -> {
            MessageDTO dto = new MessageDTO();
            dto.setId(msg.getId());
            dto.setTitle(msg.getTitle());
            dto.setContent(msg.getContent());
            dto.setMsgType(msg.getMsgType());
            dto.setIsRead(msg.getIsRead());
            dto.setCreatedAt(msg.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());
    }
}