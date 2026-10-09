package com.diy.nail.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.diy.nail.entity.Cart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 购物车 Mapper
 */
@Mapper
public interface CartMapper extends BaseMapper<Cart> {

    /** 原子累加已有条目；不存在或累加后超过上限时返回 0。 */
    @Update("""
            UPDATE cart
            SET quantity = quantity + #{quantity}
            WHERE user_id = #{userId} AND design_id = #{designId}
              AND quantity BETWEEN 1 AND 99 - #{quantity}
            """)
    int incrementQuantity(@Param("userId") Long userId,
                          @Param("designId") Long designId,
                          @Param("quantity") int quantity);

    /** 结算后只删除本次锁定的购物车快照，保留并发新增条目。 */
    @Delete("""
            <script>
            DELETE FROM cart
            WHERE user_id = #{userId} AND id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
            </script>
            """)
    int deleteCheckedOut(@Param("userId") Long userId, @Param("ids") List<Long> ids);
}
