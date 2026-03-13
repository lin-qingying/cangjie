package org.cangnova.cangjie.cfir.types

/**
 * 仓颉原始类型种类，对应 TypeKind.inc 中的基本类型。
 */
enum class PrimitiveTypeKind(val typeName: String) {
    UNIT("Unit"),
    BOOLEAN("Bool"),
    INT8("Int8"),
    INT16("Int16"),
    INT32("Int32"),
    INT64("Int64"),
    INT_NATIVE("IntNative"),
    /** 字面量整数的理想类型（编译期推断） */
    IDEAL_INT("IdealInt"),
    UINT8("UInt8"),
    UINT16("UInt16"),
    UINT32("UInt32"),
    UINT64("UInt64"),
    UINT_NATIVE("UIntNative"),
    FLOAT16("Float16"),
    FLOAT32("Float32"),
    FLOAT64("Float64"),
    /** 字面量浮点数的理想类型（编译期推断） */
    IDEAL_FLOAT("IdealFloat"),
    RUNE("Rune"),
    NOTHING("Nothing"),
    ;

    /** 是否为整数类型（含 IdealInt） */
    val isInteger: Boolean get() = this in INTEGER_SET

    /** 是否为浮点类型（含 IdealFloat） */
    val isFloat: Boolean get() = this in FLOAT_SET

    /** 是否为数值类型 */
    val isNumeric: Boolean get() = isInteger || isFloat

    /** 是否为理想类型（IdealInt 或 IdealFloat） */
    val isIdeal: Boolean get() = this == IDEAL_INT || this == IDEAL_FLOAT

    companion object {
        private val INTEGER_SET = setOf(
            INT8, INT16, INT32, INT64, INT_NATIVE,
            UINT8, UINT16, UINT32, UINT64, UINT_NATIVE,
            IDEAL_INT,
        )
        private val FLOAT_SET = setOf(FLOAT16, FLOAT32, FLOAT64, IDEAL_FLOAT)
    }
}

/**
 * 原始类型，对应仓颉编译器中的 PrimitiveTy / NothingTy。
 */
class ConePrimitiveType(
    val kind: PrimitiveTypeKind,
    override val attributes: ConeAttributes = ConeAttributes.EMPTY,
) : ConeRigidType() {

    override val isUnit: Boolean get() = kind == PrimitiveTypeKind.UNIT
    override val isNothing: Boolean get() = kind == PrimitiveTypeKind.NOTHING

    override fun equals(other: Any?): Boolean =
        other is ConePrimitiveType && kind == other.kind

    override fun hashCode(): Int = kind.hashCode()

    override fun toString(): String = kind.typeName

    companion object {
        val UNIT = ConePrimitiveType(PrimitiveTypeKind.UNIT)
        val BOOLEAN = ConePrimitiveType(PrimitiveTypeKind.BOOLEAN)
        val INT8 = ConePrimitiveType(PrimitiveTypeKind.INT8)
        val INT16 = ConePrimitiveType(PrimitiveTypeKind.INT16)
        val INT32 = ConePrimitiveType(PrimitiveTypeKind.INT32)
        val INT64 = ConePrimitiveType(PrimitiveTypeKind.INT64)
        val INT_NATIVE = ConePrimitiveType(PrimitiveTypeKind.INT_NATIVE)
        val IDEAL_INT = ConePrimitiveType(PrimitiveTypeKind.IDEAL_INT)
        val UINT8 = ConePrimitiveType(PrimitiveTypeKind.UINT8)
        val UINT16 = ConePrimitiveType(PrimitiveTypeKind.UINT16)
        val UINT32 = ConePrimitiveType(PrimitiveTypeKind.UINT32)
        val UINT64 = ConePrimitiveType(PrimitiveTypeKind.UINT64)
        val UINT_NATIVE = ConePrimitiveType(PrimitiveTypeKind.UINT_NATIVE)
        val FLOAT16 = ConePrimitiveType(PrimitiveTypeKind.FLOAT16)
        val FLOAT32 = ConePrimitiveType(PrimitiveTypeKind.FLOAT32)
        val FLOAT64 = ConePrimitiveType(PrimitiveTypeKind.FLOAT64)
        val IDEAL_FLOAT = ConePrimitiveType(PrimitiveTypeKind.IDEAL_FLOAT)
        val RUNE = ConePrimitiveType(PrimitiveTypeKind.RUNE)
        val NOTHING = ConePrimitiveType(PrimitiveTypeKind.NOTHING)
    }
}
