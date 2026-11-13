# 页面生成 Agent 接入

前端通过 Java `/v1/ai/page-agent` 接入；Java 转发到 Python `/api/v1/ai/page-agent`。前端开发代理会添加 `/api` 前缀，生产环境复用已有 Nginx 路由。所有 Java 接口要求登录；Python 仅信任公共服务令牌与 Java 写入的 `X-User-Id`、`X-Trace-Id`。

## 接口

| 方法 | 路径后缀 | 用途 |
| --- | --- | --- |
| GET | `/workspace` | 恢复当前用户的消息、`pageSpec`、`preview` |
| POST | `/messages/stream` | 提交 `{ "content": "生成员工管理页面…" }`，需求最长 4000 字符 |
| POST | `/reset` | 清空工作区并使进行中的生成失效；首版界面不展示重置按钮 |

JSON 使用现有公共响应包装，业务数据位于 `data`，业务字段统一 camelCase。工作区消息包含 `role`、`content`、`reasoningContent`；后者保存模型返回的独立推理内容，没有推理时为空字符串。页面结构与预览复用同一套字段协议，不接受任意组件、脚本或事件处理函数。`PageSpec` 的 `table`、`pagination`、`form`、`sampleData` 和字段的 `column`、`attrs`、`defaultValue` 只映射到已有 BaseTable/Form 白名单属性。客户端不传模型参数，使用公共 `default_model`。

## SSE

| 事件 | 数据 | 前端行为 |
| --- | --- | --- |
| `status` | `{stage,message,traceId}` | 更新当前工具执行状态，不写入推理或回复正文 |
| `reasoning` | `{content}` | 追加模型接口返回的推理增量，独立展示为“模型推理” |
| `message` | `{content}` | 实时追加本段文字，一轮可收到多次，不覆盖或重复完整回复 |
| `clarification` | `{question}` | 展示澄清问题，等待下一条用户消息 |
| `page_spec` | PageSpec，包含 `pageKey`、`formColumns` | 暂存当前页面结构 |
| `preview` | PagePreview，包含 `columns`、`filters`、`form`、`rows` | 暂存合成数据预览；列和表单项带有 `column`、`attrs`、`defaultValue` |
| `done` | `{status:"completed",pageChanged}` | 页面变化时一次性发布完整预览 |
| `error` | `{code,message,traceId}` | 提示后端 message，保留上一版预览 |

每 15 秒可收到 `: ping` 注释心跳，不作为消息展示。整轮生成上限 180 秒。未收到 `done` 的断流视为失败；不发布暂存结构。Java 开流前的服务错误使用公共 JSON 响应；开流后的错误保留 SSE `error`。

模型通过公共答案生成器开启真实流式调用，LangGraph 将文字增量直接送入上述 `message` 事件；工具名称和参数的分片只在后端累计，完整后执行，不作为回复展示。多轮工具调用的正文按顺序合并，澄清问题只追加一次。成功保存的助手消息与前端拼接文本一致，末尾不再重发全文。

文字可以先于页面结果出现；`page_spec`、`preview`、`done` 仍在完整校验和工作区保存成功后发送。模型失败、截断或保存失败时保留已显示的部分回复，提示错误并保留旧页面。取消或超时会关闭进行中的模型流。

页面 Agent 在公共答案生成器中显式启用思考模式（`thinking: {type:"enabled"}`），继续使用公共默认模型。Provider 将 `reasoning_content` 与 `content` 分开解析；LangGraph 将前者转为 `reasoning` 事件，并在工具调用的下一轮请求中原样回传对应助手消息的推理字段。多轮推理在显示和保存时以空行分隔，回传工具协议时保留每轮原文。其他答案生成调用未指定思考参数时保持供应商默认行为。

公共聊天气泡在模型返回推理内容时展示可折叠“模型推理”，推理增量到达时展开，工具执行、正文开始或终止时折叠，用户可手动展开。推理按纯文本转义显示，复制回答只复制正文。成功完成后推理与正文一起保存至当前用户工作区，刷新可恢复。失败时保留当前页面收到的部分推理和回答，失败内容不写入工作区；没有独立推理的普通聊天不展示此面板。

