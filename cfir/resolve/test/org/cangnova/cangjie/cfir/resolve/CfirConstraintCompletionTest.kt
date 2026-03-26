package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.constraints.CfirConstraintIssue
import org.cangnova.cangjie.cfir.constraints.CfirTypeVariable
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTypeContext
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CfirConstraintCompletionTest {

    private val completion = CfirConstraintCompletion(CfirTypeRelations(CompletionTypeContext()))

    @Test
    fun `completion fixes variable from lower bounds`() {
        val store = CfirConstraintStore()
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = 1,
            lookupTag = ConeTypeParameterLookupTag("T"),
            lowerBounds = mutableListOf(ConePrimitiveType.INT32),
        )
        store.registerTypeVariable(variable)

        completion.complete(store)

        assertEquals(ConePrimitiveType.INT32, variable.fixedType)
        assertTrue(store.issues.isEmpty())
    }

    @Test
    fun `completion prefers lower bound when it fits upper bound`() {
        val store = CfirConstraintStore()
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = 2,
            lookupTag = ConeTypeParameterLookupTag("U"),
            lowerBounds = mutableListOf(ConePrimitiveType.INT32),
            upperBounds = mutableListOf(ConePrimitiveType.INT32),
        )
        store.registerTypeVariable(variable)

        completion.complete(store)

        assertEquals(ConePrimitiveType.INT32, variable.fixedType)
    }

    @Test
    fun `completion reports conflict before fixing`() {
        val store = CfirConstraintStore()
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = 3,
            lookupTag = ConeTypeParameterLookupTag("V"),
            lowerBounds = mutableListOf(ConePrimitiveType.INT32),
            upperBounds = mutableListOf(ConePrimitiveType.BOOLEAN),
        )
        store.registerTypeVariable(variable)

        completion.complete(store)

        assertTrue(store.issues.any { it is CfirConstraintIssue.ConflictingBounds })
        assertNull(variable.fixedType)
    }

    @Test
    fun `completion finalizes ideal int to Int64`() {
        val store = CfirConstraintStore()
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = 4,
            lookupTag = ConeTypeParameterLookupTag("T"),
            lowerBounds = mutableListOf(ConePrimitiveType.IDEAL_INT),
        )
        store.registerTypeVariable(variable)

        completion.complete(store)

        assertEquals(ConePrimitiveType.INT64, variable.fixedType)
    }

    @Test
    fun `completion finalizes ideal float to Float64`() {
        val store = CfirConstraintStore()
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = 5,
            lookupTag = ConeTypeParameterLookupTag("T"),
            lowerBounds = mutableListOf(ConePrimitiveType.IDEAL_FLOAT),
        )
        store.registerTypeVariable(variable)

        completion.complete(store)

        assertEquals(ConePrimitiveType.FLOAT64, variable.fixedType)
    }

    @Test
    fun `completion joins multiple lower bounds`() {
        val store = CfirConstraintStore()
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = 6,
            lookupTag = ConeTypeParameterLookupTag("T"),
            lowerBounds = mutableListOf(ConePrimitiveType.INT32, ConePrimitiveType.INT64),
        )
        store.registerTypeVariable(variable)

        completion.complete(store)

        // join(Int32, Int64) = Int64
        assertEquals(ConePrimitiveType.INT64, variable.fixedType)
    }

    @Test
    fun `completion meets multiple upper bounds`() {
        val store = CfirConstraintStore()
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = 7,
            lookupTag = ConeTypeParameterLookupTag("T"),
            upperBounds = mutableListOf(ConePrimitiveType.INT32, ConePrimitiveType.INT64),
        )
        store.registerTypeVariable(variable)

        completion.complete(store)

        // meet(Int32, Int64) = Int32
        assertEquals(ConePrimitiveType.INT32, variable.fixedType)
    }

    @Test
    fun `completion prefers non-ideal join over ideal`() {
        val store = CfirConstraintStore()
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = 8,
            lookupTag = ConeTypeParameterLookupTag("T"),
            lowerBounds = mutableListOf(ConePrimitiveType.IDEAL_INT),
            upperBounds = mutableListOf(ConePrimitiveType.INT32),
        )
        store.registerTypeVariable(variable)

        completion.complete(store)

        // ideal int lower + Int32 upper → meet gives Int32
        assertEquals(ConePrimitiveType.INT32, variable.fixedType)
    }
}

private class CompletionTypeContext : ConeTypeContext {
    override fun supertypes(type: ConeCangJieType): Collection<ConeCangJieType> = emptyList()

    override fun isSameTypeConstructor(a: ConeCangJieType, b: ConeCangJieType): Boolean = a == b
}
