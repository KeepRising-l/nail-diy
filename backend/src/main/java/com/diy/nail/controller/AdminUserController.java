package com.diy.nail.controller;

import com.diy.nail.common.PageResult;
import com.diy.nail.common.Result;
import com.diy.nail.service.AdminService;
import com.diy.nail.vo.AdminUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端接口：平台用户管理（仅 ADMIN 可访问）
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminService adminService;

    /** 当前管理员 ID（由 JWT 过滤器写入 SecurityContext） */
    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    /** 用户分页（关键字/状态可选筛选） */
    @GetMapping
    public Result<PageResult<AdminUserVO>> page(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Integer status,
                                                @RequestParam(required = false) String role,
                                                @RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size) {
        return Result.ok(adminService.userPage(keyword, status, role, page, size));
    }

    /** 启用/禁用账号（禁用后无法登录） */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(Authentication authentication, @PathVariable Long id,
                                     @RequestBody Map<String, Integer> body) {
        adminService.updateStatus(id, body.get("status"), currentUserId(authentication));
        return Result.ok();
    }
}
