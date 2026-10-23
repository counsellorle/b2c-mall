package com.lxs.b2cmall.shop.service.impl;

import com.lxs.b2cmall.shop.dto.DashboardKpiDTO;
import com.lxs.b2cmall.shop.dto.DashboardSummaryDTO;
import com.lxs.b2cmall.shop.dto.DashboardTrendDTO;
import com.lxs.b2cmall.shop.dto.HourlyTrendDTO;
import com.lxs.b2cmall.shop.dto.MessageDTO;
import com.lxs.b2cmall.shop.entity.Message;
import com.lxs.b2cmall.shop.mapper.MessageMapper;
import com.lxs.b2cmall.shop.mapper.OrderMapper;
import com.lxs.b2cmall.shop.mapper.ProductMapper;
import com.lxs.b2cmall.shop.service.DashboardService;
import com.lxs.b2cmall.shop.vo.HourAmountVO;
import com.lxs.b2cmall.shop.vo.OrderTrendVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String SUMMARY_KEY = "dashboard:summary";
    private static final String TREND_KEY = "dashboard:trend";
    private static final String HOURLY_TREND_KEY = "dashboard:hourlyTrend";
    private static final String KPI_KEY = "dashboard:kpi";
    private static final long CACHE_EXPIRE_SECONDS = 60;

    /** 从消息文本中兜底提取订单编号，兼容 "订单号 xxx"、"订单编号 xxx"、"订单 xxx" 三种文案 */
    private static final Pattern ORDER_NO_PATTERN = Pattern.compile("订单(?:编号|号)?\\s*([A-Za-z0-9\\-]{4,})");

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
        summary.setStockWarnCount(productMapper.countStockWarn());
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
    public HourlyTrendDTO getHourlyTrend() {
        HourlyTrendDTO cached = (HourlyTrendDTO) redisTemplate.opsForValue().get(HOURLY_TREND_KEY);
        if (cached != null) {
            return cached;
        }

        // 查询今日 / 昨日各小时支付金额，并按小时索引
        Map<String, Double> todayMap = toHourMap(orderMapper.todayHourlyAmount());
        Map<String, Double> yesterdayMap = toHourMap(orderMapper.yesterdayHourlyAmount());

        // 固定补齐 00:00 ~ 23:00 共 24 个点，无数据的小时金额为 0
        HourlyTrendDTO dto = new HourlyTrendDTO();
        List<HourlyTrendDTO.HourItem> items = new ArrayList<>(24);
        for (int h = 0; h < 24; h++) {
            HourlyTrendDTO.HourItem item = new HourlyTrendDTO.HourItem();
            String hour = String.format("%02d", h);
            item.setHour(hour + ":00");
            item.setTodayAmount(todayMap.getOrDefault(hour, 0.0));
            item.setYesterdayAmount(yesterdayMap.getOrDefault(hour, 0.0));
            items.add(item);
        }
        dto.setItems(items);

        redisTemplate.opsForValue().set(HOURLY_TREND_KEY, dto, CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        return dto;
    }

    @Override
    public DashboardKpiDTO getKpi() {
        DashboardKpiDTO cached = (DashboardKpiDTO) redisTemplate.opsForValue().get(KPI_KEY);
        if (cached != null) {
            return cached;
        }

        long todayPayUser = orderMapper.todayPayUserCount();
        long yesterdayPayUser = orderMapper.yesterdayPayUserCount();
        long todayPayOrder = orderMapper.todayPayOrderCount();
        long yesterdayPayOrder = orderMapper.yesterdayPayOrderCount();
        double todayRefund = orderMapper.todayRefundAmount();
        double yesterdayRefund = orderMapper.yesterdayRefundAmount();

        // 客单价 = 当日总支付金额 ÷ 支付订单数（订单数为 0 时记 0，避免除零）
        double todayAmount = orderMapper.todayPaymentAmount();
        double yesterdayAmount = orderMapper.yesterdayPaymentAmount();
        double todayAvg = todayPayOrder == 0 ? 0 : round2(todayAmount / todayPayOrder);
        double yesterdayAvg = yesterdayPayOrder == 0 ? 0 : round2(yesterdayAmount / yesterdayPayOrder);

        DashboardKpiDTO kpi = new DashboardKpiDTO();
        kpi.setPayUserCount(todayPayUser);
        kpi.setYesterdayPayUserCount(yesterdayPayUser);
        kpi.setPayUserRate(rate(todayPayUser, yesterdayPayUser));

        kpi.setPayOrderCount(todayPayOrder);
        kpi.setYesterdayPayOrderCount(yesterdayPayOrder);
        kpi.setPayOrderRate(rate(todayPayOrder, yesterdayPayOrder));

        kpi.setRefundAmount(todayRefund);
        kpi.setYesterdayRefundAmount(yesterdayRefund);
        kpi.setRefundRate(rate(todayRefund, yesterdayRefund));

        kpi.setAvgOrderPrice(todayAvg);
        kpi.setYesterdayAvgOrderPrice(yesterdayAvg);
        kpi.setAvgOrderPriceRate(rate(todayAvg, yesterdayAvg));

        redisTemplate.opsForValue().set(KPI_KEY, kpi, CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        return kpi;
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
            dto.setOrderNo(resolveOrderNo(msg));
            dto.setCreatedAt(msg.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());
    }

    /** 优先取库表 order_no 字段；历史消息为空时从文案中兜底提取 */
    private String resolveOrderNo(Message msg) {
        if (msg.getOrderNo() != null && !msg.getOrderNo().trim().isEmpty()) {
            return msg.getOrderNo().trim();
        }
        if (msg.getContent() == null) {
            return null;
        }
        Matcher m = ORDER_NO_PATTERN.matcher(msg.getContent());
        return m.find() ? m.group(1) : null;
    }

    private Map<String, Double> toHourMap(List<HourAmountVO> list) {
        Map<String, Double> map = new HashMap<>();
        if (list != null) {
            for (HourAmountVO vo : list) {
                if (vo.getHour() != null) {
                    map.put(vo.getHour(), vo.getAmount());
                }
            }
        }
        return map;
    }

    /** 同比昨日变化率（百分比，保留两位）。昨日基数为 0 时：今日 > 0 记 +100，否则记 0（与需求示例一致） */
    private double rate(double today, double yesterday) {
        if (yesterday == 0) {
            return today > 0 ? 100.00 : 0.00;
        }
        return round2((today - yesterday) / yesterday * 100);
    }

    private double round2(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
