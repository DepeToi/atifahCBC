---
description: CI/CD：改流水线或排查 CI 失败，用本地等价命令复现
argument-hint: "<要改的流水线 / 失败的 job 或日志摘要>"
---

# CI/CD：$ARGUMENTS

## 现状

`.github/workflows/ci.yml` 三个 job：

| job | 内容 | 本地等价 |
| --- | --- | --- |
| backend | JDK 21 + `mvn -B -ntp verify` | `mvn -B -ntp verify` |
| frontend | Node 22 + `cd atifahCBC-admin-web/frontend && npm ci && npm run build` | 同左 |
| secret-scan | gitleaks-action | 本地可用 `gitleaks detect`（未安装则说明无法本地复现） |

注意：仓库当前**不是 git 仓库**，CI 配置存在但从未运行；不要把“配置了”说成“通过了”。

## 模式 A：改流水线

1. 说明改动目的（加速/加检查/修缓存），列出受影响的 job 与步骤。
2. 只做必要改动：不加 `continue-on-error`、不 skip 测试、不删 secret-scan、不放宽权限（`permissions` 保持最小）。
3. 缓存键与路径要与实际文件一致（Maven `pom.xml`、前端 `package-lock.json`）。
4. 本地跑等价命令，把输出摘要写进回复；提示改动需推送后才生效。

## 模式 B：排查失败

1. 先要真实日志片段（或用户贴的报错），不要凭想象。
2. 分类：依赖下载失败 / 编译错误 / 测试失败 / 前端类型错误 / 密钥扫描命中 / 环境差异（JDK、Node 版本、路径大小写）。
3. 本地复现同一条命令；复现不了就说明差异点（离线仓库、Windows 路径、缺少环境变量）。
4. 给最小修复；涉及密钥命中时，先确认是否真密钥（是则轮换并清理历史，不是则加 `.gitleaksignore` 并说明）。
