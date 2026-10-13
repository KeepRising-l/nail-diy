package com.diy.nail.vo;

import com.diy.nail.entity.AfterSale;
import com.diy.nail.entity.Order;
import lombok.Data;

/**
 * 售后单视图：售后主体 + 订单概要（商家端需要看订单号与金额，顾客端需要看商品）。
 */
@Data
public class AfterSaleVO {

    /** 售后单主体 */
    private AfterSale afterSale;

    /** 关联订单（订单号、状态、金额、履约方式） */
    private Order order;

    /** 申请顾客昵称（商家端展示用） */
    private String userNickname;

    /** 申请顾客手机号（商家端展示用） */
    private String userPhone;
}
