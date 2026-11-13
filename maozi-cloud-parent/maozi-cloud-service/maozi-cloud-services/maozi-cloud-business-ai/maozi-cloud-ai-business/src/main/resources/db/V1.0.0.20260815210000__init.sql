-- AI 聊天对话记录表初始化脚本，由 Flyway 在应用启动时按版本号自动执行
DROP TABLE IF EXISTS `ai_chat_conversation_record`;
CREATE TABLE `ai_chat_conversation_record` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                   `user_id` int NOT NULL COMMENT '用户ID',
                                   `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标题',
                                   `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
                                   `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
                                   `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 创建时间，默认当前时间戳
                                   PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='AI聊天对话记录';