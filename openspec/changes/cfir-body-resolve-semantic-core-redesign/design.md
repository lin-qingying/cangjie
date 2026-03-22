## 上下文

当前 CFIR 的 `BODY_RESOLVE` 语义核心仍建立在一组明显带有 Kotlin 内部抽象痕迹的约束系统、类型检查桥接层和调用补全路径之上。虽然这些实现为当前解析流程提供了可运行的中间形态，但它们并不能稳定表达官方仓颉 C++ 编译器的核心语义：双向类型检查、候选局部求解、类型兼容性分级、`QuestTy`、理想数值类型、`extend` 参与解析，以及泛型完备性检查。

现状中的主要问题集中在三个层面：

1. **语义中心错位**：`BODY_RESOLVE` 内部的表达式求解、调用补全与类型变量传播仍依赖 Kotlin 风格的约束系统对象图与补全路径。
2. **类型对比能力不足**：当前类型比较仍依赖 Kotlin 风格标记/上下文模型，难以自然表达仓颉官方 `TypeCompatibility` 的分级语义。
3. **调用补全边界不清**：`CfirCallCompleter` 当前更像在兼容旧补全体系，而不是作为 CFIR 原生 BODY 语义的直接补全入口。

这次变更的设计前提有两条必须同时成立：

- 保留 `ConeCangJieType` 及其主要具体子类作为 CFIR 统一类型表示。
- 在保留该类型表示前提下，重构其上的类型关系、类型变量、占位类型、约束传播和调用补全接入方式。

## 目标 / 非目标

**目标：**

- 重新定义 `BODY_RESOLVE` 的语义核心，使其与官方仓颉 C++ 编译器语义一致。
- 保留 `ConeCangJieType` 作为统一类型表示，明确哪些 `Cone*` 子类稳定保留、哪些过渡降级、哪些需要重新规划。
- 建立更贴合仓颉语义的类型关系系统，提供比单纯 `isSubtype` 更丰富的判定结果。
- 建立以候选局部求解为中心的约束系统，覆盖占位类型变量、构造子形状约束、`QuestTy` 回退、理想数值类型、`extend` 与泛型完备性。
- 重新定义 `CfirCallCompleter` 的补全边界，使其能够直接接入新的 BODY 语义体系。
- 保留 `CfirAbstractBodyResolveTransformer` 的 transformer / dispatcher 外壳，同时重构其内部组件装配关系。

**非目标：**

- 不新增新的顶层 `CfirResolvePhase`。
- 不引入平行于 `ConeCangJieType` 的全新 `CfirType*` 类型表示层。
- 不追求与 Kotlin FIR 内部 API 或对象命名保持兼容。
- 不在本设计中覆盖完整的数据流分析、smart cast 或所有 checker 规则实现。
- 不要求一次性删除所有现有过渡实现；允许兼容壳和迁移层在过渡阶段存在。

## 决策

### 决策 1：保留 `ConeCangJieType`，重构其上的语义系统

**选择：** 保留 `ConeCangJieType` 及其主要具体子类作为 CFIR 统一类型表示，不引入平行类型根。

**原因：**

- `ConeCangJieType` 已经是当前 CFIR 内部统一类型表示根。
- `BODY_RESOLVE`、checkers、type refs、symbol/type 写回路径都依赖这一表示。
- 真正的问题不在类型表示根，而在其上的比较、约束、变量状态和过渡推断产物。

**考虑过的替代方案：**

- **方案 A：重建一套 `CfirType*` 类型层次。**
  - 问题：会制造平行类型世界，破坏当前 CFIR 大量既有依赖。
- **方案 B：完全保留现有 `Cone*` 子类结构，只调整调用补全。**
  - 问题：无法解决类型关系、变量表示和中间推断产物的职责混乱。

### 决策 2：建立 `CfirTypeRelations`，替代现有类型比较中心

**选择：** 在 `BODY_RESOLVE` 内引入 `CfirTypeRelations`，统一承载子类型、类型兼容性、相等判断、构造子形状关系和 `extend` 关系判断。

**原因：**

- 仓颉官方语义并不只需要 `Boolean isSubtype`。
- 官方 C++ 通过 `TypeCompatibility` 明确区分 `INCOMPATIBLE`、`SUBTYPE`、`IDENTICAL`，这说明类型关系本身是分级语义。
- 调用解析、候选排序、泛型上界检查、`QuestTy` 回退和理想数值类型具体化都依赖 richer relation result。

**考虑过的替代方案：**

- **方案 A：继续依赖 `CfirTypeCheckerContext + ConeTypeContext + AbstractTypeChecker`。**
  - 问题：仍然会把仓颉语义压扁成 Kotlin 风格标记协议。
- **方案 B：只在重载排序阶段额外加补丁。**
  - 问题：类型比较能力仍然分散，无法形成统一语义中心。

### 决策 3：约束系统采用候选局部求解模型

