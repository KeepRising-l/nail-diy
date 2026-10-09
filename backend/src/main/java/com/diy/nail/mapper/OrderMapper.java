package com.diy.nail.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.diy.nail.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 订单 Mapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /** 仅当旧状态仍匹配时更新订单，防止并发状态迁移互相覆盖。 */
    @Update("""
            <script>
            UPDATE orders
            SET status = #{toStatus}
            <if test="payTime != null">, pay_time = #{payTime}</if>
            <if test="finishTime != null">, finish_time = #{finishTime}</if>
            <if test="pickupCode != null">, pickup_code = #{pickupCode}</if>
            <if test="expressCompany != null">, express_company = #{expressCompany}</if>
            <if test="expressNo != null">, express_no = #{expressNo}</if>
            WHERE id = #{id} AND status = #{fromStatus} AND deleted = 0
            </script>
            """)
    int transitionStatus(@Param("id") Long id,
                         @Param("fromStatus") String fromStatus,
                         @Param("toStatus") String toStatus,
                         @Param("payTime") LocalDateTime payTime,
                         @Param("finishTime") LocalDateTime finishTime,
                         @Param("pickupCode") String pickupCode,
                         @Param("expressCompany") String expressCompany,
                         @Param("expressNo") String expressNo);
}
