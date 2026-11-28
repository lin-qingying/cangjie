package org.cangnova.cangjie.resolve.calls.mpp

import org.cangnova.cangjie.descriptors.Visibility

/**
 * 已配对声明的后置兼容性问题。
 *
 * 问题类型携带自身所需的定位数据，并与候选匹配结果分离，不能改变既有 first-fit 配对。
 */
sealed interface CjmpCompatibilityIssue {
    data class GenericConstraint(val typeParameterIndex: Int) : CjmpCompatibilityIssue

    data object GenericParameterCount : CjmpCompatibilityIssue

    data object OpenAbstractMember : CjmpCompatibilityIssue

    data object Modifiers : CjmpCompatibilityIssue

    data object Supertypes : CjmpCompatibilityIssue

    data object EnumExhaustiveness : CjmpCompatibilityIssue

    data object ReturnType : CjmpCompatibilityIssue

    data object DeclarationType : CjmpCompatibilityIssue

    data object Mutability : CjmpCompatibilityIssue

    data class PropertyMutability(val specificIsMutable: Boolean) : CjmpCompatibilityIssue
}

/** 声明状态中参与 CJMP 兼容性比较的修饰符视图。 */
data class CjmpDeclarationModifiers(
    val visibility: Visibility,
    val isAbstract: Boolean,
    val isOpen: Boolean,
    val isSealed: Boolean,
    val isC: Boolean,
    val isStatic: Boolean,
    val isMut: Boolean,
    val isForeign: Boolean,
    val isUnsafe: Boolean,
)

/** 为已配对声明提供只读兼容性事实。 */
interface CjmpCompatibilityContext<
    D : CjmpDeclarationSymbolMarker,
    T : CjmpTypeParameterSymbolMarker,
    Type,
> : CjmpMatchingContext<D, T, Type> {
    fun isClassLike(declaration: D): Boolean

    fun isClass(declaration: D): Boolean

    fun isEnum(declaration: D): Boolean

    fun isNamedFunction(declaration: D): Boolean

    fun isPayloadEnumConstructor(declaration: D): Boolean

    fun isProperty(declaration: D): Boolean

    fun isVariable(declaration: D): Boolean

    fun isMutableProperty(declaration: D): Boolean

    fun modifiers(declaration: D): CjmpDeclarationModifiers

    /** 声明的有效直接超类型；前端按语言规则排除无法指认超类型的错误引用。 */
    fun superTypes(declaration: D): List<Type>

    fun isNonExhaustiveEnum(declaration: D): Boolean

    fun incompatibleGenericConstraintIndices(
        specific: D,
        common: D,
        typeParameterMapping: Map<T, T>,
    ): List<Int>

    fun propertyOrVariableType(declaration: D): Type?

    fun isMutableVariable(declaration: D): Boolean

    fun postMatchReturnTypeCompatibility(
        specific: D,
        common: D,
        typeParameterMapping: Map<T, T>,
    ): CjmpTypeCompatibility
}

/**
 * 匹配成功后的 common/specific 语义检查。
 *
 * 结果保留官方检查顺序。这里不报告诊断，也不重选或撤销已发布的配对；具体前端只把问题映射
 * 到其诊断工厂。修饰符和超类型差异按官方规则提前结束当前声明的后续检查。
 */
object AbstractCjmpChecker {
    fun <D, T, Type> checkMatched(
        specific: D,
        common: D,
        typeParameterMapping: Map<T, T>,
        context: CjmpCompatibilityContext<D, T, Type>,
    ): List<CjmpCompatibilityIssue>
        where D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker {
        val issues = mutableListOf<CjmpCompatibilityIssue>()

        if (context.isClassLike(specific) && context.isClassLike(common)) {
            val specificTypeParameters = context.typeParameters(specific)
            val commonTypeParameters = context.typeParameters(common)
            if (specificTypeParameters.isNotEmpty() && commonTypeParameters.isNotEmpty() &&
                specificTypeParameters.size != commonTypeParameters.size
            ) {
                issues += CjmpCompatibilityIssue.GenericParameterCount
            } else {
                context.incompatibleGenericConstraintIndices(specific, common, typeParameterMapping).forEach { index ->
                    issues += CjmpCompatibilityIssue.GenericConstraint(index)
                }
            }
            val modifierIssue = modifierIssue(specific, common, isNominal = true, context)
            if (modifierIssue != null) {
                issues += modifierIssue
                return issues
            }
            if (areCompatibleSupertypes(specific, common, typeParameterMapping, context) == CjmpTypeCompatibility.INCOMPATIBLE) {
                issues += CjmpCompatibilityIssue.Supertypes
                return issues
            }
            if (context.isEnum(specific) && context.isEnum(common) &&
                !context.isNonExhaustiveEnum(common) && context.isNonExhaustiveEnum(specific)
            ) {
                issues += CjmpCompatibilityIssue.EnumExhaustiveness
            }
            return issues
        }

        if (context.isNamedFunction(common) &&
            (context.isNamedFunction(specific) || context.isPayloadEnumConstructor(specific))
        ) {
            if (context.postMatchReturnTypeCompatibility(specific, common, typeParameterMapping) ==
                CjmpTypeCompatibility.INCOMPATIBLE
            ) {
                issues += CjmpCompatibilityIssue.ReturnType
            }
        }

        val specificType = context.propertyOrVariableType(specific)
        val commonType = context.propertyOrVariableType(common)
        if (specificType != null && commonType != null &&
            context.areTypesEquivalent(specificType, commonType, typeParameterMapping) == CjmpTypeCompatibility.INCOMPATIBLE
        ) {
            issues += CjmpCompatibilityIssue.DeclarationType
        }

        if (context.isVariable(specific) && context.isVariable(common) &&
            context.isMutableVariable(specific) != context.isMutableVariable(common)
        ) {
            issues += CjmpCompatibilityIssue.Mutability
        }

        modifierIssue(specific, common, isNominal = false, context)?.let {
            issues += it
            return issues
        }

        context.incompatibleGenericConstraintIndices(specific, common, typeParameterMapping).forEach { index ->
            issues += CjmpCompatibilityIssue.GenericConstraint(index)
        }

        return issues
    }

