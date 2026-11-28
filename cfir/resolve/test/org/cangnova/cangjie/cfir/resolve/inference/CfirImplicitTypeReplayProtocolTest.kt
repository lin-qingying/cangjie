package org.cangnova.cangjie.cfir.resolve.inference

import java.util.IdentityHashMap
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildResourceVariable
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.CfirResolutionSnapshot
import org.cangnova.cangjie.cfir.resolve.body.ImplicitBodyResolveComputationSession
import org.cangnova.cangjie.cfir.references.builder.buildErrorNamedReference
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.resolve.calls.candidate.*
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeFixVariableConstraintPosition
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirResourceVariableSymbol
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * retained 图的正式消费与事务生命周期。
 *
 * 使用生产图、Candidate、CFIR builder 和真实约束系统；观测消费/拒绝及退役结果，
 * 不读取图的私有集合，不把 applicability 或固定变量集合当成 completion。
 */
class CfirImplicitTypeReplayProtocolTest {
    @Test
    fun successfulApplicabilityWithoutCompletionDoesNotLockTarget() {
        for (outcome in listOf(CfirRetainedCallOutcome.SKIPPED, CfirRetainedCallOutcome.PARTIAL, CfirRetainedCallOutcome.REJECTED)) {
            val f = Fixture()
            val trial = f.attempt(f.privateScope, f.first, outcome)
            assertTrue(trial.isSuccessful)
            f.complete(f.formalScope, f.second)
            f.graph.retirePrivateResults(null, sourceRejected = false)
            assertTrue(f.graph.independentStorages().isNotEmpty())
        }
    }

    @Test
    fun completedPrivateCallRequiresExactFormalDeclaration() {
        val f = Fixture()
        f.complete(f.privateScope, f.first)
        assertThrows(IllegalStateException::class.java) { f.complete(f.formalScope, f.second) }
    }

    @Test
    fun completedPrivateCallCannotRetireAfterOnlyPartialFormalAttempt() {
        val f = Fixture()
        f.complete(f.privateScope, f.first)
        f.attempt(f.formalScope, f.first, CfirRetainedCallOutcome.PARTIAL)
        assertThrows(IllegalStateException::class.java) { f.graph.retirePrivateResults(null, false) }
    }

    @Test
    fun finalSourceRejectionIsAnOutcomeWithoutSuccessfulReplay() {
        val f = Fixture()
        f.complete(f.privateScope, f.first)
        f.attempt(f.formalScope, f.second, CfirRetainedCallOutcome.REJECTED)
        f.graph.retirePrivateResults(null, sourceRejected = true)
    }

    @Test
    fun finalWriterWithUnresolvedResultCannotEstablishTerminal() {
        val f = Fixture()
        val candidate = f.attempt(f.privateScope, f.first)
        val variable = ConstraintSystemTestHarness.newVariable(candidate.system, "Pending")
        f.privateCall.replaceConeTypeOrNull(variable.defaultType)
        f.graph.recordCompletedCall(f.privateScope, candidate, f.privateCall, candidate.system, ConeSubstitutor.Empty,
            f.graph.captureCompletionLease(f.privateScope, f.privateCall, candidate.system))
        f.complete(f.formalScope, f.second)
        f.graph.retirePrivateResults(null, false)
    }

    @Test
    fun astCsRollbackRevokesTerminalAndOldWriterCannotResurrectIt() {
        val f = Fixture()
        val ast = CfirResolutionSnapshot.capture(f.privateRoot)
        val cacheOwner = ImplicitBodyResolveComputationSession()
        val cache = ast.captureImplicitBodyCache(cacheOwner)
        val before = f.graph.captureReplayState(f.privateScope, f.privateRoot)
        val candidate = f.attempt(f.privateScope, f.first)
        val transaction = candidate.system.prepareTransaction()
        f.writeSuccess(f.privateScope, candidate)
        transaction.rollbackTransaction()
        ast.restoreWithImplicitBodyCache(cache, cacheOwner)
        before.restoreForRollback()
        f.graph.recordCompletedCall(f.privateScope, candidate, f.privateCall, candidate.system, ConeSubstitutor.Empty, f.lease(candidate))
        f.complete(f.formalScope, f.second)
        f.graph.retirePrivateResults(null, false)
    }

