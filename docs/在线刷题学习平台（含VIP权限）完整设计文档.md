# 在线刷题学习平台（含VIP权限）完整设计文档

## 一、项目概述

### 1.1 项目简介

前后端分离的在线面试刷题平台，面向计算机求职学习者，提供 Java、Python、前端、数据库、计算机网络等方向的面试题库与在线刷题服务。

系统包含两套解耦的权限体系：角色权限（管理员 / 普通用户）与资源权限（免费题库 / VIP 题库）。管理员具备题库全量管理能力，支持 Excel 批量导入导出、用户管理与 VIP 权限配置。

不含视频模块，聚焦刷题业务闭环，可完整部署上线。

### 1.2 技术栈

**前端**

- 核心框架：Vue 3 + TypeScript + Vite
- 状态管理：Pinia
- 路由：Vue Router 4（路由守卫 + meta 权限标记）
- UI 组件库：Naive UI
- 网络请求：Axios（全局拦截器：Token 注入、401/403 统一处理）
- 内容渲染：markdown-it + highlight.js + DOMPurify（题干/解析渲染与 XSS 防护）
- 工程规范：ESLint + 统一 TS 类型约束

**后端**

- 核心框架：Spring Boot 3.x（JDK 17）
- 权限认证：Sa-Token（无状态 Token、路由拦截、角色鉴权、账号状态校验）
- ORM：MyBatis-Plus（含逻辑删除、分页插件、自动填充）
- 数据库：MySQL 8.0（utf8mb4）
- 文件处理：Alibaba EasyExcel（题库批量导入导出）
- 缓存：Redis（验证码、刷题会话、限流计数）
- 验证码：easy-captcha（图形验证码）
- 邮件：Spring Boot Mail（SMTPS 465）
- 工具栈：Lombok、Validation 参数校验、BCrypt 密码加密

**部署环境**

- 阿里云轻量应用服务器（Ubuntu 22.04）
- Nginx（静态资源 + 反向代理 + HTTPS 终止）
- 后端 Jar 包以 Systemd 守护进程常驻
- 证书：Let's Encrypt（certbot 自动续期）

### 1.3 项目特色

- **双层权限架构解耦**：角色权限与资源权限相互独立，VIP 到期无需变更角色，权限边界清晰
- **答案字段分级**：题干、答案、解析按答题阶段分 VO 返回，答题前置阶段服务端不下发答案
- **完整刷题闭环**：顺序刷题、随机刷题、自动判题、作答流水、错题本、遗忘曲线复习调度
- **管理员高效运维**：题库 CRUD、Excel 批量导入导出（带行级错误回传）、用户与 VIP 管理
- **渐进式人机校验**：注册强制验证码，登录失败达阈值后才触发，兼顾防刷与正常用户体验
- **工程规范化**：全局异常处理、统一返回体、参数校验、逻辑删除、事务控制、越权专项测试

---

## 二、整体功能架构

### 2.1 权限模型

系统采用「角色 + 资源属性」两层模型。

**第一层：角色（user.role）**

| 角色 | 说明 | 后台管理接口 | 前台学习接口 |
| --- | --- | --- | --- |
| `admin` | 管理员 | 全部放行 | 放行（仅预览，不计入统计数据） |
| `user` | 普通用户 | 全部拦截 403 | 放行 |

**第二层：VIP 资源属性（user.vip_expire_time）**

VIP 不是角色，而是用户身上的一个时间属性：

- `vip_expire_time IS NULL` 或 `< NOW()` → 非 VIP，仅可访问 `is_vip = 0` 的题目
- `vip_expire_time >= NOW()` → VIP 有效期内，可访问全部题目
- `role = admin` → 不受 VIP 限制，用于后台运维预览

这样设计的好处：会员到期不需要修改角色字段，只需让时间自然过期；管理员与 VIP 两套逻辑互不耦合。

### 2.2 功能模块总览

**前台用户模块**

- 注册（图形验证码）、登录（失败后触发图形验证码）、退出、找回密码（邮箱验证码）
- 个人信息修改、头像上传、密码修改
- VIP 状态与到期时间展示、套餐页面、下单与模拟支付
- 分类浏览、题目搜索、题目列表、题目详情
- 顺序刷题、随机刷题、客观题自动判分、简答题留存作答
- 做题记录统计（总答题数、正确率、按分类正确率）、连续打卡天数
- 错题本自动归集、基于遗忘曲线的复习队列
- 题目收藏、个人笔记（仅本人可见）、题目点赞

**后台管理员模块**

- 用户管理：列表查询、禁用/启用、手动设置 VIP 过期时间
- 分类管理：新增、编辑、删除、排序
- 题库管理：单条 CRUD、设置 VIP 属性与难度、上下架
- 批量运维：Excel 模板下载、批量导入（行级错误回传）、按条件批量导出
- 数据统计：用户数、题目数、VIP 用户数、日均答题量

---

## 三、数据库设计

共 9 张表，覆盖用户、题库、刷题、收藏笔记、点赞、VIP 订单六类数据。

### 3.0 设计约定

- 除流水表外，所有表统一 `deleted TINYINT DEFAULT 0`，配合 MyBatis-Plus `@TableLogic` 逻辑删除。唯一例外是 `user_answer_log`：它是事实流水，只追加不修改也不删除，不设该字段
- 所有表统一 `create_time` / `update_time`，由 MP 自动填充
- 字符集统一 `utf8mb4`，引擎 InnoDB
- 题干、选项、解析统一存 Markdown 原文，渲染在前端完成
- 验证码、登录会话等短生命周期数据不落库，统一存 Redis 并设置 TTL

**唯一键约定（重要）**

唯一键**一律不包含 `deleted` 列**。如果唯一键里带 `deleted`，同一逻辑键会产生多行：`(a, 0)` 与 `(a, 1)` 并存，唯一约束实际失效，第二次软删时会撞键报错。正确做法是唯一键只约束业务键，软删的数据靠下列三种方式处理：

| 场景 | 处理方式 |
| --- | --- |
| `user` 的 username / email | 软删时把字段改写成带主键后缀的占位值（如 `__del_123`），释放原名供重新使用 |
| `user_note` | 保存走 upsert：`ON DUPLICATE KEY UPDATE content = VALUES(content), deleted = 0`，复用同一行 |
| `user_favorite` / `question_like` | toggle 语义，直接翻转同一行的 `deleted`，永不新增行 |

### 3.1 用户表 user

