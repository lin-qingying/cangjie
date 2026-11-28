# CFIR 语义 API

[English](README.md)

本模块拥有 providers、resolve、checkers 和 analysis 共用的语义契约及模型操作，模块位置对位 Kotlin FIR 的 `semantics.api`；具体操作以仓颉声明和类型的真实语义为边界。

依赖方向为 `providers → semantics.api → cfir-tree → cfir-cones`。语义契约依赖树模型，解析器、源码索引、二进制加载器和检查器的实现分别由各自模块拥有。

| 包 | 职责 |
| --- | --- |
| `cfir.resolve.providers` | 符号与声明 provider 契约、名称和 import namespace、声明元数据查询、超类型与 extend 契约、callable owner 替换。 |
| `cfir.scopes.impl` | 共同签名复制、class substitution scope、继承身份及 provenance、lookup 完整性契约。 |
| `cfir.resolve.substitution` | Cone 替换接口与实现，包括 symbol 身份映射、constructor 身份映射和替换链。 |
| `cfir.types`、`cfir.resolve` | 类型上下文、递归类型操作、lookup tag 绑定、alias 展开和语义超类型查询。 |
| `cfir.declarations`、`cfir.expressions` | 声明 overloadability 契约及已解析注解的语义事实。 |
| `cfir.session` | 契约属于本层的语义服务 session 访问入口。 |

`symbolProvider`、`cfirProvider`、`dependenciesSymbolProvider` 和 dependency provider key 的实际声明位于 `cfir.resolve.providers`。消费者从该包导入；其他 session accessor 保持各自的真实包归属。

[CfirFakeOverrideGenerator](src/org/cangnova/cangjie/cfir/scopes/impl/CfirFakeOverrideGenerator.kt) 与 [CfirClassSubstitutionScope](src/org/cangnova/cangjie/cfir/scopes/impl/CfirClassSubstitutionScope.kt) 共用签名复制。副本拥有新的 callable 和参数 symbol，重绑定自有类型参数及显式返回类型，保持 ordinal 和 provenance，并严格要求声明状态已经解析。签名副本不共享可执行正文；构造器的默认值存在性桩保留默认合同，不生产可执行默认辅助函数。

`CfirExtendProvider.modificationCount` 必须由实现提供。源码和惰性 CJO 供应器发布实际完成索引版本，明确不可变的空供应器显式声明零。类型感知父类型查询分别保存 extend 与 graph 版本，在解析回调后及缓存发布前复核。

树模块拥有正式生成的声明 builder、状态值和 expression stub 实现。SOURCE 导出分析及其冻结输入仍由 [providers](../providers/README.md) 拥有；CJO 编码与二进制包快照属于 `:cfir:cfir-serialization`。

构造器身份映射通过 `createTypeSubstitutorByTypeConstructor` 使用实际类型上下文，声明符号映射通过 `substitutorByMap` 使用实际会话。两个工厂统一位于 `cfir.resolve.substitution`，共用递归替换与属性处理；原 `cfir.types.CfirTypeSubstitutorByMap` 已删除。完整 SOURCE payload、默认 helper 生命周期及其余 LL／Analysis API 验收继续记录在 [CJMP 审计](../../docs/cjmp-progress-audit-20261008.md) 中。

Windows 使用全局排队入口：

```powershell
.\gradlew-queue.bat :cfir:semantics.api:assemble
.\gradlew-queue.bat :cfir:providers:test --tests "*CfirFakeOverrideGeneratorTest"
```
