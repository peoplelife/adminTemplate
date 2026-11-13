# 部署资源

本目录负责跨模块组合运行；应用代码、构建配置和测试仍留在三个应用内。

已具备：`mysql/init/01-init.sql`，仅用于全新空库初始化；说明见 [MySQL 准备](../docs/deployment/mysql.md)。

已生成 `compose.yml`、`nginx/default.conf`、`.env.example` 和 PostgreSQL 首次初始化脚本。应用 Dockerfile 和 .dockerignore 放各应用根目录，Compose 构建上下文分别为 `../frontend`、`../java-backend`、`../ai-chat`。

生产私有配置使用本目录的 `.env`，不得提交。数据库、公开上传及私有原文使用独立持久化卷，不放源代码目录；私有原文不挂载给 Nginx。服务间使用 java、python、mysql、postgres、redis 等服务名通信。

使用步骤和验证边界见 [Docker 部署](../docs/deployment/docker.md)。正式部署由根目录 `.github/workflows/ci-cd.yml` 完成：工作流构建并推送 GHCR 镜像，再通过 `deploy/release.sh` 在服务器更新。发布时先按版本执行页面生成菜单、GitHub 绑定表、公开注册的 MySQL 增量 SQL 和 PostgreSQL Alembic，再启动新版应用；MySQL 成功版本记入 `schema_migrations`，重复发布跳过，不会重新初始化数据库或恢复已撤销的菜单授权。`deploy/.env` 只保留在服务器，Compose 和初始化脚本仍由发布流程使用。不包含 HTTPS 或备份脚本。