```sql
CREATE TABLE `user` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密密码（60 字符）',
  `nickname` VARCHAR(50) DEFAULT '' COMMENT '用户昵称',
  `avatar` VARCHAR(255) DEFAULT '' COMMENT '头像地址',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱，用于找回密码；未绑定为 NULL',
  `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色：admin 管理员 / user 普通用户',
  `vip_expire_time` DATETIME NULL COMMENT 'VIP 过期时间，NULL = 从未开通',
  `status` TINYINT DEFAULT 1 COMMENT '1 正常 0 禁用',
  `login_fail_count` TINYINT DEFAULT 0 COMMENT '连续登录失败次数，用于锁定与触发验证码',
  `login_lock_time` DATETIME NULL COMMENT '锁定截止时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_role_status` (`role`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

> **唯一键不含 `deleted` 的原因**：若写成 `uk_username (username, deleted)`，同一用户名会产生 `(zhangsan, 0)` 与 `(zhangsan, 1)` 两行，唯一约束形同虚设，且第二个同名用户被软删时必然撞键报错。
>
> **软删时释放唯一字段**：管理员删除用户时，在同一事务内改写唯一字段再置删除标记：
>
> ```sql
> UPDATE user SET username = CONCAT('__del_', id),
>                 email = IF(email IS NULL, NULL,
>                            CONCAT('__del_', id, '_', LEFT(email, 60))),
>                 deleted = 1
> WHERE id = #{id};
> ```
>
> `username` 侧不会溢出：`__del_` 占 6 字符，`id` 最长 19 位，合计 25 < `VARCHAR(50)`。
> `email` 侧必须截断：`__del_` + `id` + `_` 最多已占 26 字符，原邮箱再拼接就会超出 `VARCHAR(100)`。这里取 `LEFT(email, 60)`，最坏情况 86 字符，安全。MySQL 严格模式（`STRICT_TRANS_TABLES`）下超长会直接报错而不是静默截断，不能省这一步。
>
> 这样同名账号可以无限次「删除 → 重新注册」，后台列表仍能看到被删记录，只是用户名带 `__del_` 前缀，一眼可辨。
>
> `email` 用 NULL 而非空串表示未绑定：唯一索引允许多个 NULL 但不允许多个空串，用空串会导致第二个未绑定邮箱的用户直接插入失败。`uk_email` 保证一个邮箱只能找回一个账号，避免找回密码时产生歧义。

### 3.2 题目分类表 question_category

