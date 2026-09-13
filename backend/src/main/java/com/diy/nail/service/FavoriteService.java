package com.diy.nail.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diy.nail.common.BusinessException;
import com.diy.nail.common.ResultCode;
import com.diy.nail.entity.Favorite;
import com.diy.nail.entity.Template;
import com.diy.nail.mapper.FavoriteMapper;
import com.diy.nail.mapper.TemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 收藏业务：模板收藏/取消收藏（切换式）、收藏模板分页
 */
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final TemplateMapper templateMapper;

    /** 收藏/取消收藏（切换），返回切换后是否已收藏 */
    public boolean toggle(Long userId, Long templateId) {
        Template template = templateMapper.selectById(templateId);
        if (template == null || template.getStatus() == null || template.getStatus() != 1) {
            throw new BusinessException("该款式不存在或已下架");
        }
        Favorite existing = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getTemplateId, templateId));
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            return false;
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setTemplateId(templateId);
        favoriteMapper.insert(favorite);
        return true;
    }

    /** 当前用户收藏的模板 ID 列表（款式库标记红心用） */
    public List<Long> listIds(Long userId) {
        return favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId))
                .stream().map(Favorite::getTemplateId).toList();
    }

    /** 当前用户收藏的模板分页（按收藏时间倒序，仅含仍上架的模板） */
    public com.diy.nail.common.PageResult<Template> page(Long userId, long page, long size) {
        Page<Favorite> result = favoriteMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getId));
        List<Long> templateIds = result.getRecords().stream().map(Favorite::getTemplateId).toList();
        if (templateIds.isEmpty()) {
            return com.diy.nail.common.PageResult.of(result.getTotal(), Collections.emptyList());
        }
        Map<Long, Template> templateMap = templateMapper.selectBatchIds(templateIds).stream()
                .filter(t -> t.getStatus() != null && t.getStatus() == 1)
                .collect(Collectors.toMap(Template::getId, Function.identity()));
        // 保持收藏时间倒序，过滤已下架/已删除的模板
        List<Template> records = templateIds.stream()
                .map(templateMap::get)
                .filter(t -> t != null)
                .toList();
        return com.diy.nail.common.PageResult.of((long) records.size(), records);
    }
}
