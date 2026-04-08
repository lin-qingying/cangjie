# 仓颉编译器项目实现状态报告

日期：2026-03-19

> 本报告通过逐模块、逐文件源码阅读撰写，标注每个组件的**真实实现深度**，
> 区分"有代码"和"逻辑真正可工作"。覆盖项目全部模块，不仅限于 CFIR。

---

## 一、项目总览

- **项目定位**：基于 Kotlin/JVM 的仓颉编程语言编译器，架构参考 Kotlin K2
- **代码规模**：~1,200+ 个 .kt 源文件，其中手写 ~500+，生成 ~700+
- **Gradle 模块数**：40 个（含聚合模块）
- **最新提交**：`8e2ef73 Fix 5 P0 CFIR correctness issues`

---

## 二、12 阶段编译管线状态

| # | 阶段 | 模块 | 状态 | 说明 |
|---|------|------|------|------|
| 1 | LOAD_PLUGINS | `:compiler:plugin` | ❌ 占位 | 无代码 |
| 2 | PARSE | `:psi` | ✅ 完整可用 | JFlex + PsiParser，424 个文件，42+ 种 PSI 节点 |
| 3 | CONDITION_COMPILE | — | ❌ 不存在 | @When 条件编译 |
| 4 | IMPORT_PACKAGE | `:cfir:cfir-serialization` | ⚠️ 反序列化可用 | .cjo 读取完整（447 行），序列化写入不存在 |
| 5 | MACRO_EXPAND | — | ❌ 不存在 | 宏系统 |
| 6 | CFIR_BUILD | `:cfir:raw-cfir:psi2cfir` | ✅ 可用 | PSI→Raw CFIR，覆盖主要声明和表达式 |
| 7 | CFIR_RESOLVE | `:cfir:resolve` + `:cfir:checkers` | ✅ 核心可用 | 详见第五节 |
| 8 | FINALIZE | — | ❌ 不存在 | 泛型单态化 |
| 9 | MANGLING | — | ❌ 不存在 | 名称修饰 |
| 10 | SAVE_CJO | `:cfir:cfir-serialization` | ❌ 仅有反序列化 | 序列化写入代码为零 |
| 11 | CFIR2CHIR | `:compiler:chir` | ⚠️ 数据模型完整，转换器不存在 | 39 个文件，接口 + 框架 + 数据流分析 |
| 12 | CODEGEN | `:compiler:codegen` | ✅ 框架完整 | 25 个文件，表达式降低 + LLVM IR 生成 |

---

## 三、基础设施层

### 3.1 `:util` — 通用工具库（15 个文件，1,579 行）

| 组件 | 实现质量 | 说明 |
|------|---------|------|
| `Printer` / `SmartPrinter` | ✅ | 带缩进打印器，14 个公共方法 |
| 集合工具（758 行） | ✅ | O(1) 优化、DSL 构建、容量智能分配、50+ 个工具方法 |
| 异常框架 | ✅ | 可附件异常（`CangJieExceptionWithAttachments`） |
| 字符串工具 | ✅ | 大小写转换等 |

### 3.2 `:common` — 领域基础模型（26 个文件，3,089 行）

| 组件 | 实现质量 | 说明 |
|------|---------|------|
| 名称系统（`Name`、`FqName`、`ClassId`、`CallableId`） | ✅ | 完整，含路径遍历、缓存 |
| 描述符（`Visibility`、`Modality`） | ✅ | 可见性比较、修饰符枚举 |
| 内置类型（`PrimitiveType`） | ✅ | 18 种基本类型（含 IntNative/UIntNative） |
| 消息系统（`CompilerMessageSeverity`、`MessageCollector`） | ✅ | 从 compiler:config 迁入 |
| `LanguageVersionSettings` | ✅ | 从 compiler:config 迁入 |
| 高性能容器（`ComponentArrayOwner`、`ArrayMap`） | ✅ | O(1) 组件注册/查找 |

### 3.3 `:common:diagnostics` — 诊断框架（34 个文件）

