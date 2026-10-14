package com.diy.nail.service;

import com.diy.nail.common.BusinessException;
import com.diy.nail.common.ResultCode;
import com.diy.nail.dto.DesignRequest;
import com.diy.nail.entity.Design;
import com.diy.nail.entity.Material;
import com.diy.nail.mapper.DesignMapper;
import com.diy.nail.mapper.MaterialMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 设计保存的参数校验测试。
 *
 * <p>重点覆盖「颜色有两种来源」这一分支：色卡（colorId）与自定义色值（colorCode）。
 * 早期版本只认 colorId，导致顾客在设计台用滑条/HEX 改色后无法保存设计（已修复的缺陷），
 * 其中 savesDesignWithCustomColorCode 即为该缺陷的回归保护。
 */
class DesignServiceTest {

    private static final Long USER = 7L;
    private static final Long SHAPE = 96L;
    private static final Long COLOR = 16L;

    private final DesignMapper designMapper = mock(DesignMapper.class);
    private final MaterialMapper materialMapper = mock(MaterialMapper.class);
    private final DesignService service = new DesignService(designMapper, materialMapper);

    // ---------- 辅助 ----------

    private Material material(Long id, String type, int status) {
        Material m = new Material();
        m.setId(id);
        m.setType(type);
        m.setStatus(status);
        m.setAuditStatus(Material.AUDIT_APPROVED);
        m.setHeat(0);
        return m;
    }

    private void stubApprovedMaterials() {
        when(materialMapper.selectById(SHAPE)).thenReturn(material(SHAPE, Material.TYPE_SHAPE, 1));
        when(materialMapper.selectById(COLOR)).thenReturn(material(COLOR, Material.TYPE_COLOR, 1));
    }

    /** 让 insert 回填主键，模拟 MyBatis-Plus 的行为 */
    private void stubInsertReturnsId(Long id) {
        when(designMapper.insert(any(Design.class))).thenAnswer(inv -> {
            inv.<Design>getArgument(0).setId(id);
            return 1;
        });
    }

    private DesignRequest request(String params) {
        DesignRequest r = new DesignRequest();
        r.setName("测试设计");
        r.setParams(params);
        r.setPreviewUrl("");
        return r;
    }

    // ---------- 正常保存 ----------

    @Test
    void savesDesignWithColorCard() {
        stubApprovedMaterials();
        stubInsertReturnsId(99L);

        Long id = service.create(USER, request("{\"fingers\":[{\"shapeId\":96,\"colorId\":16}]}"));

        assertEquals(99L, id);
    }

    @Test
    void savesDesignWithCustomColorCode() {
        // 回归保护：自定义色（colorId 为空、只有 colorCode）必须可保存
        stubApprovedMaterials();
        stubInsertReturnsId(100L);

        Long id = service.create(USER, request(
                "{\"fingers\":[{\"shapeId\":96,\"colorId\":null,\"colorCode\":\"#AABBCC\"}]}"));

        assertEquals(100L, id);
    }

    @Test
    void savesDesignWithMixedFingers() {
        // 一指用色卡、一指用自定义色、一指留空 —— 应全部接受
        stubApprovedMaterials();
        stubInsertReturnsId(101L);

        Long id = service.create(USER, request("{\"fingers\":["
                + "{\"shapeId\":96,\"colorId\":16},"
                + "{\"shapeId\":96,\"colorId\":null,\"colorCode\":\"#112233\"},"
                + "{\"shapeId\":null,\"colorId\":null,\"colorCode\":\"\"}]}"));

        assertEquals(101L, id);
    }

    @Test
    void supportsLegacySingleShapeStructure() {
        // 旧结构（无 fingers 数组）仍应支持：顶层 shapeId + colorId
        stubApprovedMaterials();
        stubInsertReturnsId(102L);

        Long id = service.create(USER, request("{\"shapeId\":96,\"colorId\":16}"));

        assertEquals(102L, id);
    }

    // ---------- 参数校验 ----------

    @Test
    void rejectsDesignWithoutShape() {
        stubApprovedMaterials();

        BusinessException e = assertThrows(BusinessException.class, () -> service.create(USER,
                request("{\"fingers\":[{\"shapeId\":null,\"colorId\":16}]}")));

        assertEquals("每根手指需选择甲型", e.getMessage());
    }

    @Test
    void rejectsDesignWithoutAnyColor() {
        stubApprovedMaterials();

        BusinessException e = assertThrows(BusinessException.class, () -> service.create(USER,
                request("{\"fingers\":[{\"shapeId\":96,\"colorId\":null,\"colorCode\":\"\"}]}")));

        assertEquals("每根手指需选择颜色（色卡或自定义色值）", e.getMessage());
    }

    @Test
    void rejectsInvalidColorCodeFormat() {
        stubApprovedMaterials();

        BusinessException e = assertThrows(BusinessException.class, () -> service.create(USER,
                request("{\"fingers\":[{\"shapeId\":96,\"colorCode\":\"red\"}]}")));

        assertEquals("自定义颜色格式不正确，应为 #RRGGBB", e.getMessage());
    }

    @Test
    void rejectsWhenNoFingerConfigured() {
        BusinessException e = assertThrows(BusinessException.class, () -> service.create(USER,
                request("{\"fingers\":[{\"shapeId\":null,\"colorId\":null,\"colorCode\":\"\"}]}")));

        assertEquals("请至少为一根手指选择甲型和颜色", e.getMessage());
    }

    @Test
    void rejectsOfflineShapeMaterial() {
        when(materialMapper.selectById(SHAPE)).thenReturn(material(SHAPE, Material.TYPE_SHAPE, 0));

        BusinessException e = assertThrows(BusinessException.class, () -> service.create(USER,
                request("{\"fingers\":[{\"shapeId\":96,\"colorId\":16}]}")));

        assertEquals("所选素材已下架或不存在，请重新选择", e.getMessage());
    }

    @Test
    void rejectsMaterialOfWrongType() {
        // 把色卡 id 填进 shapeId：类型不匹配必须被拦住
        when(materialMapper.selectById(SHAPE)).thenReturn(material(SHAPE, Material.TYPE_COLOR, 1));

        assertThrows(BusinessException.class, () -> service.create(USER,
                request("{\"fingers\":[{\"shapeId\":96,\"colorId\":16}]}")));
    }

    @Test
    void rejectsNonJsonParams() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.create(USER, request("not-a-json")));

        assertEquals("设计参数必须是合法的 JSON 对象", e.getMessage());
    }

    @Test
    void rejectsFingersNotArray() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.create(USER, request("{\"fingers\":\"oops\"}")));

        assertEquals("设计参数 fingers 必须是对象数组", e.getMessage());
    }

    // ---------- 归属校验 ----------

    @Test
    void deleteRejectsOtherUsersDesign() {
        Design other = new Design();
        other.setId(5L);
        other.setUserId(8L);
        when(designMapper.selectById(5L)).thenReturn(other);

        BusinessException e = assertThrows(BusinessException.class, () -> service.delete(USER, 5L));

        assertEquals(ResultCode.FORBIDDEN.getCode(), e.getCode());
        verify(designMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void detailThrowsNotFoundForMissingDesign() {
        when(designMapper.selectById(404L)).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class, () -> service.detail(USER, 404L));

        assertEquals(ResultCode.NOT_FOUND.getCode(), e.getCode());
    }
}
