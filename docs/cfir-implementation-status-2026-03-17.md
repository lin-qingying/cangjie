# CFIR 实现状态报告（基于代码审查）

日期：2026-03-17

> 本报告通过逐文件阅读源码撰写，标注每个组件的**真实实现深度**，
> 区分"有代码"和"逻辑真正可工作"，纠正旧版文档中的偏差。

---

## 一、旧文档勘误

| 旧文档说法 | 实际情况 |
|-----------|---------|
| ".cjo 反序列化未实现"（P1 #4 完成度 30%） | `CfirDeclDeserializer`（447 行）已完整实现，覆盖 11 种声明类型的 FlatBuffers 反序列化 |
| "缺失表达式类型解析（for-in/loop/try/throw/subscript/lambda/range/spawn）"列为 P1 待做 | `CfirExpressionsResolveTransformer` 已有这些表达式的 transform 方法，均有实际逻辑 |
| 未提及 CHIR 模块 | `compiler/chir` 已有 39 个源文件，接口+数据类完整定义 |
| 未提及 codegen 模块 | `compiler/codegen` 已有 25 个源文件，JNI LLVM 后端可用 |
| 未提及 entrypoint 模块 | `cfir/entrypoint` 已有 13 个源文件，Session 创建+管线编排可用 |
| "表达式级解析 95%" | 过度乐观——约束系统、继承成员查找、match 绑定变量等核心路径有空洞 |
| "Scope 实现 95%" | 缺少继承成员合并 scope，子类无法查到父类方法 |

---

## 二、12 阶段编译管线总览

| # | 阶段 | 模块 | 状态 | 说明 |
|---|------|------|------|------|
| 1 | LOAD_PLUGINS | `:compiler:plugins`（规划） | ❌ 不存在 | 无代码 |
| 2 | PARSE | `:psi` | ✅ 可用 | JFlex + PsiParser |
| 3 | CONDITION_COMPILE | `:compiler:condition-compile`（规划） | ❌ 不存在 | @When 条件编译 |
| 4 | IMPORT_PACKAGE | `:cfir:cfir-serialization` | ⚠️ 反序列化已实现 | .cjo 读取可工作，序列化（写入）不存在 |
| 5 | MACRO_EXPAND | `:compiler:macro`（规划） | ❌ 不存在 | 宏系统 |
| 6 | CFIR_BUILD | `:cfir:raw-cfir:psi2cfir` | ✅ 可用 | PSI→RawCFIR，覆盖主要声明和表达式 |
| 7 | CFIR_RESOLVE | `:cfir:resolve` + `:cfir:checkers` | ✅ 核心可用 | 详见第三节 |
| 8 | FINALIZE | `:compiler:finalize`（规划） | ❌ 不存在 | 泛型单态化 |
| 9 | MANGLING | `:compiler:mangling`（规划） | ❌ 不存在 | 名称修饰 |
| 10 | SAVE_CJO | `:cfir:cfir-serialization` | ❌ 仅有反序列化 | 序列化写入不存在 |
| 11 | CFIR2CHIR | `:compiler:chir` | ⚠️ 数据模型在，转换器不在 | 详见第五节 |
| 12 | CODEGEN | `:compiler:codegen` | ⚠️ JNI 绑定可用，lowering 不在 | 详见第六节 |

---

## 三、CFIR Resolve 阶段——逐组件代码审查

### 3.1 表达式类型合成（CfirExpressionsResolveTransformer，911 行）

覆盖 **20+ 种表达式**，每个 transform 方法均有真实逻辑：

