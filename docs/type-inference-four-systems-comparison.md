# 四套类型推断 / 约束系统对照表

本文对比四套与当前项目最相关的语义分析 / 类型推断实现：

1. 官方仓颉 C++ 编译器 `external/cangjie_compiler`
2. Kotlin 编译器（重点是 K2/FIR）`external/kotlin`
3. IntelliJ Cangjie 插件 `external/intellij-cangjie`
4. 当前项目的 CFIR `cfir/resolve`

目标不是给出抽象教材式定义，而是基于本仓库已读取到的生产代码证据，回答一个更工程化的问题：

> 这四者各自到底更接近单向求解，还是双向求解？

---

## 一句话总结

- **官方仓颉 C++ 编译器**：最明确的 **双向类型检查**
- **Kotlin 编译器（K2/FIR）**：**双向式 / expected-type-aware 推断**，不是纯单向
- **`external/intellij-cangjie`**：整体也更接近 **双向式推断**
- **当前 CFIR**：已经有双向骨架，但目前仍然只是 **部分双向**，尚未闭环

如果只看完整度，大致可以粗略理解为：

```text
官方仓颉 C++ ≈ Kotlin K2 ≈ external/intellij-cangjie > 当前 CFIR
```

但三者前排并不是完全同一种风格：

- 官方仓颉 C++ 更接近教科书式的 `Synthesize + Check`
- Kotlin K2 更像 “约束系统 + expected type + postponed analysis” 驱动的现代双向式推断
- `external/intellij-cangjie` 整体风格更接近 Kotlin 这一路
- 当前 CFIR 已经朝这个方向发展，但关键编排层还没有完全接通

---

## 总对照表

| 系统 | 整体判断 | expected type 自顶向下参与 | lambda / postponed | receiver / call completion | 约束系统成熟度 | 核心证据概述 |
|---|---|---|---|---|---|---|
| **官方仓颉 C++** `external/cangjie_compiler` | **明确双向** | **强** | **强** | **强** | **高** | `TypeCheckerImpl.h` 同时存在 `Synthesize(...)` / `Check(target, node)`；`LambdaExpr.cpp` 用目标函数类型驱动参数和返回类型；`IfExpr.cpp` / `Block.cpp` 把目标类型下传；`TypeCheckCall.cpp` 用目标返回类型筛候选 |
| **Kotlin 编译器（重点 K2/FIR）** `external/kotlin` | **双向式，不是纯单向** | **强** | **强** | **强** | **高** | `ArgumentCheckingProcessor.kt` 让 expected type 直接入约束；lambda 创建 postponed atom；`PostponedArgumentInputTypesResolver.kt` 按逆变位置反向塑形 lambda 参数；`ConstraintStorage.kt` 同时维护 `LOWER/UPPER/EQUALITY` |
| **IntelliJ Cangjie 插件** `external/intellij-cangjie` | **更接近双向** | **中-强** | **中-强** | **中-强** | **高** | `ResolutionContext` / `ExpressionTypingContext` 把 `expectedType` 作为核心上下文；`PostponedArgumentsAnalyzer` 优先用 expected type 分析 lambda；`CangJieConstraintSystemCompleter` 做 postponed/completion |
| **当前 CFIR** `cfir/resolve` | **部分双向，尚未完整** | **有，但不系统** | **有，但多为补丁式** | **弱-中** | **中** | `CfirResolutionMode` 有 `ContextIndependent / WithExpectedType`；`CfirInferTypeArguments` 与 `CfirCallCompleter` 会使用 expected type；但 `CfirExpressionsResolveTransformer` 大量节点仍先 `ContextIndependent` 解析，postponed marker 与 receiver-aware candidate 生命周期尚未完整接通 |

---

## 关键差异表

