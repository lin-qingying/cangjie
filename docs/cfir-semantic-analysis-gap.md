# CFIR 语义分析基础设施完备性分析

> 三方交叉对比：当前项目 × Kotlin K2 FIR × 仓颉 C++ 编译器
>
> 日期：2026-03-13
>
> **现状提示（2026-05-11）**：本文是 2026-03-13 时点的 gap 快照，后续更深入的评估见 [`cfir-semantic-analysis-maturity-vs-official-2026-04-08.md`](cfir-semantic-analysis-maturity-vs-official-2026-04-08.md)。文中 `cfir:symbols` 当前已不存在，对应实装模块为 `:cfir:semantics` + `:cfir:providers`；`cfir:diagnostics` 实际对应 `:common:diagnostics` + `:cfir:diagnostic-renderers`。

---

## 一、总体结论

当前基础设施已具备实现完整 CFIR 语义分析的**骨架能力**，但存在两个关键空缺：

1. **表达式解析引擎缺失** — IMPLICIT_TYPES 和 BODY_RESOLVE 是空壳
2. **类型推断引擎缺失** — 无子类型判定、约束求解、重载解析

| 层面 | 完备度 | 评估 |
|------|--------|------|
| 编译管线框架 | **90%** | 9-phase resolve pipeline、Processor 注册、生命周期管理齐全 |
| 类型系统（Cone） | **85%** | 14 种类型覆盖仓颉全部类型，缺泛型约束求解 |
| 符号/作用域系统 | **75%** | Provider/Scope 抽象完整，缺跨模块加载和具体 Scope 实现 |
| 声明级解析（phase 1-6） | **70%** | IMPORTS/SUPER_TYPES/TYPES/STATUS/EXTENSIONS 有实际处理逻辑 |
| 表达式级解析（phase 7-8） | **10%** | IMPLICIT_TYPES 和 BODY_RESOLVE 是空壳 |
| 检查器框架 | **80%** | 框架+生成器完整，缺具体检查规则 |
| 诊断系统 | **95%** | 工厂、渲染、收集全链路齐全 |

---

## 二、Resolve Phase 逐阶段对比

### Phase 1: IMPORTS

| | Kotlin K2 | 仓颉 C++ | 当前 CFIR | 差距 |
|---|---|---|---|---|
| 导入解析 | `FirImportResolveTransformer` longest-package-first | `ImportManager.ResolveImports()` | `CfirImportsResolveProcessor` + `CfirImportBindingResolver` | 基本对齐 |
| 冲突报告 | 内置冲突检查 | 重定义检查 | `CfirImportConflictReporter` | 已有 |
| 跨模块包查询 | `symbolProvider.hasPackage()` | `ImportManager.BuildIndex()` | `CfirSymbolProvider.hasPackage()` | 接口已有，缺 .cjo 加载实现 |

**评估：** 导入解析能力基本完备，主要缺口在跨模块符号加载（.cjo 反序列化）。

### Phase 2: SUPER_TYPES

| | Kotlin K2 | 仓颉 C++ | 当前 CFIR | 差距 |
|---|---|---|---|---|
| 超类型解析 | 两遍：Visitor 收集 + Transformer 应用 | `GetAllSuperTys()` 递归收集 | `CfirSuperTypesResolveProcessor` | 有实现 |
| 循环检测 | `SupertypeComputationSession` | `CheckInheritanceCycleDFS()` | `CfirSuperTypeChecker` | 有实现 |
| 类型别名展开 | 超类型解析时展开 | `SubstituteTypeAliasForAlias()` | 需验证 | 可能缺失 |
| Jumping phase | 支持同 phase 跨声明查询 | N/A（单遍） | `CfirResolvePhase` 标记了 jumping | 框架已有 |

**评估：** 超类型解析基础已实现。仓颉的 `extend Type <: Interface` 继承语义比 Kotlin 复杂（extend 可为任意类型添加接口实现），EXTENSIONS phase 已有对应处理器。

### Phase 3: TYPES

