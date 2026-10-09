package com.diy.nail.service;

import com.diy.nail.common.BusinessException;
import com.diy.nail.entity.Material;
import com.diy.nail.mapper.DesignMapper;
import com.diy.nail.mapper.MaterialMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MaterialServiceSecurityTest {

    private final MaterialMapper materialMapper = mock(MaterialMapper.class);
    private final MaterialService service = new MaterialService(materialMapper, mock(DesignMapper.class));

    @Test
    void pendingStickerIsVisibleOnlyToOwnerOrOperator() {
        Material material = ugc(Material.AUDIT_PENDING, 0);
        when(materialMapper.selectById(5L)).thenReturn(material);

        assertEquals(material.getImageUrl(), service.imagePath(5L, 7L, false));
        assertEquals(material.getImageUrl(), service.imagePath(5L, null, true));
        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.imagePath(5L, 8L, false)).getCode());
    }

    @Test
    void rejectedStickerIsHiddenFromUploader() {
        when(materialMapper.selectById(5L)).thenReturn(ugc(Material.AUDIT_REJECTED, 0));

        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.imagePath(5L, 7L, false)).getCode());
    }

    @Test
    void approvedStickerIsPublic() {
        Material material = ugc(Material.AUDIT_APPROVED, 1);
        when(materialMapper.selectById(5L)).thenReturn(material);

        assertEquals(material.getImageUrl(), service.imagePath(5L, null, false));
    }

    private Material ugc(String auditStatus, int status) {
        Material material = new Material();
        material.setId(5L);
        material.setCreatorId(7L);
        material.setSource(Material.SOURCE_UGC);
        material.setAuditStatus(auditStatus);
        material.setStatus(status);
        material.setImageUrl("private://stickers/20261009/example.png");
        return material;
    }
}
