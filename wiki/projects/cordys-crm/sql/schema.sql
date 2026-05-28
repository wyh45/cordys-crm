-- ================================================================
-- CordysCRM 健康管理模块 - 数据库 Schema
-- 字符集: utf8mb4
-- 排序规则: utf8mb4_unicode_ci
-- 主键策略: 雪花ID (String)，非自增
-- ================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------
-- 表1: health_archive — 电子健康档案主表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `health_archive`;
CREATE TABLE `health_archive` (
  `id`             VARCHAR(64)  NOT NULL COMMENT '主键ID（雪花算法）',
  `customer_id`    VARCHAR(64)  DEFAULT NULL COMMENT 'CRM客户ID（关联外部）',
  `customer_name`  VARCHAR(64)  NOT NULL COMMENT '客户姓名',
  `gender`         VARCHAR(8)   DEFAULT NULL COMMENT '性别',
  `age`            INT          DEFAULT NULL COMMENT '年龄',
  `blood_type`     VARCHAR(16)  DEFAULT NULL COMMENT '血型',
  `height`         DECIMAL(5,1) DEFAULT NULL COMMENT '身高(cm)',
  `weight`         DECIMAL(5,1) DEFAULT NULL COMMENT '体重(kg)',
  `phone`          VARCHAR(32)  DEFAULT NULL COMMENT '手机号',
  `idcard_no`      VARCHAR(32)  DEFAULT NULL COMMENT '身份证号',
  `archive_no`     VARCHAR(32)  DEFAULT NULL COMMENT '档案编号（体检系统归档号）',
  `blood_pressure` VARCHAR(32)  DEFAULT NULL COMMENT '血压（如 120/80）',
  `heart_rate`     INT          DEFAULT NULL COMMENT '心率（次/分）',
  `create_time`    BIGINT       NOT NULL COMMENT '创建时间戳',
  `update_time`    BIGINT       NOT NULL COMMENT '更新时间戳',
  PRIMARY KEY (`id`),
  INDEX `idx_idcard_no` (`idcard_no`),
  INDEX `idx_phone`     (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子健康档案主表';

-- ---------------------------------------------------------------
-- 表2: health_examination — 体检记录明细表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `health_examination`;
CREATE TABLE `health_examination` (
  `id`             VARCHAR(64)  NOT NULL COMMENT '主键ID',
  `customer_id`    VARCHAR(64)  NOT NULL COMMENT '健康档案ID（关联 health_archive.id）',
  `exam_no`        VARCHAR(64)  NOT NULL COMMENT '体检号（来源系统唯一标识）',
  `exam_date`      BIGINT       DEFAULT NULL COMMENT '体检日期（时间戳）',
  `exam_item`      VARCHAR(128) DEFAULT NULL COMMENT '检查项目名称',
  `result_value`   VARCHAR(256) DEFAULT NULL COMMENT '检查结果值',
  `reference_range` VARCHAR(128) DEFAULT NULL COMMENT '参考范围',
  `is_abnormal`    TINYINT(1)   DEFAULT 0 COMMENT '是否异常: 0正常 1异常',
  `result_flag`    VARCHAR(8)   DEFAULT NULL COMMENT '结果标志（-正常 ↑偏高 ↓偏低）',
  `create_time`    BIGINT       NOT NULL COMMENT '创建时间戳',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_exam_no` (`exam_no`, `exam_item`),
  INDEX `idx_customer_id` (`customer_id`),
  INDEX `idx_exam_date`   (`exam_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='体检记录明细表';

-- ---------------------------------------------------------------
-- 表3: health_follow_record — 随访记录表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `health_follow_record`;
CREATE TABLE `health_follow_record` (
  `id`             VARCHAR(64)  NOT NULL COMMENT '主键ID',
  `customer_id`    VARCHAR(64)  NOT NULL COMMENT '健康档案ID',
  `follow_date`    BIGINT       DEFAULT NULL COMMENT '随访日期（时间戳）',
  `follow_type`    VARCHAR(32)  DEFAULT NULL COMMENT '随访方式（电话/上门/短信）',
  `follow_result`  TEXT          DEFAULT NULL COMMENT '随访内容/结果',
  `next_follow_date` BIGINT     DEFAULT NULL COMMENT '下次随访日期（时间戳）',
  `create_user`    VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time`    BIGINT       NOT NULL COMMENT '创建时间戳',
  `update_time`    BIGINT       NOT NULL COMMENT '更新时间戳',
  PRIMARY KEY (`id`),
  INDEX `idx_customer_id`     (`customer_id`),
  INDEX `idx_follow_date`     (`follow_date`),
  INDEX `idx_next_follow_date`(`next_follow_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='随访记录表';

-- ---------------------------------------------------------------
-- 表4: health_follow_rule — 随访规则表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `health_follow_rule`;
CREATE TABLE `health_follow_rule` (
  `id`                  VARCHAR(64)  NOT NULL COMMENT '主键ID',
  `name`                VARCHAR(128) NOT NULL COMMENT '规则名称',
  `description`         VARCHAR(512) DEFAULT NULL COMMENT '规则描述',
  `condition_type`      VARCHAR(32)  DEFAULT NULL COMMENT '触发条件类型: BLOOD_TYPE|AGE_RANGE|DIAGNOSIS|ARCHIVE_FIELD|CUSTOM',
  `condition_expr`       TEXT          DEFAULT NULL COMMENT '触发条件表达式（JSON格式）',
  `follow_type`         VARCHAR(32)  DEFAULT NULL COMMENT '随访方式',
  `follow_result_template` TEXT        DEFAULT NULL COMMENT '随访内容模板',
  `follow_interval_days` INT          DEFAULT NULL COMMENT '随访间隔天数',
  `enabled`             TINYINT(1)   DEFAULT 1 COMMENT '是否启用: 0禁用 1启用',
  `priority`            INT          DEFAULT 100 COMMENT '优先级（数字越小优先级越高）',
  `create_time`         BIGINT       NOT NULL COMMENT '创建时间戳',
  `update_time`         BIGINT       NOT NULL COMMENT '更新时间戳',
  PRIMARY KEY (`id`),
  INDEX `idx_enabled`    (`enabled`),
  INDEX `idx_priority`   (`priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='随访规则表';

-- ---------------------------------------------------------------
-- 表5: health_knowledge — 健康知识库表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `health_knowledge`;
CREATE TABLE `health_knowledge` (
  `id`          VARCHAR(64)  NOT NULL COMMENT '主键ID',
  `title`       VARCHAR(256) NOT NULL COMMENT '标题',
  `content`     TEXT          DEFAULT NULL COMMENT '内容（富文本）',
  `tags`        VARCHAR(256)  DEFAULT NULL COMMENT '标签（逗号分隔）',
  `category`    VARCHAR(64)   DEFAULT NULL COMMENT '分类',
  `create_time` BIGINT       NOT NULL COMMENT '创建时间戳',
  `update_time` BIGINT       NOT NULL COMMENT '更新时间戳',
  PRIMARY KEY (`id`),
  INDEX `idx_category` (`category`),
  FULLTEXT INDEX `ft_title_content` (`title`, `content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康知识库表';

-- ---------------------------------------------------------------
-- 表6: health_push_record — 推送记录表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `health_push_record`;
CREATE TABLE `health_push_record` (
  `id`           VARCHAR(64)  NOT NULL COMMENT '主键ID',
  `customer_id`   VARCHAR(64)  NOT NULL COMMENT '健康档案ID',
  `knowledge_id` VARCHAR(64)  DEFAULT NULL COMMENT '推送的知识ID',
  `push_channel` VARCHAR(32)  DEFAULT NULL COMMENT '推送渠道: SMS|IN_APP|WEIXIN',
  `push_status`  VARCHAR(16)  DEFAULT NULL COMMENT '推送状态: PENDING|SUCCESS|FAILED',
  `push_time`    BIGINT       DEFAULT NULL COMMENT '推送时间（时间戳）',
  `error_msg`    VARCHAR(512)  DEFAULT NULL COMMENT '错误信息',
  `create_time`  BIGINT       NOT NULL COMMENT '创建时间戳',
  `update_time`  BIGINT       NOT NULL COMMENT '更新时间戳',
  PRIMARY KEY (`id`),
  INDEX `idx_customer_id`  (`customer_id`),
  INDEX `idx_knowledge_id` (`knowledge_id`),
  INDEX `idx_push_status`  (`push_status`),
  INDEX `idx_push_time`    (`push_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推送记录表';

-- ---------------------------------------------------------------
-- 表7: health_allergy — 过敏史表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `health_allergy`;
CREATE TABLE `health_allergy` (
  `id`           VARCHAR(64)  NOT NULL COMMENT '主键ID',
  `customer_id`   VARCHAR(64)  NOT NULL COMMENT '健康档案ID',
  `allergen`     VARCHAR(128)  DEFAULT NULL COMMENT '过敏原',
  `severity`     VARCHAR(16)   DEFAULT NULL COMMENT '严重程度: MILD|MODERATE|SEVERE',
  `create_time`  BIGINT       NOT NULL COMMENT '创建时间戳',
  `update_time`  BIGINT       NOT NULL COMMENT '更新时间戳',
  PRIMARY KEY (`id`),
  INDEX `idx_customer_id` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='过敏史表';

-- ---------------------------------------------------------------
-- 表8: health_medical_history — 病史表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `health_medical_history`;
CREATE TABLE `health_medical_history` (
  `id`             VARCHAR(64)  NOT NULL COMMENT '主键ID',
  `customer_id`    VARCHAR(64)  NOT NULL COMMENT '健康档案ID',
  `disease_name`   VARCHAR(128)  DEFAULT NULL COMMENT '疾病名称',
  `diagnosis_date` BIGINT        DEFAULT NULL COMMENT '诊断日期（时间戳）',
  `treatment_status` VARCHAR(32)  DEFAULT NULL COMMENT '治疗状态: TREATED|ONGOING|CURED',
  `family_history` VARCHAR(512)  DEFAULT NULL COMMENT '家族病史',
  `remarks`        VARCHAR(512)  DEFAULT NULL COMMENT '备注',
  `create_time`    BIGINT       NOT NULL COMMENT '创建时间戳',
  `update_time`    BIGINT       NOT NULL COMMENT '更新时间戳',
  PRIMARY KEY (`id`),
  INDEX `idx_customer_id` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='病史表';

-- ---------------------------------------------------------------
-- 表9: health_vaccination — 疫苗接种表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `health_vaccination`;
CREATE TABLE `health_vaccination` (
  `id`             VARCHAR(64)  NOT NULL COMMENT '主键ID',
  `customer_id`     VARCHAR(64)  NOT NULL COMMENT '健康档案ID',
  `vaccine_name`    VARCHAR(128)  DEFAULT NULL COMMENT '疫苗名称',
  `vaccinate_date`  BIGINT        DEFAULT NULL COMMENT '接种日期（时间戳）',
  `next_dose_date`  BIGINT        DEFAULT NULL COMMENT '下一针日期（时间戳）',
  `dose_number`    INT           DEFAULT NULL COMMENT '剂次编号（第几针）',
  `create_time`    BIGINT       NOT NULL COMMENT '创建时间戳',
  `update_time`    BIGINT       NOT NULL COMMENT '更新时间戳',
  PRIMARY KEY (`id`),
  INDEX `idx_customer_id`  (`customer_id`),
  INDEX `idx_vaccinate_date` (`vaccinate_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='疫苗接种表';

-- ---------------------------------------------------------------
-- 表10: health_archive_mapping — 档案-体检映射表（去重）
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `health_archive_mapping`;
CREATE TABLE `health_archive_mapping` (
  `id`           VARCHAR(64)  NOT NULL COMMENT '主键ID',
  `idcard_no`    VARCHAR(64)  DEFAULT NULL COMMENT '身份证号',
  `customer_id`  VARCHAR(64)  DEFAULT NULL COMMENT 'CRM客户ID',
  `archive_id`   VARCHAR(64)  NOT NULL COMMENT '健康档案ID（关联 health_archive.id）',
  `exam_no`      VARCHAR(64)  NOT NULL COMMENT '体检号（来源系统唯一标识）',
  `exam_date`    BIGINT        DEFAULT NULL COMMENT '体检日期（时间戳）',
  `sync_status`  INT          DEFAULT 0 COMMENT '同步状态: 0=待同步 1=已同步 2=失败',
  `error_msg`    VARCHAR(512)  DEFAULT NULL COMMENT '错误信息',
  `create_time`  BIGINT       NOT NULL COMMENT '创建时间戳',
  `update_time`  BIGINT       NOT NULL COMMENT '更新时间戳',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_exam_no` (`exam_no`),
  UNIQUE INDEX `uk_idcard_archive` (`idcard_no`, `archive_id`),
  INDEX `idx_archive_id`  (`archive_id`),
  INDEX `idx_sync_status`(`sync_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='档案-体检映射表（去重）';

-- ---------------------------------------------------------------
-- 初始数据: 随访规则模板（2条示例规则）
-- ---------------------------------------------------------------
INSERT INTO `health_follow_rule` (`id`, `name`, `description`, `condition_type`, `condition_expr`, `follow_type`, `follow_result_template`, `follow_interval_days`, `enabled`, `priority`, `create_time`, `update_time`) VALUES
(
  'init-rule-001',
  'O型血客户季度随访',
  'O型血客户每季度进行健康随访',
  'BLOOD_TYPE',
  '{"field":"bloodType","op":"eq","value":"O"}',
  '电话',
  '您好，您作为O型血用户，最近身体状况如何？',
  90,
  1,
  1,
  UNIX_TIMESTAMP() * 1000,
  UNIX_TIMESTAMP() * 1000
),
(
  'init-rule-002',
  '50岁以上年度体检随访',
  '50岁以上客户每年体检后进行健康跟踪',
  'AGE_RANGE',
  '{"field":"age","op":"gte","value":50}',
  '短信',
  '您好，请注意您年度体检报告已生成，请及时查看。',
  365,
  1,
  2,
  UNIX_TIMESTAMP() * 1000,
  UNIX_TIMESTAMP() * 1000
);

SET FOREIGN_KEY_CHECKS = 1;