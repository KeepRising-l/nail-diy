package com.diy.nail.service;

import com.diy.nail.mapper.AdminMapper;
import com.diy.nail.mapper.AnalyticsMapper;
import com.diy.nail.vo.AdminDashboardVO;
import com.diy.nail.vo.NameCountVO;
import com.diy.nail.vo.TrendPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 管理端看板业务：平台总览（用户/订单/销售额/待审核贴纸/角色分布/热门模板）
 * 订单与销售额口径与商家统计一致（复用 AnalyticsMapper）；趋势直接委托 AnalyticsService。
 */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final AdminMapper adminMapper;
    private final AnalyticsMapper analyticsMapper;
    private final AnalyticsService analyticsService;

    /** 平台总览 */
    public AdminDashboardVO overview() {
        AdminDashboardVO vo = new AdminDashboardVO();
        long customer = 0;
        long merchant = 0;
        long totalUsers = 0;
        List<NameCountVO> roleDist = new ArrayList<>();
        for (Map<String, Object> row : adminMapper.roleDist()) {
            long count = ((Number) row.get("c")).longValue();
            String role = String.valueOf(row.get("k"));
            totalUsers += count;
            if ("USER".equals(role)) {
                customer = count;
            } else if ("MERCHANT".equals(role)) {
                merchant = count;
            }
            roleDist.add(new NameCountVO(role, count));
        }
        vo.setTotalUsers(totalUsers);
        vo.setCustomerCount(customer);
        vo.setMerchantCount(merchant);
        vo.setRoleDist(roleDist);
        vo.setTotalOrders(analyticsMapper.countValidOrders());
        vo.setTotalSales(analyticsMapper.sumSales());
        vo.setPendingUgc(adminMapper.countPendingUgc());
        vo.setHotTemplates(new ArrayList<>(
                adminMapper.hotTemplates().stream()
                        .map(row -> new NameCountVO(String.valueOf(row.get("k")), ((Number) row.get("c")).longValue()))
                        .toList()));
        return vo;
    }

    /** 平台销售趋势（days=7/30，0=全部），口径同商家端 */
    public List<TrendPointVO> trend(int days) {
        return analyticsService.trend(days);
    }
}
