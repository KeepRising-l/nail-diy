package com.diy.nail.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端看板总览：平台用户 / 订单 / 销售额 / 待审核贴纸 / 角色分布 / 热门模板
 */
@Data
public class AdminDashboardVO {

    /** 平台用户总数（不含已删除） */
    private Long totalUsers;

    /** 顾客数 */
    private Long customerCount;

    /** 商家数 */
    private Long merchantCount;

    /** 累计有效订单数（已支付及之后，不含待支付/已取消） */
    private Long totalOrders;

    /** 平台累计销售额（元） */
    private BigDecimal totalSales;

    /** 待审核 UGC 贴纸数 */
    private Long pendingUgc;

    /** 用户角色分布（顾客/商家/管理员） */
    private List<NameCountVO> roleDist;

    /** 热门模板 TOP5（按收藏数） */
    private List<NameCountVO> hotTemplates;
}