    @Test
    fun restoringSelectedSuccessfulGenerationReactivatesItsTerminal() {
        val f = Fixture()
        val before = f.graph.captureReplayState(f.privateScope, f.privateRoot)
        f.complete(f.privateScope, f.first)
        val selected = f.graph.captureReplayState(f.privateScope, f.privateRoot)
        before.restoreForRollback()
        selected.restoreForRollback()
        assertThrows(IllegalStateException::class.java) { f.complete(f.formalScope, f.second) }
    }

    @Test
    fun concreteTypeConsumesFixedSlotWithoutReenteringFactory() {
        for (role in listOf(CfirInferenceVariableRole.INVOKE_PARAMETER, CfirInferenceVariableRole.INVOKE_RETURN,
            CfirInferenceVariableRole.OPTION_PATTERN_ELEMENT, CfirInferenceVariableRole.ITERABLE_ELEMENT)) {
            val f = Fixture()
            val variable = f.allocate(role)
            f.consume(f.privateScope, role, variable.defaultType)
            f.fix(variable)
            f.consume(f.formalScope, role, ConePrimitiveType.INT32)
            assertSame(variable, f.system.allTypeVariables[variable.typeConstructor])
            assertEquals(1, f.system.allTypeVariables.size)
            f.graph.retirePrivateResults(null, false)
        }
    }

    @Test
    fun fixedBindingAloneCannotConsumeAnExpressionSlot() {
        val f = Fixture()
        val role = CfirInferenceVariableRole.ITERABLE_ELEMENT
        val variable = f.allocate(role)
        f.consume(f.privateScope, role, variable.defaultType)
        f.fix(variable)
        assertThrows(IllegalStateException::class.java) { f.graph.retirePrivateResults(null, false) }
    }

    @Test
    fun roleAndIndexMustMatchTheExactAllocatedSlot() {
        for (wrongIndex in listOf(false, true)) {
            val f = Fixture()
            val role = CfirInferenceVariableRole.INVOKE_PARAMETER
            val variable = f.allocate(role)
            f.consume(f.privateScope, role, variable.defaultType)
            f.fix(variable)
            f.consume(f.formalScope, if (wrongIndex) role else CfirInferenceVariableRole.INVOKE_RETURN,
                ConePrimitiveType.INT32, if (wrongIndex) 1 else 0)
            assertThrows(IllegalStateException::class.java) { f.graph.retirePrivateResults(null, false) }
        }
    }

    @Test
    fun anotherOwnerWithTheSameNameDoesNotConsumeTheSlot() {
        val f = Fixture()
        val role = CfirInferenceVariableRole.OPTION_PATTERN_ELEMENT
        val variable = f.allocate(role)
        f.consume(f.privateScope, role, variable.defaultType)
        f.fix(variable)
        f.graph.consumeExpressionType(f.formalScope, f.otherSourceCall, role, 0, ConePrimitiveType.INT32, f.system)
        assertThrows(IllegalStateException::class.java) { f.graph.retirePrivateResults(null, false) }
    }

    @Test
    fun adoptedConcreteTypeMustAgreeWithTheActualOwnerBinding() {
        val f = Fixture()
        val role = CfirInferenceVariableRole.INVOKE_RETURN
        val variable = f.allocate(role)
        f.consume(f.privateScope, role, variable.defaultType)
        f.fix(variable)
        assertThrows(IllegalStateException::class.java) { f.consume(f.formalScope, role, ConePrimitiveType.BOOLEAN) }
    }

