package com.diy.nail.util;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
    private static final String CLAIM_ISSUED_AT = "iat";
    private static final String CLAIM_EXPIRES_AT = "exp";

    /** HS256 密钥至少 32 字节，并拒绝明显的重复弱密钥。 */
    public static void validateSecret(String secret) {
        if (secret == null || secret.isBlank()
                || secret.getBytes(StandardCharsets.UTF_8).length < 32
                || secret.chars().distinct().count() < 12) {
            throw new IllegalArgumentException("JWT_SECRET 必须是至少 32 字节的随机字符串");
        }
    }

    /**
     * 生成 token
     *
     * @param userId 用户 ID
     * @param role   角色
     * @param secret 签名密钥
     * @param expire 有效期（毫秒）
     */
    public static String createToken(Long userId, String role, String secret, long expire) {
        if (userId == null || userId <= 0 || role == null || role.isBlank()) {
            throw new IllegalArgumentException("JWT 用户身份不能为空");
        }
        validateSecret(secret);
        if (expire <= 0) {
            throw new IllegalArgumentException("JWT 有效期必须大于 0");
        }
        long issuedAt = Instant.now().getEpochSecond();
        Map<String, Object> payload = new HashMap<>();
        payload.put(CLAIM_USER_ID, userId);
        payload.put(CLAIM_ROLE, role);
        payload.put(CLAIM_ISSUED_AT, issuedAt);
        payload.put(CLAIM_EXPIRES_AT, issuedAt + Math.max(1, expire / 1000));
        return JWTUtil.createToken(payload, secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 校验 token 是否有效（签名 + 未过期）
     */
    public static boolean verify(String token, String secret) {
        try {
            if (!JWTUtil.verify(token, secret.getBytes(StandardCharsets.UTF_8))) {
                return false;
            }
            JWT jwt = JWTUtil.parseToken(token);
            long userId = Long.parseLong(jwt.getPayload(CLAIM_USER_ID).toString());
            String role = jwt.getPayload(CLAIM_ROLE).toString();
            long issuedAt = Long.parseLong(jwt.getPayload(CLAIM_ISSUED_AT).toString());
            long expiresAt = Long.parseLong(jwt.getPayload(CLAIM_EXPIRES_AT).toString());
            long now = Instant.now().getEpochSecond();
            return userId > 0 && !role.isBlank()
                    && issuedAt <= now && expiresAt > now && expiresAt > issuedAt;
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