| 组件 | 实现质量 | 说明 |
|------|---------|------|
| 诊断工厂（`CjDiagnosticFactory0-4`） | ✅ | 0-4 参数泛型工厂，含弃用诊断变体 |
| 诊断模型（`CjDiagnostic`，308 行） | ✅ | 密封类，3 种源位置（PSI/LightTree/OffsetsOnly）× 5 级参数 = 15 个数据类 |
| 位置策略（6 个策略类） | ✅ | PSI 标记、轻树标记、纯偏移标记 |
| 报告与收集（`DiagnosticReporter`、`BaseDiagnosticsCollector`） | ✅ | 悬挂式报告器支持延迟抑制检查 |
| 去重报告（`DeduplicatingDiagnosticReporter`） | ✅ | 防止重复诊断 |
| 渲染框架（`DiagnosticRenderer`、`DiagnosticRendererFactory`） | ✅ | 工厂模式解耦，支持扩展 |
| 诊断抑制缓存（`AbstractCangJieSuppressCache`） | ✅ | 框架完整 |

### 3.4 `:compiler:config` — 编译器配置（12 个文件）

| 组件 | 实现质量 | 说明 |
|------|---------|------|
| `CompilerConfiguration` | ✅ | 键值配置容器 |
| `ContentRoots` | ✅ | 源码根目录模型 |

### 3.5 `:compiler:phaser` — 编译阶段管理（7 个文件，219 行）

| 组件 | 实现质量 | 说明 |
|------|---------|------|
| `NamedCompilerPhase` | ✅ | 前/后条件检查、Action 执行、性能分析、嵌套 subphase |
| `PhaseConfig` / `PhaseSet` / `PhaserState` | ✅ | 对齐 K2 设计 |

### 3.6 `:compiler:arguments` — 编译器参数（11 个文件，363 行）

| 组件 | 实现质量 | 说明 |
|------|---------|------|
| `CangJieCompilerArguments` | ✅ | DSL 式参数定义框架，Builder 模式，版本生命周期管理 |

---

## 四、前端解析层

### 4.1 `:psi` — PSI 模块（424 个文件，8,396 行）

| 组件 | 实现质量 | 说明 |
|------|---------|------|
| 词法分析（`CangJieLexer`） | ✅ | JFlex，完整关键字和 Token 定义 |
| 语法分析（`CangJieParser`、`CangJieLightParser`） | ✅ | 两层解析器（标准 + 轻树），增量解析支持 |
| PSI 节点接口（42+ 种） | ✅ | 覆盖声明、表达式、模式、类型 |
| CDoc 文档注释系统 | ✅ | 独立词法/解析/PSI 子系统 |
| 宏调用语言 | ✅ | `CangJieMacroCallLanguage`、`CangJieMacroCallParserDefinition` |
| 注解器 | ✅ | `CangJieAnnotator` 高亮/错误标记 |

---

## 五、CFIR 核心层

### 5.1 `:cfir:cfir-common` — Session 基础设施（8 个文件）

| 组件 | 实现质量 | 说明 |
|------|---------|------|
| `CfirSession` | ✅ | 继承 `ComponentArrayOwner`，O(1) 组件查找 |
| `CfirModuleData` | ✅ | Source / Library / BinaryDependency 三种，142 行 |
| `CfirPlatform` | ✅ | DEFAULT / OHOS / LINUX / WINDOWS / MACOS |

### 5.2 `:cfir:cfir-cones` — 类型系统（21 个文件）

| 类型 | 实现质量 | 说明 |
|------|---------|------|
| 基本类型（`ConePrimitiveType`） | ✅ | Unit/Bool/Int*/UInt*/Float*/Rune/Nothing/Ideal* |
| 名义类型（`ConeClassLikeType`、`ConeStructType`、`ConeEnumType`） | ✅ | class/struct/enum/interface |
| 函数类型（`ConeFuncType`） | ✅ | 含 isCFunc 标记 C 函数类型 |
| 集合类型（`ConeTupleType`、`ConeArrayType`、`ConeVArrayType`） | ✅ | 元组/数组/定长数组 |
| 指针类型（`ConePointerType`、`ConeCStringType`） | ✅ | C 互操作 |
| 泛型（`ConeTypeParameterType`） | ✅ | 类型参数 |
| 特殊类型（`ConeIntersectionType`、`ConeUnionType`、`ConeErrorType`） | ✅ | 交叉/联合/错误 |
| 子类型检查（`ConeSubtypeChecker`） | ✅ | 有实现 |

### 5.3 `:cfir:cfir-tree` — IR 树（26 手写 + 238 生成 = 264 个文件）