    @Test
    fun anotherActualCsCannotStandInForTheRetainedVmOwner() {
        val f = Fixture()
        val role = CfirInferenceVariableRole.ITERABLE_ELEMENT
        val variable = f.allocate(role)
        f.consume(f.privateScope, role, variable.defaultType)
        val unrelated = ConstraintSystemTestHarness.newSystem(f.session)
        ConstraintSystemTestHarness.newVariable(unrelated, "Slot")
        f.graph.constraintSystemOwner = unrelated
        assertThrows(IllegalStateException::class.java) { f.consume(f.formalScope, role, ConePrimitiveType.INT32) }
    }

    @Test
    fun formalErrorSlotIsRecordedAsRejectionWithoutInventingABinding() {
        val f = Fixture()
        val role = CfirInferenceVariableRole.ITERABLE_ELEMENT
        val variable = f.allocate(role)
        f.consume(f.privateScope, role, variable.defaultType)
        val error = ConeErrorType(ConeSimpleDiagnostic("Rejected iterable"))
        f.consume(f.formalScope, role, error)
        assertTrue(variable.typeConstructor in f.system.notFixedTypeVariables)
        f.graph.retirePrivateResults(null, sourceRejected = true)
    }

    @Test
    fun reinferenceKeepsActualFixedVmButRevokesFirstRoundProofs() {
        val f = Fixture()
        val before = f.graph.captureReplayState(f.privateScope, f.privateRoot)
        val role = CfirInferenceVariableRole.INVOKE_RETURN
        val variable = f.allocate(role)
        f.consume(f.privateScope, role, variable.defaultType)
        f.fix(variable)
        val previousCandidate = f.complete(f.privateScope, f.first)
        before.restoreForReinference(f.system)
        f.graph.recordCompletedCall(f.privateScope, previousCandidate, f.privateCall,
            previousCandidate.system, ConeSubstitutor.Empty, f.lease(previousCandidate))
        // 第二轮沿实际类型采用路径登记新的证明，正式阶段不重新分配变量。
        f.consume(f.privateScope, role, ConePrimitiveType.INT32)
        f.consume(f.formalScope, role, ConePrimitiveType.INT32)
        f.complete(f.formalScope, f.second)
        assertSame(variable, f.system.allTypeVariables[variable.typeConstructor])
        f.graph.retirePrivateResults(null, false)
    }

    @Test
    fun syntheticInvokeTerminalUsesItsRegisteredSourceRoleAcrossRebuiltSymbols() {
        val f = Fixture()
        f.graph.registerSyntheticSymbol(f.privateScope, f.privateCall, f.first, CfirSyntheticCallRole.FUNCTION_TYPE_INVOKE)
        f.complete(f.privateScope, f.first)
        f.graph.registerSyntheticSymbol(f.formalScope, f.sourceCall, f.second, CfirSyntheticCallRole.FUNCTION_TYPE_INVOKE)
        f.complete(f.formalScope, f.second)
        f.graph.retirePrivateResults(null, false)
    }

    @Test
    fun explicitDerivedOwnerConsumesItsCanonicalSlot() {
        val f = Fixture()
        val role = CfirInferenceVariableRole.INVOKE_RETURN
        val privateDerived = buildFunctionCall { calleeReference = buildNamedReference { name = Name.identifier("invoke") } }
        f.privateScope.registerDerivedElement(f.privateCall, privateDerived)
        val variable = f.graph.variable(f.privateScope, null, privateDerived, role, 0) { ConeTypeVariable("Return") }
        f.system.registerVariable(variable)
        f.graph.consumeExpressionType(f.privateScope, privateDerived, role, 0, variable.defaultType, f.system)
        f.fix(variable)
        val formalDerived = buildFunctionCall { calleeReference = buildNamedReference { name = Name.identifier("invoke") } }
        f.formalScope.registerDerivedElement(f.sourceCall, formalDerived)
        f.graph.consumeExpressionType(f.formalScope, formalDerived, role, 0, ConePrimitiveType.INT32, f.system)
        f.graph.retirePrivateResults(null, false)
    }

