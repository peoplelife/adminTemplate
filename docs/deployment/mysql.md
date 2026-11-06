# MySQL 投产准备包

整理日期：2026-11-06。范围仅为 Java 管理后台 MySQL；Python 的 PostgreSQL、向量表和 Alembic 不在此目录中。本次只读盘点了开发库，没有改库、创建账户或执行初始化 SQL。

## 文件及执行顺序

| 文件 | 用途 | 适用范围 |
| --- | --- | --- |
| `01-init.sql` | 建表、角色菜单初始化和只读验收查询 | 仅全新空库，一次执行 |

初始化脚本的建表和插入部分故意不使用 `IF NOT EXISTS`、`INSERT IGNORE` 或覆盖更新：已有表/重复配置应报错，不能掩盖结构差异。MySQL DDL 隐式提交，建表中途失败可能留下部分表；修正原因后使用另一个空库重试，不对已有库直接重跑。不要用客户端 `--force` 忽略错误。

未导出 users、user_roles、role_menus 数据，不包含密码、用户个人信息或开发用户的权限。角色菜单分配需要重新审批；不能把开发库的 admin 授权直接当成生产授权。super_admin 按现有代码访问全部有效菜单，不依赖 role_menus。

## 当前结构与实际盘点

开发库 MySQL 版本为 8.0.28；5 张表均为 InnoDB、utf8mb4_unicode_ci，服务器默认排序规则为 utf8mb4_0900_ai_ci。新库明确选择与表一致的排序规则，避免依赖服务器默认值。生产使用经过支持与验证的 MySQL 8.x 固定版本；当前未在 MySQL 8.4 上执行兼容验收。

| 表 | 职责 | 已有关键索引/关联 |
| --- | --- | --- |
| users | 用户、状态、UTC 登录/退出和软删除时间 | username 唯一，email、phone 普通索引 |
| roles | 角色 | code 唯一 |
| menus | 动态菜单及权限项 | menu_id 唯一；parent_id 为逻辑父节点 |
| user_roles | 用户角色关系 | (user_id,role_id) 唯一，关联 users/roles |
| role_menus | 角色菜单关系 | (role_id,menu_id) 唯一，关联 roles/menus |

保留特殊列 `buttonPermission`，不要自动改成下划线。menus 的业务关联键是 menu_id，不是自增 id。保留现有外键的 ON DELETE/UPDATE CASCADE；线上不要手工物理删除用户/角色，Java 使用既有软删除/业务校验流程。

现有 username 唯一约束涵盖软删除用户，删除用户名不能自动复用；本次没有改变该规则。日期列为 DATETIME、秒精度，不携带时区；应用显式使用 UTC，JWT 失效检查依赖 last_logout，不可删除这些字段。

开发库还有旧 NestJS/Prisma 的 `_prisma_migrations`。全新 Java 库不需要创建或伪造该表；若迁移整库，应保留其历史记录，且不要再同时启动旧后端执行 Prisma 迁移。Java 当前不会启动自动建表或迁移。

## 全新环境初始化

先备好 MySQL 实例、持久化数据目录和独立管理账号。生产 MySQL 不开放公网 3306，仅允许 Java 内部网络和受控管理入口。Docker 数据必须挂载 `/var/lib/mysql`；初始化目录脚本只在数据卷为空时执行，重启不会重新初始化。

管理员在 MySQL 中创建数据库（不改开发库）：

