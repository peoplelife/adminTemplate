-- Existing databases: passwordless GitHub users and ordinary-user defaults.
-- release.sh records this version once so later administrator revocations stay intact.
SET NAMES utf8mb4;

ALTER TABLE `users` MODIFY COLUMN `password` varchar(100) NULL COMMENT '密码';

START TRANSACTION;

-- Public file writes are a management capability, never a signup default.
INSERT INTO menus (menu_id, parent_id, name, sort, hidden, is_full, type, buttonPermission, status)
SELECT 'menu_system_file_manage', m.menu_id, '公开文件管理', 4, 0, 0, 'B', 'system:file:manage', 1
FROM menus m
WHERE m.path = '/system/user' AND m.type = 'C'
  AND NOT EXISTS (SELECT 1 FROM menus WHERE menu_id = 'menu_system_file_manage' OR buttonPermission = 'system:file:manage')
LIMIT 1;

INSERT INTO roles (name, code, description, status, sort)
SELECT '普通用户', 'user', '普通用户，拥有基本权限', 1, 3
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'user');

INSERT INTO role_menus (role_id, menu_id)
SELECT r.id, m.menu_id
FROM roles r CROSS JOIN menus m
WHERE r.code = 'user' AND r.status = 1 AND m.status = 1
  AND m.type IN ('M', 'C')
  AND m.path IN ('/home', '/ai', '/ai/intelligentConversation', '/ai/page-agent')
  AND NOT EXISTS (SELECT 1 FROM role_menus rm WHERE rm.role_id = r.id AND rm.menu_id = m.menu_id);

COMMIT;
