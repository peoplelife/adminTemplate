# 部署说明

部署资源统一放在根目录 [deploy](../../deploy/README.md)。MySQL 说明见 [mysql.md](mysql.md)，初始化 SQL 的唯一位置为 [01-init.sql](../../deploy/mysql/init/01-init.sql)。

当前完成的是目录归并和 MySQL 投产资料整理。待完成：应用镜像、Compose、Nginx HTTPS/SSE、生产配置示例、PostgreSQL 迁移执行步骤、持久化卷、备份恢复及联通验收。暂不做 CI/CD。

首次管理员、密码哈希和普通角色按钮授权仍按 MySQL 准备文档验收，不能通过启动容器代替。现有数据库未被目录迁移修改。