| 分类 | 节点数 | 说明 |
|------|--------|------|
| 声明节点 | 55 | File/Class/Function/Property/Constructor/Extend/TypeAlias/Macro/Finalizer/PatternVariable 等 |
| 表达式节点 | 45 | Call/If/Match/Loop/ForIn/Try/Lambda/BinaryOp/Spawn/Synchronized/Unsafe/Macro/Quote 等 |
| 模式节点 | 7 | Binding/Const/Enum/Tuple/Type/Wildcard/Or/Expression |
| 类型引用节点 | 8 | Resolved/User/Basic/Function/Tuple/VArray/Error/Implicit |
| 访问者 | 5 | Visitor/VisitorVoid/DefaultVisitor/DefaultVisitorVoid/Transformer |
| CfirRenderer | 1 | 918 行，完整树渲染器 |

### 5.4 `:cfir:symbols` — 符号与作用域（31 个文件）

| 组件 | 实现质量 | 说明 |
|------|---------|------|
| `CfirSourceSymbolProvider` | ✅ | 源码模块符号查询 |
| `CfirBuiltinSymbolProvider` | ✅ | 内置符号（基本类型等） |
| `CfirCompositeSymbolProvider` | ✅ | 组合查询 |
| `CfirPackageMemberScope` | ✅ | 包级作用域 |
| `CfirClassDeclaredMemberScope` | ✅ | 类成员作用域（仅直接声明，**不含继承成员**） |
| `CfirLocalScopeImpl` | ✅ | 本地作用域 |
| `CfirExplicitSimpleImportingScope` | ✅ | 简单导入 |
| `CfirExplicitStarImportingScope` | ✅ | 星号导入 |
| `CfirTypeParameterScopeImpl` | ✅ | 泛型参数 |
| `CfirExtendMemberScope` | ✅ | extend 注入成员 |
| **继承成员合并 scope** | ❌ | **不存在**，子类调用继承方法报 unresolved |

### 5.5 `:cfir:resolve` — 语义解析（79 个文件）

#### 声明级解析

| 组件 | 实现质量 | 说明 |
|------|---------|------|
| `CfirImportsResolveProcessor` | ✅ | Simple import + Star import |
| `CfirSuperTypesResolveProcessor` | ✅ | 含 extend rules |
| `CfirTypeResolveTransformer` | ✅ | 显式 + 用户类型 |
| `CfirStatusResolveProcessor` | ✅ | 修饰符规范化 |
| `CfirExtensionsResolveProcessor` | ✅ | 仓颉特有 extend 解析 |
| `CfirSuperTypeChecker` | ✅ | 循环检测 |

#### 表达式类型合成（`CfirExpressionsResolveTransformer`，911 行）

| 表达式 | 实现质量 | 说明 |
|--------|---------|------|
| 字面量 | ✅ | 含 IdealType → 期望类型确定化 |
| 属性访问 / 限定访问 | ✅ | 有/无接收者两条路径 |
| 函数调用 | ✅ | Phase 3 调用解析 + 旧版回退 |
| 块/if/return/赋值 | ✅ | 完整 |
| 元组/数组/字符串插值 | ✅ | 完整 |
| 比较/二元操作/类型操作 | ✅ | AND/OR/COALESCING/is/as |
| for-in/while/loop | ✅ | 迭代变量推断 / Unit 返回 |
| throw/try-catch | ✅/⚠️ | throw → Nothing；`commonSupertype()` 简化为取 first() |
| 下标访问 | ✅ | Tuple/Array/VArray 分支 |
| lambda | ⚠️ | 仅从期望类型推断，无独立推断 |
| range/spawn | ✅ | Range\<T\> / Future\<T\> |
| match | ⚠️ | `storePatternBinding()` 是空方法 |

#### 调用解析管线

| 组件 | 实现质量 | 说明 |
|------|---------|------|
| `CfirCallResolver` (162 行) | ✅ | Tower → bestCandidates → conflictResolver 三阶段 |
| `CfirTowerResolver` (182 行) | ✅ | scope 塔遍历 + TowerGroup 分类 + 剪枝 |
| `CfirOverloadConflictResolver` (115 行) | ✅ | 三轮消歧：参数特化度 → 非泛型优先 → 默认值少优先 |
| `CfirMapArguments` | ✅ | 实参到形参映射 |
| `CfirCheckArguments` (76 行) | ✅ | 逐参数子类型检查 |
| `CfirCheckVisibility` | ✅ | 可见性验证 |
| `CfirInferTypeArguments` (163 行) | ✅ | 显式类型参数 + 约束推断 |
| `CfirBuiltinOperatorResolver` (211 行) | ✅ | 算术/位运算/移位/比较/一元，含混合宽度 |

