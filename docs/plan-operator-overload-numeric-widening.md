# 操作符重载解析 + 隐式转换/数值拓宽 实现方案

## Context

Phase 5 完成了 Checker 类型检查闭环后，剩余 P0 缺失中最高优先级的两项：

1. **操作符重载解析** — 比较运算符（`==` `!=` `<` `>` `<=` `>=`）当前硬编码返回 Bool，不校验操作数类型兼容性；算术运算符在原始类型上通过 `CfirBuiltinOperatorResolver` 回退工作，但不支持混合宽度运算。
2. **数值拓宽** — `ConeSubtypeChecker` 仅有 `IdealInt <: 任何整数` 规则，缺少具体数值拓宽（`Int8 <: Int16 <: Int32 <: Int64`），导致 `var x: Int64 = someInt32Value` 会误报 TYPE_MISMATCH。

两个特性紧密耦合：数值拓宽规则是混合宽度算术和比较操作符的基础。

---

## 已有基础设施

| 组件 | 路径 | 当前状态 |
|------|------|---------|
| `ConeSubtypeChecker` | `cfir/cfir-cones/src/.../ConeSubtypeChecker.kt` | 16 规则，规则 5 仅处理 IdealType，规则 6 原始类型仅 `kind == kind` |
| `PrimitiveTypeKind` | `cfir/cfir-cones/src/.../ConePrimitiveType.kt` | 枚举含 INT8-64/UINT8-64/FLOAT16-64/IDEAL_* |
| `CfirBuiltinOperatorResolver` | `cfir/resolve/src/.../body/CfirBuiltinOperatorResolver.kt` | 算术/位运算/一元，有 IdealType 提升，无混合宽度，无比较操作符 |
| `CfirExpressionsResolveTransformer` | `cfir/resolve/src/.../body/CfirExpressionsResolveTransformer.kt` | `transformComparisonExpression` 硬编码 Bool；`transformFunctionCall` 有 builtin 回退 |
| `ConversionUtils.kt` | `cfir/raw-cfir/.../builder/ConversionUtils.kt` | 比较操作符 → `CfirComparisonOp`；算术 → 函数名映射 |
| `CfirTypeCheckUtils` | `cfir/checkers/src/.../CfirTypeCheckUtils.kt` | 使用 `BasicConeTypeContext`（空超类型图），子类型检查自动受益于 ConeSubtypeChecker 增强 |

---

## 实现方案

### Step 1: 数值拓宽规则 — `ConeNumericWidening`

**新建** `cfir/cfir-cones/src/org/cangnova/cangjie/cfir/types/ConeNumericWidening.kt`

定义仓颉数值类型的拓宽层级（参考 C++ 编译器 `Promotion.cpp`）：

```kotlin
object ConeNumericWidening {
    /** 有符号整数拓宽序：Int8 → Int16 → Int32 → Int64 */
    private val SIGNED_INT_RANK: Map<PrimitiveTypeKind, Int> = mapOf(
        PrimitiveTypeKind.INT8 to 0,
        PrimitiveTypeKind.INT16 to 1,
        PrimitiveTypeKind.INT32 to 2,
        PrimitiveTypeKind.INT64 to 3,
    )
    /** 无符号整数拓宽序：UInt8 → UInt16 → UInt32 → UInt64 */
    private val UNSIGNED_INT_RANK: Map<PrimitiveTypeKind, Int> = mapOf(
        PrimitiveTypeKind.UINT8 to 0,
        PrimitiveTypeKind.UINT16 to 1,
        PrimitiveTypeKind.UINT32 to 2,
        PrimitiveTypeKind.UINT64 to 3,
    )
    /** 浮点拓宽序：Float16 → Float32 → Float64 */
    private val FLOAT_RANK: Map<PrimitiveTypeKind, Int> = mapOf(
        PrimitiveTypeKind.FLOAT16 to 0,
        PrimitiveTypeKind.FLOAT32 to 1,
        PrimitiveTypeKind.FLOAT64 to 2,
    )

    /** sub 是否可隐式拓宽为 super（同族且 rank(sub) <= rank(super)） */
    fun isWideningAllowed(sub: PrimitiveTypeKind, super_: PrimitiveTypeKind): Boolean

    /** 取两个同族数值类型中较宽的（用于二元运算结果类型） */
    fun widerOf(a: PrimitiveTypeKind, b: PrimitiveTypeKind): PrimitiveTypeKind?
}
```

