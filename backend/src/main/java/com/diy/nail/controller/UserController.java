package com.diy.nail.controller;

import com.diy.nail.common.Result;
import com.diy.nail.service.UserService;
import com.diy.nail.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户接口（需登录）：个人信息
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 当前登录用户 ID（由 JWT 过滤器写入 SecurityContext） */
    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    @GetMapping("/profile")
    public Result<UserVO> profile(Authentication authentication) {
        return Result.ok(userService.getProfile(currentUserId(authentication)));
    }

    @PutMapping("/profile")
    public Result<UserVO> updateProfile(Authentication authentication, @RequestBody Map<String, String> body) {
        return Result.ok(userService.updateProfile(
                currentUserId(authentication), body.get("nickname"), body.get("avatar")));
    }
}
