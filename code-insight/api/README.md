# code-insight/api/ — 扩展契约

`code-insight/` 的契约层：只声明服务、数据类型与扩展点，不承载具体能力实现。IDE / LSP 与第三方依赖本模块调用 code-insight 能力（quick fix、成员生成、补全服务）。本层只消费 Analysis API 契约与 IntelliJ Platform API，不拥有解析。

## 关键包

| 包 | 职责 |
|---|---|
| `org.cangnova.cangjie.codeinsight.api.applicators.fixes` | quick-fix factory 协议（含优先级）、诊断 → factory 列表、registrar 扩展点与聚合服务 |
| `org.cangnova.cangjie.codeinsight.api.members` | 成员生成窄接口 `CangJieMemberGenerationService`（可生成成员 = 身份 + 已渲染声明文本）与项目级注册表 |
| `org.cangnova.cangjie.ide.completion.api` | 补全服务契约（`CangJieCompletionService` / `CompletionRequest` / `CompletionSnapshot`）、项目级服务注册表与期望类型过滤契约 |
| `org.cangnova.cangjie.ide.completion.api.lookup` | 补全候选数据类型：身份、导入计划、插入、插入前复查与恢复、排序数据、符号形态 |
| `org.cangnova.cangjie.ide.completion.api.statistics` | 补全管线阶段（setup / context / candidates / insert / resolve 等）的测量契约 |

## 依赖

- `:analysis:analysis-api`
- `:psi`
- `:common`

契约类型会引用 Analysis API 类型（如 quick-fix / 成员生成以 `CaSession` / `CjTypeStatement` 为参数）；补全契约直接承载仓颉的 `ImportPath`（含组织名与别名），不另造平行字符串结构。语法层 `syntax` 不允许依赖本模块，保证「不需要 resolve 的能力」在编译期就拿不到 Analysis 类型。

## 命令

```bash
./gradlew :code-insight:api:assemble
./gradlew :code-insight:api:test
```

## 相关文档

- `../README.md` — Code Insight 总览
- `../../docs/module-catalog.md` — 模块清单
