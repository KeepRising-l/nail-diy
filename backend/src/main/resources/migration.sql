-- =============================================================
-- 素材模块重构迁移脚本（MySQL 8，适用于已有数据的旧库，不清库）
-- 使用方法：mysql -uroot -p nail_diy < migration.sql
-- ⚠ 本脚本不幂等，同一个库只能执行一次：
--   重复执行会在第一步报 [ERR] 1060 - Duplicate column name 'code'。
--   执行前先检查是否已迁移：SHOW COLUMNS FROM material LIKE 'code';
--   有结果 = 已迁移，跳过本脚本即可（全新环境请直接用 schema.sql）。
-- 变更：material 表新增 7 个字段；旧类型 PATTERN 迁移为 PATTERN_IMG；
--       甲型行按名称回写 code（绑定前端渲染器）；补充新类型 seed 数据
-- =============================================================
USE nail_diy;

-- 1. 新增字段
ALTER TABLE `material`
    ADD COLUMN `code`         VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '代码标识（SHAPE/PATTERN_STYLE/矢量DECOR 绑定前端渲染器）' AFTER `type`,
    ADD COLUMN `tags`         VARCHAR(255) NOT NULL DEFAULT '' COMMENT '检索标签，逗号分隔' AFTER `category`,
    ADD COLUMN `attrs`        JSON         DEFAULT NULL COMMENT '类型专属属性' AFTER `tags`,
    ADD COLUMN `source`       VARCHAR(20)  NOT NULL DEFAULT 'PLATFORM' COMMENT '来源：PLATFORM/UGC/AI' AFTER `attrs`,
    ADD COLUMN `creator_id`   BIGINT       DEFAULT NULL COMMENT 'UGC 来源用户' AFTER `source`,
    ADD COLUMN `audit_status` VARCHAR(20)  NOT NULL DEFAULT 'APPROVED' COMMENT '审核状态：PENDING/APPROVED/REJECTED' AFTER `creator_id`,
    ADD COLUMN `heat`         INT          NOT NULL DEFAULT 0 COMMENT '热度（被设计引用次数）' AFTER `audit_status`,
    ADD KEY `idx_source_audit` (`source`, `audit_status`),
    ADD KEY `idx_heat` (`heat`);

-- 2. 旧贴图类型迁移：PATTERN → PATTERN_IMG
UPDATE `material` SET `type` = 'PATTERN_IMG' WHERE `type` = 'PATTERN';

-- 3. 已有甲型按名称回写 code（与前端 NailArt PATHS 的 key 一致；匹配顺序敏感，长前缀优先）
UPDATE `material` SET `code` = 'xsquare'   WHERE `type` = 'SHAPE' AND `name` LIKE '%超长方%';
UPDATE `material` SET `code` = 'xstiletto' WHERE `type` = 'SHAPE' AND `name` LIKE '%超长尖%';
UPDATE `material` SET `code` = 'russian'   WHERE `type` = 'SHAPE' AND `name` LIKE '%俄罗斯%';
UPDATE `material` SET `code` = 'moval'     WHERE `type` = 'SHAPE' AND `name` LIKE '%中长%';
UPDATE `material` SET `code` = 'ballerina' WHERE `type` = 'SHAPE' AND (`name` LIKE '%芭蕾%' OR `name` LIKE '%梯形%');
UPDATE `material` SET `code` = 'edge'      WHERE `type` = 'SHAPE' AND `name` LIKE '%折角%';
UPDATE `material` SET `code` = 'flare'     WHERE `type` = 'SHAPE' AND `name` LIKE '%喇叭%';
UPDATE `material` SET `code` = 'lipstick'  WHERE `type` = 'SHAPE' AND `name` LIKE '%唇形%';
UPDATE `material` SET `code` = 'shortsq'   WHERE `type` = 'SHAPE' AND `name` LIKE '%短方%';
UPDATE `material` SET `code` = 'round'     WHERE `type` = 'SHAPE' AND `name` LIKE '%圆形%';
UPDATE `material` SET `code` = 'short'     WHERE `type` = 'SHAPE' AND `name` LIKE '%短圆%';
UPDATE `material` SET `code` = 'square'    WHERE `type` = 'SHAPE' AND `code` = '' AND (`name` LIKE '%方圆%' OR `name` LIKE '%方形%');
UPDATE `material` SET `code` = 'oval'      WHERE `type` = 'SHAPE' AND `code` = '' AND `name` LIKE '%椭圆%';
UPDATE `material` SET `code` = 'almond'    WHERE `type` = 'SHAPE' AND `code` = '' AND `name` LIKE '%杏仁%';
UPDATE `material` SET `code` = 'stiletto'  WHERE `type` = 'SHAPE' AND `code` = '' AND `name` LIKE '%尖%';

