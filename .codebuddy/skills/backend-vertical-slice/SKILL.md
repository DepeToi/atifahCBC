---
name: backend-vertical-slice
description: 后端垂直切片实现。用户要求“新增接口 / 实现某个服务 / 加一张表 / 写 CRUD / 接通网关”时使用，按 auth-service 模式落地代码与测试。
allowed-tools: Read, Grep, Glob, Edit, Write, PowerShell, Bash
---

# 后端垂直切片实现

## 参考实现

`atifahCBC-services/auth-service`：controller → dto/vo → service(+impl) → 协作者（幂等/失败计数/写库）→ mapper → entity → `db/migration` → 测试。
先读 `AuthController`、`AuthServiceImpl`、`V1__init.sql` 再动手。

## 落地顺序（保持每步可编译）

1. **契约**：路径 `/api/v1/<资源>`，DTO 用 `record` + `jakarta.validation`（中文 message），VO 不可变。
2. **迁移**：`V{n}__<desc>.sql`，表 `t_<domain>_<entity>`，主键雪花、`created_at/updated_at/deleted`、字段 COMMENT、`uk_/idx_`、`utf8mb4_0900_ai_ci`。
3. **持久层**：entity 继承 `BaseEntity`；mapper 接口放 `mapper/`，复杂 SQL 才写 `resources/mapper/*.xml`（`mapper-locations` 已配 `classpath*:/mapper/**/*.xml`）。
4. **服务层**：接口 + `impl/`；副作用拆协作者；`@Transactional` 只包 DB 写；跨服务用 Feign。
5. **接口层**：`@RestController` + `@RequiredArgsConstructor`，返回 `Result<T>`，写接口校验 `Idempotency-Key`。
6. **测试**：`src/test/java` 镜像包结构；纯 Mockito 单测优先，控制器用 `@WebMvcTest` + `@MockitoBean`。
7. **联动**：新服务要动 `atifahCBC-services/pom.xml`、网关 routes、`docker-compose.app.yml`、`deploy/sql/01-init-databases.sql`、`Dockerfile`、README。

## 骨架服务的起步清单

`mood/user/...` 目前只有 `*Application` 与 `application.yml`。新增业务需自建 `controller/dto/vo/service/service/impl/mapper/entity/config` 与 `src/test/java`；库已由 `deploy/sql` 建好，迁移从 `V1__init.sql` 开始。

## 完成判据

```bash
mvn -o -DskipTests compile
mvn -o test -pl <模块> -am
```

回复按层列出改动文件、新增接口与表、测试结果、未覆盖项。不要用“已实现”描述只有骨架的东西。
