@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [ConstraintSystemImpl] 在 inference 包中的约束传播测试。
 *
 * 对位 Kotlin K2 约束系统测试：约束添加后由 incorporation 立即传播，
 * 直接断言变量下界/上界约束与系统错误，不依赖需要完整 session 的 completion。
 */
class CfirConstraintSystemImplTest {

    /**
     * 测试使用的约束系统位置。
     */
    private val position = SimpleConstraintSystemConstraintPosition

    /**
     * 验证函数参数逆变位置可以参与类型变量推断。
     */
    @Test
    fun `function subtype constraints should infer from contravariant parameter`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val t = ConstraintSystemTestHarness.newVariable(system, "T")

        // (Int32) -> Int32  <:  (T) -> Int32，参数逆变 → T <: Int32
        val sub = ConeFunctionType(
            parameterTypes = listOf(ConePrimitiveType.INT32),
            returnType = ConePrimitiveType.INT32,
        )
        val sup = ConeFunctionType(
            parameterTypes = listOf(t.defaultType),
            returnType = ConePrimitiveType.INT32,
        )

        system.addSubtypeConstraint(sub, sup, position)

        assertTrue(ConstraintSystemTestHarness.upperBoundsOf(system, t).contains(ConePrimitiveType.INT32))
        assertTrue(system.errors.isEmpty())
    }

    /**
     * 验证变量固定按依赖顺序传播。
     */
    @Test
    fun `fixation should respect variable dependency order`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val t = ConstraintSystemTestHarness.newVariable(system, "T")
        val u = ConstraintSystemTestHarness.newVariable(system, "U")

        system.addSubtypeConstraint(u.defaultType, t.defaultType, position)
        system.addSubtypeConstraint(ConePrimitiveType.INT32, u.defaultType, position)

        // Int32 <: U 且 U <: T → U 和 T 的下界都含 Int32
        assertTrue(ConstraintSystemTestHarness.lowerBoundsOf(system, u).contains(ConePrimitiveType.INT32))
        assertTrue(ConstraintSystemTestHarness.lowerBoundsOf(system, t).contains(ConePrimitiveType.INT32))
        assertTrue(system.errors.isEmpty())
    }

    /**
     * 验证冲突约束会记录矛盾。
     */
    @Test
    fun `conflicting constraints should be reported`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val t = ConstraintSystemTestHarness.newVariable(system, "T")

        system.addSubtypeConstraint(ConePrimitiveType.INT32, t.defaultType, position)
        system.addSubtypeConstraint(t.defaultType, ConePrimitiveType.BOOLEAN, position)

        assertTrue(system.hasContradiction)
    }
}