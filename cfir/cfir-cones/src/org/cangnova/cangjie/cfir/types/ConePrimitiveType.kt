package org.cangnova.cangjie.cfir.types

/**
 * 仓颉原始类型种类，对应 TypeKind.inc 中的基本类型。
 *
 * @property typeName 类型在源码和诊断中的标准名称。
 */
enum class PrimitiveTypeKind(val typeName: String) {
    /**
     * `Unit` 类型。
     */
    UNIT("Unit"),

    /**
     * `Bool` 类型。
     */
    BOOLEAN("Bool"),

    /**
     * 8 位有符号整数。
     */
    INT8("Int8"),

    /**
     * 16 位有符号整数。
     */
    INT16("Int16"),

    /**
     * 32 位有符号整数。
     */
    INT32("Int32"),

    /**
     * 64 位有符号整数。
     */
    INT64("Int64"),

    /**
     * 原生字长有符号整数。
     */
    INT_NATIVE("IntNative"),

    /**
     * 字面量整数的理想类型（编译期推断）。
     */
    IDEAL_INT("IdealInt"),

    /**
     * 8 位无符号整数。
     */
    UINT8("UInt8"),

    /**
     * 16 位无符号整数。
     */
    UINT16("UInt16"),

    /**
     * 32 位无符号整数。
     */
    UINT32("UInt32"),

    /**
     * 64 位无符号整数。
     */
    UINT64("UInt64"),

    /**
     * 原生字长无符号整数。
     */
    UINT_NATIVE("UIntNative"),

    /**
     * 16 位浮点数。
     */
    FLOAT16("Float16"),

    /**
     * 32 位浮点数。
     */
    FLOAT32("Float32"),

    /**
     * 64 位浮点数。
     */
    FLOAT64("Float64"),

    /**
     * 字面量浮点数的理想类型（编译期推断）。
     */
    IDEAL_FLOAT("IdealFloat"),

    /**
     * Unicode 标量值字符类型。
     */
    RUNE("Rune"),

    /**
     * `Nothing` 底类型。
     */
    NOTHING("Nothing"),
    ;

    /**
     * 是否为整数类型（含 IdealInt）。
     */
    val isInteger: Boolean get() = this in INTEGER_SET

    /**
     * 是否为浮点类型（含 IdealFloat）。
     */
    val isFloat: Boolean get() = this in FLOAT_SET

    /**
     * 是否为数值类型。
     */
    val isNumeric: Boolean get() = isInteger || isFloat

    /**
     * 是否为理想类型（IdealInt 或 IdealFloat）。
     */
    val isIdeal: Boolean get() = this == IDEAL_INT || this == IDEAL_FLOAT

    /**
     * primitive 分类缓存。
     */
    companion object {
        /**
         * 所有整数 primitive 种类。
         */
        private val INTEGER_SET = setOf(
            INT8, INT16, INT32, INT64, INT_NATIVE,
            UINT8, UINT16, UINT32, UINT64, UINT_NATIVE,
            IDEAL_INT,
        )

        /**
         * 所有浮点 primitive 种类。
         */
        private val FLOAT_SET = setOf(FLOAT16, FLOAT32, FLOAT64, IDEAL_FLOAT)
    }
}

/**
 * 原始类型，对应仓颉编译器中的 PrimitiveTy / NothingTy。
 *
 * @property kind 原始类型种类。
 * @property attributes 类型附带的属性。
 */
class ConePrimitiveType(
    /** 原始类型种类。 */
    val kind: PrimitiveTypeKind,
    /** 类型附带的属性。 */
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeRigidType(), ConeTypeConstructorMarker {

    /**
     * 当前 primitive 是否是 `Unit`。
     */
    override val isUnit: Boolean get() = kind == PrimitiveTypeKind.UNIT

    /**
     * 当前 primitive 是否是 `Nothing`。
     */
    override val isNothing: Boolean get() = kind == PrimitiveTypeKind.NOTHING

    /**
     * primitive 类型按 [kind] 判等。
     */
    override fun equals(other: Any?): Boolean =
        other is ConePrimitiveType && kind == other.kind

    /**
     * primitive 类型哈希直接使用 [kind]。
     */
    override fun hashCode(): Int = kind.hashCode()


    /**
     * 常用 primitive 类型单例。
     */
    companion object {
        /** `Unit` primitive。 */
        val UNIT = ConePrimitiveType(PrimitiveTypeKind.UNIT)
        /** `Bool` primitive。 */
        val BOOLEAN = ConePrimitiveType(PrimitiveTypeKind.BOOLEAN)
        /** `Int8` primitive。 */
        val INT8 = ConePrimitiveType(PrimitiveTypeKind.INT8)
        /** `Int16` primitive。 */
        val INT16 = ConePrimitiveType(PrimitiveTypeKind.INT16)
        /** `Int32` primitive。 */
        val INT32 = ConePrimitiveType(PrimitiveTypeKind.INT32)
        /** `Int64` primitive。 */
        val INT64 = ConePrimitiveType(PrimitiveTypeKind.INT64)
        /** `IntNative` primitive。 */
        val INT_NATIVE = ConePrimitiveType(PrimitiveTypeKind.INT_NATIVE)
        /** `IdealInt` primitive marker。 */
        val IDEAL_INT = ConePrimitiveType(PrimitiveTypeKind.IDEAL_INT)
        /** `UInt8` primitive。 */
        val UINT8 = ConePrimitiveType(PrimitiveTypeKind.UINT8)
        /** `UInt16` primitive。 */
        val UINT16 = ConePrimitiveType(PrimitiveTypeKind.UINT16)
        /** `UInt32` primitive。 */
        val UINT32 = ConePrimitiveType(PrimitiveTypeKind.UINT32)
        /** `UInt64` primitive。 */
        val UINT64 = ConePrimitiveType(PrimitiveTypeKind.UINT64)
        /** `UIntNative` primitive。 */
        val UINT_NATIVE = ConePrimitiveType(PrimitiveTypeKind.UINT_NATIVE)
        /** `Float16` primitive。 */
        val FLOAT16 = ConePrimitiveType(PrimitiveTypeKind.FLOAT16)
        /** `Float32` primitive。 */
        val FLOAT32 = ConePrimitiveType(PrimitiveTypeKind.FLOAT32)
        /** `Float64` primitive。 */
        val FLOAT64 = ConePrimitiveType(PrimitiveTypeKind.FLOAT64)
        /** `IdealFloat` primitive marker。 */
        val IDEAL_FLOAT = ConePrimitiveType(PrimitiveTypeKind.IDEAL_FLOAT)
        /** `Rune` primitive。 */
        val RUNE = ConePrimitiveType(PrimitiveTypeKind.RUNE)
        /** `Nothing` primitive。 */
        val NOTHING = ConePrimitiveType(PrimitiveTypeKind.NOTHING)
    }
}
