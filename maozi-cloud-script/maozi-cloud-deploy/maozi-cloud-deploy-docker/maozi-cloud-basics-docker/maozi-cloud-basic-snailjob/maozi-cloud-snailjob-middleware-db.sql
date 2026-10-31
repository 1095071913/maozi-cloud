/*
 Navicat Premium Dump SQL

 Source Server         : 本地环境mysql
 Source Server Type    : MySQL
 Source Server Version : 80029 (8.0.29)
 Source Host           : localhost:3306
 Source Schema         : maozi-cloud-snailjob-middleware-db

 Target Server Type    : MySQL
 Target Server Version : 80029 (8.0.29)
 File Encoding         : 65001

 Date: 31/10/2026 09:14:23
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
    
DROP DATABASE IF EXISTS `maozi-cloud-snailjob-middleware-db`;
CREATE DATABASE `maozi-cloud-snailjob-middleware-db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `maozi-cloud-snailjob-middleware-db`;

-- ----------------------------
-- Table structure for sj_distributed_lock
-- ----------------------------
DROP TABLE IF EXISTS `sj_distributed_lock`;
CREATE TABLE `sj_distributed_lock` (
                                       `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '锁名称',
                                       `lock_until` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '锁定时长',
                                       `locked_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '锁定时间',
                                       `locked_by` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '锁定者',
                                       `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
                                       PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='锁定表';

-- ----------------------------
-- Records of sj_distributed_lock
-- ----------------------------
BEGIN;
INSERT INTO `sj_distributed_lock` (`name`, `lock_until`, `locked_at`, `locked_by`, `create_dt`, `update_dt`) VALUES ('clearLog', '2026-10-31 09:00:24.248', '2026-10-31 08:59:24.248', '2116334181243412480', '2026-10-31 08:59:24', '2026-10-31 08:59:24');
INSERT INTO `sj_distributed_lock` (`name`, `lock_until`, `locked_at`, `locked_by`, `create_dt`, `update_dt`) VALUES ('clearOfflineNode', '2026-10-31 09:14:25.995', '2026-10-31 09:14:20.995', '2116334181243412480', '2026-10-31 08:59:24', '2026-10-31 09:14:21');
INSERT INTO `sj_distributed_lock` (`name`, `lock_until`, `locked_at`, `locked_by`, `create_dt`, `update_dt`) VALUES ('jobClearLog', '2026-10-31 09:00:24.069', '2026-10-31 08:59:24.069', '2116334181243412480', '2026-10-31 08:59:24', '2026-10-31 08:59:24');
INSERT INTO `sj_distributed_lock` (`name`, `lock_until`, `locked_at`, `locked_by`, `create_dt`, `update_dt`) VALUES ('jobLogMerge', '2026-10-31 09:14:24.096', '2026-10-31 09:13:24.096', '2116334181243412480', '2026-10-31 08:59:24', '2026-10-31 09:13:24');
INSERT INTO `sj_distributed_lock` (`name`, `lock_until`, `locked_at`, `locked_by`, `create_dt`, `update_dt`) VALUES ('jobSummaryDashboard', '2026-10-31 09:13:44.097', '2026-10-31 09:13:24.097', '2116334181243412480', '2026-10-31 08:59:24', '2026-10-31 09:13:24');
INSERT INTO `sj_distributed_lock` (`name`, `lock_until`, `locked_at`, `locked_by`, `create_dt`, `update_dt`) VALUES ('registerNode', '2026-10-31 09:14:25.995', '2026-10-31 09:14:20.995', '2116334181243412480', '2026-10-31 08:59:24', '2026-10-31 09:14:21');
INSERT INTO `sj_distributed_lock` (`name`, `lock_until`, `locked_at`, `locked_by`, `create_dt`, `update_dt`) VALUES ('retryErrorMoreThreshold', '2026-10-31 09:10:24.276', '2026-10-31 09:09:24.276', '2116334181243412480', '2026-10-31 08:59:24', '2026-10-31 09:09:24');
INSERT INTO `sj_distributed_lock` (`name`, `lock_until`, `locked_at`, `locked_by`, `create_dt`, `update_dt`) VALUES ('retryLogMerge', '2026-10-31 09:00:24.256', '2026-10-31 08:59:24.256', '2116334181243412480', '2026-10-31 08:59:24', '2026-10-31 08:59:24');
INSERT INTO `sj_distributed_lock` (`name`, `lock_until`, `locked_at`, `locked_by`, `create_dt`, `update_dt`) VALUES ('retrySummaryDashboard', '2026-10-31 09:13:44.099', '2026-10-31 09:13:24.099', '2116334181243412480', '2026-10-31 08:59:24', '2026-10-31 09:13:24');
INSERT INTO `sj_distributed_lock` (`name`, `lock_until`, `locked_at`, `locked_by`, `create_dt`, `update_dt`) VALUES ('retryTaskMoreThreshold', '2026-10-31 09:10:24.283', '2026-10-31 09:09:24.283', '2116334181243412480', '2026-10-31 08:59:24', '2026-10-31 09:09:24');
INSERT INTO `sj_distributed_lock` (`name`, `lock_until`, `locked_at`, `locked_by`, `create_dt`, `update_dt`) VALUES ('workflowJobSummarySchedule', '2026-10-31 09:13:44.098', '2026-10-31 09:13:24.098', '2116334181243412480', '2026-10-31 08:59:24', '2026-10-31 09:13:24');
COMMIT;

-- ----------------------------
-- Table structure for sj_group_config
-- ----------------------------
DROP TABLE IF EXISTS `sj_group_config`;
CREATE TABLE `sj_group_config` (
                                   `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                   `namespace_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                   `group_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '组名称',
                                   `description` varchar(256) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '组描述',
                                   `token` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'SJ_cKqBTPzCsWA3VyuCfFoccmuIEGXjr5KT' COMMENT 'token',
                                   `group_status` tinyint NOT NULL DEFAULT '0' COMMENT '组状态 0、未启用 1、启用',
                                   `version` int NOT NULL COMMENT '版本号',
                                   `group_partition` int NOT NULL COMMENT '分区',
                                   `id_generator_mode` tinyint NOT NULL DEFAULT '1' COMMENT '唯一id生成模式 默认号段模式',
                                   `init_scene` tinyint NOT NULL DEFAULT '0' COMMENT '是否初始化场景 0:否 1:是',
                                   `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_namespace_id_group_name` (`namespace_id`,`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='组配置';

-- ----------------------------
-- Records of sj_group_config
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sj_job
-- ----------------------------
DROP TABLE IF EXISTS `sj_job`;
CREATE TABLE `sj_job` (
                          `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                          `namespace_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                          `biz_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务ID',
                          `group_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '组名称',
                          `job_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
                          `args_str` text COLLATE utf8mb4_general_ci COMMENT '执行方法参数',
                          `args_type` tinyint NOT NULL DEFAULT '1' COMMENT '参数类型 ',
                          `next_trigger_at` bigint NOT NULL COMMENT '下次触发时间',
                          `job_status` tinyint NOT NULL DEFAULT '1' COMMENT '任务状态 0、关闭、1、开启',
                          `task_type` tinyint NOT NULL DEFAULT '1' COMMENT '任务类型 1、集群 2、广播 3、切片',
                          `route_key` tinyint NOT NULL DEFAULT '4' COMMENT '路由策略',
                          `executor_type` tinyint NOT NULL DEFAULT '1' COMMENT '执行器类型',
                          `executor_info` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '执行器名称',
                          `trigger_type` tinyint NOT NULL COMMENT '触发类型 1.CRON 表达式 2. 固定时间',
                          `trigger_interval` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '间隔时长',
                          `block_strategy` tinyint NOT NULL DEFAULT '1' COMMENT '阻塞策略 1、丢弃 2、覆盖 3、并行 4、恢复',
                          `executor_timeout` int NOT NULL DEFAULT '0' COMMENT '任务执行超时时间，单位秒',
                          `max_retry_times` int NOT NULL DEFAULT '0' COMMENT '最大重试次数',
                          `parallel_num` int NOT NULL DEFAULT '1' COMMENT '并行数',
                          `retry_interval` int NOT NULL DEFAULT '0' COMMENT '重试间隔(s)',
                          `bucket_index` int NOT NULL DEFAULT '0' COMMENT 'bucket',
                          `resident` tinyint NOT NULL DEFAULT '0' COMMENT '是否是常驻任务',
                          `notify_ids` varchar(128) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '通知告警场景配置id列表',
                          `owner_id` bigint DEFAULT NULL COMMENT '负责人id',
                          `labels` varchar(512) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '标签',
                          `description` varchar(256) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '描述',
                          `ext_attrs` varchar(256) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '扩展字段',
                          `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 1、删除',
                          `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `uk_sj_job_01` (`namespace_id`,`biz_id`),
                          KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`),
                          KEY `idx_job_status_bucket_index` (`job_status`,`bucket_index`),
                          KEY `idx_create_dt` (`create_dt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='任务信息';

-- ----------------------------
-- Records of sj_job
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sj_job_executor
-- ----------------------------
DROP TABLE IF EXISTS `sj_job_executor`;
CREATE TABLE `sj_job_executor` (
                                   `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                   `namespace_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                   `group_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '组名称',
                                   `executor_info` varchar(256) COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务执行器名称',
                                   `executor_type` varchar(3) COLLATE utf8mb4_general_ci NOT NULL COMMENT '1:java 2:python 3:go',
                                   `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`),
                                   KEY `idx_create_dt` (`create_dt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='任务执行器信息';

-- ----------------------------
-- Records of sj_job_executor
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sj_job_log_message
-- ----------------------------
DROP TABLE IF EXISTS `sj_job_log_message`;
CREATE TABLE `sj_job_log_message` (
                                      `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                      `namespace_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                      `group_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '组名称',
                                      `job_id` bigint NOT NULL COMMENT '任务信息id',
                                      `task_batch_id` bigint NOT NULL COMMENT '任务批次id',
                                      `task_id` bigint NOT NULL COMMENT '调度任务id',
                                      `message` longtext COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度信息',
                                      `log_num` int NOT NULL DEFAULT '1' COMMENT '日志数量',
                                      `real_time` bigint NOT NULL DEFAULT '0' COMMENT '上报时间',
                                      `ext_attrs` varchar(256) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '扩展字段',
                                      `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      PRIMARY KEY (`id`),
                                      KEY `idx_task_batch_id_task_id` (`task_batch_id`,`task_id`),
                                      KEY `idx_create_dt` (`create_dt`),
                                      KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='调度日志';

-- ----------------------------
-- Records of sj_job_log_message
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sj_job_summary
-- ----------------------------
DROP TABLE IF EXISTS `sj_job_summary`;
CREATE TABLE `sj_job_summary` (
                                  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                  `namespace_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                  `group_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '组名称',
                                  `business_id` bigint NOT NULL COMMENT '业务id (job_id或workflow_id)',
                                  `system_task_type` tinyint NOT NULL DEFAULT '3' COMMENT '任务类型 3、JOB任务 4、WORKFLOW任务',
                                  `trigger_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '统计时间',
                                  `success_num` int NOT NULL DEFAULT '0' COMMENT '执行成功-日志数量',
                                  `fail_num` int NOT NULL DEFAULT '0' COMMENT '执行失败-日志数量',
                                  `fail_reason` varchar(512) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '失败原因',
                                  `stop_num` int NOT NULL DEFAULT '0' COMMENT '执行失败-日志数量',
                                  `stop_reason` varchar(512) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '失败原因',
                                  `cancel_num` int NOT NULL DEFAULT '0' COMMENT '执行失败-日志数量',
                                  `cancel_reason` varchar(512) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '失败原因',
                                  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_trigger_at_system_task_type_business_id` (`trigger_at`,`system_task_type`,`business_id`) USING BTREE,
                                  KEY `idx_namespace_id_group_name_business_id` (`namespace_id`,`group_name`,`business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='DashBoard_Job';

-- ----------------------------
-- Records of sj_job_summary
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sj_job_task
-- ----------------------------
DROP TABLE IF EXISTS `sj_job_task`;
CREATE TABLE `sj_job_task` (
                               `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                               `namespace_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                               `group_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '组名称',
                               `job_id` bigint NOT NULL COMMENT '任务信息id',
                               `task_batch_id` bigint NOT NULL COMMENT '调度任务id',
                               `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父执行器id',
                               `task_status` tinyint NOT NULL DEFAULT '0' COMMENT '执行的状态 0、失败 1、成功',
                               `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
                               `mr_stage` tinyint DEFAULT NULL COMMENT '动态分片所处阶段 1:map 2:reduce 3:mergeReduce',
                               `leaf` tinyint NOT NULL DEFAULT '1' COMMENT '叶子节点',
                               `task_name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '任务名称',
                               `client_info` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '客户端地址 clientId#ip:port',
                               `wf_context` text COLLATE utf8mb4_general_ci COMMENT '工作流全局上下文',
                               `result_message` text COLLATE utf8mb4_general_ci NOT NULL COMMENT '执行结果',
                               `args_str` text COLLATE utf8mb4_general_ci COMMENT '执行方法参数',
                               `args_type` tinyint NOT NULL DEFAULT '1' COMMENT '参数类型 ',
                               `ext_attrs` varchar(256) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '扩展字段',
                               `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                               PRIMARY KEY (`id`),
                               KEY `idx_task_batch_id_task_status` (`task_batch_id`,`task_status`),
                               KEY `idx_create_dt` (`create_dt`),
                               KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='任务实例';

-- ----------------------------
-- Records of sj_job_task
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sj_job_task_batch
-- ----------------------------
DROP TABLE IF EXISTS `sj_job_task_batch`;
CREATE TABLE `sj_job_task_batch` (
                                     `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `namespace_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                     `group_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '组名称',
                                     `job_id` bigint NOT NULL COMMENT '任务id',
                                     `workflow_node_id` bigint NOT NULL DEFAULT '0' COMMENT '工作流节点id',
                                     `parent_workflow_node_id` bigint NOT NULL DEFAULT '0' COMMENT '工作流任务父批次id',
                                     `workflow_task_batch_id` bigint NOT NULL DEFAULT '0' COMMENT '工作流任务批次id',
                                     `task_batch_status` tinyint NOT NULL DEFAULT '0' COMMENT '任务批次状态 0、失败 1、成功',
                                     `operation_reason` tinyint NOT NULL DEFAULT '0' COMMENT '操作原因',
                                     `execution_at` bigint NOT NULL DEFAULT '0' COMMENT '任务执行时间',
                                     `system_task_type` tinyint NOT NULL DEFAULT '3' COMMENT '任务类型 3、JOB任务 4、WORKFLOW任务',
                                     `parent_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '父节点',
                                     `ext_attrs` varchar(256) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '扩展字段',
                                     `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 1、删除',
                                     `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                     PRIMARY KEY (`id`),
                                     KEY `idx_job_id_task_batch_status` (`job_id`,`task_batch_status`),
                                     KEY `idx_create_dt` (`create_dt`),
                                     KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`),
                                     KEY `idx_workflow_task_batch_id_workflow_node_id` (`workflow_task_batch_id`,`workflow_node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='任务批次';

-- ----------------------------
-- Records of sj_job_task_batch
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sj_namespace
-- ----------------------------
DROP TABLE IF EXISTS `sj_namespace`;
CREATE TABLE `sj_namespace` (
                                `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
                                `unique_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '唯一id',
                                `description` varchar(256) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '描述',
                                `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 1、删除',
                                `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_unique_id` (`unique_id`),
                                KEY `idx_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='命名空间';

-- ----------------------------
-- Records of sj_namespace
-- ----------------------------
BEGIN;
INSERT INTO `sj_namespace` (`id`, `name`, `unique_id`, `description`, `deleted`, `create_dt`, `update_dt`) VALUES (1, 'Default', '764d604ec6fc45f68cd92514c40e9e1a', '', 0, '2026-10-18 12:30:35', '2026-10-18 12:30:35');
COMMIT;

-- ----------------------------
-- Table structure for sj_notify_config
-- ----------------------------
DROP TABLE IF EXISTS `sj_notify_config`;
CREATE TABLE `sj_notify_config` (
                                    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `namespace_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                    `group_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '组名称',
                                    `notify_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '通知名称',
                                    `system_task_type` tinyint NOT NULL DEFAULT '3' COMMENT '任务类型 1. 重试任务 2. 重试回调 3、JOB任务 4、WORKFLOW任务',
                                    `notify_status` tinyint NOT NULL DEFAULT '0' COMMENT '通知状态 0、未启用 1、启用',
                                    `recipient_ids` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '接收人id列表',
                                    `notify_threshold` int NOT NULL DEFAULT '0' COMMENT '通知阈值',
                                    `notify_scene` tinyint NOT NULL DEFAULT '0' COMMENT '通知场景',
                                    `rate_limiter_status` tinyint NOT NULL DEFAULT '0' COMMENT '限流状态 0、未启用 1、启用',
                                    `rate_limiter_threshold` int NOT NULL DEFAULT '0' COMMENT '每秒限流阈值',
                                    `description` varchar(256) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '描述',
                                    `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_namespace_id_group_name_scene_name` (`namespace_id`,`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知配置';

-- ----------------------------
-- Records of sj_notify_config
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sj_notify_recipient
-- ----------------------------
DROP TABLE IF EXISTS `sj_notify_recipient`;
CREATE TABLE `sj_notify_recipient` (
                                       `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                       `namespace_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                       `recipient_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '接收人名称',
                                       `notify_type` tinyint NOT NULL DEFAULT '0' COMMENT '通知类型 1、钉钉 2、邮件 3、企业微信 4 飞书 5 webhook',
                                       `notify_attribute` varchar(512) COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置属性',
                                       `description` varchar(256) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '描述',
                                       `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                       PRIMARY KEY (`id`),
                                       KEY `idx_namespace_id` (`namespace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='告警通知接收人';

-- ----------------------------
-- Records of sj_notify_recipient
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sj_retry
-- ----------------------------
DROP TABLE IF EXISTS `sj_retry`;
CREATE TABLE `sj_retry` (
                            `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                            `namespace_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                            `group_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '组名称',
                            `group_id` bigint NOT NULL COMMENT '组Id',
                            `scene_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '场景名称',
                            `scene_id` bigint NOT NULL COMMENT '场景ID',
                            `idempotent_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '幂等id',
                            `biz_no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '业务编号',
                            `executor_name` varchar(512) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '执行器名称',
                            `args_str` text COLLATE utf8mb4_general_ci NOT NULL COMMENT '执行方法参数',
                            `ext_attrs` text COLLATE utf8mb4_general_ci NOT NULL COMMENT '扩展字段',
                            `serializer_name` varchar(32) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'jackson' COMMENT '执行方法参数序列化器名称',
                            `next_trigger_at` bigint NOT NULL COMMENT '下次触发时间',
                            `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
                            `retry_status` tinyint NOT NULL DEFAULT '0' COMMENT '重试状态 0、重试中 1、成功 2、最大重试次数',
                            `task_type` tinyint NOT NULL DEFAULT '1' COMMENT '任务类型 1、重试数据 2、回调数据',
                            `bucket_index` int NOT NULL DEFAULT '0' COMMENT 'bucket',
                            `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父节点id',
                            `deleted` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
                            `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_scene_tasktype_idempotentid_deleted` (`scene_id`,`task_type`,`idempotent_id`,`deleted`),
                            KEY `idx_biz_no` (`biz_no`),
                            KEY `idx_idempotent_id` (`idempotent_id`),
                            KEY `idx_retry_status_bucket_index` (`retry_status`,`bucket_index`),
                            KEY `idx_parent_id` (`parent_id`),
                            KEY `idx_create_dt` (`create_dt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='重试信息表';

-- ----------------------------
-- Records of sj_retry
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sj_retry_dead_letter
-- ----------------------------
DROP TABLE IF EXISTS `sj_retry_dead_letter`;
CREATE TABLE `sj_retry_dead_letter` (
                                        `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                        `namespace_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                        `group_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '组名称',
                                        `group_id` bigint NOT NULL COMMENT '组Id',
                                        `scene_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '场景名称',
                                        `scene_id` bigint NOT NULL COMMENT '场景ID',
                                        `idempotent_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '幂等id',
                                        `biz_no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '业务编号',
                                        `executor_name` varchar(512) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '执行器名称',
                                        `serializer_name` varchar(32) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'jackson' COMMENT '执行方法参数序列化器名称',
                                        `args_str` text COLLATE utf8mb4_general_ci NOT NULL COMMENT '执行方法参数',
                                        `ext_attrs` text COLLATE utf8mb4_general_ci NOT NULL COMMENT '扩展字段',
                                        `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        PRIMARY KEY (`id`),
                                        KEY `idx_namespace_id_group_name_scene_name` (`namespace_id`,`group_name`,`scene_name`),
                                        KEY `idx_idempotent_id` (`idempotent_id`),
                                        KEY `idx_biz_no` (`biz_no`),
                                        KEY `idx_create_dt` (`create_dt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='死信队列表';

-- ----------------------------
-- Records of sj_retry_dead_letter
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sj_retry_scene_config
-- ----------------------------
DROP TABLE IF EXISTS `sj_retry_scene_config`;
CREATE TABLE `sj_retry_scene_config` (
                                         `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `namespace_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                         `scene_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '场景名称',
                                         `group_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '组名称',
                                         `scene_status` tinyint NOT NULL DEFAULT '0' COMMENT '组状态 0、未启用 1、启用',
                                         `max_retry_count` int NOT NULL DEFAULT '5' COMMENT '最大重试次数',
                                         `back_off` tinyint NOT NULL DEFAULT '1' COMMENT '1、默认等级 2、固定间隔时间 3、CRON 表达式',
                                         `trigger_interval` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '间隔时长',
                                         `notify_ids` varchar(128) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '通知告警场景配置id列表',
                                         `deadline_request` bigint unsigned NOT NULL DEFAULT '60000' COMMENT 'Deadline Request 调用链超时 单位毫秒',
                                         `executor_timeout` int unsigned NOT NULL DEFAULT '5' COMMENT '任务执行超时时间，单位秒',
                                         `route_key` tinyint NOT NULL DEFAULT '4' COMMENT '路由策略',
                                         `block_strategy` tinyint NOT NULL DEFAULT '1' COMMENT '阻塞策略 1、丢弃 2、覆盖 3、并行',
                                         `cb_status` tinyint NOT NULL DEFAULT '0' COMMENT '回调状态 0、不开启 1、开启',
                                         `cb_trigger_type` tinyint NOT NULL DEFAULT '1' COMMENT '1、默认等级 2、固定间隔时间 3、CRON 表达式',
                                         `cb_max_count` int NOT NULL DEFAULT '16' COMMENT '回调的最大执行次数',
                                         `cb_trigger_interval` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '回调的最大执行次数',
                                         `owner_id` bigint DEFAULT NULL COMMENT '负责人id',
                                         `labels` varchar(512) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '标签',
                                         `description` varchar(256) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '描述',
                                         `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_namespace_id_group_name_scene_name` (`namespace_id`,`group_name`,`scene_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='场景配置';

-- ----------------------------
-- Records of sj_retry_scene_config
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sj_retry_summary
-- ----------------------------
DROP TABLE IF EXISTS `sj_retry_summary`;
CREATE TABLE `sj_retry_summary` (
                                    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `namespace_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                    `group_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '组名称',
                                    `scene_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '场景名称',
                                    `trigger_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '统计时间',
                                    `running_num` int NOT NULL DEFAULT '0' COMMENT '重试中-日志数量',
                                    `finish_num` int NOT NULL DEFAULT '0' COMMENT '重试完成-日志数量',
                                    `max_count_num` int NOT NULL DEFAULT '0' COMMENT '重试到达最大次数-日志数量',
                                    `suspend_num` int NOT NULL DEFAULT '0' COMMENT '暂停重试-日志数量',
                                    `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uk_scene_name_trigger_at` (`namespace_id`,`group_name`,`scene_name`,`trigger_at`) USING BTREE,
                                    KEY `idx_trigger_at` (`trigger_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='DashBoard_Retry';

-- ----------------------------
-- Records of sj_retry_summary
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sj_retry_task
-- ----------------------------
DROP TABLE IF EXISTS `sj_retry_task`;
CREATE TABLE `sj_retry_task` (
                                 `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                 `namespace_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                 `group_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '组名称',
                                 `scene_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '场景名称',
                                 `retry_id` bigint NOT NULL COMMENT '重试信息Id',
                                 `ext_attrs` text COLLATE utf8mb4_general_ci NOT NULL COMMENT '扩展字段',
                                 `task_status` tinyint NOT NULL DEFAULT '1' COMMENT '重试状态',
                                 `task_type` tinyint NOT NULL DEFAULT '1' COMMENT '任务类型 1、重试数据 2、回调数据',
                                 `operation_reason` tinyint NOT NULL DEFAULT '0' COMMENT '操作原因',
                                 `client_info` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '客户端地址 clientId#ip:port',
                                 `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                 PRIMARY KEY (`id`),
                                 KEY `idx_group_name_scene_name` (`namespace_id`,`group_name`,`scene_name`),
                                 KEY `task_status` (`task_status`),
                                 KEY `idx_create_dt` (`create_dt`),
                                 KEY `idx_retry_id` (`retry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='重试任务表';

-- ----------------------------
-- Records of sj_retry_task
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sj_retry_task_log_message
-- ----------------------------
DROP TABLE IF EXISTS `sj_retry_task_log_message`;
CREATE TABLE `sj_retry_task_log_message` (
                                             `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `namespace_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                             `group_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '组名称',
                                             `retry_id` bigint NOT NULL COMMENT '重试信息Id',
                                             `retry_task_id` bigint NOT NULL COMMENT '重试任务Id',
                                             `message` longtext COLLATE utf8mb4_general_ci NOT NULL COMMENT '异常信息',
                                             `log_num` int NOT NULL DEFAULT '1' COMMENT '日志数量',
                                             `real_time` bigint NOT NULL DEFAULT '0' COMMENT '上报时间',
                                             `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             PRIMARY KEY (`id`),
                                             KEY `idx_namespace_id_group_name_retry_task_id` (`namespace_id`,`group_name`,`retry_task_id`),
                                             KEY `idx_create_dt` (`create_dt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='任务调度日志信息记录表';

-- ----------------------------
-- Records of sj_retry_task_log_message
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sj_server_node
-- ----------------------------
DROP TABLE IF EXISTS `sj_server_node`;
CREATE TABLE `sj_server_node` (
                                  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                  `namespace_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                  `group_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '组名称',
                                  `host_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '主机id',
                                  `host_ip` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '机器ip',
                                  `host_port` int NOT NULL COMMENT '机器端口',
                                  `expire_at` datetime NOT NULL COMMENT '过期时间',
                                  `node_type` tinyint NOT NULL COMMENT '节点类型 1、客户端 2、是服务端',
                                  `ext_attrs` varchar(256) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '扩展字段',
                                  `labels` varchar(512) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '标签',
                                  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_host_id_host_ip` (`host_id`,`host_ip`),
                                  KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`),
                                  KEY `idx_expire_at_node_type` (`expire_at`,`node_type`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='服务器节点';

-- ----------------------------
-- Records of sj_server_node
-- ----------------------------
BEGIN;
INSERT INTO `sj_server_node` (`id`, `namespace_id`, `group_name`, `host_id`, `host_ip`, `host_port`, `expire_at`, `node_type`, `ext_attrs`, `labels`, `create_dt`, `update_dt`) VALUES (2, 'DEFAULT_SERVER_NAMESPACE_ID', 'DEFAULT_SERVER', '2116334181243412480', '172.18.0.21', 17888, '2026-10-31 09:14:34', 2, '{\"webPort\":8080,\"systemVersion\":\"1.10.0\"}', NULL, '2026-10-31 08:59:24', '2026-10-31 01:14:04');
COMMIT;

-- ----------------------------
-- Table structure for sj_system_user
-- ----------------------------
DROP TABLE IF EXISTS `sj_system_user`;
CREATE TABLE `sj_system_user` (
                                  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                  `username` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '账号',
                                  `password` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
                                  `role` tinyint NOT NULL DEFAULT '0' COMMENT '角色：1-普通用户、2-管理员',
                                  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_username` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统用户表';

-- ----------------------------
-- Records of sj_system_user
-- ----------------------------
BEGIN;
INSERT INTO `sj_system_user` (`id`, `username`, `password`, `role`, `create_dt`, `update_dt`) VALUES (1, 'snailjob', 'e46a6a33bffa8bd95c8dcedcffc1d800705f2e2cf190e41686a751ab581e91b9', 2, '2026-10-18 12:30:35', '2026-10-31 01:11:42');
COMMIT;

-- ----------------------------
-- Table structure for sj_system_user_permission
-- ----------------------------
DROP TABLE IF EXISTS `sj_system_user_permission`;
CREATE TABLE `sj_system_user_permission` (
                                             `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `group_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '组名称',
                                             `namespace_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                             `system_user_id` bigint NOT NULL COMMENT '系统用户id',
                                             `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                             PRIMARY KEY (`id`),
                                             UNIQUE KEY `uk_namespace_id_group_name_system_user_id` (`namespace_id`,`group_name`,`system_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统用户权限表';

-- ----------------------------
-- Records of sj_system_user_permission
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sj_workflow
-- ----------------------------
DROP TABLE IF EXISTS `sj_workflow`;
CREATE TABLE `sj_workflow` (
                               `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                               `workflow_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '工作流名称',
                               `namespace_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                               `biz_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务ID',
                               `group_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '组名称',
                               `workflow_status` tinyint NOT NULL DEFAULT '1' COMMENT '工作流状态 0、关闭、1、开启',
                               `trigger_type` tinyint NOT NULL COMMENT '触发类型 1.CRON 表达式 2. 固定时间',
                               `trigger_interval` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '间隔时长',
                               `next_trigger_at` bigint NOT NULL COMMENT '下次触发时间',
                               `block_strategy` tinyint NOT NULL DEFAULT '1' COMMENT '阻塞策略 1、丢弃 2、覆盖 3、并行',
                               `executor_timeout` int NOT NULL DEFAULT '0' COMMENT '任务执行超时时间，单位秒',
                               `description` varchar(256) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '描述',
                               `flow_info` text COLLATE utf8mb4_general_ci COMMENT '流程信息',
                               `wf_context` text COLLATE utf8mb4_general_ci COMMENT '上下文',
                               `notify_ids` varchar(128) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '通知告警场景配置id列表',
                               `bucket_index` int NOT NULL DEFAULT '0' COMMENT 'bucket',
                               `version` int NOT NULL COMMENT '版本号',
                               `owner_id` bigint DEFAULT NULL COMMENT '负责人id',
                               `ext_attrs` varchar(256) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '扩展字段',
                               `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 1、删除',
                               `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `uk_sj_workflow_01` (`namespace_id`,`biz_id`),
                               KEY `idx_create_dt` (`create_dt`),
                               KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作流';

-- ----------------------------
-- Records of sj_workflow
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sj_workflow_node
-- ----------------------------
DROP TABLE IF EXISTS `sj_workflow_node`;
CREATE TABLE `sj_workflow_node` (
                                    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `namespace_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                    `node_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '节点名称',
                                    `group_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '组名称',
                                    `job_id` bigint NOT NULL COMMENT '任务信息id',
                                    `workflow_id` bigint NOT NULL COMMENT '工作流ID',
                                    `node_type` tinyint NOT NULL DEFAULT '1' COMMENT '1、任务节点 2、条件节点',
                                    `expression_type` tinyint NOT NULL DEFAULT '0' COMMENT '1、SpEl、2、Aviator 3、QL',
                                    `fail_strategy` tinyint NOT NULL DEFAULT '1' COMMENT '失败策略 1、跳过 2、阻塞',
                                    `workflow_node_status` tinyint NOT NULL DEFAULT '1' COMMENT '工作流节点状态 0、关闭、1、开启',
                                    `priority_level` int NOT NULL DEFAULT '1' COMMENT '优先级',
                                    `node_info` text COLLATE utf8mb4_general_ci COMMENT '节点信息 ',
                                    `version` int NOT NULL COMMENT '版本号',
                                    `ext_attrs` varchar(256) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '扩展字段',
                                    `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 1、删除',
                                    `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_create_dt` (`create_dt`),
                                    KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作流节点';

-- ----------------------------
-- Records of sj_workflow_node
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sj_workflow_task_batch
-- ----------------------------
DROP TABLE IF EXISTS `sj_workflow_task_batch`;
CREATE TABLE `sj_workflow_task_batch` (
                                          `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                          `namespace_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                          `group_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '组名称',
                                          `workflow_id` bigint NOT NULL COMMENT '工作流任务id',
                                          `task_batch_status` tinyint NOT NULL DEFAULT '0' COMMENT '任务批次状态 0、失败 1、成功',
                                          `operation_reason` tinyint NOT NULL DEFAULT '0' COMMENT '操作原因',
                                          `flow_info` text COLLATE utf8mb4_general_ci COMMENT '流程信息',
                                          `wf_context` text COLLATE utf8mb4_general_ci COMMENT '全局上下文',
                                          `execution_at` bigint NOT NULL DEFAULT '0' COMMENT '任务执行时间',
                                          `ext_attrs` varchar(256) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '扩展字段',
                                          `version` int NOT NULL DEFAULT '1' COMMENT '版本号',
                                          `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 1、删除',
                                          `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                          `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                          PRIMARY KEY (`id`),
                                          KEY `idx_job_id_task_batch_status` (`workflow_id`,`task_batch_status`),
                                          KEY `idx_create_dt` (`create_dt`),
                                          KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作流批次';

-- ----------------------------
-- Records of sj_workflow_task_batch
-- ----------------------------
BEGIN;
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
