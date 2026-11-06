# Workspace Rules

- 三个应用保持独立边界：frontend 为页面，java-backend 为管理后台和认证网关，ai-chat 为 AI 能力。不要将服务合成一个进程。
- 修改 Java 先读 `java-backend/AGENTS.md` 和 `java-backend/docs/java-backend-spec.md`；Python 遵循 `ai-chat/AGENTS.md`。
- 跨模块架构、部署、接口说明放根目录 `docs/architecture`、`docs/deployment`、`docs/api`；应用内部规范与设计保留在各自模块。
- Docker 组合、反向代理、数据库初始化与运维脚本放 `deploy/`；应用镜像的 Dockerfile 和 .dockerignore 放各自模块。
- MySQL 初始化源为 `deploy/mysql/init/01-init.sql`；PostgreSQL 迁移源为 `ai-chat/migrations/`。不要复制多份维护。
- 不提交 .env、密钥、原文、上传文件、数据库数据、依赖目录或备份。
- 前端接口通过 `pnpm run build:api` 生成，不手改生成文件。
- 只运行变更涉及的检查，保留用户已有未提交修改。不自动发布、推送或操作生产数据库。
