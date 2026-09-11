package com.diy.nail.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diy.nail.common.BusinessException;
import com.diy.nail.common.PageResult;
import com.diy.nail.common.ResultCode;
import com.diy.nail.dto.MaterialRequest;
import com.diy.nail.entity.Design;
import com.diy.nail.entity.Material;
import com.diy.nail.mapper.DesignMapper;
import com.diy.nail.mapper.MaterialMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 素材业务：商家端增删改查 + 顾客端上架素材浏览
 */
@Service
@RequiredArgsConstructor
public class MaterialService {

    /** 色卡颜色代码格式：#RRGGBB */
    private static final String COLOR_CODE_REGEX = "^#[0-9A-Fa-f]{6}$";

    private final MaterialMapper materialMapper;
    private final DesignMapper designMapper;

    /** 商家端：分页列表（可按类型/状态/名称关键字筛选） */
    public PageResult<Material> page(String type, Integer status, String keyword, long page, long size) {
        LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<Material>()
                .eq(StrUtil.isNotBlank(type), Material::getType, type)
                .eq(status != null, Material::getStatus, status)
                .like(StrUtil.isNotBlank(keyword), Material::getName, keyword)
                .orderByAsc(Material::getSort)
                .orderByDesc(Material::getId);
        Page<Material> result = materialMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /** 新增素材（按类型做业务校验） */
    public void create(MaterialRequest request) {
        validateByType(request);
        Material material = new Material();
        material.setType(request.getType());
        material.setName(request.getName());
        material.setImageUrl(StrUtil.nullToEmpty(request.getImageUrl()));
        material.setColorCode(StrUtil.nullToEmpty(request.getColorCode()));
        material.setCategory(StrUtil.nullToEmpty(request.getCategory()));
        material.setSort(request.getSort() == null ? 0 : request.getSort());
        material.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        materialMapper.insert(material);
    }

    /** 编辑素材（含上下架切换） */
    public void update(Long id, MaterialRequest request) {
        Material material = getById(id);
        validateByType(request);
        material.setType(request.getType());
        material.setName(request.getName());
        material.setImageUrl(StrUtil.nullToEmpty(request.getImageUrl()));
        material.setColorCode(StrUtil.nullToEmpty(request.getColorCode()));
        material.setCategory(StrUtil.nullToEmpty(request.getCategory()));
        if (request.getSort() != null) {
            material.setSort(request.getSort());
        }
        if (request.getStatus() != null) {
            material.setStatus(request.getStatus());
        }
        materialMapper.updateById(material);
    }

    /** 删除素材：被设计引用时禁止删除，提示下架 */
    public void delete(Long id) {
        Material material = getById(id);
        // 设计参数 JSON 中包含该素材 id 即视为被引用（change-05 定义 params 结构后可精确化）
        Long refCount = designMapper.selectCount(new LambdaQueryWrapper<Design>()
                .apply("params LIKE CONCAT('%', {0}, '%')", material.getId()));
        if (refCount != null && refCount > 0) {
            throw new BusinessException("该素材已被 " + refCount + " 个设计使用，建议下架");
        }
        materialMapper.deleteById(id);
    }

    /** 顾客端：仅上架素材，按排序号升序（供设计器分类浏览，可按图案分类筛选） */
    public List<Material> listOnSale(String type, String category) {
        return materialMapper.selectList(new LambdaQueryWrapper<Material>()
                .eq(StrUtil.isNotBlank(type), Material::getType, type)
                .eq(StrUtil.isNotBlank(category), Material::getCategory, category)
                .eq(Material::getStatus, 1)
                .orderByAsc(Material::getSort)
                .orderByAsc(Material::getId));
    }

    /** 按类型做业务校验：色卡必须合法颜色代码且免图，甲型/图案必须带图 */
    private void validateByType(MaterialRequest request) {
        if (Material.TYPE_COLOR.equals(request.getType())) {
            if (StrUtil.isBlank(request.getColorCode())
                    || !request.getColorCode().matches(COLOR_CODE_REGEX)) {
                throw new BusinessException("色卡必须填写正确的颜色代码（格式 #RRGGBB）");
            }
        } else {
            if (StrUtil.isBlank(request.getImageUrl())) {
                throw new BusinessException("甲型/图案素材必须上传图片");
            }
        }
    }

    /** 按 ID 查素材（不存在则抛业务异常） */
    private Material getById(Long id) {
        Material material = materialMapper.selectById(id);
        if (material == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return material;
    }
}
