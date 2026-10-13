package com.diy.nail.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 顾客提交售后申请。
 *
 * <p>退款金额不由前端传入 —— 由后端按订单实付金额计算，避免客户端伪造金额。
 */
@Data
public class AfterSaleRequest {

    /** 订单 ID */
    @NotNull(message = "订单不能为空")
    private Long orderId;

    /** 类型：REFUND_ONLY 仅退款 / RETURN_REFUND 退货退款 */
    @NotBlank(message = "请选择售后类型")
    private String type;

    /** 申请原因分类 */
    @NotBlank(message = "请选择申请原因")
    private String reason;

    /** 补充说明 */
    private String description;

    /** 凭证图（最多 3 张 URL） */
    private List<String> images;
}
