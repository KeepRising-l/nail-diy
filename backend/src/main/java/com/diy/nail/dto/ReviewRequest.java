package com.diy.nail.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 提交评价请求（仅已完成订单，每单一次）
 */
@Data
public class ReviewRequest {

    /** 被评价订单 */
    @NotNull(message = "订单不能为空")
    private Long orderId;

    /** 评分 1-5 */
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分范围为 1-5 星")
    @Max(value = 5, message = "评分范围为 1-5 星")
    private Integer rating;

    /** 评价内容（可空，≤500 字） */
    @Size(max = 500, message = "评价内容不能超过 500 字")
    private String content;

    /** 晒图 URL 列表（最多 3 张） */
    @Size(max = 3, message = "晒图最多 3 张")
    private List<String> images;
}
