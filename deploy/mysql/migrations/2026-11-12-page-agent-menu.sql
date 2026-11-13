-- Existing databases only. Fresh databases use init/01-init.sql instead.
-- Adds the entry using the existing AI parent; rerunning does not duplicate it.
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO menus (menu_id, parent_id, name, path, component, icon, sort, hidden, is_full, type, status)
SELECT 'menu_ai_page_agent', menu_id, '页面生成', '/ai/page-agent', '/ai/pageAgent/index', 'MagicStick', 2, 0, 0, 'C', 1
FROM menus
WHERE path = '/ai' AND type = 'M'
  AND NOT EXISTS (SELECT 1 FROM menus WHERE menu_id = 'menu_ai_page_agent' OR path = '/ai/page-agent');

COMMIT;

SELECT menu_id, parent_id, name, path, component FROM menus WHERE path = '/ai/page-agent';