| 表达式 | 方法 | 实现质量 |
|--------|------|---------|
| 字面量 | `transformLiteralExpression` | ✅ 完整，含 IdealType 到期望类型的确定化 |
| 属性访问 | `transformPropertyAccess` | ✅ 完整，有接收者/无接收者两条路径 |
| 限定访问 | `transformQualifiedAccess` | ✅ 完整 |
| 函数调用 | `transformFunctionCall` | ✅ 完整，Phase 3 调用解析 + 旧版回退 |
| 块表达式 | `transformBlock` | ✅ 完整 |
| if 表达式 | `transformIfExpression` | ⚠️ 多分支类型用 `ConeUnionType`，无真正 LUB 计算 |
| match 表达式 | `transformMatchExpression` | ⚠️ 模式解析有逻辑，但 `storePatternBinding()` 是空方法 |
| return | `transformReturnExpression` | ✅ 完整 |
| 赋值 | `transformAssignment` | ✅ 完整 |
| 元组字面量 | `transformTupleLiteral` | ✅ 完整 |
| 数组字面量 | `transformArrayLiteral` | ⚠️ 元素类型取第一个元素，无统一推断 |
| 字符串插值 | `transformStringInterpolation` | ✅ 完整 |
| 比较表达式 | `transformComparisonExpression` | ✅ 含内建操作符解析 |
| 二元操作 | `transformBinaryOp` | ✅ AND/OR/COALESCING/PIPELINE |
| 类型操作 | `transformTypeOperator` | ✅ is/as |
| for-in | `transformForInExpression` | ✅ 迭代变量类型推断 |
| while/loop | `transformLoopExpression` | ✅ 返回 Unit |
| throw | `transformThrowExpression` | ✅ 返回 Nothing |
| try/catch | `transformTryExpression` | ⚠️ `commonSupertype()` 简化为取第一个类型 |
| 下标访问 | `transformSubscriptExpression` | ✅ Tuple/Array/VArray 分支处理 |
| lambda | `transformLambdaExpression` | ⚠️ 从期望类型推断参数类型，无独立推断 |
| range | `transformRangeExpression` | ✅ 返回 `Range<T>` |
| spawn | `transformSpawnExpression` | ✅ 返回 `Future<T>` |
| 错误表达式 | `transformErrorExpression` | ✅ |

### 3.2 调用解析管线

| 组件 | 行数 | 实现质量 |
|------|------|---------|
| `CfirCallResolver` | 162 | ✅ Tower→bestCandidates→conflictResolver 三阶段完整 |
| `CfirTowerResolver` | 182 | ✅ scope 塔遍历 + TowerGroup 分类 + 剪枝 |
| `CfirCandidateCollector` | — | ✅ 候选收集与排序 |
| `CfirOverloadConflictResolver` | 115 | ✅ 三轮消歧：参数特化度→非泛型优先→默认值少优先 |
| `CfirResolutionStageRunner` | — | ✅ 验证阶段执行 |
| `CfirMapArguments` | — | ✅ 实参到形参的映射 |
| `CfirCheckArguments` | 76 | ✅ 逐参数子类型检查 |
| `CfirCheckVisibility` | — | ✅ 可见性验证 |
| `CfirInferTypeArguments` | 163 | ✅ 显式类型参数 + 约束推断两条路径 |
| `CfirBuiltinOperatorResolver` | 211 | ✅ 算术/位运算/移位/比较/一元，含混合宽度 |

### 3.3 声明级解析（CfirDeclarationsResolveTransformer，310 行）

| 功能 | 状态 |
|------|------|
| File scope 管理（import scope 创建） | ✅ |
| Class scope 管理（类型参数 + 成员 + extend） | ✅ |
| Function scope 管理（参数 scope + 函数体） | ✅ |
| Property/Variable 隐式类型推断 | ✅ 从初始化器推断 |
| Block scope（局部变量） | ✅ |
| PatternVariable 处理 | ✅ 含嵌套 pattern 绑定名收集 |
| Phase 推进（resolvePhase bumping） | ✅ |

### 3.4 有代码但存在严重简化的组件

| 组件 | 行数 | 问题 |
|------|------|------|
| `CfirConstraintSystemImpl` | 190 | `computeFixedType()` 取 `lowerBounds.first()` 或 `upperBounds.first()`——**无 incorporation（传递性约束合并）、无 fixation 排序**。单约束场景可工作，多约束泛型会推断错误 |
| `CfirDataFlowAnalyzerContext` | 18 | **空壳类**，无任何字段或方法。`is` 检查后无 smart cast，无变量初始化跟踪，无可达性分析 |
| `commonSupertype()` | 8 | 返回 `types.first()`——**if/try/match 多分支不同类型时结果不准** |
| `storePatternBinding()` | 4 | 方法体为空（`// TODO`）——**match 中绑定变量不会注册到 scope，后续引用报 unresolved** |
| `CfirReturnTypeCalculatorForFullBodyResolve` | 36 | 仅从 `CfirResolvedTypeRef` 提取——**无递归函数返回类型推断** |

### 3.5 Scope 实现

