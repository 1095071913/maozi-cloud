/*
 Navicat Premium Dump SQL

 Source Server         : 本地环境mysql
 Source Server Type    : MySQL
 Source Server Version : 80029 (8.0.29)
 Source Host           : localhost:3306
 Source Schema         : maozi-cloud-xxljob-middleware-db

 Target Server Type    : MySQL
 Target Server Version : 80029 (8.0.29)
 File Encoding         : 65001

 Date: 31/10/2026 09:18:37
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP DATABASE IF EXISTS `maozi-cloud-xxljob-middleware-db`;
CREATE DATABASE `maozi-cloud-xxljob-middleware-db` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `maozi-cloud-xxljob-middleware-db`;

-- ----------------------------
-- Table structure for xxl_job_group
-- ----------------------------
DROP TABLE IF EXISTS `xxl_job_group`;
CREATE TABLE `xxl_job_group` (
                                 `id` int NOT NULL AUTO_INCREMENT,
                                 `app_name` varchar(64) NOT NULL COMMENT '执行器AppName',
                                 `title` varchar(64) NOT NULL COMMENT '执行器名称',
                                 `address_type` tinyint NOT NULL DEFAULT '0' COMMENT '执行器地址类型：0=自动注册、1=手动录入',
                                 `address_list` text COMMENT '执行器地址列表，多地址逗号分隔',
                                 `update_time` datetime DEFAULT NULL,
                                 PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of xxl_job_group
-- ----------------------------
BEGIN;
INSERT INTO `xxl_job_group` (`id`, `app_name`, `title`, `address_type`, `address_list`, `update_time`) VALUES (1, 'xxl-job-executor-sample', '通用执行器Sample', 0, NULL, '2026-10-31 09:18:30');
COMMIT;

-- ----------------------------
-- Table structure for xxl_job_info
-- ----------------------------
DROP TABLE IF EXISTS `xxl_job_info`;
CREATE TABLE `xxl_job_info` (
                                `id` int NOT NULL AUTO_INCREMENT,
                                `job_group` int NOT NULL COMMENT '执行器主键ID',
                                `job_desc` varchar(255) NOT NULL,
                                `add_time` datetime DEFAULT NULL,
                                `update_time` datetime DEFAULT NULL,
                                `author` varchar(64) DEFAULT NULL COMMENT '作者',
                                `alarm_email` varchar(255) DEFAULT NULL COMMENT '报警邮件',
                                `schedule_type` varchar(50) NOT NULL DEFAULT 'NONE' COMMENT '调度类型',
                                `schedule_conf` varchar(128) DEFAULT NULL COMMENT '调度配置，值含义取决于调度类型',
                                `misfire_strategy` varchar(50) NOT NULL DEFAULT 'DO_NOTHING' COMMENT '调度过期策略',
                                `executor_route_strategy` varchar(50) DEFAULT NULL COMMENT '执行器路由策略',
                                `executor_handler` varchar(255) DEFAULT NULL COMMENT '任务handler',
                                `executor_param` text COMMENT '任务参数',
                                `executor_block_strategy` varchar(50) DEFAULT NULL COMMENT '阻塞处理策略',
                                `executor_timeout` int NOT NULL DEFAULT '0' COMMENT '任务执行超时时间，单位秒',
                                `executor_fail_retry_count` int NOT NULL DEFAULT '0' COMMENT '失败重试次数',
                                `glue_type` varchar(50) NOT NULL COMMENT 'GLUE类型',
                                `glue_source` mediumtext COMMENT 'GLUE源代码',
                                `glue_remark` varchar(128) DEFAULT NULL COMMENT 'GLUE备注',
                                `glue_updatetime` datetime DEFAULT NULL COMMENT 'GLUE更新时间',
                                `child_jobid` varchar(255) DEFAULT NULL COMMENT '子任务ID，多个逗号分隔',
                                `trigger_status` tinyint NOT NULL DEFAULT '0' COMMENT '调度状态：0-停止，1-运行',
                                `trigger_last_time` bigint NOT NULL DEFAULT '0' COMMENT '上次调度时间',
                                `trigger_next_time` bigint NOT NULL DEFAULT '0' COMMENT '下次调度时间',
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of xxl_job_info
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for xxl_job_lock
-- ----------------------------
DROP TABLE IF EXISTS `xxl_job_lock`;
CREATE TABLE `xxl_job_lock` (
                                `lock_name` varchar(50) NOT NULL COMMENT '锁名称',
                                PRIMARY KEY (`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of xxl_job_lock
-- ----------------------------
BEGIN;
INSERT INTO `xxl_job_lock` (`lock_name`) VALUES ('schedule_lock');
COMMIT;

-- ----------------------------
-- Table structure for xxl_job_log
-- ----------------------------
DROP TABLE IF EXISTS `xxl_job_log`;
CREATE TABLE `xxl_job_log` (
                               `id` bigint NOT NULL AUTO_INCREMENT,
                               `job_group` int NOT NULL COMMENT '执行器主键ID',
                               `job_id` int NOT NULL COMMENT '任务，主键ID',
                               `executor_address` varchar(255) DEFAULT NULL COMMENT '执行器地址，本次执行的地址',
                               `executor_handler` varchar(255) DEFAULT NULL COMMENT '任务handler',
                               `executor_param` text COMMENT '任务参数',
                               `executor_sharding_param` varchar(20) DEFAULT NULL COMMENT '任务分片参数，格式如 1/2',
                               `executor_fail_retry_count` int NOT NULL DEFAULT '0' COMMENT '失败重试次数',
                               `trigger_time` datetime DEFAULT NULL COMMENT '调度-时间',
                               `trigger_code` int NOT NULL COMMENT '调度-结果',
                               `trigger_msg` text COMMENT '调度-日志',
                               `handle_time` datetime DEFAULT NULL COMMENT '执行-时间',
                               `handle_code` int NOT NULL COMMENT '执行-状态',
                               `handle_msg` text COMMENT '执行-日志',
                               `alarm_status` tinyint NOT NULL DEFAULT '0' COMMENT '告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败',
                               PRIMARY KEY (`id`),
                               KEY `I_trigger_time` (`trigger_time`),
                               KEY `I_handle_code` (`handle_code`),
                               KEY `I_jobgroup` (`job_group`),
                               KEY `I_jobid` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of xxl_job_log
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for xxl_job_log_report
-- ----------------------------
DROP TABLE IF EXISTS `xxl_job_log_report`;
CREATE TABLE `xxl_job_log_report` (
                                      `id` int NOT NULL AUTO_INCREMENT,
                                      `trigger_day` datetime DEFAULT NULL COMMENT '调度-时间',
                                      `running_count` int NOT NULL DEFAULT '0' COMMENT '运行中-日志数量',
                                      `suc_count` int NOT NULL DEFAULT '0' COMMENT '执行成功-日志数量',
                                      `fail_count` int NOT NULL DEFAULT '0' COMMENT '执行失败-日志数量',
                                      `update_time` datetime DEFAULT NULL,
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `i_trigger_day` (`trigger_day`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10723 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of xxl_job_log_report
-- ----------------------------
BEGIN;
INSERT INTO `xxl_job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`) VALUES (1, '2026-10-04 00:00:00', 0, 0, 0, '2026-10-04 17:00:36');
INSERT INTO `xxl_job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`) VALUES (2, '2026-10-03 00:00:00', 0, 0, 0, '2026-10-04 17:00:37');
INSERT INTO `xxl_job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`) VALUES (3, '2026-10-02 00:00:00', 0, 0, 0, '2026-10-04 17:00:37');
INSERT INTO `xxl_job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`) VALUES (4, '2026-10-12 00:00:00', 0, 0, 0, '2026-10-12 09:32:00');
INSERT INTO `xxl_job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`) VALUES (5, '2026-10-11 00:00:00', 0, 0, 0, '2026-10-12 09:32:00');
INSERT INTO `xxl_job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`) VALUES (6, '2026-10-10 00:00:00', 0, 0, 0, '2026-10-12 09:32:00');
INSERT INTO `xxl_job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`) VALUES (10, '2026-10-16 00:00:00', 0, 0, 0, '2026-10-18 08:25:00');
INSERT INTO `xxl_job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`) VALUES (11, '2026-10-15 00:00:00', 0, 0, 0, '2026-10-17 23:59:00');
INSERT INTO `xxl_job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`) VALUES (12, '2026-10-14 00:00:00', 0, 0, 0, '2026-10-16 23:59:00');
INSERT INTO `xxl_job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`) VALUES (3112, '2026-10-17 00:00:00', 0, 0, 0, '2026-10-18 08:25:00');
INSERT INTO `xxl_job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`) VALUES (7438, '2026-10-18 00:00:00', 0, 0, 0, '2026-10-18 08:25:00');
INSERT INTO `xxl_job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`) VALUES (8956, '2026-10-30 00:00:00', 0, 0, 0, '2026-10-31 09:18:00');
INSERT INTO `xxl_job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`) VALUES (8957, '2026-10-29 00:00:00', 0, 0, 0, '2026-10-31 09:18:00');
INSERT INTO `xxl_job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`) VALUES (8958, '2026-10-28 00:00:00', 0, 0, 0, '2026-10-30 23:59:00');
INSERT INTO `xxl_job_log_report` (`id`, `trigger_day`, `running_count`, `suc_count`, `fail_count`, `update_time`) VALUES (9046, '2026-10-31 00:00:00', 0, 0, 0, '2026-10-31 09:18:00');
COMMIT;

-- ----------------------------
-- Table structure for xxl_job_logglue
-- ----------------------------
DROP TABLE IF EXISTS `xxl_job_logglue`;
CREATE TABLE `xxl_job_logglue` (
                                   `id` int NOT NULL AUTO_INCREMENT,
                                   `job_id` int NOT NULL COMMENT '任务，主键ID',
                                   `glue_type` varchar(50) DEFAULT NULL COMMENT 'GLUE类型',
                                   `glue_source` mediumtext COMMENT 'GLUE源代码',
                                   `glue_remark` varchar(128) NOT NULL COMMENT 'GLUE备注',
                                   `add_time` datetime DEFAULT NULL,
                                   `update_time` datetime DEFAULT NULL,
                                   PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of xxl_job_logglue
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for xxl_job_registry
-- ----------------------------
DROP TABLE IF EXISTS `xxl_job_registry`;
CREATE TABLE `xxl_job_registry` (
                                    `id` bigint NOT NULL AUTO_INCREMENT,
                                    `registry_group` varchar(50) NOT NULL,
                                    `registry_key` varchar(255) NOT NULL,
                                    `registry_value` varchar(255) NOT NULL,
                                    `update_time` datetime DEFAULT NULL,
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `i_g_k_v` (`registry_group`,`registry_key`,`registry_value`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of xxl_job_registry
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for xxl_job_user
-- ----------------------------
DROP TABLE IF EXISTS `xxl_job_user`;
CREATE TABLE `xxl_job_user` (
                                `id` int NOT NULL AUTO_INCREMENT,
                                `username` varchar(50) NOT NULL COMMENT '账号',
                                `password` varchar(100) NOT NULL COMMENT '密码加密信息',
                                `token` varchar(100) DEFAULT NULL COMMENT '登录token',
                                `role` tinyint NOT NULL COMMENT '角色：0-普通用户、1-管理员',
                                `permission` varchar(255) DEFAULT NULL COMMENT '权限：执行器ID列表，多个逗号分割',
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `i_username` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of xxl_job_user
-- ----------------------------
BEGIN;
INSERT INTO `xxl_job_user` (`id`, `username`, `password`, `token`, `role`, `permission`) VALUES (1, 'xxljob', '24f8879d1d4014868c1cf8a3e40b6e98ceb3c041ae9e72a3ed022089d9ec5800', '3ad7bed600c04fadab983994ff09a5c1', 1, NULL);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
