# 管理后台前端规则（Vue 3 + TS）

## 结构

- 页面在 `src/views/`，布局在 `src/layouts/`，路由在 `src/router/index.ts`，接口在 `src/api/`。
- 组件一律 `<script setup lang="ts">` + `defineProps`/`defineEmits` 类型化写法，不用 Options API。
- 状态用 Pinia（`defineStore` setup 风格）；跨页共享才建 store，页面内状态用 `ref`/`computed`。

## 接口调用

- 所有请求走 `src/api/http.ts` 的 `request<T>()`，它已解包 `Result`；不要在页面里直接 `axios`。
- 类型定义放 `src/api/types.ts`；`request<PageResult<Xxx>>` 明确泛型，禁止 `any`。
- 错误提示由响应拦截器统一 `ElMessage.error`；页面只处理业务分支，不重复弹提示。
- baseURL 来自 `VITE_API_BASE_URL`，默认 `/admin/api`；新增环境变量同步 `.env.development`/`.env.production` 与 `vite-env.d.ts`。

## UI

- 组件库固定 Element Plus；图标用 `@element-plus/icons-vue`，不引第二套 UI 库。
- 列表页：查询表单 + `el-table` + `el-pagination`，loading 与空态必须有。
- 表单校验用 `el-form` 的 `rules`，提示文案中文。
- 骨架阶段页面存在占位数据时，保留原有占位风格并在改动说明中标注“仍为占位”。

## 质量门

- `npm run build`（= `vue-tsc --noEmit && vite build`）必须通过，类型错误不能靠 `@ts-ignore` 绕过。
- 不提交 `node_modules/`、`dist/`；不把密钥写进 `.env.*`（前端环境变量会进构建产物）。
