# 当前 CFIR 语义分析相对官方仓颉编译器的实现程度评估

> 日期：2026-04-08
>
> 对比对象：当前项目 `cfir/resolve` × 官方仓颉 C++ 编译器 `external/cangjie_compiler`
>
> 目标：回答一个工程化问题——**当前项目的 CFIR 语义分析，离官方实现大约还有多远？**

---

## 一、结论摘要

当前项目的 CFIR 语义分析已经不是“只有框架”的阶段，而是一个**主干已成、细分语义仍在追平官方**的系统。

如果以官方仓颉 C++ 编译器前端语义能力作为 100% 基线，基于当前仓库中的源码、phase 注册、测试矩阵和 gap 文档综合判断：

- **整体完成度：70%–80%**
- 更贴近的中心值：**约 75%**

这个判断的依据不是 README 的状态自报，而是以下三类事实同时成立：

1. `SUPER_TYPES`、`TYPES`、`EXTENSIONS`、`IMPLICIT_TYPES`、`BODY_RESOLVE` 的主干实现已经真实存在。
2. `cfir/resolve/test` 与 `cfir/analysis-tests/testData/diagnostics*` 已经提供了可观的算法级和端到端语义回归面。
3. 项目仍存在明显的 phase 收口与语义域覆盖差距，尤其是 `CHECKERS` 尚未真正并入 resolve phase 终态，以及多个官方稳定语义域仍以 placeholder / 薄覆盖形式存在。

一句话概括：

> **当前项目已经做出了“可工作的 CFIR 语义分析主系统”，但还没有达到“与官方语义面完整对齐”的收官状态。**

---

## 二、评估方法与证据来源

本文仅基于当前仓库已读到的代码与文档证据，不依赖 README 的自报状态做结论。

### 1. 当前项目代码证据

- 总入口：`cfir/entrypoint/src/org/cangnova/cangjie/cfir/pipeline/analyse.kt`
- 总 resolve 驱动：`cfir/entrypoint/src/org/cangnova/cangjie/cfir/pipeline/CfirTotalResolveProcessor.kt`
- phase 枚举：`cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirResolvePhase.kt`
- phase 注册：`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirResolveProcessors.kt`
- checkers phase 占位：`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirCheckersResolveProcessor.kt`
- 典型核心实现：
  - `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirSupertypesResolution.kt`
  - `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt`
  - `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/CfirTypeResolver.kt`
  - `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirBodyResolveTransformer.kt`
  - `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirCallResolver.kt`
  - `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/inference/CfirCallCompleter.kt`
  - `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/services/CfirExtendIndexStore.kt`
  - `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/services/CfirTypeAwareSupertypeProviderImpl.kt`

### 2. 当前项目测试与盘点证据

- 端到端诊断测试：
  - `cfir/analysis-tests/tests-gen/org/cangnova/cangjie/cfir/analysis/tests/CfirAnalysisDiagnosticsTestGenerated.kt`
  - `cfir/analysis-tests/tests-gen/org/cangnova/cangjie/cfir/analysis/tests/CfirAnalysisDiagnostics2TestGenerated.kt`
- 语义矩阵 testdata：
  - `cfir/analysis-tests/testData/diagnostics/**/*`
  - `cfir/analysis-tests/testData/diagnostics2/**/*`
- 覆盖缺口盘点：`cfir/analysis-tests/diagnostics-coverage-gap-vs-cpp.md`
- `diagnostics2` 说明：`cfir/analysis-tests/testData/diagnostics2/README.md`

### 3. 官方仓颉 C++ 语义基线

