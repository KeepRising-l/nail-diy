package com.diy.nail.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.diy.nail.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

/**
 * 文件上传业务：图片保存到 uploads/ 按日期分子目录，返回 /uploads/... 访问路径
 */
@Service
public class FileService {

    public static final String PRIVATE_STICKER_PREFIX = "private://stickers/";

    /** 允许上传的图片扩展名 */
    private static final Set<String> IMAGE_EXTS = Set.of("jpg", "jpeg", "png", "webp");

    /** 单张图片大小上限：5MB */
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final int MAX_IMAGE_DIMENSION = 4096;

    @Value("${nail.upload.dir}")
    private String uploadDir;

    @Value("${nail.upload.private-dir}")
    private String privateUploadDir;

    /** 上传一张图片，返回前端可直接使用的相对路径 */
    public String uploadImage(MultipartFile file) {
        return uploadImage(file, IMAGE_EXTS, "仅支持 jpg/jpeg/png/webp 格式的图片", false,
                uploadDir, "/uploads/");
    }

    /** 待审核贴纸写入静态资源目录之外，审核前只能通过鉴权接口读取。 */
    public String uploadPrivatePngImage(MultipartFile file) {
        return uploadImage(file, Set.of("png"), "贴纸仅支持 PNG 格式", true,
                privateUploadDir, PRIVATE_STICKER_PREFIX);
    }

    private String uploadImage(MultipartFile file, Set<String> allowedExts, String formatMessage,
                               boolean requireTransparency, String storageDir, String pathPrefix) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        String ext = FileUtil.extName(file.getOriginalFilename());
        if (ext == null || !allowedExts.contains(ext.toLowerCase())) {
            throw new BusinessException(formatMessage);
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException("图片大小不能超过 5MB");
        }
        byte[] content;
        try {
            content = file.getBytes();
            validateImage(content, ext.toLowerCase(), requireTransparency);
        } catch (IOException e) {
            throw new BusinessException("图片内容无法识别");
        }
        String subDir = DateUtil.format(new Date(), "yyyyMMdd");
        File dir = new File(storageDir, subDir).getAbsoluteFile();
        if (!dir.exists()) {
            FileUtil.mkdir(dir);
        }
        String fileName = IdUtil.fastSimpleUUID() + "." + ext.toLowerCase();
        File target = new File(dir, fileName);
        try {
            FileUtil.writeBytes(content, target);
        } catch (Exception e) {
            throw new BusinessException("图片保存失败，请重试");
        }
        return pathPrefix + subDir + "/" + fileName;
    }

    /** 将 UGC 图片标识解析为受配置根目录约束的文件资源。 */
    public Resource loadMaterialImage(String storedPath) {
        File file = storedPath != null && storedPath.startsWith(PRIVATE_STICKER_PREFIX)
                ? resolveStoredImage(privateUploadDir,
                        storedPath.substring(PRIVATE_STICKER_PREFIX.length()))
                : resolvePublicImage(storedPath);
        if (!file.isFile() || !file.canRead()) {
            throw new BusinessException(com.diy.nail.common.ResultCode.NOT_FOUND);
        }
        return new FileSystemResource(file);
    }

    /** 数据库写入失败时清理已经落盘的私有上传。 */
    public void deletePrivateImage(String storedPath) {
        if (storedPath != null && storedPath.startsWith(PRIVATE_STICKER_PREFIX)) {
            FileUtil.del(resolveStoredImage(privateUploadDir,
                    storedPath.substring(PRIVATE_STICKER_PREFIX.length())));
        }
    }

    private File resolvePublicImage(String storedPath) {
        if (storedPath == null || !storedPath.startsWith("/uploads/")) {
            throw new BusinessException(com.diy.nail.common.ResultCode.NOT_FOUND);
        }
        return resolveStoredImage(uploadDir, storedPath.substring("/uploads/".length()));
    }

    private File resolveStoredImage(String rootDir, String relativePath) {
        File root = new File(rootDir).getAbsoluteFile();
        try {
            File file = new File(root, relativePath).getAbsoluteFile();
            if (!file.toPath().normalize().startsWith(root.toPath().normalize())) {
                throw new BusinessException(com.diy.nail.common.ResultCode.FORBIDDEN);
            }
            return file;
        } catch (IllegalArgumentException | SecurityException e) {
            throw new BusinessException(com.diy.nail.common.ResultCode.FORBIDDEN);
        }
    }

    private void validateImage(byte[] content, String ext, boolean requireTransparency) throws IOException {
        boolean validHeader = switch (ext) {
            case "jpg", "jpeg" -> startsWith(content, 0xFF, 0xD8, 0xFF);
            case "png" -> startsWith(content, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "webp" -> startsWith(content, 0x52, 0x49, 0x46, 0x46)
                    && content.length >= 12
                    && content[8] == 0x57 && content[9] == 0x45
                    && content[10] == 0x42 && content[11] == 0x50;
            default -> false;
        };
        if (!validHeader) {
            throw new BusinessException("图片扩展名与实际内容不一致");
        }
        validateWithImageIo(content, requireTransparency);
    }

    private void validateWithImageIo(byte[] content, boolean requireTransparency) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new BusinessException("图片内容无法识别");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                validateDimensions(width, height);
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new BusinessException("图片内容无法识别");
                }
                if (requireTransparency && !hasTransparentPixel(image)) {
                    throw new BusinessException("贴纸 PNG 必须包含透明背景");
                }
            } finally {
                reader.dispose();
            }
        }
    }

    private void validateDimensions(int width, int height) {
        if (width < 1 || height < 1 || width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION) {
            throw new BusinessException("图片尺寸不能超过 4096 × 4096");
        }
    }

    private boolean hasTransparentPixel(BufferedImage image) {
        if (!image.getColorModel().hasAlpha()) {
            return false;
        }
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) < 255) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean startsWith(byte[] content, int... expected) {
        if (content.length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((content[i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }
}
