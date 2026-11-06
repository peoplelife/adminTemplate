# 部署资源

本目录负责跨模块组合运行；应用代码、构建配置和测试仍留在三个应用内。

已具备：`mysql/init/01-init.sql`，仅用于全新空库初始化；说明见 [MySQL 准备](../docs/deployment/mysql.md)。

后续有实际内容时增加 `compose.yml`、`nginx/`、`scripts/`、`.env.example`。应用 Dockerfile 放各应用根目录，Compose 构建上下文分别为 `../frontend`、`../java-backend`、`../ai-chat`。不创建没有实现内容的配置文件或空脚本。

生产私有配置使用本目录的 `.env`，不得提交。数据库、公开上传及私有原文使用独立持久化卷，不放源代码目录；私有原文不挂载给 Nginx。未来服务间使用 java、python、mysql、postgres、redis 等服务名通信。

当前没有可用的生产 Compose，不执行 `docker compose up`。不包含 CI/CD。
