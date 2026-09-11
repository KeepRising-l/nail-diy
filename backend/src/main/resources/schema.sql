-- =============================================================
-- DIY美甲定制平台 数据库初始化脚本（MySQL 8）
-- 使用方法：mysql -uroot -p < schema.sql  （或导入工具执行）
-- 数据库：nail_diy
-- =============================================================

CREATE DATABASE IF NOT EXISTS nail_diy DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE nail_diy;

-- -------------------------------------------------------------
-- 1. 用户表（顾客/商家/管理员，角色用字段区分）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `phone`       VARCHAR(20)  NOT NULL COMMENT '手机号（登录账号，唯一）',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密）',
    `nickname`    VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '昵称',
    `avatar`      VARCHAR(255) NOT NULL DEFAULT '' COMMENT '头像地址',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：USER顾客/MERCHANT商家/ADMIN管理员',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_role` (`role`)
) ENGINE = InnoDB COMMENT ='用户表';

-- -------------------------------------------------------------
-- 2. 门店表（自提点）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `store`;
CREATE TABLE `store` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`          VARCHAR(50)  NOT NULL COMMENT '门店名称',
    `address`       VARCHAR(255) NOT NULL COMMENT '门店地址',
    `phone`         VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '联系电话',
    `business_hours` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '营业时间',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1营业中 0停用',
    `sort`          INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB COMMENT ='自提门店表';

-- -------------------------------------------------------------
-- 3. 素材表（甲型/色卡/图案）—— 设计器的地基，type 字段是品类扩展点
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `material`;
CREATE TABLE `material` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `type`        VARCHAR(20)  NOT NULL COMMENT '素材类型：SHAPE甲型/COLOR色卡/PATTERN图案',
    `name`        VARCHAR(50)  NOT NULL COMMENT '素材名称',
    `image_url`   VARCHAR(255) NOT NULL DEFAULT '' COMMENT '图片路径（甲型/图案需要，色卡为空）',
    `color_code`  VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '颜色代码（色卡需要，如 #FFB6C1）',
    `category`    VARCHAR(30)  NOT NULL DEFAULT '' COMMENT '图案分类（BASIC/FRENCH/GRADIENT/NATURE/GEO/SHINE，甲型/色卡为空）',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1上架 0下架',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_type_status` (`type`, `status`)
) ENGINE = InnoDB COMMENT ='设计素材表';

-- -------------------------------------------------------------
-- 4. 款式模板表（商家预置样例，顾客可浏览并一键套用）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `template`;
CREATE TABLE `template` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`         VARCHAR(50)   NOT NULL COMMENT '模板名称',
    `style_tag`    VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '风格标签：可爱/轻奢/法式等',
    `cover_url`    VARCHAR(255)  NOT NULL DEFAULT '' COMMENT '封面图',
    `design_params` JSON         NOT NULL COMMENT '设计参数（默认甲型/色卡/图案组合，JSON）',
    `price`        DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '参考价格',
    `sort`         INT           NOT NULL DEFAULT 0 COMMENT '排序号',
    `status`       TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1上架 0下架',
    `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB COMMENT ='款式模板表';

-- -------------------------------------------------------------
-- 5. 设计表（用户 DIY 保存的设计）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `design`;
CREATE TABLE `design` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`      BIGINT        NOT NULL COMMENT '所属用户',
    `name`         VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '设计名称',
    `params`       JSON          NOT NULL COMMENT '设计参数 JSON（甲型id/色卡id/图案id集合/备注）',
    `preview_url`  VARCHAR(255)  NOT NULL DEFAULT '' COMMENT '预览快照图',
    `price`        DECIMAL(10,2) NOT NULL DEFAULT 39.00 COMMENT '单价（平台统一价）',
    `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB COMMENT ='用户DIY设计表';

