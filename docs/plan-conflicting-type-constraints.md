# 方案 C：CONFLICTING_TYPE_CONSTRAINTS 诊断实现计划

## 背景

官方仓颉编译器对泛型约束冲突报告 "unable to infer generic argument"，区别于普通参数类型不匹配。
本方案实现方案 C：
- `CANNOT_INFER_PARAMETER_TYPE` — 类型变量无任何约束（无从推断）
- `CONFLICTING_TYPE_CONSTRAINTS` — 类型变量有相互矛盾的约束（约束存在但不可同时满足）
- `ARGUMENT_TYPE_MISMATCH` — 仅用于非泛型的直接参数类型不匹配

## 测试用例对应关系

| 测试文件 | 场景 | 期望诊断 | 定位 |
|---------|------|---------|------|
| `genericReturnTypeInference.cj` | `identity(42)` 期望 Int64 | **无错误**（IdealInt 兼容 Int64） | — |
| `genericConstraintFromExpectedType.cj` | `identity("hello")` 期望 Int64 | `CONFLICTING_TYPE_CONSTRAINTS` | `identity` 函数名 |
| `genericArgumentConstraintConflict.cj` | `choose(1, true)` | `CONFLICTING_TYPE_CONSTRAINTS` | `choose` 函数名 |
| `genericConstraintCascade.cj` | `pair(1, "oops")` 期望 Int64 | `CONFLICTING_TYPE_CONSTRAINTS` | `pair` 函数名 |

## 实现步骤

### 步骤 1：新增 `ConflictingTypeConstraints` ResolutionDiagnostic

**文件**: `cfir/semantics/src/org/cangnova/cangjie/cfir/diagnostic/ResolutionDiagnostic.kt`

在 `InferenceConstraintError` 之后添加：

```kotlin
/**
 * 泛型类型变量存在相互矛盾的约束（有约束但无法同时满足）。
 * 对应官方仓颉编译器的 "unable to infer generic argument" 约束冲突报告。
 * 区别于 InferenceConstraintError（变量完全无法推断）。
 */
class ConflictingTypeConstraints(
    /** 产生冲突的类型参数名，如 "T" */
    val typeParameterName: String,
    /** 下界描述（来自实参约束），如 "String" */
    val lowerBoundDescription: String,
    /** 上界描述（来自期望类型或其他约束），如 "Int64" */
    val upperBoundDescription: String,
) : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE)
```

### 步骤 2：在 `CfirErrors.kt` 添加诊断工厂（手动，不用生成器）

**文件**: `cfir/checkers/gen/org/cangnova/cangjie/cfir/analysis/diagnostics/CfirErrors.kt`

在 `CANNOT_INFER_PARAMETER_TYPE` 之后（CONSTRAINT 分组内）添加：

```kotlin
val CONFLICTING_TYPE_CONSTRAINTS: CjDiagnosticFactory1<String> = CjDiagnosticFactory1(
    "CFIR_CONFLICTING_TYPE_CONSTRAINTS",
    Severity.ERROR,
    SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED,
    CjElement::class,
    getRendererFactory()
)
```

参数 `String` 是类型参数名（如 `"T"`）。

### 步骤 3：在 `CfirErrorsDefaultMessages.kt` 添加消息模板

**文件**: `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/CfirErrorsDefaultMessages.kt`

在 `CANNOT_INFER_PARAMETER_TYPE` 消息之后添加：

```kotlin
map.put(
    CfirErrors.CONFLICTING_TYPE_CONSTRAINTS,
    "Conflicting type constraints for type parameter ''{0}'': constraints cannot be simultaneously satisfied.",
    RENDER_STRING,
)
```

需要确认 `RENDER_STRING` 渲染器是否存在，若不存在则改用 `DECLARATION_NAME` 或 `{ it }`。

### 步骤 4：修改 `CfirInferTypeArguments` 分类约束系统错误

**文件**: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/calls/stages/CfirInferTypeArguments.kt`

将当前（第 69-74 行）：

```kotlin
if (constraintSystem.hasErrors) {
    constraintSystem.errors.forEach { error ->
        candidate.callInfo.session.inferenceLogger?.logError(error, constraintSystem)
        sink.reportDiagnostic(InferenceConstraintError(error.message))
    }
}
```

改为按 `CfirConstraintIssue` 子类型分类：

```kotlin
if (constraintSystem.hasErrors) {
    constraintSystem.errors.forEach { error ->
        candidate.callInfo.session.inferenceLogger?.logError(error, constraintSystem)
        val diagnostic = when (error) {
            is CfirConstraintIssue.ConflictingBounds -> ConflictingTypeConstraints(
                typeParameterName = error.variable.lookupTag.name,
                lowerBoundDescription = error.variable.lowerBounds.joinToString(", ") { it.toString() },
                upperBoundDescription = error.variable.upperBounds.joinToString(", ") { it.toString() },
            )
            else -> InferenceConstraintError(error.message)
        }
        sink.reportDiagnostic(diagnostic)
    }
}
```

新增导入：
```kotlin
import org.cangnova.cangjie.cfir.constraints.CfirConstraintIssue
import org.cangnova.cangjie.cfir.diagnostic.ConflictingTypeConstraints
```

### 步骤 5：在 `reportCandidateDiagnostics` 处理新诊断类型

**文件**: `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/body/CfirExpressionsResolveTransformer.kt`

1. 清理 4 处 `System.err.println("[CFIR-DEBUG]...")`

2. 在 `reportCandidateDiagnostics` 的 `when` 中，在 `is VisibilityError` 之后、`else` 之前添加：

```kotlin
is ConflictingTypeConstraints -> {
    val source = callSite.source as? AbstractCjSourceElement ?: continue
    session.diagnosticReporter.reportOn(
        source,
        CfirErrors.CONFLICTING_TYPE_CONSTRAINTS,
        diagnostic.typeParameterName,
        diagnosticCtx,
    )
}
```

3. 新增导入：
```kotlin
import org.cangnova.cangjie.cfir.diagnostic.ConflictingTypeConstraints
```

### 步骤 6：更新测试文件诊断标记

**6a** `cfir/analysis-tests/testData/diagnostics/type-mismatch/genericConstraintFromExpectedType.cj`

将：
```cj
    let number: Int64 <!PATTERN_INITIALIZER_TYPE_MISMATCH!>=<!> identity("hello")
