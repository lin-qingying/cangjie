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

private typealias FunctionTypeForRigidTypeApproximation =
        context(TypeApproximatorConfiguration, AbstractTypeApproximator.Cache) (RigidTypeMarker, Int) -> CangJieTypeMarker?

typealias TypeApproximatorCachesPerConfiguration = MutableMap<TypeApproximatorConfiguration, AbstractTypeApproximator.Cache>

abstract class AbstractTypeApproximator(
    val ctx: TypeSystemInferenceExtensionContext,
    protected val languageVersionSettings: LanguageVersionSettings,
) : TypeSystemInferenceExtensionContext by ctx {

    class ApproximationResult(val type: CangJieTypeMarker?)

    /**
     * 仓颉无 CapturedType，此缓存类仅用于跟踪递归近似防止无限循环。
     */
    class Cache {
        val typesBeingApproximatedToSupertype = mutableSetOf<RigidTypeMarker>()

        @AssertionsOnly
        val typesBeingApproximatedToSubtype = mutableSetOf<RigidTypeMarker>()

        operator fun plusAssign(other: Cache) {
            @OptIn(AssertionsOnly::class)
            check(other.typesBeingApproximatedToSupertype.isEmpty() && other.typesBeingApproximatedToSubtype.isEmpty()) {
                "Combination of caches/Constraint storages is not expected to happen during type approximation"
            }
        }
    }

    private val referenceApproximateToSuperType: FunctionTypeForRigidTypeApproximation
        get() = { type, depth -> approximateSimpleToSuperType(type, depth) }
    private val referenceApproximateToSubType: FunctionTypeForRigidTypeApproximation
        get() = { type, depth -> approximateSimpleToSubType(type, depth) }

    // null means that this input type is the result, i.e. input type not contains not-allowed kind of types
    // type <: resultType
    fun approximateToSuperType(
        type: CangJieTypeMarker,
        conf: TypeApproximatorConfiguration,
        caches: TypeApproximatorCachesPerConfiguration? = null,
    ): CangJieTypeMarker? {
        return approximateEntryPoint(type, conf, caches) { type, depth -> approximateToSuperType(type, depth) }
    }

    // resultType <: type
    fun approximateToSubType(
        type: CangJieTypeMarker,
        conf: TypeApproximatorConfiguration,
        caches: TypeApproximatorCachesPerConfiguration? = null,
    ): CangJieTypeMarker? {
        return approximateEntryPoint(type, conf, caches) { type, depth -> approximateToSubType(type, depth) }
    }

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

    protected open fun CangJieTypeMarker.renderForDebugInfo(): String = toString()

    fun clearCache() {
        // 仓颉无 CapturedType，无需清除 incorporation 缓存
    }

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

    private fun CangJieTypeMarker?.toApproximationResult(): ApproximationResult = ApproximationResult(this)

    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateToSuperType(type: CangJieTypeMarker, depth: Int): CangJieTypeMarker? {
        checkExceptionalCases(type, depth, toSuper = true)?.let { return it.type }
        return approximateTo(
            AbstractTypeChecker.prepareType(ctx, type),
            referenceApproximateToSuperType,
            depth
        )
    }

    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateToSubType(type: CangJieTypeMarker, depth: Int): CangJieTypeMarker? {
        checkExceptionalCases(type, depth, toSuper = false)?.let { return it.type }
        return approximateTo(
            AbstractTypeChecker.prepareType(ctx, type),
            referenceApproximateToSubType,
            depth
        )
    }

    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateTo(
        type: CangJieTypeMarker,
        approximateTo: FunctionTypeForRigidTypeApproximation,
        depth: Int
    ): CangJieTypeMarker? {
        val rigidType = type.asRigidType() ?: return null
        return approximateTo(rigidType, depth)
    }

    context(conf: TypeApproximatorConfiguration)
    private fun CangJieTypeMarker.requiresAnonymousApproximation(
        constructor: TypeConstructorMarker = typeConstructor()
    ): Boolean {
        return conf.approximateAnonymous && constructor.isAnonymous()
    }

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

    private fun isIntersectionTypeEffectivelyNothing(constructor: IntersectionTypeConstructorMarker): Boolean {
        return constructor.supertypes().any { it.isSignedOrUnsignedNumberType() }
    }

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

    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateSimpleToSuperType(type: RigidTypeMarker, depth: Int) =
        approximateTo(type, toSuper = true, depth = depth)

    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateSimpleToSubType(type: RigidTypeMarker, depth: Int) =
        approximateTo(type, toSuper = false, depth = depth)

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

    private fun CangJieTypeMarker.defaultResult(toSuper: Boolean) = if (toSuper) anyType() else nothingType()

    private fun CangJieTypeMarker.isTrivialSub() = this == nothingType()


}
