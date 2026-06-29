/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.results

import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemMarker
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.*

/**
 * 重载候选特异性比较的扩展回调。
 *
 * 该接口用于补充类型系统无法直接用子类型关系判定的“同等或更特异”规则，例如语言
 * 特定转换、内建类型优先级或兼容性保留规则。
 */
interface SpecificityComparisonCallbacks {
    /**
     * 判断非子类型关系下的 [specific] 是否仍可视为不弱于 [general]。
     */
    fun isNonSubtypeEquallyOrMoreSpecific(specific: CangJieTypeMarker, general: CangJieTypeMarker): Boolean
}

/**
 * 重载能力检查使用的默认特异性回调。
 *
 * 默认实现不把任何非子类型关系额外视为“同等或更特异”，从而保持纯子类型判定语义。
 */
object OverloadabilitySpecificityCallbacks : SpecificityComparisonCallbacks {
    /**
     * 默认重载能力比较不接受非子类型的等价特异性。
     */
    override fun isNonSubtypeEquallyOrMoreSpecific(specific: CangJieTypeMarker, general: CangJieTypeMarker): Boolean =
        false
}

/**
 * 值参数类型及其转换前类型的配对模型。
 *
 * @property resultType 参与签名比较的最终参数类型。
 * @property originalTypeIfWasConverted 发生转换时的原始参数类型；未发生转换时为 `null`。
 */
class TypeWithConversion(
    /**
     * 参与签名比较的最终参数类型。
     */
    val resultType: CangJieTypeMarker?,

    /**
     * 发生转换时的原始参数类型；未发生转换时为 `null`。
     */
    val originalTypeIfWasConverted: CangJieTypeMarker? = null,
)

/**
 * 用于重载特异性比较的扁平化调用签名。
 *
 * 该模型把候选符号的接收者、上下文接收者、默认参数、可变参数和参数类型展开为比较所需
 * 的最小信息，避免比较阶段依赖完整声明结构。
 *
 * @param T 原始候选声明或符号的类型。
 */
class FlatSignature<out T>(
    /**
     * 该扁平签名对应的原始候选声明或符号。
     */
    val origin: T,

    /**
     * 候选签名声明的类型参数集合。
     */
    val typeParameters: Collection<TypeParameterMarker>,

    /**
     * 候选签名是否拥有扩展接收者。
     */
    val hasExtensionReceiver: Boolean,

    /**
     * 候选签名的上下文接收者数量。
     */
    val contextReceiverCount: Int,

    /**
     * 候选签名是否包含可变参数。
     */
    val hasVarargs: Boolean,

    /**
     * 候选签名中带默认值的参数数量。
     */
    val numDefaults: Int,

    /**
     * 候选声明是否来自 expect 声明。
     */
    val isExpect: Boolean,

    /**
     * 候选成员是否为合成成员。
     */
    val isSyntheticMember: Boolean,

    /**
     * 候选签名的值参数类型列表，顺序与调用参数比较顺序一致。
     */
    val valueParameterTypes: List<TypeWithConversion?>
) {
    /**
     * 当前签名是否声明了类型参数。
     */
    val isGeneric = typeParameters.isNotEmpty()

    constructor(
        origin: T,
        typeParameters: Collection<TypeParameterMarker>,
        valueParameterTypes: List<CangJieTypeMarker?>,
        hasExtensionReceiver: Boolean,
        contextReceiverCount: Int,
        hasVarargs: Boolean,
        numDefaults: Int,
        isExpect: Boolean,
        isSyntheticMember: Boolean,
    ) : this(
        origin, typeParameters, hasExtensionReceiver, contextReceiverCount, hasVarargs, numDefaults, isExpect,
        isSyntheticMember, valueParameterTypes.map(::TypeWithConversion)
    )

    companion object
}


/**
 * 签名特异性比较过程中使用的轻量约束系统接口。
 *
 * 比较逻辑只需要注册泛型参数、添加子类型约束并读取矛盾状态，因此该接口避免暴露完整
 * 推断系统实现。
 */
interface SimpleConstraintSystem {
    /**
     * 注册 [typeParameters] 并返回可用于替换泛型形参的类型替换器。
     */
    fun registerTypeVariables(typeParameters: Collection<TypeParameterMarker>): TypeSubstitutorMarker

    /**
     * 向临时约束系统添加 [subType] <: [superType] 约束。
     */
    fun addSubtypeConstraint(subType: CangJieTypeMarker, superType: CangJieTypeMarker)

    /**
     * 判断当前临时约束系统是否已经产生矛盾。
     */
    fun hasContradiction(): Boolean


    /**
     * 类型系统上下文，供子类型检查和类型替换读取类型结构。
     */
    val context: TypeSystemInferenceExtensionContext

    /**
     * 当前约束系统的只读标记对象。
     */
    val constraintSystemMarker: ConstraintSystemMarker
}

/**
 * 单次扁平签名比较的状态对象。
 *
 * 状态对象持有临时约束系统、被比较签名的泛型参数替换器以及特异性比较策略，用于在
 * 多个参数类型之间复用同一套约束状态。
 */
