package com.diy.nail.security;

import cn.hutool.core.util.StrUtil;
import com.diy.nail.entity.User;
import com.diy.nail.mapper.UserMapper;
import com.diy.nail.util.JwtUtil;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：解析 Authorization: Bearer <token>，
 * 校验通过后把用户身份写入 SecurityContext（后续接口可用当前用户）
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final UserMapper userMapper;

    public JwtAuthenticationFilter(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Value("${nail.jwt.secret}")
    private String secret;

    @Value("${nail.jwt.expire}")
    private long expire;

    @PostConstruct
    void validateSecret() {
        JwtUtil.validateSecret(secret);
        if (expire <= 0) {
            throw new IllegalStateException("JWT 有效期必须大于 0");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StrUtil.isNotBlank(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (JwtUtil.verify(token, secret)) {
                Long userId = JwtUtil.getUserId(token);
                User user = userId == null ? null : userMapper.selectById(userId);
                if (user != null && Integer.valueOf(1).equals(user.getStatus())
                        && StrUtil.isNotBlank(user.getRole())) {
                    // Spring Security 角色约定：需要 ROLE_ 前缀
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        chain.doFilter(request, response);
    }
}
