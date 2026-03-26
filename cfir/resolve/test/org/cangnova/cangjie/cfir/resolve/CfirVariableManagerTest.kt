package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class CfirVariableManagerTest {

    @Test
    fun `placeholder allocation gets fresh ids`() {
        val manager = CfirVariableManager()
        val first = manager.allocatePlaceholder(CfirTypeParameterSymbol(), "T")
        val second = manager.allocatePlaceholder(CfirTypeParameterSymbol(), "U")

        assertNotEquals(first.freshTypeId, second.freshTypeId)
    }

    @Test
    fun `instantiation allocation preserves lookup name`() {
        val manager = CfirVariableManager()
        val variable = manager.allocateInstantiationVariable(CfirTypeParameterSymbol(), "T")

        assertEquals("T", variable.lookupTag.name)
    }

    @Test
    fun `deferred boundary allocation preserves lookup name`() {
        val manager = CfirVariableManager()
        val variable = manager.allocateDeferredBoundaryVariable(CfirTypeParameterSymbol(), "R")

        assertEquals("R", variable.lookupTag.name)
    }
}
