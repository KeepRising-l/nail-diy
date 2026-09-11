package com.diy.nail.security;

import cn.hutool.core.util.StrUtil;
import com.diy.nail.util.JwtUtil;
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

    @Value("${nail.jwt.secret}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StrUtil.isNotBlank(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (JwtUtil.verify(token, secret)) {
                Long userId = JwtUtil.getUserId(token);
                String role = JwtUtil.getRole(token);
                if (userId != null && role != null) {
                    // Spring Security 角色约定：需要 ROLE_ 前缀
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        chain.doFilter(request, response);
    }
}