- 阶段定义：`external/cangjie_compiler/include/cangjie/Frontend/CompilerInstance.h`
- 语义主入口：`external/cangjie_compiler/include/cangjie/Sema/TypeChecker.h`
- 语义主编排：`external/cangjie_compiler/src/Sema/TypeChecker.cpp`
- 三段式骨架：`external/cangjie_compiler/src/Sema/TypeCheckerImpl.h`
- 预检查 / 名称解析：`external/cangjie_compiler/src/Sema/PreCheck.cpp`
- 引用 / this-super / 泛型替换：`external/cangjie_compiler/src/Sema/TypeCheckReference.cpp`
- 扩展语义：`external/cangjie_compiler/src/Sema/TypeCheckExtend.cpp`
- 诊断定义与帮助函数：`external/cangjie_compiler/src/Sema/Diags.h`
- 增量分析：
  - `external/cangjie_compiler/include/cangjie/IncrementalCompilation/IncrementalScopeAnalysis.h`
  - `external/cangjie_compiler/src/IncrementalCompilation/IncrementalScopeAnalysis.cpp`
  - `external/cangjie_compiler/src/IncrementalCompilation/ASTDiff.cpp`

---

## 三、已确认事实、推断判断与已知矛盾

为了让本文后续可维护，这里明确区分三类信息：

### 1. 已确认事实（直接来自已读代码/文档）

以下内容属于“已读到源码或文档后可以直接确认”的事实：

- `analyse.kt` 当前执行模型是：先 `runResolution(...)`，再单独 `runCheckers(...)`。
- `CfirResolveProcessors.kt` 当前只注册到 `BODY_RESOLVE`。
- `CfirCheckersResolveProcessor.kt` 当前为空文件。
- `CfirResolvePhase.kt` 的文档注释仍把 `CHECKERS` 叙述为 resolve 终态，但实际枚举值止于 `BODY_RESOLVE`。
- `cfir/resolve/test` 已覆盖调用解析、重载冲突、约束系统、类型关系、extend provider 等内部语义核心。
- `cfir/analysis-tests/testData/diagnostics` 与 `diagnostics2` 已覆盖 imports、inheritance、extensions、calls、constructor、effects、range、throw、try、visibility 等多个语义域。
- `diagnostics-coverage-gap-vs-cpp.md` 明确记录了 checker 链路空位、薄语义域和若干官方语义缺口。

### 2. 推断判断（基于事实的综合结论）

以下内容不是单文件直接写明的事实，而是基于多个证据综合得出的判断：

- 当前项目整体成熟度约为 **70%–80%，中心值约 75%**。
- `SUPER_TYPES`、`TYPES`、`BODY_RESOLVE` 已达到“主干较成熟”的级别。
- `STATUS` 更像“phase 在，但闭环偏弱”的区域。
- `EXTENSIONS` 是“phase 较轻、语义整体较强”，而不是 transformer 自身特别厚重。
- 当前最大差距不是没有语义分析，而是 `CHECKERS` 的 phase 收口和多个语义域的最终闭环。

### 3. 已知矛盾（设计与实现不一致的地方）

以下矛盾已经明确存在，且值得后续优先收敛：

| 设计/文档表述 | 当前代码现实 | 影响 |
|---|---|---|
| `CHECKERS` 是 `CFIR_RESOLVE` 的最后 phase | resolve pipeline 实际止于 `BODY_RESOLVE`，checkers 独立运行 | 终态定义不一致 |
| `CfirResolvePhase` 文档把 `CHECKERS` 视为枚举末尾 | 实际枚举中没有 `CHECKERS` 枚举值 | 设计与 API 失配 |
| 文档强调同一 phase 模型统一管理 resolve + check | 当前实现仍是 resolve / checker 两段式 | lazy resolve / analysis 语义边界模糊 |

这三类信息之所以要分开写，是为了避免后续维护时把“硬证据”和“工程判断”混成一层，导致文档越写越难更新。

---

## 四、官方 baseline：SEMA 到底包含什么

官方仓颉 C++ 编译器并不是把语义分析理解为“最后跑一遍 checker”。

从 `CompilerInstance.h` 与 `TypeCheckerImpl.h` 可见，官方前端的相关主线是：

