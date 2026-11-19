# 仓库事实核查规则

## 先证据，后结论

- 任何“已实现/已有/支持”的判断，必须来自 Grep 到的类、方法、配置项或测试；只写在 README、注释、Issue 里的能力一律视为**未实现**。
- 引用代码时给 `文件:行号`，便于复现。
- 发现文档与代码不一致：以代码为准，在回复中点出差异；差异涉及项目状态时，同步更新 `docs/repo-status.md`（含核对日期）。

## 当前已核实的空壳（2026-11-20）

`user / mood / analysis / community / content / message / growth / stats` 八个服务只有 `*ServiceApplication` 与 `application.yml`；`admin-service` 同样只有启动类；管理后台前端是静态骨架，指标显示 `--`。

为这些服务写“查询/列表/详情”等描述前，先确认对应 controller / mapper / 迁移脚本是否存在。

## 环境事实

- 目录**不是 git 仓库**：先 `git rev-parse --is-inside-work-tree`；失败时不要用 `git diff` 做评审基线，改为按路径列出改动，并在结论中声明“无版本对比”。
- 构建走 Maven 多模块，根 `pom.xml` 只聚合 common、gateway、services、admin-web/backend；前端不在 Maven 生命周期内。
- 离线构建需要本地仓库已有依赖：`-o` 失败就去掉 `-o` 重试，并把这一事实写进验证结果。
- 业务服务强依赖 Nacos 的 `common-application.yaml`；没有 Nacos 时只能跑单测，不能跑端到端。

## 不要做的事

- 不要为了让流程“看起来完整”而编造目录、ADR、契约文件；不存在就明确说不存在，或按需要新建。
- 不要修改 `target/`、`logs/`、`.idea/` 来“修复”问题。
- 不要假设存在 CI 运行记录：`.github/workflows/ci.yml` 只是配置。
