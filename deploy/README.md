# 部署资源

本目录负责跨模块组合运行；应用代码、构建配置和测试仍留在三个应用内。

已具备：`mysql/init/01-init.sql`，仅用于全新空库初始化；说明见 [MySQL 准备](../docs/deployment/mysql.md)。

已生成 `compose.yml`、`nginx/default.conf`、`.env.example` 和 PostgreSQL 首次初始化脚本。应用 Dockerfile 和 .dockerignore 放各应用根目录，Compose 构建上下文分别为 `../frontend`、`../java-backend`、`../ai-chat`。

生产私有配置使用本目录的 `.env`，不得提交。数据库、公开上传及私有原文使用独立持久化卷，不放源代码目录；私有原文不挂载给 Nginx。服务间使用 java、python、mysql、postgres、redis 等服务名通信。

使用步骤和验证边界见 [Docker 部署](../docs/deployment/docker.md)。先填写 `.env` 的模型 API Key 并检查其他参数，再执行 Compose；本机没有 Docker Engine，尚未完成实际容器构建和启动验收。不包含 HTTPS、CI/CD 或备份脚本。