```text
AST_DIFF
  ↓
SEMA
  ├─ PreTypeCheck
  ├─ DoTypeCheck
  └─ PostTypeCheck
  ↓
DESUGAR_AFTER_SEMA
  ↓
GENERIC_INSTANTIATION
  ↓
OVERFLOW_STRATEGY
```

其中 `SEMA` 内部实际覆盖：

- 声明预检查与重定义检查
- 名称解析 / declMap 构建 / 作用域收集
- super / type alias / generic 相关前置处理
- `extend` 语义检查
- 隐式类型推断
- 函数体类型检查与调用解析
- 语义诊断积累

换句话说，当前项目把官方 `SEMA` 拆成 `CFIR_BUILD + CFIR_RESOLVE`，再进一步把 `CFIR_RESOLVE` phase 化，是**合理的工程化重构**，不是脱离官方事实的空想。

---

## 五、官方 `SEMA` 到本地 `CFIR_RESOLVE` 的映射关系

虽然官方 C++ 编译器没有把 `SEMA` 内部直接公开成与本地完全同构的 phase 枚举，但从 `TypeChecker`、`PreCheck`、`TypeCheckReference`、`TypeCheckExtend` 等源码职责可以得到一张比较稳定的映射表。

| 官方语义子过程 | 官方主要位置 | 本地对应 phase / 模块 | 当前对齐度 |
|---|---|---|---|
| 声明预检查 / declMap / 重定义检查 | `PreCheck.cpp`、`TypeCheckerImpl.h` | `CFIR_BUILD` + `IMPORTS` 前置准备 | 中高 |
| 导入与符号候选空间建立 | `ImportManager`、`Collector.cpp` | `IMPORTS` | 高 |
| super / inheritance 图准备 | `PreCheck.cpp`、`TypeCheckerImpl.h` | `SUPER_TYPES` | 高 |
| 显式类型引用解析 | `TypeCheckReference.cpp` | `TYPES` | 高 |
| 修饰符 / 状态约束整理 | `PreCheck.cpp` 中的部分规则 | `STATUS` + checker 层 | 中 |
| `extend` 合法性与约束检查 | `TypeCheckExtend.cpp` | `EXTENSIONS` + extend services | 高 |
| 声明边界隐式类型推断 | `TypeChecker.cpp` 中的 `Synthesize(...)` / 返回类型推导 | `IMPLICIT_TYPES` | 中高 |
| 函数体检查 / 调用解析 / 重载决议 | `TypeChecker.cpp`、`TypeCheckCall.cpp`、`TypeArgumentInference.cpp` | `BODY_RESOLVE` | 中高 |
| 语义诊断积累与最终校验 | `Diags.h` + 各 `TypeCheck*` 文件即时诊断 | 设计上应为 `CHECKERS`，现实中是 `runCheckers(...)` 独立 pass | 中 |

这张映射表背后的关键判断是：

1. **本地 phase 切分总体合理。** 当前 `CFIR_RESOLVE` 的分层并没有脱离官方实际语义结构，反而更接近 Kotlin K2/FIR 的工程化组织方式。
2. **最不对齐的地方不是 `SUPER_TYPES` 或 `BODY_RESOLVE`，而是 `STATUS` 和 `CHECKERS` 的最终收口方式。**
3. **本地在 `IMPLICIT_TYPES` 上比官方更“phase 化”**，但这并不意味着更成熟；它只是把官方内嵌在 `TypeChecker` 里的推断行为显式拆了出来。

---

## 六、当前项目的真实状态：不是“有没有”，而是“做到几成”

当前项目的语义分析主干可以简化成：

```text
CFIR_BUILD
  ↓
CFIR_RESOLVE
  ├─ IMPORTS
  ├─ MACRO_EXPAND
  ├─ SUPER_TYPES
  ├─ TYPES
  ├─ STATUS
  ├─ EXTENSIONS
  ├─ IMPLICIT_TYPES
  └─ BODY_RESOLVE
       ↓
runCheckers(...)
```

这已经明显不是“只有框架”，因为：

