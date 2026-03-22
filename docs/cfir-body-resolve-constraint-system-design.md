# CFIR BODY_RESOLVE 约束系统与类型对比系统设计

> 面向仓颉编译器 `BODY_RESOLVE` 子系统的全新语义设计文档。
>
> 本文档以官方仓颉 C++ 编译器语义为唯一保真基线，不以 Kotlin 编译器约束系统架构为模板，不继承其对象模型，不以其内部类型系统桥接层为设计前提。本文档的目标是为 CFIR `BODY_RESOLVE` 重新定义一套仓颉原生的：
>
> - 约束系统
> - 类型对比系统
> - 类型变量与占位类型系统
> - 调用完成接入边界

---

## 一、文档目的

本文档定义一套新的 `BODY_RESOLVE` 核心语义系统，用于统一承载：

- 表达式类型合成
- 期望类型驱动的自顶向下检查
- 候选调用的局部泛型推断
- 重载决议所需的可比较结果
- `QuestTy` / 理想数值类型 / `extend` / 泛型完备性 等仓颉特有语义
- `CfirCallCompleter` 的直接调用入口

本文档同时明确：

1. 新系统属于 `CFIR_RESOLVE` 内部能力，不新增顶层编译阶段。
2. 新系统是 `BODY_RESOLVE` 的语义内核，而不是额外的 `CfirResolvePhase`。
3. 新系统不仅要重构约束系统，也要重构当前类型对比系统。
4. 保留 `ConeCangJieType` 作为 CFIR 统一类型表示；当前 `ConeStubType` / `ConeCapturedType` 及其依赖的 Kotlin 风格推断桥接层，不应被视为未来终态模型。

---

## 二、设计前提

### 2.1 以官方仓颉 C++ 编译器为唯一语义基线

官方 C++ 编译器中，语义前端的核心并不是一个 Kotlin 式“统一约束系统对象”，而是一组围绕 `TypeChecker` / `TypeManager` / `TypeArgumentInference` / `TypeCheckCall` 协作的系统：

- `Synthesize(expr)`：自底向上合成表达式类型
- `Check(expr, expectedTy)`：自顶向下检查表达式符合期望类型
- `TypeManager::IsSubtype(...)`
- `TypeManager::CheckTypeCompatibility(...)`
- `TypeManager::ConstrainByCtor(...)`
- `TypeManager::AddSumByCtor(...)`
- `TypeManager::AllocTyVar(...)`
- 占位类型变量的约束存储
- `QuestTy` 与理想数值类型的专门语义

因此，新的 CFIR 设计必须从这些仓颉事实出发，而不是先构造一个 Kotlin 风格的补全上下文，再往里塞仓颉特例。

### 2.2 当前 CFIR 的主要问题不是“没有更多类”，而是“语义中心错位”

当前仓库里已经存在：

- `CfirCallCompleter`
- `ConstraintSystemCompleter`
- `CfirTypeCheckerContext`
- `ConeTypeContext`
- `ConeTypeVariableType` / `ConeStubType` / `ConeCapturedType`

但这些能力目前呈现出两个问题：

1. **求解架构中心仍明显受 Kotlin 内部抽象影响**
2. **类型对比系统仍把仓颉类型映射进一套 Kotlin 风格的标记/上下文/状态模型中**

结果是：

- `BODY_RESOLVE` 中的表达式求解和仓颉官方语义没有完全统一
- 类型比较结果缺乏仓颉式分级语义
- 类型变量、占位类型、捕获类型的职责边界不清
- `CfirCallCompleter` 的接线点被迫服务于一套不完全适配仓颉的中间模型

---

## 三、核心结论

### 3.1 不再以 Kotlin 约束系统对象图为设计模板

新的系统不再以这些概念作为架构起点：

- `ConstraintSystemCompletionContext`
- `NewConstraintSystemImpl`
- `ConeInferenceContext`
- `ConeCapturedType` / `ConeStubType` 作为长期中心类型
- `TypeCheckerState + TypeSystemContext` 作为核心类型比较协议

这些名称和对象关系，即使暂时保留为兼容层，也不应继续主导新设计。

### 3.2 新系统的真正中心应是“仓颉语义求解引擎”

新的 `BODY_RESOLVE` 内核应围绕以下五个轴组织：

