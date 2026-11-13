-- Existing databases only. Fresh databases use init/01-init.sql.
-- No users are created and existing passwords/roles are not changed.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `user_oauth_accounts` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `user_id` bigint NOT NULL COMMENT '系统用户ID',
  `provider` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '身份提供方',
  `provider_user_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '第三方稳定用户ID',
  `provider_username` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '第三方登录名，仅展示',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_oauth_provider_user` (`provider`, `provider_user_id`),
  UNIQUE KEY `uk_oauth_user_provider` (`user_id`, `provider`),
  CONSTRAINT `fk_oauth_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='第三方账号绑定';
