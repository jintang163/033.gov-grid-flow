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
  `role`        varchar(30)  NOT NULL DEFAULT 'worker' COMMENT '角色：admin-管理员 grid_leader-网格长 worker-网格员 handler-处置员',
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
  `grid_leader_id`  bigint(20)   DEFAULT NULL COMMENT '网格长用户ID',
  `area`            decimal(10,2) DEFAULT NULL COMMENT '面积(平方公里)',
  `boundary`        text         DEFAULT NULL COMMENT '边界坐标(GeoJSON)',
  `lng`             decimal(10,6) DEFAULT NULL COMMENT '中心经度',
  `lat`             decimal(10,6) DEFAULT NULL COMMENT '中心纬度',
  `address`         varchar(255) DEFAULT NULL COMMENT '地址',
  `status`          tinyint(4)   NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `created_at`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_grid_code` (`grid_code`),
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
  `event_no`            varchar(50)  NOT NULL COMMENT '事件编号',
  `title`               varchar(200) NOT NULL COMMENT '事件标题',
  `event_type`          varchar(50)  NOT NULL COMMENT '事件类型',
  `description`         text         DEFAULT NULL COMMENT '事件描述',
  `lng`                 decimal(10,6) DEFAULT NULL COMMENT '经度',
  `lat`                 decimal(10,6) DEFAULT NULL COMMENT '纬度',
  `address`             varchar(255) DEFAULT NULL COMMENT '地址',
  `images`              text         DEFAULT NULL COMMENT '图片URL，多个逗号分隔',
  `videos`              text         DEFAULT NULL COMMENT '视频URL，多个逗号分隔',
  `anonymous`           tinyint(1)   NOT NULL DEFAULT 0 COMMENT '是否匿名：0-否 1-是',
  `reporter_id`         bigint(20)   DEFAULT NULL COMMENT '上报人ID',
  `reporter_name`       varchar(50)  DEFAULT NULL COMMENT '上报人姓名',
  `reporter_phone`      varchar(20)  DEFAULT NULL COMMENT '上报人电话',
  `grid_id`             bigint(20)   DEFAULT NULL COMMENT '所属网格ID',
  `status`              varchar(30)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待受理 APPROVED-已受理 DISPATCHED-已分派 HANDLED-已处置 COMPLETED-已办结 REJECTED-已驳回',
  `priority`            varchar(30)  NOT NULL DEFAULT 'NORMAL' COMMENT '优先级：LOW-低 NORMAL-中 HIGH-高 URGENT-紧急',
  `process_instance_id` varchar(64)  DEFAULT NULL COMMENT 'Flowable流程实例ID',
  `created_at`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`             tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_no` (`event_no`),
  KEY `idx_grid_id` (`grid_id`),
  KEY `idx_reporter_id` (`reporter_id`),
  KEY `idx_status` (`status`),
  KEY `idx_priority` (`priority`),
  KEY `idx_event_type` (`event_type`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_process_instance_id` (`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件表';

-- ---------------------------------------------
-- 6. 事件处理记录表 event_process
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
-- 8. 通知表 sys_notification
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
(3, '事件处置中心', 'EVENT', 1, '王主任', '13800000002', 2, 1);

-- ---------------------------------------------
-- 网格数据（2个）
-- ---------------------------------------------
INSERT INTO `grid_info` (`id`, `grid_code`, `grid_name`, `grid_leader_id`, `area`, `lng`, `lat`, `address`, `status`) VALUES
(1, 'GRID001', '城东第一网格', NULL, 2.50, 116.407400, 39.904200, '北京市东城区长安街1号', 1),
(2, 'GRID002', '城东第二网格', NULL, 3.20, 116.417400, 39.914200, '北京市东城区建国门内大街2号', 1);

-- ---------------------------------------------
-- 用户数据（5个）
-- 密码统一为：123456（BCrypt加密后）
-- ---------------------------------------------
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `phone`, `email`, `status`, `role`, `dept_id`, `grid_id`) VALUES
(1, 'admin',    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', '13900000001', 'admin@example.com', 1, 'admin',        1, NULL),
(2, 'leader1',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '网格长张三',  '13900000002', 'leader1@example.com', 1, 'grid_leader',  2, 1),
(3, 'worker1',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '网格员李四',  '13900000003', 'worker1@example.com', 1, 'worker',       2, 1),
(4, 'worker2',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '网格员王五',  '13900000004', 'worker2@example.com', 1, 'worker',       2, 2),
(5, 'handler1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '处置员赵六',  '13900000005', 'handler1@example.com', 1, 'handler',      3, NULL);

-- 回填网格长ID
UPDATE `grid_info` SET `grid_leader_id` = 2 WHERE `id` = 1;
UPDATE `grid_info` SET `grid_leader_id` = 2 WHERE `id` = 2;

-- ---------------------------------------------
-- 网格员关联数据
-- ---------------------------------------------
INSERT INTO `grid_member` (`grid_id`, `user_id`, `member_type`) VALUES
(1, 2, 'grid_leader'),
(1, 3, 'worker'),
(2, 2, 'grid_leader'),
(2, 4, 'worker');

-- ---------------------------------------------
-- 示例事件数据
-- ---------------------------------------------
INSERT INTO `event_info` (`id`, `event_no`, `title`, `event_type`, `description`, `lng`, `lat`, `address`, `anonymous`, `reporter_id`, `reporter_name`, `reporter_phone`, `grid_id`, `status`, `priority`) VALUES
(1, 'EV202401010001', '路灯损坏', 'public_facility', '城东第一网格区域内主路灯不亮，夜间通行不便。', 116.407400, 39.904200, '东长安街与王府井交叉口', 0, 3, '李四', '13900000003', 1, 'PENDING', 'NORMAL'),
(2, 'EV202401010002', '下水道堵塞', 'environment', '居民小区门口下水道堵塞，污水外流。', 116.417400, 39.914200, '建国门内大街8号', 0, 4, '王五', '13900000004', 2, 'PROCESSING', 'HIGH');

-- ---------------------------------------------
-- 示例事件处理记录
-- ---------------------------------------------
INSERT INTO `event_process` (`event_id`, `node_name`, `handler_id`, `handler_name`, `action`, `comment`, `handle_time`) VALUES
(1, '事件上报',   3, '李四', 'submit', '发现路灯损坏，请求维修。', '2024-01-01 09:30:00'),
(2, '事件上报',   4, '王五', 'submit', '下水道堵塞严重，需紧急处理。', '2024-01-01 10:15:00'),
(2, '事件受理',   2, '张三', 'accept', '已受理，已分派至处置中心。', '2024-01-01 10:30:00');

-- ---------------------------------------------
-- 示例通知
-- ---------------------------------------------
INSERT INTO `sys_notification` (`user_id`, `title`, `content`, `type`, `biz_id`, `is_read`) VALUES
(2, '新事件待受理', '您有1条新的事件待受理：路灯损坏', 'event', 1, 0),
(5, '新任务待处理', '您有1条新的任务待处理：下水道堵塞', 'task', 2, 0),
(3, '事件受理通知', '您上报的事件【路灯损坏】已受理', 'event', 1, 1);

SET FOREIGN_KEY_CHECKS = 1;
