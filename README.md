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
- 售后申请：对已完成订单发起仅退款 / 退货退款（含原因、说明、≤3 张凭证图），可查看处理结果
- 消息通知：顶栏铃铛入口，订单状态变更与售后审核结果自动提醒，带未读红点
- 完成后评价晒图、个人中心（昵称 / 头像 / 手机号）

**商家端**（`/merchant`，MERCHANT / ADMIN 角色）
- 数据看板：今日与累计订单数 / 营业额、待履约订单、平均评分、快捷入口
- 素材管理（甲型 / 色卡 / 图案 / UGC 贴纸审核）、模板管理（成品设计 + 封面上传）
- 订单中心：接单 → 制作（完成生成自提码）→ 发货 / 自提码核销
- 售后管理：查看售后申请（按状态筛选），同意退款或拒绝（拒绝需填原因，会推送给顾客）
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
  src/main/resources/schema.sql 全量建库脚本（15 张表 + 素材种子）
  uploads/                      公开上传目录（头像 / 封面 / 设计快照 / 素材图）
  private-uploads/              私有上传目录（待审核 UGC 贴纸，不映射为静态资源）
frontend/                       Vue 3 前端
  src/engine/                   参数化渲染引擎（甲型 / 款式 / 效果层 / 钻饰）
  src/views/                    页面（views/merchant/ 为商家端）
  src/store/                    Pinia（auth / cart）
  scripts/                      种子脚本（gen-templates / seed-stores / gen-decals）
design-system/                  视觉设计规范
docs/                           项目文档（PRD / 技术详解 / 答辩手册 / 使用文档 / 部署指南等）
```

## 设计说明

- **支付为模拟实现**：`POST /api/orders/{id}/pay` 直接置为已支付，真实支付可在此接入沙箱。
- **运费规则**：后端 `application.yml` 的 `nail.freight.amount / free-over` 为权威配置，前端结算页仅作展示。
- **设计参数结构**：`{ fingers: [ { shapeId, length, colorId, overlays, patternIds, gradient, catEye, adjust, french, dots, patternStyle, decorations, patternPos } ] }`，与设计台一致，模板与订单项快照共用。
- **安全**：JWT 无状态认证；商家接口 `/api/merchant/**` 限 MERCHANT/ADMIN；`nail.jwt.secret` 生产环境请务必更换。
- **单元测试**：`cd backend && mvn test` —— 10 个测试类 / 77 个用例，覆盖订单状态机（全流转表 / 终态封口 / 非法跳转）、运费规则（满额包邮边界）、购物车并发、设计参数校验（色卡与自定义色值两种来源）、售后流程（可申请范围 / 退款金额后端计算 / 并发防护）、文件上传安全（路径穿越 / 伪装图片 / 透明通道）、JWT（过期 / 签名 / 弱密钥）。
- **移动端适配**：响应式断点 1024 / 768 / 480；顶栏与商家端侧边栏在窄屏改为抽屉菜单；设计台预览区限高，保证下方素材面板可达。
- **逻辑删除**：核心业务表带 `deleted` 字段，由 MyBatis-Plus `@TableLogic` 统一处理。
