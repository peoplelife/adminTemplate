-- 知识库检索评测使用现有 AI 动态菜单，沿用知识库问答的角色授权。
INSERT INTO menus (menu_id, parent_id, name, path, component, icon, sort, hidden, is_full, type, status)
SELECT 'menu_ai_rag_evaluation', ai.menu_id, '知识库检索评测', '/ai/rag-evaluation',
       '/ai/ragEvaluation/index', 'DataAnalysis', 1, 0, 0, 'C', 1
FROM menus ai
WHERE ai.path = '/ai' AND ai.type = 'M'
  AND NOT EXISTS (SELECT 1 FROM menus existing WHERE existing.path = '/ai/rag-evaluation');

INSERT INTO role_menus (role_id, menu_id)
SELECT DISTINCT access.role_id, evaluation.menu_id
FROM role_menus access
JOIN menus chat ON chat.menu_id = access.menu_id AND chat.path = '/ai/intelligentConversation'
JOIN menus evaluation ON evaluation.path = '/ai/rag-evaluation'
WHERE NOT EXISTS (SELECT 1 FROM role_menus existing
                  WHERE existing.role_id = access.role_id AND existing.menu_id = evaluation.menu_id);