    @Test
    fun unresolvedFormalReferenceRecordsItsActualSourceRejection() {
        val f = Fixture()
        f.complete(f.privateScope, f.first)
        f.sourceCall.replaceCalleeReference(buildErrorNamedReference {
            name = Name.identifier("pick")
            diagnostic = ConeSimpleDiagnostic("Unresolved formal target")
        })
        f.graph.recordRejectedSourceCall(f.formalScope, f.sourceCall)
        f.graph.retirePrivateResults(null, sourceRejected = false)
    }


    @Test
    fun privateExplorationCannotDeleteCompletedTerminal() {
        for (outcome in listOf(CfirRetainedCallOutcome.SKIPPED, CfirRetainedCallOutcome.PARTIAL, CfirRetainedCallOutcome.REJECTED)) {
            val f = Fixture()
            f.complete(f.privateScope, f.first)
            f.attempt(f.privateScope, f.second, outcome)
            assertThrows(IllegalStateException::class.java) { f.complete(f.formalScope, f.second) }
        }
    }

    @Test
    fun anotherAttemptOfSameTargetDoesNotMutateCompletedInstantiation() {
        val f = Fixture()
        val completed = f.complete(f.privateScope, f.first)
        f.graph.recordCallAttempt(f.privateScope, completed, CfirRetainedCallOutcome.SKIPPED)
        f.attempt(f.privateScope, f.first, CfirRetainedCallOutcome.REJECTED)
        assertThrows(IllegalStateException::class.java) { f.complete(f.formalScope, f.second) }
    }

    @Test
    fun rollbackRevokesWriterEvenWhenCandidatePredatesSnapshot() {
        val f = Fixture()
        val candidate = f.attempt(f.privateScope, f.first)
        val ast = CfirResolutionSnapshot.capture(f.privateRoot)
        val before = f.graph.captureReplayState(f.privateScope, f.privateRoot)
        f.writeSuccess(f.privateScope, candidate)
        val old = f.lease(candidate)
        ast.restoreWithImplicitBodyCache(null, null)
        before.restoreForRollback()
        assertFalse(f.graph.isCompletionLeaseActive(f.privateScope, candidate, old))
        f.graph.recordCompletedCall(f.privateScope, candidate, f.privateCall, candidate.system, ConeSubstitutor.Empty, old)
        f.complete(f.formalScope, f.second)
        f.graph.retirePrivateResults(null, false)
    }

    @Test
    fun successfulSnapshotRestoresTheActualWriterLease() {
        val f = Fixture()
        val astBefore = CfirResolutionSnapshot.capture(f.privateRoot)
        val before = f.graph.captureReplayState(f.privateScope, f.privateRoot)
        val candidate = f.complete(f.privateScope, f.first)
        val old = f.lease(candidate)
        val astAfter = CfirResolutionSnapshot.capture(f.privateRoot)
        val after = f.graph.captureReplayState(f.privateScope, f.privateRoot)
        astBefore.restoreWithImplicitBodyCache(null, null)
        before.restoreForRollback()
        assertFalse(f.graph.isCompletionLeaseActive(f.privateScope, candidate, old))
        astAfter.restoreWithImplicitBodyCache(null, null)
        after.restoreForRollback()
        assertTrue(f.graph.isCompletionLeaseActive(f.privateScope, candidate, old))
        f.complete(f.formalScope, f.first)
        f.graph.retirePrivateResults(null, false)
    }

    @Test
    fun independentStorageMustBeClosedBeforeTerminalPublication() {
        val f = Fixture()
        val candidate = f.attempt(f.privateScope, f.first)
        ConstraintSystemTestHarness.newVariable(candidate.system, "Pending")
        f.writeSuccess(f.privateScope, candidate)
        f.complete(f.formalScope, f.second)
        f.graph.retirePrivateResults(null, false)
    }

