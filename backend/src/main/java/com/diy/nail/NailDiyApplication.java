package com.diy.nail;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DIY美甲定制平台 启动类
 */
@SpringBootApplication
@MapperScan("com.diy.nail.mapper")
public class NailDiyApplication {

    public static void main(String[] args) {
        SpringApplication.run(NailDiyApplication.class, args);
    }
}
