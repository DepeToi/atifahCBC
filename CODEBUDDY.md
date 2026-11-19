# atifahCBC — 项目上下文（CodeBuddy Code）

情绪管理微服务工程：Spring Boot 3.5 / Spring Cloud 2025 多模块后端 + Vue 3 管理后台。
现状：**认证垂直切片已完整实现**，网关具备鉴权/链路/限流；其余业务服务只有启动类。开工前先读 `docs/repo-status.md`（带日期的实证快照）。

## 事实优先

- README 的“已具备/待实现”是规划口径，**只有代码是事实来源**。动手前用 Grep 确认类、接口、配置是否存在，不要在空壳服务上假设能力。
- 仓库当前**不是 git 仓库**（无 `.git`）：`git diff` / `git log` 会报错。评审、比对改动前先 `git rev-parse --is-inside-work-tree`；失败就按文件路径核对，并明确告诉用户“无 diff 基线”。
- `target/`、`logs/`、`node_modules/` 是产物；`.idea/` 是本地 IDE 配置，都不要改、不要当依据。

## 技术栈（版本以根 pom.xml 为准）

- 后端：Java 21、Spring Boot 3.5.16、Spring Cloud 2025.0.3、Spring Cloud Alibaba 2025.0.0.0、MyBatis-Plus 3.5.17、Redisson 3.52.0、RocketMQ Spring 2.3.6、jjwt 0.12.7、Flyway。
- 前端：Vue 3.5、TypeScript 5.9、Vite 8、Element Plus、Pinia、Vue Router、Axios。
- 基础设施：MySQL 8、Redis 7、Nacos 3、RocketMQ 5、Sentinel、Nginx、Docker Compose。

## 模块与端口

| 模块 | 端口 | 库 | 现状 |
| --- | ---: | --- | --- |
| atifahCBC-gateway | 8080 | - | 路由/鉴权/TraceId/访问日志/限流装配 |
| atifahCBC-services/auth-service | 8081 | atifahCBC_auth | 完整垂直切片 + 测试 |
| user-service | 8082 | atifahCBC_user | 仅启动类 |
| mood-service | 8083 | atifahCBC_mood | 仅启动类 |
| analysis-service | 8084 | atifahCBC_analysis | 仅启动类 |
| community-service | 8085 | atifahCBC_community | 仅启动类 |
| content-service | 8086 | atifahCBC_content | 仅启动类 |
| message-service | 8087 | atifahCBC_message | 仅启动类（含 /ws 路由） |
| growth-service | 8088 | atifahCBC_growth | 仅启动类 |
| stats-service | 8089 | atifahCBC_stats | 仅启动类 |
| atifahCBC-admin-web/backend | 8090 | atifahCBC_admin | 仅启动类（BFF/RBAC 未实现） |
| atifahCBC-admin-web/frontend | 5173 | - | 静态骨架页面，指标为占位 |

`atifahCBC-common`：common-core / common-web / common-redis / common-mybatis / common-mq / common-security，各业务服务通过 `atifahCBC-services/pom.xml` 统一继承。

## 常用命令（已在本机验证）

```bash
mvn -o -DskipTests compile          # 全量编译，改动后第一道关
mvn -o test                         # 全量测试（auth 67 个用例、gateway 16 个、common 28 个）
mvn -o test -pl atifahCBC-services/auth-service -am   # 单模块 + 依赖
mvn -B -ntp verify                  # CI 等价命令（离线库不全时去掉 -o）
cd atifahCBC-admin-web/frontend && npm run build      # vue-tsc --noEmit + vite build
docker compose -f deploy/docker-compose/docker-compose.yml up -d               # 中间件
docker compose -f deploy/docker-compose/docker-compose.app.yml up -d --build   # 后端容器
```

前置：JDK 21、Maven 3.9、Node 22。业务服务启动前 Nacos 必须已发布 `common-application.yaml`（否则 `NacosCommonConfigGuard` 让服务拒绝启动）。

## 关键代码锚点

- 统一返回/错误码/异常：`common-core` 的 `Result`、`ErrorCode`、`BusinessException`，`common-web` 的 `GlobalExceptionHandler`
- 认证参考实现：`atifahCBC-services/auth-service/src/main/java/com/atifahCBC/auth/`（controller / dto / vo / service / service/impl / mapper / entity / config）
- 网关鉴权：`atifahCBC-gateway/src/main/java/com/atifahCBC/gateway/filter/AuthGlobalFilter.java`、`AccessLogGlobalFilter.java`、`TraceIdGlobalFilter.java`
- 网关路由与白名单：`atifahCBC-gateway/src/main/resources/application.yml`
- 共享配置：`deploy/nacos/config/common-application.yaml`
- 数据库迁移：`atifahCBC-services/auth-service/src/main/resources/db/migration/`（V1、V2）
- 前端请求层：`atifahCBC-admin-web/frontend/src/api/http.ts`、`api/types.ts`

## 硬约定（细节见 .codebuddy/rules/）

1. 接口统一返回 `Result<T>`，业务失败抛 `BusinessException(ErrorCode.X)`，不要手写 ResponseEntity。
2. DTO 用 `record` + `jakarta.validation`，错误消息用中文；控制器只做入参校验和编排。
3. 一服务一库；禁止跨服务直连数据库，跨服务用 Feign/HTTP。
4. Flyway 每个服务自己管 `classpath:db/migration`；**已执行的 V 脚本永远不改**，只新增 `V{n}__desc.sql`。
5. 写接口需要 `Idempotency-Key`；并发正确性靠唯一索引兜底，不靠“先查后插”。
6. 日志禁止出现手机号、密码、令牌、Authorization；关键业务结果只记 userId。
7. 新增服务/接口要同步：`atifahCBC-services/pom.xml` 模块、网关路由、`docker-compose.app.yml`、`deploy/sql/01-init-databases.sql`、Dockerfile、README 端口表。
8. 不提交密钥，不把开发默认值当生产配置。

## 生命周期入口

| 阶段 | 命令 | 产出 |
| --- | --- | --- |
| 需求分析 | `/project:requirement <需求>` | 目标/非目标、验收标准、影响面、任务包 |
| 架构与技术路线 | `/project:design <主题>` | 方案对比 + 选定理由 + 契约 + 迁移 + 回滚（ADR 落盘） |
| 开发 | `/project:feature <任务>` | 垂直切片代码 + 单元测试 + 配置注册 |
| 契约变更 | `/project:api-change <变更>` | 兼容性与联动影响清单、契约 diff |
| 评审 | `/project:review [范围]` | P0/P1/P2 分级发现（只读） |
| 验证 | `/project:verify [模块]` | 真实编译/测试/前端构建结果 |
| CI/CD | `/project:ci [变更或失败]` | 流水线等价命令、根因与修复 |
| 部署 | `/project:deploy [目标]` | 部署前检查、命令序列、健康检查、回滚方式 |

Skills 会按语义自动触发同类工作流；`/project:*` 用于显式发起。使用说明见 `docs/codebuddy-usage.md`。

## 红线

- 不擅自 `git push`、强推、`git reset --hard`、部署、`docker compose down -v`（由 `.codebuddy/settings.json` 与 hooks 拦截或询问）。
- 不为了“让测试过”而删测试、跳过校验、改断言。
- 契约变更（路径、字段、错误码、DB 结构）必须单独成 PR，并在描述里写迁移与回滚。
