# cfir/providers/ — 符号与语义 Providers

本模块实现源码、内建与组合 provider、成员 scope，以及仓颉专有的 extend、CJMP 和导出语义服务。共享契约及模型操作由 [semantics.api](../semantics.api/README.zh-CN.md) 拥有，模块划分对位 Kotlin K2 FIR。

依赖方向为 `providers → semantics.api → cfir-tree → cfir-cones`。providers 依赖语义接口层，resolve、checkers、entrypoint 和 analysis 消费各层的实际契约与实现。

## 职责与归属

| 包／模块 | 职责 |
| --- | --- |
| 本模块的 `cfir.resolve.providers` | `CfirProviderImpl` 源码索引、内建及组合 symbol provider、名称聚合、导出分析与冻结 SOURCE 输入。 |
| 本模块的 `cfir.scopes`、`cfir.scopes.impl` | 包与成员 scope、实际化及继承消费、typealias 构造器 scope。 |
| 本模块的 `cfir.resolve.providers.macro` | pre-macro 与 construction 产品、一次性文件登记、注解元数据和宏来源事实。 |
| `:cfir:semantics.api` | `CfirProvider`、`CfirSymbolProvider` 等共享契约；共同签名复制、class substitution scope、类型上下文、替换及继承查询产品。 |
| `:cfir:cfir-serialization` | 反序列化 provider、原始 CJO 包快照及 CJO 读写。 |
| `:cfir:resolve` | 正式语义阶段、语义服务实现和解析后的状态及绑定产品。 |

`symbolProvider`、`cfirProvider`、`dependenciesSymbolProvider` 与 dependency provider key 从 `cfir.resolve.providers` 导入；它们的声明由 semantics.api 下层持有。extend 等 session accessor 保持 `cfir.session` 的真实归属。

## 来源、状态与导出

`CfirProviderImpl` 通过正式 construction 产品完成一次性文件登记并封口，源码符号与声明归属索引来自同一批产品。依赖 symbol provider 与本模块源码 provider 组合，保留各自的名称、文件和声明身份。

- `:cfir:cfir-tree`、`:cfir:cfir-cones`
- `:common`、`:util`、`:resolution.common`

内建声明创建时发布 resolved 状态。共同签名复制要求已完成的状态值，参数副本拥有独立 symbol 和 callable owner；状态解析仍由其生产阶段负责。

`CfirExportAnalysis` 与 `CfirExportAnalysisInputSnapshot` 捕获完整声明、类型、来源边、实际参数／binding 向量、CJO origin 和文件归属。发布前核验同代身份；SOURCE 正文根查询严格要求当前捕获的源码归属。CJO loader 的导入事实与原始序列化属性分别保存，库声明的原始 `IMPORTED=false` 不授予 SOURCE 资格。

`CfirExportSourcePayload` 保存当前 CFIR 的节点角色、声明／表达式槽、有序实参绑定、控制目标及引用事实。位置和 literal 拼写在捕获时取值；常量、注解视图及穷尽性列表独立冻结，常量对象使用身份 memo 保留共享和循环。节点与符号只作本代身份，消费者按冻结产品查询，不能在格式层重新解析、读取活动 mapping 或生成默认 helper。CFIR payload 不等同于原生 CJO canonical 节点，完整正文 codec 的状态由滚动审计维护。

CJO 写出消费冻结分析产品。普通导入目标使用外部 FullId；显式 common part 继续走定义和具体化索引。完整正文 codec、默认 helper 生产及最终全域验收的状态见 [CJMP 滚动审计](../../docs/cjmp-progress-audit-20261008.md)。

## 验证

从仓库根目录使用排队入口：

```powershell
.\gradlew-queue.bat :cfir:providers:assemble
.\gradlew-queue.bat :cfir:providers:test
.\gradlew-queue.bat :cfir:providers:test --tests "*CfirExportAnalysisInputSnapshotTest" --tests "*CfirFakeOverrideGeneratorTest"
```

相关模块表见 [模块目录](../../docs/module-catalog.md)。
