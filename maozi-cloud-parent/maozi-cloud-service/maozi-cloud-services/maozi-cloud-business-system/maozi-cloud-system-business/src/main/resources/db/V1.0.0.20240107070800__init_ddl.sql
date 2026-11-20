/*
 Navicat Premium Dump SQL

 Source Server         : 本地环境mysql
 Source Server Type    : MySQL
 Source Server Version : 80029 (8.0.29)
 Source Host           : localhost:3306
 Source Schema         : maozi-cloud-system-service-local-db

 Target Server Type    : MySQL
 Target Server Version : 80029 (8.0.29)
 File Encoding         : 65001

 Date: 07/09/2026 12:08:31
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 系统服务数据库初始化脚本：包含配置、地区、权限、角色、用户及关联关系表的建表语句与初始数据

-- ----------------------------
-- Table structure for system_config
-- ----------------------------
-- 全局配置表：按类型存储配置项，包含配置类型定义、中间件访问信息、AI 系统提示词与 AI 对话示例
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                 `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置名称',
                                 `alias` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '别名',
                                 `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置类型',
                                 `value` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置值',
                                 `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
                                 `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除（1是 0否）',
                                 `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
                                 `version` int NOT NULL DEFAULT '0' COMMENT '版本号',
                                 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 PRIMARY KEY (`id`) USING BTREE,
                                 KEY `idx_name` (`name`) USING BTREE,
                                 KEY `idx_type` (`type`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='全局配置';

-- ----------------------------
-- Table structure for system_config_region
-- ----------------------------
-- 地区表：存储中国省市区三级行政区划数据，含经纬度、拼音、邮政编码等扩展信息
DROP TABLE IF EXISTS `system_config_region`;
CREATE TABLE `system_config_region` (
                                        `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
                                        `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
                                        `parent_id` int NOT NULL DEFAULT '1' COMMENT '父级ID',
                                        `sname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '简称',
                                        `level` int NOT NULL COMMENT '级别',
                                        `city_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '编码',
                                        `mail_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '邮政编码',
                                        `mername` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '组合名称',
                                        `lng` decimal(10,6) NOT NULL COMMENT '经度',
  `lat` decimal(10,6) NOT NULL COMMENT '纬度',
  `pinyin` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '拼音',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除（1是 0否）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
  `version` int NOT NULL DEFAULT '0' COMMENT '版本号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 创建时间，默认当前时间戳
  PRIMARY KEY (`id`) USING BTREE,
  KEY `actable_idx_name` (`name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=900001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='地区';

-- ----------------------------
-- Table structure for system_permission
-- ----------------------------
-- 权限表：树形结构存储菜单与操作权限，level 为节点深度，type 区分权限类型
DROP TABLE IF EXISTS `system_permission`;
CREATE TABLE `system_permission` (
                                     `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
                                     `parent_id` int NOT NULL DEFAULT '0' COMMENT '父ID',
                                     `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '图标',
                                     `mark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标识',
                                     `level` int NOT NULL COMMENT '深度',
                                     `route` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '路由',
                                     `service_uri` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '服务地址',
                                     `type` tinyint NOT NULL COMMENT '类型',
                                     `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
                                     `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除（1是 0否）',
                                     `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
                                     `version` int NOT NULL DEFAULT '0' COMMENT '版本号',
                                     `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 创建时间，默认当前时间戳
                                     PRIMARY KEY (`id`) USING BTREE,
                                     KEY `actable_idx_name` (`name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=271 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='权限';

-- ----------------------------
-- Table structure for system_role
-- ----------------------------
-- 角色表：存储系统角色，初始内置超级管理员角色
DROP TABLE IF EXISTS `system_role`;
CREATE TABLE `system_role` (
                               `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '描述',
                               `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
                               `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
                               `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除（1是 0否）',
                               `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
                               `version` int NOT NULL DEFAULT '0' COMMENT '版本号',
                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP, -- 创建时间，默认当前时间戳
                               PRIMARY KEY (`id`) USING BTREE,
                               KEY `actable_idx_name` (`name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=122 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='角色';

-- ----------------------------
-- Table structure for system_role_permission
-- ----------------------------
-- 角色权限关系表：记录角色与权限的多对多关联
DROP TABLE IF EXISTS `system_role_permission`;
CREATE TABLE `system_role_permission` (
                                          `role_id` int NOT NULL COMMENT '角色ID',
                                          `permission_id` int NOT NULL COMMENT '权限ID',
                                          `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
                                          `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除（1是 0否）',
                                          `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
                                          `version` int NOT NULL DEFAULT '0' COMMENT '版本号',
                                          `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 创建时间，默认当前时间戳
                                          PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2669 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='角色权限关系';

-- ----------------------------
-- Table structure for system_user
-- ----------------------------
-- 用户表：存储账号、密码、所属客户端等信息，初始内置 admin 账号
DROP TABLE IF EXISTS `system_user`;
CREATE TABLE `system_user` (
                               `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '账号',
                               `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
                               `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '图标',
                               `client_id` int NOT NULL COMMENT '客户端ID',
                               `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
                               `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
                               `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除（1是 0否）',
                               `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
                               `version` int NOT NULL DEFAULT '0' COMMENT '版本号',
                               `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 创建时间，默认当前时间戳
                               PRIMARY KEY (`id`) USING BTREE,
                               KEY `actable_idx_username` (`username`) USING BTREE,
                               KEY `actable_idx_name` (`name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=121 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='用户';

-- ----------------------------
-- Table structure for system_user_role
-- ----------------------------
-- 用户角色关系表：记录用户与角色的多对多关联
DROP TABLE IF EXISTS `system_user_role`;
CREATE TABLE `system_user_role` (
                                    `role_id` int NOT NULL COMMENT '角色ID',
                                    `user_id` int NOT NULL COMMENT '用户ID',
                                    `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除（1是 0否）',
                                    `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
                                    `version` int NOT NULL DEFAULT '0' COMMENT '版本号',
                                    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 创建时间，默认当前时间戳
                                    PRIMARY KEY (`id`) USING BTREE,
                                    KEY `actable_idx_role_id` (`role_id`) USING BTREE,
                                    KEY `actable_idx_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=240 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='用户角色关系';

SET FOREIGN_KEY_CHECKS = 1;
