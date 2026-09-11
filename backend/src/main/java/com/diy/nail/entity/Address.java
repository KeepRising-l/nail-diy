package com.diy.nail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收货地址实体（表 address，每人最多 10 条）
 */
@Data
@TableName("address")
public class Address {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 */
    private Long userId;

    /** 收货人 */
    private String receiver;

    /** 联系电话 */
    private String phone;

    /** 省 */
    private String province;

    /** 市 */
    private String city;

    /** 区/县 */
    private String district;

    /** 详细地址 */
    private String detail;

    /** 是否默认：1是 0否 */
    private Integer isDefault;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除：0正常 1删除 */
    @TableLogic
    private Integer deleted;
}
