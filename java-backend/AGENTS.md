# Java Backend AI Rules

本文件是 `java-backend` 的 AI 工作入口。凡是修改 Java、SQL、接口、配置或测试，先阅读 `docs/java-backend-spec.md`。

## 必须遵守

- 技术栈固定为 Java 21、Spring Boot 3.5.6、Maven、MyBatis-Plus 3.5.12；不要把 JPA、Kotlin 或其他项目的约定引入本项目。
- 保持 `Controller -> Service -> Mapper -> Database` 分层。Controller 只处理 HTTP 和校验，Service 承担业务与事务，Mapper 只负责数据访问。
- 请求和响应使用 DTO/`record`；不要让 Entity 直接出现在接口响应中，不要在 Controller 里拼复杂 `Map` 或写业务规则。
- `/v1` JSON Controller 必须显式返回 `Result<T>`；不要添加自动包裹响应体的 `ResponseBodyAdvice`。业务异常使用 `BizException` 和 `BusinessError`，经全局处理器转换为 `Result`；系统故障使用 `AppException` 和 `SystemError`。
- `Result.code` 是数字结果码，业务成功固定为 `0`；业务异常、鉴权和参数校验失败使用 HTTP `200`，通过 `success=false` 和独立数字业务码表达。业务层不得使用 HTTP 状态码充当业务码。HTTP 协议错误和服务器/外部服务故障保留真实 HTTP 状态码。
- 保持现有 `/v1` 路由、统一响应字段、`X-Trace-Id`、状态码和空响应契约。修改公开接口时同步更新 OpenAPI 和测试。
- 受保护接口使用 `Authorization: Bearer <token>`。新增 `/v1` 接口默认需要认证，并同步检查 `SecurityConfig`、JWT 过滤器和权限注解。
- SQL 必须参数化；不得把用户输入拼进 SQL、日志、文件路径或外部请求。密码、JWT、数据库密码和其他密钥不得写入日志或新代码。
- 不新增明文密码存储、越权、未限制分页、路径穿越、资源未关闭或吞异常代码。现有遗留问题不得扩散；涉及它们时先说明迁移风险。
- 不为了“整洁”大范围重构、改路由或更换依赖；先检查调用方、测试、配置和文档。

## 完成前检查

- 使用 JDK 21 运行至少一个相关测试；正常代码改动完成后运行 `mvn -ntp test`。
- 检查 `git diff` 和 `git diff --check`，确认没有覆盖用户改动、秘密、乱码或无关格式化。
- 最终说明修改的文件、验证命令和未处理的风险；不要把“测试没跑”说成“已完成”。
