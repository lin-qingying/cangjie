/*
 * 抽象类型近似器
 *
 * 在类型推断完成后，将编译器内部类型（捕获类型、交叉类型、整型字面量类型、本地类型等）
 * 近似为用户可见的具体类型。
 *
 * 改造说明（相对于 Kotlin K2 原版）：
 * - 移除 FlexibleTypeMarker 分支（仓颉无弹性类型，所有类型均为刚性类型）
 * - 移除 DefinitelyNotNullTypeMarker 处理（仓颉无 DefinitelyNotNull 类型）
 * - 移除 TypeVariance / effectiveVariance 逻辑（仓颉泛型全不变，无 in/out 投影）
 * - 移除星号投影处理（仓颉无 * 投影）
 * - 移除可空类型操作（isMarkedNullable、withNullability、nullableAnyType 等）
 * - 移除 Raw 类型处理（仓颉无 Raw 类型）
 * - 移除 Dynamic 类型处理
 * - 移除 K1/K2 分支判断（始终使用 K2 风格）
 * - 移除旧版缓存机制（始终使用重构后的递归追踪方案）
 * - 移除 LanguageVersionSettings 依赖（无需运行时特性开关）
 * - 简化 approximateParametrizedType：仓颉泛型全不变，直接替换近似后的实参
 */

package org.cangnova.cangjie.type

import org.cangnova.cangjie.type.model.CangJieTypeMarker
import org.cangnova.cangjie.type.model.CaptureStatus
import org.cangnova.cangjie.type.model.CapturedTypeMarker
import org.cangnova.cangjie.type.model.IntersectionTypeConstructorMarker
import org.cangnova.cangjie.type.model.RigidTypeMarker
import org.cangnova.cangjie.type.model.SimpleTypeMarker
import org.cangnova.cangjie.type.model.TypeArgumentMarker
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.cangnova.cangjie.type.model.TypeParameterMarker
import org.cangnova.cangjie.type.model.TypeSystemInferenceExtensionContext
import org.cangnova.cangjie.type.model.TypeVariableTypeConstructorMarker

typealias TypeApproximatorCachesPerConfiguration = MutableMap<TypeApproximatorConfiguration, AbstractTypeApproximator.Cache>

/**
 * 抽象类型近似器
 *
 * 将编译器内部使用的中间类型近似为用户可见的具体类型。
 * 支持两个方向：
 * - 近似到父类型（supertype）：type <: resultType
 * - 近似到子类型（subtype）：resultType <: type
 *
 * 返回 null 表示输入类型本身即为结果（不包含需要近似的内部类型）。
 *
 * @param ctx 类型系统推断扩展上下文，提供所有类型操作
 */
