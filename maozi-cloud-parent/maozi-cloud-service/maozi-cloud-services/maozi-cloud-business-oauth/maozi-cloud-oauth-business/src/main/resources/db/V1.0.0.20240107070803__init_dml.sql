-- 初始化数据（DML）：数据脚本独立于表结构，整体包裹在事务中，任一语句失败全部回滚
START TRANSACTION;


-- ----------------------------
-- Records of oauth2_registered_client
-- ----------------------------
-- 预置后台系统客户端 system，密码 bcrypt 加密存储
INSERT INTO `oauth2_registered_client` (`id`, `client_id`, `client_id_issued_at`, `client_secret`, `client_secret_expires_at`, `client_name`, `remark`, `client_authentication_methods`, `authorization_grant_types`, `redirect_uris`, `post_logout_redirect_uris`, `scopes`, `client_settings`, `token_settings`, `is_deleted`, `status`, `create_time`) VALUES (1, 'system', '2026-03-06 16:28:46', '{bcrypt}$2a$10$N94nFJjiGDJDC2D2lkj8nOWWHrxyl8KhtsnmvkW2vH0z0EGagM6Ne', NULL, '后台系统', '', 'client_secret_basic', 'refresh_token,password,client_credentials', '', '', '', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"settings.client.require-proof-key\":false,\"settings.client.require-authorization-consent\":true}', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"settings.token.reuse-refresh-tokens\":false,\"settings.token.id-token-signature-algorithm\":[\"org.springframework.security.oauth2.jose.jws.SignatureAlgorithm\",\"RS256\"],\"settings.token.access-token-time-to-live\":[\"java.time.Duration\",7200.000000000],\"settings.token.access-token-format\":{\"@class\":\"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat\",\"value\":\"reference\"},\"settings.token.refresh-token-time-to-live\":[\"java.time.Duration\",604800.000000000],\"settings.token.authorization-code-time-to-live\":[\"java.time.Duration\",300.000000000],\"settings.token.device-code-time-to-live\":[\"java.time.Duration\",300.000000000]}', '0', '1', '2026-08-13 08:55:28');

COMMIT;
