package com.diy.nail.vo;

import lombok.Data;

import java.util.List;

/**
 * 设计偏好统计（ANA-REQ-3）：甲型/色卡/图案 TOP5（按购买数量加权）
 */
@Data
public class DesignPrefsVO {

    /** 甲型使用 TOP5 */
    private List<NameCountVO> topShapes;

    /** 色卡使用 TOP5 */
    private List<NameCountVO> topColors;

    /** 图案使用 TOP5 */
    private List<NameCountVO> topPatterns;
}
