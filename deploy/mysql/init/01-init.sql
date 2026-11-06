-- MySQL production initialization and verification; 2026-11-06.
-- Fresh empty database only. Read-only verification queries are at the end.

-- Baseline exported read-only from development MySQL; 2026-11-06.
-- Fresh empty database only. Includes the initial admin account.
SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '昵称',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像URL',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `last_login` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_logout` datetime DEFAULT NULL COMMENT '最后退出时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_username` (`username`),
  KEY `idx_email` (`email`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色编码',
  `description` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色描述',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `sort` int DEFAULT '0' COMMENT '排序',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE `menus` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `menu_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '菜单ID',
  `parent_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '父菜单ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '菜单名称',
  `path` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '路由路径',
  `component` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '组件路径',
  `icon` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '菜单图标',
  `sort` int DEFAULT '0' COMMENT '显示顺序',
  `hidden` tinyint(1) DEFAULT '0' COMMENT '是否隐藏（0显示 1隐藏）',
  `is_full` tinyint(1) DEFAULT '0' COMMENT '是否全屏（0否 1是）',
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '菜单类型（M目录 C菜单 B按钮）',
  `buttonPermission` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '按钮权限标识',
  `status` tinyint(1) DEFAULT '1' COMMENT '菜单状态（0停用 1正常）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单权限表';

CREATE TABLE `user_roles` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_user_role` (`user_id`,`role_id`),
  KEY `fk_ur_role_id` (`role_id`),
  CONSTRAINT `fk_ur_role_id` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_ur_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户和角色关联表';

