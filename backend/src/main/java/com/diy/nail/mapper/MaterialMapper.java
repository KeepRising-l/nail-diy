package com.diy.nail.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.diy.nail.entity.Material;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 设计素材 Mapper
 */
@Mapper
public interface MaterialMapper extends BaseMapper<Material> {

    /** 仅待审核 UGC 可被审核，防止两个审核请求互相覆盖。 */
    @Update("""
            UPDATE material
            SET audit_status = #{auditStatus}, status = #{status}
            WHERE id = #{id} AND source = 'UGC' AND audit_status = 'PENDING' AND deleted = 0
            """)
    int auditPending(@Param("id") Long id,
                     @Param("auditStatus") String auditStatus,
                     @Param("status") int status);

    /** 查询需要保护的旧静态 UGC 地址，包含已逻辑删除的记录。 */
    @Select("""
            SELECT id, creator_id, deleted
            FROM material
            WHERE image_url = #{imageUrl}
              AND source = 'UGC'
              AND (deleted = 1 OR audit_status <> 'APPROVED' OR status <> 1)
            ORDER BY deleted DESC
            LIMIT 1
            """)
    Material selectProtectedLegacyImage(@Param("imageUrl") String imageUrl);
}
