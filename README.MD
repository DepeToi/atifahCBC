# atifahCBC

atifahCBC 是一个面向情绪管理场景的微服务工程，包含情绪打卡、趋势分析、社区内容、消息、成长、统计、管理后台等能力规划。

当前项目处于工程骨架和功能演进阶段：认证服务已经具备完整垂直切片，网关、公共模块和管理后台前端骨架可用，其余业务服务目前主要提供启动入口。

## 技术栈

### 后端

- Java 21
- Spring Boot 3.5
- Spring Cloud 2025
- Spring Cloud Gateway WebFlux
- Spring Cloud Alibaba：Nacos、Sentinel
- MyBatis-Plus
- Redis / Redisson
- RocketMQ
- JWT
- Maven 多模块
- Docker / Docker Compose

### 管理后台

- Vue 3
- TypeScript
- Vite 8
- Element Plus
- Pinia
- Vue Router
- Axios

### 基础设施

- MySQL 8
- Redis 7
- Nacos 3
- RocketMQ 5
- Sentinel Dashboard
- Nginx
- Seata（可选）

## 模块说明

```text
atifahCBC
├── atifahCBC-common/              公共能力模块
│   ├── common-core/            统一响应、异常、常量、分页与 TraceId
│   ├── common-web/             全局异常、访问日志、TraceId 与 Feign 拦截器
│   ├── common-redis/           Redis 配置与工具封装
│   ├── common-mybatis/         MyBatis-Plus 与基础实体
│   ├── common-mq/              RocketMQ 自动配置与消息发布封装
│   └── common-security/        JWT、Security 配置与 Nacos 安全检查
├── atifahCBC-gateway/             网关、鉴权、路由、限流与链路标识
├── atifahCBC-services/            业务微服务聚合模块
│   ├── auth-service/           注册、登录、刷新令牌与登出
│   ├── user-service/           用户领域骨架
│   ├── mood-service/           情绪打卡骨架
│   ├── analysis-service/       情绪分析骨架
│   ├── community-service/      社区骨架
│   ├── content-service/        内容骨架
│   ├── message-service/        消息骨架
│   ├── growth-service/         成长与打卡骨架
│   └── stats-service/          统计骨架
├── atifahCBC-admin-web/
│   ├── backend/                管理后台 BFF/RBAC 服务骨架
│   └── frontend/               Vue 3 管理后台
├── atifahCBC-app/                 用户端规划，尚未创建完整工程
├── deploy/                     Docker、Nacos、Nginx 与数据库部署资源
└── .github/workflows/ci.yml    后端、前端与密钥扫描 CI
```

## 服务端口

| 服务 | 本地端口 | 数据库 |
| --- | ---: | --- |
| Gateway | 8080 | - |
| Auth | 8081 | atifahCBC_auth |
| User | 8082 | atifahCBC_user |
| Mood | 8083 | atifahCBC_mood |
| Analysis | 8084 | atifahCBC_analysis |
| Community | 8085 | atifahCBC_community |
| Content | 8086 | atifahCBC_content |
| Message | 8087 | atifahCBC_message |
| Growth | 8088 | atifahCBC_growth |
| Stats | 8089 | atifahCBC_stats |
| Admin | 8090 | atifahCBC_admin |
| Management frontend | 5173 | - |

网关路由：

- `/api/v1/auth/**` → `lb://atifahCBC-auth`
- `/api/v1/users/**` → `lb://atifahCBC-user`
- `/api/v1/moods/**` → `lb://atifahCBC-mood`
- `/api/v1/analysis/**` → `lb://atifahCBC-analysis`
- `/api/v1/community/**` → `lb://atifahCBC-community`
- `/api/v1/contents/**` → `lb://atifahCBC-content`
- `/api/v1/messages/**` → `lb://atifahCBC-message`
- `/ws/**` → `lb:ws://atifahCBC-message`
- `/api/v1/growth/**` → `lb://atifahCBC-growth`
- `/api/v1/stats/**` → `lb://atifahCBC-stats`
- `/admin/api/**` → `lb://atifahCBC-admin`

## 环境要求

| 工具 | 要求 |
| --- | --- |
| JDK | 21 或更高 |
| Maven | 3.9 或更高 |
| Node.js | `^20.19.0` 或 `>=22.12.0` |
| Docker Desktop | 建议启用 WSL2 后端 |
| Docker Compose | 需要支持 Compose v2 |

Windows 开发机建议使用 JDK 21、Maven 3.9、Node.js 22 和 Docker Desktop。

## 快速开始

完整部署说明见 [deploy/dev-deploy.md](deploy/dev-deploy.md)。

### 1. 启动基础设施

进入部署目录并准备 Nacos 环境变量：

```bash
cd deploy/docker-compose
cp .env.example .env
openssl rand -base64 32
```

将上一步生成的值写入 `.env` 的 `NACOS_AUTH_TOKEN`。

启动 MySQL、Redis、Nacos、RocketMQ 等基础设施：

```bash
docker compose -f deploy/docker-compose/docker-compose.yml up -d
```

