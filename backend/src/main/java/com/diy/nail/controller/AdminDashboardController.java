package com.diy.nail.controller;

import com.diy.nail.common.Result;
import com.diy.nail.service.AdminDashboardService;
import com.diy.nail.vo.AdminDashboardVO;
import com.diy.nail.vo.TrendPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端接口：平台数据看板（/api/admin/** 仅 ADMIN 可访问）
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /** 平台总览：用户/订单/销售额/待审核贴纸/角色分布/热门模板 */
    @GetMapping("/overview")
    public Result<AdminDashboardVO> overview() {
        return Result.ok(adminDashboardService.overview());
    }

    /** 平台销售趋势：days=7/30，0=全部 */
    @GetMapping("/trend")
    public Result<List<TrendPointVO>> trend(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(adminDashboardService.trend(days));
    }
}
