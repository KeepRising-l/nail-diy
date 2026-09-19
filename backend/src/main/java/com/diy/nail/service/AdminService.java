package com.diy.nail.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diy.nail.common.BusinessException;
import com.diy.nail.common.PageResult;
import com.diy.nail.entity.User;
import com.diy.nail.mapper.UserMapper;
import com.diy.nail.vo.AdminUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理端业务：平台用户管理（查看 / 启用禁用）
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;

    /** 用户分页（手机号/昵称关键字、状态、角色可选筛选），新用户在前 */
    public PageResult<AdminUserVO> userPage(String keyword, Integer status, String role, long page, long size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .and(StrUtil.isNotBlank(keyword),
                        w -> w.like(User::getPhone, keyword).or().like(User::getNickname, keyword))
                .eq(status != null, User::getStatus, status)
                .eq(StrUtil.isNotBlank(role), User::getRole, role)
                .orderByDesc(User::getId);
        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        List<AdminUserVO> records = result.getRecords().stream().map(AdminUserVO::from).toList();
        return PageResult.of(result.getTotal(), records);
    }

    /** 启用/禁用账号（禁用后无法登录；不允许操作自己，防止自锁） */
    public void updateStatus(Long targetUserId, Integer status, Long currentUserId) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态参数不合法");
        }
        if (targetUserId.equals(currentUserId)) {
            throw new BusinessException("不能禁用自己的账号");
        }
        User user = userMapper.selectById(targetUserId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }
}
