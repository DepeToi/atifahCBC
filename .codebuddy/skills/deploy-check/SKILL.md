---
name: deploy-check
description: 部署检查。用户说“部署 / 上线 / 发版 / 容器起不来 / 端口冲突 / Nacos 配置”时使用；先检查再给命令，执行前确认。
allowed-tools: Read, Grep, Glob, PowerShell
---

# 部署检查

## 前置条件（逐项确认，缺一不可）

1. 产物：`mvn clean package -DskipTests` 成功，jar 在 `<module>/target/`，时间晚于最后一次改动。
2. Nacos：`common-application.yaml` 已发布（group `ATIFAHCBC_PUBLIC_GROUP`，namespace `atifahCBC_dev`）；网关单独发 `atifahCBC-gateway.yaml`。未发布时业务服务会因 `NacosCommonConfigGuard` 拒绝启动。
3. 数据库：`deploy/sql/01-init-databases.sql` 对应库已存在；本次新迁移会在启动时执行，必要时先备份。
4. 端口：8080-8090 与 5173 不与现有进程冲突；**同一端口不能同时跑 JVM 和容器**。
5. 配置：容器内中间件地址用服务名（`mysql/redis/nacos/rocketmq-namesrv`），不是 `127.0.0.1`；`APP_UID/APP_GID` 与 `logs/` 属主一致。
6. 安全：`JWT_SECRET` 非默认、CORS 已收敛、数据库凭据已替换、Actuator 暴露面受控。

## 命令（按方案选）

```bash
# 中间件
docker compose -f deploy/docker-compose/docker-compose.yml up -d
# 后端容器（可指定服务）
docker compose -f deploy/docker-compose/docker-compose.app.yml up -d --build gateway auth-service
docker compose -f deploy/docker-compose/docker-compose.app.yml ps
docker compose -f deploy/docker-compose/docker-compose.app.yml logs -f auth-service
```

## 部署后

- `GET /actuator/health`：网关 8080、auth 8081、admin 8090。
- 冒烟主链路 + 检查日志无 `Nacos` 配置缺失、无启动异常。

## 禁忌

`down -v`、`docker volume rm/prune`、`system prune` 会删数据；需要时先说明影响并让用户手动执行。回滚用镜像 tag/旧 jar，DB 迁移保持向前兼容。
