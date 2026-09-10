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
     * 结合可用目标近似根 ideal 类型，保留仍参与调用推断的复合类型分量。
     *
     * 同时处理 [ConePrimitiveType]（IDEAL_INT/IDEAL_FLOAT）和 [ConeIdealLiteralType] 两种表示。
     * 完成独立综合后的递归默认化由 [replaceIdealTypes] 负责，不能在实参获得目标前执行。
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

    /**
     * 官方 ReplaceIdealTy 的综合完成边界：递归默认化允许的复合类型分量。
     *
     * 声明初始化器、match selector 等不再等待外部目标的位置使用此入口；
     * 调用实参仍由根 ideal 近似和正常的目标类型完成过程处理。
     */
    fun replaceIdealTypes(type: ConeCangJieType): ConeCangJieType = when (type) {
        is ConeIdealLiteralType, is ConePrimitiveType -> resolveIfIdeal(type)
        is ConeTupleType -> type.elementTypes.defaultedTypesOrNull()
            ?.let { ConeTupleType(it, type.attributes) } ?: type
        is ConeClassLikeType -> type.typeArguments.map { it.type }.defaultedTypesOrNull()
            ?.let { ConeClassLikeType(type.lookupTag, it, type.attributes, type.isInterface, type.isThisType) } ?: type
        is ConeEnumType -> type.typeArguments.map { it.type }.defaultedTypesOrNull()
            ?.let { ConeEnumType(type.lookupTag, it, type.attributes, type.isRefEnum) } ?: type
        is ConeVArrayType -> replaceIdealTypes(type.elementType).let { elementType ->
            if (elementType === type.elementType) type else ConeVArrayType(elementType, type.size, type.attributes)
        }
        is ConePointerType -> replaceIdealTypes(type.pointeeType).let { pointeeType ->
            if (pointeeType === type.pointeeType) type else ConePointerType(pointeeType, type.attributes)
        }
        else -> type
    }

    /** 仅在分量发生变化时重建不可变类型；function/struct 不属于官方 ReplaceIdealTy 的递归集合。 */
    private fun List<ConeCangJieType>.defaultedTypesOrNull(): List<ConeCangJieType>? {
        var changed = false
        val result = map { original ->
            replaceIdealTypes(original).also { if (it !== original) changed = true }
        }
        return result.takeIf { changed }
    }
}