| 维度 | 官方仓颉 C++ | Kotlin K2 | `external/intellij-cangjie` | 当前 CFIR |
|---|---|---|---|---|
| **是否有显式 `Synthesize / Check` 分离** | **有，且非常明确** | **没有用这个命名，但语义上有** | **没有同名 API，但上下文与 postponed 机制体现同一模式** | **有 `ResolutionMode`，但尚未成为统一主路径** |
| **expected type 是否只是事后验证** | 否 | 否 | 否 | **部分不是，但很多地方仍接近“先算后修”** |
| **lambda 是否受目标类型反向驱动** | **强** | **强** | **强** | **部分支持** |
| **postponed 生命周期是否真实存在** | 有等价机制 | **强** | **强** | **协议层有，主流程未完整接入** |
| **分支 / block / body 是否按 expected type 检查** | **是** | **大体是** | **倾向是** | **大量仍先独立合成** |
| **receiver 是否进入统一候选 / 约束系统** | **是** | **是** | **是** | **还不完整** |

---

## 四者各自的一句话判断

### 1. 官方仓颉 C++ 编译器

**结论：标准意义上的双向类型检查。**

这是四者里表达得最直接的一套。核心入口就显式区分：

- `Synthesize(expr)`：从表达式自身综合类型
- `Check(expr, expectedType)`：按目标类型检查表达式

而且这个模式不只是表面 API：

- lambda 会用目标函数类型反向驱动参数与返回类型
- `if` / `block` 会把目标类型下传给分支和末尾表达式
- 调用解析会把目标返回类型用于候选筛选

这套实现因此最接近教科书意义上的 bidirectional typing。

---

### 2. Kotlin 编译器（重点 K2/FIR）

**结论：整体是双向式推断，不是纯单向。**

这里最容易混淆的一点是：

> Kotlin 的约束系统内部确实是“有方向的”，但这不等于它的整体推断范式就是单向。

例如：

- `ConstraintStorage.kt` 里有 `LOWER / UPPER / EQUALITY`
- `ConstraintSystemBuilder.kt` 用 `addSubtypeConstraint(lower, upper)` 来添加约束

这说明的是 **约束的内部表示方式**，不是整体信息流只能单向。

真正决定其范式的是这些行为：

- expected type 会直接进入参数检查与约束收集
- lambda 会作为 postponed argument 延迟分析
- 当 expected type 本身还是类型变量时，依然会建立专门的 postponed lambda atom
- lambda 参数位置会根据函数类型约束和逆变关系做方向调整

因此更准确的说法是：

> Kotlin K2 是一个 expected-type-aware、postponed-argument-driven 的双向式推断体系。

---

### 3. `external/intellij-cangjie`

**结论：更接近双向，不应描述为根本上的单向。**

从代码证据看，这套系统也有非常明显的 top-down + completion 闭环：

- `ResolutionContext` / `ExpressionTypingContext` 把 `expectedType` 当成表达式分析上下文的一等成员
- `PostponedArgumentsAnalyzer` 优先使用 expected type 来分析 lambda
- lambda / postponed 参数分析结果会回灌候选约束系统
- `CangJieConstraintSystemCompleter` 支持 postponed / completion 生命周期

所以它并不是单纯的 bottom-up 推断，而是 expected-type-aware 的双向式推断。

更保守的表述可以是：

> 比起 one-way，它明显更接近 bidirectional。

---

### 4. 当前 CFIR

**结论：已经有双向骨架，但目前仍只是部分双向。**

当前 CFIR 已经具备这些关键部件：

- `CfirResolutionMode.ContextIndependent / WithExpectedType`
- `CfirInferTypeArguments` 会把 expected type 转成返回类型约束
- `CfirCallCompleter` 会在 completion 阶段继续利用 expected type
- lambda 有重跑 / retranform 的补丁式路径

但它还缺少完整闭环：

- 很多表达式子树仍然先 `ContextIndependent` 解析
- expected type 传播还不是系统性的主路径
- postponed marker 基本还停留在协议层，未完全接入求解主流程
- receiver-aware candidate 构造和求解生命周期还不完整
- completion 仍更像一次性收束，而不是成熟的 staged completion

所以当前 CFIR 不能算真正意义上的完整双向系统，只能说：

> 它已经走在双向化的正确方向上，但还没追平前三者的实现完整度。

---

## 工程视角下最值得记住的区别

### 1. “内部约束有方向” 和 “整体系统是单向” 不是一回事

这点在 Kotlin 和 `external/intellij-cangjie` 上尤其重要。

一个系统完全可以：

- 内部把约束存成 `LOWER / UPPER / EQUALITY`
- 但整体仍然通过 expected type、自顶向下上下文、postponed lambda、completion 循环来实现双向式推断

