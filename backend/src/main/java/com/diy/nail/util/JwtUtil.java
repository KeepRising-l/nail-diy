package com.diy.nail.util;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具（Hutool 实现，HS256）
 */
public class JwtUtil {

    /** 载荷中的用户 ID 键 */
    public static final String CLAIM_USER_ID = "userId";
    /** 载荷中的角色键 */
    public static final String CLAIM_ROLE = "role";

    /**
     * 生成 token
     *
     * @param userId 用户 ID
     * @param role   角色
     * @param secret 签名密钥
     * @param expire 有效期（毫秒）
     */
    public static String createToken(Long userId, String role, String secret, long expire) {
        Map<String, Object> payload = new HashMap<>();
        payload.put(CLAIM_USER_ID, userId);
        payload.put(CLAIM_ROLE, role);
        return JWTUtil.createToken(payload, secret.getBytes());
    }

    /**
     * 校验 token 是否有效（签名 + 未过期）
     */
    public static boolean verify(String token, String secret) {
        try {
            return JWTUtil.verify(token, secret.getBytes());
        } catch (Exception e) {
            return false;
        }
    }

    /** 从 token 取用户 ID（调用前请先 verify） */
    public static Long getUserId(String token) {
        JWT jwt = JWTUtil.parseToken(token);
        Object v = jwt.getPayload(CLAIM_USER_ID);
        return v == null ? null : Long.valueOf(v.toString());
    }

    /** 从 token 取角色（调用前请先 verify） */
    public static String getRole(String token) {
        JWT jwt = JWTUtil.parseToken(token);
        Object v = jwt.getPayload(CLAIM_ROLE);
        return v == null ? null : v.toString();
    }
}
