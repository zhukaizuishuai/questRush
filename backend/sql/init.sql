-- ============================================================
-- QuestRush 在线刷题学习平台 - 数据库初始化脚本
-- 设计文档：docs/在线刷题学习平台（含VIP权限）完整设计文档.md 第三章（原样落地）
-- 使用：mysql -uroot -p < init.sql（先执行 CREATE DATABASE 部分）
--
-- 种子数据说明：
--   1. 管理员账号 admin / 初始密码 admin123：因密码需 BCrypt 加密，不在 SQL 中硬编码，
--      由 DataInitializer（CommandLineRunner）在应用启动时检测并插入，初始密码打印在启动日志。
--   2. 种子分类与题目同样由 DataInitializer 幂等插入（表为空才插入）。
-- ============================================================

CREATE DATABASE IF NOT EXISTS `quest_rush` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `quest_rush`;

-- ------------------------------------------------------------
-- 3.1 用户表
-- 唯一键不含 deleted：软删时改写 username/email 释放原名（见应用内 UserMapper.softDelete）
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 3.2 题目分类表（题目只允许挂载在叶子分类上）
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 3.3 题目表
-- title_md5 为题干去空白后的 MD5，服务导入幂等查重；普通索引而非唯一索引（文档 3.3）
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 3.4 题目选项表（判断题同样落两条选项：A.正确 / B.错误；简答题无选项数据）
-- ------------------------------------------------------------
CREATE TABLE `question_option` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `question_id` BIGINT NOT NULL COMMENT '题目 ID',
  `option_code` CHAR(1) NOT NULL COMMENT '选项标识 A/B/C/D',
  `option_content` TEXT NOT NULL COMMENT '选项内容（Markdown）',
  `sort` INT DEFAULT 0 COMMENT '选项展示顺序',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  KEY `idx_question_sort` (`question_id`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目选项表';

-- ------------------------------------------------------------
-- 3.5 用户做题状态表（一题一条；写入统一 INSERT ... ON DUPLICATE KEY UPDATE）
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 3.6 用户作答流水表（文档 3.0 唯一例外：事实流水，只追加，不设 deleted / update_time）
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 3.7 用户收藏表（toggle 语义，翻转同一行 deleted，永不新增行）
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 3.8 用户笔记表（保存走 upsert：ON DUPLICATE KEY UPDATE content = VALUES(content), deleted = 0）
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 3.9 VIP 订单表（MOCK 渠道点击支付即已支付，后置逻辑与真实支付一致）
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 3.10 题目点赞表（一人一题最多一票；取消点赞走软删除）
-- ------------------------------------------------------------
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
