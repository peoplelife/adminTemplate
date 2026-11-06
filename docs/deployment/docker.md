# 单机 Docker 部署

目标为 Ubuntu 22、4 核 4 GB 服务器，公网入口为 `http://189.24.64.102/`，沿用宝塔 Nginx 的 80 端口。需要 Docker Engine 和 Docker Compose v2（建议 2.30 以上）。这里只开放 HTTP 端口，不配置 HTTPS 或 CI/CD。

## 配置分别放在哪里

| 位置 | 职责 | 提交 Git |
| --- | --- | --- |
| `deploy/.env` | 服务器实际密码、密钥、模型和开关 | 否 |
| `deploy/.env.example` | 可填写的配置项模板 | 是 |
| `deploy/compose.yml` | 容器、内网地址、环境变量映射、数据卷、启动依赖 | 是 |
| `deploy/nginx/default.conf` | 容器内静态页面、反向代理、上传大小、SSE 超时 | 是 |
| `deploy/nginx/baota-proxy.conf.example` | 宿主机宝塔站点的代理参考，不挂入容器 | 是 |
| 三个模块的 `Dockerfile`、`.dockerignore` | 镜像构建及排除本地配置 | 是 |
| Java `application.yml`、`application-prod.yml` | 配置定义，生产值由容器环境变量覆盖 | 是 |
| Python `app/core/config.py` | 公共 Settings，生产值由容器环境变量覆盖 | 是 |

不要将开发机的 `.env` 放入镜像，也不要把 Windows 本机路径填写到生产容器配置中。Compose 的 `.env` 用于替换变量；各服务通过 `environment` 明确接收所需配置，不会收到全部密钥。

前端 Docker 构建固定 `VITE_BASE_API=/api`、`VITE_API_URL=/api`、关闭 Mock。Vite 配置在构建时生效；运行 Nginx 容器时设置 `VITE_*` 不会重新修改静态文件。浏览器请求 `/api/v1/...`，Nginx 去掉 `/api` 后转给 Java 的 `/v1/...`；`/api/public/...` 转到 Java `/public/...`。页面不绑定公网 IP，未来切换域名无需更改这两个前端路径。

Java 激活 `prod`。Python 设置 `APP_ENV=production`、`DEBUG=false`；同一 `DATABASE_URL` 供应用及 Alembic 使用。`RAG_SERVICE_TOKEN` 同时映射给 Java 和 Python，不要另外填写两套凭证。当前生成器仅接入 DeepSeek，`DEFAULT_MODEL` 应选择其可用模型，不要仅改成 Qwen/OpenAI 模型名。

## 一条命令部署

首次部署和后续更新均可在服务器运行：

```sh
bash /opt/apps/adminTemplate/deploy/deploy.sh
```

脚本自动定位 deploy 目录，检查 Docker、`.env` 和 Compose 配置，然后停止前端/Java/Python、逐个构建镜像、等待数据库健康、重新执行 Alembic 迁移，最后启动应用并等待健康检查。适用于当前 4 GB 服务器，更新时会有维护停机，构建阶段失败也会保持应用停止。

任一步失败会返回非零状态并打印容器状态，不继续后面的步骤，不删除数据卷，不自动回滚数据库。修复错误后可以重新运行，构建会复用缓存。备份、拉取代码、上传 `.env`、配置宝塔和业务验收仍需单独完成；脚本不修改 DNS、Swap 或宝塔。首次使用前填写生产配置，更新前先备份并执行 `git pull --ff-only`。

## 首次部署

在服务器克隆仓库并进入部署目录：

```sh
git clone git@github.com:peoplelife/adminTemplate.git
cd adminTemplate/deploy
cp .env.example .env
chmod 600 .env
```

编辑 `.env`：

- 数据库和 Redis 密码分别用 `openssl rand -hex 32` 生成。PostgreSQL/Redis 密码会组成 URL，当前配置要求使用十六进制值，避免 URL 转义问题。
- JWT 密钥至少 32 个 UTF-8 字节；内部服务凭证也使用独立随机值。
- 初始管理员由 MySQL SQL 固定创建：用户名 `admin`、密码 `admin123`，绑定 `super_admin`；无需填写管理员密码环境变量。
- 填写 `DEEPSEEK_API_KEY` 和 `RAG_EMBEDDING_API_KEY`。开启重排时填写 `RAG_RERANK_API_KEY`；不用重排则设 `RAG_RERANK_ENABLED=false`。
- 默认 Embedding 为 `BAAI/bge-m3`，必须与现有知识库的模型、维度一致。切换模型不能代替重建索引。

本次本地生成的 `deploy/.env` 已随机生成内部密码和凭证，且已复制现有模型 API Key；该文件不在 Git 中，服务器克隆后仍需自行创建。不要把校验输出中的完整环境配置发到公开渠道。

所有后续命令在 `deploy/` 中执行：

```sh
docker compose --env-file .env config --quiet
docker compose build frontend
docker compose build java
docker compose build python
docker compose up -d
docker compose ps -a
docker compose logs --tail=100 migrate java python
```

启动链为 PostgreSQL 就绪 → `migrate` 成功 → Python 就绪 → Java 就绪 → 前端；Java 同时等待 MySQL 初始化完成，Python 同时等待 Redis。`migrate` 正常退出码应为 0；`up -d` 返回不代表所有服务已经就绪，要检查健康状态和日志。

MySQL 仅在空数据卷首次启动时执行 `mysql/init/01-init.sql`，包含表、角色、菜单及初始管理员 `admin` / `admin123`，同时绑定 `super_admin`。PostgreSQL 首次初始化执行 `postgres/init/01-init.sh`，创建 pgvector 和普通应用账号；全部业务表由 `alembic upgrade head` 在 `public` 中创建。初始化目录不是增量升级工具，不会在已有数据卷上重复执行。

