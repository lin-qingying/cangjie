@file:OptIn(org.cangnova.cangjie.util.PrivateForInline::class)

package org.cangnova.cangjie.cfir.resolve.body

import java.util.IdentityHashMap
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildResourceVariable
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.CfirResolutionSnapshot
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.calls.*
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.registerHierarchyProviders
import org.cangnova.cangjie.cfir.resolve.calls.candidate.*
import org.cangnova.cangjie.cfir.resolve.inference.*
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeParameterBasedTypeVariable
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityChecker
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.substitution.asCone
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.name.*
import org.cangnova.cangjie.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** 实际 syntheticGenerator 的 fake carrier 与 canonical lambda 源码槽不同，正式消费必须来自 queued atom。 */
class CfirSyntheticPCLACompletionTest {
    @Test
    fun actualQueuedResolvedSyntheticCarrierReceivesFormalOutcomeBeforeSourceRetirement() {
        val f = Fixture()
        val atom = f.resolveSynthetic()
        val carrier = atom.expression as CfirFunctionCall
        assertSame(f.lambda, atom.candidate.callInfo.callSite)
        assertNotSame(atom.expression, atom.candidate.callInfo.callSite)
        assertTrue(carrier.calleeReference is CfirResolvedNamedReference)
        assertTrue(atom.candidate.isSuccessful)
        assertThrows(IllegalStateException::class.java) { f.graph.retirePrivateResults(null, false) }
        val source = f.resolveSource()
        f.completeActualSystem()
        f.rootCall.transformSingle(f.finalWriter(), null)
        assertTrue(source.isCompleted)
        assertTrue(f.resource.resourceSourceCache!!.getStatus(f.resource.symbol) is CfirImplicitBodyResolveComputationStatus.Computed)
        assertEquals(CfirResolvePhase.BODY_RESOLVE, f.resource.resolvePhase)
        assertThrows(IllegalStateException::class.java) { f.finalWriter() }
    }

    @Test
    fun factoryBindsActualQueuedCarrierBeforeQueueDispatch() {
        val f = Fixture()
        val atom = f.resolveSynthetic()
        val source = f.resolveSource()
        f.completeActualSystem()
        val writer = f.finalWriter()
        atom.expression.transformSingle(writer, null)
        assertFalse(source.isCompleted)
        // 已经由同一真正 final writer 写回的 atom 从队列摘下；源码退休仍必须核验其正式 outcome。
        f.rootCandidate.postponedPCLACalls.removeAll { it === atom }
        f.rootCall.transformSingle(writer, null)
        assertTrue(source.isCompleted)
        assertThrows(IllegalStateException::class.java) { f.finalWriter() }
    }

    @Test
    fun queuedCarrierIntroducedAfterFactoryUsesItsActualCandidate() {
        val f = Fixture()
        val atom = f.resolveSynthetic()
        val source = f.resolveSource()
        f.completeActualSystem()
        f.rootCandidate.postponedPCLACalls.removeAll { it === atom }
        val writer = f.finalWriter()
        f.rootCandidate.postponedPCLACalls += atom
        f.rootCall.transformSingle(writer, null)
        assertTrue(source.isCompleted)
        assertThrows(IllegalStateException::class.java) { f.finalWriter() }
    }

    @Test
    fun provisionalResolvedCarrierCannotSupplyFormalOutcome() {
        val f = Fixture()
        f.resolveSynthetic()
        val source = f.resolveSource()
        f.completeActualSystem()
        f.rootCall.transformSingle(f.transformer.components.callCompleter.createCompletionResultsWriter(
            f.rootCandidate.system.buildCurrentSubstitutor().asCone()), null)
        assertFalse(source.isCompleted)
        assertTrue(f.resource.resourceSourceCache!!.getStatus(f.resource.symbol) is CfirImplicitBodyResolveComputationStatus.ComputedType)
        assertThrows(IllegalStateException::class.java) { f.graph.retirePrivateResults(null, false) }
        f.rootCall.transformSingle(f.finalWriter(), null)
        assertTrue(source.isCompleted)
    }

    @Test
    fun reinferenceRevokesQueuedCarrierWriterAndAllowsFreshFinalWriter() {
        val f = Fixture()
        val atom = f.resolveSynthetic()
        val source = f.resolveSource()
        val replay = f.graph.captureReplayState(f.scope, f.rootCall)
        f.completeActualSystem()
        val oldWriter = f.finalWriter()
        replay.restoreForReinference(f.rootCandidate.system)
        val oldReference = (atom.expression as CfirFunctionCall).calleeReference
        atom.expression.transformSingle(oldWriter, null)
        f.rootCall.transformSingle(oldWriter, null)
        assertSame(oldReference, (atom.expression as CfirFunctionCall).calleeReference)
        assertFalse(source.isCompleted)
        f.rootCall.transformSingle(f.finalWriter(), null)
        assertTrue(source.isCompleted)
    }

