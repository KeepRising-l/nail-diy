package com.diy.nail.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.diy.nail.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 消息通知 Mapper。
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /** 把某用户的所有未读通知标记为已读 */
    @Update("""
            UPDATE notification
            SET is_read = 1, read_time = NOW()
            WHERE user_id = #{userId} AND is_read = 0 AND deleted = 0
            """)
    int markAllRead(@Param("userId") Long userId);
}
