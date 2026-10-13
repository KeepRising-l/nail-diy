package com.diy.nail.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 运费规则视图：供前端结算页展示。
 *
 * <p>权威配置在 application.yml 的 nail.freight.*（后端计费时以此为准），
 * 此处仅做透出，避免前端硬编码一份规则导致「展示金额」与「实际计费」不一致。
 */
@Data
@AllArgsConstructor
public class FreightConfigVO {

    /** 邮寄固定运费（元） */
    private BigDecimal amount;

    /** 满额包邮门槛（元）：商品合计 ≥ 该值免运费 */
    private BigDecimal freeOver;
}