    @Test
    fun resolvedCarrierTargetMustMatchActualQueuedCandidate() {
        val f = Fixture()
        val atom = f.resolveSynthetic()
        val source = f.resolveSource()
        f.completeActualSystem()
        val carrier = atom.expression as CfirFunctionCall
        carrier.replaceCalleeReference(buildResolvedNamedReference {
            name = f.functionSymbol.cfir.name
            resolvedSymbol = f.functionSymbol
        })
        val failure = assertThrows(IllegalStateException::class.java) { f.rootCall.transformSingle(f.finalWriter(), null) }
        assertEquals("PCLA completion atom differs from the actual resolved target", failure.message)
        assertFalse(source.isCompleted)
    }

    @Test
    fun delegatedQueuedCarrierUsesItsOwnActualFactoryAndPreservesParentLease() {
        val f = Fixture()
        val atom = f.resolveSynthetic()
        val source = f.resolveSource()
        f.completeActualSystem()
        var delegated = false
        f.rootCandidate.pclaCallWriters[atom] = { actual, substitutor ->
            assertSame(atom, actual)
            actual.expression.transformSingle(f.transformer.components.callCompleter.createCompletionResultsWriter(
                substitutor, completionSystem = f.rootCandidate.system, completionRoot = actual.expression,
                completionCandidate = (actual as ConeAtomWithCandidate).candidate,
            ), null)
            assertFalse(source.isCompleted)
            delegated = true
        }
        f.rootCall.transformSingle(f.finalWriter(), null)
        assertTrue(delegated)
        assertTrue(source.isCompleted)
        assertThrows(IllegalStateException::class.java) { f.finalWriter() }
    }

    @Test
    fun unselectedFormalTowerAllocationIsCanceledWithoutOutcome() {
        val f = Fixture()
        val unselected = f.allocateUnselectedCandidate()
        assertFalse(f.graph.isCandidateWithdrawn(unselected))
        f.resolveSynthetic()
        val source = f.resolveSource()
        f.completeActualSystem()
        f.delegateSyntheticCarrier()
        f.rootCall.transformSingle(f.finalWriter(), null)
        assertTrue(source.isCompleted)
        assertTrue(f.graph.isCandidateWithdrawn(unselected))
    }

    @Test
    fun selectedNullOutcomeCannotRetireEvenAfterDeterminateSourceError() {
        val f = Fixture()
        f.resolveSynthetic()
        f.completeActualSystem()
        f.delegateSyntheticCarrier()
        f.rootCall.transformSingle(f.finalWriter(), null)
        val selected = f.allocateUnselectedCandidate()
        f.factoryForActualSelectedCandidate(selected)
        assertThrows(IllegalStateException::class.java) { f.graph.retirePrivateResults(null, false) }
        assertThrows(IllegalStateException::class.java) { f.graph.retirePrivateResults(null, true) }
    }

    @Test
    fun selectingRealCandidateCancelsAllocationAlternativeAndReplayRestoresIt() {
        val f = Fixture()
        val alternative = f.allocateUnselectedCandidate(sameCallSite = true)
        val replay = f.graph.captureReplayState(f.scope, f.rootCall)
        f.finalWriter()
        assertTrue(f.graph.isCandidateWithdrawn(alternative))
        replay.restoreForRollback()
        assertFalse(f.graph.isCandidateWithdrawn(alternative))
        assertThrows(IllegalStateException::class.java) { f.graph.retirePrivateResults(null, true) }
    }

    @Test
    fun determinateSourceErrorCannotRetireSelectedPartialAfterAlternativeCancellation() {
        val f = Fixture()
        val alternative = f.allocateUnselectedCandidate(sameCallSite = true)
        f.finalWriter()
        val error = ConeErrorType(org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic("actual source rejected"))
        f.resource.replaceReturnTypeRef(error.toCfirResolvedTypeRef())
        assertTrue(f.graph.isCandidateWithdrawn(alternative))
        assertThrows(IllegalStateException::class.java) { f.graph.retirePrivateResults(null, true) }
    }

    @Test
    fun actualReplacementWithdrawsOldQueuedCandidateWithoutLastWins() {
        val f = Fixture()
        val oldAtom = f.resolveSynthetic()
        val source = f.resolveSource()
        f.completeActualSystem()
        val writer = f.finalWriter()
        val current = f.copyActualCandidate(oldAtom.candidate, f.transformer.resolutionContext)
        val carrier = oldAtom.expression as CfirFunctionCall
        carrier.replaceCalleeReference(CfirNamedReferenceWithCandidate(f.source, current.callInfo.name, current))
        f.graph.recordCallAttempt(f.scope, current, CfirRetainedCallOutcome.PARTIAL)
        f.rootCandidate.postponedPCLACalls += ConeAtomWithCandidate(carrier, current)
        assertTrue(f.graph.isCandidateWithdrawn(oldAtom.candidate))
        f.rootCall.transformSingle(writer, null)
        assertTrue(source.isCompleted)
        assertFalse(f.rootCandidate.postponedPCLACalls.any { it === oldAtom })
    }

    @Test
    fun twoLiveUnscopedCandidatesCannotOverwriteTheSameCarrier() {
        val f = Fixture()
        val atom = f.resolveSynthetic()
        val context = BodyResolveContext(ReturnTypeCalculator.Default, CfirDataFlowAnalyzerContext()).apply { file = f.context.file }
        val resolution = ResolutionContext(f.session, f.transformer.components, context)
        val first = f.copyActualCandidate(atom.candidate, resolution)
        val second = f.copyActualCandidate(atom.candidate, resolution)
        f.rootCandidate.postponedPCLACalls.clear()
        f.rootCandidate.postponedPCLACalls += ConeAtomWithCandidate(atom.expression, first)
        f.rootCandidate.postponedPCLACalls += ConeAtomWithCandidate(atom.expression, second)
        val failure = assertThrows(IllegalStateException::class.java) { f.finalWriter() }
        assertEquals("Completion carrier has conflicting actual candidates", failure.message)
    }

