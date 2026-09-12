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
-- 3. 素材表 —— 设计器的地基，type 字段是品类扩展点
--    SHAPE/PATTERN_STYLE/DECOR(矢量) 用 code 绑定前端渲染器，无需上传图片；
--    PATTERN_IMG/STICKER/DECOR(立体) 为图片素材；COLOR 只有 color_code
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `material`;
CREATE TABLE `material` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `type`        VARCHAR(20)  NOT NULL COMMENT '素材类型：SHAPE甲型/COLOR色卡/PATTERN_IMG贴图/PATTERN_STYLE参数化款式/DECOR装饰件/STICKER贴纸',
    `code`        VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '代码标识（SHAPE/PATTERN_STYLE/矢量DECOR 绑定前端渲染器，同类型内唯一）',
    `name`        VARCHAR(50)  NOT NULL COMMENT '素材名称',
    `image_url`   VARCHAR(255) NOT NULL DEFAULT '' COMMENT '图片路径（贴图/贴纸/立体装饰需要）',
    `color_code`  VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '颜色代码（色卡需要，如 #FFB6C1）',
    `category`    VARCHAR(30)  NOT NULL DEFAULT '' COMMENT '分类（贴图：BASIC/NATURE/GEO/CUTE/SHINE；色卡：色系；甲型：长度系列）',
    `tags`        VARCHAR(255) NOT NULL DEFAULT '' COMMENT '检索标签，逗号分隔',
    `attrs`       JSON         DEFAULT NULL COMMENT '类型专属属性：甲型{series,length}；色卡{finish}；参数化款式{paramsSchema,defaults}；装饰{material}',
    `source`      VARCHAR(20)  NOT NULL DEFAULT 'PLATFORM' COMMENT '来源：PLATFORM平台/UGC用户上传/AI生成',
    `creator_id`  BIGINT       DEFAULT NULL COMMENT 'UGC 来源用户（平台素材为 NULL）',
    `audit_status` VARCHAR(20) NOT NULL DEFAULT 'APPROVED' COMMENT '审核状态：PENDING待审/APPROVED通过/REJECTED驳回',
    `heat`        INT          NOT NULL DEFAULT 0 COMMENT '热度（被设计引用次数）',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1上架 0下架',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_type_status` (`type`, `status`),
    KEY `idx_source_audit` (`source`, `audit_status`),
    KEY `idx_heat` (`heat`)
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

-- =============================================================
-- 14. 素材 Seed 数据（新环境一键起库即可演示）
--     SHAPE/PATTERN_STYLE/DECOR 为代码注册素材（code 绑定前端渲染器，无图）
-- =============================================================

-- 14.1 甲型（8 种纯轮廓，code 对应前端 NailArt CONTOURS；长度是设计器里的独立维度，不入甲型）
INSERT INTO `material` (`type`, `code`, `name`, `image_url`, `category`, `attrs`, `sort`) VALUES
('SHAPE', 'round',     '圆形',     '',  '', NULL, 1),
('SHAPE', 'squoval',   '方圆形',   '',  '', NULL, 2),
('SHAPE', 'square',    '方形',     '',  '', NULL, 3),
('SHAPE', 'oval',      '椭圆形',   '',  '', NULL, 4),
('SHAPE', 'almond',    '杏仁形',   '',  '', NULL, 5),
('SHAPE', 'ballerina', '芭蕾形',   '',  '', NULL, 6),
('SHAPE', 'stiletto',  '尖形',     '',  '', NULL, 7),
('SHAPE', 'russian',   '俄罗斯方', '',  '', NULL, 8);

