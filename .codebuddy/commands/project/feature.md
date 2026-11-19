---
description: 按垂直切片开发一个功能：契约→迁移→领域→接口→测试→联动配置，完成定义见内文
argument-hint: "<任务或服务，如 mood-service 创建打卡接口>"
---

# 开发任务：$ARGUMENTS

以 `atifahCBC-services/auth-service` 为唯一参考实现，遵守 `.codebuddy/rules/`。

## 0. 开工前

1. 确认任务范围（来自 `/project:requirement` 或 `/project:design` 的任务包）；范围不清就先补需求分析。
2. Grep 目标模块现状：是否已有 controller/service/mapper/迁移；空壳服务要从零建目录。
3. 读一遍 `AuthController`、`AuthServiceImpl`、`V1__init.sql`，对齐风格。

## 1. 顺序（每步可编译）

1. **契约**：路径、DTO、VO、错误码。DTO 用 `record` + `jakarta.validation`，中文提示。
2. **迁移**：`db/migration/V{n}__xxx.sql`（只新增，不改历史）；表 `t_<domain>_<entity>`，含 `created_at/updated_at/deleted`、`COMMENT`、`uk_/idx_`。
3. **领域**：entity（继承 `BaseEntity`）→ mapper（接口 + `resources/mapper/*.xml`，简单查询用 `LambdaQueryWrapper` 就够，别硬写 XML）。
4. **服务**：service 接口 + impl；把幂等/限流/写库等副作用拆成小协作者，事务只包 DB 写。
5. **接口**：controller 只做入参校验与编排，返回 `Result<T>`；写接口处理 `Idempotency-Key`。
6. **测试**：`src/test/java/...`，纯 Mockito 单测优先，控制器用 `@WebMvcTest`；覆盖成功/参数非法/冲突/重复调用。
7. **联动配置**：网关路由+白名单、`docker-compose.app.yml`、`deploy/sql` 建库、README 端口/路由表（仅新增服务时）。
8. **前端**（如需）：`api/types.ts` 加类型 → `api/*.ts` 封装 → `views/` 页面 → 路由。

## 2. 硬性检查

- 没有裸 `ResponseEntity`、裸错误码、`System.out`、`printStackTrace`。
- 没有敏感字段进日志；没有把 Redis/MQ 调用放进 `@Transactional`。
- 没有修改已执行的 Flyway 脚本；没有跨服务直连库。
- 新配置项有默认值且可由环境变量覆盖，并同步 `deploy/nacos/config/common-application.yaml`（若属共享配置）。

## 3. 完成定义

```bash
mvn -o -DskipTests compile
mvn -o test -pl <改动模块> -am
mvn -o test                # 改了 common-* 时必做
cd atifahCBC-admin-web/frontend && npm run build   # 改了前端时必做
```

回复里给出：改了哪些文件（按层分组）、新增接口/表、测试用例数与结果、未覆盖范围、下一步建议。测试失败不许隐瞒，也不许删测试凑绿。
