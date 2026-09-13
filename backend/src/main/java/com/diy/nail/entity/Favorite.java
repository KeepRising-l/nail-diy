package com.diy.nail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏实体（表 favorite）：用户对款式模板的收藏，(user_id, template_id) 唯一
 */
@Data
@TableName("favorite")
public class Favorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 */
    private Long userId;

    /** 模板 */
    private Long templateId;

    private LocalDateTime createTime;
}