因此判断单向还是双向，不能只看 `addSubtypeConstraint(...)` 这种底层 API。

---

### 2. 判断“双向程度”最有用的几个观察点

比起只看术语，下面这些现象更能说明问题：

1. **expected type 是否会真正进入求解过程，而不只是事后验证**
2. **lambda 是否会因为目标类型不同而走不同分析路径**
3. **是否存在 postponed argument / postponed lambda 生命周期**
4. **completion 是否是分阶段的，而不是一次性收束**
5. **receiver 是否进入候选与约束系统，而不是走局部捷径**

从这些角度看：

- 官方仓颉 C++：五项都很强
- Kotlin K2：五项也很强，只是实现风格更偏现代约束系统
- `external/intellij-cangjie`：大体跟随 Kotlin 这一风格
- 当前 CFIR：前两项开始具备，但后三项还明显不完整

---

## 结论

如果把这四者压缩成一句最短的话：

- **官方仓颉 C++**：明确双向
- **Kotlin K2**：双向式，不是纯单向
- **`external/intellij-cangjie`**：也更接近双向
- **当前 CFIR**：部分双向，尚未完整

如果把它们放在同一条演进线上，可以理解为：

```text
“局部 expected-type 支持”
    -> “postponed + completion 驱动的双向式推断”
    -> “完整、自洽、全路径接通的双向类型检查”
```

当前 CFIR 仍处在从第一阶段走向第二阶段的途中，而前三者已经处在第二阶段甚至更接近第三阶段。

---

## 当前 CFIR：应该向 Kotlin 借什么，向官方仓颉 C++ 守什么

这一节把前面的结论压缩成一条更适合指导实现的设计原则：

> **向 Kotlin 借“求解框架与编排方法”，向官方仓颉 C++ 守“语义边界与最终行为”。**

也可以再直白一点理解为：

- **Kotlin 负责告诉我们：怎么做一个现代推断引擎**
- **官方仓颉 C++ 负责告诉我们：仓颉里什么才算对**

### 总表

| 类别 | 向 Kotlin 借 | 向官方仓颉 C++ 守 |
|---|---|---|
| 总体原则 | 借 **工程化推断框架** | 守 **仓颉语言语义真值** |
| `BODY_RESOLVE` 组织方式 | 借 `expected type + postponed + completion` 的编排 | 守 `Synthesize + Check` 的语义入口划分 |
| 候选调用求解 | 借 candidate-local solving、completion mode、postponed lifecycle | 守仓颉候选比较规则、合法性规则、诊断归因 |
| 约束系统实现 | 借约束存储、事务、fixation、completion 的工程套路 | 守 `QuestTy`、理想数值、`extend`、`Any/Nothing` 等仓颉规则 |
| lambda / postponed | 借 delayed analysis、revised expected type、lambda atom 生命周期 | 守仓颉 lambda 的真实语义，不被 Kotlin 函数类型假设反向定义 |
| 类型关系层 | 可借 dependency graph、join/meet、properness 等工具思路 | **必须守**官方 `TypeCompatibility` 分级语义，而不是退化成 `Boolean isSubtype` |
| 结果写回 | 借 completed inference result 流向后续 checking 的做法 | 守 resolved call / diagnostics / completion result 的仓颉语义含义 |
| 长期对象模型 | 借机制，不借对象图 | 不以 `NewConstraintSystemImpl` / `TypeCheckerState` / `ConeCapturedType` 那套为终态 |

---

### 一、应该向 Kotlin 借什么

应该借的不是 Kotlin 语言语义本身，而是它在复杂推断问题上的工程组织能力。

尤其值得借的有三大块：

#### 1. postponed / completion 编排

这部分是当前 CFIR 与 Kotlin K2 差距最明显、也最值得直接学习的地方。

建议借用的思想包括：

- postponed argument 生命周期
- lambda 延迟分析
- revised expected type
- staged completion / staged fixation
- completion mode 区分

这些机制的价值不在于“像 Kotlin”，而在于它们能把复杂场景拆成可维护、可解释的求解流程。

#### 2. candidate-local inference 生命周期

每个候选各自维护局部求解状态，而不是把所有问题提前揉成一个全局大锅。这一点对于：