-- 14.2 预设色卡（30 色，category 为色系，attrs.finish 为质感：SOLID纯色/JELLY冰透/GLITTER碎钻/CAT_EYE猫眼）
INSERT INTO `material` (`type`, `name`, `color_code`, `category`, `attrs`, `sort`) VALUES
('COLOR', '奶霜白',   '#F5E6DA', 'NUDE',   JSON_OBJECT('finish','JELLY'),   1),
('COLOR', '裸杏',     '#EFD5C8', 'NUDE',   JSON_OBJECT('finish','SOLID'),   2),
('COLOR', '肉桂拿铁', '#E8C4B8', 'NUDE',   JSON_OBJECT('finish','SOLID'),   3),
('COLOR', '焦糖奶咖', '#DDB0A0', 'NUDE',   JSON_OBJECT('finish','SOLID'),   4),
('COLOR', '樱花粉',   '#F8C8DC', 'PINK',   JSON_OBJECT('finish','JELLY'),   5),
('COLOR', '蜜桃粉',   '#F2A7C3', 'PINK',   JSON_OBJECT('finish','SOLID'),   6),
('COLOR', '干枯玫瑰', '#E08BB0', 'PINK',   JSON_OBJECT('finish','SOLID'),   7),
('COLOR', '梅子粉',   '#C9739A', 'PINK',   JSON_OBJECT('finish','SOLID'),   8),
('COLOR', '芭蕾粉',   '#FADCE4', 'PINK',   JSON_OBJECT('finish','JELLY'),   9),
('COLOR', '酒红',     '#8C4435', 'RED',    JSON_OBJECT('finish','SOLID'),   10),
('COLOR', '正红',     '#C8443C', 'RED',    JSON_OBJECT('finish','SOLID'),   11),
('COLOR', '枫叶红',   '#B03A2E', 'RED',    JSON_OBJECT('finish','SOLID'),   12),
('COLOR', '南瓜橘',   '#D9622B', 'RED',    JSON_OBJECT('finish','SOLID'),   13),
('COLOR', '杏子橘',   '#E8873A', 'RED',    JSON_OBJECT('finish','JELLY'),   14),
('COLOR', '鹅黄',     '#E6D38A', 'YELLOW', JSON_OBJECT('finish','SOLID'),   15),
('COLOR', '芥末黄',   '#C9C25E', 'YELLOW', JSON_OBJECT('finish','SOLID'),   16),
('COLOR', '牛油果绿', '#A8B86E', 'GREEN',  JSON_OBJECT('finish','SOLID'),   17),
('COLOR', '薄荷绿',   '#9CCDB8', 'GREEN',  JSON_OBJECT('finish','JELLY'),   18),
('COLOR', '松石绿',   '#4E9A94', 'TEAL',   JSON_OBJECT('finish','SOLID'),   19),
('COLOR', '雾霾蓝',   '#7FA8C9', 'TEAL',   JSON_OBJECT('finish','JELLY'),   20),
('COLOR', '牛仔蓝',   '#7A8FC4', 'BLUE',   JSON_OBJECT('finish','SOLID'),   21),
('COLOR', '藏青',     '#5A6FA8', 'BLUE',   JSON_OBJECT('finish','SOLID'),   22),
('COLOR', '葡萄紫',   '#6B4A7A', 'PURPLE', JSON_OBJECT('finish','SOLID'),   23),
('COLOR', '深紫罗兰', '#4A3560', 'PURPLE', JSON_OBJECT('finish','SOLID'),   24),
('COLOR', '纯黑',     '#2B2B2E', 'DARK',   JSON_OBJECT('finish','SOLID'),   25),
('COLOR', '奶白',     '#FAFAF7', 'SILVER', JSON_OBJECT('finish','SOLID'),   26),
('COLOR', '月光银',   '#C0C4CC', 'SILVER', JSON_OBJECT('finish','GLITTER'), 27),
('COLOR', '银河灰',   '#A8ACB8', 'SILVER', JSON_OBJECT('finish','GLITTER'), 28),
('COLOR', '碎钻粉晶', '#E8D5E0', 'GLITTER',JSON_OBJECT('finish','GLITTER'), 29),
('COLOR', '碎钻冰蓝', '#C8DCE8', 'GLITTER',JSON_OBJECT('finish','GLITTER'), 30);

