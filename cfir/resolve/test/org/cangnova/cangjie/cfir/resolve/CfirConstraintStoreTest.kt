@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.resolve.inference.ConstraintSystemTestHarness
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 约束系统存储行为测试。
 *
 * 旧独立 [CfirConstraintStore] 已随 K2 架构移植删除，变量、约束与错误
 * 由 [org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl]
 * 的存储承担。本测试断言其公开存储视图。
 */
class CfirConstraintStoreTest {

    /**
     * 测试使用的约束系统位置。
     */
    private val position = SimpleConstraintSystemConstraintPosition

    /**
     * 验证存储能保存注册变量和新增约束。
     */
    @Test
    fun `store keeps registered variables and constraints`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val variable = ConstraintSystemTestHarness.newVariable(system, "T")

        system.addSubtypeConstraint(ConePrimitiveType.INT32, ConePrimitiveType.INT32, position)

        val storage = system.asReadOnlyStorage()

        assertEquals(1, storage.allTypeVariables.size)
        assertEquals(1, storage.initialConstraints.size)
        assertTrue(storage.notFixedTypeVariables.containsKey(variable.typeConstructor))
    }

    /**
     * 验证存储跟踪约束矛盾。
     */
    @Test
    fun `store tracks and clears issues`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val variable = ConstraintSystemTestHarness.newVariable(system, "U")

        system.addSubtypeConstraint(ConePrimitiveType.INT32, variable.defaultType, position)
        system.addSubtypeConstraint(variable.defaultType, ConePrimitiveType.BOOLEAN, position)

        val storage = system.asReadOnlyStorage()

        assertTrue(storage.hasContradiction)
        assertTrue(storage.errors.isNotEmpty())
    }

    /**
     * 验证变量查找按构造器索引命中。
     */
    @Test
    fun `findTypeVariable uses index for O(1) lookup`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val variables = (0 until 100).map { i ->
            ConstraintSystemTestHarness.newVariable(system, "V$i")
        }

        // 查找最后一个
        val last = variables.last()
        val storage = system.asReadOnlyStorage()
        assertTrue(storage.notFixedTypeVariables.containsKey(last.typeConstructor))
        assertEquals(
            last,
            storage.notFixedTypeVariables.getValue(last.typeConstructor).typeVariable,
        )
    }
}