首次启动会执行 [deploy/sql/01-init-databases.sql](deploy/sql/01-init-databases.sql)，创建各微服务使用的独立数据库。

### 2. 导入 Nacos 配置

将 [deploy/nacos/config/common-application.yaml](deploy/nacos/config/common-application.yaml) 发布到 Nacos：

- Namespace：`atifahCBC_dev`
- Data ID：`common-application.yaml`
- Group：`ATIFAHCBC_PUBLIC_GROUP`
- Format：YAML

未发布共享配置时，业务服务会拒绝启动。Nacos 导入方式见 [deploy/nacos/README.md](deploy/nacos/README.md)。

### 3. 构建后端

在仓库根目录执行：

```bash
mvn clean package -DskipTests
```

快速编译检查：

```bash
mvn -o -q -DskipTests compile
```

完整验证：

```bash
mvn -B -ntp verify
```

### 4. 运行后端

方案 A：直接运行 JAR。

```bash
java -jar atifahCBC-gateway/target/atifahCBC-gateway-1.0.0-SNAPSHOT.jar
java -jar atifahCBC-services/auth-service/target/auth-service-1.0.0-SNAPSHOT.jar
java -jar atifahCBC-services/user-service/target/user-service-1.0.0-SNAPSHOT.jar
java -jar atifahCBC-admin-web/backend/target/admin-service-1.0.0-SNAPSHOT.jar
```

方案 B：使用 Docker Compose 启动后端容器。

先构建后端 JAR，再执行：

```bash
docker compose -f deploy/docker-compose/docker-compose.app.yml up -d --build
```

按需启动指定服务：

```bash
docker compose -f deploy/docker-compose/docker-compose.app.yml up -d --build gateway auth-service
```

停止后端容器：

```bash
docker compose -f deploy/docker-compose/docker-compose.app.yml down
```

不要同时运行相同端口的 JVM 和 Docker 后端实例。

### 5. 启动管理后台

```bash
cd atifahCBC-admin-web/frontend
npm ci
npm run dev
```

访问：

- 管理后台：`http://localhost:5173`
- 网关：`http://localhost:8080`
- 网关健康检查：`http://localhost:8080/actuator/health`
- Auth 健康检查：`http://localhost:8081/actuator/health`
- 管理服务健康检查：`http://localhost:8090/actuator/health`

## 认证能力