1. **双向类型检查**：`Synthesize + Check`
2. **候选局部求解**：每个调用候选各自建立局部求解状态
3. **类型对比分级结果**：不是只有 `Boolean isSubtype`
4. **类型变量约束传播**：支持占位类型变量、构造子约束、理想数值类型、`QuestTy`
5. **调用补全接入**：`CfirCallCompleter` 直接消费候选求解结果，而不是驱动一套外来补全体系

### 3.3 新系统必须同时重构“约束系统”和“类型对比系统”

只改约束系统而不改类型对比系统，最终会产生一个结构性问题：

- 求解层想表达仓颉语义
- 类型比较层却仍以 Kotlin 的标记/上下文术语和对象约束为核心

这两层会长期互相拖拽，导致任何仓颉特性都变成“在 Kotlin 桥接层上打补丁”。

因此，本设计把二者视为同一个子系统的两个面：

- **约束系统**：负责“如何求解”
- **类型对比系统**：负责“如何比较、合并、归约类型关系”

---

## 四、系统在 `CFIR_RESOLVE` 中的位置

### 4.1 阶段边界

新系统仍处于现有 phase 边界之内：

```text
CFIR_RESOLVE
├─ IMPORTS
├─ SUPER_TYPES
├─ TYPES
├─ STATUS
├─ EXTENSIONS
├─ IMPLICIT_TYPES
│  └─ 仅负责声明边界级隐式类型就绪
├─ BODY_RESOLVE
│  └─ 新语义核心系统所在位置
└─ CHECKERS
   └─ 消费 BODY_RESOLVE 结果并生成最终诊断
```

### 4.2 不新增 `CfirResolvePhase`

本文档仍坚持：

- 不新增单独的“约束求解 phase”
- 不新增单独的“类型比较 phase”

因为它们都属于 `BODY_RESOLVE` 内部语义引擎，不是顶层流水线节点。

### 4.3 `CfirCallCompleter` 的接入定位

未来的 `CfirCallCompleter` 不再是“驱动一套 Kotlin 风格补全系统”的入口，而是：

- 选择候选求解结果
- 触发候选完成
- 写回替换结果 / 结果类型 / 可调用绑定 / lambda 边界信息

换句话说：

```text
CfirExpressionsResolveTransformer
    ↓
Call Resolution / Candidate Selection
    ↓
CfirCallCompleter
    ↓
CfirCallCompletion
    ↓
CfirCandidateSolveResult / CfirCallCompletionResult
```

---

## 五、设计目标

### 5.1 主目标

1. **与官方仓颉语义一致**
2. **分层清晰，逻辑可解释**
3. **能直接作为 `BODY_RESOLVE` 的统一语义核心**
4. **最终可被 `CfirCallCompleter` 直接接入**
5. **不以 Kotlin 内部类型桥接层作为长期依赖**

### 5.2 非目标

本文档不追求：

- 与 Kotlin FIR 内部 API 的兼容
- 保留现有所有 `Cone*` 推断中间类型作为长期抽象
- 一次性完成所有 checker / data-flow / smart-cast 设计

---

## 六、重新定义 `BODY_RESOLVE` 语义核心

### 6.1 顶层结构

新的 `BODY_RESOLVE` 核心建议定义为：

```text
BODY_RESOLVE Core
├─ CfirBidirectionalTyping
│  ├─ synthesize(expr)
│  └─ check(expr, expectedType)
├─ CfirCallResolution
│  ├─ 候选收集
│  ├─ 候选过滤
│  ├─ 候选局部求解
│  └─ 重载排序
├─ CfirConstraints
│  ├─ 变量管理
│  ├─ 约束登记
│  ├─ 约束传播
│  ├─ 补全
│  └─ 结果构造
├─ CfirTypeRelations
│  ├─ 子类型判断
│  ├─ 类型兼容性判断
│  ├─ 类型相等判断
│  ├─ 构造子形状关系
│  └─ extend 关系判断
└─ CfirDiagnosticMapping
```

### 6.2 核心思想

这个结构与旧设计最大的差别是：

- 不再把“约束系统”当成唯一中心
- 而是把它放进一个更大的“仓颉语义求解引擎”里

其中：

