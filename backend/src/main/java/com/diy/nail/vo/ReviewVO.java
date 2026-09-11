package com.diy.nail.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价视图：评价内容 + 脱敏昵称 + 关联订单号与商品名快照
 */
@Data
public class ReviewVO {

    /** 评价 id */
    private Long id;

    /** 订单 id */
    private Long orderId;

    /** 订单号 */
    private String orderNo;

    /** 商品名快照（订单明细拼接） */
    private String designNames;

    /** 评分 1-5 */
    private Integer rating;

    /** 评价内容 */
    private String content;

    /** 晒图 URL 列表 */
    private List<String> images;

    /** 评价用户昵称（脱敏：首字符+****） */
    private String nickname;

    /** 商家回复 */
    private String reply;

    /** 评价时间 */
    private LocalDateTime createTime;
}