CREATE TABLE `role_menus` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '菜单ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_role_menu` (`role_id`,`menu_id`),
  KEY `fk_rm_menu_id_v2` (`menu_id`),
  CONSTRAINT `fk_rm_menu_id_v2` FOREIGN KEY (`menu_id`) REFERENCES `menus` (`menu_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_rm_role_id` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色和菜单关联表';



-- Initial administrator, roles and menus. No development user data.
-- Seed data for the fresh database.
SET NAMES utf8mb4;
SET time_zone = '+00:00';
START TRANSACTION;

INSERT INTO `roles` (`name`,`code`,`description`,`status`,`sort`) VALUES ('超级管理员', 'super_admin', '系统超级管理员，拥有所有权限', '1', '1');
INSERT INTO `roles` (`name`,`code`,`description`,`status`,`sort`) VALUES ('管理员', 'admin', '系统管理员，拥有大部分权限', '1', '2');
INSERT INTO `roles` (`name`,`code`,`description`,`status`,`sort`) VALUES ('普通用户', 'user', '普通用户，拥有基本权限', '1', '3');

INSERT INTO `menus` (`menu_id`,`parent_id`,`name`,`path`,`component`,`icon`,`sort`,`hidden`,`is_full`,`type`,`buttonPermission`,`status`) VALUES ('1', '-1', '系统管理', '/system', NULL, 'Setting', '1', '0', '0', 'M', NULL, '1');
INSERT INTO `menus` (`menu_id`,`parent_id`,`name`,`path`,`component`,`icon`,`sort`,`hidden`,`is_full`,`type`,`buttonPermission`,`status`) VALUES ('1001', '1', '用户管理', '/system/user', '/system/user/index', 'User', '1', '0', '0', 'C', NULL, '1');
INSERT INTO `menus` (`menu_id`,`parent_id`,`name`,`path`,`component`,`icon`,`sort`,`hidden`,`is_full`,`type`,`buttonPermission`,`status`) VALUES ('1002', '1', '菜单管理', '/system/menu', '/system/menu/index', 'Menu', '2', '0', '0', 'C', NULL, '1');
INSERT INTO `menus` (`menu_id`,`parent_id`,`name`,`path`,`component`,`icon`,`sort`,`hidden`,`is_full`,`type`,`buttonPermission`,`status`) VALUES ('2', '-1', '首页', '/home', '/home/index', 'HomeFilled', '0', '0', '0', 'M', NULL, '1');
INSERT INTO `menus` (`menu_id`,`parent_id`,`name`,`path`,`component`,`icon`,`sort`,`hidden`,`is_full`,`type`,`buttonPermission`,`status`) VALUES ('1003', '1', '角色管理', '/system/role', '/system/role/index', 'Avatar', '3', '0', '0', 'C', '', '1');
INSERT INTO `menus` (`menu_id`,`parent_id`,`name`,`path`,`component`,`icon`,`sort`,`hidden`,`is_full`,`type`,`buttonPermission`,`status`) VALUES ('3', '-1', '系统工具', '/tool', '', 'Box', '2', '0', '0', 'M', '', '1');
INSERT INTO `menus` (`menu_id`,`parent_id`,`name`,`path`,`component`,`icon`,`sort`,`hidden`,`is_full`,`type`,`buttonPermission`,`status`) VALUES ('menu_1765334818268_277', '3', '表单构建', '/tool/build', '/tool/build/index', 'CreditCard', '0', '0', '0', 'C', '', '1');
INSERT INTO `menus` (`menu_id`,`parent_id`,`name`,`path`,`component`,`icon`,`sort`,`hidden`,`is_full`,`type`,`buttonPermission`,`status`) VALUES ('menu_1778841163540_899', '-1', 'AI', '/ai', '', 'Files', '3', '0', '0', 'M', '', '1');
INSERT INTO `menus` (`menu_id`,`parent_id`,`name`,`path`,`component`,`icon`,`sort`,`hidden`,`is_full`,`type`,`buttonPermission`,`status`) VALUES ('menu_1778841682940_869', 'menu_1778841163540_899', '知识库问答', '/ai/intelligentConversation', '/ai/intelligentConversation/index', '', '0', '0', '0', 'C', '', '1');
INSERT INTO `menus` (`menu_id`,`parent_id`,`name`,`path`,`component`,`icon`,`sort`,`hidden`,`is_full`,`type`,`buttonPermission`,`status`) VALUES ('menu_ai_rag_evaluation', 'menu_1778841163540_899', '知识库检索评测', '/ai/rag-evaluation', '/ai/ragEvaluation/index', 'DataAnalysis', '1', '0', '0', 'C', NULL, '1');

INSERT INTO `users` (`username`, `password`, `nickname`, `status`)
VALUES ('admin', 'admin123', '超级管理员', 1);

INSERT INTO `user_roles` (`user_id`, `role_id`)
SELECT u.id, r.id FROM users u CROSS JOIN roles r
WHERE u.username = 'admin' AND r.code = 'super_admin';

COMMIT;


-- READ-ONLY VERIFICATION: only this section may be rerun after restoring a backup.
SELECT VERSION() AS mysql_version, DATABASE() AS current_database,
       @@session.time_zone AS session_time_zone, @@global.time_zone AS global_time_zone,
       @@sql_mode AS sql_mode;

SELECT TABLE_NAME, ENGINE, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('users', 'roles', 'menus', 'user_roles', 'role_menus')
ORDER BY TABLE_NAME;

SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE,
       GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS indexed_columns
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('users', 'roles', 'menus', 'user_roles', 'role_menus')
GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE
ORDER BY TABLE_NAME, INDEX_NAME;

SELECT 'orphan_user_roles' AS check_name, COUNT(*) AS violations
FROM user_roles ur LEFT JOIN users u ON u.id = ur.user_id LEFT JOIN roles r ON r.id = ur.role_id
WHERE u.id IS NULL OR r.id IS NULL
UNION ALL
SELECT 'orphan_role_menus', COUNT(*)
FROM role_menus rm LEFT JOIN roles r ON r.id = rm.role_id LEFT JOIN menus m ON m.menu_id = rm.menu_id
WHERE r.id IS NULL OR m.id IS NULL
UNION ALL
SELECT 'orphan_menu_parents', COUNT(*)
FROM menus m LEFT JOIN menus p ON p.menu_id = m.parent_id
WHERE m.parent_id IS NOT NULL AND m.parent_id NOT IN ('', '0', '-1') AND p.id IS NULL;

SELECT r.code, r.status, COUNT(DISTINCT ur.user_id) AS assigned_users,
       COUNT(DISTINCT rm.menu_id) AS assigned_menus
FROM roles r LEFT JOIN user_roles ur ON ur.role_id = r.id
LEFT JOIN role_menus rm ON rm.role_id = r.id
GROUP BY r.id, r.code, r.status ORDER BY r.id;

SELECT COUNT(DISTINCT u.id) AS active_super_admin_accounts
FROM users u JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id
WHERE u.status = 1 AND u.deleted_at IS NULL AND r.status = 1 AND r.code = 'super_admin';

SELECT menu_id, parent_id, name, path, component
FROM menus WHERE path IN ('/ai', '/ai/intelligentConversation', '/ai/rag-evaluation') ORDER BY sort;
