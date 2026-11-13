# GitHub 登录实施计划

目标：任何 GitHub 用户首次授权即可创建普通用户并登录；已有绑定继续进入原系统账号，沿用 JWT、账号状态、退出撤销和角色菜单权限。

已确认范围：登录页增加 GitHub 登录；头像菜单增加账号绑定弹窗；未绑定的 GitHub 自动创建普通用户，不通过用户名或邮箱合并已有账号；关联表只保存平台用户 ID 和登录名，不保存 GitHub access token。Python 不改动。

## 公开注册扩展（用户已确认）

- [x] Java：首次授权事务内创建独立随机用户名、GitHub 昵称、无密码用户，分配现有 `user` 角色并保存绑定；重复及并发授权复用同一身份，失败回滚用户/角色关系/绑定。已有绑定的禁用、删除、无角色/菜单检查继续生效，不重新注册绕过限制。
- [x] 密码与解绑：`users.password` 允许 NULL；账号密码登录显式拒绝无密码账号，不能将 NULL 当成字符串密码；第三方账号只有 GitHub 登录方式时拒绝解绑。已有密码账号仍可正常解绑，不新增设置密码页面。
- [x] 默认权限：复用 `user` 角色，首次升级默认授予首页、AI 父菜单、知识库问答、页面生成；不授予系统管理或检索评测。管理员以后可调整角色菜单。
- [x] 公开文件边界：通用文件上传/批量上传/删除统一要求 `system:file:manage`，不授予注册默认角色；超级管理员与显式授权管理角色可用。知识库私有文档上传保持现有隔离，检索评测只控制默认菜单。
- [x] 数据库与发布：同步空库和增量 SQL；发布脚本记录 MySQL 脚本执行版本，成功后登记、失败停止，本期 SQL 可重试，已执行版本跳过，避免每次发布恢复已撤销的授权。显式脚本名单继续保留。
- [x] 前端与接口：登录页说明首次授权自动注册；第三方账号解绑错误使用后端 message；若 OpenAPI 文案变化，通过 `pnpm run build:api` 更新生成接口。
- [x] 验证：先补首次注册、重复/并发、事务回滚、同名不合并、无密码拒登、禁止唯一方式解绑及普通用户越权测试；运行 JDK21 全量测试、前端测试/类型/build、发布脚本成功/失败/重复升级测试及独立评审。

实现拆分：Java 的 OAuth 状态校验保留在 `GitHubOAuthService`，注册的数据库事务使用 auth 目录内独立注册服务；SQL 与版本执行放 `deploy/mysql`、`deploy/release.sh`；前端复用现有登录和账号绑定组件。本次不新增依赖、不迁移旧账号密码、不操作生产数据库；用户已要求提交并合并到本地 `matser`，远端发布另行安排。

## 接口与授权

- `GET /v1/auth/github/config`：是否启用。
- `POST /v1/auth/github/authorize`：开始登录；返回 authorizationUrl、state。
- `POST /v1/me/github/authorize`：当前用户开始绑定。
- `GET /v1/auth/github/callback`：GitHub 回调，校验一次性 state、五分钟有效期，使用服务端 PKCE verifier 换取身份；结果临时存入 HttpSession，跳转固定前端 `/#/oauth/github`。
- `POST /v1/auth/github/complete`：使用前端 sessionStorage 保留的 state 消费登录结果，通过 JSON 返回现有 LoginResponse。
- `POST /v1/me/github/complete`：同时验证当前 JWT 用户等于发起绑定的用户，完成绑定。
- `GET /v1/me/github`、`DELETE /v1/me/github`：查看、解除当前用户的绑定。

HttpSession 只保存五分钟的授权事务和待消费结果；Cookie HttpOnly、SameSite=Lax，HTTPS 回调时启用 Secure。不把 JWT 放入地址栏。GitHub 仅请求公开身份，不请求仓库、组织或邮箱权限。固定回调和前端地址来自后端配置，不接收客户端 redirect 参数。网络错误不记录授权码、密钥、access token 或原始响应。

## 模块和验证

- [x] Java：`config/GitHubOAuthProperties`、`service/auth/GitHubOAuthClient`、`GitHubOAuthService`、`controller/auth/GitHubOAuthController`、`dto/auth/GitHubOAuthDtos`；复用 AuthService 签发逻辑。
- [x] MySQL：`user_oauth_accounts`，唯一约束 `(provider, provider_user_id)` 和 `(user_id, provider)`；同步 fresh init 与 existing database 增量 SQL。使用 H2 验证 Mapper 和唯一约束。
- [x] 先写失败测试：授权 URL/PKCE、回调 state/过期/重放、已有绑定登录、绑定冲突、绑定用户不匹配、禁用/无角色账号、外部错误脱敏。
- [x] 前端：公共登录后初始化入口；登录按钮、账号绑定弹窗、公开回调页；回调只消费一次，清理 sessionStorage，错误使用后端 message。
- [x] 使用测试导出的完整 Java OpenAPI 执行 `pnpm run build:api`；不手写生成文件。
- [x] 部署：Compose 传递 `APP_AUTH_GITHUB_*`，更新 `.env.example` 和配置手册。功能默认关闭，缺少密钥不能启动授权。
- [x] JDK21 全量测试、前端测试/类型检查/build、diff/编码检查、合并前评审。

最终验证结果：JDK21 `mvn -ntp test` 221 项通过；使用最新完整 OpenAPI 执行 `pnpm run build:api`，前端 Node 测试 52 项、`vue-tsc --noEmit`、`pnpm build` 通过；发布脚本 4 项成功/重复/失败/重试检查通过。Compose 与 CI YAML、五项环境变量、SQL 初始化/增量权限和 diff/编码已检查。独立评审确认公开文件写入的普通用户权限边界已补齐；H2 测试验证迁移可重复且不授予普通用户管理权限。评审同时修复了代理上游失败时 error log 可能记录授权码的问题；容器配置和宝塔精确回调 location 示例均排除原始 access/error log。前端构建保留已有依赖 eval 和大 chunk 提示，不影响构建成功。

本期一并补齐生产页面生成菜单：复用已有 `2026-11-12-page-agent-menu.sql`，由 `release.sh` 在数据库健康后先按版本执行菜单、GitHub 绑定表、公开注册 SQL，再执行 Alembic 并启动新版应用。发布脚本已覆盖首次执行、版本跳过、失败中止和失败重试，CI 新增部署脚本检查并作为镜像构建前置条件；不自动扫描执行未来 SQL。

上线验收待办：配置服务器环境变量；部署时由 `release.sh` 按版本自动应用本期三份 MySQL 增量 SQL；部署新镜像并调整宝塔回调 location；在真实浏览器验收新账号注册、重复登录、默认菜单、禁用/权限和密码账号绑定/解绑。真实 GitHub、MySQL 和 Nginx 运行效果未在本次自动化测试中验证。

现有账号密码的明文兼容是已有债务，本功能不新增密码、不复制该校验。功能开发于 `feature/github-login`，按用户安排合并到本地 `matser`。真实 GitHub 往返需用户创建 OAuth App 并填写配置后验收。