使用初始管理员登录，按 [MySQL 准备说明](mysql.md) 验收角色权限、上传、问答和评测。不要把“容器已启动”视为“业务已可用”。本次不修改密码存储逻辑。

## 网络与数据

仅 `frontend` 绑定 `127.0.0.1:${HTTP_PORT:-8080}:80`，不占用宝塔的 80 端口，也不公开 8080。浏览器 → 宝塔 Nginx 的 80 → 本机 8080 → 容器 Nginx → Java → Python。只需放行已有 HTTP 和 SSH 入口；Java、Python、MySQL、PostgreSQL、Redis 不映射公网端口。Compose 内服务名用于通信，不能填写服务器公网 IP 或 `127.0.0.1` 代替其他容器。

| 数据卷 | 容器内路径 | 内容 |
| --- | --- | --- |
| `mysql-data` | `/var/lib/mysql` | 管理后台数据库 |
| `postgres-data` | `/var/lib/postgresql/data` | RAG 和评测数据库 |
| `redis-data` | `/data` | Redis AOF |
| `java-uploads` | `/data/uploads` | Java 公开上传文件 |
| `rag-documents` | `/data/rag-documents` | Python 私有原文 |

Compose 项目名固定为 `admin-template`，实际卷名带此前缀。私有原文卷只挂给 Python，不挂给 Nginx。重建容器不会自动导入开发机的数据；已有数据库、原文和公开上传文件需另行迁移并保持对应关系。

日志输出到容器标准输出，每个容器按 10 MiB × 3 份轮转。Dockerfile 中健康检查只验证服务响应，不能代替模型调用或业务验收。Python 保持单 worker；各容器已设置适合当前服务器的起始内存上限，见下表。

## 宝塔入口和内存配置

在宝塔修改对应 IP 站点，参考 `deploy/nginx/baota-proxy.conf.example` 将整个站点代理到 `http://127.0.0.1:8080`。不要再新建同名站点或替换全局 nginx.conf；先备份站点配置，并通过宝塔的配置检测再重载。清理会截获请求的旧 location/静态文件缓存规则，保证 `/api/` 和前端资源都进入代理。外层宝塔和内层容器都需关闭代理响应缓冲并保持 300 秒读超时，否则 SSE 可能积攒后一次返回。

先在宿主机验证 `curl -I http://127.0.0.1:8080/`，再验证公网入口。当前 HTTP_PORT 为 8080；如果修改，同时更新宝塔代理目标。宿主机原有 MySQL、PostgreSQL、Redis 与容器内实例独立，不要删除或停止它们来释放端口。

| 容器 | 总内存上限 | 内部限制 |
| --- | --- | --- |
| Java | 768 MiB | 堆 128～384 MiB、直接内存最多 64 MiB；连接池最多 5、最少空闲 1 |
| Python | 640 MiB | 单 worker，PostgreSQL 连接池 3 + 临时连接 2 |
| MySQL | 640 MiB | Buffer Pool 128 MiB，最多 40 个连接 |
| PostgreSQL | 384 MiB | shared_buffers 96 MiB、work_mem 2 MiB、maintenance_work_mem 32 MiB，最多 30 个连接 |
| Redis | 128 MiB | 数据最多 64 MiB，noeviction，满后拒绝新增写入而非自动驱逐现有键 |
| 前端 Nginx | 96 MiB | 静态页面和代理 |
| Alembic migrate | 256 MiB | 一次性任务，迁移后退出 |

长期运行上限合计 2656 MiB（约 2.59 GiB），不是预分配内存，也不是实测容量保证。Java 上限包含堆外及线程开销；数据库每连接内存和 Redis AOF 也有额外开销。Ubuntu、宝塔及原有数据库仍会消耗内存，超限可能导致容器 OOM。建议宿主机配置约 4 GiB Swap 缓冲短时高峰，并观察 `free -h`、`docker stats` 和 `docker compose ps -a`，再根据真实并发和文档大小调整。

构建不受 Compose 的运行内存上限控制。按上述三个 build 命令逐个构建；前端构建的 Node 堆上限为 768 MiB，Maven 堆上限为 512 MiB，均不等于构建进程总内存限制。在小内存服务器更新时，可在备份后的维护窗口先停止前端、Java、Python再构建，减少旧服务与构建进程争抢内存；构建失败不启动新版本。

## 更新与停止

更新前备份 MySQL、PostgreSQL、公开上传和私有原文。修改 `.env` 中初始化数据库密码不会自动修改已有库账号密码，需先在数据库内完成对应变更。

有数据库迁移的更新，在维护窗口执行：

```sh
git pull --ff-only
docker compose build frontend
docker compose build java
docker compose build python
docker compose stop frontend java python
docker compose up -d mysql postgres redis
docker compose run --rm migrate
# 上一步成功后才继续；失败时保持应用停止并检查日志。
docker compose up -d --force-recreate
docker compose ps -a
```

这里显式停止应用后迁移，避免旧实例与新表结构同时工作。不要直接依赖反复重启应用来处理迁移失败。应用代码回退不代表数据库可直接回退，数据库回退需要单独确认迁移及恢复方案。

```sh
docker compose down
```

普通 `down` 保留数据卷。不要执行 `down -v`，该选项会删除项目数据卷。备份和恢复脚本、HTTPS 配置及服务器全流程验收尚未完成。

## 当前验证边界

配置生成后需要校验 Compose 的变量展开、依赖、挂载、路由和镜像构建。开发机没有 Docker Engine，实际镜像构建、Nginx 启动、空库初始化及服务器连通验收仍需在具备 Docker 的环境执行。