- `CfirBidirectionalTyping` 负责语义入口
- `CfirTypeRelations` 负责类型关系判定
- `CfirConstraints` 负责未知量求解
- `CfirCallResolution` 负责将候选与求解组织成调用语义

---

## 七、类型对比系统：新的中心，不再依赖 Kotlin 式桥接

### 7.1 为什么必须单独重构类型对比系统

当前类型比较主要围绕：

- `CfirTypeCheckerContext`
- `ConeTypeContext`
- `TypeCheckerState`
- `AbstractTypeChecker`

这条链路的问题不是“不能用”，而是：

1. 它把仓颉类型关系折叠成 Kotlin 风格标记协议
2. 它天然鼓励用 `Boolean isSubtype` 作为中心判断
3. 它把 `ConeStubType` / `ConeCapturedType` / `TypeCheckerState` 变成核心类型系统概念
4. 它很难自然表达仓颉官方 C++ 的 `TypeCompatibility` 分级语义

因此，新的设计建议把“类型对比系统”独立出来，不再以 Kotlin 类型检查基础设施为中心。

### 7.2 新的类型对比结果模型

官方 C++ 明确暴露：

```cpp
enum class TypeCompatibility { INCOMPATIBLE, SUBTYPE, IDENTICAL };
```

CFIR 新系统建议扩展为更适合 `BODY_RESOLVE` 的结果模型：

```text
CfirTypeRelationResult
├─ IDENTICAL
├─ SUBTYPE
├─ COMPATIBLE_WITH_CONVERSION
├─ COMPATIBLE_WITH_QUEST_FALLBACK
├─ CONSTRAINABLE_BY_VARIABLES
└─ INCOMPATIBLE
```

说明：

- `IDENTICAL`：完全相同
- `SUBTYPE`：严格子类型
- `COMPATIBLE_WITH_CONVERSION`：需要隐式语义转换，例如数值拓宽
- `COMPATIBLE_WITH_QUEST_FALLBACK`：仅在允许 `QuestTy` 退化的位置成立
- `CONSTRAINABLE_BY_VARIABLES`：当前不能立即判定，但可转化为类型变量约束
- `INCOMPATIBLE`：不可兼容

这比 `Boolean isSubtype` 更接近仓颉官方语义，也更适合驱动调用解析和候选排序。

### 7.3 新的类型对比引擎

建议引入：

```text
CfirTypeRelations
├─ compare(source, target, mode)
├─ isSubtype(source, target)
├─ isIdentical(source, target)
├─ inferConversion(source, target)
├─ normalizeForComparison(type)
├─ compareConstructorShape(a, b)
└─ resolveExtendRelation(a, b)
```

这套引擎应直接服务于：

- 表达式 `check(expr, expectedType)`
- 参数到形参匹配
- 重载排序
- 泛型上界检查
- 构造子形状约束

### 7.4 类型对比系统的输入前处理

在真正做关系判定前，应统一做一层归一化：

- 类型别名展开
- 理想数值类型归约准备
- 联合类型 / 交叉类型规整
- 泛型占位类型识别
- `QuestTy` 位置语义标注
- `extend` 可见语义准备

建议抽象为：

- `CfirTypeNormalizer`
- `CfirTypeAliasResolver`
- `CfirIdealNumericResolver`

---

## 八、保留 `ConeCangJieType` 前提下重构类型模型

### 8.1 保留 `ConeCangJieType` 作为统一类型根

本文档明确保留 `ConeCangJieType`，原因是：

- 它已经是当前 CFIR 中统一的类型表示根
- 大量真实仓颉语义类型已经稳定承载在 `Cone*` 具体类型上
- `BODY_RESOLVE`、checkers、type refs、symbol/type 写回路径都依赖这一统一表示

因此，新设计**不是**推翻 `ConeCangJieType`，而是：

- 保留 `ConeCangJieType` 作为外部统一类型表示
- 保留真实语义类型对应的主要 `Cone*` 具体类型
- 重构其上的类型关系引擎、变量表示、占位类型语义和过渡推断产物

建议保留的稳定类型表示包括：

- `ConePrimitiveType`
- `ConeClassLikeType`
- `ConeStructType`
- `ConeEnumType`
- `ConeFuncType`
- `ConeTupleType`
- `ConeArrayType` / `ConeVArrayType`
- `ConePointerType` / `ConeCStringType`
- `ConeUnionType`
- `ConeIntersectionType`
- `ConeQuestType`
- `ConeErrorType`