**选择：** 每个调用候选建立自己的局部求解状态，并以此驱动补全、排序与结果写回。

**原因：**

- 与官方仓颉 C++ 的函数匹配 / 类型参数推断形态一致。
- 更利于解释候选为何失败、何时进入 `QuestTy` 回退、何时需要理想数值类型具体化。
- 更符合 `CfirCallCompleter` 的调用补全入口定位。

**考虑过的替代方案：**

- **方案 A：建立共享的全局约束系统。**
  - 问题：更容易引入候选间状态污染，也更接近 Kotlin 风格体系。
- **方案 B：只做“解析后补全”，不做候选局部求解。**
  - 问题：无法自然表达重载排序与候选可行性判断。

### 决策 4：保留 `CfirAbstractBodyResolveTransformer` 外壳，重构内部组件装配

**选择：** 保留 transformer / dispatcher 的遍历与分发外壳，不新增并行 body resolve 框架；重构 `BodyResolveTransformerComponents` 及其内部服务图。

**原因：**

- 当前 transformer 外壳承担的主要是遍历、分发、组件注入和写回编排，不直接绑定旧约束系统对象图。
- 真正需要变化的是 `subtypeChecker`、`conflictResolver`、`inferenceComponents`、`resolutionContext`、`callResolver` 这些内部组件组合。

**考虑过的替代方案：**

- **方案 A：推翻整个 `CfirAbstractBodyResolveTransformer` 架构。**
  - 问题：成本高，且与本次变更目标不成比例。
- **方案 B：完全不调整 components，只在 `CfirCallCompleter` 内做局部替换。**
  - 问题：会把新的语义核心压进错误的协作边界。

### 决策 5：`CfirCallCompleter` 成为新语义体系的直接接入点

**选择：** 由 `CfirCallCompleter` 直接接入 `CfirCallCompletion`，并消费新的候选局部求解结果。

**原因：**

- `CfirCallCompleter` 已经是 CFIR 现有调用补全入口。
- 它最适合作为“补全入口壳层”，而不是继续承担旧补全细节和旧约束存储操控。

**考虑过的替代方案：**

- **方案 A：让 `CfirExpressionsResolveTransformer` 直接管理全部补全过程。**
  - 问题：会扩大表达式 transformer 的职责，破坏分层。
- **方案 B：继续让旧 `ConstraintSystemCompleter` 主导补全。**
  - 问题：违背本次语义重建的核心目标。

## 风险 / 权衡

- **[风险] 新旧语义系统会在过渡期并存，导致命名与职责混杂。** → **缓解措施：** 明确 `ConeCangJieType` 保留、`ConeStubType`/`ConeCapturedType` 降级、`CfirTypeRelations`/`CfirConstraints` 成为新的语义中心。
- **[风险] `CfirAbstractBodyResolveTransformer` 组件装配改动过大，可能影响现有 body resolve 路径。** → **缓解措施：** 保留 transformer / dispatcher 外壳，仅重构 components 和补全协作链。
- **[风险] 若先重做约束系统而不重做类型关系系统，会继续保留错误的比较中心。** → **缓解措施：** 设计上将二者视为同一子系统的两个面，同步推进。
- **[风险] 保留 `ConeCangJieType` 可能被误读为完全保留当前 `Cone*` 子类结构。** → **缓解措施：** 规范明确区分稳定具体类型、过渡中间类型和待重新规划的推断产物。
- **[风险] `QuestTy`、理想数值类型和 `extend` 等仓颉特有语义可能继续被边缘化为特例补丁。** → **缓解措施：** 在类型关系、约束传播和候选排序需求中将其列为一等语义能力。

## 迁移计划

1. 先建立规范层契约，明确 `cfir-body-resolve-semantic-core`、`cfir-type-system`、`cfir-call-resolution` 的行为要求。
2. 在设计层明确 `CfirTypeRelations`、`CfirConstraints`、`CfirCallCompletion` 与 `CfirAbstractBodyResolveTransformer` 的边界关系。
3. 实现时先从类型关系与变量表示入手，再推进候选局部求解与调用补全接入。
4. 保留现有过渡实现作为兼容壳，逐步替换内部服务组合。
5. 当新补全路径稳定后，再清理旧的 Kotlin 风格约束系统与中间推断产物。

## 开放问题

- `ConeTypeVariableType` 在过渡期应保留到什么程度，何时完全退居为引用壳？
- `ConeDeferredType` 与 `ConePlaceholderType` 的边界如何定义，是否需要更多细分？
- `CfirTypeRelations` 的结果分级是否需要完全对齐官方 `TypeCompatibility`，还是保留更细的 CFIR 内部分级？
- `CfirCallCompletion` 与 `CfirCallResolver` 的最终边界应如何划分，哪些逻辑属于候选收集，哪些属于候选补全？
- 现有 checkers 中哪些类型检查能力应迁回 `BODY_RESOLVE`，哪些仍保留在 `CHECKERS` 层？
