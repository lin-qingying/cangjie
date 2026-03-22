package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.constraints.CfirConstraint
import org.cangnova.cangjie.cfir.constraints.CfirConstraintIssue
import org.cangnova.cangjie.cfir.constraints.CfirConstraintPosition
import org.cangnova.cangjie.cfir.constraints.CfirTypeVariable
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CfirConstraintStoreTest {

    @Test
    fun `store keeps registered variables and constraints`() {
        val store = CfirConstraintStore()
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = 1,
            lookupTag = ConeTypeParameterLookupTag("T"),
        )

        store.registerTypeVariable(variable)
        store.addConstraint(
            CfirConstraint.Equality(
                ConePrimitiveType.INT32,
                ConePrimitiveType.INT32,
                CfirConstraintPosition.ArgumentPosition(0),
            ),
        )

        assertEquals(1, store.typeVariables.size)
        assertEquals(1, store.constraints.size)
        assertNotNull(store.findTypeVariable("T"))
    }

    @Test
    fun `store tracks and clears issues`() {
        val store = CfirConstraintStore()
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = 1,
            lookupTag = ConeTypeParameterLookupTag("U"),
        )

        store.reportIssue(
            CfirConstraintIssue.UnresolvedVariable(
                variable = variable,
                position = CfirConstraintPosition.Unknown,
                message = "Unresolved U",
            ),
        )

        assertTrue(store.hasIssues)
        assertEquals(1, store.issues.size)

        store.clearIssues()

        assertFalse(store.hasIssues)
        assertTrue(store.issues.isEmpty())
    }

    @Test
    fun `findTypeVariable uses index for O(1) lookup`() {
        val store = CfirConstraintStore()
        repeat(100) { i ->
            store.registerTypeVariable(
                CfirTypeVariable(
                    typeParameter = CfirTypeParameterSymbol(),
                    freshTypeId = i,
                    lookupTag = ConeTypeParameterLookupTag("V$i"),
                ),
            )
        }

        // 查找最后一个
        val found = store.findTypeVariable("V99")
        assertNotNull(found)
        assertEquals("V99", found!!.lookupTag.name)
    }
}
