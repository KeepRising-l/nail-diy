package com.diy.nail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设计素材实体（表 material）
 * 类型：SHAPE甲型 / COLOR色卡 / PATTERN图案（type 字段是品类扩展点）
 */
@Data
@TableName("material")
public class Material {

    /** 素材类型常量 */
    public static final String TYPE_SHAPE = "SHAPE";
    public static final String TYPE_COLOR = "COLOR";
    public static final String TYPE_PATTERN = "PATTERN";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 素材类型 */
    private String type;

    /** 素材名称 */
    private String name;

    /** 图片路径（甲型/图案需要，色卡为空） */
    private String imageUrl;

    /** 颜色代码（色卡需要，如 #FFB6C1） */
    private String colorCode;

    /** 图案分类（BASIC/FRENCH/GRADIENT/NATURE/GEO/SHINE，甲型/色卡可空） */
    private String category;

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