| | Kotlin K2 | 仓颉 C++ | 当前 CFIR | 差距 |
|---|---|---|---|---|
| 显式类型解析 | `FirTypeResolveTransformer` + scope 栈管理 | `ResolveNames()` + `SetTypeTy()` | `CfirTypeRefResolver` + `CfirSpecificTypeResolverTransformer` | 有实现 |
| Scope 管理 | `PersistentList<FirScope>` 不可变栈 | `ScopeManager` 可变栈 | `CfirScopeSession` | 框架已有，缺具体 scope 类型 |
| 类型参数 scope | `FirMemberTypeParameterScope` | 隐含在作用域层级中 | 缺失 | 需实现 |
| 嵌套类 scope | `getNestedClassifierScope()` | 层级 scopeName 管理 | 缺失 | 需实现 |

**评估：** 类型解析服务已有骨架。关键缺口：**具体 Scope 实现类**（类成员 scope、类型参数 scope、导入 scope、局部 scope 等）。

### Phase 4: STATUS

| | Kotlin K2 | 仓颉 C++ | 当前 CFIR | 差距 |
|---|---|---|---|---|
| 修饰符规范化 | `FirStatusResolveTransformer` + override 链分析 | 内嵌在 PreCheck 中 | `CfirStatusResolveProcessor` | 有实现 |
| 可见性解析 | `StatusComputationSession` 缓存 | 单遍检查 | 存在但需验证深度 | 基本对齐 |

**评估：** STATUS 阶段相对简单，当前实现可覆盖大部分需求。

### Phase 5: EXTENSIONS

| | Kotlin K2 | 仓颉 C++ | 当前 CFIR | 差距 |
|---|---|---|---|---|
| Extend 解析 | N/A（Kotlin 无 extend） | `declToExtendMap` + `builtinTyToExtendMap` | `CfirExtensionsResolveProcessor` | 框架已有 |

**评估：** 这是仓颉特有阶段。框架已有，需在 scope 查找中集成 extend 成员注入。

### Phase 6: IMPLICIT_TYPES（核心缺口一）

| | Kotlin K2 | 仓颉 C++ | 当前 CFIR | 差距 |
|---|---|---|---|---|
| 隐式类型推断 | `ReturnTypeCalculatorForFullBodyResolve` + 约束系统 | `Synthesize()`（自底向上推断） | 空壳 `CfirImplicitTypesResolveProcessor` | **完全缺失** |
| 泛型约束求解 | `ConstraintSystem` + `VariableFixationFinder` | `CollectAndCheckAssumption()` | 完全缺失 | **核心空缺** |

**评估：** 隐式类型推断是语义分析的核心难点。K2 使用完整的约束系统（fresh type variables → constraint incorporation → fixation），仓颉 C++ 用双向类型检查（Synthesis + Check）。当前 CFIR **没有任何推断引擎**。

### Phase 7: BODY_RESOLVE（核心缺口二）

| | Kotlin K2 | 仓颉 C++ | 当前 CFIR | 差距 |
|---|---|---|---|---|
| 函数调用解析 | Tower-based `FirCallResolver` + 多阶段候选过滤 | `FunctionMatchingUnit` 候选匹配 | 完全缺失 | **核心空缺** |
| 重载解析 | `ConeOverloadConflictResolver` 多策略组合 | 候选排序 + 最佳匹配 | 完全缺失 | **核心空缺** |
| 表达式类型合成 | `FirExpressionsResolveTransformer` | `TypeCheckerImpl::Synthesize()` | 完全缺失 | **核心空缺** |
| 数据流分析 | `FirDataFlowAnalyzer` + CFG | 无独立 DFA（后端做） | 完全缺失 | 可后置 |
| Smart cast | `DataFlowAnalyzerContext` | 无 | 完全缺失 | 可后置 |
| 模式匹配检查 | `FirWhenExhaustivenessComputer` | 内嵌在 TypeChecker 中 | 缺失 | 仓颉特有，需实现 |

