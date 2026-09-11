package com.diy.nail.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 自提核销请求（商家输入顾客出示的自提码）
 */
@Data
public class VerifyRequest {

    /** 6 位自提码 */
    @NotBlank(message = "自提码不能为空")
    private String pickupCode;
}
