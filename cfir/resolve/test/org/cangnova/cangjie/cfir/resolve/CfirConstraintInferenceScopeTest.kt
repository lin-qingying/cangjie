@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.resolve.inference.ConstraintSystemTestHarness
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTypeVariable
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.runTransaction
import org.cangnova.cangjie.type.model.TypeVariableInferenceScope
import org.cangnova.cangjie.type.model.TypeVariableWithInferenceScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 合并调用系统后，约束传播、局部依赖来源及事务回滚必须同时成立。 */
class CfirConstraintInferenceScopeTest {
    private val position = SimpleConstraintSystemConstraintPosition

    @Test
    fun `nested equality constraints converge without losing their concrete type`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val variables = List(4) { index -> variable(system, "T$index", TypeVariableInferenceScope()) }
        for ((index, variable) in variables.withIndex()) {
            system.addEqualityConstraint(variable.defaultType, ConePrimitiveType.INT64, position)
            if (index > 0) system.addEqualityConstraint(variable.defaultType, variables[index - 1].defaultType, position)
        }
        val counts = variables.map { ConstraintSystemTestHarness.constraintsOf(system, it).size }
        repeat(3) {
            variables.zipWithNext().forEach { (left, right) ->
                system.addEqualityConstraint(right.defaultType, left.defaultType, position)
            }
        }
        assertFalse(system.hasContradiction)
        assertEquals(counts, variables.map { ConstraintSystemTestHarness.constraintsOf(system, it).size })
        for (variable in variables) {
            assertTrue(ConstraintSystemTestHarness.constraintsOf(system, variable).any {
                it.kind.isEqual() && it.type == ConePrimitiveType.INT64
            })
        }
    }

    @Test
    fun `local proof added after a cross-call proof is retained and deduplicated`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val scope = TypeVariableInferenceScope()
        val a = variable(system, "A", scope)
        val b = variable(system, "B", scope)
        val nested = variable(system, "T", TypeVariableInferenceScope())
        system.addSubtypeConstraint(a.defaultType, nested.defaultType, position)
        system.addSubtypeConstraint(nested.defaultType, b.defaultType, position)
        assertTrue(ConstraintSystemTestHarness.constraintsOf(system, a).any {
            it.type == b.defaultType && !it.isLocalToInferenceScope
        })

        system.addSubtypeConstraint(a.defaultType, b.defaultType, position)
        val count = ConstraintSystemTestHarness.constraintsOf(system, a).size
        repeat(3) { system.addSubtypeConstraint(a.defaultType, b.defaultType, position) }
        val constraints = ConstraintSystemTestHarness.constraintsOf(system, a)
        assertEquals(count, constraints.size)
        assertTrue(constraints.any { it.type == b.defaultType && it.isLocalToInferenceScope })
        assertFalse(system.hasContradiction)
    }

    @Test
    fun `failed candidate transaction does not publish its local proof`() {
        val system = ConstraintSystemTestHarness.newSystem()
        val scope = TypeVariableInferenceScope()
        val a = variable(system, "A", scope)
        val b = variable(system, "B", scope)
        val nested = variable(system, "T", TypeVariableInferenceScope())
        system.addSubtypeConstraint(a.defaultType, nested.defaultType, position)
        system.addSubtypeConstraint(nested.defaultType, b.defaultType, position)
        val before = ConstraintSystemTestHarness.constraintsOf(system, a).toList()
        system.runTransaction {
            addSubtypeConstraint(a.defaultType, b.defaultType, position)
            false
        }
        assertEquals(before, ConstraintSystemTestHarness.constraintsOf(system, a))
        assertFalse(ConstraintSystemTestHarness.constraintsOf(system, a).any {
            it.type == b.defaultType && it.isLocalToInferenceScope
        })
    }

    /** 使用真实 Cone 变量和约束系统，只为测试显式指定各次实例化身份。 */
    private fun variable(system: ConstraintSystemImpl, name: String, scope: TypeVariableInferenceScope): ConeTypeVariable =
        object : ConeTypeVariable(name), TypeVariableWithInferenceScope {
            override val inferenceScope: TypeVariableInferenceScope = scope
        }.also(system::registerVariable)
}
