-- =============================================
-- 网格化管理系统 数据库初始化脚本
-- MySQL 5.7+
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------
-- 1. 部门表 sys_dept
-- ---------------------------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`        varchar(100) NOT NULL COMMENT '部门名称',
  `code`        varchar(50)  NOT NULL COMMENT '部门编码',
  `parent_id`   bigint(20)   DEFAULT NULL COMMENT '父部门ID',
  `leader`      varchar(50)  DEFAULT NULL COMMENT '负责人',
  `phone`       varchar(20)  DEFAULT NULL COMMENT '联系电话',
  `sort`        int(11)      NOT NULL DEFAULT 0 COMMENT '排序',
  `status`      tinyint(4)   NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `created_at`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- ---------------------------------------------
-- 2. 用户表 sys_user
-- ---------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username`    varchar(50)  NOT NULL COMMENT '用户名',
  `password`    varchar(100) NOT NULL COMMENT '密码(BCrypt加密)',
  `real_name`   varchar(50)  DEFAULT NULL COMMENT '真实姓名',
  `phone`       varchar(20)  DEFAULT NULL COMMENT '手机号',
  `email`       varchar(100) DEFAULT NULL COMMENT '邮箱',
  `avatar`      varchar(255) DEFAULT NULL COMMENT '头像URL',
  `status`      tinyint(4)   NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `role`        varchar(30)  NOT NULL DEFAULT 'worker' COMMENT '角色：admin-系统管理员 street_manager-街道管理员 grid_leader-网格长 worker-网格员 handler-处置员 supervisor-督查员',
  `dept_id`     bigint(20)   DEFAULT NULL COMMENT '部门ID',
  `grid_id`     bigint(20)   DEFAULT NULL COMMENT '所属网格ID',
  `openid`      varchar(100) DEFAULT NULL COMMENT '微信OpenID',
  `created_at`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_grid_id` (`grid_id`),
  KEY `idx_role` (`role`),
  KEY `idx_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ---------------------------------------------