### 8.2 `ConeRigidType` 的问题

当前 `ConeCangJieType` 文档把大量类型都塞进 `ConeRigidType`，其中包括：

- 真实语义类型
- 类型变量类型
- 存根类型
- 捕获类型
- 错误类型

这会导致一个结构性问题：

- “刚性已知类型”
- “推断过程中的中间占位”
- “错误恢复产物”

被混在同一个层次里。

新的设计建议不是替换 `ConeCangJieType` 根层次，也不是引入一套平行的 `CfirType*` 类型表示层，而是重新划清现有 `Cone*` 子类体系内部的职责边界：

```text
ConeCangJieType
├─ 稳定的语义具体类型
├─ ConeTypeVariableType / 类型变量引用
├─ ConePlaceholderType / ConeDeferredType
├─ ConeQuestType / ConeErrorType
└─ 仅用于过渡期的推断产物
```

也就是说，真正需要被削弱的不是 `ConeCangJieType`，而是把所有中间语义都压到 `ConeRigidType` 下面的设计习惯。

### 8.2 `ConeStubType` 的问题

当前 `ConeStubType` 明确写着：

- 对应 K2 `ConeStubType`
- 用于子类型检查 / builder inference

这说明它是明显的 Kotlin 中间产物投影。

在新的仓颉原生设计中，建议将其降级为过渡实现，长期由更明确的 `Cone*` 内部语义类型替代，例如：

- `ConePlaceholderType`：表示暂未知但已分配稳定标识的占位类型
- `ConeDeferredType`：表示需要进一步分析后才能确定的类型

它们的职责不同于 Kotlin `stub type`：

- 不再服务于 builder inference 术语
- 明确服务于仓颉的 placeholder tyvar、递归返回类型、未注解 lambda 参数等场景

### 8.3 `ConeCapturedType` 的问题

当前 `ConeCapturedType` 也是直接影射 Kotlin captured type。可问题在于：

- 仓颉没有 Kotlin 那套通配符投影语义中心
- 当前 captured type 在仓颉里缺乏独立语言来源

因此建议：

1. 不把 `ConeCapturedType` 作为长期核心语义类型
2. 若需要表达“从泛型关系中临时引出的内部比较类型”，使用更直白的仓颉内部术语，例如：
   - `ConeBoundProjectionType`
   - 或 `ConeDerivedConstraintType`
3. 若没有明确语义来源，则不保留该抽象

### 8.4 `ConeTypeVariableType` 的保留方式

类型变量本身在仓颉是必须存在的，但不应再用“默认类型 + rigid type 子类”的方式表达。

更准确地说，应保留它作为过渡表示，但把真正的语义中心移到独立变量状态对象上。建议引入：

- `ConeTypeVariableId`
- `ConeTypeVariableState`
- `ConeTypeVariableRef`

并将 `ConeTypeVariableType` 降级为类型图中的引用壳。也就是说：

- 类型变量是求解状态中的实体
- 在类型图中只出现 `ConeTypeVariableRef` 这类引用视图
- 不再把“变量状态本身”伪装成一种普通具体类型

---

## 九、新的约束系统设计

### 9.1 中心抽象

建议重新定义以下对象：

```text
CfirConstraints
├─ CfirConstraintStore
├─ CfirVariableManager
├─ CfirConstraintPropagation
├─ CfirConstraintCompleter
├─ CfirConstraintResultBuilder
└─ CfirConstraintIssues
```

### 9.2 `CfirConstraint`

建议最小集合如下：

- `EqualityConstraint(a, b)`
- `SubtypeConstraint(a, b)`
- `CompatibilityConstraint(a, b, mode)`
- `ExpectedTypeConstraint(exprId, expectedType)`
- `ConstructorConstraint(variable, constructorShape)`
- `SumConstraint(variable, constructorShape)`
- `ExtendConstraint(baseType, requiredInterface)`
- `GenericUpperBoundConstraint(variable, upperBound)`
- `GenericCompletenessConstraint(variable, context)`
- `IdealNumericConstraint(literal, targetSpace)`

其中：

- `ConstructorConstraint` / `SumConstraint` 是为了对齐官方 C++ 的 `ConstrainByCtor` / `AddSumByCtor`
- 不再把这些高阶语义伪装成简单 subtype 关系

