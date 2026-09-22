package com.lxs.b2cmall.shop.controller;

import com.lxs.b2cmall.shop.dto.DashboardSummaryDTO;
import com.lxs.b2cmall.shop.dto.DashboardTrendDTO;
import com.lxs.b2cmall.shop.dto.MessageDTO;
import com.lxs.b2cmall.shop.service.DashboardService;
import com.lxs.b2cmall.shop.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/summary")
    public Result<DashboardSummaryDTO> getSummary() {
        return Result.success(dashboardService.getSummary());
    }

    @GetMapping("/trend")
    public Result<DashboardTrendDTO> getTrend() {
        return Result.success(dashboardService.getTrend());
    }

    @GetMapping("/messages")
    public Result<List<MessageDTO>> getMessages() {
        return Result.success(dashboardService.getMessages());
    }
}