**评估：** 函数体解析是最大的工程量。需要：调用解析器、重载解析器、表达式类型合成器、赋值兼容性检查等。

### Phase 8: CHECKERS

| | Kotlin K2 | 仓颉 C++ | 当前 CFIR | 差距 |
|---|---|---|---|---|
| 检查器框架 | `CheckersComponent` + Composed 模式 | 内嵌在 TypeChecker 中 | `CheckersComponent` + 生成器 | 框架完备 |
| 具体检查规则 | ~200+ 检查器 | ~大量诊断函数 | `CfirBasicDeclarationCheckers` / `CfirBasicExpressionCheckers` | 极少量规则 |

**评估：** 框架完整，检查规则需要按需逐步添加。

---

## 三、基础设施组件明细

### A. 已完备（可直接使用）

| 组件 | 说明 |
|------|------|
| `CfirResolvePhase` | 9-phase 枚举 + jumping/monotonic 契约 |
| `CfirTotalResolveProcessor` | 全阶段编排器 |
| `CfirResolveProcessor` 层次 | Transformer-based / Global 双模式 |
| `CfirSession` + `ComponentArrayOwner` | O(1) 组件注册/查找 |
| `CfirSymbol` 体系 | 25 种符号类型，覆盖全部声明 |
| `ConeCangJieType` 体系 | 14 种类型（Primitive、Class、Func、Tuple、Array、TypeParam、Error 等） |
| `CfirSymbolProvider` 抽象 | 类查找、顶层可调用查找、包存在性 |
| `CfirScope` 抽象 | processClassifiers/Functions/Properties 处理器模式 |
| 诊断全链路 | Factory(0-4) → Reporter → Collector → Renderer |
| 检查器框架 | Declaration/Expression/Type 三类 + MppCheckerKind + 生成器 |
| Raw CFIR 构建 | PSI → CFIR 转换（lazy body 支持） |

### B. 需要实现的核心组件

#### P0 — 阻塞性缺失

| 组件 | K2 对应 | 仓颉 C++ 对应 | 工作量 |
|------|---------|---------------|--------|
| **表达式类型合成器** | `FirExpressionsResolveTransformer` | `TypeCheckerImpl::Synthesize()` | 大 |
| **调用解析器** | `FirCallResolver` + Tower | `FunctionMatchingUnit` 匹配 | 大 |
| **重载解析器** | `ConeOverloadConflictResolver` | 候选排序/最佳匹配 | 中 |
| **类型兼容性检查** | `AbstractTypeChecker` | `TypeManager::IsSubtype()` | 中 |
| **返回类型推算器** | `ReturnTypeCalculator` | `Synthesize` on body | 中 |

#### P1 — 功能性缺失（限制完整语义覆盖）

| 组件 | K2 对应 | 仓颉 C++ 对应 | 工作量 |
|------|---------|---------------|--------|
| **具体 Scope 实现** | ClassDeclaredMemberScope、ImportingScope 等 ~12 种 | ScopeManager 多级 | 中 |
| **泛型约束系统** | `ConstraintSystem` + Incorporator + Fixation | `CollectAndCheckAssumption()` | 大 |
| **泛型实例化** | TypeSubstitutor | `GenericInstantiationManager` | 中 |
| **跨模块符号加载** | BinaryMetadataProvider | `.cjo` FlatBuffers 加载 | 中 |
| **extend 成员查找** | N/A | `GetDeclExtends()` / `GetAllExtendsByTy()` | 中 |

#### P2 — 增强性缺失（可后置实现）

| 组件 | 说明 |
|------|------|
| 数据流分析 / Smart Cast | 可后置到 checker 阶段或后端 |
| 模式匹配穷尽性检查 | 仓颉特有，可作为 checker |
| Spawn 表达式类型检查 | `Future<T>` 包装，较独立 |
| const 求值 | K2 有独立阶段，可后置 |
| 宏展开后的类型检查 | 依赖宏系统完成度 |

---

## 四、仓颉特有语义 vs Kotlin 差异点

