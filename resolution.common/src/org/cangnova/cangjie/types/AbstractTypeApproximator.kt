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
     * With this flag enabled:
     * - Track currently being approximated types, so we could catch recursion instead of using the controversial `depth > 3` condition
     * - Put computed results to relevant caches to reuse them
     * - Mark some places that previously were workarounds for caching/recursion prevention as obsolete
     */
    private val capturedTypeApproximationReworked: Boolean = true

    // Those caches are only used prior to 2.2 (without CapturedTypeApproximationReworked)
    private val cacheForIncorporationConfigToSuperDirection = ConcurrentHashMap<CangJieTypeMarker, ApproximationResult>()
    private val cacheForIncorporationConfigToSubtypeDirection = ConcurrentHashMap<CangJieTypeMarker, ApproximationResult>()

    private val referenceApproximateToSuperType: FunctionTypeForRigidTypeApproximation
        get() = { type, depth -> approximateSimpleToSuperType(type, depth) }
    private val referenceApproximateToSubType: FunctionTypeForRigidTypeApproximation
        get() = { type, depth -> approximateSimpleToSubType(type, depth) }

    companion object {
        // This value is only used prior to 2.2 (without CapturedTypeApproximationReworked)
        const val CACHE_FOR_INCORPORATION_MAX_SIZE = 500
    }

    class Cache {
        val resultsForSupertype = mutableMapOf<CapturedTypeMarker, ApproximationResult>()
        val resultsForSubtype = mutableMapOf<CapturedTypeMarker, ApproximationResult>()

        // We assume that no approximation cycles should be met when approximating to a type's lower bound
        // Currently, the known sources of approximation cycles are
        // - captured types with recursive bounds
        // - recursive local types
        val typesBeingApproximatedToSupertype = mutableSetOf<RigidTypeMarker>()

        // Non-trivial lower bounds are always brought via explicitly specified/inferred `in` projection where no recursion should happen.
        @AssertionsOnly
        val typesBeingApproximatedToSubtype = mutableSetOf<RigidTypeMarker>()

        operator fun plusAssign(other: Cache) {
            resultsForSupertype += other.resultsForSupertype
            resultsForSubtype += other.resultsForSubtype

            @OptIn(AssertionsOnly::class)
            check(other.typesBeingApproximatedToSupertype.isEmpty() && other.typesBeingApproximatedToSubtype.isEmpty()) {
                "Combination of caches/Constraint storages is not expected to happen during type approximation"
            }
        }
    }

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
        cacheForIncorporationConfigToSubtypeDirection.clear()
        cacheForIncorporationConfigToSuperDirection.clear()
    }

    context(conf: TypeApproximatorConfiguration)
    private fun checkExceptionalCases(
        type: CangJieTypeMarker, depth: Int, toSuper: Boolean
    ): ApproximationResult? {
        return when {
            type.isSpecial() ->
                null.toApproximationResult()

            type.isError() ->
                // todo -- fix builtIns. Now builtIns here is DefaultBuiltIns
                (if (!conf.approximateErrorTypes) null else type.defaultResult(toSuper)).toApproximationResult()

            // Limiting approximation depth is obsolete
            !capturedTypeApproximationReworked && depth > 3 ->
                type.defaultResult(toSuper).toApproximationResult()

            else -> null
        }
    }

    private fun CangJieTypeMarker?.toApproximationResult(): ApproximationResult = ApproximationResult(this)

    context(conf: TypeApproximatorConfiguration)
    private inline fun cachedValue(
        type: CangJieTypeMarker,
        toSuper: Boolean,
        approximate: () -> CangJieTypeMarker?
    ): CangJieTypeMarker? {
        // Approximator depends on a configuration, so cache should take it into account
        // Here, we cache only types for configuration "from incorporation", which is used most intensively
        // More predictable caches are used since capturedTypeApproximationReworked
        if (capturedTypeApproximationReworked || conf !is TypeApproximatorConfiguration.IncorporationConfiguration) return approximate()

        val cache = if (toSuper) cacheForIncorporationConfigToSuperDirection else cacheForIncorporationConfigToSubtypeDirection

        if (cache.size > CACHE_FOR_INCORPORATION_MAX_SIZE) return approximate()

        return cache.getOrPut(type, { approximate().toApproximationResult() }).type
    }

    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateToSuperType(type: CangJieTypeMarker, depth: Int): CangJieTypeMarker? {
        checkExceptionalCases(type, depth, toSuper = true)?.let { return it.type }

        return cachedValue(type, toSuper = true) {
            approximateTo(
                AbstractTypeChecker.prepareType(ctx, type),
                referenceApproximateToSuperType,
                depth
            )
        }
    }

    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun approximateToSubType(type: CangJieTypeMarker, depth: Int): CangJieTypeMarker? {
        checkExceptionalCases(type, depth, toSuper = false)?.let { return it.type }

        return cachedValue(type, toSuper = false) {
            approximateTo(
                AbstractTypeChecker.prepareType(ctx, type),
                referenceApproximateToSubType,
                depth
            )
        }
    }

    // Don't call this method directly, it should be used only in approximateToSuperType/approximateToSubType (use these methods instead)
    // This method contains detailed implementation only for type approximation, it doesn't check exceptional cases and doesn't use cache
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
    private fun CangJieTypeMarker.requiresLocalOrAnonymousApproximation(
        constructor: TypeConstructorMarker = typeConstructor()
    ): Boolean {
        return conf.approximateLocalTypes && conf.shouldApproximateLocalType(ctx, this) && constructor.isLocalType() ||
                conf.approximateAnonymous && constructor.isAnonymous()
    }

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
        }

        if (result == null) return null

        /*
         * AbstractTypeChecker captures any projections in the super type by default, which may lead to the situation, when some local
         *   type with projection is approximated to some public type with captured (from subtyping) type argument (which is obviously
         *   incorrect)
         *
         * interface Invariant<A>
         * private fun <B> Invariant<B>.privateFunc() = object : Invariant<B> {}
         *
         * fun Invariant<in Number>.publicFunc() = privateFunc()
         *
         * Here type of `privateFunc()` is _anonymous_<in Number>, and `findCorrespondingSupertypes` for it and `Invariant` as type
         *   constructor returns `Invariant<Captured(in Number)>`
         */
        cache.typesBeingApproximatedToSupertype += type
        (approximateTo(result, true, depth) as? RigidTypeMarker)?.let { result = it }
        cache.typesBeingApproximatedToSupertype -= type

        return result
    }

    private fun isIntersectionTypeEffectivelyNothing(constructor: IntersectionTypeConstructorMarker): Boolean {
        // We consider intersection as Nothing only if one of it's component is a primitive number type
        // It's intentional we're not trying to prove population of some type as it was in OI

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

        /**
         * For case ALLOWED:
         * A <: A', B <: B' => A & B <: A' & B'
         *
         * For other case -- it's impossible to find some type except Nothing as subType for intersection type.
         */
        val baseResult = when (conf.intersectionStrategy) {
            TypeApproximatorConfiguration.IntersectionStrategy.ALLOWED -> if (!thereIsApproximation) {
                return null
            } else {
                intersectTypes(newTypes, upperBoundForApproximation, toSuper, depth)
            }
            TypeApproximatorConfiguration.IntersectionStrategy.TO_FIRST -> if (toSuper) newTypes.first() else return type.defaultResult(toSuper = false)
            // commonSupertypeCalculator should handle flexible types correctly
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


    context(conf: TypeApproximatorConfiguration, cache: Cache)
    private fun approximateCapturedType(
        capturedType: CapturedTypeMarker,
        toSuper: Boolean,
        depth: Int,
    ): CangJieTypeMarker? {
        val currentlyBeingApproximated = when {
            toSuper -> cache.typesBeingApproximatedToSupertype
            // We only track potential loops in lower bounds to raise an assertion
            else -> @OptIn(AssertionsOnly::class) cache.typesBeingApproximatedToSubtype
        }
        val computedResults = when {
            toSuper -> cache.resultsForSupertype
            else -> cache.resultsForSubtype
        }

        computedResults[capturedType]?.let { return it.type }

        if (capturedTypeApproximationReworked && !currentlyBeingApproximated.add(capturedType)) {
            if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
                error("Captured types loop should be handled at approximateParametrizedType")
            }

            return createErrorType(
                "Captured types loop should be handled at approximateParametrizedType",
                capturedType,
            )
        }

        val result = doApproximateCapturedType(capturedType, toSuper, depth)

        if (capturedTypeApproximationReworked) {
            currentlyBeingApproximated.remove(capturedType)
        }

        if (!capturedTypeApproximationReworked) return result
        // There's no really much sense to store something beside FROM_EXPRESSION,
        // which might be quite long-living even surviving between the calls
        if (capturedType.captureStatus() != CaptureStatus.FROM_EXPRESSION) return result

        computedResults[capturedType] = ApproximationResult(result)

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
            1 -> supertypes.single().replaceRecursionWithStarProjection(capturedType)

            // Consider the following example:
            // A.getA()::class.java, where `getA()` returns some class from Java
            // From `::class` we are getting type KClass<Cap<out A!>>, where Cap<out A!> have two supertypes:
            // - Any (from declared upper bound of type parameter for KClass)
            // - (A..A?) -- from A!, projection type of captured type

            // Now, after approximation we were getting type `KClass<out A>`, because { Any & (A..A?) } = A,
            // but in old inference type was equal to `KClass<out A!>`.

            // Important note that from the point of type system first type is more specific:
            // Here, approximation of KClass<Cap<out A!>> is a type KClass<T> such that KClass<Cap<out A!>> <: KClass<out T> =>
            // So, the the more specific type for T would be "some non-null (because of declared upper bound type) subtype of A", which is `out A`

            // But for now, to reduce differences in behaviour of old and new inference, we'll approximate such types to `KClass<out A!>`

            // Once NI will be more stabilized, we'll use more specific type

            else -> intersectTypes(supertypes.map { it.replaceRecursionWithStarProjection(capturedType) })
        }
        val baseSubType = capturedType.lowerType() ?: nothingType()

        val approximatedSuperType by lazy(LazyThreadSafetyMode.NONE) {
            approximateToSuperType(baseSuperType, depth)
        }
        val approximatedSubType by lazy(LazyThreadSafetyMode.NONE) { approximateToSubType(baseSubType, depth) }

        if (!conf.shouldApproximateCapturedType(capturedType)) {
            /**
             * Here everything is ok if bounds for this captured type should not be approximated.
             * But. If such bounds contains some unauthorized types, then we cannot leave this captured type "as is".
             * And we cannot create new capture type, because meaning of new captured type is not clear.
             * So, we will just approximate such types
             *
             * TODO remove workaround when we can create captured types with external identity KT-65228.
             * todo handle flexible types
             */
            if (capturedTypeApproximationReworked || approximatedSuperType == null && approximatedSubType == null) {
                // Avoid avoiding approximation bounds of a captured type while one shouldn't be approximated itself doesn't look
                // universally correct. Though by construction of different captured kinds, currently it's only relevant
                // to a situation when some FOR_INCORPORATION is put into another captured type with a kind FROM_EXPRESSION and that
                // case is handled via IncorporationConfiguration.
                // TODO: consider replacing content of the captured types together with KT-65228
                return null
            }
        }
        val baseResult = if (toSuper) approximatedSuperType ?: baseSuperType else approximatedSubType ?: baseSubType

        // C = in Int, Int <: C => Int? <: C?
        // C = out Number, C <: Number => C? <: Number?
        return baseResult
    }

    private fun CangJieTypeMarker.replaceRecursionWithStarProjection(capturedType: CapturedTypeMarker): CangJieTypeMarker {
        // Recursion is being handled via approximateParametrizedType
        if (capturedTypeApproximationReworked) return this
        // This replacement is important for resolving the code like below in K2.
        //     fun bar(y: FieldOrRef<*>) = y.field
        //     interface FieldOrRef<FF : AbstractField<FF>> { val field: FF }
        //     abstract class AbstractField<out F : AbstractField<F>>
        // During resolving the value parameter y type, K1 also builds a type for a star projection *.
        // See fun TypeParameterDescriptor.starProjectionType(): KotlinType and fun buildStarProjectionTypeByTypeParameters.
        // Thanks to it, K1 builds the star projection type as AbstractField<*> and no other approximation is needed.
        //
        // In turn, K2 never makes such a thing (K2 star projection has no associated type).
        // Instead, it resolves y.field as CapturedType(*) C (see usage one line below),
        // and the constructor of this captured type has a star projection and a supertype of `AbstractField<C>`.
        //
        // Without this replacement, the type approximator currently cannot handle such a situation properly
        // and builds AbstractField<AbstractField<AbstractField<Any?>>>.
        // The check it == type here is intended to find a recursion inside a captured type.
        // A similar replacement for baseSubType looks unnecessary, no hits in the tests.

        return this
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

        if (typeConstructor.isCapturedTypeConstructor()) {
            val capturedType = type.asCapturedType()
            require(capturedType != null) {
                // KT-16147
                "Type is inconsistent -- somewhere we create type with typeConstructor = $typeConstructor " +
                        "and class: ${type::class.java.canonicalName}. type.toString() = $type"
            }
            return approximateCapturedType(capturedType, toSuper, depth)
        }

        if (typeConstructor.isIntersection()) {
            return approximateIntersectionType(type, toSuper, depth)
        }

        if (typeConstructor is TypeVariableTypeConstructorMarker) {
            return if (!conf.shouldApproximateTypeVariableBasedType(typeConstructor)) null else type.defaultResult(toSuper)
        }

        if (typeConstructor.isIntegerLiteralConstantTypeConstructor()) {
            return if (conf.approximateIntegerLiteralConstantTypes) {
                // We ensure that expectedTypeForIntegerLiteralType is only used for top-level and possibly flexible ILTs.
                // Otherwise, we can accidentally approximate nested ILTs to wrong types.
                check(conf.expectedTypeForIntegerLiteralType == null || depth <= 0)
                typeConstructor.getApproximatedIntegerLiteralType(conf.expectedTypeForIntegerLiteralType)
            } else null
        }

        if (typeConstructor.isIntegerConstantOperatorTypeConstructor()) {
            return if (conf.approximateIntegerConstantOperatorTypes) {
                // We ensure that expectedTypeForIntegerLiteralType is only used for top-level and possibly flexible ILTs.
                // Otherwise, we can accidentally approximate nested ILTs to wrong types.
                check(conf.expectedTypeForIntegerLiteralType == null || depth <= 0)
                typeConstructor.getApproximatedIntegerLiteralType(conf.expectedTypeForIntegerLiteralType)
            } else null
        }

        return approximateLocalTypes(type, toSuper, depth) // simple classifier type
    }

    private fun isApproximateDirectionToSuper(toSuper: Boolean) = toSuper

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
            val capturedType = simpleArgumentType.asCapturedType()

            fun approximateToSuperTypeWithRecursionPrevention(): ApproximationResult? {
                if (capturedTypeApproximationReworked && simpleArgumentType in cache.typesBeingApproximatedToSupertype) {
                    if (capturedType != null && conf.shouldApproximateCapturedType(capturedType) ||
                        simpleArgumentType.requiresLocalOrAnonymousApproximation()
                    ) {
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

        if (newArguments.all { it == null }) return approximateLocalTypes(type, toSuper, depth)

        val newArgumentsList = List(type.argumentsCount()) { index -> newArguments[index] ?: type.getArgument(index) }
        val approximatedType = type.replaceArguments(newArgumentsList)
        return approximateLocalTypes(approximatedType, toSuper, depth) ?: approximatedType
    }

    context(conf: TypeApproximatorConfiguration)
    private fun createApproximatedResultForInconsistentArgumentVariance(
        type: RigidTypeMarker,
        parameter: TypeParameterMarker,
        argument: TypeArgumentMarker,
        index: Int,
        toSuper: Boolean,
    ): RigidTypeMarker {
        if (conf.approximateErrorTypes) return type.defaultResult(toSuper)

        return createErrorType(
            "Inconsistent type: $type ($index parameter has unsupported argument form)",
            type
        )
    }

    context(conf: TypeApproximatorConfiguration, _: Cache)
    private fun shouldUseSubTypeForCapturedArgument(
        subType: CangJieTypeMarker,
        capturedArgumentType: CangJieTypeMarker,
        depth: Int,
    ): Boolean {
        if (subType.isTrivialSub()) return false
        return subType != nothingType()
    }

    private fun CangJieTypeMarker.defaultResult(toSuper: Boolean) = if (toSuper) anyType() else nothingType()

    private fun CangJieTypeMarker.isTrivialSuper() = this == anyType()

    private fun CangJieTypeMarker.isTrivialSub() = this == nothingType()

    override fun CapturedTypeMarker.typeParameter(): TypeParameterMarker? {
        with(ctx) {
            return typeParameter()
        }
    }
}
