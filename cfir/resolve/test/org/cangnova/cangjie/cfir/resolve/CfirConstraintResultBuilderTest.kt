@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.resolve.inference.ConstraintSystemTestHarness
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.cangnova.cangjie.type.model.safeSubstitute
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 约束求解结果状态测试。
 *
 * 旧 [CfirConstraintResultBuilder] 独立结果构造器已随 K2 架构移植删除，
 * 结果状态由 [org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl]
 * 的存储（[org.cangnova.cangjie.resolve.calls.inference.ConstraintSystem.asReadOnlyStorage]）
 * 与当前替换器（[org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilder.buildCurrentSubstitutor]）
 * 承担。本测试断言这些状态。
 */
class CfirConstraintResultBuilderTest {

    /**
     * 测试使用的约束系统位置。
     */
    private val position = SimpleConstraintSystemConstraintPosition

    /**
     * 验证存储区分已注册变量和未解析变量。
     */
    @Test
    fun `result builder separates fixed and unresolved variables`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val fixed = ConstraintSystemTestHarness.newVariable(system, "T")
        val unresolved = ConstraintSystemTestHarness.newVariable(system, "U")

        // T 有下界约束，U 没有任何约束
        system.addSubtypeConstraint(ConePrimitiveType.INT32, fixed.defaultType, position)

        val storage = system.asReadOnlyStorage()

        assertEquals(2, storage.allTypeVariables.size)
        assertEquals(2, storage.notFixedTypeVariables.size)
        assertTrue(storage.fixedTypeVariables.isEmpty())
        assertTrue(!system.hasContradiction)
        assertEquals(0, system.errors.size)
        assertTrue(storage.notFixedTypeVariables.containsKey(fixed.typeConstructor))
        assertTrue(storage.notFixedTypeVariables.containsKey(unresolved.typeConstructor))
    }

    /**
     * 验证冲突约束会记录系统错误。
     */
    @Test
    fun `result builder counts issues by kind`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val variable = ConstraintSystemTestHarness.newVariable(system, "E")

        system.addSubtypeConstraint(ConePrimitiveType.INT32, variable.defaultType, position)
        system.addSubtypeConstraint(variable.defaultType, ConePrimitiveType.BOOLEAN, position)

        assertTrue(system.hasContradiction)
        assertTrue(system.errors.isNotEmpty())
    }

    /**
     * 验证未固定变量的当前替换器保持原样。
     */
    @Test
    fun `unfixed variable is not substituted by current substitutor`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val variable = ConstraintSystemTestHarness.newVariable(system, "T")

        val substituted = with(system) {
            system.buildCurrentSubstitutor().safeSubstitute(variable.defaultType)
        }

        assertEquals(variable.defaultType, substituted)
    }
}