| Scope | 文件 | 状态 |
|-------|------|------|
| `CfirClassDeclaredMemberScope` | 69 行 | ✅ 懒初始化成员索引，按名称检索 |
| `CfirLocalScopeImpl` | 56 行 | ✅ 可变 scope，支持动态添加 |
| `CfirExplicitSimpleImportingScope` | — | ✅ 简单导入 |
| `CfirExplicitStarImportingScope` | — | ✅ 星号导入 |
| `CfirPackageMemberScope` | — | ✅ 包级成员 |
| `CfirTypeParameterScopeImpl` | — | ✅ 类型参数 |
| `CfirExtendMemberScope` | — | ✅ extend 注入成员 |
| **继承成员 scope** | — | ❌ **不存在**。`ClassDeclaredMemberScope` 仅索引直接声明的成员，**子类调用继承自父类的方法会报 unresolved** |

---

## 四、Checkers 模块——基于代码的检查器清单

### 4.1 已注册的检查器（17 个）

**声明类（9 个）：**

| 检查器 | 实现质量 |
|--------|---------|
| `CfirInitializerTypeMismatchChecker` | ✅ 变量初始化类型检查 |
| `CfirPropertyInitializerTypeMismatchChecker` | ✅ 属性初始化类型检查 |
| `CfirExtendTargetLegalityChecker` | ✅ extend 目标合法性 |
| `CfirExtendInterfaceKindChecker` | ✅ extend 接口种类 |
| `CfirExtendDuplicateInterfaceChecker` | ✅ extend 重复接口 |
| `CfirExtendOrphanRuleChecker` | ✅ extend 孤儿规则 |
| `CfirExtendGenericUsageChecker` | ✅ extend 泛型使用 |
| `CfirExtendSpecializationConflictChecker` | ✅ extend 特化冲突 |
| `CfirExtendDefaultImplementationConflictChecker` | ✅ extend 默认实现冲突 |

**表达式类（8 个）：**

| 检查器 | 实现质量 |
|--------|---------|
| `CfirIfConditionTypeMismatchChecker` | ✅ if 条件必须为 Bool |
| `CfirLoopConditionTypeMismatchChecker` | ✅ while 条件必须为 Bool |
| `CfirAssignmentTypeMismatchChecker` | ✅ 赋值类型不匹配 |
| `CfirReturnTypeMismatchChecker` | ✅ 返回值类型不匹配 |
| `CfirArgumentTypeMismatchChecker` | ✅ 函数参数类型不匹配 |
| `CfirLiteralNumericOverflowChecker` | ✅ 字面量溢出 |
| `CfirConstEvalArithmeticChecker` | ✅ 常量算术检查 |
| `CfirMatchExhaustivenessChecker` | ⚠️ **枚举穷尽性不工作**（见下） |

### 4.2 检查器中的已知空洞

| 问题 | 代码证据 |
|------|---------|
| **枚举穷尽性检查不工作** | `getEnumConstructorNames()` 直接 `return emptyList()`（第 151-156 行），无法获取枚举构造器列表 |
| **布尔穷尽性过于保守** | `computeMissingBooleanCases()` 无法区分 true/false 常量值，`hasTrue = true` 后 `hasFalse` 仍可能为 false |

### 4.3 缺失的检查器类别

- 未使用变量/导入
- 不可达代码
- 可见性违规（跨模块 private/internal 访问）
- mut 限制（对不可变接收者调用 mut 方法）
- 继承合法性（非 extend 的类继承规则）
- 重复定义检查
- 类型参数约束违反

---

## 五、cfir-serialization 模块（10 个文件）

| 组件 | 行数 | 状态 |
|------|------|------|
| `CfirDeclDeserializer` | 447 | ✅ **完整**：Class/Interface/Struct/Enum/Function/Property/Variable/Extend/TypeAlias/TypeParameter/ValueParameter 全覆盖 |
| `CfirTypeDeserializer` | — | ✅ 从 FlatBuffers SemaTy 重建 ConeCangJieType |
| `CfirDeserializationContext` | — | ✅ 缓存 + moduleData |
| `CfirDeserializedSymbolProvider` | — | ✅ 按 ClassId/Name 查找反序列化符号 |
| `CjoManager` | — | ✅ .cjo 文件管理 |
| `CjoSearchPath` | — | ✅ 包搜索路径 |
| `ModuleDataProvider` | — | ✅ 模块数据 |
| **序列化（CFIR→.cjo 写入）** | 0 | ❌ **完全不存在** |

---

## 六、后端模块

### 6.1 CHIR 模块（39 个文件）——纯接口层设计

接口 + 数据类定义齐全，**无任何变换逻辑**。

