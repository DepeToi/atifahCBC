---
name: ci-triage
description: CI 流水线变更与失败排查。用户说“CI 挂了 / 构建失败 / 改 workflow / 加流水线检查”时使用。
allowed-tools: Read, Grep, Glob, PowerShell, Edit, Write
---

# CI 变更与排障

## 流水线现状

`.github/workflows/ci.yml`：backend（JDK 21，`mvn -B -ntp verify`）、frontend（Node 22，`npm ci && npm run build`）、secret-scan（gitleaks）。
仓库**未初始化 git**，配置从未实际运行；谈“CI 通过”前先确认有运行记录。

## 排障顺序

1. 拿真实日志，按 job 分类：依赖/编译/测试/类型/密钥扫描/环境。
2. 本地复现同一命令（Linux 差异要说明：大小写敏感、路径分隔符、换行符、JDK 发行版）。
3. 常见根因：
   - `mvn verify` 在 CI 失败但本地过 → 本地用了 `-o` 或跳过了测试；检查是否有依赖未被父 pom 管理。
   - 前端 `npm ci` 失败 → `package-lock.json` 与 `package.json` 不同步。
   - `vue-tsc` 报错 → 本地只跑了 `vite build`。
   - gitleaks 命中 → 先判断真假密钥；真密钥要轮换并清理历史，误报才加白名单。
4. 给最小修复，附本地验证输出。

## 改流水线原则

- 只加不减：不删测试、不加 `continue-on-error`、不跳过 secret-scan。
- `permissions` 保持最小；缓存键与 lock 文件路径必须真实存在。
- 改完给出“推送后预期行为”，不要承诺已通过。