    @Test
    fun concreteSubstitutorCannotReplaceAnActualFixedBinding() {
        for (fixed in listOf(false, true)) {
            val f = Fixture()
            val candidate = f.attempt(f.privateScope, f.first)
            val variable = f.graph.variable(f.privateScope, candidate, f.privateCall, CfirInferenceVariableRole.INVOKE_RETURN, 0) { ConeTypeVariable("Owned") }
            candidate.system.registerVariable(variable)
            if (fixed) candidate.system.fixVariable(variable, ConePrimitiveType.INT32, ConeFixVariableConstraintPosition(variable))
            val replacement = if (fixed) ConePrimitiveType.BOOLEAN else ConePrimitiveType.INT32
            val externalSubstitutor = object : ConeSubstitutor() {
                override fun substituteOrNull(type: ConeCangJieType): ConeCangJieType? = if (type == variable.defaultType) replacement else null
                override fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection? = null
            }
            f.privateCall.replaceCalleeReference(buildResolvedNamedReference { name = Name.identifier("pick"); resolvedSymbol = candidate.symbol })
            f.privateCall.replaceConeTypeOrNull(ConePrimitiveType.INT32)
            val lease = f.graph.captureCompletionLease(f.privateScope, f.privateCall, candidate.system)
            f.graph.recordCompletedCall(f.privateScope, candidate, f.privateCall, candidate.system, externalSubstitutor, lease)
            f.complete(f.formalScope, f.second)
            f.graph.retirePrivateResults(null, false)
        }
    }

    @Test
    fun retainedAttemptStorageDoesNotChangeWithLaterLiveSystemWrites() {
        val f = Fixture()
        val candidate = f.attempt(f.privateScope, f.first, CfirRetainedCallOutcome.PARTIAL)
        val saved = f.graph.independentStorages().single()
        val later = ConstraintSystemTestHarness.newVariable(candidate.system, "Later")
        assertFalse(later.typeConstructor in saved.allTypeVariables)
        assertSame(later, candidate.system.allTypeVariables[later.typeConstructor])
    }

    @Test
    fun reinferenceCannotPromoteRejectedSlotsToViablePartial() {
        val f = Fixture()
        val before = f.graph.captureReplayState(f.privateScope, f.privateRoot)
        val rejected = f.attempt(f.privateScope, f.first, CfirRetainedCallOutcome.REJECTED)
        val variable = f.graph.variable(f.privateScope, rejected, f.privateCall, CfirInferenceVariableRole.INVOKE_RETURN, 0) { ConeTypeVariable("Failed") }
        f.system.registerVariable(variable)
        before.restoreForReinference(f.system)
        val formal = f.attempt(f.formalScope, f.first)
        assertTrue(f.graph.variablesForFormalCandidate(f.formalScope, formal).isEmpty())
        assertSame(variable, f.system.allTypeVariables[variable.typeConstructor])
    }

    @Test
    fun registryReplacementRevokesNewGraphWriterWhenOldOwnerReturns() {
        val f = Fixture()
        val candidate = f.attempt(f.privateScope, f.first)
        val replacement = CfirImplicitTypeComputation(f.resource, f.privateRoot,
            mapOf<CfirElement, CfirElement>(f.sourceRoot to f.privateRoot, f.sourceCall to f.privateCall, f.otherSourceCall to f.otherPrivateCall),
            emptyMap(), null, f.registry, emptySet())
        f.registry[f.resource] = replacement
        val scope = CfirImplicitTypeComputationScope(replacement, CfirImplicitTypeComputationMode.PRIVATE)
        val lease = replacement.captureCompletionLease(scope, f.privateCall, candidate.system)
        assertTrue(replacement.isCompletionLeaseActive(scope, candidate, lease))
        f.registry[f.resource] = f.graph
        assertFalse(replacement.isCompletionLeaseActive(scope, candidate, lease))
        assertTrue(replacement.isCandidateWithdrawn(candidate))
        assertThrows(IllegalStateException::class.java) { replacement.captureCompletionLease(scope, f.privateCall, candidate.system) }
        val before = f.system.captureStorage()
        assertThrows(IllegalStateException::class.java) { replacement.withPrivateOrigins { ConstraintSystemTestHarness.newVariable(f.system, "Ghost") } }
        assertEquals(before.allTypeVariables.keys, f.system.allTypeVariables.keys)
    }

