---
name: frontend-page-integration
description: 管理后台前端页面接入。用户要求“加一个管理页面 / 接后端接口 / 改表格或表单 / 前端联调”时使用。
allowed-tools: Read, Grep, Glob, Edit, Write, PowerShell
---

# 前端页面接入

## 结构约定

- `src/api/types.ts` 放接口类型；`src/api/http.ts` 已封装 axios 与 `Result` 解包，统一走 `request<T>()`。
- 页面放 `src/views/`，路由在 `src/router/index.ts`，布局在 `src/layouts/AdminLayout.vue`。
- baseURL 默认 `/admin/api`（`VITE_API_BASE_URL`），由网关转发到 `atifahCBC-admin`。

## 顺序

1. **确认后端契约**：管理端接口在 `/admin/api/**`，网关要求 `role=admin`；若后端未实现，先说明需要后端先提供什么，不要在前端 mock 成“已完成”。
2. **类型**：`types.ts` 增加 `XxxRow` / `XxxQuery` / `PageResult<T>` 组合，字段与后端 VO 对齐，不用 `any`。
3. **接口**：`api/<domain>.ts` 用 `request` 封装，返回已解包的数据；错误交给拦截器提示。
4. **页面**：`<script setup lang="ts">`；查询表单 + `el-table` + `el-pagination`；loading、空态、错误态齐全。
5. **路由与菜单**：`router/index.ts` 增加路由（懒加载），必要时在布局菜单加条目。
6. **验证**：`npm run build` 必须过（`vue-tsc --noEmit`）。后端未就绪时说明联调前置条件。

## 注意

- 不要新增第二套 UI 库或状态库；现有 Element Plus + Pinia 足够。
- 不要在页面里重复弹 `ElMessage`（拦截器已处理 HTTP 错误，业务码错误由 `request` 抛出）。
- 骨架页面的占位指标（`--`）在接入真实数据前要标注，别让占位看起来像真实数据。
