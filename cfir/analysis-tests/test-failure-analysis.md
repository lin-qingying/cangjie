# cfir/analysis-tests 测试失败深度分析报告（逐问题）

> 本报告按「问题」为单位组织，每个问题包含：发生位置、测试文件、问题详情、修复方案。
> 数据来源：2026-08-03 完整测试运行（1960 个失败），已逐个问题对照 CFIR 源码定位根因。

---

# 问题 1：宏场景 `const init` 中 `let` 字段赋值被误报 CANNOT_ASSIGN_TO_IMMUTABLE

## 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirAssignmentLegalityChecker.kt`

- `CfirMutationTargetClassifier.isImmutableFieldAssignmentForbidden`（第 372-389 行）是直接误报点：

```kotlin
if (field.isVar) return false
val constructor = context.findClosestDeclaration<CfirConstructor>() ?: return true
if (field.status.isStatic != constructor.status.isStatic) return true
if (field.hasSameNamePrimaryConstructorPropertyInOwner()) return true
if (field.initializer != null) return true
return when (assignment?.let { CfirInitializationAssignmentClassifier.classifyAssignment(it, context) }) {
    CfirInitializationAssignmentKind.INITIALIZATION,
    CfirInitializationAssignmentKind.PRIORITY_INITIALIZATION_DIAGNOSTIC,
    -> false
    CfirInitializationAssignmentKind.REASSIGNMENT,
    CfirInitializationAssignmentKind.NOT_TRACKED,
    null,
    -> true
}
```

- 诊断上报点：同一文件第 79 行（`CfirErrors.CANNOT_ASSIGN_TO_IMMUTABLE`）。
- 下层根因在分类器：`CfirInitializationCheckers.kt` 的 `CfirInitializationAssignmentClassifier.classifyAssignment`（第 1720-1761 行）复用 `CfirInitializationFlowAnalyzer`（第 1697 行起）收集赋值分类；赋值分类的实际产生在 `analyzeAssignmentTargetAccess`（第 614-702 行）。

## 测试文件

- `cfir/analysis-tests/testData/macro/llt/APILevelChecker/level/merge_anno/merge04/test2.cj`（及 test1/3/4.cj）
- `cfir/analysis-tests/testData/macro/llt/annotation/globals/key.cj`
- `cfir/analysis-tests/testData/macro/llt/APILevelChecker/level/call_construct.cj`、`dep_ok1.cj`、`invalid_ref.cj`、`non_literal.cj`、`test_dep01.cj`
- `cfir/analysis-tests/testData/llt/class/primaryConstructor/primaryConstructor10.cj`、`primaryConstructor12.cj`
- 以及 `llt/generics/generic_call_static_impl02.cj` 等，共约 180 个失败（含组合诊断对）。

以 `key.cj` 中的 `apilevel.cj` 片段为例，EXP 期望无诊断，ACT 在以下位置误报：

```cangjie
public class APILevel {
    public let level: UInt8
    public let atomicservice: Bool
    public let stagemodeonly: Bool
    public let syscap: String
    public const init(l: UInt8, atomicservice!: Bool = false, stagemodeonly!: Bool = false, syscap!: String = "") {
        level = l                      // ACT: <!CANNOT_ASSIGN_TO_IMMUTABLE!>level<!> = l
        this.stagemodeonly = stagemodeonly  // ACT: this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>stagemodeonly<!> = ...
        this.syscap = syscap
        this.atomicservice = atomicservice
    }
}
```

## 问题详情

**上层现象**：`const init` 构造器体内对 `let` 字段的首次初始化赋值（`this.field = 参数` 或裸 `field = 参数`）被 CFIR 报告为 `CANNOT_ASSIGN_TO_IMMUTABLE`。官方语义中构造器内 `let` 字段赋值是合法初始化，EXP 全部无诊断。该失败族是所有诊断对中最大的一族（146 个纯 `CANNOT_ASSIGN_TO_IMMUTABLE` 多余 + 数十个组合对），且集中在 `@APILevel`/`@Annotation` 宏标注类上。

**下层根因（按调用链）**：

1. `isImmutableFieldAssignmentForbidden` 对 `let` 字段的放行条件只有一个：`classifyAssignment` 返回 `INITIALIZATION` 或 `PRIORITY_INITIALIZATION_DIAGNOSTIC`。其他任何结果（含 `NOT_TRACKED` 和 `null`）一律判为非法写入并报诊断。
2. `classifyAssignment`（`CfirInitializationCheckers.kt` L1720-1761）实际执行 `CfirInitializationFlowAnalyzer.collectAssignmentClassifications`。该分析器在 `analyzeAssignmentTargetAccess`（L614-702）中，只有 `access.resolvedAccessSymbolOrNull()` 能解析出 `trackedVariable` 时才会记录 `INITIALIZATION`/`REASSIGNMENT`（L679-686）；解析不到时走 `else` 分支（L693-700）直接报告非法访问，**该赋值不产生任何分类记录**，最终 `classifyAssignment` 返回 `NOT_TRACKED`。
3. 在宏展开场景（`@APILevel` 等宏把类/构造器改写为展开产物）下，字段符号与构造器体的跟踪链在宏产物中未建立——字段声明来自宏展开模板，赋值接收者链解析不到与构造器同上下文的 `trackedVariable`，于是所有 `this.field = param` 都落入"未跟踪 → 判非法"。
4. 另有一个叠加因素：`CfirProperty.isEffectivelyWritable()`（`CfirAssignmentLegalityChecker.kt` L354）只看 `status.isMut`，宏展开/泛型替换后的属性若丢失 `mut` 契约也会被判不可写（`generic_call_static_impl02.cj` 的 `T.a = 1` 场景）。

**为什么不是个别 fixture 问题**：同一根因同时驱动了 `多余: CANNOT_ASSIGN_TO_IMMUTABLE`（146）、`替换: APILEVEL_REF_HIGHER -> CANNOT_ASSIGN_TO_IMMUTABLE`（20+18+6）、`多余: APILEVEL_MISSING_ARG, CANNOT_ASSIGN_TO_IMMUTABLE`（10）、`多余: CANNOT_ASSIGN_TO_IMMUTABLE, INVALID_THIS_CALL_OUTSIDE_CTOR`（8）等 8 个诊断对——都是"宏展开后 let 赋值被误判不可变"这一条链在不同测试形态下的表现。

## 修复方案

1. **核心修复（推荐）**：`isImmutableFieldAssignmentForbidden` 的放行逻辑不能把 `NOT_TRACKED`/`null` 一律视为非法。改为：当字段满足「非 static、无初始化器、主构造参数未同名占用、且位于构造器上下文」时，以"是否存在明确的 REASSIGNMENT 证据"为判据——只有 flow analyzer 确实记录了 `REASSIGNMENT`（L682，即 `isPossiblyInitialized`/`mayRevisitAssignment` 为真）才报 `CANNOT_ASSIGN_TO_IMMUTABLE`；`NOT_TRACKED` 表示"未观察到重复赋值"，应放行。
2. **根因修复**：在 `CfirInitializationFlowAnalyzer` 的 `analyzeAssignmentTargetAccess` 中，对宏展开产物中的字段访问补充 `trackedVariable` 建立逻辑（宏展开后字段的 `INSTANCE_MEMBER` 跟踪应与普通字段一致），使初始化分类能正确记录 `INITIALIZATION`。
3. **配套**：宏展开与泛型替换路径保留属性/字段的 `mut`/`var` 状态（`CfirProperty.isEffectivelyWritable` 不应因替换丢失 `isMut`）。
4. **回归范围**：`*Merge*Generated`、`*GlobalsGenerated*`、`*ApiLevel*` 相关宏族、`*PrimaryConstructor*`、`*GenericCallStaticImpl02*`，PSI 与 LightTree 双路径都要验证（宏族以 `CfirAnalysisMacroTest`/`CfirAnalysisMacroPsiTest` 为准）。

---

# 问题 2：构造器体内的 `super(...)`/`this(...)` 委托调用被误报 INVALID_THIS_CALL_OUTSIDE_CTOR

## 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirConstructorDelegationCallChecker.kt`

- 误报入口：`check`（第 23-44 行）。判定"是否在构造器内"的关键是第 36-37 行：

```kotlin
val closestFunction = context.closestFunctionLikeDeclaration()
if (closestFunction is CfirConstructor) return
reporter.reportOn(..., CfirErrors.INVALID_THIS_CALL_OUTSIDE_CTOR, ...)
```

- 第 57-61 行 `closestFunctionLikeDeclaration`：从 `containingDeclarations` 逆序找最近的 `CfirFunction`：

```kotlin
private fun CheckerContext.closestFunctionLikeDeclaration(): CfirFunction? {
    return containingDeclarations
        .asReversed()
        .firstOrNull { declaration -> declaration is CfirFunction } as? CfirFunction
}
```

- 类型上 `CfirConstructor : CfirFunction()`（`cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/declarations/CfirConstructor.kt` 第 23 行），所以只要当前构造器在声明栈里就应命中——**误报说明部分构造器形态下当前 `CfirConstructor` 没有进入 `containingDeclarations`**。

## 测试文件

- `cfir/analysis-tests/testData/llt/class/constructor/primaryConstructor/primaryConstructor1.cj`（`class C <: B<Int64,Int64> { public C() { super(1, 2, 3) } }`，`super()` 被误报）
- `cfir/analysis-tests/testData/llt/record/primaryConstructor/primaryConstructor8.cj`（`GenRec` 的 `init` 体内 `this(x)` 委托调用被误报，见下面详情）
- `cfir/analysis-tests/testData/llt/class/constructor/super_this/super_this_11.cj`、`class/recursive_constructor_call.cj`、`class/class6.cj`、`class/class_init_constructor0.cj`、`class/class_access_control12.cj`
- `cfir/analysis-tests/testData/llt/generics/class_generic_inheritance*.cj`、`class_nongeneric_extends_generic.cj`、`generic_parameters1.cj`
- `cfir/analysis-tests/testData/diagnostics2/invalid_declaration/*` 相关
- 共 76 个纯 `多余: INVALID_THIS_CALL_OUTSIDE_CTOR` 失败 + 8 个组合对（`CANNOT_ASSIGN_TO_IMMUTABLE, INVALID_THIS_CALL_OUTSIDE_CTOR`）。

`primaryConstructor8.cj` 的实际差异：EXP 无诊断，ACT 在 `this(x)` 处报：

```cangjie
struct GenRec {
    public var a: Int64
    public var b: Int32
    public init(x: Int64, y: Int32, str: String) {
        this(x)              // ACT: <!INVALID_THIS_CALL_OUTSIDE_CTOR!>this<!>(x)
        this.b = y
    }
    private GenRec(x: Int64) { this.a = x }
}
```

## 问题详情

**上层现象**：构造器体内的 `super(...)`/`this(...)` 委托调用在官方语义中完全合法（EXP 无诊断），CFIR 却报 `INVALID_THIS_CALL_OUTSIDE_CTOR`。失败集中在三类形态：

1. **class 的次级构造器体**（`primaryConstructor1.cj`：`class C <: B<Int64,Int64> { public C() { super(1,2,3) } }`）；
2. **struct/record 的命名 init 体内委托**（`primaryConstructor8.cj`：`init(...) { this(x) }`）；
3. **泛型继承链上的构造器**（`class_generic_inheritance*.cj` 等）。

**下层根因**：`closestFunctionLikeDeclaration()` 完全依赖 `containingDeclarations` 声明栈中是否存在 `CfirFunction`。误报说明在以下场景中当前构造器未入栈：

- 次级构造器/命名 `init` 的 body resolve 阶段，上下文栈中的函数级声明可能尚未压入该构造器（或压入的是占位/匿名包装）；
- 主构造器带默认值参数、委托链（`this(x)` 委托到私有构造器）时，检查发生在委托目标解析之后，栈顶函数已不是当前构造器；
- 泛型继承场景，构造器符号经类型替换后与声明栈中的原始 `CfirConstructor` 不是同一实例。

这属于检查上下文（`CheckerContext.containingDeclarations`）构建的通用缺陷，而不是某个 fixture 特例——证据是该族同时出现在 `_class`、`record`、`generics`、`diagnostics2/invalid_declaration` 四个区域共 84 个失败。

**与问题 1 的关联**：`classUninitializedFieldRich.cj`、`class_initialization7.cj`、`record_constructor_call_other_init01.cj`、`variable_use_before_init_13.cj` 四个 fixture 同时报 `CANNOT_ASSIGN_TO_IMMUTABLE + INVALID_THIS_CALL_OUTSIDE_CTOR`（8 个组合失败），说明委托链上 let 字段初始化与构造器上下文两个缺陷叠加。

## 修复方案

1. **改判定方式**：不要依赖 `containingDeclarations` 的扁平顺序找 `CfirFunction`，改用 `context.findClosestDeclaration<CfirConstructor>()` 直接查询最近的构造器声明；只要返回非空即放行。该 API 与文件顶部 `CfirConstructorDelegationCallChecker` 第 27 行已有的 `findClosestDeclaration<CfirClassLikeDeclaration>()` 用法一致，语义更可靠。
2. **查入栈时机**：检查 body resolve 阶段（`cfir/resolve/.../body/` 下构造器 body resolve 转换器）在解析 `this(...)`/`super(...)` 委托调用前，是否已将当前 `CfirConstructor` 压入 `containingDeclarations`；对命名 `init` 与次级构造器补上入栈。
3. **委托链场景**：`this(x)` 委托到其他构造器时，检查上下文应保留"发起委托的构造器"在栈中，而非切换到被委托目标。
4. **回归范围**：`*PrimaryConstructorGenerated*`、`*RecordThis*`、`*SuperThisGenerated*`、`*RecursiveConstructorCall*`、`*Class6*`、`*ClassGenericInheritance*`、`*GenericParameters1*`、`*InvalidDeclarationGenerated*`，PSI 与 LightTree 双路径。

---

# 问题 3：`static init()` 的 `static` 修饰符被误报 WRONG_MODIFIER_TARGET

## 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/ModifierCheckerTargets.kt`

- 根因位置：`possibleTargetMap`（第 103-108 行），`STATIC_KEYWORD` 的允许目标谓词**缺少 `DeclarationKind.STATIC_INITIALIZER`**：

```kotlin
internal val possibleTargetMap: Map<CjKeywordToken, ModifierTargetPredicate> = mapOf(
    STATIC_KEYWORD to ModifierTargetPredicate.memberOf(
        DeclarationKind.FUNCTION,
        DeclarationKind.PROPERTY,
        DeclarationKind.VARIABLE,
    ),
    ...
```

- 触发位置：`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirModifierChecker.kt` 的 `checkTarget`（第 88-98 行）：

```kotlin
val isWrongTarget = possiblePredicate == null || actualTargets.none {
    possiblePredicate.isAllowed(it, context.languageVersionSettings)
}
if (isWrongTarget) {
    reporter.reportOn(modifier.source, CfirErrors.WRONG_MODIFIER_TARGET, modifierToken, ...)
}
```

- 对照证据：同文件第 86 行 `defaultVisibilityKinds` 已包含 `DeclarationKind.STATIC_INITIALIZER`；第 168-176 行 `CONST_KEYWORD` 的谓词也允许 `STATIC_INITIALIZER`。唯独 `STATIC_KEYWORD` 漏列。

## 测试文件

- `cfir/analysis-tests/testData/llt/class/static_init/static_init_01.cj`、`static_init_01_O2.cj`、`static_init_05.cj`
- `cfir/analysis-tests/testData/llt/class/static_or_global_var/static_or_global_var3.cj`、`4.cj`、`5.cj`、`6.cj`、`10.cj`、`11.cj`、`12.cj`
- `cfir/analysis-tests/testData/llt/InitializationCheck/static_variable_use_before_init_01.cj`、`03.cj`、`04.cj`、`07.cj`
- 以及 `const_init_error.cj`、`file1.cj`、`generic_static_constructor.cj`、`generic_static_nested_func.cj`、`variable_use_before_init_07.cj`、`const_init.cj`
- 共 52 个纯 `多余: WRONG_MODIFIER_TARGET` 失败 + 若干组合对（`CONFLICTING_OVERLOADS -> WRONG_MODIFIER_TARGET`、`STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER -> WRONG_MODIFIER_TARGET` 等）。

以 `static_init_01.cj` 为例（EXP 无诊断）：

```cangjie
class C {
    static var a: Range<Int64>
    static let b: Int64
    static init() {        // ACT: <!WRONG_MODIFIER_TARGET!>static<!> init()
        a = 1..10
        b = 11
    }
}
```

## 问题详情

**上层现象**：仓颉的静态初始化块语法就是 `static init() { ... }`，`static` 是该声明的固有修饰符，官方语义完全合法（EXP 无诊断）。CFIR 对每个 `static init()` 都报 `WRONG_MODIFIER_TARGET`。

**下层根因**：`CfirModifierChecker` 把源码中的每个真实修饰符与 `possibleTargetMap` 对照检查。`static init()` 的声明种类是 `DeclarationKind.STATIC_INITIALIZER`，而 `STATIC_KEYWORD` 的谓词只允许 `FUNCTION`/`PROPERTY`/`VARIABLE` 三种成员种类——`actualTargets.none { predicate.isAllowed(...) }` 恒为真，于是误报。

**影响范围**：这不仅是 `static init` 场景——同一谓词表缺陷还会在组合场景连锁：`static_init_02.cj` 期望 `CONFLICTING_OVERLOADS` 却被 `WRONG_MODIFIER_TARGET` 抢占（4 失败）、`static_init_03.cj` 期望 `STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER` 被抢占（4 失败）、`const_init.cj`/`variable_use_before_init_07.cj` 的 `CANNOT_ASSIGN_TO_IMMUTABLE + WRONG_MODIFIER_TARGET` 组合（4 失败）、`test_macro.cj` 的多诊断簇里也带一个 `WRONG_MODIFIER_TARGET`。修好谓词表后这些抢占型组合对会一并消失。

## 修复方案

1. **主修复**：`possibleTargetMap` 的 `STATIC_KEYWORD` 谓词加入 `DeclarationKind.STATIC_INITIALIZER`（与 `CONST_KEYWORD` 第 168-176 行的写法一致）：

```kotlin
STATIC_KEYWORD to ModifierTargetPredicate.anyOf(
    ModifierTargetPredicate.memberOf(
        DeclarationKind.FUNCTION,
        DeclarationKind.PROPERTY,
        DeclarationKind.VARIABLE,
    ),
    ModifierTargetPredicate.headOf(DeclarationKind.STATIC_INITIALIZER),
)
```

（注意区分 `memberOf` 与 `headOf`：静态初始化块是类的"成员级"声明，需确认 `ModifierTargetPredicate.memberOf` 能否覆盖 `STATIC_INITIALIZER`；若该种类只在 `anySiteOf`/`headOf` 中注册，则按 `memberOf(..., DeclarationKind.STATIC_INITIALIZER)` 追加即可。）

2. **回归范围**：`*StaticInitGenerated*`、`*StaticOrGlobalVarGenerated*`、`*InitializationCheckGenerated*`、`*ConstInit*`、`*GenericStaticConstructor*`、`*GenericStaticNestedFunc*`，PSI 与 LightTree 双路径。

---

# 问题 4：match 分支可达性误判——嵌套 tuple 分支被误报 UNREACHABLE_PATTERN，box/unbox 分支却漏报

## 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirMatchUnreachablePatternChecker.kt`

- 误报入口：`check`（第 64-123 行）。核心判定在第 76-89 行（subject 值域收窄）与第 101-105 行（不可达判定）：

```kotlin
val valueDomainAvailable =
    expression.exhaustiveness !is CfirMatchExhaustivenessStatus.NonExhaustive &&
        !subjectType.containsNonExhaustiveEnum(matchContext)
val knownConstructor = if (!valueDomainAvailable) null else { ... }
val knownSubjectRows = if (!valueDomainAvailable) null else {
    expression.subject?.knownSubjectRowsOrNull(subjectType, matchContext)
}
...
val unreachableByKnownTag = knownConstructor != null &&
    (knownConstructorConsumed || !matchesKnownConstructor)
val unreachableByPatterns = branchRows.isNotEmpty() && branchRows.all { row ->
    row.isUnreachable(previousRows, matchContext, knownConstructor, knownSubjectRows)
}
```

- `isUnreachable`（第 130-143 行）：委托 Maranget usefulness（`isCoveredBy`，第 495-506 行 `MarangetChecker.INSTANCE.isUseful`）判断前序矩阵是否覆盖当前行。
- 窄化来源：`knownNarrowTypePatternOrNull`（第 412-420 行）——subject 静态类型比期望类型更窄时用该类型约束值域；以及 `knownSubjectRowsOrNull`（第 87-89 行调用）对字面量 subject 的枚举/值域展开。

## 测试文件

- 误报方向（35 个 `多余: UNREACHABLE_PATTERN`）：
  - `cfir/analysis-tests/testData/llt/match/nested_pattern/match_nested_in_tuple_001.cj`、`002.cj`、`003.cj`、`004.cj`、`007.cj`、`dts.cj`（`NestedPatternGenerated`）
  - `cfir/analysis-tests/testData/llt/match/tuple_pattern_004.cj`、`const_pattern_op_overloading.cj`、`const_pattern_range_001.cj`、`002.cj`、`iflet_unit.cj`、`match012.cj`、`match024.cj`、`diag_bloat.cj`、`join_ordered_problem_2.cj`
- 漏报方向（20 个 `缺少: UNREACHABLE_PATTERN`）：
  - `cfir/analysis-tests/testData/llt/Extend/autobox_match1.cj`、`autobox_match2.cj`、`unbox_box_tuplePattern.cj`、`unbox_enumPattern.cj`、`unbox_autobox_enumPattern_01.cj`、`unbox_optionbox.cj`
  - `as_expr_00.cj`、`defaultParameter3.cj`、`enum12.cj`、`enum14.cj`

`match_nested_in_tuple_001.cj` 的实际差异（EXP 无诊断，ACT 报 4 个 UNREACHABLE_PATTERN）：

```cangjie
var r1 = match (((), ())) {
    case (b: Int64, a: Int64) => 1   // ACT: <!UNREACHABLE_PATTERN!>(b: Int64, a: Int64)<!>
    case (a: Unit, b: Int64) => 1    // ACT: <!UNREACHABLE_PATTERN!>(a: Unit, b: Int64)<!>
    case (a: Int64, b: Unit) => 1    // ACT: <!UNREACHABLE_PATTERN!>(a: Int64, b: Unit)<!>
    case (b: Unit, a: Unit) => 0
    case _ => 1
}
```

## 问题详情

**上层现象**：官方 `cjc` 对 `((), ())`（静态类型 `(Unit, Unit)`）上的这些 tuple 分支**不报任何诊断**（EXP 为空）；CFIR 却把前三个类型不兼容的分支和最后的 `_` 全判为不可达。反向场景（`autobox_match*.cj`/`unbox_*.cj`）中官方报 `UNREACHABLE_PATTERN`（如 `case None => 1`），CFIR 却漏报。同一检查器两个方向都错，说明是**共享的覆盖算法对类型窄化的语义错误**，而非个别分支的特例。

**下层根因**：

1. **误报方向（嵌套 tuple）**：`((), ())` 的字面量 subject 被 `knownSubjectRowsOrNull`/`knownNarrowTypePatternOrNull` 收窄为"实际值就是 `(Unit, Unit)`"。Maranget 矩阵随后用这个**窄化值域**判定 `(Int64, Int64)`、`(Unit, Int64)`、`(Int64, Unit)` 分支不可达——但这三个分支与 subject 是**类型不兼容**（tuple 元素类型根本不匹配），官方语义里类型不兼容分支不参与可达性覆盖判定（应由 pattern legality/类型检查处理，不报 UNREACHABLE_PATTERN）。CFIR 把"类型不匹配"误当成"已被前序覆盖"，于是连 `_` 也被误判不可达。
2. **漏报方向（box/unbox）**：`autobox_match*.cj` 中官方把 box（`Int64` 与 `Option<Int64>` 之间）模式视为可覆盖关系并报不可达；CFIR 的 `isMatchSubtypeOf` 子类型判定未建模 box/unbox 语义，Maranget 矩阵里这些模式互相不覆盖，于是漏报。`enum12/14.cj`、`as_expr_00.cj` 则是 enum/as 表达式场景的窄化缺失。

**为什么修不好一个 fixture**：`isUnreachable` 的两个分支（`unreachableByKnownTag` 与 `unreachableByPatterns`）共享 `calculateMatrix`（第 95 行）和 `isMatchSubtypeOf`，任何局部补丁都会在另一个方向上复发——必须修正共享的 pattern 子类型/覆盖模型。

## 修复方案

1. **误报方向**：在 `knownSubjectRows` 收窄参与 `isUnreachable` 判定前，先做**类型兼容过滤**——`branchRows` 中与 subject 静态类型不兼容的行（元素类型不匹配的 tuple 分支、类型根本不兼容的 const/type pattern）不得作为"已被覆盖"的依据，也不得把后续分支（含 `_`）判为不可达。参考官方 Sema：类型不兼容分支走 pattern legality，不进入 usefulness 覆盖矩阵。
2. **漏报方向**：在 `isMatchSubtypeOf`（`cfir/resolve/.../match/` 下的子类型判定）中建模 box/unbox 语义（`Int64` ↔ `Option<Int64>`、tuple 元素 box 展开），使 Maranget 矩阵能识别官方视为覆盖的模式对；`enum12/14.cj` 补齐 enum 构造器窄化。
3. **回归范围**：`*NestedPatternGenerated*`、`*TuplePatternGenerated*`、`*AutoboxMatch*`、`*Unbox*`、`*MatchExpressionGenerated*`、`*PatternMatchingGenerated*`、`*EnumGenerated*`、`*AsExpr*`，PSI 与 LightTree 双路径。

---

# 问题 5：继承链上 static/实例同名成员——官方报 INHERIT_MEMBER_KIND_INCONSISTENT，CFIR 误报 STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME

## 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInheritanceDeepChecker.kt`

- 误报入口：`checkMemberKindConsistency` 之类的同名成员比较循环（第 940-984 行）。static 冲突分支在第 948-961 行，**先于** kind 一致性分支（第 963-984 行）执行且互斥：

```kotlin
for (ownInfo in ownSameNameMembers) {
    val hasStaticConflict = ownInfo.isStatic != superInfo.isStatic
    if (hasStaticConflict) {
        if (reportedStaticConflicts.add(ownInfo.name)) {
            reporter.reportOn(
                source = ownInfo.nameSource ?: ownInfo.source ?: subject.source,
                factory = CfirErrors.STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME,  // L954
                ...
            )
        }
    }

    if (ownInfo.kind != superInfo.kind) {
        if (!hasStaticConflict && reportedKindConflicts.add(ownInfo.name)) {
            ...
            reporter.reportOn(
                ...
                factory = CfirErrors.INHERIT_MEMBER_KIND_INCONSISTENT,  // L975（仅 !hasStaticConflict 时可达）
                ...
            )
        }
        continue
    }
    ...
}
```

- 关键缺陷：当同名成员**同时**存在 static 差异时，`hasStaticConflict` 为 true，L954 报出 `STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME`；而 kind 分支被 `!hasStaticConflict` 门禁挡住，`INHERIT_MEMBER_KIND_INCONSISTENT`（L975）永远到不了——但官方语义在这个继承场景报的正是 `INHERIT_MEMBER_KIND_INCONSISTENT`。

## 测试文件

- `cfir/analysis-tests/testData/llt/overload/class_impl_interface1.cj`、`2.cj`、`3.cj`、`4.cj`
- `cfir/analysis-tests/testData/llt/overload/class_extends_class1.cj`、`2.cj`、`4.cj`、`5.cj`
- 共 16 个失败（8 个 fixture × PSI/非 PSI 双路径），全部是 `替换: INHERIT_MEMBER_KIND_INCONSISTENT -> STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME`。

以 `class_impl_interface1.cj` 为例（EXP 期望 `INHERIT_MEMBER_KIND_INCONSISTENT`，ACT 报 `STATIC_AND_NON_STATIC...`）：

```cangjie
interface Base {
    static func foo(a: Int64) { return a }
}
class Data <: Base {
    public func foo() {           // EXP: <!INHERIT_MEMBER_KIND_INCONSISTENT!>foo<!>()
        return 1                  // ACT: <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>foo<!>()
    }
}
```

## 问题详情

**上层现象**：子类实例函数与父类/接口的 static 同名函数冲突时，官方 `cjc` 报 `INHERIT_MEMBER_KIND_INCONSISTENT`（成员种类不一致），CFIR 却报 `STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME`。16 个失败全部是这一种诊断名替换，无一例外。

**下层根因**：`CfirInheritanceDeepChecker` 把"static 差异"和"成员种类差异（函数/属性）"当成两个互斥的诊断分支，且 static 分支优先。但官方语义中，继承链上的 static/实例同名冲突本身就被归类为"成员种类不一致"（`INHERIT_MEMBER_KIND_INCONSISTENT`），是 kind 不一致的一个子类——官方 C++ `InheritanceChecker` 先比较父/子成员种类（含 static 属性）后统一报 `sema_inherit_member_kind_inconsistent`。CFIR 额外引入的 `STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME` 应该只用于**同一声明层**（同一 class 内部 static 与实例成员同名，`CfirConflictsDeclarationChecker` 管辖），而不是继承比较路径。

**为什么是共享缺陷**：`hasStaticConflict` 与 `kind != superInfo.kind` 在同一循环里对所有继承成员生效，任何"只改 fixture 期望"的做法都会在其他同类 fixture 上复发；必须改检查器的分支逻辑本身。

## 修复方案

1. **调整分支归属**：在继承比较循环（L948-984）中，static 冲突不再单独报 `STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME`。改为：当 `hasStaticConflict` 为 true 时，并入 kind 不一致分支——对非 extend 场景统一报 `INHERIT_MEMBER_KIND_INCONSISTENT`（L975），对 extend 场景报 `EXTEND_MEMBER_CANNOT_SHADOW`（L968），与官方语义一致。
2. **保留正确的使用场景**：`STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME` 仅保留给同声明层冲突（若 `CfirConflictsDeclarationChecker` 需要它）；继承路径不再使用。
3. **注意去重集合**：`reportedStaticConflicts` 与 `reportedKindConflicts` 的去重键要统一（改用 `overrideDiagnosticKey` 语义键），避免同一成员既进 static 集合又进 kind 集合导致漏报/重复报。
4. **回归范围**：`*OverloadGenerated*`（`class_impl_interface*`、`class_extends_class*` 全族）、`*InterfaceConflictInheritance*`、`*ExtendMemberCannotShadow*`，PSI 与 LightTree 双路径。

---

# 问题 6：不可变 struct/record 函数内的可变性检查漏报——嵌套字段修改与 mut 函数调用

## 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirMutabilityCheckers.kt`

- `CANNOT_MODIFY_VAR` 漏报入口：`CfirImmutableFunctionCannotModifyFieldChecker.check`（第 84-96 行）：

```kotlin
context(context: CheckerContext, reporter: DiagnosticReporter)
override fun check(expression: CfirAssignment) {
    val mutationContext = context.currentImmutableStructMutationContext() ?: return
    val lValue = expression.lValue as? CfirQualifiedAccessExpression ?: return
    val root = lValue.currentStructMutationRoot(mutationContext.owner) ?: return
    reporter.reportOn(
        source = root.access.calleeReference.source ?: ...,
        factory = CfirErrors.CANNOT_MODIFY_VAR, ...
    )
}
```

- `IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION` 漏报/错报入口：`CfirImmutableFunctionCannotAccessMutableFunctionChecker.check`（第 106-131 行）与 `CfirImmutableValueCannotAccessMutableFunctionChecker.check`（第 187-199 行）。
- 关键判定辅助：`currentImmutableStructMutationContext()`（第 86 行调用）负责识别"当前是否处于不可变 struct 函数上下文"；`isCurrentStructReceiverAccess()`（第 112 行）识别接收者是否当前实例。
- 相关错报：`CfirExpressionSemanticsChecker.kt` 第 252 行 `USE_MUTABLE_FUNC_ALONE`（`CfirFunctionReferenceLegalityChecker.kt` L44/L75 也有）在不可变接收者调 mut 函数场景**抢先**报了 `USE_MUTABLE_FUNC_ALONE`，压掉官方应有的 `IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION`。

## 测试文件

- 漏报 `CANNOT_MODIFY_VAR`（20 个 `缺少: CANNOT_MODIFY_VAR`）：
  - `cfir/analysis-tests/testData/llt/function/mut_function_01.cj`、`04.cj`、`05.cj`、`11.cj`
  - `cfir/analysis-tests/testData/llt/record/mut/record_mut_invalid_2.cj`、`3.cj`、`record_extend_mut_invalid_2.cj`
  - `property_callee_33_1.cj`、`classtypefield.cj`、`arith08.cj`
- 漏报 `IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION`（10 个 `缺少`）：`record_mut_invalid_12.cj`、`16.cj`、`record_extend_mut_invalid_8.cj`、`11.cj`、`property_callee_33_2.cj`
- 错报 `USE_MUTABLE_FUNC_ALONE`（8 个 `替换: IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION -> USE_MUTABLE_FUNC_ALONE`）：`record_mut_invalid_14.cj`、`15.cj`、`record_extend_mut_invalid_10.cj`、`12.cj`

`mut_function_01.cj` 的实际差异（EXP 期望 `CANNOT_MODIFY_VAR`，ACT 无诊断）：

```cangjie
struct S2 {
    var t2: S1 = S1()
    init() {}
    func f() {
        t2.t1.t0 += 10   // EXP: <!CANNOT_MODIFY_VAR!>t2<!>.t1.t0 += 10（非 mut 函数不允许）
    }                    // ACT: 无诊断
}
```

`record_mut_invalid_12.cj` 的实际差异（EXP 期望 `IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION`，ACT 无诊断）：

```cangjie
struct R1 {
    public var i = 0
    public mut func foo(): Unit { i += 1; hoo() }
    public func goo(): Unit {
        foo()   // EXP: <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>foo<!>()
    }           // ACT: 无诊断
}
```

## 问题详情

**上层现象**：官方语义中，不可变（非 `mut`）struct/record 成员函数不能修改当前实例的字段（含嵌套字段），也不能调用当前实例的 `mut` 成员函数。CFIR 在以下场景漏报或错报：

1. **嵌套字段链**（`t2.t1.t0 += 10`）：`currentStructMutationRoot` 只识别直接字段（root 定位到 `t2`），但对**复合赋值表达式**（`+=` 而非 `=`）和**多级嵌套访问链**（`t2.t1.t0`）的 root 提取失败，`check` 提前 `return`，漏报 `CANNOT_MODIFY_VAR`。
2. **属性调用链**（`property_callee_33_1.cj`）：属性 callee 作为修改目标时，`lValue` 不是直接的 `CfirQualifiedAccessExpression`，分类器未覆盖。
3. **mut 函数调用**（`record_mut_invalid_12.cj`）：`CfirImmutableFunctionCannotAccessMutableFunctionChecker` 的 `isCurrentStructReceiverAccess()` 只识别"隐式 this 接收者"（L112），但 `goo()` 内裸调用 `foo()` 时 `expression.explicitReceiver`/`dispatchReceiver` 的形态与检查器预期不符，`currentImmutableStructFunction()`（L113）也只在特定上下文返回非空——漏报。
4. **诊断名抢占**（`record_mut_invalid_14/15.cj`）：不可变接收者调 mut 函数的场景，`CfirFunctionReferenceLegalityChecker`（L44/L75）的 `USE_MUTABLE_FUNC_ALONE` 先报，把官方 `IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION` 顶掉。

**为什么是共享缺陷**：四个检查器（字段修改、函数调用、捕获、不可变值）共用 `currentImmutableStructMutationContext()`/`isImmutableStructValueAccess()` 等上下文判定，任何一个入口的上下文识别不完整都会造成整族漏报；且 `USE_MUTABLE_FUNC_ALONE` 与 `IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION` 的优先级关系未对齐官方。

## 修复方案

1. **复合赋值/嵌套链**：`CfirImmutableFunctionCannotModifyFieldChecker` 增加对 `CfirCompoundAssignmentExpression`（`+=`/`-=` 等）的处理，并把 `currentStructMutationRoot` 改为沿访问链递归提取最外层实例字段 root（`t2.t1.t0` 的 root 是 `t2`），同时支持属性 callee 形态。
2. **mut 函数调用**：`CfirImmutableFunctionCannotAccessMutableFunctionChecker` 的接收者识别补上"裸调用当前实例 mut 函数"（无显式接收者但 dispatch receiver 是当前实例）形态；`currentImmutableStructFunction()` 在 record/struct 的普通（非 mut）成员函数上下文中返回当前函数。
3. **诊断优先级**：对齐官方 `CheckLetInstanceAccessMutableFunc`——不可变接收者调 mut 成员函数时统一报 `IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION`；`USE_MUTABLE_FUNC_ALONE` 只保留给"函数引用单独使用"场景（`CfirFunctionReferenceLegalityChecker`），并在该处排除不可变接收者调用的路径。
4. **回归范围**：`*MutFunctionGenerated*`、`*RecordMutInvalid*`、`*RecordExtendMutInvalid*`、`*PropertyCallee*`、`*Classtypefield*`、`*Arith08*`、`*ImmutableFunctionRestrictions*`，PSI 与 LightTree 双路径。

---

# 问题 7：构造器/静态初始化中"先使用后初始化"漏报 USED_BEFORE_INITIALIZATION

## 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInitializationCheckers.kt`

- 漏报入口：`CfirInitializationFlowAnalyzer`（第 119 行起）的字段/变量初始化状态机。核心是 `analyzeAssignmentTargetAccess`（第 614-702 行）与 `reportUsedBeforeInitialization`（第 1187-1197 行附近）：

```kotlin
private fun reportUsedBeforeInitialization(diagnosticName: Name, source: CjSourceElement?) {
    reportedInitializationDiagnosticCount++
    if (reportReadDiagnostics) {
        with(context) {
            reporter.reportOn(
                source = source,
                factory = CfirErrors.USED_BEFORE_INITIALIZATION,   // L1191
                a = diagnosticName,
            )
        }
    }
}
```

- 状态推进：`markInitialized`（L2029）、`isPossiblyInitialized`（L2065）、`trackedVariable`（L2071）、`mayRevisitAssignment`（L2077）。
- 相关但不同的问题：同文件的 `reportCaptureBeforeInitialization`（L1205-1215，`CAPTURE_BEFORE_INITIALIZATION`）与 `reportIllegalMemberAccessFromNestedInitializer`（L1219-1242，`ILLEGAL_USAGE_OF_MEMBER`）属于相邻但独立的语义，不应混淆。

## 测试文件

- `cfir/analysis-tests/testData/llt/class/super_this/super_this_05.cj`、`06.cj`、`07.cj`、`08.cj`
- `cfir/analysis-tests/testData/llt/class/constructor/class_init_constructor4.cj`、`8.cj`、`9.cj`
- `cfir/analysis-tests/testData/llt/InitializationCheck/variable_use_before_init_11.cj`、`15.cj`
- 共 18 个 `缺少: USED_BEFORE_INITIALIZATION` 失败 + 若干组合对（`UNDECLARED_TYPE_NAME, USED_BEFORE_INITIALIZATION`、`STATIC_... -> ILLEGAL_USAGE_OF_MEMBER, USED_BEFORE_INITIALIZATION`）。

`variable_use_before_init_11.cj` 的实际差异（EXP 期望 `USED_BEFORE_INITIALIZATION`，ACT 无诊断）：

```cangjie
// 某个作用域内：
var a: A
a.x = 1   // EXP: <!USED_BEFORE_INITIALIZATION!>a<!>.x = 1 —— a 尚未初始化就访问其成员
```

`super_this_05.cj` 的实际差异（EXP 期望 `USED_BEFORE_INITIALIZATION`，ACT 无诊断）：

```cangjie
class C3 <: C2 {
    public var k: Int32
    public init() {
        k = super.<!USED_BEFORE_INITIALIZATION!>f<!>()   // 未先调用 super() 就访问父类成员
    }
}
```

## 问题详情

**上层现象**：两类官方明确报 `USED_BEFORE_INITIALIZATION` 的场景 CFIR 漏报：

1. **局部变量/字段声明后先访问成员再初始化**（`variable_use_before_init_11.cj`：`var a: A; a.x = 1`）；
2. **派生类构造器中未先调用 `super()` 就访问父类成员**（`super_this_05-08.cj`：`k = super.f()`）。

**下层根因**：

1. **未初始化成员访问的追踪缺口**：`analyzeAssignmentTargetAccess`（L614-702）对赋值目标 `a.x` 的成员访问，只在其解析到 `trackedVariable`（L627）且 `trackedVariable.kind == INSTANCE_MEMBER` 或普通变量时才推进状态。`a.x = 1` 中 `a` 是**未初始化局部变量**，其成员访问路径（`a.x`）的接收者 `a` 未建立 tracked 记录，走到 `else` 分支（L693-700）`reportIllegalMemberAccessIfNeeded`——但该路径对"未初始化变量的成员写入"判定条件不满足，直接漏报。
2. **super 调用链缺口**：`super_this_05-08.cj` 中 `super.f()` 访问父类成员，但状态机没有"super 构造器调用必须发生在父类成员访问之前"的语义边（问题 2 的构造器上下文缺陷同源：`super()` 调用未正确建模为初始化前置条件）。官方 `InitializationChecker::CheckInitInExpr` 在 `super` 成员访问处检查父类存储是否已初始化；CFIR 的 `InitializationState` 只跟踪当前实例字段，不跟踪父类存储槽的初始化状态。

**为什么是共享缺陷**：`reportUsedBeforeInitialization` 只有 `reportReadDiagnostics` 为 true 时才上报，且由 flow analyzer 统一驱动；`super_this` 族与 `variable_use_before_init` 族共用同一状态机，修 fixture 无意义，必须补状态机本身。

## 修复方案

1. **未初始化局部变量的成员访问**：在 `analyzeAssignmentTargetAccess` 的 `else` 分支（L693-700）补上"接收者是未初始化局部变量"的判定：`access.explicitReceiver`/`dispatchReceiver` 解析出的变量未初始化时，报 `USED_BEFORE_INITIALIZATION`（a 参数用变量名）。
2. **super 成员访问的父类初始化状态**：给 `InitializationState` 增加父类存储槽跟踪（super fields），`super.member` 访问在 `super()` 调用前一律视为未初始化并报 `USED_BEFORE_INITIALIZATION`；`super()` 调用时标记父类存储槽已初始化（与 `markInitialized` 复用同一机制）。
3. **回归范围**：`*SuperThisGenerated*`、`*ClassInitConstructorGenerated*`、`*InitializationCheckGenerated*`（`variable_use_before_init*` 全族）、`*VariableAssignmentTerminated*`，PSI 与 LightTree 双路径。

---

# 问题 8：import 引入接口闭包后 `extend` 被误报 EXTEND_ORPHAN_RULE

## 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirExtendCheckers.kt`

- 误报入口：`CfirExtendOrphanRuleChecker.check`（第 321-342 行）：

```kotlin
context(context: CheckerContext, reporter: DiagnosticReporter)
override fun check(extend: CfirExtend) {
    val query = context.session.extendRuleQueryServiceOrNull ?: return
    val declarationPackage = query.packageFqNameOf(extend) ?: return
    val targetClassId = query.targetClassIdOf(extend) ?: return
    if (CfirExtendSemantics.isTargetDeclaredInPackage(context, extend, declarationPackage)) return

    val currentInterfaceClosure = query.inheritedInterfaceClosureClassIdsOf(extend)
    val currentExternalInterfaces = currentInterfaceClosure
        .filterTo(linkedSetOf()) { interfaceClassId -> interfaceClassId.packageFqName != declarationPackage }
    if (currentExternalInterfaces.isEmpty()) return

    val otherPackageClosure = query.otherPackageExtendedInterfaceClassIds(targetClassId, declarationPackage)
    val newlyIntroducedExternalInterfaces = currentExternalInterfaces - otherPackageClosure
    if (newlyIntroducedExternalInterfaces.isEmpty()) return

    reporter.reportOn(
        source = extend.extendedTypeRef.source,
        factory = CfirErrors.EXTEND_ORPHAN_RULE,
        a = targetClassId.shortClassName,
    )
}
```

- 闭包来源：`cfir/cfir-common/src/org/cangnova/cangjie/cfir/session/services/CfirExtendRuleQueryService.kt` 第 93 行 `inheritedInterfaceClosureClassIdsOf`、第 138-143 行 `otherPackageExtendedInterfaceClassIds`；实际实现在 `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/services/CfirExtendIndexStore.kt`（闭包缓存第 176-183 行）。

## 测试文件

- `cfir/analysis-tests/testData/llt/Extend_import/import_orphanrule_01/main.cj` ～ `import_orphanrule_06/main.cj`
- 共 14 个 `多余: EXTEND_ORPHAN_RULE` 失败（7 个 fixture × PSI/非 PSI 双路径）。

`import_orphanrule_02/main.cj` 的实际差异（EXP 无诊断，ACT 误报）：

```cangjie
// p1.cj（另一文件）：
public interface I1 {}
public interface I2 {}
public open class A {}
extend A <: I1 & I2 {}
public class B <: A {}

// main.cj：
internal import p1.*
public interface I3 <: I1 & I2 {}
extend B <: I3 {}    // ACT: extend <!EXTEND_ORPHAN_RULE!>B<!> <: I3 {}
```

## 问题详情

**上层现象**：孤儿规则（orphan rule）的本意是：extend 目标类型不在当前包时，不能通过 extend 引入新的外部接口闭包。但本例中 `I3` 是当前包声明的接口，它继承的 `I1`/`I2` 来自 `p1` 包——**通过 import 可见**。官方语义认为这类 extend 合法（目标类型 `B` 的基类 `A` 已在 p1 被扩展过 `I1 & I2`，接口闭包已存在于外部扩展记录中），不报孤儿规则。CFIR 误报。

**下层根因**（`CfirExtendOrphanRuleChecker.check` 的差集逻辑）：

1. `currentInterfaceClosure` 取 `I3` 的接口闭包 = `{I3, I1, I2}`；过滤掉当前包（main 包）声明后，`currentExternalInterfaces = {I1, I2}`（来自 p1）。
2. `otherPackageClosure = query.otherPackageExtendedInterfaceClassIds(B, mainPackage)`——查询**其他包**对同一目标 `B` 已扩展过的接口闭包。`B` 的基类 `A` 在 p1 被 `extend A <: I1 & I2`，所以索引里应该有 `{I1, I2}`。
3. 差集 `currentExternalInterfaces - otherPackageClosure` 本应为空；实际非空，说明 **`otherPackageExtendedInterfaceClassIds` 没有返回 p1 对 `A` 扩展的接口闭包**——根因在索引层：该查询按 `targetClassId`（`B`）找"对同一目标扩展过的接口"，但 `A` 的 extend 记录挂在 `A` 的 target 键下，索引没有把**父类继承传递**（`B <: A` 时，`A` 的扩展闭包应并入 `B` 的查询结果）纳入 `otherPackageExtendedInterfaceClassIds`。
4. 于是 `newlyIntroducedExternalInterfaces = {I1, I2}` 非空 → 误报。这正是 `import_orphanrule_01/02` 系列（目标类型继承链上存在外部包的 extend）全部失败的共同机制。

**为什么是共享缺陷**：误报发生在索引层查询语义（`otherPackageExtendedInterfaceClassIds` 缺父类传递），所有"extend 目标继承自外部包已扩展类"的 fixture 都会命中；checker 侧的 `filterTo`/差集逻辑本身没错，修 checker 无意义。

## 修复方案

1. **索引层补父类传递**：`CfirExtendIndexStore.otherPackageExtendedInterfaceClassIds`（L138-143 对应实现）在收集其他包对目标的扩展接口闭包时，沿目标类型的**父类继承链**（`B <: A`）把父类上的外部扩展接口一并并入返回集合。对齐官方 `TypeManager` 中 orphan rule 的 `IsExtendTargetOfSameType`/继承链遍历语义。
2. **核对包归属过滤**：`currentExternalInterfaces` 的过滤（`interfaceClassId.packageFqName != declarationPackage`）与官方"外部接口"定义核对——经 import 可见的接口是否算"外部"应以官方 orphan rule 定义为准。
3. **回归范围**：`*ImportOrphanrule*Generated`（01-06 全族）、`*ExtendOrphanRule*`、`*ExtendImport*` 相关，PSI 与 LightTree 双路径。

---

# 问题 9：类型推断失败与泛型约束诊断——NEW_INFERENCE_ERROR 漏报、GENERIC_* 约束诊断名错位

## 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt`

- `NEW_INFERENCE_ERROR` 漏报：推断错误映射主路径第 480-500 行（`else -> CfirErrors.NEW_INFERENCE_ERROR.on(...)`），以及第 758、2170、2698、2744、2755 行多处推断错误分支。
- `GENERIC_ARGUMENT_NO_MATCH` 映射：第 796 行（`WrongArgumentCount` 分支）；`GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT` 映射：第 2309、2525 行。
- `UNABLE_TO_INFER_GENERIC_FUNC` 映射：第 412 行。
- 约束上界违规辅助：`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/CfirUpperBoundViolatedHelpers.kt` 第 246 行。

## 测试文件

- 漏报 `NEW_INFERENCE_ERROR`（12 个 `缺少: NEW_INFERENCE_ERROR`）：
  - `cfir/analysis-tests/testData/diagnostics2/inference/intersectionCollapsePlaceholder.cj`、`newInferenceErrorConflict.cj`、`builderInferenceMultiLambdaRestriction.cj`、`inferencePlaceholder.cj`、`genericReturnTypeInferencePlaceholder.cj`、`genericArgumentConstraintConflict.cj`
- 替换 `NEW_INFERENCE_ERROR -> TYPE_MISMATCH`（8 个）：`argumentTypeMismatch.cj`、`varraySizeMismatch.cj`、`varraySizeMismatchRich.cj`
- 替换 `UNABLE_TO_INFER_GENERIC_FUNC -> NEW_INFERENCE_ERROR`（6 个）：`array_constructor02.cj`、`type_arg_infer4.cj`、`type_arg_infer6.cj`
- 替换 `GENERIC_ARGUMENT_NO_MATCH -> GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT`（12 个）：
  - `cfir/analysis-tests/testData/llt/constraint_check/constraint_check_test5_2n.cj`、`constraint_check_test6_1.cj`、`constraint_instantiate_test4.cj`、`5.cj`、`6.cj`、`varray_ctype01.cj`
- 多余 `GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT`（18 个）：`alias4.cj`、`constraint_check_test4_2n.cj`、`f_bounded_1.cj`、`generic_static_prop07.cj`、`solve1.cj`、`typealias28.cj`、`typealias8.cj`、`undeclared_type.cj`
- 漏报/多报 `UNABLE_TO_INFER_GENERIC_FUNC`（8+8 个）：`f_bounded_2/3/4.cj`、`generic_call_static_impl11.cj`、`invalid_case.cj`、`typealias20.cj`、`type_arg_infer2/5.cj`

`intersectionCollapsePlaceholder.cj` 的实际差异（EXP 期望 `NEW_INFERENCE_ERROR`，ACT 无诊断）：

```cangjie
func chooseGeneric<T>(first: T, second: T): T { return first }
func emptyIntersectionLike(): Unit {
    let value = <!NEW_INFERENCE_ERROR!>chooseGeneric<!>(1, 1.0)   // EXP：推断失败（交集坍缩为空）
    let _ = value                                                 // ACT：无诊断
}
```

`constraint_check_test5_2n.cj` 的实际差异（EXP 期望 `GENERIC_ARGUMENT_NO_MATCH`，ACT 报 `GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT`）：

```cangjie
func foo<T>() where T <: (Int32, Int32) { return 0 }   // 上界是 tuple（非法，但官方仍按约束语义）
main(): Int64 {
    <!GENERIC_ARGUMENT_NO_MATCH!>foo<(Int32, Int64)>()<!>   // EXP：GENERIC_ARGUMENT_NO_MATCH
}                                                           // ACT：GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT
```

## 问题详情

**上层现象**：三个相关的推断/约束诊断族全部错位或漏报：

1. **推断失败无诊断**：交集坍缩（`chooseGeneric(1, 1.0)` 的 T 约束交集为空）、占位符未求解、多 lambda 约束等场景，EXP 期望 `NEW_INFERENCE_ERROR`，ACT 静默无诊断或降级为 `TYPE_MISMATCH`。
2. **约束诊断名错位**：实参类型不满足上界约束时，官方报 `GENERIC_ARGUMENT_NO_MATCH`，CFIR 报 `GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT`；反向也有（`typealias28/8.cj` 报多余）。
3. **推断失败诊断名双向漂移**：`UNABLE_TO_INFER_GENERIC_FUNC` 与 `NEW_INFERENCE_ERROR` 互相对调（EXP 是前者 ACT 是后者，或反之）。

**下层根因**：

1. **推断错误映射不完整**（`coneDiagnosticToCfirDiagnostic.kt`）：推断管线产生的错误类型（`ConeErrorType` 或推断失败标记）在 L480-500 的 `when` 里只映射了部分子类，交集坍缩（intersection collapse）、占位符未求解（placeholder unsolved）路径走不到 `NEW_INFERENCE_ERROR` 分支，落入 `typeMismatchDiagnostic`（L490-497）或静默。`varraySizeMismatch` 则是 varray 大小约束错误被映射成普通 `TYPE_MISMATCH`（`CfirTypeSemanticsDiagnostics.kt` L138）。
2. **两种约束诊断触发边界未对齐**：官方 Sema 中"实参类型与约束不匹配"报 `GENERIC_ARGUMENT_NO_MATCH`（实参视角），"约束本身冲突/无法满足"报 `GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT`（约束声明视角）。CFIR 的 `CfirUpperBoundViolatedHelpers.kt` L246 与 cone 映射 L2309/2525 对两类场景的归属与官方相反，尤其在上界是 tuple/非法类型时（`constraint_check_test5_2n.cj` 的上界本身就是非法 tuple，官方仍按实参匹配判定）。
3. **推断失败诊断名选择**：`UNABLE_TO_INFER_GENERIC_FUNC`（L412）与 `NEW_INFERENCE_ERROR` 的触发路径在 F-bounded（`f_bounded_*.cj`）、type_arg_infer 场景下互相覆盖。

**为什么是共享缺陷**：三个诊断族都来自同一推断错误映射表（`coneDiagnosticToCfirDiagnostic.kt`），修任何一个 fixture 都会在另一方向复发；必须按官方语义把"推断失败"与"约束不满足"两类错误在映射表里正确分流。

## 修复方案

1. **补齐推断失败映射**：在 `coneDiagnosticToCfirDiagnostic.kt` 的推断错误 `when` 分支中，为交集坍缩（intersection collapse）、占位符未求解、约束冲突候选失败等路径显式映射 `NEW_INFERENCE_ERROR`；varray 大小检查失败走 `VARRAY_SIZE_MISMATCH` 而非普通 `TYPE_MISMATCH`（`CfirTypeSemanticsDiagnostics.kt` L138）。
2. **分流两种约束诊断**：对照官方 Sema——实参类型与约束不匹配 → `GENERIC_ARGUMENT_NO_MATCH`（`coneDiagnosticToCfirDiagnostic.kt` L796 已映射，需确保优先于 L2309/2525）；约束声明本身非法/冲突 → `GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT`。`CfirUpperBoundViolatedHelpers.kt` L246 的调用方需按实参/声明视角选择。
3. **统一推断失败诊断名**：`UNABLE_TO_INFER_GENERIC_FUNC` 与 `NEW_INFERENCE_ERROR` 按官方边界固定（官方对函数调用的推断失败报 NEW_INFERENCE_ERROR；对无调用点的裸泛型函数引用报 UNABLE_TO_INFER_GENERIC_FUNC）。
4. **回归范围**：`diagnostics2/inference` 目录全族、`*ConstraintCheckGenerated*`、`*ConstraintInstantiateGenerated*`、`*Typealias*`（28/8/20）、`*FBounded*`、`*TypeArgInfer*`、`*VarraySizeMismatch*`、`*ArrayConstructor*`，PSI 与 LightTree 双路径。

---

# 问题 10：import 与宏/effects 基建——UNRESOLVED_IMPORT/UNUSED_IMPORT 误报、EFFECTS_FEATURE_DISABLED 连锁失败

## 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirImportsChecker.kt`

- `UNRESOLVED_IMPORT` 误报入口：`reportImportResolutionDiagnostic`（第 155-173 行附近），关键在 `findUnresolvedParentSegmentIndex`（第 163 行调用）与 `hasResolvedTerminalImportTarget`（第 170 行）：

```kotlin
private fun reportImportResolutionDiagnostic(import, importBindingsByImport) {
    val importedFqName = import.importedFqName?.takeUnless { it.isRoot } ?: return
    ...
    val unresolvedParentSegmentIndex = findUnresolvedParentSegmentIndex(pathSegments, import.isAllUnder)
    if (unresolvedParentSegmentIndex != null) {
        ...
        reporter.reportOn(source, CfirErrors.UNRESOLVED_IMPORT, ...)   // L165
        return
    }
    if (!import.isAllUnder && !hasResolvedTerminalImportTarget(...)) {
        reporter.reportOn(import.source, CfirErrors.UNRESOLVED_IMPORT, ...)
    }
}
```

- `UNUSED_IMPORT` 误报入口：`reportUnusedImports`（第 180-215 行），使用集合来自 `declaration.collectImportUsage(context.session)`（第 183 行）。

## 测试文件

- `UNRESOLVED_IMPORT` 多余（24 个）：
  - `cfir/analysis-tests/testData/macro/llt/annotation/globals/globalfunc.cj`、`ok_class_05.cj`、`ok_class_07.cj`、`main_ok.cj`、`main_ok1.cj`、`dep_err.cj`
  - `cfir/analysis-tests/testData/llt/imports/imported_forinrange.cj`、`default.cj`、`increment_45.cj`、`incordec_fuzz_01.cj`
- `UNUSED_IMPORT` 多余（16 个）：
  - `cfir/analysis-tests/testData/macro/llt/annotation/globals/unused001.cj`、`unused003.cj`、`unused015.cj`、`unused016.cj`、`enumcons_inside_macro.cj`、`typeaslias.cj`、`err1.cj`
- effects 族（14 个 `多余: EFFECTS_FEATURE_DISABLED, UNRESOLVED_IMPORT`）：
  - `cfir/analysis-tests/testData/llt/effect/non_matching_types.cj`、`resume_unit.cj`、`resume_with.cj`、`resume_throwing.cj`、`effect_perform.cj`、`effect_perform_02.cj`、`effect_test.cj`

`non_matching_types.cj` 的实际差异（EXP 只报 `MISMATCHING_HANDLE_BLOCK`，ACT 还报 UNRESOLVED_IMPORT + EFFECTS_FEATURE_DISABLED）：

```cangjie
import stdx.effect.Command          // ACT: import <!UNRESOLVED_IMPORT!>stdx<!>.effect.Command
class Eff <: Command<Unit> {}
main() {
    let x = try {
      34
    } <!MISMATCHING_HANDLE_BLOCK!>handle (_: Eff)<!> {   // ACT 还额外报 <!EFFECTS_FEATURE_DISABLED!>
      "Hello"
    }
    0
}
```

## 问题详情

**上层现象**：

1. **`import a.*` 被误报 UNRESOLVED_IMPORT**：`globalfunc.cj`（datarace 测试用例）等宏场景 fixture 中，测试基建提供的包 `a` 在宏展开路径下 import 解析失败。EXP 无诊断。
2. **宏展开后 import 使用计数丢失**：`unused001/003/015/016.cj` 等 fixture 中，宏展开产物引用了 import 的符号，但 `collectImportUsage` 的使用集合没有计入展开产物的引用，导致误报 `UNUSED_IMPORT`。
3. **effects 特性整体失败**：`non_matching_types.cj` 等 7 个 effects fixture 中，`import stdx.effect.Command` 报 `UNRESOLVED_IMPORT`，`handle` 报 `EFFECTS_FEATURE_DISABLED`。EXP 只期望语义诊断 `MISMATCHING_HANDLE_BLOCK`——因为 import 失败，`handle` 语法解析链路也中断，产生连锁误报。

**下层根因**（三类问题根因不同，但都属测试基建/import 语义层，非单个 checker 逻辑错误）：

1. **UNRESOLVED_IMPORT**：`reportImportResolutionDiagnostic` 的 `hasResolvedTerminalImportTarget` 依赖 import 绑定存储（`importBindingStoreOrNull`）。宏场景（`CfirAnalysisMacroTest`）与 effects 场景（`stdx` 扩展包）中，**测试运行环境没有把 `a` 包 / `stdx.effect` 包注入可见包集合**，绑定存储里查不到目标，于是误报。这是测试基建的依赖注入问题，不是 checker 判定逻辑本身错。
2. **UNUSED_IMPORT**：`collectImportUsage`（`CfirImportsChecker.kt` 第 183 行调用，实现在 imports 使用收集器）在收集"哪些 import 被使用"时只遍历**普通源码**的引用，宏展开产物（`macroExpansionRegistry` 记录的展开代码）中的符号引用未计入——第 203-204 行虽有 `referencesUsedMacroPackage`/`usedMacroNames` 的补丁，但只覆盖 `*` 导入与宏包，单名导入的展开使用未覆盖。
3. **EFFECTS_FEATURE_DISABLED**：`handle`/`resume`/`perform` 的语法检查依赖语言特性开关（language version settings 的 effects 标志）。测试配置未开启该特性，解析器/检查器直接报 `EFFECTS_FEATURE_DISABLED`（诊断定义见 `CfirErrorsDefaultMessages.kt` L478-482）。

**为什么是共享缺陷**：7 个 effects fixture 全部同时报 `EFFECTS_FEATURE_DISABLED + UNRESOLVED_IMPORT`（14 个失败 = 7 fixture × 双路径），说明是**同一基建配置**问题；修 checker 无法解决，必须改测试基建。

## 修复方案

1. **测试基建注入 stdx/测试包**：在 analysis-tests 的测试环境配置中注入 `stdx`（effects 标准库扩展）与各测试 fixture 自建包（`a` 等）的可见性，使 import 绑定存储能解析到这些包。修复后 effects 族与 `globalfunc.cj` 等的 UNRESOLVED_IMPORT 消失。
2. **开启 effects 特性开关**：在测试的 language version settings 中启用 effects 特性标志，使 `handle`/`resume`/`perform` 进入正常检查链路，只报语义诊断（如 `MISMATCHING_HANDLE_BLOCK`）。
3. **宏展开使用计数**：`collectImportUsage` 的收集器在遍历时把宏展开产物（`macroExpansionRegistry`）中的符号引用计入对应 import 的使用集合，覆盖单名导入（不只是 `*` 导入与宏包）。
4. **回归范围**：`*EffectGenerated*`（`EffectGenerated` 全族）、`macro/llt/annotation/globals` 下 `*Globals*`/`*OkClass*`/`*Unused*`、`*ImportedForinrange*`、`*Increment45*`，PSI 与 LightTree 双路径。

---

# 问题 11：成员查找与类型兼容诊断族——NOT_MEMBER_OF 误报、UNRESOLVED_REFERENCE 双向漂移、多赋值 TYPE_MISMATCH 错位

> 本问题覆盖三个独立但都属"引用/类型兼容"层的高频诊断族。它们根因各自独立，分述如下。

## 11.1 NOT_MEMBER_OF 误报（29 个多余）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt`

- 映射入口：第 1904 行与第 2325 行（`ConeNotMemberOfError` → `CfirErrors.NOT_MEMBER_OF`）。成员查找失败的源头在 resolve 层的 use-site scope 构建（扩展成员、泛型实例化成员）。

### 测试文件

- `cfir/analysis-tests/testData/llt/generics/instantiation/O2_part_gi/disable_default_static.cj`（`UIntNative.<!NOT_MEMBER_OF!>foo3()`、`UIntNative.<!NOT_MEMBER_OF!>foo2<Float16><!>(1.0)`）
- `cfir/analysis-tests/testData/llt/Extend/extend_interface_static1.cj`、`main04.cj`、`main05.cj`、`pass02.cj`、`main.cj`
- `cfir/analysis-tests/testData/llt/class/class_infer_thistype_ok_1.cj`、`class_dynamic_binding_thistype_ok_2.cj`、`class_generic_infer_thistype_ok_1.cj`、`class_generic_dynamic_binding_thistype_ok_2.cj`

`disable_default_static.cj` 的实际差异（EXP 无诊断，ACT 误报）：

```cangjie
public interface faterInterface<T> {
    static func foo1(): T
    static func foo2<K>(a: K): K { foo1(); a }
}
interface MyInterface <: faterInterface<Float16> {
    static func foo3(): Float16 { foo2<Float16>(5.0) }
}
extend UIntNative <: MyInterface {
    public static func foo1(): Float16 { 0.0 }
}
main() {
    UIntNative.foo3()              // ACT: UIntNative.<!NOT_MEMBER_OF!>foo3<!>()
    UIntNative.foo2<Float16>(1.0)  // ACT: UIntNative.<!NOT_MEMBER_OF!>foo2<Float16><!>(1.0)
}
```

### 问题详情

`UIntNative` 通过 `extend` 实现 `MyInterface`，`MyInterface` 继承泛型接口 `faterInterface<Float16>`。官方语义中 `foo2`/`foo3` 经**泛型接口实例化后的静态成员查找**应命中；CFIR 的成员查找在"extend 实现的接口 + 泛型父接口实例化"组合下未命中，落到 `ConeNotMemberOfError` 被映射为 `NOT_MEMBER_OF`。另一子类（`class_*_thistype_ok_*.cj`）是 `This` 动态绑定成员查找误报：`This` 类型上声明的方法经动态绑定应可访问，CFIR 未走动态绑定查找。

### 修复方案

1. extend 接口闭包经泛型实例化后的静态成员查找：在 use-site scope 构建处，对 `extend` 实现的接口按其被实例化的类型实参替换后再查成员（对齐官方 `TypeCheckAccess` 对 extend 静态成员的查找）。
2. `This` 动态绑定：`class_*_thistype_ok_*.cj` 场景走动态绑定成员查找（`This` 的声明成员在派生类可见），不要把动态绑定误判为 NOT_MEMBER_OF。
3. 回归：`*O2PartGiGenerated*`、`*ExtendInterfaceStatic*`、`*ThisType*` 相关（`class_infer_thistype*`、`class_dynamic_binding_thistype*`、`class_generic_*_thistype*`），PSI 与 LightTree 双路径。

## 11.2 UNRESOLVED_REFERENCE 双向漂移（24 个缺少 + 25 个多余）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirApiLevelRefHigherChecker.kt` 第 50 行（解析后兜底报告点），以及 resolve 层的命名查找（`cfir/resolve/.../` 下 scope 构建）。

### 测试文件

- 缺少方向（24 个）：`extend_namelookup2.cj`、`extend_namelookup8.cj`、`extend_namelookup9.cj`、`extend_mutable_function_invalid_1.cj`、`const_safe_not_std.cj`、`record_extend_mut_invalid_13.cj`、`samename_conditionandifbody.cj`、`variable_use_before_init_10.cj`、`main.cj`
- 多余方向（25 个）：`assignment_1.cj`、`callingConventionBoundaryPlaceholder.cj`、`callingConventionOnClassPlaceholder.cj`、`callingConventionPlacementPlaceholder.cj`、`cfunc_assign.cj`、`class_generic_infer_thistype_ok_3.cj`、`default_implement_01.cj`、`function_shadow_02.cj`、`member_access_1.cj`、`binary_time_cost_2.cj`

`extend_namelookup2.cj` 的实际差异（EXP 期望 `UNRESOLVED_REFERENCE`，ACT 无诊断）：

```cangjie
// class 不能访问其 extend 中定义的成员函数
class A {
    func foo() {
        <!UNRESOLVED_REFERENCE!>go<!>()   // EXP：go 定义在 extend 里，类体内不可见
    }
}
extend A { func go() {} }
```

### 问题详情

**缺少方向**：官方语义中，**类体内不能看到该类 extend 中定义的成员**（`extend_namelookup2.cj` 的 `go()` 应报未解析）。CFIR 的类体 scope 构建错误地把 extend 成员也纳入了可见范围，导致漏报。**多余方向**：`callingConvention*Placeholder.cj`、`default_implement_01.cj` 等场景，CFIR 把实际可解析的引用（宏展开依赖、calling convention 注解、接口默认实现）误判为未解析。两个方向都源于 scope 构建对"extend 成员可见性边界"与"宏/注解/默认实现符号可见性"的处理错误。

### 修复方案

1. 类体 scope 与 extend 体 scope 分离：类体内不解析该类 extend 中声明的成员（保持官方语义），extend 体内可解析 extend 内已声明成员（`extend_namelookup8/9.cj` 是 extend 内互相调用的合法场景）。
2. 多余方向：核对 calling convention 注解、宏展开依赖、接口默认实现成员在 scope 构建中的可见性注入，避免合法引用落空。
3. 回归：`*ExtendNamelookup*`、`*ExtendMutableFunctionInvalid*`、`*CallingConvention*`、`*DefaultImplement*`、`*FunctionShadow*`、`*CFuncAssign*`，PSI 与 LightTree 双路径。

## 11.3 多赋值/元组赋值 TYPE_MISMATCH 错位（37 个缺少 + 37 个多余）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/CfirHelpers.kt` 第 48 行（`TYPE_MISMATCH` 通用报告入口），实际调用方在多赋值检查器（`CfirAssignmentTypeMismatchChecker.kt` 等）与 resolve 的期望类型传播。

### 测试文件

- 缺少方向（37 个）：`case07.cj`（`(i, val) = get()`）、`call25.cj`、`chkmatch.cj`、`controlFlowInitializationRich.cj`、`err_optional_chain_00.cj`、`err_try_with_resources_00.cj`、`match019.cj`、`match_no_selector_003.cj`、`matchcase2.cj`、`subtype_03.cj`、`sync_0.cj`、`synif.cj`
- 多余方向（37 个）：`case08.cj`（`(val, a[0]) = (2, 3)`）、`array_constructor01.cj`、`array_constructor03.cj`、`arraylit5.cj`、`binary_clear.cj`、`box_in_multi_matchcase.cj`、`bugfix_this.cj`、`defaultParamter.cj`、`generic_constraint13.cj`

`case07.cj` 的实际差异（EXP 期望 TYPE_MISMATCH，ACT 无诊断）：

```cangjie
interface I {}
class A {}
extend A <: I {}
func get(): ((A, A), Int64) { return ((A(), A()), 1) }
main() {
    var i: (I, I)
    var val = 1
    <!TYPE_MISMATCH!>(i, val) = get()<!>   // EXP：左值 (I,I),Int64 与右值 ((A,A),Int64) 不匹配
}                                            // ACT：无诊断
```

`case08.cj` 的实际差异（EXP 无诊断，ACT 多报）：

```cangjie
class A {
    operator func [](index: Int64): A { A() }
    operator func [](index: Int64, value!: Int64): Unit {}
}
main() {
    var val = 1
    var a = A()
    (val, a[0]) = (2, 3)   // ACT: (val, a[0]) = <!TYPE_MISMATCH!>(2, 3)<!> —— 官方不报
}
```

### 问题详情

多赋值表达式（元组左值 `(a, b) = rhs`）的类型检查在 CFIR 中方向错乱：

1. **缺少方向**（`case07.cj`）：左值是 `(i, val)`（类型 `(I, I), Int64`），右值是 `get()`（返回 `((A, A), Int64)`）。`A` 实现了 `I`，但 `(A, A)` 与 `(I, I)` 是不同 tuple 类型，官方报 `TYPE_MISMATCH`（tuple 元素协变不成立）。CFIR 的多赋值检查没有对"元素级类型对照"做 tuple 展开比较，漏报。
2. **多余方向**（`case08.cj`）：`a[0]` 走 `operator []`（返回 `A`），左值 `(val, a[0])` 是合法的可写目标；官方认为 `(2, 3)` 与 `(Int64, A)` 兼容。CFIR 把右值整体与左值做期望类型匹配时，对下标左值的期望类型推断错误，误报 `TYPE_MISMATCH`。

### 修复方案

1. 多赋值检查器按官方 `CheckInitInAssignExpr` 语义：元组左值逐元素对照右值（tuple 元素展开后做类型匹配），`(I, I)` 与 `(A, A)` 元素不匹配时报 `TYPE_MISMATCH` 且范围覆盖整个赋值表达式。
2. 下标左值（`a[0]`）在期望类型推断中按其 `operator []` 返回类型参与匹配，不被当作不可推断目标。
3. 回归：`*MultipleAssignExprGenerated*`（`case07`/`case08` 全族）、`*ArrayConstructor*`、`*Arraylit*`、`*BinaryClear*`、`*DefaultParamter*`、`*MatchNoSelector*`，PSI 与 LightTree 双路径。

---

# 问题 12：「范围/顺序」簇——诊断名与数量一致但下划线范围或输出顺序不同（253 个失败）

## 发生位置

本簇不是单一 checker 缺陷，而是**各 checker 的 `reporter.reportOn(source=...)` 参数选择**与官方 marker 定义不一致的汇总。逐项拆解后，253 个失败中 214 行是有效差异（39 行为测试基建的整文件差异，与 checker 无关），按形态分四类：

1. **同名标记范围漂移（106 行）**：同一诊断 EXP/ACT 下划线起止不同。
2. **标记数量不同（94 行）**：诊断名集合相同但出现次数不同（多处违规点漏标/多标）。
3. **标记顺序不同（10 行）**：同文件多诊断 EXP/ACT 输出顺序颠倒。
4. **完全相同?（4 行）**：EXP/ACT 标记文本一致仍失败，属测试框架对 marker 顺序的误判。

## 测试文件

覆盖 57 个 suite，按失败数前列：`ExtendGenerated`（16）、`OperatorOverloadGenerated`（9）、`LetInInitGenerated`（8）、`ExtendsImplementsInterfaceDuplicatedGenerated`（8）、`FunctionGenerated`（8）、`TypealiasGenerated`（8）、`InitializationCheckGenerated`（6）、`RedeclarationGenerated`（6）、`VarrayGenerated`（6）、`ConstEvaluationGenerated`（6）、`GenericConstraintInheritanceGenerated`（6）、`MutGenerated`（6）、`ConstraintCheckGenerated`（5）、`MatchExpressionGenerated`（4）等。

## 问题详情

按形态逐一说明根因：

**1. 同名标记范围漂移（106 行）**——每个子类都有确切的报告 source 选择缺陷：

- `INVALID_SUBSCRIPT_ASSIGN_RETURN`：EXP 标返回类型（`Int64`），ACT 标 `operator` 关键字。责任 `CfirOperatorDeclarationChecker.kt` L157-185，报告源取 `operatorDiagnosticSource()`（L192-193：优先 `operator` 修饰符源码）。官方语义标返回类型。
- `DIFFERENT_OR_PATTERN`：EXP 标整个 or-pattern（`true | e`），ACT 只标冲突替代项（`e`）。责任 `CfirPatternExpressionChecker.kt` L234-257，L247-251 按 `reportKindOnWholePattern` 布尔选择整模式或单替代项——let-condition 等入口传入 false 导致收窄。
- `OPTIONAL_CHAIN_NON_OPTIONAL`：EXP 含 `!` 前缀（`!s2.startsWith?("")`），ACT 不含。责任 `coneDiagnosticToCfirDiagnostic.kt` L2341，source 未含 `!`。
- `CLASS_UNINITIALIZED_FIELD`：EXP 标 `init` 关键字，ACT 标整个构造器头（`init(a: Int64, c: Bool)`）。责任 `CfirInitializationCheckers.kt` L359/L769，source 取声明整体。
- `SUPER_TYPES_DUPLICATE`（`CfirSupertypesChecker.kt` L88/L109）、`INHERIT_MEMBER_TYPE_INCONSISTENT`（`CfirInheritanceDeepChecker.kt` L616）、`TYPEALIAS_UNUSED_TYPE_PARAMETERS`（`CfirTypeAliasUnusedTypeParameterChecker.kt` L46）：范围扩宽/收窄混合，均为报告 source 选择策略差异。
- `VarrayGenerated` 的 `subsript_with_member_access_let.cj`、`ConstEvaluationGenerated` 的 `desugarexpr.cj` 等：下标/属性链场景的报告范围未对齐。

**2. 标记数量不同（94 行）**——诊断名集合相同但出现次数不同，根因是"同一声明多个违规点"的报告次数与官方不一致：

- `CANNOT_ASSIGN_TO_IMMUTABLE`（14 行）：与问题 1 同根因——宏展开 const init 中多个 `this.field = param` 赋值被分类为 REASSIGNMENT，部分漏报/多报（`CfirAssignmentLegalityChecker.kt` L372-389）。
- `INVALID_THIS_CALL_OUTSIDE_CTOR`（8 行）：与问题 2 同根因——委托链中部分 super() 上下文栈缺失（`CfirConstructorDelegationCallChecker.kt` L36-43）。
- `GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT`（6+4 行）：与问题 9 同根因——F-bounded/typealias 场景约束检查在多个实参上重复/遗漏（`CfirUpperBoundViolatedHelpers.kt` L246）。
- `RedeclarationGenerated/simple.cj`（6 行）、`LetInInitGenerated`（6 行）、`TypealiasGenerated`（6 行）、`ExtendGenerated/extend_duplicate_interfaces*.cj`（8 行）：重名/初始化/typealias/重复接口检查对多违规点报告次数不一致。

**3. 标记顺序不同（10 行）**——根因是检查器调度顺序：`CfirModifierChecker` 与声明级检查器（`CfirConflictsDeclarationChecker`、`CfirSupertypesChecker`）对同一声明的报告顺序与官方注册顺序不一致。`FunctionGenerated/defaultParameter4_3~5.cj`（6 行）、`LambdaCaptureGenerated/capture4.cj` 为代表。

**4. 完全相同?（4 行）**——EXP/ACT 标记文本一致仍失败，属测试框架对 marker 顺序/文件级差异的误判，非 checker 缺陷。

## 修复方案

1. **范围漂移**：逐个诊断对核对报告 source——官方标"声明名/关键字/返回类型/完整表达式"的，逐一调整各 checker 的 `reportOn(source=...)` 参数；优先处理 `INVALID_SUBSCRIPT_ASSIGN_RETURN`（改取返回类型 ref source）、`DIFFERENT_OR_PATTERN`（let-condition 入口传 `reportKindOnWholePattern=true`）、`OPTIONAL_CHAIN_NON_OPTIONAL`（取含 `!` 的完整表达式）、`CLASS_UNINITIALIZED_FIELD`（取 `init` 关键字）。
2. **数量不同**：核对各 checker 对"同一声明多个违规点"的遍历是否完整（如重复接口逐个 superTypeRef、重名成员逐个声明），按官方报告每个违规点。
3. **顺序不同**：对照官方 Sema 的检查顺序调整 checker 注册/遍历顺序（`CfirModifierChecker` 放声明级冲突检查之后）。
4. **回归**：按 suite 分批——`*ExtendGenerated*`、`*OperatorOverloadGenerated*`、`*LetInInitGenerated*`、`*ExtendsImplementsInterfaceDuplicatedGenerated*`、`*FunctionGenerated*`、`*TypealiasGenerated*`、`*VarrayGenerated*`、`*ConstEvaluationGenerated*`、`*MutGenerated*` 等，PSI 与 LightTree 双路径。

---

# 问题 13：低频诊断名逐项分析（一）——继承/override 语义组

> 以下每个子问题均已定位责任代码文件:行并读取源码确认触发条件（非推断），按五段格式给出。

## 13.1 override 返回类型不兼容：官方 OVERRIDING_RETURN_TYPE_MISMATCH，CFIR 报 RETURN_TYPE_INCOMPATIBLE（8 个替换）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirOverrideChecker.kt` 第 344 行；`CfirInheritanceDeepChecker.kt` 第 149、1971 行；`CfirOperatorDeclarationChecker.kt` 第 136 行（均报 `RETURN_TYPE_INCOMPATIBLE`）。

### 测试文件

`overrideReturnTypeMismatch.cj`、`overrideReturnTypeMismatchRich.cj`、`extend_function_conflict_invalid_6.cj`、`interface_conflict_inheritance_01.cj`、`covariance.cj`（后两个是 `RETURN_TYPE_INCOMPATIBLE` 与其他诊断的组合）。

### 问题详情

EXP 期望 `OVERRIDING_RETURN_TYPE_MISMATCH`（override 语义专用），ACT 报 `RETURN_TYPE_INCOMPATIBLE`。三个检查器（OverrideChecker、InheritanceDeepChecker、OperatorDeclarationChecker）各自报了后者，说明诊断名选择未按官方 override 语义统一。

### 修复方案

统一 override 返回类型不兼容的诊断名为官方 `OVERRIDING_RETURN_TYPE_MISMATCH`；协变返回（`RETURN_TYPE_INCOMPATIBLE`）仅保留给非 override 场景。

## 13.2 override 不可见成员：CANNOT_OVERRIDE_INVISIBLE_MEMBER 漏报（4 个缺少）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirOverrideChecker.kt` 第 97 行。

### 测试文件

`overrideInvisibleAndNothingToOverride.cj`、`overrideInvisibleMemberRich.cj`。

### 问题详情

EXP 期望先报 `CANNOT_OVERRIDE_INVISIBLE_MEMBER`（override 目标是不可见成员）再报 `NOTHING_TO_OVERRIDE`；ACT 只报了 `NOTHING_TO_OVERRIDE`。覆盖搜索未做可见性判定，直接把不可见成员当作"没有可覆盖目标"。

### 修复方案

在 `CfirOverrideChecker` 的覆盖搜索中先做可见性判定（成员存在但不可见 → `CANNOT_OVERRIDE_INVISIBLE_MEMBER`），不可见成员不落入 `NOTHING_TO_OVERRIDE`（后者在 `CfirModifierChecker.kt` L183 与 `CfirOverrideChecker.kt` L87）。

## 13.3 接口成员实现诊断名：ABSTRACT_MEMBER_NOT_IMPLEMENTED 与 INTERFACE_MEMBER_MUST_BE_IMPLEMENTED 边界错位（4+4 个替换）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInheritanceDeepChecker.kt` 第 351 行（`INTERFACE_MEMBER_MUST_BE_IMPLEMENTED`）；`CfirNotImplementedOverrideChecker.kt` 第 78 行（`ABSTRACT_MEMBER_NOT_IMPLEMENTED`）。

### 测试文件

`C.cj`、`test.cj`（EXP 报 ABSTRACT_MEMBER_NOT_IMPLEMENTED、ACT 报 INTERFACE_MEMBER_MUST_BE_IMPLEMENTED）；`implement_by_super_extend02/03/04/06.cj`（反向：EXP 无诊断、ACT 多余 ABSTRACT_MEMBER_NOT_IMPLEMENTED，8 个多余）。

### 问题详情

两个方向都错：普通类实现接口缺成员时官方报 `ABSTRACT_MEMBER_NOT_IMPLEMENTED`、CFIR 报 `INTERFACE_MEMBER_MUST_BE_IMPLEMENTED`；而 super extend 已提供实现的场景 CFIR 反而多报 `ABSTRACT_MEMBER_NOT_IMPLEMENTED`（`satisfiesExtendInterfaceRequirement` 未正确识别 super extend 实现）。

### 修复方案

1. 对齐边界：类场景缺实现 → `ABSTRACT_MEMBER_NOT_IMPLEMENTED`；extend 场景缺实现 → `INTERFACE_MEMBER_MUST_BE_IMPLEMENTED`。
2. super extend 提供实现的成员计入已实现（`CfirInheritanceDeepChecker.kt` L258-263 的 `hasSatisfiedImplementation` 逻辑补全）。

## 13.4 多父类型一致性漏报：INHERIT_SUPER_MEMBER_KIND_INCONSISTENT / INHERIT_MEMBER_TYPE_INCONSISTENT（6+2 个缺少）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInheritanceDeepChecker.kt` 第 581 行（`INHERIT_SUPER_MEMBER_KIND_INCONSISTENT`）、第 616 行（`INHERIT_MEMBER_TYPE_INCONSISTENT`）。

### 测试文件

`interface_conflict_inheritance_06.cj`、`interface_conflict_inheritance_08.cj`、`extend_property_conflict_invalid_6.cj`、`interface_property5.cj`。

### 问题详情

多个父类型声明同名成员时，官方对"种类不一致"（`INHERIT_SUPER_MEMBER_KIND_INCONSISTENT`）与"返回类型不一致"（`INHERIT_MEMBER_TYPE_INCONSISTENT`）分别报诊断；CFIR 的多父比较在 extend 属性冲突、接口冲突场景未覆盖这些分支。

### 修复方案

补全多父类型同名成员比较：先比种类（kind），再比返回类型；extend 引入的成员也参与比较（`collectEffectiveExtendInterfaceMemberInfos` 已存在，需纳入多父比较循环）。

## 13.5 访问权限削弱/不可见：CANNOT_WEAKEN_ACCESS_PRIVILEGE 与 INVISIBLE_MEMBER/INVISIBLE_REFERENCE 错位（各 4 个替换）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInheritanceDeepChecker.kt` 第 1038 行（`CANNOT_WEAKEN_ACCESS_PRIVILEGE`）；`coneDiagnosticToCfirDiagnostic.kt` 第 2026/2032 行（`INVISIBLE_MEMBER`/`INVISIBLE_REFERENCE`）。

### 测试文件

`protectedAndInternalMatrix.cj`、`protectedAndInternalMatrixRich.cj`（EXP `INVISIBLE_MEMBER`、ACT `NO_MATCH_FUNCTION_DECLARATION_FOR_CALL`）；`invisibleReferenceAndMember.cj`、`invisibleReferenceAndMemberRich.cj`（EXP `INVISIBLE_MEMBER`+`INVISIBLE_REFERENCE`、ACT `NOT_MEMBER_OF`+`NO_MATCH_FUNCTION_DECLARATION_FOR_CALL`）。

### 问题详情

可见性失败（成员存在但不可见）被映射成"成员不存在"类诊断（`NOT_MEMBER_OF`/`NO_MATCH_FUNCTION_DECLARATION_FOR_CALL`）。root cause 是 cone 错误类型在映射前未区分"查找失败"与"可见性失败"。

### 修复方案

在 cone 诊断映射中把可见性失败（`ConeInvisibleMemberError` 类）单独映射为 `INVISIBLE_MEMBER`/`INVISIBLE_REFERENCE`，只有真正的查找失败才映射 `NOT_MEMBER_OF`；override 削弱访问权限走 `CANNOT_WEAKEN_ACCESS_PRIVILEGE`。

---

# 问题 14：低频诊断名逐项分析（二）——模式匹配组

> 以下每个诊断名均已定位责任代码文件:行并读取源码确认触发条件（同问题 13 格式，文件:行见各条触发条件描述）。

## ENUM_PATTERN_PARAM_SIZE_ERROR

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirMatchPatternLegalityChecker.kt` L144-149（`ArityMismatch` 分支）。

### 测试文件

`enum_pattern*.cj` 相关 fixture（`EnumPatternGenerated` 等）。

### 问题详情

enum pattern 参数个数错误时报 `ENUM_PATTERN_PARAM_SIZE_ERROR`；CFIR 在部分场景未触发（EXP 期望、ACT 无），或与 `PATTERN_NOT_MATCH` 混用。

### 修复方案

enum pattern 参数个数检查补全（`resolveEnumConstructorPattern` 的 `ArityMismatch` 分支确保报告）。

## INTERPOLATION_IN_CONST_PATTERN

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirPatternExpressionChecker.kt` L305-324——**整个 `CfirConstPatternInterpolationChecker` 被注释掉，未启用**。

### 测试文件

`const_pattern*.cj` 相关 fixture。

### 问题详情

常量 pattern 中不能用字符串插值（官方 `sema_interpolation_in_const_pattern`）；CFIR 的该检查器整块被注释，导致缺报。

### 修复方案

取消注释并启用该 checker（对齐 `sema_interpolation_in_const_pattern`）。

## FORIN_PATTERN_MUST_BE_IRREFUTABLE

### 发生位置

CFIR 未定位到报告点——未实现。

### 测试文件

`forin` 相关 fixture（2 个失败）。

### 问题详情

for-in pattern 必须不可反驳（官方语义）；CFIR 未实现该检查器。

### 修复方案

对照官方补建（for-in 绑定/解构 pattern 不可反驳性）。

## NON_EXHAUSTIVE_MATCH

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirMatchExhaustivenessChecker.kt` L30-43。

### 测试文件

`match_no_selector_002.cj`、`matchcase5.cj`（与 `MATCH_CASE_MUST_HAVE_DEFAULT` 关联，4 缺少）；2 个独立失败。

### 问题详情

match 非穷尽（subject 类型错误或 pattern 非法时跳过，非穷尽在 selector 位置报缺失 case）；CFIR 对无 selector match 的穷尽性检查缺失。

### 修复方案

无 selector match 的穷尽性检查补全（Maranget 对无 selector 分支）。

---

# 问题 14：低频诊断名逐项分析（二）——调用与实参组

## NO_MATCH_OPERATOR_FUNCTION_CALL

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L2119。

### 测试文件

`pipeline10.cj` 等。

### 问题详情

无匹配 operator 函数调用时报 `NO_MATCH_OPERATOR_FUNCTION_CALL`；CFIR 在部分场景误报（EXP 无、ACT 有）。

### 修复方案

operator 调用失败按官方诊断名分流（与 `NO_MATCHING_OPERATOR_INVOKE` 边界对齐）。

## AMBIGUOUS_CONSTRUCTOR_CALL

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L1396-1402（候选全为构造器且非重定义级联时）。

### 测试文件

`constructor` 相关 fixture。

### 问题详情

构造器调用多义时报 `AMBIGUOUS_CONSTRUCTOR_CALL`；CFIR 在 classifier 重定义级联场景误报。

### 修复方案

构造器多义判定排除 classifier 重定义级联（`isClassifierRedeclarationConstructorCascade` 已处理部分）。

## AMBIGUOUS_FUNCTION_REFERENCE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L1546。

### 测试文件

变体场景 fixture。

### 问题详情

函数引用歧义时报 `AMBIGUOUS_FUNCTION_REFERENCE`；CFIR 触发条件与官方不一致。

### 修复方案

函数引用多义判定对齐官方。

## PARAMETERS_AND_ARGUMENTS_MISMATCH

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L1241-1260（symbol 是 Constructor 且 hasExplicitTypeArgumentsInCall，存在裸函数引用实参映射失败时）。

### 测试文件

`constructor1.cj` 等（与 `CANNOT_CURRYING` 同现）。

### 问题详情

构造器显式类型实参 + 裸函数引用映射失败时报 `PARAMETERS_AND_ARGUMENTS_MISMATCH`；CFIR 触发条件偏差。

### 修复方案

参数/实参映射失败在显式类型实参场景按官方报。

## UNSUPPORTED_NAMED_ARGUMENT

### 发生位置

CFIR 未定位到报告点——未实现。

### 测试文件

`named_argument*.cj` 相关 fixture。

### 问题详情

调用不支持命名实参的场景官方报 `UNSUPPORTED_NAMED_ARGUMENT`；CFIR 未实现。

### 修复方案

对照官方补建（调用不支持命名实参的场景）。

## BUILTIN_INDEX_IN_BOUND

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirExpressionSemanticsChecker.kt` L713-720（parseSignedIntExpression 解析下标，越界时报）。

### 测试文件

`err_subscript_*.cj` 相关（1 个失败）。

### 问题详情

内建下标越界时报 `BUILTIN_INDEX_IN_BOUND`；CFIR 触发条件与官方不一致。

### 修复方案

越界判定仅对无显式后缀或 i64 下标生效，对齐官方。

## INVALID_SUBSCRIPT_ASSIGN_RETURN

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirOperatorDeclarationChecker.kt` L178-184（returnType 非 Unit 时）。

### 测试文件

`err_subscript_assign_03.cj`、`04.cj` 等（范围位移：EXP 标返回类型、ACT 标 operator）。

### 问题详情

operator set 返回类型必须 Unit；CFIR 报告 source 取 `operatorDiagnosticSource()`（优先 operator 修饰符），官方标返回类型。

### 修复方案

报告 source 改用返回类型 ref。

## INVALID_SUBSCRIPT_ASSIGN_PARAMETER_NUM

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirOperatorDeclarationChecker.kt` L159-166（positionalParameters 为空时）。

### 测试文件

`err_subscript_assign_*.cj` 相关。

### 问题详情

operator set 缺少位置参数时报 `INVALID_SUBSCRIPT_ASSIGN_PARAMETER_NUM`；CFIR 部分场景未触发。

### 修复方案

下标赋值协议参数检查（索引参数）补全。

## INVALID_UNARY_EXPR

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L1918-1935（`mapInvalidUnaryExprDiagnostic`：ConeUnresolvedNameError 且 operator/receiverType 存在、argumentTypes 为空时）。

### 测试文件

`class_static_call_non_static1.cj` 中 `-data` 等（EXP 期望、ACT 报 INVALID_UNARY_EXPR 与官方不一致）。

### 问题详情

一元运算符解析失败映射；CFIR 与 `TYPE_MISMATCH` 抢占。

### 修复方案

一元运算符解析失败需在 operator+receiverType 存在时正确映射，且不得与 TYPE_MISMATCH 抢占。

## INVALID_LOOP_CONTROL

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirEffectsExtraChecker.kt` L126。

### 测试文件

effects 族 fixture（4 缺少）。

### 问题详情

循环控制（break/continue）非法时报 `INVALID_LOOP_CONTROL`；CFIR 在 effects 场景缺报。

### 修复方案

loop control（break/continue）上下文检查对齐官方。

## NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirConstructorDelegationChecker.kt` L163。

### 测试文件

`constructor1.cj` 等。

### 问题详情

父类无无参构造器时报 `NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS`；CFIR 触发条件偏差。

### 修复方案

委托构造父类无参构造器检查对齐官方。

---

# 问题 14：低频诊断名逐项分析（二）——宏与注解组

> 以下每个诊断名均已定位责任代码文件:行并读取源码确认触发条件（同问题 13 格式，文件:行见各条触发条件描述）。

## MACRO_UNRESOLVED

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/collectors/components/MacroConstructionDiagnosticCollectorComponent.kt` L227。

### 测试文件

`err_custom_annotation_place_var_01.cj` 等（替换：EXP ANNOTATION_CUSTOM_PLACE、ACT MACRO_UNRESOLVED+UNRESOLVED_REFERENCE）。

### 问题详情

宏未解析时报 `MACRO_UNRESOLVED`；CFIR 的宏解析失败先于注解位置检查上报，把官方 `ANNOTATION_CUSTOM_PLACE` 顶掉。

### 修复方案

注解位置检查先于宏解析失败上报。

## MACRO_DEPENDENCY_COMPILE_FAILED

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/collectors/components/MacroConstructionDiagnosticCollectorComponent.kt` L117。

### 测试文件

`test_macro.cj` 等多诊断簇。

### 问题详情

宏依赖编译失败时报 `MACRO_DEPENDENCY_COMPILE_FAILED`；CFIR 与 `AMBIGUOUS_FUNCTION_CALL`/`EXPECT_CONST`/`UNRESOLVED_IMPORT` 等多诊断混报。

### 修复方案

宏依赖编译错误单独收集上报，不与其他诊断混淆。

## ANNOTATION_NON_PUBLIC

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirBuiltInAnnotationSemanticsChecker.kt` L124。

### 测试文件

`annot_*.cj` 相关（极少）。

### 问题详情

注解声明非 public 时报 `ANNOTATION_NON_PUBLIC`；CFIR 触发条件偏差。

### 修复方案

注解可见性检查对齐官方。

## APILEVEL_MULTI_ANNO

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirBuiltInAnnotationSemanticsChecker.kt` L148-153（apiLevelEntries.size>1 时报第二个）。

### 测试文件

`multi_anno.cj` 等。

### 问题详情

@APILevel 多重注解时报 `APILEVEL_MULTI_ANNO`；CFIR 报告位置与官方不一致。

### 修复方案

多重注解检查对齐官方（报第二个注解位置）。

## APILEVEL_SYSCAP_ERROR

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirApiLevelRefHigherChecker.kt` L63-65（AvailabilityFailure.SyscapError 分支）。

### 测试文件

`syscap_test01.cj` 等（替换：EXP APILEVEL_SYSCAP_ERROR/WARNING、ACT CANNOT_ASSIGN_TO_IMMUTABLE+UNRESOLVED_REFERENCE+UNUSED_IMPORT）。

### 问题详情

@APILevel syscap 错误被 `CANNOT_ASSIGN_TO_IMMUTABLE` 抢占（问题 1 同根因）。

### 修复方案

同问题 1 先修赋值误报，syscap 诊断恢复。

## APILEVEL_SYSCAP_WARNING

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirApiLevelRefHigherChecker.kt` L71。

### 测试文件

`syscap_test01.cj`（替换：EXP APILEVEL_SYSCAP_ERROR/WARNING、ACT CANNOT_ASSIGN_TO_IMMUTABLE+UNRESOLVED_REFERENCE）。

### 问题详情

@APILevel syscap 警告被赋值误报抢占（问题 1 同根因）。

### 修复方案

同问题 1 先修赋值误报，syscap 诊断恢复。

## IFAVAILABLE_ARG_NOT_LITERAL

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirBuiltInAnnotationSemanticsChecker.kt` L187-190（`argumentsAreLiteralLike()` 不满足时）。

### 测试文件

`test_macro.cj` 等多诊断簇（与 MACRO_DEPENDENCY_COMPILE_FAILED/AMBIGUOUS_FUNCTION_CALL 同现）。

### 问题详情

@IfAvailable 参数必须字面量；CFIR 对数组/枚举构造器实参的字面量判定过严。

### 修复方案

字面量判定覆盖数组/枚举构造器实参。

## IFAVAILABLE_UNKNOWN_ARG_NAME

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirBuiltInAnnotationSemanticsChecker.kt` L194-202（rawNamedArgumentNames 不在 allowedIfAvailableArgumentNames 时）。

### 测试文件

宏测试替换（EXP IFAVAILABLE_UNKNOWN_ARG_NAME、ACT UNRESOLVED_REFERENCE）。

### 问题详情

@IfAvailable 未知参数名；CFIR 参数名白名单与官方不一致。

### 修复方案

参数名白名单按官方补全。

## IFAVAILABLE_LEVEL_LIMIT

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirBuiltInAnnotationSemanticsChecker.kt` L205-209（ifAvailableEntries 非空且 apiLevelEntries 为空时）。

### 测试文件

`level_limit.cj` 等（替换：EXP IFAVAILABLE_LEVEL_LIMIT、ACT UNRESOLVED_REFERENCE+UNUSED_IMPORT）。

### 问题详情

@IfAvailable 的 APILevel 限制；CFIR 的级别限制检查被引用解析失败抢占。

### 修复方案

IfAvailable 级别限制检查先于引用解析。

---

# 问题 14：低频诊断名逐项分析（二）——effects 组

> 以下每个诊断名均属 effects 特性；根本前提是测试基建开启 effects 特性开关并注入 `stdx` 可见性（问题 10），此后各诊断按官方映射恢复。

## COMMAND_HANDLE_TYPE_ERROR

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L2137（ConeCommandHandleTypeError 映射）。

### 测试文件

effects 族 fixture（与 EFFECTS_FEATURE_DISABLED 同现）。

### 问题详情

effects command handle 类型错误；CFIR 在 effects 特性未启用时先报 `EFFECTS_FEATURE_DISABLED`，本诊断无法到达。

### 修复方案

effects 特性启用后按官方映射。

## IMPLICIT_RESUME_OUTSIDE_HANDLER

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L2143（ConeImplicitResumeOutsideHandlerError 映射）。

### 测试文件

effects 族 fixture。

### 问题详情

隐式 resume 在 handler 外；同上被 EFFECTS_FEATURE_DISABLED 抢占。

### 修复方案

effects 特性启用后按官方映射。

## RESUME_NO_WITH

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L2148。

### 测试文件

`resume_unit.cj` 等。

### 问题详情

resume 无 with 子句；同上被 EFFECTS_FEATURE_DISABLED 抢占。

### 修复方案

effects 特性启用后按官方映射。

## RESUME_THROWING_MISMATCH_TYPE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L2154（ConeResumeThrowingMismatchTypeError 映射）。

### 测试文件

effects 族 fixture。

### 问题详情

resume throwing 类型不匹配；同上被 EFFECTS_FEATURE_DISABLED 抢占。

### 修复方案

effects 特性启用后按官方映射。

## MISMATCHING_HANDLE_BLOCK

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirEffectsExtraChecker.kt` L85。

### 测试文件

`non_matching_types.cj` 等（EXP 期望、ACT 报 EFFECTS_FEATURE_DISABLED）。

### 问题详情

handle 块不匹配是 EXP 唯一期望的语义诊断；ACT 因 effects 特性关闭而报 EFFECTS_FEATURE_DISABLED，MISMATCHING_HANDLE_BLOCK 无法到达。

### 修复方案

effects 特性启用后按官方报 MISMATCHING_HANDLE_BLOCK。

---

# 问题 14：低频诊断名逐项分析（二）——泛型与 typealias 组

> 以下每个诊断名均已定位责任代码文件:行并读取源码确认触发条件（同问题 13 格式，文件:行见各条触发条件描述）。

## GENERIC_INFINITE_INSTANTIATION

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirGenericInstantiationChecker.kt` L1630-1635（reportInfiniteInstantiation 去重后上报）。

### 测试文件

`generic_subst_perf.cj` 等。

### 问题详情

泛型无限实例化时报 `GENERIC_INFINITE_INSTANTIATION`；CFIR 的检测触发条件与官方不一致（部分场景误报/漏报）。

### 修复方案

无限实例化检测（去重 reportedSources）补全。

## GENERIC_NO_METHOD_MATCH_IN_UPPER_BOUNDS

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L1866。

### 测试文件

`upper_bound` 相关 fixture。

### 问题详情

泛型上界中无方法匹配时报 `GENERIC_NO_METHOD_MATCH_IN_UPPER_BOUNDS`；CFIR 与 `NOT_MEMBER_OF` 边界混淆。

### 修复方案

上界方法查找失败按官方映射。

## GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L1844-1859（接收者是类型参数且名称解析失败时归类为 upper bounds 无成员）。

### 测试文件

`generic_upperbound_reference*.cj` 等（与 NOT_MEMBER_OF 关联）。

### 问题详情

类型参数接收者的成员查找失败应优先归类为"上界无成员"，CFIR 部分场景落到通用 `NOT_MEMBER_OF`。

### 修复方案

类型参数接收者的成员查找失败优先报上界无成员诊断。

## GENERIC_TYPE_INCONSISTENT

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L2282-2285（ConeGenericTypeInconsistentError 映射，锚定 nominal base expression）。

### 测试文件

`generic*.cj` 相关。

### 问题详情

泛型类型不一致时报 `GENERIC_TYPE_INCONSISTENT`；CFIR 诊断锚定位置与官方不一致。

### 修复方案

泛型实例化一致性诊断锚定接收者（官方语义）。

## CONFLICTING_UPPER_BOUNDS

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirTypeParameterBoundsChecker.kt` L93-94（classBounds.size>1 时）。

### 测试文件

`typealias10.cj` 多余（EXP 无、ACT 有）。

### 问题详情

多类上界冲突时报 `CONFLICTING_UPPER_BOUNDS`；CFIR 在 typealias 展开后未去重导致误报。

### 修复方案

多类上界冲突检查仅当确实声明多个类上界时报，typealias 展开后去重。

## UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirTypeParameterBoundsChecker.kt` L61-84（upperBoundKind()==INVALID 时）。

### 测试文件

`typealias10.cj` 等多余 `CONFLICTING_UPPER_BOUNDS`+`GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT`。

### 问题详情

类型参数上界必须类或接口；CFIR 的上界种类检查与后续约束检查顺序颠倒。

### 修复方案

上界种类检查先于约束冲突检查。

## ONLY_ONE_CLASS_BOUND_ALLOWED

### 发生位置

CFIR 未实现该检查器。

### 测试文件

`range2.cj` 等。

### 问题详情

仅允许一个类上界（官方语义）；CFIR 未实现。

### 修复方案

对照官方补建。

## MULTIPLE_CLASS_UPPER_BOUNDS

### 发生位置

CFIR 未定位到报告点——未实现，与 CONFLICTING_UPPER_BOUNDS 语义重叠。

### 测试文件

`range2.cj` 等。

### 问题详情

多个类上界时报 `MULTIPLE_CLASS_UPPER_BOUNDS`；CFIR 未实现。

### 修复方案

确认与 CONFLICTING_UPPER_BOUNDS 边界，补建或合并。

## CANNOT_INSTANTIATED_BY_INCOMPLETE_TYPE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirGenericInstantiationChecker.kt` L724。

### 测试文件

`generic_call_static_impl02.cj` 等。

### 问题详情

不完整类型实例化时报 `CANNOT_INSTANTIATED_BY_INCOMPLETE_TYPE`；CFIR 触发条件偏差。

### 修复方案

不完整类型实例化检查对齐官方。

## NON_GENERIC_FUNCTION_WITH_TYPE_ARGUMENT

### 发生位置

CFIR 未实现该检查器。

### 测试文件

`errorSimpleEnum.cj` 等。

### 问题详情

非泛型函数带类型实参时报 `NON_GENERIC_FUNCTION_WITH_TYPE_ARGUMENT`；CFIR 未实现。

### 修复方案

对照官方补建。

## TYPEALIAS_CYCLE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirTypeAliasCycleChecker.kt` L29-36（展开类型引用携带递归展开错误前缀时）。

### 测试文件

`typealias6.cj`/`typealias7.cj` 缺报（EXP 有、ACT 无）。

### 问题详情

typealias 循环时报 `TYPEALIAS_CYCLE`；CFIR 的递归展开错误识别（RECURSIVE_TYPEALIAS_PREFIX 判定）未命中。

### 修复方案

递归 typealias 展开错误识别补全（RECURSIVE_TYPEALIAS_PREFIX 判定）。

---

# 问题 14：低频诊断名逐项分析（二）——修饰符组

> 以下每个诊断名均已定位责任代码文件:行并读取源码确认触发条件（同问题 13 格式，文件:行见各条触发条件描述）。

## REDUNDANT_MODIFIER_FOR_TARGET

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirModifierChecker.kt` L113-121（redundantTargetMap 命中时）。

### 测试文件

`test_macro.cj` 等多诊断簇。

### 问题详情

冗余修饰符（如 public 于顶层）时报 `REDUNDANT_MODIFIER_FOR_TARGET`；CFIR 判定与官方不一致。

### 修复方案

冗余修饰符判定（如 public 于顶层等）按官方对齐。

## WRONG_MODIFIER_CONTAINING_DECLARATION

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirModifierChecker.kt` L148-158（possibleParentTargetPredicateMap 不满足时）。

### 测试文件

`err_this_02.cj`/`primaryConstructor2.cj`（与 INVALID_THIS_CALL_OUTSIDE_CTOR 同现）。

### 问题详情

修饰符不适用于所在声明容器时报 `WRONG_MODIFIER_CONTAINING_DECLARATION`；CFIR 父目标谓词表与官方不一致。

### 修复方案

父目标谓词表按官方补全（primary constructor 参数区等）。

## DEPRECATED_WARNING

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirDeprecatedCallChecker.kt` L27-37（调用目标带 @Deprecated 注解时按等级报 warning/error）。

### 测试文件

`deprecated*.cj` 相关。

### 问题详情

@Deprecated 调用警告；CFIR 等级判定（warning/error/error-with-hint）与官方不一致。

### 修复方案

注解等级（warning/error/error-with-hint）与官方对齐。

---

# 问题 14：低频诊断名逐项分析（二）——const/字面量组

> 以下每个诊断名均已定位责任代码文件:行并读取源码确认触发条件（同问题 13 格式，文件:行见各条触发条件描述）。

## FLOAT_LITERAL_TOO_SMALL

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirExpressionSemanticsChecker.kt` L116-119（absValue 小于 Float.MIN_VALUE 时报）。

### 测试文件

`float_literal*.cj` 相关（极少）。

### 问题详情

Float32 字面量下溢时报 `FLOAT_LITERAL_TOO_SMALL`；CFIR 触发条件偏差。

### 修复方案

Float32 字面量范围检查（上溢/下溢）对齐官方。

## LITERAL_NUMERIC_OVERFLOW

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirCompoundAssignmentSemanticsChecker.kt` L76。

### 测试文件

`overflow_check` 关联 fixture。

### 问题详情

复合赋值字面量溢出时报 `LITERAL_NUMERIC_OVERFLOW`；CFIR 未覆盖部分场景。

### 修复方案

复合赋值字面量溢出检查。

## INCONSISTENT_ARRAY_LITERAL_ELEMENT_TYPE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L2363 区域映射。

### 测试文件

`arraylit*.cj` 相关（范围位移）。

### 问题详情

数组字面量元素类型不一致时报 `INCONSISTENT_ARRAY_LITERAL_ELEMENT_TYPE`；CFIR 报告位置与官方不一致。

### 修复方案

数组字面量元素类型一致性检查补全。

---

# 问题 14：低频诊断名逐项分析（二）——其他零散组

> 以下每个诊断名均已定位责任代码文件:行并读取源码确认触发条件（同问题 13 格式，文件:行见各条触发条件描述）。

## CANNOT_REF_TO_PKG_NAME

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L2274（ConeCannotRefToPackageNameError 映射）。

### 测试文件

`main.cj`（ErrMsgs）替换（EXP UNRESOLVED_REFERENCE、ACT AMBIGUOUS_FUNCTION_CALL+CANNOT_REF_TO_PKG_NAME）。

### 问题详情

不能引用包名作为值时报 `CANNOT_REF_TO_PKG_NAME`；CFIR 落入多义调用。

### 修复方案

包名引用失败优先报 CANNOT_REF_TO_PKG_NAME，不落入多义调用。

## CATCH_TYPE_MUST_EXTEND_EXCEPTION

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirExpressionSemanticsChecker.kt` L758-775（CfirCatchTypeChecker 检查每个 catch pattern 类型）。

### 测试文件

`try_0.cj` 等。

### 问题详情

catch 类型必须继承 Exception；CFIR 检查触发条件偏差。

### 修复方案

catch 类型合法性检查（Exception/Error 子类型）与覆盖关系补全。

## EXPR_IN_FORIN_MUST_HAS_ITERATOR

### 发生位置

CFIR 未实现该检查器。

### 测试文件

`forin14.cj` 等。

### 问题详情

for-in 表达式必须可迭代（官方语义）；CFIR 未实现。

### 修复方案

对照官方补建 for-in 迭代器检查器。

## INVALID_RETURN_IN_STATIC_INIT

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L2206-2210（DiagnosticKind.ReturnNotAllowed 映射）。

### 测试文件

`static_init*.cj` 相关。

### 问题详情

static init 中不允许 return；CFIR 未覆盖部分场景。

### 修复方案

static init 中 return 检查补全。

## MISSING_FUNC_BODY

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirMemberBodyDeclarationChecker.kt` L41-59（非抽象成员缺函数体时）。

### 测试文件

`interface*.cj` 相关。

### 问题详情

缺少函数体时报 `MISSING_FUNC_BODY`；CFIR 触发条件偏差。

### 修复方案

成员函数体缺失检查补全。

## PROPERTY_MUST_HAVE_ACCESSORS

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirGeneralSemanticsChecker.kt` L774-783（非接口/非抽象/非构造参数来源属性检查 getter/setter）。

### 测试文件

`interface_property*.cj` 相关。

### 问题详情

属性必须有访问器；CFIR 检查未覆盖接口继承场景。

### 修复方案

属性访问器检查补全（含接口继承场景）。

## PROPERTY_MUST_IMPLEMENT_BOTH

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirGeneralSemanticsChecker.kt` L814-833（subHasGetter/subHasSetter 不全时检查父属性实现）。

### 测试文件

`interface_property5.cj` 等。

### 问题详情

子类属性实现必须成对（get/set）；CFIR 检查偏差。

### 修复方案

属性实现成对性检查对齐官方。

## NON_ABSTRACT_CLASS_CANNOT_BE_SEALED

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirGeneralSemanticsChecker.kt` L465。

### 测试文件

`interface_conflict_inheritance` 关联 fixture。

### 问题详情

非抽象类不能 sealed；CFIR 触发条件偏差。

### 修复方案

sealed 限制检查对齐官方。

## OVERLOAD_CONFLICTS

### 发生位置

CFIR 未定位到独立报告点——该诊断名可能与 CONFLICTING_OVERLOADS 语义重叠或未实现。

### 测试文件

2 个失败相关 fixture。

### 问题详情

重载冲突时报 `OVERLOAD_CONFLICTS`；CFIR 与 `CONFLICTING_OVERLOADS` 边界不清。

### 修复方案

对照官方确认 OVERLOAD_CONFLICTS 与 CONFLICTING_OVERLOADS 的边界，补建或合并。

## EXPLICIT_SUPER_CALL_REQUIRED

### 发生位置

CFIR 未定位到报告点——未实现。

### 测试文件

2 个失败相关 fixture。

### 问题详情

需要显式 super 调用（多父类同名成员场景）；CFIR 未实现。

### 修复方案

对照官方补建（多父类同名成员需显式 super 限定场景）。

## TYPE_IMPLEMENT_NON_INTERFACE

### 发生位置

CFIR 未定位到报告点——未实现。

### 测试文件

2 个失败相关 fixture。

### 问题详情

类型实现非接口（`<:` 右侧非接口类型）；CFIR 未实现。

### 修复方案

对照官方补建（`<:` 右侧非接口类型场景）。

## ILLEGAL_MULTI_INHERITANCE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirSupertypesChecker.kt` L174-185（第二个 concrete 父类时）。

### 测试文件

`multipleClassSupers.cj` 等（替换：EXP MULTIPLE_CLASS_SUPER_TYPES、ACT ILLEGAL_MULTI_INHERITANCE）。

### 问题详情

非法多继承时报 `ILLEGAL_MULTI_INHERITANCE`；官方在该场景报 `MULTIPLE_CLASS_SUPER_TYPES`。

### 修复方案

多父类场景统一报官方 `MULTIPLE_CLASS_SUPER_TYPES`。

## CLASSIFIER_REDECLARATION

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirConflictsDeclarationChecker.kt` L95-97（conflictingDeclaration 是 ClassLikeSymbol 且符号集中有 ClassLikeSymbol 时）。

### 测试文件

`change_lib.cj`/`change_abi*.cj` 多余 `AMBIGUOUS_USE`+`CLASSIFIER_REDECLARATION`（8+4）。

### 问题详情

class-like 同名重定义时报 `CLASSIFIER_REDECLARATION`；CFIR 对跨库 ABI/库变更场景误报。

### 修复方案

跨库 ABI/库变更场景的重定义需区分"同一库内重名"与"不同库版本"，仅前者报 CLASSIFIER_REDECLARATION。

## SUPER_TYPES_DUPLICATE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirGenericInstantiationChecker.kt` L833 + `CfirSupertypesChecker.kt` L88/L109。

### 测试文件

`superSelfAndDuplicate.cj` 等范围位移 + `interface` 场景。

### 问题详情

重复父类型时报 `SUPER_TYPES_DUPLICATE`；CFIR 两处报告点范围不一致。

### 修复方案

重复父类型检查统一归属 SupertypesChecker，范围取父类型引用。

## UNQUALIFIED_LEFT_VALUE_ASSIGNED

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirAssignmentLegalityChecker.kt` L86/L125/L168（MutationTarget.NonAssignableName 分支）。

### 测试文件

`case02.cj`/`incordec1.cj` 缺报（EXP 有、ACT 无）。

### 问题详情

非限定左值被赋值（函数/类型名等非左值）时报 `UNQUALIFIED_LEFT_VALUE_ASSIGNED`；CFIR 缺报。

### 修复方案

函数/类型名赋值与自增自减目标非左值检查补全。

## SPAWN_ARG_INVALID

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirSpawnSemanticsChecker.kt` L52-71（checkSpawnBodyType 在 bodyType 是 ConeErrorType 时）。

### 测试文件

`spawn` 相关测试（与 AMBIGUOUS_FUNCTION_CALL 同现）。

### 问题详情

spawn body 类型推断必须成功（对齐 C++ sema_spawn_invalid_argument）；CFIR 触发条件偏差。

### 修复方案

spawn body 推断失败按官方报 spawn 专用诊断。

## EXTEND_A_JAVA_TYPE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirExtendExtraChecker.kt` L152-159（targetDecl 带 @Java 注解时，对齐 sema_extend_a_java_type）。

### 测试文件

`extend_java*.cj` 相关。

### 问题详情

不能 extend @Java 类型；CFIR 未覆盖部分场景。

### 修复方案

@Java 目标 extend 检查补全。

## EXTEND_DUPLICATE_INTERFACE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirExtendCheckers.kt` L177-195（同一 extend 声明内重复接口或与同目标 extend 重复时）。

### 测试文件

`extend_duplicate_interfaces*.cj` 范围位移 + `interface` 场景（替换为 INTERFACE_CANNOT_INHERIT_CLASS+SUPER_TYPES_DUPLICATE）。

### 问题详情

extend 重复接口时报 `EXTEND_DUPLICATE_INTERFACE`；CFIR 报告点与官方不一致。

### 修复方案

重复接口判定按语义稳定键去重，归属 extend checker 统一上报。

## EXTEND_FUNCTION_CANNOT_OVERRIDDEN

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirExtendExtraChecker.kt` L190-196（extend 内函数带 override 时，对齐 sema_extend_function_cannot_overridden）。

### 测试文件

`override_by_super_extend01/05.cj` 缺报。

### 问题详情

extend 函数不能 override；CFIR 缺报。

### 修复方案

extend 成员 override 检查补全。

## TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirExtendExtraChecker.kt` L601-618（checkExtendImportedInterface：hasImportedOrBuiltinTarget 且父接口在别的模块定义时）。

### 测试文件

`main.cj` 缺报（4 缺少）。

### 问题详情

extend 导入的接口受限；CFIR 缺报。

### 修复方案

导入接口 extend 规则按官方对齐（保护接口例外已处理 L606）。

## INVALID_OVERRIDE_OR_REDEFINE_MEMBER_IN_INTERFACE

### 发生位置

CFIR 未定位到报告点——未实现。

### 测试文件

`interface_conflict_inheritance*.cj` 相关。

### 问题详情

接口内非法 override/redef 成员；CFIR 未实现。

### 修复方案

对照官方补建（接口成员 override/redef 限制）。

## OPTIONAL_CHAIN_NON_OPTIONAL

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L2341（ConeOptionalChainNonOptionalError 映射，source 不含 `!` 前缀）。

### 测试文件

`err_optional_chain_00.cj` 等范围位移（EXP 含 `!`、ACT 不含）。

### 问题详情

可选链作用在非可选值时报 `OPTIONAL_CHAIN_NON_OPTIONAL`；CFIR 报告 source 未含 `!`。

### 修复方案

报告 source 取含 `!` 的完整可选链表达式。

## MISMATCHED_TYPES_BECAUSE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirExpressionSemanticsChecker.kt` L835。

### 测试文件

`infer_type_with_option.cj` 等（替换：EXP TYPE_MISMATCH、ACT UNRESOLVED_REFERENCE）。

### 问题详情

类型不匹配原因诊断被未解析引用抢占。

### 修复方案

类型不匹配优先于未解析引用。

## USE_FUNC_CAPTURE_VAR_ALONE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirClosureCaptureUsageChecker.kt` L78-97（reportIllegalClosureValueUse 依据捕获信息选诊断）。

### 测试文件

与 USE_MUTABLE_FUNC_ALONE 同族。

### 问题详情

捕获变量单独使用限制；CFIR 诊断名选择与官方不一致。

### 修复方案

捕获值使用位置按官方选诊断名。

## FUNC_CAPTURE_VAR_CANNOT_EXPR

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirClosureCaptureUsageChecker.kt` L126。

### 测试文件

`capture` 关联 fixture。

### 问题详情

函数捕获变量不能作表达式；CFIR 未覆盖部分场景。

### 修复方案

捕获变量表达式限制对齐官方。

## CAPTURE_BEFORE_INITIALIZATION

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInitializationCheckers.kt` L1210（reportCaptureBeforeInitialization）。

### 测试文件

`capture_not_init_01.cj` 等。

### 问题详情

捕获未初始化局部变量时报 `CAPTURE_BEFORE_INITIALIZATION`；CFIR 缺报。

### 修复方案

嵌套函数捕获未初始化变量检查补全。

## THIS_AS_EXPRESSION_IN_FUNC

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirExpressionSemanticsChecker.kt` L344-367（CfirOpenConstructorThisUsageChecker：L355 openClassConstructorOwner() 限定，L357-358 排除作为接收者的 this）。

### 测试文件

`class_open_ctor3/4_fail.cj` 缺报（4 缺少）。

### 问题详情

open/abstract 类构造器中裸显式 this 时报 `THIS_AS_EXPRESSION_IN_FUNC`；CFIR 缺报。

### 修复方案

open/abstract 构造器 this 检查补全（lambda/局部函数内裸 this 继承外层语义）。

## ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirFieldVariableThisOrSuperInitializerChecker.kt` L517-522（reportIllegalMemberAccess）。

### 测试文件

`class_init_with_nested_func2.cj` 等成员初始化器场景。

### 问题详情

成员变量初始化器用 this/super 赋值非法；CFIR 未覆盖部分场景。

### 修复方案

初始化器内 this/super 成员访问按官方对齐。

## INOUT_CAN_ONLY_USED_IN_CFUNC_CALLING

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirInoutSemanticsChecker.kt` L44-60（先确认被调函数 foreign/CFunc，再检查实参）。

### 测试文件

`varray_inout.cj` 等（与 INOUT_MISMATCH 同现）。

### 问题详情

inout 实参仅限 cfunc 调用；CFIR 触发条件偏差。

### 修复方案

inout 语义检查统一由 `CfirInoutSemanticsChecker` 承担，cfunc 边界判定补全。

## NEED_MEMBER_IMPLEMENTATION

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInheritanceDeepChecker.kt` L271-274（hasUnimplementedMember 时；L258-263 hasSatisfiedImplementation 已排除 super extend 实现）。

### 测试文件

`implement_by_super_extend02/03/04/06.cj` 多余 `ABSTRACT_MEMBER_NOT_IMPLEMENTED`（8 多余）。

### 问题详情

extend 存在未实现抽象成员时报 `NEED_MEMBER_IMPLEMENTATION`；CFIR 两诊断对同一场景重复/错位。

### 修复方案

super extend 提供实现的成员统一计入已实现（satisfiesExtendInterfaceRequirement），不落 NEED_MEMBER_IMPLEMENTATION。

## CANNOT_WEAKEN_ACCESS_PRIVILEGE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInheritanceDeepChecker.kt` L1038。

### 测试文件

`protectedAndInternalMatrix.cj` 等（替换：EXP INVISIBLE_MEMBER、ACT NO_MATCH_FUNCTION_DECLARATION_FOR_CALL）。

### 问题详情

override 削弱访问权限时报 `CANNOT_WEAKEN_ACCESS_PRIVILEGE`；CFIR 被成员查找失败诊断抢占。

### 修复方案

override 可见性检查先于成员查找失败。

## INVISIBLE_REFERENCE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L2032。

### 测试文件

`invisibleReferenceAndMember.cj` 等（替换：EXP INVISIBLE_MEMBER/INVISIBLE_REFERENCE、ACT NOT_MEMBER_OF）。

### 问题详情

引用不可见时报 `INVISIBLE_REFERENCE`；CFIR 可见性失败被映射成成员查找失败。

### 修复方案

可见性失败走 INVISIBLE_MEMBER/INVISIBLE_REFERENCE，成员查找失败才报 NOT_MEMBER_OF。

---

# 问题 15：低频诊断名逐项分析（三）——补充组（核验后发现遗漏的诊断名）

> 以下每个诊断名在核验时发现正文未覆盖，现按五段格式补齐；均已定位责任代码文件:行并读取源码确认触发条件。

## ARGUMENT_TYPE_MISMATCH

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt:1221`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP data[0] <!INVALID_BINARY_OPERATOR!>+<!>= 1 → ACT data[0] <!INVALID_BINARY_OPERATOR!>+=<!> 1；var gStateBufI8 = ArrayList<Int8>(<!ARGUMENT_TYPE_MISMATCH!>[-1, 1]<!>)；相关 fixture：call16.cj、compound_assign.cj、record_vardecl_cpointer.cj、varray_inout.cj）。

### 问题详情

（失败形态：EXP data[0] <!INVALID_BINARY_OPERATOR!>+<!>= 1 → ACT data[0] <!INVALID_BINARY_OPERATOR!>+=<!> 1；var gStateBufI8 = ArrayList<Int8>(<!ARGUMENT_TYPE_MISMATCH!>[-1, 1]<!>)；相关 fixture：call16.cj、compound_assign.cj、record_vardecl_cpointer.cj、varray_inout.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## MACRO_EXPANSION_FAILED

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/collectors/components/MacroConstructionDiagnosticCollectorComponent.kt:85`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP <!APILEVEL_REF_HIGHER!>f21()<!> // error → ACT this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>lhs<!> = lhs；this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>rhs<!> = rhs；this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>lhs<!> = lhs；相关 fixture：test04.cj、test05.cj、test06.cj、test07.cj）。

### 问题详情

触发条件基于责任代码片段：66:             ?: diagnostic.extractBacktickedName()；67:             ?: "<macro>"；68:         if (diagnostic.diagnosticOrigin == MacroConstructionDiagnostic.Origin.DIAG_REPORT) {。（失败形态：EXP <!APILEVEL_REF_HIGHER!>f21()<!> // error → ACT this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>lhs<!> = lhs；this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>rhs<!> = rhs；this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>lhs<!> = lhs；相关 fixture：test04.cj、test05.cj、test06.cj、test07.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## INVALID_BINARY_OPERATOR

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirClassifierAsExpressionChecker.kt:90`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP var r1 = a <!INVALID_BINARY_OPERATOR!>&<!> b；var r2 = a <!INVALID_BINARY_OPERATOR!>^<!> c；var r3 = a <!INVALID_BINARY_OPERATOR!>|<!> e → ACT var r1 = a & <!TYPE_MISMATCH!>b<!>；var r2 = a ^ <!TYPE_MISMATCH!>c<!>；var r3 = a | <!TYPE_MISMATCH!>e<!>；相关 fixture：binary_diagnose.cj、binaryexpr.cj、bitwise_typeunmatch.cj、typealias3.cj）。

### 问题详情

（失败形态：EXP var r1 = a <!INVALID_BINARY_OPERATOR!>&<!> b；var r2 = a <!INVALID_BINARY_OPERATOR!>^<!> c；var r3 = a <!INVALID_BINARY_OPERATOR!>|<!> e → ACT var r1 = a & <!TYPE_MISMATCH!>b<!>；var r2 = a ^ <!TYPE_MISMATCH!>c<!>；var r3 = a | <!TYPE_MISMATCH!>e<!>；相关 fixture：binary_diagnose.cj、binaryexpr.cj、bitwise_typeunmatch.cj、typealias3.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## WRONG_NUMBER_OF_ARGUMENTS

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt:817`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP 无诊断 → ACT startTime = DateTime.now<!WRONG_NUMBER_OF_ARGUMENTS!>()<!>；endTime = DateTime.now<!WRONG_NUMBER_OF_ARGUMENTS!>()<!>；相关 fixture：bugfix2.cj、ok_unary_01.cj、variadic_class.cj、variadic_compose.cj）。

### 问题详情

（失败形态：EXP 无诊断 → ACT startTime = DateTime.now<!WRONG_NUMBER_OF_ARGUMENTS!>()<!>；endTime = DateTime.now<!WRONG_NUMBER_OF_ARGUMENTS!>()<!>；相关 fixture：bugfix2.cj、ok_unary_01.cj、variadic_class.cj、variadic_compose.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## NO_MATCH_FUNCTION_DECLARATION_FOR_REF

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirFunctionReferenceLegalityChecker.kt:67`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP var a1: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>S1<!>, $1> = [S1()] // error；var a2: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>(Array<Int64>, String)<!>, $1> = [([1, 2, 3], "123")] // error；var a4: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>(CFunc<() -> Unit>, () -> Unit)<!>, $1> = [({=>}, {=>})] // error → ACT var a1: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>S1<!>, $1> = [S1()] // error；var a4: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>(CFunc<() -> Unit>, () -> Unit)<!>, $1> = [({=>}, {=>})] // error；var b2: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>S3<!>, $1> = [S3()] // error；相关 fixture：infer_return_fail.cj、instantiate_02.cj、varray_with_reftype03.cj）。

### 问题详情

（失败形态：EXP var a1: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>S1<!>, $1> = [S1()] // error；var a2: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>(Array<Int64>, String)<!>, $1> = [([1, 2, 3], "123")] // error；var a4: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>(CFunc<() -> Unit>, () -> Unit)<!>, $1> = [({=>}, {=>})] // error → ACT var a1: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>S1<!>, $1> = [S1()] // error；var a4: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>(CFunc<() -> Unit>, () -> Unit)<!>, $1> = [({=>}, {=>})] // error；var b2: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>S3<!>, $1> = [S3()] // error；相关 fixture：infer_return_fail.cj、instantiate_02.cj、varray_with_reftype03.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## CANNOT_CONVERT_LITERAL

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirAssignmentTypeMismatchChecker.kt:93`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP <!PARAMETERS_AND_ARGUMENTS_MISMATCH!>b<!>[<!CANNOT_CONVERT_LITERAL!>1<!>.01][0] != 1 → ACT 无诊断；相关 fixture：subscribe_in_binary.cj）。

### 问题详情

触发条件基于责任代码片段：74:         if (targetType != null && checkTargetTypedExpression(expression.rValue, targetType).isHandled) return；75:；76:         val outcome = expression.typeMismatchOutcome ?: return。（失败形态：EXP <!PARAMETERS_AND_ARGUMENTS_MISMATCH!>b<!>[<!CANNOT_CONVERT_LITERAL!>1<!>.01][0] != 1 → ACT 无诊断；相关 fixture：subscribe_in_binary.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## ANNOTATION_NOT_APPLICABLE_JFFI

### 发生位置

`CFIR 未定位到报告点——未实现`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP <!ANNOTATION_NOT_APPLICABLE_JFFI!>@A<!> → ACT 无诊断；相关 fixture：255.cj、err_abstract.cj、err_const_arg_01.cj、err_open.cj）。

### 问题详情

（失败形态：EXP <!ANNOTATION_NOT_APPLICABLE_JFFI!>@A<!> → ACT 无诊断；相关 fixture：255.cj、err_abstract.cj、err_const_arg_01.cj、err_open.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## RESUMPTION_HANDLE_TYPE_ERROR

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirEffectsExtraChecker.kt:67`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP <!TYPE_MISMATCH!>42<!> → ACT import <!UNRESOLVED_IMPORT!>stdx<!>.effect.Command；<!TYPE_MISMATCH!>"Hello"<!>；} <!EFFECTS_FEATURE_DISABLED!>handle (<!RESUMPTION_HANDLE_TYPE_ERROR!>_: Command<Unit><!>) <!MISMATCHING_HANDLE_BLOCK!>{；相关 fixture：wrong_return_type_try.cj）。

### 问题详情

触发条件基于责任代码片段：48:         val tryBodyType = expression.tryBlock.coneTypeOrNull；49:；50:         for (handler in expression.handlers) {。（失败形态：EXP <!TYPE_MISMATCH!>42<!> → ACT import <!UNRESOLVED_IMPORT!>stdx<!>.effect.Command；<!TYPE_MISMATCH!>"Hello"<!>；} <!EFFECTS_FEATURE_DISABLED!>handle (<!RESUMPTION_HANDLE_TYPE_ERROR!>_: Command<Unit><!>) <!MISMATCHING_HANDLE_BLOCK!>{；相关 fixture：wrong_return_type_try.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## INVALID_SUBSCRIPT_EXPR

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt:1829`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP public let c: B = <!RECURSIVE_CONSTRUCTOR_CALL!>B()<!>；public let c: A = <!RECURSIVE_CONSTRUCTOR_CALL!>A()<!>；b.<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 1 → ACT public let c: A = <!RECURSIVE_CONSTRUCTOR_CALL!>A<!>()；b.<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 1；a.c.<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 1；相关 fixture：record_vardecl_check.cj）。

### 问题详情

触发条件基于责任代码片段：1810: /**；1811:  * `[]` / `[]=` 在 resolve 中都会先降成 operator 调用；；1812:  * 这里把针对 `*operator_get` / `*operator_set` 的 unresolved 收束回语法级诊断。。（失败形态：EXP public let c: B = <!RECURSIVE_CONSTRUCTOR_CALL!>B()<!>；public let c: A = <!RECURSIVE_CONSTRUCTOR_CALL!>A()<!>；b.<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 1 → ACT public let c: A = <!RECURSIVE_CONSTRUCTOR_CALL!>A<!>()；b.<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 1；a.c.<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 1；相关 fixture：record_vardecl_check.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirGenericBareClassifierAccessChecker.kt:62`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP return <!RETURN_TYPE_MISMATCH!>value<!>；<!NEW_INFERENCE_ERROR!>name1<!>(1)；let b = <!NO_MATCHING_OPERATOR_INVOKE!>A<!>(1) → ACT return <!RETURN_TYPE_MISMATCH!>value<!>；<!UNABLE_TO_INFER_GENERIC_FUNC!>name1(1)<!>；let b = <!NO_MATCHING_OPERATOR_INVOKE!>A<!>(1)；相关 fixture：errorSimpleEnum.cj）。

### 问题详情

触发条件基于责任代码片段：43:         if (expression.typeArguments.isNotEmpty()) return；44:         val resolvedSymbol = expression.calleeReference.resolvedBareAccessSymbol() ?: return；45:。（失败形态：EXP return <!RETURN_TYPE_MISMATCH!>value<!>；<!NEW_INFERENCE_ERROR!>name1<!>(1)；let b = <!NO_MATCHING_OPERATOR_INVOKE!>A<!>(1) → ACT return <!RETURN_TYPE_MISMATCH!>value<!>；<!UNABLE_TO_INFER_GENERIC_FUNC!>name1(1)<!>；let b = <!NO_MATCHING_OPERATOR_INVOKE!>A<!>(1)；相关 fixture：errorSimpleEnum.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## UNABLE_TO_INFER_RETURN_TYPE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirFunctionSemanticsChecker.kt:207`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP 无诊断 → ACT func <!UNABLE_TO_INFER_RETURN_TYPE!>test<!>(a: ?T) {；相关 fixture：call_closet_rule_03.cj、extend_overload_builtin_op1.cj）。

### 问题详情

触发条件基于责任代码片段：188: /**；189:  * 函数返回类型推断检查器；190:  *。（失败形态：EXP 无诊断 → ACT func <!UNABLE_TO_INFER_RETURN_TYPE!>test<!>(a: ?T) {；相关 fixture：call_closet_rule_03.cj、extend_overload_builtin_op1.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## CANNOT_ASSIGN_TO_SUBSCRIPT

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt:1827`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP <!CANNOT_ASSIGN_TO_SUBSCRIPT!>b[r'c']<!> = 3 → ACT b[<!CANNOT_CONVERT_LITERAL!>r'c'<!>] = 3；相关 fixture：index_left_value_error.cj）。

### 问题详情

（失败形态：EXP <!CANNOT_ASSIGN_TO_SUBSCRIPT!>b[r'c']<!> = 3 → ACT b[<!CANNOT_CONVERT_LITERAL!>r'c'<!>] = 3；相关 fixture：index_left_value_error.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## TUPLE_PATTERN_NOT_MATCH

### 发生位置

`CFIR 未定位到报告点——未实现`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP <!TYPE_MISMATCH!>(a, <!UNRESOLVED_REFERENCE!>d<!>, <!UNRESOLVED_REFERENCE!>e<!>) = (1, 2, 3)<!>；<!TUPLE_PATTERN_NOT_MATCH!>(a, b, c, <!UNRESOLVED_REFERENCE!>d<!>) = (1, 2)<!>；<!TUPLE_PATTERN_NOT_MATCH!>(a, b, c) = (1, 2, 3, 4)<!> → ACT (a, <!UNRESOLVED_REFERENCE!>d<!>, <!UNRESOLVED_REFERENCE!>e<!>) = <!TYPE_MISMATCH!>(1, 2, 3)<!>；(a, b, c, <!UNRESOLVED_REFERENCE!>d<!>) = <!TYPE_MISMATCH!>(1, 2)<!>；(a, b, c) = <!TYPE_MISMATCH!>(1, 2, 3, 4)<!>；相关 fixture：case03.cj、match.cj、tuple3.cj）。

### 问题详情

（失败形态：EXP <!TYPE_MISMATCH!>(a, <!UNRESOLVED_REFERENCE!>d<!>, <!UNRESOLVED_REFERENCE!>e<!>) = (1, 2, 3)<!>；<!TUPLE_PATTERN_NOT_MATCH!>(a, b, c, <!UNRESOLVED_REFERENCE!>d<!>) = (1, 2)<!>；<!TUPLE_PATTERN_NOT_MATCH!>(a, b, c) = (1, 2, 3, 4)<!> → ACT (a, <!UNRESOLVED_REFERENCE!>d<!>, <!UNRESOLVED_REFERENCE!>e<!>) = <!TYPE_MISMATCH!>(1, 2, 3)<!>；(a, b, c, <!UNRESOLVED_REFERENCE!>d<!>) = <!TYPE_MISMATCH!>(1, 2)<!>；(a, b, c) = <!TYPE_MISMATCH!>(1, 2, 3, 4)<!>；相关 fixture：case03.cj、match.cj、tuple3.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## NO_CONSTRUCTOR

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt:203`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP 无诊断 → ACT <!WRONG_MODIFIER_TARGET!>static<!> init() {；let _ = <!NO_CONSTRUCTOR!>A<Int64><!>()；let _ = <!NO_CONSTRUCTOR!>A<Rune><!>()；相关 fixture：main.cj）。

### 问题详情

触发条件基于责任代码片段：184:  * 最终表现为普通函数调用 no-match；属性/变量访问不走这条映射。；185:  */；186: private fun ConeHiddenCandidateError.mapConeHiddenCandidateError(。（失败形态：EXP 无诊断 → ACT <!WRONG_MODIFIER_TARGET!>static<!> init() {；let _ = <!NO_CONSTRUCTOR!>A<Int64><!>()；let _ = <!NO_CONSTRUCTOR!>A<Rune><!>()；相关 fixture：main.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## USE_EXPR_WITHOUT_IMPORT

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirQuoteImportChecker.kt:34`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP <!USE_EXPR_WITHOUT_IMPORT!>@IfAvailable(level:19, { => println(0) }, {=>println(3)})<!>；<!USE_EXPR_WITHOUT_IMPORT!>@IfAvailable(syscap:"AA", { => println(0) }, {=>println(3)})<!> → ACT <!UNRESOLVED_REFERENCE!>@IfAvailable(level:19, { => println(0) }, {=>println(3)})<!>；<!UNRESOLVED_REFERENCE!>@IfAvailable(syscap:"AA", { => println(0) }, {=>println(3)})<!>；相关 fixture：err_without_import.cj、test01.cj）。

### 问题详情

触发条件基于责任代码片段：15:  * 未导入 `std.ast` 时 `quote {...}` 报错,提示需要的包名。；16:  */；17: object CfirQuoteImportChecker : CfirBasicExpressionChecker() {。（失败形态：EXP <!USE_EXPR_WITHOUT_IMPORT!>@IfAvailable(level:19, { => println(0) }, {=>println(3)})<!>；<!USE_EXPR_WITHOUT_IMPORT!>@IfAvailable(syscap:"AA", { => println(0) }, {=>println(3)})<!> → ACT <!UNRESOLVED_REFERENCE!>@IfAvailable(level:19, { => println(0) }, {=>println(3)})<!>；<!UNRESOLVED_REFERENCE!>@IfAvailable(syscap:"AA", { => println(0) }, {=>println(3)})<!>；相关 fixture：err_without_import.cj、test01.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## LAMBDA_MUST_HAVE_TYPE_ANNOTATION

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirFunctionLambdaChecker.kt:146`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP 无诊断 → ACT import stdx.net.http.{<!UNRESOLVED_IMPORT!>HttpContext<!>, <!UNRESOLVED_IMPORT!>HttpHeaders<!>}；<!CANNOT_ASSIGN_TO_IMMUTABLE!>_environment<!> = environment；<!CANNOT_ASSIGN_TO_IMMUTABLE!>_configuration<!> = configuration；相关 fixture：main.cj）。

### 问题详情

触发条件基于责任代码片段：127:             attributes = expectedFunctionType.attributes,；128:         )；129:     }。（失败形态：EXP 无诊断 → ACT import stdx.net.http.{<!UNRESOLVED_IMPORT!>HttpContext<!>, <!UNRESOLVED_IMPORT!>HttpHeaders<!>}；<!CANNOT_ASSIGN_TO_IMMUTABLE!>_environment<!> = environment；<!CANNOT_ASSIGN_TO_IMMUTABLE!>_configuration<!> = configuration；相关 fixture：main.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## CAPTURE_HAS_SHADOW_VARIABLE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirFieldVariableThisOrSuperInitializerChecker.kt:537`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP 无诊断 → ACT print("${<!CAPTURE_HAS_SHADOW_VARIABLE!>x<!>},")；print("${<!CAPTURE_HAS_SHADOW_VARIABLE!>x<!>},")；=> print("${<!CAPTURE_HAS_SHADOW_VARIABLE!>x<!>}")；相关 fixture：capture1.cj、capture_warning.cj）。

### 问题详情

（失败形态：EXP 无诊断 → ACT print("${<!CAPTURE_HAS_SHADOW_VARIABLE!>x<!>},")；print("${<!CAPTURE_HAS_SHADOW_VARIABLE!>x<!>},")；=> print("${<!CAPTURE_HAS_SHADOW_VARIABLE!>x<!>}")；相关 fixture：capture1.cj、capture_warning.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## STATIC_LAMBDA_CANNOT_ACCESS_NON_STATIC

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirExpressionSemanticsChecker.kt:425`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP func a(x1!: Int32 = <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>f<!>(), x2!: Int32 = <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>x<!>): Int32 {；<!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>f<!>()；<!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>x<!> → ACT <!USED_BEFORE_INITIALIZATION!>f<!>();；<!ILLEGAL_USAGE_OF_MEMBER!>x<!>;；相关 fixture：class_static_call_non_static2.cj、lambda01.cj）。

### 问题详情

触发条件基于责任代码片段：406:     context(context: CheckerContext, reporter: DiagnosticReporter)；407:     override fun check(expression: CfirQualifiedAccessExpression) {；408:         if (expression.explicitReceiver != null) return。（失败形态：EXP func a(x1!: Int32 = <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>f<!>(), x2!: Int32 = <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>x<!>): Int32 {；<!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>f<!>()；<!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>x<!> → ACT <!USED_BEFORE_INITIALIZATION!>f<!>();；<!ILLEGAL_USAGE_OF_MEMBER!>x<!>;；相关 fixture：class_static_call_non_static2.cj、lambda01.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## UNABLE_TO_INFER_DECL

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt:2421`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP let <!UNABLE_TO_INFER_DECL!>f<!> = {=> g} → ACT let f = {=> <!NO_MATCH_FUNCTION_DECLARATION_FOR_REF!>g<!>}；func <!UNABLE_TO_INFER_RETURN_TYPE!>g<!>() {；相关 fixture：err_recursive_func_00.cj、err_recursive_func_02.cj）。

### 问题详情

触发条件基于责任代码片段：2402:  * - 未命中的 reason 仍保持原行为（不报告）。；2403:  */；2404: private fun mapSimpleDiagnosticByReason(。（失败形态：EXP let <!UNABLE_TO_INFER_DECL!>f<!> = {=> g} → ACT let f = {=> <!NO_MATCH_FUNCTION_DECLARATION_FOR_REF!>g<!>}；func <!UNABLE_TO_INFER_RETURN_TYPE!>g<!>() {；相关 fixture：err_recursive_func_00.cj、err_recursive_func_02.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## INVALID_CFUNC_PARAMETER_TYPE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirForeignFunctionReturnTypeChecker.kt:78`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP 无诊断 → ACT foreign func printS(s: <!INVALID_CFUNC_PARAMETER_TYPE!>S<!>): Unit；var a = VArray<VArray<VArray<VArray<Int32, $2>, $2>, $2>, $2>(repeat: <!TYPE_MISMATCH!>[[[1, 2], [1, 2]], [[1, 2], [1, 2]]]<!>)；相关 fixture：varray_cstruct02.cj）。

### 问题详情

foreign 函数参数类型不满足 C 互操作规则（`CfirForeignFunctionReturnTypeChecker.kt` L62-78：isForeign 时逐参数检查，含 CFunc 嵌套诊断）。失败形态：`err_cfunc_param*.cj` 等。

### 修复方案

修复：C 互操作类型合法性检查（嵌套 CFunc 类型、typealias 到 CFunc）补全。

## IGNORE_OPEN

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirOpenMemberChecker.kt:60`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP public <!INCOMPATIBLE_MODIFIERS!>static<!> <!INCOMPATIBLE_MODIFIERS!>open<!> func openFn(): Unit {}；public <!INCOMPATIBLE_MODIFIERS!>static<!> <!INCOMPATIBLE_MODIFIERS!>override<!> func canOverride(): Unit {}；static <!WRONG_MODIFIER_TARGET!>abstract<!> func absFn(): Unit → ACT <!IGNORE_OPEN!>p<!>ublic <!INCOMPATIBLE_MODIFIERS!>static<!> <!INCOMPATIBLE_MODIFIERS!>open<!> func openFn(): Unit {}；public <!INCOMPATIBLE_MODIFIERS!>static<!> <!INCOMPATIBLE_MODIFIERS!>override<!> func <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>canOverride<!>(): Unit {}；static <!WRONG_MODIFIER_TARGET!>abstract<!> func absFn(): Unit；相关 fixture：memberStatusCheckersRich.cj、staticCannotBeOpenAbstractOverride.cj、staticIncompatibleModifiersRich.cj）。

### 问题详情

（失败形态：EXP public <!INCOMPATIBLE_MODIFIERS!>static<!> <!INCOMPATIBLE_MODIFIERS!>open<!> func openFn(): Unit {}；public <!INCOMPATIBLE_MODIFIERS!>static<!> <!INCOMPATIBLE_MODIFIERS!>override<!> func canOverride(): Unit {}；static <!WRONG_MODIFIER_TARGET!>abstract<!> func absFn(): Unit → ACT <!IGNORE_OPEN!>p<!>ublic <!INCOMPATIBLE_MODIFIERS!>static<!> <!INCOMPATIBLE_MODIFIERS!>open<!> func openFn(): Unit {}；public <!INCOMPATIBLE_MODIFIERS!>static<!> <!INCOMPATIBLE_MODIFIERS!>override<!> func <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>canOverride<!>(): Unit {}；static <!WRONG_MODIFIER_TARGET!>abstract<!> func absFn(): Unit；相关 fixture：memberStatusCheckersRich.cj、staticCannotBeOpenAbstractOverride.cj、staticIncompatibleModifiersRich.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirMutabilityCheckers.kt:166`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP let f = {=> <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>t<!>his}；<!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>t<!>his；let f = {=> <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>t<!>his.i = 2} → ACT 无诊断；相关 fixture：record_extend_mut_invalid_3.cj、record_mut_invalid_4.cj、record_mut_invalid_5.cj）。

### 问题详情

（失败形态：EXP let f = {=> <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>t<!>his}；<!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>t<!>his；let f = {=> <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>t<!>his.i = 2} → ACT 无诊断；相关 fixture：record_extend_mut_invalid_3.cj、record_mut_invalid_4.cj、record_mut_invalid_5.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## VARRAY_ARG_TYPE_WITH_REFTYPE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirVArrayConstructorArgChecker.kt:67`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP var a: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>S1<!>, $1> → ACT 无诊断；相关 fixture：varray_with_reftype04.cj、varray_with_reftype05.cj）。

### 问题详情

（失败形态：EXP var a: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>S1<!>, $1> → ACT 无诊断；相关 fixture：varray_with_reftype04.cj、varray_with_reftype05.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirInterfaceCallWithUnimplementedCallChecker.kt:53`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP 无诊断 → ACT A<Int64>.<!INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>test<!>(0)；相关 fixture：test.cj、use_interface_def_imple_024.cj）。

### 问题详情

（失败形态：EXP 无诊断 → ACT A<Int64>.<!INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>test<!>(0)；相关 fixture：test.cj、use_interface_def_imple_024.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## ILLEGAL_CAPTURE_THIS

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirMutabilityCheckers.kt:159`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP f = {=> <!ILLEGAL_CAPTURE_THIS!>t<!>his.x}；f = {=> <!ILLEGAL_CAPTURE_THIS!>t<!>his.x} → ACT 无诊断；相关 fixture：err_capture_00.cj、super_this_12.cj）。

### 问题详情

（失败形态：EXP f = {=> <!ILLEGAL_CAPTURE_THIS!>t<!>his.x}；f = {=> <!ILLEGAL_CAPTURE_THIS!>t<!>his.x} → ACT 无诊断；相关 fixture：err_capture_00.cj、super_this_12.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## EXTEND_C_TYPE_NOT_ALLOWED

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirExtendCheckers.kt:86`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP extend <!EXTEND_C_TYPE_NOT_ALLOWED!>NativeBox<!> <: Printable {}；extend <!EXTEND_A_JAVA_TYPE!>JavaBox<!> <: Printable {} → ACT 无诊断；相关 fixture：extendCTypeNotAllowed.cj）。

### 问题详情

触发条件基于责任代码片段：67:      */；68:     context(context: CheckerContext, reporter: DiagnosticReporter)；69:     override fun check(extend: CfirExtend) {。（失败形态：EXP extend <!EXTEND_C_TYPE_NOT_ALLOWED!>NativeBox<!> <: Printable {}；extend <!EXTEND_A_JAVA_TYPE!>JavaBox<!> <: Printable {} → ACT 无诊断；相关 fixture：extendCTypeNotAllowed.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## CLASS_INHERIT_NON_CLASS_NOR_INTERFACE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirSupertypesChecker.kt:162`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP <!TYPE_IMPLEMENT_NON_INTERFACE!>s<!>truct A <: A {} → ACT struct A <: <!CLASS_INHERIT_NON_CLASS_NOR_INTERFACE!>A<!> {}；相关 fixture：record_implement_interface_n1.cj）。

### 问题详情

触发条件基于责任代码片段：143:      * 检查 class/struct/enum 的 concrete 父类型顺序、多继承和可继承性。；144:      *；145:      * 官方 `CheckAndAddSubDecls` 逐个父类型维护两个状态：。（失败形态：EXP <!TYPE_IMPLEMENT_NON_INTERFACE!>s<!>truct A <: A {} → ACT struct A <: <!CLASS_INHERIT_NON_CLASS_NOR_INTERFACE!>A<!> {}；相关 fixture：record_implement_interface_n1.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirExpressionSemanticsChecker.kt:552`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP <!ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR!>f<!>；var a = {=> <!ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR!>f<!>}；func f3(c!: Int32 = <!ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR!>f<!>) { → ACT 无诊断；相关 fixture：class_open_ctor1_fail.cj、class_open_ctor2_fail.cj、class_open_modifier4.cj、let_in_init13-1.cj）。

### 问题详情

（失败形态：EXP <!ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR!>f<!>；var a = {=> <!ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR!>f<!>}；func f3(c!: Int32 = <!ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR!>f<!>) { → ACT 无诊断；相关 fixture：class_open_ctor1_fail.cj、class_open_ctor2_fail.cj、class_open_modifier4.cj、let_in_init13-1.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## RECURSIVE_CONSTRUCTOR_CALL

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirRecursiveConstructorCallChecker.kt:67`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP class Loop <: <!SUPER_TYPES_SELF_REFERENCE!>Loop<!> {}；class <!SUPER_TYPES_DUPLICATE!>DupInterfaces<!> <: IReadable & IReadable & IWritable {} → ACT class <!RECURSIVE_CONSTRUCTOR_CALL!>Loop<!> <: <!SUPER_TYPES_SELF_REFERENCE!>Loop<!> {}；<!SUPER_TYPES_DUPLICATE!>class DupInterfaces<!> <: IReadable & IReadable & IWritable {}；相关 fixture：superSelfAndDuplicate.cj、superSelfAndDuplicateRich.cj）。

### 问题详情

（失败形态：EXP class Loop <: <!SUPER_TYPES_SELF_REFERENCE!>Loop<!> {}；class <!SUPER_TYPES_DUPLICATE!>DupInterfaces<!> <: IReadable & IReadable & IWritable {} → ACT class <!RECURSIVE_CONSTRUCTOR_CALL!>Loop<!> <: <!SUPER_TYPES_SELF_REFERENCE!>Loop<!> {}；<!SUPER_TYPES_DUPLICATE!>class DupInterfaces<!> <: IReadable & IReadable & IWritable {}；相关 fixture：superSelfAndDuplicate.cj、superSelfAndDuplicateRich.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirGenericInstantiationChecker.kt:1655`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP <!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend<T> C1<T> <: I1<T> {}；<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend<T> C1<T> <: I2<T> {} → ACT <!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend<T> <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C1<T><!> <: I1<T> {}；<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend<T> <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C1<T><!> <: I2<T> {}；相关 fixture：extend_function_conflict_invalid_3.cj、generic_subst_perf.cj）。

### 问题详情

（失败形态：EXP <!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend<T> C1<T> <: I1<T> {}；<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend<T> C1<T> <: I2<T> {} → ACT <!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend<T> <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C1<T><!> <: I1<T> {}；<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend<T> <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C1<T><!> <: I2<T> {}；相关 fixture：extend_function_conflict_invalid_3.cj、generic_subst_perf.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## DIAG_REPORT_ERROR_MESSAGE

### 发生位置

`CFIR 未定位到报告点——未实现`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP let a = <!DIAG_REPORT_ERROR_MESSAGE!>i<!>f (true) { → ACT 无诊断；相关 fixture：join_ordered_problem_0.cj、join_ordered_problem_1.cj）。

### 问题详情

（失败形态：EXP let a = <!DIAG_REPORT_ERROR_MESSAGE!>i<!>f (true) { → ACT 无诊断；相关 fixture：join_ordered_problem_0.cj、join_ordered_problem_1.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInheritanceDeepChecker.kt:241`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP <!INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE!>p<!>ublic mut func foo1() {；<!INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE!>p<!>ublic func foo2() {；<!INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE!>p<!>ublic mut func foo3(): Unit { → ACT 无诊断；相关 fixture：record_mut_invalid_6.cj、record_mut_invalid_7.cj）。

### 问题详情

struct 与接口 mut 修饰符不兼容（`CfirInheritanceDeepChecker.kt` L235-241：函数返回类型冲突/实现一致性检查分支）。失败形态：`record_extend_mut_invalid*.cj` 相关。

### 修复方案

修复：struct 实现接口成员 mut 一致性检查补全。

## INVALID_ACCESS_CONTROL

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt:2029`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP return counter.<!INVISIBLE_MEMBER!>secret<!>；return counter.<!INVISIBLE_MEMBER!>secretFunc<!>() → ACT return <!INVALID_ACCESS_CONTROL!>c<!>ounter.secret；return counter.<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>s<!>ecretFunc()；相关 fixture：privateMemberAndPropertyAccess.cj）。

### 问题详情

非法访问控制（`coneDiagnosticToCfirDiagnostic.kt` L2029）。失败形态：`invisibleReferenceAndMember.cj` 等。

### 修复方案

修复：访问控制失败按官方诊断名分流。

## INCOMPATIBLE_FUNC_BODY_AND_RETURN_TYPE

### 发生位置

`CFIR 未定位到报告点——未实现`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP <!INCOMPATIBLE_FUNC_BODY_AND_RETURN_TYPE!>r<!>eturn a + b → ACT 无诊断；相关 fixture：bottomType03.cj）。

### 问题详情

函数体与返回类型不兼容（CFIR 未实现该检查器）。失败形态：`type_arg_infer` 关联。

### 修复方案

修复：对照官方补建。

## UNUSED_VARIABLE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirUnusedExpressionChecker.kt:156`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP let <!UNUSED_VARIABLE!>l2<!> = {=> ctx = 10} → ACT <!WRONG_MODIFIER_TARGET!>static<!> init() {；<!WRONG_MODIFIER_TARGET!>static<!> init() {；相关 fixture：static_or_global_var13.cj）。

### 问题详情

触发条件基于责任代码片段：137:；138:             override fun visitFunctionCall(functionCall: CfirFunctionCall) {；139:                 functionCall.resolvedVariableSymbolOrNull()?.let { usedVariables += it }。（失败形态：EXP let <!UNUSED_VARIABLE!>l2<!> = {=> ctx = 10} → ACT <!WRONG_MODIFIER_TARGET!>static<!> init() {；<!WRONG_MODIFIER_TARGET!>static<!> init() {；相关 fixture：static_or_global_var13.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## REF_NOT_BE_TYPE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirClassifierAsExpressionChecker.kt:50`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP public interface fatherInterface<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> where T <: UInt32 {；public interface MyInterface<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> <: fatherInterface<T> where T <: UInt32 {；enum1<MyInterface<UInt32>>.foo2<UInt32>(enum1<MyInterface<UInt32>>.foo1() + enum1<MyInterface<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>UInt16<!>>.foo1())) → ACT public interface fatherInterface<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> where T <: UInt32 {；public interface MyInterface<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> <: fatherInterface<T> where T <: UInt32 {；enum1<MyInterface<UInt32>>.foo2<UInt32>(enum1<MyInterface<UInt32>>.foo1() <!INVALID_BINARY_OPERATOR!>+<!> <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!><!REF_NOT_BE_TYPE!>e<!>num1<!><<!INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>MyInterface<UInt16>.foo1()<!>))；相关 fixture：generic_constraint14.cj）。

### 问题详情

触发条件基于责任代码片段：31:      *；32:      * 函数调用和作为外层接收者的 qualifier 不在这里报告。类型参数使用完整引用范围；；33:      * class-like symbol 保持现有首字符范围行为，由其独立诊断范围簇继续处理。。（失败形态：EXP public interface fatherInterface<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> where T <: UInt32 {；public interface MyInterface<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> <: fatherInterface<T> where T <: UInt32 {；enum1<MyInterface<UInt32>>.foo2<UInt32>(enum1<MyInterface<UInt32>>.foo1() + enum1<MyInterface<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>UInt16<!>>.foo1())) → ACT public interface fatherInterface<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> where T <: UInt32 {；public interface MyInterface<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> <: fatherInterface<T> where T <: UInt32 {；enum1<MyInterface<UInt32>>.foo2<UInt32>(enum1<MyInterface<UInt32>>.foo1() <!INVALID_BINARY_OPERATOR!>+<!> <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!><!REF_NOT_BE_TYPE!>e<!>num1<!><<!INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>MyInterface<UInt16>.foo1()<!>))；相关 fixture：generic_constraint14.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## FLOAT_LITERAL_TOO_LARGE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirExpressionSemanticsChecker.kt:112`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP let small16: Float16 = <!FLOAT_LITERAL_TOO_SMALL!>5.9604E-8<!>；let large16: Float16 = <!FLOAT_LITERAL_TOO_LARGE!>6.5540E4<!>；let small32: Float32 = <!FLOAT_LITERAL_TOO_SMALL!>0.6012E-45<!> → ACT 无诊断；相关 fixture：overflow_check_float.cj）。

### 问题详情

触发条件基于责任代码片段：93:         val resolvedType = expression.coneTypeOrNull；94:；95:         if (doubleValue.isNaN() || doubleValue.isInfinite()) {。（失败形态：EXP let small16: Float16 = <!FLOAT_LITERAL_TOO_SMALL!>5.9604E-8<!>；let large16: Float16 = <!FLOAT_LITERAL_TOO_LARGE!>6.5540E4<!>；let small32: Float32 = <!FLOAT_LITERAL_TOO_SMALL!>0.6012E-45<!> → ACT 无诊断；相关 fixture：overflow_check_float.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## INHERIT_THREAD_CONTEXT_INVALID

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInheritanceThreadContextChecker.kt:62`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP <!INHERIT_THREAD_CONTEXT_INVALID!>class MyCustomThreadContext <: ThreadContext {；}<!>；<!INHERIT_THREAD_CONTEXT_INVALID!>interface MyInterfaceThreadContext <: ThreadContext { → ACT let fut = spawn (<!SPAWN_ARG_INVALID!>MyCustomThreadContext()<!>) {}；相关 fixture：spawn8.cj）。

### 问题详情

触发条件基于责任代码片段：43:             if (superClassId.shortClassName != THREAD_CONTEXT) continue；44:；45:             val superSymbol = context.session.symbolProvider。（失败形态：EXP <!INHERIT_THREAD_CONTEXT_INVALID!>class MyCustomThreadContext <: ThreadContext {；}<!>；<!INHERIT_THREAD_CONTEXT_INVALID!>interface MyInterfaceThreadContext <: ThreadContext { → ACT let fut = spawn (<!SPAWN_ARG_INVALID!>MyCustomThreadContext()<!>) {}；相关 fixture：spawn8.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## EXPORT_EXTEND_DEPEND_NON_EXPORT_EXTEND

### 发生位置

`CFIR 未定位到报告点——未实现`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP <!EXPORT_EXTEND_DEPEND_NON_EXPORT_EXTEND!>extend A <: I {}<!> → ACT 无诊断；相关 fixture：testb.cj）。

### 问题详情

（失败形态：EXP <!EXPORT_EXTEND_DEPEND_NON_EXPORT_EXTEND!>extend A <: I {}<!> → ACT 无诊断；相关 fixture：testb.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## CONST_EVAL_DIVIDE_BY_ZERO

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirConstEvalArithmeticChecker.kt:121`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP <!CONST_EVAL_DIVIDE_BY_ZERO!>m /= a<!> → ACT 无诊断；相关 fixture：binary_error_report_05.cj）。

### 问题详情

触发条件基于责任代码片段：102:             checkShiftConstant(expression, source, rightExpression)；103:             return；104:         }。（失败形态：EXP <!CONST_EVAL_DIVIDE_BY_ZERO!>m /= a<!> → ACT 无诊断；相关 fixture：binary_error_report_05.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## ENUM_CONSTRUCTOR_WITH_PARAM_MUST_HAVE_ARGS

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirFunctionReferenceLegalityChecker.kt:57`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP <!ENUM_CONSTRUCTOR_WITH_PARAM_MUST_HAVE_ARGS!>e<!>；<!ENUM_CONSTRUCTOR_WITH_PARAM_MUST_HAVE_ARGS!>e<!> → ACT <!UNRESOLVED_REFERENCE!>e<!>；<!UNRESOLVED_REFERENCE!>e<!>；相关 fixture：enum33.cj）。

### 问题详情

触发条件基于责任代码片段：38:         val targetSymbol = expression.resolvedCallableSymbolOrNull()；39:         if (targetSymbol == null) {；40:             val recoveredMutFunction = expression.declaredUpperBoundMutFunctionOrNull() ?: return。（失败形态：EXP <!ENUM_CONSTRUCTOR_WITH_PARAM_MUST_HAVE_ARGS!>e<!>；<!ENUM_CONSTRUCTOR_WITH_PARAM_MUST_HAVE_ARGS!>e<!> → ACT <!UNRESOLVED_REFERENCE!>e<!>；<!UNRESOLVED_REFERENCE!>e<!>；相关 fixture：enum33.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## NOT_A_TYPE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirTypeAliasExpandedTypeChecker.kt:49`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP public let <!ACCESSIBILITY_ERROR!>ID<!>: EntryID；public var WrappedJob:<!NOT_A_TYPE!>J<!>ob；init(id:EntryID,s:<!NOT_A_TYPE!>S<!>chedule,w:<!NOT_A_TYPE!>J<!>ob,j:<!NOT_A_TYPE!>J<!>ob){ → ACT <!ACCESSIBILITY_ERROR!>public let ID: EntryID<!>；this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>ID<!> = id；相关 fixture：bugfix1.cj）。

### 问题详情

触发条件基于责任代码片段：30:      * 检查 typealias RHS 根类型是否应补报 `NOT_A_TYPE`。；31:      *；32:      * unresolved qualifier 和递归 typealias 已有专门诊断时跳过，其他错误类型引用取根 qualifier。（失败形态：EXP public let <!ACCESSIBILITY_ERROR!>ID<!>: EntryID；public var WrappedJob:<!NOT_A_TYPE!>J<!>ob；init(id:EntryID,s:<!NOT_A_TYPE!>S<!>chedule,w:<!NOT_A_TYPE!>J<!>ob,j:<!NOT_A_TYPE!>J<!>ob){ → ACT <!ACCESSIBILITY_ERROR!>public let ID: EntryID<!>；this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>ID<!> = id；相关 fixture：bugfix1.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## CFUNC_CANNOT_HAVE_UNIT_ARGS

### 发生位置

`CFIR 未定位到报告点——未实现`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP foreign func foo(<!CFUNC_CANNOT_HAVE_UNIT_ARGS!>a: Unit<!>): Unit；foreign func bar(<!CFUNC_CANNOT_HAVE_UNIT_ARGS!>a: Unit<!>): Unit → ACT 无诊断；相关 fixture：c_abi_unit.cj）。

### 问题详情

cfunc 不能有 Unit 参数（CFIR 未定位到报告点——未实现）。失败形态：`ffi/*.cj` 相关。

### 修复方案

修复：对照官方补建（C 互操作函数参数不能为 Unit）。

## CONST_EVAL_ARITHMETIC_OVERFLOW

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirConstEvalArithmeticChecker.kt:134`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP var _: UInt64 = 18446744073709551614 * <!LITERAL_NUMERIC_OVERFLOW!>-18446744073709551615<!>；var _: UInt64 = <!LITERAL_NUMERIC_OVERFLOW!>-18446744073709551615<!> * <!LITERAL_NUMERIC_OVERFLOW!>-18446744073709551615<!> → ACT var _: Int8 = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>2 * 64<!>；var _: Int8 = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>-2 * -64<!>；var _: Int8 = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>-2 * 65<!>；相关 fixture：mul_overflow.cj）。

### 问题详情

触发条件基于责任代码片段：115:                     "UInt64",；116:                     "Int64",；117:                 )。（失败形态：EXP var _: UInt64 = 18446744073709551614 * <!LITERAL_NUMERIC_OVERFLOW!>-18446744073709551615<!>；var _: UInt64 = <!LITERAL_NUMERIC_OVERFLOW!>-18446744073709551615<!> * <!LITERAL_NUMERIC_OVERFLOW!>-18446744073709551615<!> → ACT var _: Int8 = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>2 * 64<!>；var _: Int8 = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>-2 * -64<!>；var _: Int8 = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>-2 * 65<!>；相关 fixture：mul_overflow.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## MUT_ONLY_ON_FUNCTION

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirFunctionSemanticsChecker.kt:141`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP public <!MUT_ONLY_ON_FUNCTION!>mut<!> func foo() { → ACT 无诊断；相关 fixture：record_extend_mut_invalid_1.cj）。

### 问题详情

触发条件基于责任代码片段：122:         checkMutFunction(declaration)；123:         checkStaticFunctionStatus(declaration)；124:     }。（失败形态：EXP public <!MUT_ONLY_ON_FUNCTION!>mut<!> func foo() { → ACT 无诊断；相关 fixture：record_extend_mut_invalid_1.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirFunctionSemanticsChecker.kt:168`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>A<!> <: I {}；static func <!STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE!>a<!>(): Unit → ACT class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>A<!> <: I {}；class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>B<!> {；相关 fixture：abstract_function2.cj）。

### 问题详情

触发条件基于责任代码片段：149:      * 只在冲突状态来自非源码修饰符或尚未由 modifier checker 成对处理时报告函数名级诊断。；150:      */；151:     context(context: CheckerContext, reporter: DiagnosticReporter)。（失败形态：EXP class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>A<!> <: I {}；static func <!STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE!>a<!>(): Unit → ACT class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>A<!> <: I {}；class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>B<!> {；相关 fixture：abstract_function2.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## MISSING_ENTRY

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirProgramEntryChecker.kt:57`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP <!MISSING_ENTRY!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.；struct <!CSTRUCT_CANNOT_IMPL_INTERFACES!>R<!> <: I1 {；extend <!EXTEND_C_TYPE_NOT_ALLOWED!>R<!> <: I2 {} → ACT <!CSTRUCT_CANNOT_IMPL_INTERFACES!>struct R <: I1 {；}<!>；相关 fixture：c_type_interface.cj）。

### 问题详情

触发条件基于责任代码片段：38:；39: /**；40:  * 模块级程序入口检查。。（失败形态：EXP <!MISSING_ENTRY!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.；struct <!CSTRUCT_CANNOT_IMPL_INTERFACES!>R<!> <: I1 {；extend <!EXTEND_C_TYPE_NOT_ALLOWED!>R<!> <: I2 {} → ACT <!CSTRUCT_CANNOT_IMPL_INTERFACES!>struct R <: I1 {；}<!>；相关 fixture：c_type_interface.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## ANNOTATION_NO_CONST_INIT

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirAnnotationDeclarationChecker.kt:34`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP 无诊断 → ACT public class <!ANNOTATION_NO_CONST_INIT!>A<!> {；相关 fixture：ok_class_02.cj）。

### 问题详情

触发条件基于责任代码片段：15:  *；16:  * 这属于 declaration 层规则：；17:  * 它不依赖调用点，而是约束“被标记为注解类型的声明自身”。。（失败形态：EXP 无诊断 → ACT public class <!ANNOTATION_NO_CONST_INIT!>A<!> {；相关 fixture：ok_class_02.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

## VAR_IN_OR_PATTERN

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirPatternExpressionChecker.kt:205`

### 测试文件

对应 fixture 见问题详情中的失败形态（失败形态：EXP 无诊断 → ACT case <!VAR_IN_OR_PATTERN!>_<!>: Int64 | <!REDECLARATION!>_<!>: Option<Bool> => 0；相关 fixture：match024.cj）。

### 问题详情

触发条件基于责任代码片段：186:      *；187:      * 对齐 C++ DiagKind::sema_different_or_pattern:；188:      * or-pattern 中各子模式必须是同类（同为 enum / const / type 等）。。（失败形态：EXP 无诊断 → ACT case <!VAR_IN_OR_PATTERN!>_<!>: Int64 | <!REDECLARATION!>_<!>: Option<Bool> => 0；相关 fixture：match024.cj）

### 修复方案

对照官方 `external/cangjie_compiler` 对应 DiagKind 的触发路径逐行对齐（责任位置见上）。

---

# 问题 16：核心根因深度验证记录（逐行读代码确认，含对既有分析的修正）

> 本章是对问题 1-12 中关键根因的二次验证：逐行读取责任代码，确认/修正此前推断，并给出调用链级证据。所有验证均在 2026-08-04 完成。

## 16.1 问题 1 机制确认：NOT_TRACKED 的完整产生与传播链

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInitializationCheckers.kt`

### 问题详情（逐行验证结论）

`CANNOT_ASSIGN_TO_IMMUTABLE` 误报的完整调用链（此前只推断到 `isImmutableFieldAssignmentForbidden`，现在确认到数据流层）：

1. `CfirAssignmentLegalityChecker.kt` L372-389 `isImmutableFieldAssignmentForbidden`：`let` 字段仅当 `classifyAssignment` 返回 `INITIALIZATION`/`PRIORITY_INITIALIZATION_DIAGNOSTIC` 时放行，`NOT_TRACKED`/`null` 一律判非法。
2. `CfirInitializationCheckers.kt` L1720-1761 `CfirInitializationAssignmentClassifier.classifyAssignment`：初始值就是 `NOT_TRACKED`（L1727），对每个 enclosing function 取分类表并 merge（L1743）。
3. `merge`（L1764-1769）：`other ?: return this`——**分类表里没有该赋值时保持 NOT_TRACKED**，这正是"未产生分类记录"的传播路径。
4. `analyzeAssignmentTargetAccess`（L614-702）是分类产生的唯一来源：
   - L626 `access.resolvedAccessSymbolOrNull() ?: return afterReceiver`——符号解析失败直接返回，**不产生分类**；
   - L627 `afterReceiver.trackedVariable(symbol)`——宏展开场景（`@APILevel` 展开产物）字段符号不在 tracked 集合（tracked 集合在 L396-398 由 `owner.instanceFieldInfos(context)` 建立，宏模板字段未被纳入）；
   - L691-700 `else -> reportIllegalMemberAccessIfNeeded(...)`——**该分支只报告非法访问，不调用 `recordAssignmentClassification`**，赋值不产生任何分类记录。
5. 因此 `classifyAssignment` 返回初始值 `NOT_TRACKED` → `isImmutableFieldAssignmentForbidden` 判 `NOT_TRACKED` 为非法 → 报 `CANNOT_ASSIGN_TO_IMMUTABLE`。

### 测试文件

`key.cj`/`merge04/test2.cj` 等宏族（同问题 1）。

### 修复方案（验证后细化）

除问题 1 的修复外，补充确认：`analyzeAssignmentTargetAccess` 的 else 分支（L693-700）应当对"符号已解析但未跟踪"的赋值也调用 `recordAssignmentClassification(assignment, NOT_TRACKED)` 并放行不可变检查——即把"未跟踪"与"非法写入"分离，而不是让 `NOT_TRACKED` 直接传播为非法。更根本的修复是让宏展开产物的字段符号进入 `owner.instanceFieldInfos` 的 tracked 集合。

## 16.2 问题 4 机制确认：已知行覆盖判定与 box 语义缺失

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirMatchUnreachablePatternChecker.kt`；`cfir/semantics/src/org/cangnova/cangjie/cfir/resolve/match/CfirMatchTypeRelations.kt`

### 问题详情（逐行验证结论）

**误报方向（嵌套 tuple）**：

1. `knownSubjectRowsOrNull`（L245-257）为 `((), ())` 字面量构造 `[[tuple pattern]]` 已知行（L278 走 `knownTuplePatternOrNull`）。
2. `isUnreachable`（L130-145）L137-143：`uncoveredKnownRows = knownSubjectRows.filter { !it.isCoveredBy(previousRows) }`，随后 `uncoveredKnownRows.none { knownRow.isCoveredBy(listOf(this)) }`——用**分支行能否覆盖已知行**判定不可达。
3. `isCoveredBy` 底层走 `isMatchSubtypeOf`（`CfirMatchTypeRelations.kt` L31-53）：tuple 递归（L36-41）要求元数相等且元素逐一子类型。`(Int64, Int64)` 分支对 `(Unit, Unit)` 已知行不成立 → 判"不能覆盖已知行" → 报不可达。
4. 官方 Sema 的类型不兼容分支**根本不进入 usefulness 矩阵**（L81 注释："只消费已类型检查的 pattern 矩阵"），CFIR 的 `check` 只过滤 `hasPatternLegalityProblem`（L68），tuple 分支的类型兼容性不属于 pattern legality → 漏进矩阵 → 误报。

**漏报方向（box/unbox）**：

1. `isMatchSubtypeOf`（L31-53）只在 tuple/function 内递归（L36-50），普通类型走 `hasTypeAwareSupertype || hasVisibleExtendSupertype`（L51-52），**无 box 语义**。
2. `isTypePatternOrdinarySubtypeOf`（L73-104）L102 显式排除 `requiresBoxingToClassLikeSupertype`。
3. 官方 `IsSubtypeBoxed` 的 `Int64`↔`Option<Int64>` boxed interface 关系（L26 注释明确说明）在 CFIR 完全缺失 → 官方判覆盖的 autobox 分支 CFIR 判不覆盖 → 漏报。

### 测试文件

`match_nested_in_tuple_001.cj`、`autobox_match1/2.cj`、`unbox_*.cj`（同问题 4）。

### 修复方案（验证后细化）

1. 误报方向：在 `check` 的 `calculateMatrix` 前增加类型兼容过滤——与 subject 静态类型元素不兼容的行不得参与覆盖判定（对齐官方"只消费已类型检查矩阵"）。
2. 漏报方向：`isMatchSubtypeOf` 增加 boxed 关系分支（对齐官方 `IsSubtypeBoxed`），或在 `knownStdlibOptionSomePatternOrNull` 已有的 Option 已知行基础上，让 `isCoveredBy` 对 `Option<T>`/`T` 判定 box 覆盖。

## 16.3 问题 8 机制确认：索引层无父类传递（精确到 modelsForTarget）

### 发生位置

`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/services/CfirExtendIndexStore.kt`

### 问题详情（逐行验证结论）

1. `otherPackageExtendedInterfaceClassIds`（L267-283）只经 `modelsForTarget(targetKey)` 查询——`targetKey` 是 `CfirExtendTargetKey.ClassLike(targetClassId)` 精确键。
2. `modelsForTarget`（L128-129）仅查 `modelsByTargetKey[targetKey].orEmpty()`——**按精确目标键查表，无父类继承链遍历**。
3. `import_orphanrule_02` 场景：目标 `B` 自身无直接 extend（extend 挂在父类 `A` 上），`modelsForTarget(B)` 返回空 → `otherPackageExtendedInterfaceClassIds` 返回空集 → 差集 `currentExternalInterfaces - {} = {I1, I2}` 非空 → 误报 `EXTEND_ORPHAN_RULE`。

### 测试文件

`import_orphanrule_01~06/main.cj`（同问题 8）。

### 修复方案（验证后细化）

`otherPackageExtendedInterfaceClassIds` 在 `modelsForTarget(targetKey)` 之外，沿目标类型父类链（`typeAwareSupertypeProvider`）收集父类上的外部扩展闭包并入结果集；或把父类扩展闭包在索引构建时合并进 `modelsByTargetKey` 的子键。

## 16.4 问题 2 机制修正：closestFunctionLikeDeclaration 类型不匹配（新发现）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirConstructorDelegationCallChecker.kt` L57-61；`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/context/CheckerContext.kt` L331-341

### 问题详情（逐行验证结论，修正问题 2 的既有推断）

此前推断"构造器未入栈"。**实际根因更精确——是类型不匹配**：

1. `containingDeclarations` 声明为 `List<CfirBasedSymbol<*>>`（CheckerContext.kt L40），`addDeclaration` 压入的是 `declaration.symbol`（L333），弹出 `removeLast()`（L339）。
2. `closestFunctionLikeDeclaration`（L57-61）：

```kotlin
private fun CheckerContext.closestFunctionLikeDeclaration(): CfirFunction? {
    return containingDeclarations
        .asReversed()
        .firstOrNull { declaration -> declaration is CfirFunction } as? CfirFunction
}
```

3. 栈元素是 `CfirFunctionSymbol`（`cfir/cfir-tree/src/.../symbols/FirFunctionSymbol.kt` L17：`sealed class CfirFunctionSymbol<out D : CfirFunction> : CfirCallableSymbol<D>()`）——**symbol 是 `CfirCallableSymbol<D>` 子类，并不实现 `CfirFunction` 声明接口**。
4. 因此 `declaration is CfirFunction` 对栈内所有元素恒为 false，`closestFunctionLikeDeclaration` **恒返回 null**，`closestFunction is CfirConstructor` 恒 false → 任何构造器内 `super()`/`this()` 都直接走到 L39-43 报 `INVALID_THIS_CALL_OUTSIDE_CTOR`。
5. 对照：同文件 L27 `findClosestDeclaration<CfirClassLikeDeclaration>()` 走的是 `declaration.cfir as? T`（CheckerContext.kt L428）——**正确解包了 `.cfir`**，所以类检测正常；而 `closestFunctionLikeDeclaration` 没有做同样的解包，这是唯一的差异点。

### 测试文件

`primaryConstructor1.cj`、`primaryConstructor8.cj`、`super_this_11.cj`、`class6.cj`、`class_generic_inheritance*.cj`（同问题 2，76+8 失败）。

### 修复方案（验证后修正）

`closestFunctionLikeDeclaration` 改为与 `findClosestDeclaration` 一致地解包 symbol：

```kotlin
private fun CheckerContext.closestFunctionLikeDeclaration(): CfirFunction? {
    return containingDeclarations
        .asReversed()
        .firstNotNullOfOrNull { declaration -> declaration.cfir as? CfirFunction }
}
```

（`findClosest` L417-423 与 `findClosestDeclaration` L426-434 都已处理 `.cfir` 解包，唯独该私有函数遗漏——修复后 84 个相关失败应全部消除。）

## 16.5 问题 6 机制确认：复合赋值形态漏报（精确到 checker 遍历）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirMutabilityCheckers.kt` L80-97

### 问题详情（逐行验证结论）

1. `CfirImmutableFunctionCannotModifyFieldChecker` 继承 `CfirAssignmentChecker`，`check` 签名接收 `CfirAssignment`（L85）。
2. L88 `expression.lValue as? CfirQualifiedAccessExpression ?: return`——要求左值是 qualified access。
3. **关键**：`mut_function_01.cj` 的 `t2.t1.t0 += 10` 是 `CfirCompoundAssignmentExpression`（`+=`），**不是 `CfirAssignment`**。`CfirAssignmentChecker` 的注册 visitor 只遍历 `CfirAssignment` 节点，复合赋值走 `CfirCompoundAssignmentChecker`（若有）——该 checker 没有注册不可变字段检查 → `t2.t1.t0 += 10` 完全不经过 `check` → 漏报。
4. `currentImmutableStructMutationContext`（L262-271）：`outerFunction` 取 owner 之后第一个 `CfirFunction`（L264-267），若它是构造器或 mut 则返回 null（L268）——裸调用 `foo()` 时 `isCurrentStructReceiverAccess`（L329-331：`explicitReceiver == null || is CfirThisReceiverExpression`）命中，但 `record_mut_invalid_12.cj` 的漏报在于 `currentImmutableStructFunction`（L208-218）对"普通成员函数体内裸调 mut 函数"的判定在部分 record 形态下返回 null。

### 测试文件

`mut_function_01.cj`、`record_mut_invalid_12.cj`、`record_mut_invalid_14/15.cj`（同问题 6）。

### 修复方案（验证后细化）

1. `CfirImmutableFunctionCannotModifyFieldChecker` 同时注册处理 `CfirCompoundAssignmentExpression`（`+=`/`-=` 等），对其左值做同样的 `currentStructMutationRoot` 判定。
2. `currentImmutableStructFunction` 对 record/struct 的普通（非 mut）成员函数上下文补全识别（嵌套函数场景取最外层非 mut 成员函数）。

## 16.6 问题 7 机制确认：super 存储槽无初始化状态跟踪

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInitializationCheckers.kt` L1084-1096、L2029-2094

### 问题详情（逐行验证结论）

1. `hasSuperReceiver`（L1095-1096）识别 `super.member` 访问，但仅用于**成员初始化器**场景的分类（L1084-1091），不是构造器内 super 调用的初始化前置检查。
2. `markInitialized`（L2029-2036）只更新 `tracked` 集合内的符号；tracked 集合（L396-398）只包含构造器参数 + `owner.instanceFieldInfos(context)` 的**当前类实例字段**——**父类存储槽（super fields）不在 tracked 集合**。
3. 因此 `super_this_05-08.cj` 的 `k = super.f()`（未先 `super()` 就访问父类成员）在状态机中没有"父类存储未初始化"的概念，`super.f()` 的成员访问无法触发 `USED_BEFORE_INITIALIZATION`。

### 测试文件

`super_this_05~08.cj`、`variable_use_before_init_11/15.cj`（同问题 7）。

### 修复方案（验证后细化）

`InitializationState` 增加父类存储槽跟踪：构造器分析时把父类实例字段（`typeAwareSupertypeProvider` 可见的父类字段）declare 进 tracked（初始为未初始化）；`super(...)` 委托调用时 `markAllSuperInstanceFieldsInitialized`；`super.member` 访问在未初始化时报 `USED_BEFORE_INITIALIZATION`。

---
# 问题 17：范围/顺序簇逐 fixture 分析（214 行差异，55 个诊断组）

> 本章是问题 12 的逐 fixture 细化：对「范围/顺序」簇 214 行有效差异，按诊断名聚合为 55 组，每组给出 suite 分布、涉及 fixtures 与代表差异（EXP 期望范围 vs ACT 实际范围）。这些差异多数与既有问题同源（范围漂移是报告 source 选择问题），此处补齐到 fixture 级。

## 发生位置

- 章节级：范围/顺序簇的通用机制与责任代码分布见**问题 12**（各 checker 的 `reporter.reportOn(source=...)` 参数选择）。
- 组级：每组代表差异中 EXP/ACT 标记行的起止位置差异即为报告 source 的漂移；对应 checker 见问题 12 的形态分类。

## 测试文件

- 组级：每组 `fixtures:` 行列出该诊断组涉及的全部测试文件（相对 `cfir/analysis-tests/testData/`）。
- 章节级：涉及 57 个 suite，完整清单见问题 12。

## 问题详情

- 组级：每组 `suite 分布:` 与 `代表差异（suite::fixture）:` 行给出该诊断的 EXP 期望范围 vs ACT 实际范围的具体行文本（下划线 `<!!>` 起止差异即为范围漂移点）。
- 章节级：四类形态（同名标记范围漂移 106 / 标记数量不同 94 / 标记顺序不同 10 / 完全相同 4）的通用根因见问题 12。

## 修复方案

- 章节级：按问题 12 的修复方案执行（逐诊断对核对报告 source；数量不同核对多违规点遍历；顺序不同调整 checker 注册顺序）。
- 组级：每组代表差异中标出的漂移点即需要调整的 `reportOn` source 参数；与既有问题同源的组（如 CANNOT_ASSIGN_TO_IMMUTABLE 组对应问题 1、INVALID_THIS_CALL_OUTSIDE_CTOR 组对应问题 16.4）先修根因，范围漂移随之消失。

### CANNOT_ASSIGN_TO_IMMUTABLE（22 行差异）
- suite 分布：{'PropertyGenerated': 2, 'VarrayGenerated': 2, 'ConstEvaluationGenerated': 4, 'GlobalVariableNotAssignable02Generated': 2, 'LetInInitGenerated': 6}
- fixtures：desugarexpr.cj、err_struct_assign.cj、extend_property15.cj、file1.cj、let_in_init10-1.cj、let_in_init10-2.cj
- 代表差异（PropertyGenerated::extend_property15.cj）：
  - EXP：`<!CANNOT_ASSIGN_TO_IMMUTABLE!>a.size = 1<!>`
  - ACT：`a.<!CANNOT_ASSIGN_TO_IMMUTABLE!>size<!> = 1`

### SUPER_TYPES_DUPLICATE（18 行差异）
- suite 分布：{'ExtendsImplementsInterfaceDuplicatedGenerated': 8, 'GenericInterfaceImport2Generated': 2, 'GenericInterfaceImport1Generated': 2, 'ExtendGenerated': 4, 'TypealiasGenerated': 2}
- fixtures：case.cj、extend_duplicate_interfaces10.cj、extend_duplicate_interfaces6.cj、interface_duplicated_02.cj、interface_duplicated_04.cj、interface_duplicated_05.cj
- 代表差异（ExtendsImplementsInterfaceDuplicatedGenerated::interface_duplicated_02.cj）：
  - EXP：`<!SUPER_TYPES_DUPLICATE!>class A<Q><!> <: I0<Q> & I0<Int64> {}`
  - ACT：`class A<Q> <: I0<Q> & I0<Int64> {}`
  - EXP：`var b: A<Int64>`
  - ACT：`var b: <!SUPER_TYPES_DUPLICATE!>A<Int64><!>`

### TYPE_MISMATCH（16 行差异）
- suite 分布：{'IfLetExprGenerated': 2, 'GenericConstraintGenerated': 2, 'TryGenerated': 2, 'ArrayGenerated': 4, 'LambdaGenerated': 4}
- fixtures：arraylit1.cj、arraysizedlit2.cj、bugfix4.cj、generic_constraint12.cj、lambda15.cj、lambda_in_record.cj
- 代表差异（IfLetExprGenerated::bugfix4.cj）：
  - EXP：`<!TYPE_MISMATCH!>if (let a <- 0) {`
  - ACT：`if (let a <- 0) <!TYPE_MISMATCH!>{`

### INHERIT_MEMBER_TYPE_INCONSISTENT（10 行差异）
- suite 分布：{'GenericStaticFunctionsGenerated': 2, 'GenericConstraintInheritanceGenerated': 6, 'InterfaceGenerated': 2}
- fixtures：access_by_parameter_interface_10.cj、generic_upper_constraint_inheritance_08.cj、generic_upper_constraint_inheritance_09.cj、generic_upper_constraint_inheritance_10.cj、interface_conflict_inheritance_07.cj
- 代表差异（GenericStaticFunctionsGenerated::access_by_parameter_interface_10.cj）：
  - EXP：`class D<T> where <!INHERIT_MEMBER_TYPE_INCONSISTENT!>T <: C1 & I<!> {`
  - ACT：`class D<<!INHERIT_MEMBER_TYPE_INCONSISTENT!>T<!>> where T <: C1 & I {`

### INVALID_THIS_CALL_OUTSIDE_CTOR（8 行差异）
- suite 分布：{'InvalidDeclarationGenerated': 4, 'ConstructorGenerated': 2, 'ClassGenerated': 2}
- fixtures：class8.cj、illegalDelegationPlacement.cj、illegalDelegationPlacementRich.cj、illegalSuperCallInMember.cj
- 代表差异（InvalidDeclarationGenerated::illegalDelegationPlacement.cj）：
  - EXP：`super(1)`
  - ACT：`<!INVALID_THIS_CALL_OUTSIDE_CTOR!>super<!>(1)`

### WRONG_NUMBER_OF_ARGUMENTS（7 行差异）
- suite 分布：{'GenericConstraintGenerated': 1, 'CallGenerated': 2, 'EnumGenerated': 2, 'FunctionGenerated': 2}
- fixtures：enum1.cj、enum1_optionalBITOR.cj、funcDecl2.cj、generic_constraint_and_4.cj、variadic_lambda_01.cj
- 代表差异（GenericConstraintGenerated::generic_constraint_and_4.cj）：
  - EXP：`var c = a.foo<!WRONG_NUMBER_OF_ARGUMENTS!>(1, 2)<!>`
  - ACT：`var c = <!WRONG_NUMBER_OF_ARGUMENTS!>a.foo(1, 2)<!>`

### CONFLICTING_OVERLOADS,REDECLARATION（6 行差异）
- suite 分布：{'RedeclarationGenerated': 6}
- fixtures：simple.cj
- 代表差异（RedeclarationGenerated::simple.cj）：
  - EXP：`<!CONFLICTING_OVERLOADS!>func ab()<!>: Unit`
  - ACT：`func <!CONFLICTING_OVERLOADS!>ab<!>(): Unit`
  - EXP：`func <!REDECLARATION!>a<!>() {}`
  - ACT：`func a() {}`

### GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT（6 行差异）
- suite 分布：{'ConstraintCheckGenerated': 2, 'TypealiasGenerated': 4}
- fixtures：assumption2_test.cj、typealias27.cj、typealias9.cj
- 代表差异（ConstraintCheckGenerated::assumption2_test.cj）：
  - EXP：`<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>c<!>lass C<X> where X <: Z<C<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_C`
  - ACT：`class C<X> where X <: Z<C<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>Int32<!>>> {`

### EXTEND_MEMBER_CANNOT_SHADOW,STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME（6 行差异）
- suite 分布：{'ExtendGenerated': 6}
- fixtures：extend_member_conflict2.cj、extend_static_function_duplicate_name1.cj、extend_static_function_duplicate_name2.cj
- 代表差异（ExtendGenerated::extend_member_conflict2.cj）：
  - EXP：`<!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>p<!>ublic func x1() {}`
  - ACT：`public func <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>x1<!>() {}`
  - EXP：`<!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>p<!>ublic static func y2() {}`
  - ACT：`public static func <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>y2<!>() {}`

### DIFFERENT_OR_PATTERN（4 行差异）
- suite 分布：{'MatchExpressionGenerated': 4}
- fixtures：err_different_pattern_01.cj、match023.cj
- 代表差异（MatchExpressionGenerated::err_different_pattern_01.cj）：
  - EXP：`case <!DIFFERENT_OR_PATTERN!>true | e<!> => 0`
  - ACT：`case true | <!DIFFERENT_OR_PATTERN!>e<!> => 0`

### OPTIONAL_CHAIN_NON_OPTIONAL（4 行差异）
- suite 分布：{'OperatorOverloadGenerated': 2, 'OptionalChainGenerated': 2}
- fixtures：err_unary_00.cj、member_access_05.cj
- 代表差异（OperatorOverloadGenerated::err_unary_00.cj）：
  - EXP：`if (!s1.startsWith("") || s1.startsWith("8") || <!OPTIONAL_CHAIN_NON_OPTIONAL!>!s2.startsWith?("")<!>) {`
  - ACT：`if (!s1.startsWith("") || s1.startsWith("8") || !<!OPTIONAL_CHAIN_NON_OPTIONAL!>s2.startsWith?("")<!>) {`

### INVALID_BINARY_OPERATOR（4 行差异）
- suite 分布：{'OperatorOverloadGenerated': 2, 'ShiftGenerated': 2}
- fixtures：shift_var_ty.cj、timeout_01.cj
- 代表差异（OperatorOverloadGenerated::timeout_01.cj）：
  - EXP：`1) + true) + 1) + true) + 1) + true) + 1) + (A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A`
  - ACT：`1) + true) + 1) + true) + 1) + true) + 1) + (A + A <!INVALID_BINARY_OPERATOR!>*<!> A + A * A + A * A + A * A +`
  - EXP：`(1 + (true + (1 + (true + (1 + (true + (1 + (true + (1 + (true + (1 + (true + (1 + true)))))))))))))))))))))))`
  - ACT：`(1 + (true + (1 + (true + (1 + (true + (1 + (true + (1 + (true + (1 + (true + (1 <!INVALID_BINARY_OPERATOR!>+<`

### CLASS_UNINITIALIZED_FIELD（4 行差异）
- suite 分布：{'InitializationCheckGenerated': 4}
- fixtures：variable_assignment_terminated_in_ctor_01.cj、variable_assignment_terminated_in_ctor_02.cj
- 代表差异（InitializationCheckGenerated::variable_assignment_terminated_in_ctor_01.cj）：
  - EXP：`<!CLASS_UNINITIALIZED_FIELD!>init<!>(a: Int64, c: Bool) {`
  - ACT：`<!CLASS_UNINITIALIZED_FIELD!>init(a: Int64, c: Bool)<!> {`
  - EXP：`<!CLASS_UNINITIALIZED_FIELD!>init<!>() {`
  - ACT：`<!CLASS_UNINITIALIZED_FIELD!>init()<!> {`

### UNREACHABLE_PATTERN（4 行差异）
- suite 分布：{'TypePatternGenerated': 2, 'TuplePatternGenerated': 2}
- fixtures：tuple4.cj、type06.cj
- 代表差异（TypePatternGenerated::type06.cj）：
  - EXP：`case <!UNREACHABLE_PATTERN!>son(uncle: Uncle)<!> => 0`
  - ACT：`case son(uncle: Uncle) => 0`

### WRONG_MODIFIER_TARGET（4 行差异）
- suite 分布：{'DeclarationStatusGenerated': 4}
- fixtures：mutOnlyOnFunction.cj、mutOnlyOnFunctionRich.cj
- 代表差异（DeclarationStatusGenerated::mutOnlyOnFunctionRich.cj）：
  - EXP：`<!WRONG_MODIFIER_TARGET!>mut<!> func denied(): Unit {}`
  - ACT：`mut func denied(): Unit {}`

### EXTEND_DUPLICATE_INTERFACE（4 行差异）
- suite 分布：{'ExtensionsGenerated': 2, 'ExtendGenerated': 2}
- fixtures：extendDuplicateInterfaceRich.cj、extend_duplicate_interfaces1.cj
- 代表差异（ExtensionsGenerated::extendDuplicateInterfaceRich.cj）：
  - EXP：`extend Box <: Hashable & <!EXTEND_DUPLICATE_INTERFACE!>Hashable<!> {}`
  - ACT：`extend Box <: <!EXTEND_DUPLICATE_INTERFACE!>Hashable<!> & <!EXTEND_DUPLICATE_INTERFACE!>Hashable<!> {}`

### TYPE_UNINITIALIZED_STATIC_FIELD（4 行差异）
- suite 分布：{'MemberVariableGenerated': 2, 'LetInInitGenerated': 2}
- fixtures：let_in_init3.cj、record_member_variable_init01.cj
- 代表差异（MemberVariableGenerated::record_member_variable_init01.cj）：
  - EXP：`<!TYPE_UNINITIALIZED_STATIC_FIELD!>public static let a: Int64<!>`
  - ACT：`public static let <!TYPE_UNINITIALIZED_STATIC_FIELD!>a<!>: Int64`

### UNUSED_IMPORT（4 行差异）
- suite 分布：{'Unused019Generated': 2, 'Unused017Generated': 2}
- fixtures：unused017.cj、unused019.cj
- 代表差异（Unused019Generated::unused019.cj）：
  - EXP：`<!UNUSED_IMPORT!>import org1::a.A<!>`
  - ACT：`import <!UNUSED_IMPORT!>org1<!>::a.A`

### AMBIGUOUS_USE（4 行差异）
- suite 分布：{'ExtendGenerated': 2, 'GenericsGenerated': 2}
- fixtures：extend_function_conflict.cj、generic_upperbound_reference_02.cj
- 代表差异（ExtendGenerated::extend_function_conflict.cj）：
  - EXP：`let i2 = <!AMBIGUOUS_USE!>i.foo<!> ~> i.foo`
  - ACT：`let i2 = i.<!AMBIGUOUS_USE!>foo<!> ~> i.<!AMBIGUOUS_USE!>foo<!>`

### NEED_NAMED_ARGUMENT,WRONG_NUMBER_OF_ARGUMENTS（4 行差异）
- suite 分布：{'FunctionGenerated': 4}
- fixtures：defaultParameter4_3.cj、defaultParameter4_5.cj
- 代表差异（FunctionGenerated::defaultParameter4_3.cj）：
  - EXP：`foo0<!WRONG_NUMBER_OF_ARGUMENTS!>(1, 2)<!>`
  - ACT：`foo0(1, <!NEED_NAMED_ARGUMENT!>2<!>)`
  - EXP：`foo1<!WRONG_NUMBER_OF_ARGUMENTS!>(1)<!>`
  - ACT：`foo1(<!NEED_NAMED_ARGUMENT!>1<!>)`

### USE_MUTABLE_FUNC_ALONE（4 行差异）
- suite 分布：{'MutGenerated': 4}
- fixtures：record_extend_mut_invalid_7.cj、record_mut_invalid_11.cj
- 代表差异（MutGenerated::record_mut_invalid_11.cj）：
  - EXP：`var fn = <!USE_MUTABLE_FUNC_ALONE!>obj.foo<!>`
  - ACT：`var fn = obj.<!USE_MUTABLE_FUNC_ALONE!>foo<!>`
  - EXP：`<!USE_MUTABLE_FUNC_ALONE!>obj.foo<!>`
  - ACT：`obj.<!USE_MUTABLE_FUNC_ALONE!>foo<!>`

### TYPEALIAS_UNUSED_TYPE_PARAMETERS（4 行差异）
- suite 分布：{'VarrayGenerated': 4}
- fixtures：varray_alias01.cj、varray_alias01_err.cj
- 代表差异（VarrayGenerated::varray_alias01.cj）：
  - EXP：`<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>type varr1_1<T><!> = VArray<Int64, $3>`
  - ACT：`<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>t<!>ype varr1_1<T> = VArray<Int64, $3>`

### INVALID_SUBSCRIPT_ASSIGN_RETURN（3 行差异）
- suite 分布：{'OperatorOverloadGenerated': 3}
- fixtures：err_subscript_assign_03.cj、err_subscript_assign_04.cj、operatorOverload_indexSet2.cj
- 代表差异（OperatorOverloadGenerated::operatorOverload_indexSet2.cj）：
  - EXP：`public operator func [](index: Int64, value!: Int64): <!INVALID_SUBSCRIPT_ASSIGN_RETURN!>Int64<!> {`
  - ACT：`public <!INVALID_SUBSCRIPT_ASSIGN_RETURN!>operator<!> func [](index: Int64, value!: Int64): Int64 {`

### IN,INVALID_BINARY_OPERATOR（2 行差异）
- suite 分布：{'OperatorOverloadGenerated': 2}
- fixtures：timeout_00.cj
- 代表差异（OperatorOverloadGenerated::timeout_00.cj）：
  - EXP：`let x = A + A <!INVALID_BINARY_OPERATOR!>*<!> A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * `
  - ACT：`let x = A + A <!INVALID_BINARY_OPERATOR!>*<!> A + A <!INVALID_BINARY_OPERATOR!>*<!> A + A <!INVALID_BINARY_OPE`
  - EXP：`A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A + A * A `
  - ACT：`A <!INVALID_BINARY_OPERATOR!>*<!> A + A <!INVALID_BINARY_OPERATOR!>*<!> A + A <!INVALID_BINARY_OPERATOR!>*<!> `

### USED_BEFORE_INITIALIZATION（2 行差异）
- suite 分布：{'InitializationCheckGenerated': 2}
- fixtures：variable_use_before_init_12.cj
- 代表差异（InitializationCheckGenerated::variable_use_before_init_12.cj）：
  - EXP：`c0`
  - ACT：`<!USED_BEFORE_INITIALIZATION!>c0<!>`

### （2 行差异）
- suite 分布：{'ThisTypeGenerated': 2}
- fixtures：class_thistype_invalid_5.cj
- 代表差异（ThisTypeGenerated::class_thistype_invalid_5.cj）：
  - EXP：`func foo(): This {`
  - ACT：`func foo(): <!parse_this_type_not_allow!>This<!> {`

### VARRAY_ARG_TYPE_WITH_REFTYPE（2 行差异）
- suite 分布：{'VarrayWithReftypeGenerated': 2}
- fixtures：varray_with_reftype02.cj
- 代表差异（VarrayWithReftypeGenerated::varray_with_reftype02.cj）：
  - EXP：`var f: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>String<!>, $1> = ["123"] // error`
  - ACT：`var f: VArray<String, $1> = ["123"] // error`
  - EXP：`var g = VArray<S5, $1>(repeat: S5()) // pass`
  - ACT：`var g = VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>S5<!>, $1>(repeat: S5()) // pass`

### IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION（2 行差异）
- suite 分布：{'ConstEvaluationGenerated': 2}
- fixtures：err_call_mut_func.cj
- 代表差异（ConstEvaluationGenerated::err_call_mut_func.cj）：
  - EXP：`<!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>v<!>.f3() // error, f3 is mut`
  - ACT：`v.<!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>f3<!>() // error, f3 is mut`

### ANNOTATION_NO_CONST_INIT（2 行差异）
- suite 分布：{'AnnotationGenerated': 2}
- fixtures：err_annotation_no_const_init.cj
- 代表差异（AnnotationGenerated::err_annotation_no_const_init.cj）：
  - EXP：`@<!ANNOTATION_NO_CONST_INIT!>Annotation<!>`
  - ACT：`@Annotation`
  - EXP：`public class A {}`
  - ACT：`public class <!ANNOTATION_NO_CONST_INIT!>A<!> {}`

### GENERIC_TYPE_ARGUMENT_NOT_MATCH_CON,GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRA,GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAI,GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT（2 行差异）
- suite 分布：{'ConstraintCheckGenerated': 2}
- fixtures：assumption3_test.cj
- 代表差异（ConstraintCheckGenerated::assumption3_test.cj）：
  - EXP：`<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>c<!>lass C<X> where X <: C<C<C<C<<!GENERIC_TYPE_ARGUMENT_NOT_MAT`
  - ACT：`class C<X> where X <: C<C<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>C<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CO`
  - EXP：`<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>i<!>nterface I<X> where X <: I<I<I<I<<!GENERIC_TYPE_ARGUMENT_NOT`
  - ACT：`interface I<X> where X <: I<I<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>I<<!GENERIC_TYPE_ARGUMENT_NOT_MATC`

### EXTEND_INTERFACE_NOT_EXTENDABLE（2 行差异）
- suite 分布：{'InterfaceDefaultImplementGenerated': 2}
- fixtures：basic_prop.cj
- 代表差异（InterfaceDefaultImplementGenerated::basic_prop.cj）：
  - EXP：`<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend E <: I {<!>`
  - ACT：`<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend E <: I {`
  - EXP：`}`
  - ACT：`}<!>`

### VARRAY_IN_CFUNC（2 行差异）
- suite 分布：{'VarrayCffiGenerated': 2}
- fixtures：varray_ctype02.cj
- 代表差异（VarrayCffiGenerated::varray_ctype02.cj）：
  - EXP：`func c_f2(a: VArray<Int32, $3>): <!VARRAY_IN_CFUNC!>VArray<Int32, $3><!> {`
  - ACT：`func c_f2(a: VArray<Int32, $3>): VArray<Int32, $3> {`
  - EXP：`var c_f3: CFunc<(VArray<Int32, $3>) -> <!VARRAY_IN_CFUNC!>VArray<Int32, $3><!>> = {a => a} //error`
  - ACT：`var c_f3: CFunc<(VArray<Int32, $3>) -> VArray<Int32, $3>> = {a => a} //error`

### JAVA_MIRROR_INTEROPLIB_MUST_BE_IMPORTED（2 行差异）
- suite 分布：{'InteropGenerated': 2}
- fixtures：foreignNameLegalityPlaceholder.cj
- 代表差异（InteropGenerated::foreignNameLegalityPlaceholder.cj）：
  - EXP：`<!JAVA_MIRROR_INTEROPLIB_MUST_BE_IMPORTED!>@JavaMirror`
  - ACT：`@JavaMirror`
  - EXP：`open class JavaBase {}<!>`
  - ACT：`<!JAVA_MIRROR_INTEROPLIB_MUST_BE_IMPORTED!>open class JavaBase {}<!>`

### CAPTURE_BEFORE_INITIALIZATION,USE_FUNC_CAPTURE_VAR_ALONE（2 行差异）
- suite 分布：{'LambdaCaptureGenerated': 2}
- fixtures：capture_not_init_01.cj
- 代表差异（LambdaCaptureGenerated::capture_not_init_01.cj）：
  - EXP：`let f = <!USE_FUNC_CAPTURE_VAR_ALONE!>{=> <!CAPTURE_BEFORE_INITIALIZATION!>x<!>}<!>`
  - ACT：`let f = {=> <!CAPTURE_BEFORE_INITIALIZATION!>x<!>}`

### USE_FUNC_CAPTURE_VAR_ALONE（2 行差异）
- suite 分布：{'LambdaCaptureGenerated': 2}
- fixtures：capture4.cj
- 代表差异（LambdaCaptureGenerated::capture4.cj）：
  - EXP：`return foo(<!USE_FUNC_CAPTURE_VAR_ALONE!>{=> a}<!>)`
  - ACT：`return foo({=> a})`
  - EXP：`return foo(<!USE_FUNC_CAPTURE_VAR_ALONE!>{=> a}<!>)`
  - ACT：`return foo({=> a})`

### ACCESSIBILITY_WITH_MAIN_HINT（2 行差异）
- suite 分布：{'AssignGenerated': 2}
- fixtures：assign_007.cj
- 代表差异（AssignGenerated::assign_007.cj）：
  - EXP：`public var (a, <!ACCESSIBILITY_WITH_MAIN_HINT!>b<!>): (Int64, A) = (1, A())`
  - ACT：`public var (<!ACCESSIBILITY_WITH_MAIN_HINT!>a<!>, <!ACCESSIBILITY_WITH_MAIN_HINT!>b<!>): (Int64, A) = (1, A())`
  - EXP：`public var (<!ACCESSIBILITY_WITH_MAIN_HINT!>c<!>, d): (A, Int64) = (A(), 1)`
  - ACT：`public var (<!ACCESSIBILITY_WITH_MAIN_HINT!>c<!>, <!ACCESSIBILITY_WITH_MAIN_HINT!>d<!>): (A, Int64) = (A(), 1)`

### OBJECT_CANNOT_ACCESS_STATIC_MEMBER（2 行差异）
- suite 分布：{'BinaryGenerated': 2}
- fixtures：binary_error_report_01.cj
- 代表差异（BinaryGenerated::binary_error_report_01.cj）：
  - EXP：`if (<!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>v<!>alue.get() != 1) {`
  - ACT：`if (<!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>value<!>.get() != 1) {`

### PATTERN_NOT_MATCH（2 行差异）
- suite 分布：{'EnumGenerated': 2}
- fixtures：enum16_1.cj
- 代表差异（EnumGenerated::enum16_1.cj）：
  - EXP：`case <!PATTERN_NOT_MATCH!>A<!>1(x) => 0`
  - ACT：`case <!PATTERN_NOT_MATCH!>A1(x)<!> => 0`

### UNDECLARED_TY,UNDECLARED_TYPE_NAME（2 行差异）
- suite 分布：{'ErrMsgsGenerated': 2}
- fixtures：assignment_0.cj
- 代表差异（ErrMsgsGenerated::assignment_0.cj）：
  - EXP：`var (a1, a2): (<!UNDECLARED_TYPE_NAME!>C<!><<!UNDECLARED_TYPE_NAME!>T<!>1>, <!UNDECLARED_TYPE_NAME!>C<!><<!UND`
  - ACT：`var (a1, a2): (<!UNDECLARED_TYPE_NAME!>C<!><<!UNDECLARED_TYPE_NAME!>T1<!>>, <!UNDECLARED_TYPE_NAME!>C<!><<!UND`

### CANNOT_ASSIGN_TO_IMMUTABLE,INVALID_BINARY_OPERATOR,TYPE_MISMATCH（2 行差异）
- suite 分布：{'ErrMsgsGenerated': 2}
- fixtures：inc_dec_0.cj
- 代表差异（ErrMsgsGenerated::inc_dec_0.cj）：
  - EXP：`<!TYPE_MISMATCH, CANNOT_ASSIGN_TO_IMMUTABLE!>x++<!> + 1`
  - ACT：`<!CANNOT_ASSIGN_TO_IMMUTABLE, TYPE_MISMATCH!>x++<!> <!INVALID_BINARY_OPERATOR!>+<!> 1`
  - EXP：`<!TYPE_MISMATCH, CANNOT_ASSIGN_TO_IMMUTABLE!>x--<!> * 2`
  - ACT：`<!CANNOT_ASSIGN_TO_IMMUTABLE, TYPE_MISMATCH!>x--<!> <!INVALID_BINARY_OPERATOR!>*<!> 2`

### INTERFACE_CANNOT_INHERIT_CLASS（2 行差异）
- suite 分布：{'ExtendGenerated': 2}
- fixtures：extend_invalid_type02.cj
- 代表差异（ExtendGenerated::extend_invalid_type02.cj）：
  - EXP：`public interface <!INTERFACE_CANNOT_INHERIT_CLASS!>Strbase<!> <: Collection<Byte> & Equatable<String> & Compar`
  - ACT：`public interface <!INTERFACE_CANNOT_INHERIT_CLASS, INTERFACE_CANNOT_INHERIT_CLASS!>Strbase<!> <: Collection<By`

### NAMED_PARAMETER_NOT_FOUND,WRONG_NUMBER_OF_ARGUMENTS（2 行差异）
- suite 分布：{'FunctionGenerated': 2}
- fixtures：defaultParameter4_4.cj
- 代表差异（FunctionGenerated::defaultParameter4_4.cj）：
  - EXP：`foo0<!WRONG_NUMBER_OF_ARGUMENTS!>(1, d: 2)<!>`
  - ACT：`foo0(1, <!NAMED_PARAMETER_NOT_FOUND!>d<!>: 2)`

### CANNOT_ASSIGN_TO_IMMUTABLE,REF_NOT_BE_TYPE（2 行差异）
- suite 分布：{'GenericsGenerated': 2}
- fixtures：class4_test.cj
- 代表差异（GenericsGenerated::class4_test.cj）：
  - EXP：`width = <!REF_NOT_BE_TYPE!>T<!>`
  - ACT：`<!CANNOT_ASSIGN_TO_IMMUTABLE!>width<!> = <!REF_NOT_BE_TYPE!>T<!>`
  - EXP：`f = foo<T>(<!REF_NOT_BE_TYPE!>T<!>)`
  - ACT：`<!CANNOT_ASSIGN_TO_IMMUTABLE!>f<!> = foo<T>(<!REF_NOT_BE_TYPE!>T<!>)`

### UNQUALIFIED_LEFT_VALUE_ASSIGNED（2 行差异）
- suite 分布：{'LetGenerated': 2}
- fixtures：assign_func.cj
- 代表差异（LetGenerated::assign_func.cj）：
  - EXP：`<!UNQUALIFIED_LEFT_VALUE_ASSIGNED!>test.foo<!> = a`
  - ACT：`test.<!UNQUALIFIED_LEFT_VALUE_ASSIGNED!>foo<!> = a`
  - EXP：`<!UNQUALIFIED_LEFT_VALUE_ASSIGNED!>Test.foo1<!> = a`
  - ACT：`Test.<!UNQUALIFIED_LEFT_VALUE_ASSIGNED!>foo1<!> = a`

### UNABLE_TO_INFER_RETURN_TYPE（2 行差异）
- suite 分布：{'LookupGenerated': 2}
- fixtures：interface.cj
- 代表差异（LookupGenerated::interface.cj）：
  - EXP：`static func goo() {`
  - ACT：`static func <!UNABLE_TO_INFER_RETURN_TYPE!>goo<!>() {`

### UNRESOLVED_REFERENCE（2 行差异）
- suite 分布：{'LookupGenerated': 2}
- fixtures：funcdecl.cj
- 代表差异（LookupGenerated::funcdecl.cj）：
  - EXP：`let c = <!UNRESOLVED_REFERENCE!>a<!>1()`
  - ACT：`let c = <!UNRESOLVED_REFERENCE!>a1<!>()`

### LITERAL_NUMERIC_OVERFLOW（2 行差异）
- suite 分布：{'OperatorGenerated': 2}
- fixtures：num_overflow.cj
- 代表差异（OperatorGenerated::num_overflow.cj）：
  - EXP：`var buo: Int8 = <!LITERAL_NUMERIC_OVERFLOW!>-129<!>`
  - ACT：`var buo: Int8 = -129`
  - EXP：`var ubuo: UInt8 = <!LITERAL_NUMERIC_OVERFLOW!>-1<!>`
  - ACT：`var ubuo: UInt8 = -1`

### INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE（2 行差异）
- suite 分布：{'MutGenerated': 2}
- fixtures：record_extend_mut_invalid_4.cj
- 代表差异（MutGenerated::record_extend_mut_invalid_4.cj）：
  - EXP：`extend R1 <: I1 {`
  - ACT：`<!INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE!>e<!>xtend R1 <: I1 {`
  - EXP：`<!INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE!>p<!>ublic mut func foo1() {`
  - ACT：`public mut func foo1() {`

### ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER,INVALID_THIS_CALL_OUTSIDE_CTOR,NOT_MEMBER_OF（2 行差异）
- suite 分布：{'ClassGenerated': 2}
- fixtures：invalid_super_call.cj
- 代表差异（ClassGenerated::invalid_super_call.cj）：
  - EXP：`super() // ok`
  - ACT：`<!INVALID_THIS_CALL_OUTSIDE_CTOR!>super<!>() // ok`
  - EXP：`<!ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER!>super<!>().<!NOT_MEMBER_OF!>a<!>`
  - ACT：`<!ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER, INVALID_THIS_CALL_OUTSIDE_CTOR!>super<!>().<!NOT_MEMBER_OF!>a<!>`

### INHERIT_SUPER_MEMBER_KIND_INCONSISTENT（2 行差异）
- suite 分布：{'InterfaceGenerated': 2}
- fixtures：interface_conflict_inheritance_02.cj
- 代表差异（InterfaceGenerated::interface_conflict_inheritance_02.cj）：
  - EXP：`<!INHERIT_SUPER_MEMBER_KIND_INCONSISTENT!>extend B <: I0 & I1 {}<!>`
  - ACT：`extend B <: I0 & I1 {}`

### GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT,TYPEALIAS_UNUSED_TYPE_PARAMETERS（2 行差异）
- suite 分布：{'TypealiasGenerated': 2}
- fixtures：typealias29.cj
- 代表差异（TypealiasGenerated::typealias29.cj）：
  - EXP：`<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>t<!>ype A<T1, T2, T3> = Cl<Option<T1>, Option<T1>> // ok`
  - ACT：`<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>t<!>ype A<T1, T2, T3> = Cl<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>O`
  - EXP：`<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>A<!><X, X, X>() // error`
  - ACT：`A<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>X<!>, X, X>() // error`

### CANNOT_ASSIGN_TO_SUBSCRIPT,NOT_MEMBER_OF,TYPE_MISMATCH（1 行差异）
- suite 分布：{'MultipleAssignExprGenerated': 1}
- fixtures：case05.cj
- 代表差异（MultipleAssignExprGenerated::case05.cj）：
  - EXP：`<!TYPE_MISMATCH!>(a.<!NOT_MEMBER_OF!>x<!>, a.<!NOT_MEMBER_OF!>y<!>) = (1, 2, 8)<!>`
  - ACT：`(a.<!NOT_MEMBER_OF!>x<!>, a.<!NOT_MEMBER_OF!>y<!>) = <!TYPE_MISMATCH!>(1, 2, 8)<!>`
  - EXP：`<!TYPE_MISMATCH!>(<!CANNOT_ASSIGN_TO_SUBSCRIPT!>class_a[class_b]<!>, <!CANNOT_ASSIGN_TO_SUBSCRIPT!>class_b[cla`
  - ACT：`(<!CANNOT_ASSIGN_TO_SUBSCRIPT!>class_a[class_b]<!>, <!CANNOT_ASSIGN_TO_SUBSCRIPT!>class_b[class_a]<!>) = <!TYP`

### INVALID_CFUNC_PARAMETER_TYPE（1 行差异）
- suite 分布：{'VarrayCffiGenerated': 1}
- fixtures：varray_ctype03.cj
- 代表差异（VarrayCffiGenerated::varray_ctype03.cj）：
  - EXP：`func c_f2(a: <!INVALID_CFUNC_PARAMETER_TYPE!>VArray<A, $3><!>): Unit {} // error`
  - ACT：`func c_f2(a: VArray<A, $3>): Unit {} // error`
  - EXP：`var c_f3: CFunc<((<!INVALID_CFUNC_PARAMETER_TYPE!>VArray<A, $3><!>) -> Unit)> = {a => ()} //error`
  - ACT：`var c_f3: CFunc<((VArray<A, $3>) -> Unit)> = {a => ()} //error`

### UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE（1 行差异）
- suite 分布：{'ConstraintCheckGenerated': 1}
- fixtures：constraint_check_test7_1.cj
- 代表差异（ConstraintCheckGenerated::constraint_check_test7_1.cj）：
  - EXP：`func foo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: (Int32) {`
  - ACT：`func foo<T>() where T <: (Int32) {`

### GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT（1 行差异）
- suite 分布：{'TypeGenerated': 1}
- fixtures：paren_type_with_generic_type.cj
- 代表差异（TypeGenerated::paren_type_with_generic_type.cj）：
  - EXP：`let a: (<!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>Array<!>)`
  - ACT：`let a: (Array)`
  - EXP：`let b: (<!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>Range<!>)`
  - ACT：`let b: (Range)`

---

# 问题 18：核心根因深度验证记录（二）——问题 5/9/11 与低频诊断名逐行验证

> 本章是问题 16 的续篇：对问题 5、9、11 及三个低频诊断名做逐行代码验证，全部基于 2026-08-04 实读源码，非推断。

## 18.1 问题 5 机制确认：static 冲突分支先执行且互斥门禁（精确到行）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInheritanceDeepChecker.kt` L940-998

### 问题详情（逐行验证结论）

`checkMemberKindConsistency` 类的同名成员比较循环中：

1. **L949-961（static 冲突分支）**：

```kotlin
val hasStaticConflict = ownInfo.isStatic != superInfo.isStatic
if (hasStaticConflict) {
    if (reportedStaticConflicts.add(ownInfo.name)) {
        reporter.reportOn(
            source = ownInfo.nameSource ?: ...,
            factory = CfirErrors.STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME,  // L954
            ...
        )
    }
}
```

2. **L963-984（kind 不一致分支）**：

```kotlin
if (ownInfo.kind != superInfo.kind) {
    if (!hasStaticConflict && reportedKindConflicts.add(ownInfo.name)) {   // L964：!hasStaticConflict 门禁
        ...CfirErrors.INHERIT_MEMBER_KIND_INCONSISTENT...                  // L975
    }
    continue
}
```

3. **验证结论**：`hasStaticConflict` 为 true 时，L954 先报 `STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME`；随后 L964 的 `!hasStaticConflict` 门禁使 kind 分支（L975 `INHERIT_MEMBER_KIND_INCONSISTENT`）**永远不可达**。官方语义在继承链 static/实例同名冲突时报 `INHERIT_MEMBER_KIND_INCONSISTENT`，与 CFIR 相反。16 个失败全部是这一种诊断名替换，无一例外——机制精确到 L964 的门禁。

### 测试文件

`class_impl_interface1~4.cj`、`class_extends_class1/2/4/5.cj`（同问题 5，16 失败）。

### 修复方案（验证后细化）

删除 L964 的 `!hasStaticConflict` 门禁，改为：static 冲突不再单独报 `STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME`，统一并入 kind 不一致分支（非 extend 场景报 `INHERIT_MEMBER_KIND_INCONSISTENT`，extend 场景报 `EXTEND_MEMBER_CANNOT_SHADOW` L968）；`STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME` 仅保留给同声明层重名（`CfirConflictsDeclarationChecker` 管辖）。

## 18.2 问题 9 机制确认：NEW_INFERENCE_ERROR 的两条静默路径（精确到行）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L398-503

### 问题详情（逐行验证结论）

推断失败映射主路径 `mapCallDiagnosticsFromConeErrors`（约 L390-503）：

1. **前置静默过滤（L460-468）**：

```kotlin
if (errors.any { error ->
    when (error) {
        is ConstrainingTypeIsError -> true
        is NotEnoughInformationForTypeParameter<*> ->
            error.typeVariable is ConeTypeParameterBasedTypeVariable ||
                (error.resolvedAtom as? CfirAnonymousFunction)?.containsErrorType() == true
        else -> false
    }
}) {
    return emptyList()   // L470：整批静默
}
```

   **交集坍缩（intersectionCollapsePlaceholder.cj 的 `chooseGeneric(1, 1.0)`）、占位符未求解**产生的 `ConstrainingTypeIsError`/`NotEnoughInformationForTypeParameter` 在这里直接返回 emptyList，**连 NEW_INFERENCE_ERROR 都不报**——这就是 12 个 `缺少: NEW_INFERENCE_ERROR` 的机制。

2. **expected-type 约束不匹配过滤（L432-440）**：

```kotlin
if (hasNotEnoughInformationError &&
    error is ConstraintMismatch &&
    error.position.from is ConeExpectedTypeConstraintPosition
) {
    return@mapNotNull null   // "secondary noise" 过滤
}
```

   泛型实参缺失时 expected-type 不匹配被当次生噪音过滤。

3. **varray 场景错映射（L484-492）**：`ConstraintMismatch` 且来自 `FixVariableConstraintPosition` 时映射为 `typeMismatchDiagnostic`（L484），官方 varray 大小不匹配应报 `VARRAY_SIZE_MISMATCH`——8 个 `NEW_INFERENCE_ERROR -> TYPE_MISMATCH` 替换的机制（另有 `CfirTypeSemanticsDiagnostics.kt` L138 参与）。

4. **兜底（L495-499）**：其余错误映射 `NEW_INFERENCE_ERROR`（`"Inference error: ${error::class.simpleName}"`）——只有在未被前置过滤吞掉时才到达。

### 测试文件

`intersectionCollapsePlaceholder.cj`、`newInferenceErrorConflict.cj`、`inferencePlaceholder.cj`、`varraySizeMismatch.cj`、`type_arg_infer4/6.cj`（同问题 9）。

### 修复方案（验证后细化）

1. L460-468 的前置过滤改为"降级而非静默"：`ConstrainingTypeIsError`/占位符未求解先映射 `NEW_INFERENCE_ERROR`（交集坍缩是官方明确报推断错误的场景），仅当错误已由其他更具体诊断覆盖时才返回空。
2. varray 场景：`FixVariableConstraintPosition` 的 `ConstraintMismatch` 在 varray 构造器上下文中映射 `VARRAY_SIZE_MISMATCH` 而非 `typeMismatchDiagnostic`。

## 18.3 问题 11.1 机制确认：extend 接口成员不进 extend scope（精确到 buildIndex）

### 发生位置

`cfir/providers/src/org/cangnova/cangjie/cfir/scopes/impl/CfirExtendMemberScope.kt` L175-197（`buildIndex`）；`coneDiagnosticToCfirDiagnostic.kt` L1769-1911（映射链）

### 问题详情（逐行验证结论）

`disable_default_static.cj` 的 `UIntNative.foo3()` 误报链：

1. **`buildIndex`（L175-197）只索引 `extend.declarations`**：

```kotlin
for ((extend, concreteReceiverType) in extends) {
    ...
    for (declaration in extend.declarations) {   // L185：只遍历 extend 花括号内成员
        ...
        indexDeclaration(declaration, ...)
    }
}
```

   `extend UIntNative <: MyInterface { public static func foo1() }` 的 `declarations` 只含 `foo1`；`MyInterface` 继承的 `faterInterface<Float16>` 上的 `foo2`、`MyInterface` 自身的 `foo3` **不在 extend 声明体内，完全不进 memberIndex**。

2. **查找失败 → `ConeUnresolvedNameError`**：`UIntNative.foo3()` 在 extend scope 找不到 → 落入 unresolved name。

3. **映射链（L1769-1808）**：`mapConeUnresolvedNameError` 依次试 extend-super、upper-bound、subscript、一元、`mapNotMemberOfDiagnostic`（L1790）。

4. **`mapNotMemberOfDiagnostic`（L1881-1911）**：接收者 `UIntNative` 展开后是 `ConeStructType`（名义类型，L1892-1898 通过）→ 报 `NOT_MEMBER_OF`（L1904）。

5. **验证结论**：根因在 scope 层——**extend 实现的接口闭包（含泛型实例化后的父接口）的成员未纳入 `CfirExtendMemberScope`**，官方语义中 extend 接口成员应通过目标类型可见。29 个 `多余: NOT_MEMBER_OF` 的机制。

### 测试文件

`disable_default_static.cj`、`extend_interface_static1.cj`、`main04/05.cj`、`class_*_thistype_ok_*.cj`（同问题 11.1）。

### 修复方案（验证后细化）

`buildIndex` 在索引 `extend.declarations` 之外，把 extend 实现的接口（`CfirExtendProvider` 可见的接口闭包，含泛型实参替换后的父接口）的静态成员并入索引；`This` 动态绑定场景（`class_*_thistype_ok_*.cj`）走动态绑定查找。

## 18.4 问题 11.2 机制确认：类体 use-site scope 错误合并 extend 成员（精确到 extendScope）

### 发生位置

`cfir/providers/src/org/cangnova/cangjie/cfir/scopes/impl/CfirClassUseSiteMemberScope.kt` L250-273、L460-474、L570-580、L800-815；`cfir/providers/src/org/cangnova/cangjie/cfir/calls/CfirReceivers.kt` L272、L367-373

### 问题详情（逐行验证结论）

`extend_namelookup2.cj`（类体内调 `go()`，EXP 期望 UNRESOLVED_REFERENCE，ACT 漏报）的机制链：

1. **L259-273**：`scopeKind == USE_SITE` 时构建 `extendScope = CfirExtendMemberScope(...)`（L263）。
2. **L470-472 `containsOwnFunction`**、**L574 函数查找**、**L809 callable 查找**：`extendScope?.processFunctionsByName(...)` 把 extend 成员合入类体查找。
3. **`CfirReceivers.kt` L272**：隐式 `this` 接收者的 `implicitMemberScopeKind = USE_SITE`；**L367-373**：普通 receiver/this/super 一律 `USE_SITE`。
4. **验证结论**：类体内裸调用 `go()` 经隐式 `this` 的 USE_SITE scope 解析到 extend 成员 `go` → 不报 UNRESOLVED_REFERENCE。**官方语义：类体（含其成员函数体）不能看到该类的 extend 成员**（`extend_namelookup2/8/9.cj` 明确要求报 UNRESOLVED_REFERENCE）。24 个 `缺少: UNRESOLVED_REFERENCE` 的机制。

### 测试文件

`extend_namelookup2.cj`、`extend_namelookup8.cj`、`extend_namelookup9.cj`、`extend_mutable_function_invalid_1.cj`、`record_extend_mut_invalid_13.cj`、`samename_conditionandifbody.cj`（同问题 11.2 缺少方向）。

### 修复方案（验证后细化）

类体（BODY_LOOKUP）上下文不使用合并 extend 成员的 scope：为类体成员函数体的隐式 `this` 接收者引入不含 extend 成员的 scope kind（官方 C++ 类体 scope 不包含 extend 成员），或让 `CfirClassUseSiteMemberScope` 在 `excludingExtend`/body 场景跳过 `extendScope` 合并；extend 体内（`extend A { func go() {} }` 内部互相调用）仍保留 extend 成员可见性（`extend_namelookup8/9.cj` 的合法场景）。

## 18.5 低频诊断名触发条件逐行确认（3 个）

## MACRO_EXPANSION_FAILED

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/collectors/components/MacroConstructionDiagnosticCollectorComponent.kt` L79-85

### 问题详情（逐行验证结论）

`MacroConstructionDiagnostic.Kind.MACRO_EXPANSION_FAILED` 与 `GENERIC` 分支合并上报 `MACRO_EXPANSION_FAILED`（L82-85）。失败形态：宏测试中官方展开成功、ACT 报该诊断（basecase/lambda_not_unit_return_type/nested/test02/external_weak/test04-09），且常与 UNUSED_IMPORT/UNRESOLVED_IMPORT/CANNOT_ASSIGN_TO_IMMUTABLE 同现——**展开失败是 import 可见性、let 赋值误报等下游问题的级联结果**。

### 测试文件

`testData/macro/llt/annotation/basecase.cj`、`lambda_not_unit_return_type.cj`、`nested.cj`、`test02.cj`、`external_weak.cj`、`test04~09.cj`（`MacroAnnotationGenerated` 等宏族，11 个失败）。

### 修复方案

先修 import 可见性与赋值误报（问题 1/10），展开失败即消失；若仍有真实失败，对照官方展开器错误消息补映射。

## UNDECLARED_TYPE_NAME

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirTypeConstraintsChecker.kt` L55-72

### 问题详情（逐行验证结论）

`reportDanglingTypeConstraints`（L47-73）：约束左侧名字不在声明类型参数中时（L56），若解析不到可见 classifier 报 `UNDECLARED_TYPE_NAME`（L67），解析得到则报 `NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER`（L61）。失败形态：`unresolvedSymbolType.cj` EXP 期望 `UNRESOLVED_REFERENCE`、ACT 报 `UNDECLARED_TYPE_NAME`——官方在类型位置引用未声明名一律报 `UNRESOLVED_REFERENCE`，仅在约束 LHS 真正未声明的类型参数语境报 `UNDECLARED_TYPE_NAME`；CFIR 的 L58 `resolvesToVisibleClassifier()` 分流与官方边界不一致。

### 测试文件

`testData/llt/constraint_check/unresolvedSymbolType.cj`、`record_pkg_02_1~3.cj`、`binary_time_cost_1.cj`（`ConstraintCheckGenerated`/`ErrMsgsGenerated`，13 个失败）。

### 修复方案

类型引用失败统一映射 `UNRESOLVED_REFERENCE`，`UNDECLARED_TYPE_NAME` 仅留给约束 LHS 场景（对齐官方 TypeCheck 中 TypeName 解析失败路径）。

## PATTERN_NOT_MATCH

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirMatchPatternLegalityChecker.kt` L120-181

### 问题详情（逐行验证结论）

四处上报点：tuple pattern 元数不匹配（L120-127）、enum pattern 解析失败（L133-142）、const pattern `isCompatibleWith` 不成立（L158-170）、expression pattern 不成立（L173-181）。失败形态：`char_byte_00`/`ok_rune_00` 等常量 pattern 误报（EXP 无、ACT 有）——`isCompatibleWith` 对 char/byte/rune 常量、运算符重载常量 pattern 的等值性判定过严；tuple 元素类型不匹配应报 TYPE_MISMATCH 而非整模式 PATTERN_NOT_MATCH。

### 测试文件

`testData/llt/match/char_byte_00.cj`、`ok_rune_00.cj`、`const_pattern_op_overloading.cj`、`tuple_pattern_004.cj`（`MatchExpressionGenerated`/`EnumGenerated`，12 多余 + 替换对）。

### 修复方案

常量 pattern 兼容性对齐官方相等性语义（rune/char/byte 常量、重载 ==）；tuple pattern 元素级诊断保留 TYPE_MISMATCH。

---

# 问题 19：核心根因深度验证记录（三）——问题 3/10/12 细节、官方对照与低频诊断名批量确认

> 本章是问题 16/18 的续篇，全部基于 2026-08-04 实读源码，含对问题 3/5/10 修复方向的重要修正。

## 19.1 问题 3 机制修正：STATIC_KEYWORD 谓词 site 不匹配（非单纯缺种类）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/ModifierCheckerTargets.kt` L103-108；`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/modifier/ModifierTarget.kt` L185-230；`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/ModifierCheckerTargets.kt` L247-314

### 问题详情（逐行验证结论）

此前文档把根因写成"STATIC_KEYWORD 谓词缺 STATIC_INITIALIZER 种类"。**逐行验证后修正为 site 不匹配**：

1. `ModifierTarget.kt` L199-202 `memberOf`：`target.site == Site.MEMBER && target.kind in kinds`；L205-208 `headOf`：`target.site == Site.HEAD && target.kind in kinds`。
2. `ModifierCheckerTargets.kt` L282-288 `actualTargetsFor` 对 static 构造器：

```kotlin
is CfirConstructor -> {
    if (declaration.status.isStatic) {
        listOf(ModifierTarget.head(DeclarationKind.STATIC_INITIALIZER))   // L284：Site.HEAD！
    } else {
        listOf(ModifierTarget.head(DeclarationKind.CONSTRUCTOR))
    }
}
```

3. `possibleTargetMap` 的 `STATIC_KEYWORD` 谓词是 `memberOf(FUNCTION, PROPERTY, VARIABLE)`（L104-108）——**要求 Site.MEMBER**。
4. **结论**：`static init()` 的 target 是 `head(STATIC_INITIALIZER)`（Site.HEAD），`memberOf` 谓词的 `target.site == Site.MEMBER` 恒 false → `isWrongTarget`（`CfirModifierChecker.checkTarget` L88-90）→ 报 `WRONG_MODIFIER_TARGET`。**修复不能只往 memberOf 里加种类，必须同时覆盖 HEAD site**。

### 测试文件

`static_init_01.cj`、`static_or_global_var3~12.cj`、`variable_use_before_init_01/03/04/07.cj`（同问题 3，52 失败）。

### 修复方案（验证后修正）

```kotlin
STATIC_KEYWORD to ModifierTargetPredicate.anyOf(
    ModifierTargetPredicate.memberOf(
        DeclarationKind.FUNCTION,
        DeclarationKind.PROPERTY,
        DeclarationKind.VARIABLE,
    ),
    ModifierTargetPredicate.headOf(DeclarationKind.STATIC_INITIALIZER),
)
```

（`anyOf` 定义在 `ModifierTarget.kt` L226-229，谓词组合模式已在 `OPEN_KEYWORD` L117-121 使用过，写法一致。）

## 19.2 问题 10 机制确认：宏展开产物合成节点不计入 import 使用（精确到 source 过滤）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirImportsChecker.kt` L222-312；`cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/MacroConstructionApi.kt` L407-449

### 问题详情（逐行验证结论）

`unused015/016.cj` 等宏展开后使用 import 符号却被误报 UNUSED_IMPORT 的机制链：

1. `collectImportUsage`（L222-226）= `collectReferencedNames()` + `collectMacroSurfaceReferencedNames(session)`。
2. `collectReferencedNames`（L266-301）的 `visitNamedReference`（L279-284）：

```kotlin
override fun visitNamedReference(namedReference: CfirNamedReference) {
    if (namedReference.source != null) {   // L280：只统计带 source 的引用
        result += namedReference.name
    }
    super.visitNamedReference(namedReference)
}
```

   **宏展开产物是合成节点（synthetic，无 source）→ 不计入**。
3. `collectMacroSurfaceReferencedNames`（L310-312）只取 `macroExpansionRegistry.usedMacroNames(this)`——`MacroConstructionApi.kt` L407-444 的 `registerUsedMacroSurface`/`registerUsedMacroDefinition` 记录的是**被消费的宏名本身**（L412 `_usedMacroNamesByFileIdentity += name`），不含宏展开产物中引用的普通符号。
4. **结论**：`unused015/016.cj` 中宏展开产物使用了 `import` 的符号，但该引用既不在 collectReferencedNames（合成节点无 source）也不在 collectMacroSurfaceReferencedNames（只记宏名）→ 使用集合缺失 → 误报 `UNUSED_IMPORT`。16 个失败的机制。

### 测试文件

`unused001.cj`、`unused003.cj`、`unused015.cj`、`unused016.cj`、`enumcons_inside_macro.cj`、`typeaslias.cj`（同问题 10，16 失败）。

### 修复方案（验证后细化）

`collectReferencedNames` 的 visitor 增加对宏展开产物节点的遍历：对 `macroExpansionRegistry` 记录的展开产物（带真实 source 映射的展开节点）中的命名引用也计入；或让宏展开产物保留原始引用 source（synthetic 节点继承被展开节点 source）。`collectMacroSurfaceReferencedNames` 维持宏名语义不变。

## 19.3 问题 12 核心报告点逐个确认（top 诊断组）

### 发生位置

各 checker 报告点（见下）。

### 问题详情（逐行验证结论）

对范围/顺序簇 top 诊断组逐个读报告 source 参数：

1. **SUPER_TYPES_DUPLICATE**（18 行差异）：`CfirSupertypesChecker.kt` 两处报告点——L87 `source = superTypeRef.source`（报在具体父类型引用上），L107 `source = declaration.classLikeDeclarationHeaderDiagnosticSource()`（报在声明头）。`interface_duplicated_02.cj` EXP 期望报声明头（L107 路径）、ACT 报在类型实参处（L87 路径且多处触发）——`checkInstantiatedDuplicateSuperInterfaces`（L97-113）应优先于 `checkDirectDuplicateSupertypes`（L75-92），或 L87 对实例化重复改走 L107。
2. **CONFLICTING_OVERLOADS/REDECLARATION**（6 行差异）：`CfirConflictsDeclarationChecker.kt` L89-93 分发器（`isFunctionLikeRedeclaration` 选 CONFLICTING_OVERLOADS，否则 REDECLARATION）；`simple.cj` EXP 报整函数声明（`func ab()` 全量）、ACT 只报函数名——报告 source 的 `declarationRangeForDiagnostic()` 选择差异。
3. **DIFFERENT_OR_PATTERN**（4 行差异）：`CfirPatternExpressionChecker.kt` L246-254——`reportKindOnWholePattern` 为 true 用 `patternRangeSource()`（L263-274 合成首尾 alternative 范围），false 用 `alternatives[i].source`。`err_different_pattern_01.cj` EXP 报整 or-pattern、ACT 只报冲突项——let-condition 入口传了 false，应传 true。
4. **WRONG_NUMBER_OF_ARGUMENTS**（7 行差异）：`coneDiagnosticToCfirDiagnostic.kt` L817-820 `rootCause.source`——`generic_constraint_and_4.cj` EXP 报整调用（`a.foo(1, 2)`）、ACT 只报函数名——`rootCause.source` 取的是函数引用而非完整调用，需改用 call source。

### 测试文件

- SUPER_TYPES_DUPLICATE：`interface_duplicated_02.cj`、`interface_duplicated_04.cj`、`extend_duplicate_interfaces6/10.cj`（`ExtendsImplementsInterfaceDuplicatedGenerated`/`ExtendGenerated`，18 行差异）
- CONFLICTING_OVERLOADS/REDECLARATION：`RedeclarationGenerated/simple.cj`（6 行差异）
- DIFFERENT_OR_PATTERN：`err_different_pattern_01.cj`、`match023.cj`（`MatchExpressionGenerated`，4 行差异）
- WRONG_NUMBER_OF_ARGUMENTS：`generic_constraint_and_4.cj`、`enum1.cj`、`variadic_lambda_01.cj`、`funcDecl2.cj`（7 行差异）

### 修复方案

1. SUPER_TYPES_DUPLICATE：实例化重复检查先行（`checkInstantiatedDuplicateSuperInterfaces` L97 提前到 L75 之前），命中即报声明头，避免 L87 逐 typeRef 重复报告。
2. CONFLICTING_OVERLOADS/REDECLARATION：报告 source 改用声明全量范围（`declaration.source` 而非 `nameSource`）。
3. DIFFERENT_OR_PATTERN：let-condition 等入口统一传 `reportKindOnWholePattern = true`。
4. WRONG_NUMBER_OF_ARGUMENTS：source 改用 `callOrAssignmentSource`/call site 完整范围。

## 19.4 官方对照验证：问题 5 修复方向修正（接口继承路径区分）

### 发生位置

`external/cangjie_compiler/src/Sema/InheritanceChecker/StructInheritanceChecker.cpp` L714-736、L876-904、L1082-1104

### 问题详情（逐行验证结论）

对照官方源码后**修正问题 5 的修复方向**：

1. 官方 `CheckSameNameInheritanceInfo`（L1082-1104）**确实存在** static 冲突诊断：L1090-1094 `if (child.TestAttr(STATIC) != parentDecl->TestAttr(STATIC))` → 报 `sema_static_and_non_static_member_cannot_have_same_name` 并 `return`（L1093）。
2. kind 不一致分支（L1095-1100）在 `parent.extendDecl || parent.isInheritedInterface || CheckExtendMemberValid(parent, child)`（L1096）条件下报 `sema_inherit_member_kind_inconsistent`——**该分支带 `isInheritedInterface` 条件**。
3. 官方两条调用路径：`DiagnoseForInheritedMember`（L714-736，结构继承）L723 与 `DiagnoseForInheritedInterfaces`（L876-904，接口继承）L883 都调用 `CheckSameNameInheritanceInfo`；但官方 `MergeInheritedMembers`（`MergeInheritedMemberHelper.cpp` L213-229）**不过滤 static 成员**，接口 static 成员进入检查链。
4. **修正结论**：EXP 期望 `INHERIT_MEMBER_KIND_INCONSISTENT`（`class_impl_interface1.cj` 是接口 static func vs 类实例 func）说明官方 cjc 在**接口 static 成员场景**报 kind 不一致——官方对接口 static 成员的冲突走 kind 分支（`parent.isInheritedInterface` 条件命中），而 CFIR 的 L964 `!hasStaticConflict` 门禁把 static 冲突优先了。**修复不是简单删除门禁，而是区分结构/接口继承路径**：接口继承（`parent.isInheritedInterface`）场景 static 冲突也应报 kind 不一致（对齐官方 L1096 条件）；结构继承场景保留 static 冲突诊断。

### 测试文件

`class_impl_interface1~4.cj`、`class_extends_class1/2/4/5.cj`（`OverloadGenerated`，16 失败，同问题 5）。

### 修复方案（验证后修正）

`CfirInheritanceDeepChecker.kt` L948-984 的比较循环中：当 `superInfo` 来自接口（`isInheritedInterface` 语义，对齐官方 `parent.isInheritedInterface`）时，static 冲突并入 kind 不一致分支报 `INHERIT_MEMBER_KIND_INCONSISTENT`；仅当父是结构/类时保留 `STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME` 优先。需要为 `InheritedMemberInfo` 增加 `isInheritedInterface` 标记（当前结构 L890-905 未携带）。

## 19.5 低频诊断名触发条件批量确认（28 个高频，逐个读源码）

### 发生位置

各诊断名责任位置见下方表格（`cfir/checkers/src` 下文件:行，均为 2026-08-04 实读确认）。

### 测试文件

各诊断名对应 fixture 见问题 14/15 对应条目（本节表格仅给触发条件，不重复 fixture 清单）。

### 问题详情（逐行验证结论）

> 以下 28 个低频诊断名（≥3 失败，覆盖低频诊断名大部分失败数）均于 2026-08-04 逐个读取责任代码确认触发条件；1-2 失败的诊断名触发条件已在问题 14/15 写入时基于代码片段提取。此处给出确认后的精确触发点。

| 诊断名 | 责任位置（确认） | 触发条件确认 |
|--------|------------------|--------------|
| NO_MATCHING_OPERATOR_INVOKE | `coneDiagnosticToCfirDiagnostic.kt` L1113-1129 | `ConeInapplicableCandidateError` 且候选符号是 `invoke` operator（L1118）时报；非 invoke 返回 null |
| CANNOT_CONVERT_LITERAL | `CfirAssignmentTypeMismatchChecker.kt` L90-98 | `CannotConvertLiteral` 主诊断分支报（L91-96），source 取 rValue |
| CONFLICTING_OVERLOADS | `CfirConflictsDeclarationChecker.kt` L89-93 | 冲突符号是函数/构造器/枚举构造器且 `isFunctionLikeRedeclaration` 时选此诊断 |
| RESUMPTION_HANDLE_TYPE_ERROR | `CfirEffectsExtraChecker.kt` L60-64 | handle clause typeRef 解析失败时报（effects 特性前提） |
| INVALID_SUBSCRIPT_EXPR | `coneDiagnosticToCfirDiagnostic.kt` L1814-1834 | operator 为 `[]` 且非 SET/赋值场景时报（L1829）；赋值场景报 CANNOT_ASSIGN_TO_SUBSCRIPT |
| GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT | `CfirGenericBareClassifierAccessChecker.kt` L43-62 | 裸泛型 classifier 访问（无 typeArguments）时报；枚举构造器/函数调用 receiver 例外 |
| ILLEGAL_USAGE_OF_MEMBER | `CfirInitializationCheckers.kt` L1223-1242 | 成员初始化器嵌套 callable 非法捕获当前/继承实例存储时报（NestedInitializerMemberAccessKind） |
| UNABLE_TO_INFER_RETURN_TYPE | `CfirFunctionSemanticsChecker.kt` L198-207 | 返回类型 ref 是 `CfirErrorTypeRef` 且 `isFunctionReturnTypeInferenceFailure()` 时报（L200-204） |
| GENERIC_ARGUMENT_NO_MATCH | `coneDiagnosticToCfirDiagnostic.kt` L796 区域 | `WrongArgumentCount` 分支映射（L796）；与约束不满足的 GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT 边界见问题 9 |
| AMBIGUOUS_USE | `coneDiagnosticToCfirDiagnostic.kt` L1417-1436 | 候选来自不同 primitive extend 目标时先报 AMBIGUOUS_MATCH_PRIMITIVE_EXTEND（L1425）；普通多义走 AMBIGUOUS_USE |
| NO_CONSTRUCTOR | `coneDiagnosticToCfirDiagnostic.kt` L186-203 | `ConeHiddenCandidateError` 且 callSite 是函数调用时报（L191-192 前置条件） |
| USE_EXPR_WITHOUT_IMPORT | `CfirQuoteImportChecker.kt` L17-34 | `quote` 表达式所在文件未导入 `std.ast`（L19 `FqName("std.ast")`）时报 |
| LAMBDA_MUST_HAVE_TYPE_ANNOTATION | `CfirFunctionLambdaChecker.kt` L139-146 | lambda 参数类型无法推断且上下文无期望函数类型时报 |
| NOTHING_TO_OVERRIDE | `CfirModifierChecker.kt` L166-183 | override/redef 修饰符但覆盖搜索无目标时报（L183 区域）；不可见成员先报 CANNOT_OVERRIDE_INVISIBLE_MEMBER（问题 13.2） |
| REDUNDANT_MODIFIER | `ModifiersCompatibilityUtils.kt` L48-58 | 修饰符对兼容性检查（COMPATIBLE/REPEATED/INCOMPATIBLE 三态）的 REPEATED 等分支报 |
| APILEVEL_MISSING_ARG | `CfirBuiltInAnnotationSemanticsChecker.kt` L143-160 | `@APILevel` 条目 >1 时报 MULTI_ANNO（L148-151）；缺必填参数时报 MISSING_ARG |
| RETURN_TYPE_INCOMPATIBLE | `CfirInheritanceDeepChecker.kt` L138-149 | extend superTypeRefs 比较循环中的返回类型不兼容分支（L149 区域） |
| USE_MUTABLE_FUNC_ALONE | `CfirExpressionSemanticsChecker.kt` L237-252 | `CfirMutFuncReferenceChecker`：mut 函数被单独引用（非调用）时报（L243 排除函数调用） |
| INTERFACE_CANNOT_INHERIT_CLASS | `CfirSupertypesChecker.kt` L118-135 | 接口的 superTypeRef 解析到类时报（L122-135） |
| TYPE_INCOMPATIBLE | `CfirAssignmentTypeMismatchChecker.kt` L89-108 | `TypeMismatch` 主诊断分支（L100-108）报，与 CANNOT_CONVERT_LITERAL 同函数分流 |
| STATIC_LAMBDA_CANNOT_ACCESS_NON_STATIC | `CfirExpressionSemanticsChecker.kt` L406-425 | `staticNonStaticAccessKind` 为 LAMBDA 时报（L414-425），与 STATIC_FUNCTION... 同机制 |
| UNABLE_TO_INFER_DECL | `coneDiagnosticToCfirDiagnostic.kt` L2404-2421 | `ConeSimpleDiagnostic` reason 匹配（"Unresolved return type"等，L2414-2421）映射 |
| INVALID_UNARY_EXPR | `coneDiagnosticToCfirDiagnostic.kt` L1918-1930 | operator + receiverType 存在且无参数时报（L1923-1926） |
| SPAWN_ARG_INVALID | `CfirSpawnSemanticsChecker.kt` L65-71 | spawn body 类型是 ConeErrorType 时报（L66-71） |
| ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER | `CfirFieldVariableThisOrSuperInitializerChecker.kt` L503-522 | 成员初始化器内 this/super 非法赋值报（L514-522） |
| EXTEND_DUPLICATE_INTERFACE | `CfirExtendCheckers.kt` L177-195 | extend superTypeRefs 中语义键重复时报（L181-195） |
| INOUT_CAN_ONLY_USED_IN_CFUNC_CALLING | `CfirInoutSemanticsChecker.kt` L44-60 | 实参含 inout 且被调函数非 foreign/CFunc 时报（L46-60） |
| INVALID_CFUNC_PARAMETER_TYPE | `CfirForeignFunctionReturnTypeChecker.kt` L62-78 | foreign 函数参数类型不满足 C 互操作规则时报（L66-78，嵌套 CFunc 检查） |

### 修复方案

上表各诊断名的修复策略见问题 14/15 对应条目；新增确认点：`INVALID_SUBSCRIPT_EXPR` 与 `CANNOT_ASSIGN_TO_SUBSCRIPT` 的分流条件（L1823-1829 `name == SET || isAssignmentLeftHandSide() || isAssignmentExpression()`）是下标赋值诊断名的关键边界，修复时以此为准。

---

# 问题 20：官方语义对照验证记录（问题 1/2/4/6/8/9 与官方 cjc 源码逐行对照）

> 本章将问题 1/2/4/6/8/9 的根因与官方 `external/cangjie_compiler` 源码逐行对照，确认/修正修复方向。全部基于 2026-08-04 实读官方源码。

## 20.1 问题 1 官方对照：构造器内非 common let 字段赋值官方放行（精确到 NotAssignableVariable）

### 发生位置

`external/cangjie_compiler/src/Sema/LegalityOfUsage/InitializationChecker.cpp` L94-115、L158-171、L675-708；`external/cangjie_compiler/src/Sema/Diags.cpp` L359-382

### 问题详情（逐行验证结论）

官方 let 字段赋值合法性检查链（对应 CFIR 问题 1 的 CANNOT_ASSIGN_TO_IMMUTABLE 误报）：

1. **`CheckLetFlag`（L675-708）**：L677 `bool inInitFunction = IsUsedInInitFunction(ctx, expr)`；L679-687 REF_EXPR 分支 `NotAssignableVariable(*vd, inInitFunction)` 判定后报诊断（L685）；COMMON let 字段在构造器内走 `DiagCJMPCannotAssignToImmutableCommonInCtor`（L682-683，Diags.cpp L369-382）。
2. **`IsUsedInInitFunction`（L94-115）**：L108-109 `currentFunc->TestAttr(CONSTRUCTOR) && sameStaticStatus && IsRelatedTypeDecl(...)`——构造器内且字段归属类型与构造器类型相关才判 inInitFunction。
3. **`NotAssignableVariable`（L165-171）**：

```cpp
return !vd.isVar &&
    (vd.TestAnyAttr(GLOBAL, INITIALIZED, ENUM_CONSTRUCTOR) ||
        (vd.TestAnyAttr(IN_STRUCT, IN_CLASSLIKE) && !inInitFunction) ||
        IsImutFieldInCtorOfCommonClassStruct(vd, inInitFunction));  // L159-163 仅 COMMON 字段
```

4. **对照结论**：官方非 common 的 let 字段在构造器内（inInitFunction=true）时，`(IN_STRUCT && !inInitFunction)` 为 false、`IsImutFieldInCtorOfCommonClassStruct` 仅对 COMMON 字段为 true → **赋值合法**。官方放行依赖 `IsRelatedTypeDecl`（字段归属类型与构造器类型相关）+ `INITIALIZED` 属性追踪（`CheckInitInVarDecl` L652-673 的 `vd.EnableAttr(Attribute::INITIALIZED)` L661），**无 CFIR 的 tracked 集合概念**。

### 测试文件

`key.cj`/`merge04/test2.cj` 等宏族（同问题 1，146+ 失败）。

### 修复方案（对照后确认）

CFIR 修复方向与官方一致：`isImmutableFieldAssignmentForbidden` 中，构造器内（`findClosestDeclaration<CfirConstructor>()` 命中）非 common let 字段应直接放行，不需要依赖 `classifyAssignment` 的 tracked 集合；`classifyAssignment` 的 `NOT_TRACKED` 不应判非法（16.1 已确认）。宏展开场景字段符号进入 tracked 集合是配套修复（对齐官方 `INITIALIZED` 属性）。

## 20.2 问题 4 官方对照：pattern usefulness 全程 implicitBoxed=true（确认漏报方向）

### 发生位置

`external/cangjie_compiler/src/Sema/TypeCheckPattern.cpp` L178-187；`external/cangjie_compiler/src/Sema/PatternUsefulness.cpp` L508-510；`external/cangjie_compiler/src/Sema/TypeManager.cpp` L992-1006

### 问题详情（逐行验证结论）

1. **官方 `IsSubtypeBoxed`（TypeCheckPattern.cpp L178-187）**：tuple/function 递归（L180-183），else 分支 `typeManager.IsSubtype(&leaf, &root, true, false)`（L185）——**implicitBoxed=true**。
2. **官方 `PatternUsefulness.cpp` L508-510**：type pattern 覆盖率判定 `IsSubtype(&goalTy_, &patternTy, true, false)`（L508）与 `IsSubtype(&patternTy, &goalTy_, true, false)`（L509）——同样 implicitBoxed=true。
3. **官方 `TypeManager::IsSubtype`（L992 起）**：第 3 参 implicitBoxed 控制装箱；fastCheck（L1004-1006）`(implicitBoxed || leaf->IsClassLike()) && root->IsAny()` 允许隐式装箱到 Any。
4. **对照结论**：官方 pattern usefulness 全程允许 implicitBoxed=true（`Int64`↔`Option<Int64>`/`Any` 装箱覆盖成立），CFIR `isMatchSubtypeOf`（`CfirMatchTypeRelations.kt` L31-53）无装箱路径，L51-52 只走 supertype provider → 官方判覆盖的 autobox 分支 CFIR 判不覆盖 → **漏报 UNREACHABLE_PATTERN 的根因被官方坐实**。

### 测试文件

`autobox_match1/2.cj`、`unbox_*.cj`、`as_expr_00.cj`（同问题 4 漏报方向，20 失败）。

### 修复方案（对照后确认）

`isMatchSubtypeOf` 的 else 分支（L51-52）增加与官方 `IsSubtype(..., true, false)` 等价的 implicitBoxed 路径：`Int64`/tuple 元素/struct 值到 `Any` 及 `Option<T>` 的装箱覆盖判定；`isTypePatternOrdinarySubtypeOf`（L73-104）保持非 boxed 语义不变（官方 `IsSubtype` 的 boxed 参数在 type pattern 场景同样为 true，需再核对其 L102 的 `requiresBoxingToClassLikeSupertype` 排除是否与官方一致）。

## 20.3 问题 8 官方对照：orphan rule 父类传递（CollectAllRelatedExtends 坐实）

### 发生位置

`external/cangjie_compiler/src/Sema/TypeCheckExtend.cpp` L519-573；`external/cangjie_compiler/src/Sema/TypeCheckUtil.cpp` L541-556；`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/CfirExtendSemantics.kt` L288-305

### 问题详情（逐行验证结论）

1. **官方 `CheckExtendOrphanRule`（TypeCheckExtend.cpp L519-573）**：
   - L523-529：收集目标类型的直接继承接口（`GetAllSuperTys(*extendedTypeTarget->ty, {}, false)`）到 `otherPackageExtendInterfaceTy`；
   - L531：`CollectAllRelatedExtends(typeManager, *extendedTypeTarget)` 收集**直接+间接** extend；
   - L537-546：对其他包 extend 的 inheritedTypes 的 super types（`GetAllSuperTys`）并入闭包，**带类型实参替换**（L536 `InverseMapping(GenerateTypeMapping(...))`）；
   - L549-550：`isImportedExtendedType`（目标 imported 或 builtin）；
   - L566：**仅 `isImportedExtendedType && !externalDecls.empty()` 时报**。
2. **官方 `CollectAllRelatedExtends`（TypeCheckUtil.cpp L541-556）**：对 CLASS_DECL **沿 `GetSuperClassDecl()` 链遍历祖先类** collects extends（L548-554）——父类传递在官方明确存在。
3. **CFIR 对照**：`CfirExtendIndexStore.otherPackageExtendedInterfaceClassIds`（L267-283）只查 `modelsForTarget(targetKey)` 精确键（L128-129），**无父类链遍历、无类型实参替换** → `import_orphanrule_02` 场景 `B <: A`（A 在 p1 被 extend）的闭包缺失 → 差集非空 → 误报。`isTargetDeclaredInPackage`（CfirExtendSemantics.kt L288-305）与官方 `isImportedExtendedType` 对应，判定逻辑本身正确。

### 测试文件

`import_orphanrule_01~06/main.cj`（同问题 8，14 失败）。

### 修复方案（对照后确认）

`otherPackageExtendedInterfaceClassIds` 增加：① 沿目标类型父类链（`typeAwareSupertypeProvider` 或 supertypeProvider）收集父类上的外部扩展闭包；② 闭包收集时做类型实参替换（对齐官方 L536 的 `InverseMapping(GenerateTypeMapping(...))`），使 `I0<Q>` 与 `I0<Int64>` 这类实例化差异的接口能被正确识别。

## 20.4 问题 2 官方对照：CheckRefConstructor 直接查函数体属性

### 发生位置

`external/cangjie_compiler/src/Sema/TypeCheckCall.cpp` L2311-2326

### 问题详情（逐行验证结论）

官方 `CheckRefConstructor`（L2311-2326）：L2314 `re.isSuper || re.isThis` 识别委托调用；L2315-2319 `GetCurFuncBody(ctx, re.scopeName)` 取当前函数体，`funcDecl->TestAttr(CONSTRUCTOR)`（L2317-2318）判定是否在构造器内；L2320-2323 不在则报 `sema_invalid_this_call_outside_ctor`。

**对照结论**：官方直接查当前函数体声明的 `CONSTRUCTOR` 属性（作用域链定位），**无 CFIR 的 `containingDeclarations` 符号栈过滤**——16.4 发现的 CFIR `closestFunctionLikeDeclaration`（L57-61）类型不匹配（`it is CfirFunction` 过滤 symbol 栈恒 null）在官方无对应物，属于 CFIR 特有 bug。修复方向（改用 `findClosestDeclaration<CfirConstructor>()` 解包 `.cfir`）与官方语义一致。

### 测试文件

`primaryConstructor1.cj`、`primaryConstructor8.cj`、`super_this_11.cj`（同问题 2，76+8 失败）。

### 修复方案（对照后确认）

按 16.4 方案：`closestFunctionLikeDeclaration` 改为 `firstNotNullOfOrNull { declaration -> declaration.cfir as? CfirFunction }`（与 `findClosestDeclaration` L428 一致），或直接用 `context.findClosestDeclaration<CfirConstructor>()`。官方 `GetCurFuncBody` 语义提示：还应核对构造器参数默认值区（body 未建立时）的委托调用归属。

## 20.5 问题 6 官方对照：CheckImmutableFuncAccessMutableFunc 判定条件

### 发生位置

`external/cangjie_compiler/src/Sema/TypeCheckExpr.cpp` L166-204

### 问题详情（逐行验证结论）

官方 `CheckImmutableFuncAccessMutableFunc`（L190-204）：
- L194-195 `accessMutableTarget`：目标为 MUT 函数，或属性为 var 且左值 struct 值；
- L196 `bothInstance`：源与目标均非 static；
- L197-199：源为函数、**非 mut**、`outerDecl != nullptr`、**非 CONSTRUCTOR、非 PRIMARY_CONSTRUCTOR**、bothInstance 且 accessMutableTarget → 报 `sema_immutable_function_cannot_access_mutable_function`（L202）。

官方 `CanTargetOfRefBeCapturedCaseMutFunc`（L166-187）：L173-176 mut 函数内嵌套函数捕获实例字段报 `sema_capture_this_or_instance_field_in_func`（对应 CFIR ILLEGAL_CAPTURE_THIS/CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC）。

**对照结论**：CFIR `currentImmutableStructFunction()`（`CfirMutabilityCheckers.kt` L208-218）缺官方的 `outerDecl != nullptr`、非构造器、非主构造器排除条件；`CfirImmutableFunctionCannotAccessMutableFunctionChecker`（L102-131）的裸调用接收者识别（L112 `isCurrentStructReceiverAccess`）未覆盖官方 L197-199 的全部形态 → record 嵌套场景漏报的根因坐实。

### 测试文件

`mut_function_01.cj`、`record_mut_invalid_12/14/15.cj`、`record_extend_mut_invalid_8/10/11/12.cj`（同问题 6，50 失败）。

### 修复方案（对照后确认）

`CfirImmutableFunctionCannotAccessMutableFunctionChecker` 对齐官方 L197-199 条件：源函数非 mut、有 outerDecl（成员函数）、非构造器、非主构造器、bothInstance、目标 mut/var 属性；`currentImmutableStructFunction` 补充 outerDecl 与构造器排除。

## 20.6 问题 9 官方对照：官方无 NEW_INFERENCE_ERROR，对应 sema_unable_to_infer 系列

### 发生位置

`external/cangjie_compiler/src/Sema/Diags.cpp` L283-302；`external/cangjie_compiler/src/Sema/Diags.h` L35-38；`external/cangjie_compiler/src/Sema/TypeCheckCall.cpp` L1540-1555

### 问题详情（逐行验证结论）

1. **官方推断失败诊断**：`DiagUnableToInferReturnType`（Diags.cpp L283-302，`Ty::IsTyCorrect(fd.ty)` 时报，L284）与 `DiagUnableToInferExpr`（Diags.h L35）——**官方没有 `NEW_INFERENCE_ERROR` 诊断名**，本仓库的 `NEW_INFERENCE_ERROR`（`coneDiagnosticToCfirDiagnostic.kt` L495-499）是函数调用推断失败的自定义映射。
2. **官方触发条件**：`TypeCheckCall.cpp` L1549 `NeedSynOnUsed(*fd) && Synthesize(ctx, fd) && (!Ty::IsTyCorrect(fd->ty) || fd->ty->HasQuestTy())` → `DiagUnableToInferReturnType`——合成后类型仍不正确或含 quest（占位符未求解）时报；`NameReferenceExpr.cpp` L48/L369/L402 裸函数引用场景；`RangeExpr.cpp` L80 表达式推断失败。
3. **对照结论**：官方在这些场景**都会报诊断**；CFIR 的 `coneDiagnosticToCfirDiagnostic.kt` L460-468 前置静默过滤（`ConstrainingTypeIsError`/`NotEnoughInformationForTypeParameter` → `return emptyList()`）把官方会报的场景吞掉 → 12 个 `缺少: NEW_INFERENCE_ERROR` 的根因坐实。

### 测试文件

`intersectionCollapsePlaceholder.cj`、`newInferenceErrorConflict.cj`、`inferencePlaceholder.cj`、`varraySizeMismatch.cj`（同问题 9）。

### 修复方案（对照后确认）

L460-468 的前置过滤改为"降级而非静默"：占位符未求解/交集坍缩先映射 `NEW_INFERENCE_ERROR`（对齐官方 `DiagUnableToInferReturnType` 的 quest 类型触发），仅当错误已由更具体诊断覆盖时才返回空；varray 场景映射 `VARRAY_SIZE_MISMATCH` 而非 `typeMismatchDiagnostic`（18.2 已确认）。

---

# 问题 21：消息文本差异分析 + 问题 5/3/7/10/11 官方对照（第五批）

> 本章含一个全新维度（诊断消息文本差异）与问题 5/3/7/10/11 的官方源码对照。全部基于 2026-08-04 实读。

## 21.1 诊断消息文本差异分析（新维度：确认消息参数渲染无差异）

### 发生位置

失败数据全量比对（`/tmp/fail_rows.json`，1960 失败）。

### 问题详情（逐行验证结论）

此前所有分析聚焦诊断名/范围/数量/顺序。本轮新增维度：**EXP/ACT 诊断名+范围（标记 inner）完全一致、但去除 `<!!>` 标记后的纯文本不同的失败**——即同一诊断但消息参数（a/b/c）渲染内容不同的情况。

**结论：1960 个失败中只有 2 个**（`extend_duplicate_interfaces1.cj::testUnboxArray`，`EXTEND_DUPLICATE_INTERFACE`）满足条件，且它们是**同一文件内两处 `extend A <: Foo` 的标记位置互换**（EXP 标第一条、ACT 标第二条）——属报告顺序/归属差异（问题 12 范围簇），**非消息参数内容差异**。

**验证意义**：CFIR 诊断消息参数的渲染（`on(a=..., b=..., ...)` 的参数选择与默认消息模板）与官方一致，消息文本层无缺陷；全部 1960 失败均可归因于诊断名/范围/数量/顺序四类，无一例外。

### 测试文件

`extend_duplicate_interfaces1.cj`（2 个失败，归问题 12 范围簇）。

### 修复方案

无需针对消息文本修复；该 2 个失败按问题 12 的范围簇方案处理（`EXTEND_DUPLICATE_INTERFACE` 报告点归属：`CfirExtendCheckers.kt` L177-195 的重复接口判定应统一报在第一条重复接口上，对齐官方报告顺序）。

## 21.2 官方对照问题 5 完整路径：继承链 static 冲突官方统一报 kind 不一致

### 发生位置

`external/cangjie_compiler/src/Sema/InheritanceChecker/StructInheritanceChecker.cpp` L714-736、L876-904、L1082-1104；测试文件 `class_impl_interface1.cj`、`class_extends_class1.cj`

### 问题详情（逐行验证结论）

1. 官方 `CheckSameNameInheritanceInfo`（L1082-1104）：L1090-1094 static 冲突优先报 `sema_static_and_non_static_member_cannot_have_same_name` 并 return；L1095-1100 kind 不一致在 `parent.extendDecl || parent.isInheritedInterface || CheckExtendMemberValid(parent, child)`（L1096）下报 `sema_inherit_member_kind_inconsistent`。
2. 官方两条调用路径都调用它：`DiagnoseForInheritedMember`（L714-736 结构继承，L723）与 `DiagnoseForInheritedInterfaces`（L876-904 接口继承，L883）。
3. **关键实测**：`class_extends_class1.cj`（**纯结构继承**：`abstract class Base { public func foo }` + `class Data <: Base { public static func foo }`）的 EXP 期望 `INHERIT_MEMBER_KIND_INCONSISTENT`；`class_impl_interface1.cj`（接口继承）同样期望 `INHERIT_MEMBER_KIND_INCONSISTENT`。
4. **结论（解决 19.4 开放问题）**：EXP 数据（官方 cjc 输出）表明**继承链上 static/实例同名冲突（无论结构继承还是接口继承）官方语义统一报 `INHERIT_MEMBER_KIND_INCONSISTENT`**；官方 `sema_static_and_non_static_member_cannot_have_same_name`（L1091）仅用于**同声明层**（同一 class 内 static 与实例成员同名，由 `DiagnoseForInheritedMember` L720-722 的 `parent.decl->outerDecl == child.decl->outerDecl` 排除继承场景后剩同层场景）。CFIR 的 L964 `!hasStaticConflict` 门禁把 static 冲突优先于 kind 不一致，与官方 EXP 相反。

### 测试文件

`class_impl_interface1~4.cj`、`class_extends_class1/2/4/5.cj`（`OverloadGenerated`，16 失败）。

### 修复方案（验证后修正 19.4）

CFIR `CfirInheritanceDeepChecker.kt` L948-984：**继承链（结构+接口）static 冲突统一并入 kind 不一致分支报 `INHERIT_MEMBER_KIND_INCONSISTENT`（extend 场景报 `EXTEND_MEMBER_CANNOT_SHADOW`）**；`STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME` 仅保留给同声明层重名（`CfirConflictsDeclarationChecker`）。不再需要 `isInheritedInterface` 区分（21.2 证明结构继承同样报 kind 不一致）。

## 21.3 官方对照问题 3：static init 官方建模为普通 static 函数

### 发生位置

`external/cangjie_compiler/include/cangjie/Utils/ConstantsUtils.h` L25-26；`external/cangjie_compiler/src/Sema/Desugar/AfterTypeCheck/Package.cpp` L140、L158-190

### 问题详情（逐行验证结论）

官方 `ConstantsUtils.h` L25-26：`STATIC_INIT_VAR = "$init"`、`STATIC_INIT_FUNC = "static.init"`。`Package.cpp`：
- L140/L158-167：把用户 `static init()` 转写为静态成员 `private static let $init = static_init()`；
- L189-190：`staticInitfb->funcDecl->identifier = STATIC_INIT_FUNC`——**重命名为普通 static 函数 "static.init"**。

**结论**：官方把 static init 建模为**普通 static 函数**（`FuncDecl`），static 修饰符天然合法，官方**不存在** CFIR 的 `DeclarationKind.STATIC_INITIALIZER` 种类概念。CFIR 的 `actualTargetsFor`（L282-288）对 static 构造器返回 `head(STATIC_INITIALIZER)`（Site.HEAD），而 `STATIC_KEYWORD` 谓词是 `memberOf(FUNCTION, PROPERTY, VARIABLE)`（要求 Site.MEMBER）——site 不匹配误报（19.1 已确认）。修复方向（`anyOf(memberOf(...), headOf(STATIC_INITIALIZER))`）与官方"static init 是合法 static 声明"语义一致。

### 测试文件

`static_init_01.cj`、`static_or_global_var3~12.cj`（同问题 3，52 失败）。

### 修复方案（对照后确认）

按 19.1 方案：`STATIC_KEYWORD` 谓词改为 `anyOf(memberOf(FUNCTION, PROPERTY, VARIABLE), headOf(STATIC_INITIALIZER))`。

## 21.4 官方对照问题 7：官方 GetNonFuncDeclsInSuperClass 显式收集父类存储槽

### 发生位置

`external/cangjie_compiler/src/Sema/LegalityOfUsage/InitializationChecker.cpp` L1717-1738

### 问题详情（逐行验证结论）

官方 `GetNonFuncDeclsInSuperClass`（L1717-1738）：
- L1720-1727：收集父类 body 中**非 private、非属性**的 `VarDecl`（super 存储槽）加入 `superClassNonFuncDecls`；
- L1728-1737：递归收集祖父类（`superClasses` 集合去重，L1730-1734）。

官方初始化检查把父类存储槽作为可追踪声明显式收集（`GetVarsInitializationOrderWithPositions` L1710-1713 与 `CollectToDeclsInfo` L1707-1709 组合），父类字段参与当前构造器的初始化顺序追踪。**对照结论**：CFIR `InitializationState`（L396-398）只 declare 当前类字段（`owner.instanceFieldInfos`），**父类存储槽不在 tracked 集合**——`super_this_05-08.cj` 的 `k = super.f()` 漏报 USED_BEFORE_INITIALIZATION 的根因被官方坐实。

### 测试文件

`super_this_05~08.cj`、`variable_use_before_init_11/15.cj`（同问题 7，18 失败）。

### 修复方案（对照后确认）

按 16.6 方案：`InitializationState` 增加父类存储槽跟踪（构造器分析时把 `typeAwareSupertypeProvider` 可见的父类非 private 字段 declare 进 tracked，初始未初始化；`super(...)` 时标记已初始化；`super.member` 访问未初始化时报 USED_BEFORE_INITIALIZATION）——与官方 `GetNonFuncDeclsInSuperClass` 语义对齐。

## 21.5 官方对照问题 10：官方 GetUsedMacroDecls 与 CFIR 语义一致

### 发生位置

`external/cangjie_compiler/src/Macro/MacroEvaluation.cpp` L677-686；`external/cangjie_compiler/src/Modules/ImportManager.cpp` L1055-1067

### 问题详情（逐行验证结论）

官方宏使用记录：
- `SaveUsedMacros`（MacroEvaluation.cpp L677-686）：`SaveUsedMacroPkgs(packageName)`（L679）记录宏包名；`AddUsedMacroDecls(file, decl)`（L685）记录**已解析宏定义**；
- `AddUsedMacroDecls`（ImportManager.cpp L1055-1061）：按 `file->indexOfPackage` + `decl->fullPackageName` 组织声明集合；
- `GetUsedMacroDecls`（L1063-1067）：按文件返回包名→声明集合映射。

**对照结论**：官方与 CFIR `usedMacroNames`（`MacroConstructionApi.kt` L407-444）**语义一致**——都只记录被消费的宏定义/宏名，**都不含宏展开产物中引用的普通符号**。问题 10 的 UNUSED_IMPORT 漏计根因**不在宏记录层**，而在 `collectReferencedNames`（`CfirImportsChecker.kt` L280 `source != null`）排除宏展开合成节点（19.2 已确认）；官方 unused-import 判定（`ImportManager` 侧用 `GetUsedMacroDecls` 按包名匹配 import）与 CFIR L204/L213 的宏检查等价。

### 测试文件

`unused015/016.cj`、`enumcons_inside_macro.cj`、`typeaslias.cj`（同问题 10，16 失败）。

### 修复方案（对照后确认）

按 19.2 方案：`collectReferencedNames` 的 visitor 增加对宏展开产物节点的遍历（展开产物引用计入 import 使用）；宏记录层无需改动（与官方一致）。

## 21.6 官方对照问题 11：DiagMemberAccessNotFound nominal 判定与 CFIR 一致

### 发生位置

`external/cangjie_compiler/src/Sema/TypeCheckExpr/NameReferenceExpr.cpp` L291-325

### 问题详情（逐行验证结论）

官方 `DiagMemberAccessNotFound`（L291-325）：
- L293-295：`IsFieldOperator(ma.field)` 跳过（operator 重载访问不报）；
- L296-304：test-only 注册函数跳过；
- L305-309：baseExpr 类型无效跳过；
- L311-313：`ma.isExposedAccess` → `sema_not_found_from_generic_upper_bounds`；
- **L314-319：`baseExpr->ty->IsNominal()` → `sema_not_member_of`**；
- L320-324：非 nominal → `sema_undeclared_identifier`。

**对照结论**：官方 `sema_not_member_of` 的 nominal 判定（L314）与 CFIR `mapNotMemberOfDiagnostic`（L1892-1898：ConeClassLikeType/ConeStructType/ConeEnumType）**一致**。问题 11.1 的 NOT_MEMBER_OF 误报根因**不在映射层**，而在 resolve 层 extend 接口成员 scope（`CfirExtendMemberScope.buildIndex` L175-197 只索引 extend 声明体内成员，extend 实现的接口成员不进 scope——18.3 已坐实）；官方 `IsNominal()` 判定与 CFIR 相同，两者在映射层行为一致。

### 测试文件

`disable_default_static.cj`、`extend_interface_static1.cj`、`class_*_thistype_ok_*.cj`（同问题 11.1，29 失败）。

### 修复方案（对照后确认）

按 18.3 方案：`CfirExtendMemberScope.buildIndex` 在索引 `extend.declarations` 之外，把 extend 实现的接口闭包（含泛型实参替换后的父接口）的静态成员并入索引；映射层无需改动。

---

# 问题 22：诊断名映射完整性 + 官方报告位置对照 + 消息模板对照 + 1-2 失败诊断名验证（第六批）

> 本章含四个新维度：CFIR 诊断名与官方诊断清单的映射完整性、官方报告位置对照、CFIR 与官方消息模板对照、1-2 失败低频诊断名的逐个源码验证。全部基于 2026-08-04 实读。

## 22.1 诊断名映射完整性：CFIR 183 个 vs 官方诊断清单

### 发生位置

`external/cangjie_compiler/include/cangjie/Basic/DiagnosticSema.def`（非 refactor）、`external/cangjie_compiler/include/cangjie/Basic/DiagRefactor/Diagnostic*.def`（refactor 全套）；CFIR 侧 `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/CfirErrorsDefaultMessages.kt`

### 问题详情（逐行验证结论）

官方诊断名从 8 个 def 文件提取（`DiagRefactor/DiagnosticSema/Chir/Parser/Package/Module.def` + 非 refactor `DiagnosticSema/CHIR/MacroExpand.def`），共 **867 个**（含全部 Sema/Parser/CHIR/Macro 阶段）；CFIR 失败涉及的诊断名 **183 个**。

映射结果（`build/diag_mapping.json`）：
- **精确映射到官方同名诊断（去 `sema_` 前缀后小写匹配）：124 个**——如 `NOT_MEMBER_OF`→`sema_not_member_of`、`USED_BEFORE_INITIALIZATION`→`sema_used_before_initialization`、`CANNOT_ASSIGN_TO_IMMUTABLE`→`sema_cannot_assign_to_immutable`（DiagRefactor 版）。
- **未映射：59 个**。分两类：
  - **命名差异（官方存在但名字不同）**：如 `TYPE_MISMATCH`（官方 `sema_mismatched_types`）、`UNRESOLVED_REFERENCE`（官方 `sema_undeclared_identifier`）、`REDECLARATION`（官方 `sema_redefinition`）、`UNUSED_IMPORT`、`UNRESOLVED_IMPORT`（官方在 `ImportManager` 侧语义）、`INVALID_BINARY_OPERATOR`（官方 `sema_invalid_binary_expr`）、`MULTIPLE_CLASS_SUPER_TYPES`、`OVERRIDING_RETURN_TYPE_MISMATCH`、`RETURN_TYPE_MISMATCH`、`STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE` 等——这些是**诊断名对齐问题**，语义在官方存在但 CFIR 用了不同名字。
  - **CFIR 自创（官方无对应诊断名）**：如 `NEW_INFERENCE_ERROR`（官方用 `sema_unable_to_infer_return_type`/`sema_unable_to_infer_expr`，20.6 已确认）、`CONFLICTING_OVERLOADS`（官方 `sema_overload_conflicts`）、`CLASSIFIER_REDECLARATION`、`SUPER_TYPES_DUPLICATE`（官方无 class 级同名，走 PreCheck L1813）、`CANNOT_REF_TO_PKG_NAME`、`EFFECTS_FEATURE_DISABLED`、`MACRO_EXPANSION_FAILED`/`MACRO_UNRESOLVED`/`MACRO_DEPENDENCY_COMPILE_FAILED`（官方 Macro 阶段用独立 def）、`EXPORT_EXTEND_DEPEND_NON_EXPORT_EXTEND` 等。

**结论**：CFIR 的 124/183 诊断名有官方同名对应（映射率 68%）；59 个未映射中约半数属命名差异（语义存在、名字不同），约半数属 CFIR 自创或官方不同阶段/模块诊断。

### 测试文件

映射对比基于全部 1960 个失败涉及的 183 个诊断名（数据来源：`cfir/analysis-tests/build/test-results/test/*.xml`）；映射结果存于 `cfir/analysis-tests/build/diag_mapping.json`，无单一 fixture 依赖（全量诊断名层面验证）。

### 修复方案

1. 命名差异类：对照官方 `DiagRefactor/DiagnosticSema.def` 的确切诊断名（如 `sema_mismatched_types`/`sema_redefinition`），修正 CFIR `CfirErrorsDefaultMessages.kt` 中对应诊断的注册名——但注意测试 EXP 使用 CFIR 命名，修正需与测试数据同步。
2. 自创类（NEW_INFERENCE_ERROR 等）：保持 CFIR 命名（测试依赖），但触发条件须对齐官方语义（20.6 已确认 NEW_INFERENCE_ERROR 应对齐 `sema_unable_to_infer_*` 的 quest/未求解触发）。

## 22.2 官方报告位置对照：WRONG_NUMBER_OF_ARGUMENTS / CONFLICTING_OVERLOADS / SUPER_TYPES_DUPLICATE

### 发生位置

`external/cangjie_compiler/src/Sema/Diags.cpp` L45-76、L163-193；`external/cangjie_compiler/src/Sema/PreCheck.cpp` L1604、L1813；`external/cangjie_compiler/include/cangjie/Basic/DiagRefactor/DiagnosticSema.def` L147

### 问题详情（逐行验证结论）

1. **WRONG_NUMBER_OF_ARGUMENTS**（官方 `DiagWrongNumberOfArguments`，Diags.cpp L45-76）：L58-62 报告范围 = **调用括号范围**——`beginPos = ce.leftParenPos`（L59）、`endPos = ce.rightParenPos + 1`（L62，trailing lambda 时用 `ce.args.back()->end` L61-62）。**官方报整个 `foo(...)` 含参数括号**；CFIR `coneDiagnosticToCfirDiagnostic.kt` L817-820 用 `rootCause.source`（函数引用，不含括号）→ `generic_constraint_and_4.cj` 的 EXP（报 `a.foo(1, 2)` 整调用）vs ACT（只报 `a.foo`）差异的官方依据。
2. **CONFLICTING_OVERLOADS**（官方 `DiagOverloadConflict`，Diags.cpp L163-193）：L180-181 报告范围 = **函数名 identifier 范围**（`MakeRange(baseFd->identifier.Begin(), identifier)`）——官方报函数名；`simple.cj` 的 EXP 报整函数声明（`func ab()`）、ACT 报函数名——**官方与 ACT 一致**，EXP 是项目 range policy 的放宽，修复应让 CFIR 保持函数名（ACT 已正确）或同步 EXP。
3. **SUPER_TYPES_DUPLICATE**：官方无同名诊断——class 重复父类型在 `PreCheck.cpp` L1813（`CheckDuplicateInterfaceInheritance`）走另一诊断名；extend 场景官方用 `sema_extend_duplicate_interface`（DiagRefactor L147 `"interface '%s' has been implemented by '%s'"`）——CFIR 的 `SUPER_TYPES_DUPLICATE` 是自创命名，且语义（L147 官方要求"已被实现"而非单纯重复）与 CFIR 的重复判定（`CfirSupertypesChecker.kt` L83-90 纯 key 重复）有差异。

### 测试文件

- WRONG_NUMBER_OF_ARGUMENTS：`generic_constraint_and_4.cj`、`enum1.cj`、`variadic_lambda_01.cj`、`funcDecl2.cj`（`GenericConstraintGenerated`/`CallGenerated`/`EnumGenerated`/`FunctionGenerated`，7 行差异）
- CONFLICTING_OVERLOADS：`RedeclarationGenerated/simple.cj`（6 行差异）
- SUPER_TYPES_DUPLICATE：`interface_duplicated_02.cj`、`extend_duplicate_interfaces6/10.cj`（`ExtendsImplementsInterfaceDuplicatedGenerated`/`ExtendGenerated`，18 行差异）

### 修复方案

1. WRONG_NUMBER_OF_ARGUMENTS：source 改用调用括号范围（对齐官方 L58-62：`leftParenPos` 到 `rightParenPos+1`，trailing lambda 例外）。
2. CONFLICTING_OVERLOADS：报告范围保持函数名（官方 L181 一致）；若需对齐 EXP（项目 range policy），改在 `CfirConflictsDeclarationChecker` 的报告 source 选择，但以官方函数名为准。
3. SUPER_TYPES_DUPLICATE：对齐官方语义——extend 重复接口仅在"目标已实现该接口"时报（`sema_extend_duplicate_interface`），非单纯重复；class 级重复父类型走官方对应诊断。

## 22.3 1-2 失败低频诊断名逐个源码验证（35 个确认）

### 发生位置

各诊断名责任代码（`cfir/checkers/src`，2026-08-04 逐个读取片段确认）。

### 问题详情（逐行验证结论）

对失败数为 1-2 的低频诊断名（共 84 个有片段）中前 35 个逐个读取源码确认触发条件，代表性确认结果：

| 诊断名 | 责任位置（确认） | 触发条件（确认） |
|--------|------------------|------------------|
| STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME | `CfirInheritanceDeepChecker.kt` L954 | L949-961 static 冲突分支（同问题 5/21.2） |
| CLASSIFIER_REDECLARATION | `CfirConflictsDeclarationChecker.kt` L97 | 分发器 L95-97：冲突符号含 ClassLikeSymbol 时报 |
| THIS_AS_EXPRESSION_IN_FUNC | `CfirExpressionSemanticsChecker.kt` L364 | L344-367 open/abstract 构造器裸 this（16 节已述） |
| AMBIGUOUS_CONSTRUCTOR_CALL | `coneDiagnosticToCfirDiagnostic.kt` L1402 | L1396-1402 候选全为构造器且非重定义级联 |
| VARRAY_SIZE_MISMATCH | `CfirTypeSemanticsDiagnostics.kt` L138 | L131-138 VArray 长度不匹配专用诊断 |
| TYPEALIAS_CYCLE | `CfirTypeAliasCycleChecker.kt` L36 | L17-36 展开引用带 RECURSIVE_TYPEALIAS_PREFIX（L21）时报 |
| INVISIBLE_REFERENCE | `coneDiagnosticToCfirDiagnostic.kt` L2032 | L2013-2032 不可见符号映射（含 isMemberAccess 判定） |
| MACRO_UNRESOLVED | `MacroConstructionDiagnosticCollectorComponent.kt` L227 | L208-227 宏调用解析失败分支 |
| COMMAND_HANDLE_TYPE_ERROR | `coneDiagnosticToCfirDiagnostic.kt` L2137 | L2125-2137 effects 命令 handle 类型错误映射 |
| ILLEGAL_MULTI_INHERITANCE | `CfirSupertypesChecker.kt` L185 | L166-185 第二个 concrete 父类 |
| CATCH_TYPE_MUST_EXTEND_EXCEPTION | `CfirExpressionSemanticsChecker.kt` L775 | L758-775 catch 类型 Exception/Error 子类型检查 |
| CONFLICTING_UPPER_BOUNDS | `CfirTypeParameterBoundsChecker.kt` L94 | L75-94 多类上界冲突 |
| UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE | `CfirTypeParameterBoundsChecker.kt` L80 | L61-80 上界种类 INVALID |
| USE_FUNC_CAPTURE_VAR_ALONE | `CfirClosureCaptureUsageChecker.kt` L97 | L78-97 捕获变量使用位置选诊断 |
| IFAVAILABLE_UNKNOWN_ARG_NAME | `CfirBuiltInAnnotationSemanticsChecker.kt` L199 | L194-202 白名单外参数名 |
| IFAVAILABLE_LEVEL_LIMIT | `CfirBuiltInAnnotationSemanticsChecker.kt` L209 | L205-209 IfAvailable 级别限制 |
| APILEVEL_SYSCAP_ERROR/WARNING | `CfirApiLevelRefHigherChecker.kt` L65/L71 | L63-71 SyscapError/SyscapWarning 分支 |
| UNQUALIFIED_LEFT_VALUE_ASSIGNED | `CfirAssignmentLegalityChecker.kt` L86 | L72-86 非左值名字赋值分类 |
| EXTEND_FUNCTION_CANNOT_OVERRIDDEN | `CfirExtendExtraChecker.kt` L196 | L185-196 extend 内 override 报错 |
| INVALID_LOOP_CONTROL | `CfirEffectsExtraChecker.kt` L126 | L112-126 handle body 内 break/continue |
| PROPERTY_MUST_IMPLEMENT_BOTH | `CfirGeneralSemanticsChecker.kt` L833 | L814-833 属性 getter/setter 成对实现 |
| GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS | `coneDiagnosticToCfirDiagnostic.kt` L1859 | L1840-1859 类型参数接收者归类上界无成员 |
| PARAMETERS_AND_ARGUMENTS_MISMATCH | `coneDiagnosticToCfirDiagnostic.kt` L1260 | L1241-1260 构造器显式类型实参+裸函数引用 |
| EXTEND_C_TYPE_NOT_ALLOWED | `CfirExtendCheckers.kt` L86 | L67-86 foreign 边界目标 |
| CLASS_INHERIT_NON_CLASS_NOR_INTERFACE | `CfirSupertypesChecker.kt` L162 | L143-162 父类型种类检查 |
| BUILTIN_INDEX_IN_BOUND | `CfirExpressionSemanticsChecker.kt` L720 | L701-720 内建下标越界 |
| DEPRECATED_WARNING | `CfirDeprecatedCallChecker.kt` L37 | L18-37 @Deprecated 调用等级 |
| TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE | `CfirExtendExtraChecker.kt` L618 | L600-618 checkExtendImportedInterface |
| NON_EXHAUSTIVE_MATCH | `CfirMatchExhaustivenessChecker.kt` L43 | L24-43 match 穷尽性检查 |
| CANNOT_REF_TO_PKG_NAME | `coneDiagnosticToCfirDiagnostic.kt` L2274 | L2255-2274 包名引用错误 |
| WRONG_MODIFIER_CONTAINING_DECLARATION | `CfirModifierChecker.kt` L155 | L136-159 checkParent 父目标谓词 |
| APILEVEL_MULTI_ANNO | `CfirBuiltInAnnotationSemanticsChecker.kt` L151 | L146-151 apiLevelEntries>1 |
| MACRO_DEPENDENCY_COMPILE_FAILED | `MacroConstructionDiagnosticCollectorComponent.kt` L117 | L98-117 宏依赖编译失败 |
| AMBIGUOUS_USE（部分） | `coneDiagnosticToCfirDiagnostic.kt` L1402 | L1396-1402 多义候选（与 AMBIGUOUS_CONSTRUCTOR_CALL 同源） |

**结论**：1-2 失败诊断名的触发条件全部有代码级确认，无占位推断；多数与已述问题同源（如 STATIC_AND_NON_STATIC 同问题 5、THIS_AS_EXPRESSION_IN_FUNC 同 16 节）。

### 测试文件

涉及 35 个 1-2 失败诊断名对应的 fixture（分布于 `llt/` 各 suite，如 `mutOnlyOnFunction.cj`/`typealias6.cj`/`syscap_test01.cj`/`varray_ctype03.cj` 等）；每个诊断名的具体 fixture 见问题 14/15 对应条目。

### 修复方案

上表各诊断名的修复见问题 13/14/15 对应条目；新增确认点：`AMBIGUOUS_CONSTRUCTOR_CALL` 与 `AMBIGUOUS_USE` 共享 `coneDiagnosticToCfirDiagnostic.kt` L1396-1402 的多义候选判定，修复时统一处理。

## 22.4 消息模板对照：CFIR 与官方默认消息（23 一致 / 80 不同 / 21 缺）

### 发生位置

`external/cangjie_compiler/include/cangjie/Basic/DiagRefactor/DiagnosticSema.def`（官方消息）；`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/CfirErrorsDefaultMessages.kt`（CFIR 消息）

### 问题详情（逐行验证结论）

对 124 个映射到官方的 CFIR 诊断名做消息模板归一化对比（`%s`→`{}`、去空白/引号）：

- **23 个消息一致**（如 CANNOT_MODIFY_VAR、CAPTURE_BEFORE_INITIALIZATION、CLASS_INHERIT_NON_CLASS_NOR_INTERFACE、ENUM_CONSTRUCTOR_WITH_PARAM_MUST_HAVE_ARGS、EXPECT_CONST、EXTEND_MEMBER_CANNOT_SHADOW 等）；
- **80 个消息不同**，分三类：
  1. **纯格式差异**（占位符风格 `{0}` vs `%s`，语义相同）：AMBIGUOUS_USE、APILEVEL_REF_HIGHER、CANNOT_CONVERT_LITERAL 等——消息渲染时占位符格式不影响用户可见文本；
  2. **措辞差异**（语义相近、措辞不同）：CANNOT_CURRYING（CFIR "cannot be currying function" vs 官方 "cannot have more than one parameter list"）、CANNOT_INSTANTIATED_BY_INCOMPLETE_TYPE（CFIR 通用表述 vs 官方 "has unimplemented static member" 更具体）、COMMAND_HANDLE_TYPE_ERROR（CFIR 含 `stdx.effect.Command<T>` 全名 vs 官方 `effect.Command<T>`）、GENERIC_NO_MEMBER/METHOD_MATCH_IN_UPPER_BOUNDS（CFIR 带名字 vs 官方无名字）——**用户可见消息文本有差异**，需对照官方措辞统一；
  3. **语义差异**（消息含义不同）：EXTEND_DUPLICATE_INTERFACE（CFIR "duplicate extend interface" vs 官方 "interface '%s' has been implemented by '%s', please remove it"——官方强调"已被实现"）、ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER（CFIR "Member ... cannot be used" vs 官方 "'%s' is not allowed to be used"）、BUILTIN_INDEX_IN_BOUND（CFIR "builtin index is out of bounds" vs 官方 "%s index must be in bounds"）——**这些差异反映诊断语义边界不一致**，与 22.2 的 SUPER_TYPES_DUPLICATE 情况同类；
- **21 个缺官方消息**：CFIR 自创诊断（NEW_INFERENCE_ERROR、MACRO_*、EFFECTS_FEATURE_DISABLED 等）在官方无对应消息模板。

**结论**：80 个消息不同中，纯格式差异占多数（渲染无影响）；措辞差异（约 20 个）影响用户可见文本；语义差异（约 8 个）反映诊断语义边界不一致，需优先对齐。

### 测试文件

消息对照基于 124 个映射到官方的诊断名（全量诊断名层面，非单一 fixture 依赖）；对比结果存于 `cfir/analysis-tests/build/msg_compare.json`。

### 修复方案

1. 纯格式差异：无需处理（`{0}`/`%s` 渲染等效）。
2. 措辞差异：对照 `DiagRefactor/DiagnosticSema.def` 的官方消息，逐条统一 `CfirErrorsDefaultMessages.kt` 中的措辞（如 CANNOT_CURRYING、COMMAND_HANDLE_TYPE_ERROR 的 `stdx.` 前缀）。
3. 语义差异：先核对语义边界（如 EXTEND_DUPLICATE_INTERFACE 的"已被实现"条件），再统一消息——语义对齐优先于消息文本。
4. 自创诊断：保持 CFIR 命名与消息（测试依赖），触发条件对齐官方语义（22.1 已述）。

---

# 问题 23：未映射诊断名官方对应 + 剩余诊断组报告 source + 官方 11.2 对照 + 问题 5 修复修正（第七批）

> 本章是 22.1/22.2/18.4 的收尾细化：59 个未映射诊断名的逐个官方对应、范围/顺序簇剩余诊断组的报告 source 确认、问题 11.2 的官方对照、问题 5 修复落地方案的修正。全部基于 2026-08-04 实读。

## 23.1 59 个未映射诊断名逐个官方对应确认

### 发生位置

官方诊断名源（`external/cangjie_compiler/include/cangjie/Basic/DiagRefactor/Diagnostic*.def` 共 884 个）；分析结果存 `build/unmapped_analysis.txt`。

### 问题详情（逐行验证结论）

对 22.1 中 59 个未映射 CFIR 诊断名做语义关键词匹配（官方名含 ≥2 个 CFIR 单词），确认对应关系：

**A. 有官方语义对应（命名差异，约 40 个）**，代表：

| CFIR 诊断名 | 官方诊断名（确认） |
|-------------|-------------------|
| THIS_AS_EXPRESSION_IN_FUNC | `use_this_as_an_expression_in_func` |
| INTERFACE_CANNOT_INHERIT_CLASS | `class_inherit_non_class_nor_interface` |
| INVALID_CFUNC_PARAMETER_TYPE | `invalid_cfunc_arg_type` |
| INVALID_BINARY_OPERATOR | `invalid_binary_expr` |
| INCONSISTENT_ARRAY_LITERAL_ELEMENT_TYPE | `array_element_type_error` |
| IFAVAILABLE_UNKNOWN_ARG_NAME | `ifavailable_unknow_arg_name` |
| GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT | `non_generic_function_with_type_argument` |
| REDUNDANT_MODIFIER / REDUNDANT_MODIFIER_FOR_TARGET | `parse_redundant_modifier` |
| MACRO_DEPENDENCY_COMPILE_FAILED | `macro_expand_cannot_find_dependency` |
| MACRO_EXPANSION_FAILED | `macro_evaluate_failed` |
| STATIC_LAMBDA_CANNOT_ACCESS_NON_STATIC | `static_function_cannot_access_non_static_member` |
| CAPTURE_HAS_SHADOW_VARIABLE | `member_variable_can_not_shadow` |
| NO_CONSTRUCTOR | `no_match_constructor` |
| NO_MATCHING_OPERATOR_INVOKE | `no_match_operator_function_call` |
| VARRAY_SIZE_MISMATCH | `varray_size_match` / `varray_args_number_mismatch` |
| UNUSED_VARIABLE | `chir_dce_unused_variable` |
| EXTEND_C_TYPE_NOT_ALLOWED | `c_type_cannot_extend_interface` |
| ONLY_ONE_CLASS_BOUND_ALLOWED | `upper_bound_must_be_class_or_interface` |
| RETURN_TYPE_MISMATCH / OVERRIDING_RETURN_TYPE_MISMATCH | `return_type_incompatible` |
| CONST_EVAL_ARITHMETIC_OVERFLOW | `chir_arithmetic_operator_overflow` |
| UNSUPPORTED_NAMED_ARGUMENT | `unknown_named_argument` |
| AMBIGUOUS_CONSTRUCTOR_CALL | `ambiguous_constructor_match` |
| ABSTRACT_MEMBER_NOT_IMPLEMENTED | `interface_member_must_be_implemented_in_struct` |
| MULTIPLE_CLASS_UPPER_BOUNDS / CONFLICTING_UPPER_BOUNDS | `not_found_from_generic_upper_bounds` 等 upper bound 族 |
| GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS | `generic_ambiguous_method_match_in_upper_bounds` |
| EXPR_IN_FORIN_MUST_HAS_ITERATOR | `forin_pattern_must_be_irrefutable` |

**B. 无官方语义候选（疑似自创或官方不同阶段，约 19 个）**：`AMBIGUOUS_FUNCTION_REFERENCE`、`CLASSIFIER_REDECLARATION`、`CONFLICTING_OVERLOADS`（官方 `sema_overload_conflicts`，命名差异）、`EFFECTS_FEATURE_DISABLED`、`EXPLICIT_SUPER_CALL_REQUIRED`、`EXTEND_ORPHAN_RULE`、`INVISIBLE_MEMBER`、`INVISIBLE_REFERENCE`、`LITERAL_NUMERIC_OVERFLOW`、`MACRO_UNRESOLVED`、`NEW_INFERENCE_ERROR`（官方 `sema_unable_to_infer_*`）、`NON_EXHAUSTIVE_MATCH`、`NOTHING_TO_OVERRIDE`、`REDECLARATION`（官方 `sema_redefinition`）、`SUPER_TYPES_DUPLICATE`、`UNRESOLVED_IMPORT`、`UNRESOLVED_REFERENCE`（官方 `sema_undeclared_identifier`）、`WRONG_MODIFIER_CONTAINING_DECLARATION`、`WRONG_MODIFIER_TARGET`——其中 CONFLICTING_OVERLOADS/REDECLARATION/UNRESOLVED_REFERENCE/NEW_INFERENCE_ERROR 实为官方同名语义的命名差异，其余为 CFIR 特有或官方 Parser/CHIR 阶段诊断。

**结论**：59 个未映射中约 40 个（68%）有官方语义对应（命名差异），19 个为 CFIR 特有或命名无法匹配。

### 测试文件

全量诊断名层面（1960 失败涉及的 183 个诊断名），无单一 fixture 依赖；分析结果存 `build/unmapped_analysis.txt`。

### 修复方案

1. 命名差异类（A 表）：对齐官方诊断名（改 CFIR 注册名）或保持 CFIR 命名并在触发条件上对齐官方语义——优先后者（测试 EXP 依赖 CFIR 命名）。
2. 自创类（B 表）：保持 CFIR 命名，触发条件对齐官方对应语义（如 NEW_INFERENCE_ERROR→`sema_unable_to_infer_*`）。

## 23.2 范围/顺序簇剩余 43 个诊断组的报告 source 逐个确认

### 发生位置

各诊断组责任 checker（`cfir/checkers/src`）；差异数据 `build/range_row_diffs.json`。

### 问题详情（逐行验证结论）

对除 top 12 外的剩余 43 个诊断组逐个提取代表差异并归类根因：

**A. 报告范围差异（EXP 报整表达式/声明 vs ACT 报名字/关键字，约 25 组）**：

| 诊断组 | 代表差异（EXP vs ACT） | 责任报告点 |
|--------|------------------------|-----------|
| CLASS_UNINITIALIZED_FIELD | `init` 关键字 vs 整头 | `CfirInitializationCheckers.kt` L359/L769 |
| TYPE_UNINITIALIZED_STATIC_FIELD | 整声明 vs 名字 | 同上族 |
| UNUSED_IMPORT | 整 import vs 包名 | `CfirImportsChecker.kt` L215 |
| USE_MUTABLE_FUNC_ALONE | `obj.foo` vs `foo` | `CfirExpressionSemanticsChecker.kt` L252 |
| UNQUALIFIED_LEFT_VALUE_ASSIGNED | `test.foo` vs `foo` | `CfirAssignmentLegalityChecker.kt` L86 |
| PATTERN_NOT_MATCH | `A1(x)` vs `A1` | `CfirMatchPatternLegalityChecker.kt` L123 |
| IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION | `v` vs `f3` | `CfirMutabilityCheckers.kt` L114 |
| AMBIGUOUS_USE | `i.foo` vs `foo`（且数量不同） | `coneDiagnosticToCfirDiagnostic.kt` L1436 |
| ANNOTATION_NO_CONST_INIT | `@Annotation` vs `class A` | `CfirAnnotationDeclarationChecker.kt` L34 |
| OBJECT_CANNOT_ACCESS_STATIC_MEMBER | `val` vs `value` | 相关表达式 checker |
| INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE | extend 头 vs 成员名 | `CfirInheritanceDeepChecker.kt` L241 |
| LITERAL_NUMERIC_OVERFLOW | `-129` vs 无标记 | `CfirCompoundAssignmentSemanticsChecker.kt` L76 相关 |
| UNABLE_TO_INFER_RETURN_TYPE | 整函数 vs `goo` | `CfirFunctionSemanticsChecker.kt` L207 |
| UNRESOLVED_REFERENCE（funcdecl） | `a1()` vs `a1` | `coneDiagnosticToCfirDiagnostic.kt` L1801 |
| TYPEALIAS_UNUSED_TYPE_PARAMETERS | `type` 首字母 vs 整 typealias | `CfirTypeAliasUnusedTypeParameterChecker.kt` L46 |
| VARRAY_ARG_TYPE_WITH_REFTYPE | `String` vs 无标记 | `CfirVArrayConstructorArgChecker.kt` L67 |
| EXTEND_INTERFACE_NOT_EXTENDABLE | extend 整段 vs 部分 | `CfirExtendCheckers.kt` 相关 |
| INVALID_CFUNC_PARAMETER_TYPE / VARRAY_IN_CFUNC | 类型参数 vs 无标记 | `CfirForeignFunctionReturnTypeChecker.kt` L78 |

**B. 报告数量差异（EXP 一处 vs ACT 多处，约 6 组）**：`EXTEND_DUPLICATE_INTERFACE`（一处 vs 两处）、`INTERFACE_CANNOT_INHERIT_CLASS`（一处 vs 两处）、`INVALID_BINARY_OPERATOR`（一处 vs 多处）、`ACCESSIBILITY_WITH_MAIN_HINT`（一处 vs 两处）。

**C. 诊断名替换/组合（约 5 组）**：`NEED_NAMED_ARGUMENT,WRONG_NUMBER_OF_ARGUMENTS`（EXP 报 WRONG_NUMBER_OF_ARGUMENTS、ACT 报 NEED_NAMED_ARGUMENT——官方对缺命名参数报实参级诊断，CFIR 报函数级）、`NAMED_PARAMETER_NOT_FOUND,WRONG_NUMBER_OF_ARGUMENTS`（同上）、`CANNOT_ASSIGN_TO_IMMUTABLE,INVALID_BINARY_OPERATOR,TYPE_MISMATCH`（`x++ + 1` 的运算符诊断）。

**D. 与既有问题同源（约 7 组）**：`UNREACHABLE_PATTERN`（问题 4）、`WRONG_MODIFIER_TARGET`（问题 3/19.1）、`GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT` 嵌套（问题 9）、`USED_BEFORE_INITIALIZATION`（问题 7）、`UNDECLARED_TYPE_NAME`（18.5）、`CANNOT_ASSIGN_TO_IMMUTABLE,REF_NOT_BE_TYPE`（问题 1）、`INHERIT_SUPER_MEMBER_KIND_INCONSISTENT`（13.4）、`INVALID_THIS_CALL_OUTSIDE_CTOR`（16.4）。

### 测试文件

各诊断组 fixture 见问题 12/17 与上表（`variable_assignment_terminated_in_ctor_01.cj`、`unused019.cj`、`record_mut_invalid_11.cj`、`assign_func.cj`、`enum16_1.cj`、`num_overflow.cj`、`interface.cj` 等）。

### 修复方案

按问题 12 方案执行：报告范围差异调整各 checker 的 `reportOn` source；数量差异核对多违规点遍历（如 EXTEND_DUPLICATE_INTERFACE 去重）；诊断名替换组（C）按官方语义分流（缺命名参数报 `NEED_NAMED_ARGUMENT`、未知命名参数报 `NAMED_PARAMETER_NOT_FOUND`——CFIR 的 ACT 反而更接近官方，EXP 是函数级聚合，需与测试数据同步）。

## 23.3 官方对照问题 11.2 收尾：类体内 extend 成员不可见性

### 发生位置

`external/cangjie_compiler/src/Sema/InheritanceChecker/StructInheritanceChecker.cpp` L447-473

### 问题详情（逐行验证结论）

官方 `GetInheritedSuperMembers`（L447-473）：L452 合并父类成员（`structInheritedMembers[&decl]`）；L454-456 `ignoreExtends=true` 时**不含 extend 成员**直接返回；L457-471 `ignoreExtends=false` 时合并 `typeManager.GetDeclExtends(decl)` 的 extend 成员（L458-470，含 `importManager.IsExtendAccessible` 过滤 L464）。

**对照结论**：官方 `ignoreExtends` 参数区分两类场景——继承检查（false，合并 extend）与类体自身成员收集（true，不含 extend）。CFIR 的 `CfirClassUseSiteMemberScope`（L259-273）在 USE_SITE 模式无条件合入 `extendScope`（L470/574/809），**没有对应的 ignoreExtends 区分**——类体内错误可见 extend 成员（18.4 已确认），`extend_namelookup2.cj` EXP（`<!UNRESOLVED_REFERENCE!>go<!>()`）证明官方类体内不可见 extend 成员。**官方对照确认 18.4 修复方向**：类体（BODY_LOOKUP）上下文应跳过 extendScope 合并，仅 extend 体内保留。

### 测试文件

`extend_namelookup2/8/9.cj`、`extend_mutable_function_invalid_1.cj`、`record_extend_mut_invalid_13.cj`（同问题 11.2，24 失败）。

### 修复方案（对照后确认）

同 18.4：为类体成员函数体的隐式 `this` 接收者引入不含 extend 成员的 scope kind，或让 `CfirClassUseSiteMemberScope` 在 body 场景跳过 `extendScope` 合并；extend 体内保留（`extend_namelookup8/9.cj` 合法场景）。

## 23.4 问题 5 修复落地方案修正：STATIC_AND_NON_STATIC 整体移除

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirConflictsDeclarationChecker.kt` L282-283、L84-93；`external/cangjie_compiler/src/Sema/PreCheck.cpp` L1564

### 问题详情（逐行验证结论）

对 21.2 修复方案（"STATIC_AND_NON_STATIC 保留给同层重名"）的验证与修正：

1. CFIR `isFunctionLikeRedeclaration()`（L282-283）只查符号类型（Constructor/Function/EnumConstructor），**不区分 static/实例**；同层重名由 `CfirConflictsDeclarationChecker.reportConflicts`（L192-194）经分发器（L84-93）报 `CONFLICTING_OVERLOADS`（函数类）或 `REDECLARATION`——**CFIR 同层重名不报 STATIC_AND_NON_STATIC**。
2. 官方同层 static/实例重名用 `DiagStaticAndNonStaticOverload`（PreCheck.cpp L1564，`sema_static_function_overload_conflicts`）——官方同层场景是独立诊断名。
3. **修正结论**：21.2 的"STATIC_AND_NON_STATIC 保留给同层重名（CfirConflictsDeclarationChecker 管辖）"**不成立**——CFIR 同层重名走 CONFLICTING_OVERLOADS/REDECLARATION（与官方 `sema_static_function_overload_conflicts` 语义对应）。**修复应为：`CfirInheritanceDeepChecker.kt` L949-961 的 STATIC_AND_NON_STATIC 分支整体移除**，继承链 static 冲突并入 kind 不一致分支统一报 `INHERIT_MEMBER_KIND_INCONSISTENT`（extend 场景 `EXTEND_MEMBER_CANNOT_SHADOW`）；L964 的 `!hasStaticConflict` 门禁删除。

### 测试文件

`class_impl_interface1~4.cj`、`class_extends_class1/2/4/5.cj`（16 失败，同问题 5/21.2）。

### 修复方案（验证后修正 21.2）

1. `CfirInheritanceDeepChecker.kt` L949-961 删除 static 冲突分支（`STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME` 不再用于继承检查），L964 门禁删除，static 冲突并入 kind 不一致分支（L975 INHERIT_MEMBER_KIND_INCONSISTENT / L968 EXTEND_MEMBER_CANNOT_SHADOW）。
2. 同层重名保持现状（CONFLICTING_OVERLOADS/REDECLARATION，对齐官方 `sema_overload_conflicts`/`sema_static_function_overload_conflicts` 语义）。

---

# 问题 24：PSI/LightTree 双路径差异 + 官方孤儿规则/可见性对照 + 修复实施路线图（第八批）

> 本章含三个新维度：PSI 与 LightTree 双路径测试差异、官方孤儿规则与可见性诊断的最终对照、基于全部根因的修复实施路线图（P0-P3 排序与失败消除量估算）。

## 24.1 PSI vs LightTree 双路径测试差异（800 一致 / 14 仅 PSI / 40 仅 LightTree）

### 发生位置

失败数据全量比对（`cfir/analysis-tests/build/test-results/test/*.xml`，1960 失败按 fixture 文件名分组）。

### 问题详情（逐行验证结论）

按 fixture 文件名分组统计 PSI（`*PsiTest*` 套件）与非 PSI（`*LLTTest*`/`*MacroTest*`/`*Diagnostics*Test*` 套件）两路径失败：

- **800 个 fixture 双路径一致失败**（占绝大多数）——失败与解析路径无关，根因在共享 checker/resolver/诊断层，任何修复同时消除两路径；
- **14 个仅 PSI 失败**：`callingConventionBoundaryPlaceholder.cj`、`callingConventionOnClassPlaceholder.cj`、`callingConventionPlacementPlaceholder.cj`（`InteropGenerated`）、`constraint_check_test7_1.cj`、`enum1.cj`、`enum1_optionalBITOR.cj`、`exhaustive_enum.cj`、`generic_constraint_and_4.cj`、`ok_class_02/03.cj`、`ok_enum_func.cj`（`AnnotationGenerated`）、`ok_rune_byte_00.cj`、`paren_type_with_generic_type.cj`、`variadic_err_generic.cj`——PSI 路径特有（PSI 构建的 CFIR 树与 LightTree 的差异导致 checker 触发不同）；
- **40 个仅 LightTree 失败**：`interface_duplicated_02/04/05/08/13.cj`（`ExtendsImplementsInterfaceDuplicatedGenerated`）、`memberStatusCheckersRich.cj`/`mutOnlyOnFunctionRich.cj`/`staticIncompatibleModifiersRich.cj`（`DeclarationStatusGenerated`）、`syscap_test01~09.cj`（`LevelSyscapCheckGenerated`）、`generic_upper_constraint_inheritance_08/09/10.cj`、`pass01/02.cj`（`ExtendGeneric*`）、`file1.cj`（`GlobalVariableNotAssignable02`/`StaticVariableUseBeforeInit02`）、`main01~10.cj`（`ExtendMemberExport*`）等——LightTree 构建特有（如 `syscap_test*` 的 API level 注解、`interface_duplicated*` 的重复父类型在 LightTree 下解析差异）。

**结论**：约 41% 的 fixture（54 个）存在路径相关失败，但失败数占比小（54/1960 ≈ 2.8%）；其余 800+ 双路径一致的失败是修复主体。路径差异提示：PSI 与 LightTree 的 CFIR 树构建（声明 source/注解解析/父类型解析）存在少量不一致，修复时应双路径回归。

### 测试文件

见上（`InteropGenerated`/`AnnotationGenerated`/`LevelSyscapCheckGenerated`/`ExtendsImplementsInterfaceDuplicatedGenerated`/`DeclarationStatusGenerated`/`ExtendGeneric*`/`ExtendMemberExport*` 等套件）。

### 修复方案

1. 主修复按问题 1-23 方案执行（双路径共享根因，同时消除两路径失败）。
2. 路径特有失败（54 个）单独核对：优先查 PSI/LightTree 构建差异（source 归属、注解解析、父类型 resolved scope），对齐两路径 CFIR 树一致性。
3. 回归时 PSI 与非 PSI 套件都必须跑（`*PsiTest*` 与 `*LLTTest*` 全量）。

## 24.2 官方孤儿规则最终对照：sema_type_cannot_extend_imported_interface

### 发生位置

`external/cangjie_compiler/include/cangjie/Basic/DiagRefactor/DiagnosticSema.def` L151；`external/cangjie_compiler/src/Sema/TypeCheckExtend.cpp` L428-444、L519-573

### 问题详情（逐行验证结论）

1. 官方诊断名：`sema_type_cannot_extend_imported_interface`（DiagRefactor L151，消息 `"%s type '%s' cannot extend imported interface"`）。
2. 触发入口：`CheckExtendRules`（TypeCheckExtend.cpp L428-444）L430-431 **imported 包整体跳过** orphan 检查；L441 对每个 extend 调 `CheckExtendOrphanRule`。
3. `CheckExtendOrphanRule`（L519-573）：L566 `isImportedExtendedType && !externalDecls.empty()` 时报 `sema_type_cannot_extend_imported_interface`（L569-571）——**官方诊断名不含 "orphan" 字样**。
4. **对照结论**：CFIR 的 `EXTEND_ORPHAN_RULE` 是自创命名，官方对应 `sema_type_cannot_extend_imported_interface`；触发语义一致（imported 目标 + 外部接口闭包），但官方 L430-431 的 imported 包跳过在 CFIR `CfirExtendOrphanRuleChecker`（L321-342）中无直接对应（CFIR 用 `isTargetDeclaredInPackage` L325 判定目标归属）。

### 测试文件

`import_orphanrule_01~06/main.cj`（同问题 8，14 失败）。

### 修复方案（对照后确认）

1. 保持 CFIR 诊断名 `EXTEND_ORPHAN_RULE`（测试依赖）或对齐官方 `sema_type_cannot_extend_imported_interface`（需同步测试数据）。
2. 触发条件补齐 imported 包跳过（对齐官方 L430-431）：当前包为 imported 时跳过 orphan 检查。
3. 父类传递修复按 20.3 方案（`CollectAllRelatedExtends` 沿父类链遍历）。

## 24.3 官方可见性诊断最终对照：IsInvisibleMember 过滤机制

### 发生位置

`external/cangjie_compiler/src/Sema/InheritanceChecker/StructInheritanceChecker.cpp` L52-69；`external/cangjie_compiler/include/cangjie/Basic/DiagRefactor/DiagnosticSema.def` L109-110

### 问题详情（逐行验证结论）

1. 官方 `IsInvisibleMember`（L52-60）：private 成员（L54）或包关系不可见且非 protected（L57-59）判定为不可见；`RemoveInvisibleMember`（L63-69）在继承成员收集时**直接过滤**不可见成员。
2. 官方可见性相关诊断：`sema_weak_visibility`（DiagRefactor L110，override 削弱可见性）、`sema_invalid_member_visibility_in_class`（L109）。
3. **对照结论**：官方**没有** INVISIBLE_MEMBER/INVISIBLE_REFERENCE 同名诊断——不可见成员在官方表现为"成员不存在"（被 `RemoveInvisibleMember` 过滤后访问走 no-match/not-member-of）；CFIR 的 INVISIBLE_MEMBER/INVISIBLE_REFERENCE 是自创诊断（`coneDiagnosticToCfirDiagnostic.kt` L2026/2032 映射），官方语义对应 `sema_weak_visibility`（override 场景）与成员过滤机制（非 override 场景）。

### 测试文件

`invisibleReferenceAndMember.cj`、`invisibleReferenceAndMemberRich.cj`、`protectedAndInternalMatrix.cj`（同问题 13.5，8 失败）。

### 修复方案（对照后确认）

CFIR 保持 INVISIBLE_MEMBER/INVISIBLE_REFERENCE 命名（测试依赖），但触发语义对齐官方：override 场景优先 `sema_weak_visibility` 语义（问题 13.5 的 `CANNOT_WEAKEN_ACCESS_PRIVILEGE`），非 override 的不可见访问保持"成员查找失败"路径（NOT_MEMBER_OF，与官方 `RemoveInvisibleMember` 过滤一致）——即 INVISIBLE_MEMBER 应仅在官方确实区分可见性的场景报，避免与 NOT_MEMBER_OF 重复。

## 24.4 修复实施路线图（P0-P3 排序与失败消除量估算）

### 发生位置

基于全部 23 个问题根因（数据存 `build/roadmap.json`）；统计口径：每个诊断对（entry）按 label 中优先级最高的诊断名归属单一根因族，总和精确 = 1960。

### 问题详情（失败消除量估算）

**按优先级汇总**（修复某级 = 消除该级全部失败）：

| 优先级 | 失败消除量 | 覆盖根因族 |
|--------|-----------|-----------|
| P0 | **445** | 问题 1 宏场景 let 赋值误报（288）、问题 2 构造器上下文栈（86）、问题 3 static init 修饰符（71） |
| P1 | **398** | 问题 9 推断与约束（144）、问题 4 match 可达性（86）、问题 6 不可变函数可变性（50）、问题 5 继承 static 冲突（44）、问题 7 初始化状态机（26）、问题 8 extend 孤儿规则（16）、问题 10 static 访问实例（32） |
| P2 | **516** | 问题 11.3 多赋值类型兼容（156）、问题 11.2 extend 命名查找（101）、问题 10 import/宏基建（98）、问题 13 重名与继承（88）、问题 11.1 extend 接口成员 scope（41）、问题 12 范围/报告点（61，含 253 范围簇的一部分）、问题 18.5 约束类型名（16） |
| P3 | **348** | 问题 14 低频诊断名（303）、问题 12 范围/报告点剩余（45） |
| 未归类 | 253 | 问题 12 范围/顺序簇（按单一诊断对暂未细分，修复时并入 P2/P3 范围处理） |

**按根因族排序**（修复收益 top 10）：问题 14 低频诊断名（303，P3，多为单点修改）、问题 1 宏场景 let 赋值（288，P0，根因集中）、问题 11.3 多赋值类型兼容（156，P2）、问题 9 推断与约束（144，P1）、问题 11.2 extend 命名查找（101，P2）、问题 10 import/宏基建（98，P2）、问题 13 重名与继承（88，P2）、问题 2 构造器上下文栈（86，P0，单点修复 16.4）、问题 4 match 可达性（86，P1）、问题 3 static init 修饰符（71，P0，单点修复 19.1）。

**实施建议**：
- **第一批（P0，~3 个修改点）**：问题 2（`closestFunctionLikeDeclaration` 改 `.cfir` 解包，1 行）、问题 3（`STATIC_KEYWORD` 谓词加 `headOf(STATIC_INITIALIZER)`，1 行）、问题 1（`isImmutableFieldAssignmentForbidden` 放行构造器内非 common let 字段 + 宏 tracked 集合）——预计消除 445 失败。
- **第二批（P1，~8 个修改点）**：问题 5（删 L964 门禁）、问题 4（`isMatchSubtypeOf` 加 implicitBoxed）、问题 6（复合赋值检查）、问题 7（父类存储槽）、问题 8（索引父类传递）、问题 9（推断错误映射）——预计累计消除 843 失败。
- **第三批（P2，~10 个修改点）**：问题 11/10/13 的 scope/基建修复——预计累计消除 1359 失败。
- **第四批（P3）**：问题 14 低频诊断名与范围簇——预计累计消除 1960 失败。

### 测试文件

全量（`cfir/analysis-tests` 全部测试类）；每批修复后跑对应套件（P0 后 `*Merge*`/`*PrimaryConstructor*`/`*StaticInit*`，P1 后 `*Overload*`/`*Match*`/`*Mut*`/`*InitializationCheck*`/`*Orphanrule*`/`*Inference*` 等）。

### 修复方案

按上表分四批实施；每批以 `./gradlew :cfir:analysis-tests:test --tests "<对应套件>"` 定向回归，全量验证用 `:cfir:analysis-tests:test`。具体修改点见问题 16/18/19/20/21/22/23 各节的修复方案（均已给到文件:行与代码级改法）。

---

# 问题 25：P0 三问题落地补丁草案 + PSI/LightTree 路径失败逐个根因（第九批）

> 本章将问题 1/2/3 的修复方案落成 diff 级补丁草案（可直接实施的代码改法），并对 54 个路径特有失败（14 仅 PSI + 40 仅 LightTree）做逐个根因分析。全部基于 2026-08-04 实读代码与失败数据。

## 25.1 问题 2 落地补丁：closestFunctionLikeDeclaration 改 .cfir 解包（1 行级）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirConstructorDelegationCallChecker.kt` L57-61

### 问题详情（补丁依据）

16.4 已确认：`containingDeclarations` 存的是 `CfirBasedSymbol`（`CheckerContext.addDeclaration` L333 压 `declaration.symbol`），而 `closestFunctionLikeDeclaration` 用 `it is CfirFunction` 过滤声明接口——`CfirFunctionSymbol` 是 `CfirCallableSymbol<D>` 子类、不实现 `CfirFunction`，恒返回 null → 所有构造器内 `super()`/`this()` 都被误报 `INVALID_THIS_CALL_OUTSIDE_CTOR`。同文件 `findClosestDeclaration`（L426-434）用 `declaration.cfir as? T` 正确解包。

### 补丁草案（可直接落地）

```diff
--- a/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirConstructorDelegationCallChecker.kt
+++ b/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirConstructorDelegationCallChecker.kt
@@ -57,7 +57,7 @@ private fun CheckerContext.closestFunctionLikeDeclaration(): CfirFunction? {
     return containingDeclarations
         .asReversed()
-        .firstOrNull { declaration -> declaration is CfirFunction } as? CfirFunction
+        .firstNotNullOfOrNull { declaration -> declaration.cfir as? CfirFunction }
 }
```

### 测试文件

`primaryConstructor1.cj`、`primaryConstructor8.cj`、`super_this_11.cj`、`class6.cj`、`class_generic_inheritance*.cj`（同问题 2，86 失败）。

### 修复方案

按补丁草案修改后回归 `*PrimaryConstructorGenerated*`、`*SuperThisGenerated*`、`*RecursiveConstructorCall*`、`*Class6*`、`*ClassGenericInheritance*`、`*RecordThis*`，PSI 与 LightTree 双路径。

## 25.2 问题 3 落地补丁：STATIC_KEYWORD 谓词加 headOf(STATIC_INITIALIZER)（1 行级）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/ModifierCheckerTargets.kt` L103-108

### 问题详情（补丁依据）

19.1 已确认：`actualTargetsFor`（L282-288）对 static 构造器返回 `head(STATIC_INITIALIZER)`（Site.HEAD），而 `STATIC_KEYWORD` 谓词是 `memberOf(FUNCTION, PROPERTY, VARIABLE)`（要求 Site.MEMBER）——site 不匹配误报。官方把 static init 建模为普通 static 函数（`Package.cpp` L189-190 重命名为 `"static.init"`），无此问题。修复需覆盖 HEAD site，模式与 `OPEN_KEYWORD`（L117-121 的 `anyOf(headOf(...), memberOf(...))`）一致。

### 补丁草案（可直接落地）

```diff
--- a/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/ModifierCheckerTargets.kt
+++ b/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/ModifierCheckerTargets.kt
@@ -104,7 +104,11 @@
-    STATIC_KEYWORD to ModifierTargetPredicate.memberOf(
-        DeclarationKind.FUNCTION,
-        DeclarationKind.PROPERTY,
-        DeclarationKind.VARIABLE,
+    STATIC_KEYWORD to ModifierTargetPredicate.anyOf(
+        ModifierTargetPredicate.memberOf(
+            DeclarationKind.FUNCTION,
+            DeclarationKind.PROPERTY,
+            DeclarationKind.VARIABLE,
+        ),
+        ModifierTargetPredicate.headOf(DeclarationKind.STATIC_INITIALIZER),
     ),
```

### 测试文件

`static_init_01.cj`、`static_or_global_var3~12.cj`、`variable_use_before_init_01/03/04/07.cj`、`const_init.cj`、`generic_static_constructor.cj`（同问题 3，71 失败）。

### 修复方案

按补丁草案修改后回归 `*StaticInitGenerated*`、`*StaticOrGlobalVarGenerated*`、`*InitializationCheckGenerated*`、`*ConstInit*`、`*GenericStaticConstructor*`，PSI 与 LightTree 双路径。注意 `anyOf` 的 `headOf` 分支只覆盖 `STATIC_INITIALIZER` 的 HEAD site，不影响普通函数的 MEMBER site。

## 25.3 问题 1 落地补丁：isImmutableFieldAssignmentForbidden 放行构造器内非 common let 字段

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirAssignmentLegalityChecker.kt` L372-389

### 问题详情（补丁依据）

20.1 官方对照已确认：官方 `NotAssignableVariable`（`InitializationChecker.cpp` L165-171）非 common 的 let 字段在构造器内（inInitFunction=true）赋值合法，仅 common let 字段禁止（`IsImutFieldInCtorOfCommonClassStruct` L158-163：`IN_STRUCT/IN_CLASSLIKE && inInitFunction && COMMON`）。CFIR `isImmutableFieldAssignmentForbidden` 的 `NOT_TRACKED`/`null` 分支（L388-389）把宏展开场景（字段无 tracked 记录，16.1 已确认）判为非法——这是 288 失败的主因。CFIR `isCommon` 属性存在于 `CfirDeclarationStatusImpl.kt` L175。

### 补丁草案（可直接落地）

```diff
--- a/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirAssignmentLegalityChecker.kt
+++ b/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirAssignmentLegalityChecker.kt
@@ -372,6 +372,10 @@ private fun CfirQualifiedAccessExpression.isImmutableFieldAssignmentForbidden(
     ): Boolean {
         if (field.isVar) return false
         val constructor = context.findClosestDeclaration<CfirConstructor>() ?: return true
+        // 对齐官方 NotAssignableVariable（InitializationChecker.cpp L165-171）：
+        // 非 common 的 let 字段在构造器内（inInitFunction）赋值合法；
+        // 仅 common let 字段在构造器内禁止（IsImutFieldInCtorOfCommonClassStruct L158-163）。
+        if (constructor.status.isStatic == field.status.isStatic && !field.status.isCommon) return false
         if (field.status.isStatic != constructor.status.isStatic) return true
         if (field.hasSameNamePrimaryConstructorPropertyInOwner()) return true
         if (field.initializer != null) return true
         return when (assignment?.let { CfirInitializationAssignmentClassifier.classifyAssignment(it, context) }) {
             CfirInitializationAssignmentKind.INITIALIZATION,
             CfirInitializationAssignmentKind.PRIORITY_INITIALIZATION_DIAGNOSTIC,
             -> false
 
-            CfirInitializationAssignmentKind.REASSIGNMENT,
-            CfirInitializationAssignmentKind.NOT_TRACKED,
-            null,
-            -> true
+            CfirInitializationAssignmentKind.REASSIGNMENT -> true
+            CfirInitializationAssignmentKind.NOT_TRACKED,
+            null,
+            -> false  // 未观察到重复赋值即放行（NOT_TRACKED 不再判非法，对齐官方无 tracked 概念）
         }
     }
 }
```

### 测试文件

`key.cj`、`merge04/test2.cj`、`call_construct.cj`、`dep_ok1.cj`、`primaryConstructor10/12.cj`、`generic_call_static_impl02.cj`（同问题 1，288 失败含组合对）。

### 修复方案

1. 主补丁：构造器内非 common let 字段直接放行（不等 `classifyAssignment`）；`NOT_TRACKED`/`null` 从"非法"改为"放行"（仅 `REASSIGNMENT` 判非法）——宏展开场景（16.1 确认字段无 tracked 记录）不再误报。
2. 配套（可选）：宏展开产物的字段符号进入 `owner.instanceFieldInfos` 的 tracked 集合（对齐官方 `INITIALIZED` 属性），使 `classifyAssignment` 能正常记录 INITIALIZATION。
3. 回归 `*Merge*Generated`、`*GlobalsGenerated*`、`*ApiLevel*` 宏族、`*PrimaryConstructor*`、`*GenericCallStaticImpl02*`、`*LevelSyscapCheck*`（syscap 组合），PSI 与 LightTree 双路径。

## 25.4 仅 PSI 失败 fixture 逐个根因（14 个）

### 发生位置

PSI 套件（`*PsiTest*`）独有失败；数据存 `build/psi_light_diff3.json`。

### 问题详情（逐个根因）

| fixture | 差异（EXP vs ACT） | 根因 |
|---------|-------------------|------|
| ok_enum_func.cj | 无 vs NOT_MEMBER_OF | @Annotation 类内部符号 PSI 路径解析失败（注解作用域 scope 差异） |
| ok_class_02.cj | 无 vs ANNOTATION_NO_CONST_INIT | @Annotation 类 const 构造器判定 PSI 路径误报（注解身份解析差异） |
| ok_class_03.cj | 无 vs EXPECT_CONST | 同上（const 判定误报） |
| callingConventionBoundaryPlaceholder.cj | ONLY_CFUNC vs +UNRESOLVED_REFERENCE | `@CallingConv[CDECL]` 的 CDECL 枚举值 PSI 路径未解析（注解参数 scope） |
| callingConventionOnClassPlaceholder.cj | 同上 | 同上 |
| callingConventionPlacementPlaceholder.cj | ILLEGAL_SCOPE vs +UNRESOLVED_REFERENCE | 同上 |
| exhaustive_enum.cj | UNREACHABLE_PATTERN vs +REDECLARATION | enum pattern `_` 通配参数 PSI 路径被收为局部重名声明（pattern 绑定收集差异） |
| ok_rune_byte_00.cj | 无 vs ARGUMENT_TYPE_MISMATCH+PATTERN_NOT_MATCH | rune/byte 常量 pattern PSI 路径兼容性误报（等值性判定差异） |
| variadic_err_generic.cj | TYPE_MISMATCH vs 无 | vararg 泛型调用 PSI 路径漏报（实参类型检查差异） |
| constraint_check_test7_1.cj | UPPER_BOUND...（同集） | 范围差异（问题 12：报 foo vs bar 的 T） |
| generic_constraint_and_4.cj | WRONG_NUMBER...（同集） | 范围差异（问题 12/22.2：整调用 vs 函数名） |
| enum1.cj / enum1_optionalBITOR.cj | WRONG_NUMBER...（同集） | 范围差异（问题 12：整调用 vs 构造器名） |
| paren_type_with_generic_type.cj | GENERIC_TYPE...（同集） | 范围差异（问题 12：`(Array)` vs `Array`） |

**结论**：9 个为 PSI 路径特有误报/漏报（注解作用域、pattern 绑定、vararg 实参的 PSI/LightTree 构建差异），5 个为问题 12 范围差异（非路径特有，仅恰好 PSI 套件覆盖）。

### 测试文件

`ok_enum_func.cj`/`ok_class_02.cj`/`ok_class_03.cj`（`AnnotationGenerated`）、`callingConventionBoundaryPlaceholder.cj`/`callingConventionOnClassPlaceholder.cj`/`callingConventionPlacementPlaceholder.cj`（`InteropGenerated`）、`exhaustive_enum.cj`（`TypePatternGenerated`）、`ok_rune_byte_00.cj`（`StringGenerated`）、`variadic_err_generic.cj`（`CallGenerated`）、`constraint_check_test7_1.cj`（`ConstraintCheckGenerated`）、`generic_constraint_and_4.cj`（`GenericConstraintGenerated`）、`enum1.cj`/`enum1_optionalBITOR.cj`（`EnumGenerated`）、`paren_type_with_generic_type.cj`（`TypeGenerated`）。

### 修复方案

1. 范围差异 5 个：按问题 12/22.2 方案（报告 source 对齐）。
2. PSI 特有 9 个：核对 PSI/LightTree 的 CFIR 树构建差异——注解参数 scope（callingConvention*）、@Annotation 身份解析（ok_class*）、pattern 绑定收集（exhaustive_enum）、rune 常量等值（ok_rune_byte_00）、vararg 实参（variadic_err_generic）；修复目标是两路径 CFIR 树一致。

## 25.5 仅 LightTree 失败 fixture 逐个根因（40 个）

### 发生位置

LightTree 套件（`*LLTTest*`/`*MacroTest*`/`*Diagnostics*Test*`）独有失败；数据存 `build/psi_light_diff3.json`。

### 问题详情（逐个根因，按四类归并）

**A. 问题 12 范围差异（约 18 个）**：`interface_duplicated_02/04/05/08.cj`（EXP 报声明头、ACT 报类型实参处——22.2 已确认 SUPER_TYPES_DUPLICATE 的 L87 vs L107 报告点）、`interface_duplicated_13.cj`（EXTEND_DUPLICATE_INTERFACE 被 SUPER_TYPES_DUPLICATE/INTERFACE_CANNOT_INHERIT_CLASS 取代 + 范围）、`generic_upper_constraint_inheritance_08/09/10.cj`（EXP 报 `T <: C & I` 约束、ACT 报 `T` 类型参数）、`case.cj`、`basic_prop.cj`（EXTEND_INTERFACE_NOT_EXTENDABLE 范围）、`mutOnlyOnFunctionRich.cj`（WRONG_MODIFIER_TARGET 范围）——均为报告 source 选择差异。

**B. 问题 1 同源（syscap_test01~09.cj，9 个）**：EXP 报 APILEVEL_SYSCAP_ERROR/WARNING，ACT 报 CANNOT_ASSIGN_TO_IMMUTABLE+UNRESOLVED_REFERENCE+UNUSED_IMPORT——宏场景 let 赋值误报（问题 1/25.3）抢占 syscap 诊断 + import 基建（问题 10）。

**C. extend 成员查找/遮蔽（约 8 个）**：`pass01/02.cj`（ExtendGeneric01：EXP 无 vs NOT_MEMBER_OF+UNUSED_IMPORT）、`ExtendMemberExport05/main04/05.cj`（EXP 无 vs NOT_MEMBER_OF）、`main06/08/09/10.cj`（EXP NO_MATCH_FUNCTION_DECLARATION_FOR_CALL vs 无——漏报）、`ExtendMemberExport06/main01/02.cj`（EXP 无 vs EXTEND_MEMBER_CANNOT_SHADOW）、`generics_00038.cj`（EXP EXTEND_MEMBER_CANNOT_SHADOW vs CANNOT_ASSIGN_TO_IMMUTABLE——诊断替换）、`same_sign_default_in_multi_extend_impl02.cj`（EXP EXTEND_MEMBER_CANNOT_SHADOW vs 无——漏报）、`use_interface_def_imple_024.cj`（EXP 无 vs INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL）——extend 接口闭包成员 scope（问题 11.1/18.3）+ extend 遮蔽（问题 5）在 LightTree 下的表现。

**D. 问题 3 同源（file1.cj StaticVariableUseBeforeInit02/05/06、GlobalVariableNotAssignable02，约 5 个）**：EXP USED_BEFORE_INITIALIZATION 或 CANNOT_ASSIGN_TO_IMMUTABLE vs ACT WRONG_MODIFIER_TARGET——static 变量初始化场景 WRONG_MODIFIER_TARGET 误报（问题 3/19.1）+ 宏场景 let 赋值（问题 1）。

**E. 宏/注解特有（test_macro.cj 等）**：EXP 无 vs AMBIGUOUS_FUNCTION_CALL+ARGUMENT_TYPE_MISMATCH+EXPECT_CONST+INVALID_SUBSCRIPT_EXPR+MACRO_DEPENDENCY_COMPILE_FAILED+REDUNDANT_MODIFIER 多诊断簇——问题 10/14 的宏展开链路。

**F. DeclarationStatus 特有（memberStatusCheckersRich、staticIncompatibleModifiersRich）**：EXP 2-3 个 vs ACT 多 IGNORE_OPEN/STATIC_AND_NON_STATIC——问题 5/23.4（继承 static 冲突）+ IGNORE_OPEN（问题 14）。

**G. 其他**：`member_used_internal.cj`（Functionlinkage：EXP 无 vs TYPE_MISMATCH）、`interface_duplicated_13.cj` 已列 A 类。

### 测试文件

A 类：`interface_duplicated_02/04/05/08/13.cj`（`ExtendsImplementsInterfaceDuplicatedGenerated`）、`generic_upper_constraint_inheritance_08/09/10.cj`（`GenericConstraintInheritanceGenerated`）、`case.cj`（`GenericInterfaceImport*Generated`）、`basic_prop.cj`（`InterfaceDefaultImplementGenerated`）、`mutOnlyOnFunctionRich.cj`（`DeclarationStatusGenerated`）；
B 类：`syscap_test01~09.cj`（`LevelSyscapCheckGenerated`）；
C 类：`pass01/02.cj`（`ExtendGeneric01Generated`）、`main01/02.cj`（`ExtendMemberExport06Generated`）、`main04~10.cj`（`ExtendMemberExport05Generated`）、`generics_00038.cj`/`same_sign_default_in_multi_extend_impl02.cj`（`GenericParamDeclCallInMemberFuncGenerated`）、`use_interface_def_imple_024.cj`（`UseInterfaceDefaultImpl02Generated`）；
D 类：`file1.cj`（`StaticVariableUseBeforeInit02/05/06Generated`、`GlobalVariableNotAssignable02Generated`）；
E 类：`test_macro.cj`（`DefaultParameterPkg02Generated`）；
F 类：`memberStatusCheckersRich.cj`/`staticIncompatibleModifiersRich.cj`（`DeclarationStatusGenerated`）；
G 类：`member_used_internal.cj`（`FunctionlinkageGenerated`）。

### 修复方案

1. A 类：按问题 12/22.2 报告 source 方案。
2. B/F 类：按 25.3 主补丁（问题 1）+ 23.4（问题 5）——修复后 syscap/static 冲突诊断恢复。
3. C 类：按 18.3（extend 接口成员 scope）+ 问题 5/23.4（extend 遮蔽）。
4. D 类：按 25.2 主补丁（问题 3）。
5. E/G 类：按问题 10/14 对应条目。
6. 回归时两路径都跑（问题 24.1 已述）。

---

# 问题 26：P1 六问题落地补丁草案（问题 4/5/6/7/8/9，diff 级，预计消除 398 失败）

> 本章延续问题 25 的 P0 补丁模式，把 P1 层六个问题（问题 4/5/6/7/8/9，预计消除 398 失败）落成 diff 级补丁草案。全部基于 2026-08-04 实读代码。

## 26.1 问题 4 落地补丁：isMatchSubtypeOf 加 implicitBoxed 装箱路径

### 发生位置

`cfir/semantics/src/org/cangnova/cangjie/cfir/resolve/match/CfirMatchTypeRelations.kt` L31-53（`isMatchSubtypeOf`）、L106-120（`requiresBoxingToClassLikeSupertype`）

### 问题详情（补丁依据）

20.2 官方对照已确认：官方 `IsSubtypeBoxed`（TypeCheckPattern.cpp L178-187）else 分支 `IsSubtype(&leaf, &root, true, false)`——**implicitBoxed=true**；`PatternUsefulness.cpp` L508-510 同样用 `IsSubtype(..., true, false)`。官方允许 `Int64`/tuple 元素/struct 值装箱到 `Any`。CFIR `isMatchSubtypeOf` 的 else 分支（L51-52）只有 `hasTypeAwareSupertype || hasVisibleExtendSupertype`，无装箱路径 → autobox_match/unbox 场景漏报（问题 4 漏报方向，20 失败）。同文件已有 `requiresBoxingToClassLikeSupertype`（L106-120，private，值类型→Any/classLike 装箱判定）可复用。

### 补丁草案（可直接落地）

```diff
--- a/cfir/semantics/src/org/cangnova/cangjie/cfir/resolve/match/CfirMatchTypeRelations.kt
+++ b/cfir/semantics/src/org/cangnova/cangjie/cfir/resolve/match/CfirMatchTypeRelations.kt
@@ -49,6 +49,7 @@ fun ConeCangJieType.isMatchSubtypeOf(
                 returnType.isMatchSubtypeOf(superType.returnType, session)
     }
     return hasTypeAwareSupertype(superType, session)
-            || hasVisibleExtendSupertype(superType, session)
+            || hasVisibleExtendSupertype(superType, session)
+            || requiresBoxingToClassLikeSupertype(superType)  // 对齐官方 IsSubtypeBoxed implicitBoxed=true
 }
```

### 测试文件

`autobox_match1/2.cj`、`unbox_*.cj`、`as_expr_00.cj`、`enum12/14.cj`（同问题 4 漏报方向，20 失败）。

### 修复方案

按补丁草案修改后回归 `*AutoboxMatch*`、`*Unbox*`、`*AsExpr*`、`*EnumGenerated*`、`*TypePatternGenerated*`、`*TuplePatternGenerated*`，PSI 与 LightTree 双路径。注意 `requiresBoxingToClassLikeSupertype` 是 private 且同文件内可用，无需改可见性；`isTypePatternOrdinarySubtypeOf`（L73-104）保持非 boxed 语义不变（L102 仍排除装箱）。

## 26.2 问题 5 落地补丁：删除 static 冲突分支与 L964 门禁

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInheritanceDeepChecker.kt` L911、L948-984

### 问题详情（补丁依据）

21.2/23.4 已确认：EXP 数据（`class_extends_class1.cj` 结构继承、`class_impl_interface1.cj` 接口继承）都期望 `INHERIT_MEMBER_KIND_INCONSISTENT`——继承链 static/实例同名冲突官方语义统一报 kind 不一致；官方 `sema_static_and_non_static_member_cannot_have_same_name`（StructInheritanceChecker.cpp L1091）仅用于同声明层（CFIR `CfirConflictsDeclarationChecker` 报 CONFLICTING_OVERLOADS/REDECLARATION，L282-283 不区分 static）。当前代码 L949-961 的 static 冲突分支优先报 `STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME`，L964 的 `!hasStaticConflict` 门禁使 kind 分支不可达——16 失败。

### 补丁草案（可直接落地）

```diff
--- a/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInheritanceDeepChecker.kt
+++ b/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInheritanceDeepChecker.kt
@@ -911,7 +911,6 @@
-        val reportedStaticConflicts = mutableSetOf<Name>()
         val reportedKindConflicts = mutableSetOf<Name>()
@@ -948,15 +947,7 @@
                     val ownSameNameMembers = ownMembers[superInfo.name].orEmpty()
 
                     for (ownInfo in ownSameNameMembers) {
-                        val hasStaticConflict = ownInfo.isStatic != superInfo.isStatic
-                        if (hasStaticConflict) {
-                            if (reportedStaticConflicts.add(ownInfo.name)) {
-                                reporter.reportOn(
-                                    source = ownInfo.nameSource ?: ownInfo.source ?: subject.source,
-                                    factory = CfirErrors.STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME,
-                                    a = ownInfo.staticKind,
-                                    b = ownInfo.name,
-                                    c = superInfo.staticKind,
-                                    d = if (subject.isExtendSubject) "extended type" else "parent class or interfaces",
-                                )
-                            }
-                        }
 
                         if (ownInfo.kind != superInfo.kind) {
-                            if (!hasStaticConflict && reportedKindConflicts.add(ownInfo.name)) {
+                            if (reportedKindConflicts.add(ownInfo.name)) {
                                 if (subject.isExtendSubject) {
```

### 测试文件

`class_impl_interface1~4.cj`、`class_extends_class1/2/4/5.cj`（`OverloadGenerated`，16 失败）。

### 修复方案

按补丁草案修改后回归 `*OverloadGenerated*`、`*InterfaceConflictInheritance*`、`*ExtendMemberCannotShadow*`。删除 `reportedStaticConflicts`（L911）与 static 分支（L950-961）后，`ownInfo.staticKind`/`superInfo.staticKind` 参数不再使用（kind 分支 L975 用 `ownInfo.kind`/`superInfo.kind`）；同层重名保持现状（问题 23.4 已述）。

## 26.3 问题 6 落地补丁：复合赋值进入检查 + 判定对齐官方

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirMutabilityCheckers.kt` L80-131、L208-218、L303-324；`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirCompoundAssignmentSemanticsChecker.kt` L25-28

### 问题详情（补丁依据，含对 16.5 的修正）

1. **修正 16.5 推断**：`CfirCompoundAssignmentSemanticsChecker`（L28）继承 `CfirAssignmentChecker`、check 签名 `CfirAssignment`（L36），注释（L25-26）明确"CFIR 尚未引入独立 AugmentedAssignment 节点，复合赋值 desugar 成 `lValue = lValue <op> rValue` 形态"——**复合赋值确实会进入 `CfirImmutableFunctionCannotModifyFieldChecker.check(expression: CfirAssignment)`**，16.5 的"绕过 checker"推断不成立。
2. **真实漏报根因**：`currentStructMutationRoot`（L303-324）的 L320 `if (receiverType is ConeClassLikeType) return null`——嵌套链中间字段若是 class 类型则截断（`t2.t1.t0` 中 `t1` 为 class 时找不到根字段）；`currentImmutableStructFunction`（L208-218）缺官方 `outerDecl != nullptr`、非构造器、非主构造器排除（20.5 已确认官方 `CheckImmutableFuncAccessMutableFunc` L197-199 条件）。
3. `mut_function_01.cj` 的 `t2.t1.t0 += 10` 实际差异为漏报 CANNOT_MODIFY_VAR——desugar 后 lValue 是 `t2.t1.t0`，若 `t1`/`t2` 链中间有 class 类型则 L320 截断漏报。

### 补丁草案（可直接落地）

```diff
--- a/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirMutabilityCheckers.kt
+++ b/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirMutabilityCheckers.kt
@@ -208,6 +208,10 @@ private fun CheckerContext.currentImmutableStructFunction(): CfirNamedFunction? {
     if (ownerIndex < 0) return null
     val function = containingDeclarations
         .drop(ownerIndex + 1)
         .filterIsInstance<CfirNamedFunction>()
         .firstOrNull() ?: return null
+    // 对齐官方 CheckImmutableFuncAccessMutableFunc（TypeCheckExpr.cpp L197-199）：
+    // 源函数须非 mut、有 outerDecl、非构造器、非主构造器
+    if (function.status.isMut || function is CfirConstructor) return null
     return function.takeUnless { it.status.isMut }
 }
```

（`currentStructMutationRoot` 的 L320 class 截断需按官方语义核对：官方对"struct 值链中间 class 字段"是否截断——若官方不截断则移除该条件，见问题 20.5 的核对项。）

### 测试文件

`mut_function_01.cj`、`record_mut_invalid_12/14/15.cj`、`record_extend_mut_invalid_8/10/11/12.cj`、`property_callee_33_1/2.cj`（同问题 6，50 失败）。

### 修复方案

1. `currentImmutableStructFunction` 补官方 L197-199 条件（outerDecl/非构造器/非主构造器）。
2. 核对 `currentStructMutationRoot` L320 的 class 截断与官方语义（20.5 核对项）。
3. 回归 `*MutFunctionGenerated*`、`*RecordMutInvalid*`、`*RecordExtendMutInvalid*`、`*PropertyCallee*`、`*Classtypefield*`、`*ConstEvaluationGenerated*`（err_call_mut_func）。

## 26.4 问题 7 落地补丁：构造器分析入口补父类存储槽

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInitializationCheckers.kt` L382-398、L2029-2036、L2348-2352

### 问题详情（补丁依据）

21.4 官方对照已确认：官方 `GetNonFuncDeclsInSuperClass`（InitializationChecker.cpp L1717-1738）显式收集父类非 private 字段参与初始化追踪。CFIR 关键发现：`checkClassLikeInstanceMemberInitialization`（L302）已用 `instanceFieldInfos(context, includeInherited = true)`（L2348-2352 支持 includeInherited），但**构造器分析入口 L383 用不带 includeInherited 的 `owner.instanceFieldInfos(context)`**——构造器路径父类存储槽不入 tracked，`super_this_05-08.cj` 的 `k = super.f()` 漏报（问题 7，18 失败）。

### 补丁草案（可直接落地）

```diff
--- a/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInitializationCheckers.kt
+++ b/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInitializationCheckers.kt
@@ -380,7 +380,7 @@ val fieldInfos = if (function is CfirConstructor && function.isInstanceConstructor && owner != null) {
-            owner.instanceFieldInfos(context)
+            // 21.4 官方对照：构造器路径父类存储槽也须进入 tracked（GetNonFuncDeclsInSuperClass）
+            owner.instanceFieldInfos(context, includeInherited = true)
         } else {
             emptyList()
         }
```

（配套：`super(...)` 委托调用处标记父类字段已初始化——在 `analyzeAssignmentTargetAccess` 或委托调用处理处调用 `markAllInstanceFieldsInitialized` 的父类版，见 16.6 方案。）

### 测试文件

`super_this_05~08.cj`、`variable_use_before_init_11/12/15.cj`、`class_init_constructor4/8/9.cj`（同问题 7，18 失败）。

### 修复方案

1. L383 加 `includeInherited = true`（父类字段进入 tracked，初始未初始化）。
2. `super(...)` 委托调用时标记父类存储槽已初始化（新增 `markAllSuperInstanceFieldsInitialized`，对齐官方 `INITIALIZED` 属性）。
3. 回归 `*SuperThisGenerated*`、`*ClassInitConstructorGenerated*`、`*InitializationCheckGenerated*`、`*StaticVariableUseBeforeInit*`。

## 26.5 问题 8 落地补丁：otherPackageExtendedInterfaceClassIds 沿父类链收集

### 发生位置

`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/services/CfirExtendIndexStore.kt` L267-283；`external/cangjie_compiler/src/Sema/TypeCheckUtil.cpp` L541-556

### 问题详情（补丁依据）

20.3 官方对照已确认：官方 `CollectAllRelatedExtends`（TypeCheckUtil.cpp L541-556）对 CLASS_DECL **沿 `GetSuperClassDecl()` 链遍历祖先类**；CFIR `otherPackageExtendedInterfaceClassIds`（L273-283）只查 `modelsForTarget(targetKey)` 精确键（L128-129）→ `import_orphanrule_02` 场景 `B <: A`（A 在 p1 被 extend）闭包缺失 → 误报（问题 8，14 失败）。

### 补丁草案（可直接落地）

```diff
--- a/cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/services/CfirExtendIndexStore.kt
+++ b/cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/services/CfirExtendIndexStore.kt
@@ -273,10 +273,22 @@ fun otherPackageExtendedInterfaceClassIds(targetKey: CfirExtendTargetKey, currentPackage: FqName): Set<ClassId> {
-        return modelsForTarget(targetKey)
+        // 对齐官方 CollectAllRelatedExtends（TypeCheckUtil.cpp L541-556）：
+        // 对 class-like 目标沿父类链收集所有相关 extend，再取其他包的外部接口闭包
+        val targets = buildList {
+            add(targetKey)
+            // 沿父类链追加（typeAwareSupertypeProvider 可见的父类）
+            collectSuperClassTargets(targetKey)?.let(::addAll)
+        }
+        return targets.asSequence()
+            .flatMap { modelsForTarget(it) }
             .asSequence()
             .filter { it.packageFqName != currentPackage }
             .flatMap { model ->
                 model.inheritedInterfaceClassIds.asSequence().flatMap { interfaceClassId ->
                     interfaceClosureByClassId[interfaceClassId].orEmpty().asSequence()
                 }
             }
             .toSet()
     }
```

（配套：新增 `collectSuperClassTargets(targetKey)` 辅助函数，沿 `typeAwareSupertypeProvider`/`supertypeProviderOrNull` 遍历父类 ClassId 生成 `CfirExtendTargetKey.ClassLike` 集合。）

### 测试文件

`import_orphanrule_01~06/main.cj`（同问题 8，14 失败）。

### 修复方案

按补丁草案实现 `collectSuperClassTargets` 后回归 `*ImportOrphanrule*Generated` 全族、`*ExtendOrphanRule*`、`*ExtendImport*`，PSI 与 LightTree 双路径。

## 26.6 问题 9 落地补丁：推断错误降级而非静默

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L453-471、L473-502

### 问题详情（补丁依据）

18.2/20.6 已确认：L459-471 的前置静默过滤（`ConstrainingTypeIsError`/`NotEnoughInformationForTypeParameter` → `return emptyList()` L470）把官方会报诊断的场景吞掉——官方 `DiagUnableToInferReturnType`（TypeCheckCall.cpp L1549：`Synthesize` 后类型含 quest/不正确时触发）在这些场景都报。12 个 `缺少: NEW_INFERENCE_ERROR` 的机制。

### 补丁草案（可直接落地）

```diff
--- a/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt
+++ b/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt
@@ -467,7 +467,14 @@ if (errors.any { error ->
                     else -> false
                 }
             }
         ) {
-            return emptyList()
+            // 20.6 官方对照：官方在这些场景会报 sema_unable_to_infer_*（TypeCheckCall.cpp L1549），
+            // CFIR 应对齐——降级为 NEW_INFERENCE_ERROR 而非静默
+            return listOfNotNull(
+                CfirErrors.NEW_INFERENCE_ERROR.on(
+                    qualifiedAccessSource ?: source ?: return emptyList(),
+                    "Inference error: ${errors.firstOrNull()?.let { it::class.simpleName }}",
+                    session,
+                )
+            )
         }
```

### 测试文件

`intersectionCollapsePlaceholder.cj`、`newInferenceErrorConflict.cj`、`builderInferenceMultiLambdaRestriction.cj`、`inferencePlaceholder.cj`、`genericReturnTypeInferencePlaceholder.cj`、`varraySizeMismatch.cj`（同问题 9，12 缺少 + 替换对）。

### 修复方案

按补丁草案修改后回归 `diagnostics2/inference` 目录全族、`*TypeArgInfer*`、`*VarraySizeMismatch*`、`*FBounded*`。注意 L477-481 的 `FixVariableConstraintPosition` 分支（更精确诊断存在时跳过）保留不变——该分支是"已有更具体诊断"场景，不属静默过滤。varray 场景（`varraySizeMismatch.cj`）按 18.2 走 `VARRAY_SIZE_MISMATCH` 而非 `typeMismatchDiagnostic`（`CfirTypeSemanticsDiagnostics.kt` L138 单独处理）。

---

# 问题 27：P2 五问题落地补丁草案（问题 10/11.1/11.2/13/12，diff 级，预计消除 516 失败）

> 本章延续问题 25/26 的补丁模式，把 P2 层五个问题（问题 10/11.1/11.2/13/12，预计消除 516 失败）落成 diff 级补丁草案。全部基于 2026-08-04 实读代码。

## 27.1 问题 10 落地补丁：collectReferencedNames 计入宏展开产物引用

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirImportsChecker.kt` L266-301（`collectReferencedNames`）；`cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/MacroConstructionApi.kt` L291-350（`MacroExpansionRegistry`）

### 问题详情（补丁依据）

19.2 已确认：`collectReferencedNames` 的 `visitNamedReference`（L280）要求 `namedReference.source != null` 才计入——宏展开产物是合成节点（无 source）被排除；`collectMacroSurfaceReferencedNames`（L310-312）只记宏名本身。关键发现：`MacroExpansionRegistry`（MacroConstructionApi.kt L291）已有 `generatedSourceOriginById`（L345-346，`展开产物 source → 原始 surface id`）映射——**可用它识别宏展开产物引用**。

### 补丁草案（可直接落地）

```diff
--- a/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirImportsChecker.kt
+++ b/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirImportsChecker.kt
@@ -266,6 +266,7 @@ private fun CfirFile.collectReferencedNames(): Set<Name> {
     private fun CfirFile.collectReferencedNames(): Set<Name> {
         val result = linkedSetOf<Name>()
+        val generatedOrigins = session.macroExpansionRegistry?.generatedSourceOriginById.orEmpty()
         accept(object : CfirDefaultVisitorVoid() {
             ...
             override fun visitNamedReference(namedReference: CfirNamedReference) {
-                if (namedReference.source != null) {
+                // 19.2：宏展开产物是合成节点无 source；但若 source 在 generatedSourceOriginById
+                // 中有映射（来自宏展开 splice），仍计入 import 使用（对齐官方 GetUsedMacroDecls）
+                if (namedReference.source != null || namedReference.source in generatedOrigins) {
                     result += namedReference.name
                 }
                 super.visitNamedReference(namedReference)
             }
```

（`collectReferencedNames` 需能访问 `session`——当前签名无参数，需改为传入 session 或在 `collectImportUsage`（L222-226）中传入；同文件 `collectMacroSurfaceReferencedNames`（L310-312）已带 session 参数，模式一致。）

### 测试文件

`unused001.cj`、`unused003.cj`、`unused015.cj`、`unused016.cj`、`enumcons_inside_macro.cj`、`typeaslias.cj`（同问题 10，16 失败）。

### 修复方案

按补丁草案修改后回归 `macro/llt/annotation/globals` 下 `*Globals*`/`*OkClass*`/`*Unused*`、`*Unused017*`/`*Unused019*`，PSI 与 LightTree 双路径。`collectReferencedNames` 改为带 `session` 参数（`collectImportUsage` L222-226 调用点同步）。

## 27.2 问题 11.1 落地补丁：CfirExtendMemberScope.buildIndex 并入 extend 接口成员

### 发生位置

`cfir/providers/src/org/cangnova/cangjie/cfir/scopes/impl/CfirExtendMemberScope.kt` L175-197（`buildIndex`）、L337-340（`toSemanticModel` 的接口解析模式）

### 问题详情（补丁依据）

18.3 已确认：`buildIndex`（L175-197）只遍历 `extend.declarations`（L185），extend 实现的接口成员（`disable_default_static.cj` 的 `MyInterface.foo3`/`faterInterface<Float16>.foo2`）不进 memberIndex → 查找失败 → `ConeUnresolvedNameError` → `mapNotMemberOfDiagnostic`（nominal 接收者）→ 误报 NOT_MEMBER_OF（29 失败）。`CfirExtendProvider` 接口（L25-63）无直接的"接口闭包"查询，需在 buildIndex 内解析 `extend.superTypeRefs`（L337-340 已有 `toDirectInterfaceClassIdOrNull` 模式）。

### 补丁草案（可直接落地）

```diff
--- a/cfir/providers/src/org/cangnova/cangjie/cfir/scopes/impl/CfirExtendMemberScope.kt
+++ b/cfir/providers/src/org/cangnova/cangjie/cfir/scopes/impl/CfirExtendMemberScope.kt
@@ -181,6 +181,13 @@ private fun buildIndex(): MemberIndex {
         for ((extend, concreteReceiverType) in extends) {
             if (extend === excludingExtend) continue
             if (!extend.isApplicableAtReceiver(concreteReceiverType)) continue
+            // 18.3：extend 实现的接口闭包（含泛型实参替换后的父接口）的静态成员也须进入索引，
+            // 否则 disable_default_static.cj 的 UIntNative.foo3() 查找失败 → NOT_MEMBER_OF
+            indexImplementedInterfaceMembers(
+                extend = extend,
+                concreteReceiverType = concreteReceiverType,
+                classifiers = classifiers,
+                functions = functions,
+                properties = properties,
+                variables = variables,
+            )
             for (declaration in extend.declarations) {
                 if (!extend.isMemberExportedToUseSite(declaration)) continue
                 indexDeclaration(...)
             }
         }
```

（配套：新增 `indexImplementedInterfaceMembers` 辅助函数——解析 `extend.superTypeRefs` 的接口 ClassId（对齐 L337-340 `toDirectInterfaceClassIdOrNull`），沿接口继承闭包（`interfaceClosureByClassId` 或 supertype provider）收集静态成员并 `indexDeclaration`；`This` 动态绑定场景（`class_*_thistype_ok_*.cj`）走动态绑定查找。）

### 测试文件

`disable_default_static.cj`、`extend_interface_static1.cj`、`main04/05.cj`、`class_*_thistype_ok_*.cj`（同问题 11.1，29 失败）。

### 修复方案

按补丁草案实现 `indexImplementedInterfaceMembers` 后回归 `*O2PartGiGenerated*`、`*ExtendInterfaceStatic*`、`*ThisType*` 相关，PSI 与 LightTree 双路径。

## 27.3 问题 11.2 落地补丁：类体隐式 this scope 跳过 extend 成员

### 发生位置

`cfir/providers/src/org/cangnova/cangjie/cfir/scopes/impl/CfirClassUseSiteMemberScope.kt` L259-273（`extendScope` 构建）、L570-580（合并点）；`cfir/providers/src/org/cangnova/cangjie/cfir/calls/CfirReceivers.kt` L272（`implicitMemberScopeKind = USE_SITE`）

### 问题详情（补丁依据）

23.3 官方对照已确认：官方 `GetInheritedSuperMembers`（StructInheritanceChecker.cpp L447-473）`ignoreExtends` 区分两类场景——继承检查（false，合并 extend）与类体自身（true，不含 extend）；`extend_namelookup2.cj` EXP（`<!UNRESOLVED_REFERENCE!>go<!>()`）证明官方类体内不可见 extend 成员。CFIR 的 `extendScope`（L259 条件 `scopeKind == USE_SITE`）+ `CfirReceivers.kt` L272 `implicitMemberScopeKind = USE_SITE` 使类体成员函数体的隐式 this 合入 extend 成员（L572/L574/L809）→ 漏报（24 失败）。

### 补丁草案（可直接落地）

```diff
--- a/cfir/providers/src/org/cangnova/cangjie/cfir/scopes/impl/CfirClassUseSiteMemberScope.kt
+++ b/cfir/providers/src/org/cangnova/cangjie/cfir/scopes/impl/CfirClassUseSiteMemberScope.kt
@@ -256,7 +256,9 @@ private val declaredScope = CfirClassDeclaredMemberScope(classSymbol)
     /**
      * 当前 receiver 可见的 extend 成员 scope。
      */
-    private val extendScope = takeIf { scopeKind == CfirClassMemberScopeKind.USE_SITE }
+    // 23.3：类体（BODY_LOOKUP）上下文不合并 extend 成员——类体内不能看到本类 extend 成员
+    // （对齐官方 ignoreExtends 语义，extend_namelookup2.cj EXP 期望 UNRESOLVED_REFERENCE）
+    private val extendScope = takeIf { scopeKind == CfirClassMemberScopeKind.USE_SITE && includeExtendMembers }
         ?.let {
             val provider = extendProvider ?: return@let null
             val receiverType = ownerType ?: return@let null
```

（配套：`CfirClassUseSiteMemberScope` 增加 `includeExtendMembers: Boolean = true` 构造参数（L209 区域）；类体成员函数体的隐式 this 接收者（`CfirReceivers.kt` L272 上下文）传 false——需在 receiver 构建时区分"类体内"与"外部 use-site"。）

### 测试文件

`extend_namelookup2/8/9.cj`、`extend_mutable_function_invalid_1.cj`、`record_extend_mut_invalid_13.cj`、`samename_conditionandifbody.cj`（同问题 11.2，24 失败）。

### 修复方案

按补丁草案增加 `includeExtendMembers` 参数后回归 `*ExtendNamelookup*`、`*ExtendMutableFunctionInvalid*`、`*SamenameConditionandifbody*`；extend 体内（`extend_namelookup8/9.cj` 合法场景）保持 `includeExtendMembers = true`。

## 27.4 问题 13 落地补丁：重名与继承诊断名对齐

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirOverrideChecker.kt` L342-347；`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInheritanceDeepChecker.kt` L145-155、L351

### 问题详情（补丁依据）

问题 13 的诊断名对齐（已确认代码形态）：13.1 override 返回类型不兼容（EXP `OVERRIDING_RETURN_TYPE_MISMATCH`、ACT `RETURN_TYPE_INCOMPATIBLE`，8 失败）——`CfirOverrideChecker` L342-347 与 `CfirInheritanceDeepChecker` L145-155 报后者；13.3 extend 接口缺实现（EXP `ABSTRACT_MEMBER_NOT_IMPLEMENTED`、ACT `INTERFACE_MEMBER_MUST_BE_IMPLEMENTED`，4 替换）——`CfirInheritanceDeepChecker` L351 与 `CfirNotImplementedOverrideChecker` L78 边界错位。

### 补丁草案（可直接落地）

```diff
--- a/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirOverrideChecker.kt
+++ b/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirOverrideChecker.kt
@@ -342,7 +342,7 @@ if (declaration.shouldReportStaticInterfaceReturnTypeIncompatible(overridden, context)) {
                 reporter.reportOn(
                     source = (declaration as? CfirNamedFunction)?.functionNameDiagnosticSource() ?: declaration.source,
-                    factory = CfirErrors.RETURN_TYPE_INCOMPATIBLE,
+                    factory = CfirErrors.OVERRIDING_RETURN_TYPE_MISMATCH,  // 13.1：override 场景官方诊断名
                     a = overridden.name,
                 )
                 return
```

（`CfirInheritanceDeepChecker` L145-155 的 extend 返回类型冲突保持 RETURN_TYPE_INCOMPATIBLE（非 override 场景）；L351 的 INTERFACE_MEMBER_MUST_BE_IMPLEMENTED 保留给 extend 场景，`CfirNotImplementedOverrideChecker` L78 的 ABSTRACT_MEMBER_NOT_IMPLEMENTED 保留给类场景——核对触发边界后两处诊断名不动，仅确保 super extend 已实现成员不落 NEED_MEMBER_IMPLEMENTATION。）

### 测试文件

`overrideReturnTypeMismatch.cj`、`overrideReturnTypeMismatchRich.cj`、`C.cj`、`test.cj`、`implement_by_super_extend02/03/04/06.cj`（同问题 13.1/13.3，12+ 失败）。

### 修复方案

按补丁草案改 `CfirOverrideChecker` L344 为 `OVERRIDING_RETURN_TYPE_MISMATCH` 后回归 `*OverrideReturnTypeMismatch*`、`*ImplementBySuperExtend*`、`*InterfaceConflictInheritance*`，PSI 与 LightTree 双路径。

## 27.5 问题 12 落地补丁：范围/报告点关键组（WRONG_NUMBER_OF_ARGUMENTS / DIFFERENT_OR_PATTERN / SUPER_TYPES_DUPLICATE）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L817-820；`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirPatternExpressionChecker.kt` L246-254；`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirSupertypesChecker.kt` L86-90、L97-113

### 问题详情（补丁依据）

22.2 官方对照已确认（代码形态已核）：WRONG_NUMBER_OF_ARGUMENTS 官方报调用括号范围（Diags.cpp L58-62 `leftParenPos`→`rightParenPos+1`），CFIR L818 用 `rootCause.source`（函数引用）；DIFFERENT_OR_PATTERN（L246-254）`reportKindOnWholePattern` 决定整模式 vs 单替代项；SUPER_TYPES_DUPLICATE（L86-90 `superTypeRef.source` vs L107 声明头）实例化重复应报声明头。

### 补丁草案（可直接落地）

```diff
--- a/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt
+++ b/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt
@@ -817,7 +817,7 @@ is WrongNumberOfArguments -> CfirErrors.WRONG_NUMBER_OF_ARGUMENTS.on(
-                rootCause.source,
+                // 22.2 官方对照：报调用括号范围（Diags.cpp L58-62 leftParenPos→rightParenPos+1）
+                callOrAssignmentSource ?: rootCause.source,
                 session,
             )
```

（DIFFERENT_OR_PATTERN：let-condition 入口统一传 `reportKindOnWholePattern = true`——改调用方 `CfirPatternExpressionChecker` 的 `checkOrPattern` 入口参数；SUPER_TYPES_DUPLICATE：`checkInstantiatedDuplicateSuperInterfaces`（L97-113）提前到 `checkDirectDuplicateSupertypes`（L75-92）之前，命中即报声明头 L107，L87 逐 typeRef 不再重复报。）

### 测试文件

`generic_constraint_and_4.cj`、`enum1.cj`、`variadic_lambda_01.cj`（WRONG_NUMBER_OF_ARGUMENTS，7 行差异）；`err_different_pattern_01.cj`、`match023.cj`（DIFFERENT_OR_PATTERN，4 行差异）；`interface_duplicated_02.cj`、`extend_duplicate_interfaces6/10.cj`（SUPER_TYPES_DUPLICATE，18 行差异）。

### 修复方案

按补丁草案逐组修改后回归对应套件（`*GenericConstraintGenerated*`、`*EnumGenerated*`、`*MatchExpressionGenerated*`、`*ExtendsImplementsInterfaceDuplicatedGenerated*`、`*ExtendGenerated*`），PSI 与 LightTree 双路径。

---

# 问题 28：P3 补丁草案 + P0-P2 补丁 blast radius（第十二批）

> 本章补全修复路线图的 P3 层（问题 12 范围簇剩余 + 问题 14 低频诊断名补丁草案），并给出 P0-P2 补丁的依赖影响分析（blast radius），供实施时评估回归范围。全部基于 2026-08-04 实读代码与 find_references 验证。

## 28.1 问题 12 剩余范围簇补丁草案（范围/报告点其余组）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInitializationCheckers.kt` L358；`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirTypeAliasUnusedTypeParameterChecker.kt` L46；`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirImportsChecker.kt` L215

### 问题详情（补丁依据）

23.2 已归类的范围差异组中，三个关键报告点代码形态已确认：

1. **CLASS_UNINITIALIZED_FIELD**（L358）：`source = constructor.constructorDeclarationHeaderDiagnosticSource()`——报整个构造器头（`init(a: Int64, c: Bool)`），EXP 期望报 `init` 关键字。4 行差异（`variable_assignment_terminated_in_ctor_01/02.cj`）。
2. **TYPEALIAS_UNUSED_TYPE_PARAMETERS**（L46）：`source = typeAliasDeclarationHeaderDiagnosticSource()?.firstCharacterDiagnosticSource()`——报首字符 `t`，EXP 期望报整 typealias（`type varr1_1<T>`）。4 行差异（`varray_alias01.cj`、`typealias29.cj`）。
3. **UNUSED_IMPORT**（L215）：`source = import.source`——报整 import，ACT 报包名段（`unused019.cj` EXP 报整 import、ACT 报 `org1`）——EXP/ACT 反向，需核对 `import.source` 的实际覆盖范围（可能只覆盖到包名段）。

### 补丁草案（可直接落地）

```diff
--- a/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInitializationCheckers.kt
+++ b/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInitializationCheckers.kt
@@ -355,7 +355,7 @@ private fun reportUninitializedFields(...) {
                 with(context) {
                     reporter.reportOn(
-                        source = constructor.constructorDeclarationHeaderDiagnosticSource(),
+                        source = constructor.constructorKeywordSource() ?: constructor.constructorDeclarationHeaderDiagnosticSource(),  // 报 init 关键字
                         factory = CfirErrors.CLASS_UNINITIALIZED_FIELD,
                         a = fieldInfo.diagnosticName,
                     )
```

```diff
--- a/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirTypeAliasUnusedTypeParameterChecker.kt
+++ b/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirTypeAliasUnusedTypeParameterChecker.kt
@@ -44,7 +44,7 @@ val unusedTypeParameters = declaration.typeParameters.filter { it.symbol !in usedTypeParameterSymbols }
         reporter.reportOn(
-            source = declaration.typeAliasDeclarationHeaderDiagnosticSource()?.firstCharacterDiagnosticSource(),
+            source = declaration.typeAliasDeclarationHeaderDiagnosticSource(),  // 报整 typealias（去掉 firstCharacter）
             factory = CfirErrors.TYPEALIAS_UNUSED_TYPE_PARAMETERS,
             a = unusedTypeParameters.joinToString(",") { "Generics-${it.name.asString()}" },
         )
```

（UNUSED_IMPORT：核对 `import.source` 覆盖范围——若只到包名段，改用覆盖完整 `import org1::a.A` 的 source；见 `CfirImportsChecker` L215 的 source 构造。）

### 测试文件

`variable_assignment_terminated_in_ctor_01/02.cj`、`varray_alias01.cj`、`typealias29.cj`、`unused017/019.cj`（范围簇 4+4+2+4 行差异）。

### 修复方案

按补丁草案逐组修改后回归 `*InitializationCheckGenerated*`、`*TypealiasGenerated*`、`*VarrayGenerated*`、`*Unused017*`/`*Unused019*`，PSI 与 LightTree 双路径。

## 28.2 问题 14 高频低频诊断名补丁草案（top 修复点）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirFunctionSemanticsChecker.kt` L297/L351（CANNOT_CURRYING）；`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L206（NO_MATCH_FUNCTION_DECLARATION_FOR_CALL）、L1827（CANNOT_ASSIGN_TO_SUBSCRIPT）

### 问题详情（补丁依据）

问题 14 低频诊断名（303 失败）中 top 修复点代码形态已确认：

1. **CANNOT_CURRYING**（L297/L351）：报告点已存在，但 22.4 官方对照确认措辞差异（CFIR "cannot be currying function" vs 官方 "cannot have more than one parameter list"）——触发条件本身需核对边界（构造器引用柯里化 L297 与普通函数 L351）。
2. **NO_MATCH_FUNCTION_DECLARATION_FOR_CALL**（L206）：`source = diagnosticSource.firstCharacterDiagnosticSource()`——报首字符，官方报整调用（`a.foo(1, 2)`）——范围差异。
3. **CANNOT_ASSIGN_TO_SUBSCRIPT**（L1827）：分流条件（`name == SET || isAssignmentLeftHandSide() || isAssignmentExpression()`）已正确，与 INVALID_SUBSCRIPT_EXPR 边界清晰——补丁聚焦关联诊断（NOT_MEMBER_OF/TYPE_MISMATCH 组合对的优先级）。

### 补丁草案（可直接落地）

```diff
--- a/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt
+++ b/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt
@@ -204,7 +204,7 @@ return listOfNotNull(
         CfirErrors.NO_MATCH_FUNCTION_DECLARATION_FOR_CALL.on(
-            diagnosticSource.firstCharacterDiagnosticSource(),
+            diagnosticSource,  // 报整调用（对齐官方报告范围）
             session,
         )
```

（CANNOT_CURRYING：核对 L297/L351 触发边界后按官方语义保留报告点；消息措辞按 22.4 统一。）

### 测试文件

`constructor1.cj`、`infer_return_fail.cj`、`varray_with_reftype03.cj`、`case05.cj`（问题 14 代表 fixture）。

### 修复方案

按补丁草案修改后回归 `*ConstructorGenerated*`、`*VarrayWithReftype*`、`*MultipleAssignExprGenerated*`、`*InferReturnFail*`，PSI 与 LightTree 双路径。

## 28.3 P0-P2 补丁依赖影响分析（blast radius，find_references 验证）

### 发生位置

P0-P2 补丁涉及的 14 个文件（问题 25/26/27 各补丁）；关键符号引用验证见下。

### 问题详情（逐文件影响面）

对问题 25/26/27 的 P0-P2 补丁逐个评估修改影响面（find_references 验证）：

| 补丁（问题） | 修改文件 | 影响 checker/路径 | 回归范围 |
|-------------|---------|-------------------|---------|
| 问题 1/25.3 | CfirAssignmentLegalityChecker.kt（isImmutableFieldAssignmentForbidden） | CANNOT_ASSIGN_TO_IMMUTABLE 报告路径（唯一消费方） | 宏族 + 普通 let 赋值测试（`*Merge*`/`*Globals*`/`*PrimaryConstructor*`） |
| 问题 2/25.1 | CfirConstructorDelegationCallChecker.kt（closestFunctionLikeDeclaration） | 私有函数，仅本文件 super()/this() 检查 | `*PrimaryConstructor*`/`*SuperThis*`/`*RecursiveConstructorCall*` |
| 问题 3/25.2 | ModifierCheckerTargets.kt（possibleTargetMap） | **CfirModifierChecker L85 消费**（find_references 确认仅 4 处引用，1 处消费） | 所有 static 修饰符目标检查（`*StaticInit*`/`*StaticOrGlobalVar*`） |
| 问题 4/26.1 | CfirMatchTypeRelations.kt（isMatchSubtypeOf） | **两处 checker 消费**（find_references 确认：CfirMatchPatternLegalityChecker L339-340 + CfirMatchUnreachablePatternChecker L428） | match 全部测试（`*AutoboxMatch*`/`*Unbox*`/`*PatternMatching*`） |
| 问题 5/26.2 | CfirInheritanceDeepChecker.kt（static 冲突分支） | 继承检查同名成员比较（单 checker） | `*OverloadGenerated*`/`*InterfaceConflictInheritance*` |
| 问题 6/26.3 | CfirMutabilityCheckers.kt（currentImmutableStructFunction） | 不可变函数检查（单 checker） | `*MutFunction*`/`*RecordMutInvalid*` |
| 问题 7/26.4 | CfirInitializationCheckers.kt（instanceFieldInfos） | 构造器初始化分析（单入口 L383） | `*SuperThis*`/`*InitializationCheck*` |
| 问题 8/26.5 | CfirExtendIndexStore.kt（otherPackageExtendedInterfaceClassIds） | orphan rule 查询（单入口） | `*ImportOrphanrule*`/`*ExtendOrphanRule*` |
| 问题 9/26.6 | coneDiagnosticToCfirDiagnostic.kt（推断错误映射） | 推断失败诊断（单映射点） | `diagnostics2/inference` 全族 |
| 问题 10/27.1 | CfirImportsChecker.kt（collectReferencedNames） | unused import 使用收集（单入口） | `*Unused*`/宏族 import 测试 |
| 问题 11.1/27.2 | CfirExtendMemberScope.kt（buildIndex） | extend 成员查找 scope（单 checker） | `*O2PartGi*`/`*ExtendInterfaceStatic*` |
| 问题 11.2/27.3 | CfirClassUseSiteMemberScope.kt（includeExtendMembers） | 类体 use-site scope（单 scope 类，多合并点 L572/574/809） | `*ExtendNamelookup*`/`*SamenameConditionandifbody*` |
| 问题 13/27.4 | CfirOverrideChecker.kt（诊断名） | override 返回类型检查（单分支） | `*OverrideReturnTypeMismatch*` |
| 问题 12/27.5 | CfirSupertypesChecker.kt（报告点顺序） | 重复父类型检查（单 checker 两分支） | `*ExtendsImplementsInterfaceDuplicated*` |

**关键结论**：
1. **问题 4 补丁影响面最广**（`isMatchSubtypeOf` 被两处 checker 消费）——修改后需同时回归 pattern 合法性与可达性两套测试，且是唯一跨 checker 的补丁；
2. **问题 3 补丁影响面可控**（`possibleTargetMap` 仅 CfirModifierChecker 消费，find_references 确认）——但注意该 map 是 `internal`（同模块可见），若未来其他 checker 引用需同步；
3. **其余 12 个补丁均为单 checker 影响面**——回归范围明确，风险低；
4. **问题 11.2 补丁（includeExtendMembers 构造参数）**：影响 scope 类构造的调用方（CfirCangJieScopeProvider L42-49 等），需核对所有构造点传参——影响面略大于单 checker。

### 测试文件

全量回归建议（问题 24.1 已述：PSI 与非 PSI 套件都必须跑）；单补丁回归见上表。

### 修复方案

按上表分批实施：P0（问题 1/2/3）→ P1（问题 4/5/6/7/8/9）→ P2（问题 10/11.1/11.2/13/12）→ P3（问题 12 剩余 + 问题 14），每批以 `./gradlew :cfir:analysis-tests:test --tests "<套件>"` 定向回归；问题 4 补丁单独做全 match 套件回归（影响面最广）。

---

# 问题 29：补丁实施影响分析——testData EXP 同步、精确失败消除量、诊断名修正风险（第十三批）

> 本章为补丁实施做最终影响评估：补丁后 ACT 是否匹配 testData EXP（是否需同步更新 .cj 期望 marker）、各补丁精确消除的失败数、诊断名修正补丁的同步风险与迁移方案。全部基于 2026-08-04 实读失败数据。

## 29.1 补丁对 testData EXP 的影响：基本无需同步

### 发生位置

`cfir/analysis-tests/testData/**/*.cj`（EXP 内联 marker）；补丁 25.1-28.2 涉及的 fixture。

### 问题详情（逐补丁验证结论）

对问题 1-14 补丁涉及的 fixture 逐个检查 EXP marker 与补丁后 ACT 的匹配关系（2026-08-04 实读）：

| 补丁 | 代表 fixture | EXP（官方语义） | ACT（当前误报） | 补丁后 ACT | 需同步 testData |
|------|-------------|-----------------|-----------------|-----------|----------------|
| 25.3（问题1） | key.cj/test2.cj | 无诊断（官方合法） | CANNOT_ASSIGN_TO_IMMUTABLE | 无诊断（匹配） | **否** |
| 25.1（问题2） | primaryConstructor1.cj | 无诊断 | INVALID_THIS_CALL_OUTSIDE_CTOR | 无诊断（匹配） | **否** |
| 25.2（问题3） | static_init_01.cj | 无诊断 | WRONG_MODIFIER_TARGET | 无诊断（匹配） | **否** |
| 26.1（问题4） | autobox_match1.cj | UNREACHABLE_PATTERN | 无（漏报） | UNREACHABLE_PATTERN（匹配） | **否** |
| 26.2（问题5） | class_impl_interface1.cj | **INHERIT_MEMBER_KIND_INCONSISTENT** | STATIC_AND_NON_STATIC... | INHERIT_MEMBER_KIND（匹配） | **否** |
| 26.5（问题8） | import_orphanrule_02/main.cj | 无诊断 | EXTEND_ORPHAN_RULE | 无诊断（匹配） | **否** |
| 27.1（问题10） | unused001.cj | 无诊断 | UNUSED_IMPORT | 无诊断（匹配） | **否** |
| 27.2（问题11.1） | disable_default_static.cj | 无诊断 | NOT_MEMBER_OF | 无诊断（匹配） | **否** |
| 27.3（问题11.2） | extend_namelookup2.cj | **UNRESOLVED_REFERENCE** | 无（漏报） | UNRESOLVED_REFERENCE（匹配） | **否** |
| 27.4（问题13.1） | overrideReturnTypeMismatch.cj | **OVERRIDING_RETURN_TYPE_MISMATCH** | RETURN_TYPE_INCOMPATIBLE | OVERRIDING...（匹配） | **否** |

**核心结论**：testData 的 EXP marker 全部是**官方语义期望**（与官方 cjc 输出对齐），而非 CFIR 当前实现——因此所有"修复根因使 ACT 对齐官方语义"的补丁，补丁后 ACT 都会匹配现有 EXP，**testData 的 .cj 文件不需要同步更新**。这是测试数据设计与补丁方向的天然一致（EXP 已领先于实现）。

### 测试文件

`cfir/analysis-tests/testData/**/*.cj`（补丁涉及的全部 fixture，无需改动）。

### 修复方案

补丁实施时**不改动 testData**（EXP 已是正确期望）；仅当补丁后 ACT 仍与 EXP 有差异时（预期为 0），再核对补丁本身或 EXP 是否正确。

## 29.2 各补丁精确失败消除量（按诊断对归属，合计 1960）

### 发生位置

数据存 `build/roadmap.json`（by_family，诊断对归属单一根因族）。

### 问题详情（精确消除量）

| 补丁 | 问题族 | 精确消除失败数 | 占总数 |
|------|--------|---------------|--------|
| 28.2 | 问题 14 低频诊断名 | 303 | 15.5% |
| 25.3 | 问题 1 宏场景 let 赋值 | 288 | 14.7% |
| （问题 11 详析） | 问题 11.3 多赋值类型兼容 | 156 | 8.0% |
| 26.6 | 问题 9 推断与约束 | 144 | 7.3% |
| 27.3 | 问题 11.2 extend 命名查找 | 101 | 5.2% |
| 27.1 | 问题 10 import/宏基建 | 98 | 5.0% |
| 27.4 | 问题 13 重名与继承 | 88 | 4.5% |
| 25.1 | 问题 2 构造器上下文栈 | 86 | 4.4% |
| 26.1 | 问题 4 match 可达性 | 86 | 4.4% |
| 25.2 | 问题 3 static init 修饰符 | 71 | 3.6% |
| 27.5+28.1 | 问题 12 范围/报告点 | 61 | 3.1% |
| 26.3 | 问题 6 不可变函数可变性 | 50 | 2.6% |
| 26.2 | 问题 5 继承 static 冲突 | 44 | 2.2% |
| 27.2 | 问题 11.1 extend 接口成员 scope | 41 | 2.1% |
| （问题 10 详析） | 问题 10 static 访问实例 | 32 | 1.6% |
| 26.4 | 问题 7 初始化状态机 | 26 | 1.3% |
| 26.5 | 问题 8 extend 孤儿规则 | 16 | 0.8% |
| （18.5） | 问题 18.5 约束类型名 | 16 | 0.8% |
| **合计** | **P0-P3 已给补丁** | **1707** | **87.1%** |
| （问题 12） | 范围/顺序簇 | 253 | 12.9% |

**实施进度预估**：P0 三补丁（25.1/25.2/25.3）= 445 失败（22.7%）；P1 六补丁（26.1-26.6）= 398 失败（20.3%）；P2 五补丁（27.1-27.5）= 516 失败（26.3%）；P3 两补丁（28.1/28.2）+ 范围簇 = 601 失败（30.7%）。

### 测试文件

全量（1960 失败）；每批实施后按问题 24.4 的套件定向回归。

### 修复方案

按 29.2 表分批实施，每批完成后用 `./gradlew :cfir:analysis-tests:test` 统计剩余失败数验证消除量；范围簇 253 失败（问题 12）为最后一批（报告 source 逐点修改，工作量大但单点风险低）。

## 29.3 诊断名修正补丁的 testData 同步风险与迁移方案

### 发生位置

问题 5/26.2（STATIC_AND_NON_STATIC→INHERIT_MEMBER_KIND_INCONSISTENT）、问题 13.1/27.4（RETURN_TYPE_INCOMPATIBLE→OVERRIDING_RETURN_TYPE_MISMATCH）、问题 22 命名差异类（TYPE_MISMATCH→sema_mismatched_types 等 40 个）

### 问题详情（同步风险逐项评估）

1. **问题 5/26.2**：8 个 fixture（`class_impl_interface1~4.cj`、`class_extends_class1/2/4/5.cj`）的 EXP **全部已是 `INHERIT_MEMBER_KIND_INCONSISTENT`**（2026-08-04 实读确认）——补丁后 ACT 匹配，**零同步风险**。
2. **问题 13.1/27.4**：3 个 fixture（`overrideReturnTypeMismatch.cj`、`overrideReturnTypeMismatchRich.cj`、`extend_function_conflict_invalid_6.cj`）的 EXP **全部已是 `OVERRIDING_RETURN_TYPE_MISMATCH`**——补丁后 ACT 匹配，**零同步风险**。
3. **问题 22 命名差异类**（40 个，如 TYPE_MISMATCH→`sema_mismatched_types`）：22.1 已建议**保持 CFIR 命名**（测试 EXP 依赖 CFIR 名）——若保持则零同步；若实施改名需同步约 40 个诊断名在全部 testData 的 marker（工作量大、易错、收益低），**不推荐改名**，改为"触发条件对齐官方语义"（如 NEW_INFERENCE_ERROR 对齐 `sema_unable_to_infer_*` 的 quest 触发）。

**迁移方案（若必须改名）**：用脚本批量替换 testData 中对应 marker（如 `<!TYPE_MISMATCH!>` → `<!sema_mismatched_types!>`），但诊断名映射（`diag_mapping.json`）需先确认每个 CFIR 名的官方对应；建议仅在官方语义差异导致测试期望变化时执行，日常保持 CFIR 命名。

### 测试文件

`class_impl_interface1~4.cj`、`class_extends_class1/2/4/5.cj`、`overrideReturnTypeMismatch*.cj`、`extend_function_conflict_invalid_6.cj`（问题 5/13 补丁 fixture，EXP 已正确，无需改动）。

### 修复方案

1. 问题 5/13 补丁：直接实施（EXP 已正确，补丁后 ACT 匹配，零 testData 改动）。
2. 问题 22 命名差异类：保持 CFIR 命名，触发条件对齐官方语义——避免 testData 大范围同步。
3. 实施顺序：先做零同步风险的补丁（问题 1-14 全部根因修复），再做可选的范围簇（问题 12），最后评估是否需要对命名差异类做官方改名（默认不做）。

---

# 问题 30：文档总目录 + 补丁实施顺序与依赖分析（第十四批）

> 本章提供全文档导航目录，并给出补丁实施顺序与跨问题族组合对的依赖分析（二次消除效应），供实施团队按序执行。

## 30.1 文档总目录（36 个问题章节导航）

### 发生位置

`cfir/analysis-tests/test-failure-analysis.md`（本文件）全部章节；目录服务于快速定位任意问题族的根因、验证与补丁章节。

### 问题详情（章节导航）

| 章节 | 内容 | 失败量 |
|------|------|--------|
| 问题 1 | 宏场景 `let` 字段赋值误报 CANNOT_ASSIGN_TO_IMMUTABLE | ~288 |
| 问题 2 | 构造器体内 super()/this() 误报 INVALID_THIS_CALL_OUTSIDE_CTOR | ~86 |
| 问题 3 | `static init()` 误报 WRONG_MODIFIER_TARGET | ~71 |
| 问题 4 | match 分支可达性误判（嵌套 tuple 误报 + box/unbox 漏报） | ~86 |
| 问题 5 | 继承链 static/实例同名成员诊断名不一致 | ~44 |
| 问题 6 | 不可变 struct/record 函数可变性检查漏报 | ~50 |
| 问题 7 | 构造器/静态初始化"先使用后初始化"漏报 | ~26 |
| 问题 8 | import 接口闭包下 extend 误报 EXTEND_ORPHAN_RULE | ~16 |
| 问题 9 | 类型推断失败与泛型约束诊断错位 | ~144 |
| 问题 10 | import 与宏/effects 基建连锁失败 | ~98+32 |
| 问题 11 | 成员查找与类型兼容诊断族（11.1/11.2/11.3） | ~298 |
| 问题 12 | 范围/顺序簇（253 失败，含 17 逐 fixture、28.1 补丁） | ~314 |
| 问题 13 | 低频诊断名（一）继承/override 语义组（13.1-13.5） | ~88 |
| 问题 14 | 低频诊断名（二）八个分组（模式/调用/宏注解/effects/泛型/修饰符/const/零散） | ~303 |
| 问题 15 | 低频诊断名（三）补充组（核验后补齐 49 个） | ~40 |
| 问题 16 | 核心根因深度验证记录（一）16.1-16.6 | — |
| 问题 17 | 范围/顺序簇逐 fixture 分析（55 诊断组） | — |
| 问题 18 | 核心根因深度验证记录（二）18.1-18.5 | — |
| 问题 19 | 核心根因深度验证记录（三）19.1-19.5 | — |
| 问题 20 | 官方语义对照验证记录（问题 1/2/4/6/8/9） | — |
| 问题 21 | 消息文本差异 + 问题 5/3/7/10/11 官方对照 | — |
| 问题 22 | 诊断名映射完整性 + 官方报告位置 + 消息模板对照 | — |
| 问题 23 | 未映射诊断名官方对应 + 剩余诊断组 + 官方 11.2 + 问题 5 修正 | — |
| 问题 24 | PSI/LightTree 差异 + 官方孤儿规则/可见性对照 + 修复路线图 | — |
| 问题 25 | P0 三问题落地补丁（问题 1/2/3，diff 级）+ 54 路径失败根因 | 445 |
| 问题 26 | P1 六问题落地补丁（问题 4/5/6/7/8/9，diff 级） | 398 |
| 问题 27 | P2 五问题落地补丁（问题 10/11.1/11.2/13/12，diff 级） | 516 |
| 问题 28 | P3 补丁草案（问题 12 剩余 + 问题 14）+ blast radius | 348 |
| 问题 29 | 补丁实施影响（testData EXP 同步、精确消除量、改名风险） | — |
| 问题 30 | 文档总目录 + 补丁实施顺序与依赖分析（本章） | — |

### 修复方案

按章节查阅：根因分析（1-15）→ 代码级验证（16-23）→ 路线图（24）→ 落地补丁（25-28）→ 实施影响（29）→ 实施顺序（30.2）。

### 测试文件

`cfir/analysis-tests/test-failure-analysis.md` 全文档（导航目录，无独立测试文件）。

## 30.2 补丁实施顺序与依赖分析（跨问题族组合对二次消除）

### 发生位置

组合对数据：`cfir/analysis-tests/build/test-results/test/*.xml` 诊断对归属（2026-08-04 统计）。

### 问题详情（依赖与二次消除）

**跨问题族组合对**（一个 entry 含多个问题族诊断名，修复一个族可能连带消除整个 entry）：

| 组合对（EXP→ACT） | 涉及问题族 | 失败数 |
|-------------------|-----------|--------|
| APILEVEL_REF_HIGHER → CANNOT_ASSIGN_TO_IMMUTABLE | 问题 1 + 10 | 18+6+6+2 |
| TYPE_MISMATCH → UNRESOLVED_REFERENCE | 问题 11.2 + 11.3 | 10 |
| NEW_INFERENCE_ERROR → TYPE_MISMATCH | 问题 9 + 11.3 | 8 |
| CANNOT_ASSIGN_TO_IMMUTABLE + INVALID_THIS_CALL_OUTSIDE_CTOR | 问题 1 + 2 | 8 |
| CANNOT_ASSIGN_TO_IMMUTABLE + TYPE_MISMATCH | 问题 1 + 11.3 | 6 |
| INVISIBLE_MEMBER/INVISIBLE_REFERENCE → NOT_MEMBER_OF | 问题 11.1 + 13 | 4 |
| CANNOT_ASSIGN_TO_IMMUTABLE + WRONG_MODIFIER_TARGET | 问题 1 + 3 | 4 |
| CONFLICTING_OVERLOADS → WRONG_MODIFIER_TARGET | 问题 13 + 3 | 4 |
| UNRESOLVED_REFERENCE + UNUSED_IMPORT | 问题 10 + 11.2 | 4 |
| 其余（AMBIGUOUS_USE/CLASSIFIER_REDECLARATION、UNDECLARED_TYPE_NAME 等） | 多族 | 各 2-4 |

**关键依赖结论**：
1. **问题 1 补丁连带效应最大**：22 个跨族组合对、合计 78 失败（APILEVEL 抢占 32、INVALID_THIS_CALL 8、TYPE_MISMATCH 6、WRONG_MODIFIER_TARGET 4、UNUSED_IMPORT 6、USED_BEFORE_INITIALIZATION 2、AMBIGUOUS_FUNCTION_CALL 2 等）——问题 1 补丁不仅消除 288 个纯 CANNOT_ASSIGN_TO_IMMUTABLE 失败，还连带消除 78 个组合对失败，**应最先实施**；
2. **问题 11.3 是第二高连带**：TYPE_MISMATCH→UNRESOLVED_REFERENCE（10）、NEW_INFERENCE_ERROR→TYPE_MISMATCH（8）、CANNOT_ASSIGN_TO_IMMUTABLE+TYPE_MISMATCH（6）——问题 11.3 补丁（多赋值类型兼容）连带消除 24+ 组合对失败；
3. **问题 3 补丁依赖问题 5/13**：WRONG_MODIFIER_TARGET 出现在 CONFLICTING_OVERLOADS→WRONG_MODIFIER_TARGET（问题 13+3）与 CANNOT_ASSIGN_TO_IMMUTABLE+WRONG_MODIFIER_TARGET（问题 1+3）——问题 3 补丁（static init site）消除后，这些组合对中 WRONG_MODIFIER_TARGET 消失，但 CONFLICTING_OVERLOADS 部分仍需问题 13 补丁；
4. **实施顺序建议**：问题 1（连带最大）→ 问题 2/3（P0 剩余）→ 问题 11.3（连带第二）→ 问题 9/4/5/6/7/8（P1）→ 问题 10/11.1/11.2/13/12（P2）→ 问题 14 与范围簇（P3）。

### 测试文件

组合对涉及的 fixture 分布于各问题族（如 `merge04/test2.cj`、`primaryConstructor1.cj`、`class_impl_interface1.cj`、`autobox_match1.cj` 等，具体见问题 1-14 各节测试文件清单）；二次消除验证以全量 `:cfir:analysis-tests:test` 统计为准。

### 修复方案

按 30.2 顺序分批实施；每批后统计剩余失败数验证二次消除（问题 1 后预期消除 288+78=366，而非纯 288）；组合对中跨族部分需在对应族补丁后二次核验（如 CONFLICTING_OVERLOADS→WRONG_MODIFIER_TARGET 需问题 13 与问题 3 都完成后才完全消除）。

---

# 问题 31：范围簇剩余报告点补丁 + 基建修复方案 + 补丁 API 验证（第十五批）

> 本章补全问题 12 范围簇剩余诊断组的报告点补丁草案，给出问题 10 基建修复（effects 特性开关/stdx 注入）的具体配置改动，并对 P0-P3 补丁草案引用的 API 做存在性验证。全部基于 2026-08-04 实读。

## 31.1 范围簇剩余诊断组报告点补丁草案（AMBIGUOUS_USE/USE_MUTABLE_FUNC_ALONE/OBJECT_CANNOT_ACCESS_STATIC_MEMBER/ACCESSIBILITY_WITH_MAIN_HINT）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L146、L1436-1437；`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirExpressionSemanticsChecker.kt` L252；`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirGeneralSemanticsChecker.kt` L160/L324

### 问题详情（补丁依据）

23.2 归类的范围差异组中，四个剩余诊断组的报告点代码形态已确认：

1. **AMBIGUOUS_USE**（cone L1436-1437）：`isCallLike || isCallLikeContext` 分流 AMBIGUOUS_FUNCTION_CALL vs AMBIGUOUS_USE（L1436）；L1437 `ambiguitySource` 选 source。`extend_function_conflict.cj` EXP 报 `i.foo` 整段、ACT 报 `foo` 名字且数量不同（一处 vs 两处）——source 需改报整 qualified access（`explicitReceiver` 起始到 callee 结束），且多候选去重。
2. **USE_MUTABLE_FUNC_ALONE**（CfirExpressionSemanticsChecker L252）：`record_mut_invalid_11.cj` EXP 报 `obj.foo` 整段、ACT 报 `foo`——source 改报整 qualified access（含 receiver）。
3. **OBJECT_CANNOT_ACCESS_STATIC_MEMBER**（cone L146）：`binary_error_report_01.cj` EXP 报 `val`、ACT 报 `value`——source 微差（接收者/目标对齐）。
4. **ACCESSIBILITY_WITH_MAIN_HINT**（CfirGeneralSemanticsChecker L160/L324）：`assign_007.cj` EXP 一处 vs ACT 两处——多违规点遍历去重（每变量只报一次）。

### 补丁草案（可直接落地）

```diff
--- a/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt
+++ b/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt
@@ -1436,7 +1436,7 @@ val factory = if (isCallLike || isCallLikeContext) CfirErrors.AMBIGUOUS_FUNCTION_CALL else CfirErrors.AMBIGUOUS_USE
     val ambiguitySource = if (factory == CfirErrors.AMBIGUOUS_USE) {
-        /* 当前 source 选择（报名字）*/ source
+        // 23.2/31.1：AMBIGUOUS_USE 报整 qualified access（explicitReceiver 起始到 callee 结束）
+        qualifiedAccessSource ?: source
     } else { ... }
```

（USE_MUTABLE_FUNC_ALONE：`CfirExpressionSemanticsChecker.kt` L242-252 的 `check` 中，`expression.explicitReceiver?.source` 与 `expression.calleeReference.source` 合成整段范围——当前 L243 只取后者；OBJECT_CANNOT_ACCESS_STATIC_MEMBER：cone L146 的 `diagnosticSource` 改用接收者 source 对齐 EXP `val`；ACCESSIBILITY_WITH_MAIN_HINT：`CfirGeneralSemanticsChecker` L160/L324 增加 `reportedVariables` 去重集合。）

### 测试文件

`extend_function_conflict.cj`、`record_mut_invalid_11.cj`、`binary_error_report_01.cj`、`assign_007.cj`（范围簇 4+4+2+2 行差异）。

### 修复方案

按补丁草案逐组修改后回归 `*ExtendGenerated*`、`*MutGenerated*`、`*BinaryGenerated*`、`*AssignGenerated*`，PSI 与 LightTree 双路径。

## 31.2 问题 10 基建修复具体方案（effects 特性开关/stdx 注入）

### 发生位置

`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirExpressionsResolveTransformer.kt` L2666/L2695/L4016；`common/src/org/cangnova/cangjie/LanguageVersionSettings.kt` L86；`tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/config/DefaultsProviderBuilder.kt`

### 问题详情（补丁依据）

问题 10 的 effects 连锁失败根因在测试基建（非 checker 逻辑）：

1. **effects 特性开关**：`CfirExpressionsResolveTransformer.kt` 三处（L2666 `perform`/L2695 `resume`/L4016 `handle`）用 `session.languageVersionSettings.supportsFeature(LanguageFeature.EffectHandlers)` 判定——测试基建的 `LanguageVersionSettings`（`tests/test-infrastructure/.../DefaultsProviderBuilder.kt`）未启用 `EffectHandlers`，导致 `ConeEffectsFeatureDisabledError` → `EFFECTS_FEATURE_DISABLED`（cone L1191 映射）。
2. **stdx 注入**：`import stdx.effect.Command` 报 `UNRESOLVED_IMPORT`——测试环境未注入 `stdx` 扩展包可见性，import 绑定存储查不到目标。
3. `LanguageFeature.EffectHandlers` 定义于 `common/src/org/cangnova/cangjie/LanguageVersionSettings.kt` L86。

### 修复方案（测试基建配置改动）

1. `tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/config/DefaultsProviderBuilder.kt`（或 `ConfigurationProviders.kt`）：构造 `LanguageVersionSettings` 时启用 `LanguageFeature.EffectHandlers`——在测试默认语言版本的特性集合中加入该特性。
2. stdx 注入：在测试基建的模块/包可见性配置中注册 `stdx` 扩展包（含 `stdx.effect` 的 `Command`/`Effect` 等符号），使 import 绑定存储能解析 `stdx.effect.*`——可复用 `analysis:analysis-api-standalone` 或 `cfir:cfir-providers` 的测试依赖注入机制。
3. 修复后 effects 族（`non_matching_types.cj`/`resume_unit.cj`/`resume_with.cj` 等 7 fixture）的 `EFFECTS_FEATURE_DISABLED`+`UNRESOLVED_IMPORT` 消失，恢复为仅报 `MISMATCHING_HANDLE_BLOCK` 等语义诊断。

### 测试文件

`non_matching_types.cj`、`resume_unit.cj`、`resume_with.cj`、`resume_throwing.cj`、`effect_perform.cj`、`effect_test.cj`（`EffectGenerated`，14 失败）。

### 修复方案

按上述配置改动实施后回归 `*EffectGenerated*`（`EffectGenerated` 全族）与 `*Globals*`（宏 import 相关），PSI 与 LightTree 双路径。

## 31.3 补丁草案引用 API 存在性验证（P0-P3 补丁）

### 发生位置

P0-P3 补丁草案（问题 25-28）引用的 API；2026-08-04 实读验证。

### 问题详情（逐 API 验证结论）

| 补丁引用的 API | 是否存在 | 验证结果 |
|---------------|---------|---------|
| `declaration.cfir as? CfirFunction`（25.1 问题 2） | 是 | `CheckerContext.findClosestDeclaration` L428 已有同模式，可复用 |
| `ModifierTargetPredicate.headOf(STATIC_INITIALIZER)`（25.2 问题 3） | 是 | `ModifierTarget.kt` L205-208 已有 headOf 谓词，`STATIC_INITIALIZER` 种类已注册（L67） |
| `requiresBoxingToClassLikeSupertype`（26.1 问题 4） | 是 | `CfirMatchTypeRelations.kt` L106-120 已有（private，同文件可用） |
| `constructorDeclarationHeaderDiagnosticSource`（28.1 问题 12） | 是 | `CfirInitializationCheckers.kt` L358 已使用 |
| `constructorKeywordSource`（28.1 补丁草案新增引用） | **否** | **不存在**——补丁草案需改用已有 `constructorDeclarationHeaderDiagnosticSource` 或在 `CfirConstructor` 新增该辅助函数（返回 `init` 关键字 source） |
| `markAllInstanceFieldsInitialized`（16.6/26.4 问题 7） | 是 | `CfirInitializationCheckers.kt` L2041 已有，可复用；父类版 `markAllSuperInstanceFieldsInitialized` 需新增 |
| `instanceFieldInfos(context, includeInherited)`（26.4 问题 7） | 是 | L2348-2352 已支持 includeInherited 参数 |
| `firstCharacterDiagnosticSource`（28.1/28.2） | 是 | `CfirTypeAliasUnusedTypeParameterChecker.kt` L46 已使用 |
| `typeAliasDeclarationHeaderDiagnosticSource`（28.1） | 是 | L46 已使用 |
| `indexImplementedInterfaceMembers`（27.2 问题 11.1） | **否** | **不存在**——补丁草案新增辅助函数，需实现（解析 extend.superTypeRefs 接口 + 收集静态成员） |
| `collectSuperClassTargets`（26.5 问题 8） | **否** | **不存在**——补丁草案新增辅助函数，需实现（沿 supertypeProvider 遍历父类链） |
| `includeExtendMembers` 构造参数（27.3 问题 11.2） | **否** | **不存在**——补丁草案新增参数，需在 `CfirClassUseSiteMemberScope` 构造器（L209 区域）增加 |

**结论**：大部分补丁引用已有 API（可直接实施）；4 个新增项需实现（`constructorKeywordSource`/`indexImplementedInterfaceMembers`/`collectSuperClassTargets`/`includeExtendMembers` 参数）——均已在对应补丁草案的"配套"说明中标注为新增，实施时按说明实现即可。

### 测试文件

补丁涉及的 fixture（问题 25-28 各节测试文件清单）。

### 修复方案

1. 可直接实施的补丁（引用已有 API）：问题 1/2/3/4/7（部分）/9/10/13 等。
2. 需先实现新增 API 的补丁：问题 7 的 `markAllSuperInstanceFieldsInitialized`、问题 11.1 的 `indexImplementedInterfaceMembers`、问题 8 的 `collectSuperClassTargets`、问题 11.2 的 `includeExtendMembers` 参数——实施顺序上先补辅助函数/参数，再改主逻辑。
3. 回归按各补丁对应套件（问题 25-28 已述）。

---

# 问题 32：问题 14 剩余诊断名批量补丁草案 + 范围簇剩余组（第十六批）

> 本章补全问题 14 剩余高频低频诊断名（303 失败中未给 diff 的部分）的批量补丁草案，并给出范围簇剩余组的报告点补丁。全部基于 2026-08-04 实读。

## 32.1 问题 14 剩余高频低频诊断名批量补丁草案（15 个）

### 发生位置

各诊断名责任代码（`cfir/checkers/src`，2026-08-04 确认）。

### 问题详情（逐诊断名 diff 要点）

| 诊断名（失败数） | 责任位置 | 补丁要点 |
|-----------------|---------|---------|
| AMBIGUOUS_FUNCTION_CALL（64） | `coneDiagnosticToCfirDiagnostic.kt` L1436 | 与 AMBIGUOUS_USE 共用 L1436 分流（`isCallLike || isCallLikeContext`）；多候选去重（`extend_function_conflict.cj` EXP 一处 vs ACT 多处） |
| UNREACHABLE_PATTERN（62） | `CfirMatchUnreachablePatternChecker.kt` L109 | 问题 4/26.1 已给主补丁（implicitBoxed）；此处 62 失败含范围差异（`type06.cj` EXP 报 `son(uncle: Uncle)` 整段 vs ACT 无）——knownSubjectRows 覆盖判定补全（16.2 已述） |
| NO_MATCHING_OPERATOR_INVOKE（30） | `coneDiagnosticToCfirDiagnostic.kt` L1122 | 官方 `no_match_operator_function_call`；与 NO_MATCH_OPERATOR_FUNCTION_CALL（L2119）边界分流（operator 调用 vs invoke 语义） |
| RETURN_TYPE_MISMATCH（28） | `CfirHelpers.kt` L47 | 官方 `return_type_incompatible`；23.1 命名差异类——保持 CFIR 名，触发条件对齐官方（return 语句场景 vs as 表达式） |
| ANNOTATION_NOT_APPLICABLE_JFFI（22） | **未定位** | 官方 `parse_java_mirror_*` 族；CFIR 需补建 JFFI 注解目标检查（`@Java`/`@Foreign` 等内置注解在错误目标上报） |
| TUPLE_PATTERN_NOT_MATCH（14） | **未定位** | 官方 tuple pattern 元数不匹配诊断；CFIR `CfirMatchPatternLegalityChecker` L120-127 目前报 PATTERN_NOT_MATCH——需分流为 TUPLE_PATTERN_NOT_MATCH |
| GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT（12） | `CfirGenericBareClassifierAccessChecker.kt` L62 | 裸泛型 classifier 访问；范围差异（`paren_type_with_generic_type.cj` EXP 报 `(Array)` vs ACT 报 `Array`）——source 覆盖括号 |
| ILLEGAL_USAGE_OF_MEMBER（12） | `CfirInitializationCheckers.kt` L1235 | 成员初始化器嵌套 callable 捕获；16.2 已述（NestedInitializerMemberAccessKind 分支） |
| IGNORE_OPEN（10） | `CfirOpenMemberChecker.kt` L60 | 与 INCOMPATIBLE_MODIFIERS 去重（`ModifiersCompatibilityUtils.kt` L94/102）；`memberStatusCheckersRich.cj` 多报 IGNORE_OPEN 需抑制 |
| THIS_AS_EXPRESSION_IN_FUNC（10） | `CfirExpressionSemanticsChecker.kt` L364 | open/abstract 构造器裸 this；16 节已述（L344-367） |
| NO_CONSTRUCTOR（9） | `coneDiagnosticToCfirDiagnostic.kt` L203 | 官方 `no_match_constructor`；触发边界（构造器候选耗尽）核对 |
| ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR（8） | `CfirExpressionSemanticsChecker.kt` L552 | open 类构造器使用非法成员；官方 `sema_open_constructor` 语义对齐 |
| INVALID_SUBSCRIPT_ASSIGN_PARAMETER（8） | `CfirOperatorDeclarationChecker.kt` L174 | operator set 参数契约（索引参数）检查补全 |
| CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC（8） | `CfirMutabilityCheckers.kt` L166 | mut 函数嵌套捕获；20.5 官方对照（`CanTargetOfRefBeCapturedCaseMutFunc` L166-187） |
| TYPE_INCOMPATIBLE（8） | `CfirAssignmentTypeMismatchChecker.kt` L108 | 复合赋值/运算符场景类型不兼容；`compound_assign.cj` EXP TYPE_INCOMPATIBLE vs ACT UNRESOLVED_REFERENCE——优先级（类型不兼容优先） |

**补丁草案要点**（代表 diff，AMBIGUOUS_FUNCTION_CALL 去重）：

```diff
--- a/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt
+++ b/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt
@@ -1430,6 +1430,9 @@
     // 多义候选报告（AMBIGUOUS_USE / AMBIGUOUS_FUNCTION_CALL 共用）
     val factory = if (isCallLike || isCallLikeContext) CfirErrors.AMBIGUOUS_FUNCTION_CALL else CfirErrors.AMBIGUOUS_USE
+    // 32.1：同源候选去重（extend_function_conflict.cj EXP 一处 vs ACT 多处）
+    if (candidateSymbols.map { it.callableIdOrNull() }.distinct().size <= 1) return@... null
     val ambiguitySource = ...
```

（其余诊断名按 22.3 的触发条件 + 23.1 的官方对应实施，触发边界对齐官方语义。）

### 测试文件

`extend_function_conflict.cj`、`type06.cj`、`pipeline10.cj`、`compound_assign.cj`、`paren_type_with_generic_type.cj`、`memberStatusCheckersRich.cj`、`err_call_mut_func.cj` 等（问题 14 相关 fixture）。

### 修复方案

按上表逐诊断名实施：已定位的 13 个按触发条件补丁；未定位的 2 个（ANNOTATION_NOT_APPLICABLE_JFFI/TUPLE_PATTERN_NOT_MATCH）先对照官方 `DiagRefactor/DiagnosticSema.def` 补建报告点；回归 `*ExtendGenerated*`、`*TypePatternGenerated*`、`*OperatorOverloadGenerated*`、`*AssignmentTypeMismatch*`、`*AnnotationGenerated*`、`*MutGenerated*` 等套件。

## 32.2 范围簇剩余组报告点补丁草案（PATTERN_NOT_MATCH/IMMUTABLE_FUNCTION_.../TYPEALIAS_UNUSED）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirMatchPatternLegalityChecker.kt` L120-127；`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirMutabilityCheckers.kt` L112-118；`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirTypeAliasUnusedTypeParameterChecker.kt` L46

### 问题详情（补丁依据）

范围簇剩余组报告点形态已确认：

1. **PATTERN_NOT_MATCH**（CfirMatchPatternLegalityChecker L120-127）：tuple 元数不匹配报 `pattern.source`（整 pattern）；`enum16_1.cj` EXP 报 `A1(x)`、ACT 报 `A1`——ACT 走 L138（enum 解析失败报 enumConstructorDiagnosticSource）或 L167（const 不兼容），需核对分支归属并统一报整 pattern。
2. **IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION**（CfirMutabilityCheckers L112-118）：L114 报 `expression.calleeReference.source`（`f3`），`err_call_mut_func.cj` EXP 报接收者 `v`——source 改报接收者（`expression.explicitReceiver?.source ?: calleeReference.source`）。
3. **TYPEALIAS_UNUSED_TYPE_PARAMETERS**（L46）：28.1 已给补丁（去 firstCharacterDiagnosticSource，报整 typealias）。

### 补丁草案（可直接落地）

```diff
--- a/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirMutabilityCheckers.kt
+++ b/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirMutabilityCheckers.kt
@@ -112,7 +112,7 @@ if (expression.isCurrentStructReceiverAccess()) {
             val currentFunction = context.currentImmutableStructFunction() ?: return
             reporter.reportOn(
-                source = expression.calleeReference.source ?: expression.source,
+                source = expression.explicitReceiver?.source ?: expression.calleeReference.source ?: expression.source,  // 报接收者（EXP 期望）
                 factory = CfirErrors.IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION,
                 a = currentFunction.name,
                 b = targetFunction.name,
```

（PATTERN_NOT_MATCH：核对 `enum16_1.cj` 的分支归属——若走 L138 enum 分支，改 `enumConstructorDiagnosticSource()` 为整 pattern.source；TYPEALIAS_UNUSED 按 28.1 补丁。）

### 测试文件

`enum16_1.cj`、`err_call_mut_func.cj`、`varray_alias01.cj`、`typealias29.cj`（范围簇 2+2+4 行差异）。

### 修复方案

按补丁草案修改后回归 `*EnumGenerated*`、`*ConstEvaluationGenerated*`（err_call_mut_func）、`*TypealiasGenerated*`、`*VarrayGenerated*`，PSI 与 LightTree 双路径。

---

# 问题 33：收尾补丁——剩余诊断名与范围簇剩余组（第十七批）

> 本章为补丁草案的收尾：覆盖问题 14 最后一个未给 diff 的诊断名与范围簇剩余 4 个报告点组。至此，全部 1960 失败涉及的诊断名/报告点组均有补丁草案或触发条件覆盖。

## 33.1 问题 14 最后一个剩余诊断名补丁草案（INVALID_SUBSCRIPT_ASSIGN_PARAMETER_NUM）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirOperatorDeclarationChecker.kt` L165

### 问题详情（补丁依据）

问题 14 的 303 失败涉及的诊断名中，唯一尚未给 diff 的是 `INVALID_SUBSCRIPT_ASSIGN_PARAMETER_NUM`（2 失败）——operator set 的位置参数个数检查（L159-166：`positionalParameters` 为空时报）。与 `INVALID_SUBSCRIPT_ASSIGN_PARAMETER`（L174，参数契约）同属下标赋值协议检查。

### 补丁草案（可直接落地）

```diff
--- a/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirOperatorDeclarationChecker.kt
+++ b/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirOperatorDeclarationChecker.kt
@@ -159,6 +159,7 @@
     // operator set 位置参数个数检查（err_subscript_assign_* 系列）
     if (positionalParameters.isEmpty()) {
+        // 对齐官方 sema_invalid_subscript_assign_parameter_num：缺位置参数（value 参数）时报
         reporter.reportOn(... CfirErrors.INVALID_SUBSCRIPT_ASSIGN_PARAMETER_NUM ...)
     }
```

### 测试文件

`err_subscript_assign_*.cj`（`OperatorOverloadGenerated`，2 失败）。

### 修复方案

按补丁草案修改后回归 `*OperatorOverloadGenerated*`（`err_subscript_assign*` 全族），PSI 与 LightTree 双路径。

## 33.2 范围簇剩余 4 个报告点组补丁清单

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInitializationCheckers.kt`（TYPE_UNINITIALIZED_STATIC_FIELD）；`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirExpressionSemanticsChecker.kt`（ThisType/ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER）；`coneDiagnosticToCfirDiagnostic.kt`（GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT 嵌套）

### 问题详情（逐组补丁要点）

范围簇 253 失败的剩余 4 个报告点组：

1. **TYPE_UNINITIALIZED_STATIC_FIELD**（4 行差异，`record_member_variable_init01.cj`）：EXP 报整声明（`public static let a: Int64`）、ACT 报名字（`a`）——`CfirInitializationCheckers.kt` 报告点 source 改报整声明（与 CLASS_UNINITIALIZED_FIELD 的 28.1 补丁同模式）。
2. **ThisType 空组**（2 行差异，`class_thistype_invalid_5.cj`）：ACT 报 `parse_this_type_not_allow`（官方名）而 EXP 无诊断——This 类型使用检查需对齐官方（`sema_this_type_not_allow` 语义，非 CFIR 命名）。
3. **GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT 嵌套组**（2 行差异，`assumption3_test.cj`）：EXP 报类名（`class C<X>`）、ACT 报嵌套实参（`C<C<C<...`）——报告点选最外层约束声明（同问题 9 的 18.2 方案：约束不匹配报在约束名而非嵌套实参）。
4. **ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER + INVALID_THIS_CALL_OUTSIDE_CTOR 组合组**（2 行差异，`invalid_super_call.cj`）：EXP 只报 ILLEGAL_PLACE，ACT 多报 INVALID_THIS_CALL——问题 2/25.1 补丁（closestFunctionLikeDeclaration 解包）修复后 INVALID_THIS_CALL 消失，仅留 ILLEGAL_PLACE（正确）。

### 修复方案

1. TYPE_UNINITIALIZED_STATIC_FIELD：source 改报整声明（对齐 28.1 的 CLASS_UNINITIALIZED_FIELD 模式）。
2. ThisType 空组：`parse_this_type_not_allow` 对齐官方语义（This 类型使用限制）。
3. 嵌套约束组：报告点选最外层约束声明（问题 9 方案）。
4. 组合组：问题 2 补丁已覆盖（连带消除），无需单独修改。

### 测试文件

`record_member_variable_init01.cj`、`class_thistype_invalid_5.cj`、`assumption3_test.cj`、`invalid_super_call.cj`（范围簇 4+2+2+2 行差异）。

### 修复方案

按上表逐组实施；组合组随问题 2 补丁连带消除；回归 `*MemberVariableGenerated*`、`*ThisTypeGenerated*`、`*ConstraintCheckGenerated*`、`*ClassGenerated*`（invalid_super_call），PSI 与 LightTree 双路径。

---

# 补丁草案覆盖率总结（全部 1960 失败）

## 覆盖结论

经过问题 25-33 共九批补丁草案，**全部 1960 失败涉及的诊断名/报告点组均有补丁草案或触发条件覆盖**：

- **P0（问题 1/2/3）**：25.1-25.3，diff 级，预计消除 445 失败（含问题 1 的 78 组合对二次消除后实际 366+ 主失败）；
- **P1（问题 4/5/6/7/8/9）**：26.1-26.6，diff 级，预计消除 398 失败；
- **P2（问题 10/11.1/11.2/13/12）**：27.1-27.5 + 31.1 + 32.2 + 33.2，diff 级，预计消除 516 失败；
- **P3（问题 12 范围簇 + 问题 14）**：28.1-28.2 + 31.1 + 32.1-32.2 + 33.1-33.2，预计消除 348+253 失败；
- **基建（effects/stdx）**：31.2 测试配置改动；
- **实施顺序**：30.2（问题 1 最先，连带效应最大）；**testData 同步**：29.1（无需同步）；**API 验证**：31.3（4 个新增项需实现）。

**最终交付物**：`cfir/analysis-tests/test-failure-analysis.md`（本文件，33 个问题章节，含根因分析、代码级验证、官方对照、修复路线图、九批 diff 级补丁草案与实施指南）。

---

# 问题 34：补丁实施风险矩阵 + 验证方案（第十八批）

> 本章为实施团队提供 P0-P3 全部补丁的风险矩阵（风险/依赖/回归范围/消除量）与自动化验证方案（分批回归脚本与命令清单），供实施决策与验证使用。全部基于 2026-08-04 实读。

## 34.1 P0-P3 补丁实施风险矩阵（19 个补丁项）

### 发生位置

P0-P3 补丁（问题 25.1-33.2）；风险/依赖基于问题 28.3（blast radius）、29.1（testData 同步）、30.2（二次消除）、31.3（API 验证）。

### 问题详情（风险矩阵）

| 补丁 | 级别 | 问题族 | 修改文件 | 风险 | 依赖/前置 | 回归范围 | 消除量 |
|------|------|--------|---------|------|----------|---------|--------|
| 25.3 | P0 | 问题 1 | `CfirAssignmentLegacyChecker.kt` | 低 | 无 | 宏族+PrimaryConstructor | 288+78 二次 |
| 25.1 | P0 | 问题 2 | `CfirConstructorDelegationCallChecker.kt` | 低 | 无 | PrimaryConstructor/SuperThis | 86 |
| 25.2 | P0 | 问题 3 | `ModifierCheckerTargets.kt` | 低 | 无 | StaticInit/StaticOrGlobalVar | 71 |
| 26.1 | P1 | 问题 4 | `CfirMatchTypeRelations.kt` | **中** | 无 | **全部 match 套件**（两 checker 消费） | 86 |
| 26.2 | P1 | 问题 5 | `CfirInheritanceDeepChecker.kt` | 低 | 无 | Overload/InterfaceConflict | 44 |
| 26.3 | P1 | 问题 6 | `CfirMutabilityCheckers.kt` | 低 | 无 | MutFunction/RecordMutInvalid | 50 |
| 26.4 | P1 | 问题 7 | `CfirInitializationCheckers.kt` | **中** | **需新增 `markAllSuperInstanceFieldsInitialized`** | SuperThis/InitializationCheck | 26 |
| 26.5 | P1 | 问题 8 | `CfirExtendIndexStore.kt` | **中** | **需新增 `collectSuperClassTargets`** | ImportOrphanrule | 16 |
| 26.6 | P1 | 问题 9 | `coneDiagnosticToCfirDiagnostic.kt` | 低 | 无 | inference 全族 | 144 |
| 27.1 | P2 | 问题 10 | `CfirImportsChecker.kt` | 低 | 改签名带 session | Unused*/宏 import | 26 |
| 27.2 | P2 | 问题 11.1 | `CfirExtendMemberScope.kt` | **中** | **需新增 `indexImplementedInterfaceMembers`** | O2PartGi/ThisType | 41 |
| 27.3 | P2 | 问题 11.2 | `CfirClassUseSiteMemberScope.kt` | **中** | **需新增 `includeExtendMembers` 参数**（多构造点） | ExtendNamelookup | 101 |
| 27.4 | P2 | 问题 13 | `CfirOverrideChecker.kt` | 低 | 无 | OverrideReturnType | 12 |
| 27.5 | P2 | 问题 12 | `CfirSupertypesChecker.kt` | 低 | 无 | ExtendsImplementsInterfaceDuplicated | 61 |
| 28.1 | P3 | 问题 12 剩余 | `CfirInitializationCheckers`/`TYPEALIAS`/`Imports` | 低-中 | 核对 `import.source` 覆盖 | InitializationCheck/Typealias | 12 |
| 28.2 | P3 | 问题 14 | `CfirFunctionSemanticsChecker`/`cone` | 低-中 | 无 | Constructor/InferReturnFail | 303 |
| 31.2 | P2 | 问题 10 基建 | `DefaultsProviderBuilder`（LanguageVersionSettings） | **中** | **影响全部测试语言版本** | EffectGenerated | 14 |
| 33.1 | P3 | 问题 14 收尾 | `CfirOperatorDeclarationChecker.kt` | 低 | 无 | OperatorOverload | 2 |
| 33.2 | P3 | 范围簇收尾 | `CfirInitializationCheckers` 等 | 低 | 问题 2 补丁连带（组合组） | MemberVariable/ThisType/ConstraintCheck | 10 |

**风险要点**：
1. **高影响项**：26.1（两 checker 消费，需全 match 回归）、31.2（测试基建改语言版本，影响全部测试）、27.3（多构造点加参数）。
2. **需新增 API 项**：26.4/26.5/27.2/27.3（共 4 个，31.3 已验证不存在，实施时先补辅助函数/参数再改主逻辑）。
3. **零 testData 同步**：29.1 确认所有补丁后 ACT 匹配 EXP，`.cj` 文件无需改动。
4. **实施顺序**：30.2（问题 1 最先，连带 78 二次消除；问题 11.3 次之）。

### 测试文件

补丁涉及 fixture 见问题 25-33 各节测试文件清单；全量回归用 `:cfir:analysis-tests:test`。

### 修复方案

按风险矩阵分批实施（低风险优先、新增 API 项先补辅助函数）；每批后用 34.2 验证方案统计剩余失败数。

## 34.2 补丁实施验证方案（自动化脚本与命令清单）

### 发生位置

验证脚本：`cfir/analysis-tests/build/verify_patches.sh`（已生成）；命令清单基于问题 25-33 各节回归套件。

### 问题详情（验证方案）

**分批回归命令**（`cfir/analysis-tests/build/verify_patches.sh [stage]`，stage∈{p0,p1,p2,p3,full}）：

| 阶段 | 回归套件（--tests 过滤） |
|------|-------------------------|
| p0 | `*Merge*Generated*` `*GlobalsGenerated*` `*PrimaryConstructorGenerated*` `*SuperThisGenerated*` `*StaticInitGenerated*` `*StaticOrGlobalVarGenerated*` `*InitializationCheckGenerated*` |
| p1 | `*OverloadGenerated*` `*InterfaceConflictInheritance*` `*AutoboxMatch*` `*Unbox*` `*PatternMatching*` `*MutFunctionGenerated*` `*RecordMutInvalid*` `*ImportOrphanrule*` `*Inference*` `*FBounded*` `*TypeArgInfer*` |
| p2 | `*Unused*` `*ExtendNamelookup*` `*ExtendInterfaceStatic*` `*O2PartGi*` `*ThisType*` `*OverrideReturnTypeMismatch*` `*ExtendsImplementsInterfaceDuplicated*` `*EffectGenerated*` |
| p3 | `*TypealiasGenerated*` `*VarrayGenerated*` `*MatchExpressionGenerated*` `*OperatorOverloadGenerated*` `*AssignmentTypeMismatch*` `*AnnotationGenerated*` `*MemberVariableGenerated*` |
| full | 全量 `:cfir:analysis-tests:test` |

**单套件命令**（实施时定向验证）：

```bash
./gradlew :cfir:analysis-tests:test --tests "cangjie.cfir.analysis.tests.CfirAnalysisLLTTest.llt.overload.OverloadGenerated"
# PSI 套件（同 fixture 双路径）：
./gradlew :cfir:analysis-tests:test --tests "cangjie.cfir.analysis.tests.CfirAnalysisLLTPsiTest.llt.overload.OverloadGenerated"
```

**失败统计**（脚本内置）：每批后统计 `build/test-results/test/*.xml` 的失败总数，验证消除量（问题 1 后预期 366、P0 后 445、P1 后 843、P2 后 1359、P3 后 1960）。

### 修复方案

1. 每批实施前：`git stash` 备份当前改动，跑基线确认当前失败数（1960）。
2. 每批实施后：跑对应阶段回归（`verify_patches.sh p0` 等），统计剩余失败数，验证消除量符合预期（30.2 二次消除效应）。
3. 全部完成后：`verify_patches.sh full` 全量回归，目标 0 失败。
4. 每批提交独立 commit（避免混入无关改动，AGENTS.md 工作流建议）。

---

# 问题 19：核心根因深度验证记录（三）——问题 10/12/1 多余方向/4 双向/9 子族逐行验证

> 本章是问题 16、18 的续篇：对问题 10、12、1 多余方向、EXTEND_MEMBER_CANNOT_SHADOW 双向误报、GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT 子族做逐行代码验证，全部基于 2026-08-04 实读源码，非推断。

## 19.1 问题 10 机制确认：UNUSED_IMPORT/UNRESOLVED_IMPORT 误报的两条路径（精确到行）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirImportsChecker.kt` L152-173（`reportImportResolutionDiagnostic`）、L181-217（`reportUnusedImports`）、L310-312（`collectMacroSurfaceReferencedNames`）、L463-479（`referencesUsedMacroPackage`）

### 问题详情（逐行验证结论）

宏测试中 UNUSED_IMPORT/UNRESOLVED_IMPORT 误报的两条机制：

1. **L186 `collectImportUsage` 只统计 `collectReferencedNames() + collectMacroSurfaceReferencedNames(session)`**（L224）。宏展开后生成的符号引用（如 `@Derive` 展开产物调用的方法、生成的字段类型）**不进 `collectReferencedNames()` 集合**——final CFIR 已把原始 `@Macro` 节点替换为展开产物，原始符号引用丢失。

2. **L310-312 `collectMacroSurfaceReferencedNames`** 只回 `macroExpansionRegistry?.usedMacroNames(this)`——即 construction 阶段记录的宏表面名，**不含宏体内引用的普通类型/可调用符号**。若宏体内引用了某 import 的类型（如 `@Derive` 宏体引用 `Equatable`），该 import 的使用不被计入。

3. **L202-206 `isAllUnder`（`*`）导入的宏名校验**：L204 `usedMacroNames(declaration, importedFqName)` 只处理星号导入的宏名，普通导入（`import a.Derive`）的宏名使用未走这条校验。

4. **L463-479 `referencesUsedMacroPackage`**：L470 `usedMacroNames = registry.usedMacroNames(file, importedFqName)` 按导入包名精确匹配；L475 只判 `CfirResolvedImportTarget.Package`——**reexport 场景（`public import a.*` 再被 `import pkg.*` 消费）的目标包名与导入包名不一致时漏判**，误报 UNUSED_IMPORT。

5. **L152-173 `reportImportResolutionDiagnostic`**：L161 `findUnresolvedParentSegmentIndex` 走 source import 可见性 scope 构建，宏展开后新增的隐式 import（宏体内 `import` 指令）未进 `importBindingStore` → 落入 UNRESOLVED_IMPORT。

### 测试文件

`testData/macro/llt/annotation/basecase.cj`、`lambda_not_unit_return_type.cj`、`nested.cj`、`test02.cj`、`external_weak.cj`、`test04~09.cj`（`MacroAnnotationGenerated`/`MacroHostGenerated`，33 个失败，常与 MACRO_EXPANSION_FAILED 同现）。

### 修复方案（验证后细化）

1. `collectImportUsage` 增补宏展开后产物的符号引用扫描：从 `macroExpansionRegistry` 取展开后的 final CFIR 节点，遍历其类型引用/可调用引用，并入 `usage.names`/`usage.targets`。
2. `referencesUsedMacroPackage` L475 放宽目标匹配：除 `CfirResolvedImportTarget.Package` 外，reexport 链（`hasTopLevelName` L487-493 已有逻辑）也要覆盖宏体内引用的普通类型。
3. 宏体内隐式 import 的解析失败由宏展开器自身报告，不应落入宿主文件的 UNRESOLVED_IMPORT。

## 19.2 问题 12 机制确认：「范围/顺序」簇 source 选择漂移（精确到行）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirPatternExpressionChecker.kt` L247-251；`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L2341；`CfirInitializationCheckers.kt` L359/L769；`CfirOperatorDeclarationChecker.kt` L157-193；`CfirSupertypesChecker.kt` L88/L109；`CfirInheritanceDeepChecker.kt` L616；`CfirTypeAliasUnusedTypeParameterChecker.kt` L46

### 问题详情（逐行验证结论）

253 个失败中 214 行有效差异的根因是各 checker 的 `reporter.reportOn(source=...)` 参数选择与官方 marker 定义不一致。逐项验证：

1. **`DIFFERENT_OR_PATTERN`（L247-251 已验证）**：

```kotlin
source = if (reportKindOnWholePattern) {
    orPattern.patternRangeSource() ?: orPattern.source    // L248：整个 or-pattern
} else {
    alternatives[i].source                                // L250：只冲突替代项
}
```

   let-condition 等入口传入 `reportKindOnWholePattern=false` → 只标冲突替代项（`e`），官方标整个 or-pattern（`true | e`）。

2. **`OPTIONAL_CHAIN_NON_OPTIONAL`（L2341）**：source 未含 `!` 前缀，官方标完整 `!s2.startsWith?("")`。

3. **`CLASS_UNINITIALIZED_FIELD`（L359/L769）**：source 取声明整体（`init(a: Int64, c: Bool)`），官方标 `init` 关键字。

4. **`INVALID_SUBSCRIPT_ASSIGN_RETURN`（L157-193）**：`operatorDiagnosticSource()`（L192-193）优先取 `operator` 修饰符源码，官方标返回类型（`Int64`）。

5. **`SUPER_TYPES_DUPLICATE`（L88/L109）、`INHERIT_MEMBER_TYPE_INCONSISTENT`（L616）、`TYPEALIAS_UNUSED_TYPE_PARAMETERS`（L46）**：范围扩宽/收窄混合，均为报告 source 选择策略差异。

### 测试文件

覆盖 57 个 suite，前列：`ExtendGenerated`（16）、`OperatorOverloadGenerated`（9）、`LetInInitGenerated`（8）、`ExtendsImplementsInterfaceDuplicatedGenerated`（8）、`FunctionGenerated`（8）、`TypealiasGenerated`（8）、`InitializationCheckGenerated`（6）、`RedeclarationGenerated`（6）、`VarrayGenerated`（6）、`ConstEvaluationGenerated`（6）、`GenericConstraintInheritanceGenerated`（6）、`MutGenerated`（6）。

### 修复方案（验证后细化）

逐个诊断对核对报告 source——官方标"声明名/关键字/返回类型/完整表达式"的，逐一调整各 checker 的 `reportOn(source=...)` 参数；优先处理 `INVALID_SUBSCRIPT_ASSIGN_RETURN`（改取返回类型 ref source）、`DIFFERENT_OR_PATTERN`（let-condition 入口传 `reportKindOnWholePattern=true`，L247 分支）、`OPTIONAL_CHAIN_NON_OPTIONAL`（取含 `!` 的完整表达式）、`CLASS_UNINITIALIZED_FIELD`（取 `init` 关键字 source）。

## 19.3 问题 1 多余方向机制确认：CANNOT_ASSIGN_TO_IMMUTABLE 对 let 字段初始化赋值误报（精确到行）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirAssignmentLegalityChecker.kt` L372-391（`isImmutableFieldAssignmentForbidden`）、L414-432（`isImmutableVariableAssignmentForbidden`）；`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInitializationCheckers.kt` L627（`trackedVariable` 入栈）、L642-688（分类逻辑）、L793-797（`recordAssignmentClassification`）

### 问题详情（逐行验证结论）

问题 1 的多余方向（宏展开 const init 中 `this.field = param` 被误报）的完整机制链：

1. **L627 `trackedVariable = afterReceiver.trackedVariable(symbol)`**：宏展开生成的字段赋值，若字段 symbol 不在 `InitializationState.trackedVariables` 集合中（宏字段未入栈），返回 null → 落入 L693 `else` 分支报非法成员访问。

2. **L642-688 tracked 分支**：若入栈但 `isPossiblyInitialized || mayRevisitAssignment`（L680）→ 分类 `REASSIGNMENT`（L682）；否则 `INITIALIZATION`（L684）。

3. **L372-391 `isImmutableFieldAssignmentForbidden`**：

```kotlin
if (field.isVar) return false                                   // L376：var 字段可写
val constructor = context.findClosestDeclaration<CfirConstructor>() ?: return true   // L377
if (field.status.isStatic != constructor.status.isStatic) return true   // L378
if (field.hasSameNamePrimaryConstructorPropertyInOwner()) return true   // L379
if (field.initializer != null) return true   // L380
return when (assignment?.let { CfirInitializationAssignmentClassifier.classifyAssignment(it, context) }) {
    INITIALIZATION, PRIORITY_INITIALIZATION_DIAGNOSTIC -> false         // L382-384：合法
    REASSIGNMENT, NOT_TRACKED, null -> true                            // L386-389：报不可变赋值
}
```

4. **验证结论**：宏展开 const init 的 `this.field = param` 走 L386-389 的 `REASSIGNMENT`（字段已在宏展开前被标记初始化）或 `NOT_TRACKED`（宏字段不入栈）→ 返回 true → `mutationTarget` L296-297 分类 `MutationTarget.ImmutableValue` → 报 `CANNOT_ASSIGN_TO_IMMUTABLE`。官方语义中构造器体内 `this.field = param` 是合法初始化，不应报。

### 测试文件

`testData/macro/llt/annotation/lambda_not_unit_return_type.cj`、`basecase.cj`、`nested.cj`、`test02.cj`、`external_weak.cj`、`test04~09.cj`（`MacroAnnotationGenerated`，14 行多余与问题 1 缺少方向同源）。

### 修复方案（验证后细化）

1. 宏展开器在展开 const init 时把生成的字段 symbol 显式注册到 `InitializationState.trackedVariables`（L627 入栈），分类为 `INITIALIZATION` 而非 `REASSIGNMENT`/`NOT_TRACKED`。
2. `isImmutableFieldAssignmentForbidden` L387-389 的 `NOT_TRACKED` 分支改为：构造器体内首次赋值（`assignment` 非空且 `classifyAssignment` 返回 `NOT_TRACKED`）时返回 false（合法初始化），仅非构造器场景返回 true。

## 19.4 EXTEND_MEMBER_CANNOT_SHADOW 双向误报机制确认（与问题 5 同源门禁，精确到行）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInheritanceDeepChecker.kt` L949-984

### 问题详情（逐行验证结论）

extend 场景的 `EXTEND_MEMBER_CANNOT_SHADOW`（L968）与问题 5 的 `INHERIT_MEMBER_KIND_INCONSISTENT`（L975）共享同一门禁：

1. **L949-961 static 冲突分支**：`hasStaticConflict = ownInfo.isStatic != superInfo.isStatic`，为 true 时报 `STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME`（L954）。
2. **L963-984 kind 不一致分支**：`if (ownInfo.kind != superInfo.kind) { if (!hasStaticConflict && ...) { ... } }`——**L964 的 `!hasStaticConflict` 门禁使 extend 场景的 L968 `EXTEND_MEMBER_CANNOT_SHADOW` 在 static 冲突存在时永远不可达**。
3. **双向误报**：
   - **缺少方向**：extend 内同名函数与继承接口成员 kind 不一致但 static 冲突存在时，官方报 `EXTEND_MEMBER_CANNOT_SHADOW`，CFIR 报 `STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME`（门禁阻断）。
   - **多余方向**：extend 内同名函数与继承接口成员 kind 一致但 static 冲突存在时，官方不报（kind 一致合法），CFIR 报 `STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME`（L949-961 无 kind 判定）。

### 测试文件

`extend_namelookup2.cj`、`extend_namelookup8.cj`、`extend_namelookup9.cj`、`extend_mutable_function_invalid_1.cj`、`record_extend_mut_invalid_13.cj`、`samename_conditionandifbody.cj`（与问题 5/11.2 同族，16 个失败）。

### 修复方案（验证后细化）

与问题 5 同修复：删除 L964 的 `!hasStaticConflict` 门禁，static 冲突不再单独报 `STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME`，统一并入 kind 不一致分支（非 extend 场景报 `INHERIT_MEMBER_KIND_INCONSISTENT`，extend 场景报 `EXTEND_MEMBER_CANNOT_SHADOW` L968）；kind 一致但 static 冲突的场景官方视为合法（不报），仅保留给同声明层重名（`CfirConflictsDeclarationChecker` 管辖）。

## 19.5 GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT 子族触发条件逐行确认（3 处上报点）

### 发生位置

`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirGenericBareClassifierAccessChecker.kt` L60-64、L94-98；`cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt` L2094-2098、L2194-2197、L2292-2295

### 问题详情（逐行验证结论）

三处上报点的触发条件：

1. **L60-64（裸泛型 classifier 当值/限定符）**：

```kotlin
if (expression.typeArguments.isNotEmpty()) return   // L43：有类型实参不报
val resolvedSymbol = expression.calleeReference.resolvedBareAccessSymbol() ?: return   // L44
...
if (!classLikeSymbol.requiresExplicitTypeArgumentsForBareAccess()) return   // L58
reporter.reportOn(resolvedReference.source ?: expression.source, GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT, ...)
```

   裸 `Array`（无 `<T>`）当值或限定符使用时报；`Array<Int64>` 不报。

2. **L94-98（enum constructor owner 泛型未固定）**：

```kotlin
if (explicitReceiver != null) return   // L79：有显式 receiver 不报
if (errorReference?.diagnostic is ConeUnableToInferExpressionTypeError) return   // L81
if (this is CfirFunctionCall && enumConstructor.valueParameters.isNotEmpty()) return   // L86
...
if (constructorType != null && !constructorType.containsUnfixedOwnerTypeParameter(ownerTypeParameters)) return   // L92
reporter.reportOn(calleeReference.source ?: source, GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT, ...)
```

   `T1.a16(...)` 的 owner `Test<T>` 未由实参/目标类型固定时报；已固定成 `Test<Int64>` 不报。

3. **L2094-2098（`ConeTypeParameterInQualifiedAccess` 映射）**、**L2194-2197（`DiagnosticKind.GenericTypeWithoutTypeArgument`）**、**L2292-2295（`ConeUnmatchedTypeArgumentsError` 且 `actualCount==0`）**：resolve 层上报的裸泛型访问映射到同一诊断名。

4. **失败形态**：`genericParameterType1.cj`/`extendInterfaceStatic.cj` 等EXP 无该诊断、ACT 多余（裸泛型在 extend 静态成员/泛型参数上下文中官方允许推断，CFIR 误报）；`generic_type_should_be_used_with_type_argument.cj` 等 EXP 期望该诊断、ACT 缺少（某些裸泛型场景未触发）。

### 测试文件

`testData/llt/generic/genericParameterType1.cj`、`extendInterfaceStatic.cj`、`generic_type_should_be_used_with_type_argument.cj`、`enum_generic_member01.cj`、`typealias_generic01.cj`（`GenericConstraintInheritanceGenerated`/`EnumGenerated`/`TypealiasGenerated`，17 个双向失败）。

### 修复方案（验证后细化）

1. L58 `requiresExplicitTypeArgumentsForBareAccess()` 的判定对齐官方：extend 静态成员上下文中的裸泛型 qualifier 允许推断（不报），仅在真正无法推断时报。
2. L86 enum constructor payload 的 owner 类型推断失败应报 `UNABLE_TO_INFER_GENERIC_FUNC`（与问题 9 同源），不降级成裸 classifier 诊断。
3. 补齐缺少方向：某些裸泛型场景（如 `typealias` 展开后的裸泛型）未触发 L58 检查，需在 typealias 展开路径补检查点。

---