拓宽规则（严格同族）：
- `Int8 <: Int16 <: Int32 <: Int64`
- `UInt8 <: UInt16 <: UInt32 <: UInt64`
- `Float16 <: Float32 <: Float64`
- **不跨族**：`Int32` 不能拓宽为 `UInt32`，`Int32` 不能拓宽为 `Float64`

### Step 2: 更新 `ConeSubtypeChecker` — 规则 6 增加拓宽

**修改** `cfir/cfir-cones/src/.../ConeSubtypeChecker.kt`

原规则 6（第 89-91 行）：
```kotlin
// 规则 6: 原始类型 — 仅同种 kind
if (subType is ConePrimitiveType && superType is ConePrimitiveType) {
    return subType.kind == superType.kind
}
```

修改为：
```kotlin
// 规则 6: 原始类型 — 同种 kind 或数值拓宽
if (subType is ConePrimitiveType && superType is ConePrimitiveType) {
    return subType.kind == superType.kind
        || ConeNumericWidening.isWideningAllowed(subType.kind, superType.kind)
}
```

**影响范围**：所有使用 `ConeSubtypeChecker` 的地方自动受益，包括：
- `CfirTypeCheckUtils.isSubtypeOf()` → checker 类型检查
- `CfirCheckArguments` → 调用参数验证
- 任何未来的子类型检查场景

### Step 3: 内建比较操作符 — 增强 `CfirBuiltinOperatorResolver`

**修改** `cfir/resolve/src/.../body/CfirBuiltinOperatorResolver.kt`

新增比较操作符处理：

```kotlin
/** 比较操作符名称（对应 CfirComparisonOp） */
private val COMPARISON_OPS = setOf("equal", "notEqual", "less", "greater", "lessEqual", "greaterEqual")

// 在 tryResolveBuiltinOperator 的 when 中新增分支：
opName in COMPARISON_OPS && argumentTypes.size == 1 ->
    resolveComparisonOp(receiverType, argumentTypes.first())
```

`resolveComparisonOp` 逻辑：
- 两端均为数值类型 → Bool（通过拓宽规则判断兼容性）
- 两端均为 Bool → Bool（仅 `==`/`!=`）
- 两端均为 Rune → Bool
- 两端均为 String → Bool
- 其他 → null（非内建比较）

### Step 4: 增强 `transformComparisonExpression` — 类型校验

**修改** `cfir/resolve/src/.../body/CfirExpressionsResolveTransformer.kt`

当前实现（第 550-563 行）无条件返回 Bool。修改为：

```kotlin
override fun transformComparisonExpression(
    comparisonExpression: CfirComparisonExpression,
    data: CfirResolutionMode,
): CfirExpression {
    // 递归解析左右操作数
    comparisonExpression.left = comparisonExpression.left
        .transform(transformer, CfirResolutionMode.ContextIndependent)
    comparisonExpression.right = comparisonExpression.right
        .transform(transformer, CfirResolutionMode.ContextIndependent)

    val leftType = comparisonExpression.left.coneTypeOrNull
    val rightType = comparisonExpression.right.coneTypeOrNull

    // 尝试内建比较操作符解析
    if (leftType != null && rightType != null) {
        val opFuncName = comparisonExpression.operation.toFunctionName()
        val result = CfirBuiltinOperatorResolver.tryResolveBuiltinOperator(
            Name.identifier(opFuncName), leftType, listOf(rightType)
        )
        if (result != null) {
            comparisonExpression.replaceConeTypeOrNull(result)
            return comparisonExpression
        }
    }

    // TODO: 用户类型操作符重载（Equatable/Comparable 接口）
    // 回退：返回 Bool（保持向后兼容）
    comparisonExpression.replaceConeTypeOrNull(builtinTypes.boolType)
    return comparisonExpression
}
```

新增 `CfirComparisonOp.toFunctionName()` 扩展（加在 `CfirOperatorExpressions.kt` 或单独工具文件中）：

