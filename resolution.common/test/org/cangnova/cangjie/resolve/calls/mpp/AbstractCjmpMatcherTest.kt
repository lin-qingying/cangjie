package org.cangnova.cangjie.resolve.calls.mpp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AbstractCjmpMatcherTest {
    private data class TypeParameter(val name: String) : CjmpTypeParameterSymbolMarker

    private data class Declaration(
        val label: String,
        val parameterTypes: List<String?> = emptyList(),
        val returnType: String? = "Unit",
        val questReturnType: Boolean = false,
        val typeParameters: List<TypeParameter> = emptyList(),
        val classLikeKind: CjmpClassLikeKind? = null,
        val matchingCallableKind: CjmpCallableKind? = CjmpCallableKind.FUNCTION,
        val function: Boolean = true,
        val requiresImplementation: Boolean = false,
        val namedParameters: Boolean = false,
        val parameterName: String = "p",
        val parameterDefault: Boolean = false,
    ) : CjmpDeclarationSymbolMarker {
        init {
            require(!questReturnType || returnType == null) { "Quest must not carry a concrete return type" }
        }
    }

    private class Context(
        private val checkParameterDefaultValueConflict: Boolean = true,
    ) : CjmpMatchingContext<Declaration, TypeParameter, String> {
        override fun classLikeKind(declaration: Declaration): CjmpClassLikeKind? = declaration.classLikeKind

        override fun isExtend(declaration: Declaration): Boolean = false

        override fun haveSameExtendKey(first: Declaration, second: Declaration): CjmpTypeCompatibility =
            CjmpTypeCompatibility.INCOMPATIBLE

        override fun callableKind(declaration: Declaration): CjmpCallableKind? = declaration.matchingCallableKind

        override fun isFunction(declaration: Declaration): Boolean = declaration.function

        override fun isConstructor(declaration: Declaration): Boolean = false

        override fun areEnumConstructors(specific: Declaration, common: Declaration): Boolean = false

        override fun areEnumConstructorTypesEquivalent(
            specific: Declaration,
            common: Declaration,
            typeParameterMapping: Map<TypeParameter, TypeParameter>,
        ): CjmpTypeCompatibility = error("This context contains no enum constructors")

        override fun typeParameters(declaration: Declaration): List<TypeParameter> = declaration.typeParameters

        override fun valueParameters(declaration: Declaration): List<CjmpMatchingValueParameter<String>> =
            declaration.parameterTypes.mapIndexed { index, type ->
                CjmpMatchingValueParameter(
                    type,
                    isNamed = declaration.namedParameters,
                    name = "${declaration.parameterName}$index",
                    hasDefaultValue = declaration.parameterDefault,
                )
            }

        override fun checkParameterDefaultValueConflict(): Boolean = checkParameterDefaultValueConflict

        override fun areTypesEquivalent(
            specific: String?,
            common: String?,
            typeParameterMapping: Map<TypeParameter, TypeParameter>,
        ): CjmpTypeCompatibility = compare(specific, common, typeParameterMapping)

        override fun areReturnTypesCompatible(
            specific: Declaration,
            common: Declaration,
            typeParameterMapping: Map<TypeParameter, TypeParameter>,
        ): CjmpTypeCompatibility {
            // 官方 TypeManager 先排无效类型，再许可任侧真实 Quest；不能把已有错误伪装成待推断。
            if (specific.returnType?.containsErrorType() == true || common.returnType?.containsErrorType() == true) {
                return CjmpTypeCompatibility.UNRESOLVED
            }
            if (specific.returnType == null && !specific.questReturnType ||
                common.returnType == null && !common.questReturnType
            ) {
                return CjmpTypeCompatibility.UNRESOLVED
            }
            if (specific.questReturnType || common.questReturnType) return CjmpTypeCompatibility.COMPATIBLE
            return compare(specific.returnType, common.returnType, typeParameterMapping)
        }

        override fun needToReportMissingBody(common: Declaration, specific: Declaration): Boolean =
            specific.requiresImplementation

        private fun compare(
            specific: String?,
            common: String?,
            typeParameterMapping: Map<TypeParameter, TypeParameter>,
        ): CjmpTypeCompatibility {
            if (specific == null || common == null) return CjmpTypeCompatibility.UNRESOLVED
            if (specific.containsErrorType() || common.containsErrorType()) return CjmpTypeCompatibility.UNRESOLVED
            val substitutedCommon = typeParameterMapping.entries
                .firstOrNull { (commonParameter, _) -> commonParameter.name == common }
                ?.value?.name ?: common
            return if (specific == substitutedCommon) {
                CjmpTypeCompatibility.COMPATIBLE
            } else {
                CjmpTypeCompatibility.INCOMPATIBLE
            }
        }

        private fun String.containsErrorType(): Boolean =
            this == "Error" || contains("<Error") || contains(", Error")
    }

    @Test
    fun `nominal kinds use an exhaustive semantic classification`() {
        val specific = Declaration("specific", classLikeKind = CjmpClassLikeKind.TYPE_ALIAS)
        val common = Declaration("common", classLikeKind = CjmpClassLikeKind.CLASS)

        assertEquals(
            CjmpMatchResult.Mismatched(CjmpMismatchKind.CLASS_KIND),
            AbstractCjmpMatcher.matchEligibility(specific, common, Context()),
        )
    }

    @Test
    fun `nominal generic eligibility distinguishes genericity from generic arity`() {
        val context = Context()
        val parentMapping = mapOf(TypeParameter("parent T") to TypeParameter("child U"))
        val common = Declaration(
            "common",
            typeParameters = listOf(TypeParameter("common T")),
            classLikeKind = CjmpClassLikeKind.CLASS,
        )
        val twoParameterSpecific = Declaration(
            "specific",
            typeParameters = listOf(TypeParameter("specific U"), TypeParameter("specific V")),
            classLikeKind = CjmpClassLikeKind.CLASS,
        )
        val nonGenericSpecific = Declaration("non-generic", classLikeKind = CjmpClassLikeKind.CLASS)

        assertEquals(CjmpMatchResult.Matched, AbstractCjmpMatcher.matchEligibility(twoParameterSpecific, common, context))
        assertEquals(
            parentMapping,
            AbstractCjmpMatcher.typeParameterMapping(twoParameterSpecific, common, parentMapping, context),
        )
        assertEquals(
            CjmpMatchResult.Mismatched(CjmpMismatchKind.NOMINAL_GENERICNESS),
            AbstractCjmpMatcher.matchEligibility(nonGenericSpecific, common, context),
        )
        assertEquals(
            CjmpMatchResult.Matched,
            AbstractCjmpMatcher.matchEligibility(nonGenericSpecific, nonGenericSpecific, context),
        )
    }

    @Test
    fun `callable generic arity remains exact`() {
        val specific = Declaration("specific", typeParameters = listOf(TypeParameter("T"), TypeParameter("U")))
        val common = Declaration("common", typeParameters = listOf(TypeParameter("V")))

        assertEquals(
            CjmpMatchResult.Mismatched(CjmpMismatchKind.FUNCTION_TYPE_PARAMETER_COUNT),
            AbstractCjmpMatcher.matchEligibility(specific, common, Context()),
        )
    }

    @Test
    fun `non callable declarations cannot match through an unspecified kind`() {
        val specific = Declaration("specific", matchingCallableKind = null)
        val common = Declaration("common", matchingCallableKind = null)

        assertEquals(
            CjmpMatchResult.Mismatched(CjmpMismatchKind.CALLABLE_KIND),
            AbstractCjmpMatcher.matchEligibility(specific, common, Context()),
        )
    }

    @Test
    fun `different callable roles are rejected before signature matching`() {
        val specific = Declaration("specific", matchingCallableKind = CjmpCallableKind.FUNCTION)
        val common = Declaration("common", matchingCallableKind = CjmpCallableKind.VARIABLE)

        assertEquals(
            CjmpMatchResult.Mismatched(CjmpMismatchKind.CALLABLE_KIND),
            AbstractCjmpMatcher.matchEligibility(specific, common, Context()),
        )
    }

    @Test
    fun `signature facts do not preempt later parameter name events`() {
        val specific = Declaration("specific", listOf("Int64"), namedParameters = true, parameterName = "specific")
        val common = Declaration("common", listOf("Int64"), namedParameters = true, parameterName = "common")
        val context = Context()
        assertEquals(CjmpMatchResult.Matched, AbstractCjmpMatcher.matchSignature(specific, common, context))
        assertEquals(
            CjmpMatchResult.ParameterMismatched(CjmpParameterMismatch(CjmpMismatchKind.PARAMETER_NAMES, 0)),
            AbstractCjmpMatcher.match(specific, common, context),
        )
    }

    @Test
    fun `signature facts do not freeze mutable default transfer state`() {
        val specific = Declaration("specific", listOf("Int64"), parameterDefault = true)
        val common = Declaration("common", listOf("Int64"), parameterDefault = true)
        val context = Context()
        assertEquals(CjmpMatchResult.Matched, AbstractCjmpMatcher.matchSignature(specific, common, context))
        assertEquals(
            CjmpMatchResult.ParameterMismatched(CjmpParameterMismatch(CjmpMismatchKind.PARAMETER_DEFAULT_VALUE_BOTH_SIDES, 0)),
            AbstractCjmpMatcher.match(specific, common, context),
        )
    }

    @Test
    fun `historical parser-owned default rule does not reject both-side defaults in the matcher`() {
        val specific = Declaration("specific", listOf("Int64"), parameterDefault = true)
        val common = Declaration("common", listOf("Int64"), parameterDefault = true)

        assertEquals(
            CjmpMatchResult.ParameterMismatched(
                CjmpParameterMismatch(CjmpMismatchKind.PARAMETER_DEFAULT_VALUE_BOTH_SIDES, 0),
            ),
            AbstractCjmpMatcher.match(specific, common, Context()),
        )
        assertEquals(CjmpMatchResult.Matched, AbstractCjmpMatcher.match(specific, common, Context(false)))
    }

    @Test
    fun `candidate facts preserve parent substitution and all compatible alternatives`() {
        val specific = Declaration("specific", parameterTypes = listOf("U"))
        val incompatibleCommon = Declaration("commonString", parameterTypes = listOf("String"))
        val firstCompatibleCommon = Declaration("commonGeneric", parameterTypes = listOf("T"))
        val laterCompatibleCommon = Declaration("commonUnit", parameterTypes = listOf("U"))
        val context = Context()

        val result = AbstractCjmpMatcher.matchSpecificAgainstPotentialCommon(
            specific = specific,
            commonCandidates = listOf(incompatibleCommon, firstCompatibleCommon, laterCompatibleCommon),
            context = context,
            parentTypeParameterMapping = mapOf(TypeParameter("T") to TypeParameter("U")),
        )

        assertEquals(listOf(incompatibleCommon, firstCompatibleCommon, laterCompatibleCommon), result.map { it.common })
        assertEquals(
            listOf(CjmpMatchResult.Mismatched(CjmpMismatchKind.PARAMETER_TYPES), CjmpMatchResult.Matched, CjmpMatchResult.Matched),
            result.map { it.compatibility },
        )
        assertEquals(mapOf(TypeParameter("T") to TypeParameter("U")), result[1].typeParameterMapping)
    }

    @Test
    fun `unresolved signature has an unresolved compatibility fact`() {
        val specific = Declaration("specific", parameterTypes = listOf(null))
        val common = Declaration("common", parameterTypes = listOf("Int64"))
        val context = Context()

        val result = AbstractCjmpMatcher.match(specific, common, context)

        assertEquals(CjmpMatchResult.TypeNotResolved, result)
    }

    @Test
    fun `a concrete String return is not deferred against Int64`() {
        val specific = Declaration("specific", returnType = "String")
        val common = Declaration("common", returnType = "Int64")
        val context = Context()

        val result = AbstractCjmpMatcher.matchSpecificAgainstPotentialCommon(
            specific = specific,
            commonCandidates = listOf(common),
            context = context,
        )

        assertSame(common, result.single().common)
        assertEquals(CjmpMatchResult.Mismatched(CjmpMismatchKind.FUNCTION_TYPE), result.single().compatibility)
    }

    @Test
    fun `unresolved return type has an unresolved compatibility fact`() {
        val specific = Declaration("specific", returnType = null)
        val common = Declaration("common", returnType = "Int64")
        val context = Context()

        val result = AbstractCjmpMatcher.matchSpecificAgainstPotentialCommon(
            specific = specific,
            commonCandidates = listOf(common),
            context = context,
        )

        assertEquals(CjmpMatchResult.TypeNotResolved, result.single().compatibility)
        assertSame(common, result.single().common)
    }

    @Test
    fun `a missing common return fact is not a true Quest`() {
        val specific = Declaration("specific", returnType = "String")
        val common = Declaration("common", returnType = null)
        val context = Context()

        val result = AbstractCjmpMatcher.matchSpecificAgainstPotentialCommon(
            specific = specific,
            commonCandidates = listOf(common),
            context = context,
        )

        assertEquals(CjmpMatchResult.TypeNotResolved, result.single().compatibility)
        assertSame(common, result.single().common)
    }

    @Test
    fun `specific Quest does not hide recursively erroneous common return types`() {
        val specific = Declaration("specific", returnType = null, questReturnType = true)
        val common = Declaration("common", returnType = "Box<Error>")
        val context = Context()

        val result = AbstractCjmpMatcher.matchSpecificAgainstPotentialCommon(
            specific = specific,
            commonCandidates = listOf(common),
            context = context,
        )

        assertEquals(CjmpMatchResult.TypeNotResolved, result.single().compatibility)
        assertSame(common, result.single().common)
    }

    @Test
    fun `common Quest does not hide recursively erroneous specific return types`() {
        val specific = Declaration("specific", returnType = "Box<Error>")
        val common = Declaration("common", returnType = null, questReturnType = true)
        val context = Context()

        val result = AbstractCjmpMatcher.matchSpecificAgainstPotentialCommon(
            specific = specific,
            commonCandidates = listOf(common),
            context = context,
        )

        assertEquals(CjmpMatchResult.TypeNotResolved, result.single().compatibility)
        assertSame(common, result.single().common)
    }

    @Test
    fun `true specific Quest matches resolved Unit and concrete value returns`() {
        val specific = Declaration("specific", returnType = null, questReturnType = true)
        val commonCandidates = listOf(
            Declaration("common empty body", returnType = "Unit"),
            Declaration("common value body", returnType = "Int64"),
        )
        val facts = AbstractCjmpMatcher.matchSpecificAgainstPotentialCommon(specific, commonCandidates, Context())

        assertEquals(commonCandidates, facts.map { it.common })
        assertEquals(listOf(CjmpMatchResult.Matched, CjmpMatchResult.Matched), facts.map { it.compatibility })
    }

    @Test
    fun `true common Quest matches a resolved Unit specific return`() {
        val specific = Declaration("specific empty body", returnType = "Unit")
        val common = Declaration("common pending body", returnType = null, questReturnType = true)

        assertEquals(CjmpMatchResult.Matched, AbstractCjmpMatcher.match(specific, common, Context()))
    }

    @Test
    fun `two genuine Quest returns can match before their bodies are resolved`() {
        val specific = Declaration("specific pending body", returnType = null, questReturnType = true)
        val common = Declaration("common pending body", returnType = null, questReturnType = true)

        assertEquals(CjmpMatchResult.Matched, AbstractCjmpMatcher.match(specific, common, Context()))
    }

    @Test
    fun `resolved empty body Unit is concrete and incompatible with Int64`() {
        val specific = Declaration("specific empty body", returnType = "Unit")
        val common = Declaration("common value body", returnType = "Int64")

        assertEquals(
            CjmpMatchResult.Mismatched(CjmpMismatchKind.FUNCTION_TYPE),
            AbstractCjmpMatcher.match(specific, common, Context()),
        )
    }

    @Test
    fun `ordinary null return facts do not gain Quest compatibility from the other declaration`() {
        val missing = Declaration("missing return fact", returnType = null)
        val quest = Declaration("genuine Quest", returnType = null, questReturnType = true)

        assertEquals(CjmpMatchResult.TypeNotResolved, AbstractCjmpMatcher.match(missing, quest, Context()))
        assertEquals(CjmpMatchResult.TypeNotResolved, AbstractCjmpMatcher.match(quest, missing, Context()))
    }

    @Test
    fun `root error types take precedence over Quest in either direction`() {
        val erroneous = Declaration("invalid explicit type", returnType = "Error")
        val quest = Declaration("genuine Quest", returnType = null, questReturnType = true)

        assertEquals(CjmpMatchResult.TypeNotResolved, AbstractCjmpMatcher.match(erroneous, quest, Context()))
        assertEquals(CjmpMatchResult.TypeNotResolved, AbstractCjmpMatcher.match(quest, erroneous, Context()))
    }

    @Test
    fun `concrete return compatibility still uses the supplied generic substitution`() {
        val specific = Declaration("specific", returnType = "U")
        val common = Declaration("common", returnType = "T")

        assertEquals(
            CjmpMatchResult.Matched,
            AbstractCjmpMatcher.match(specific, common, Context(), mapOf(TypeParameter("T") to TypeParameter("U"))),
        )
    }

    @Test
    fun `candidate facts are independent of the order of specific requests`() {
        val first = Declaration("first", parameterTypes = listOf("Int64"))
        val second = Declaration("second", parameterTypes = listOf("Int64"))
        val candidates = listOf(Declaration("common1", parameterTypes = listOf("Int64")), Declaration("common2", parameterTypes = listOf("Int64")))
        val context = Context()
        val secondBeforeFirst = AbstractCjmpMatcher.matchSpecificAgainstPotentialCommon(second, candidates, context)
        val firstResult = AbstractCjmpMatcher.matchSpecificAgainstPotentialCommon(first, candidates, context)
        val secondAfterFirst = AbstractCjmpMatcher.matchSpecificAgainstPotentialCommon(second, candidates, context)
        assertEquals(secondBeforeFirst, secondAfterFirst)
        assertEquals(firstResult, secondAfterFirst)
        assertEquals(2, firstResult.count { it.compatibility == CjmpMatchResult.Matched })
    }

    @Test
    fun `implementation requirement stays separate from signature compatibility`() {
        val specific = Declaration("abstract", requiresImplementation = true)
        val common = Declaration("default")
        val facts = AbstractCjmpMatcher.matchSpecificAgainstPotentialCommon(specific, listOf(common), Context())
        assertEquals(CjmpMatchResult.Matched, facts.single().compatibility)
        assertTrue(facts.single().requiresImplementation)
    }

    @Test
    fun `type parameter mappings are snapshots of caller input`() {
        val mapping = mutableMapOf(TypeParameter("T") to TypeParameter("U"))
        val specific = Declaration("specific", parameterTypes = listOf("U"))
        val common = Declaration("common", parameterTypes = listOf("T"))
        val facts = AbstractCjmpMatcher.matchSpecificAgainstPotentialCommon(specific, listOf(common), Context(), mapping)
        mapping.clear()
        assertEquals(mapOf(TypeParameter("T") to TypeParameter("U")), facts.single().typeParameterMapping)
    }
}
