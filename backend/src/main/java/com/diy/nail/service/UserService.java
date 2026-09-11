package com.diy.nail.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diy.nail.common.BusinessException;
import com.diy.nail.common.ResultCode;
import com.diy.nail.dto.LoginRequest;
import com.diy.nail.dto.LoginResponse;
import com.diy.nail.dto.RegisterRequest;
import com.diy.nail.entity.User;
import com.diy.nail.mapper.UserMapper;
import com.diy.nail.util.JwtUtil;
import com.diy.nail.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户业务：注册、登录、个人信息
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${nail.jwt.secret}")
    private String jwtSecret;

    @Value("${nail.jwt.expire}")
    private long jwtExpire;

    /** 注册（仅顾客角色） */
    public void register(RegisterRequest request) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));
        if (count != null && count > 0) {
            throw new BusinessException("该手机号已注册");
        }
        User user = new User();
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setAvatar("");
        user.setRole(User.ROLE_USER);
        user.setStatus(1);
        userMapper.insert(user);
    }

    /** 登录：校验通过返回 token + 用户信息 */
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("手机号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }
        String token = JwtUtil.createToken(user.getId(), user.getRole(), jwtSecret, jwtExpire);
        return new LoginResponse(token, UserVO.from(user));
    }

    /** 当前用户信息 */
    public UserVO getProfile(Long userId) {
        User user = getById(userId);
        return UserVO.from(user);
    }

    /** 修改昵称/头像 */
    public UserVO updateProfile(Long userId, String nickname, String avatar) {
        User user = getById(userId);
        if (StrUtil.isNotBlank(nickname)) {
            user.setNickname(nickname);
        }
        if (StrUtil.isNotBlank(avatar)) {
            user.setAvatar(avatar);
        }
        userMapper.updateById(user);
        return UserVO.from(user);
    }

    /** 按 ID 查用户（不存在则抛业务异常） */
    public User getById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return user;
    }
}
