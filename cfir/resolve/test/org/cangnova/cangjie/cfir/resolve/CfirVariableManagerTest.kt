package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

/**
 * [CfirVariableManager] 类型变量分配策略测试。
 */
class CfirVariableManagerTest {

    /**
     * 验证 placeholder 分配会生成单调递增的新 id。
     */
    @Test
    fun `placeholder allocation gets fresh ids`() {
        val manager = CfirVariableManager()
        val first = manager.allocatePlaceholder(CfirTypeParameterSymbol(), "T")
        val second = manager.allocatePlaceholder(CfirTypeParameterSymbol(), "U")

        assertNotEquals(first.freshTypeId, second.freshTypeId)
    }

    /**
     * 验证实例化变量保留原始 lookup name。
     */
    @Test
    fun `instantiation allocation preserves lookup name`() {
        val manager = CfirVariableManager()
        val variable = manager.allocateInstantiationVariable(CfirTypeParameterSymbol(), "T")

        assertEquals("T", variable.lookupTag.name)
    }

    /**
     * 验证 deferred boundary 变量保留原始 lookup name。
     */
    @Test
    fun `deferred boundary allocation preserves lookup name`() {
        val manager = CfirVariableManager()
        val variable = manager.allocateDeferredBoundaryVariable(CfirTypeParameterSymbol(), "R")

        assertEquals("R", variable.lookupTag.name)
    }
}