```kotlin
fun CfirComparisonOp.toFunctionName(): String = when (this) {
    CfirComparisonOp.EQ -> "equal"
    CfirComparisonOp.NE -> "notEqual"
    CfirComparisonOp.LT -> "less"
    CfirComparisonOp.GT -> "greater"
    CfirComparisonOp.LE -> "lessEqual"
    CfirComparisonOp.GE -> "greaterEqual"
}
```

### Step 5: 混合宽度算术 — 增强 `resolveIdealTypePromotion`

**修改** `cfir/resolve/src/.../body/CfirBuiltinOperatorResolver.kt`

将 `resolveIdealTypePromotion` 重命名为 `resolveNumericPromotion`，增加混合宽度逻辑：

```kotlin
private fun resolveNumericPromotion(
    receiverType: ConeCangjieType,
    argType: ConeCangjieType,
): ConeCangjieType {
    val receiverIsIdeal = receiverType.isIdealType
    val argIsIdeal = argType.isIdealType

    return when {
        // IdealType 规则（保持不变）
        receiverIsIdeal && argIsIdeal -> receiverType
        receiverIsIdeal -> argType
        argIsIdeal -> receiverType
        // 新增：混合宽度 → 取较宽类型
        receiverType is ConePrimitiveType && argType is ConePrimitiveType -> {
            val wider = ConeNumericWidening.widerOf(receiverType.kind, argType.kind)
            if (wider != null) ConePrimitiveType(wider) else receiverType
        }
        else -> receiverType
    }
}
```

---

## 文件清单

### 新建文件（1 个）

| 文件 | 模块 | 说明 |
|------|------|------|
| `cfir/cfir-cones/src/.../types/ConeNumericWidening.kt` | cfir-cones | 数值拓宽层级和 rank 查询 |

### 修改文件（3 个）

| 文件 | 变更 |
|------|------|
| `cfir/cfir-cones/src/.../types/ConeSubtypeChecker.kt` | 规则 6 增加拓宽判定（~3 行改动） |
| `cfir/resolve/src/.../body/CfirBuiltinOperatorResolver.kt` | +比较操作符解析 +混合宽度提升 +rename（~60 行） |
| `cfir/resolve/src/.../body/CfirExpressionsResolveTransformer.kt` | transformComparisonExpression 增加类型校验（~15 行） |

### 可能新增的辅助文件

| 文件 | 条件 | 说明 |
|------|------|------|
| `cfir/cfir-tree/src/.../expressions/CfirComparisonOpUtils.kt` | 若 `toFunctionName()` 不适合放在生成代码旁边 | ComparisonOp → 函数名映射 |

---

## 设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 拓宽层级放在哪 | cfir-cones 模块（新文件） | ConeSubtypeChecker 和 CfirBuiltinOperatorResolver 都需要，cfir-cones 是两者共同依赖 |
| 拓宽是否跨族 | 严格同族（有符号/无符号/浮点各自独立） | 对齐仓颉语言规范和 C++ 编译器行为，跨族需要显式转换 |
| 比较操作符回退 | 内建不匹配时仍返回 Bool | 保持向后兼容，避免大量代码突然报错；后续 Equatable/Comparable 接口集成时再收紧 |
| 混合宽度不兼容时 | 返回 receiverType | 后续由 checker 报 TYPE_MISMATCH，保持 resolve 不崩溃 |
| IntNative/UIntNative | 暂不参与拓宽 | 平台相关类型，宽度不确定，安全起见排除 |

---

## 验证

1. `./gradlew :cfir:cfir-cones:test` — 新增拓宽测试通过
2. `./gradlew :cfir:cfir-cones:compileKotlin` — 编译无错
3. `./gradlew :cfir:resolve:compileKotlin` — 编译无错
4. `./gradlew :cfir:resolve:test` — 现有测试不回归
5. 人工验证场景：
   - `var x: Int64 = int32Value` → 不报 TYPE_MISMATCH（拓宽通过）
   - `var x: Int32 = int64Value` → 报 TYPE_MISMATCH（窄化不允许）
   - `Int32 + Int64` → 结果类型 Int64（混合宽度）
   - `1 == 2` → Bool（内建比较）
   - `Int32 == Int64` → Bool（拓宽兼容比较）