```
改为：
```cj
    let number: Int64 = <!CONFLICTING_TYPE_CONSTRAINTS!>identity<!>("hello")
```

**6b** `cfir/analysis-tests/testData/diagnostics/type-mismatch/genericArgumentConstraintConflict.cj`

将：
```cj
    let value = choose(1, <!ARGUMENT_TYPE_MISMATCH!>true<!>)
```
改为：
```cj
    let value = <!CONFLICTING_TYPE_CONSTRAINTS!>choose<!>(1, true)
```

**6c** `cfir/analysis-tests/testData/diagnostics/type-mismatch/genericConstraintCascade.cj`

将：
```cj
    let number: Int64 = pair(1, <!ARGUMENT_TYPE_MISMATCH!>"oops"<!>)
```
改为：
```cj
    let number: Int64 = <!CONFLICTING_TYPE_CONSTRAINTS!>pair<!>(1, "oops")
```

**6d** `cfir/analysis-tests/testData/diagnostics/type-mismatch/genericReturnTypeInference.cj`
无需修改（已无错误标记，期望无诊断）。

### 步骤 7（可选）：同步 `CfirDiagnosticsList.kt`

**文件**: `cfir/checkers/checkers-component-generator/src/org/cangnova/cangjie/cfir/checkers/generator/diagnostics/CfirDiagnosticsList.kt`

在 `CONSTRAINT` 分组的 `CANNOT_INFER_PARAMETER_TYPE` 之后添加：

```kotlin
val CONFLICTING_TYPE_CONSTRAINTS by error<CjElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
    parameter<String>("typeParameterName")
}
```

## 数据流分析（实现后）

```
泛型调用: identity("hello")，期望类型 Int64

1. CfirCreateFreshTypeVariableSubstitutorStage
   → 创建 CfirTypeVariable(T)，注册到约束系统

2. CfirCheckArguments.checkWithConstraintSystem()
   → argType=String, paramType=T（已注册类型变量）
   → isRegisteredTypeVariable(T)=true
   → addSubtypeConstraint(String, T, ArgumentPosition(0))  ← 不报错，只添加约束
   → 返回 true

3. CfirInferTypeArguments
   → collectReturnTypeConstraint: T <: Int64（来自期望类型）
   → fixAllVariables():
     T.lowerBounds=[String], T.upperBounds=[Int64]
     join(String) = String，isCompatible(String, Int64)=false
     → reportIssue(ConflictingBounds(T, ...))
   → constraintSystem.hasErrors=true
   → error is ConflictingBounds
   → sink.reportDiagnostic(ConflictingTypeConstraints("T", "String", "Int64"))

4. CfirCallResolver
   → candidate.diagnostics 含 ConflictingTypeConstraints（INAPPLICABLE）
   → 返回 ResolvedWithErrors(candidate)

5. resolveCallWithPhase3 → reportCandidateDiagnostics
   → diagnostic is ConflictingTypeConstraints
   → reportOn(source, CONFLICTING_TYPE_CONSTRAINTS, "T", ctx)
   → 定位在 identity 函数名
```

## 注意事项

1. **`shouldAddExpectedTypeConstraint` 逻辑**：当前实现在变量已有 lowerBounds 时跳过 expectedType 约束（见 `CfirInferTypeArguments.kt` 第 151-163 行）。这会阻止 `genericConstraintCascade` 场景中 `T <: Int64` 约束的添加。需要移除或修改此判断，让 expectedType 约束始终被添加。

2. **构建缓存**：所有测试运行需加 `--no-build-cache`。

3. **`RENDER_STRING` 渲染器**：需要确认 `CfirErrorsDefaultMessages.kt` 中可用的渲染器类型。

## 验证命令

```bash
./gradlew :cfir:analysis-tests:test \
  --tests "org.cangnova.cangjie.cfir.analysis.tests.CfirAnalysisDiagnosticsTestGenerated" \
  --no-build-cache 2>&1 | tail -20
```