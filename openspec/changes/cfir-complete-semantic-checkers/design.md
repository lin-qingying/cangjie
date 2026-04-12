## 上下文

当前 CFIR checker 框架已具备完整的基础设施：

- **诊断定义**：`CfirDiagnosticsList`（约 1958 行）定义了 40+ 诊断分组，覆盖仓颉全部语义检查场景
- **生成代码**：`CfirErrors.kt`（诊断工厂）、`CfirErrorsDefaultMessages.kt`（默认消息）、`DeclarationCheckers.kt` / `ExpressionCheckers.kt` / `TypeCheckers.kt`（checker 注册容器）均已生成
- **checker 基类**：`CfirDeclarationChecker<D>`、`CfirExpressionChecker<E>`、`CfirTypeChecker` 已定义，使用 Kotlin context receivers 模式
- **注册体系**：`CheckersComponent` 通过 `ComposedDeclarationCheckers` / `ComposedExpressionCheckers` / `ComposedTypeCheckers` 组合多个 checker 集合
- **Collector 管线**：`CfirCheckerRunningDiagnosticCollectorVisitor` 驱动 CFIR 树遍历，在各节点类型分发到对应 checker

**已实现（约 30%）**：`CommonDeclarationCheckers` 注册了 modifier、conflict、supertypes、override、extend 系列、constructor delegation、imports、type constraint/bounds、initializer type mismatch 等 checker。`CommonExpressionCheckers` 注册了 loop condition、if condition、match（case type + pattern legality + exhaustiveness）、assignment、return type mismatch、literal overflow、const eval arithmetic、constructor delegation call、mutability、generic bare classifier、super reference 等 checker。

**未实现（约 70%）**：General、Function、Expression（大量子项）、GenericDeep、InheritanceDeep、ClassStruct、Property、ConstDeclaration、AnnotationExtra、Inout、VArrayExtra、EffectsExtra、Deprecated、CommonSpecific、ExtendExtra、Spawn、Interface、JavaInterop、JavaMirror、CJMapping、ObjCInterop、ObjCCJMapping、ForeignName、IfAvailable、APILevel、Hide、Mock、Unused 等分组对应的 checker。

**C++ 参考实现**（`external/cangjie_compiler/src/Sema/`）结构：
- 核心入口：`TypeChecker.cpp` + `TypeCheckerImpl.h`
- 按主题分文件：`TypeCheckDecl.cpp`、`TypeCheckClassLike.cpp`、`TypeCheckExtend.cpp`、`TypeCheckGeneric.cpp`、`TypeCheckExpr/`（28 个子文件）、`TypeCheckAnnotation.cpp`、`TypeCheckCall.cpp`、`TypeCheckReference.cpp`、`TypeCheckType.cpp`、`TypeCheckPattern.cpp`、`TypeCheckMatchExpr.cpp`、`TypeCheckOverflow.cpp` 等
- 子目录：`InheritanceChecker/`、`LegalityOfUsage/`、`FFI/`、`CJMP/`、`GenericInstantiation/`、`NativeFFI/`

**K2 FIR checker 参考**（`external/kotlin/compiler/fir/checkers/`）：每个 checker 是一个 `object` 继承 `FirXxxChecker`，在 `check()` 方法中通过 `reporter.reportOn()` 报告诊断，所有 checker 在 `CommonDeclarationCheckers` 等注册表中集中注册。

## 目标 / 非目标

**目标：**
- 为 `CfirDiagnosticsList` 中全部 40+ 诊断分组实现对应的 checker，使 CFIR 前端管线具备与官方 C++ 编译器对等的语义校验能力
- 严格按照 K2 FIR checker 框架模式编写：每个 checker 为 Kotlin `object`，继承对应的 `CfirDeclarationChecker<D>` / `CfirExpressionChecker<E>` / `CfirTypeChecker`，使用 context receivers 注入 `CheckerContext` 和 `DiagnosticReporter`
- 每个 checker 的语义检查逻辑必须对齐 `external/cangjie_compiler/src/Sema/` 中对应的 C++ 实现，不得盲目发明语义规则
- 所有新 checker 在 `CommonDeclarationCheckers` / `CommonExpressionCheckers` / `CommonTypeCheckers` 中集中注册

**非目标：**
- 不修改诊断定义（`CfirDiagnosticsList` 已完备）
- 不修改 checker 框架基础设施（生成器、collector、visitor 管线）
- 不实现 CFIR resolve 阶段逻辑（checker 依赖的 resolve 信息由 resolve 管线提供）
- 不实现后端 CHIR/CodeGen 相关检查
- 不实现运行时行为验证，仅覆盖编译期静态语义检查

## 决策

### 决策 1：按诊断分组组织 checker 文件

**选择**：每个诊断分组对应一个或一组 checker 文件，文件命名为 `Cfir<Group>Checker.kt`。

