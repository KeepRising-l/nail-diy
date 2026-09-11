package com.diy.nail.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diy.nail.common.BusinessException;
import com.diy.nail.common.PageResult;
import com.diy.nail.common.ResultCode;
import com.diy.nail.dto.DesignRequest;
import com.diy.nail.entity.Design;
import com.diy.nail.entity.Material;
import com.diy.nail.mapper.DesignMapper;
import com.diy.nail.mapper.MaterialMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * DIY 设计业务：顾客保存/编辑/删除自己的设计，参数引用素材做存在性与上架校验
 */
@Service
@RequiredArgsConstructor
public class DesignService {

    /** 平台标准价（本期统一计费，不按素材计价） */
    private static final BigDecimal STANDARD_PRICE = new BigDecimal("39.00");

    private final DesignMapper designMapper;
    private final MaterialMapper materialMapper;

    /** 我的设计分页列表（按创建时间倒序） */
    public PageResult<Design> page(Long userId, long page, long size) {
        LambdaQueryWrapper<Design> wrapper = new LambdaQueryWrapper<Design>()
                .eq(Design::getUserId, userId)
                .orderByDesc(Design::getId);
        Page<Design> result = designMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /** 保存设计：校验参数后入库，价格取平台标准价，返回新设计 id */
    public Long create(Long userId, DesignRequest request) {
        validateParams(request.getParams());
        Design design = new Design();
        design.setUserId(userId);
        design.setName(request.getName());
        design.setParams(request.getParams());
        design.setPreviewUrl(StrUtil.nullToEmpty(request.getPreviewUrl()));
        design.setPrice(STANDARD_PRICE);
        designMapper.insert(design);
        return design.getId();
    }

    /** 编辑设计（仅本人）：更新名称与参数，传入新快照则一并更新 */
    public void update(Long userId, Long id, DesignRequest request) {
        Design design = getOwned(userId, id);
        validateParams(request.getParams());
        design.setName(request.getName());
        design.setParams(request.getParams());
        if (StrUtil.isNotBlank(request.getPreviewUrl())) {
            design.setPreviewUrl(request.getPreviewUrl());
        }
        designMapper.updateById(design);
    }

    /** 删除设计（仅本人，逻辑删除） */
    public void delete(Long userId, Long id) {
        getOwned(userId, id);
        designMapper.deleteById(id);
    }

    /** 设计详情（仅本人，供编辑回填） */
    public Design detail(Long userId, Long id) {
        return getOwned(userId, id);
    }

    /** 按 id 查本人设计；不存在返回 404，不是本人的返回 403 */
    private Design getOwned(Long userId, Long id) {
        Design design = designMapper.selectById(id);
        if (design == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!design.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "不能操作其他人的设计");
        }
        return design;
    }

    /**
     * 校验设计参数：合法 JSON 对象，甲型/色卡/图案均须为上架且类型匹配的素材。
     * 支持两种结构：
     * 1. 五指结构（change-05 起前端实际发送）：{fingers:[{shapeId,colorId,patternIds,...}]}，至少一指配置完整
     * 2. 旧单甲结构（模板套用/历史数据）：{shapeId,colorId,patternIds}
     */
    private void validateParams(String params) {
        if (!JSONUtil.isTypeJSONObject(params)) {
            throw new BusinessException("设计参数必须是合法的 JSON 对象");
        }
        JSONObject obj = JSONUtil.parseObj(params);
        JSONArray fingers = obj.getJSONArray("fingers");
        if (fingers != null) {
            int configured = 0;
            for (Object item : fingers) {
                if (!(item instanceof JSONObject)) {
                    throw new BusinessException("设计参数 fingers 必须是对象数组");
                }
                JSONObject finger = (JSONObject) item;
                Long shapeId = finger.getLong("shapeId");
                Long colorId = finger.getLong("colorId");
                if (shapeId == null && colorId == null) {
                    continue;
                }
                if (shapeId == null || colorId == null) {
                    throw new BusinessException("每根手指需同时选择甲型和色卡");
                }
                checkMaterial(shapeId, Material.TYPE_SHAPE);
                checkMaterial(colorId, Material.TYPE_COLOR);
                checkPatterns(finger.getJSONArray("patternIds"));
                configured++;
            }
            if (configured == 0) {
                throw new BusinessException("请至少为一根手指选择甲型和色卡");
            }
            return;
        }
        Long shapeId = obj.getLong("shapeId");
        Long colorId = obj.getLong("colorId");
        if (shapeId == null || colorId == null) {
            throw new BusinessException("设计参数必须包含甲型（shapeId）和色卡（colorId）");
        }
        checkMaterial(shapeId, Material.TYPE_SHAPE);
        checkMaterial(colorId, Material.TYPE_COLOR);
        checkPatterns(obj.getJSONArray("patternIds"));
    }

    /** 图案 id 列表逐个校验（可为空） */
    private void checkPatterns(JSONArray patternIds) {
        if (patternIds == null) {
            return;
        }
        for (Object pid : patternIds) {
            checkMaterial(Long.valueOf(pid.toString()), Material.TYPE_PATTERN);
        }
    }

    /** 素材必须存在、上架且类型匹配 */
    private void checkMaterial(Long materialId, String expectType) {
        Material material = materialMapper.selectById(materialId);
        if (material == null || material.getStatus() == null || material.getStatus() != 1
                || !expectType.equals(material.getType())) {
            throw new BusinessException("所选素材已下架或不存在，请重新选择");
        }
    }
}