    @Test
    fun retiredGraphCannotPublishThroughHeldOldWriterOrCreateAnotherWriter() {
        val f = Fixture()
        val candidate = f.complete(f.privateScope, f.first)
        val old = f.lease(candidate)
        f.complete(f.formalScope, f.first)
        f.graph.retirePrivateResults(null, false)
        assertFalse(f.graph.isCompletionLeaseActive(f.privateScope, candidate, old))
        assertThrows(IllegalStateException::class.java) { f.graph.captureCompletionLease(f.privateScope, f.privateCall, candidate.system) }
    }

    @Test
    fun reinferenceRevokesOnlyAffectedSourceGrants() {
        val f = Fixture()
        val first = f.attempt(f.privateScope, f.first)
        val other = f.attempt(f.privateScope, f.first, callSite = f.otherPrivateCall)
        val lease = f.graph.captureCompletionLease(f.privateScope, f.privateRoot, f.system)
        val before = f.graph.captureReplayState(f.privateScope, f.privateCall)
        before.restoreForReinference(f.system)
        assertFalse(f.graph.isCompletionLeaseActive(f.privateScope, first, lease))
        assertTrue(f.graph.isCompletionLeaseActive(f.privateScope, other, lease))
        before.restoreForRollback()
        assertTrue(f.graph.isCompletionLeaseActive(f.privateScope, first, lease))
        assertTrue(f.graph.isCompletionLeaseActive(f.privateScope, other, lease))
    }

    @Test
    fun rollbackRestoresAllOwnedCandidateQueuesByIdentity() {
        val f = Fixture()
        val first = f.attempt(f.privateScope, f.first)
        val other = f.attempt(f.privateScope, f.first, callSite = f.otherPrivateCall)
        val before = f.graph.captureReplayState(f.privateScope, f.privateRoot)
        first.onPCLACompletionResultsWritingCallbacks += { }
        other.onPCLACompletionResultsWritingCallbacks += { }
        before.restoreForRollback()
        assertTrue(first.onPCLACompletionResultsWritingCallbacks.isEmpty())
        assertTrue(other.onPCLACompletionResultsWritingCallbacks.isEmpty())
    }

    @Test
    fun concreteFormalConsumerUsesTheActualDefaultCollectorOwner() {
        val f = Fixture()
        val role = CfirInferenceVariableRole.ITERABLE_ELEMENT
        val variable = f.allocate(role)
        f.consume(f.privateScope, role, variable.defaultType)
        f.fix(variable)
        f.graph.retainDefaultFinalStorage(f.system.captureStorage(), f.system)
        val context = org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext(
            org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator.Default,
            org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzerContext(),
        )
        context.withImplicitTypeComputationScope(f.formalScope) {
            context.consumeInferenceType(f.sourceCall, role, ConePrimitiveType.INT32)
        }
        f.graph.retirePrivateResults(null, false)
    }