#### 约束系统

| 组件 | 实现质量 | 说明 |
|------|---------|------|
| `CfirConstraintSystemImpl` (190 行) | ⚠️ | `computeFixedType()` 取 `first()`，**无 incorporation、无 fixation 排序**，单约束可工作，多约束会出错 |
| `CfirDataFlowAnalyzerContext` (18 行) | ❌ | **空壳**，无 smart cast、无变量初始化跟踪 |

### 5.6 `:cfir:checkers` — 诊断检查（39 手写 + 14 生成）

#### 已注册的检查器（17 个）

**声明检查器（9 个）：**

| 检查器 | 实现质量 |
|--------|---------|
| `CfirInitializerTypeMismatchChecker` | ✅ |
| `CfirPropertyInitializerTypeMismatchChecker` | ✅ |
| `CfirExtendTargetLegalityChecker` | ✅ |
| `CfirExtendInterfaceKindChecker` | ✅ |
| `CfirExtendDuplicateInterfaceChecker` | ✅ |
| `CfirExtendOrphanRuleChecker` | ✅ |
| `CfirExtendGenericUsageChecker` | ✅ |
| `CfirExtendSpecializationConflictChecker` | ✅ |
| `CfirExtendDefaultImplementationConflictChecker` | ✅ |

**表达式检查器（8 个）：**

| 检查器 | 实现质量 |
|--------|---------|
| `CfirIfConditionTypeMismatchChecker` | ✅ |
| `CfirLoopConditionTypeMismatchChecker` | ✅ |
| `CfirAssignmentTypeMismatchChecker` | ✅ |
| `CfirReturnTypeMismatchChecker` | ✅ |
| `CfirArgumentTypeMismatchChecker` | ✅ |
| `CfirLiteralNumericOverflowChecker` | ✅ |
| `CfirConstEvalArithmeticChecker` | ✅ |
| `CfirMatchExhaustivenessChecker` | ⚠️ | `getEnumConstructorNames()` 返回 `emptyList()`，枚举穷尽性不工作 |

#### 缺失的检查器类别

未使用变量/导入、不可达代码、可见性违规、mut 限制、继承合法性、重复定义、类型参数约束违反。

### 5.7 `:cfir:entrypoint` — 前端入口（15 个文件）

| 组件 | 实现质量 | 说明 |
|------|---------|------|
| `CfirAbstractSessionFactory` / `CfirDefaultSessionFactory` | ✅ | 模板方法 + Context 扩展点 |
| `CfirSessionConfigurator` | ✅ | Session 配置 DSL |
| `analyse()` 函数 | ✅ | 顶层分析入口 |
| `ComponentsContainers` / `CheckersContainers` | ✅ | 组件与检查器注册 |
| 三种 Session 创建 | ✅ | Shared / Library / Source |

### 5.8 `:cfir:cfir-serialization` — 序列化（10 个文件）

| 组件 | 实现质量 | 说明 |
|------|---------|------|
| `CfirDeclDeserializer` (447 行) | ✅ | 11 种声明类型，FlatBuffers 零拷贝读取 |
| `CfirTypeDeserializer` | ✅ | SemaTy → ConeCangJieType |
| `CjoManager` | ✅ | .cjo 缓存加载，并发安全 |
| `CfirDeserializedSymbolProvider` | ✅ | 按 ClassId/Name 查找 |
| **序列化（写入方向）** | ❌ | **完全不存在** |

### 5.9 `:cfir:raw-cfir` — Raw CFIR 构建

| 模块 | 文件数 | 实现质量 | 说明 |
|------|--------|---------|------|
| `raw-cfir-common` | 4 | ✅ | `AbstractRawCfirBuilder<T>` 模板方法 |
| `psi2cfir` | 2 | ✅ | PSI → Raw CFIR，~3000 行，覆盖主要节点 |
| `light-tree2cfir` | 6 | ✅ | LightTree → Raw CFIR，职责拆分清晰 |

### 5.10 `:cfir:diagnostic-renderers` — 诊断渲染（1 个文件）

| 组件 | 实现质量 | 说明 |
|------|---------|------|
| `CfirDiagnosticRenderers` | ❌ 空壳 | 仅 `RENDER_TYPE` 调用 `type.toString()`，缺少格式化逻辑 |

---