abstract class AbstractTypeApproximator(
    val ctx: TypeSystemInferenceExtensionContext,
) : TypeSystemInferenceExtensionContext by ctx {

    class ApproximationResult(val type: CangJieTypeMarker?)

    /**
     * 近似缓存
     *
     * 追踪正在被近似的类型以检测递归，并缓存已计算的结果以避免重复计算。
     */
    class Cache {
        val resultsForSupertype = mutableMapOf<CapturedTypeMarker, ApproximationResult>()
        val resultsForSubtype = mutableMapOf<CapturedTypeMarker, ApproximationResult>()

        /** 追踪正在向父类型方向近似的类型，用于检测递归 */
        val typesBeingApproximatedToSupertype = mutableSetOf<RigidTypeMarker>()

        /** 追踪正在向子类型方向近似的类型（用于断言） */
        val typesBeingApproximatedToSubtype = mutableSetOf<RigidTypeMarker>()

        operator fun plusAssign(other: Cache) {
            resultsForSupertype += other.resultsForSupertype
            resultsForSubtype += other.resultsForSubtype
            check(other.typesBeingApproximatedToSupertype.isEmpty() && other.typesBeingApproximatedToSubtype.isEmpty()) {
                "不应在类型近似过程中合并缓存/约束存储"
            }
        }
    }

    // =================================================================
    // 公开 API
    // =================================================================

    /**
     * 将类型近似到父类型方向。
     * type <: resultType
     *
     * @return 近似后的类型，若输入类型无需近似则返回 null
     */
    fun approximateToSuperType(
        type: CangJieTypeMarker,
        conf: TypeApproximatorConfiguration,
        caches: TypeApproximatorCachesPerConfiguration? = null,
    ): CangJieTypeMarker? {
        return approximateEntryPoint(type, conf, caches) { type, depth -> approximateToSuperType(type, depth) }
    }

    /**
     * 将类型近似到子类型方向。
     * resultType <: type
     *
     * @return 近似后的类型，若输入类型无需近似则返回 null
     */
    fun approximateToSubType(
        type: CangJieTypeMarker,
        conf: TypeApproximatorConfiguration,
        caches: TypeApproximatorCachesPerConfiguration? = null,
    ): CangJieTypeMarker? {
        return approximateEntryPoint(type, conf, caches) { type, depth -> approximateToSubType(type, depth) }
    }

    // =================================================================
    // 入口与上下文设置
    // =================================================================

    private inline fun approximateEntryPoint(
        type: CangJieTypeMarker,
        conf: TypeApproximatorConfiguration,
        caches: TypeApproximatorCachesPerConfiguration?,
        approximateTo: context(TypeApproximatorConfiguration, Cache) (CangJieTypeMarker, Int) -> CangJieTypeMarker?,
    ): CangJieTypeMarker? {
        return context(conf, caches?.getOrPut(conf, ::Cache) ?: Cache()) {
            try {
                approximateTo(type, -type.typeDepthForApproximation())
            } catch (e: StackOverflowError) {
                throw RuntimeException("类型近似时栈溢出: ${type.renderForDebugInfo()}", e)
            }
        }
    }

    protected open fun CangJieTypeMarker.renderForDebugInfo(): String = toString()

    // =================================================================
    // 异常与边界检查
    // =================================================================

    context(conf: TypeApproximatorConfiguration)
    private fun checkExceptionalCases(
        type: CangJieTypeMarker, depth: Int, toSuper: Boolean,
    ): ApproximationResult? {
        return when {
            type.isSpecial() ->
                null.toApproximationResult()

            type.isError() ->
                (if (!conf.approximateErrorTypes) null else type.defaultResult(toSuper)).toApproximationResult()

            else -> null
        }
    }

    private fun CangJieTypeMarker?.toApproximationResult(): ApproximationResult = ApproximationResult(this)

    // =================================================================
    // 带上下文的近似入口（处理异常情况后委托给主逻辑）
    // =================================================================

    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateToSuperType(type: CangJieTypeMarker, depth: Int): CangJieTypeMarker? {
        checkExceptionalCases(type, depth, toSuper = true)?.let { return it.type }
        // 仓颉所有类型均为刚性类型
        val rigidType = type.asRigidType() ?: return null
        return approximateTo(rigidType, toSuper = true, depth = depth)
    }

    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateToSubType(type: CangJieTypeMarker, depth: Int): CangJieTypeMarker? {
        checkExceptionalCases(type, depth, toSuper = false)?.let { return it.type }
        val rigidType = type.asRigidType() ?: return null
        return approximateTo(rigidType, toSuper = false, depth = depth)
    }

    // =================================================================
    // 主分发逻辑
    // =================================================================

    /**
     * 对刚性类型执行近似。
     * 根据类型的具体种类（参数化类型、捕获类型、交叉类型等）分发到对应处理器。
     */
    context(conf: TypeApproximatorConfiguration, cache: Cache)
    private fun approximateTo(
        type: RigidTypeMarker,
        toSuper: Boolean,
        depth: Int,
    ): CangJieTypeMarker? {
        // 有泛型实参的类型：递归近似各实参
        if (type.argumentsCount() != 0) {
            return approximateParametrizedType(type, toSuper, depth + 1)
        }

        require(type is SimpleTypeMarker)
        val typeConstructor = type.typeConstructor()

        // 捕获类型
        if (typeConstructor.isCapturedTypeConstructor()) {
            val capturedType = type.asCapturedType()
            require(capturedType != null) {
                "类型不一致 -- typeConstructor = $typeConstructor 与 class = ${type::class.java.canonicalName} 不匹配。" +
                        "type.toString() = $type"
            }
            return approximateCapturedType(capturedType, toSuper, depth)
        }

        // 交叉类型
        if (typeConstructor.isIntersection()) {
            return approximateIntersectionType(type, toSuper, depth)
        }

        // 类型变量
        if (typeConstructor is TypeVariableTypeConstructorMarker) {
            return if (!conf.shouldApproximateTypeVariableBasedType(typeConstructor)) null
            else type.defaultResult(toSuper)
        }

        // 整型字面量常量类型（如 IdealInt）
        if (typeConstructor.isIntegerLiteralConstantTypeConstructor()) {
            if (!conf.approximateIntegerLiteralConstantTypes) return null
            check(conf.expectedTypeForIntegerLiteralType == null || depth <= 0)
            return typeConstructor.getApproximatedIntegerLiteralType(conf.expectedTypeForIntegerLiteralType)
        }

        // 整型常量运算类型
        if (typeConstructor.isIntegerConstantOperatorTypeConstructor()) {
            if (!conf.approximateIntegerConstantOperatorTypes) return null
            check(conf.expectedTypeForIntegerLiteralType == null || depth <= 0)
            return typeConstructor.getApproximatedIntegerLiteralType(conf.expectedTypeForIntegerLiteralType)
        }

        // 本地/匿名类型
        return approximateLocalTypes(type, toSuper, depth)
    }

    // =================================================================
    // 参数化类型近似
    // =================================================================

    /**
     * 近似参数化类型（带泛型实参的类型，如 List<CapturedType>）。
     *
     * 仓颉泛型全不变（invariant），因此：
     * - 父方向：直接用近似后的实参替换（尽力近似，非严格子类型关系）
     * - 子方向：仅当近似后的实参与原实参相等时才替换，否则返回默认类型
     */
    context(conf: TypeApproximatorConfiguration, cache: Cache)
    private fun approximateParametrizedType(
        type: RigidTypeMarker,
        toSuper: Boolean,
        depth: Int,
    ): RigidTypeMarker? {
        val typeConstructor = type.typeConstructor()
        if (typeConstructor.parametersCount() != type.argumentsCount()) {
            return if (!conf.approximateErrorTypes) {
                createErrorType(
                    "类型不一致: $type (parameters.size = ${typeConstructor.parametersCount()}, " +
                            "arguments.size = ${type.argumentsCount()})",
                    type
                )
            } else type.defaultResult(toSuper)
        }

        val newArguments = arrayOfNulls<TypeArgumentMarker?>(type.argumentsCount())

        for (index in 0 until type.argumentsCount()) {
            val argument = type.getArgument(index)
            val argumentType = argument.getType() ?: continue

            // 递归防护：检测父方向近似循环
            if (toSuper) {
                val rigidArgumentType = argumentType.asRigidType()
                if (rigidArgumentType != null && rigidArgumentType in cache.typesBeingApproximatedToSupertype) {
                    val capturedType = rigidArgumentType.asCapturedType()
                    if (capturedType != null && conf.shouldApproximateCapturedType(capturedType) ||
                        rigidArgumentType.requiresLocalOrAnonymousApproximation()
                    ) {
                        // 递归检测到，用顶层类型替代
                        newArguments[index] = createTypeArgument(anyType())
                    }
                    continue
                }
            }

            val approximatedArgument = if (toSuper) {
                approximateToSuperType(argumentType, depth)
            } else {
                approximateToSubType(argumentType, depth)
            }

            if (approximatedArgument != null) {
                // 仓颉泛型全不变：子方向若实参类型发生变化则无法安全表示
                if (!toSuper && !AbstractTypeChecker.equalTypes(this@AbstractTypeApproximator, argumentType, approximatedArgument)) {
                    return type.defaultResult(toSuper)
                }
                newArguments[index] = createTypeArgument(approximatedArgument)
            }
        }

        if (newArguments.all { it == null }) return approximateLocalTypes(type, toSuper, depth)

        val newArgumentsList = List(type.argumentsCount()) { index -> newArguments[index] ?: type.getArgument(index) }
        val approximatedType = type.replaceArguments(newArgumentsList)
        return approximateLocalTypes(approximatedType, toSuper, depth) ?: approximatedType
    }

    // =================================================================
    // 捕获类型近似
    // =================================================================

    context(conf: TypeApproximatorConfiguration, cache: Cache)
    private fun approximateCapturedType(
        capturedType: CapturedTypeMarker,
        toSuper: Boolean,
        depth: Int,
    ): CangJieTypeMarker? {
        val currentlyBeingApproximated = if (toSuper) {
            cache.typesBeingApproximatedToSupertype
        } else {
            cache.typesBeingApproximatedToSubtype
        }
        val computedResults = if (toSuper) {
            cache.resultsForSupertype
        } else {
            cache.resultsForSubtype
        }

        computedResults[capturedType]?.let { return it.type }

        if (!currentlyBeingApproximated.add(capturedType)) {
            return createErrorType(
                "捕获类型循环应在 approximateParametrizedType 中处理",
                capturedType,
            )
        }

        val result = doApproximateCapturedType(capturedType, toSuper, depth)

        currentlyBeingApproximated.remove(capturedType)

        // 仅缓存 FROM_EXPRESSION 状态的结果，其生命周期较长
        if (capturedType.captureStatus() == CaptureStatus.FROM_EXPRESSION) {
            computedResults[capturedType] = ApproximationResult(result)
        }

        return result
    }

    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun doApproximateCapturedType(
        capturedType: CapturedTypeMarker,
        toSuper: Boolean,
        depth: Int,
    ): CangJieTypeMarker? {
        val supertypes = capturedType.typeConstructor().supertypes()
        val baseSuperType = when (supertypes.size) {
            0 -> anyType()
            1 -> supertypes.single()
            else -> {
                // 仓颉无星号投影，直接使用捕获类型的投影类型
                val projection = capturedType.typeConstructorProjection()
                projection.getType()!!
            }
        }
        val baseSubType = capturedType.lowerType() ?: nothingType()

        val approximatedSuperType by lazy(LazyThreadSafetyMode.NONE) {
            approximateToSuperType(baseSuperType, depth)
        }
        val approximatedSubType by lazy(LazyThreadSafetyMode.NONE) {
            approximateToSubType(baseSubType, depth)
        }

        if (!conf.shouldApproximateCapturedType(capturedType)) {
            return null
        }

        return if (toSuper) approximatedSuperType ?: baseSuperType else approximatedSubType ?: baseSubType
    }

    // =================================================================
    // 交叉类型近似
    // =================================================================

    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateIntersectionType(
        type: RigidTypeMarker,
        toSuper: Boolean,
        depth: Int,
    ): CangJieTypeMarker? {
        val typeConstructor = type.typeConstructor()
        assert(typeConstructor.isIntersection()) {
            "应为交叉类型: $type, typeConstructor class: ${typeConstructor::class.java.canonicalName}"
        }
        assert(typeConstructor.supertypes().isNotEmpty()) {
            "交叉类型的成员列表不应为空: $type"
        }

        val upperBoundForApproximation = type.getUpperBoundForApproximationOfIntersectionType()

        if (toSuper && upperBoundForApproximation != null &&
            conf.intersectionStrategy == TypeApproximatorConfiguration.IntersectionStrategy.TO_COMMON_SUPERTYPE
        ) {
            return approximateToSuperType(upperBoundForApproximation, depth) ?: upperBoundForApproximation
        }

        var thereIsApproximation = false
        val newTypes = typeConstructor.supertypes().map {
            val newType = if (toSuper) approximateToSuperType(it, depth) else approximateToSubType(it, depth)
            if (newType != null) {
                thereIsApproximation = true
                newType
            } else it
        }

        /**
         * ALLOWED: A <: A', B <: B' => A & B <: A' & B'
         * 其他策略在子方向下无法找到有意义的子类型（只有 Nothing）
         */
        return when (conf.intersectionStrategy) {
            TypeApproximatorConfiguration.IntersectionStrategy.ALLOWED -> {
                if (!thereIsApproximation) null
                else intersectTypes(newTypes, upperBoundForApproximation, toSuper, depth)
            }

            TypeApproximatorConfiguration.IntersectionStrategy.TO_FIRST -> {
                if (toSuper) newTypes.first() else type.defaultResult(toSuper = false)
            }

            TypeApproximatorConfiguration.IntersectionStrategy.TO_COMMON_SUPERTYPE -> {
                if (!toSuper) return type.defaultResult(toSuper = false)
                val resultType = commonSuperType(newTypes)
                approximateToSuperType(resultType, depth) ?: resultType
            }
        }
    }

    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun intersectTypes(
        newTypes: List<CangJieTypeMarker>,
        upperBoundForApproximation: CangJieTypeMarker?,
        toSuper: Boolean,
        depth: Int,
    ): CangJieTypeMarker {
        val intersectionType = intersectTypes(newTypes)

        if (upperBoundForApproximation == null) {
            return intersectionType
        }

        val alternativeTypeApproximated = if (toSuper) {
            approximateToSuperType(upperBoundForApproximation, depth)
        } else {
            approximateToSubType(upperBoundForApproximation, depth)
        } ?: upperBoundForApproximation

        return createTypeWithUpperBoundForIntersectionResult(intersectionType, alternativeTypeApproximated)
    }

    // =================================================================
    // 本地/匿名类型近似
    // =================================================================

    context(conf: TypeApproximatorConfiguration)
    private fun CangJieTypeMarker.requiresLocalOrAnonymousApproximation(
        constructor: TypeConstructorMarker = typeConstructor(),
    ): Boolean {
        return conf.approximateLocalTypes && conf.shouldApproximateLocalType(ctx, this) && constructor.isLocalType() ||
                conf.approximateAnonymous && constructor.isAnonymous()
    }

    /**
     * 将本地或匿名类型近似为其最近的非本地/非匿名父类型。
     * 使用 BFS 搜索父类型层级，忽略 Any。
     */
    context(conf: TypeApproximatorConfiguration, cache: Cache)
    private fun approximateLocalTypes(
        type: RigidTypeMarker,
        toSuper: Boolean,
        depth: Int,
    ): RigidTypeMarker? {
        if (!toSuper) return null
        if (!conf.approximateLocalTypes && !conf.approximateAnonymous) return null

        val constructor = type.typeConstructor()
        if (!type.requiresLocalOrAnonymousApproximation(constructor)) return null

        val typeCheckerContext = newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false,
        )

        // BFS 查找非本地/非匿名父类型
        val visited = mutableSetOf<RigidTypeMarker>()
        val queue = ArrayDeque<RigidTypeMarker>().apply { add(type) }
        var result: RigidTypeMarker? = null

        while (queue.isNotEmpty()) {
            val currentType = queue.removeFirst()
            if (!visited.add(currentType)) continue
            val currentConstructor = currentType.typeConstructor()
            if (!currentType.requiresLocalOrAnonymousApproximation(currentConstructor)) {
                result = currentType
                break
            }
            currentConstructor.supertypes()
                .flatMap { AbstractTypeChecker.findCorrespondingSupertypes(typeCheckerContext, type, it.typeConstructor()) }
                .filterTo(queue) { !it.typeConstructor().isAnyConstructor() }
        }

        if (result == null) {
            result = ctx.anyType()
        }

        // 递归近似找到的父类型（其中可能仍包含需要近似的捕获类型等）
        cache.typesBeingApproximatedToSupertype += type
        val furtherApproximated = approximateToSuperType(result, depth) as? RigidTypeMarker
        if (furtherApproximated != null) {
            result = furtherApproximated
        }
        cache.typesBeingApproximatedToSupertype -= type

        return result
    }

    // =================================================================
    // 辅助方法
    // =================================================================

    /**
     * 计算多个类型的公共父类型。
     * 用于交叉类型的 TO_COMMON_SUPERTYPE 策略。
     */
    protected abstract fun commonSuperType(types: List<CangJieTypeMarker>): CangJieTypeMarker

    /**
     * 返回默认近似结果。
     * 父方向返回 Any（顶层类型），子方向返回 Nothing（底层类型）。
     */
    private fun CangJieTypeMarker.defaultResult(toSuper: Boolean): SimpleTypeMarker =
        if (toSuper) anyType() else nothingType()

    /** 判断类型是否为平凡父类型（Any） */
    private fun CangJieTypeMarker.isTrivialSuper() = isAny()

    /** 判断类型是否为平凡子类型（Nothing） */
    private fun CangJieTypeMarker.isTrivialSub() = isNothing()

    override fun CapturedTypeMarker.typeParameter(): TypeParameterMarker? {
        with(ctx) {
            return typeParameter()
        }
    }
}