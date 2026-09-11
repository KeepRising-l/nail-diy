package com.diy.nail.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 商家发货请求（邮寄订单填写物流信息）
 */
@Data
public class ShipRequest {

    /** 快递公司 */
    @NotBlank(message = "快递公司不能为空")
    @Size(max = 50, message = "快递公司不能超过50字")
    private String expressCompany;

    /** 物流单号 */
    @NotBlank(message = "物流单号不能为空")
    @Size(max = 100, message = "物流单号不能超过100字")
    private String expressNo;
}
