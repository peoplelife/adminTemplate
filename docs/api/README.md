# 跨模块接口约定

- 浏览器调用 Java `/v1/**`，使用 `Authorization: Bearer <token>`。
- Java 调用 Python `/api/v1/rag/**`，传递服务凭证、认证用户 ID 和 `X-Trace-Id`；浏览器不持有服务凭证。
- Java OpenAPI 入口为 `/v3/api-docs`；前端在 frontend 中运行 `pnpm run build:api` 生成接口，通过 `OPENAPI_URL` 指定来源。
- JSON 接口沿用当前 `Result` / `ApiResponse` 契约；SSE 不包装普通 JSON 响应，保留 start、sources、message、done、error 事件。

细节见 [Java 规范](../../java-backend/docs/java-backend-spec.md)、[Python 说明](../../ai-chat/README.md)、[评测进度](../../ai-chat/docs/rag-evaluation-progress.md)。不复制维护自动生成的完整接口清单。
