# AI Chat API

基于 FastAPI 的对话和文档 RAG 服务，提供知识库管理、文档上传与索引、检索预览和流式对话。

## 功能

- RAG 知识库管理、检索和 Server-Sent Events (SSE) 对话
- PostgreSQL、Redis 与可配置的 LLM provider
- 在线检索评测集、样本管理和历史报告；完成范围与下一步见 [评测进度](docs/rag-evaluation-progress.md)
- 评测时可开启答案生成，复用正式 RAG Prompt，并由版本化评审 Prompt 评价相关性、正确性和忠实性；未填参考答案时不评正确性。

## 环境要求

- Python 3.12 或更高版本
- [uv](https://docs.astral.sh/uv/)
- PostgreSQL 和 Redis
- 调用模型服务所需的 API key

## 安装与运行

```bash
uv sync
```

在项目根目录创建 `.env`，按实际环境设置数据库、Redis 和模型服务配置。例如：

```dotenv
DATABASE_URL=postgresql+asyncpg://postgres:postgres@localhost:5432/ai_agent_course
REDIS_URL=redis://localhost:6379/0
DEFAULT_LLM_PROVIDER=deepseek
DEFAULT_MODEL=deepseek-chat
DEEPSEEK_API_KEY=your-api-key
```

配置项及默认值见 [`app/core/config.py`](app/core/config.py)。先启动 PostgreSQL 和 Redis；首次运行及更新数据库结构时统一执行：

```bash
uv run alembic upgrade head
```

启动开发服务器：

```bash
uv run uvicorn app.main:app --reload
```

服务默认地址为 <http://127.0.0.1:8000>，交互式 API 文档为 <http://127.0.0.1:8000/docs>。健康检查接口为 `/api/v1/health` 和 `/api/v1/health/redis`。

## RAG 文档服务

文档 RAG 使用 `DATABASE_URL` 指向 PostgreSQL，并要求数据库已安装 pgvector 扩展。另需配置内部服务凭证、私有文件目录和 Embedding 服务：

```dotenv
RAG_SERVICE_TOKEN=replace-with-a-random-secret
RAG_DOCUMENT_ROOT=D:/private/ai-chat-documents
RAG_EMBEDDING_BASE_URL=https://api.siliconflow.cn/v1
RAG_EMBEDDING_API_KEY=your-api-key
RAG_EMBEDDING_MODEL=BAAI/bge-m3
RAG_RERANK_ENABLED=false
RAG_RERANK_BASE_URL=https://api.siliconflow.cn/v1
RAG_RERANK_API_KEY=your-api-key
RAG_RERANK_MODEL=BAAI/bge-reranker-v2-m3
RAG_RERANK_CANDIDATE_K=20
```

确认 `DATABASE_URL` 指向正确数据库且当前角色有安装 `vector` 扩展及创建表的权限，再执行上述 `uv run alembic upgrade head`。已运行旧迁移的数据库沿用 `0001_rag_tables` 版本，无需重新建表或清空数据。

索引和查询统一使用公共配置中的 `RAG_EMBEDDING_*`，知识库的 `embedding_model` 仅记录创建时的模型。当前向量维度固定为 1024；更换模型后必须重建所有已有文档索引，并在重建完成前停止查询，避免新旧向量混用。

重排默认关闭。设置 `RAG_RERANK_ENABLED=true`、填写 `RAG_RERANK_API_KEY` 并重启 Python 后，检索先召回至少 `RAG_RERANK_CANDIDATE_K` 条候选，再由 SiliconFlow 重排并取前 `top_k` 条。流式对话、检索预览与评测共用该链路；重排失败会明确报错，不自动降级。结果中的 `similarity` 仍是向量相似度，返回顺序由重排决定。评测报告的配置快照记录重排开关、模型和候选数量。无需数据库迁移或重新索引。

后续修改模型表结构时，生成并检查迁移文件，再升级数据库：

```bash
uv run alembic revision --autogenerate -m "describe schema change"
uv run alembic upgrade head
uv run alembic check
```

文档上传和重建索引由 FastAPI 请求同步完成，只需启动 API：

```bash
uv run uvicorn app.main:app --reload
```

请求会依次完成文件保存、解析、切块、Embedding 和片段写入，成功返回时文档状态为 `ready`。请求失败或中断时不会后台续跑，调用方需要重新上传或重建。`RAG_DOCUMENT_ROOT` 是 FastAPI 使用的私有目录，不应映射为静态资源目录。所有表均由根目录 Alembic 迁移管理并放在 PostgreSQL `public` schema。

文档解析结果为 `ParsedDocument / ParsedSegment`，切分结果为独立的 `ChunkResult / ChunkDocument`，不复用解析对象。切分配置集中在 `app/document_chunking/schemas.py` 的 `ChunkConfig`；上传和重建沿用知识库已有的 `chunk_size / chunk_overlap`，单位为字符。默认 `paragraph` 策略先按段落、换行和标点递归切分，超长段落继续拆分；`fixed_size` 仅作为内部可选兜底。重叠不跨解析片段（章节或 PDF 页），`min_chunk_size` 仅用于尝试合并短块，不能合并的短块仍保留。

解析能力独立放在 `app/document_parsing/`（`schemas.py` 定义解析对象，`service.py` 提供解析入口）；切分能力独立放在 `app/document_chunking/`。切分模块的 `schemas.py` 定义类型和配置，`chunkers.py` 实现 `BaseChunker / ParagraphChunker / FixedSizeChunker`，`chunker_factory.py` 按策略选择算法，`service.py` 提供统一入口。新增算法时实现 `split_spans()`，返回解析片段内的字符区间，并添加策略及工厂映射；来源元数据和短块合并复用基类。两个模块均不依赖 RAG、数据库或 Embedding，由 RAG 管理服务编排。

片段的页码、章节、文档 ID 和 generation 沿用原有结构化列；现有 `metadata` JSON 保存切分策略、参数、解析片段序号及字符偏移。偏移基于规范化后的解析片段，不是原始文件的字节位置。成功入库时同步记录文档和片段的 `parser_version / splitter_version`，不新增数据库表或列。`token_count` 当前仍为字符数估算，元数据通过 `token_count_method=character_estimate` 标识，不应作为模型上下文的精确 token 预算。已有文档需要重建索引才会补齐这些元数据。

普通/RAG 会话通过 `/api/v1/rag/conversations` 管理，消息入口为 `/{conversationId}/messages/stream`。流包含 `start/sources/message/done/error`，最终回答持久化成功后才发送 `done`；失败或取消时保存已生成的部分回答。等待模型期间每约 15 秒发送 `: ping`，单次生成上限为 180 秒。

## 测试

全链路追踪统一使用 `X-Trace-Id` 请求/响应头。Java JSON 和 SSE 事件使用 `traceId`，Python JSON 使用 `trace_id`；它们携带同一个值。外部 ID 仅接受 1～128 位字母、数字或 `._:-`，缺失或非法时由公共中间件生成。`clientRequestId` 仅用于消息幂等，与追踪 ID 无关。升级时先运行 `uv run alembic upgrade head`，保留历史消息的追踪值。

```bash
uv run python -m unittest discover -s tests
```

真实 PostgreSQL 的会话可靠性检查需要显式开启，会保留标记为验证用途的合成会话：

```powershell
$env:RUN_RAG_POSTGRES_CHECKS='1'
uv run python -m unittest tests.test_rag_conversations_postgres -v
```

列表分页依赖位于 `app/dependencies/pagination.py`：`PageParams` 统一 `page / page_size` 参数，`Page[T]` 统一 `items / total / page / pageSize` 响应。知识库、文档和会话列表从第 1 页开始，默认每页 20 条、最多 100 条；Repository 使用相同权限条件查询总数和分页数据。不再使用时间加 ID 的游标编码。消息历史仍按 `before_seq` 查询。

接口业务编排服务保留在 `app/services/`：`rag_management_service.py` 编排知识库和文档管理，`rag_retrieval_service.py` 承担检索，`rag_conversation_service.py` 承担会话和流式生成。文档解析、切分及提示词服务分别归属 `app/document_parsing/`、`app/document_chunking/`、`app/prompts/`。

答案生成能力位于 `app/answer_generation/service.py`，统一复用 LLM Provider；未指定模型时读取公共配置 `DEFAULT_MODEL`，调用方也可以传入已支持的模型。RAG 会话只负责检索、Prompt 组装和消息持久化，其他 Agent 或评测流程可复用生成器并传入自己的 Prompt 消息。
