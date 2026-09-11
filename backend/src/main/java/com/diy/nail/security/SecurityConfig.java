package com.diy.nail.security;

import cn.hutool.json.JSONUtil;
import com.diy.nail.common.Result;
import com.diy.nail.common.ResultCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Spring Security 配置：
 * - 无状态会话（JWT），关闭 CSRF
 * - 放行：注册登录、健康检查、上传文件
 * - 保护：其余 /api/** 需登录；商家/管理接口按角色限制
 * - 未登录 401 / 无权限 403，统一 JSON 返回
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 公开接口
                        .requestMatchers("/api/auth/**", "/api/health", "/uploads/**").permitAll()
                        // 商品/模板/素材浏览公开（落地页、设计器需游客可访问），下单后才需登录
                        .requestMatchers(HttpMethod.GET, "/api/templates/**", "/api/materials/**").permitAll()
                        // 商家端接口（后续 change 使用）
                        .requestMatchers("/api/merchant/**").hasAnyRole("MERCHANT", "ADMIN")
                        // 管理端接口（后续 change 使用）
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 其余全部需要登录
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, e) ->
                                writeJson(response, Result.error(ResultCode.UNAUTHORIZED)))
                        .accessDeniedHandler((request, response, e) ->
                                writeJson(response, Result.error(ResultCode.FORBIDDEN))));
        return http.build();
    }

    /** 以统一 JSON 结构写出 401/403 响应 */
    private void writeJson(HttpServletResponse response, Result<Void> result) throws IOException {
        response.setStatus(result.getCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JSONUtil.toJsonStr(result));
    }
}
