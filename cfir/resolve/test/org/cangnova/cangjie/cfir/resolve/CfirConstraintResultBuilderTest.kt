package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.constraints.CfirConstraintIssue
import org.cangnova.cangjie.cfir.constraints.CfirConstraintPosition
import org.cangnova.cangjie.cfir.constraints.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.constraints.CfirTypeVariable
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CfirConstraintResultBuilderTest {

    @Test
    fun `result builder separates fixed and unresolved variables`() {
        val store = CfirConstraintStore()
        val fixed = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = 1,
            lookupTag = ConeTypeParameterLookupTag("T"),
        ).also {
            it.fixedType = ConePrimitiveType.INT32
            it.lowerBounds += ConePrimitiveType.INT32
        }
        val unresolved = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = 2,
            lookupTag = ConeTypeParameterLookupTag("U"),
        )

        store.registerTypeVariable(fixed)
        store.registerTypeVariable(unresolved)

        val result = CfirConstraintResultBuilder().build(store)

        assertEquals(listOf(fixed), result.fixedVariables)
        assertEquals(listOf(unresolved), result.unresolvedVariables)
        assertTrue(result.substitutor is CfirTypeSubstitutorByMap)
        assertEquals(0, result.constraintCount)
        assertEquals(2, result.typeVariableCount)
        assertTrue(!result.hasErrors)
        assertTrue(!result.isFullyResolved)
        assertEquals(listOf("Int32"), result.lowerBoundsByVariable["T"])
        assertEquals(emptyList<String>(), result.upperBoundsByVariable["T"])
        assertTrue(result.issueCountByKind.isEmpty())
        assertEquals(listOf("Int32"), result.lowerBoundsOf("T"))
        assertEquals(emptyList<String>(), result.upperBoundsOf("T"))
    }

    @Test
    fun `result builder counts issues by kind`() {
        val store = CfirConstraintStore()
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = 1,
            lookupTag = ConeTypeParameterLookupTag("E"),
        )
        store.registerTypeVariable(variable)
        store.reportIssue(
            CfirConstraintIssue.UnresolvedVariable(
                variable = variable,
                position = CfirConstraintPosition.Unknown,
                message = "Unresolved E",
            ),
        )

        val result = CfirConstraintResultBuilder().build(store)

        assertEquals(1, result.issueCountByKind["UnresolvedVariable"])
        assertEquals(1, result.issueCountOf("UnresolvedVariable"))
        assertTrue(result.hasErrors)
    }

    @Test
    fun `result builder finalizes ideal types in substitution`() {
        val store = CfirConstraintStore()
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = 1,
            lookupTag = ConeTypeParameterLookupTag("T"),
        ).also {
            it.fixedType = ConePrimitiveType.IDEAL_INT
        }
        store.registerTypeVariable(variable)

        val result = CfirConstraintResultBuilder().build(store)

        // ideal int 应被终结化为 Int64
        val substituted = result.substitutor.substituteOrSelf(
            org.cangnova.cangjie.cfir.types.ConeTypeParameterType(ConeTypeParameterLookupTag("T")),
        )
        assertEquals(ConePrimitiveType.INT64, substituted)
    }
}
