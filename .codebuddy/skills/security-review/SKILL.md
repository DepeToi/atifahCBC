---
name: security-review
description: 安全审查。用户提到“安全检查 / 有没有泄露密钥 / 鉴权是否可靠 / 上线前安全确认”，或改动涉及登录、鉴权、密钥、敏感数据、SQL 时使用。
allowed-tools: Read, Grep, Glob, Bash(git diff:*), Bash(git status:*)
---

# 安全审查

## 高频问题清单（本项目）

1. **密钥与默认值**：`JWT_SECRET` 是否仍是 `atifahCBC-dev-secret-change-me-please-32bytes`；MySQL/Redis/Nacos 是否 root/空密码；`.env` 是否被提交。
2. **网关鉴权**：`atifahCBC.gateway.auth.enabled` 是否为 true；白名单是否被扩大（除 `/api/v1/auth/`、`/actuator/health|info|prometheus` 外不应有）；`/admin/api/` 是否仍校验 `role=admin`。
3. **身份头伪造**：客户端传入的 `X-User-Id`/`X-Username` 必须被网关覆盖或剥离；业务服务不能直接信任这两个头做鉴权。
4. **敏感数据**：手机号/密码/令牌/Authorization 不得进日志、异常消息、响应体；`WebSocket ?token=` 不得透传下游。
5. **认证实现**：BCrypt 哈希、账号不存在等时比对、失败计数先占用后校验、Refresh Token 轮换与撤销。
6. **注入**：SQL 拼接、`${}` 拼接、路径穿越（`../`，网关已规范化但下游也要防）、命令注入。
7. **越权（IDOR）**：按 `userId` 查询时是否强制带归属条件，而不是仅凭入参 ID。
8. **CORS/暴露面**：`CORS_ALLOWED_ORIGINS` 是否为 `*`；Actuator 是否暴露了 gateway/metrics 等敏感端点。

## 输出

`高危（可利用）→ 中危 → 低危/加固 → 生产前必改清单`，每条带 `文件:行号` 与利用条件。不要回显真实密钥。