- 有真正的 `CfirTotalResolveProcessor`
- 有 phase 注册表和 per-phase processor
- 有真实的 type / supertype / call / inference / extend 实现
- 有 checker 组件与 collector 链
- 有端到端诊断测试和内部单元测试

当前最核心的判断不是“是否存在语义分析”，而是：

> **resolve 主干存在且已能工作，但 phase 终态、checker 集成与官方语义面闭环仍未完全收束。**

---

## 七、逐 phase 成熟度对照

### 总表

| Phase | 当前状态 | 关键证据 | 与官方相比的判断 |
|---|---|---|---|
| `IMPORTS` | 已实装 | `CfirResolveProcessors.kt`、`CfirImportsResolveProcessor.kt`、`diagnostics/coverage/imports/*` | 接近官方 |
| `MACRO_EXPAND` | 已接线，但非本文主评估重点 | `CfirResolveProcessors.kt` 注册了 `CfirMacroExpandResolveProcessor` | 已有接缝 |
| `SUPER_TYPES` | 较成熟 | `CfirSupertypesResolution.kt`、`CfirTypeAwareSupertypeProviderTest.kt` | 接近官方主干 |
| `TYPES` | 较成熟 | `CfirTypeResolveTransformer.kt`、`CfirTypeResolver.kt` | 接近官方主干 |
| `STATUS` | 部分实装 | `CfirStatusResolveProcessor.kt` 存在，但 phase 内语义变换偏轻 | 有 phase，但闭环偏弱 |
| `EXTENSIONS` | 语义较成熟，phase 形态偏轻 | `CfirExtensionsResolveProcessor.kt`、`CfirExtendIndexStore.kt`、`coverage/extensions/*` | 仓颉特有能力中较强 |
| `IMPLICIT_TYPES` | 引擎层较强，端到端覆盖仍不均 | `CfirImplicitTypesResolveProcessor`、constraint / inference tests | 主干已在，语言矩阵未满 |
| `BODY_RESOLVE` | 较成熟 | `CfirBodyResolveTransformer.kt`、`CfirCallResolver.kt`、calls/overloads/inference tests | 接近官方主干 |
| `CHECKERS` | 设计已定义，实际未 phase 化集成 | `CfirResolvePhase.kt` 注释、空的 `CfirCheckersResolveProcessor.kt`、`analyse.kt` 单独 `runCheckers(...)` | 当前最清晰的设计-实现断层 |

### 速查矩阵：phase × 代码 × 测试 × 主要风险

这张表更适合作为后续维护时的快速入口：

| Phase | 主代码入口 | 主要测试/证据 | 当前主要风险 |
|---|---|---|---|
| `IMPORTS` | `CfirImportsResolveProcessor.kt` | `diagnostics/coverage/imports/*` | 内部 processor 级专项回归仍相对薄 |
| `SUPER_TYPES` | `CfirSupertypesResolution.kt` | `CfirTypeAwareSupertypeProviderTest.kt`、inheritance/supertypes diagnostics | 与 typealias / 边界继承交互是否完全收口仍需持续验证 |
| `TYPES` | `CfirTypeResolveTransformer.kt`、`CfirTypeResolver.kt` | type relation / type-ref tests | 类型引用层 modifier / projection checker 回归仍偏薄 |
| `STATUS` | `CfirStatusResolveProcessor.kt` | declaration-status diagnostics | 规则更多落在 checker 层，phase 内闭环偏弱 |
| `EXTENSIONS` | `CfirExtensionsResolveProcessor.kt`、`CfirExtendIndexStore.kt` | `coverage/extensions/*`、extend provider tests | phase 自身较轻，语义分散在 services，后续易出现“实现位置分散”维护成本 |
| `IMPLICIT_TYPES` | `CfirImplicitTypesResolveProcessor`、inference internals | constraint / graph / completion tests、`diagnostics2/inference/*` | 引擎强于 source-level 语义矩阵 |
| `BODY_RESOLVE` | `CfirBodyResolveTransformer.kt`、`CfirCallResolver.kt` | call / overload / tower / arguments tests | 深层 call ambiguity、effects/throw/try 仍在补 |
| `CHECKERS` | `runCheckers(...)` in `analyse.kt` | `diagnostics*`、gap 文档 | 尚未 phase 化，默认注册链仍有空位 |