以下是**不能直接复用 K2 架构**的部分：

| 仓颉特性 | K2 无对应 | 实现建议 |
|----------|----------|----------|
| **`extend` 声明** | Kotlin extension function 是语法糖，仓颉 extend 是真实成员注入 | 已有 EXTENSIONS phase，需在 scope 查找中集成 extend 成员 |
| **值类型 `struct`** | Kotlin 仅有 `value class`（inline） | Cone 类型已区分，需在赋值/传参时检查值语义 |
| **`mut` 可变性** | Kotlin 用 `val`/`var` | STATUS phase 需额外处理 mut 方法调用限制 |
| **元组类型** | Kotlin 无原生元组 | `ConeTupleType` 已有，需在表达式解析中支持元组构造/解构 |
| **`spawn` 表达式** | Kotlin 协程是库级 | 需特殊的 `Future<T>` 返回类型推断 |
| **Union / Intersection 类型** | Kotlin 仅内部使用 intersection | 仓颉在类型检查中使用，需在子类型判定中支持 |
| **Range 表达式** | Kotlin range 是库级 | 内建范围类型检查 |
| **模式匹配** | Kotlin `when` 更简单 | 需独立的模式匹配类型推断和穷尽性检查 |
| **C 互操作类型** | Kotlin 有 JNI/cinterop | `ConeCInteropTypes` 已定义，需在类型兼容性中特殊处理 |

---

## 五、K2 FIR 关键架构模式参考

以下是 K2 中被验证有效的架构模式，建议 CFIR 实现时遵循：

### 模式 1：两遍式变换（Two-Pass Transformation）

以超类型解析为例：
1. **Visitor 遍历**（`FirSupertypeResolverVisitor`）— 收集信息
2. **Transformer 遍历**（`FirApplySupertypesTransformer`）— 应用变换

适用于：需要全局信息才能做局部变换的阶段。

### 模式 2：Tower 名称解析

调用候选搜索按层级进行：
```
local scope → class member scope → imports → package → star imports
```
- 每层内按优先级停止
- 显式接收者跳过 tower，直接查目标 scope

### 模式 3：Session-Scoped 计算

每个 transformer 持有：
- `session: CfirSession` — 模块单例
- `scopeSession: ScopeSession` — 本次 resolve 运行的 scope 缓存

### 模式 4：Lazy Resolution 守卫

- `lazyResolveToPhase()` 检查 phase 兼容性
- Jumping phase 单独追踪
- 局部类触发嵌套 phase 执行

### 模式 5：Designated Resolve（Analysis API 模式）

仅解析指定声明（非整个文件），用于 IDE 懒解析：
- `DesignationState` 追踪目标声明
- 只对目标做完整解析，跳过无关声明

---

## 六、仓颉 C++ 编译器关键设计参考

### TypeChecker 三阶段流程

```
PrepareTypeCheck()    // 声明级：符号表、类型解析、继承链、泛型约束
    ↓
DoTypeCheck()         // 表达式级：函数体类型合成、重载解析
    ↓
PostTypeCheck()       // 收尾验证
```

对应到 CFIR resolve 的映射：

| C++ 阶段 | CFIR Phase |
|----------|------------|
| `PrepareTypeCheck` 符号表构建 | IMPORTS |
| `PrepareTypeCheck` 声明类型解析 | TYPES |
| `PrepareTypeCheck` 继承链检查 | SUPER_TYPES |
| `PrepareTypeCheck` 修饰符检查 | STATUS |
| `PrepareTypeCheck` 泛型约束收集 | TYPES (扩展) |
| `DoTypeCheck` 表达式合成 | BODY_RESOLVE |
| `DoTypeCheck` 重载解析 | BODY_RESOLVE |
| `PostTypeCheck` | CHECKERS |

### 双向类型检查（Bidirectional Type Checking）

```
Synthesize(expr) → Type    // 自底向上推断表达式类型
Check(expr, Type) → Bool   // 自顶向下验证表达式符合期望类型
```

