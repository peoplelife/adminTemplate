# 页面 Agent 模板文档检索实施计划

目标：用户提出需求后，Agent 查询真实组件说明，使用受约束配置生成和修改预览。扩展现有工作流，不新增数据库表、Embedding 服务或独立设置。

## 已确认设计

- Markdown 来源为 `frontend/docs/base-table-usage.md` 和 `frontend/docs/form-usage.md`，按标题分节做中文关键词及属性名检索，返回标题、来源和原文。文档禁止声明尚未实现的控件。
- 新增第六个工具 `search_template_docs(query)`；涉及生成和修改时先查文档。查询结果作为工具观察交给模型，随后调用现有创建或修改工具。模板说明只作为资料，不能授权任意代码执行。
- PageSpec 保留现有字段名，新增 `table`、`pagination`、`form`、`sampleData`。字段增加 `column`、`attrs`、`defaultValue`；原有 `formColumns` 保留。
- `table`：border、stripe、size、showHeader、height。固定表头用真实 ElTable height。
- `pagination`：enabled、pageSize、pageSizes、background；禁用时展示全部过滤结果。
- `form`：labelPosition、labelWidth、size、disabled；字段 attrs 支持 placeholder、clearable、disabled、maxlength、min、max、step、precision。
- `column`：width、minWidth、align、fixed、sortable、showOverflowTooltip。字段顺序即列顺序。
- sampleData 最多 100 行，字段值按控件类型和选项校验；行 ID 由编译器分配。未指定时仍生成默认示例；空数组表示无数据。修改结构保留仍有效的示例值，不触及用户业务数据库。
- PagePreview 保留原协议，新增字段配置体现在 columns/form 的 column、attrs、defaultValue；表格/分页/表单整体配置直接读取同次 PageSpec，避免重复保存两份配置。
- 工具 JSON Schema 从 Pydantic 定义产生，避免人工维护多份字段白名单。局部修改合并嵌套配置，全部校验成功后替换。
- Docker 使用命名构建上下文 `template-docs` 将真实前端文档打入 Python 镜像；开发环境直接读取仓库 frontend/docs。不会在运行时抓取任意 URL 或任意服务器文件。
- Prompt 新增 v2 并切换页面 Agent，v1 保留原样。

## 执行步骤

- [x] Python 测试覆盖中文查边框/分页、实际文档来源、配置局部修改、示例数据类型和原子失败、真实图查文档后修改；完整测试 173 项（6 项 PostgreSQL opt-in 跳过）。
- [x] 实现 schemas/compiler/tools/documentation 与 Prompt v2，接入已有 LangGraph；页面 Agent 已显式使用 `page_generation@v2`。
- [x] Java typed DTO 同步上述字段，完成 OpenAPI 和 JSON 转发；JDK21 `mvn -ntp test` 184 项通过。
- [x] 前端按 PageSpec 配置渲染 BaseTable/Form，覆盖取消边框、分页关闭、表单属性、列属性、示例数据显示；25 项测试、类型检查和生产构建通过。搜索表单不会套用新增表单的默认值。
- [x] 导出真实 Java OpenAPI 并运行 `pnpm run build:api`，生成文件未手工修改。
- [x] Compose/Actions 通过命名构建上下文传入 `frontend/docs`；已验证 Python 全量测试、前端测试/类型检查/build、Java 测试。
- [x] 更新接入说明及限制：仅声明式已有组件配置；真实 API、函数式 render、任意代码仍不开放。

## 尚未完成

- 未在生产服务器重新构建镜像并验收命名构建上下文；需由部署流程运行新的 CI release。
- 真实默认模型已完成独立冒烟验证，确认先调用 `search_template_docs` 后 `patch_page_spec`，取消边框并替换两行示例；尚未完成浏览器端到端验收和真实 Redis 链路验证。
- 本机无 Docker CLI：Compose/Actions YAML 解析及上下文配置检查通过，但镜像构建及 `docker compose config` 需在 CI/服务器执行。

当前工作已在 feature/page-generation-agent 分支中，保留现有未提交修改；本任务不提交、推送、部署或修改数据库。
