---
description: 构建与测试验证：跑真实命令并如实报告结果
argument-hint: "[模块路径，如 atifahCBC-services/mood-service；缺省为全量]"
---

# 验证：$ARGUMENTS

只跑命令、读结果、报结论。不要为了让结果变绿而改测试或跳检查。

## 执行顺序

1. `mvn -o -DskipTests compile`
   - `-o` 因本地缺依赖失败 → 去掉 `-o` 重试，并记录这一情况。
2. 指定模块时：`mvn -o test -pl <模块路径> -am`；否则 `mvn -o test`。
3. 涉及 `atifahCBC-common/**` 时必跑全量 `mvn -o test`。
4. 涉及前端：`cd atifahCBC-admin-web/frontend; npm run build`（首次 `npm ci`）。
5. 涉及网关/服务启动行为时，可另跑 `mvn -B -ntp verify`，但说明它不等同于 CI（仓库未接 git，CI 未实际执行）。

## 报告格式

```text
命令：<实际执行的命令>
结果：PASS / FAIL
耗时：
用例：共 N，失败 M（列出失败用例名）
关键输出：<前 15 行有用信息>
```

失败时给根因判断（编译错误/断言失败/环境缺失/依赖缺失）与最小修复建议；不要顺手改代码，除非用户要求。