### 9.3 `CfirConstraintStore`

它应保存：

- 变量集合
- 约束集合
- 未解变量集合
- 候选局部替换结果
- 延迟分析单元
- 问题集合
- `QuestTy` 回退决策

### 9.4 `CfirVariableManager`

官方 C++ 中 `AllocTyVar(...)` 明确服务于：

- 实例化阶段的占位类型
- 暂未知类型（未注解 lambda 参数、递归返回类型）

因此新系统里应显式建：

- `CfirVariableManager.allocatePlaceholder(...)`
- `CfirVariableManager.allocateInstantiationVariable(...)`
- `CfirVariableManager.allocateDeferredBoundaryVariable(...)`

### 9.5 `CfirConstraintPropagation`

负责：

- 传播简单的相等 / 子类型 / 兼容性关系
- 更新变量 upper/lower bounds
- 执行 constructor-shaped 约束扩展
- 执行 sum constraints 同步
- 触发 extend 相关条件收束

### 9.6 `CfirConstraintCompleter`

新的 completer 不再围绕 Kotlin 的补全模式组织，而是围绕仓颉的候选局部求解过程组织。

建议主循环：

```text
while (session can still progress) {
  1. propagate simple constraints
  2. 分析已就绪的延迟分析单元
  3. 提交已可固定的变量
  4. 收敛理想数值类型决策
  5. 在允许时尝试 `QuestTy` 回退
  6. 完成候选局部结果
}
```

这里没有必要保留 `FULL / PARTIAL / PCLA_POSTPONED_CALL` 这些 Kotlin 命名。仓颉系统应改成更语义化的求解模式：

- `LOCAL_COMPLETE`
- `LOCAL_PARTIAL`
- `OUTER_AWARE_DEFERRED`

### 9.7 `CfirConstraintResultBuilder`

职责：

- 把局部求解会话变成可写回的候选结果
- 产出：
- 结果类型
- 实例化后的类型参数
- 未解变量
- 回退决策
- 候选可行性

---

## 十、双向类型检查

### 10.1 顶层接口

建议新定义：

```text
CfirBidirectionalTyping
├─ synthesize(expr, context): CfirSynthesizeResult
└─ check(expr, expectedType, context): CfirTypeCheckResult
```

### 10.2 `synthesize`

职责：

- 直接合成字面量、变量引用、属性访问、控制流表达式的类型
- 对调用表达式启动候选局部求解
- 返回：
- 表达式类型
- 候选集合 / 已选候选
- 新产生的约束
- 延迟分析单元

### 10.3 `check`

职责：

- 通过 `CfirTypeRelations.compare(...)` 先判断直接兼容性
- 若不够，则将 expectedType 转入候选局部约束系统
- 必要时允许：
- 理想数值类型具体化
- `QuestTy` 回退
- 构造子形状约束

---

## 十一、候选局部求解模型

### 11.1 默认一候选一会话

每个候选各自求解：

```text
调用点
├─ CandidateA -> SessionA
├─ CandidateB -> SessionB
└─ CandidateC -> SessionC
```

这是本文档最重要的结构判断之一。

理由：

- 与官方仓颉 C++ 的函数匹配 / 类型参数推断形态一致
- 更容易解释重载歧义
- 更适合仓颉的期望类型反推与 `extend` 过滤

### 11.2 候选结果输出

每个候选输出：

- `candidateStatus`
- `relationScore`
- `resultType`
- `typeArgumentInstantiation`
- `remainingUnsolvedVariables`
- `questFallbackUsed`
- `requiredConversions`
- `usedExtends`

这些结果用于重载排序。

---

## 十二、重载排序不再只看“是否成立”

新的重载决议应消费类型对比结果和候选求解结果，而不是单看“候选能不能成立”。

建议排序输入至少包括：

1. 参数匹配是否 `IDENTICAL`
2. 参数匹配是否 `SUBTYPE`
3. 是否依赖隐式转换
4. 是否依赖 `QuestTy` 回退
5. 是否仍有未解变量
6. 是否依赖 extend 语义补齐
7. 实例化后参数签名的专门性

这与官方 C++ “先求解、再比较实例化签名”的思路保持一致。

---

