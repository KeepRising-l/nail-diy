package com.diy.nail.vo;

import com.diy.nail.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端用户视图对象（脱敏：不返回密码）
 */
@Data
public class AdminUserVO {

    private Long id;
    private String phone;
    private String nickname;
    private String role;
    /** 状态：1正常 0禁用 */
    private Integer status;
    private LocalDateTime createTime;

    public static AdminUserVO from(User user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
