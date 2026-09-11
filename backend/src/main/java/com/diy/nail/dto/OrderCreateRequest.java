package com.diy.nail.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建订单请求（整购物车结算）
 */
@Data
public class OrderCreateRequest {

    /** 履约方式：SELF_PICKUP自提 / EXPRESS邮寄 */
    @NotBlank(message = "履约方式不能为空")
    @Pattern(regexp = "SELF_PICKUP|EXPRESS", message = "履约方式不合法")
    private String deliveryType;

    /** 自提门店（自提时必填） */
    private Long storeId;

    /** 收货地址（邮寄时必填） */
    private Long addressId;

    /** 顾客备注 */
    @Size(max = 255, message = "备注不能超过255字")
    private String remark;
}
