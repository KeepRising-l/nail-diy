package com.diy.nail.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商家处理售后申请（同意退款 / 拒绝）。
 */
@Data
public class AfterSaleHandleRequest {

    /** true 同意退款，false 拒绝 */
    @NotNull(message = "请指定处理结果")
    private Boolean approved;

    /** 拒绝原因（拒绝时必填，由 Service 校验） */
    private String rejectReason;
}