    /** 仓颉要求直接超类型集合相同；common 类型先经已发布的泛型映射比较，声明顺序不参与匹配。 */
    private fun <D, T, Type> areCompatibleSupertypes(
        specific: D,
        common: D,
        typeParameterMapping: Map<T, T>,
        context: CjmpCompatibilityContext<D, T, Type>,
    ): CjmpTypeCompatibility where D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker {
        val specificTypes = context.superTypes(specific)
        val commonTypes = context.superTypes(common)
        if (specificTypes.size != commonTypes.size) return CjmpTypeCompatibility.INCOMPATIBLE
        val comparisons = commonTypes.map { commonType ->
            specificTypes.map { specificType ->
                context.areTypesEquivalent(specificType, commonType, typeParameterMapping)
            }
        }

        // 错误类型只表示某条匹配边尚不可判定，不能遮住其他已经确定的类型差异。
        // 分别验证已确定和可能的完整一对一匹配，结果不依赖超类型声明顺序。
        fun hasCompleteMatching(allowUnresolved: Boolean): Boolean {
            val commonForSpecific = IntArray(specificTypes.size) { -1 }
            fun match(commonIndex: Int, visited: BooleanArray): Boolean {
                for (specificIndex in specificTypes.indices) {
                    val comparison = comparisons[commonIndex][specificIndex]
                    if (comparison != CjmpTypeCompatibility.COMPATIBLE &&
                        !(allowUnresolved && comparison == CjmpTypeCompatibility.UNRESOLVED)
                    ) continue
                    if (visited[specificIndex]) continue
                    visited[specificIndex] = true
                    val previousCommon = commonForSpecific[specificIndex]
                    if (previousCommon < 0 || match(previousCommon, visited)) {
                        commonForSpecific[specificIndex] = commonIndex
                        return true
                    }
                }
                return false
            }
            return commonTypes.indices.all { match(it, BooleanArray(specificTypes.size)) }
        }
        return when {
            hasCompleteMatching(allowUnresolved = false) -> CjmpTypeCompatibility.COMPATIBLE
            hasCompleteMatching(allowUnresolved = true) -> CjmpTypeCompatibility.UNRESOLVED
            else -> CjmpTypeCompatibility.INCOMPATIBLE
        }
    }

    private fun <D, T, Type> modifierIssue(
        specific: D,
        common: D,
        isNominal: Boolean,
        context: CjmpCompatibilityContext<D, T, Type>,
    ): CjmpCompatibilityIssue? where D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker {
        val specificModifiers = context.modifiers(specific)
        val commonModifiers = context.modifiers(common)
        val isFunction = context.isNamedFunction(common) || context.isPayloadEnumConstructor(common)
        val isFunctionOrProperty = isFunction || context.isProperty(common)
        if (!isNominal && specificModifiers.isStatic != commonModifiers.isStatic) {
            return CjmpCompatibilityIssue.Modifiers
        }

        if (!isNominal && context.isProperty(specific) && context.isProperty(common) &&
            context.isMutableProperty(specific) != context.isMutableProperty(common)
        ) {
            return CjmpCompatibilityIssue.PropertyMutability(context.isMutableProperty(specific))
        }

        if (!isNominal && !context.isVariable(specific) && !context.isProperty(specific) &&
            specificModifiers.isMut != commonModifiers.isMut
        ) {
            return CjmpCompatibilityIssue.Modifiers
        }

        if (specificModifiers.visibility != commonModifiers.visibility) {
            return CjmpCompatibilityIssue.Modifiers
        }

        if (!isNominal && specificModifiers.isForeign != commonModifiers.isForeign) {
            return CjmpCompatibilityIssue.Modifiers
        }

        if (!isNominal && specificModifiers.isUnsafe != commonModifiers.isUnsafe) {
            return CjmpCompatibilityIssue.Modifiers
        }

        val abstractOrOpenDiffers = specificModifiers.isAbstract != commonModifiers.isAbstract ||
                specificModifiers.isOpen != commonModifiers.isOpen
        val sealedAbstractAllowed = isNominal && context.isClass(common) &&
                specificModifiers.isSealed && commonModifiers.isAbstract

        if (abstractOrOpenDiffers) {
            when {
                isFunctionOrProperty && specificModifiers.isAbstract && commonModifiers.isOpen ->
                    return CjmpCompatibilityIssue.OpenAbstractMember
                isFunctionOrProperty && commonModifiers.isAbstract && specificModifiers.isOpen -> Unit
                commonModifiers.isAbstract && commonModifiers.isStatic && specificModifiers.isStatic -> Unit
                sealedAbstractAllowed -> Unit
                else -> return CjmpCompatibilityIssue.Modifiers
            }
        }

        if (isNominal && specificModifiers.isC != commonModifiers.isC) {
            return CjmpCompatibilityIssue.Modifiers
        }

        return if (isNominal && specificModifiers.isSealed != commonModifiers.isSealed && !sealedAbstractAllowed) {
            CjmpCompatibilityIssue.Modifiers
        } else null
    }
}
