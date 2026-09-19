package com.diy.nail.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 管理端看板聚合查询：只读聚合，不走 BaseMapper
 */
@Mapper
public interface AdminMapper {

    /** 用户角色分布：role -> count */
    @Select("SELECT role AS k, COUNT(*) AS c FROM user WHERE deleted = 0 GROUP BY role")
    List<Map<String, Object>> roleDist();

    /** 待审核 UGC 贴纸数（顾客上传、尚未审核） */
    @Select("SELECT COUNT(*) FROM material WHERE deleted = 0 AND source = 'UGC' AND audit_status = 'PENDING'")
    Long countPendingUgc();

    /** 热门模板 TOP5（按收藏数，同数按模板 id 倒序） */
    @Select("SELECT t.name AS k, COUNT(*) AS c FROM favorite f"
            + " JOIN template t ON f.template_id = t.id AND t.deleted = 0"
            + " GROUP BY f.template_id, t.name"
            + " ORDER BY c DESC, f.template_id DESC LIMIT 5")
    List<Map<String, Object>> hotTemplates();
}
