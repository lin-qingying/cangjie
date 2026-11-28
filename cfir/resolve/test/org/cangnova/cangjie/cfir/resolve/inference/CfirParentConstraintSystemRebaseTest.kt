@file:OptIn(org.cangnova.cangjie.util.PrivateForInline::class)

package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzerContext
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeFixVariableConstraintPosition
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTypeVariable
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.resolve.calls.inference.model.*
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.type.model.TypeVariableInferenceScope
import org.cangnova.cangjie.type.model.TypeVariableWithInferenceScope
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** 使用项目完整类型上下文，验证 Kotlin PCLA/CS 原始入口上的派生、合并、固定及事务行为。 */
class CfirParentConstraintSystemRebaseTest {
    private val position = SimpleConstraintSystemConstraintPosition
    private fun variable(system: ConstraintSystemImpl, name: String = "T") = ConstraintSystemTestHarness.newVariable(system, name)
    private fun bounds(system: ConstraintSystemImpl, t: ConeTypeVariable) = system.notFixedTypeVariables.getValue(t.typeConstructor).constraints
    private fun fix(system: ConstraintSystemImpl, t: ConeTypeVariable) = system.fixVariable(t, ConePrimitiveType.INT32, ConeFixVariableConstraintPosition(t))

    @Test
    fun `same keys import parent added and removed rows`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val t = variable(parent)
        val child = parent.fork(asOuter = true)
        val local = variable(child, "Local")
        child.addSubtypeConstraint(local.defaultType, ConePrimitiveType.INT32, position)
        parent.addSubtypeConstraint(ConePrimitiveType.INT32, t.defaultType, position)
        child.rebaseToOwner()
        assertTrue(bounds(child, t).any { it.kind.isLower() && it.type == ConePrimitiveType.INT32 })
        parent.removeConstraintsForVariable(t.typeConstructor) { it.kind.isLower() }
        child.rebaseToOwner()
        assertFalse(bounds(child, t).any { it.kind.isLower() })
        assertTrue(bounds(child, local).any { it.kind.isUpper() })
        assertSame(t, child.allTypeVariables[t.typeConstructor])
    }

    @Test
    fun `parent growth preserves distinct constructors and inferenceScopes`() {
        class ScopedVariable(override val inferenceScope: TypeVariableInferenceScope) :
            ConeTypeVariable("same"), TypeVariableWithInferenceScope
        val parent = ConstraintSystemTestHarness.newSystem()
        val first = ScopedVariable(TypeVariableInferenceScope())
        parent.registerVariable(first)
        val child = parent.fork(asOuter = true)
        val second = ScopedVariable(TypeVariableInferenceScope())
        parent.registerVariable(second)
        child.rebaseToOwner()
        assertSame(first, child.allTypeVariables[first.typeConstructor])
        assertSame(second, child.allTypeVariables[second.typeConstructor])
        assertSame(first.inferenceScope, (child.allTypeVariables[first.typeConstructor] as TypeVariableWithInferenceScope).inferenceScope)
        assertNotSame(first.typeConstructor, second.typeConstructor)
        assertEquals(2, child.outerSystemVariablesPrefixSize)
    }

    @Test
    fun `late parent fixation incorporates local relation without resurrection`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val t = variable(parent)
        val child = parent.fork(asOuter = true)
        val u = variable(child, "U")
        child.addSubtypeConstraint(t.defaultType, u.defaultType, position)
        parent.addSubtypeConstraint(ConePrimitiveType.INT32, t.defaultType, position)
        fix(parent, t)
        child.rebaseToOwner()
        assertEquals(ConePrimitiveType.INT32, child.fixedTypeVariables[t.typeConstructor])
        assertFalse(t.typeConstructor in child.notFixedTypeVariables)
        assertSame(t, child.allTypeVariables[t.typeConstructor])
        assertTrue(bounds(child, u).any { it.kind.isLower() && it.type == ConePrimitiveType.INT32 })
    }

    @Test
    fun `source fixation keeps initial constraint identity through commit`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val child = parent.fork()
        val local = variable(child)
        fix(child, local)
        val initial = child.initialConstraints.single()
        parent.integrateDerivedStorage(child.captureStorage())
        assertSame(initial, parent.initialConstraints.single())
        assertSame(local, parent.allTypeVariables[local.typeConstructor])
    }

    @Test
    fun `receiver removal uses real expression identity and keeps other receiver`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val t = variable(parent)
        val removed = CallResolutionTestFixtures.buildTypedExpression(ConePrimitiveType.INT32)
        val kept = CallResolutionTestFixtures.buildTypedExpression(ConePrimitiveType.INT32)
        val removedPosition = object : ReceiverConstraintPosition<CfirExpression>(removed) {}
        val keptPosition = object : ReceiverConstraintPosition<CfirExpression>(kept) {}
        parent.addSubtypeConstraint(t.defaultType, ConePrimitiveType.INT32, removedPosition)
        val child = parent.fork(asOuter = true)
        child.addSubtypeConstraint(t.defaultType, ConePrimitiveType.INT32, keptPosition)
        parent.removeConstraintsForVariable(t.typeConstructor) { (it.position.from as? ReceiverConstraintPosition<*>)?.argument === removed }
        child.rebaseToOwner()
        assertFalse(bounds(child, t).any { (it.position.from as? ReceiverConstraintPosition<*>)?.argument === removed })
        assertTrue(bounds(child, t).any { (it.position.from as? ReceiverConstraintPosition<*>)?.argument === kept })
        parent.integrateDerivedStorage(child.captureStorage())
        assertFalse(bounds(parent, t).any { (it.position.from as? ReceiverConstraintPosition<*>)?.argument === removed })
    }

    private fun fork(parent: ConstraintSystemImpl, t: ConeTypeVariable) {
        val initial = InitialConstraint(t.defaultType, t.defaultType, ConstraintKind.UPPER, position)
        parent.addInitialConstraint(initial)
        val incorporation = IncorporationConstraintPosition(initial)
        fun row(type: ConePrimitiveType) = Constraint(ConstraintKind.EQUALITY, type, incorporation, derivedFrom = emptySet(), isNoInfer = false)
        parent.constraintsFromAllForkPoints += incorporation to listOf(
            setOf(t to row(ConePrimitiveType.BOOLEAN)),
            setOf(t to row(ConePrimitiveType.INT32)),
        )
    }

    @Test
    fun `actual parent fork outcome replaces old private choice`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val t = variable(parent)
        fork(parent, t)
        val child = parent.fork(asOuter = true)
        child.resolveForkPointsConstraints()
        parent.addSubtypeConstraint(t.defaultType, ConePrimitiveType.INT32, position)
        parent.resolveForkPointsConstraints()
        child.rebaseToOwner()
        assertTrue(child.constraintsFromAllForkPoints.isEmpty())
        assertFalse(bounds(child, t).any { it.type == ConePrimitiveType.BOOLEAN })
        assertTrue(bounds(child, t).any { it.type == ConePrimitiveType.INT32 })
        assertFalse(child.hasContradiction)
        parent.integrateDerivedStorage(child.captureStorage())
        assertTrue(parent.constraintsFromAllForkPoints.isEmpty())
    }

    @Test
    fun `fork probe is pure and nested rollback restores removed raw rows`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val t = variable(parent)
        parent.addSubtypeConstraint(t.defaultType, ConePrimitiveType.INT32, position)
        fork(parent, t)
        val before = parent.captureStorage()
        val originalRow = parent.notFixedTypeVariables.getValue(t.typeConstructor)
        assertFalse(parent.hasContradiction)
        assertSame(originalRow, parent.notFixedTypeVariables.getValue(t.typeConstructor))
        assertEquals(before.notFixedTypeVariables.getValue(t.typeConstructor).constraints, bounds(parent, t))
        assertSame(before.constraintsFromAllForkPoints.single(), parent.constraintsFromAllForkPoints.single())
        val outer = parent.prepareTransaction()
        parent.removeConstraintsForVariable(t.typeConstructor) { true }
        val inner = parent.prepareTransaction()
        parent.addSubtypeConstraint(ConePrimitiveType.BOOLEAN, t.defaultType, position)
        inner.rollbackTransaction()
        assertTrue(bounds(parent, t).isEmpty())
        outer.rollbackTransaction()
        assertSame(originalRow, parent.notFixedTypeVariables.getValue(t.typeConstructor))
        assertEquals(before.notFixedTypeVariables.getValue(t.typeConstructor).constraints, bounds(parent, t))
        parent.resolveForkPointsConstraints()
        assertFalse(parent.hasContradiction)
    }

    @Test
    fun `private cannot fix active parent and failed commit is atomic`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val t = variable(parent)
        val child = parent.fork()
        fix(child, t)
        val before = parent.captureStorage()
        assertThrows(IllegalStateException::class.java) { parent.integrateDerivedStorage(child.captureStorage()) }
        assertEquals(before.notFixedTypeVariables.keys, parent.notFixedTypeVariables.keys)
        assertTrue(parent.fixedTypeVariables.isEmpty())
        assertSame(t, parent.allTypeVariables[t.typeConstructor])
    }

    @Test
    fun `baseline from reverted parent transaction cannot commit`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val transaction = parent.prepareTransaction()
        variable(parent)
        val child = parent.fork(asOuter = true)
        transaction.rollbackTransaction()
        assertThrows(IllegalStateException::class.java) { parent.integrateDerivedStorage(child.captureStorage()) }
        assertTrue(parent.allTypeVariables.isEmpty())
        assertTrue(parent.initialConstraints.isEmpty())
    }

    @Test
    fun `partial and closed successor forest is order independent and idempotent`() {
        val first = ConstraintSystemTestHarness.newSystem()
        val t = variable(first)
        first.addSubtypeConstraint(ConePrimitiveType.INT32, t.defaultType, position)
        val partial = first.captureStorage()
        val closed = first.fork()
        fix(closed, t)
        val finalStorage = closed.captureStorage()
        for (forest in listOf(listOf(partial, finalStorage), listOf(finalStorage, partial))) {
            val target = ConstraintSystemTestHarness.newSystem()
            target.integrateInferenceForest(forest, requireClosed = true)
            val count = target.initialConstraints.size
            target.integrateInferenceForest(forest, requireClosed = true)
            assertTrue(target.notFixedTypeVariables.isEmpty())
            assertEquals(ConePrimitiveType.INT32, target.fixedTypeVariables[t.typeConstructor])
            assertSame(t, target.allTypeVariables[t.typeConstructor])
            assertEquals(count, target.initialConstraints.size)
        }
    }

    @Test
    fun `uncovered partial and conflicting completed roots fail atomically`() {
        val first = ConstraintSystemTestHarness.newSystem()
        val t = variable(first)
        val target = ConstraintSystemTestHarness.newSystem()
        assertThrows(IllegalStateException::class.java) { target.integrateInferenceForest(listOf(first.captureStorage()), requireClosed = true) }
        val left = first.fork()
        val right = first.fork()
        fix(left, t)
        right.fixVariable(t, ConePrimitiveType.BOOLEAN, ConeFixVariableConstraintPosition(t))
        assertThrows(IllegalStateException::class.java) { target.integrateInferenceForest(listOf(left.captureStorage(), right.captureStorage()), requireClosed = true) }
        assertTrue(target.allTypeVariables.isEmpty())
    }

    @Test
    fun `captured outer Candidate rebases both lazy and later getter access`() {
        val session = ConstraintSystemTestHarness.newSession()
        val parent = ConstraintSystemTestHarness.newSystem(session)
        val t = variable(parent)
        val template = parent.fork(asOuter = true)
        val context = BodyResolveContext(ReturnTypeCalculator.Default, CfirDataFlowAnalyzerContext())
        val candidate = Candidate(
            symbol = CallResolutionTestFixtures.buildFunctionSymbol(session, "read"),
            dispatchReceiver = null, givenExtensionReceiver = null,
            explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            constraintSystemFactory = session.inferenceComponents.constraintSystemFactory,
            baseSystem = template.currentStorage(),
            callInfo = CallResolutionTestFixtures.buildCallInfo(session, "read"),
            originScope = null, bodyResolveContext = context,
        ).apply { initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList()) }
        parent.addSubtypeConstraint(ConePrimitiveType.INT32, t.defaultType, position)
        assertTrue(bounds(candidate.system, t).any { it.type == ConePrimitiveType.INT32 })
        fix(parent, t)
        assertEquals(ConePrimitiveType.INT32, candidate.system.fixedTypeVariables[t.typeConstructor])
        assertFalse(t.typeConstructor in candidate.system.notFixedTypeVariables)
    }

    @Test
    fun `PCLA child commit keeps current parent constraints`() {
        val session = ConstraintSystemTestHarness.newSession()
        val outer = CallResolutionTestFixtures.buildCandidate(
            session, CallResolutionTestFixtures.buildFunctionSymbol(session, "outer"),
            CallResolutionTestFixtures.buildCallInfo(session, "outer"),
        )
        val pcla = CfirPCLAInferenceSession(outer, session.inferenceComponents)
        val t = variable(pcla.currentCommonSystem)
        val child = pcla.currentCommonSystem.fork(asOuter = true)
        child.addSubtypeConstraint(t.defaultType, ConePrimitiveType.INT32, position)
        pcla.currentCommonSystem.addSubtypeConstraint(ConePrimitiveType.INT32, t.defaultType, position)
        pcla.integrateChildSession(emptyList(), child.captureStorage()) {}
        assertEquals(2, pcla.currentCommonSystem.initialConstraints.size)
        assertTrue(bounds(pcla.currentCommonSystem, t).any { it.kind.isEqual() })
        assertSame(t, pcla.currentCommonSystem.allTypeVariables[t.typeConstructor])
    }
    @Test
    fun `receiver tombstone preserves a later legitimate constraint from the same expression`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val t = variable(parent)
        val receiver = CallResolutionTestFixtures.buildTypedExpression(ConePrimitiveType.INT32)
        val receiverPosition = object : ReceiverConstraintPosition<CfirExpression>(receiver) {}
        parent.addSubtypeConstraint(t.defaultType, ConePrimitiveType.INT32, receiverPosition)
        val oldSource = parent.initialConstraints.single()
        val child = parent.fork(asOuter = true)
        parent.removeConstraintsForVariable(t.typeConstructor) {
            (it.position.from as? ReceiverConstraintPosition<*>)?.argument === receiver
        }
        parent.addSubtypeConstraint(t.defaultType, ConePrimitiveType.INT32, receiverPosition)
        val newSource = parent.initialConstraints.last()
        child.rebaseToOwner()
        assertTrue(bounds(child, t).any { it.position.initialConstraint === newSource })
        assertFalse(bounds(child, t).any { it.position.initialConstraint === oldSource })
        parent.integrateDerivedStorage(child.captureStorage())
        assertTrue(bounds(parent, t).any { it.position.initialConstraint === newSource })
    }

    @Test
    fun `derived row deletion survives rebase with unchanged constructor set`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val t = variable(parent)
        val u = variable(parent, "U")
        parent.addSubtypeConstraint(t.defaultType, u.defaultType, position)
        parent.addSubtypeConstraint(ConePrimitiveType.INT32, t.defaultType, position)
        assertTrue(bounds(parent, u).any { it.kind.isLower() && it.type == ConePrimitiveType.INT32 })
        val child = parent.fork(asOuter = true)
        val keys = child.allTypeVariables.keys.toSet()
        parent.removeConstraintsForVariable(u.typeConstructor) {
            it.kind.isLower() && it.type == ConePrimitiveType.INT32
        }
        child.rebaseToOwner()
        assertEquals(keys, child.allTypeVariables.keys)
        assertFalse(bounds(child, u).any { it.kind.isLower() && it.type == ConePrimitiveType.INT32 })
        parent.integrateDerivedStorage(child.captureStorage())
        assertFalse(bounds(parent, u).any { it.kind.isLower() && it.type == ConePrimitiveType.INT32 })
    }

    @Test
    fun `actual common system is stable during lambda completion and resumes synchronization`() {
        val session = ConstraintSystemTestHarness.newSession()
        val parent = ConstraintSystemTestHarness.newSystem(session)
        val t = variable(parent)
        val template = parent.fork(asOuter = true)
        val context = BodyResolveContext(ReturnTypeCalculator.Default, CfirDataFlowAnalyzerContext())
        val candidate = Candidate(
            symbol = CallResolutionTestFixtures.buildFunctionSymbol(session, "read"),
            dispatchReceiver = null, givenExtensionReceiver = null,
            explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            constraintSystemFactory = session.inferenceComponents.constraintSystemFactory,
            baseSystem = template.currentStorage(),
            callInfo = CallResolutionTestFixtures.buildCallInfo(session, "read"),
            originScope = null, bodyResolveContext = context,
        ).apply { initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList()) }
        val actual = candidate.system
        actual.withParentRebaseSuspended {
            val liveRow = actual.notFixedTypeVariables.getValue(t.typeConstructor)
            parent.addSubtypeConstraint(ConePrimitiveType.INT32, t.defaultType, position)
            assertSame(actual, candidate.system)
            assertSame(liveRow, candidate.system.notFixedTypeVariables.getValue(t.typeConstructor))
        }
        assertTrue(bounds(candidate.system, t).any { it.kind.isLower() && it.type == ConePrimitiveType.INT32 })
    }

    @Test
    fun `an unrelated closed root cannot conceal a partial with the same constructor key`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val t = variable(parent)
        val unrelated = ConstraintSystemTestHarness.newSystem()
        unrelated.registerVariable(t)
        fix(unrelated, t)
        val target = ConstraintSystemTestHarness.newSystem()
        assertThrows(IllegalStateException::class.java) {
            target.integrateInferenceForest(listOf(parent.captureStorage(), unrelated.captureStorage()), requireClosed = true)
        }
        assertTrue(target.allTypeVariables.isEmpty())
    }

    @Test
    fun `parent fixation cannot excuse an earlier illicit private fixation`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val t = variable(parent)
        val child = parent.fork()
        fix(child, t)
        fix(parent, t)
        val before = parent.captureStorage()
        assertThrows(IllegalStateException::class.java) { parent.integrateDerivedStorage(child.captureStorage()) }
        assertSame(before.initialConstraints.single(), parent.initialConstraints.single())
        assertFalse(t.typeConstructor in parent.notFixedTypeVariables)
    }

    @Test
    fun `rebasing a local constraint checks the latest fixed parent binding`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val t = variable(parent)
        val child = parent.fork(asOuter = true)
        child.addEqualityConstraint(t.defaultType, ConePrimitiveType.BOOLEAN, position)
        fix(parent, t)
        child.rebaseToOwner()
        assertEquals(ConePrimitiveType.INT32, child.fixedTypeVariables[t.typeConstructor])
        assertTrue(child.hasContradiction)
        assertFalse(t.typeConstructor in child.notFixedTypeVariables)
    }

    @Test
    fun `snapshot keeps initial row and constructor identity after live source mutation`() {
        val source = ConstraintSystemTestHarness.newSystem()
        val t = variable(source)
        source.addSubtypeConstraint(ConePrimitiveType.INT32, t.defaultType, position)
        val snapshot = source.captureStorage()
        val row = snapshot.notFixedTypeVariables.getValue(t.typeConstructor).constraints.single()
        source.removeConstraintsForVariable(t.typeConstructor) { true }
        assertSame(row, snapshot.notFixedTypeVariables.getValue(t.typeConstructor).constraints.single())
        assertSame(t, snapshot.allTypeVariables[t.typeConstructor])
    }

    @Test
    @OptIn(UnstableSystemMergeMode::class)
    fun `overlapping standard merge applies closed successor and ignores obsolete partial`() {
        val source = ConstraintSystemTestHarness.newSystem()
        val t = variable(source)
        source.addSubtypeConstraint(ConePrimitiveType.INT32, t.defaultType, position)
        val partial = source.captureStorage()
        val closed = source.fork()
        fix(closed, t)
        val target = ConstraintSystemTestHarness.newSystem()
        target.mergeOtherSystem(closed.captureStorage())
        target.mergeOtherSystem(partial)
        assertEquals(ConePrimitiveType.INT32, target.fixedTypeVariables[t.typeConstructor])
        assertTrue(target.notFixedTypeVariables.isEmpty())
    }

    @Test
    fun `PCLA finally restores the view while actual candidate retains lambda delta`() {
        val session = ConstraintSystemTestHarness.newSession()
        val outer = CallResolutionTestFixtures.buildCandidate(
            session, CallResolutionTestFixtures.buildFunctionSymbol(session, "outer"),
            CallResolutionTestFixtures.buildCallInfo(session, "outer"),
        )
        val pcla = CfirPCLAInferenceSession(outer, session.inferenceComponents)
        val common = pcla.currentCommonSystem
        val t = variable(common)
        val context = BodyResolveContext(ReturnTypeCalculator.Default, CfirDataFlowAnalyzerContext())
        context.inferenceSession = pcla
        val candidate = Candidate(
            symbol = CallResolutionTestFixtures.buildFunctionSymbol(session, "nested"),
            dispatchReceiver = null, givenExtensionReceiver = null,
            explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            constraintSystemFactory = session.inferenceComponents.constraintSystemFactory,
            baseSystem = common.fork(asOuter = true).currentStorage(),
            callInfo = CallResolutionTestFixtures.buildCallInfo(session, "nested"),
            originScope = null, bodyResolveContext = context,
        ).apply { initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList()) }
        val actual = candidate.system
        pcla.runLambdaCompletion(candidate, forOverloadByLambdaReturnType = false) {
            assertSame(actual, pcla.currentCommonSystem)
            val row = actual.notFixedTypeVariables.getValue(t.typeConstructor)
            actual.addSubtypeConstraint(ConePrimitiveType.INT32, t.defaultType, position)
            assertSame(actual, candidate.system)
            assertSame(row, candidate.system.notFixedTypeVariables.getValue(t.typeConstructor))
        }
        assertSame(common, pcla.currentCommonSystem)
        assertTrue(common.initialConstraints.isEmpty())
        pcla.integrateChildSession(emptyList(), actual.captureStorage()) {}
        assertSame(common, pcla.currentCommonSystem)
        assertTrue(bounds(common, t).any { it.kind.isLower() && it.type == ConePrimitiveType.INT32 })
    }

    @Test
    fun `own PCLA session preserves source owner across child view and transfer`() {
        val session = ConstraintSystemTestHarness.newSession()
        val outer = CallResolutionTestFixtures.buildCandidate(
            session, CallResolutionTestFixtures.buildFunctionSymbol(session, "outer"),
            CallResolutionTestFixtures.buildCallInfo(session, "outer"),
        )
        val parent = CfirPCLAInferenceSession(outer, session.inferenceComponents)
        val common = parent.currentCommonSystem
        val t = variable(common)
        val context = BodyResolveContext(ReturnTypeCalculator.Default, CfirDataFlowAnalyzerContext())
        context.inferenceSession = parent
        val candidate = Candidate(
            symbol = CallResolutionTestFixtures.buildFunctionSymbol(session, "nested"),
            dispatchReceiver = null, givenExtensionReceiver = null,
            explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            constraintSystemFactory = session.inferenceComponents.constraintSystemFactory,
            baseSystem = common.fork(asOuter = true).currentStorage(),
            callInfo = CallResolutionTestFixtures.buildCallInfo(session, "nested"),
            originScope = null, bodyResolveContext = context,
        ).apply { initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList()) }
        val actual = candidate.system
        val sourceOwner = actual.parentConstraintSystemOwner
        val child = parent.createChildSession(candidate)
        context.inferenceSession = child
        val local = variable(child.currentCommonSystem, "BodyLocal")
        child.currentCommonSystem.addSubtypeConstraint(ConePrimitiveType.INT32, local.defaultType, position)

        assertSame(actual, candidate.system)
        assertSame(sourceOwner, candidate.system.parentConstraintSystemOwner)
        assertFalse(local.typeConstructor in candidate.system.allTypeVariables)
        child.applyResultsToMainCandidate()
        assertSame(sourceOwner, candidate.system.parentConstraintSystemOwner)
        assertSame(local, candidate.system.allTypeVariables[local.typeConstructor])
        assertTrue(bounds(candidate.system, local).any { it.kind.isLower() && it.type == ConePrimitiveType.INT32 })

        context.inferenceSession = parent
        parent.integrateChildSession(emptyList(), candidate.system.captureStorage()) {}
        assertSame(local, common.allTypeVariables[local.typeConstructor])
        assertSame(t, common.allTypeVariables[t.typeConstructor])
    }

    @Test
    fun `nested outer branch replays intermediate source delta on actual ancestor`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val t = variable(parent)
        val intermediate = parent.fork(asOuter = true)
        val middle = variable(intermediate, "Middle")
        intermediate.addSubtypeConstraint(ConePrimitiveType.INT32, middle.defaultType, position)
        val child = intermediate.fork(asOuter = true)
        val local = variable(child, "Local")
        child.addSubtypeConstraint(ConePrimitiveType.INT32, local.defaultType, position)

        child.rebaseToOwner(parent)
        assertSame(parent, child.parentConstraintSystemOwner)
        assertSame(t, child.allTypeVariables[t.typeConstructor])
        assertSame(middle, child.allTypeVariables[middle.typeConstructor])
        assertSame(local, child.allTypeVariables[local.typeConstructor])
        assertEquals(2, child.outerSystemVariablesPrefixSize)
        assertTrue(bounds(child, middle).any { it.kind.isLower() && it.type == ConePrimitiveType.INT32 })
        assertTrue(bounds(child, local).any { it.kind.isLower() && it.type == ConePrimitiveType.INT32 })
        assertFalse(middle.typeConstructor in parent.allTypeVariables)
    }

    @Test
    fun `actual ancestor receives nested source variables and constraints`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val t = variable(parent)
        val intermediate = parent.fork(asOuter = true)
        val middle = variable(intermediate, "Middle")
        intermediate.addSubtypeConstraint(ConePrimitiveType.INT32, middle.defaultType, position)
        val child = intermediate.fork(asOuter = true)
        val local = variable(child, "Local")
        child.addSubtypeConstraint(ConePrimitiveType.INT32, local.defaultType, position)

        parent.integrateDerivedStorage(child.captureStorage())
        assertSame(t, parent.allTypeVariables[t.typeConstructor])
        assertSame(middle, parent.allTypeVariables[middle.typeConstructor])
        assertSame(local, parent.allTypeVariables[local.typeConstructor])
        assertTrue(bounds(parent, middle).any { it.kind.isLower() && it.type == ConePrimitiveType.INT32 })
        assertTrue(bounds(parent, local).any { it.kind.isLower() && it.type == ConePrimitiveType.INT32 })
    }

    @Test
    fun `ancestor replay rejects child fixation of intermediate active variable`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val t = variable(parent)
        // 独立 clone 没有 outer-prefix 的提前拒绝，仍必须在移交时遵守真实 source owner。
        val intermediate = parent.fork()
        val middle = variable(intermediate, "Middle")
        val child = intermediate.fork()
        fix(child, middle)
        assertThrows(IllegalStateException::class.java) { parent.integrateDerivedStorage(child.captureStorage()) }
        assertSame(t, parent.allTypeVariables[t.typeConstructor])
        assertEquals(1, parent.allTypeVariables.size)
        assertTrue(parent.fixedTypeVariables.isEmpty())
        assertTrue(parent.initialConstraints.isEmpty())
    }

    @Test
    fun `ancestor replay preserves intermediate source ownership even after later same fixation`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val t = variable(parent)
        val intermediate = parent.fork()
        fix(intermediate, t)
        val child = intermediate.fork()
        fix(parent, t)
        assertThrows(IllegalStateException::class.java) { child.rebaseToOwner(parent) }
        assertEquals(ConePrimitiveType.INT32, parent.fixedTypeVariables[t.typeConstructor])
    }


    @Test
    fun `repeated ancestor updates preserve intermediate outer variables and rows`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val t = variable(parent)
        val intermediate = parent.fork(asOuter = true)
        val middle = variable(intermediate, "Middle")
        intermediate.addSubtypeConstraint(ConePrimitiveType.INT32, middle.defaultType, position)
        val middleSource = intermediate.initialConstraints.single()
        val child = intermediate.fork(asOuter = true)
        val local = variable(child, "Local")
        child.addSubtypeConstraint(ConePrimitiveType.INT32, local.defaultType, position)
        child.rebaseToOwner(parent)

        parent.addSubtypeConstraint(ConePrimitiveType.INT32, t.defaultType, position)
        child.synchronizeWithOwner(parent)
        assertSame(parent, child.parentConstraintSystemOwner)
        assertEquals(2, child.outerSystemVariablesPrefixSize)
        assertEquals(setOf(t.typeConstructor, middle.typeConstructor), child.allTypeVariables.keys.take(2).toSet())
        assertSame(middle, child.allTypeVariables[middle.typeConstructor])
        assertTrue(bounds(child, middle).any { it.position.initialConstraint === middleSource })
        assertTrue(bounds(child, t).any { it.kind.isLower() })
        assertTrue(bounds(child, local).any { it.kind.isLower() })
        assertThrows(IllegalArgumentException::class.java) { fix(child, middle) }
        assertFalse(middle.typeConstructor in parent.allTypeVariables)
    }

    @Test
    fun `ancestor retargeting does not erase captured intermediate fixation ownership`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val t = variable(parent)
        val intermediate = parent.fork()
        val middle = variable(intermediate, "Middle")
        val child = intermediate.fork()
        child.rebaseToOwner(parent)
        parent.addSubtypeConstraint(ConePrimitiveType.INT32, t.defaultType, position)
        child.synchronizeWithOwner(parent)
        // 无 outer prefix 的 clone 单独验证 source ownership，不能靠提前的 prefix guard 通过。
        fix(child, middle)
        val before = parent.captureStorage()
        assertThrows(IllegalStateException::class.java) { parent.integrateDerivedStorage(child.captureStorage()) }
        assertEquals(before.allTypeVariables.keys, parent.allTypeVariables.keys)
        assertEquals(before.initialConstraints, parent.initialConstraints)
        assertTrue(parent.fixedTypeVariables.isEmpty())
        assertTrue(middle.typeConstructor in intermediate.notFixedTypeVariables)
    }

    @Test
    fun `reverted intermediate snapshot is rejected after ancestor retargeting`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val t = variable(parent)
        val intermediate = parent.fork(asOuter = true)
        val transaction = intermediate.prepareTransaction()
        val middle = variable(intermediate, "TrialMiddle")
        val child = intermediate.fork(asOuter = true)
        child.rebaseToOwner(parent)
        transaction.rollbackTransaction()
        parent.addSubtypeConstraint(ConePrimitiveType.INT32, t.defaultType, position)
        val before = parent.captureStorage()
        assertThrows(IllegalStateException::class.java) { child.synchronizeWithOwner(parent) }
        assertFalse(middle.typeConstructor in intermediate.allTypeVariables)
        assertFalse(middle.typeConstructor in parent.allTypeVariables)
        assertEquals(before.initialConstraints, parent.initialConstraints)
    }

    @Test
    fun `source may legitimately fix its own variable across ancestor view changes`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        variable(parent)
        val intermediate = parent.fork(asOuter = true)
        val middle = variable(intermediate, "Middle")
        val child = intermediate.fork(asOuter = true)
        child.rebaseToOwner(parent)
        // 此固定来自实际中间 source，之后捕获的 source 快照保存该源操作身份。
        fix(intermediate, middle)
        child.rebaseToOwner(intermediate)
        child.rebaseToOwner(parent)
        assertEquals(ConePrimitiveType.INT32, child.fixedTypeVariables[middle.typeConstructor])
        assertFalse(middle.typeConstructor in child.notFixedTypeVariables)
        assertSame(middle, child.allTypeVariables[middle.typeConstructor])
        assertEquals(2, child.outerSystemVariablesPrefixSize)
        assertFalse(middle.typeConstructor in parent.allTypeVariables)
    }

    @Test
    fun `clone inherited outer prefix survives ancestor rebase and parent growth`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val t = variable(parent)
        val intermediate = parent.fork(asOuter = true)
        val middle = variable(intermediate, "Middle")
        val child = intermediate.fork()
        assertEquals(1, child.outerSystemVariablesPrefixSize)
        child.rebaseToOwner(parent)
        assertEquals(1, child.outerSystemVariablesPrefixSize)
        assertThrows(IllegalArgumentException::class.java) { fix(child, t) }
        val later = variable(parent, "LaterParent")
        child.synchronizeWithOwner(parent)
        assertEquals(setOf(t.typeConstructor, later.typeConstructor), child.allTypeVariables.keys.take(child.outerSystemVariablesPrefixSize).toSet())
        assertSame(middle, child.allTypeVariables[middle.typeConstructor])
        assertEquals(2, child.outerSystemVariablesPrefixSize)
    }

    @Test
    fun `raw root setBase retains its actual source baseline`() {
        val source = ConstraintSystemTestHarness.newSystem()
        val t = variable(source)
        val clone = ConstraintSystemTestHarness.newSystem()
        clone.setBaseSystem(source.currentStorage())
        assertSame(source, clone.parentConstraintSystemOwner)
        source.addSubtypeConstraint(ConePrimitiveType.INT32, t.defaultType, position)
        clone.rebaseToOwner()
        assertTrue(bounds(clone, t).any { it.kind.isLower() })
    }

    @Test
    fun `ordinary rebase and child installation preserve existing row objects`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val t = variable(parent)
        val parentRow = parent.notFixedTypeVariables.getValue(t.typeConstructor)
        val child = parent.fork(asOuter = true)
        val childRow = child.notFixedTypeVariables.getValue(t.typeConstructor)
        parent.addSubtypeConstraint(ConePrimitiveType.INT32, t.defaultType, position)
        child.rebaseToOwner()
        assertSame(childRow, child.notFixedTypeVariables.getValue(t.typeConstructor))
        child.addSubtypeConstraint(t.defaultType, ConePrimitiveType.INT32, position)
        parent.integrateDerivedStorage(child.captureStorage())
        assertSame(parentRow, parent.notFixedTypeVariables.getValue(t.typeConstructor))
        assertTrue(parentRow.constraints.any { it.kind.isEqual() })
    }

    @Test
    fun `forest receiver rejects its own active variable fixed by child`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val t = variable(parent)
        val child = parent.fork()
        fix(child, t)
        val before = parent.captureStorage()
        assertThrows(IllegalStateException::class.java) {
            parent.integrateInferenceForest(listOf(child.captureStorage()), requireClosed = false)
        }
        assertEquals(before.notFixedTypeVariables.keys, parent.notFixedTypeVariables.keys)
        assertTrue(parent.fixedTypeVariables.isEmpty())
    }

    @Test
    fun `forest cannot excuse illicit child fixation after parent fixes same value`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val t = variable(parent)
        val child = parent.fork()
        fix(child, t)
        fix(parent, t)
        val before = parent.captureStorage()
        assertThrows(IllegalStateException::class.java) {
            parent.integrateInferenceForest(listOf(child.captureStorage()), requireClosed = true)
        }
        assertSame(before.initialConstraints.single(), parent.initialConstraints.single())
    }

    @Test
    fun `rebase rejects incoming binding which differs from its journal`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val child = parent.fork()
        val local = variable(child)
        fix(child, local)
        child.fixedTypeVariables[local.typeConstructor] = ConePrimitiveType.BOOLEAN
        assertThrows(IllegalStateException::class.java) {
            parent.integrateDerivedStorage(child.captureStorage())
        }
        assertTrue(parent.allTypeVariables.isEmpty())
    }

    @Test
    fun `covering root cannot hide a different incoming fixed binding`() {
        val source = ConstraintSystemTestHarness.newSystem()
        val t = variable(source)
        fix(source, t)
        source.fixedTypeVariables[t.typeConstructor] = ConePrimitiveType.BOOLEAN
        val earlier = source.captureStorage()
        source.fixedTypeVariables[t.typeConstructor] = ConePrimitiveType.INT32
        val later = source.fork()
        later.markCouldBeResolvedWithUnrestrictedBuilderInference()
        val target = ConstraintSystemTestHarness.newSystem()
        assertThrows(IllegalStateException::class.java) {
            target.integrateInferenceForest(listOf(earlier, later.captureStorage()), requireClosed = true)
        }
        assertTrue(target.allTypeVariables.isEmpty())
    }

    @Test
    fun `receiver retirement does not retain a mutable source predicate`() {
        val source = ConstraintSystemTestHarness.newSystem()
        val t = variable(source)
        source.addSubtypeConstraint(t.defaultType, ConePrimitiveType.INT32, position)
        var remove = true
        source.removeConstraintsForVariable(t.typeConstructor) { remove }
        remove = false
        val target = ConstraintSystemTestHarness.newSystem()
        target.integrateInferenceForest(listOf(source.captureStorage()), requireClosed = false)
        assertTrue(bounds(target, t).isEmpty())
        assertTrue(bounds(source, t).isEmpty())
    }

    @Test
    fun `postponed clearing retires source entries and preserves later parent entries`() {
        val parent = ConstraintSystemTestHarness.newSystem()
        val t = variable(parent)
        parent.markPostponedVariable(t)
        val child = parent.fork(asOuter = true)
        child.removePostponedVariables()
        val later = variable(parent, "Later")
        parent.markPostponedVariable(later)
        child.rebaseToOwner()
        assertFalse(t in child.postponedTypeVariables)
        assertTrue(later in child.postponedTypeVariables)
    }

    @Test
    fun `functional cache owns its source path independently of caller list`() {
        val source = ConstraintSystemTestHarness.newSystem()
        val t = variable(source)
        val path = mutableListOf(t.typeConstructor to 0)
        source.putBuiltFunctionalExpectedTypeForPostponedArgument(t.typeConstructor, path, ConePrimitiveType.INT32)
        path.clear()
        val snapshot = source.captureStorage()
        assertEquals(listOf(t.typeConstructor to 0), snapshot.builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables.keys.single().second)
        val target = ConstraintSystemTestHarness.newSystem()
        target.integrateInferenceForest(listOf(snapshot), requireClosed = false)
        assertEquals(ConePrimitiveType.INT32, target.getBuiltFunctionalExpectedTypeForPostponedArgument(t.typeConstructor, listOf(t.typeConstructor to 0)))
    }

    @Test
    fun `fixed substitution journal does not retain a mutable substitutor`() {
        val source = ConstraintSystemTestHarness.newSystem()
        val t = variable(source)
        fix(source, t)
        var replacement = ConePrimitiveType.BOOLEAN
        val substitutor = object : ConeSubstitutor() {
            override fun substituteOrNull(type: org.cangnova.cangjie.cfir.types.ConeCangJieType): org.cangnova.cangjie.cfir.types.ConeCangJieType? =
                if (type == ConePrimitiveType.INT32) replacement else null
            override fun substituteArgument(projection: org.cangnova.cangjie.cfir.types.ConeTypeProjection, index: Int): org.cangnova.cangjie.cfir.types.ConeTypeProjection? = null
        }
        source.substituteFixedVariables(substitutor)
        replacement = ConePrimitiveType.INT32
        val target = ConstraintSystemTestHarness.newSystem()
        target.integrateInferenceForest(listOf(source.captureStorage()), requireClosed = true)
        assertEquals(ConePrimitiveType.BOOLEAN, target.fixedTypeVariables[t.typeConstructor])
    }

    @Test
    fun `candidate getter preserves storage already published as read only`() {
        val session = ConstraintSystemTestHarness.newSession()
        val parent = ConstraintSystemTestHarness.newSystem(session)
        val t = variable(parent)
        val context = BodyResolveContext(ReturnTypeCalculator.Default, CfirDataFlowAnalyzerContext())
        val candidate = Candidate(
            symbol = CallResolutionTestFixtures.buildFunctionSymbol(session, "read"),
            dispatchReceiver = null, givenExtensionReceiver = null,
            explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            constraintSystemFactory = session.inferenceComponents.constraintSystemFactory,
            baseSystem = parent.fork(asOuter = true).currentStorage(),
            callInfo = CallResolutionTestFixtures.buildCallInfo(session, "read"),
            originScope = null, bodyResolveContext = context,
        ).apply { initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList()) }
        val actual = candidate.system
        val published = actual.asReadOnlyStorage()
        parent.addSubtypeConstraint(ConePrimitiveType.INT32, t.defaultType, position)
        assertSame(actual, candidate.system)
        assertTrue(published.initialConstraints.isEmpty())
        assertTrue(published.notFixedTypeVariables.getValue(t.typeConstructor).constraints.isEmpty())
    }

    @Test
    fun `independent collector clone accepts a completed source successor`() {
        val source = ConstraintSystemTestHarness.newSystem()
        val t = variable(source)
        val base = source.asReadOnlyStorage()
        val completed = source.fork()
        fix(completed, t)
        val collector = ConstraintSystemTestHarness.newSystem()
        collector.setBaseSystem(base)
        collector.integrateInferenceForest(listOf(completed.captureStorage()), requireClosed = true)
        assertEquals(ConePrimitiveType.INT32, collector.fixedTypeVariables[t.typeConstructor])
        assertTrue(collector.notFixedTypeVariables.isEmpty())
    }

}
