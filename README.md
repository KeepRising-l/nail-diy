# Nail DIY —— DIY 穿戴甲定制平台

毕业设计项目：顾客在可视化设计台上设计穿戴甲（手模实时预览、五指独立编辑），保存设计加购下单，商家接单精工制作，到店自提（凭自提码核销）或快递到家。

## 技术栈

| 端 | 技术 |
| --- | --- |
| 前端 | Vue 3 · Vite · Element Plus · Pinia · Vue Router · ECharts · html2canvas |
| 后端 | Spring Boot 3.3 · Spring Security + JWT · MyBatis-Plus 3.5 · MySQL 8 |
| 种子脚本 | Python 3（pymysql、pillow、resvg-py） |

## 功能一览

**顾客端**
- 注册 / 登录（JWT，手机号 + 密码）
- 首页：平台公告（商家发布，游客可见）、精选款式
- 款式灵感库：风格筛选、一键套用模板、卡片红心收藏 + 我的收藏
- 可视化设计台：手模实时预览，五指独立编辑（甲型 / 长度 / 色号 / 渐变 / 猫眼 / 法式 / 波点 / 贴花 / 甲面调整），支持上传自定义贴纸（透明底 PNG，审核通过后全员可用），设计快照保存
- 购物车 → 结算：到店自提（选门店，免运费）或商家邮寄（运费满额包邮）
- 订单全程状态追踪（待支付 → 待接单 → 制作中 → 待自提/待发货 → 已发货 → 已签收/已核销），自提码核销
- 完成后评价晒图、个人中心（昵称 / 头像 / 手机号）

**商家端**（`/merchant`，MERCHANT / ADMIN 角色）
- 数据看板：今日与累计订单数 / 营业额、待履约订单、平均评分、快捷入口
- 素材管理（甲型 / 色卡 / 图案 / UGC 贴纸审核）、模板管理（成品设计 + 封面上传）
- 订单中心：接单 → 制作（完成生成自提码）→ 发货 / 自提码核销
- 评价管理（回复 / 删除违规）、公告管理（发布 / 置顶 / 上下线）
- 门店管理：自提门店增删改、营业状态切换（停用即时对顾客隐藏）
- 销售统计：销售趋势、履约方式占比、评价分布、设计偏好 TOP5

**管理端**（`/admin`，仅 ADMIN 角色）
- 数据看板：平台用户（顾客 / 商家）、累计订单与销售额、销售趋势、用户角色分布、热门款式 TOP5（按收藏）、待审核贴纸数
- 用户管理：按手机号 / 昵称 / 状态 / 角色筛选，禁用与恢复账号（禁止操作自己，防自锁）

## 快速开始

环境要求：JDK 17、Maven 3.8+、Node 18+、MySQL 8；种子脚本另需 Python 3。

```bash
# 1. 建库（含素材与模板种子，可重复执行）
mysql -uroot -p < backend/src/main/resources/schema.sql

# 2. 设置本地配置（JWT_SECRET 至少 32 位且不可提交到仓库）
export MYSQL_PASSWORD=你的密码   # Windows: set MYSQL_PASSWORD=你的密码
export JWT_SECRET=你的随机长密钥 # Windows: set JWT_SECRET=你的随机长密钥

# 3. 启动后端（8080）
cd backend && mvn spring-boot:run

# 4. 启动前端（默认 5173，/api 与 /uploads 已代理到 8080）
cd frontend && npm install && npm run dev
```

### 演示数据（可选，但推荐）

```bash
# 0. 起一个前端 dev server（模板封面生成脚本会从它拉素材清单，端口须为 5199）
cd frontend && npx vite --port 5199

# 1. 页面注册顾客测试账号：13800001234 / Abc123456

# 2. 生成款式灵感库 6 款模板（含手模合成封面）
python frontend/scripts/gen-templates.py

# 3. 生成 3 家自提门店 + 演示商家 + 平台管理员账号（均幂等，可重复执行）
python frontend/scripts/seed-stores.py
```

### 测试账号

| 角色 | 手机号 | 密码 | 来源 |
| --- | --- | --- | --- |
| 顾客 | 13800001234 | Abc123456 | 页面注册 |
| 商家 | 13900009999 | Abc123456 | `seed-stores.py` 生成（角色 MERCHANT） |
| 管理员 | 13900000001 | Abc123456 | `seed-stores.py` 生成（角色 ADMIN） |

## 目录结构

```
backend/                        Spring Boot 后端
  src/main/resources/schema.sql 全量建库脚本（表结构 + 素材种子）
  uploads/                      上传文件目录（头像 / 封面 / 设计快照）
frontend/                       Vue 3 前端
  src/views/                    页面（views/merchant/ 为商家端）
  src/store/                    Pinia（auth / …）
  scripts/                      种子脚本（gen-templates / seed-stores / gen-decals）
design-system/                  视觉设计规范
docs/                           功能流程文档
```

## 设计说明

- **支付为模拟实现**：`POST /api/orders/{id}/pay` 直接置为已支付，真实支付可在此接入沙箱。
- **运费规则**：后端 `application.yml` 的 `nail.freight.amount / free-over` 为权威配置，前端结算页仅作展示。
- **设计参数结构**：`{ fingers: [ { shapeId, length, colorId, overlays, patternIds, gradient, catEye, adjust, french, dots, patternStyle, decorations, patternPos } ] }`，与设计台一致，模板与订单项快照共用。
- **安全**：JWT 无状态认证；商家接口 `/api/merchant/**` 限 MERCHANT/ADMIN；`nail.jwt.secret` 生产环境请务必更换。
- **单元测试**：`cd backend && mvn test` 覆盖订单状态机（全流转表/终态封口/非法跳转）与运费规则（满额包邮边界）。
- **逻辑删除**：核心业务表带 `deleted` 字段，由 MyBatis-Plus `@TableLogic` 统一处理。