### 1. `IMPORTS`

这一阶段已经不是薄壳。

- 有实际 processor 注册。
- 有 import 相关 diagnostics 回归。
- 项目测试已经覆盖 unresolved import、alias conflict、import target not found 等常见失败模式。

与官方相比，这一块已经进入“**常见语义已对齐，剩余问题主要在细部与内部隔离测试**”的状态。

### 2. `SUPER_TYPES`

这是当前项目最成熟的 phase 之一。

- `CfirSupertypesResolution.kt` 本身体量和职责都比较完整。
- extend-aware supertype provider 也有测试保护。
- 继承 / override / super 相关 diagnostics 已经具备成体系回归。

这部分已经明显不是“待实现”，而是“**基本能承担官方主干职责**”。

### 3. `TYPES`

显式类型解析也已经相当成型。

- `CfirTypeResolveTransformer.kt`
- `CfirTypeResolver.kt`

配合 type relation / join-meet / type-ref extension 等测试，可以认为声明头部类型解析已经具备相当实用性。

### 4. `STATUS`

这是一个“**phase 在，但闭环较弱**”的区域。

从结构上看：

- 有 `CfirStatusResolveProcessor`
- 有状态 / 修饰符相关 diagnostics 与 checker

但从本轮探索到的代码特征看，这一阶段的 phase 内变换更像是“状态锚点 + 后续 checker 补齐”，而不是像 `SUPER_TYPES`、`TYPES` 那样形成强语义闭环。

这意味着它是可用的，但成熟度不如类型和调用主干。

### 5. `EXTENSIONS`

这是当前项目最值得肯定的仓颉特有能力之一。

虽然 `CfirExtensionsResolveProcessor.kt` 本身不算很厚，但真正的语义实现并不薄：

- `CfirExtendIndexStore.kt`
- `CfirTypeAwareSupertypeProviderImpl.kt`
- 大量 `coverage/extensions/*` 与 provider/service tests

因此对 `EXTENSIONS` 更准确的描述不是“phase 很强”，而是：

> **phase 本身较轻，但 extension 语义整体已经比较成熟。**

### 6. `IMPLICIT_TYPES`

这里的成熟度很容易被误判。

如果只看端到端 testdata，会觉得这块还在补；但如果看 `cfir/resolve/test`，会发现：

- constraint system
- constraint graph / store / completion
- inference logging

这些内部引擎已经不弱。

所以更准确的判断是：

- **推断引擎内部：较强**
- **语言级推断语义矩阵：仍不均衡**

### 7. `BODY_RESOLVE`

这是当前项目另一块明显成熟的主干。

- `CfirBodyResolveTransformer.kt`
- `CfirCallResolver.kt`
- `CfirCallCompleter.kt`
- calls/overloads/arguments/tower/inference 等一整组测试

它还不是“官方全覆盖”，但已经能承担真正的 body resolve 主体工作，而不是示意性代码。

### 8. `CHECKERS`

这是当前最关键的结构性短板。

`CfirResolvePhase.kt` 的文档把“到达 `CHECKERS`”描述为 resolve 终态，但当前真实代码并非如此：

- `CfirResolvePhase` 枚举实际上止于 `BODY_RESOLVE`
- `CfirResolveProcessors.kt` 也只注册到 `BODY_RESOLVE`
- `CfirCheckersResolveProcessor.kt` 目前是空文件
- `analyse.kt` 明确在 `runResolution(...)` 之后单独调用 `runCheckers(...)`

这意味着当前系统的真实模型是：

```text
resolve pipeline 到 BODY_RESOLVE 结束
  ↓
额外执行 checker pass
```

而不是文档设计中的：

