package com.diy.nail.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.diy.nail.entity.AfterSale;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 售后申请 Mapper。
 */
@Mapper
public interface AfterSaleMapper extends BaseMapper<AfterSale> {

    /**
     * 仅待处理的售后单可被处理，防止两个审核请求互相覆盖（并发防护）。
     *
     * @return 影响行数，1 表示本次处理生效
     */
    @Update("""
            UPDATE after_sale
            SET status = #{status}, reject_reason = #{rejectReason}, handle_time = NOW()
            WHERE id = #{id} AND status = 'PENDING' AND deleted = 0
            """)
    int handlePending(@Param("id") Long id,
                      @Param("status") String status,
                      @Param("rejectReason") String rejectReason);
}
