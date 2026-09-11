package com.diy.nail.dto;

import com.diy.nail.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录成功返回：token + 用户信息
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private UserVO user;
}
