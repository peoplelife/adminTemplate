# GitHub 登录配置

功能默认关闭。启用后任何 GitHub 用户都可以在登录页授权：已有绑定进入原账号，未绑定的 GitHub 身份自动创建普通用户并登录。注册使用 GitHub 稳定用户 ID，不根据相同用户名或邮箱合并账号。系统管理员仍可禁用账号或调整普通用户角色权限。

## 1. 创建 GitHub OAuth App

使用你自己的 GitHub 账号打开 [OAuth Apps](https://github.com/settings/developers)，选择 **New OAuth App**。这是登录授权应用，不是 GitHub Actions Secret、GHCR Token 或仓库 Deploy Key。

| GitHub 字段 | 生产示例 |
| --- | --- |
| Application name | adminTemplate |
| Homepage URL | `https://admin.example.com/` |
| Authorization callback URL | `https://admin.example.com/api/v1/auth/github/callback` |

创建后复制 **Client ID**，点击 **Generate a new client secret** 保存 **Client Secret**。只需要公开用户身份，不需要开通仓库、组织或邮箱权限，也不需要 Webhook 或 Device Flow。

将示例域名替换为你浏览器实际访问系统的地址。当前服务器用 IP 验证时，可以使用 `http://189.24.64.102/` 和 `http://189.24.64.102/api/v1/auth/github/callback`；正式使用建议先在宝塔为网站配置 HTTPS。回调地址必须与应用配置一致。

本地和生产建议各建一个 OAuth App，分别使用自己的 Client ID、Secret 和回调地址。完整 OAuth 往返期间，浏览器必须保持同一站点地址，不能在 localhost、127.0.0.1、域名、IP 之间切换。

## 2. 服务器 Docker 配置

在服务器 `/opt/apps/adminTemplate/deploy/.env` 增加以下配置。Client Secret 仅放服务器，不放前端、Git 仓库或对话消息中。

```dotenv
APP_AUTH_GITHUB_ENABLED=true
APP_AUTH_GITHUB_CLIENT_ID=填入ClientID
APP_AUTH_GITHUB_CLIENT_SECRET=填入ClientSecret
APP_AUTH_GITHUB_REDIRECT_URI=https://admin.example.com/api/v1/auth/github/callback
APP_AUTH_GITHUB_FRONTEND_REDIRECT_URI=https://admin.example.com/#/oauth/github
```

前四项描述 GitHub 授权入口和后端回调；最后一项是 Java 处理回调后返回的前端页面，不填写到 GitHub 的 Authorization callback URL。服务器需能通过 HTTPS 访问 `github.com` 和 `api.github.com`，无需对外开放 Java 或数据库端口。

现有双层 Nginx 路由继续使用 `/api/` 转发，Hash 回调页 `/#/oauth/github` 由前端路由处理。容器 Nginx 已在精确回调 location 关闭 access log 和原始 error log，因为上游失败日志也会包含带授权码的请求 URL。宝塔宿主机 Nginx 也需按 `deploy/nginx/baota-proxy.conf.example` 对该 location 设置 `access_log off;`、`error_log /dev/null;`，保留其余站点配置。回调问题通过浏览器 HTTP 状态、完成接口的 trace ID 和 Java 脱敏异常日志排查。

OAuth 临时 HttpSession Cookie 使用 HttpOnly、SameSite=Lax；HTTPS 回调会启用 Secure。浏览器需允许本站 Cookie 和 sessionStorage。后端回调和前端返回地址必须使用相同协议、主机与端口；配置校验会拒绝跨站组合。

## 3. 数据库随发布升级

新数据库的 `deploy/mysql/init/01-init.sql` 已包含 `user_oauth_accounts`。现有 CI/CD 的 `release.sh` 会在新版应用启动前依次执行页面生成菜单、GitHub 绑定表和公开注册三份增量 SQL；已有库不需要手工补表。注册升级允许 `users.password` 为 NULL，复用或创建普通用户角色 `user`，一次性授予首页、AI 目录、知识库问答和页面生成菜单，不授予系统管理或检索评测。原有密码和用户角色关系不变。升级通过 `schema_migrations` 记录，成功版本后续跳过，管理员之后撤销的菜单授权不会在每次部署时恢复。失败版本不登记，修复后可重试。

正常 CI/CD 发布会自动应用，无需手工执行初始化或增量 SQL。MySQL 升级失败会中止发布，开发期间未直接连接生产数据库。功能关闭时普通账号密码登录保持可用。

## 4. 部署与使用

先发布包含本功能的新前后端镜像，再让 Java 容器读取新的环境变量。只修改 `.env` 而镜像没有更新，不会出现新功能。现有 CI/CD 使用 `IMAGE_TAG` 选择 GHCR 镜像，并重新创建容器、读取配置。已经运行新镜像、只调整配置时，重新运行最近成功版本的部署任务，或在部署目录使用该版本的 `IMAGE_TAG` 执行 `release.sh`。

1. 新用户在登录页选择 GitHub 登录，授权后自动进入新建的普通用户账号。
2. 再次登录进入同一账号；默认可见首页、知识库问答和页面生成。新用户继承 `user` 角色当前的全部权限；本期只添加这些默认菜单，不添加系统管理权限，也不移除管理员之前授予的额外权限。管理员可在角色管理中调整 `user` 角色授权；已有普通用户也共享这个角色的菜单配置。
3. 已有系统账号若希望保留原数据和角色，应先用账号密码登录，再从头像 → 账号绑定 → 绑定 GitHub；不要先用未绑定的 GitHub 登录另建账号。
4. 有密码的账号可解绑；无密码的 GitHub 新账号只有这一种登录方式，系统会拒绝解绑以防失去访问。本期不提供设置密码功能，一个 GitHub 只能绑定一个系统账号。

修改 GitHub 用户名不改变系统账号归属，绑定依据为 GitHub 稳定用户 ID。系统生成独立用户名，以授权时的 GitHub 登录名作为昵称；新账号不设置默认密码。被禁用、删除或撤销角色/菜单的已绑定账号仍会被拒绝登录，不会重新注册绕过限制。系统不保存 GitHub access token。

通用公开文件上传、批量上传和删除接口要求 `system:file:manage` 权限，普通用户默认不拥有。超级管理员沿用现有权限规则；需要让其他管理角色操作用户头像时，在角色授权中分配用户管理下的“公开文件管理”按钮权限。知识库文档使用独立、按用户隔离的上传接口，不受此限制。检索评测这里只限制默认菜单，保留原有按用户隔离的 API 授权语义。

## 5. 本地运行

示例：前端访问 `http://127.0.0.1:5173/`，Vite 的 `/api` 代理指向 Java。GitHub 开发 OAuth App 的 Homepage URL 填 `http://127.0.0.1:5173/`，Authorization callback URL 填 `http://127.0.0.1:5173/api/v1/auth/github/callback`。即使 Java 监听其他端口，回调也走浏览器访问的前端站点，以保留同站 Cookie。

在启动 Java 的 PowerShell 窗口依次设置，每条命令均为一行：

```powershell
$env:APP_AUTH_GITHUB_ENABLED='true'
$env:APP_AUTH_GITHUB_CLIENT_ID='填入开发ClientID'
$env:APP_AUTH_GITHUB_CLIENT_SECRET='填入开发ClientSecret'
$env:APP_AUTH_GITHUB_REDIRECT_URI='http://127.0.0.1:5173/api/v1/auth/github/callback'
$env:APP_AUTH_GITHUB_FRONTEND_REDIRECT_URI='http://127.0.0.1:5173/#/oauth/github'
```

通过 IDE 启动时，把同名配置放在 Java Run Configuration 的 Environment variables 中。前端无需配置密钥；只要 API 代理可用即可。

## 6. 验证边界

自动化测试使用 GitHub HTTP 测试替身和 H2，不访问真实 GitHub、MySQL 或生产服务器。真实授权、Cookie 在代理后的行为及生产 MySQL 脚本，需要填写 OAuth App 配置后验收新用户注册、重复登录、默认菜单、管理接口无权限、无密码拒登/拒绝解绑和已有账号绑定流程。

临时授权会在五分钟后失效，Java 重启也会清除进行中的授权，重新点击按钮即可。当前部署是单 Java 实例；扩展到多实例时，需要共享临时 Session 或配置会话粘滞。

参考：[GitHub OAuth 授权流程](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps)。
