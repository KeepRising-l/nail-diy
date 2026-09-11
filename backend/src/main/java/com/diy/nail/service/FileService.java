package com.diy.nail.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.diy.nail.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Set;

/**
 * 文件上传业务：图片保存到 uploads/ 按日期分子目录，返回 /uploads/... 访问路径
 */
@Service
public class FileService {

    /** 允许上传的图片扩展名 */
    private static final Set<String> IMAGE_EXTS = Set.of("jpg", "jpeg", "png", "webp");

    /** 单张图片大小上限：5MB */
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

    @Value("${nail.upload.dir}")
    private String uploadDir;

    /** 上传一张图片，返回前端可直接使用的相对路径 */
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        String ext = FileUtil.extName(file.getOriginalFilename());
        if (ext == null || !IMAGE_EXTS.contains(ext.toLowerCase())) {
            throw new BusinessException("仅支持 jpg/jpeg/png/webp 格式的图片");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException("图片大小不能超过 5MB");
        }
        String subDir = DateUtil.format(new Date(), "yyyyMMdd");
        File dir = FileUtil.file(uploadDir, subDir);
        if (!dir.exists()) {
            FileUtil.mkdir(dir);
        }
        String fileName = IdUtil.fastSimpleUUID() + "." + ext.toLowerCase();
        File target = FileUtil.file(dir, fileName);
        try (InputStream in = file.getInputStream()) {
            FileUtil.writeFromStream(in, target);
        } catch (IOException e) {
            throw new BusinessException("图片保存失败，请重试");
        }
        return "/uploads/" + subDir + "/" + fileName;
    }
}
