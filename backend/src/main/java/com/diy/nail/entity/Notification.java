package com.diy.nail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息通知实体（表 notification）。
 *
 * <p>在关键业务节点写入：订单状态变更、售后审核结果等。
 * 不做 WebSocket 实时推送，采用「登录后拉取 + 未读红点」的轻量方案 ——
 * 实现成本低、对现有架构零侵入，也符合毕设的规模。
 */
@Data
@TableName("notification")
public class Notification {

    /** 类型：订单相关 */
    public static final String TYPE_ORDER = "ORDER";
    /** 类型：售后相关 */
    public static final String TYPE_AFTER_SALE = "AFTER_SALE";
    /** 类型：系统消息 */
    public static final String TYPE_SYSTEM = "SYSTEM";

    /** 关联业务类型：订单 */
    public static final String BIZ_ORDER = "ORDER";
    /** 关联业务类型：售后单 */
    public static final String BIZ_AFTER_SALE = "AFTER_SALE";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收者 ID */
    private Long userId;

    /** 类型：ORDER / AFTER_SALE / SYSTEM */
    private String type;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 关联业务 ID（订单 ID / 售后单 ID） */
    private Long bizId;

    /** 关联业务类型：ORDER / AFTER_SALE */
    private String bizType;

    /** 是否已读：0未读 1已读 */
    private Integer isRead;

    /** 读取时间 */
    private LocalDateTime readTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除：0正常 1删除 */
    @TableLogic
    private Integer deleted;
}