| 分类 | 文件 | 状态 |
|------|------|------|
| 数据模型（ChirDeclaration/Expression/Type/Value） | 8 个接口 + Default 实现 | ✅ 定义完整 |
| 控制流（ChirBlock/Instruction） | 接口定义 | ✅ |
| 符号表（ChirSymbol/SymbolTable/ReferenceBinder） | 3 个 | ✅ 接口 |
| Pipeline（Scheduler/Pass/Gate/Cache） | 5 个 | ✅ 接口 |
| 分析（DataFlowEngine/BaselineAnalyses） | 3 个 | ✅ 接口 |
| 序列化（PackageCodec/Schema/RoundTrip） | 4 个 | ✅ 接口 |
| **CFIR→CHIR 转换器** | — | ❌ **不存在** |
| **任何实际 Pass 实现** | — | ❌ **不存在** |

### 6.2 Codegen 模块（25 个文件）

| 组件 | 状态 |
|------|------|
| `JniLlvmBackend`（67 行） | ✅ **可工作**——通过 JNI 调用 LLVM C API（contextCreate/moduleParseAssembly/writeBitcode/moduleVerify） |
| `LlvmBackendFactory`（33 行） | ✅ 含版本检查 |
| `LlvmBackend` 接口 + `LlvmBackendCapabilities` | ✅ |
| `ChirToLlvmLoweringFramework` | ⚠️ 框架定义 |
| `IRBuilder` / `TypeLowering` / `ExpressionLoweringDispatcher` | ⚠️ 有文件，**需进一步验证逻辑深度** |
| `CGModule` / `CGContext` / `CGFunction` | ⚠️ 上下文定义 |
| **从 CHIR 到 LLVM IR 文本的完整 lowering** | ❌ **不存在**（前提 CFIR→CHIR 也不存在） |

### 6.3 Entrypoint 模块（13 个文件）

| 组件 | 状态 |
|------|------|
| `CfirDefaultSessionFactory` / `CfirAbstractSessionFactory` | ✅ Session 创建和组件注册 |
| `CfirSessionConfigurator` | ✅ |
| `CfirFrontendPipelinePhase` | ✅ 前端阶段定义 |
| `analyse.kt` | ✅ 分析入口 |
| `CfirTotalResolveProcessor`（entrypoint 版） | ✅ |
| `ComponentsContainers` / `StructuredProviders` | ✅ 组件组装 |
| `CheckersContainers` | ✅ Checker 注册 |

### 6.4 测试基础设施

| 组件 | 状态 |
|------|------|
| `CfirCliFacade`（101 行） | ✅ CLI 管线测试门面，可串联 Configuration→Frontend→Output |
| `CfirOutputArtifact` | ✅ |
| 测试服务注册 | ✅ |

---

## 七、实际可工作的端到端链路

```
PSI ──→ RawCfirBuilder ──→ CFIR 树
         │
         ├─ 声明级解析（import/supertype/type/status/extension）  ✅
         ├─ 表达式类型合成（20+ 种表达式）                        ✅
         ├─ 函数调用解析（Tower + 4 阶段验证 + 重载消歧）         ✅
         ├─ 泛型推断（简化约束系统）                              ⚠️ 单约束可工作
         ├─ 内建操作符（算术/比较/位运算/一元 + 数值拓宽）         ✅
         ├─ 17 个检查器                                          ✅ (枚举穷尽性除外)
         └─ .cjo 反序列化 ──→ 库符号加载                         ✅

         ↓↓↓ 以下链路断裂 ↓↓↓

         CFIR ──✖──→ CHIR（39 个接口文件，转换器不存在）
         CHIR ──✖──→ LLVM IR（JNI 绑定可用，lowering 代码不存在）
         .cjo 序列化（写入方向完全不存在）
```

---

## 八、基于代码的真实缺口清单

### 🔴 P0 — CFIR 内部影响正确性的空洞

| # | 缺口 | 代码证据 | 影响 |
|---|------|---------|------|
| 1 | **继承成员 scope 缺失** | `CfirClassDeclaredMemberScope` 仅索引 `klass.declarations`，不递归查找父类 | 子类调用继承方法报 unresolved |
| 2 | **约束系统 incorporation 缺失** | `computeFixedType()` 取 `first()`，无传递性合并 | 多约束泛型推断错误 |
| 3 | **match 绑定变量未注册** | `storePatternBinding()` 为空方法 | match 分支中的绑定变量后续不可见 |
| 4 | **commonSupertype 简化** | `return types.first()` | if/try/match 多类型分支结果类型不准 |
| 5 | **枚举穷尽性不工作** | `getEnumConstructorNames()` 返回 `emptyList()` | match 枚举不报非穷尽警告 |