## 十三、`QuestTy` 与理想数值类型的一等建模

### 13.1 `QuestTy`

不应把 `QuestTy` 视为简单的错误恢复类型。

它应被建模为：

- 一种合法但受位置约束的退化结果
- 参与候选排序
- 参与诊断解释

建议独立策略对象：

- `CfirQuestFallbackRules`

### 13.2 理想数值类型

`IDEAL_INT` / `IDEAL_FLOAT` 也不应只是 primitive subtype 特例。

建议独立策略对象：

- `CfirIdealNumericRules`

职责：

- 决定何时保持理想类型
- 决定何时具体化
- 决定具体化对候选排序的影响

---

## 十四、`extend` 语义必须进入核心引擎

`extend` 不能只留在 `EXTENSIONS` phase 的索引构建中。

在 `BODY_RESOLVE` 中，它还必须参与：

- 成员查找
- 参数/receiver 兼容性
- 泛型约束检查
- 重载过滤
- 诊断归因

因此应引入：

- `CfirExtendRelation`
- `CfirExtendCandidateFilter`

而不是在调用解析末尾做零散补丁。

---

## 十五、与 `CfirCallCompleter` 的直接接入方案

### 15.1 新的接口形状

未来的 `CfirCallCompleter` 应直接依赖：

```text
CfirCallCompletion
├─ complete(call, candidate, expectedType, mode)
└─ completePartially(...)
```

返回：

```text
CfirCallCompletionResult
├─ finalResultType
├─ finalCandidate
├─ typeArgumentMapping
├─ appliedSubstitutions
├─ unresolvedVariables
├─ deferredUnitsInfo
├─ relationInfo
└─ constraintIssues
```

推荐接线方式：

- `CfirCallCompleter` 作为现有调用补全入口保留
- 其内部不再直接操纵旧约束存储与旧补全细节
- 改为通过一个薄适配层把 `call / candidate / expectedType / resolutionMode` 转换为 `CfirCallCompletion` 的输入
- `CfirCallCompletion` 返回仓颉原生求解结果，再由 `CfirCallCompleter` 负责写回 CFIR 节点

### 15.2 `CfirCallCompleter` 的职责收缩

`CfirCallCompleter` 本身应只负责：

- 从 `CfirExpression` / `CfirResolvable` 提取调用点信息
- 调用 `CfirCallCompletion`
- 将结果写回 CFIR

它不再承担：

- 驱动 Kotlin 风格补全模式细节
- 直接操作 Kotlin 风格约束存储
- 在内部处理 top-level lambda hack

### 15.3 写回对象

写回时只应写：

- 选中候选
- 结果类型
- 实例化后的类型参数
- lambda / 延迟分析边界的稳定结果

最终用户诊断仍交由 `CHECKERS` 在消费约束问题后生成。

---

## 十六、迁移判断：哪些现有抽象应保留，哪些应降级

### 16.1 可以作为兼容壳暂时保留

- `CfirCallCompleter`（接口壳）
- `CfirExpressionsResolveTransformer`（调用入口）
- `ConePrimitiveType` / `ConeClassLikeType` / `ConeStructType` / `ConeEnumType` / `ConeFuncType` / `ConeTupleType`

### 16.2 应降级为过渡实现

- `CfirTypeCheckerContext`
- `ConeTypeContext`
- `CfirTypeSubstitutorByMap`
- `ConstraintSystemCompleter`

这些可以作为迁移过渡层，但不应再作为长期核心语义接口。

### 16.3 应从长期设计中移除或重命名

- `ConeStubType`
- `ConeCapturedType`
- 任何以 Kotlin builder inference / captured projection 为语义来源的类型中间物

注意：这里的“移除或重命名”不包括 `ConeCangJieType` 本身，也不包括已经稳定表达仓颉真实语义的主要 concrete `Cone*` 类型。

### 16.4 应重建而不是修补

- 类型关系比较入口
- 变量分配与约束传播入口
- overload ranking 输入模型
- QuestTy fallback 逻辑
- ideal numeric concretization 逻辑

---

## 十七、对 `CfirAbstractBodyResolveTransformer` 架构的影响

### 17.1 不推翻 transformer / dispatcher 外壳