```text
resolve pipeline 一直推进到 CHECKERS 终态
```

这正是当前“离官方/离设计闭环还差一步”的最直观证据。

---

## 八、测试面成熟度：为什么说它已经不是早期原型

当前测试面可以分成两层。

### 1. 算法 / 内部单元测试

主要集中在 `cfir/resolve/test`，覆盖：

- overload conflict
- call reduction / map arguments / map type arguments
- constraint system / graph / completion / store
- type relations / join / meet
- extend provider / supertype provider

这说明内部语义引擎已经具有**可被单独验证的算法骨架**。

### 2. 端到端语义诊断测试

主要集中在：

- `cfir/analysis-tests/testData/diagnostics`
- `cfir/analysis-tests/testData/diagnostics2`

覆盖域包括：

- imports
- inheritance / supertypes
- extensions
- type mismatch
- calls
- constructor
- declaration-status
- initialization
- effects
- match / pattern
- mut
- range
- throw / try
- visibility

这已经不是“能跑几个 smoke case”的规模，而是**真实开始按语义域组织回归面**。

---

## 九、为什么它还不能说接近 100%

### 1. `CHECKERS` 还没有真正 phase 化

这是最清楚的架构闭环缺口，影响的不只是美观，而是：

- resolve 终态定义
- lazy resolve 契约
- IDE / analysis 模型的一致性
- phase 级测试与诊断收口方式

### 2. checker 层与 resolver 层覆盖不平衡

`cfir/analysis-tests/diagnostics-coverage-gap-vs-cpp.md` 明确指出：

- `memberDeclarationCheckers` 当前为空
- `invalidDeclarationCheckers` 当前为空
- `CommonTypeCheckers` 仍很薄
- `CommonLanguageVersionSettingsCheckers` 当前为空

这类问题不是“少几个测试文件”，而是**默认 checker 链路仍有结构性空位**。

### 3. 多个官方稳定语义域仍在“薄覆盖 / placeholder”阶段

`diagnostics2/README.md` 与 gap 文档都表明，当前仍在推进中的薄区包括：

- 调用绑定深水区（普通调用歧义、函数引用歧义、参数名不匹配）
- effects 的完整 handler / resumption 语义
- `range` / `jump` / `throw` / `try`
- inference 的语言级矩阵
- 更深的 interop / `inout`

因此现在的短板不是“核心不会算”，而是：

> **很多官方语义已经进入项目视野，但尚未全部收成稳定的 producer + checker + runnable regression matrix。**

---

## 十、成熟度拆分：为什么整体是约 75%

把整体成熟度拆成几个维度来看，会更容易理解这个结论。

| 维度 | 当前判断 | 说明 |
|---|---|---|
| 架构完成度 | **约 85%** | `CFIR_BUILD + CFIR_RESOLVE` 主体清晰，phase 基本齐全，但 `CHECKERS` 终态未闭环 |
| 核心 resolve 引擎完成度 | **约 80%** | super types / types / call / inference / extend 主体已经存在 |
| 端到端语义覆盖完成度 | **约 65%–75%** | 测试域广，但多个官方语义域仍薄或 placeholder |
| checker 闭环完成度 | **约 60%** | checker 组件很多，但 phase 集成和默认注册仍有明显空位 |

综合下来，当前项目最合理的整体判断仍然是：

```text
70%–80%，中心值约 75%
```

这个分值不是“功能数 / 文件数”的估计，而是基于：

- 主干 phase 是否真实可用
- 最难的语义引擎是否已落地
- 测试是否已经形成回归面
- 是否仍存在结构性短板

---

## 十一、成熟度评分口径

为了避免“75%”变成纯主观印象，这里明确本文采用的评分口径。

### 1. 评分并不是“文件存在率”

下列情况**不会**直接算作高完成度：

- 只有空 processor 或空 checker 容器
- README / 设计文档写成“已实现”，但没有代码闭环
- 只有 placeholder 测试文件，没有 runnable regression