    @Test
    fun privateSelectionWithdrawalAndReplayPreserveActualCandidateMetadata() {
        val f = Fixture()
        val atom = f.resolveSynthetic()
        val scope = CfirImplicitTypeComputationScope(f.graph, CfirImplicitTypeComputationMode.PRIVATE)
        val context = BodyResolveContext(ReturnTypeCalculator.Default, CfirDataFlowAnalyzerContext()).apply {
            file = f.context.file
            inferenceSession = f.pcla
            implicitTypeComputationScope = scope
        }
        val resolution = ResolutionContext(f.session, f.transformer.components, context)
        val info = CallInfo(callSite = f.privateLambda, callKind = atom.candidate.callInfo.callKind,
            name = atom.candidate.callInfo.name, explicitReceiver = null, arguments = listOf(f.privateLambda),
            isUsedAsGetClassReceiver = false, typeArguments = emptyList(), session = f.session,
            containingFile = f.context.file, containingDeclarations = emptyList(), resolutionMode = ResolutionMode.ContextIndependent)
        val previous = f.copyActualCandidate(atom.candidate, resolution, info)
        f.graph.recordCallAttempt(scope, previous, CfirRetainedCallOutcome.PARTIAL)
        val snapshot = f.graph.captureReplayState(scope, f.privateCall)
        val current = f.copyActualCandidate(atom.candidate, resolution, info)
        assertSame(previous.callInfo.callSite, current.callInfo.callSite)
        assertSame(previous.symbol, current.symbol)
        f.graph.recordCallAttempt(scope, current, CfirRetainedCallOutcome.PARTIAL)
        assertTrue(f.graph.isCandidateWithdrawn(previous))
        snapshot.restoreForRollback()
        assertFalse(f.graph.isCandidateWithdrawn(previous))
        assertTrue(f.graph.isCandidateWithdrawn(current))
    }

