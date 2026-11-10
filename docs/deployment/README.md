# 部署说明

部署资源统一放在根目录 [deploy](../../deploy/README.md)。MySQL 说明见 [mysql.md](mysql.md)，初始化 SQL 的唯一位置为 [01-init.sql](../../deploy/mysql/init/01-init.sql)。

三个模块的 Dockerfile、Compose、HTTP/SSE Nginx、生产配置模板、Alembic 迁移服务、持久化卷和 CI/CD 工作流已配置，详见 [Docker 部署](docker.md)。服务器已完成首次部署；HTTPS 及备份恢复脚本仍需单独完善。

首次管理员、密码哈希和普通角色按钮授权仍按 MySQL 准备文档验收，不能通过启动容器代替。现有数据库未被目录迁移修改。