建议 CFIR 表达式解析采用类似模式，对应 K2 的 `ResolutionMode`：
- `ContextIndependent` ≈ Synthesize
- `WithExpectedType` ≈ Check

### 类型兼容性检查

仓颉 C++ 的 `TypeManager::CheckTypeCompatibility()` 返回枚举值，而非简单 bool：
- `Compatible` — 直接兼容
- `NeedConversion` — 需要隐式转换（如数值拓宽）
- `Incompatible` — 不兼容

建议 CFIR 子类型检查也采用分级结果。

---

## 七、推荐实现路径

```
Phase 1: 子类型系统 + Scope 实现（奠基）
   ├─ 实现 ConeSubtypeChecker（IsSubtype 判定）
   ├─ 实现 5 种核心 Scope
   │   ├─ CfirPackageScope（包级声明）
   │   ├─ CfirImportScope（导入声明）
   │   ├─ CfirClassMemberScope（类成员）
   │   ├─ CfirTypeParameterScope（类型参数）
   │   └─ CfirLocalScope（局部变量）
   └─ 实现 extend 成员注入到 scope

Phase 2: 表达式类型合成（核心突破）
   ├─ CfirExpressionResolveTransformer
   │   ├─ 字面量类型合成
   │   ├─ 变量引用解析
   │   ├─ 属性访问解析
   │   └─ 简单函数调用（单候选）
   ├─ 基础调用解析（无重载，单候选匹配）
   └─ 返回类型推算器（显式返回类型的函数体）

Phase 3: 调用解析 + 重载（功能完整）
   ├─ Tower-based 候选收集（或简化版）
   ├─ 重载解析策略
   │   ├─ 参数数量匹配
   │   ├─ 类型兼容性排序
   │   └─ 最佳匹配选择
   └─ 泛型类型参数推断（基础约束系统）

Phase 4: 高级特性（语义完备）
   ├─ 完整泛型约束系统
   ├─ 模式匹配类型推断 + 穷尽性
   ├─ Smart cast / DFA
   └─ 跨模块符号加载（.cjo）
```

---

## 八、当前模块文件统计

| 模块 | src 文件数 | gen 文件数 | 状态 |
|------|-----------|-----------|------|
| `cfir:resolve` | 27 | 0 | 声明级有实现，表达式级是空壳 |
| `cfir:cfir-tree` | 18 | 219 | 完善（声明/表达式/模式/引用/类型 全覆盖） |
| `cfir:symbols` | 17 | 0 | Provider/Scope 抽象完整 |
| `cfir:checkers` | 11 | 12 | 框架完备，规则极少 |
| `cfir:diagnostics` | 29 | 2 | 全链路完备 |
| `cfir:cfir-cones` | 17 | 0 | 类型系统完善（14 种类型） |
| `cfir:cfir-common` | 6 | 0 | Session 核心 |
| `cfir:raw-cfir` | 6 | 0 | PSI → CFIR 转换 |

---

## 九、总结

**评级：骨架完备，肌肉待长。**

- **可改写树契约已就绪** — CFIR 树已支持选择性可变（`var` 字段、`replaceXxx()` 方法、`transformChildren()` 原地变换），resolve 处理器可正常推进声明的 phase 状态。
- **框架层**（Phase pipeline、Session、Symbol/Scope 抽象、Diagnostic、Checker）已达到 K2 结构对齐度，可直接在此基础上填充实现。
- **声明级解析**（IMPORTS → STATUS）已有实际处理逻辑，距离生产可用还需补全具体 Scope 类型和跨模块加载。
- **表达式级解析**（IMPLICIT_TYPES → BODY_RESOLVE）是核心空缺，需要从零实现子类型判定、表达式合成、调用解析三大引擎。
- 仓颉特有语义（extend、struct 值语义、mut、元组、spawn、模式匹配）需要在 K2 框架基础上做针对性扩展，扩展点已被框架预留。

**建议从 Phase 1（子类型系统 + Scope 实现）开始，这是表达式解析的前置条件。**
