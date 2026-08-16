@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.resolve.inference.ConstraintSystemTestHarness
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 约束系统变量依赖图测试。
 *
 * 旧独立 [CfirConstraintGraph] 已随 K2 架构移植删除，依赖信息由
 * [org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl]
 * 的 [org.cangnova.cangjie.resolve.calls.inference.ConstraintSystem.asReadOnlyStorage]
 * 暴露的 [org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage.typeVariableDependencies]
 * 承担。本测试直接断言该依赖图。
 */
class CfirConstraintGraphTest {

    /**
     * 测试使用的约束系统位置。
     */
    private val position = SimpleConstraintSystemConstraintPosition

    /**
     * 验证互不依赖的变量之间没有依赖边。
     */
    @Test
    fun `independent variables returned in single batch`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val t = ConstraintSystemTestHarness.newVariable(system, "T")
        val u = ConstraintSystemTestHarness.newVariable(system, "U")

        system.addSubtypeConstraint(ConePrimitiveType.INT32, t.defaultType, position)
        system.addSubtypeConstraint(ConePrimitiveType.BOOLEAN, u.defaultType, position)

        val dependencies = system.asReadOnlyStorage().typeVariableDependencies
        assertFalse(dependencies[t.typeConstructor]?.contains(u.typeConstructor) ?: false)
        assertFalse(dependencies[u.typeConstructor]?.contains(t.typeConstructor) ?: false)
    }

    /**
     * 验证引用了其他变量的变量会记录依赖边。
     */
    @Test
    fun `dependent variable returned after dependency`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val t = ConstraintSystemTestHarness.newVariable(system, "T")
        val u = ConstraintSystemTestHarness.newVariable(system, "U")

        // U <: T → U 的约束引用 T，U 依赖 T
        system.addSubtypeConstraint(u.defaultType, t.defaultType, position)

        val dependencies = system.asReadOnlyStorage().typeVariableDependencies
        assertTrue(dependencies[u.typeConstructor]?.contains(t.typeConstructor) ?: false)
    }

    /**
     * 验证链式依赖 T-U-V 的依赖边完整记录。
     */
    @Test
    fun `chain dependency T-U-V solved in order`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val t = ConstraintSystemTestHarness.newVariable(system, "T")
        val u = ConstraintSystemTestHarness.newVariable(system, "U")
        val v = ConstraintSystemTestHarness.newVariable(system, "V")

        // T <: U，U <: V → U 依赖 T，V 依赖 U
        system.addSubtypeConstraint(t.defaultType, u.defaultType, position)
        system.addSubtypeConstraint(u.defaultType, v.defaultType, position)

        val dependencies = system.asReadOnlyStorage().typeVariableDependencies
        assertTrue(dependencies[u.typeConstructor]?.contains(t.typeConstructor) ?: false)
        assertTrue(dependencies[v.typeConstructor]?.contains(u.typeConstructor) ?: false)
    }

    /**
     * 验证未固定变量保留在存储中并携带自身构造器。
     */
    @Test
    fun `unfixed variables stay in storage keyed by constructor`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val t = ConstraintSystemTestHarness.newVariable(system, "T")

        system.addSubtypeConstraint(ConePrimitiveType.INT32, t.defaultType, position)

        val storage = system.asReadOnlyStorage()
        val marker: TypeConstructorMarker = t.typeConstructor
        assertTrue(storage.notFixedTypeVariables.containsKey(marker))
        assertEquals(t, storage.notFixedTypeVariables.getValue(marker).typeVariable)
    }
}