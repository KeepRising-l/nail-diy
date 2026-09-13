package com.diy.nail.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 公告新增/编辑请求
 */
@Data
public class NoticeRequest {

    /** 公告标题 */
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 100, message = "公告标题不能超过100字")
    private String title;

    /** 公告内容 */
    @NotBlank(message = "公告内容不能为空")
    @Size(max = 2000, message = "公告内容不能超过2000字")
    private String content;

    /** 是否置顶：1是 0否 */
    private Integer isTop;

    /** 状态：1发布 0下线 */
    private Integer status;
}