```sql
CREATE TABLE `question_category` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL COMMENT '分类名称',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父分类 ID，0 = 一级分类',
  `sort` INT DEFAULT 0 COMMENT '排序权重，越大越靠前',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_parent_sort` (`parent_id`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目分类表';
```

> 业务约束：题目只允许挂载在叶子分类上（后端新增/编辑时校验），避免查询子分类题目时需要递归。一级分类仅用于分组展示。
>
> **删除分类的校验**（后端必须做，否则题目的 `category_id` 会悬空）：
>
> 1. 存在未删除的子分类 → 拒绝删除，提示先处理子分类
> 2. 该分类下存在启用状态的题目（`status = 1`）→ 拒绝删除，提示先下架或迁移题目
> 3. 该分类下仅有已下架或已删除的题目 → 允许删除
>
> 前端删除按钮点击后先调校验接口，由后端返回可删原因或阻塞原因，不要把校验逻辑写在前端。

### 3.3 题目表 question

```sql
CREATE TABLE `question` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `category_id` BIGINT NOT NULL COMMENT '所属叶子分类 ID',
  `type` TINYINT NOT NULL COMMENT '1 单选 2 多选 3 判断 4 简答',
  `difficulty` TINYINT DEFAULT 2 COMMENT '1 简单 2 中等 3 困难',
  `title` MEDIUMTEXT NOT NULL COMMENT '题干（Markdown）',
  `answer` VARCHAR(200) DEFAULT '' COMMENT '客观题标准答案，单选/判断存 A，多选存归一化后的 ABD',
  `answer_text` MEDIUMTEXT COMMENT '简答题参考答案（Markdown）',
  `analysis` MEDIUMTEXT COMMENT '题目解析（Markdown）',
  `is_vip` TINYINT DEFAULT 0 COMMENT '0 免费 1 VIP 专属',
  `status` TINYINT DEFAULT 1 COMMENT '1 启用 0 下架',
  `title_md5` CHAR(32) DEFAULT '' COMMENT '题干 MD5，用于导入查重',
  `like_count` INT DEFAULT 0 COMMENT '点赞数，冗余字段，由点赞事务同步增减',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_cat_vip_status` (`category_id`, `is_vip`, `status`, `deleted`),
  KEY `idx_difficulty` (`difficulty`),
  KEY `idx_like_count` (`like_count`),
  KEY `idx_dedup` (`category_id`, `title_md5`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';
```

> `title_md5` 是题干去空白后的 MD5，服务于 Excel 导入的幂等查重（见 4.4）。这里建的是普通索引而非唯一索引：唯一索引会与「覆盖」导入策略冲突，且会误伤管理员手动录入的相似题目，查重逻辑放在导入流程里显式处理更可控。

> `like_count` 是冗余字段。列表页要展示点赞数，若每次 `COUNT(*)` 聚合会给查询带来额外开销；冗余后用 `like_count = like_count ± 1` 原子更新即可。代价是必须保证点赞表与计数在同一事务内变更，并预留一个「按题目 id 重算计数」的管理员兜底接口。

### 3.4 题目选项表 question_option

```sql
CREATE TABLE `question_option` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `question_id` BIGINT NOT NULL COMMENT '题目 ID',
  `option_code` CHAR(1) NOT NULL COMMENT '选项标识 A/B/C/D',
  `option_content` TEXT NOT NULL COMMENT '选项内容（Markdown）',
  `sort` INT DEFAULT 0 COMMENT '选项展示顺序',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  KEY `idx_question_sort` (`question_id`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目选项表';
```

> 判断题同样落两条选项（A. 正确 / B. 错误），四种题型在判分逻辑上统一处理。简答题无选项数据。

### 3.5 用户做题状态表 user_question_record

保存「用户对某题的当前掌握状态」，一题一条，服务错题本与复习调度。

```sql
CREATE TABLE `user_question_record` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `question_id` BIGINT NOT NULL COMMENT '题目 ID',
  `last_answer` VARCHAR(500) DEFAULT '' COMMENT '最近一次作答内容',
  `is_correct` TINYINT NULL COMMENT '最近一次结果 1 正确 0 错误 NULL 简答未判分',
  `total_count` INT DEFAULT 0 COMMENT '累计作答次数',
  `wrong_count` INT DEFAULT 0 COMMENT '累计答错次数',
  `continuous_correct` TINYINT DEFAULT 0 COMMENT '连续答对次数，用于掌握判定',
  `review_level` TINYINT DEFAULT 0 COMMENT '遗忘曲线层级 0-5',
  `next_review_time` DATETIME NULL COMMENT '下次复习时间，NULL = 不在复习队列',
  `mastered` TINYINT DEFAULT 0 COMMENT '1 已掌握，移出错题本',
  `submit_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最近作答时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除，用户清除某题练习记录时置 1',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_question` (`user_id`, `question_id`),
  KEY `idx_wrong_book` (`user_id`, `mastered`, `is_correct`, `deleted`),
  KEY `idx_review_queue` (`user_id`, `mastered`, `next_review_time`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户做题状态表';
```

> 复习队列索引把 `mastered` 放在 `next_review_time` 之前：查询条件固定为 `mastered = 0 AND next_review_time <= NOW()`，等值列在前、范围列在后，索引才能被完整利用。

写入统一使用 `INSERT ... ON DUPLICATE KEY UPDATE`，天然处理并发重复提交。

### 3.6 用户作答流水表 user_answer_log

每次提交追加一条，服务正确率统计、答题趋势、每日打卡。

```sql
CREATE TABLE `user_answer_log` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `question_id` BIGINT NOT NULL COMMENT '题目 ID',
  `user_answer` VARCHAR(500) DEFAULT '' COMMENT '本次作答内容',
  `is_correct` TINYINT NULL COMMENT '1 正确 0 错误 NULL 简答未判分',
  `submit_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  KEY `idx_user_time` (`user_id`, `submit_time`),
  KEY `idx_user_correct` (`user_id`, `is_correct`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户作答流水表';
```

> 为什么要拆两张表：状态表保证错题本与复习队列能靠唯一键 O(1) 命中，流水表保证正确率的分母是真实作答次数而非去重题数。若只用一张唯一键表兼顾两者，「正确率」会退化成「已做题中答对的比例」，且无法识别反复做错的题。
>
> 本表是 3.0 所述「统一 `deleted`」规则的唯一例外：它是事实流水，只追加、不修改、不删除，因此不设 `deleted` 字段，也不需要 `update_time`。所有统计都基于它，删一条流水会让历史数据对不上账。

### 3.7 用户收藏表 user_favorite

```sql
CREATE TABLE `user_favorite` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `question_id` BIGINT NOT NULL COMMENT '题目 ID',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除，取消收藏即置 1',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_question` (`user_id`, `question_id`),
  KEY `idx_user_deleted` (`user_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目收藏表';
```

### 3.8 用户笔记表 user_note

```sql
CREATE TABLE `user_note` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `question_id` BIGINT NOT NULL COMMENT '题目 ID',
  `content` TEXT NOT NULL COMMENT '笔记内容（Markdown），仅本人可见',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_question` (`user_id`, `question_id`),
  KEY `idx_user_deleted` (`user_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目笔记表';
```

> 保存笔记必须走 upsert，不能「先删后插」：唯一键不含 `deleted`，若先插入新行必然撞键。
>
> ```sql
> INSERT INTO user_note (user_id, question_id, content)
> VALUES (#{userId}, #{questionId}, #{content})
> ON DUPLICATE KEY UPDATE content = VALUES(content), deleted = 0;
> ```
>
> 删除笔记走 `UPDATE user_note SET deleted = 1`，之后再次保存同一题的笔记时，上面的 upsert 会把 `deleted` 置回 0，复用同一行。

### 3.9 VIP 订单表 vip_order

```sql
CREATE TABLE `vip_order` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `order_no` VARCHAR(32) NOT NULL COMMENT '订单号',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `plan_type` VARCHAR(20) NOT NULL COMMENT '套餐：month 月 / quarter 季 / year 年',
  `months` TINYINT NOT NULL COMMENT '开通月数',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '订单金额',
  `pay_channel` VARCHAR(20) DEFAULT 'MOCK' COMMENT '支付渠道：MOCK 模拟 / ALIPAY',
  `status` TINYINT DEFAULT 0 COMMENT '0 待支付 1 已支付 2 已关闭',
  `pay_time` DATETIME NULL COMMENT '支付时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VIP 订单表';
```

口径说明：本项目不接真实支付。`MOCK` 渠道点击支付即置为已支付，走与真实支付完全相同的后置逻辑（订单状态流转 + 权益发放），后续接支付宝只需替换 `pay_channel` 分支。这一点在简历与答辩中如实说明即可。

### 3.10 题目点赞表 question_like

```sql
CREATE TABLE `question_like` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `question_id` BIGINT NOT NULL COMMENT '题目 ID',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除，取消点赞即置 1',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_question` (`user_id`, `question_id`),
  KEY `idx_question_deleted` (`question_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目点赞表';
```

> 唯一键 `(user_id, question_id)` 保证一人一题最多一票，这是防刷的第一道防线，取消点赞走软删除而非物理删除，避免反复点赞产生新记录。

---

## 四、核心业务规则设计

### 4.1 鉴权规则

**角色鉴权**

- 后台管理接口（`/api/admin/**`）统一由 Sa-Token 拦截器校验 `role = admin`，非管理员返回 403
- 普通用户调用后台接口返回 403，不返回任何业务数据

**账号状态校验（易漏项）**

Sa-Token 不会因为数据库里 `status` 改成 0 而自动失效已签发的 Token。必须在全局拦截器中补一次校验：

```java
// 每次请求校验：账号是否被禁用、是否被逻辑删除
if (user == null || user.getStatus() == 0 || user.getDeleted() == 1) {
    StpUtil.logout();
    throw new BizException(ResultCode.ACCOUNT_DISABLED);
}
```

**VIP 资源鉴权**

VIP 鉴权必须覆盖全部题目出口，而不是只做列表和详情。统一封装为一个方法，所有涉及题目 id 的入口都调用它：

```java
// QuestionAccessChecker.checkReadable(questionId, userId)
// 1. 题目不存在或已下架 → 404
// 2. role = admin → 放行
// 3. is_vip = 1 且不在 VIP 有效期内 → 抛 40301（VIP_REQUIRED）
```

**鉴权覆盖矩阵**

| 入口 | 接口 | 是否鉴权 | 说明 |
| --- | --- | --- | --- |
| 题目列表 | `GET /api/question/list` | 是 | SQL 层直接按权限过滤 `is_vip` |
| 题目搜索 | `GET /api/question/search` | 是 | 同上，复用同一套过滤条件 |
| 题目详情 | `GET /api/question/detail` | 是 | 逐条鉴权，防 id 遍历 |
| 提交答题 | `POST /api/practice/submit` | 是 | 防普通用户对 VIP 题作答 |
| 收藏 | `POST /api/favorite/toggle` | 是 | 防收藏 VIP 题 |
| 收藏列表 | `GET /api/favorite/list` | 是 | 越权条目脱敏 |
| 笔记保存 | `POST /api/note/save` | 是 | 防给 VIP 题写笔记 |
| 笔记查询 | `GET /api/note/detail` | 是 | 同上 |
| 错题本 | `GET /api/practice/wrong/list` | 是 | VIP 过期后返回脱敏条目，不下发题干 |
| 复习队列 | `GET /api/practice/review/list` | 是 | 同上 |
| 点赞 | `POST /api/like/toggle` | 是 | 防对 VIP 题点赞，点赞成功会间接确认题目存在 |

> 脱敏规则：若用户已无该题访问权限，返回题目 id 与分类名，题干与选项置空，前端显示「该题目需会员权限」，不暴露任何题目内容。

### 4.2 答案字段分级（核心）

这是刷题平台的底线。答案与解析只能在提交之后下发，否则用户打开浏览器控制台就能看到答案，刷题失去意义。

**VO 分层**

| VO | 使用场景 | 包含字段 |
| --- | --- | --- |
| `QuestionListVO` | 列表、搜索 | id、分类、题型、难度、isVip、题干纯文本摘要 |
| `QuestionPracticeVO` | 答题前详情、刷题页 | id、分类、题型、难度、题干、选项（仅 code 与 content）、是否已收藏 |
| `QuestionSubmitVO` | 提交后返回 | isCorrect、正确答案、解析、本次掌握状态变化 |
| `QuestionAdminVO` | 后台管理 | 全部字段，含 answer / answerText / analysis |

**服务端约束**

- 查询答题前置场景时，SQL 不 select `answer`、`answer_text`、`analysis` 三列，从数据出口而非前端隐藏层面杜绝泄漏
- 题目实体与前台 VO 之间用 MapStruct 或手写转换，禁止把 `Question` 实体直接序列化返回
- 后端单测断言：`QuestionPracticeVO` 中 `answer` 字段必须为 null

### 4.3 刷题业务规则

**刷题会话（解决随机刷题分页问题）**

`ORDER BY RAND() LIMIT` 会导致翻页重复、漏题，且数据量大时性能差。改为会话方案：

1. 进入刷题页调用 `POST /api/practice/session`，参数：分类 id、模式（顺序/随机）、题目数量、难度筛选
2. 后端按权限查出全部可用题目 id，顺序模式按 id 排序，随机模式用 `Collections.shuffle` 打乱后取前 N 个
3. 生成 `sessionId` 与有序 id 列表存入 Redis（单机可用本地缓存），TTL 2 小时
4. 前端按 `sessionId + index` 逐题拉取，天然无重复、无漏题
5. 重新开始练习时重新生成会话

**判分规则**

- 单选题：答案完全相等
- 多选题：将用户答案与标准答案都做「按字符排序后拼接」的归一化，再比较。用户答 `BD` 与标准答案 `DB` 判定为正确
- 判断题：按单选项处理，标准答案存 `A`（正确）或 `B`（错误）
- 简答题：不判分，`is_correct` 置 NULL，仅保存作答内容，用户可主动查看参考答案

**错题本与遗忘曲线复习**

提交后更新状态表：

- 答对：`continuous_correct + 1`，`review_level` 提升一级，`next_review_time` 按层级间隔顺延
- 答错：`continuous_correct` 归零，`wrong_count + 1`，`review_level` 归零，`next_review_time = NOW() + 1 天`
- `continuous_correct >= 3` 或 `review_level >= 5` → `mastered = 1`，同时把 `next_review_time` 置 NULL，双保险确保它退出复习队列
- 复习间隔：1 天、2 天、4 天、7 天、15 天

两个列表的查询条件：

```sql
-- 错题本
WHERE user_id = ? AND deleted = 0 AND mastered = 0 AND is_correct = 0

-- 复习队列
WHERE user_id = ? AND deleted = 0 AND mastered = 0 AND next_review_time <= NOW()
```

复习队列**必须带 `mastered = 0`**：判定掌握时虽然会把 `next_review_time` 置 NULL，但历史数据、人工改库、掌握判定逻辑调整都可能留下 `mastered = 1` 却仍有 `next_review_time` 的行，只按时间过滤会让已掌握的题反复冒出来。两个条件都写，互为兜底。

**统计口径**

正确率基于流水表：`正确条数 / 总作答条数`。前端展示时标明口径，区分「去重题数正确率」与「总作答正确率」。

**并发提交**

状态表写入统一使用 `INSERT ... ON DUPLICATE KEY UPDATE`，避免唯一键冲突报错；`total_count`、`wrong_count` 使用 `total_count = total_count + 1` 形式做原子累加，不依赖应用层先读后写。

### 4.4 Excel 导入导出规则

**权限**：仅 admin 可调用，前端不向普通用户暴露入口。

**模板列定义（一行一题）**

| 列 | 字段 | 必填 | 说明 |
| --- | --- | --- | --- |
| A | 分类名称 | 是 | 需已存在且为叶子分类 |
| B | 题型 | 是 | 单选 / 多选 / 判断 / 简答 |
| C | 难度 | 否 | 简单 / 中等 / 困难，默认中等 |
| D | 题干 | 是 | 支持 Markdown |
| E | 选项 | 条件必填 | 格式 `A.内容|B.内容|C.内容`，简答题留空 |
| F | 正确答案 | 是 | 单选 A，多选 ABD，判断 A 或 B |
| G | 解析 | 否 | 支持 Markdown |
| H | 是否 VIP | 否 | 0 或 1，默认 0 |

**导入幂等（必须实现）**

同一份文件导两次不能让题库翻倍。查重键为「分类 + 题干 MD5」，题干先做去空白、统一换行的归一化再取 MD5。跨分类的相同题干不算重复——同一道题归属不同分类是合理的。

导入接口增加 `duplicateStrategy` 参数，由管理员在页面上选择：

| 策略 | 行为 | 适用场景 |
| --- | --- | --- |
| `SKIP`（默认） | 已存在则跳过，计入 `skippedCount`，不动库 | 追加新题，最常用也最安全 |
| `OVERWRITE` | 已存在则更新题干/选项/答案/解析，保留题目 id 与点赞数 | 修正题库内容 |
| `ERROR` | 已存在则记为失败行，返回行号与「题目重复」 | 要求导入前题库必须是干净的 |

实现要点：

1. 每批入库前，用本批所有 `(category_id, title_md5)` 一次性查库，拿到 `已存在题目 id` 的映射，不要在循环里逐条查
2. **批内也要查重**——同一个文件里可能有两行完全相同的题，用内存 `Set` 挡住，否则第二条会绕过查库直接插入
3. `OVERWRITE` 策略下更新选项时，先将该题旧选项全部逻辑删除再插入新选项，选项 id 变化不影响历史作答记录（记录只存 `question_id`）
4. 返回结构补充 `skippedCount`：`{ successCount, skippedCount, failCount, failures: [...] }`

**导入流程**

- 使用 EasyExcel 的 `AnalysisEventListener` 流式读取，每积累 500 条批量入库一次，避免大文件 OOM
- 校验项：分类是否存在且为叶子、题型是否合法、题干非空、客观题选项与答案非空、答案字符是否落在选项范围内、**重复检测（按上表策略处理）**
- 失败行不中断整体流程，收集 `{行号, 失败原因}` 列表
- 单批写入包在同一事务中，该批失败则整批回滚并返回该批行号范围
- 返回结构：`{ successCount, skippedCount, failCount, failures: [...] }`，前端表格展示失败明细并支持下载失败清单
- 导入完成后，前端展示「新增 X 条 / 跳过 Y 条 / 失败 Z 条」的汇总，不只是一个「成功」提示

**限制**

- 仅允许 `.xlsx`（校验文件头而非仅校验扩展名）
- 单文件 ≤ 5 MB，≤ 5000 行
- 上传目录不授予执行权限，文件名重命名为 UUID

**导出**：支持按分类、是否 VIP、难度、状态筛选，分批写出避免内存溢出。

### 4.5 VIP 开通规则

- 套餐：月卡 / 季卡 / 年卡，对应 1 / 3 / 12 个月
- 下单生成 `vip_order`，模拟支付即置为已支付
- 权益发放（事务内）：

```sql
UPDATE user SET vip_expire_time =
  IF(vip_expire_time IS NULL OR vip_expire_time < NOW(), NOW(), vip_expire_time)
  + INTERVAL #{months} MONTH
WHERE id = #{userId};
```

- 续费按「剩余时长顺延」而非覆盖，避免用户提前续费损失时长
- 管理员手动设置走独立接口，直接覆盖 `vip_expire_time`

### 4.6 点赞规则

**基本规则**

- 需登录，未登录点击直接跳登录
- 一人一题最多一票，重复点击为取消点赞（toggle 语义）
- 点赞前先走 `QuestionAccessChecker`，VIP 题无权限时返回 40301
- 题目由管理员录入，无作者概念，不存在给自己刷赞的场景

**写入流程（事务内，共 4 步）**

```java
// 开启事务（隔离级别用 MySQL 默认的 REPEATABLE READ，无需调整）
//
// 1. 占位 upsert：确保行一定存在，并拿到该唯一键的排他行锁
//    行不存在 → 插入 deleted = 1（未点赞）；行已存在 → 空更新，但同样加排他锁
//    INSERT INTO question_like (user_id, question_id, deleted) VALUES (?, ?, 1)
//    ON DUPLICATE KEY UPDATE deleted = deleted
//    并发的第二个请求会阻塞在这一步，直到前一个事务提交后才继续
//
// 2. 当前读：必须 FOR UPDATE
//    SELECT deleted FROM question_like WHERE user_id = ? AND question_id = ? FOR UPDATE
//    此时行必然存在，直接取 deleted
//
// 3. 推导目标状态与计数增量
//    newDeleted = (oldDeleted == 1) ? 0 : 1;   // 0 = 已点赞，1 = 已取消
//    delta      = (newDeleted == 0) ? +1 : -1; // 点赞 +1，取消 -1
//
// 4. 写入最终状态（行已存在，用 UPDATE 即可）
//    UPDATE question_like SET deleted = #{newDeleted} WHERE user_id = ? AND question_id = ?
//
// 5. 更新冗余计数，不允许减成负数
//    UPDATE question SET like_count = like_count + #{delta}
//    WHERE id = ? AND like_count + #{delta} >= 0
// 提交事务
```

**为什么是这个顺序（三个坑，缺一个就失效）**

1. **不能省掉第 1 步直接 SELECT**：行不存在时，`SELECT ... FOR UPDATE` 加的是 gap lock。两个事务可以同时拿到同一区间的 gap lock，随后各自 INSERT 时插入意向锁互相冲突 → **死锁**，其中一个被回滚。先执行占位 upsert 让行真实存在，并发请求就退化成「阻塞等待 → 走 ON DUPLICATE KEY UPDATE 分支」，从死锁变成正常的锁等待。
2. **第 2 步必须是 `FOR UPDATE`**：MySQL 在 REPEATABLE READ 下普通 SELECT 是**快照读**，读的是事务启动时的数据版本，即使已经持有行锁也读不到另一个事务刚提交的修改。只有 `FOR UPDATE`（或 `LOCK IN SHARE MODE`）才是当前读。少了 `FOR UPDATE`，第 3 步算出的 `delta` 依然基于旧值。
3. **不能依赖 upsert 的返回值**：MySQL 的 `ON DUPLICATE KEY UPDATE` **不返回更新后的行值**（`RETURNING` 是 PostgreSQL 的语法，MySQL 没有），所以 `delta` 必须由第 2 步的显式查询推导。

五步要么全成功要么全回滚。

**并发验证**

两个请求 A、B 同时对同一题点赞，初始无记录：

| 时序 | A | B |
| --- | --- | --- |
| 第 1 步 | 插入 `deleted = 1`，持排他锁 | 唯一键冲突，阻塞等待 |
| 第 2-5 步 | 读到 1 → 置 0 → 计数 +1 | 阻塞中 |
| 提交 | 提交，释放锁 | 获得锁 |
| 第 2 步 | — | 当前读拿到 **0**（A 已提交的最终值） |
| 第 3-5 步 | — | 0 → 1，计数 **-1** |

最终结果：点赞表一行 `deleted = 1`，`like_count = 0`。符合 toggle 语义，与自测用例「并发点两次赞，`like_count` 只 +1」的等价结果一致（两次 toggle = 点赞后取消）。

计数减到 0 为止，不允许负数——避免历史脏数据导致取消点赞时把计数更新成负值。

**前端配合**

按钮点击后立即置灰，等服务端响应返回再恢复，避免用户连点产生连续 toggle。前端可做乐观更新提升手感，但以服务端返回的最终计数为准。

**兜底措施**

提供管理员接口 `POST /api/admin/question/recalc-like`，按 `question_like` 重新统计并校正 `like_count`，用于数据异常时修复，不对外暴露。

**不做什么**

- 不做评论、不做评论点赞：UGC 内容会带来剧透、审核、防刷一整套治理成本，与刷题核心业务不成比例
- 不做匿名点赞：需要额外引入设备指纹或 IP 维度，收益不抵成本
- 点赞不进入刷题统计：点赞只反映题目热度，不参与掌握状态判定

### 4.7 验证码规则

**为什么不放数据库**

验证码是短生命周期、高频读写、无需追溯的数据。建表需要额外的定时清理任务，还会在高并发下产生无意义的写压力。统一放 Redis 并设置 TTL，过期自动回收，表结构保持干净。

**图形验证码**

- 生成：后端用 easy-captcha（或 Hutool `CircleCaptcha`）生成 4 位字母数字干扰图，返回 base64 图片与 `captchaId`（UUID）
- 存储：`captcha:graph:{captchaId}`，TTL 5 分钟
- 校验：不区分大小写；**无论成功还是失败都立即删除**，强制下一次请求重新获取。只校验不销毁的话，攻击者拿到一个通过的 `captchaId` 就能反复重放，验证码形同虚设
- 触发时机（渐进式，避免无谓地伤害正常用户体验）：

  | 场景 | 是否要求验证码 |
  | --- | --- |
  | 注册 | 强制要求 |
  | 登录（正常） | 不要求 |
  | 登录（同一账号连续失败 ≥ 3 次） | 要求 |
  | 登录（账号锁定后首次解锁） | 要求 |
  | 找回密码 | 强制要求 |

- 渐进式的服务端判定：登录失败时 `login_fail_count + 1`，达到 3 次后返回 `422` 且 `requireCaptcha = true`，前端据此展示验证码输入框
- 防刷：同一 IP 每分钟最多获取 20 次验证码

**邮箱验证码**

- 6 位数字，存储 `captcha:mail:{email}`，TTL 10 分钟，校验成功后立即删除
- 同一邮箱 60 秒内只能发送 1 次，同一 IP 每日最多发送 10 次，防止邮件轰炸
- 用途：找回密码、绑定/换绑邮箱。注册不强制邮箱验证，避免抬高演示门槛
- **部署坑**：阿里云轻量服务器默认封禁 25 出站端口，直接连 SMTP 会超时。必须改用 465 SSL（SMTPS）或第三方邮件 API（如阿里云邮件推送）

**登录失败提示统一**

- 无论账号不存在还是密码错误，一律返回「用户名或密码错误」，不区分。否则攻击者可通过提示差异枚举出哪些用户名已注册
- 仅账号被禁用时单独提示，这个信息对正常用户是必要的

**IP 维度限流**

`login_fail_count` 是账号维度的，挡不住「换用户名、同一 IP 持续爆破」。因此在 Redis 增加 IP 维度计数：同一 IP 15 分钟内登录失败 20 次，锁定该 IP 30 分钟。Nginx 层再兜一层请求频率限制。

**前端约定**

- 点击验证码图片刷新（URL 带时间戳防缓存）
- 提交失败后自动刷新验证码，避免用户拿已失效的图反复提交
- 验证码输入框不缓存、不自动填充

---

## 五、前后端项目结构

### 5.1 后端 Spring Boot 工程结构

```
com.learn
├── LearnApplication.java
├── config
│   ├── SaTokenConfig.java            // 拦截器、账号状态校验
│   ├── MyBatisPlusConfig.java        // 分页插件、逻辑删除、自动填充
│   ├── CorsConfig.java
│   └── GlobalExceptionConfig.java    // 全局异常处理
├── controller
│   ├── AuthController.java
│   ├── UserController.java
│   ├── CategoryController.java
│   ├── QuestionController.java
│   ├── PracticeController.java       // 会话、提交、错题本、复习队列
│   ├── FavoriteController.java
│   ├── NoteController.java
│   ├── LikeController.java           // 点赞 toggle
│   ├── VipController.java
│   └── admin
│       ├── AdminUserController.java
│       ├── AdminCategoryController.java
│       └── AdminQuestionController.java   // CRUD + 导入导出
├── service / impl
├── mapper
├── entity                            // 数据库实体（永不直接返回给前端）
├── vo                                // 按场景拆分：List / Practice / Submit / Admin
├── dto                               // 请求体、Excel DTO
├── exception                         // 自定义异常与错误码枚举
├── checker                           // QuestionAccessChecker 统一鉴权
└── util
```

### 5.2 前端 Vue3 + TS 工程结构

```
src
├── api
│   ├── request.ts        // Axios 实例：Token 注入、401 跳登录、40301 弹会员引导
│   ├── auth.ts / user.ts / category.ts / question.ts
│   └── practice.ts / favorite.ts / note.ts / like.ts / vip.ts / admin.ts
├── router                // 路由守卫 + meta.requiresAuth / meta.requiresAdmin
├── stores                // Pinia：user store（含 vipExpireTime 计算属性）
├── types                 // 全局 TS 类型，与后端 VO 一一对应
├── components
│   ├── MarkdownRender.vue    // markdown-it + highlight.js + DOMPurify
│   ├── QuestionCard.vue      // 列表项，不含答案
│   ├── OptionGroup.vue       // 单选/多选/判断选项渲染
│   ├── LikeButton.vue        // 点赞，乐观更新 + 防重复提交
│   └── VipBadge.vue
├── views
│   ├── login / register
│   ├── home
│   ├── question        // 分类浏览、列表、搜索、详情
│   ├── practice        // 刷题页、错题本、复习队列
│   ├── user            // 个人中心、会员页、收藏、笔记、统计
│   └── admin           // 后台全部页面
└── App.vue / main.ts
```

**前端权限处理约定**

- 路由守卫只做「不进不该进的页面」，属于体验层；真正的权限判定全在服务端
- 请求拦截器统一处理：401 清除本地状态跳登录，40301 弹出开通会员引导
- 前端展示 VIP 徽标仅做视觉区分，点击后由服务端返回决定能否查看

---

## 六、核心接口清单

统一返回体：

```json
{ "code": 0, "message": "ok", "data": {} }
```

错误码：`401` 未登录、`403` 无角色权限、`40301` 需开通会员、`404` 资源不存在、`422` 参数校验失败、`500` 服务端异常。

分页统一参数 `pageNum` / `pageSize`，返回 `data: { list, total, pageNum, pageSize }`。

### 6.1 认证与个人中心

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/auth/captcha` | 获取图形验证码，返回 `captchaId` 与 base64 图片 |
| POST | `/api/auth/register` | 注册，需图形验证码，密码 BCrypt 加密 |
| POST | `/api/auth/login` | 登录，失败 3 次起要求验证码，5 次锁定 15 分钟 |
| POST | `/api/auth/logout` | 退出 |
| POST | `/api/auth/email-code` | 发送邮箱验证码（找回密码 / 绑定邮箱） |
| POST | `/api/auth/reset-password` | 邮箱验证码校验通过后重置密码 |
| GET | `/api/user/info` | 个人信息，含 VIP 状态与到期时间 |
| PUT | `/api/user/update` | 修改昵称、邮箱 |
| PUT | `/api/user/password` | 修改密码，需校验原密码 |
| POST | `/api/user/avatar` | 头像上传，≤2MB，jpg/png/webp |

### 6.2 题库与学习

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/category/list` | 分类树 |
| GET | `/api/question/list` | 题目列表，返回 `QuestionListVO`，按权限过滤 |
| GET | `/api/question/search` | 题目搜索，同上 |
| GET | `/api/question/detail` | 答题前详情，返回 `QuestionPracticeVO`，不含答案 |
| POST | `/api/practice/session` | 创建刷题会话，返回 sessionId 与题目总数 |
| GET | `/api/practice/next` | 按 index 取题，返回 `QuestionPracticeVO` |
| POST | `/api/practice/submit` | 提交答题，返回 `QuestionSubmitVO` |
| GET | `/api/practice/wrong/list` | 错题本，越权条目脱敏 |
| GET | `/api/practice/review/list` | 复习队列（`next_review_time <= NOW()`） |
| GET | `/api/practice/stats` | 总答题数、正确率、按分类正确率、连续打卡天数 |
| POST | `/api/favorite/toggle` | 收藏 / 取消收藏 |
| GET | `/api/favorite/list` | 收藏列表，越权条目脱敏 |
| POST | `/api/note/save` | 保存笔记 |
| GET | `/api/note/detail` | 获取某题笔记 |
| POST | `/api/like/toggle` | 点赞 / 取消点赞，返回最新 `likeCount` 与当前 `liked` |

### 6.3 VIP

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/vip/plans` | 套餐列表 |
| POST | `/api/vip/order` | 创建订单 |
| POST | `/api/vip/pay/mock` | 模拟支付 |
| GET | `/api/vip/orders` | 我的订单 |

### 6.4 管理员

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/user/list` | 用户列表 |
| PUT | `/api/admin/user/status` | 禁用 / 启用，并强制登出 |
| PUT | `/api/admin/user/vip` | 手动设置 VIP 过期时间 |
| POST/PUT/DELETE | `/api/admin/category/**` | 分类 CRUD |
| POST/PUT/DELETE | `/api/admin/question/**` | 题目 CRUD、上下架 |
| GET | `/api/admin/excel/template` | 下载导入模板 |
| POST | `/api/admin/excel/import` | 批量导入，参数 `duplicateStrategy`（SKIP/OVERWRITE/ERROR），返回成功/跳过/失败明细 |
| GET | `/api/admin/excel/export` | 按条件批量导出 |
| POST | `/api/admin/question/recalc-like` | 按点赞表重算 `like_count`，数据兜底 |
| GET | `/api/admin/stats` | 数据统计 |

---

## 七、部署方案

1. **服务器**：阿里云轻量应用服务器（Ubuntu 22.04）
2. **环境**：JDK 17、MySQL 8、Nginx、Maven
3. **后端**：`mvn package` 打 Jar，Systemd 守护进程常驻，配置 `Restart=always`
4. **前端**：`npm run build` 产出 dist，部署至 Nginx 静态目录，配置 `try_files` 支持 history 路由
5. **反向代理**：Nginx 托管静态资源，`/api` 前缀转发至 Spring Boot
6. **HTTPS**：Let's Encrypt 证书，certbot 自动续期，80 端口强制跳转 443
7. **安全配置**

   - MySQL 仅监听内网，安全组不放行 3306
   - 安全组仅开放 443 / 22，22 端口限制来源 IP 或改用密钥登录
   - 密码 BCrypt 加密存储；数据库账号密码走环境变量，不入库、不进 Git
   - Nginx 层对登录、注册、验证码接口做限流
   - 上传目录禁止脚本执行
   - 发信使用 465 SSL 或第三方邮件 API，不直连 25 端口（云厂商默认封禁）
8. **备份**：MySQL 每日定时备份，异地留存

---

## 八、开发顺序

1. 初始化前后端脚手架，整合 Sa-Token、全局异常处理、统一返回体、MyBatis-Plus 逻辑删除与自动填充
2. 建表（9 张），生成实体、VO、Mapper，补齐索引
3. 登录注册、图形验证码（渐进式触发）、邮箱验证码、角色与 VIP 字段、账号状态校验与登录锁定
4. `QuestionAccessChecker` 统一鉴权方法 + 答案字段分级（先立规矩，后写业务）
5. 管理员后台：分类 CRUD、题目单条 CRUD
6. 前台题目列表、搜索、详情，接入鉴权
7. 刷题核心：会话、逐题作答、判分、状态表与流水表写入、统计
8. 错题本、复习队列（遗忘曲线调度）
9. 收藏、笔记、点赞（含计数冗余与事务一致性）
10. VIP 套餐与模拟支付
11. EasyExcel 导入导出（含失败明细回传、题干 MD5 查重保证导入幂等）
12. 管理员用户管理、VIP 配置、统计
13. 全功能自测 + 越权专项测试 + Bug 修复
14. 部署上线（HTTPS、Systemd、备份）

---

## 九、自测清单

**权限专项（必须逐条验）**

| 用例 | 期望 |
| --- | --- |
| 普通用户直接请求 `/api/admin/question/list` | 403，无数据 |
| 普通用户用 VIP 题 id 请求题目详情 | 40301 |
| 普通用户直接 POST `/api/practice/submit` 提交 VIP 题 | 40301，不落库 |
| 普通用户收藏 / 给 VIP 题写笔记 / 点赞 | 40301 |
| VIP 过期后访问错题本中的 VIP 题 | 返回脱敏条目，无题干 |
| 管理员被禁用后携带旧 Token 请求 | 401，强制登出 |
| 抓包查看答题前详情响应体 | 不含 answer / analysis 字段 |
| 修改前端路由强行进入 `/admin` | 页面可进，接口全部 403 |

**业务专项**

- 多选题答案乱序（`BD` vs `DB`）判定正确
- 同一题并发提交两次，状态表不报错且计数正确
- 随机刷题翻页无重复、无漏题
- 导入含错行的 Excel：正确行入库，错误行返回行号与原因
- 导入 5000 行不 OOM
- VIP 续费按时长顺延而非覆盖
- 点赞再取消，计数回到原值；重复点击不产生第二条点赞记录
- 同一用户并发发起两次点赞 toggle：点赞表始终只有一行，最终 `deleted` 与 `like_count` 回到初始值（不出现 +2 或死锁回滚）
- 手动把 `like_count` 改成错误值后，调用重算接口能校正回来
- 同一个 `captchaId` 重复使用第二次，校验失败（一次性生效）
- 不存在的用户名与密码错误，返回完全一致的提示（无账号枚举）
- 连续输错 3 次后，登录接口返回 `requireCaptcha = true`
- 同一邮箱 60 秒内重复发送验证码，被拒绝
- 同一份 Excel 连导两次，第二次全部命中跳过，题目总数不变
- 导入策略选 `ERROR` 时，重复行返回行号与「题目重复」
- 一个文件内两行题干相同，只入库一条
- 删除分类时，若分类下仍有启用状态的题目，被拒绝并给出提示
- 已掌握（`mastered = 1`）的题目不再出现在复习队列中
- 软删一个用户后，用相同用户名重新注册成功；再软删一次也不报错

---

## 十、项目亮点与面试追问预案

### 10.1 亮点表述（可直接讲）

- **双层权限架构**：角色权限（admin/user）与资源权限（免费/VIP）解耦，VIP 以过期时间作为资源属性，会员到期无需变更角色，两套逻辑互不干扰
- **答案字段分级**：按答题阶段拆分 VO，答题前置阶段 SQL 层就不查询答案列，从数据出口而非前端隐藏层面杜绝答案泄漏
- **刷题会话机制**：以服务端生成固定题序的会话替代 `ORDER BY RAND()`，解决随机刷题翻页重复与全表排序的性能问题
- **状态表与流水表分离**：状态表（唯一键）服务错题本与复习队列的 O(1) 定位，流水表服务正确率与趋势统计，避免去重存储导致的统计口径失真
- **遗忘曲线复习调度**：基于作答结果动态调整复习间隔（1/2/4/7/15 天），连续答对 3 次或达到最高层级判定为已掌握
- **Excel 批量运维**：流式读取 + 分批入库 + 行级错误回传 + 题干 MD5 查重保证重复导入不翻倍，失败行不阻断整体流程

### 10.2 常见追问与回答要点

| 追问 | 回答要点 |
| --- | --- |
| VIP 到期了怎么处理？ | 不在到期时跑定时任务改状态，而是每次访问实时比较 `vip_expire_time` 与当前时间，避免定时任务延迟或失败导致权限不一致 |
| 前端隐藏了 VIP 题，用户还能绕过吗？ | 前端只是体验层，所有题目出口在服务端统一走 `QuestionAccessChecker`，覆盖列表、搜索、详情、提交、收藏、笔记、错题本、复习队列、点赞共 11 个入口 |
| 用户被禁用后 Token 怎么办？ | Sa-Token 不会自动失效，因此在全局拦截器补了账号状态校验，命中即 logout 并抛 401 |
| 随机刷题怎么保证不重复？ | 会话方案，服务端一次性生成有序 id 列表存 Redis，前端按 index 取题 |
| 为什么分两张表存作答记录？ | 一张唯一键表做当前状态（错题本、复习队列要 O(1) 命中），一张追加表做统计（正确率的分母必须是真实作答次数） |
| 支付是真的吗？ | 模拟支付，但订单状态流转与权益发放逻辑与真实支付完全一致，接支付宝只需替换渠道分支 |
| 数据库设计有什么权衡？ | 逻辑删除保证可追溯，但唯一键不能带 `deleted`（否则同一逻辑键会产生多行、约束失效），改为软删时改写唯一字段或走 upsert 复用行；流水表作为事实记录不设 `deleted`；题干存 Markdown 保证代码题可渲染，但需注意 XSS 过滤 |
| 点赞数为什么冗余存储？ | 列表页每条都 `COUNT(*)` 代价高，冗余后用原子增减维护；代价是要保证与点赞表同事务，并预留按源表重算的兜底接口 |
| 点赞并发怎么保证计数不出错？ | 五步事务：先占位 upsert 让行真实存在（否则两个 gap lock 会互相死锁）→ `SELECT ... FOR UPDATE` 当前读（RR 下普通 SELECT 是快照读，读不到锁内最新值）→ 推导 delta → 更新状态 → `like_count + delta` 原子更新。少了 FOR UPDATE 或占位 upsert，任意一步都会失效 |
| 为什么不做评论？ | 评论是 UGC，会引入剧透（评论里直接写答案会架空答案分级）、审核、防刷一整套治理成本，与刷题核心业务不成比例，做了反而稀释重点 |
| 验证码为什么存 Redis 不建表？ | 短生命周期、高频读写、无需追溯；建表要额外写定时清理任务，还会在高并发下产生无意义写压力，TTL 自动回收更干净 |
| 验证码怎么防止被重放？ | 服务端校验通过或失败都立即删除 key，一次性的。只校验不销毁等于没有验证码 |
| 登录为什么不是每次都要验证码？ | 渐进式触发：正常登录不要求，同一账号失败 3 次后才要求。在安全性与体验之间取平衡，也避免验证码成为正常用户的负担 |
| 登录失败提示为什么不做区分？ | 区分「用户不存在」和「密码错误」会泄漏哪些账号已注册，是典型的账号枚举漏洞，统一提示可规避 |
| 同一份 Excel 导两次会翻倍吗？ | 不会。按「分类 + 题干 MD5」查重，默认 SKIP 策略，也支持 OVERWRITE 与 ERROR；批内还用内存 Set 挡住文件内部的重复行 |
| 唯一键为什么不带 deleted？ | 带了之后同一逻辑键会产生 `(a,0)` 与 `(a,1)` 两行，约束形同虚设且第二次软删必撞键。改为唯一键只约束业务键，软删时改写唯一字段或走 upsert 复用行 |
| 软删除后同名用户名怎么释放？ | 删除时把 username 改写成 `__del_{id}` 再置删除标记，同名账号可无限次「删除→重注册」，后台仍能看清被删记录 |
| 有什么不足？ | 目前单机部署，Redis 会话可平滑迁移到集群；题目检索用了 `LIKE`，量大时应接 Elasticsearch |
