package com.diy.nail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设计素材实体（表 material）
 * 类型：SHAPE甲型 / COLOR色卡 / PATTERN_IMG贴图 / PATTERN_STYLE参数化款式 / DECOR装饰件 / STICKER贴纸
 * SHAPE/PATTERN_STYLE/矢量DECOR 用 code 绑定前端渲染器，无需图片；其余为图片或纯颜色素材
 */
@Data
@TableName("material")
public class Material {

    /** 素材类型常量 */
    public static final String TYPE_SHAPE = "SHAPE";
    public static final String TYPE_COLOR = "COLOR";
    public static final String TYPE_PATTERN_IMG = "PATTERN_IMG";
    public static final String TYPE_PATTERN_STYLE = "PATTERN_STYLE";
    public static final String TYPE_DECOR = "DECOR";
    public static final String TYPE_STICKER = "STICKER";

    /** 来源常量 */
    public static final String SOURCE_PLATFORM = "PLATFORM";
    public static final String SOURCE_UGC = "UGC";
    public static final String SOURCE_AI = "AI";

    /** 审核状态常量 */
    public static final String AUDIT_PENDING = "PENDING";
    public static final String AUDIT_APPROVED = "APPROVED";
    public static final String AUDIT_REJECTED = "REJECTED";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 素材类型 */
    private String type;

    /** 代码标识（SHAPE/PATTERN_STYLE/矢量DECOR 绑定前端渲染器，同类型内唯一） */
    private String code;

    /** 素材名称 */
    private String name;

    /** 图片路径（贴图/贴纸/立体装饰需要） */
    private String imageUrl;

    /** 颜色代码（色卡需要，如 #FFB6C1） */
    private String colorCode;

    /** 分类（贴图：BASIC/NATURE/GEO/CUTE/SHINE；色卡：色系；甲型：长度系列） */
    private String category;

    /** 检索标签，逗号分隔 */
    private String tags;

    /** 类型专属属性 JSON 字符串（甲型{series,length}；色卡{finish}；款式{paramsSchema,defaults}；装饰{material}） */
    private String attrs;

    /** 来源：PLATFORM平台 / UGC用户上传 / AI生成 */
    private String source;

    /** UGC 来源用户（平台素材为 NULL） */
    private Long creatorId;

    /** 审核状态：PENDING待审 / APPROVED通过 / REJECTED驳回 */
    private String auditStatus;

    /** 热度（被设计引用次数） */
    private Integer heat;

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