## 六、后端层

### 6.1 `:compiler:chir` — CHIR 数据模型（39 个文件，3,174 行）

| 组件 | 实现质量 | 说明 |
|------|---------|------|
| 数据模型（Declaration/Expression/Type/Value） | ✅ | 接口 + Default 实现完整 |
| 控制流（Block/Instruction/Terminator） | ✅ | 接口定义 |
| `ChirBuilder` (118 行) | ✅ | 包/声明注册 + 符号表 + 引用绑定 + 验证 |
| `ChirPipelineScheduler` (51 行) | ✅ | 拓扑排序 Pass 执行 |
| `ChirDataFlowEngine` (143 行) | ✅ | 通用前向/后向分析框架，迭代收敛 |
| `ChirValidator` / `ChirInvariants` | ✅ | 控制流不变性检查 |
| `RemoveUnreachableBlocksTransformation` | ✅ | 完整 Pass 示例 |
| `ChirVisitor` / `ChirWalker` / `ChirPrinter` | ✅ | 访问者 + 调试输出 |
| **CFIR→CHIR 转换器** | ❌ | **不存在** |
| **实际 Pass 实现**（除去不可达块删除） | ❌ | **不存在** |

### 6.2 `:compiler:codegen` — 代码生成（25 个文件，1,751 行）

| 组件 | 实现质量 | 说明 |
|------|---------|------|
| `DefaultChirToLlvmCodeGenerator` (66 行) | ✅ | CHIR 验证 → Lowering → 模块分割 → 后端调用 |
| `ChirToLlvmLoweringFramework` (102 行) | ✅ | 3 个 Pass：符号收集、类型映射、控制流验证 |
| `ExpressionLoweringDispatcher` (418 行) | ✅ | 算术/逻辑/内存/调用/Phi/select/cast 完整 |
| `CGModule` (223 行) | ✅ | 类型声明/全局变量/Runtime 符号/LLVM IR 文本生成 |
| `TypeLowering` | ✅ | CHIR → LLVM 类型转换 |
| `JniLlvmBackend` (67 行) | ✅ | JNI → LLVM C API（contextCreate/moduleParseAssembly/writeBitcode/moduleVerify） |
| `LlvmBackendFactory` (33 行) | ✅ | 含版本检查 |

### 6.3 `:llvm-interop` — LLVM JNI 互操作

| 模块 | 文件数 | 实现质量 | 说明 |
|------|--------|---------|------|
| `llvm-interop-api` | 8 Kt | ✅ | `LlvmBindings` 235 行，200+ 方法；`LlvmBuilder` 275 行；Context/Module 完整 |
| `llvm-interop-jni` | 3 Kt + 9 C++ | ✅ | 全部 native 方法有 C++ 实现，覆盖 Context/Module/Builder/Types/Values/Bitcode |

### 6.4 `:compiler:frontend` — 前端基础设施入口（15 个文件，1,028 行）

| 组件 | 实现质量 | 说明 |
|------|---------|------|
| `CfirFrontendPipelinePhase` (239 行) | ✅ | 完整：环境创建 → Session 构建 → CFIR 构建与解析 |
| `VfsBasedProjectEnvironment` (170 行) | ✅ | VFS 搜索范围管理 |
| `GroupedCjSources` (150 行) | ✅ | 源文件收集、分组、排序 |
| `CfirConfigurationPhase` | ❌ | 空文件 |
| 后端管线阶段集成 | ❌ | CLI 中未连接 codegen |

---

## 七、Analysis API 层

### 7.1 `:analysis:analysis-api` — 接口定义（18 个文件）

| 组件 | 实现质量 | 说明 |
|------|---------|------|
| `CaSession` | ✅ | 核心会话接口，委托 25+ 个 provider/checker |
| `analyze()` 高阶函数 | ✅ | 按元素/按模块两种入口 |
| 生命周期管理（`CaLifetimeOwner`/`Token`/`Factory`） | ✅ | 防止会话泄漏 |
| 权限系统（`CaAnalysisPermissionRegistry`/`Checker`） | ✅ | 支持受限分析 |
| 25+ 组件接口 | ✅ | Resolver/SymbolProvider/TypeProvider/DiagnosticProvider/ScopeProvider 等 |

### 7.2 `:analysis:analysis-api-impl-base` — 基础实现（9 个文件）