-- 4. 补充参数化款式与矢量装饰件 seed（code 唯一，重复执行请先清理同 code 行）
INSERT INTO `material` (`type`, `code`, `name`, `category`, `tags`, `attrs`, `sort`)
SELECT 'PATTERN_STYLE', t.code, t.name, t.category, t.tags, t.attrs, t.sort FROM (
    SELECT 'FRENCH'   AS code, '法式微笑线' AS name, 'FRENCH'    AS category, '法式,经典,百搭' AS tags,
           JSON_OBJECT('paramsSchema', JSON_ARRAY(JSON_OBJECT('key','color','label','微笑线颜色','type','color','default','#FFFFFF'), JSON_OBJECT('key','height','label','指尖高度','type','range','min',8,'max',40,'default',26), JSON_OBJECT('key','curvature','label','弧度','type','range','min',0,'max',100,'default',72))) AS attrs, 1 AS sort
    UNION ALL SELECT 'OMBRE',   '渐变晕染',   'GRADIENTS', '渐变,晕染', JSON_OBJECT('paramsSchema', JSON_ARRAY(JSON_OBJECT('key','color2','label','渐变辅色','type','color','default','#FFFFFF'), JSON_OBJECT('key','position','label','过渡位置','type','range','min',0,'max',100,'default',50))), 2
    UNION ALL SELECT 'DOTS',    '波点',       'BASICS',    '波点,可爱,复古', JSON_OBJECT('paramsSchema', JSON_ARRAY(JSON_OBJECT('key','density','label','密度','type','range','min',3,'max',15,'default',7), JSON_OBJECT('key','size','label','点径','type','range','min',1,'max',5,'default',2.5), JSON_OBJECT('key','color','label','点色(留空自动)','type','color','default',''))), 3
    UNION ALL SELECT 'LEOPARD', '豹纹',       'NATURE',    '豹纹,动物纹,辣妹', JSON_OBJECT('paramsSchema', JSON_ARRAY(JSON_OBJECT('key','density','label','密度','type','range','min',3,'max',15,'default',7), JSON_OBJECT('key','size','label','斑径','type','range','min',1,'max',5,'default',2.5), JSON_OBJECT('key','color','label','斑色(留空自动)','type','color','default',''))), 4
    UNION ALL SELECT 'MARBLE',  '大理石纹',   'NATURE',    '大理石,轻奢,高级', JSON_OBJECT('paramsSchema', JSON_ARRAY(JSON_OBJECT('key','veinColor','label','纹路颜色','type','color','default','#FFFFFF'), JSON_OBJECT('key','density','label','纹路密度','type','range','min',2,'max',8,'default',4), JSON_OBJECT('key','thickness','label','纹路粗细','type','range','min',1,'max',4,'default',1.6))), 5
    UNION ALL SELECT 'TORTOISE','玳瑁琥珀',   'NATURE',    '玳瑁,琥珀,复古', JSON_OBJECT('paramsSchema', JSON_ARRAY(JSON_OBJECT('key','spotColor','label','斑纹颜色','type','color','default','#4A2C17'), JSON_OBJECT('key','density','label','斑块密度','type','range','min',3,'max',10,'default',6))), 6
    UNION ALL SELECT 'AURA',    '极光晕染',   'GRADIENTS', '极光,晕染,氛围感', JSON_OBJECT('paramsSchema', JSON_ARRAY(JSON_OBJECT('key','centerColor','label','光晕颜色','type','color','default','#F8C8DC'), JSON_OBJECT('key','radius','label','光晕范围','type','range','min',20,'max',70,'default',45), JSON_OBJECT('key','cy','label','光晕位置','type','range','min',10,'max',90,'default',45))), 7
    UNION ALL SELECT 'SWIRLS',  '复古曲线',   'BASICS',    '曲线,复古,手绘', JSON_OBJECT('paramsSchema', JSON_ARRAY(JSON_OBJECT('key','lineColor','label','线条颜色','type','color','default','#FFFFFF'), JSON_OBJECT('key','count','label','线条数','type','range','min',2,'max',6,'default',3), JSON_OBJECT('key','width','label','线宽','type','range','min',1,'max',3,'default',1.4))), 8
    UNION ALL SELECT 'GEO',     '几何拼色',   'BASICS',    '几何,拼色,简约', JSON_OBJECT('paramsSchema', JSON_ARRAY(JSON_OBJECT('key','color2','label','拼色','type','color','default','#FFFFFF'), JSON_OBJECT('key','mode','label','分割方式','type','select','options',JSON_ARRAY('half','diagonal','tip'),'default','diagonal'))), 9
    UNION ALL SELECT 'STARS',   '星星点缀',   'BASICS',    '星星,梦幻,点缀', JSON_OBJECT('paramsSchema', JSON_ARRAY(JSON_OBJECT('key','color','label','星色(留空自动)','type','color','default',''), JSON_OBJECT('key','count','label','数量','type','range','min',2,'max',8,'default',4), JSON_OBJECT('key','size','label','星径','type','range','min',1.5,'max',4,'default',2.4))), 10
) t
WHERE NOT EXISTS (SELECT 1 FROM `material` m WHERE m.`type` = 'PATTERN_STYLE' AND m.`code` = t.code);