-- -------------------------------------------------------------
-- 6. 购物车表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL COMMENT '用户',
    `design_id`   BIGINT       NOT NULL COMMENT '设计',
    `quantity`    INT          NOT NULL DEFAULT 1 COMMENT '数量',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_design` (`user_id`, `design_id`)
) ENGINE = InnoDB COMMENT ='购物车表';

-- -------------------------------------------------------------
-- 7. 收货地址表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `address`;
CREATE TABLE `address` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL COMMENT '用户',
    `receiver`    VARCHAR(50)  NOT NULL COMMENT '收货人',
    `phone`       VARCHAR(20)  NOT NULL COMMENT '联系电话',
    `province`    VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '省',
    `city`        VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '市',
    `district`    VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '区/县',
    `detail`      VARCHAR(255) NOT NULL COMMENT '详细地址',
    `is_default`  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认：1是 0否',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB COMMENT ='收货地址表';

-- -------------------------------------------------------------
-- 8. 订单表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no`       VARCHAR(32)   NOT NULL COMMENT '订单号（唯一）',
    `user_id`        BIGINT        NOT NULL COMMENT '下单用户',
    `delivery_type`  VARCHAR(20)   NOT NULL COMMENT '履约方式：SELF_PICKUP自提/EXPRESS邮寄',
    `store_id`       BIGINT        DEFAULT NULL COMMENT '自提门店（自提时有值）',
    `receiver_name`  VARCHAR(50)   DEFAULT NULL COMMENT '收货人（邮寄时有值）',
    `receiver_phone` VARCHAR(20)   DEFAULT NULL COMMENT '收货电话',
    `receiver_addr`  VARCHAR(255)  DEFAULT NULL COMMENT '收货地址全文（邮寄时有值）',
    `goods_amount`   DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '商品金额',
    `freight`        DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '运费',
    `total_amount`   DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '订单总额',
    `status`         VARCHAR(30)   NOT NULL DEFAULT 'PENDING_PAY' COMMENT '订单状态',
    `pickup_code`    VARCHAR(10)   NOT NULL DEFAULT '' COMMENT '自提码（进入待自提时生成）',
    `express_company` VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '快递公司',
    `express_no`     VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '物流单号',
    `remark`         VARCHAR(255)  NOT NULL DEFAULT '' COMMENT '顾客备注',
    `pay_time`       DATETIME      DEFAULT NULL COMMENT '支付时间',
    `finish_time`    DATETIME      DEFAULT NULL COMMENT '完成时间',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB COMMENT ='订单表';

-- -------------------------------------------------------------
-- 9. 订单明细表（设计快照）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`     BIGINT        NOT NULL COMMENT '订单',
    `design_id`    BIGINT        DEFAULT NULL COMMENT '关联设计（可空）',
    `design_name`  VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '设计名称快照',
    `design_params` JSON         DEFAULT NULL COMMENT '设计参数快照（商家按此制作）',
    `preview_url`  VARCHAR(255)  NOT NULL DEFAULT '' COMMENT '预览图快照',
    `price`        DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '单价快照',
    `quantity`     INT           NOT NULL DEFAULT 1 COMMENT '数量',
    `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`)
) ENGINE = InnoDB COMMENT ='订单明细表';

-- -------------------------------------------------------------
-- 10. 订单状态变更日志表（状态时间线）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `order_status_log`;
CREATE TABLE `order_status_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`    BIGINT       NOT NULL COMMENT '订单',
    `from_status` VARCHAR(30)  NOT NULL DEFAULT '' COMMENT '原状态',
    `to_status`   VARCHAR(30)  NOT NULL COMMENT '新状态',
    `operator`    VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '操作人（用户/商家）',
    `remark`      VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`)
) ENGINE = InnoDB COMMENT ='订单状态日志表';

-- -------------------------------------------------------------
-- 11. 评价表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`    BIGINT       NOT NULL COMMENT '订单（唯一，每单一次）',
    `user_id`     BIGINT       NOT NULL COMMENT '评价用户',
    `rating`      TINYINT      NOT NULL DEFAULT 5 COMMENT '评分 1-5',
    `content`     VARCHAR(500) NOT NULL DEFAULT '' COMMENT '评价内容',
    `images`      JSON         DEFAULT NULL COMMENT '晒图（最多3张）',
    `reply`       VARCHAR(500) NOT NULL DEFAULT '' COMMENT '商家回复',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '删除（管理员删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order` (`order_id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB COMMENT ='评价表';

-- -------------------------------------------------------------
-- 12. 收藏表（模板收藏）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT   NOT NULL COMMENT '用户',
    `template_id` BIGINT   NOT NULL COMMENT '模板',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_tpl` (`user_id`, `template_id`)
) ENGINE = InnoDB COMMENT ='收藏表';

-- -------------------------------------------------------------
-- 13. 公告表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`       VARCHAR(100) NOT NULL COMMENT '标题',
    `content`     TEXT         NOT NULL COMMENT '内容',
    `is_top`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否置顶：1是 0否',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1发布 0下线',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB COMMENT ='公告表';
