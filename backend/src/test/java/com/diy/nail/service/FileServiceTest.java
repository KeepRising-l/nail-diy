package com.diy.nail.service;

import com.diy.nail.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileServiceTest {

    @TempDir
    Path uploadDir;

    @Test
    void validPngIsStoredUnderConfiguredDirectory() throws Exception {
        FileService service = service();
        String path = service.uploadImage(file("preview.png", png(2, 2)));

        assertTrue(path.matches("/uploads/\\d{8}/[a-f0-9]{32}\\.png"));
        assertTrue(Files.isRegularFile(uploadDir.resolve("public")
                .resolve(path.substring("/uploads/".length()))));
    }

    @Test
    void pendingStickerIsStoredOutsidePublicDirectory() throws Exception {
        FileService service = service();
        String path = service.uploadPrivatePngImage(file("sticker.png", png(2, 2)));

        assertTrue(path.matches("private://stickers/\\d{8}/[a-f0-9]{32}\\.png"));
        assertTrue(service.loadMaterialImage(path).exists());
        assertTrue(Files.notExists(uploadDir.resolve("public")));
    }

    @Test
    void extensionMustMatchDecodableImageContent() throws Exception {
        FileService service = service();

        assertThrows(BusinessException.class,
                () -> service.uploadImage(file("preview.jpg", png(1, 1))));
        assertThrows(BusinessException.class,
                () -> service.uploadImage(file("preview.png", new byte[]{
                        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})));
    }

    @Test
    void stickerUploadOnlyAcceptsPng() throws Exception {
        FileService service = service();
        MockMultipartFile jpeg = new MockMultipartFile("file", "sticker.jpg", "image/jpeg", jpeg());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.uploadPrivatePngImage(jpeg));

        assertEquals("贴纸仅支持 PNG 格式", error.getMessage());
    }

    @Test
    void stickerPngMustContainTransparentPixels() throws Exception {
        FileService service = service();
        BufferedImage opaque = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.uploadPrivatePngImage(file("sticker.png", encode(opaque, "png"))));

        assertEquals("贴纸 PNG 必须包含透明背景", error.getMessage());
    }

    @Test
    void oversizedImageDimensionsAreRejected() throws Exception {
        FileService service = service();

        assertThrows(BusinessException.class,
                () -> service.uploadImage(file("wide.png", png(4097, 1))));
    }

    @Test
    void oversizedFileAndPathTraversalAreRejected() {
        FileService service = service();

        assertThrows(BusinessException.class,
                () -> service.uploadImage(file("large.png", new byte[5 * 1024 * 1024 + 1])));
        assertThrows(BusinessException.class,
                () -> service.loadMaterialImage("private://stickers/../outside.png"));
    }

    private FileService service() {
        FileService service = new FileService();
        ReflectionTestUtils.setField(service, "uploadDir", uploadDir.resolve("public").toString());
        ReflectionTestUtils.setField(service, "privateUploadDir", uploadDir.resolve("private").toString());
        return service;
    }

    private MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, "image/png", content);
    }

    private byte[] png(int width, int height) throws Exception {
        return encode(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png");
    }

    private byte[] jpeg() throws Exception {
        return encode(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "jpg");
    }

    private byte[] encode(BufferedImage image, String format) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }
}