-- 3. 网格表 grid_info
-- ---------------------------------------------
DROP TABLE IF EXISTS `grid_info`;
CREATE TABLE `grid_info` (
  `id`              bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `grid_code`       varchar(50)  NOT NULL COMMENT '网格编码',
  `grid_name`       varchar(100) NOT NULL COMMENT '网格名称',
  `grid_level`      tinyint(4)   NOT NULL DEFAULT 3 COMMENT '网格层级：1-街道 2-社区 3-网格 4-微网格',
  `parent_id`       bigint(20)   DEFAULT NULL COMMENT '父级网格ID',
  `grid_leader_id`  bigint(20)   DEFAULT NULL COMMENT '网格长用户ID',
  `area`            decimal(10,2) DEFAULT NULL COMMENT '面积(平方公里)',
  `boundary`        text         DEFAULT NULL COMMENT '边界坐标(GeoJSON)',
  `lng`             decimal(10,6) DEFAULT NULL COMMENT '中心经度',
  `lat`             decimal(10,6) DEFAULT NULL COMMENT '中心纬度',
  `address`         varchar(255) DEFAULT NULL COMMENT '地址',
  `sort`            int(11)      NOT NULL DEFAULT 0 COMMENT '排序',
  `status`          tinyint(4)   NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `created_at`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_grid_code` (`grid_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_grid_level` (`grid_level`),
  KEY `idx_grid_leader_id` (`grid_leader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网格表';

-- ---------------------------------------------
-- 4. 网格员表 grid_member
-- ---------------------------------------------
DROP TABLE IF EXISTS `grid_member`;
CREATE TABLE `grid_member` (
  `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `grid_id`     bigint(20)   NOT NULL COMMENT '网格ID',
  `user_id`     bigint(20)   NOT NULL COMMENT '用户ID',
  `member_type` varchar(30)  NOT NULL DEFAULT 'worker' COMMENT '成员类型：grid_leader-网格长 worker-网格员',
  `created_at`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_grid_user` (`grid_id`, `user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_member_type` (`member_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网格员表';

-- ---------------------------------------------
-- 5. 事件表 event_info
-- ---------------------------------------------
DROP TABLE IF EXISTS `event_info`;
CREATE TABLE `event_info` (
  `id`                  bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `client_id`           varchar(64)  DEFAULT NULL COMMENT '客户端生成ID，用于离线同步幂等去重',
  `event_no`            varchar(50)  NOT NULL COMMENT '事件编号',
  `title`               varchar(200) NOT NULL COMMENT '事件标题',
  `event_type`          varchar(50)  NOT NULL COMMENT '事件类型',
  `description`         text         DEFAULT NULL COMMENT '事件描述',
  `lng`                 decimal(10,6) DEFAULT NULL COMMENT '经度',
  `lat`                 decimal(10,6) DEFAULT NULL COMMENT '纬度',
  `address`             varchar(255) DEFAULT NULL COMMENT '地址',
  `images`              text         DEFAULT NULL COMMENT '图片URL，多个逗号分隔',
  `videos`              text         DEFAULT NULL COMMENT '视频URL，多个逗号分隔',
  `voice_url`           varchar(255) DEFAULT NULL COMMENT '语音描述URL',
  `anonymous`           tinyint(1)   NOT NULL DEFAULT 0 COMMENT '是否匿名：0-否 1-是',
  `reporter_id`         bigint(20)   DEFAULT NULL COMMENT '上报人ID',
  `reporter_name`       varchar(50)  DEFAULT NULL COMMENT '上报人姓名',
  `reporter_phone`      varchar(20)  DEFAULT NULL COMMENT '上报人电话',
  `grid_id`             bigint(20)   DEFAULT NULL COMMENT '所属网格ID',
  `status`              varchar(30)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待受理 APPROVED-已受理 DISPATCHED-已分派 HANDLED-已处置 COMPLETED-已办结 REJECTED-已驳回',
  `priority`            varchar(30)  NOT NULL DEFAULT 'NORMAL' COMMENT '优先级：LOW-低 NORMAL-中 HIGH-高 URGENT-紧急',
  `event_timestamp`     datetime     DEFAULT NULL COMMENT '客户端上报时间戳（离线同步时排序用）',
  `dispatched_at`       datetime     DEFAULT NULL COMMENT '分派处置时间（计时起点）',
  `deadline_at`         datetime     DEFAULT NULL COMMENT '处置截止时间',
  `urge_level`          int(11)      DEFAULT 0 COMMENT '催办等级：0-未催办 1-黄色预警 2-红色超时 3-升级督办',
  `is_high_recurrence`  tinyint(1)   NOT NULL DEFAULT 0 COMMENT '是否高复发事件：0-否 1-是',
  `recurrence_count`    int(11)      NOT NULL DEFAULT 1 COMMENT '该地点同类事件复发次数',
  `recurrence_group_key` varchar(100) DEFAULT NULL COMMENT '复发分组key（eventType+lng+lat哈希后生成）',
  `process_instance_id` varchar(64)  DEFAULT NULL COMMENT 'Flowable流程实例ID',
  `created_at`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`             tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_no` (`event_no`),
  UNIQUE KEY `uk_client_id` (`client_id`),
  KEY `idx_grid_id` (`grid_id`),
  KEY `idx_reporter_id` (`reporter_id`),
  KEY `idx_status` (`status`),
  KEY `idx_priority` (`priority`),
  KEY `idx_event_type` (`event_type`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_event_timestamp` (`event_timestamp`),
  KEY `idx_dispatched_at` (`dispatched_at`),
  KEY `idx_deadline_at` (`deadline_at`),
  KEY `idx_urge_level` (`urge_level`),
  KEY `idx_is_high_recurrence` (`is_high_recurrence`),
  KEY `idx_recurrence_group_key` (`recurrence_group_key`),
  KEY `idx_process_instance_id` (`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件表';

-- ---------------------------------------------
-- 6. 催办规则表 event_urge_rule
-- ---------------------------------------------
DROP TABLE IF EXISTS `event_urge_rule`;
CREATE TABLE `event_urge_rule` (
  `id`               bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `event_type`       varchar(50) NOT NULL COMMENT '事件类型编码',
  `event_type_name` varchar(100) DEFAULT NULL COMMENT '事件类型名称',
  `time_limit_hours` int(11) NOT NULL DEFAULT 24 COMMENT '处置时限（小时）',
  `warning_ratio`    decimal(5,2) NOT NULL DEFAULT 0.20 COMMENT '预警阈值比例，剩余时间低于此值触发黄色预警',
  `escalate_level`  varchar(20) DEFAULT 'GRID_LEADER' COMMENT '超时升级对象：WORKER GRID_LEADER SUPERVISOR ADMIN',
  `enabled`          tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`          tinyint(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_type` (`event_type`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='催办规则表';

-- ---------------------------------------------
-- 7. 催办模板表 event_urge_template
-- ---------------------------------------------
DROP TABLE IF EXISTS `event_urge_template`;
CREATE TABLE `event_urge_template` (
  `id`               bigint(20) NOT NULL AUTO_INCREMENT,
  `template_code`    varchar(50) NOT NULL COMMENT '模板编码：WARNING-预警 URGENT-超时 ESCALATE-升级',
  `template_name`    varchar(100) NOT NULL COMMENT '模板名称',
  `title_template`   varchar(200) NOT NULL COMMENT '标题模板',
  `content_template` text NOT NULL COMMENT '内容模板',
  `channel`          varchar(50) NOT NULL DEFAULT 'INNER' COMMENT '接收渠道：INNER SMS EMAIL WECHAT 多渠道逗号分隔',
  `enabled`          tinyint(1) NOT NULL DEFAULT 1,
  `created_at`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`          tinyint(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='催办模板表';

-- ---------------------------------------------
-- 8. 催办记录表 event_urge_record
-- ---------------------------------------------
DROP TABLE IF EXISTS `event_urge_record`;
CREATE TABLE `event_urge_record` (
  `id`            bigint(20) NOT NULL AUTO_INCREMENT,
  `event_id`      bigint(20) NOT NULL COMMENT '事件ID',
  `event_no`      varchar(50) DEFAULT NULL,
  `urge_level`    int(11) NOT NULL COMMENT '催办等级：1预警 2超时 3升级',
  `rule_id`       bigint(20) DEFAULT NULL,
  `template_id`   bigint(20) DEFAULT NULL,
  `title`         varchar(200) DEFAULT NULL,
  `content`       text,
  `channel`       varchar(50) DEFAULT NULL,
  `receiver_id`   bigint(20) DEFAULT NULL,
  `receiver_name` varchar(50) DEFAULT NULL,
  `send_status`   int(11) NOT NULL DEFAULT 0 COMMENT '0待发 1已发 2失败',
  `error_msg`     varchar(500) DEFAULT NULL,
  `is_read`       tinyint(4) NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
  `read_at`       datetime DEFAULT NULL COMMENT '读取时间',
  `created_at`    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       tinyint(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_event_id` (`event_id`),
  KEY `idx_urge_level` (`urge_level`),
  KEY `idx_send_status` (`send_status`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='催办记录表';

-- ---------------------------------------------
-- 9. 事件处理记录表 event_process
-- ---------------------------------------------
DROP TABLE IF EXISTS `event_process`;
CREATE TABLE `event_process` (
  `id`                bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `event_id`          bigint(20)   NOT NULL COMMENT '事件ID',
  `task_id`           bigint(20)   DEFAULT NULL COMMENT '任务ID',
  `node_name`         varchar(100) NOT NULL COMMENT '节点名称',
  `handler_id`        bigint(20)   DEFAULT NULL COMMENT '处理人ID',
  `handler_name`      varchar(50)  DEFAULT NULL COMMENT '处理人姓名',
  `action`            varchar(30)  NOT NULL COMMENT '操作：submit-提交 accept-受理 assign-分派 process-处理 complete-办结 reject-驳回',
  `comment`           text         DEFAULT NULL COMMENT '处理意见',
  `attachments`       text         DEFAULT NULL COMMENT '附件URL，多个逗号分隔',
  `handle_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '处理时间',
  `duration_seconds`  int(11)      DEFAULT NULL COMMENT '处理耗时(秒)',
  PRIMARY KEY (`id`),
  KEY `idx_event_id` (`event_id`),
  KEY `idx_handler_id` (`handler_id`),
  KEY `idx_action` (`action`),
  KEY `idx_handle_time` (`handle_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件处理记录表';

-- ---------------------------------------------
-- 7. 事件评价表 event_evaluation
-- ---------------------------------------------
DROP TABLE IF EXISTS `event_evaluation`;
CREATE TABLE `event_evaluation` (
  `id`            bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `event_id`      bigint(20)   NOT NULL COMMENT '事件ID',
  `reporter_id`   bigint(20)   NOT NULL COMMENT '评价人ID',
  `speed_score`   tinyint(4)   NOT NULL DEFAULT 5 COMMENT '速度评分：1-5',
  `effect_score`  tinyint(4)   NOT NULL DEFAULT 5 COMMENT '效果评分：1-5',
  `content`       text         DEFAULT NULL COMMENT '评价内容',
  `created_at`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_reporter` (`event_id`, `reporter_id`),
  KEY `idx_reporter_id` (`reporter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件评价表';

-- ---------------------------------------------
-- 8. 图像比对表 event_image_comparison
-- ---------------------------------------------
DROP TABLE IF EXISTS `event_image_comparison`;
CREATE TABLE `event_image_comparison` (
  `id`                  bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `event_id`            bigint(20)   NOT NULL COMMENT '事件ID',
  `process_id`          bigint(20)   DEFAULT NULL COMMENT '关联处置记录ID',
  `before_image`        varchar(255) NOT NULL COMMENT '处置前图片URL（上报图片）',
  `after_image`         varchar(255) NOT NULL COMMENT '处置后图片URL',
  `similarity`          decimal(5,2) NOT NULL COMMENT '相似度 0-100',
  `heatmap_image`       varchar(255) DEFAULT NULL COMMENT '热力图URL',
  `judgment`          varchar(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'AI判定：PASS-合格 FAIL-不合格 PENDING-待判定',
  `judgment_reason`     varchar(500) DEFAULT NULL COMMENT 'AI判定理由',
  `created_at`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_event_id` (`event_id`),
  KEY `idx_process_id` (`process_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件图像比对表';

-- ---------------------------------------------
-- 9. 通知表 sys_notification
-- ---------------------------------------------
DROP TABLE IF EXISTS `sys_notification`;
CREATE TABLE `sys_notification` (
  `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`     bigint(20)   NOT NULL COMMENT '接收用户ID',
  `title`       varchar(200) NOT NULL COMMENT '通知标题',
  `content`     text         DEFAULT NULL COMMENT '通知内容',
  `type`        varchar(30)  NOT NULL DEFAULT 'system' COMMENT '类型：system-系统通知 event-事件通知 task-任务通知',
  `biz_id`      bigint(20)   DEFAULT NULL COMMENT '业务ID',
  `is_read`     tinyint(1)   NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读 1-已读',
  `created_at`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_type` (`type`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- =============================================
-- 基础测试数据
-- =============================================

-- ---------------------------------------------
-- 部门数据（3个）
-- ---------------------------------------------
INSERT INTO `sys_dept` (`id`, `name`, `code`, `parent_id`, `leader`, `phone`, `sort`, `status`) VALUES
(1, '总公司', 'HQ', NULL, '张总', '13800000000', 1, 1),
(2, '网格管理部', 'GRID', 1, '李部长', '13800000001', 1, 1),
(3, '事件处置中心', 'EVENT', 1, '王主任', '13800000002', 2, 1),
(4, '市政科', 'MUNICIPAL', 3, '赵科', '13800000003', 1, 1),
(5, '环卫科', 'ENVIRONMENTAL', 3, '钱科', '13800000004', 2, 1),
(6, '综合执法大队', 'LAW_ENFORCEMENT', 3, '孙队', '13800000005', 3, 1),
(7, '交通科', 'TRAFFIC', 3, '周科', '13800000006', 4, 1),
(8, '安监科', 'SAFETY', 3, '吴科', '13800000007', 5, 1),
(9, '民政科', 'CIVIL_AFFAIRS', 3, '郑科', '13800000008', 6, 1),
(10, '治安科', 'PUBLIC_SECURITY', 3, '冯科', '13800000009', 7, 1),
(11, '城管科', 'URBAN_MANAGEMENT', 3, '陈科', '13800000010', 8, 1),
(12, '水务科', 'WATER', 3, '褚科', '13800000011', 9, 1),
(13, '园林绿化科', 'GREENING', 3, '卫科', '13800000012', 10, 1),
(14, '住建科', 'HOUSING', 3, '蒋科', '13800000013', 11, 1),
(15, '电力科', 'POWER', 3, '沈科', '13800000014', 12, 1),
(16, '综合协调科', 'OTHER', 3, '韩科', '13800000015', 13, 1);

-- ---------------------------------------------
-- 网格数据（街道-社区-网格-微网格 四级结构）
-- ---------------------------------------------
INSERT INTO `grid_info` (`id`, `grid_code`, `grid_name`, `grid_level`, `parent_id`, `grid_leader_id`, `area`, `lng`, `lat`, `address`, `sort`, `status`) VALUES
-- 街道级（1级）
(1, 'STREET001', '城东区街道', 1, NULL, NULL, 15.50, 116.407400, 39.904200, '北京市东城区', 1, 1),
(2, 'STREET002', '城西区街道', 1, NULL, NULL, 18.20, 116.397400, 39.914200, '北京市西城区', 2, 1),

-- 社区级（2级）
(3, 'COMM001', '长安街社区', 2, 1, NULL, 5.20, 116.405400, 39.906200, '东城区长安街社区', 1, 1),
(4, 'COMM002', '王府井社区', 2, 1, NULL, 4.80, 116.412400, 39.908200, '东城区王府井社区', 2, 1),
(5, 'COMM003', '金融街社区', 2, 2, NULL, 6.50, 116.395400, 39.916200, '西城区金融街社区', 1, 1),

-- 网格级（3级）
(6, 'GRID001', '长安街第一网格', 3, 3, NULL, 2.50, 116.404400, 39.905200, '东城区长安街1号', 1, 1),
(7, 'GRID002', '长安街第二网格', 3, 3, NULL, 2.70, 116.406400, 39.907200, '东城区长安街2号', 2, 1),
(8, 'GRID003', '王府井第一网格', 3, 4, NULL, 2.30, 116.411400, 39.907200, '东城区王府井大街1号', 1, 1),
(9, 'GRID004', '金融街第一网格', 3, 5, NULL, 3.00, 116.394400, 39.915200, '西城区金融街1号', 1, 1),

-- 微网格级（4级）
(10, 'MICRO001', '长安街1号微网格', 4, 6, NULL, 0.80, 116.403800, 39.904800, '东城区长安街1号院', 1, 1),
(11, 'MICRO002', '长安街2号微网格', 4, 6, NULL, 0.90, 116.405000, 39.905600, '东城区长安街2号院', 2, 1),
(12, 'MICRO003', '王府井A区微网格', 4, 8, NULL, 1.00, 116.410800, 39.906800, '东城区王府井A区', 1, 1);

-- ---------------------------------------------
-- 用户数据
-- 密码统一为：123456（BCrypt加密后）
-- ---------------------------------------------
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `phone`, `email`, `status`, `role`, `dept_id`, `grid_id`) VALUES
(1, 'admin',         '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员',   '13900000001', 'admin@example.com',         1, 'admin',          1, NULL),
(2, 'street_mgr1',   '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '街道管理员刘一', '13900000002', 'street1@example.com',       1, 'street_manager', 2, 1),
(3, 'street_mgr2',   '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '街道管理员陈二', '13900000003', 'street2@example.com',       1, 'street_manager', 2, 2),
(4, 'grid_leader1',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '网格长张三',   '13900000004', 'leader1@example.com',       1, 'grid_leader',    2, 6),
(5, 'grid_leader2',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '网格长李四',   '13900000005', 'leader2@example.com',       1, 'grid_leader',    2, 8),
(6, 'worker1',       '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '网格员王五',   '13900000006', 'worker1@example.com',       1, 'worker',         2, 10),
(7, 'worker2',       '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '网格员赵六',   '13900000007', 'worker2@example.com',       1, 'worker',         2, 11),
(8, 'worker3',       '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '网格员孙七',   '13900000008', 'worker3@example.com',       1, 'worker',         2, 12),
(9, 'handler1',      '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '市政处置员周八',   '13900000009', 'handler1@example.com',      1, 'handler',        4, NULL),
(10, 'supervisor1',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '督查员吴九',   '13900000010', 'supervisor1@example.com',   1, 'supervisor',     1, 1),
(11, 'handler_env',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '环卫处置员郑十',   '13900000011', 'handler_env@example.com',   1, 'handler',        5, NULL),
(12, 'handler_law',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '执法队员冯十一',   '13900000012', 'handler_law@example.com',   1, 'handler',        6, NULL),
(13, 'handler_tra',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '交通处置员陈十二',   '13900000013', 'handler_tra@example.com',   1, 'handler',        7, NULL),
(14, 'handler_safe', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '安监处置员褚十三',   '13900000014', 'handler_safe@example.com',  1, 'handler',        8, NULL),
(15, 'handler_civil','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '民政处置员卫十四',   '13900000015', 'handler_civil@example.com', 1, 'handler',        9, NULL),
(16, 'handler_police','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '治安处置员蒋十五',  '13900000016', 'handler_police@example.com',1, 'handler',        10, NULL),
(17, 'handler_urban','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '城管处置员沈十六',   '13900000017', 'handler_urban@example.com', 1, 'handler',        11, NULL),
(18, 'handler_water','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '水务处置员韩十七',   '13900000018', 'handler_water@example.com', 1, 'handler',        12, NULL),
(19, 'handler_green','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '绿化处置员杨十八',   '13900000019', 'handler_green@example.com', 1, 'handler',        13, NULL),
(20, 'handler_house','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '住建处置员朱十九',   '13900000020', 'handler_house@example.com', 1, 'handler',        14, NULL),
(21, 'handler_power','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '电力处置员秦二十',   '13900000021', 'handler_power@example.com', 1, 'handler',        15, NULL),
(22, 'handler_other','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '综合协调员尤廿一',   '13900000022', 'handler_other@example.com', 1, 'handler',        16, NULL);

-- 回填网格长ID
UPDATE `grid_info` SET `grid_leader_id` = 4 WHERE `id` = 6;
UPDATE `grid_info` SET `grid_leader_id` = 5 WHERE `id` = 8;

-- ---------------------------------------------
-- 网格员关联数据
-- ---------------------------------------------
INSERT INTO `grid_member` (`grid_id`, `user_id`, `member_type`) VALUES
(6, 4, 'grid_leader'),
(8, 5, 'grid_leader'),
(10, 6, 'worker'),
(11, 7, 'worker'),
(12, 8, 'worker'),
(6, 6, 'worker'),
(6, 7, 'worker'),
(8, 8, 'worker');

-- ---------------------------------------------
-- 示例事件数据
-- ---------------------------------------------
INSERT INTO `event_info` (`id`, `event_no`, `title`, `event_type`, `description`, `lng`, `lat`, `address`, `anonymous`, `reporter_id`, `reporter_name`, `reporter_phone`, `grid_id`, `status`, `priority`) VALUES
(1, 'EV202401010001', '路灯损坏', 'public_facility', '长安街1号微网格区域内主路灯不亮，夜间通行不便。', 116.403800, 39.904800, '东长安街1号院门口', 0, 6, '王五', '13900000006', 10, 'PENDING', 'NORMAL'),
(2, 'EV202401010002', '下水道堵塞', 'environment', '王府井A区微网格居民小区门口下水道堵塞，污水外流。', 116.410800, 39.906800, '王府井A区北门', 0, 8, '孙七', '13900000008', 12, 'PROCESSING', 'HIGH'),
(3, 'EV202401010003', '垃圾清运不及时', 'environment', '长安街第二网格区域内垃圾桶满溢，异味严重。', 116.406400, 39.907200, '长安街2号院旁', 0, 7, '赵六', '13900000007', 11, 'PENDING', 'LOW'),
(4, 'EV202401010004', '邻里纠纷', 'dispute', '楼上楼下因噪音问题产生矛盾，需要调解。', 116.404400, 39.905200, '长安街第一网格3号楼', 1, NULL, '匿名居民', '13800000001', 6, 'COMPLETED', 'NORMAL');

-- ---------------------------------------------
-- 示例事件处理记录
-- ---------------------------------------------
INSERT INTO `event_process` (`event_id`, `node_name`, `handler_id`, `handler_name`, `action`, `comment`, `handle_time`) VALUES
(1, '事件上报',   6, '王五', 'submit', '发现路灯损坏，请求维修。', '2024-01-01 09:30:00'),
(2, '事件上报',   8, '孙七', 'submit', '下水道堵塞严重，需紧急处理。', '2024-01-01 10:15:00'),
(2, '事件受理',   5, '李四', 'accept', '已受理，已分派至处置中心。', '2024-01-01 10:30:00'),
(3, '事件上报',   7, '赵六', 'submit', '垃圾桶满溢多日，影响环境。', '2024-01-02 08:45:00'),
(4, '事件上报',   NULL, '匿名居民', 'submit', '噪音扰民，希望协调解决。', '2024-01-02 14:20:00'),
(4, '事件受理',   4, '张三', 'accept', '已受理，安排网格员调解。', '2024-01-02 15:00:00'),
(4, '事件处置',   6, '王五', 'process', '已上门调解，双方达成和解。', '2024-01-02 17:30:00'),
(4, '事件办结',   4, '张三', 'complete', '事件已办结，居民满意。', '2024-01-02 18:00:00');

-- ---------------------------------------------
-- 示例通知
-- ---------------------------------------------
INSERT INTO `sys_notification` (`user_id`, `title`, `content`, `type`, `biz_id`, `is_read`) VALUES
(4, '新事件待受理', '您有2条新的事件待受理', 'event', 1, 0),
(5, '新事件待受理', '您有1条新的事件待受理', 'event', 2, 0),
(9, '新任务待处理', '您有1条新的任务待处理：下水道堵塞', 'task', 2, 0),
(6, '事件受理通知', '您上报的事件【路灯损坏】已受理', 'event', 1, 1),
(7, '事件受理通知', '您上报的事件【垃圾清运不及时】已受理', 'event', 3, 0),
(10, '督查提醒', '长安街社区有1件逾期未处理事件', 'system', NULL, 0);

-- ---------------------------------------------
-- 9. 周边资源：摄像头 resource_camera
-- ---------------------------------------------
DROP TABLE IF EXISTS `resource_camera`;
CREATE TABLE `resource_camera` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `camera_code` varchar(50) NOT NULL COMMENT '摄像头编号',
  `camera_name` varchar(100) NOT NULL COMMENT '摄像头名称',
  `camera_type` varchar(30) NOT NULL DEFAULT 'public' COMMENT '类型：public-公共治安 traffic-交通监控 private-小区监控',
  `lng` decimal(10,6) NOT NULL COMMENT '经度',
  `lat` decimal(10,6) NOT NULL COMMENT '纬度',
  `address` varchar(255) DEFAULT NULL COMMENT '地址',
  `rtsp_url` varchar(500) DEFAULT NULL COMMENT 'RTSP流地址',
  `hls_url` varchar(500) DEFAULT NULL COMMENT 'HLS播放地址',
  `grid_id` bigint(20) DEFAULT NULL COMMENT '所属网格ID',
  `manufacturer` varchar(100) DEFAULT NULL COMMENT '厂商(海康/大华等)',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '0-离线 1-在线',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_camera_code` (`camera_code`),
  KEY `idx_lng_lat` (`lng`,`lat`), KEY `idx_grid_id` (`grid_id`), KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='周边资源-摄像头';

-- ---------------------------------------------
-- 10. 周边资源：应急物资 resource_emergency
-- ---------------------------------------------
DROP TABLE IF EXISTS `resource_emergency`;
CREATE TABLE `resource_emergency` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `resource_code` varchar(50) NOT NULL COMMENT '物资编号',
  `resource_name` varchar(100) NOT NULL COMMENT '物资名称(消防栓/灭火器/AED急救箱/应急包等)',
  `resource_type` varchar(30) NOT NULL COMMENT 'fire_fighting-消防 first_aid-急救 flood_ctr-防汛 medical-医疗 rescue-救援',
  `quantity` int(11) NOT NULL DEFAULT 1 COMMENT '数量',
  `lng` decimal(10,6) NOT NULL COMMENT '经度',
  `lat` decimal(10,6) NOT NULL COMMENT '纬度',
  `address` varchar(255) DEFAULT NULL COMMENT '位置',
  `grid_id` bigint(20) DEFAULT NULL COMMENT '所属网格',
  `manager` varchar(50) DEFAULT NULL COMMENT '管理员姓名',
  `manager_phone` varchar(20) DEFAULT NULL COMMENT '管理员电话',
  `expire_date` date DEFAULT NULL COMMENT '有效期',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '0-不可用 1-可用 2-用完',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_resource_code` (`resource_code`),
  KEY `idx_lng_lat` (`lng`,`lat`), KEY `idx_grid_id` (`grid_id`), KEY `idx_type` (`resource_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='周边资源-应急物资';

-- ---------------------------------------------
-- 11. 网格员实时位置 grid_member_location
-- ---------------------------------------------
DROP TABLE IF EXISTS `grid_member_location`;
CREATE TABLE `grid_member_location` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '网格员用户ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT '网格员姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `grid_id` bigint(20) DEFAULT NULL COMMENT '所属网格',
  `lng` decimal(10,6) NOT NULL COMMENT '当前经度',
  `lat` decimal(10,6) NOT NULL COMMENT '当前纬度',
  `address` varchar(255) DEFAULT NULL COMMENT '位置描述',
  `on_duty` tinyint(4) NOT NULL DEFAULT 0 COMMENT '0-离岗 1-在岗',
  `last_report_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后上报时间',
  `accuracy` decimal(10,2) DEFAULT NULL COMMENT '定位精度(米)',
  `battery` int(11) DEFAULT NULL COMMENT '设备电量',
  PRIMARY KEY (`id`), UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_lng_lat` (`lng`,`lat`), KEY `idx_grid_id` (`grid_id`), KEY `idx_on_duty` (`on_duty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网格员实时位置';

-- ---------------------------------------------
-- 12. 文件水印存证表 file_watermark
-- ---------------------------------------------
DROP TABLE IF EXISTS `file_watermark`;
CREATE TABLE `file_watermark` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_url` varchar(255) NOT NULL COMMENT '文件访问URL',
  `original_md5` varchar(64) NOT NULL COMMENT '原始文件MD5值',
  `watermarked_md5` varchar(64) NOT NULL COMMENT '加水印后文件MD5值',
  `stored_md5` varchar(64) DEFAULT NULL COMMENT '实际落盘文件MD5（加密后为加密文件MD5）',
  `watermark_info` varchar(500) DEFAULT NULL COMMENT '水印信息JSON（上报时间、网格员姓名、事件编号）',
  `event_id` bigint(20) DEFAULT NULL COMMENT '关联事件ID',
  `event_no` varchar(64) DEFAULT NULL COMMENT '事件编号（冗余，便于按事件查询）',
  `reporter_id` bigint(20) DEFAULT NULL COMMENT '上报人ID',
  `is_encrypted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否加密：0-否 1-是',
  `encryption_key_id` bigint(20) DEFAULT NULL COMMENT '加密密钥ID',
  `target_dept_id` bigint(20) DEFAULT NULL COMMENT '目标处置部门ID（仅加密文件有值）',
  `tamper_verified` tinyint(1) NOT NULL DEFAULT 0 COMMENT '篡改检测：0-未检测 1-检测正常 2-检测异常',
  `tamper_verify_time` datetime DEFAULT NULL COMMENT '最后检测时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_url` (`file_url`),
  KEY `idx_event_id` (`event_id`),
  KEY `idx_event_no` (`event_no`),
  KEY `idx_reporter_id` (`reporter_id`),
  KEY `idx_target_dept_id` (`target_dept_id`),
  KEY `idx_original_md5` (`original_md5`),
  KEY `idx_watermarked_md5` (`watermarked_md5`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件水印存证表';

-- ---------------------------------------------
-- 13. 加密密钥表 encryption_key
-- ---------------------------------------------
DROP TABLE IF EXISTS `encryption_key`;
CREATE TABLE `encryption_key` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `key_type` varchar(20) NOT NULL COMMENT '密钥类型：AES-对称密钥 RSA_PUBLIC-公钥 RSA_PRIVATE-私钥',
  `key_name` varchar(100) NOT NULL COMMENT '密钥名称',
  `key_content` text NOT NULL COMMENT '密钥内容（敏感密钥已用主密钥加密）',
  `key_encrypted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '密钥内容是否已加密：0-否 1-是',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '所属部门ID（用于数字信封，仅处置部门可解密）',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_key_type` (`key_type`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加密密钥表';

-- ---------------------------------------------
-- 14. 事件智能分派记录表 event_dispatch_record
-- ---------------------------------------------
DROP TABLE IF EXISTS `event_dispatch_record`;
CREATE TABLE `event_dispatch_record` (
  `id`                  bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `event_id`            bigint(20) NOT NULL COMMENT '事件ID',
  `recommended_dept_code` varchar(64)  DEFAULT NULL COMMENT '推荐部门编码',
  `recommended_dept_name` varchar(128) DEFAULT NULL COMMENT '推荐部门名称',
  `confidence`          decimal(5,4)   DEFAULT NULL COMMENT '分类置信度(0-1)',
  `auto_dispatch`       tinyint(1)     DEFAULT 0 COMMENT '是否自动分派(0否1是)',
  `dispatch_method`     varchar(32)    DEFAULT NULL COMMENT '分类方法(rule/model/hybrid/fallback)',
  `actual_dept_code`    varchar(64)    DEFAULT NULL COMMENT '实际分派部门编码',
  `actual_dept_name`    varchar(128)   DEFAULT NULL COMMENT '实际分派部门名称',
  `status`              varchar(32)    DEFAULT 'RECOMMENDED' COMMENT '状态(RECOMMENDED/AUTO_DISPATCHED/ADOPTED/REJECTED)',
  `adopted`             tinyint(1)     DEFAULT 0 COMMENT '是否被采纳(0否1是)',
  `feedback`            varchar(500)   DEFAULT NULL COMMENT '反馈意见',
  `model_scores`        text           DEFAULT NULL COMMENT '模型各分类得分JSON',
  `created_by`          bigint(20)     DEFAULT NULL COMMENT '创建人',
  `created_at`          datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by`          bigint(20)     DEFAULT NULL COMMENT '更新人',
  `updated_at`          datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`             tinyint(4)     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_event_id` (`event_id`),
  KEY `idx_status` (`status`),
  KEY `idx_adopted` (`adopted`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件智能分派记录';

-- ---------------------------------------------
-- 周边资源示例数据：摄像头
-- ---------------------------------------------
INSERT INTO `resource_camera` (`id`, `camera_code`, `camera_name`, `camera_type`, `lng`, `lat`, `address`, `rtsp_url`, `hls_url`, `grid_id`, `manufacturer`, `status`) VALUES
(1, 'CAM001', '长安街东口摄像头', 'public',   116.408400, 39.905200, '东长安街与王府井交叉口东', 'rtsp://192.168.1.101:554/stream1', 'http://192.168.1.101/hls/stream1.m3u8', 1, '海康威视', 1),
(2, 'CAM002', '建国门路口监控',   'traffic',  116.418400, 39.915200, '建国门内大街与朝阳门南小街交叉口', 'rtsp://192.168.1.102:554/stream1', 'http://192.168.1.102/hls/stream1.m3u8', 2, '大华', 1),
(3, 'CAM003', '阳光小区门口',     'private',  116.406400, 39.903200, '阳光小区北门', 'rtsp://192.168.1.103:554/stream1', 'http://192.168.1.103/hls/stream1.m3u8', 1, '海康威视', 1),
(4, 'CAM004', '文化广场中心',     'public',   116.409400, 39.906200, '城东文化广场中央', 'rtsp://192.168.1.104:554/stream1', NULL, 1, '海康威视', 0),
(5, 'CAM005', '地铁口A出口',      'traffic',  116.419400, 39.916200, '建国门地铁站A出口', 'rtsp://192.168.1.105:554/stream1', 'http://192.168.1.105/hls/stream1.m3u8', 2, '大华', 1);

-- ---------------------------------------------
-- 周边资源示例数据：应急物资
-- ---------------------------------------------
INSERT INTO `resource_emergency` (`id`, `resource_code`, `resource_name`, `resource_type`, `quantity`, `lng`, `lat`, `address`, `grid_id`, `manager`, `manager_phone`, `expire_date`, `status`, `remark`) VALUES
(1, 'EMG001', '室外消防栓',   'fire_fighting', 1,  116.407800, 39.904800, '长安街1号门口东侧', 1, '张三', '13900000011', NULL, 1, '水压正常，去年年检合格'),
(2, 'EMG002', '手提式灭火器', 'fire_fighting', 10, 116.408800, 39.905800, '城东社区服务中心门口', 1, '李四', '13900000012', '2027-06-30', 1, 'ABC干粉灭火器，有效期至2027年'),
(3, 'EMG003', 'AED除颤仪',   'first_aid',     1,  116.417800, 39.914800, '建国门社区医院大厅', 2, '王医生', '13900000013', '2028-12-31', 1, '飞利浦品牌，每月巡检'),
(4, 'EMG004', '急救箱',       'first_aid',     3,  116.406800, 39.903800, '阳光小区物业办公室', 1, '物业王姐', '13900000014', '2026-12-31', 1, '内含绷带、消毒棉片等常用药品'),
(5, 'EMG005', '防汛沙袋',     'flood_ctr',     50, 116.418800, 39.915800, '建国门地下通道入口处', 2, '赵主任', '13900000015', NULL, 1, '雨季专用，存放在通道西侧库房'),
(6, 'EMG006', '救援绳索套装', 'rescue',        2,  116.409800, 39.906800, '城东消防站', 1, '消防刘队', '13900000016', NULL, 1, '50米安全绳+挂钩+安全带'),
(7, 'EMG007', '应急照明手电', 'rescue',        20, 116.416800, 39.913800, '第二网格居委会', 2, '孙主任', '13900000017', '2029-01-01', 1, 'LED强光手电，备用电池充足');

-- ---------------------------------------------
-- 网格员实时位置示例数据
-- ---------------------------------------------
INSERT INTO `grid_member_location` (`id`, `user_id`, `user_name`, `phone`, `grid_id`, `lng`, `lat`, `address`, `on_duty`, `last_report_time`, `accuracy`, `battery`) VALUES
(1, 2, '网格长张三', '13900000002', 1, 116.407000, 39.904000, '城东第一网格居委会', 1, '2024-01-01 09:28:00', 5.50, 78),
(2, 3, '网格员李四', '13900000003', 1, 116.408500, 39.905500, '王府井步行街北口', 1, '2024-01-01 09:30:00', 3.20, 85),
(3, 4, '网格员王五', '13900000004', 2, 116.417600, 39.914600, '建国门内大街地铁站附近', 1, '2024-01-01 09:29:00', 4.80, 62);

-- ---------------------------------------------
-- 示例密钥数据（用于数字信封加密）
-- 注意：敏感密钥（RSA_PRIVATE/AES）初始 key_encrypted=0，系统启动时会自动用主密钥加密迁移，迁移后 key_encrypted=1
-- ---------------------------------------------
-- 处置中心RSA密钥对（dept_id=3 对应处置中心部门ID；若实际处置中心部门ID不同请根据业务修改）
INSERT INTO `encryption_key` (`key_type`, `key_name`, `key_content`, `key_encrypted`, `dept_id`, `status`) VALUES
('RSA_PUBLIC',  '处置中心公钥',     'MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCqGKukO1De7zhZj6+H0qtjTkVxwTCpvKe4eCZ0FPqri0cb2JZfXJ/DgYSF6vUpwmJG8wVQZKjeGcjDOL5UlsuusFncCzWBQ7RKNUSesmQRMSGkVb1/3j+skZ6UtW+5u09lHNsj6tQ51s1SPrCBkedbNf0Tp0GbMJDyR4e9T04ZZwIDAQAB', 0, 3, 1),
('RSA_PRIVATE', '处置中心私钥(启动后自动加密存储)', 'MIICXAIBAAKBgQCqGKukO1De7zhZj6+H0qtjTkVxwTCpvKe4eCZ0FPqri0cb2JZfXJ/DgYSF6vUpwmJG8wVQZKjeGcjDOL5UlsuusFncCzWBQ7RKNUSesmQRMSGkVb1/3j+skZ6UtW+5u09lHNsj6tQ51s1SPrCBkedbNf0Tp0GbMJDyR4e9T04ZZwIDAQABAoGAFijko56+qGyN8M0RVyaRAXz++xTqHBLh3tx4VgMtrQ+WEgCjhoTwo23KMBAuJGSYnRmoBZM3lMfTKevIkAidPExvYCdm5dYq3XToLkkLv5L2pIIVOFMDG+KESnAFV7l2c+cnzRMW0+b6f8mR1CJzZuxVLL6Q02fvLi55/mbSYxECQQDeAw6fiIQXGukBI4eMZZt4nscy2o12KyYner3VpoeE+Np2q+Z3pvAMd/aNzQ/W9WaI+NRfcxUJrmfPwIGm63ilAkEAxCL5HQb2bQr4ByorcMWm/hEP2MZzROV73yF41hPsRC9m66KrheO9HPTJuo3/9s5p+sqGxOlFL0NDt4SkosjgGwJAFklyR1uZ/wPJjj611cdBcztlPdqoxssQGnh85BzCj/u3WqBpE2vjvyyvyI5kX6zk7S0ljKtt2jny2+00VsBerQJBAJGC1Mg5Oydo5NwD6BiROrPxGo2bpTbu/fhrT8ebHkTz2eplU9VQQSQzY1oZMVX8i1m5WUTLPz2yLJIBQVdXqhMCQBGoiuSoSjafUhV7i1cEGpb88h5NBYZzWXGZ37sJ5QsW+sJyoNde3xH8vdXhzU7eT82D6X/scw9RZz+/6rCJ4=', 0, 3, 1),
('AES',         '全局AES密钥(启动后自动加密存储)', 'YjA2NDFhOGQwYjQwNDE2MGE2YzRjNTkxNmY3YjQwOWE=', 0, NULL, 1);

-- ---------------------------------------------
-- NLP智能分派种子训练数据（60条已采纳的分派记录）
-- ---------------------------------------------
INSERT INTO `event_dispatch_record` (`id`, `event_id`, `recommended_dept_code`, `recommended_dept_name`, `confidence`, `auto_dispatch`, `dispatch_method`, `actual_dept_code`, `actual_dept_name`, `status`, `adopted`, `feedback`, `model_scores`, `created_at`) VALUES
-- 市政类
(1, 5, 'MUNICIPAL', '市政科', 0.92, 1, 'rule', 'MUNICIPAL', '市政科', 'ADOPTED', 1, '自动分派', NULL, '2024-01-15 09:30:00'),
(2, 6, 'MUNICIPAL', '市政科', 0.88, 0, 'model', 'MUNICIPAL', '市政科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-16 10:15:00'),
(3, 7, 'MUNICIPAL', '市政科', 0.95, 1, 'rule', 'MUNICIPAL', '市政科', 'ADOPTED', 1, '自动分派', NULL, '2024-01-17 14:20:00'),
(4, 8, 'MUNICIPAL', '市政科', 0.90, 1, 'hybrid', 'MUNICIPAL', '市政科', 'ADOPTED', 1, '自动分派', NULL, '2024-01-18 11:45:00'),
(5, 9, 'MUNICIPAL', '市政科', 0.87, 0, 'rule', 'MUNICIPAL', '市政科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-19 08:30:00'),
-- 环卫类
(6, 10, 'ENVIRONMENTAL', '环卫科', 0.93, 1, 'rule', 'ENVIRONMENTAL', '环卫科', 'ADOPTED', 1, '自动分派', NULL, '2024-01-15 10:00:00'),
(7, 11, 'ENVIRONMENTAL', '环卫科', 0.91, 1, 'model', 'ENVIRONMENTAL', '环卫科', 'ADOPTED', 1, '自动分派', NULL, '2024-01-16 09:20:00'),
(8, 12, 'ENVIRONMENTAL', '环卫科', 0.89, 0, 'hybrid', 'ENVIRONMENTAL', '环卫科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-17 13:45:00'),
(9, 13, 'ENVIRONMENTAL', '环卫科', 0.94, 1, 'rule', 'ENVIRONMENTAL', '环卫科', 'ADOPTED', 1, '自动分派', NULL, '2024-01-18 15:30:00'),
(10, 14, 'ENVIRONMENTAL', '环卫科', 0.86, 0, 'rule', 'ENVIRONMENTAL', '环卫科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-19 11:00:00'),
-- 城管类
(11, 15, 'URBAN_MANAGEMENT', '城管科', 0.88, 0, 'rule', 'URBAN_MANAGEMENT', '城管科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-15 11:30:00'),
(12, 16, 'URBAN_MANAGEMENT', '城管科', 0.92, 1, 'model', 'URBAN_MANAGEMENT', '城管科', 'ADOPTED', 1, '自动分派', NULL, '2024-01-16 14:15:00'),
(13, 17, 'URBAN_MANAGEMENT', '城管科', 0.90, 0, 'hybrid', 'URBAN_MANAGEMENT', '城管科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-17 10:45:00'),
(14, 18, 'URBAN_MANAGEMENT', '城管科', 0.87, 0, 'rule', 'URBAN_MANAGEMENT', '城管科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-18 09:30:00'),
(15, 19, 'URBAN_MANAGEMENT', '城管科', 0.91, 1, 'model', 'URBAN_MANAGEMENT', '城管科', 'ADOPTED', 1, '自动分派', NULL, '2024-01-19 14:20:00'),
-- 交通类
(16, 20, 'TRAFFIC', '交通科', 0.89, 0, 'rule', 'TRAFFIC', '交通科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-15 14:00:00'),
(17, 21, 'TRAFFIC', '交通科', 0.93, 1, 'model', 'TRAFFIC', '交通科', 'ADOPTED', 1, '自动分派', NULL, '2024-01-16 11:30:00'),
(18, 22, 'TRAFFIC', '交通科', 0.86, 0, 'hybrid', 'TRAFFIC', '交通科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-17 15:00:00'),
(19, 23, 'TRAFFIC', '交通科', 0.90, 0, 'rule', 'TRAFFIC', '交通科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-18 14:15:00'),
(20, 24, 'TRAFFIC', '交通科', 0.88, 0, 'rule', 'TRAFFIC', '交通科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-19 09:45:00'),
-- 安监类
(21, 25, 'SAFETY', '安监科', 0.94, 1, 'rule', 'SAFETY', '安监科', 'ADOPTED', 1, '自动分派', NULL, '2024-01-15 15:30:00'),
(22, 26, 'SAFETY', '安监科', 0.91, 0, 'model', 'SAFETY', '安监科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-16 16:00:00'),
(23, 27, 'SAFETY', '安监科', 0.89, 0, 'hybrid', 'SAFETY', '安监科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-17 11:15:00'),
(24, 28, 'SAFETY', '安监科', 0.92, 1, 'rule', 'SAFETY', '安监科', 'ADOPTED', 1, '自动分派', NULL, '2024-01-18 10:30:00'),
(25, 29, 'SAFETY', '安监科', 0.87, 0, 'rule', 'SAFETY', '安监科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-19 15:30:00'),
-- 民政类
(26, 30, 'CIVIL_AFFAIRS', '民政科', 0.88, 0, 'rule', 'CIVIL_AFFAIRS', '民政科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-15 09:45:00'),
(27, 31, 'CIVIL_AFFAIRS', '民政科', 0.90, 0, 'model', 'CIVIL_AFFAIRS', '民政科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-16 10:30:00'),
(28, 32, 'CIVIL_AFFAIRS', '民政科', 0.92, 1, 'hybrid', 'CIVIL_AFFAIRS', '民政科', 'ADOPTED', 1, '自动分派', NULL, '2024-01-17 14:30:00'),
(29, 33, 'CIVIL_AFFAIRS', '民政科', 0.86, 0, 'rule', 'CIVIL_AFFAIRS', '民政科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-18 15:45:00'),
(30, 34, 'CIVIL_AFFAIRS', '民政科', 0.91, 0, 'model', 'CIVIL_AFFAIRS', '民政科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-19 10:15:00'),
-- 治安类
(31, 35, 'PUBLIC_SECURITY', '治安科', 0.93, 1, 'rule', 'PUBLIC_SECURITY', '治安科', 'ADOPTED', 1, '自动分派', NULL, '2024-01-15 16:00:00'),
(32, 36, 'PUBLIC_SECURITY', '治安科', 0.89, 0, 'model', 'PUBLIC_SECURITY', '治安科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-16 15:15:00'),
(33, 37, 'PUBLIC_SECURITY', '治安科', 0.87, 0, 'hybrid', 'PUBLIC_SECURITY', '治安科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-17 16:30:00'),
(34, 38, 'PUBLIC_SECURITY', '治安科', 0.90, 0, 'rule', 'PUBLIC_SECURITY', '治安科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-18 11:00:00'),
(35, 39, 'PUBLIC_SECURITY', '治安科', 0.92, 1, 'model', 'PUBLIC_SECURITY', '治安科', 'ADOPTED', 1, '自动分派', NULL, '2024-01-19 16:45:00'),
-- 水务类
(40, 40, 'WATER', '水务科', 0.91, 1, 'rule', 'WATER', '水务科', 'ADOPTED', 1, '自动分派', NULL, '2024-01-15 08:15:00'),
(41, 41, 'WATER', '水务科', 0.88, 0, 'model', 'WATER', '水务科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-16 09:00:00'),
(42, 42, 'WATER', '水务科', 0.93, 1, 'hybrid', 'WATER', '水务科', 'ADOPTED', 1, '自动分派', NULL, '2024-01-17 10:00:00'),
(43, 43, 'WATER', '水务科', 0.89, 0, 'rule', 'WATER', '水务科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-18 16:00:00'),
(44, 44, 'WATER', '水务科', 0.87, 0, 'rule', 'WATER', '水务科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-19 08:45:00'),
-- 绿化类
(45, 45, 'GREENING', '园林绿化科', 0.90, 0, 'rule', 'GREENING', '园林绿化科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-15 13:00:00'),
(46, 46, 'GREENING', '园林绿化科', 0.92, 1, 'model', 'GREENING', '园林绿化科', 'ADOPTED', 1, '自动分派', NULL, '2024-01-16 13:45:00'),
(47, 47, 'GREENING', '园林绿化科', 0.88, 0, 'hybrid', 'GREENING', '园林绿化科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-17 09:30:00'),
(48, 48, 'GREENING', '园林绿化科', 0.86, 0, 'rule', 'GREENING', '园林绿化科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-18 13:30:00'),
(49, 49, 'GREENING', '园林绿化科', 0.91, 0, 'model', 'GREENING', '园林绿化科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-19 13:15:00'),
-- 住建类
(50, 50, 'HOUSING', '住建科', 0.89, 0, 'rule', 'HOUSING', '住建科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-15 12:00:00'),
(51, 51, 'HOUSING', '住建科', 0.91, 0, 'model', 'HOUSING', '住建科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-16 12:30:00'),
(52, 52, 'HOUSING', '住建科', 0.87, 0, 'hybrid', 'HOUSING', '住建科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-17 13:00:00'),
(53, 53, 'HOUSING', '住建科', 0.90, 0, 'rule', 'HOUSING', '住建科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-18 12:45:00'),
(54, 54, 'HOUSING', '住建科', 0.88, 0, 'rule', 'HOUSING', '住建科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-19 12:00:00'),
-- 电力类
(55, 55, 'POWER', '电力科', 0.92, 1, 'rule', 'POWER', '电力科', 'ADOPTED', 1, '自动分派', NULL, '2024-01-15 17:00:00'),
(56, 56, 'POWER', '电力科', 0.88, 0, 'model', 'POWER', '电力科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-16 17:30:00'),
(57, 57, 'POWER', '电力科', 0.90, 0, 'hybrid', 'POWER', '电力科', 'ADOPTED', 1, '一键采纳', NULL, '2024-01-17 17:15:00'),
-- 综合协调类
(58, 58, 'OTHER', '综合协调科', 0.75, 0, 'fallback', 'OTHER', '综合协调科', 'ADOPTED', 1, '人工确认', NULL, '2024-01-15 18:00:00'),
(59, 59, 'OTHER', '综合协调科', 0.78, 0, 'fallback', 'OTHER', '综合协调科', 'ADOPTED', 1, '人工确认', NULL, '2024-01-16 18:30:00'),
(60, 60, 'OTHER', '综合协调科', 0.72, 0, 'fallback', 'OTHER', '综合协调科', 'ADOPTED', 1, '人工确认', NULL, '2024-01-17 18:00:00');

-- ---------------------------------------------
-- NLP训练用示例事件数据（与分派记录对应）
-- ---------------------------------------------
INSERT INTO `event_info` (`id`, `event_no`, `title`, `event_type`, `description`, `lng`, `lat`, `address`, `anonymous`, `reporter_id`, `grid_id`, `status`, `priority`) VALUES
(5, 'EV202401150001', '井盖破损严重', 'public_facility', '长安街1号井盖破损，存在安全隐患。', 116.403800, 39.904800, '东长安街1号', 0, 6, 10, 'COMPLETED', 'HIGH'),
(6, 'EV202401160001', '路灯不亮', 'public_facility', '王府井大街路灯损坏，夜间照明不足。', 116.410800, 39.906800, '王府井大街', 0, 7, 12, 'COMPLETED', 'NORMAL'),
(7, 'EV202401170001', '护栏变形', 'public_facility', '道路护栏被车辆撞变形。', 116.406400, 39.907200, '长安街2号', 0, 8, 11, 'COMPLETED', 'NORMAL'),
(8, 'EV202401180001', '路面坑洼', 'public_facility', '路面出现大坑洼，影响车辆通行。', 116.404400, 39.905200, '长安街3号', 0, 6, 10, 'COMPLETED', 'HIGH'),
(9, 'EV202401190001', '消防栓漏水', 'public_facility', '路边消防栓漏水，浪费水资源。', 116.405400, 39.906200, '长安街4号', 0, 7, 10, 'COMPLETED', 'NORMAL'),
(10, 'EV202401150002', '垃圾满溢', 'environment', '小区垃圾桶满溢，臭气熏天。', 116.403800, 39.904800, '阳光小区', 0, 6, 10, 'COMPLETED', 'NORMAL'),
(11, 'EV202401160002', '公厕卫生差', 'environment', '东城区公厕卫生条件差，需要清理。', 116.410800, 39.906800, '东城区公厕', 0, 7, 12, 'COMPLETED', 'NORMAL'),
(12, 'EV202401170002', '下水道堵塞', 'environment', '居民楼下水道堵塞，污水外流。', 116.406400, 39.907200, '幸福小区', 0, 8, 11, 'COMPLETED', 'HIGH'),
(13, 'EV202401180002', '道路扬尘', 'environment', '施工路段扬尘严重，影响居民。', 116.404400, 39.905200, '施工路段', 0, 6, 10, 'COMPLETED', 'NORMAL'),
(14, 'EV202401190002', '化粪池外溢', 'environment', '小区化粪池外溢，污染环境。', 116.405400, 39.906200, '和平小区', 0, 7, 10, 'COMPLETED', 'HIGH'),
(15, 'EV202401150003', '占道经营', 'public_facility', '商贩占道经营，堵塞人行道。', 116.403800, 39.904800, '商业街', 0, 6, 10, 'COMPLETED', 'NORMAL'),
(16, 'EV202401160003', '违建搭建', 'public_facility', '小区内私搭乱建现象严重。', 116.410800, 39.906800, '阳光小区', 0, 7, 12, 'COMPLETED', 'HIGH'),
(17, 'EV202401170003', '噪音扰民', 'environment', '夜间施工噪音扰民，无法休息。', 116.406400, 39.907200, '施工工地', 0, 8, 11, 'COMPLETED', 'HIGH'),
(18, 'EV202401180003', '违规广告牌', 'public_facility', '违规设置户外广告牌，影响市容。', 116.404400, 39.905200, '主干道', 0, 6, 10, 'COMPLETED', 'NORMAL'),
(19, 'EV202401190003', '油烟污染', 'environment', '餐饮油烟直排，影响周边居民。', 116.405400, 39.906200, '美食街', 0, 7, 10, 'COMPLETED', 'NORMAL'),
(20, 'EV202401150004', '交通拥堵', 'traffic', '早高峰主干道交通严重拥堵。', 116.403800, 39.904800, '主干道', 0, 6, 10, 'COMPLETED', 'NORMAL'),
(21, 'EV202401160004', '违停严重', 'traffic', '小区周边车辆乱停乱放。', 116.410800, 39.906800, '小区周边', 0, 7, 12, 'COMPLETED', 'NORMAL'),
(22, 'EV202401170004', '红绿灯故障', 'traffic', '路口红绿灯不亮，交通混乱。', 116.406400, 39.907200, '十字路口', 0, 8, 11, 'COMPLETED', 'HIGH'),
(23, 'EV202401180004', '路障破损', 'traffic', '道路隔离栏破损，存在安全隐患。', 116.404400, 39.905200, '快速路', 0, 6, 10, 'COMPLETED', 'NORMAL'),
(24, 'EV202401190004', '斑马线模糊', 'traffic', '人行横道斑马线磨损严重，看不清。', 116.405400, 39.906200, '学校门口', 0, 7, 10, 'COMPLETED', 'NORMAL'),
(25, 'EV202401150005', '燃气泄漏', 'safety_hazard', '居民楼疑似燃气泄漏，有异味。', 116.403800, 39.904800, '居民楼', 0, 6, 10, 'COMPLETED', 'URGENT'),
(26, 'EV202401160005', '电线裸露', 'safety_hazard', '电线杆电线裸露，有触电危险。', 116.410800, 39.906800, '街道', 0, 7, 12, 'COMPLETED', 'HIGH'),
(27, 'EV202401170005', '危房开裂', 'safety_hazard', '老旧房屋墙体开裂，有倒塌风险。', 116.406400, 39.907200, '老城区', 0, 8, 11, 'COMPLETED', 'HIGH'),
(28, 'EV202401180005', '化学品泄漏', 'safety_hazard', '工厂化学品泄漏，需要紧急处理。', 116.404400, 39.905200, '工业园区', 0, 6, 10, 'COMPLETED', 'URGENT'),
(29, 'EV202401190005', '消防通道堵塞', 'safety_hazard', '小区消防通道被车辆堵塞。', 116.405400, 39.906200, '小区', 0, 7, 10, 'COMPLETED', 'HIGH'),
(30, 'EV202401150006', '低保申请', 'service', '居民申请低保，需要民政部门协助。', 116.403800, 39.904800, '居委会', 0, 6, 10, 'COMPLETED', 'LOW'),
(31, 'EV202401160006', '邻里纠纷', 'dispute', '邻居因噪音问题产生矛盾。', 116.410800, 39.906800, '居民楼', 0, 7, 12, 'COMPLETED', 'NORMAL'),
(32, 'EV202401170006', '养老服务咨询', 'service', '咨询社区养老服务相关政策。', 116.406400, 39.907200, '社区服务中心', 0, 8, 11, 'COMPLETED', 'LOW'),
(33, 'EV202401180006', '残疾人帮扶', 'service', '残疾人家庭需要生活帮扶。', 116.404400, 39.905200, '居民家', 0, 6, 10, 'COMPLETED', 'NORMAL'),
(34, 'EV202401190006', '贫困救助', 'service', '贫困户申请临时救助。', 116.405400, 39.906200, '居委会', 0, 7, 10, 'COMPLETED', 'NORMAL'),
(35, 'EV202401150007', '偷盗事件', 'security', '小区发生电动车偷盗事件。', 116.403800, 39.904800, '小区停车场', 0, 6, 10, 'COMPLETED', 'HIGH'),
(36, 'EV202401160007', '打架斗殴', 'security', '街头有人打架斗殴，影响治安。', 116.410800, 39.906800, '街头', 0, 7, 12, 'COMPLETED', 'HIGH'),
(37, 'EV202401170007', '传销活动', 'security', '疑似传销窝点在小区活动。', 116.406400, 39.907200, '居民楼', 0, 8, 11, 'COMPLETED', 'HIGH'),
(38, 'EV202401180007', '诈骗事件', 'security', '居民遭遇电信诈骗，已报警。', 116.404400, 39.905200, '居民家', 0, 6, 10, 'COMPLETED', 'HIGH'),
(39, 'EV202401190007', '聚众赌博', 'security', '有人在公园聚众赌博。', 116.405400, 39.906200, '公园', 0, 7, 10, 'COMPLETED', 'HIGH'),
(40, 'EV202401150008', '水管爆裂', 'environment', '主水管爆裂，大量漏水。', 116.403800, 39.904800, '主干道', 0, 6, 10, 'COMPLETED', 'HIGH'),
(41, 'EV202401160008', '高层停水', 'environment', '高层住宅楼水压不足，停水。', 116.410800, 39.906800, '高层小区', 0, 7, 12, 'COMPLETED', 'NORMAL'),
(42, 'EV202401170008', '下水道反水', 'environment', '一楼住户下水道反水严重。', 116.406400, 39.907200, '居民楼', 0, 8, 11, 'COMPLETED', 'HIGH'),
(43, 'EV202401180008', '水表损坏', 'environment', '居民家水表损坏，需要更换。', 116.404400, 39.905200, '居民家', 0, 6, 10, 'COMPLETED', 'LOW'),
(44, 'EV202401190008', '路面积水', 'environment', '暴雨后路面积水严重。', 116.405400, 39.906200, '低洼路段', 0, 7, 10, 'COMPLETED', 'NORMAL'),
(45, 'EV202401150009', '树木倒伏', 'environment', '大风刮倒树木，挡路。', 116.403800, 39.904800, '街道', 0, 6, 10, 'COMPLETED', 'NORMAL'),
(46, 'EV202401160009', '绿化修剪', 'environment', '绿化带树木需要修剪。', 116.410800, 39.906800, '公园', 0, 7, 12, 'COMPLETED', 'LOW'),
(47, 'EV202401170009', '病虫害', 'environment', '行道树发生病虫害。', 116.406400, 39.907200, '行道树', 0, 8, 11, 'COMPLETED', 'NORMAL'),
(48, 'EV202401180009', '公园设施损坏', 'environment', '公园健身器材损坏。', 116.404400, 39.905200, '公园', 0, 6, 10, 'COMPLETED', 'NORMAL'),
(49, 'EV202401190009', '杂草丛生', 'environment', '路边绿化带杂草丛生。', 116.405400, 39.906200, '路边', 0, 7, 10, 'COMPLETED', 'LOW'),
(50, 'EV202401150010', '房屋裂缝', 'public_facility', '居民楼墙体出现裂缝。', 116.403800, 39.904800, '居民楼', 0, 6, 10, 'COMPLETED', 'HIGH'),
(51, 'EV202401160010', '屋顶漏水', 'public_facility', '顶层住户屋顶漏水严重。', 116.410800, 39.906800, '顶层住户', 0, 7, 12, 'COMPLETED', 'HIGH'),
(52, 'EV202401170010', '物业纠纷', 'dispute', '业主与物业公司产生纠纷。', 116.406400, 39.907200, '小区', 0, 8, 11, 'COMPLETED', 'NORMAL'),
(53, 'EV202401180010', '拆迁咨询', 'service', '咨询拆迁安置相关政策。', 116.404400, 39.905200, '拆迁办', 0, 6, 10, 'COMPLETED', 'LOW'),
(54, 'EV202401190010', '电梯故障', 'public_facility', '高层电梯频繁故障。', 116.405400, 39.906200, '高层住宅楼', 0, 7, 10, 'COMPLETED', 'HIGH'),
(55, 'EV202401150011', '突然停电', 'public_facility', '大面积突然停电，原因不明。', 116.403800, 39.904800, '片区', 0, 6, 10, 'COMPLETED', 'HIGH'),
(56, 'EV202401160011', '变压器冒烟', 'public_facility', '路边变压器冒烟，有焦味。', 116.410800, 39.906800, '路边', 0, 7, 12, 'COMPLETED', 'URGENT'),
(57, 'EV202401170011', '线路老化', 'public_facility', '老旧小区线路老化严重。', 116.406400, 39.907200, '老旧小区', 0, 8, 11, 'COMPLETED', 'HIGH'),
(58, 'EV202401150012', '不明事项咨询', 'service', '政策不明确，需要多部门协调。', 116.403800, 39.904800, '服务中心', 0, 6, 10, 'COMPLETED', 'LOW'),
(59, 'EV202401160012', '跨部门事项', 'service', '涉及多个部门的复杂事项。', 116.410800, 39.906800, '政务大厅', 0, 7, 12, 'COMPLETED', 'NORMAL'),
(60, 'EV202401170012', '历史遗留问题', 'service', '历史遗留问题，需要协调处理。', 116.406400, 39.907200, '相关部门', 0, 8, 11, 'COMPLETED', 'NORMAL');

SET FOREIGN_KEY_CHECKS = 1;
