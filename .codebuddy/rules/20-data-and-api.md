# 数据与接口契约规则

## 数据库

- 一服务一库（`atifahCBC_<domain>`）；跨库查询用接口聚合，不做跨库 JOIN。
- Flyway 位置固定：`src/main/resources/db/migration`，由 common-mybatis 在启动时执行。
- **已执行脚本禁止修改**。任何变更新增 `V{n}__<snake_case>.sql`，`n` 单调递增，不跳号、不复用。
- 表命名 `t_<domain>_<entity>`；主键 `id BIGINT UNSIGNED`（雪花，`IdWorker.getId()`），业务外键列加 `uk_`。
- 必备列：`created_at`/`updated_at` `DATETIME(3)` 带默认值与 `ON UPDATE`，`deleted TINYINT` 逻辑删除（MyBatis-Plus 全局已配）。
- 每个字段写 `COMMENT`；字符集 `utf8mb4` / `utf8mb4_0900_ai_ci`；索引命名 `uk_` / `idx_`。
- 迁移要**向前兼容**：先加列/加表并给默认值，删列与收紧约束放到后续版本；禁止在同一次发布里既改结构又依赖新结构读旧数据。

## HTTP 契约

- 路径：`/api/v1/<资源复数>`；管理端统一 `/admin/api/**`（网关要求 `role=admin`）。
- 统一响应：`{ code, message, data, timestamp }`，`code=0` 成功；业务码见 `ErrorCode`。
- 写接口必须支持 `Idempotency-Key`（UUID）；缺失返回 400 并给明确中文提示（参考 `AuthController#register`）。
- 分页参数统一 `page`（从 1 起）+ `size`，返回 `PageResult{records,total,page,size}`。
- 时间：`Asia/Shanghai`，序列化 `yyyy-MM-dd HH:mm:ss`（`common-application.yaml` 已配）。
- 认证：网关白名单最小化；除白名单外都需要 `Authorization: Bearer`；WebSocket 用 `?token=`，网关校验后剥离。

## 契约变更（必须单独 PR）

变更前先输出影响清单，再动代码：

| 变更 | 要同步的位置 |
| --- | --- |
| 新增/修改路径 | 网关 routes、白名单、README 路由表、前端 `api/*.ts` |
| 新增字段 | DTO（record）、校验、前端 types、契约文档、测试 |
| 新增错误码 | `ErrorCode`、`GlobalExceptionHandler` 状态映射、前端提示文案 |
| 分页/排序语义 | 契约文档、Mapper SQL、前端表格 |
| 表结构 | 新增 `V{n}` 迁移、实体、回滚说明 |

- 破坏性变更（删字段、改类型、改语义）默认不允许；确需变更走“新增字段 + 双写 + 下线旧字段”的三步。
- 契约文档放 `docs/contracts/`（当前不存在则新建），一个服务一个文件，含请求/响应示例与错误码表。
