package com.diy.nail.controller;

import com.diy.nail.common.BusinessException;
import com.diy.nail.common.Result;
import com.diy.nail.entity.Material;
import com.diy.nail.service.FileService;
import com.diy.nail.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PathVariable;
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

    /** 按类型获取上架素材（图案可按 category 分类，支持标签/关键字检索），按排序号升序；登录用户额外可见本人待审贴纸 */
    @GetMapping
    public Result<List<Material>> list(
            Authentication authentication,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String keyword) {
        return Result.ok(materialService.listOnSale(type, category, tag, keyword, currentUserId(authentication)));
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
        Long userId = currentUserId(authentication);
        if (userId == null) {
            throw new BusinessException("请先登录后再上传贴纸");
        }
        String imageUrl = fileService.uploadPrivatePngImage(file);
        try {
            return Result.ok(materialService.createSticker(userId, name, imageUrl));
        } catch (RuntimeException e) {
            fileService.deletePrivateImage(imageUrl);
            throw e;
        }
    }

    /** 私有贴纸读取：通过后公开；待审核时仅上传者和运营角色可见。 */
    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> image(Authentication authentication, @PathVariable Long id) {
        String storedPath = materialService.imagePath(
                id, currentUserId(authentication), isOperator(authentication));
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(fileService.loadMaterialImage(storedPath));
    }

    /** 当前登录用户 ID（游客/匿名返回 null） */
    private Long currentUserId(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof Long uid ? uid : null;
    }

    private boolean isOperator(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "ROLE_MERCHANT".equals(role) || "ROLE_ADMIN".equals(role));
    }
}
