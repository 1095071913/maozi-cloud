-- Sentinel dashboard metric persistence.
-- The database itself is auto-created by the JDBC url parameter `createDatabaseIfNotExist=true`,
-- and this script (spring.sql.init.mode=always) is executed on every startup to create the table
-- and indexes automatically (idempotent DDL).
--
-- The auto-created database inherits the server default collation (utf8mb4_0900_ai_ci on MySQL 8),
-- so normalize the database default charset/collation to the project standard utf8mb4_general_ci.
-- "ALTER DATABASE" without a name applies to the current (default) database of this connection.

ALTER DATABASE CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `sentinel_metric` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `gmt_create`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `app`           VARCHAR(100) NOT NULL COMMENT '应用名称',
  `timestamp`     BIGINT       NOT NULL COMMENT '监控数据时间戳（毫秒）',
  `resource`      VARCHAR(512) NOT NULL COMMENT '资源名称',
  `pass_qps`      BIGINT       NOT NULL DEFAULT 0 COMMENT '通过QPS',
  `success_qps`   BIGINT       NOT NULL DEFAULT 0 COMMENT '成功QPS',
  `block_qps`     BIGINT       NOT NULL DEFAULT 0 COMMENT '拒绝QPS',
  `exception_qps` BIGINT       NOT NULL DEFAULT 0 COMMENT '异常QPS',
  `rt`            DOUBLE       NOT NULL DEFAULT 0 COMMENT '总RT（平均RT*成功QPS）',
  `count`         INT          NOT NULL DEFAULT 0 COMMENT '聚合条数',
  PRIMARY KEY (`id`),
  -- Semantic uniqueness: one aggregated row per app/resource/timestamp, re-pulled data is overwritten by upsert.
  UNIQUE KEY `uk_app_resource_timestamp` (`app`, `resource`, `timestamp`),
  -- Supports the aggregate query "list recently active resources of an app".
  KEY `idx_app_timestamp` (`app`, `timestamp`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'Sentinel 监控指标表';
