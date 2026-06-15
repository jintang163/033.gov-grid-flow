-- =============================================================
-- 跨街道协同流转功能 - 数据库初始化脚本
-- =============================================================
-- 执行前请确认：
-- 1. 数据库中已存在 event_info, sys_dept, sys_user, grid_info 等基础表
-- 2. 已有表使用相同的字符集和排序规则
-- =============================================================

-- -------------------------------------------------------------
-- 表: event_cross_street_transfer - 跨街道协同流转记录表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `event_cross_street_transfer`;
CREATE TABLE `event_cross_street_transfer` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `event_id` bigint(20) NOT NULL COMMENT '关联事件ID',
  `event_no` varchar(64) DEFAULT NULL COMMENT '事件编号',
  `event_title` varchar(255) DEFAULT NULL COMMENT '事件标题',
  `event_type` varchar(64) DEFAULT NULL COMMENT '事件类型',
  `source_dept_id` bigint(20) DEFAULT NULL COMMENT '转出机构ID',
  `source_dept_name` varchar(128) DEFAULT NULL COMMENT '转出机构名称',
  `source_grid_id` bigint(20) DEFAULT NULL COMMENT '转出网格ID',
  `source_grid_name` varchar(128) DEFAULT NULL COMMENT '转出网格名称',
  `target_dept_id` bigint(20) NOT NULL COMMENT '转入机构ID',
  `target_dept_name` varchar(128) NOT NULL COMMENT '转入机构名称',
  `target_dept_code` varchar(64) DEFAULT NULL COMMENT '转入机构编码',
  `target_type` varchar(32) NOT NULL DEFAULT 'STREET' COMMENT '转派类型：STREET-相邻街道 BUREAU-委办局 COUNTY-区级部门',
  `transfer_reason` text COMMENT '转派原因',
  `cross_boundary_description` text COMMENT '跨界描述',
  `impact_range` text COMMENT '影响范围',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING_APPROVAL' COMMENT '流转状态：PENDING_APPROVAL-待审批 APPROVED-已通过 REJECTED-已驳回 TRANSFERRED-已转派 ACCEPTED-已接收 PROCESSING-处理中 COMPLETED-已完成',
  `applicant_id` bigint(20) DEFAULT NULL COMMENT '申请人ID',
  `applicant_name` varchar(64) DEFAULT NULL COMMENT '申请人姓名',
  `applicant_time` datetime DEFAULT NULL COMMENT '申请时间',
  `approver_id` bigint(20) DEFAULT NULL COMMENT '审批人ID',
  `approver_name` varchar(64) DEFAULT NULL COMMENT '审批人姓名',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `approve_comment` text COMMENT '审批意见',
  `receiver_id` bigint(20) DEFAULT NULL COMMENT '接收人ID',
  `receiver_name` varchar(64) DEFAULT NULL COMMENT '接收人姓名',
  `receive_time` datetime DEFAULT NULL COMMENT '接收时间',
  `handler_id` bigint(20) DEFAULT NULL COMMENT '处理人ID',
  `handler_name` varchar(64) DEFAULT NULL COMMENT '处理人姓名',
  `process_start_time` datetime DEFAULT NULL COMMENT '处理开始时间',
  `process_end_time` datetime DEFAULT NULL COMMENT '处理结束时间',
  `process_result` varchar(255) DEFAULT NULL COMMENT '处理结果',
  `process_description` text COMMENT '处理过程描述',
  `lng` decimal(15,12) DEFAULT NULL COMMENT '经度',
  `lat` decimal(15,12) DEFAULT NULL COMMENT '纬度',
  `address` varchar(255) DEFAULT NULL COMMENT '发生地址',
  `urgency_level` varchar(32) DEFAULT 'MEDIUM' COMMENT '紧急程度：LOW-低 MEDIUM-普通 HIGH-重要 URGENT-紧急',
  `attachments` varchar(1000) DEFAULT NULL COMMENT '附件URL，多个逗号分隔',
  `coordination_note` text COMMENT '协作说明',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '追溯链路ID',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_event_id` (`event_id`),
  KEY `idx_source_dept_id` (`source_dept_id`),
  KEY `idx_target_dept_id` (`target_dept_id`),
  KEY `idx_status` (`status`),
  KEY `idx_target_type` (`target_type`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_approver_id` (`approver_id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_handler_id` (`handler_id`),
  KEY `idx_applicant_time` (`applicant_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跨街道协同流转记录表';

-- -------------------------------------------------------------
-- 补充: event_info 状态枚举扩展 (如需要)
-- -------------------------------------------------------------
-- 注意：如果 event_info 表的 status 字段是枚举类型，请执行以下语句扩展枚举值
-- ALTER TABLE `event_info` MODIFY COLUMN `status` enum('PENDING','APPROVED','DISPATCHED','HANDLED','COMPLETED','REJECTED','TRANSFERRING','TRANSFERRED') DEFAULT 'PENDING' COMMENT '事件状态';

-- -------------------------------------------------------------
-- 插入初始测试数据 (可选)
-- -------------------------------------------------------------
-- 如果需要测试数据，可以取消以下注释
/*
INSERT INTO `event_cross_street_transfer` 
(`event_id`, `event_no`, `event_title`, `event_type`, `source_dept_id`, `source_dept_name`, `source_grid_id`, `source_grid_name`,
 `target_dept_id`, `target_dept_name`, `target_dept_code`, `target_type`, `transfer_reason`, `cross_boundary_description`, `impact_range`,
 `status`, `applicant_id`, `applicant_name`, `applicant_time`, `urgency_level`, `lng`, `lat`, `address`, `trace_id`)
VALUES 
(1, 'EVT2025060001', '跨街道河道污染事件', 'environment', 1, 'XX街道办事处', 1, '第一网格',
 2, 'YY街道办事处', 'STREET_002', 'STREET', '该河道位于两个街道交界处，污染已扩散至相邻街道辖区',
 '河道从XX街道流向YY街道，交界处水质明显恶化', '影响范围约2公里，涉及沿岸3个小区',
 'PENDING_APPROVAL', 1, '张三', NOW(), 'HIGH', 116.4074, 39.9042, 'XX街道与YY街道交界处河道', REPLACE(UUID(), '-', ''));
*/

-- =============================================================
-- 功能说明:
-- =============================================================
-- 1. 支持三种转派类型：相邻街道(STREET)、委办局(BUREAU)、区级部门(COUNTY)
-- 2. 完整流转状态链路：
--    申请流转 -> 待审批(PENDING_APPROVAL) -> 审批通过(APPROVED) -> 已转派(TRANSFERRED)
--    -> 已接收(ACCEPTED) -> 处理中(PROCESSING) -> 已完成(COMPLETED)
--    审批不通过 -> 已驳回(REJECTED)
-- 3. 全链路追溯：每条流转记录都有完整的操作人、时间、意见记录
-- 4. 紧急程度分级：低(LOW)、普通(MEDIUM)、重要(HIGH)、紧急(URGENT)
-- 5. 通知推送：各节点状态变更时自动推送通知给相关人员
-- =============================================================
