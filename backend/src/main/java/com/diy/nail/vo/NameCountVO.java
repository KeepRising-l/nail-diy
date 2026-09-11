package com.diy.nail.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用"名称+数量"统计项（设计偏好 TOP5、评价分布等复用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NameCountVO {

    /** 统计维度名称（素材名/标签等） */
    private String name;

    /** 数量 */
    private Long count;
}