### 2. 评分主要看四件事

| 评分维度 | 权重倾向 | 判断方式 |
|---|---|---|
| 架构与执行模型 | 高 | phase 是否真实注册并参与主流程 |
| 核心语义引擎 | 最高 | 是否有真实 resolver / inference / call resolution / supertype logic |
| checker 与诊断闭环 | 中高 | 是否默认接线、是否形成统一语义终态 |
| 测试与回归面 | 高 | 是否有算法级单测 + 源码级 inline diagnostics |

### 3. 本文的百分比分义

```text
0%–30%   仅有框架或接口
30%–50%  主流程可跑，但核心语义弱
50%–70%  核心语义存在，测试开始成形
70%–80%  主干稳定，语义域广，仍有明显缺口
80%–90%  与官方高度接近，仅剩边角和收尾
90%+     基本官方对齐
```

因此把当前项目放在 **70%–80%**，含义是：

> **核心语义主干已经成立，且已有相当测试面，但离“官方级完整语义面 + 完整 phase 收口”仍有明确距离。**

---

## 十二、下一步最值得优先收敛的方向

如果目标是更接近官方语义而不是只继续零散补点，优先级建议如下。

### P0：闭合 resolve 终态模型

优先解决 `CHECKERS` 是否真正并入 `CfirResolvePhase` 终态的问题。

这会直接影响：

- phase 契约与代码现实一致性
- lazy resolve 的语义终态定义
- 测试和 analysis API 对“已分析完成”的判断标准

建议拆成三个最小动作：

1. 决定 `CHECKERS` 是否继续保留在 `CfirResolvePhase` 设计中。
2. 若保留，则补齐：
   - `CfirResolvePhase` 末尾枚举值
   - `CfirResolveProcessors.kt` 注册
   - `CfirCheckersResolveProcessor.kt` 的真实处理逻辑
3. 统一更新 `analyse.kt`、lazy resolve 契约和相关测试预期，避免“文档说在 resolve 内，代码却在 resolve 外”。

### P1：补齐 checker 默认注册链路

优先修复“checker 已实现但默认不跑”的结构性空洞，例如：

- `memberDeclarationCheckers`
- invalid declaration checker 链

这类工作投入小、收益大，能把已有能力立即转化为稳定可见的语义输出。

建议优先处理两类最容易见效的点：

1. **已实现但默认不跑的 checker**
   - `CfirStaticModifierCompatibilityChecker`
   - `CfirMutModifierApplicabilityChecker`
2. **已有诊断名但缺直接名称级保护的语义**
   - `NO_CONSTRUCTOR`
   - `DEPRECATED_MODIFIER_*`
   - `MISMATCHING_HANDLE_BLOCK`

### P2：系统补薄语义域而不是继续零散补例子

最值得优先补齐的域：

1. `call/*` 的歧义与函数引用绑定
2. `effects/*` / `throw/*` / `try/*`
3. `range/*` / `jump/*`
4. inference placeholder 的实义化

进一步展开后，P2 更适合按“语义域矩阵”而不是“单诊断名清单”推进：

| 语义域 | 当前状态 | 建议推进方式 |
|---|---|---|
| `call/*` | 参数绑定多，候选选择少 | 优先补普通调用歧义、函数引用歧义、参数名不匹配 |
| `effects/*` | smoke coverage 为主 | 先补 `MISMATCHING_HANDLE_BLOCK`，再扩 handle/resumption/return-flow |
| `range/*` / `jump/*` | 已进入视野但仍薄 | 先补非法 loop control、range step 边界、元素类型不一致 |
| `throw/*` / `try/*` | 已有目录但未成矩阵 | 补 wrong throw type、catch type error、return-in-handle-block |
| `inference/*` | 引擎强，source-level 弱 | 把 placeholder 拆成每个推断分支一个稳定回归文件 |

---

## 十三、文档维护规则

本文不是一次性分析稿，而应被当作**阶段性基线文档**维护。

### 1. 什么时候必须更新这份文档