-- 14.3 参数化款式（10 种渲染算法，attrs.paramsSchema 描述可调参数，前端引擎按 code 渲染）
INSERT INTO `material` (`type`, `code`, `name`, `category`, `tags`, `attrs`, `sort`) VALUES
('PATTERN_STYLE', 'FRENCH',  '法式微笑线', 'FRENCH',    '法式,经典,百搭', JSON_OBJECT('paramsSchema', JSON_ARRAY(JSON_OBJECT('key','color','label','微笑线颜色','type','color','default','#FFFFFF'), JSON_OBJECT('key','height','label','指尖高度','type','range','min',8,'max',40,'default',26), JSON_OBJECT('key','curvature','label','弧度','type','range','min',0,'max',100,'default',72))), 1),
('PATTERN_STYLE', 'OMBRE',   '渐变晕染',   'GRADIENTS', '渐变,晕染', JSON_OBJECT('paramsSchema', JSON_ARRAY(JSON_OBJECT('key','color2','label','渐变辅色','type','color','default','#FFFFFF'), JSON_OBJECT('key','position','label','过渡位置','type','range','min',0,'max',100,'default',50))), 2),
('PATTERN_STYLE', 'DOTS',    '波点',       'BASICS',    '波点,可爱,复古', JSON_OBJECT('paramsSchema', JSON_ARRAY(JSON_OBJECT('key','density','label','密度','type','range','min',3,'max',15,'default',7), JSON_OBJECT('key','size','label','点径','type','range','min',1,'max',5,'default',2.5), JSON_OBJECT('key','color','label','点色(留空自动)','type','color','default',''))), 3),
('PATTERN_STYLE', 'LEOPARD', '豹纹',       'NATURE',    '豹纹,动物纹,辣妹', JSON_OBJECT('paramsSchema', JSON_ARRAY(JSON_OBJECT('key','density','label','密度','type','range','min',3,'max',15,'default',7), JSON_OBJECT('key','size','label','斑径','type','range','min',1,'max',5,'default',2.5), JSON_OBJECT('key','color','label','斑色(留空自动)','type','color','default',''))), 4),
('PATTERN_STYLE', 'MARBLE',  '大理石纹',   'NATURE',    '大理石,轻奢,高级', JSON_OBJECT('paramsSchema', JSON_ARRAY(JSON_OBJECT('key','veinColor','label','纹路颜色','type','color','default','#FFFFFF'), JSON_OBJECT('key','density','label','纹路密度','type','range','min',2,'max',8,'default',4), JSON_OBJECT('key','thickness','label','纹路粗细','type','range','min',1,'max',4,'default',1.6))), 5),
('PATTERN_STYLE', 'TORTOISE','玳瑁琥珀',   'NATURE',    '玳瑁,琥珀,复古', JSON_OBJECT('paramsSchema', JSON_ARRAY(JSON_OBJECT('key','spotColor','label','斑纹颜色','type','color','default','#4A2C17'), JSON_OBJECT('key','density','label','斑块密度','type','range','min',3,'max',10,'default',6))), 6),
('PATTERN_STYLE', 'AURA',    '极光晕染',   'GRADIENTS', '极光,晕染,氛围感', JSON_OBJECT('paramsSchema', JSON_ARRAY(JSON_OBJECT('key','centerColor','label','光晕颜色','type','color','default','#F8C8DC'), JSON_OBJECT('key','radius','label','光晕范围','type','range','min',20,'max',70,'default',45), JSON_OBJECT('key','cy','label','光晕位置','type','range','min',10,'max',90,'default',45))), 7),
('PATTERN_STYLE', 'SWIRLS',  '复古曲线',   'BASICS',    '曲线,复古,手绘', JSON_OBJECT('paramsSchema', JSON_ARRAY(JSON_OBJECT('key','lineColor','label','线条颜色','type','color','default','#FFFFFF'), JSON_OBJECT('key','count','label','线条数','type','range','min',2,'max',6,'default',3), JSON_OBJECT('key','width','label','线宽','type','range','min',1,'max',3,'default',1.4))), 8),
('PATTERN_STYLE', 'GEO',     '几何拼色',   'BASICS',    '几何,拼色,简约', JSON_OBJECT('paramsSchema', JSON_ARRAY(JSON_OBJECT('key','color2','label','拼色','type','color','default','#FFFFFF'), JSON_OBJECT('key','mode','label','分割方式','type','select','options',JSON_ARRAY('half','diagonal','tip'),'default','diagonal'))), 9),
('PATTERN_STYLE', 'STARS',   '星星点缀',   'BASICS',    '星星,梦幻,点缀', JSON_OBJECT('paramsSchema', JSON_ARRAY(JSON_OBJECT('key','color','label','星色(留空自动)','type','color','default',''), JSON_OBJECT('key','count','label','数量','type','range','min',2,'max',8,'default',4), JSON_OBJECT('key','size','label','星径','type','range','min',1.5,'max',4,'default',2.4))), 10);

-- 14.4 矢量装饰件（code 绑定前端 DECO_PATHS；attrs.material 为材质分类）
INSERT INTO `material` (`type`, `code`, `name`, `category`, `attrs`, `sort`) VALUES
('DECOR', 'star',     '星形钻',   'GEM',   JSON_OBJECT('material','水钻'), 1),
('DECOR', 'round',    '圆钻',     'GEM',   JSON_OBJECT('material','水钻'), 2),
('DECOR', 'drop',     '水滴钻',   'GEM',   JSON_OBJECT('material','水钻'), 3),
('DECOR', 'marquise', '马眼钻',   'GEM',   JSON_OBJECT('material','水钻'), 4),
('DECOR', 'oval',     '椭圆钻',   'GEM',   JSON_OBJECT('material','水钻'), 5),
('DECOR', 'hex',      '六边钻',   'GEM',   JSON_OBJECT('material','水钻'), 6),
('DECOR', 'triangle', '三角钻',   'GEM',   JSON_OBJECT('material','水钻'), 7),
('DECOR', 'heart',    '爱心钻',   'GEM',   JSON_OBJECT('material','水钻'), 8),
('DECOR', 'bar',      '长条金属', 'METAL', JSON_OBJECT('material','金属'), 9),
('DECOR', 'pearl',    '珍珠',     'PEARL', JSON_OBJECT('material','珍珠'), 10);

