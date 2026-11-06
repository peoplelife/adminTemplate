# Java Backend

基于 Java 21、Spring Boot 3.5.6、Maven 和 MyBatis-Plus 的管理后台。沿用 `backend` 的非 AI 接口、MySQL 五张表和前端 JSON 契约，并通过 Python AI 服务提供非流式聊天和基于会话的 SSE 对话接口。

## 项目结构

```text
java-backend/
├── pom.xml
├── src/main/java/com/admin/template/
│   ├── AdminTemplateApplication.java
│   ├── controller/              # HTTP 路由、请求和响应
│   │   └── auth/user/role/menu/file/ai/
│   ├── service/                 # 业务规则及事务
│   │   └── auth/user/role/menu/file/ai/
│   ├── mapper/                  # MyBatis 数据访问
│   ├── entity/                  # 五张表的映射
│   ├── dto/                     # 请求、响应和查询投影
│   ├── config/                  # Security、MVC、MyBatis、上传和 OpenAPI 配置
│   ├── security/                # JWT、权限注解/拦截器和登录限流
│   └── common/
│       ├── response/            # 统一响应
│       ├── exception/           # 业务异常及统一处理
│       └── web/                 # 请求 ID
├── src/main/resources/
│   ├── application.yml          # 默认本机开发配置
│   ├── application-test.yml     # 测试环境配置
│   └── application-prod.yml     # 生产环境配置
└── src/test/                    # HTTP 契约、业务及数据库映射测试
```

调用方向为 `Controller → Service → Mapper → MySQL`。Controller 不直接访问数据库，Entity 不作为接口请求模型。当前业务规模下 Service 使用具体类并通过构造器注入。

## 启动

安装 JDK 21 和 Maven 3.9+，将 `JAVA_HOME` 指向 JDK 21。默认不激活 profile，直接使用 `src/main/resources/application.yml` 中的本机开发配置：

```powershell
cd java-backend
mvn -ntp spring-boot:run
```

默认配置保留标准 Spring Boot 环境变量覆盖，可在 IntelliJ Run Configuration 或启动终端中设置。Java 服务不读取 NestJS 的 `.env`。配置随应用 classpath 加载，不依赖工作目录下的额外 `config` 目录，因此无需为发现配置调整工作目录。MySQL 需要已有 `users`、`roles`、`menus`、`user_roles`、`role_menus` 表；本应用不建表、不执行数据库迁移。

| 配置 | 用途 |
| --- | --- |
| `SERVER_PORT` | 监听端口，默认 `3000` |
| `SPRING_DATASOURCE_URL` | MySQL JDBC 地址 |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | MySQL 账号与密码 |
| `APP_JWT_SECRET` | HS256 密钥，至少 32 个 UTF-8 字节 |
| `APP_USER_DEFAULT_PASSWORD` | 创建用户时省略密码的默认值，默认 `123456` |
| `APP_UPLOAD_DIRECTORY` | 公开文件根目录，默认工作目录下的 `public` |
| `AI_CHAT_CONNECT_TIMEOUT` / `AI_CHAT_READ_TIMEOUT` | AI 服务连接和读取超时，默认 `3s` / `120s` |
| `AI_CHAT_ASYNC_TIMEOUT` | Java 异步请求最长持续时间（包括 RAG SSE），默认 `5m` |
| `AI_RAG_BASE_URL` | Python RAG 服务地址，默认 `http://127.0.0.1:8000` |
| `AI_RAG_SERVICE_TOKEN` | Java 调用 Python RAG 内部接口使用的服务凭证；须与 Python `RAG_SERVICE_TOKEN` 一致 |
| `AI_RAG_CONNECT_TIMEOUT` / `AI_RAG_READ_TIMEOUT` | RAG 服务连接和读取超时，默认 `3s` / `60s` |

Java 与原 Node 服务同时运行时，请为其中一个配置不同的 `SERVER_PORT`。

测试环境使用 `src/main/resources/application-test.yml`，默认使用 H2 的 MySQL 兼容模式，也可通过 `SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD` 和 `SPRING_DATASOURCE_DRIVERCLASSNAME` 覆盖为其他测试数据库。启动测试环境时显式激活 `test` profile；该 profile 使用正常的鉴权规则。