认证服务已提供以下接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/auth/register` | 手机号注册，需要 `Idempotency-Key` |
| POST | `/api/v1/auth/login` | 手机号密码登录 |
| POST | `/api/v1/auth/refresh` | Refresh Token 轮换 |
| POST | `/api/v1/auth/logout` | 撤销 Refresh Token |

认证实现包含：

- BCrypt 密码存储
- 登录失败次数限制
- 注册幂等键防重
- 并发注册唯一键冲突恢复
- Refresh Token 持久化与轮换
- 登出幂等撤销
- 账号不存在与密码错误的统一响应
- 账号不存在时执行等时密码校验
- Access Token 只承载用户身份（subject 与脱敏用户名），不包含管理端角色

## 网关鉴权

网关默认开启鉴权，除白名单外所有请求都需要 `Authorization: Bearer <accessToken>`：

- 白名单：`/api/v1/auth/`、`/actuator/health`、`/actuator/info`、`/actuator/prometheus`。
- `/admin/api/**` 额外要求 `role=admin`，否则返回 403；C 端用户令牌无法访问管理端接口。
- WebSocket `/ws/**` 因浏览器无法设置请求头，改用 `?token=<accessToken>`，网关校验后剥离该参数再转发。
- 鉴权失败返回统一 `Result` 结构（40100 / 40300），失败日志只记录路径、状态和原因。

限流规则默认关闭，见网关 `atifahCBC.gateway.rate-limit` 配置。

## 配置约定

主要可通过环境变量覆盖的配置：

| 环境变量 | 用途 | 默认值 |
| --- | --- | --- |
| `NACOS_ADDR` | Nacos 地址 | `127.0.0.1:8848` |
| `NACOS_NAMESPACE` | Nacos 命名空间 | `atifahCBC_dev` |
| `MYSQL_HOST` | MySQL 地址 | `127.0.0.1` |
| `MYSQL_PORT` | MySQL 端口 | `3306` |
| `MYSQL_USER` | MySQL 用户名 | `root` |
| `MYSQL_PASSWORD` | MySQL 密码 | `root` |
| `REDIS_HOST` | Redis 地址 | `127.0.0.1` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `ROCKETMQ_NAMESRV` | RocketMQ NameServer | `127.0.0.1:9876` |
| `SENTINEL_DASHBOARD` | Sentinel 控制台 | `127.0.0.1:8858` |
| `JWT_SECRET` | 网关与认证服务共享密钥 | 开发默认值 |
| `CORS_ALLOWED_ORIGINS` | 网关允许的来源 | `*` |
| `ATIFAHCBC_LOG_DIR` | 日志目录 | `logs` |

Docker 环境由 [deploy/docker-compose/docker-compose.app.yml](deploy/docker-compose/docker-compose.app.yml) 注入容器服务名地址：

- `mysql`
- `redis`
- `nacos`
- `rocketmq-namesrv`
- `sentinel-dashboard`

容器内不要使用宿主机的 `127.0.0.1` 访问这些服务。

## 日志与监控

后端日志同时写入控制台和服务日志文件。

本地 JVM 日志默认位于仓库根目录的 `logs/`：

```text
logs/atifahCBC-gateway.log
logs/atifahCBC-auth.log
```

网关和业务服务暴露 Actuator、Prometheus metrics 与健康检查。

可访问：

- 网关：`http://localhost:8080/actuator/health`
- Prometheus：`http://localhost:8080/actuator/prometheus`
- Sentinel：`http://localhost:8858`
- RocketMQ Dashboard：`http://localhost:8180`
- Nacos：`http://localhost:8849`

## CI

GitHub Actions 会在拉取请求和推送到 `main` 分支时执行：

- JDK 21 后端 `mvn verify`
- Node.js 22 前端依赖安装与构建
- CodeBuddy hook 单元测试
- Gitleaks 密钥扫描

工作流配置见 [.github/workflows/ci.yml](.github/workflows/ci.yml)。

## AI 编程工具（CodeBuddy Code）

仓库根目录包含 CodeBuddy Code 项目级配置：

- `CODEBUDDY.md`：团队共享的项目上下文与开发约定。
- `.codebuddy/settings.json`：共享权限设置，禁止读取或修改常见密钥文件，并对推送、强制重置、部署、删除卷等不可逆操作要求确认。
- `.codebuddy/rules/`：代码风格、代码优化原则、后端/前端约定、测试与 API 约定。
- `.codebuddy/commands/project/`：`/project:review`、`/project:fix-issue`、`/project:deploy`、`/project:verify` 和 `/project:api-change` 项目命令。
- `.codebuddy/skills/`：安全审查、部署检查、后端垂直切片、前端页面接入和接口联动检查工作流。
- `.codebuddy/agents/`：只读代码审查和安全审查子代理。
- `.codebuddy/hooks/`：Hooks——执行前确认推送、发布、删除卷/容器、递归删除和读取密钥等命令，写入后校验 JSON/YAML 语法。

详细使用说明见 [docs/codebuddy-usage.md](docs/codebuddy-usage.md)。

安装并登录 CodeBuddy Code 后，从仓库根目录启动：

```bash
codebuddy
```

进入会话后可使用项目命令，例如：

```text
/project:review
/project:fix-issue 修复具体问题
/project:deploy 检查 gateway 的部署配置
/project:verify
```

个人偏好可放在 `CODEBUDDY.local.md` 或 `.codebuddy/CODEBUDDY.md`，个人权限和 MCP 配置可放在 `.codebuddy/settings.local.json`。这些本地文件已加入 `.gitignore`，不会提交到仓库。MCP 服务需根据个人环境单独配置；不要把密钥或生产环境凭据加入共享设置。

## 当前实现状态

### 已具备

- Maven 多模块结构
- Nacos 服务注册与配置
- 网关路由、TraceId 与访问日志
- JWT 鉴权、管理端角色校验与超时控制
- Sentinel 接入与可配置网关限流规则
- Prometheus 指标暴露
- Redis、RocketMQ、MyBatis-Plus 公共能力
- MySQL 独立数据库初始化
- Docker Compose 部署
- 管理后台前端路由和页面骨架
- 完整认证服务及相关测试
- 后端和前端构建 CI

### 待实现

- mood、analysis、community、content、message、growth、stats 业务接口
- 用户领域和业务数据库实体
- 管理后台 BFF 接口
- 管理端登录与 RBAC（网关已按 `role=admin` 校验，管理端令牌签发待接入）
- App 客户端工程
- 管理后台真实指标和图表
- 离线打卡同步队列
- 内容审核、运营配置和数据导出
- 消息和社区实时通信业务

## 安全注意事项

生产部署前必须修改以下内容：

- 确认网关鉴权保持启用，并在数据库中把管理账号的 `role` 更新为 `admin`
- 为网关和认证服务配置相同且足够强的 `JWT_SECRET`
- 将 CORS 来源收敛为实际域名
- 修改 MySQL、Redis、Nacos 和 Sentinel 凭据
- 禁用或限制未使用的管理接口与运维端口
- 检查日志脱敏和数据导出权限
- 使用独立配置值替代所有开发环境默认值
- 不要提交 `.env`、密钥和生产数据库凭据

## 常用命令

```bash
# 后端完整验证
mvn -B -ntp verify

# 前端类型检查与构建
cd atifahCBC-admin-web/frontend
npm run build

# 启动中间件
docker compose -f deploy/docker-compose/docker-compose.yml up -d

# 启动全部后端容器
docker compose -f deploy/docker-compose/docker-compose.app.yml up -d --build

# 查看后端容器状态
docker compose -f deploy/docker-compose/docker-compose.app.yml ps

# 查看网关日志
docker compose -f deploy/docker-compose/docker-compose.app.yml logs -f gateway

# 停止后端容器
docker compose -f deploy/docker-compose/docker-compose.app.yml down
```