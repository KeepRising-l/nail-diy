package com.diy.nail.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 收货地址新增/编辑请求
 */
@Data
public class AddressRequest {

    /** 收货人 */
    @NotBlank(message = "收货人不能为空")
    @Size(max = 50, message = "收货人不能超过50字")
    private String receiver;

    /** 联系电话 */
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String phone;

    /** 省 */
    @NotBlank(message = "省份不能为空")
    private String province;

    /** 市 */
    @NotBlank(message = "城市不能为空")
    private String city;

    /** 区/县 */
    @NotBlank(message = "区县不能为空")
    private String district;

    /** 详细地址 */
    @NotBlank(message = "详细地址不能为空")
    @Size(max = 255, message = "详细地址不能超过255字")
    private String detail;

    /** 是否默认：1是 0否 */
    private Integer isDefault;
}
