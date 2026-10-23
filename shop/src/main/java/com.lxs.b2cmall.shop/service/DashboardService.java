package com.lxs.b2cmall.shop.service;

import com.lxs.b2cmall.shop.dto.DashboardKpiDTO;
import com.lxs.b2cmall.shop.dto.DashboardSummaryDTO;
import com.lxs.b2cmall.shop.dto.DashboardTrendDTO;
import com.lxs.b2cmall.shop.dto.HourlyTrendDTO;
import com.lxs.b2cmall.shop.dto.MessageDTO;

import java.util.List;

public interface DashboardService {
    DashboardSummaryDTO getSummary();
    DashboardTrendDTO getTrend();
    /** 24 小时今日/昨日支付金额双曲线 */
    HourlyTrendDTO getHourlyTrend();
    /** 4 项核心经营 KPI（含同比昨日变化率） */
    DashboardKpiDTO getKpi();
    List<MessageDTO> getMessages();
}