package org.cangnova.cangjie.cfir.types

/**
 * 理想类型解析器。
 *
 * 对齐 C++ 编译器的 ReplaceIdealTy() —
 * 将编译期 IdealInt/IdealFloat 解析为具体的原始类型。
 */
object IdealTypeResolver {

    /**
     * 将 IdealInt 解析为目标整数类型。
     * 若目标类型为整数则采用，否则默认 Int64。
     */
    fun resolveIdealInt(targetType: ConeCangJieType? = null): ConePrimitiveType {
        if (targetType is ConePrimitiveType && targetType.kind.isInteger && !targetType.kind.isIdeal) {
            return targetType
        }
        return ConePrimitiveType.INT64
    }

    /**
     * 将 IdealFloat 解析为目标浮点类型。
     * 若目标类型为浮点则采用，否则默认 Float64。
     */
    fun resolveIdealFloat(targetType: ConeCangJieType? = null): ConePrimitiveType {
        if (targetType is ConePrimitiveType && targetType.kind.isFloat && !targetType.kind.isIdeal) {
            return targetType
        }
        return ConePrimitiveType.FLOAT64
    }

    /**
     * 如果 [type] 是 IdealType 则解析为具体类型，否则原样返回。
     */
    fun resolveIfIdeal(type: ConeCangJieType, targetType: ConeCangJieType? = null): ConeCangJieType {
        if (type !is ConePrimitiveType) return type
        return when (type.kind) {
            PrimitiveTypeKind.IDEAL_INT -> resolveIdealInt(targetType)
            PrimitiveTypeKind.IDEAL_FLOAT -> resolveIdealFloat(targetType)
            else -> type
        }
    }
}