    private class Fixture {
        val session = CallResolutionTestFixtures.newTestSession()
        private val source = requireNotNull(CallResolutionTestFixtures.buildTypedExpression(ConePrimitiveType.INT32).source)
        val sourceCall = call()
        val privateCall = call()
        val otherSourceCall = call()
        val otherPrivateCall = call()
        val sourceRoot = buildBlock { statements.addAll(listOf(sourceCall, otherSourceCall)) }
        val privateRoot = buildBlock { statements.addAll(listOf(privateCall, otherPrivateCall)) }
        val first = CallResolutionTestFixtures.buildFunctionSymbol(session, "first", ConePrimitiveType.INT32)
        val second = CallResolutionTestFixtures.buildFunctionSymbol(session, "second", ConePrimitiveType.INT32)
        val resource = buildResourceVariable {
            source = this@Fixture.source
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            status = CfirDeclarationStatusImpl()
            initializer = sourceRoot
            isVar = false
            symbol = CfirResourceVariableSymbol(CallableId(Name.identifier("resource")))
            returnTypeRef = ConePrimitiveType.INT32.toCfirResolvedTypeRef()
            name = Name.identifier("resource")
        }
        val registry = IdentityHashMap<CfirResourceVariable, CfirImplicitTypeComputation>()
        val graph = CfirImplicitTypeComputation(resource, privateRoot,
            mapOf<CfirElement, CfirElement>(sourceRoot to privateRoot, sourceCall to privateCall, otherSourceCall to otherPrivateCall),
            emptyMap(), null, registry, emptySet())
        init { registry[resource] = graph }
        val privateScope = CfirImplicitTypeComputationScope(graph, CfirImplicitTypeComputationMode.PRIVATE)
        val formalScope = CfirImplicitTypeComputationScope(graph, CfirImplicitTypeComputationMode.FORMAL)
        val system = ConstraintSystemTestHarness.newSystem(session)
        private val writerLeases = IdentityHashMap<Candidate, CfirRetainedCompletionLease>()
        fun lease(candidate: Candidate): CfirRetainedCompletionLease = checkNotNull(writerLeases[candidate])

        fun attempt(scope: CfirImplicitTypeComputationScope, symbol: CfirNamedFunctionSymbol,
            outcome: CfirRetainedCallOutcome = CfirRetainedCallOutcome.SKIPPED,
            callSite: CfirElement = site(scope)): Candidate {
            val template = CallResolutionTestFixtures.buildCallInfo(session, "pick")
            val info = CallInfo(callSite = callSite, callKind = CallKind.Function, name = Name.identifier("pick"),
                explicitReceiver = null, arguments = emptyList(), isUsedAsGetClassReceiver = false,
                typeArguments = emptyList(), session = session, containingFile = template.containingFile,
                containingDeclarations = emptyList(), resolutionMode = ResolutionMode.ContextIndependent)
            return CallResolutionTestFixtures.buildCandidate(session, symbol, info).also {
                graph.recordCallAttempt(scope, it, outcome)
            }
        }

        fun complete(scope: CfirImplicitTypeComputationScope, symbol: CfirNamedFunctionSymbol): Candidate =
            attempt(scope, symbol).also { writeSuccess(scope, it) }

        fun writeSuccess(scope: CfirImplicitTypeComputationScope, candidate: Candidate) {
            val call = site(scope)
            val lease = graph.captureCompletionLease(scope, call, candidate.system)
            writerLeases[candidate] = lease
            call.replaceCalleeReference(buildResolvedNamedReference {
                name = Name.identifier("pick")
                resolvedSymbol = candidate.symbol
            })
            call.replaceConeTypeOrNull(ConePrimitiveType.INT32)
            graph.recordCompletedCall(scope, candidate, call, candidate.system, ConeSubstitutor.Empty, lease)
        }

        fun allocate(role: CfirInferenceVariableRole): ConeTypeVariable =
            graph.variable(privateScope, null, privateCall, role, 0) { ConeTypeVariable("Slot") }
                .also { system.registerVariable(it) }

        fun fix(variable: ConeTypeVariable) =
            system.fixVariable(variable, ConePrimitiveType.INT32, ConeFixVariableConstraintPosition(variable))

        fun consume(scope: CfirImplicitTypeComputationScope, role: CfirInferenceVariableRole,
            type: ConeCangJieType, index: Int = 0) =
            graph.consumeExpressionType(scope, site(scope), role, index, type, system)

        private fun site(scope: CfirImplicitTypeComputationScope): CfirFunctionCall =
            if (scope.mode == CfirImplicitTypeComputationMode.PRIVATE) privateCall else sourceCall

        private fun call(): CfirFunctionCall = buildFunctionCall {
            calleeReference = buildNamedReference { name = Name.identifier("pick") }
        }
    }
}
