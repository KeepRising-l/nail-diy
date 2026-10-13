package com.diy.nail.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diy.nail.common.BusinessException;
import com.diy.nail.common.PageResult;
import com.diy.nail.common.ResultCode;
import com.diy.nail.dto.TemplateRequest;
import com.diy.nail.entity.Template;
import com.diy.nail.mapper.TemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 款式模板业务：商家端增删改查 + 顾客端上架模板浏览/详情
 */
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateMapper templateMapper;

    /** 商家端：分页列表（可按风格标签/状态/名称关键字筛选） */
    public PageResult<Template> page(String styleTag, Integer status, String keyword, long page, long size) {
        LambdaQueryWrapper<Template> wrapper = new LambdaQueryWrapper<Template>()
                .eq(StrUtil.isNotBlank(styleTag), Template::getStyleTag, styleTag)
                .eq(status != null, Template::getStatus, status)
                .like(StrUtil.isNotBlank(keyword), Template::getName, keyword)
                .orderByAsc(Template::getSort)
                .orderByDesc(Template::getId);
        Page<Template> result = templateMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /** 新增模板 */
    public Long create(TemplateRequest request) {
        validate(request);
        Template template = new Template();
        template.setName(request.getName());
        template.setStyleTag(StrUtil.nullToEmpty(request.getStyleTag()));
        template.setCoverUrl(request.getCoverUrl());
        template.setDesignParams(request.getDesignParams());
        template.setPrice(request.getPrice());
        template.setSort(request.getSort() == null ? 0 : request.getSort());
        template.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        templateMapper.insert(template);
        return template.getId();
    }

    /** 编辑模板（含上下架切换） */
    public void update(Long id, TemplateRequest request) {
        Template template = getById(id);
        validate(request);
        template.setName(request.getName());
        template.setStyleTag(StrUtil.nullToEmpty(request.getStyleTag()));
        template.setCoverUrl(request.getCoverUrl());
        template.setDesignParams(request.getDesignParams());
        template.setPrice(request.getPrice());
        if (request.getSort() != null) {
            template.setSort(request.getSort());
        }
        if (request.getStatus() != null) {
            template.setStatus(request.getStatus());
        }
        templateMapper.updateById(template);
    }

    /** 删除模板（逻辑删除） */
    public void delete(Long id) {
        getById(id);
        templateMapper.deleteById(id);
    }

    /** 顾客端：仅上架模板，按排序号升序分页（风格标签可选筛选） */
    public PageResult<Template> pageOnSale(String styleTag, long page, long size) {
        LambdaQueryWrapper<Template> wrapper = new LambdaQueryWrapper<Template>()
                .eq(StrUtil.isNotBlank(styleTag), Template::getStyleTag, styleTag)
                .eq(Template::getStatus, 1)
                .orderByAsc(Template::getSort)
                .orderByAsc(Template::getId);
        Page<Template> result = templateMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /** 顾客端：模板详情（仅上架可见，供一键套用取设计参数） */
    public Template detail(Long id) {
        Template template = templateMapper.selectById(id);
        if (template == null || template.getStatus() == null || template.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return template;
    }

    /** 业务校验：封面图必填、设计参数必须合法 JSON 对象、价格不能为负 */
    private void validate(TemplateRequest request) {
        if (StrUtil.isBlank(request.getCoverUrl())) {
            throw new BusinessException("模板必须上传封面图");
        }
        if (StrUtil.isBlank(request.getDesignParams()) || !JSONUtil.isTypeJSONObject(request.getDesignParams())) {
            throw new BusinessException("设计参数必须是合法的 JSON 对象");
        }
        if (request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("参考价格不能为负数");
        }
    }

    /** 按 ID 查模板（不存在则抛业务异常） */
    private Template getById(Long id) {
        Template template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return template;
    }
}
