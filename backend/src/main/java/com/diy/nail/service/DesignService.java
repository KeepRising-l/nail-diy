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
        increaseMaterialHeat(request.getParams());
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
                String colorCode = finger.getStr("colorCode");
                // 颜色有两种来源：色卡（colorId）或自定义色值（colorCode，设计台的滑条/HEX 输入）
                boolean hasColor = colorId != null || StrUtil.isNotBlank(colorCode);
                if (shapeId == null && !hasColor) {
                    continue;
                }
                if (shapeId == null) {
                    throw new BusinessException("每根手指需选择甲型");
                }
                if (!hasColor) {
                    throw new BusinessException("每根手指需选择颜色（色卡或自定义色值）");
                }
                checkMaterial(shapeId, Material.TYPE_SHAPE);
                if (colorId != null) {
                    checkMaterial(colorId, Material.TYPE_COLOR);
                }
                checkColorCode(colorCode);
                checkPatterns(finger.getJSONArray("patternIds"));
                configured++;
            }
            if (configured == 0) {
                throw new BusinessException("请至少为一根手指选择甲型和颜色");
            }
            return;
        }
        Long shapeId = obj.getLong("shapeId");
        Long colorId = obj.getLong("colorId");
        String colorCode = obj.getStr("colorCode");
        if (shapeId == null) {
            throw new BusinessException("设计参数必须包含甲型（shapeId）");
        }
        if (colorId == null && StrUtil.isBlank(colorCode)) {
            throw new BusinessException("设计参数必须包含颜色（色卡 colorId 或自定义色值 colorCode）");
        }
        checkMaterial(shapeId, Material.TYPE_SHAPE);
        if (colorId != null) {
            checkMaterial(colorId, Material.TYPE_COLOR);
        }
        checkColorCode(colorCode);
        checkPatterns(obj.getJSONArray("patternIds"));
    }

    /** 自定义色值格式校验（#RRGGBB；为空则跳过，表示该手指用色卡取色） */
    private void checkColorCode(String colorCode) {
        if (StrUtil.isNotBlank(colorCode) && !colorCode.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new BusinessException("自定义颜色格式不正确，应为 #RRGGBB");
        }
    }

    /** 贴图 id 列表逐个校验（可为空；接受贴图/贴纸两种类型） */
    private void checkPatterns(JSONArray patternIds) {
        if (patternIds == null) {
            return;
        }
        for (Object pid : patternIds) {
            checkMaterial(Long.valueOf(pid.toString()), Material.TYPE_PATTERN_IMG, Material.TYPE_STICKER);
        }
    }

    /** 素材必须存在、上架、审核通过且类型匹配（可传多个可接受类型） */
    private void checkMaterial(Long materialId, String... expectTypes) {
        Material material = materialMapper.selectById(materialId);
        boolean typeMatch = material != null
                && java.util.Arrays.asList(expectTypes).contains(material.getType());
        if (!typeMatch || material.getStatus() == null || material.getStatus() != 1
                || !Material.AUDIT_APPROVED.equals(material.getAuditStatus())) {
            throw new BusinessException("所选素材已下架或不存在，请重新选择");
        }
    }

    /** 统计设计引用的素材 id（shapeId/colorId/patternIds，五指与单甲结构兼容） */
    private java.util.Set<Long> collectMaterialIds(String params) {
        java.util.Set<Long> ids = new java.util.HashSet<>();
        JSONObject obj = JSONUtil.parseObj(params);
        JSONArray fingers = obj.getJSONArray("fingers");
        java.util.List<JSONObject> nodes = new java.util.ArrayList<>();
        if (fingers != null) {
            for (Object item : fingers) {
                if (item instanceof JSONObject) {
                    nodes.add((JSONObject) item);
                }
            }
        } else {
            nodes.add(obj);
        }
        for (JSONObject node : nodes) {
            if (node.getLong("shapeId") != null) {
                ids.add(node.getLong("shapeId"));
            }
            if (node.getLong("colorId") != null) {
                ids.add(node.getLong("colorId"));
            }
            JSONArray patternIds = node.getJSONArray("patternIds");
            if (patternIds != null) {
                for (Object pid : patternIds) {
                    ids.add(Long.valueOf(pid.toString()));
                }
            }
        }
        return ids;
    }

    /** 引用素材热度 +1（保存设计时调用，失败不影响主流程） */
    private void increaseMaterialHeat(String params) {
        for (Long id : collectMaterialIds(params)) {
            Material material = materialMapper.selectById(id);
            if (material != null) {
                material.setHeat((material.getHeat() == null ? 0 : material.getHeat()) + 1);
                materialMapper.updateById(material);
            }
        }
    }
}