- 泛型调用
- 重载排序
- lambda 参与调用推断
- receiver 与参数交织约束

都很关键。

当前 CFIR 文档里已经明确把“候选局部求解”列成核心轴之一，这一方向本身就是对的。

#### 3. expected type 真正进入求解过程

Kotlin K2 值得借的，不是某个类名，而是这个原则：

> expected type 不应只是解析完成后的外层验证，而应当真正进入求解过程。

这包括：

- 参数检查时进入约束系统
- lambda 分析时参与参数与返回类型塑形
- call completion 时继续作为候选完成输入

这条原则对当前 CFIR 尤其重要，因为它现在已经有 `WithExpectedType` 和 `CallCompleter`，但还没有把二者彻底接成主路径。

---

### 二、只能参考 Kotlin，不能照搬 Kotlin 的地方

这部分是最容易犯错的：

> **Kotlin 适合作为实现参考，不适合作为语义中心。**

尤其不应把这些东西直接当成长期终态模型：

- `NewConstraintSystemImpl`
- `ConstraintSystemCompletionContext`
- `TypeCheckerState + TypeSystemContext`
- `ConeCapturedType` / `ConeStubType` 作为长期核心抽象
- Kotlin 内部对象图与命名关系

原因很简单：这些是 **Kotlin 为 Kotlin 自己的问题组织出来的内部工程结构**。

一旦 CFIR 直接把它们当中心，后面所有仓颉特性都会被迫在 Kotlin 的桥接层上“打补丁”，这正是 `docs/cfir-body-resolve-constraint-system-design.md` 反复要避免的事。

---

### 三、必须向官方仓颉 C++ 守什么

这部分不能让 Kotlin 反客为主。

#### 1. 守语义入口：`Synthesize + Check`

这是官方仓颉 C++ 最清晰、也最适合作为 CFIR 语义入口的部分。

应当坚持：

- 哪些表达式先综合类型
- 哪些表达式按期望类型检查
- 哪些节点必须走 top-down 检查路径

这些边界应该由仓颉语义决定，而不是由某套借来的 completion machinery 决定。

#### 2. 守类型关系模型：不要退化成 `Boolean isSubtype`

官方仓颉 C++ 并不是只有一个扁平的 subtype 布尔判断。文档已经明确指出，需要保留更贴近仓颉的分级关系，例如：

- `IDENTICAL`
- `SUBTYPE`
- `COMPATIBLE_WITH_CONVERSION`
- `CONSTRAINABLE_BY_VARIABLES`
- `INCOMPATIBLE`

这点非常关键。

如果 CFIR 继续让“类型关系中心 = `Boolean isSubtype`”，那么很多仓颉语义都会被错误地折叠进 Kotlin 风格判定里，后面只会越来越多补丁。

#### 3. 守仓颉特有语义真值

这些内容必须以官方仓颉 C++ 为准，而不是以 Kotlin 的已有抽象去近似：

- `QuestTy`
- 理想数值类型
- `extend` 关系
- `Any` / `Nothing` 的语言级有效性规则
- 候选合法性与最佳解选择
- 诊断 blame 的归因方式

这些都属于“仓颉里什么叫对”的范畴，不能被借来的框架反向定义。

---

### 四、最实用的落地版本

如果把这套取舍压缩成最能指导实现的三借三守，可以记成下面这样。

#### CFIR 应该向 Kotlin 借的三件大事

1. **postponed / completion 编排**
2. **candidate-local inference 生命周期**
3. **expected type 真正进入求解流程的工程实现**

#### CFIR 必须向官方仓颉 C++ 守的三件大事

1. **语义入口：`Synthesize + Check`**
2. **类型关系模型：不是只有 `isSubtype`，而是仓颉式 compatibility 分级**
3. **语言特性真值：`QuestTy`、理想数值、`extend`、候选合法性、诊断归因**

---

### 五、最后压成一句设计口号

可以把当前 CFIR 的长期原则直接写成：

> **借 Kotlin 的求解 machinery，守官方仓颉 C++ 的语义 law。**

如果要再展开成一句解释，就是：

> Kotlin 适合作为“如何组织一个现代推断引擎”的工程参考；官方仓颉 C++ 才是“仓颉里什么行为正确”的最终语义基线。