**替代方案**：
- A）将所有检查放在少数大文件中（如 `CfirDeclarationStatusCheckers.kt` 承载所有声明状态相关检查）——缺点：文件过大，难以维护
- B）每个诊断条目一个 checker——缺点：文件数量爆炸，注册表冗长

**理由**：与 K2 FIR 的组织方式一致。K2 中 `FirModifierChecker`、`FirClassChecker`、`FirEnumChecker` 等按功能域划分。同一功能域中相关的检查共享上下文信息，放在同一 checker 中可减少重复的 CFIR 树查询。如果单个分组过于庞大（如 CommonSpecific 有 30+ 诊断），可拆分为子 checker（如 `CfirCommonSpecificMatchChecker`、`CfirCommonSpecificModifierChecker`）。

### 决策 2：使用 object singleton 模式

**选择**：每个 checker 为 Kotlin `object`（单例），无状态。

**替代方案**：使用 `class` 实例化——但 checker 本身不应持有可变状态，K2 FIR 也使用 object 模式。

**理由**：与已有 checker 实现保持一致（如 `CfirModifierChecker`、`CfirSupertypesChecker` 均为 object）。object 模式自动保证��程安全且零分配成本。

### 决策 3：checker 分类归属策略

**选择**：
- **Declaration checker**：检查的目标是 CFIR 声明节点（CfirClassLikeDeclaration、CfirFunction、CfirProperty 等）→ 继承 `CfirDeclarationChecker<D>`
- **Expression checker**：检查的目标是 CFIR 表达式/语句节点（CfirFunctionCall、CfirAssignment、CfirMatchExpression 等）→ 继承 `CfirExpressionChecker<E>`
- **Type checker**：检查的目标是类型引用节点 → 继承 `CfirTypeChecker`

对于跨越声明和表达式的检查（如 `EXPRESSION.CAPTURE_BEFORE_INITIALIZATION` 需要分析变量声明和使用点），将 checker 放在使用点一侧（expression checker），通过 CheckerContext 访问声明信息。

**替代方案**：引入新的 checker 类别（如 `CfirSemanticChecker`）——但这要求修改生成器和 collector，违反非目标约束。

### 决策 4：实现批次划分

**选择**：按优先级和依赖关系分 4 个批次实现：

- **批次 1（核心语义）**：General、Function、Expression、DeclarationStatus 补充——这些是最基础的语义检查，覆盖面最广
- **批次 2（类型系统深层）**：GenericDeep、InheritanceDeep、ClassStruct、Property、ConstDeclaration——依赖完善的类型信息
- **批次 3（语言特性）**：AnnotationExtra、ExtendExtra、Deprecated、Effects/EffectsExtra、Spawn、Interface、Inout、VArray、Match 补充、Unused——面向特定语言特性
- **批次 4（互操作与平台）**：JavaInterop、JavaMirror、CJMapping、ObjCInterop、ObjCCJMapping、ForeignName、IfAvailable、APILevel、Hide、Mock、CommonSpecific——面向特定平台和互操作���景

**理由**：批次 1~2 覆盖最多日常编码场景，优先实现可尽早提供价值。批次 3~4 面向进阶场景，可渐进交付。

### 决策 5：诊断报告方式

**选择**：使用 `reporter.reportOn(source, CfirErrors.DIAGNOSTIC_NAME, arg1, arg2, ...)` 模式，其中 `source` 从 CFIR 节点的 `source` 属性获取 PSI 元素。

**理由**：与已有 checker 一致，且与 `CfirErrors` 生成的工厂方法 API 匹配。

## 风险 / 权衡

**[风险] CFIR 声明/表达式树 API 不完整**
→ 部分 checker 可能依赖尚未建模的 CFIR 属性（如 `@Deprecated` 注解信息、`@Java` 标记等）。缓解：先实现可用的 checker，对 API 缺失的部分在 checker 中留 TODO 注释并跳过相关检查，待 CFIR 模型补充后再启用。

**[风险] C++ 语义逻辑翻译偏差**
→ C++ 参考实现使用命令式风格，CFIR 使用函数式/访问者风格，直接翻译可能引入语义偏差。缓解：为每个 checker 编写测试用例，对齐官方编译器的错误输出行为。

**[风险] checker 执行顺序依赖**
→ 部分 checker 可能假设其他 checker 已经执行（如泛型深层检查假设基本类型检查已完成）。缓解：checker 应设计为无状态且独立，不依赖其他 checker 的执行结果。共享的中间计算通过 CheckerContext 提供。

**[权衡] 覆盖完整性 vs 实现成本**
→ 40+ 诊断分组、300+ 个诊断条目的全覆盖工作量巨大。权衡：按批次交付，每批次确保功能完整且可测试。

**[权衡] 严格对齐 C++ vs 惯用 Kotlin**
→ 某些 C++ 实现的检查逻辑（如递归遍历 + 全局状态）不适合直接翻译为 Kotlin。权衡：保持语义对齐，但实现方式遵循 K2 FIR 的惯用模式（visitor + context receivers + immutable 数据）。
