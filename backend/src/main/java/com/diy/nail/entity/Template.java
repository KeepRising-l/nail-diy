package com.diy.nail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 款式模板实体（表 template）
 * 商家预置的成品设计样例，顾客可浏览并一键套用（设计参数与用户 DIY 设计同一结构）
 */
@Data
@TableName("template")
public class Template {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模板名称 */
    private String name;

    /** 风格标签：可爱/轻奢/法式等 */
    private String styleTag;

    /** 封面图路径 */
    private String coverUrl;

    /** 设计参数 JSON（默认甲型/色卡/图案组合，与用户设计共用结构） */
    private String designParams;

    /** 参考价格 */
    private BigDecimal price;

    /** 排序号（越小越靠前） */
    private Integer sort;

    /** 状态：1上架 0下架 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除：0正常 1删除 */
    @TableLogic
    private Integer deleted;
}
