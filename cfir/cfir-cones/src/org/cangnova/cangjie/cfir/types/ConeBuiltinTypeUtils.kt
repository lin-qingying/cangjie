package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.name.ClassId

// ============================================================
// 内建原始类型判断（基于 PrimitiveTypeKind，O(1)）
// 适用于 ConePrimitiveType：Int64, Bool, Float64 等
// ============================================================

val ConeCangjieType.isBoolean: Boolean
    get() = this is ConePrimitiveType && kind == PrimitiveTypeKind.BOOLEAN

val ConeCangjieType.isInt8: Boolean
    get() = this is ConePrimitiveType && kind == PrimitiveTypeKind.INT8

val ConeCangjieType.isInt16: Boolean
    get() = this is ConePrimitiveType && kind == PrimitiveTypeKind.INT16

val ConeCangjieType.isInt32: Boolean
    get() = this is ConePrimitiveType && kind == PrimitiveTypeKind.INT32

val ConeCangjieType.isInt64: Boolean
    get() = this is ConePrimitiveType && kind == PrimitiveTypeKind.INT64

val ConeCangjieType.isFloat16: Boolean
    get() = this is ConePrimitiveType && kind == PrimitiveTypeKind.FLOAT16

val ConeCangjieType.isFloat32: Boolean
    get() = this is ConePrimitiveType && kind == PrimitiveTypeKind.FLOAT32

val ConeCangjieType.isFloat64: Boolean
    get() = this is ConePrimitiveType && kind == PrimitiveTypeKind.FLOAT64

val ConeCangjieType.isRune: Boolean
    get() = this is ConePrimitiveType && kind == PrimitiveTypeKind.RUNE

/** 内建整数类型（含 IdealInt） */
val ConeCangjieType.isIntegerType: Boolean
    get() = this is ConePrimitiveType && kind.isInteger

/** 内建浮点类型（含 IdealFloat） */
val ConeCangjieType.isFloatType: Boolean
    get() = this is ConePrimitiveType && kind.isFloat

/** 内建数值类型（整数 + 浮点） */
val ConeCangjieType.isNumericType: Boolean
    get() = this is ConePrimitiveType && kind.isNumeric

/** 是否为内建原始类型 */
val ConeCangjieType.isPrimitiveType: Boolean
    get() = this is ConePrimitiveType

// ---- IdealType 判断（仓颉特有，编译期字面量推断） ----

val ConeCangjieType.isIdealInt: Boolean
    get() = this is ConePrimitiveType && kind == PrimitiveTypeKind.IDEAL_INT

val ConeCangjieType.isIdealFloat: Boolean
    get() = this is ConePrimitiveType && kind == PrimitiveTypeKind.IDEAL_FLOAT

val ConeCangjieType.isIdealType: Boolean
    get() = this is ConePrimitiveType && kind.isIdeal

// ============================================================
// 标准库类型判断（基于 ClassId）
// 适用于 ConeClassLikeType/ConeStructType/ConeEnumType 等
// ============================================================

/** 提取类类型的 ClassId（内建原始类型返回 null） */
val ConeCangjieType.classId: ClassId?
    get() = when (this) {
        is ConeClassLikeType -> classId
        is ConeStructType -> classId
        is ConeEnumType -> classId
        is ConeTypeAliasType -> classId
        else -> null
    }

val ConeCangjieType.isString: Boolean
    get() = classId == StdlibClassIds.String

val ConeCangjieType.isArray: Boolean
    get() = classId == StdlibClassIds.Array

val ConeCangjieType.isOption: Boolean
    get() = classId == StdlibClassIds.Option
