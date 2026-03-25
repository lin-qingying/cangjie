package org.cangnova.cangjie.cfir.types

/**
 * 理想字面量类型 — 对齐仓颉编译器的 IdealInt / IdealFloat。
 *
 * 仓颉中无后缀的整数字面量初始类型为 IdealInt，
 * 无后缀的浮点字面量初始类型为 IdealFloat。
 * 它们在类型推断完成后被近似（approximate）为具体的原始类型。
 *
 * 参考 Kotlin K2 的 ConeIntegerLiteralType，
 * 扩展为同时支持整数和浮点两种理想字面量类型。
 *
 * 注：[ConePrimitiveType] 的 [PrimitiveTypeKind.IDEAL_INT] / [PrimitiveTypeKind.IDEAL_FLOAT]
 * 用于简单的类型判断（如 [isIdealType]），本类层次则用于推断阶段的值追踪与类型近似。
 *
 * @see IdealTypeResolver
 */
sealed class ConeIdealLiteralType : ConeSimpleCangJieType(), ConeTypeConstructorMarker {

    /** 此理想类型可以解析为的所有具体原始类型（对齐 C++ 的 GetIdealTypesByKind） */
    abstract val possibleTypes: Collection<ConePrimitiveType>

    /** 类型推断无上下文时的默认解析目标 */
    abstract val defaultType: ConePrimitiveType

    final override val typeArguments: List<ConeTypeProjection> get() = emptyList()
    final override val attributes: ConeAttributes get() = ConeAttributes.Empty

    /**
     * 根据期望类型近似为具体的原始类型。
     *
     * 若 [expectedType] 是兼容的具体数值类型则采用，否则返回 [defaultType]。
     */
    abstract fun getApproximatedType(expectedType: ConeCangJieType? = null): ConePrimitiveType

    companion object
}

// ============================================================
// 理想整数字面量
// ============================================================

/**
 * 理想整数字面量类型。
 *
 * 可解析为：Int8, Int16, Int32, Int64, IntNative,
 *           UInt8, UInt16, UInt32, UInt64, UIntNative
 * 默认解析为：Int64
 *
 * 对齐 C++ 编译器 TypeKind::TYPE_IDEAL_INT。
 */
sealed class ConeIdealIntLiteralType : ConeIdealLiteralType() {

    override val possibleTypes: Collection<ConePrimitiveType>
        get() = POSSIBLE_INT_TYPES

    override val defaultType: ConePrimitiveType
        get() = ConePrimitiveType.INT64

    override fun getApproximatedType(expectedType: ConeCangJieType?): ConePrimitiveType {
        if (expectedType is ConePrimitiveType && expectedType.kind.isInteger && !expectedType.kind.isIdeal) {
            return expectedType
        }
        return defaultType
    }

    override fun toString(): String = "IdealInt"

    companion object {
        /** 理想整数类型可解析为的所有具体整数类型（对齐 C++ GetIdealTypesByKind） */
        val POSSIBLE_INT_TYPES: List<ConePrimitiveType> = listOf(
            ConePrimitiveType.INT8, ConePrimitiveType.INT16,
            ConePrimitiveType.INT32, ConePrimitiveType.INT64,
            ConePrimitiveType.INT_NATIVE,
            ConePrimitiveType.UINT8, ConePrimitiveType.UINT16,
            ConePrimitiveType.UINT32, ConePrimitiveType.UINT64,
            ConePrimitiveType.UINT_NATIVE,
        )
    }
}

/**
 * 具有确定常量值的理想整数字面量类型。
 *
 * 对应字面量表达式中的整数常量（如 `42`, `0xFF`）。
 */
class ConeIdealIntConstantType(
    val value: Long,
) : ConeIdealIntLiteralType() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeIdealIntConstantType) return false
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = "IdealInt($value)"
}

/**
 * 由整数运算产生的理想整数类型（常量值未知）。
 *
 * 用于编译期无法确定具体值的整数运算结果。
 */
class ConeIdealIntOperatorType : ConeIdealIntLiteralType() {

    override fun equals(other: Any?): Boolean =
        this === other || other is ConeIdealIntOperatorType

    override fun hashCode(): Int = javaClass.hashCode()
}

// ============================================================
// 理想浮点字面量
// ============================================================

/**
 * 理想浮点字面量类型。
 *
 * 可解析为：Float16, Float32, Float64
 * 默认解析为：Float64
 *
 * 对齐 C++ 编译器 TypeKind::TYPE_IDEAL_FLOAT。
 */
sealed class ConeIdealFloatLiteralType : ConeIdealLiteralType() {

    override val possibleTypes: Collection<ConePrimitiveType>
        get() = POSSIBLE_FLOAT_TYPES

    override val defaultType: ConePrimitiveType
        get() = ConePrimitiveType.FLOAT64

    override fun getApproximatedType(expectedType: ConeCangJieType?): ConePrimitiveType {
        if (expectedType is ConePrimitiveType && expectedType.kind.isFloat && !expectedType.kind.isIdeal) {
            return expectedType
        }
        return defaultType
    }

    override fun toString(): String = "IdealFloat"

    companion object {
        /** 理想浮点类型可解析为的所有具体浮点类型 */
        val POSSIBLE_FLOAT_TYPES: List<ConePrimitiveType> = listOf(
            ConePrimitiveType.FLOAT16,
            ConePrimitiveType.FLOAT32,
            ConePrimitiveType.FLOAT64,
        )
    }
}

/**
 * 具有确定常量值的理想浮点字面量类型。
 *
 * 对应字面量表达式中的浮点常量（如 `3.14`, `1e10`）。
 */
class ConeIdealFloatConstantType(
    val value: Double,
) : ConeIdealFloatLiteralType() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeIdealFloatConstantType) return false
        return value.toBits() == other.value.toBits()
    }

    override fun hashCode(): Int = value.toBits().hashCode()

    override fun toString(): String = "IdealFloat($value)"
}

/**
 * 由浮点运算产生的理想浮点类型（常量值未知）。
 */
class ConeIdealFloatOperatorType : ConeIdealFloatLiteralType() {

    override fun equals(other: Any?): Boolean =
        this === other || other is ConeIdealFloatOperatorType

    override fun hashCode(): Int = javaClass.hashCode()
}