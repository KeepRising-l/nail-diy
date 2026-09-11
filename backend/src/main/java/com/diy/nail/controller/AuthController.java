package com.diy.nail.controller;

import com.diy.nail.common.Result;
import com.diy.nail.dto.LoginRequest;
import com.diy.nail.dto.LoginResponse;
import com.diy.nail.dto.RegisterRequest;
import com.diy.nail.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口（公开）：注册、登录
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.ok("注册成功", null);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(userService.login(request));
    }
}
