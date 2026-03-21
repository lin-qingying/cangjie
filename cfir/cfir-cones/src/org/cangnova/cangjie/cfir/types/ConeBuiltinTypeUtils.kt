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
    get() = this is ConePrimitiveType && kind == PrimitiveTypeKind.IDEAL_INT

val ConeCangJieType.isIdealFloat: Boolean
    get() = this is ConePrimitiveType && kind == PrimitiveTypeKind.IDEAL_FLOAT

val ConeCangJieType.isIdealType: Boolean
    get() = this is ConePrimitiveType && kind.isIdeal

// ============================================================
// 标准库类型判断（基于 ClassId）
// 适用于 ConeClassLikeType/ConeStructType/ConeEnumType 等
// ============================================================

/** 提取类类型的 ClassId（内建原始类型返回 null） */
val ConeCangJieType.classId: ClassId?
    get() = when (this) {
        is ConeClassLikeType -> classId
        is ConeStructType -> classId
        is ConeEnumType -> classId
        is ConeTypeAliasType -> classId
        else -> null
    }

val ConeCangJieType.isString: Boolean
    get() = classId == StdlibClassIds.String

val ConeCangJieType.isArray: Boolean
    get() = classId == StdlibClassIds.Array

val ConeCangJieType.isOption: Boolean
    get() = classId == StdlibClassIds.Option
