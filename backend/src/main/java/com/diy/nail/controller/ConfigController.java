package com.diy.nail.controller;

import com.diy.nail.common.Result;
import com.diy.nail.vo.FreightConfigVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 公开配置接口：把平台规则透出给前端，避免在客户端硬编码一份副本。
 * 目前只包含运费规则（结算页展示用）。
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    /** 邮寄固定运费（元），权威配置见 application.yml 的 nail.freight.amount */
    @Value("${nail.freight.amount}")
    private BigDecimal freightAmount;

    /** 满额包邮门槛（元），权威配置见 application.yml 的 nail.freight.free-over */
    @Value("${nail.freight.free-over}")
    private BigDecimal freightFreeOver;

    /** 运费规则（不敏感，游客可读；后端计费仍以配置为准） */
    @GetMapping("/freight")
    public Result<FreightConfigVO> freight() {
        return Result.ok(new FreightConfigVO(freightAmount, freightFreeOver));
    }
}
