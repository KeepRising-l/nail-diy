package com.diy.nail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自提门店实体（表 store）
 */
@Data
@TableName("store")
public class Store {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 门店名称 */
    private String name;

    /** 门店地址 */
    private String address;

    /** 联系电话 */
    private String phone;

    /** 营业时间 */
    private String businessHours;

    /** 状态：1营业中 0停用 */
    private Integer status;

    /** 排序号 */
    private Integer sort;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
