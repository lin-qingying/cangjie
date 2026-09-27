package org.cangnova.cangjie.resolve.calls.mpp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class AbstractCjmpMatcherTest {
    private data class TypeParameter(val name: String) : CjmpTypeParameterSymbolMarker

    private data class Declaration(
        val label: String,
        val parameterTypes: List<String?> = emptyList(),
        val returnType: String? = "Unit",
        val typeParameters: List<TypeParameter> = emptyList(),
        val function: Boolean = true,
    ) : CjmpDeclarationSymbolMarker

    private class Context : CjmpMatchingContext<Declaration, TypeParameter, String> {
        val bindings = mutableListOf<Pair<Declaration, Declaration>>()
        val mismatches = mutableListOf<Triple<Declaration, CjmpMismatchKind, Declaration>>()
        val parameterMismatches = mutableListOf<Triple<Declaration, CjmpParameterMismatch, Declaration>>()

        override fun classLikeKind(declaration: Declaration): String? = null

        override fun isExtend(declaration: Declaration): Boolean = false

        override fun haveSameExtendKey(first: Declaration, second: Declaration): CjmpTypeCompatibility =
            CjmpTypeCompatibility.INCOMPATIBLE

        override fun callableKind(declaration: Declaration): String = "function"

        override fun isFunction(declaration: Declaration): Boolean = declaration.function

        override fun isConstructor(declaration: Declaration): Boolean = false

        override fun typeParameters(declaration: Declaration): List<TypeParameter> = declaration.typeParameters

        override fun valueParameters(declaration: Declaration): List<CjmpMatchingValueParameter<String>> =
            declaration.parameterTypes.mapIndexed { index, type ->
                CjmpMatchingValueParameter(type, isNamed = false, name = "p$index", hasDefaultValue = false)
            }

        override fun returnType(declaration: Declaration): String? = declaration.returnType

        override fun areTypesEquivalent(
            specific: String?,
            common: String?,
            typeParameterMapping: Map<TypeParameter, TypeParameter>,
        ): CjmpTypeCompatibility = compare(specific, common, typeParameterMapping)

        override fun isSubtypeOf(
            specific: String?,
            common: String?,
            typeParameterMapping: Map<TypeParameter, TypeParameter>,
        ): CjmpTypeCompatibility = compare(specific, common, typeParameterMapping)

        override fun needToReportMissingBody(common: Declaration, specific: Declaration): Boolean = false

        override fun trySetSpecificImplementation(
            specific: Declaration,
            common: Declaration,
            typeParameterMapping: Map<TypeParameter, TypeParameter>,
        ): Boolean {
            if (bindings.any { it.second == common }) return false
            bindings += specific to common
            return true
        }

        override fun recordMismatch(specific: Declaration, kind: CjmpMismatchKind, common: Declaration) {
            mismatches += Triple(specific, kind, common)
        }

        override fun recordParameterMismatch(
            specific: Declaration,
            mismatch: CjmpParameterMismatch,
            common: Declaration,
        ) {
            parameterMismatches += Triple(specific, mismatch, common)
        }

        private fun compare(
            specific: String?,
            common: String?,
            typeParameterMapping: Map<TypeParameter, TypeParameter>,
        ): CjmpTypeCompatibility {
            if (specific == null || common == null) return CjmpTypeCompatibility.UNRESOLVED
            val substitutedCommon = typeParameterMapping.entries
                .firstOrNull { (commonParameter, _) -> commonParameter.name == common }
                ?.value?.name ?: common
            return if (specific == substitutedCommon) {
                CjmpTypeCompatibility.COMPATIBLE
            } else {
                CjmpTypeCompatibility.INCOMPATIBLE
            }
        }
    }

    @Test
    fun `candidate matching applies parent type substitution and keeps first fit`() {
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

        assertSame(firstCompatibleCommon, result)
        assertEquals(listOf(specific to firstCompatibleCommon), context.bindings)
        assertEquals(listOf(Triple(specific, CjmpMismatchKind.PARAMETER_TYPES, incompatibleCommon)), context.mismatches)
    }

    @Test
    fun `unresolved signature is recorded and never bound`() {
        val specific = Declaration("specific", parameterTypes = listOf(null))
        val common = Declaration("common", parameterTypes = listOf("Int64"))
        val context = Context()

        val result = AbstractCjmpMatcher.match(specific, common, context)

        assertEquals(CjmpMatchResult.TypeNotResolved, result)
        assertEquals(emptyList<Pair<Declaration, Declaration>>(), context.bindings)
    }
}
