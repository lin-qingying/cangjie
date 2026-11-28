@file:OptIn(org.cangnova.cangjie.util.PrivateForInline::class, org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeFixVariableConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.inference.components.LegacyVariableReadinessCalculator
import org.cangnova.cangjie.resolve.calls.inference.components.TrivialConstraintTypeInferenceOracle
import org.cangnova.cangjie.resolve.calls.inference.components.VariableFixationFinder
import org.cangnova.cangjie.resolve.calls.inference.components.VariableReadinessCalculator
import org.cangnova.cangjie.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** 实际结构输出仍未固定时，原 readiness 应优先求出其输出，不从首个分支提前固定整体结果。 */
class CfirStructuralReturnReadinessTest {
    @Test
    fun secondFunctionReturnIsChosenBeforeWholeResult() {
        verifyRemainingOutputFirst { ConeFunctionType(emptyList(), it) }
    }

    @Test
    fun secondTupleComponentIsChosenBeforeWholeResult() {
        verifyRemainingOutputFirst { ConeTupleType(listOf(it, ConePrimitiveType.BOOLEAN)) }
    }

    private fun verifyRemainingOutputFirst(resultType: (ConeCangJieType) -> ConeCangJieType) {
        for (legacy in listOf(true, false)) {
            val session = ConstraintSystemTestHarness.newSession()
            val system = ConstraintSystemTestHarness.newSystem(session)
            val result = ConstraintSystemTestHarness.newVariable(system, "Result")
            val first = ConstraintSystemTestHarness.newVariable(system, "FirstReturn")
            val second = ConstraintSystemTestHarness.newVariable(system, "SecondReturn")
            system.addSubtypeConstraint(resultType(first.defaultType), result.defaultType, SimpleConstraintSystemConstraintPosition)
            system.addSubtypeConstraint(resultType(second.defaultType), result.defaultType, SimpleConstraintSystemConstraintPosition)
            system.addSubtypeConstraint(ConePrimitiveType.INT64, first.defaultType, SimpleConstraintSystemConstraintPosition)
            system.addSubtypeConstraint(ConePrimitiveType.BOOLEAN, second.defaultType, SimpleConstraintSystemConstraintPosition)
            // 对应实际 trace 的检查点：第一返回 VM 已由原系统固定，第二返回 VM 仍有合法 lower 输入。
            system.fixVariable(first, ConePrimitiveType.INT64, ConeFixVariableConstraintPosition(first))
            val oracle = TrivialConstraintTypeInferenceOracle.create(session.typeContext)
            val finder = VariableFixationFinder(session.languageVersionSettings,
                if (legacy) LegacyVariableReadinessCalculator(oracle, session.languageVersionSettings)
                else VariableReadinessCalculator(oracle, session.languageVersionSettings))
            with(system) {
                // 修正只改变优先级，不将整体结果的原 proper-constraint 许可关闭。
                assertTrue(finder.typeVariableHasProperConstraint(result.typeConstructor))
                val chosen = finder.findFirstVariableForFixation(listOf(result.typeConstructor, second.typeConstructor),
                    emptyList(), ConstraintSystemCompletionMode.FULL, result.defaultType)
                assertEquals(second.typeConstructor, chosen?.variable)
                assertTrue(chosen?.isReady == true)
            }
            assertSame(first, system.allTypeVariables[first.typeConstructor])
            assertSame(second, system.allTypeVariables[second.typeConstructor])
            assertTrue(result.typeConstructor in system.notFixedTypeVariables)
            assertTrue(second.typeConstructor in system.notFixedTypeVariables)
            assertTrue(system.errors.isEmpty())
        }
    }

    @Test
    fun topLevelVariableKeepsItsShallowDependencyPriority() {
        for (legacy in listOf(true, false)) {
            val session = ConstraintSystemTestHarness.newSession()
            val system = ConstraintSystemTestHarness.newSystem(session)
            val result = ConstraintSystemTestHarness.newVariable(system, "Result")
            val other = ConstraintSystemTestHarness.newVariable(system, "Other")
            system.addSubtypeConstraint(ConePrimitiveType.INT64, result.defaultType, SimpleConstraintSystemConstraintPosition)
            system.addSubtypeConstraint(other.defaultType, result.defaultType, SimpleConstraintSystemConstraintPosition)
            val oracle = TrivialConstraintTypeInferenceOracle.create(session.typeContext)
            val finder = VariableFixationFinder(session.languageVersionSettings,
                if (legacy) LegacyVariableReadinessCalculator(oracle, session.languageVersionSettings)
                else VariableReadinessCalculator(oracle, session.languageVersionSettings))
            with(system) {
                val chosen = finder.findFirstVariableForFixation(listOf(result.typeConstructor, other.typeConstructor),
                    emptyList(), ConstraintSystemCompletionMode.FULL, result.defaultType)
                assertEquals(result.typeConstructor, chosen?.variable)
                assertTrue(chosen?.isReady == true)
            }
            assertTrue(system.errors.isEmpty())
        }
    }
}
