package com.lxs.b2cmall.shop.controller;

import com.lxs.b2cmall.shop.dto.DashboardKpiDTO;
import com.lxs.b2cmall.shop.dto.DashboardSummaryDTO;
import com.lxs.b2cmall.shop.dto.DashboardTrendDTO;
import com.lxs.b2cmall.shop.dto.HourlyTrendDTO;
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

    /** 24 小时今日/昨日支付金额双曲线（X 轴 00:00~23:00） */
    @GetMapping("/hourlyTrend")
    public Result<HourlyTrendDTO> getHourlyTrend() {
        return Result.success(dashboardService.getHourlyTrend());
    }

    /** 4 项核心经营 KPI：支付客户数/支付订单数/退款金额/客单价，含同比昨日变化率 */
    @GetMapping("/kpi")
    public Result<DashboardKpiDTO> getKpi() {
        return Result.success(dashboardService.getKpi());
    }

    @GetMapping("/messages")
    public Result<List<MessageDTO>> getMessages() {
        return Result.success(dashboardService.getMessages());
    }
}