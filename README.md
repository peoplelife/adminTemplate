# Admin Template

Vue 管理前端、Java 管理后台和 Python AI 服务组成的统一项目。根目录统一管理 Git，三个应用独立管理依赖、构建和测试。

```text
adminTemplate/
├─ frontend/          # Vue、OpenAPI 生成的接口
├─ java-backend/      # Java 21、认证权限、MySQL、AI 网关
├─ ai-chat/           # Python、FastAPI、模型、PostgreSQL、Alembic
├─ deploy/            # 跨模块部署资源
│  └─ mysql/init/01-init.sql
└─ docs/
   ├─ architecture/   # 跨模块架构
   ├─ deployment/     # 部署说明
   └─ api/            # 跨模块接口约定
```

## 本地运行

各命令在独立终端执行，先启动数据库等依赖。生产密码和模型密钥不放 Git。

Python（工作目录 `ai-chat`）：

```powershell
uv sync --locked
uv run alembic upgrade head
uv run uvicorn app.main:app --reload
```

Java（工作目录 `java-backend`；JDK 21）：

```powershell
mvn -ntp spring-boot:run
```

前端（工作目录 `frontend`）：

```powershell
pnpm install --frozen-lockfile
pnpm dev
```

更多配置见 [Python README](ai-chat/README.md)、[Java README](java-backend/README.md)。

## 文档与部署

- [系统关系](docs/architecture/README.md)
- [接口约定](docs/api/README.md)
- [部署状态](docs/deployment/README.md)
- [MySQL 投产准备](docs/deployment/mysql.md)
- [部署资源](deploy/README.md)

Dockerfile、生产 Compose、HTTP Nginx 和生产配置模板已生成，使用步骤见 [Docker 部署](docs/deployment/docker.md)。实际容器构建及服务器验收、HTTPS 和备份脚本尚未完成。

## 2026-11-06 目录迁移

Python 已从 `D:/myselfProjectPython/ai-chat` 移入本项目，IDE、终端和启动配置需要使用新目录。本地 `.env` 的私有原文目录已指向新位置，原文文件随模块保留。虚拟环境启动器可能保留旧绝对路径，优先使用 `uv run`，需要时重建 `.venv`。

旧 NestJS 项目及旧 Compose 保存在 `D:/myselfProject/legacy-admin-backend/`，不再属于当前项目。

根仓库使用 `matser` 主分支，远端为 `git@github.com:peoplelife/adminTemplate.git`。三个旧远端仓库保留历史；新仓库从当前代码开始记录，不导入旧提交。模块原有 `.git` 备份在项目外 `D:/myselfProject/adminTemplate-git-before-unify-20261106/`。
