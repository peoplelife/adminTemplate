# AI 对话接口文档

本文件描述当前后端实际提供的 AI 会话接口。旧的非流式 `POST /api/ai/chat` 未实现，已从文档中移除。完整定义以后端 OpenAPI 为准。

## 通用约定

- 基础路径：`/v1/ai`
- 认证：`Authorization: Bearer <token>`
- JSON 字段：camelCase
- 流式响应：`text/event-stream`
- 限流：流式对话每个 IP 每分钟最多 20 次，超限返回 HTTP 429

## 会话列表

```http
GET /v1/ai/conversations
Authorization: Bearer <token>
```

响应 `data` 是会话数组：

```json
{
  "code": "OK",
  "data": [
    {
      "id": 12,
      "userId": 1,
      "title": "帮我策划一个智能家居营销方案",
      "model": "configured-model",
      "latestMessage": "当然可以，我们先明确目标客群。",
      "createdAt": "2026-07-31T08:00:00.000Z",
      "updatedAt": "2026-07-31T08:01:00.000Z"
    }
  ],
  "message": "操作成功",
  "success": true,
  "requestId": "request-id",
  "timestamp": "2026-07-31T08:01:00.000Z"
}
```

## 会话消息

```http
GET /v1/ai/conversations/{conversationId}/messages
Authorization: Bearer <token>
```

响应 `data` 包含 `conversation` 和 `messages`。消息字段包括 `id`、`conversationId`、`role`、`content`、`model`、`promptTokens`、`completionTokens`、`totalTokens`、`finishReason` 和 `createdAt`。

## 流式对话

创建新会话：

```http
POST /v1/ai/conversations/messages/stream
Content-Type: application/json
Authorization: Bearer <token>

{
  "message": "帮我写一个产品宣传文案",
  "model": "optional-model"
}
```

继续已有会话：

```http
POST /v1/ai/conversations/{conversationId}/messages/stream
Content-Type: application/json
Authorization: Bearer <token>

{
  "message": "再给我一个更年轻化的版本"
}
```

服务端按 SSE 格式发送以下事件数据：

```text
data: {"type":"start","conversationId":12,"model":"configured-model","title":"帮我写一个产品宣传文案"}

data: {"type":"chunk","conversationId":12,"content":"当然"}

data: {"type":"done","conversationId":12,"model":"configured-model","reply":"当然可以...","usage":{"promptTokens":20,"completionTokens":30,"totalTokens":50},"finishReason":"stop"}
```

连接建立前的参数、认证和限流错误使用标准 JSON 错误响应与真实 HTTP 状态码。连接建立后的模型错误通过 SSE 发送：

```text
data: {"type":"error","code":"AI_STREAM_ERROR","message":"AI 对话失败"}
```

前端应使用 `fetch` 读取响应流，按空行拆分 SSE 帧，再解析每个 `data:` 后的 JSON。请求取消时应中止 `fetch`，服务端会停止上游生成并保存已经产生的部分回复。
