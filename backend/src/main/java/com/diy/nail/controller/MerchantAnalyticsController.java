package com.diy.nail.controller;

import com.diy.nail.common.Result;
import com.diy.nail.service.AnalyticsService;
import com.diy.nail.vo.AnalyticsOverviewVO;
import com.diy.nail.vo.DesignPrefsVO;
import com.diy.nail.vo.TrendPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商家端接口：统计看板（仅 MERCHANT/ADMIN 可访问）
 */
@RestController
@RequestMapping("/api/merchant/analytics")
@RequiredArgsConstructor
public class MerchantAnalyticsController {

    private final AnalyticsService analyticsService;

    /** 总览：卡片 + 履约占比 + 评价分布 */
    @GetMapping("/overview")
    public Result<AnalyticsOverviewVO> overview() {
        return Result.ok(analyticsService.overview());
    }

    /** 销售趋势：days=7/30，0=全部 */
    @GetMapping("/trend")
    public Result<List<TrendPointVO>> trend(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(analyticsService.trend(days));
    }

    /** 设计偏好 TOP5（甲型/色卡/图案） */
    @GetMapping("/design-prefs")
    public Result<DesignPrefsVO> designPrefs() {
        return Result.ok(analyticsService.designPrefs());
    }
}
