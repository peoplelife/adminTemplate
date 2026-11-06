# 系统架构

```text
浏览器 → Vue 前端 → Java 管理后台 → Python AI 服务
                       ↓                ↓
                     MySQL       PostgreSQL / pgvector
                                        ↓
                                 Embedding、Rerank、LLM
```

Java 验证用户身份和权限，以服务凭证和用户 ID 调用 Python。Python 对知识库、会话和评测执行用户隔离；私有原文由 Python 管理。Redis 按服务实际用途配置，不作为另一套业务数据库。

三端独立构建和启动。目录合并不改变 HTTP 契约、数据库职责、Prompt 管理或迁移机制。