```powershell
mvn -ntp package
java -jar target/admin-template-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=test
```

生产环境使用 `src/main/resources/application-prod.yml`，通过 `SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD` 和 `APP_JWT_SECRET` 提供数据库连接及 JWT 密钥，并显式激活 `prod` profile：

```powershell
mvn -ntp package
java -jar target/admin-template-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

既有 JWT 只有在两端使用相同 HS256 密钥且密钥满足长度要求时才能互通；更换密钥需要重新登录。开发模式未设置密钥时使用仅供本地运行的默认值。

## Apifox 接口文档

启动 Java 服务后，在 Apifox 项目中选择「导入数据 → OpenAPI / Swagger → URL 导入」，填入：

[http://localhost:3000/v3/api-docs](http://localhost:3000/v3/api-docs)

文档包含中文接口分组和名称、请求及响应字段说明、示例和必填约束。代码中的 OpenAPI 文档不再自动声明 Apifox Auth；受保护接口运行时仍要求 `Authorization: Bearer <token>`。导入已有接口时选择更新/覆盖已有接口，检查预览后确认，避免保留旧 Auth 配置。若修改了 `SERVER_PORT`，导入地址也使用对应端口。

[http://localhost:3000/docs](http://localhost:3000/docs) 是浏览器查看文档的 Swagger UI 页面；Apifox 导入使用上面的 OpenAPI JSON 地址。修改代码后需重启 Java 服务，再重新导入，已导入的 Apifox 文档不会自动更新。

## 接口

| 模块 | 路由 |
| --- | --- |
| 根路径 | `GET /` |
| 认证 | `POST /v1/auth/login`、`POST /v1/auth/logout` |
| 当前用户 | `GET /v1/me`、`GET /v1/me/menus` |
| 用户 | `GET/POST /v1/users`、`GET/PATCH/DELETE /v1/users/{id}`、`GET /v1/users/export` |
| 角色 | `GET/POST /v1/roles`、`GET/PATCH/DELETE /v1/roles/{id}` |
| 菜单 | `GET/POST /v1/menus`、`PATCH/DELETE /v1/menus/{id}` |
| 文件 | `POST/DELETE /v1/files`、`POST /v1/files/batch` |
| AI 会话 | `GET/POST /v1/ai/conversations`、`GET/PATCH/DELETE /v1/ai/conversations/{conversationId}`、`GET /v1/ai/conversations/{conversationId}/messages`、`POST /v1/ai/conversations/{conversationId}/messages/stream`（SSE） |
| AI 知识库 | `GET/POST /v1/ai/knowledge-bases`、`GET /v1/ai/knowledge-bases/{knowledgeBaseId}`、`POST /v1/ai/knowledge-bases/{knowledgeBaseId}/retrieval-preview` |
| AI 评测集 | `GET/POST /v1/ai/evaluation-datasets`、`PUT/DELETE /v1/ai/evaluation-datasets/{datasetId}` |
| AI 评测样本 | `GET/POST /v1/ai/evaluation-datasets/{datasetId}/cases`、`PUT/DELETE /v1/ai/evaluation-datasets/{datasetId}/cases/{caseId}` |
| AI 评测报告 | `GET/POST /v1/ai/evaluation-datasets/{datasetId}/runs`、`GET /v1/ai/evaluation-datasets/{datasetId}/runs/{runId}` |
| AI 文档 | `POST /v1/ai/knowledge-bases/{knowledgeBaseId}/documents`（multipart，同步完成索引）、`GET /v1/ai/knowledge-bases/{knowledgeBaseId}/documents`、`GET /v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}`、`GET /v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/content`、`POST /v1/ai/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/reindex`（同步重建） |

`/v1` JSON 接口由 Controller 显式返回统一 `Result<T>`：成功和错误都包含 `code`、`data`、`message`、`success`、`traceId`、`timestamp`、`details`。成功时 `code=0`；业务异常、鉴权和参数校验失败使用 HTTP 200，并返回独立数字业务码，例如密码错误 `110001`、Token 无效 `110002`、账号禁用 `110003`、用户名已存在 `120002`、参数校验失败 `100001`。错误时 `success=false`、`data=null`，字段校验详情使用 `path`、`message`、`type`。完整码表见 [项目规范](docs/java-backend-spec.md)。HTTP 协议错误和服务器/外部服务故障保留真实 HTTP 状态码。保留 `X-Trace-Id`、204 空响应和 Excel 原始附件响应。除登录外，管理接口使用 `Authorization: Bearer <token>`，业务权限沿用现有菜单权限代码。

上传返回的 `/public/uploads/...` 等相对 URL 可公开访问。每文件不超过 10 MiB，每批最多 10 个，沿用图片、PDF、Word、Excel MIME 白名单。文件操作限定在公开文件根目录内。静态文件统一使用 `/public/**`，旧的根路径文件链接需要补上 `/public` 前缀，磁盘文件无需移动。

认证默认开启：仅根页、登录、`/docs/**`、`/swagger-ui/**`、`/v3/api-docs/**` 和 `/public/**` 公开；其他路径必须携带有效 Bearer Token，包括新增业务路由。公开白名单由 `JwtAuthenticationFilter.PUBLIC_ENDPOINTS` 定义，安全链与 JWT 过滤器共用。未登录仍沿用 HTTP 200、业务码 110005 的接口约定；已登录访问不存在的路由返回 404。SSE 异步派发沿用初次请求的认证结果。


新 AI 会话接口使用 `knowledgeBaseId` 区分普通对话和知识库对话；发送消息时传入 `clientRequestId` 和 `content`。会话流额外返回 `sources` 引用事件，`done` 表示回答已保存；刷新页面可通过消息历史接口读取消息和来源快照。接口需要业务 JWT，服务间凭证由 Java 配置并且不由客户端提交。

会话流开始前的业务失败返回 HTTP 200 的 `Result` JSON，例如生成冲突 `160123`、幂等内容冲突 `160125`；开始后通过 SSE `error` 事件返回失败。Python 每约 15 秒发送 `: ping` 注释心跳，单次生成上限为 180 秒；中断/超时保留已生成的部分回答。启动 Java 时必须提供与 Python `RAG_SERVICE_TOKEN` 一致的 `AI_RAG_SERVICE_TOKEN`，包括从 IDE 重新启动时。内部请求使用 HTTP/1.1，以兼容 Uvicorn 并支持 PATCH。

`/v1/knowledge-bases/**` 未迁移。此 Java 工程不直接依赖 PostgreSQL、pgvector、Ollama 或 LangChain。

在线评测沿用 JWT、服务凭证和追踪头，所有者取自已认证身份。创建返回 201、删除返回 204，分页使用 `page / pageSize`。一次同步评测 1～50 道题，可开启答案生成及评审；前端评测请求和 Java RAG 读取默认均为 60 秒，超时后应先刷新运行历史确认是否保存。前端入口为“AI → 知识库检索评测”（`/#/ai/rag-evaluation`），菜单组件路径为 `/ai/ragEvaluation/index`。菜单 SQL 见 `docs/sql/2026-11-05-add-ai-evaluation-menu.sql`；使用前需重启三端并升级 Python Alembic 迁移。

## 测试

```powershell
mvn -ntp clean test
mvn -ntp package
```

Maven 测试通过 `@ActiveProfiles({"test", "unit-test"})` 激活 `test` 配置和测试专用安全链，使用 H2 的 MySQL 兼容模式，不需要外部数据库；项目不提供真实 MySQL 集成测试 profile。

迁移保留原后端的密码存储/比较方式；登录限流为每进程、每 IP 每分钟 10 次。

JWT 验证签名后，每次请求查询当前账户：已禁用返回 110003，账户不存在、已删除或签发时间不晚于 `last_logout` 返回 110002，均沿用 HTTP 200 业务错误契约。退出登录更新 UTC 退出截止时间，使该账户所有旧 Token 失效；重新登录签发截止时间之后的 Token，并处理 JWT 秒精度下的同秒退出/重登。无需数据库变更。已有请求及 SSE 不会被中途终止，账户禁用检查在下一次请求生效。数据库故障保留系统错误，不转换成 Token 无效。

知识库、文档和会话列表使用页码分页：请求 `page`（默认 1）、`pageSize`（默认 20，最大 100），响应 `items / total / page / pageSize`。Java 转发给 Python 时使用 `page / page_size`。消息历史继续使用 `beforeSeq / limit`。