-- 14.5 贴图素材（31 张 seed PNG，category：NATURE自然/GEO几何/CUTE可爱/SHINE闪亮）
INSERT INTO `material` (`type`, `name`, `image_url`, `category`, `tags`, `sort`) VALUES
('PATTERN_IMG', '蝴蝶结', '/uploads/seed/pat_bow.png',        'CUTE',   '蝴蝶结,可爱', 1),
('PATTERN_IMG', '爱心',   '/uploads/seed/pat_heart.png',      'CUTE',   '爱心,甜妹', 2),
('PATTERN_IMG', '糖果',   '/uploads/seed/pat_candy.png',      'CUTE',   '糖果,甜', 3),
('PATTERN_IMG', '草莓',   '/uploads/seed/pat_strawberry.png', 'CUTE',   '草莓,水果', 4),
('PATTERN_IMG', '樱桃',   '/uploads/seed/pat_cherry.png',     'CUTE',   '樱桃,水果', 5),
('PATTERN_IMG', '玫瑰',   '/uploads/seed/pat_rose.png',       'CUTE',   '玫瑰,花,浪漫', 6),
('PATTERN_IMG', '蝴蝶',   '/uploads/seed/pat_butterfly.png',  'NATURE', '蝴蝶,仙气', 7),
('PATTERN_IMG', '小花',   '/uploads/seed/pat_flower.png',     'NATURE', '花,碎花', 8),
('PATTERN_IMG', '樱花',   '/uploads/seed/pat_sakura.png',     'NATURE', '樱花,日系', 9),
('PATTERN_IMG', '叶子',   '/uploads/seed/pat_leaf.png',       'NATURE', '叶子,清新', 10),
('PATTERN_IMG', '云朵',   '/uploads/seed/pat_cloud.png',      'NATURE', '云朵,天空', 11),
('PATTERN_IMG', '月亮',   '/uploads/seed/pat_moon.png',       'NATURE', '月亮,夜空', 12),
('PATTERN_IMG', '星星',   '/uploads/seed/pat_star.png',       'NATURE', '星星,夜空', 13),
('PATTERN_IMG', '雪花',   '/uploads/seed/pat_snow.png',       'NATURE', '雪花,圣诞,冬', 14),
('PATTERN_IMG', '彩虹',   '/uploads/seed/pat_rainbow.png',    'NATURE', '彩虹,活泼', 15),
('PATTERN_IMG', '西瓜',   '/uploads/seed/pat_melon.png',      'NATURE', '西瓜,水果,夏', 16),
('PATTERN_IMG', '波浪',   '/uploads/seed/pat_wave.png',       'NATURE', '波浪,海洋', 17),
('PATTERN_IMG', '波点贴', '/uploads/seed/pat_dots.png',       'GEO',    '波点,复古', 18),
('PATTERN_IMG', '格纹',   '/uploads/seed/pat_gingham.png',    'GEO',    '格纹,学院', 19),
('PATTERN_IMG', '条纹',   '/uploads/seed/pat_stripes.png',    'GEO',    '条纹,简约', 20),
('PATTERN_IMG', '圆环',   '/uploads/seed/pat_ring.png',       'GEO',    '圆环,几何', 21),
('PATTERN_IMG', '彩旗',   '/uploads/seed/pat_flags.png',      'GEO',    '彩旗,派对', 22),
('PATTERN_IMG', '金箔',   '/uploads/seed/pat_foil.png',       'SHINE',  '金箔,轻奢', 23),
('PATTERN_IMG', '珍珠贴', '/uploads/seed/pat_pearl.png',      'SHINE',  '珍珠,优雅', 24),
('PATTERN_IMG', '铆钉',   '/uploads/seed/pat_stud.png',       'SHINE',  '铆钉,朋克', 25),
('PATTERN_IMG', '星光',   '/uploads/seed/pat_sparkle.png',    'SHINE',  '星光,闪', 26),
('PATTERN_IMG', '闪电',   '/uploads/seed/pat_bolt.png',       'SHINE',  '闪电,酷', 27),
('PATTERN_IMG', '皇冠',   '/uploads/seed/pat_crown.png',      'SHINE',  '皇冠,华丽', 28),
('PATTERN_IMG', '音符',   '/uploads/seed/pat_note.png',       'CUTE',   '音符,音乐', 29),
('PATTERN_IMG', '钻石贴', '/uploads/seed/pat_diamond.png',    'SHINE',  '钻石,闪', 30);
