package com.diy.nail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户 DIY 设计实体（表 design）
 * 本 change 仅用于统计素材被引用次数，完整设计功能在 change-05 实现
 */
@Data
@TableName("design")
public class Design {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 */
    private Long userId;

    /** 设计名称 */
    private String name;

    /** 设计参数 JSON（甲型id/色卡id/图案id集合/备注） */
    private String params;

    /** 预览快照图 */
    private String previewUrl;

    /** 单价 */
    private BigDecimal price;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除：0正常 1删除 */
    @TableLogic
    private Integer deleted;
}
