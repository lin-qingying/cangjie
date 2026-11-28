@file:OptIn(org.cangnova.cangjie.util.PrivateForInline::class)

package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.body.CfirBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzerContext
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.resolve.calls.ConeAtomWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** 完成实际返回值的结构依赖，同时保留共享系统中不属于此调用的变量及外层变量。 */
class CfirCompletionBoundaryVariablesTest {
    @Test
    fun nestedReturnDependencyIsSolvedWithoutFixingAnUnrelatedSharedVariable() {
        val f = Fixture()
        val result = ConstraintSystemTestHarness.newVariable(f.system, "Result")
        val element = ConstraintSystemTestHarness.newVariable(f.system, "Element")
        val unrelated = ConstraintSystemTestHarness.newVariable(f.system, "Unrelated")
        val tuple = ConeTupleType(listOf(element.defaultType, ConePrimitiveType.BOOLEAN))
        f.system.addSubtypeConstraint(tuple, result.defaultType, SimpleConstraintSystemConstraintPosition)
        f.system.addSubtypeConstraint(ConePrimitiveType.INT64, element.defaultType, SimpleConstraintSystemConstraintPosition)
        f.system.addSubtypeConstraint(ConePrimitiveType.BOOLEAN, unrelated.defaultType, SimpleConstraintSystemConstraintPosition)

        val completed = f.complete(result.defaultType)

        assertEquals(ConePrimitiveType.INT64, completed.fixedTypeVariables[element.typeConstructor])
        assertEquals(ConeTupleType(listOf(ConePrimitiveType.INT64, ConePrimitiveType.BOOLEAN)),
            completed.fixedTypeVariables[result.typeConstructor])
        assertTrue(unrelated.typeConstructor in completed.notFixedTypeVariables)
        assertSame(unrelated, completed.allTypeVariables[unrelated.typeConstructor])
        assertTrue(completed.errors.isEmpty())
    }

    @Test
    fun structuralDependencyDoesNotPermitFixingItsActualOuterVariable() {
        val f = Fixture()
        val parent = f.session.inferenceComponents.createConstraintSystem()
        val outer = ConstraintSystemTestHarness.newVariable(parent, "Outer")
        parent.addSubtypeConstraint(ConePrimitiveType.INT64, outer.defaultType, SimpleConstraintSystemConstraintPosition)
        f.system.addOuterSystem(parent.captureStorage())
        val result = ConstraintSystemTestHarness.newVariable(f.system, "Result")
        f.system.addSubtypeConstraint(ConeTupleType(listOf(outer.defaultType, ConePrimitiveType.BOOLEAN)),
            result.defaultType, SimpleConstraintSystemConstraintPosition)

        val completed = f.complete(result.defaultType)

        assertTrue(outer.typeConstructor in completed.notFixedTypeVariables)
        assertTrue(result.typeConstructor in completed.notFixedTypeVariables)
        assertSame(outer, completed.allTypeVariables[outer.typeConstructor])
        assertTrue(parent.fixedTypeVariables.isEmpty())
        assertTrue(completed.errors.isEmpty())
    }

    @Test
    fun directTupleBoundaryPreservesActualConstructorAndLeavesUnrelatedVariable() {
        val f = Fixture()
        val element = ConstraintSystemTestHarness.newVariable(f.system, "Element")
        val unrelated = ConstraintSystemTestHarness.newVariable(f.system, "Element")
        f.system.addSubtypeConstraint(ConePrimitiveType.INT64, element.defaultType, SimpleConstraintSystemConstraintPosition)
        f.system.addSubtypeConstraint(ConePrimitiveType.BOOLEAN, unrelated.defaultType, SimpleConstraintSystemConstraintPosition)
        val completed = f.complete(ConeTupleType(listOf(element.defaultType, ConePrimitiveType.BOOLEAN)))
        assertEquals(ConePrimitiveType.INT64, completed.fixedTypeVariables[element.typeConstructor])
        assertSame(element, completed.allTypeVariables[element.typeConstructor])
        assertSame(unrelated, completed.allTypeVariables[unrelated.typeConstructor])
        assertTrue(unrelated.typeConstructor in completed.notFixedTypeVariables)
    }

    @Test
    fun directFunctionBoundaryVisitsSemanticReturnComponents() {
        val f = Fixture()
        val element = ConstraintSystemTestHarness.newVariable(f.system, "Element")
        f.system.addSubtypeConstraint(ConePrimitiveType.INT64, element.defaultType, SimpleConstraintSystemConstraintPosition)
        val completed = f.complete(org.cangnova.cangjie.cfir.types.ConeFunctionType(emptyList(), element.defaultType))
        assertEquals(ConePrimitiveType.INT64, completed.fixedTypeVariables[element.typeConstructor])
        assertSame(element, completed.allTypeVariables[element.typeConstructor])
        assertTrue(completed.errors.isEmpty())
    }

    private class Fixture {
        val session = CallResolutionTestFixtures.newTestSession()
        val system = session.inferenceComponents.createConstraintSystem()

        fun complete(returnType: org.cangnova.cangjie.cfir.types.ConeCangJieType): ConstraintSystemImpl {
            val info = CallResolutionTestFixtures.buildCallInfo(session, "make")
            val context = BodyResolveContext(ReturnTypeCalculator.Default, CfirDataFlowAnalyzerContext()).apply {
                file = info.containingFile
            }
            val candidate = Candidate(
                symbol = CallResolutionTestFixtures.buildFunctionSymbol(session, "make", returnType),
                dispatchReceiver = null,
                givenExtensionReceiver = null,
                explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                constraintSystemFactory = session.inferenceComponents.constraintSystemFactory,
                baseSystem = system.captureStorage(),
                callInfo = info,
                originScope = null,
                bodyResolveContext = context,
            ).apply {
                initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList())
                initializeArgumentMapping(emptyList(), linkedMapOf())
            }
            val transformer = CfirBodyResolveTransformer(session, ScopeSession(), outerBodyResolveContext = context)
            ConstraintSystemCompleter(transformer.components).complete(
                candidate.system.asConstraintSystemCompleterContext(),
                ConstraintSystemCompletionMode.PCLA_POSTPONED_CALL,
                listOf(ConeAtomWithCandidate(info.callSite as CfirExpression, candidate)),
                returnType,
                transformer.resolutionContext,
            ) { _, _ -> error("The boundary fixture contains no postponed arguments") }
            return candidate.system
        }
    }
}
