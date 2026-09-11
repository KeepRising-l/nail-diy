package com.diy.nail.controller;

import com.diy.nail.common.PageResult;
import com.diy.nail.common.Result;
import com.diy.nail.dto.MaterialRequest;
import com.diy.nail.entity.Material;
import com.diy.nail.service.FileService;
import com.diy.nail.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 运营端接口：素材增删改查、批量上下架、UGC 审核、图片上传（仅 MERCHANT/ADMIN 可访问）
 */
@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantMaterialController {

    private final MaterialService materialService;
    private final FileService fileService;

    /** 分页列表：按类型/状态/审核状态/名称关键字筛选 */
    @GetMapping("/materials")
    public Result<PageResult<Material>> page(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String auditStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(materialService.page(type, status, auditStatus, keyword, page, size));
    }

    /** 新增素材 */
    @PostMapping("/materials")
    public Result<Void> create(@Valid @RequestBody MaterialRequest request) {
        materialService.create(request);
        return Result.ok();
    }

    /** 编辑素材（含上下架切换） */
    @PutMapping("/materials/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody MaterialRequest request) {
        materialService.update(id, request);
        return Result.ok();
    }

    /** 删除素材（被设计引用时拒绝并提示下架） */
    @DeleteMapping("/materials/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        materialService.delete(id);
        return Result.ok();
    }

    /** 批量上下架：{ "ids": [1,2,3], "status": 1 } */
    @PostMapping("/materials/batch-status")
    public Result<Void> batchStatus(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> rawIds = (List<Integer>) body.get("ids");
        Integer status = body.get("status") == null ? null : Integer.valueOf(body.get("status").toString());
        List<Long> ids = rawIds == null ? null : rawIds.stream().map(Long::valueOf).toList();
        materialService.batchUpdateStatus(ids, status);
        return Result.ok();
    }

    /** UGC 素材审核：{ "approved": true } */
    @PostMapping("/materials/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        materialService.audit(id, Boolean.TRUE.equals(body.get("approved")));
        return Result.ok();
    }

    /** 图片上传：仅支持 jpg/jpeg/png/webp 且 ≤5MB，返回访问路径 */
    @PostMapping("/upload/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileService.uploadImage(file));
    }
}