本文档设计的这套新 BODY 语义内核，会显著影响 `BODY_RESOLVE` 内部语义服务图，但**不会直接推翻** `CfirAbstractBodyResolveTransformer` / `CfirAbstractBodyResolveTransformerDispatcher` 这种 transformer + dispatcher 的组织方式。

原因是这层架构当前承担的主要是：

- phase transformer 外壳
- 声明/表达式 transform 分发
- body resolve 共享组件容器注入
- 遍历与写回编排

这些职责本身并不依赖 Kotlin 风格约束系统对象图，因此可继续保留为 `BODY_RESOLVE` 的编排壳层。

### 17.2 真正会被重构的是 `BodyResolveTransformerComponents`

当前 `CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents` 集中持有：

- `subtypeChecker`
- `conflictResolver`
- `inferenceComponents`
- `resolutionContext`
- `towerResolver`
- `callResolver`

新的 BODY 语义落地后，这里会成为最主要的重构点。

推荐方向是：

- `subtypeChecker` → 升级为更一般的 `typeRelations`
- `conflictResolver` → 升级为消费 richer relation result 的 overload 排序能力
- `inferenceComponents` → 被更仓颉原生的 `constraints` / solver services 替代或吸收
- `resolutionContext` → 承载候选局部求解、expected type、deferred units 等更完整语义上下文
- `callResolver` → 从“解析 + 旧式推断协作”转向“候选收集 + 候选局部求解 + completion 调用”

也就是说，`components` 仍然保留，但其内部挂载的服务组合会被重塑。

### 17.3 对 `CfirExpressionsResolveTransformer` 的影响

`CfirExpressionsResolveTransformer` 不需要被架构性推翻，但它与 `callResolver` / `callCompleter` 的协作边界会重新定义。

当前更接近：

```text
expression transform
    ↓
resolve call
    ↓
select candidate
    ↓
old completion / substitutor / subtype check
```

新设计更接近：

```text
expression transform
    ↓
collect candidates
    ↓
candidate-local solving
    ↓
completion
    ↓
ranking
    ↓
write-back
```

因此，影响主要是内部协作协议，而不是 transformer 访问者外壳。

### 17.4 结论：保留壳层，重构语义服务图

对 `CfirAbstractBodyResolveTransformer` 的总体判断如下：

- **保留**：transformer / dispatcher / declarations-vs-expressions 分发模型
- **保留**：共享 components 容器这一组织方式
- **重构**：components 内部服务图
- **重构**：`expressionsTransformer -> callResolver -> callCompleter` 协作链
- **不建议**：为了新约束系统专门推翻整个 body resolve transformer 架构

更准确地说，新设计影响的是 `BODY_RESOLVE` 的**语义中枢**，而不是它的**遍历外壳**。

---

## 十八、推荐模块内部分层

建议在 `:cfir:resolve` 内部形成如下逻辑边界：

```text
resolve/
├─ body/
│  ├─ expressions/
│  ├─ calls/
│  └─ completion/
├─ typing/
│  ├─ relations/
│  ├─ normalization/
│  ├─ variables/
│  └─ compatibility/
├─ constraints/
│  ├─ collection/
│  ├─ propagation/
│  ├─ completion/
│  └─ materialization/
└─ diagnostics/
```

这里的重点不是目录名，而是边界：

- 类型关系引擎独立
- 约束系统独立
- 调用完成只依赖这两者，不再反过来定义它们

---

## 十九、结论

新的 `BODY_RESOLVE` 设计不应再被表述为“参考 Kotlin K2 的约束系统，然后做仓颉适配”。

更准确的描述应是：

1. **以官方仓颉 C++ 编译器的双向类型检查与类型管理模型为核心语义来源**
2. **重建一套仓颉原生的类型对比系统**
3. **在其上建立候选局部求解的约束系统**
4. **让 `CfirCallCompleter` 直接调用这套求解引擎，而不是继续耦合 Kotlin 风格 completion/storage 体系**
5. **保留 `ConeCangJieType` 作为统一类型表示，同时将 `ConeStubType` / `ConeCapturedType` / Kotlin 风格 type checker context 视为过渡层，而非未来终态**

最终目标不是“把 Kotlin 的架构翻译成仓颉术语”，而是让 `BODY_RESOLVE` 拥有一个真正属于仓颉语义的、分层清晰、逻辑自洽、可直接接线的统一求解核心。