    @Test
    fun newAstReferenceBeforeFactoryWithdrawsOldQueueInsteadOfChoosingQueueOrder() {
        val f = Fixture()
        f.resolveSynthetic()
        val step = CallResolutionTestFixtures.buildFunctionSymbol(f.session, "step", ConePrimitiveType.UNIT)
        val call = buildFunctionCall {
            source = f.source
            coneTypeOrNull = ConePrimitiveType.UNIT
            calleeReference = buildResolvedNamedReference { name = step.cfir.name; resolvedSymbol = step }
        }
        f.graph.registerDerivedElement(f.scope, f.unusedSite, call)
        val info = CallInfo(callSite = call, callKind = CallKind.Function, name = step.cfir.name,
            explicitReceiver = null, arguments = emptyList(), isUsedAsGetClassReceiver = false, typeArguments = emptyList(),
            session = f.session, containingFile = f.context.file, containingDeclarations = emptyList(), resolutionMode = ResolutionMode.ContextIndependent)
        val old = CandidateFactory(f.transformer.resolutionContext, info).createCandidate(
            callInfo = info, symbol = step, originScope = null, explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER).apply {
            initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList())
            initializeArgumentMapping(emptyList(), linkedMapOf())
        }
        f.graph.recordCallAttempt(f.scope, old, CfirRetainedCallOutcome.PARTIAL)
        val oldAtom = ConeAtomWithCandidate(call, old)
        f.rootCandidate.postponedPCLACalls.add(0, oldAtom)
        val current = f.copyActualCandidate(old, f.transformer.resolutionContext)
        f.graph.freshVariables(f.scope, current, emptyList()) { emptyList() }
        // 重查已经发布实际 AST 引用，但尚未进入 completer/recordCallAttempt。
        call.replaceCalleeReference(CfirNamedReferenceWithCandidate(f.source, info.name, current))
        (f.lambda.anonymousFunction.body!!.statements as MutableList<CfirStatement>).add(call)
        val source = f.resolveSource()
        f.completeActualSystem()
        assertFalse(f.graph.isCandidateWithdrawn(old))
        assertFalse(f.graph.isCandidateWithdrawn(current))
        val writer = f.finalWriter()
        assertFalse(f.graph.isCandidateWithdrawn(current), "queue-first selection revoked the actual AST candidate")
        assertTrue(f.graph.isCandidateWithdrawn(old))
        assertSame(current, (call.calleeReference as CfirNamedReferenceWithCandidate).candidate)
        f.rootCall.transformSingle(writer, null)
        assertTrue(source.isCompleted)
        assertFalse(f.rootCandidate.postponedPCLACalls.any { it === oldAtom })
    }


    @Test
    fun newAstReferenceAfterFactorySelectsQueuedCandidateBeforeCheckingOldBinding() {
        val f = Fixture()
        val oldAtom = f.resolveSynthetic()
        val source = f.resolveSource()
        f.completeActualSystem()
        val writer = f.finalWriter()
        val current = f.allocateReplacementCandidate(oldAtom.candidate)
        val carrier = oldAtom.expression as CfirFunctionCall
        // factory 已绑定旧 carrier；重查发布实际新引用/atom，尚未记录任何新 trial 或 outcome。
        carrier.replaceCalleeReference(CfirNamedReferenceWithCandidate(f.source, current.callInfo.name, current))
        f.rootCandidate.postponedPCLACalls.removeAll { it === oldAtom }
        f.rootCandidate.postponedPCLACalls += ConeAtomWithCandidate(carrier, current)
        assertSame(f.lambda, current.callInfo.callSite)
        assertNotSame(carrier, current.callInfo.callSite)
        assertSame(oldAtom.candidate.symbol, current.symbol)
        assertTrue(oldAtom.candidate.freshVariables.zip(current.freshVariables).all { (old, new) -> old === new })
        assertFalse(f.graph.isCandidateWithdrawn(oldAtom.candidate))
        assertFalse(f.graph.isCandidateWithdrawn(current))
        f.rootCall.transformSingle(writer, null)
        assertTrue(source.isCompleted)
        assertTrue(f.graph.isCandidateWithdrawn(oldAtom.candidate))
        assertTrue(f.resource.resourceSourceCache!!.getStatus(f.resource.symbol) is CfirImplicitBodyResolveComputationStatus.Computed)
        assertEquals(CfirResolvePhase.BODY_RESOLVE, f.resource.resolvePhase)
    }

    @Test
    fun callbackPublishesNewAstCandidateBeforeDispatchWithoutQueueOrderSelection() {
        for (newAtomFirst in listOf(false, true)) {
            val f = Fixture()
            val oldAtom = f.resolveSynthetic()
            val publisher = CallResolutionTestFixtures.buildFunctionSymbol(f.session, "publish", ConePrimitiveType.UNIT)
            val call = buildFunctionCall {
                source = f.source
                coneTypeOrNull = ConePrimitiveType.UNIT
                calleeReference = buildResolvedNamedReference { name = publisher.cfir.name; resolvedSymbol = publisher }
            }
            f.graph.registerDerivedElement(f.scope, f.unusedSite, call)
            val info = CallInfo(callSite = call, callKind = CallKind.Function, name = publisher.cfir.name,
                explicitReceiver = null, arguments = emptyList(), isUsedAsGetClassReceiver = false, typeArguments = emptyList(),
                session = f.session, containingFile = f.context.file, containingDeclarations = emptyList(),
                resolutionMode = ResolutionMode.ContextIndependent)
            val publisherCandidate = CandidateFactory(f.transformer.resolutionContext, info).createCandidate(
                callInfo = info, symbol = publisher, originScope = null,
                explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER).apply {
                initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList())
                initializeArgumentMapping(emptyList(), linkedMapOf())
            }
            call.replaceCalleeReference(CfirNamedReferenceWithCandidate(f.source, info.name, publisherCandidate))
            f.graph.recordCallAttempt(f.scope, publisherCandidate, CfirRetainedCallOutcome.PARTIAL)
            (f.lambda.anonymousFunction.body!!.statements as MutableList<CfirStatement>).add(call)
            val publisherAtom = ConeAtomWithCandidate(call, publisherCandidate)
            f.rootCandidate.postponedPCLACalls.add(0, publisherAtom)
            val source = f.resolveSource()
            f.completeActualSystem()
            val writer = f.finalWriter()
            val current = f.allocateReplacementCandidate(oldAtom.candidate)
            val carrier = oldAtom.expression as CfirFunctionCall
            val newAtom = ConeAtomWithCandidate(carrier, current)
            if (newAtomFirst) f.rootCandidate.postponedPCLACalls.add(1, newAtom)
            else f.rootCandidate.postponedPCLACalls += newAtom
            var published = false
            var selected = false
            // 正式 writer 的前一候选回调发布新 AST；根队列已捕获 old/new 的实际 atom。
            lateinit var publish: (ConeSubstitutor) -> Unit
            publish = {
                assertFalse(published)
                assertFalse(source.isCompleted)
                assertFalse(f.graph.isCandidateWithdrawn(oldAtom.candidate))
                assertFalse(f.graph.isCandidateWithdrawn(current))
                carrier.replaceCalleeReference(CfirNamedReferenceWithCandidate(f.source, current.callInfo.name, current))
                published = true
                publisherCandidate.onPCLACompletionResultsWritingCallbacks.removeAll { it === publish }
            }
            publisherCandidate.onPCLACompletionResultsWritingCallbacks += publish
            current.onPCLACompletionResultsWritingCallbacks += {
                assertTrue(published)
                assertTrue(f.graph.isCandidateWithdrawn(oldAtom.candidate))
                assertFalse(f.graph.isCandidateWithdrawn(current))
                assertFalse(source.isCompleted)
                selected = true
            }
            f.rootCall.transformSingle(writer, null)
            assertTrue(published)
            assertTrue(selected, "dispatch skipped the current AST candidate; newAtomFirst=$newAtomFirst")
            assertTrue(source.isCompleted)
            assertFalse(f.rootCandidate.postponedPCLACalls.any { it === oldAtom })
            assertTrue(f.rootCandidate.postponedPCLACalls.any { it === newAtom })
            assertSame(current.symbol, (carrier.calleeReference as CfirResolvedNamedReference).resolvedSymbol)
        }
    }

    @Test
    fun twoLiveUnscopedCandidatesAfterFactoryRemainDispatchConflict() {
        val f = Fixture()
        val atom = f.resolveSynthetic()
        val source = f.resolveSource()
        f.completeActualSystem()
        val context = BodyResolveContext(ReturnTypeCalculator.Default, CfirDataFlowAnalyzerContext()).apply { file = f.context.file }
        val resolution = ResolutionContext(f.session, f.transformer.components, context)
        val first = f.copyActualCandidate(atom.candidate, resolution)
        val second = f.copyActualCandidate(atom.candidate, resolution)
        assertNull(first.implicitTypeComputationScope)
        assertNull(second.implicitTypeComputationScope)
        f.rootCandidate.postponedPCLACalls.clear()
        f.rootCandidate.postponedPCLACalls += ConeAtomWithCandidate(atom.expression, first)
        val writer = f.finalWriter()
        (atom.expression as CfirFunctionCall).replaceCalleeReference(
            CfirNamedReferenceWithCandidate(f.source, second.callInfo.name, second))
        f.rootCandidate.postponedPCLACalls.clear()
        f.rootCandidate.postponedPCLACalls += ConeAtomWithCandidate(atom.expression, second)
        val failure = assertThrows(IllegalStateException::class.java) { f.rootCall.transformSingle(writer, null) }
        assertEquals("Queued completion carrier changed its actual candidate", failure.message)
        assertSame(second, ((atom.expression as CfirFunctionCall).calleeReference as CfirNamedReferenceWithCandidate).candidate)
        assertFalse(source.isCompleted)
    }

    @Test
    fun revokedFactoryWriterCannotSelectAstReplacementFromLaterGeneration() {
        val f = Fixture()
        val oldAtom = f.resolveSynthetic()
        val source = f.resolveSource()
        val replay = f.graph.captureReplayState(f.scope, f.rootCall)
        f.completeActualSystem()
        val oldWriter = f.finalWriter()
        replay.restoreForReinference(f.rootCandidate.system)
        val current = f.allocateReplacementCandidate(oldAtom.candidate)
        val carrier = oldAtom.expression as CfirFunctionCall
        val reference = CfirNamedReferenceWithCandidate(f.source, current.callInfo.name, current)
        carrier.replaceCalleeReference(reference)
        f.rootCandidate.postponedPCLACalls.removeAll { it === oldAtom }
        f.rootCandidate.postponedPCLACalls += ConeAtomWithCandidate(carrier, current)
        carrier.transformSingle(oldWriter, null)
        f.rootCall.transformSingle(oldWriter, null)
        assertSame(reference, carrier.calleeReference)
        assertFalse(f.graph.isCandidateWithdrawn(oldAtom.candidate))
        assertFalse(f.graph.isCandidateWithdrawn(current))
        assertFalse(source.isCompleted)
        f.rootCall.transformSingle(f.finalWriter(), null)
        assertTrue(source.isCompleted)
    }

    @Test
    fun lambdaOwnerResetRevokesUnscopedReceiptAndHeldFullWriter() {
        val f = Fixture()
        val lambdaAtom = f.resolveSynthetic()
        f.completeActualSystem()
        val owner = lambdaAtom.candidate
        val target = CallResolutionTestFixtures.buildFunctionSymbol(f.session, "unscopedBody", ConePrimitiveType.UNIT)
        val carrier = buildFunctionCall {
            source = f.source
            coneTypeOrNull = ConePrimitiveType.UNIT
            calleeReference = buildNamedReference { name = target.cfir.name }
        }
        fun candidate(): Candidate {
            val info = CallInfo(callSite = carrier, callKind = CallKind.Function, name = target.cfir.name,
                explicitReceiver = null, arguments = emptyList(), isUsedAsGetClassReceiver = false, typeArguments = emptyList(),
                session = f.session, containingFile = f.context.file, containingDeclarations = emptyList(),
                resolutionMode = ResolutionMode.ContextIndependent)
            return CallResolutionTestFixtures.buildCandidate(f.session, target, info).apply {
                initializeArgumentMapping(emptyList(), linkedMapOf())
            }
        }
        val first = candidate()
        val firstAtom = ConeAtomWithCandidate(carrier, first)
        var firstWrites = 0
        first.onPCLACompletionResultsWritingCallbacks += { firstWrites++ }
        carrier.replaceCalleeReference(CfirNamedReferenceWithCandidate(f.source, target.cfir.name, first))
        owner.postponedPCLACalls += firstAtom
        val parentState = f.rootCandidate.capturePCLACompletionState()
        val actualFull = f.transformer.components.callCompleter.runFullCompletionForCall(owner, lambdaAtom.expression as CfirFunctionCall)
        val writer = f.transformer.components.callCompleter.createCompletionResultsWriterForFullCompletion(
            actualFull, actualFull.system.captureStorage().buildAbstractResultingSubstitutor(f.session.typeContext).asCone())
        lambdaAtom.expression.transformSingle(writer, null)
        assertEquals(1, firstWrites)

        owner.resetLambdaBodyResolutionResults()
        // 只恢复 parent 不恢复已 reset 的真实 generated lambda；不能复活 child 的完成事实或 held writer。
        f.rootCandidate.restorePCLACompletionState(parentState)
        val second = candidate()
        val secondAtom = ConeAtomWithCandidate(carrier, second)
        val reference = CfirNamedReferenceWithCandidate(f.source, target.cfir.name, second)
        carrier.replaceCalleeReference(reference)
        owner.postponedPCLACalls += listOf(firstAtom, secondAtom)
        lambdaAtom.expression.transformSingle(writer, null)
        assertSame(reference, carrier.calleeReference)
        assertEquals(1, firstWrites)
        val failure = assertThrows(IllegalStateException::class.java) {
            val freshFull = f.transformer.components.callCompleter.runFullCompletionForCall(owner, lambdaAtom.expression as CfirFunctionCall)
            f.transformer.components.callCompleter.createCompletionResultsWriterForFullCompletion(
                freshFull, freshFull.system.captureStorage().buildAbstractResultingSubstitutor(f.session.typeContext).asCone())
        }
        assertEquals("Completion carrier has conflicting actual candidates", failure.message)
    }

    @Test
    fun scopedOnlyFormalWriterSurvivesReceiptOwnerStateRestore() {
        val f = Fixture()
        f.resolveSynthetic()
        val source = f.resolveSource()
        f.completeActualSystem()
        assertTrue(f.rootCandidate.postponedPCLACalls.filterIsInstance<ConeAtomWithCandidate>()
            .all { it.candidate.implicitTypeComputationScope != null })
        val beforeFactory = f.rootCandidate.capturePCLACompletionState()
        val actualFull = f.transformer.components.callCompleter.runFullCompletionForCall(f.rootCandidate, f.rootCall)
        val writer = f.transformer.components.callCompleter.createCompletionResultsWriterForFullCompletion(
            actualFull, actualFull.system.captureStorage().buildAbstractResultingSubstitutor(f.session.typeContext).asCone())
        // 这里只恢复 PCLA owner 状态，没有撤销 graph 的实际 FORMAL lease/binding。
        f.rootCandidate.restorePCLACompletionState(beforeFactory)
        f.rootCall.transformSingle(writer, null)
        assertTrue(source.isCompleted)
        assertTrue(f.resource.resourceSourceCache!!.getStatus(f.resource.symbol) is CfirImplicitBodyResolveComputationStatus.Computed)
        assertEquals(CfirResolvePhase.BODY_RESOLVE, f.resource.resolvePhase)
    }


    /** graph rollback/reinfer 可恢复语义配置，但不能恢复成功 source 撤权后的 selection/lease。 */
    @Test
    fun sourceWithdrawalSurvivesGraphReplayWithoutChangingActualVm() {
        for (reinfer in listOf(false, true)) {
            val f = Fixture()
            val lambdaOwner = f.resolveSynthetic().candidate
            val previous = f.allocateUnselectedCandidate()
            val carrier = previous.callInfo.callSite as CfirFunctionCall
            carrier.replaceCalleeReference(CfirNamedReferenceWithCandidate(f.source, previous.callInfo.name, previous))
            f.graph.recordCallAttempt(f.scope, previous, CfirRetainedCallOutcome.PARTIAL)
            val state = previous.capturePCLACompletionState()
            val lease = f.graph.captureCompletionLease(f.scope, carrier, f.rootCandidate.system)
            assertTrue(f.graph.isCompletionLeaseActive(f.scope, previous, lease))
            f.graph.bindFormalCompletionToLease(f.scope, previous, lease)
            val replay = f.graph.captureReplayState(f.scope, carrier)
            val before = f.rootCandidate.withConstraintSystemForCompletionResults { it.captureStorage() }

            lambdaOwner.resetLambdaBodyResolutionResults(listOf(previous))
            previous.restorePCLACompletionState(state.copy())
            if (reinfer) replay.restoreForReinference(f.rootCandidate.system) else replay.restoreForRollback()

            assertTrue(previous.isSourceCompletionWithdrawn)
            assertTrue(f.graph.isCandidateWithdrawn(previous))
            assertFalse(f.graph.isCompletionLeaseActive(f.scope, previous, lease))
            assertThrows(IllegalStateException::class.java) { f.graph.selectCompletionCandidate(f.scope, previous) }
            assertThrows(IllegalStateException::class.java) { f.factoryForActualSelectedCandidate(previous) }
            val after = f.rootCandidate.withConstraintSystemForCompletionResults { it.captureStorage() }
            assertEquals(before.allTypeVariables.keys, after.allTypeVariables.keys)
            before.allTypeVariables.forEach { (constructor, variable) ->
                assertSame(variable, after.allTypeVariables[constructor])
            }
            assertEquals(before.fixedTypeVariables, after.fixedTypeVariables)
        }
    }

    private class Fixture {
        val session = CallResolutionTestFixtures.newTestSession().also {
            it.register(CfirBuiltinTypes::class, CfirBuiltinTypes())
            it.register(CfirAccessibilityChecker::class, CfirAccessibilityChecker(it))
        }
        val resourceType = ConeClassLikeType(ConeClassLikeLookupTagImpl(StdlibClassIds.Resource))
        val implementationId = ClassId(FqName.ROOT, Name.identifier("R"))
        val implementationType = ConeClassLikeType(ConeClassLikeLookupTagImpl(implementationId))
        val functionType = ConeFunctionType(emptyList(), ConePrimitiveType.UNIT)
        val source = checkNotNull(CallResolutionTestFixtures.buildTypedExpression(ConePrimitiveType.INT32).source)
        val functionSymbol = CallResolutionTestFixtures.buildFunctionSymbol(session, "makeResource", implementationType, listOf(functionType))
        val lambda = lambda()
        val privateLambda = lambda()
        val rootCall = call(lambda)
        val privateCall = call(privateLambda)
        val unusedSite = call(lambda)
        val privateUnusedSite = call(privateLambda)
        val resource = resource(rootCall)
        val privateResource = resource(privateCall)
        val registry = IdentityHashMap<CfirResourceVariable, CfirImplicitTypeComputation>()
        val graph = CfirImplicitTypeComputation(resource, privateCall,
            mapOf<CfirElement, CfirElement>(resource to privateResource, rootCall to privateCall,
                lambda to privateLambda, lambda.anonymousFunction to privateLambda.anonymousFunction,
                unusedSite to privateUnusedSite),
            emptyMap(), null, registry, emptySet()).also { registry[resource] = it }
        val scope = CfirImplicitTypeComputationScope(graph, CfirImplicitTypeComputationMode.FORMAL)
        val context = BodyResolveContext(ReturnTypeCalculator.Default, CfirDataFlowAnalyzerContext())
        val transformer: CfirBodyResolveTransformer
        val rootCandidate: Candidate
        val pcla: CfirPCLAInferenceSession
        val tryExpression = buildTryExpression {
            resources += resource
            tryBlock = buildBlock { coneTypeOrNull = ConePrimitiveType.UNIT }
            coneTypeOrNull = ConePrimitiveType.UNIT
        }

        /** 走真实 CandidateFactory/fresh-variable allocation seam；未把该候选装进引用或队列。 */
        fun allocateUnselectedCandidate(sameCallSite: Boolean = false): Candidate {
            val actualSite = if (sameCallSite) rootCall else unusedSite
            val info = CallInfo(callSite = actualSite, callKind = CallKind.Function, name = functionSymbol.cfir.name,
                explicitReceiver = null, arguments = listOf(lambda), isUsedAsGetClassReceiver = false, typeArguments = emptyList(),
                session = session, containingFile = context.file, containingDeclarations = emptyList(), resolutionMode = ResolutionMode.ContextIndependent)
            val candidate = CandidateFactory(transformer.resolutionContext, info).createCandidate(
                callInfo = info,
                symbol = functionSymbol, originScope = null, explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            )
            graph.freshVariables(scope, candidate, emptyList()) { emptyList() }
            return candidate
        }
        fun copyActualCandidate(original: Candidate, resolution: ResolutionContext, info: CallInfo = original.callInfo): Candidate = CandidateFactory(resolution, info)
            .createCandidate(callInfo = info, symbol = original.symbol as CfirCallableSymbol<*>, originScope = null,
                explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER).apply {
                initializeSubstitutorAndVariables(original.substitutor, original.freshVariables)
                if (info === original.callInfo) initializeArgumentMapping(original.arguments, LinkedHashMap(original.argumentMapping))
                else {
                    val argument = ConeSimpleLeafResolutionAtom(info.arguments.single())
                    initializeArgumentMapping(listOf(argument), linkedMapOf(argument to original.argumentMapping.values.single()))
                }
            }

        /** 新 allocation 保留实际声明参数/constructor；发布引用之前不伪造试算或正式 outcome。 */
        fun allocateReplacementCandidate(original: Candidate): Candidate =
            copyActualCandidate(original, transformer.resolutionContext).also { current ->
                graph.freshVariables(scope, current, (current.symbol.cfir as CfirFunction).typeParameters) {
                    current.freshVariables.map { it as ConeTypeParameterBasedTypeVariable }
                }
            }

        fun factoryForActualSelectedCandidate(candidate: Candidate) {
            val carrier = buildFunctionCallCopy(unusedSite) {
                calleeReference = CfirNamedReferenceWithCandidate(source, candidate.callInfo.name, candidate)
            }
            graph.registerDerivedElement(scope, candidate.callInfo.callSite, carrier)
            transformer.components.callCompleter.createCompletionResultsWriter(rootCandidate.system.buildCurrentSubstitutor().asCone(),
                completionSystem = rootCandidate.system, completionRoot = carrier, completionCandidate = candidate)
        }
        fun delegateSyntheticCarrier() {
            val atom = rootCandidate.postponedPCLACalls.filterIsInstance<ConeAtomWithCandidate>().single { it.candidate.callInfo.callSite === lambda }
            rootCandidate.pclaCallWriters[atom] = { actual, substitutor ->
                actual.expression.transformSingle(transformer.components.callCompleter.createCompletionResultsWriter(substitutor,
                    completionSystem = rootCandidate.system, completionRoot = actual.expression,
                    completionCandidate = (actual as ConeAtomWithCandidate).candidate), null)
            }
        }

        init {
            session.registerHierarchyProviders(listOf<CfirClassLikeDeclaration>(
                ExtendTestFixtures.newInterface(session.moduleData, "Resource", StdlibClassIds.Resource),
                ExtendTestFixtures.newClass(session.moduleData, "R", implementationId, listOf(resourceType.toCfirResolvedTypeRef())),
            ))
            context.file = CallResolutionTestFixtures.buildCallInfo(session, "makeResource").containingFile
            context.implicitTypeComputationScope = scope
            transformer = CfirBodyResolveTransformer(session, ScopeSession(), outerBodyResolveContext = context)
            val info = CallInfo(callSite = rootCall, callKind = CallKind.Function, name = Name.identifier("makeResource"),
                explicitReceiver = null, arguments = listOf(lambda), isUsedAsGetClassReceiver = false, typeArguments = emptyList(),
                session = session, containingFile = context.file, containingDeclarations = emptyList(), resolutionMode = ResolutionMode.ContextIndependent)
            rootCandidate = Candidate(symbol = functionSymbol, dispatchReceiver = null, givenExtensionReceiver = null,
                explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                constraintSystemFactory = session.inferenceComponents.constraintSystemFactory,
                baseSystem = session.inferenceComponents.createConstraintSystem().captureStorage(), callInfo = info,
                originScope = null, bodyResolveContext = context).apply {
                initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList())
                val argument = ConeSimpleLeafResolutionAtom(lambda)
                initializeArgumentMapping(listOf(argument), linkedMapOf(argument to functionSymbol.cfir.valueParameters.single()))
            }
            rootCall.replaceCalleeReference(CfirNamedReferenceWithCandidate(source, info.name, rootCandidate))
            graph.constraintSystemOwner = rootCandidate.system
            graph.storeResult(resource.returnTypeRef as CfirResolvedTypeRef)
            graph.recordCallAttempt(scope, rootCandidate, CfirRetainedCallOutcome.PARTIAL)
            pcla = CfirPCLAInferenceSession(rootCandidate, session.inferenceComponents)
            context.inferenceSession = pcla
        }

        fun resolveSynthetic(): ConeAtomWithCandidate {
            transformer.components.syntheticCallGenerator.resolveAnonymousFunctionExpressionWithSyntheticOuterCall(
                lambda, ResolutionMode.WithExpectedType(functionType.toCfirResolvedTypeRef()), transformer.resolutionContext,
            )
            pcla.applyResultsToMainCandidate()
            return rootCandidate.postponedPCLACalls.filterIsInstance<ConeAtomWithCandidate>().single {
                it.candidate.callInfo.callSite === lambda
            }
        }
        fun resolveSource(): CfirResourceSourceCompletion {
            ResourceVariableReturnTypeCalculator(transformer.components, listOf(resource), tryExpression, resourceType)
                .resolveResource(resource) { resource }
            pcla.applyResultsToMainCandidate()
            return checkNotNull(resource.resourceSourceCompletion)
        }
        fun completeActualSystem() {
            ConstraintSystemCompleter(transformer.components).complete(rootCandidate.system.asConstraintSystemCompleterContext(),
                ConstraintSystemCompletionMode.FULL, listOf(ConeAtomWithCandidate(rootCall, rootCandidate)),
                rootCandidate.substitutedReturnType(), transformer.resolutionContext) { _, _ -> error("Lambda was analyzed by the generator") }
            assertTrue(rootCandidate.system.notFixedTypeVariables.isEmpty())
        }
        fun finalWriter() = transformer.components.callCompleter.createCompletionResultsWriter(
            rootCandidate.system.buildCurrentSubstitutor().asCone(), completionSystem = rootCandidate.system,
            completionRoot = rootCall, completionCandidate = rootCandidate,
        )
        private fun lambda() = buildAnonymousFunctionExpression {
            source = this@Fixture.source
            anonymousFunction = buildAnonymousFunction {
                source = this@Fixture.source
                moduleData = session.moduleData
                resolvePhase = CfirResolvePhase.RAW_CFIR
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = true
                status = CfirDeclarationStatusImpl()
                returnTypeRef = ConePrimitiveType.UNIT.toCfirResolvedTypeRef()
                typeRef = functionType.toCfirResolvedTypeRef()
                body = buildBlock { coneTypeOrNull = ConePrimitiveType.UNIT }
                symbol = CfirAnonymousFunctionSymbol()
                hasExplicitParameterList = true
                isLambda = true
            }
            isTrailingLambda = false
        }
        private fun call(argument: CfirExpression) = buildFunctionCall {
            source = this@Fixture.source
            coneTypeOrNull = implementationType
            calleeReference = buildResolvedNamedReference { name = functionSymbol.cfir.name; resolvedSymbol = functionSymbol }
            argumentList = buildArgumentList { arguments += argument }
        }
        private fun resource(initializer: CfirExpression) = buildResourceVariable {
            source = this@Fixture.source
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.IMPLICIT_TYPES
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            status = CfirDeclarationStatusImpl()
            returnTypeRef = implementationType.toCfirResolvedTypeRef()
            this.initializer = initializer
            name = Name.identifier("b")
            symbol = CfirResourceVariableSymbol(CallableId(name))
            isVar = false
        }
    }
}
