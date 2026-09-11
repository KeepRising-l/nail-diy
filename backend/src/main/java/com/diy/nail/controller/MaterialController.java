package com.diy.nail.controller;

import com.diy.nail.common.Result;
import com.diy.nail.entity.Material;
import com.diy.nail.service.FileService;
import com.diy.nail.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 顾客端接口：浏览上架素材、UGC 贴纸上传（需登录）
 */
@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;
    private final FileService fileService;

    /** 按类型获取上架素材（图案可按 category 分类，支持标签/关键字检索），按排序号升序 */
    @GetMapping
    public Result<List<Material>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String keyword) {
        return Result.ok(materialService.listOnSale(type, category, tag, keyword));
    }

    /** 素材热度排行（看板/推荐位用） */
    @GetMapping("/hot-rank")
    public Result<List<Material>> hotRank(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(materialService.hotRank(type, limit));
    }

    /** UGC 贴纸上传：透明底 PNG，进入待审核，通过后全员可见 */
    @PostMapping("/sticker")
    public Result<Material> uploadSticker(Authentication authentication,
                                          @RequestParam("file") MultipartFile file,
                                          @RequestParam(required = false) String name) {
        Long userId = (Long) authentication.getPrincipal();
        String imageUrl = fileService.uploadImage(file);
        return Result.ok(materialService.createSticker(userId, name, imageUrl));
    }
}
