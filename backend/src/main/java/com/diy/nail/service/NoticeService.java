package com.diy.nail.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diy.nail.common.BusinessException;
import com.diy.nail.common.PageResult;
import com.diy.nail.common.ResultCode;
import com.diy.nail.dto.NoticeRequest;
import com.diy.nail.entity.Notice;
import com.diy.nail.mapper.NoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 公告业务：商家端增删改查 + 顾客端已发布公告浏览
 */
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeMapper noticeMapper;

    /** 商家端：分页列表（可按标题关键字/状态筛选），置顶优先、新公告靠前 */
    public PageResult<Notice> page(String keyword, Integer status, long page, long size) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<Notice>()
                .like(StrUtil.isNotBlank(keyword), Notice::getTitle, keyword)
                .eq(status != null, Notice::getStatus, status)
                .orderByDesc(Notice::getIsTop)
                .orderByDesc(Notice::getId);
        Page<Notice> result = noticeMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /** 新增公告 */
    public Long create(NoticeRequest request) {
        Notice notice = new Notice();
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setIsTop(request.getIsTop() == null ? 0 : request.getIsTop());
        notice.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        noticeMapper.insert(notice);
        return notice.getId();
    }

    /** 编辑公告（含置顶/上下线切换） */
    public void update(Long id, NoticeRequest request) {
        Notice notice = getById(id);
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setIsTop(request.getIsTop() == null ? 0 : request.getIsTop());
        notice.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        noticeMapper.updateById(notice);
    }

    /** 删除公告（逻辑删除） */
    public void delete(Long id) {
        getById(id);
        noticeMapper.deleteById(id);
    }

    /** 顾客端：已发布公告（置顶优先、新的在前，取前 5 条供首页展示） */
    public List<Notice> listPublished() {
        return noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, 1)
                .orderByDesc(Notice::getIsTop)
                .orderByDesc(Notice::getId)
                .last("LIMIT 5"));
    }

    /** 按 ID 查公告（不存在则抛业务异常） */
    public Notice getById(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return notice;
    }
}
