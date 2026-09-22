package com.lxs.b2cmall.shop.service;

import com.lxs.b2cmall.shop.dto.DashboardSummaryDTO;
import com.lxs.b2cmall.shop.dto.DashboardTrendDTO;
import com.lxs.b2cmall.shop.dto.MessageDTO;

import java.util.List;

public interface DashboardService {
    DashboardSummaryDTO getSummary();
    DashboardTrendDTO getTrend();
    List<MessageDTO> getMessages();
}