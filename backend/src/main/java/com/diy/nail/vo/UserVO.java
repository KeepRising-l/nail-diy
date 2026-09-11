package com.diy.nail.vo;

import com.diy.nail.entity.User;
import lombok.Data;

/**
 * 用户信息视图对象（脱敏：不返回密码）
 */
@Data
public class UserVO {

    private Long id;
    private String phone;
    private String nickname;
    private String avatar;
    private String role;

    public static UserVO from(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        return vo;
    }
}
