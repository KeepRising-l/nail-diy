package com.diy.nail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体（表 user）
 * 角色：USER顾客 / MERCHANT商家 / ADMIN管理员
 */
@Data
@TableName("user")
public class User {

    /** 角色常量 */
    public static final String ROLE_USER = "USER";
    public static final String ROLE_MERCHANT = "MERCHANT";
    public static final String ROLE_ADMIN = "ADMIN";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 手机号（登录账号，唯一） */
    private String phone;

    /** 密码（BCrypt 密文） */
    private String password;

    private String nickname;

    private String avatar;

    /** 角色 */
    private String role;

    /** 状态：1正常 0禁用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除：0正常 1删除 */
    @TableLogic
    private Integer deleted;
}