### 🟠 P1 — 阻塞端到端编译的断点

| # | 缺口 | 现状 | 工作量估算 |
|---|------|------|----------|
| 1 | **.cjo 序列化（写入）** | 反序列化有 447 行完整实现，序列化代码为零 | 中 |
| 2 | **CFIR→CHIR 转换器** | CHIR 数据模型 39 个文件已定义，无转换逻辑 | 大 |
| 3 | **CHIR→LLVM IR lowering** | JNI 后端可调用 LLVM API，无 IR 生成代码 | 大 |

### 🟡 P2 — 功能性缺失

| # | 缺口 | 说明 |
|---|------|------|
| 1 | 数据流分析 / Smart Cast | `CfirDataFlowAnalyzerContext` 是 18 行空壳 |
| 2 | 缺失的检查器（~10 类） | 未使用变量、不可达代码、可见性违规、mut 限制等 |
| 3 | 用户自定义操作符重载 | 仅有内建操作符，无 Equatable/Comparable 接口查找 |
| 4 | lambda 独立参数类型推断 | 当前仅从期望类型推断，无 SAM 转换 |
| 5 | 递归函数返回类型推断 | `ReturnTypeCalculatorForFullBodyResolve` 仅提取已解析类型 |

### ⬜ P3 — 完全不存在的编译阶段

| 阶段 | 模块 |
|------|------|
| LOAD_PLUGINS | `:compiler:plugins` |
| CONDITION_COMPILE | `:compiler:condition-compile` |
| MACRO_EXPAND | `:compiler:macro` |
| FINALIZE（泛型单态化） | `:compiler:finalize` |
| MANGLING（名称修饰） | `:compiler:mangling` |

---

## 九、文件规模统计

| 区域 | src 文件 | gen 文件 | test 文件 |
|------|---------|---------|----------|
| cfir/ 全部 | 305 | 246 | 53 |
| compiler/ 全部 | 107 | — | 33 |

### Resolve 模块核心文件行数

| 文件 | 行数 |
|------|------|
| `CfirExpressionsResolveTransformer.kt` | 911 |
| `CfirDeclDeserializer.kt` | 447 |
| `CfirDeclarationsResolveTransformer.kt` | 310 |
| `CfirBuiltinOperatorResolver.kt` | 211 |
| `CfirConstraintSystemImpl.kt` | 190 |
| `CfirTowerResolver.kt` | 182 |
| `CfirCallResolver.kt` | 162 |
| `CfirInferTypeArguments.kt` | 163 |
| `CfirMatchExhaustivenessChecker.kt` | 161 |
| `ConeOverloadConflictResolver.kt` | 115 |

---

## 十、修正后的完成度评估

```
Resolve 管线框架（Phase/Processor/Session）  ████████████████████  100%
声明级解析（IMPORTS→EXTENSIONS）              ██████████████████░░   90%
表达式类型合成（20+ 种表达式覆盖）            █████████████████░░░   85%  ← 有空洞
调用解析 + 重载消歧                          █████████████████░░░   85%
内建操作符 + 数值拓宽                        ███████████████████░   95%
Scope 实现（7 种，缺继承合并）               ████████████████░░░░   80%  ← 旧文档高估
泛型约束系统（简化版，缺 incorporation）      ██████████████░░░░░░   70%  ← 旧文档高估
检查器（17 个，枚举穷尽性不工作）            ███████████████░░░░░   75%
诊断系统                                    ███████████████████░   95%
.cjo 反序列化                               ███████████████████░   95%  ← 旧文档低估
.cjo 序列化（写入）                          ░░░░░░░░░░░░░░░░░░░░    0%
CHIR 数据模型                               ████████████████░░░░   80%  ← 旧文档未提及
CFIR→CHIR 转换                              ░░░░░░░░░░░░░░░░░░░░    0%
LLVM 后端 JNI 绑定                          ███████████████████░   95%  ← 旧文档未提及
CHIR→LLVM IR lowering                       ░░░░░░░░░░░░░░░░░░░░    0%
数据流分析 / Smart Cast                      ░░░░░░░░░░░░░░░░░░░░    0%
```

**核心判断：** CFIR Resolve 阶段已有大量可工作的代码，但关键路径上存在 5 个影响正确性的空洞（继承成员、约束 incorporation、match 绑定、LUB 计算、枚举穷尽性）。端到端编译被 CFIR→CHIR→LLVM 链路的完全缺失所阻塞。