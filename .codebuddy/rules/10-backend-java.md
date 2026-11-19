# 后端编码规则（Java / Spring Boot）

## 分层与包结构

以 `atifahCBC-services/auth-service` 为唯一参考实现：

```
com.atifahCBC.<domain>
├── XxxServiceApplication.java
├── config/      @ConfigurationProperties、配置类
├── controller/  @RestController，只做校验与编排
├── dto/         入参 record + jakarta.validation
├── vo/          出参 record/不可变对象
├── service/     接口 + 协作者（幂等、限流、写库）
│   └── impl/    实现类
├── mapper/      MyBatis-Plus Mapper 接口
└── entity/      表映射实体，继承 BaseEntity
```

- 包名 `com.atifahCBC.<domain>`；artifactId 小写连字符（`mood-service`）。类名不加服务名前缀，除非与公共类冲突。
- 依赖注入用构造器：类上 `@RequiredArgsConstructor`，字段 `private final`。禁止字段 `@Autowired`。

## 返回与异常

- 控制器返回 `Result<T>` / `Result<Void>`；分页返回 `Result<PageResult<T>>`。
- 业务失败抛 `BusinessException`：`new BusinessException(ErrorCode.CONFLICT, "手机号已注册")`。
- 新增错误码只改 `ErrorCode` 枚举，遵循 `4xx00 / 5xx00` 段位；不要在业务代码里写裸数字。
- 需要特定 HTTP 状态时，优先复用 `GlobalExceptionHandler` 已有的码映射，不要新写 `@ExceptionHandler`。

## 校验与数据

- 入参用 `record` + `@NotBlank/@Pattern/@Size`，message 用中文，提示面向终端用户。
- 敏感字段（手机号、密码、令牌）不得进入日志与异常消息；日志只记 `userId`。
- 金额、计数用 `BigDecimal`/整型明确单位，不用 `double`。

## 事务与远程调用

- 事务只包住数据库写操作；Redis/RocketMQ/Feign 调用放在事务外。参考 `AuthServiceImpl` 把写库集中到 `LoginTokenWriter` 的短事务。
- 不要用“先查后插”保证唯一：依赖唯一索引 + `DuplicateKeyException` 兜底，再配合幂等键返回同一结果。
- 跨服务只能走 HTTP/Feign；禁止直连别的服务的库。

## 日志与可观测

- 统一用 `@Slf4j`；成功用 `info` 且带业务主键，失败用 `warn` 且说明原因，未预期异常用 `error(msg, ex)`。
- 不要打印整个请求体；`GlobalExceptionHandler` 对不可读请求体的处理就是范例。
- 依赖 MDC 里的 `traceId`/`userId`，不要自己造新的链路头；下游身份头是 `X-User-Id`/`X-Username`（网关写入）。

## 注释与文档

- 公共类、公共方法写中文 Javadoc，参数用 `@param`，与现有文件风格一致（`@author`、`@since`）。
- 关键取舍（为什么不用事务、为什么等时比对）写成简短注释，说明“为什么”而不是“做了什么”。

## 新增服务自检

1. `atifahCBC-services/pom.xml` 加 `<module>`
2. 服务 `application.yml`：端口、`spring.application.name`、Nacos group、库名、producer group
3. `Dockerfile` + `deploy/docker-compose/docker-compose.app.yml` 服务块
4. 网关 `spring.cloud.gateway.server.webflux.routes` 路由 + `deploy/sql/01-init-databases.sql` 建库
5. `README.MD` 端口表与路由表