| 组件 | 实现质量 | 说明 |
|------|---------|------|
| `CaBaseSession` (67 行) | ⚠️ | 纯委托，25+ 个 `by` 委托注入，无业务逻辑 |
| 权限/生命周期实现 | ⚠️ | 骨架性质 |

### 7.3 `:analysis:analysis-api-cfir` — CFIR 实现（13 个文件）

| 组件 | 实现质量 | 说明 |
|------|---------|------|
| `CaCfirSession` (74 行) | ✅ | 注入 20+ 个 CFIR provider |
| `CaCfirResolutionFacade` / `Impl` | ✅ | 诊断收集与过滤 |
| 20+ 个 CaCfir* Provider | ⚠️ | 文件存在，多为适配器或简化实现 |

---

## 八、测试基础设施

### 8.1 `:tests:test-infrastructure`（95 个 testFixtures 文件）

| 组件 | 实现质量 | 说明 |
|------|---------|------|
| `AbstractCangjieCompilerTest` | ✅ | JUnit5 测试基类 |
| `TestConfigurationBuilder` DSL | ✅ | 配置构造器 |
| Handler 链（Pre → Analysis → Post，5 级优先级） | ✅ | 灵活的处理管线 |
| 服务框架（18 个文件） | ✅ | TestServices / Assertions / SourceFileProvider / ArtifactsProvider 等 |
| 指令系统（15+ 指令集） | ✅ | 语言设置、编译器阶段、诊断指令 |
| CodeMetaInfo 系统 | ✅ | 内联诊断标注解析与渲染 |
| 前端适配（CfirDefaultFacade / CfirAnalyzerFacade / CfirCliFacade） | ✅ | 多前端支持 |
| 模块结构（TestModule / DependencyKind） | ✅ | 依赖关系建模 |

**成熟度：★★★★★ (5/5)**

### 8.2 `:cfir:analysis-tests`（11 testFixtures + 1 tests-gen）

| 组件 | 实现质量 | 说明 |
|------|---------|------|
| `AbstractCfirAnalysisDiagnosticsTest` | ✅ | Kotlin 风格内联诊断标注（`<!DIAG!>...<!>`） |
| `AbstractCfirPhasedDiagnosticTest` | ✅ | 支持编译器阶段指令 |
| `CfirInlineDiagnosticsChecker` | ✅ | 内联标注与实际诊断比对 |
| `TestGeneratorForCfirAnalysisTests` | ✅ | 自动从 testData/diagnostics 生成测试 |

### 8.3 Raw CFIR 测试（6 testFixtures + 4 tests-gen）

| 组件 | 说明 |
|------|------|
| 4 种测试入口 | Normal / LazyBodiesByAst / LazyBodiesByStub / SourceElementMapping |
| Golden 文件对比 | `.cj` → `.txt`，支持 `-Dupdate.test.data=true` |
| Coverage Matrix | 特性 ↔ 测试文件映射追踪 |

### 8.4 Resolve 单元测试（12 个文件）

| 测试 | 覆盖领域 |
|------|---------|
| `CfirTowerGroupTest` | 塔式符号查找 |
| `CfirMapArgumentsTest` | 参数映射 |
| `CfirCheckArgumentsTest` | 参数类型检查 |
| `CfirOverloadConflictResolverTest` | 重载消歧 |
| `CfirExtend*Test`（5 个） | Extend 声明/作用域/索引/处理器 |

---

## 九、实际可工作的端到端链路

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

         CFIR ──✖──→ CHIR（数据模型+框架完整，转换器不存在）
         CHIR ──✖──→ LLVM IR（降低管线完整，但前提 CFIR→CHIR 不存在）
         .cjo 序列化（写入方向完全不存在）
