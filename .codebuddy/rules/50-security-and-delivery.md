# 安全与交付规则

## 安全红线

- 不提交密钥：`.env`、`*.pem`、`*.key`、生产凭据一律不进仓库（`.gitignore` 已忽略，仍要自查）。
- 开发默认值只用于本地：`JWT_SECRET` 默认串、MySQL `root/root`、`CORS_ALLOWED_ORIGINS=*` 在生产必须替换/收敛。
- 日志不得包含手机号、密码、令牌、Authorization、完整请求体。
- 鉴权默认开启；白名单只允许认证入口与健康/指标端点；管理端接口依赖 `role=admin`。
- 密码只存 BCrypt 哈希；比对时账号不存在也要等时比对（见 `AuthServiceImpl`）。
- 新增外部输入（路径、文件、URL、SQL 片段）必须校验与参数化，禁止字符串拼接 SQL。

## CI

`.github/workflows/ci.yml` 三个 job：后端 `mvn -B -ntp verify`（JDK 21）、前端 `npm ci && npm run build`（Node 22）、gitleaks 扫描。

- 改 CI 前先在本地跑等价命令，结果贴进说明。
- 不擅自放宽 CI（不删测试、不 skip、不加 `continue-on-error`）。
- 仓库尚未初始化 git，CI 只在推送后才会真正执行；不要声称“CI 已通过”，除非看到运行记录。

## 部署

两套方案（详见 `deploy/dev-deploy.md`）：

- 方案 A：JVM 直跑 jar（`java -jar <module>/target/...-SNAPSHOT.jar`）
- 方案 B：容器（`deploy/docker-compose/docker-compose.app.yml`，每模块一个 Dockerfile）

部署前检查：

1. 产物存在且是最新构建（`mvn clean package -DskipTests`）
2. Nacos 已发布 `common-application.yaml`（group `ATIFAHCBC_PUBLIC_GROUP`）
3. 数据库已按 `deploy/sql/01-init-databases.sql` 建库，Flyway 迁移随启动执行
4. 端口不冲突（同一端口不能同时有 JVM 与容器）
5. 容器内用服务名（`mysql`/`redis`/`nacos`）而不是 `127.0.0.1`
6. 关闭鉴权、放开 CORS、复用开发密钥都算**未完成部署**

部署后验证：`/actuator/health`（网关 8080、业务服务各自端口）、关键接口冒烟、日志无异常堆栈。

回滚：镜像/包按版本回退，配置回退到上一个 Nacos 版本；DB 变更保持向前兼容，回滚不依赖删列。

## 需要人工确认

`git push`、强推、`git reset --hard`、`mvn deploy`、`docker push`、`gh release`、`docker compose down -v`、删除卷/容器/目录。先说明影响，等确认再执行。
