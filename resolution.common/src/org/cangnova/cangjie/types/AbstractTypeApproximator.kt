/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.types

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.resolve.calls.CommonSuperTypeCalculator.commonSuperType
import org.cangnova.cangjie.resolve.calls.inference.model.AssertionsOnly
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 刚性类型近似函数的上下文函数类型。
 *
 * 函数在指定配置和递归缓存下，把刚性类型向父类型或子类型方向近似。
 */
private typealias FunctionTypeForRigidTypeApproximation =
        context(TypeApproximatorConfiguration, AbstractTypeApproximator.Cache) (RigidTypeMarker, Int) -> CangJieTypeMarker?

/**
 * 按近似配置分组保存的类型近似缓存。
 */
typealias TypeApproximatorCachesPerConfiguration = MutableMap<TypeApproximatorConfiguration, AbstractTypeApproximator.Cache>

/**
 * 类型近似器抽象基类。
 *
 * 近似器根据 [TypeApproximatorConfiguration] 将内部类型形态稳定化为父类型或子类型方向
 * 的可消费类型，主要服务于类型变量固定、公开签名暴露以及前后端边界传递。
 */
abstract class AbstractTypeApproximator(
    /**
     * 类型系统推断上下文，提供类型构造器、参数、替换和内建类型操作。
     */
    val ctx: TypeSystemInferenceExtensionContext,

    /**
     * 当前语言版本设置，保留给版本化近似规则使用。
     */
    protected val languageVersionSettings: LanguageVersionSettings,
) : TypeSystemInferenceExtensionContext by ctx {

    /**
     * 单次近似尝试的结果包装。
     */
    class ApproximationResult(
        /**
         * 近似后的类型；为 `null` 表示输入类型无需近似。
         */
        val type: CangJieTypeMarker?,
    )

    /**
     * 仓颉无 CapturedType，此缓存类仅用于跟踪递归近似防止无限循环。
     */
    class Cache {
        /**
         * 当前正在向父类型方向近似的刚性类型集合。
         */
        val typesBeingApproximatedToSupertype = mutableSetOf<RigidTypeMarker>()

        /**
         * 当前正在向子类型方向近似的刚性类型集合，仅用于断言。
         */
        @AssertionsOnly
        val typesBeingApproximatedToSubtype = mutableSetOf<RigidTypeMarker>()

        /**
         * 合并另一个缓存。
         *
         * 类型近似缓存不支持跨约束系统合并；该操作只允许合并空递归跟踪集合。
         */
        operator fun plusAssign(other: Cache) {
            @OptIn(AssertionsOnly::class)
            check(other.typesBeingApproximatedToSupertype.isEmpty() && other.typesBeingApproximatedToSubtype.isEmpty()) {
                "Combination of caches/Constraint storages is not expected to happen during type approximation"
            }
        }
    }

    /**
     * 向父类型近似的刚性类型函数引用。
     */
    private val referenceApproximateToSuperType: FunctionTypeForRigidTypeApproximation
        get() = { type, depth -> approximateSimpleToSuperType(type, depth) }

    /**
     * 向子类型近似的刚性类型函数引用。
     */
    private val referenceApproximateToSubType: FunctionTypeForRigidTypeApproximation
        get() = { type, depth -> approximateSimpleToSubType(type, depth) }

    // null means that this input type is the result, i.e. input type not contains not-allowed kind of types
    // type <: resultType
    /**
     * 将 [type] 向父类型方向近似。
     *
     * 返回 `null` 表示 [type] 已满足 [conf] 要求，无需替换。
     */
    fun approximateToSuperType(
        type: CangJieTypeMarker,
        conf: TypeApproximatorConfiguration,
        caches: TypeApproximatorCachesPerConfiguration? = null,
    ): CangJieTypeMarker? {
        return approximateEntryPoint(type, conf, caches) { type, depth -> approximateToSuperType(type, depth) }
    }

    // resultType <: type
    /**
     * 将 [type] 向子类型方向近似。
     *
     * 返回 `null` 表示 [type] 已满足 [conf] 要求，无需替换。
     */
    fun approximateToSubType(
        type: CangJieTypeMarker,
        conf: TypeApproximatorConfiguration,
        caches: TypeApproximatorCachesPerConfiguration? = null,
    ): CangJieTypeMarker? {
        return approximateEntryPoint(type, conf, caches) { type, depth -> approximateToSubType(type, depth) }
    }

    /**
     * 建立近似配置和缓存上下文，并执行实际近似函数。
     */
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
                throw RuntimeException("StackOverflowError during type approximation for ${type.renderForDebugInfo()}", e)
            }
        }
    }

    /**
     * 渲染用于异常诊断的类型文本。
     */
    protected open fun CangJieTypeMarker.renderForDebugInfo(): String = toString()

    /**
     * 清理近似缓存。
     */
    fun clearCache() {
        // 仓颉无 CapturedType，无需清除 incorporation 缓存
    }

    /**
     * 处理特殊类型和错误类型等可提前返回的近似分支。
     */
    context(conf: TypeApproximatorConfiguration)
    private fun checkExceptionalCases(
        type: CangJieTypeMarker, depth: Int, toSuper: Boolean
    ): ApproximationResult? {
        return when {
            type.isSpecial() ->
                null.toApproximationResult()

            type.isError() ->
                (if (!conf.approximateErrorTypes) null else type.defaultResult(toSuper)).toApproximationResult()

            else -> null
        }
    }

    /**
     * 将可空类型包装为 [ApproximationResult]。
     */
    private fun CangJieTypeMarker?.toApproximationResult(): ApproximationResult = ApproximationResult(this)

    /**
     * 对普通类型执行向父类型方向的内部近似。
     */
    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateToSuperType(type: CangJieTypeMarker, depth: Int): CangJieTypeMarker? {
        checkExceptionalCases(type, depth, toSuper = true)?.let { return it.type }
        return approximateTo(
            AbstractTypeChecker.prepareType(ctx, type),
            referenceApproximateToSuperType,
            depth
        )
    }

    /**
     * 对普通类型执行向子类型方向的内部近似。
     */
    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateToSubType(type: CangJieTypeMarker, depth: Int): CangJieTypeMarker? {
        checkExceptionalCases(type, depth, toSuper = false)?.let { return it.type }
        return approximateTo(
            AbstractTypeChecker.prepareType(ctx, type),
            referenceApproximateToSubType,
            depth
        )
    }

    /**
     * 将类型准备为刚性类型后委托给指定方向的近似函数。
     */
    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateTo(
        type: CangJieTypeMarker,
        approximateTo: FunctionTypeForRigidTypeApproximation,
        depth: Int
    ): CangJieTypeMarker? {
        val rigidType = type.asRigidType() ?: return null
        return approximateTo(rigidType, depth)
    }

    /**
     * 判断当前类型是否因匿名类型构造器而需要近似。
     */
    context(conf: TypeApproximatorConfiguration)
    private fun CangJieTypeMarker.requiresAnonymousApproximation(
        constructor: TypeConstructorMarker = typeConstructor()
    ): Boolean {
        return conf.approximateAnonymous && constructor.isAnonymous()
    }

    /**
     * 将匿名类型沿父类型方向近似为可公开表达的非匿名类型。
     */
    context(conf: TypeApproximatorConfiguration, cache: Cache)
    private fun approximateAnonymousTypes(
        type: RigidTypeMarker,
        toSuper: Boolean,
        depth: Int,
    ): RigidTypeMarker? {
        if (!toSuper) return null
        if (!conf.approximateAnonymous) return null

        val constructor = type.typeConstructor()
        if (!type.requiresAnonymousApproximation(constructor)) return null
        val typeCheckerContext = newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false
        )

        var result: RigidTypeMarker? = null

        run {
            val visited = mutableSetOf<RigidTypeMarker>()
            val queue = ArrayDeque<RigidTypeMarker>().apply { add(type) }

            while (queue.isNotEmpty()) {
                val currentType = queue.removeFirst()
                if (!visited.add(currentType)) continue
                val currentConstructor = currentType.typeConstructor()
                if (!currentType.requiresAnonymousApproximation(currentConstructor)) {
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
        }

        if (result == null) return null

        cache.typesBeingApproximatedToSupertype += type
        (approximateTo(result, true, depth) as? RigidTypeMarker)?.let { result = it }
        cache.typesBeingApproximatedToSupertype -= type

        return result
    }

    /**
     * 判断交叉类型是否等价于 Nothing 方向的空交叉。
     */
    private fun isIntersectionTypeEffectivelyNothing(constructor: IntersectionTypeConstructorMarker): Boolean {
        return constructor.supertypes().any { it.isSignedOrUnsignedNumberType() }
    }

    /**
     * 按配置处理交叉类型近似。
     */
    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateIntersectionType(
        type: RigidTypeMarker,
        toSuper: Boolean,
        depth: Int
    ): CangJieTypeMarker? {
        val typeConstructor = type.typeConstructor()
        assert(typeConstructor.isIntersection()) {
            "Should be intersection type: $type, typeConstructor class: ${typeConstructor::class.java.canonicalName}"
        }
        assert(typeConstructor.supertypes().isNotEmpty()) {
            "Supertypes for intersection type should not be empty: $type"
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

        val baseResult = when (conf.intersectionStrategy) {
            TypeApproximatorConfiguration.IntersectionStrategy.ALLOWED -> if (!thereIsApproximation) {
                return null
            } else {
                intersectTypes(newTypes, upperBoundForApproximation, toSuper, depth)
            }
            TypeApproximatorConfiguration.IntersectionStrategy.TO_FIRST -> if (toSuper) newTypes.first() else return type.defaultResult(toSuper = false)
            TypeApproximatorConfiguration.IntersectionStrategy.TO_COMMON_SUPERTYPE -> {
                if (!toSuper) return type.defaultResult(toSuper = false)
                val resultType = commonSuperType(newTypes)
                approximateToSuperType(resultType, depth) ?: resultType
            }
        }

        return baseResult
    }

    /**
     * 使用新的组成类型重建交叉类型，并按需要附加近似上界。
     */
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

    /**
     * 将简单刚性类型向父类型方向近似。
     */
    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateSimpleToSuperType(type: RigidTypeMarker, depth: Int) =
        approximateTo(type, toSuper = true, depth = depth)

    /**
     * 将简单刚性类型向子类型方向近似。
     */
    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateSimpleToSubType(type: RigidTypeMarker, depth: Int) =
        approximateTo(type, toSuper = false, depth = depth)

    /**
     * 根据类型构造器种类执行单个刚性类型的核心近似逻辑。
     */
    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateTo(
        type: RigidTypeMarker,
        toSuper: Boolean,
        depth: Int
    ): CangJieTypeMarker? {
        if (type.argumentsCount() != 0) {
            return approximateParametrizedType(type, toSuper, depth + 1)
        }

        require(type is SimpleTypeMarker)
        val typeConstructor = type.typeConstructor()

        // 仓颉无 CapturedType，跳过 captured type 近似

        if (typeConstructor.isIntersection()) {
            return approximateIntersectionType(type, toSuper, depth)
        }

        if (typeConstructor is TypeVariableTypeConstructorMarker) {
            return if (!conf.shouldApproximateTypeVariableBasedType(typeConstructor)) null else type.defaultResult(toSuper)
        }

        if (typeConstructor.isIntegerLiteralConstantTypeConstructor()) {
            return if (conf.approximateIntegerLiteralConstantTypes) {
                check(conf.expectedTypeForIntegerLiteralType == null || depth <= 0)
                typeConstructor.getApproximatedIntegerLiteralType(conf.expectedTypeForIntegerLiteralType)
            } else null
        }

        if (typeConstructor.isIntegerConstantOperatorTypeConstructor()) {
            return if (conf.approximateIntegerConstantOperatorTypes) {
                check(conf.expectedTypeForIntegerLiteralType == null || depth <= 0)
                typeConstructor.getApproximatedIntegerLiteralType(conf.expectedTypeForIntegerLiteralType)
            } else null
        }

        return approximateAnonymousTypes(type, toSuper, depth) // simple classifier type
    }

    /**
     * 近似带类型实参的参数化类型。
     */
    context(conf: TypeApproximatorConfiguration, cache: Cache)
    private fun approximateParametrizedType(
        type: RigidTypeMarker,
        toSuper: Boolean,
        depth: Int
    ): RigidTypeMarker? {
        val typeConstructor = type.typeConstructor()
        if (typeConstructor.parametersCount() != type.argumentsCount()) {
            return if (!conf.approximateErrorTypes) {
                createErrorType(
                    "Inconsistent type: $type (parameters.size = ${typeConstructor.parametersCount()}, arguments.size = ${type.argumentsCount()})",
                    type
                )
            } else type.defaultResult(toSuper)
        }

        val newArguments = arrayOfNulls<TypeArgumentMarker?>(type.argumentsCount())

        loop@ for (index in 0 until type.argumentsCount()) {
            val parameter = typeConstructor.getParameter(index)
            val argument = type.getArgument(index)

            val argumentType = argument.getType() ?: continue

            val simpleArgumentType = argumentType.asRigidType() ?: return type.defaultResult(toSuper)

            fun approximateToSuperTypeWithRecursionPrevention(): ApproximationResult? {
                if (simpleArgumentType in cache.typesBeingApproximatedToSupertype) {
                    if (simpleArgumentType.requiresAnonymousApproximation()) {
                        newArguments[index] = parameter.getUpperBounds().firstOrNull()?.asTypeArgument() ?: anyType().asTypeArgument()
                    } else {
                        // Just leave the argument type as is
                    }
                    return null
                }

                return ApproximationResult(
                    approximateToSuperType(argumentType, depth)
                )
            }

            val approximatedToSubType: CangJieTypeMarker? by lazy(LazyThreadSafetyMode.NONE) {
                approximateToSubType(argumentType, depth)
            }

            val approximatedArgument = if (toSuper) {
                approximateToSuperTypeWithRecursionPrevention()?.type ?: continue@loop
            } else {
                approximatedToSubType ?: continue@loop
            }

            newArguments[index] = approximatedArgument.asTypeArgument()
        }

        if (newArguments.all { it == null }) return approximateAnonymousTypes(type, toSuper, depth)

        val newArgumentsList = List(type.argumentsCount()) { index -> newArguments[index] ?: type.getArgument(index) }
        val approximatedType = type.replaceArguments(newArgumentsList)
        return approximateAnonymousTypes(approximatedType, toSuper, depth) ?: approximatedType
    }

    /**
     * 返回当前类型在指定方向上的默认近似结果。
     */
    private fun CangJieTypeMarker.defaultResult(toSuper: Boolean) = if (toSuper) anyType() else nothingType()

    /**
     * 判断当前类型是否为向子类型近似时的平凡 Nothing 结果。
     */
    private fun CangJieTypeMarker.isTrivialSub() = this == nothingType()


}