```

---

## 十、基于代码的真实缺口清单

### 🔴 P0 — 影响正确性的空洞

| # | 缺口 | 代码证据 | 影响 |
|---|------|---------|------|
| 1 | **继承成员 scope 缺失** | `CfirClassDeclaredMemberScope` 仅索引 `klass.declarations` | 子类调用继承方法报 unresolved |
| 2 | **约束系统 incorporation 缺失** | `computeFixedType()` 取 `first()` | 多约束泛型推断错误 |
| 3 | **match 绑定变量未注册** | `storePatternBinding()` 为空方法 | 绑定变量后续不可见 |
| 4 | **commonSupertype 简化** | `return types.first()` | if/try/match 多类型分支结果不准 |
| 5 | **枚举穷尽性不工作** | `getEnumConstructorNames()` 返回 `emptyList()` | match 枚举不报非穷尽警告 |

### 🟠 P1 — 阻塞端到端编译的断点

| # | 缺口 | 现状 | 工作量 |
|---|------|------|--------|
| 1 | **.cjo 序列化（写入）** | 反序列化 447 行完整，序列化代码为零 | 中 |
| 2 | **CFIR→CHIR 转换器** | CHIR 数据模型+框架完整，无转换逻辑 | 大 |
| 3 | **CLI 后端集成** | CLI 前端完整，codegen 未连接 | 中 |

### 🟡 P2 — 功能性缺失

| # | 缺口 | 说明 |
|---|------|------|
| 1 | 数据流分析 / Smart Cast | `CfirDataFlowAnalyzerContext` 是 18 行空壳 |
| 2 | 缺失的检查器（~10 类） | 未使用变量、不可达代码、可见性违规、mut 限制等 |
| 3 | 用户自定义操作符重载 | 仅有内建操作符 |
| 4 | lambda 独立参数类型推断 | 仅从期望类型推断 |
| 5 | 递归函数返回类型推断 | 仅提取已解析类型 |
| 6 | 诊断渲染器 | `CfirDiagnosticRenderers` 仅 1 个文件，仅 `type.toString()` |

### ⬜ P3 — 完全不存在的编译阶段

| 阶段 | 说明 |
|------|------|
| LOAD_PLUGINS | 插件框架 |
| CONDITION_COMPILE | @When 条件编译 |
| MACRO_EXPAND | 宏展开系统 |
| FINALIZE | 泛型单态化、溢出策略 |
| MANGLING | 名称修饰 |

---

## 十一、完成度评估

```
基础设施（util + common + diagnostics）       ████████████████████  100%
PSI 解析（词法 + 语法 + 42+ 节点）             ████████████████████  100%
CFIR 数据模型（cones + tree + symbols）        ████████████████████  100%
Raw CFIR 构建（PSI/LightTree → Raw CFIR）      ███████████████████░   95%
Resolve 管线框架（Phase/Processor/Session）     ████████████████████  100%
声明级解析（IMPORTS→EXTENSIONS）               ██████████████████░░   90%
表达式类型合成（20+ 种表达式覆盖）             █████████████████░░░   85%
调用解析 + 重载消歧                           █████████████████░░░   85%
内建操作符 + 数值拓宽                         ███████████████████░   95%
Scope 实现（8 种，缺继承合并）                ████████████████░░░░   80%
泛型约束系统（缺 incorporation）              ██████████████░░░░░░   70%
检查器（17 个，枚举穷尽性不工作）             ███████████████░░░░░   75%
诊断系统（框架 + 收集 + 渲染）                ███████████████████░   95%
.cjo 反序列化                                ███████████████████░   95%
.cjo 序列化（写入）                           ░░░░░░░░░░░░░░░░░░░░    0%
CFIR 前端入口（Session + Pipeline）            ████████████████████  100%
CLI 入口（前端完整，后端未连接）              ████████████████░░░░   80%
CHIR 数据模型 + 框架                          ████████████████░░░░   80%
CFIR→CHIR 转换                               ░░░░░░░░░░░░░░░░░░░░    0%
Codegen 降低管线                              ████████████████████  100%
LLVM JNI 后端                                ███████████████████░   95%
Analysis API 接口                             ████████████████████  100%
Analysis API CFIR 实现                        ██████████████░░░░░░   70%
测试基础设施                                  ████████████████████  100%
数据流分析 / Smart Cast                       ░░░░░░░░░░░░░░░░░░░░    0%
```

---

## 十二、核心判断

**前端（阶段 2-7）：✅ 可工作** — PSI 解析完整，CFIR 构建和语义解析核心链路通畅，但有 5 个 P0 正确性空洞。

**后端（阶段 11-12）：✅ 框架完整，链路断裂** — CHIR 数据模型和 codegen 降低管线均已实现，但 CFIR→CHIR 转换器不存在，导致整条链路无法接通。

**序列化（阶段 4/10）：半完成** — 读取（反序列化）完整，写入（序列化）为零。

**整体**：项目在 CFIR 前端达到了**生产级框架 + 核心可用**的水平，后端框架完整但缺少关键连接件（CFIR→CHIR 转换器），5 个未实现的编译阶段（插件/条件编译/宏/终结/名称修饰）阻塞完整管线。
