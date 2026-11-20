/*
 Navicat Premium Dump SQL

 Source Server         : 本地环境mysql
 Source Server Type    : MySQL
 Source Server Version : 80029 (8.0.29)
 Source Host           : localhost:3306
 Source Schema         : maozi-cloud-oauth-local-db

 Target Server Type    : MySQL
 Target Server Version : 80029 (8.0.29)
 File Encoding         : 65001

 Date: 14/08/2026 13:59:49
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for oauth2_authorization
-- ----------------------------
-- OAuth2 授权信息表：存储授权码、访问/刷新/OIDC/设备码等凭据的值、签发与过期时间及元数据
DROP TABLE IF EXISTS `oauth2_authorization`;
CREATE TABLE `oauth2_authorization` (
                                        `id` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
                                        `registered_client_id` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
                                        `principal_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL,
                                        `authorization_grant_type` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
                                        `authorized_scopes` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL,
                                        `attributes` blob,
                                        `state` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
                                        `authorization_code_value` blob,
                                        `authorization_code_issued_at` datetime DEFAULT NULL,
                                        `authorization_code_expires_at` datetime DEFAULT NULL,
                                        `authorization_code_metadata` blob,
                                        `access_token_value` blob,
                                        `access_token_issued_at` datetime DEFAULT NULL,
                                        `access_token_expires_at` datetime DEFAULT NULL,
                                        `access_token_metadata` blob,
                                        `access_token_type` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
                                        `access_token_scopes` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL,
                                        `oidc_id_token_value` blob,
                                        `oidc_id_token_issued_at` datetime DEFAULT NULL,
                                        `oidc_id_token_expires_at` datetime DEFAULT NULL,
                                        `oidc_id_token_metadata` blob,
                                        `refresh_token_value` blob,
                                        `refresh_token_issued_at` datetime DEFAULT NULL,
                                        `refresh_token_expires_at` datetime DEFAULT NULL,
                                        `refresh_token_metadata` blob,
                                        `user_code_value` blob,
                                        `user_code_issued_at` datetime DEFAULT NULL,
                                        `user_code_expires_at` datetime DEFAULT NULL,
                                        `user_code_metadata` blob,
                                        `device_code_value` blob,
                                        `device_code_issued_at` datetime DEFAULT NULL,
                                        `device_code_expires_at` datetime DEFAULT NULL,
                                        `device_code_metadata` blob,
                                        PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for oauth2_authorization_consent
-- ----------------------------
-- OAuth2 授权同意表：记录用户（principal）对客户端已同意的授权范围
DROP TABLE IF EXISTS `oauth2_authorization_consent`;
CREATE TABLE `oauth2_authorization_consent` (
                                                `registered_client_id` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
                                                `principal_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL,
                                                `authorities` varchar(1000) COLLATE utf8mb4_general_ci NOT NULL,
                                                PRIMARY KEY (`registered_client_id`,`principal_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for oauth2_registered_client
-- ----------------------------
-- OAuth2 注册客户端表：客户端密钥、认证方式、授权类型、回调地址与令牌设置等配置
DROP TABLE IF EXISTS `oauth2_registered_client`;
CREATE TABLE `oauth2_registered_client` (
                                            `id` bigint NOT NULL AUTO_INCREMENT,
                                            `client_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                                            `client_id_issued_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                            `client_secret` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                                            `client_secret_expires_at` datetime DEFAULT NULL,
                                            `client_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                                            `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                                            `client_authentication_methods` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'client_secret_basic',
                                            `authorization_grant_types` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                                            `redirect_uris` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                                            `post_logout_redirect_uris` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                                            `scopes` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
                                            `client_settings` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                                            `token_settings` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                                            `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除（1是 0否）',
                                            `status` char(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1',
                                            `create_time` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP,
                                            PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=94 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET FOREIGN_KEY_CHECKS = 1;
