@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.resolve.inference.ConstraintSystemTestHarness
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.resolve.calls.inference.model.ExpectedTypeConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl] 基础约束传播测试。
 *
 * 对位 Kotlin K2 约束系统测试：断言 incorporation 后的变量下界/上界约束与系统错误。
 */
class CfirConstraintSystemFoundationTest {

    /**
     * 测试使用的约束系统位置。
     */
    private val position = SimpleConstraintSystemConstraintPosition

    /**
     * 测试使用的 expected type 约束位置。
     */
    private val expectedTypePosition = object : ExpectedTypeConstraintPosition<Unit>(Unit) {}

    /**
     * 验证 subtype 约束会把实际类型记录为类型变量下界。
     */
    @Test
    fun `subtype constraint binds lower bound for type variable`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val variable = ConstraintSystemTestHarness.newVariable(system, "T")

        system.addSubtypeConstraint(ConePrimitiveType.INT32, variable.defaultType, position)

        assertEquals(
            listOf(ConePrimitiveType.INT32),
            ConstraintSystemTestHarness.lowerBoundsOf(system, variable),
        )
    }

    /**
     * 验证 class-like 类型相等时会继续分解并传播匹配的类型实参。
     */
    /**
     * 验证 expected type 约束按 subtype 规则传播。
     */
    @Test
    fun `expected type constraint propagates like subtype`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val variable = ConstraintSystemTestHarness.newVariable(system, "E")

        system.addSubtypeConstraint(
            ConePrimitiveType.INT32,
            variable.defaultType,
            expectedTypePosition,
        )

        assertEquals(
            listOf(ConePrimitiveType.INT32),
            ConstraintSystemTestHarness.lowerBoundsOf(system, variable),
        )
    }

    /**
     * 验证 upper bound 约束会写入变量上界。
     */
    @Test
    fun `upper bound constraint records bound on variable`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val variable = ConstraintSystemTestHarness.newVariable(system, "U")

        system.addSubtypeConstraint(variable.defaultType, ConePrimitiveType.INT32, position)

        assertEquals(
            listOf(ConePrimitiveType.INT32),
            ConstraintSystemTestHarness.upperBoundsOf(system, variable),
        )
    }

    /**
     * 验证兼容性检查失败时会记录不兼容错误。
     */
    @Test
    fun `compatible constraint records incompatibility issue when relation fails`() {
        val system = ConstraintSystemTestHarness.newSystem()

        system.addSubtypeConstraint(ConePrimitiveType.BOOLEAN, ConePrimitiveType.INT32, position)

        assertTrue(system.errors.isNotEmpty())
    }

    /**
     * 验证 expected type 不兼容时会报告不兼容错误。
     */
    @Test
    fun `expected type incompatible relation reports issue`() {
        val system = ConstraintSystemTestHarness.newSystem()

        system.addSubtypeConstraint(
            ConePrimitiveType.BOOLEAN,
            ConePrimitiveType.INT32,
            expectedTypePosition,
        )

        assertTrue(system.errors.isNotEmpty())
    }

    /**
     * 验证 id 函数调用场景的端到端类型变量约束。
     */
    @Test
    fun `end to end id function inference`() {
        // fun id<T>(x: T): T  调用 id(42)
        val system = ConstraintSystemTestHarness.newSystem()
        val variable = ConstraintSystemTestHarness.newVariable(system, "T")

        // 参数约束：Int64 <: T
        system.addSubtypeConstraint(ConePrimitiveType.INT64, variable.defaultType, position)

        assertTrue(ConstraintSystemTestHarness.lowerBoundsOf(system, variable).contains(ConePrimitiveType.INT64))
        assertTrue(system.errors.isEmpty())
    }

    /**
     * 验证函数类型参数位置的逆变传播会记录变量上界。
     */
    @Test
    fun `function type propagation with type variable`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val variable = ConstraintSystemTestHarness.newVariable(system, "T")

        // (Int32) -> Unit  <:  (T) -> Unit
        system.addSubtypeConstraint(
            ConeFunctionType(listOf(ConePrimitiveType.INT32), ConePrimitiveType.UNIT),
            ConeFunctionType(listOf(variable.defaultType), ConePrimitiveType.UNIT),
            position,
        )

        // 函数参数逆变：T <: Int32
        assertTrue(ConstraintSystemTestHarness.upperBoundsOf(system, variable).contains(ConePrimitiveType.INT32))
    }
}