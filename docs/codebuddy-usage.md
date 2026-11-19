# CodeBuddy Code 使用指南（本项目）

本仓库的项目级 AI 能力由 `CODEBUDDY.md` + `.codebuddy/` 组成，覆盖需求 → 设计 → 开发 → 测试 → 评审 → CI/CD → 部署。全部随仓库提交，团队共享。

## 1. 能力地图

| 阶段 | 显式命令 | 自动技能（按语义触发） | 约束来源 |
| --- | --- | --- | --- |
| 需求分析 | `/project:requirement` | `requirement-analysis` | rules/00 |
| 架构与技术路线 | `/project:design` | `architecture-design` | rules/10、20 |
| 开发 | `/project:feature` | `backend-vertical-slice`、`frontend-page-integration` | rules/10、20、30、40 |
| 契约变更 | `/project:api-change` | `api-contract-check` | rules/20 |
| 评审 | `/project:review` | 子代理 `code-reviewer`、`security-reviewer` | rules 全部 |
| 缺陷修复 | `/project:fix-issue` | — | rules/40 |
| 验证 | `/project:verify` | — | rules/40 |
| CI/CD | `/project:ci` | `ci-triage` | rules/50 |
| 部署 | `/project:deploy` | `deploy-check` | rules/50 |

安全审查技能 `security-review` 在改动鉴权、密钥、敏感数据时自动触发。

## 2. 首次使用

1. 在仓库根目录启动 `codebuddy`，首次会询问是否信任本目录（信任后项目 rules/skills 才生效）。
2. 输入 `/hooks` 审阅并确认 `.codebuddy/settings.json` 里的两个 hook（Bash 守卫、配置校验）。hook 是 Beta 能力，未确认前不会执行。
3. `/skills` 确认 8 个项目技能已加载，`/agents` 确认两个只读评审代理。
4. `/permissions` 查看 deny/ask 规则，需要放宽时改 `.codebuddy/settings.local.json`（不提交）。

## 3. 典型用法

```text
/project:requirement 用户能按天记录心情并查看近 7 天趋势
→ 产出验收标准、影响面、任务包（含允许修改路径）

/project:design mood-service 打卡写入与趋势查询链路
→ 方案对比 + ADR（docs/adr/0001-*.md）+ 契约草案

/project:feature 实现 mood-service 创建打卡接口
→ 迁移 V1、entity/mapper/service/controller、单测、网关与 compose 联动

/project:verify atifahCBC-services/mood-service
→ 编译 + 单测 + 前端构建的真实结果

/project:review
→ P0/P1/P2 分级发现，只读

/project:deploy gateway
→ 部署前检查 → 命令序列 → 等确认 → 健康检查 → 回滚方式

/project:fix-issue 登录连续失败后账号被永久锁定
→ 复现 → 根因 → 先补失败用例 → 修复 → 验证
```

需求描述越具体（角色、场景、异常分支），产出越可用；信息不足时命令会先提问而不是猜。

## 4. 文件职责

| 文件 | 作用 | 何时改 |
| --- | --- | --- |
| `CODEBUDDY.md` | 项目上下文：技术栈、模块端口、命令、硬约定、命令入口 | 架构/技术栈/端口变化时 |
| `.codebuddy/rules/00-repo-truth.md` | 事实核查纪律、空壳清单、环境事实 | 实现度明显变化时 |
| `.codebuddy/rules/10-backend-java.md` | 分层、返回/异常、事务、日志、新增服务清单 | 后端约定变化时 |
| `.codebuddy/rules/20-data-and-api.md` | 迁移、表规范、HTTP 契约、变更同步表 | 契约或迁移策略变化时 |
| `.codebuddy/rules/30-frontend.md` | 前端结构、请求层、UI 与质量门 | 前端栈变化时 |
| `.codebuddy/rules/40-testing.md` | 测试写法、覆盖要求、验证顺序 | 测试栈或门禁变化时 |
| `.codebuddy/rules/50-security-and-delivery.md` | 安全红线、CI、部署与回滚 | 上线流程变化时 |
| `.codebuddy/commands/project/*.md` | 9 个显式工作流 | 流程调整时 |
| `.codebuddy/skills/*/SKILL.md` | 自动触发的领域知识 | 领域规则变化时 |
| `.codebuddy/agents/*.md` | 只读评审/安全子代理 | 评审口径变化时 |
| `.codebuddy/hooks/*.js` | 危险命令拦截、写入后 JSON/YAML 校验 | 防护策略变化时 |
| `docs/repo-status.md` | 带日期的实证快照 | 每次重要变更后刷新 |

规则越具体越有效：写“接口必须返回 Result”比写“注意代码质量”有用。

## 5. 维护

- **新增服务**：更新 `CODEBUDDY.md` 模块表、`docs/repo-status.md`、`rules/10-backend-java.md` 的联动清单（若流程变化）。
- **契约变更**：跑 `/project:api-change`，同步 `docs/contracts/`；契约文档目录不存在时先建。
- **hook 调试**：手动喂输入验证，例如
  `echo '{"tool_input":{"command":"git push origin main"}}' | node .codebuddy/hooks/guard-bash.js`
  无输出=放行；输出 `permissionDecision`=命中规则。
- **规则冲突**：以 `CODEBUDDY.md` 的红线与 `rules/50` 的安全要求为准；发现 skill 与规则矛盾时改 skill。

## 6. 边界与限制

- Skills/命令是**提示词工作流**，不是 CI 作业；它们不会自动在流水线里执行。
- hooks 依赖本机 `node` 与 Git Bash（Windows）；缺少 node 时 hook 静默跳过，不会阻断工作。
- 仓库尚未初始化 git，评审/部署命令会显式说明“无 diff 基线/无 CI 记录”，不会假装有。
- 个人偏好放 `CODEBUDDY.local.md` 或 `.codebuddy/settings.local.json`（已被 `.gitignore` 忽略）；不要把个人密钥写进共享配置。
