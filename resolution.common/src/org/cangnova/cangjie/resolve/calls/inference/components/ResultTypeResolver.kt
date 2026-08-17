/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.resolve.calls.CommonSuperTypeCalculator
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import org.cangnova.cangjie.resolve.calls.inference.components.TypeVariableDirectionCalculator.ResolveDirection
import org.cangnova.cangjie.resolve.calls.inference.extractTypeForGivenRecursiveTypeParameter
import org.cangnova.cangjie.resolve.calls.inference.hasRecursiveTypeParametersWithGivenSelfType
import org.cangnova.cangjie.resolve.calls.inference.isEqualityConstraintCompatible
import org.cangnova.cangjie.resolve.calls.inference.model.*
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.*
import org.cangnova.cangjie.types.AbstractTypeApproximator
import org.cangnova.cangjie.types.TypeApproximatorConfiguration

/**
 * 类型变量固定时的结果类型解析器。
 *
 * 该组件根据类型变量收集到的上下界、等价约束、递归 self type 约束以及整型字面量约束，
 * 选择一个可作为固定结果的类型，并在必要时调用类型近似器把内部类型稳定化。
 */
class ResultTypeResolver(
    /**
     * 用于把内部类型近似为可固定结果类型的近似器。
     */
    val typeApproximator: AbstractTypeApproximator,

    /**
     * 判断约束结果类型是否足够平凡、可直接作为固定结果的 oracle。
     */
    val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,

    /**
     * 当前语言版本设置，供兼容性分支和特性开关读取。
     */
    private val languageVersionSettings: LanguageVersionSettings,
) {
    /**
     * 结果类型解析所需的约束系统上下文。
     *
     * 上下文同时提供类型系统操作、约束系统构建能力和未固定变量集合，使解析器可以在
     * 不持有具体实现的情况下完成替换、proper type 判断和约束兼容性探测。
     */
    interface Context : TypeSystemInferenceExtensionContext, ConstraintSystemBuilder {
        /**
         * 当前仍未固定的类型变量及其约束。
         */
        val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>

        /**
         * 外层约束系统变量在当前系统中的前缀数量。
         */
        val outerSystemVariablesPrefixSize: Int

        /**
         * 构建把未固定变量替换为 stub type 的替换器。
         */
        fun buildNotFixedVariablesToStubTypesSubstitutor(): TypeSubstitutorMarker
    }

    /**
     * 根据递归 self type 上界约束为类型变量构建默认类型。
     */
    context(c: Context)
    private fun TypeVariableMarker.getDefaultTypeForSelfType(constraints: List<Constraint>): CangJieTypeMarker? {
        val typeVariableConstructor = freshTypeConstructor()
        val typeParameter = typeVariableConstructor.typeParameter ?: return null

        val typesForRecursiveTypeParameters = constraints.mapNotNull { constraint ->
            if (constraint.position.from !is DeclaredUpperBoundConstraintPosition<*>) return@mapNotNull null
            constraint.type.extractTypeForGivenRecursiveTypeParameter(typeParameter)
        }.takeIf { it.isNotEmpty() } ?: return null

        return c.createPlaceholderTypeForSelfType(typeVariableConstructor, typesForRecursiveTypeParameters)
    }

    /**
     * 在没有可用 proper 约束时，根据固定方向选择类型变量默认结果类型。
     */
    context(c: Context)
    private fun TypeVariableMarker.getDefaultType(direction: ResolveDirection, constraints: List<Constraint>): CangJieTypeMarker {
        getDefaultTypeForSelfType(constraints)?.let { return it }

        return if (direction == ResolveDirection.TO_SUBTYPE) c.nothingType() else c.anyType()
    }

    /**
     * 解析 [variableWithConstraints] 在指定 [direction] 下的最终固定类型。
     */
    context(c: Context)
    fun findResultType(variableWithConstraints: VariableWithConstraints, direction: ResolveDirection): CangJieTypeMarker {
        findResultTypeOrNull(variableWithConstraints, direction)?.let { return it }

        // no proper constraints
        return variableWithConstraints.typeVariable.getDefaultType(direction, variableWithConstraints.constraints)
    }

    /**
     * 将当前类型向父类型近似；若类型是整型字面量类型，则使用 [superTypeCandidate] 作为期望类型。
     */
    context(c: Context)
    private fun CangJieTypeMarker.approximateToSuperTypeOrSelf(superTypeCandidate: CangJieTypeMarker?): CangJieTypeMarker {
        // In case we have an ILT as the subtype, we approximate it using the upper type as the expected type.
        // This is more precise than always approximating it to Int or UInt.
        // Note, we shouldn't have nested ILTs because they can only appear as a constraint on a type variable
        // that we would have fixed earlier.
        if (typeConstructor().isIntegerLiteralTypeConstructor()) {
            return typeApproximator.approximateToSuperType(
                this,
                TypeApproximatorConfiguration.TopLevelIntegerLiteralTypeApproximationWithExpectedType(superTypeCandidate)
            ) ?: this
        }

        return typeApproximator.approximateToSuperType(this, TypeApproximatorConfiguration.InternalTypesApproximation) ?: this
    }

    /**
     * 将当前类型向子类型近似，近似失败时保持原类型。
     */
    private fun CangJieTypeMarker.approximateToSubTypeOrSelf(): CangJieTypeMarker {
        return typeApproximator.approximateToSubType(this, TypeApproximatorConfiguration.InternalTypesApproximation) ?: this
    }

    /**
     * 尝试解析 [variableWithConstraints] 的结果类型；缺少可用约束时返回 `null`。
     */
    context(c: Context)
    fun findResultTypeOrNull(
        variableWithConstraints: VariableWithConstraints,
        direction: ResolveDirection,
    ): CangJieTypeMarker? {
val resultTypeFromEqualConstraint = findResultIfThereIsEqualsConstraint(variableWithConstraints, isStrictMode = false)
        if (resultTypeFromEqualConstraint?.isAppropriateResultTypeFromEqualityConstraints() == true) return resultTypeFromEqualConstraint

        val subType = variableWithConstraints.findSubType()
        val subTypeIsIntersection = subType?.typeConstructor()?.isIntersection() == true
        val allowIntersectionResult =
            languageVersionSettings.supportsFeature(LanguageFeature.AllowIntersectionTypesInInference)
        if (subTypeIsIntersection && !allowIntersectionResult) {
            // 多个互斥下界约束收敛出的交集候选（如 choose(1, true) 得到 Hashable & ToString）：
            // 公共父类型计算（filterStrictSupertypes）保证交集成员互不可比，因此不存在能同时
            // 满足全部下界约束的单一具体类型；对齐官方 cjc 的 sema_unable_to_infer_generic_func
            // 直接报告推断失败，由固定阶段生成用户可见诊断。
            // 特性开关开启时跳过该处理，保持 Kotlin K2 兼容的推断行为。
            return c.createErrorType(SOLVER_FAILURE_MARKER, null)
        }
        val superType = variableWithConstraints.findSuperType()

        val (preparedSubType, preparedSuperType) = variableWithConstraints.prepareSubAndSuperTypes(subType, superType)

        val resultTypeFromDirection = if (direction == ResolveDirection.TO_SUBTYPE || direction == ResolveDirection.UNKNOWN) {
            variableWithConstraints.resultType(preparedSubType, preparedSuperType)
        } else {
            variableWithConstraints.resultType(preparedSuperType, preparedSubType)
        }

        // In the general case, we can have here two types, one from EQUAL constraint which must be ILT-based,
        // and the second one from UPPER/LOWER constraints (subType/superType based)
        // The logic of choice here is:
        // - if one type is null, we return another one
        // - we return type from UPPER/LOWER constraints if it's more precise (in fact, only Int/Short/Byte/Long is allowed here)
        // - otherwise we return ILT-based type
        val resultType = when {
            resultTypeFromEqualConstraint == null -> resultTypeFromDirection
            resultTypeFromDirection == null -> resultTypeFromEqualConstraint
            !resultTypeFromDirection.typeConstructor().isNothingConstructor() &&
                    AbstractTypeChecker.isSubtypeOf(c, resultTypeFromDirection, resultTypeFromEqualConstraint) -> resultTypeFromDirection
            else -> resultTypeFromEqualConstraint
        }
        if (resultType != null) return resultType

        // 存在 proper 约束却仍无法确定结果类型（约束矛盾或互斥候选），且不是被特性
        // 开关允许的交集场景时，报告推断失败，由固定阶段生成 UNABLE_TO_INFER_GENERIC_FUNC。
        if (variableWithConstraints.hasProperConstraints() && !(subTypeIsIntersection && allowIntersectionResult)) {
            return c.createErrorType(SOLVER_FAILURE_MARKER, null)
        }
        return null
    }

    /**
     * 判断变量是否含有可用于固定的 proper 约束。
     */
    context(c: Context)
    private fun VariableWithConstraints.hasProperConstraints(): Boolean =
        constraints.any { it.isProperConstraint() }

    /**
     * 判断等价约束得到的结果类型是否已经不含整型字面量常量构造器。
     */
    context(c: Context)
    private fun CangJieTypeMarker.isAppropriateResultTypeFromEqualityConstraints(): Boolean {
        return !contains { type ->
            type.typeConstructor().isIntegerLiteralConstantTypeConstructor()
        }
    }

    /**
     * The general approach to approximation of resulting types (in K2) is to
     * - always approximate ILTs
     * - always approximate captured types unless this leads to a contradiction.
     * A contradiction can appear if we have some captured type C = CapturedType(*) in the subtype and in the supertype.
     *
     * Example: A<C> <: T <: A<C>
     *
     * If we were to approximate the result type, we would end up with a contradiction
     * A<*> </: A<C>
     *
     * In comparison, types from equality constraints are never approximated because it would always lead to a contradiction.
     * We evaluated a never-approximate approach but found it to be infeasible as it introduces many new errors
     * (type mismatches, REIFIED_TYPE_FORBIDDEN_SUBSTITUTION, TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR, etc.).
     */
    context(c: Context)
    private fun VariableWithConstraints.prepareSubAndSuperTypes(
        subType: CangJieTypeMarker?,
        superType: CangJieTypeMarker?,
    ): Pair<CangJieTypeMarker?, CangJieTypeMarker?> {
        val approximatedSubType = subType?.approximateToSuperTypeOrSelf(superType)
        val approximatedSuperType = superType?.approximateToSubTypeOrSelf()

        val preparedSubType = when {
            approximatedSubType == null -> null
            shouldBeUsedWithoutApproximation(subType, approximatedSubType, this) -> subType
            else -> approximatedSubType
        }

        val preparedSuperType = when {
            approximatedSuperType == null -> null
            shouldBeUsedWithoutApproximation(superType, approximatedSuperType, this) -> superType
            superType.typeConstructor().hasRecursiveTypeParametersWithGivenSelfType() -> superType
            else -> approximatedSuperType
            // Super type should be the most flexible, sub type should be the least one
        }

        return preparedSubType to preparedSuperType
    }

    /**
     * Returns `true` if using [approximatedResultType] as result type leads to a contradiction.
     *
     * If [resultType] and [approximatedResultType] are referentially equal, it means there is nothing to approximate in the first place.
     * Therefore `false` is returned.
     *
     * Only used when [LanguageFeature.ImprovedCapturedTypeApproximationInInference] is enabled.
     */
    context(c: Context)
    private fun shouldBeUsedWithoutApproximation(
        resultType: CangJieTypeMarker,
        approximatedResultType: CangJieTypeMarker,
        variableWithConstraints: VariableWithConstraints,
    ): Boolean {
        if (resultType === approximatedResultType || c.hasContradiction) return false

        // TODO(related to KT-64802) This if shouldn't be necessary but removing it breaks
        // compiler/testData/diagnostics/tests/unsignedTypes/conversions/inferenceForSignedAndUnsignedTypes.kt
        if (resultType.typeConstructor().isIntegerLiteralTypeConstructor()) return false

        return !c.isEqualityConstraintCompatible(approximatedResultType, variableWithConstraints.typeVariable.defaultType())
    }

    /**
     * 在两个方向的候选类型之间选择当前变量的结果类型。
     */
    context(c: Context)
    private fun VariableWithConstraints.resultType(
        firstCandidate: CangJieTypeMarker?,
        secondCandidate: CangJieTypeMarker?,
    ): CangJieTypeMarker? {
        if (firstCandidate == null || secondCandidate == null) {
            val singleCandidate = firstCandidate ?: secondCandidate ?: return null
            return singleCandidate.takeIf { isSuitableType(it, this) }
        }

        specialResultForIntersectionType(firstCandidate, secondCandidate)?.let { intersectionWithAlternative ->
            return intersectionWithAlternative
        }

        if (isSuitableType(firstCandidate, this)) return firstCandidate

        return secondCandidate.takeIf { isSuitableType(it, this) }
    }

    /**
     * 为交叉类型候选构造带上界的特殊结果类型。
     */
    context(c: Context)
    private fun specialResultForIntersectionType(firstCandidate: CangJieTypeMarker, secondCandidate: CangJieTypeMarker): CangJieTypeMarker? {
        if (firstCandidate.typeConstructor().isIntersection()) {
            if (!AbstractTypeChecker.isSubtypeOf(c, firstCandidate.toPublicType(), secondCandidate.toPublicType())) {
                return c.createTypeWithUpperBoundForIntersectionResult(firstCandidate, secondCandidate)
            }
        }

        return null
    }

    /**
     * 把内部候选类型近似为公开声明可见的类型形态。
     */
    private fun CangJieTypeMarker.toPublicType(): CangJieTypeMarker =
        typeApproximator.approximateToSuperType(this, TypeApproximatorConfiguration.PublicDeclaration.SaveAnonymousTypes) ?: this

    /**
     * 判断 [resultType] 是否满足 [variableWithConstraints] 中所有可用于固定的 proper 约束。
     */
    context(c: Context)
    private fun isSuitableType(resultType: CangJieTypeMarker, variableWithConstraints: VariableWithConstraints): Boolean {
        val filteredConstraints = variableWithConstraints.constraints.filter { it.isProperConstraint() }

        // TODO(KT-68213) this loop is only used for checking of incompatible ILT approximations in K1
        // It shouldn't be necessary in K2
        // but removing it breaks compiler/fir/analysis-tests/testData/resolve/inference/kt53494.kt
        for (constraint in filteredConstraints) {
            if (!checkConstraint(constraint.type, constraint.kind, resultType)) return false
        }

        if (resultType.typeConstructor().isAnyConstructor() && !variableWithConstraints.isAnyResultTypeAllowed()) {
            return false
        }

        if (resultType.typeConstructor().isNothingConstructor() && !variableWithConstraints.isNothingResultTypeAllowed()) {
            return false
        }

        // if resultType is not Nothing
        if (trivialConstraintTypeInferenceOracle.isSuitableResultedType(resultType)) return true


        return filteredConstraints.any { it.kind.isUpper() }
    }

    /**
     * 判断当前变量是否允许以 Any 类型作为固定结果。
     */
    context(c: Context)
    private fun VariableWithConstraints.isAnyResultTypeAllowed(): Boolean {
        return constraints.any { constraint ->
            constraint.type.typeConstructor().isAnyConstructor()
        }
    }

    /**
     * 判断当前变量是否允许以 Nothing 类型作为固定结果。
     */
    context(c: Context)
    private fun VariableWithConstraints.isNothingResultTypeAllowed(): Boolean {
        return constraints.any { constraint ->
            constraint.type.typeConstructor().isNothingConstructor()
        }
    }

    /**
     * 从下界约束中计算可作为固定结果的子类型候选。
     */
    context(c: Context)
    private fun VariableWithConstraints.findSubType(): CangJieTypeMarker? {
        val lowerConstraintTypes = prepareLowerConstraints(constraints)

        if (lowerConstraintTypes.isNotEmpty()) {
            if (lowerConstraintTypes.size > 1 &&
                lowerConstraintTypes.all { type ->
                    type.asRigidType()?.isStubTypeForVariableInSubtyping() == true
                }
            ) {
                // This situation is only allowed to happen when semi-fixing for input types for OverloadResolutionByLambdaReturnType
                check(c.allowSemiFixationToOtherTypeVariables) {
                    "Only type-variable built constraints $lowerConstraintTypes found for $typeVariable"
                }
                return null
            }
            val types = sinkIntegerLiteralTypes(lowerConstraintTypes)
            var commonSuperType = CommonSuperTypeCalculator.commonSuperType(types)

            if (commonSuperType.contains { it.asRigidType()?.isStubTypeForVariableInSubtyping() == true }) {
                val typesWithoutStubs = types.filter { lowerType ->
                    !lowerType.contains { it.asRigidType()?.isStubTypeForVariableInSubtyping() == true }
                }

                when {
                    typesWithoutStubs.isNotEmpty() -> {
                        commonSuperType = CommonSuperTypeCalculator.commonSuperType(typesWithoutStubs)
                    }
                    // `typesWithoutStubs.isEmpty()` means that there are no lower constraints without type variables.
                    // It's only possible for the PCLA case, because otherwise none of the constraints would be considered as proper.
                    // So, we just get currently computed `commonSuperType` and substitute all local stub types
                    // with corresponding type variables.
                    c.outerSystemVariablesPrefixSize > 0 -> {
                        commonSuperType = c.createSubstitutionFromSubtypingStubTypesToTypeVariables().safeSubstitute(commonSuperType)
                    }
                }
            }

            return commonSuperType
        }

        return null
    }

    /**
     * 准备用于计算公共父类型的下界约束类型列表。
     *
     * 当约束中混入未固定变量时，该方法会按 PCLA 规则把未固定变量替换为 stub type。
     */
    context(c: Context)
    private fun prepareLowerConstraints(constraints: List<Constraint>): List<CangJieTypeMarker> {
        var atLeastOneProper = false
        var atLeastOneNonProper = false

        val lowerConstraintTypes = mutableListOf<CangJieTypeMarker>()

        for (constraint in constraints) {
            if (constraint.kind != ConstraintKind.LOWER) continue
            if (constraint.isNoInfer) continue

            val type = constraint.type

            lowerConstraintTypes.add(type)

            if (type.isProperTypeForFixation()) {
                atLeastOneProper = true
            } else {
                atLeastOneNonProper = true
            }
        }

        if (!atLeastOneProper) return emptyList()

        // PCLA slow path
        // We only allow using TVs fixation for nested PCLA calls
        if (c.outerSystemVariablesPrefixSize > 0) {
            val notFixedToStubTypesSubstitutor = c.buildNotFixedVariablesToStubTypesSubstitutor()
            return lowerConstraintTypes.map { notFixedToStubTypesSubstitutor.safeSubstitute(it) }
        }

        if (!atLeastOneNonProper) return lowerConstraintTypes

        val notFixedToStubTypesSubstitutor = c.buildNotFixedVariablesToStubTypesSubstitutor()

        return lowerConstraintTypes.map { if (it.isProperTypeForFixation()) it else notFixedToStubTypesSubstitutor.safeSubstitute(it) }
    }

    /**
     * 调整整型字面量类型在公共父类型计算中的顺序，使非字面量候选优先沉淀为结果。
     */
    context(c: Context)
    private fun sinkIntegerLiteralTypes(types: List<CangJieTypeMarker>): List<CangJieTypeMarker> {
        return types.sortedBy { type ->
            val containsILT = type.contains { it.asRigidType()?.isIntegerLiteralType() ?: false }
            if (containsILT) 1 else 0
        }
    }

    /**
     * 根据上界约束计算交叉后的父类型候选。
     */
    context(c: Context)
    private fun computeUpperType(upperConstraints: List<Constraint>): CangJieTypeMarker {
        return c.intersectTypes(upperConstraints.map { it.type })
    }

    /**
     * 从上界约束中计算可作为固定结果的父类型候选。
     */
    context(c: Context)
    private fun VariableWithConstraints.findSuperType(): CangJieTypeMarker? {
        val upperConstraints = constraints.filter {
            if (it.kind != ConstraintKind.UPPER) return@filter false
            it.isProperConstraint()
        }

        if (upperConstraints.isNotEmpty()) {
            return computeUpperType(upperConstraints)
        }

        return null
    }

    /**
     * 判断该约束是否可直接参与类型变量固定。
     */
    context(c: Context)
    private fun Constraint.isProperConstraint(): Boolean {
        return type.isProperTypeForFixation() && !isNoInfer
    }

    /**
     * 判断当前类型在固定阶段是否为 proper type。
     */
    context(c: Context)
    private fun CangJieTypeMarker.isProperTypeForFixation(): Boolean =
        isProperTypeForFixation(c.notFixedTypeVariables.keys) { c.isProperType(it) }

    /**
     * 在存在 proper 等价约束时解析可代表该等价约束集合的结果类型。
     */
    context(c: Context)
    fun findResultIfThereIsEqualsConstraint(variableWithConstraints: VariableWithConstraints, isStrictMode: Boolean): CangJieTypeMarker? {
        val properEqualityConstraints = variableWithConstraints.constraints.filter {
            it.kind == ConstraintKind.EQUALITY && it.isProperConstraint()
        }

        return representativeFromEqualityConstraints(variableWithConstraints, properEqualityConstraints, isStrictMode)
    }

    // Discriminate integer literal types as they are less specific than separate integer types (Int, Short...)
    /**
     * 从 proper 等价约束集合中选择单一代表类型。
     *
     * 非严格模式下允许整型字面量类型作为退路；严格模式下只接受不含整型字面量类型的代表。
     */
    context(c: Context)
    private fun representativeFromEqualityConstraints(
        variableWithConstraints: VariableWithConstraints,
        properEqualityConstraints: List<Constraint>,
        // Allow only types not-containing ILT and which might work as a representative of other ones from EQ constraints
        // TODO: Consider making it always `true` (see KT-70062)
        isStrictMode: Boolean
    ): CangJieTypeMarker? {
        if (properEqualityConstraints.isEmpty()) return null

        val constraintTypes = properEqualityConstraints.map { it.type }
        val nonLiteralTypes = constraintTypes.filter { constraintType ->
            if (isStrictMode)
                !constraintType.contains { it.typeConstructor().isIntegerLiteralTypeConstructor() }
            else
                !constraintType.typeConstructor().isIntegerLiteralTypeConstructor()
        }

        nonLiteralTypes.singleBestRepresentative()?.let { return it }

        if (isStrictMode) return null

        constraintTypes.singleBestRepresentative()?.let { return it }
        // At this point, it seems like the constraint system has found a contradiction.

        val typeVariable = variableWithConstraints.typeVariable
        // If there's a contradiction, it's best if we fix the variable to exactly
        // the type argument the user has provided for it, so that the diagnostics
        // remain accurate.
        return properEqualityConstraints.findExplicitTypeArgumentConstraintFor(typeVariable)?.type ?: constraintTypes.first()
    }

    /**
     * If there's a contradiction, type argument variables should be fixed to the explicit arguments
     * to make sure the error diagnostics and further checks in checkers remain accurate.
     */
    context(c: Context)
    private fun List<Constraint>.findExplicitTypeArgumentConstraintFor(typeVariable: TypeVariableMarker): Constraint? =
        firstOrNull {
            it.position.from is ExplicitTypeParameterConstraintPosition<*> && it.position.initialConstraint.a == typeVariable.defaultType()
        }

    companion object {
        /**
         * 推断失败的中性错误类型标记（createErrorType 的 debugName/reason）。
         *
         * 该标记本身不携带用户可见诊断；固定阶段检测到该标记时统一走"无法推断"
         * 错误路径（UNABLE_TO_INFER_GENERIC_FUNC），与既有无法推断场景共用同一
         * 报告逻辑，避免在 ResultTypeResolver 与诊断收集器之间泄漏内部类型。
         */
        const val SOLVER_FAILURE_MARKER = "CJ_SOLVER_FAILURE_MARKER"
    }
}
