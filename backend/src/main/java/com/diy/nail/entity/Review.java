package com.diy.nail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价实体（表 review，与订单一一对应，每单限评一次）
 */
@Data
@TableName("review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单（唯一） */
    private Long orderId;

    /** 评价用户 */
    private Long userId;

    /** 评分 1-5 */
    private Integer rating;

    /** 评价内容 */
    private String content;

    /** 晒图 JSON 数组字符串（最多 3 张 URL） */
    private String images;

    /** 商家回复 */
    private String reply;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除：0正常 1删除（商家删除违规评价） */
    @TableLogic
    private Integer deleted;
}