class FlatSignatureComparisonState(
    /**
     * 签名比较使用的临时约束系统。
     */
    private val cs: SimpleConstraintSystem,

    /**
     * 一般候选签名声明的类型参数。
     */
    private val typeParameters: Collection<TypeParameterMarker>,

    /**
     * 一般候选签名类型参数对应的替换器。
     */
    private val typeSubstitutor: TypeSubstitutorMarker,

    /**
     * 非子类型关系下的额外特异性比较回调。
     */
    private val callbacks: SpecificityComparisonCallbacks,

    /**
     * 类型层面的特异性快速判定器。
     */
    private val specificityComparator: TypeSpecificityComparator,
) {
    /**
     * 判断 [specificType] 是否严格弱于 [generalType]。
     *
     * 当普通子类型关系无法直接证明时，该方法会按需要向临时约束系统注入约束，以确认
     * 泛型一般候选是否仍可能被更特异候选覆盖。
     */
    fun isLessSpecific(specificType: CangJieTypeMarker, generalType: CangJieTypeMarker): Boolean {
        if (specificityComparator.isDefinitelyLessSpecific(specificType, generalType)) {
            return true
        } else if (typeParameters.isEmpty() || with(cs.context) {
                val parameterConstructors = typeParameters.map { parameter -> parameter.getTypeConstructor() }.toSet()
                !generalType.contains { it.typeConstructor() in parameterConstructors }
            }) {
            if (!AbstractTypeChecker.isSubtypeOf(cs.context, specificType, generalType)) {
                if (!callbacks.isNonSubtypeEquallyOrMoreSpecific(specificType, generalType)) {
                    return true
                }
            }
        } else {
            val substitutedGeneralType = with(cs.context) { typeSubstitutor.safeSubstitute(generalType) }

            /**
             * Example:
             * fun <X> Array<out X>.sort(): Unit {}
             * fun <Y: Comparable<Y>> Array<out Y>.sort(): Unit {}
             * Here, when we try solve this CS(Y is variables) then Array<out X> <: Array<out Y> and this system impossible to solve,
             * so we capture types from receiver and value parameters.
             */
            val specificCapturedType = AbstractTypeChecker.prepareType(cs.context, specificType)

            cs.addSubtypeConstraint(specificCapturedType, substitutedGeneralType)
            if (cs.hasContradiction()) {
                return true
            }
        }

        return false
    }
}

/**
 * 比较两个签名的值参数类型是否逐项满足“同等或更特异”。
 *
 * @param specific 被检查的更特异候选。
 * @param general 作为基准的一般候选。
 * @param typeKindSelector 从参数转换模型中选择本轮比较使用的类型。
 */
private fun <T> FlatSignatureComparisonState.isValueParameterTypeEquallyOrMoreSpecific(
    specific: FlatSignature<T>,
    general: FlatSignature<T>,
    typeKindSelector: (TypeWithConversion?) -> CangJieTypeMarker?,
): Boolean {
    val specificContextReceiverCount = specific.contextReceiverCount
    val generalContextReceiverCount = general.contextReceiverCount

    var specificValueParameterTypes = specific.valueParameterTypes
    var generalValueParameterTypes = general.valueParameterTypes

    if (specificContextReceiverCount != generalContextReceiverCount) {
        specificValueParameterTypes = specificValueParameterTypes.drop(specificContextReceiverCount)
        generalValueParameterTypes = generalValueParameterTypes.drop(generalContextReceiverCount)
    }

    for (index in specificValueParameterTypes.indices) {
        val specificType = typeKindSelector(specificValueParameterTypes[index]) ?: continue
        val generalType = typeKindSelector(generalValueParameterTypes[index]) ?: continue

        if (isLessSpecific(specificType, generalType)) return false
    }

    return true
}

/**
 * 在 [specific] 至少与 [general] 同等特异时创建签名比较状态。
 *
 * 返回 `null` 表示接收者、上下文接收者或值参数类型比较已证明 [specific] 不能作为更
 * 特异候选。
 */
fun <T> SimpleConstraintSystem.signatureComparisonStateIfEquallyOrMoreSpecific(
    specific: FlatSignature<T>,
    general: FlatSignature<T>,
    callbacks: SpecificityComparisonCallbacks,
    specificityComparator: TypeSpecificityComparator,
    useOriginalSamTypes: Boolean = false,
): FlatSignatureComparisonState? {
    if (specific.hasExtensionReceiver != general.hasExtensionReceiver) return null
    if (specific.contextReceiverCount > general.contextReceiverCount) return null
    if (specific.valueParameterTypes.size - specific.contextReceiverCount != general.valueParameterTypes.size - general.contextReceiverCount)
        return null

    val typeSubstitutor = registerTypeVariables(general.typeParameters)
    val state = FlatSignatureComparisonState(this, general.typeParameters, typeSubstitutor, callbacks, specificityComparator)

    if (!state.isValueParameterTypeEquallyOrMoreSpecific(specific, general) { it?.resultType }) {
        return null
    }

    if (useOriginalSamTypes && !state.isValueParameterTypeEquallyOrMoreSpecific(specific, general) { it?.originalTypeIfWasConverted }) {
        return null
    }

    return state
}

/**
 * 判断 [specific] 签名是否与 [general] 同等或更特异。
 */
fun <T> SimpleConstraintSystem.isSignatureEquallyOrMoreSpecific(
    specific: FlatSignature<T>,
    general: FlatSignature<T>,
    callbacks: SpecificityComparisonCallbacks,
    specificityComparator: TypeSpecificityComparator,
    useOriginalSamTypes: Boolean = false
): Boolean {
    return signatureComparisonStateIfEquallyOrMoreSpecific(specific, general, callbacks, specificityComparator, useOriginalSamTypes) != null
}