当前默认 `deepseek-chat` 已实测支持开启思考与工具调用，供应商本次返回的模型名为 `deepseek-flash`；应用没有替换默认配置。模型协议参考 [DeepSeek Thinking Mode](https://api-docs.deepseek.com/guides/thinking_mode/)。

## 存储与目录

- Redis 使用公共连接，以用户 ID 隔离；闲置 7 天过期，保存最近 40 条消息。最近 20 条作为下一轮模型历史。
- `ai-chat/app/api/v1/page_agent.py`：HTTP 边界；`app/schemas/page_agent.py`：接口 DTO；`app/services/page_agent_service.py`：业务编排；`app/repositories/page_agent_repository.py`：Redis 访问；`app/page_generation/`：LangGraph、受限工具、页面校验及编译能力。
- `java-backend` 的 `AiPageAgentController`、`AiPageAgentService`、`PageAgentDtos`：网关入口与接口类型；RAG、评测和页面 Agent 共用 `AiServiceClient`，不依赖彼此的业务 service。
- `frontend/src/components/ai/`：公共聊天组件；`src/views/ai/pageAgent/`：双栏工作区及 BaseTable/Form 预览。知识库来源由业务 slot 提供。
- 预览交互使用浏览器内的合成数据，不调用业务数据库。刷新或重新生成会恢复合成快照。
- 页面生成涉及表格边框、分页、表单标签或示例数据时，Agent 先查询 `frontend/docs/base-table-usage.md`、`frontend/docs/form-usage.md` 的真实分节，再调用结构化工具。生产 Python 镜像通过 Docker 命名构建上下文把这两份文档放入 `/app/template-docs`；文档缺失时返回明确错误。

## 菜单与启动

入口为 AI → 页面生成；路由 `/ai/page-agent`，组件 `/ai/pageAgent/index`，沿用现有动态菜单和角色授权机制。

新数据库使用 `deploy/mysql/init/01-init.sql`。已有数据库由本期 `deploy/release.sh` 在新版应用启动前自动执行 `deploy/mysql/migrations/2026-11-12-page-agent-menu.sql`，与 GitHub 绑定表一同升级；不要重新执行初始化 SQL。脚本可重复执行，已有菜单不会重复插入或被覆盖。超级管理员自动可见，其他角色通过现有菜单管理授权。菜单脚本只增加记录，不新增表。

本机 `127.0.0.1:3306/admin_template` 此前已执行增量菜单脚本并验证组件路径。生产库由 CI/CD 发布时应用，本次未直接连接生产数据库。发布后重新登录可刷新前端菜单列表。

运行前确认 Python 已执行 `uv sync --locked`、公共 Redis 可用、公共模型配置有效、Java 的服务令牌与 Python 一致。Java/Python 需要加载本轮代码；前端 API 已使用 Java OpenAPI 运行命令生成，不需手工补接口。

## 验证记录与待办

- Python（2026-11-13）：182 项，176 项通过，6 项 PostgreSQL opt-in 跳过；覆盖真实 Agent 图的工具调用、文档检索、HTTP/SSE、用户隔离、取消、错误恢复，以及独立推理增量、工具请求中的推理原样回传和工作区分离保存。当前 Redis 行为由内存替身验证。
- 前端（2026-11-13）：30 项测试、`vue-tsc --noEmit`、`pnpm build` 通过；覆盖独立推理与正文拼接、多轮工具状态切换、完成/失败折叠、文本转义和普通聊天回归。构建仍有既有依赖 eval 和较大 chunk 提示；未进行登录浏览器验收。
- Java：184 项全部通过；鉴权、伪造身份覆盖、trace 传递、原样 SSE 转发、开流前错误和完整配置 OpenAPI 通过本地 HTTP stub 验证；已再次执行 `pnpm run build:api`。
- 真实默认模型冒烟验证通过：记录到 `search_template_docs → search_template_docs → patch_page_spec`，实际取消边框并把示例替换为张三、李四两行；未连接 Redis 或业务数据库。
- 本次真实默认模型流式验证：`search_template_docs → patch_page_spec` 取消边框成功；约 1.42 秒收到首段、5.02 秒完成，共 221 个文字增量，保存正文等于 SSE 拼接结果。Redis 使用内存替身，未连接真实 Redis 或业务数据库；该耗时仅是本次样本记录。
- 独立推理实测（2026-11-13）：默认模型启用思考后仍完成 `search_template_docs → patch_page_spec`；约 1.15 秒收到首段推理、3.20 秒收到首段正文、3.82 秒完成。17 个推理增量与 87 个正文增量分别拼接后等于保存字段；Redis 使用内存替身。耗时仅代表本次样本。
- Compose/Actions YAML 解析与命名文档上下文检查通过；本机未安装 Docker CLI，未执行镜像构建或 `docker compose config`。
- 待完成：在真实运行环境验证 Redis 和 Java→Python 链路，登录浏览器验收布局、连续修改及失败恢复，并在 CI/服务器验收新镜像。
- 后续扩展：真实业务 API 绑定、导出代码、工作区长期版本存储均不属于本次实现。
