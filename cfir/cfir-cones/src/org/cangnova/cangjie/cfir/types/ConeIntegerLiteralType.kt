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

    /**
     * 此理想类型可以解析为的所有具体原始类型（对齐 C++ 的 GetIdealTypesByKind）。
     */
    abstract val possibleTypes: Collection<ConePrimitiveType>

    /**
     * 类型推断无上下文时的默认解析目标。
     */
    abstract val defaultType: ConePrimitiveType

    /**
     * ideal literal 不携带类型实参。
     */
    final override val typeArguments: List<ConeTypeProjection> get() = emptyList()

    /**
     * ideal literal 不携带额外属性。
     */
    final override val attributes: ConeAttributes get() = ConeAttributes.Empty

    /**
     * 根据期望类型近似为具体的原始类型。
     *
     * 若 [expectedType] 是兼容的具体数值类型则采用，否则返回 [defaultType]。
     */
    abstract fun getApproximatedType(expectedType: ConeCangJieType? = null): ConePrimitiveType

    /**
     * ideal literal 类型的伴生命名空间。
     */
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

    /**
     * IdealInt 可近似到的所有具体整数 primitive 类型。
     */
    override val possibleTypes: Collection<ConePrimitiveType>
        get() = POSSIBLE_INT_TYPES

    /**
     * IdealInt 无上下文时默认解析为 Int64。
     */
    override val defaultType: ConePrimitiveType
        get() = ConePrimitiveType.INT64

    /**
     * 根据期望整数类型近似 IdealInt。
     */
    override fun getApproximatedType(expectedType: ConeCangJieType?): ConePrimitiveType {
        if (expectedType is ConePrimitiveType && expectedType.kind.isInteger && !expectedType.kind.isIdeal) {
            return expectedType
        }
        return defaultType
    }


    companion object {
        /**
         * 理想整数类型可解析为的所有具体整数类型（对齐 C++ GetIdealTypesByKind）。
         */
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
    /**
     * 整数字面量的常量值。
     */
    val value: Long,
) : ConeIdealIntLiteralType() {

    /**
     * 常量 IdealInt 按常量值判等。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeIdealIntConstantType) return false
        return value == other.value
    }

    /**
     * 常量 IdealInt 的结构哈希。
     */
    override fun hashCode(): Int = value.hashCode()

}

/**
 * 由整数运算产生的理想整数类型（常量值未知）。
 *
 * 用于编译期无法确定具体值的整数运算结果。
 */
class ConeIdealIntOperatorType : ConeIdealIntLiteralType() {

    /**
     * 运算产生的 IdealInt 不区分具体节点，按类型种类判等。
     */
    override fun equals(other: Any?): Boolean =
        this === other || other is ConeIdealIntOperatorType

    /**
     * 运算 IdealInt 的稳定哈希。
     */
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

    /**
     * IdealFloat 可近似到的所有具体浮点 primitive 类型。
     */
    override val possibleTypes: Collection<ConePrimitiveType>
        get() = POSSIBLE_FLOAT_TYPES

    /**
     * IdealFloat 无上下文时默认解析为 Float64。
     */
    override val defaultType: ConePrimitiveType
        get() = ConePrimitiveType.FLOAT64

    /**
     * 根据期望浮点类型近似 IdealFloat。
     */
    override fun getApproximatedType(expectedType: ConeCangJieType?): ConePrimitiveType {
        if (expectedType is ConePrimitiveType && expectedType.kind.isFloat && !expectedType.kind.isIdeal) {
            return expectedType
        }
        return defaultType
    }

    companion object {
        /**
         * 理想浮点类型可解析为的所有具体浮点类型。
         */
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
    /**
     * 浮点字面量的常量值。
     */
    val value: Double,
) : ConeIdealFloatLiteralType() {

    /**
     * 常量 IdealFloat 按 IEEE bit 表示判等，区分 `-0.0` 与 `0.0` 等边界。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeIdealFloatConstantType) return false
        return value.toBits() == other.value.toBits()
    }

    /**
     * 常量 IdealFloat 的结构哈希。
     */
    override fun hashCode(): Int = value.toBits().hashCode()

}

/**
 * 由浮点运算产生的理想浮点类型（常量值未知）。
 */
class ConeIdealFloatOperatorType : ConeIdealFloatLiteralType() {

    /**
     * 运算产生的 IdealFloat 不区分具体节点，按类型种类判等。
     */
    override fun equals(other: Any?): Boolean =
        this === other || other is ConeIdealFloatOperatorType

    /**
     * 运算 IdealFloat 的稳定哈希。
     */
    override fun hashCode(): Int = javaClass.hashCode()
}
