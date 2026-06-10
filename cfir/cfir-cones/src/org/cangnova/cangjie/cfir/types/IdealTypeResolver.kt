package org.cangnova.cangjie.cfir.types

/**
 * 理想类型解析器。
 *
 * 对齐 C++ 编译器的 ReplaceIdealTy() —
 * 将编译期 IdealInt/IdealFloat 解析为具体的原始类型。
 *
 * 同时支持 [ConePrimitiveType]（简单判断）和 [ConeIdealLiteralType]（推断阶段）两种表示。
 */
object IdealTypeResolver {

    /**
     * 将 IdealInt 解析为目标整数类型。
     * 若目标类型为整数或 `Option<整数>` 则采用，否则默认 Int64。
     */
    fun resolveIdealInt(targetType: ConeCangJieType? = null): ConeCangJieType {
        targetType?.optionElementType?.let { optionElementType ->
            if (optionElementType is ConePrimitiveType && optionElementType.kind.isInteger && !optionElementType.kind.isIdeal) {
                return targetType
            }
        }
        if (targetType is ConePrimitiveType && targetType.kind.isInteger && !targetType.kind.isIdeal) {
            return targetType
        }
        return ConePrimitiveType.INT64
    }

    /**
     * 将 IdealFloat 解析为目标浮点类型。
     * 若目标类型为浮点或 `Option<浮点>` 则采用，否则默认 Float64。
     */
    fun resolveIdealFloat(targetType: ConeCangJieType? = null): ConeCangJieType {
        targetType?.optionElementType?.let { optionElementType ->
            if (optionElementType is ConePrimitiveType && optionElementType.kind.isFloat && !optionElementType.kind.isIdeal) {
                return targetType
            }
        }
        if (targetType is ConePrimitiveType && targetType.kind.isFloat && !targetType.kind.isIdeal) {
            return targetType
        }
        return ConePrimitiveType.FLOAT64
    }

    /**
     * 如果 [type] 是理想类型则解析为具体类型，否则原样返回。
     *
     * 同时处理 [ConePrimitiveType]（IDEAL_INT/IDEAL_FLOAT）和 [ConeIdealLiteralType] 两种表示。
     */
    fun resolveIfIdeal(type: ConeCangJieType, targetType: ConeCangJieType? = null): ConeCangJieType {
        return when (type) {
            is ConeIdealLiteralType -> type.getApproximatedType(targetType)
            is ConePrimitiveType -> when (type.kind) {
                PrimitiveTypeKind.IDEAL_INT -> resolveIdealInt(targetType)
                PrimitiveTypeKind.IDEAL_FLOAT -> resolveIdealFloat(targetType)
                else -> type
            }
            else -> type
        }
    }
}
