# 测试规则

## 基线

- 现有测试全绿：`mvn -o test`（auth-service 60+ 用例、gateway 16 个、common 各模块少量）。这是任何改动前的对照基线。
- 技术栈：JUnit 5 + Mockito + AssertJ（`spring-boot-starter-test` 已引入），Web 层用 `@WebMvcTest` + `MockMvc`。

## 写法

- 纯逻辑优先纯单测：`@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks`，不启 Spring 上下文（参考 `LoginFailureGuardTest`、`TokenFactoryTest`）。
- 控制器测试用 `@WebMvcTest(XxxController.class)` + `@MockitoBean`（Spring Boot 3.4+ 用 `@MockitoBean`，不要用已弃用的 `@MockBean`）。
- 命名：`XxxTest`；测试方法用中文或 `method_shouldXxx_whenYyy` 描述**行为**，不写 `test1`。
- 每个用例覆盖：成功路径、参数非法、业务冲突、并发/重复调用、边界（过期、空值、超限）。
- 断言用 AssertJ（`assertThat`），不要只断言 `assertNotNull` 就算覆盖。
- 不连真实 MySQL/Redis/RocketMQ/Nacos；外部依赖全部 mock。集成测试如确需，放到独立的 `*IT` 命名并说明如何运行。

## 覆盖要求

- 新增业务代码必须带测试；改公共模块（common-*）必须跑该模块测试并说明是否影响其他服务。
- 修 bug 先补一个能复现的失败用例，再改实现。
- 纯骨架服务（只有启动类）不要求造测试；开始写业务时同步建测试目录。

## 验证顺序

1. `mvn -o -DskipTests compile` —— 编译与依赖
2. `mvn -o test -pl <module> -am` —— 改动模块
3. `mvn -o test` —— 全量回归（公共模块必做）
4. 前端改动追加 `npm run build`

报告结果时必须贴真实输出摘要（用例数、失败项），失败就如实说失败。
