package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.name.ClassId

// ============================================================
// 内建原始类型判断（基于 PrimitiveTypeKind，O(1)）
// 适用于 ConePrimitiveType：Int64, Bool, Float64 等
// ============================================================

val ConeCangJieType.isBoolean: Boolean
    get() = this is ConePrimitiveType && kind == PrimitiveTypeKind.BOOLEAN

val ConeCangJieType.isInt8: Boolean
    get() = this is ConePrimitiveType && kind == PrimitiveTypeKind.INT8

val ConeCangJieType.isInt16: Boolean
    get() = this is ConePrimitiveType && kind == PrimitiveTypeKind.INT16

val ConeCangJieType.isInt32: Boolean
    get() = this is ConePrimitiveType && kind == PrimitiveTypeKind.INT32

val ConeCangJieType.isInt64: Boolean
    get() = this is ConePrimitiveType && kind == PrimitiveTypeKind.INT64

val ConeCangJieType.isFloat16: Boolean
    get() = this is ConePrimitiveType && kind == PrimitiveTypeKind.FLOAT16

val ConeCangJieType.isFloat32: Boolean
    get() = this is ConePrimitiveType && kind == PrimitiveTypeKind.FLOAT32

val ConeCangJieType.isFloat64: Boolean
    get() = this is ConePrimitiveType && kind == PrimitiveTypeKind.FLOAT64

val ConeCangJieType.isRune: Boolean
    get() = this is ConePrimitiveType && kind == PrimitiveTypeKind.RUNE

/** 内建整数类型（含 IdealInt） */
val ConeCangJieType.isIntegerType: Boolean
    get() = this is ConePrimitiveType && kind.isInteger

/** 内建浮点类型（含 IdealFloat） */
val ConeCangJieType.isFloatType: Boolean
    get() = this is ConePrimitiveType && kind.isFloat

/** 内建数值类型（整数 + 浮点） */
val ConeCangJieType.isNumericType: Boolean
    get() = this is ConePrimitiveType && kind.isNumeric

/** 是否为内建原始类型 */
val ConeCangJieType.isPrimitiveType: Boolean
    get() = this is ConePrimitiveType

// ---- IdealType 判断（仓颉特有，编译期字面量推断） ----

val ConeCangJieType.isIdealInt: Boolean
    get() = this is ConeIdealIntLiteralType || (this is ConePrimitiveType && kind == PrimitiveTypeKind.IDEAL_INT)

val ConeCangJieType.isIdealFloat: Boolean
    get() = this is ConeIdealFloatLiteralType || (this is ConePrimitiveType && kind == PrimitiveTypeKind.IDEAL_FLOAT)

val ConeCangJieType.isIdealType: Boolean
    get() = this is ConeIdealLiteralType || (this is ConePrimitiveType && kind.isIdeal)

/** 是否为理想字面量类型（[ConeIdealLiteralType] 形式） */
val ConeCangJieType.isIdealLiteralType: Boolean
    get() = this is ConeIdealLiteralType

// ============================================================
// 标准库类型判断（基于 ClassId）
// 适用于 ConeClassLikeType/ConeStructType/ConeEnumType 等
// ============================================================

/** 提取类类型的 ClassId（内建原始类型返回 null） */
val ConeCangJieType.classId: ClassId?
    get() = when (this) {
        is ConeClassifierType -> lookupTag.classId
        is ConeTypeAliasType -> classId
        else -> null
    }

val ConeCangJieType.isString: Boolean
    get() = classId == StdlibClassIds.String

val ConeCangJieType.isArray: Boolean
    get() = classId == StdlibClassIds.Array

/**
 * 提取标准库 `Array<T>` 的元素类型。
 *
 * 这里故意只识别名义 `Array<T>`，不再依赖已经被移除的 `ConeArrayType`。
 */
val ConeCangJieType.arrayElementType: ConeCangJieType?
    get() = when (this) {
        is ConeErrorType ->
            delegatedType?.arrayElementType
                ?: if (classId == StdlibClassIds.Array) typeArguments.singleOrNull()?.type else null
        is ConeClassLikeType ->
            if (classId == StdlibClassIds.Array) typeArguments.singleOrNull()?.type else null
        is ConeStructType ->
            if (classId == StdlibClassIds.Array) typeArguments.singleOrNull()?.type else null
        is ConeTypeAliasType -> expandedType?.arrayElementType
        else -> null
    }

val ConeCangJieType.isOption: Boolean
    get() = classId == StdlibClassIds.Option

/**
 * 提取标准库 `Option<T>` 的 payload 类型 `T`。
 *
 * 官方 `??`、`?` 链和 `Some`/`None` 模式都以 `Option<T>` 的唯一类型实参
 * 作为解包后的语义类型，因此这里集中暴露该投影，避免各个 resolve/checker
 * 分散手写 `StdlibClassIds.Option` 判断。
 */
val ConeCangJieType.optionElementType: ConeCangJieType?
    get() = when (this) {
        is ConeErrorType ->
            delegatedType?.optionElementType
                ?: if (classId == StdlibClassIds.Option) typeArguments.singleOrNull()?.type else null
        is ConeClassLikeType ->
            if (classId == StdlibClassIds.Option) typeArguments.singleOrNull()?.type else null
        is ConeEnumType ->
            if (classId == StdlibClassIds.Option) typeArguments.singleOrNull()?.type else null
        is ConeTypeAliasType -> expandedType?.optionElementType
        else -> null
    }
