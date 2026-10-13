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
 * 素材业务：运营端增删改查/审核/批量上下架 + 顾客端上架素材浏览 + UGC 贴纸上传
 */
@Service
@RequiredArgsConstructor
public class MaterialService {

    /** 色卡颜色代码格式：#RRGGBB */
    private static final String COLOR_CODE_REGEX = "^#[0-9A-Fa-f]{6}$";

    private final MaterialMapper materialMapper;
    private final DesignMapper designMapper;

    /** 运营端：分页列表（可按类型/状态/审核状态/名称关键字筛选） */
    public PageResult<Material> page(String type, Integer status, String auditStatus,
                                     String keyword, long page, long size) {
        LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<Material>()
                .eq(StrUtil.isNotBlank(type), Material::getType, type)
                .eq(status != null, Material::getStatus, status)
                .eq(StrUtil.isNotBlank(auditStatus), Material::getAuditStatus, auditStatus)
                .like(StrUtil.isNotBlank(keyword), Material::getName, keyword)
                .orderByAsc(Material::getSort)
                .orderByDesc(Material::getId);
        Page<Material> result = materialMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(this::maskPrivateImageUrl);
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /** 新增素材（按类型做业务校验；同类型下 code 唯一） */
    public Long create(MaterialRequest request) {
        validateByType(request);
        checkCodeUnique(null, request.getType(), request.getCode());
        Material material = new Material();
        applyRequest(material, request);
        material.setSource(Material.SOURCE_PLATFORM);
        material.setAuditStatus(Material.AUDIT_APPROVED);
        material.setHeat(0);
        materialMapper.insert(material);
        return material.getId();
    }

    /** 编辑素材（含上下架切换） */
    public void update(Long id, MaterialRequest request) {
        Material material = getById(id);
        String storedUgcImage = Material.SOURCE_UGC.equals(material.getSource())
                ? material.getImageUrl() : null;
        validateByType(request);
        checkCodeUnique(id, request.getType(), request.getCode());
        applyRequest(material, request);
        if (storedUgcImage != null
                && ("/api/materials/" + id + "/image").equals(request.getImageUrl())) {
            material.setImageUrl(storedUgcImage);
        }
        materialMapper.updateById(material);
    }

    /** 删除素材：被设计引用时禁止删除，提示下架 */
    public void delete(Long id) {
        Material material = getById(id);
        // 设计参数 JSON 中包含该素材 id 即视为被引用
        Long refCount = designMapper.selectCount(new LambdaQueryWrapper<Design>()
                .apply("params LIKE CONCAT('%', {0}, '%')", material.getId()));
        if (refCount != null && refCount > 0) {
            throw new BusinessException("该素材已被 " + refCount + " 个设计使用，建议下架");
        }
        materialMapper.deleteById(id);
    }

    /** 批量上下架 */
    public void batchUpdateStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要操作的素材");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态值不合法");
        }
        for (Long id : ids) {
            Material material = getById(id);
            material.setStatus(status);
            materialMapper.updateById(material);
        }
    }

    /** UGC 素材审核：通过则上架可见，驳回则记录状态 */
    public void audit(Long id, boolean approved) {
        Material material = getById(id);
        if (!Material.AUDIT_PENDING.equals(material.getAuditStatus())) {
            throw new BusinessException("该素材不在待审核状态");
        }
        String auditStatus = approved ? Material.AUDIT_APPROVED : Material.AUDIT_REJECTED;
        int updated = materialMapper.auditPending(id, auditStatus, approved ? 1 : 0);
        if (updated != 1) {
            throw new BusinessException("该素材审核状态已变化，请刷新后重试");
        }
    }

    /** 顾客端：UGC 贴纸上传（透明底 PNG，进入待审核，审核通过后全员可见） */
    public Material createSticker(Long userId, String name, String imageUrl) {
        if (StrUtil.isBlank(imageUrl) || !imageUrl.startsWith(FileService.PRIVATE_STICKER_PREFIX)) {
            throw new BusinessException("贴纸图片不能为空");
        }
        String stickerName = StrUtil.isBlank(name) ? "我的贴纸" : name.trim();
        if (stickerName.length() > 50) {
            throw new BusinessException("贴纸名称不能超过50字");
        }
        Material material = new Material();
        material.setType(Material.TYPE_STICKER);
        material.setName(stickerName);
        material.setImageUrl(imageUrl);
        material.setSource(Material.SOURCE_UGC);
        material.setCategory("UGC");
        material.setCreatorId(userId);
        material.setAuditStatus(Material.AUDIT_PENDING);
        material.setHeat(0);
        material.setSort(0);
        material.setStatus(0);
        materialMapper.insert(material);
        maskPrivateImageUrl(material);
        return material;
    }

    /**
     * 顾客端：上架素材（按排序号升序；支持类型/分类/标签/关键字筛选）。
     * 审核通过的全员可见；UGC 贴纸在上传者本人面前待审核期间也可见，便于确认上传结果。
     */
    public List<Material> listOnSale(String type, String category, String tag, String keyword, Long viewerId) {
        LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<Material>()
                .eq(StrUtil.isNotBlank(type), Material::getType, type)
                .eq(StrUtil.isNotBlank(category), Material::getCategory, category)
                .like(StrUtil.isNotBlank(tag), Material::getTags, tag)
                .like(StrUtil.isNotBlank(keyword), Material::getName, keyword)
                .and(w -> {
                    w.and(approved -> approved.eq(Material::getStatus, 1)
                            .eq(Material::getAuditStatus, Material.AUDIT_APPROVED));
                    if (viewerId != null) {
                        w.or(ow -> ow.eq(Material::getSource, Material.SOURCE_UGC)
                                .eq(Material::getCreatorId, viewerId)
                                .eq(Material::getAuditStatus, Material.AUDIT_PENDING));
                    }
                })
                .orderByAsc(Material::getSort)
                .orderByAsc(Material::getId);
        List<Material> materials = materialMapper.selectList(wrapper);
        materials.forEach(this::maskPrivateImageUrl);
        return materials;
    }

    /** 私有贴纸审核通过后公开；待审核时仅上传者和运营角色可读取。 */
    public String imagePath(Long id, Long viewerId, boolean operator) {
        Material material = getById(id);
        boolean supportedPath = StrUtil.isNotBlank(material.getImageUrl())
                && (material.getImageUrl().startsWith(FileService.PRIVATE_STICKER_PREFIX)
                || material.getImageUrl().startsWith("/uploads/"));
        if (!supportedPath || !Material.SOURCE_UGC.equals(material.getSource())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        boolean publicImage = Integer.valueOf(1).equals(material.getStatus())
                && Material.AUDIT_APPROVED.equals(material.getAuditStatus());
        boolean pendingOwner = Material.AUDIT_PENDING.equals(material.getAuditStatus())
                && viewerId != null && viewerId.equals(material.getCreatorId());
        if (!publicImage && !pendingOwner && !operator) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return material.getImageUrl();
    }

    /** 素材热度排行（统计看板用，可按类型过滤） */
    public List<Material> hotRank(String type, int limit) {
        int size = Math.max(1, Math.min(limit, 50));
        List<Material> materials = materialMapper.selectList(new LambdaQueryWrapper<Material>()
                .eq(StrUtil.isNotBlank(type), Material::getType, type)
                .eq(Material::getStatus, 1)
                .eq(Material::getAuditStatus, Material.AUDIT_APPROVED)
                .orderByDesc(Material::getHeat)
                .orderByDesc(Material::getId)
                .last("LIMIT " + size));
        materials.forEach(this::maskPrivateImageUrl);
        return materials;
    }

    /** 热度 +1（设计保存/下单时按引用素材调用） */
    public void increaseHeat(Long materialId) {
        if (materialId == null) {
            return;
        }
        Material material = materialMapper.selectById(materialId);
        if (material != null) {
            material.setHeat((material.getHeat() == null ? 0 : material.getHeat()) + 1);
            materialMapper.updateById(material);
        }
    }

    /** 按类型做业务校验：色卡必须合法颜色代码；代码注册素材必须带 code；图片素材必须带图 */
    private void validateByType(MaterialRequest request) {
        String type = request.getType();
        if (Material.TYPE_COLOR.equals(type)) {
            if (StrUtil.isBlank(request.getColorCode())
                    || !request.getColorCode().matches(COLOR_CODE_REGEX)) {
                throw new BusinessException("色卡必须填写正确的颜色代码（格式 #RRGGBB）");
            }
            return;
        }
        // 甲型/参数化款式为代码注册素材，code 必填、无需图片
        if (Material.TYPE_SHAPE.equals(type) || Material.TYPE_PATTERN_STYLE.equals(type)) {
            if (StrUtil.isBlank(request.getCode())) {
                throw new BusinessException("甲型/参数化款式必须填写代码标识（绑定前端渲染器）");
            }
            return;
        }
        // 装饰件：矢量款填 code，立体款传图，二者至少其一
        if (Material.TYPE_DECOR.equals(type)) {
            if (StrUtil.isBlank(request.getCode()) && StrUtil.isBlank(request.getImageUrl())) {
                throw new BusinessException("装饰件需填写代码标识（矢量款）或上传图片（立体款）");
            }
            return;
        }
        // 贴图/贴纸必须带图
        if (StrUtil.isBlank(request.getImageUrl())) {
            throw new BusinessException("贴图/贴纸素材必须上传图片");
        }
    }

    /** 同类型下 code 唯一（code 为空则不校验） */
    private void checkCodeUnique(Long excludeId, String type, String code) {
        if (StrUtil.isBlank(code)) {
            return;
        }
        Long count = materialMapper.selectCount(new LambdaQueryWrapper<Material>()
                .eq(Material::getType, type)
                .eq(Material::getCode, code)
                .ne(excludeId != null, Material::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException("该类型下代码标识 " + code + " 已存在");
        }
    }

    /** 请求字段写入实体（新建/编辑共用） */
    private void applyRequest(Material material, MaterialRequest request) {
        material.setType(request.getType());
        material.setCode(StrUtil.nullToEmpty(request.getCode()).trim());
        material.setName(request.getName());
        material.setImageUrl(StrUtil.nullToEmpty(request.getImageUrl()));
        material.setColorCode(StrUtil.nullToEmpty(request.getColorCode()));
        material.setCategory(StrUtil.nullToEmpty(request.getCategory()));
        material.setTags(StrUtil.nullToEmpty(request.getTags()));
        material.setAttrs(StrUtil.isBlank(request.getAttrs()) ? null : request.getAttrs());
        if (request.getSort() != null) {
            material.setSort(request.getSort());
        } else if (material.getSort() == null) {
            material.setSort(0);
        }
        if (request.getStatus() != null) {
            material.setStatus(request.getStatus());
        } else if (material.getStatus() == null) {
            material.setStatus(1);
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

    private void maskPrivateImageUrl(Material material) {
        boolean privatePath = material.getImageUrl() != null
                && material.getImageUrl().startsWith(FileService.PRIVATE_STICKER_PREFIX);
        boolean hiddenLegacyUgc = Material.SOURCE_UGC.equals(material.getSource())
                && (!Integer.valueOf(1).equals(material.getStatus())
                || !Material.AUDIT_APPROVED.equals(material.getAuditStatus()));
        if (privatePath || hiddenLegacyUgc) {
            material.setImageUrl("/api/materials/" + material.getId() + "/image");
        }
    }
}