INSERT INTO `material` (`type`, `code`, `name`, `category`, `attrs`, `sort`)
SELECT 'DECOR', t.code, t.name, t.category, t.attrs, t.sort FROM (
    SELECT 'star'     AS code, '星形钻'   AS name, 'GEM'   AS category, JSON_OBJECT('material','水钻') AS attrs, 1 AS sort
    UNION ALL SELECT 'round',    '圆钻',     'GEM',   JSON_OBJECT('material','水钻'), 2
    UNION ALL SELECT 'drop',     '水滴钻',   'GEM',   JSON_OBJECT('material','水钻'), 3
    UNION ALL SELECT 'marquise', '马眼钻',   'GEM',   JSON_OBJECT('material','水钻'), 4
    UNION ALL SELECT 'oval',     '椭圆钻',   'GEM',   JSON_OBJECT('material','水钻'), 5
    UNION ALL SELECT 'hex',      '六边钻',   'GEM',   JSON_OBJECT('material','水钻'), 6
    UNION ALL SELECT 'triangle', '三角钻',   'GEM',   JSON_OBJECT('material','水钻'), 7
    UNION ALL SELECT 'heart',    '爱心钻',   'GEM',   JSON_OBJECT('material','水钻'), 8
    UNION ALL SELECT 'bar',      '长条金属', 'METAL', JSON_OBJECT('material','金属'), 9
    UNION ALL SELECT 'pearl',    '珍珠',     'PEARL', JSON_OBJECT('material','珍珠'), 10
) t
WHERE NOT EXISTS (SELECT 1 FROM `material` m WHERE m.`type` = 'DECOR' AND m.`code` = t.code);
