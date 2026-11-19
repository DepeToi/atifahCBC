---
description: 部署：先做部署前检查，给出命令序列，执行前必须人工确认，事后健康检查与回滚方案
argument-hint: "[目标，如 全部 / gateway / auth-service]"
---

# 部署：$ARGUMENTS

部署会改动共享环境。**先检查、再给命令、等确认、最后执行并验证**。

## 1. 部署前检查（逐项给结论）

1. 构建产物：`mvn clean package -DskipTests` 是否已跑、jar 时间是否最新。
2. Nacos：`common-application.yaml` 是否已发布（group `ATIFAHCBC_PUBLIC_GROUP`，namespace `atifahCBC_dev`）；网关单独用 `atifahCBC-gateway.yaml`。
3. 数据库：`deploy/sql/01-init-databases.sql` 对应的库是否已建；本次是否有新 `V{n}` 迁移，是否需要先备份。
4. 端口冲突：同端口不能同时有 JVM 与容器（8080-8090、5173）。
5. 配置安全：`JWT_SECRET` 是否为非默认值、`CORS_ALLOWED_ORIGINS` 是否收敛、数据库/Redis 凭据是否替换、Actuator 暴露面是否受控。
6. 中间件连通：容器内用服务名 `mysql/redis/nacos/rocketmq-namesrv`，不是 `127.0.0.1`。

任一项不满足，先停下来说明阻塞点，不要继续。

## 2. 命令序列（按方案给）

方案 A（JVM）：
```bash
mvn clean package -DskipTests
java -jar atifahCBC-gateway/target/atifahCBC-gateway-1.0.0-SNAPSHOT.jar
java -jar atifahCBC-services/auth-service/target/auth-service-1.0.0-SNAPSHOT.jar
```

方案 B（容器）：
```bash
docker compose -f deploy/docker-compose/docker-compose.yml up -d              # 中间件
docker compose -f deploy/docker-compose/docker-compose.app.yml up -d --build <服务>
docker compose -f deploy/docker-compose/docker-compose.app.yml ps
docker compose -f deploy/docker-compose/docker-compose.app.yml logs -f <服务>
```

## 3. 确认

把“将执行哪些命令、影响哪些服务、是否需要停旧实例、回滚怎么做”写清楚，等用户明确同意后再运行。用户已在本轮授权则直接执行，但仍要先输出计划。

## 4. 部署后验证

- `curl` 健康检查：网关 `http://localhost:8080/actuator/health`、服务各自端口、admin `8090`。
- 冒烟：`POST /api/v1/auth/login`（用测试账号）或本次改动的主链路。
- 日志：`docker compose ... logs` 或 `logs/<service>.log`，确认无启动异常、无 Nacos 配置缺失告警。

## 5. 回滚

写清回滚命令（镜像 tag / 旧 jar / compose 回退）与数据回滚限制（向前兼容的迁移不删列）。失败就如实报告并给下一步。
