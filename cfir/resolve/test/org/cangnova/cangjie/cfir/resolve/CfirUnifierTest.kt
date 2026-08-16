@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.resolve.inference.ConstraintSystemTestHarness
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 约束系统统一语义测试。
 *
 * 旧 [CfirUnifier] 独立统一器已随 K2 架构移植删除，统一语义由
 * [org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl]
 * 的 incorporation 承担。本测试改为直接添加 subtype/equality 约束并断言传播结果。
 */
class CfirUnifierTest {

    /**
     * 测试使用的约束系统位置。
     */
    private val position = SimpleConstraintSystemConstraintPosition

    /**
     * 验证 identical 类型统一成功。
     */
    @Test
    fun `unify identical types succeeds`() {
        val system = ConstraintSystemTestHarness.newSystem()

        system.addSubtypeConstraint(ConePrimitiveType.INT32, ConePrimitiveType.INT32, position)

        assertTrue(system.errors.isEmpty())
    }

    /**
     * 验证具体类型统一到 placeholder 会增加变量下界。
     */
    @Test
    fun `unify concrete with placeholder adds lower bound`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val variable = ConstraintSystemTestHarness.newVariable(system, "T")

        system.addSubtypeConstraint(ConePrimitiveType.INT32, variable.defaultType, position)

        assertTrue(ConstraintSystemTestHarness.lowerBoundsOf(system, variable).contains(ConePrimitiveType.INT32))
    }

    /**
     * 验证 placeholder 统一到具体类型会增加变量上界。
     */
    @Test
    fun `unify placeholder with concrete adds upper bound`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val variable = ConstraintSystemTestHarness.newVariable(system, "T")

        system.addSubtypeConstraint(variable.defaultType, ConePrimitiveType.INT32, position)

        assertTrue(ConstraintSystemTestHarness.upperBoundsOf(system, variable).contains(ConePrimitiveType.INT32))
    }

    /**
     * 验证函数类型统一会按参数逆变和返回协变传播约束。
     */
    @Test
    fun `unify function types with contravariant params`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val variable = ConstraintSystemTestHarness.newVariable(system, "T")

        // (Int32) -> T  <:  (T) -> Int64
        val argFn = ConeFunctionType(listOf(ConePrimitiveType.INT32), variable.defaultType)
        val paramFn = ConeFunctionType(listOf(variable.defaultType), ConePrimitiveType.INT64)

        system.addSubtypeConstraint(argFn, paramFn, position)

        // 参数逆变：T <: Int32；返回协变：T <: Int64
        assertTrue(ConstraintSystemTestHarness.upperBoundsOf(system, variable).isNotEmpty())
    }

    /**
     * 验证 tuple 类型统一按元素逐项分解。
     */
    @Test
    fun `unify tuple types element by element`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val variable = ConstraintSystemTestHarness.newVariable(system, "T")

        val argTuple = ConeTupleType(listOf(ConePrimitiveType.INT32, ConePrimitiveType.BOOLEAN))
        val paramTuple = ConeTupleType(listOf(variable.defaultType, ConePrimitiveType.BOOLEAN))

        system.addSubtypeConstraint(argTuple, paramTuple, position)

        // tuple 协变：T 的下界含 Int32
        assertTrue(ConstraintSystemTestHarness.lowerBoundsOf(system, variable).contains(ConePrimitiveType.INT32))
    }

    /**
     * 验证 intersection 参数类型可以分解并统一成功。
     */
    @Test
    fun `unify intersection param type`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val variable = ConstraintSystemTestHarness.newVariable(system, "T")

        // T <: A & B → 上界整体记录为交叉类型
        val inter = ConeIntersectionType(listOf(ConePrimitiveType.INT32, ConePrimitiveType.BOOLEAN))

        system.addSubtypeConstraint(variable.defaultType, inter, position)

        assertTrue(ConstraintSystemTestHarness.upperBoundsOf(system, variable).contains(inter))
    }

    /**
     * 验证 ideal int 与具体整数统一成功。
     */
    @Test
    fun `unify ideal int with concrete int succeeds`() {
        val system = ConstraintSystemTestHarness.newSystem()

        system.addSubtypeConstraint(ConePrimitiveType.IDEAL_INT, ConePrimitiveType.INT32, position)

        assertTrue(!system.hasContradiction)
    }
}