-- ============================================================
-- Seata AT 模式 undo_log 表初始化脚本
-- ------------------------------------------------------------
-- 用途：Seata AT 模式在分支事务本地数据源中需依赖 undo_log 表
--       记录修改前后的回滚影像，用于全局事务回滚时逆向恢复数据。
-- 执行方式：由 Flyway 在应用启动时按版本号自动执行；
--           库中每个参与 AT 模式的业务库均需独立存在该表。
-- ============================================================
CREATE TABLE `undo_log` (
                            `id` bigint(20) NOT NULL AUTO_INCREMENT, -- 自增主键
                            `branch_id` bigint(20) NOT NULL, -- 分支事务 ID
                            `xid` varchar(100) NOT NULL, -- 全局事务 ID
                            `context` varchar(128) NOT NULL, -- 序列化方式等上下文
                            `rollback_info` longblob NOT NULL, -- 回滚数据（数据修改前后镜像）
                            `log_status` int(11) NOT NULL, -- 状态（0 正常，1 防御）
                            `log_created` datetime NOT NULL, -- 创建时间
                            `log_modified` datetime NOT NULL, -- 修改时间
                            `ext` varchar(100) DEFAULT NULL, -- 预留扩展字段
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;