```sql
CREATE DATABASE admin_template CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

在项目根目录的 deploy/mysql/init 下使用匹配目标版本的 MySQL 客户端执行。下面是 Linux/Bash 命令；`-p` 交互输入，不把密码写入命令、脚本或 Git：

```bash
mysql --host=MYSQL_HOST --user=DEPLOY_USER -p --default-character-set=utf8mb4 admin_template < 01-init.sql
```

预期：5 张业务表、3 个角色、10 个菜单，孤儿关系计数均为 0。尚未开通管理员时 active_super_admin_accounts=0 是预期结果，但不能据此宣布应用可登录。

先由负责人确认首次管理员用户名、交付方式和凭证。当前代码仍直接保存/比较密码，属于投产阻断项；此包不生成默认弱口令或新增明文密码 INSERT。完成 Java 密码哈希改造后，再按最终格式准备一次性的管理员初始化步骤，并将其绑定到 super_admin。此项尚未完成，不能直接把哈希放入当前库并期望现有登录成功。

普通角色还需初始化实际按钮权限并授予菜单。目前导出的菜单 buttonPermission 为空；仅拥有页面菜单，不等于拥有接口权限。代码需要的权限如下：

```text
system:user:list / export / create / update / delete
system:role:list / create / update / delete
system:menu:list / create / update / delete
```

其中 `/ export` 等表示同前缀，例如 `system:user:export`。权限项使用现有 B 类型菜单维护，关联到对应父菜单；由负责人确定哪些角色可获得这些权限，本包不自动授予 admin/user。

## 账户、配置及时间

管理员负责建库、结构初始化和后续升级。Java 单独使用运行账号，只授予 `admin_template` 的 SELECT、INSERT、UPDATE、DELETE；不授予全局权限、DDL、GRANT OPTION 或 root。由 DBA 在受控入口创建账号及强密码，账号 host 按实际 Docker 网络范围确定，以下只是授予已有账号权限的示例：

```sql
GRANT SELECT, INSERT, UPDATE, DELETE ON admin_template.* TO 'admin_app'@'APP_HOST_SCOPE';
SHOW GRANTS FOR 'admin_app'@'APP_HOST_SCOPE';
```

Java 生产环境变量：

```dotenv
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/admin_template?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=admin_app
SPRING_DATASOURCE_PASSWORD=<通过生产环境安全注入>
```

mysql 是未来 Compose 服务名，不是开发机地址。不要提交真实 .env 或将密钥写入镜像。生产数据库默认时区和应用连接会话均应使用 `+00:00`，并验证；JDBC 的 serverTimezone=UTC 不能单独证明服务器/会话时区已正确设置。MySQL 可通过 `--default-time-zone=+00:00` 启动。保持严格 SQL 模式，字段超长应报错，不能通过关闭 STRICT 模式掩盖。

## 迁移已有数据：与空库初始化二选一

若需要保留已有用户/角色关系和菜单自定义，采用完整逻辑备份恢复，不能再执行 01-init.sql 初始化。先停止旧应用写入，盘点业务表与迁移历史；在隔离环境恢复并验证 Java 登录、UTC 时间、权限和菜单。开发账号、个人信息、密码不能未经审批带入生产。

不要对已有库直接执行 CREATE TABLE 基线；也不要再执行旧的新增评测菜单 SQL，01-init.sql 已包含该菜单。日后结构升级应使用经过编号和审阅的增量 SQL，并登记执行时间、数据库、版本、操作人和结果；当前尚未接入自动 MySQL 迁移工具。

## 备份与恢复演练

以下为 Linux/Bash、匹配目标版本 MySQL 客户端示例，密码交互输入：

```bash
umask 077
mysqldump --host=MYSQL_HOST --user=BACKUP_USER -p \
  --single-transaction --quick --no-tablespaces --set-gtid-purged=OFF \
  --default-character-set=utf8mb4 admin_template > admin_template-YYYYMMDD-HHMMSS.sql
```

必须检查命令退出码、文件非空并计算校验值；失败的 dump 也可能留下文件。备份含密码及个人信息，应加密、限制访问、异机保存。初始建议每日全量、至少保留最近 7 份；最终频率与保留时长依据可接受的数据损失确定。需要时间点恢复时另行启用并保留 binlog，此次未配置。备份期间避免 DDL；备份账号权限由 DBA 按版本及工具核定，不复用应用账号。

恢复到独立空库 `admin_template_restore`，先由管理员创建同字符集库：

```bash
mysql --host=MYSQL_HOST --user=DEPLOY_USER -p --default-character-set=utf8mb4 admin_template_restore < admin_template-YYYYMMDD-HHMMSS.sql

```

恢复后仅执行 01-init.sql 末尾标注的只读验收查询，不要重新执行建表和初始化部分。不要直接覆盖生产库。演练还需隔离 Java 实例连接恢复库，核对表行数、管理员登录、禁用账户、旧 Token 失效、菜单及授权。旧备份可能包含已退出账户的旧截止时间，恢复到生产前需安排会话统一失效（例如轮换 JWT 签名密钥）。Java 上传文件和 Python 数据/原文另有备份边界，MySQL dump 不包含这些文件。

## 投产验收与未完成项

- 已完成：只读导出结构，去除自增计数；筛选角色和菜单配置；整理索引、外键、权限、初始化、备份恢复步骤。
- 上线前完成：密码哈希和首次管理员开通；普通角色权限审批及初始化；固定生产 MySQL 版本；创建运行/运维账号；挂载持久化卷；备份排程与异机保存；真实恢复演练。
- 未执行：生产建库、SQL 初始化、授权或任何现有数据修改；空库 SQL 实际重放及 MySQL 8.4 兼容测试；真实备份恢复与 Docker 部署。
- 下一步：先确认使用全新数据库还是保留已有数据；按对应路径在隔离测试实例验收，之后再连接生产 Java。