出现以下任一变化时，应至少复查本文中的“事实列表”“逐 phase 成熟度对照”和“P0/P1/P2 路线图”：

1. `cfir/resolve` 中新增、删除或重命名 resolve phase。
2. `analyse.kt`、`CfirTotalResolveProcessor.kt`、`CfirResolveProcessors.kt` 的执行模型发生变化。
3. `CfirCheckersResolveProcessor.kt` 从空壳变为真实实现，或 checkers 被正式并入 resolve phase。
4. `Common*Checkers` 的默认注册链路发生显著变化。
5. `cfir/analysis-tests/testData/diagnostics*` 新增一整类语义域目录，或 placeholder 大量收实。
6. 官方 `external/cangjie_compiler/src/Sema/*` 的对照结论被重新核实或修正。

### 2. 更新时优先改哪几段

不同类型变化，优先更新的章节不同：

| 变化类型 | 优先更新章节 |
|---|---|
| phase / 执行模型变化 | “已确认事实、推断判断与已知矛盾” + “逐 phase 成熟度对照” |
| checker 接线变化 | “已知矛盾” + “P0/P1 路线图” |
| 测试矩阵扩展 | “测试面成熟度” + “速查矩阵” + “成熟度拆分” |
| 官方语义重新对读 | “官方 baseline” + “官方 `SEMA` 到本地映射关系” |

### 3. 更新时的最小复核清单

每次更新本文，至少复核以下问题：

- `CfirResolvePhase.kt` 的实际枚举值是否仍与文档判断一致？
- `CfirResolveProcessors.kt` 是否仍只注册到 `BODY_RESOLVE`？
- `analyse.kt` 是否仍然单独调用 `runCheckers(...)`？
- `CfirCheckersResolveProcessor.kt` 是否仍为空或仅为占位？
- `diagnostics-coverage-gap-vs-cpp.md` 中记录的主要空位是否已经变化？
- `diagnostics2/README.md` 中 placeholder/backlog 的边界是否已经收缩？

### 4. 如何避免把文档写旧

更新时建议遵循这条顺序：

```text
先改“已确认事实”
  ↓
再改“推断判断”
  ↓
最后再调百分比和路线图
```

原因很简单：

- 事实层最稳定，也最容易复核
- 判断层依赖事实层
- 百分比和优先级是最容易被先入为主印象带偏的部分

换句话说，**不要先改“75% 变成 82%”，而应先确认事实层到底发生了什么变化。**

---

## 十四、推荐的阅读顺序

如果后续有人要继续推进这一方向，建议按如下顺序阅读仓库内容：

1. `docs/cfir-semantic-analysis-maturity-vs-official-2026-04-08.md`（本文）
2. `cjfir-compiler-stages.md` 中 `CFIR_RESOLVE` 一节
3. `cfir/entrypoint/.../analyse.kt`
4. `cfir/resolve/.../CfirResolveProcessors.kt`
5. `cfir/analysis-tests/diagnostics-coverage-gap-vs-cpp.md`
6. `cfir/analysis-tests/testData/diagnostics2/README.md`
7. 官方基线：`external/cangjie_compiler/src/Sema/*`

这样能先理解：

- 设计目标是什么
- 现实代码停在哪
- 测试面承认的缺口是什么
- 官方语义上限在哪里

---

## 十五、一句话总结

当前项目的 CFIR 语义分析已经具备真实可工作的主干能力，尤其在 `SUPER_TYPES`、`TYPES`、`EXTENSIONS`、`BODY_RESOLVE` 等核心环节上已经明显跨过“框架期”。

它与官方仓颉编译器的差距，已经不再主要体现为“有没有语义分析”，而是：

> **phase 终态是否真正闭合、checker 层是否完全接通、以及官方稳定语义域是否都已经收成可持续回归保护。**

因此，把当前状态描述为“**约 75% 完成度的、主干已成型的 CFIR 语义分析系统**”是比较贴近代码现实的说法。
