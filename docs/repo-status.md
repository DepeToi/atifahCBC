# 仓库现状快照

> 核对日期：2026-11-20。这是**实证快照**，不是规划清单。任何结论以代码为准；本文件过期时请按文末方法重新核对并更新日期。

## 构建与测试

| 命令 | 结果 |
| --- | --- |
| `mvn -o -DskipTests compile` | SUCCESS（全模块） |
| `mvn -o test` | BUILD SUCCESS：auth-service 67 个用例、gateway 16 个、common 28 个，0 失败 |
| 前端 `npm run build`（vue-tsc + vite） | PASS，built in 726ms；警告：主 chunk 1.02MB（Element Plus 未拆包） |

环境：Windows 11、JDK 21.0.2、Maven 3.9.12、离线本地仓库可用。

## 模块实现度

| 模块 | 实现度 | 依据 |
| --- | --- | --- |
| common-core | 统一 `Result`/`PageResult`/`ErrorCode`/`BusinessException`/TraceId | 源码 + 测试 |
| common-web | 全局异常、TraceId/访问日志过滤器、Feign 透传 | 源码 + 4 个测试 |
| common-redis / common-mybatis / common-mq / common-security | 自动配置、工具类、JWT、BCrypt | 源码 + 少量测试 |
| gateway | 路由、JWT 鉴权、管理端角色、WebSocket token、TraceId、访问日志、Sentinel 限流装配 | 源码 + 4 个测试 |
| auth-service | 注册/登录/刷新/登出、幂等、失败锁定、令牌轮换、Flyway V1/V2 | 源码 + 7 个测试类 |
| user/mood/analysis/community/content/message/growth/stats | **仅启动类 + application.yml** | 目录内无 controller/service/mapper |
| admin-service | 仅启动类 + application.yml | 同上 |
| admin-web 前端 | 路由 + 4 个视图骨架，指标占位 `--` | 源码 |

## 已知缺口

- 除 auth 外**没有任何业务接口**；管理端 BFF/RBAC、管理端登录签发令牌均未实现。
- 只有 auth 有 `db/migration`（V1、V2），其余服务的库为空。
- `docs/contracts/`、`docs/adr/` 不存在；`docs/` 下只有本文件与 `codebuddy-usage.md`。
- 仓库**未初始化 git**（无 `.git`），因此 `.github/workflows/ci.yml` 从未真正运行，也没有 PR 历史。
- `atifahCBC-app/` 只有占位，用户端工程未创建。
- 前端主 bundle 1.02MB，未做路由级拆包/Element Plus 按需引入。

## README 与实际的关系

README 的“模块说明 / 待实现”基本符合实际；其中“已具备 CodeBuddy 配置、docs/contracts、ADR”一项在本次之前**不成立**，现已补齐 `.codebuddy/` 与 `docs/codebuddy-usage.md`，契约与 ADR 目录仍待按需创建。

## 如何刷新本文件

1. `mvn -o -DskipTests compile` + `mvn -o test`，记录真实结果（失败也要写）。
2. `Get-ChildItem -Recurse -Filter *Controller.java`、`*Mapper.java`、`db/migration` 确认实现度。
3. 更新上方表格与日期；只写验证过的内容。
