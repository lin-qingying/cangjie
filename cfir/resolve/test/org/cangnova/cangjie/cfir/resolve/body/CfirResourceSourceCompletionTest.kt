@file:OptIn(org.cangnova.cangjie.util.PrivateForInline::class, org.cangnova.cangjie.cfir.resolve.dfa.cfg.CfgInternals::class)

package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.diagnostic.ConePostponedInferenceDiagnostic
import java.util.IdentityHashMap
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirDefaultRawCopyContext
import org.cangnova.cangjie.cfir.CfirRawCopyState
import org.cangnova.cangjie.cfir.DeepCopyCfirTreeWithSymbols
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildResourceVariable
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.diagnostic.ConeTypeMismatchError
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildErrorExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCallCopy
import org.cangnova.cangjie.cfir.expressions.builder.buildTryExpression
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.CfirResolutionSnapshot
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.calls.ConeAtomWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.registerHierarchyProviders
import org.cangnova.cangjie.cfir.resolve.calls.candidate.*
import org.cangnova.cangjie.cfir.resolve.inference.*
import org.cangnova.cangjie.cfir.resolve.dfa.*
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph
import org.cangnova.cangjie.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeArgumentConstraintPosition
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.substitution.asCone
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.symbols.CfirResourceVariableSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Resource source 的正式完成边界。
 * 使用真实 CS/PCLA/production calculator 和 completion writer；不检查图的私有集合，
 * 不由 fixed map、回调名称或直接修改 completion outcome 制造 source 完成。
 */
class CfirResourceSourceCompletionTest {
    @Test
    fun pendingSpecificationAddsConstraintToActualPclaAndStaysPreview() {
        val f = Fixture(pending = true)
        val task = f.resolveSource()
        val variable = checkNotNull(f.pendingVariable)
        assertSame(variable, f.pcla.currentCommonSystem.allTypeVariables[variable.typeConstructor])
        assertTrue(ConstraintSystemTestHarness.upperBoundsOf(f.pcla.currentCommonSystem, variable).contains(f.resourceType))
        assertTrue(f.resource.returnTypeRef.coneTypeOrNull is ConeTypeVariableType)
        assertTrue(f.resource.initializer!!.coneTypeOrNull is ConeTypeVariableType)
        assertTrue(f.cache().getStatus(f.resource.symbol) is CfirImplicitBodyResolveComputationStatus.ComputedType)
        assertEquals(CfirResolvePhase.IMPLICIT_TYPES, f.resource.resolvePhase)
        assertFalse(task.isCompleted)
        assertTrue(task.isOwnedBy(f.rootCandidate))
    }

    @Test
    fun actualFinalWriterPublishesSourceInitializerAndCacheThenRetires() {
        val f = Fixture(pending = true, retained = true)
        val task = f.resolveSource()
        f.writeFull()
        assertTrue(task.isCompleted)
        assertEquals(f.implementationType, f.resource.returnTypeRef.coneTypeOrNull)
        assertEquals(f.implementationType, f.resource.initializer!!.coneTypeOrNull)
        val computed = f.cache().getStatus(f.resource.symbol) as CfirImplicitBodyResolveComputationStatus.Computed
        assertSame(f.resource, computed.transformedDeclaration)
        assertSame(f.resource.returnTypeRef, computed.resolvedTypeRef)
        assertEquals(CfirResolvePhase.BODY_RESOLVE, f.resource.resolvePhase)
        assertFalse(f.rootCandidate.onPCLACompletionResultsWritingCallbacks.any { it === task })
        assertThrows(IllegalStateException::class.java) { f.newFinalWriter() }
    }

    @Test
    fun fixedTypesAndProvisionalWriterDoNotPublishOrRetireSource() {
        val f = Fixture(pending = true, retained = true)
        val task = f.resolveSource()
        f.completeActualSystem()
        f.rootCall.transformSingle(f.transformer.components.callCompleter.createCompletionResultsWriter(f.substitutor()), null)
        assertFalse(task.isCompleted)
        assertTrue(f.cache().getStatus(f.resource.symbol) is CfirImplicitBodyResolveComputationStatus.ComputedType)
        assertEquals(CfirResolvePhase.IMPLICIT_TYPES, f.resource.resolvePhase)
        assertThrows(IllegalStateException::class.java) { f.graph!!.retirePrivateResults(null, false) }
        f.writeFull()
        assertTrue(task.isCompleted)
    }

    @Test
    fun emptyPrivateTerminalTableCannotHideAnOutstandingFormalCall() {
        val f = Fixture(retained = true)
        f.privatePartialAttempt()
        f.rootCall.transformSingle(f.transformer.components.callCompleter.createCompletionResultsWriter(ConeSubstitutor.Empty), null)
        // 尚未安装 source task，直接检验 FORMAL 的 final outcome 义务，而非 pending task guard。
        assertThrows(IllegalStateException::class.java) { f.graph!!.retirePrivateResults(null, false) }
        f.resolveSource()
        f.writeFull()
    }

    @Test
    fun resourceProtocolRejectionKeepsDiagnosticAndDoesNotInvalidateTryResult() {
        for (type in listOf(ConePrimitiveType.NOTHING, ConePrimitiveType.INT32)) {
            val f = Fixture()
            f.resource.replaceReturnTypeRef(type.toCfirResolvedTypeRef(f.resource.source))
            val task = f.resolveSource()
            f.writeFull()
            assertTrue(task.isCompleted)
            val error = f.resource.returnTypeRef.coneType as ConeErrorType
            val diagnostic = assertInstanceOf(ConeTypeMismatchError::class.java, error.diagnostic)
            assertEquals(type, diagnostic.actualType)
            assertEquals(ConeTypeMismatchError.Origin.RESOURCE_PROTOCOL, diagnostic.origin)
            assertEquals(ConePrimitiveType.UNIT, f.tryExpression.coneTypeOrNull)
        }
    }

    @Test
    fun resourceTypeFixedToNothingReportsProtocolMismatchDuringFinalWrite() {
        val f = Fixture(pending = true)
        f.tryExpression.replaceConeTypeOrNull(ConeErrorType(ConePostponedInferenceDiagnostic))
        val task = f.resolveSource()
        val variable = checkNotNull(f.pendingVariable)
        assertTrue(f.resource.returnTypeRef.coneTypeOrNull is ConeTypeVariableType)
        f.rootCandidate.system.addEqualityConstraint(
            variable.defaultType, ConePrimitiveType.NOTHING, ConeArgumentConstraintPosition(f.resource),
        )
        f.completeActualSystem(ConePrimitiveType.NOTHING)
        f.rootCall.transformSingle(f.newFinalWriter(), null)
        assertTrue(task.isCompleted)
        val type = assertInstanceOf(ConeErrorType::class.java, f.resource.returnTypeRef.coneTypeOrNull)
        val diagnostic = assertInstanceOf(ConeTypeMismatchError::class.java, type.diagnostic)
        assertEquals(ConePrimitiveType.NOTHING, diagnostic.actualType)
        assertEquals(ConeTypeMismatchError.Origin.RESOURCE_PROTOCOL, diagnostic.origin)
        assertEquals(ConePrimitiveType.UNIT, f.tryExpression.coneTypeOrNull)
    }

    @Test
    fun concreteImplementationRemainsAValidResource() {
        val f = Fixture()
        assertEquals(CfirResourceTypeCheck.VALID, checkResourceSpecification(f.resource, f.resourceType, f.session, null))
        assertEquals(f.implementationType, f.resource.returnTypeRef.coneTypeOrNull)
    }

    @Test
    fun foreignPendingConstructorCannotBeReplacedBySameNamedOwnerVariable() {
        val f = Fixture(pending = true)
        val other = ConstraintSystemTestHarness.newSystem(f.session)
        ConstraintSystemTestHarness.newVariable(other, "ResourceType")
        assertThrows(IllegalStateException::class.java) { checkResourceSpecification(f.resource, f.resourceType, f.session, other) }
        assertTrue(f.resource.returnTypeRef.coneTypeOrNull is ConeTypeVariableType)
        assertTrue(f.rootCandidate.system.fixedTypeVariables.isEmpty())
    }

    @Test
    fun finalSourceInitializerErrorPreservesItsOriginalDiagnostic() {
        val f = Fixture()
        val diagnostic = ConeSimpleDiagnostic("initializer source rejected")
        val error = ConeErrorType(diagnostic)
        f.resource.replaceInitializer(buildErrorExpression { this.diagnostic = diagnostic; expression = CallResolutionTestFixtures.buildTypedExpression(error) })
        f.resolveSource()
        f.writeFull()
        assertSame(error, f.resource.returnTypeRef.coneTypeOrNull)
        val propagated = f.tryExpression.coneTypeOrNull as ConeErrorType
        assertSame(diagnostic, (propagated.diagnostic as ConeUnreportedDuplicateDiagnostic).original)
        assertTrue(f.cache().getStatus(f.resource.symbol) is CfirImplicitBodyResolveComputationStatus.Computed)
    }

    @Test
    fun initializerErrorPreservesTheExplicitResourceBindingType() {
        val f = Fixture(explicitType = true)
        val diagnostic = ConeSimpleDiagnostic("initializer source rejected")
        val error = ConeErrorType(diagnostic)
        f.resource.replaceInitializer(buildErrorExpression {
            this.diagnostic = diagnostic
            expression = CallResolutionTestFixtures.buildTypedExpression(error)
        })
        f.resolveSource()
        f.writeFull()
        assertEquals(f.implementationType, f.resource.returnTypeRef.coneTypeOrNull)
        val propagated = f.tryExpression.coneTypeOrNull as ConeErrorType
        assertSame(diagnostic, (propagated.diagnostic as ConeUnreportedDuplicateDiagnostic).original)
        assertTrue(f.cache().getStatus(f.resource.symbol) is CfirImplicitBodyResolveComputationStatus.Computed)
    }

    @Test
    fun initializerSynthesisFailureDoesNotCheckTheResourceProtocol() {
        val f = Fixture(explicitType = true)
        f.resource.replaceReturnTypeRef(ConePrimitiveType.INT32.toCfirResolvedTypeRef(f.resource.source))
        val diagnostic = ConeSimpleDiagnostic("initializer source rejected")
        f.resource.replaceInitializer(buildErrorExpression {
            this.diagnostic = diagnostic
            expression = CallResolutionTestFixtures.buildTypedExpression(ConeErrorType(diagnostic))
        })
        f.resolveSource()
        f.writeFull()
        assertEquals(ConePrimitiveType.INT32, f.resource.returnTypeRef.coneTypeOrNull)
        val propagated = assertInstanceOf(ConeErrorType::class.java, f.tryExpression.coneTypeOrNull)
        assertSame(diagnostic, (propagated.diagnostic as ConeUnreportedDuplicateDiagnostic).original)
    }

    @Test
    fun astAndActualCsRollbackRevokeSourceTaskAndOldWriter() {
        val f = Fixture(pending = true, retained = true)
        val ast = CfirResolutionSnapshot.capture(f.tryExpression)
        val replay = f.graph!!.captureReplayState(f.scope!!, f.rootCall)
        val transaction = f.rootCandidate.system.prepareTransaction()
        val task = f.resolveSource()
        f.completeActualSystem()
        val writer = f.newFinalWriter()
        f.rootCall.transformSingle(writer, null)
        transaction.rollbackTransaction()
        ast.restoreWithImplicitBodyCache(null, null)
        replay.restoreForRollback()
        assertFalse(task.isCurrent)
        assertNull(f.resource.resourceSourceCompletion)
        assertNull(f.resource.resourceSourceCache)
        assertTrue(f.rootCandidate.system.fixedTypeVariables.isEmpty())
        f.rootCall.transformSingle(writer, null)
        assertTrue(f.resource.returnTypeRef.coneTypeOrNull is ConeTypeVariableType)
        assertFalse(f.rootCandidate.onPCLACompletionResultsWritingCallbacks.any { it === task })
    }

    @Test
    fun replayRestoresSourceSlotOutsideTheCapturedPrivateInitializer() {
        val f = Fixture(pending = true, retained = true)
        val task = f.resolveSource()
        val privateAst = CfirResolutionSnapshot.capture(f.privateCall)
        val graph = checkNotNull(f.graph)
        val privateScope = CfirImplicitTypeComputationScope(graph, CfirImplicitTypeComputationMode.PRIVATE)
        val replay = graph.captureReplayState(privateScope, f.privateCall)
        val transaction = f.rootCandidate.system.prepareTransaction()
        f.writeFull()
        assertEquals(CfirResolvePhase.BODY_RESOLVE, f.resource.resolvePhase)
        transaction.rollbackTransaction()
        privateAst.restoreWithImplicitBodyCache(null, null)
        replay.restoreForRollback()
        assertTrue(f.resource.returnTypeRef.coneTypeOrNull is ConeTypeVariableType)
        assertTrue(f.resource.initializer!!.coneTypeOrNull is ConeTypeVariableType)
        assertEquals(CfirResolvePhase.IMPLICIT_TYPES, f.resource.resolvePhase)
        assertTrue(f.cache().getStatus(f.resource.symbol) is CfirImplicitBodyResolveComputationStatus.ComputedType)
        assertFalse(task.isCompleted)
        assertTrue(task.isOwnedBy(f.rootCandidate))
        f.writeFull()
        assertTrue(task.isCompleted)
    }

    @Test
    fun successfulSnapshotRestoresSameSourceTaskCacheOwnerAndRetirement() {
        val f = Fixture(pending = true, retained = true)
        val beforeAst = CfirResolutionSnapshot.capture(f.tryExpression)
        val beforeReplay = f.graph!!.captureReplayState(f.scope!!, f.rootCall)
        val task = f.resolveSource()
        f.writeFull()
        val selectedCache = f.cache()
        val afterAst = CfirResolutionSnapshot.capture(f.tryExpression)
        val afterReplay = checkNotNull(f.graph).captureReplayState(checkNotNull(f.scope), f.rootCall)
        // 真正固定的父 binding 保留；这里只重放与同一 binding 对应的 AST/cache/图代次。
        beforeAst.restoreWithImplicitBodyCache(null, null)
        beforeReplay.restoreForRollback()
        assertFalse(task.isCurrent)
        afterAst.restoreWithImplicitBodyCache(null, null)
        afterReplay.restoreForRollback()
        assertSame(task, f.resource.resourceSourceCompletion)
        assertTrue(task.isCurrent && task.isCompleted)
        assertSame(selectedCache, f.cache())
        assertTrue(f.cache().getStatus(f.resource.symbol) is CfirImplicitBodyResolveComputationStatus.Computed)
        assertEquals(f.implementationType, f.graph.resultTypeRef!!.coneType)
        assertThrows(IllegalStateException::class.java) { f.newFinalWriter() }
    }

    @Test
    fun callbackMutationSkipsWithdrawnEntryAndCompletesNewActualSourceTask() {
        val f = Fixture(retained = true)
        val original = f.resolveSource()
        var withdrawnRan = false
        val withdrawn: (ConeSubstitutor) -> Unit = { withdrawnRan = true }
        val extra = f.newResource(f.implementationType, f.resolvedInitializer(), "later")
        var additional: CfirResourceSourceCompletion? = null
        val mutate: (ConeSubstitutor) -> Unit = {
            f.rootCandidate.onPCLACompletionResultsWritingCallbacks.removeAll { it === withdrawn }
            f.tryExpression.replaceResources(f.tryExpression.resources + extra)
            val cache = ImplicitBodyResolveComputationSession()
            extra.resourceSourceCache = cache
            additional = CfirResourceSourceCompletion(extra, f.tryExpression, f.resourceType, cache, null, null).also { source ->
                extra.resourceSourceCompletion = source
                cache.previewResourceSource(extra)
                f.pcla.registerResourceSourceCompletion(source)
            }
        }
        f.rootCandidate.onPCLACompletionResultsWritingCallbacks.add(0, mutate)
        f.rootCandidate.onPCLACompletionResultsWritingCallbacks += withdrawn
        f.writeFull()
        assertFalse(withdrawnRan)
        assertTrue(original.isCompleted)
        assertTrue(checkNotNull(additional).isCompleted)
        assertTrue(extra.resourceSourceCache!!.getStatus(extra.symbol) is CfirImplicitBodyResolveComputationStatus.Computed)
    }

    @Test
    fun newResourcesMaterializedDuringAstTrialLoseTheirCallbackAndCache() {
        val f = Fixture()
        val ast = CfirResolutionSnapshot.capture(f.tryExpression)
        val extra = f.newResource(f.implementationType, f.resolvedInitializer(), "later")
        f.tryExpression.replaceResources(f.tryExpression.resources + extra)
        val cache = ImplicitBodyResolveComputationSession()
        extra.resourceSourceCache = cache
        val task = CfirResourceSourceCompletion(extra, f.tryExpression, f.resourceType, cache, null, null)
        extra.resourceSourceCompletion = task
        cache.previewResourceSource(extra)
        f.pcla.registerResourceSourceCompletion(task)
        ast.restoreWithImplicitBodyCache(null, null)
        assertFalse(task.isCurrent)
        assertNull(extra.resourceSourceCache)
        assertTrue(cache.getStatus(extra.symbol) is CfirImplicitBodyResolveComputationStatus.NotComputed)
        assertFalse(f.rootCandidate.onPCLACompletionResultsWritingCallbacks.any { it === task })
        assertEquals(listOf(f.resource), f.tryExpression.resources)
    }

    @Test
    fun privateTreeSourceCompletionLeavesCanonicalGraphForItsFormalWriter() {
        val f = Fixture(retained = true, privateTree = true)
        val privateTask = f.resolveSource()
        f.writeFull()
        assertTrue(privateTask.isCompleted)
        val canonical = f.canonicalResource
        f.useCanonicalSourceOwner()
        val canonicalTask = f.resolveSource()
        assertNotSame(privateTask, canonicalTask)
        f.writeFull()
        assertTrue(canonicalTask.isCompleted)
        assertSame(canonical, f.resource)
        assertThrows(IllegalStateException::class.java) { f.newFinalWriter() }
    }

    @Test
    fun childSourceTaskTransfersToActualParentAndRunsAfterItsCallWriter() {
        val f = Fixture(pending = true)
        f.resolveSource()
        val sourceCall = f.resolvedInitializer()
        val privateCall = f.resolvedInitializer()
        val resource = f.newResource(f.implementationType, sourceCall, "child")
        val registry = IdentityHashMap<CfirResourceVariable, CfirImplicitTypeComputation>()
        val graph = CfirImplicitTypeComputation(resource, privateCall, mapOf<CfirElement, CfirElement>(sourceCall to privateCall),
            emptyMap(), null, registry, setOf(checkNotNull(f.pendingVariable).typeConstructor))
        registry[resource] = graph
        val scope = CfirImplicitTypeComputationScope(graph, CfirImplicitTypeComputationMode.FORMAL)
        val childContext = BodyResolveContext(ReturnTypeCalculator.Default, CfirDataFlowAnalyzerContext()).apply {
            file = f.context.file
            inferenceSession = f.pcla
            implicitTypeComputationScope = scope
        }
        val info = CallInfo(callSite = sourceCall, callKind = CallKind.Function, name = Name.identifier("make"),
            explicitReceiver = null, arguments = emptyList(), isUsedAsGetClassReceiver = false, typeArguments = emptyList(),
            session = f.session, containingFile = childContext.file, containingDeclarations = emptyList(), resolutionMode = ResolutionMode.ContextIndependent)
        val childRoot = f.resolvedInitializer()
        val childCandidate = Candidate(symbol = f.functionSymbol, dispatchReceiver = null, givenExtensionReceiver = null,
            explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            constraintSystemFactory = f.session.inferenceComponents.constraintSystemFactory,
            baseSystem = f.pcla.currentCommonSystem.captureStorage(), callInfo = CallInfo(
                callSite = childRoot, callKind = CallKind.Function, name = Name.identifier("make"),
                explicitReceiver = null, arguments = emptyList(), isUsedAsGetClassReceiver = false, typeArguments = emptyList(),
                session = f.session, containingFile = childContext.file, containingDeclarations = emptyList(),
                resolutionMode = ResolutionMode.ContextIndependent), originScope = null, bodyResolveContext = childContext).apply {
            initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList())
            initializeArgumentMapping(emptyList(), linkedMapOf())
        }
        graph.constraintSystemOwner = f.pcla.currentCommonSystem
        val child = f.pcla.createChildSession(childCandidate)
        childContext.inferenceSession = child
        val sourceCandidate = Candidate(symbol = f.functionSymbol, dispatchReceiver = null, givenExtensionReceiver = null,
            explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            constraintSystemFactory = f.session.inferenceComponents.constraintSystemFactory,
            baseSystem = child.currentCommonSystem.captureStorage(), callInfo = info, originScope = null, bodyResolveContext = childContext).apply {
            initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList())
            initializeArgumentMapping(emptyList(), linkedMapOf())
        }
        graph.recordCallAttempt(scope, sourceCandidate, CfirRetainedCallOutcome.PARTIAL)
        sourceCall.replaceCalleeReference(CfirNamedReferenceWithCandidate(sourceCall.source, Name.identifier("make"), sourceCandidate))
        childCandidate.postponedPCLACalls += ConeAtomWithCandidate(sourceCall, sourceCandidate)
        val cache = ImplicitBodyResolveComputationSession()
        resource.resourceSourceCache = cache
        val task = CfirResourceSourceCompletion(resource, f.tryExpression, f.resourceType, cache, graph, null)
        resource.resourceSourceCompletion = task
        graph.bindFormalResourceSource(task)
        cache.previewResourceSource(resource)
        child.registerResourceSourceCompletion(task)
        child.applyResultsToMainCandidate()
        var callWritten = false
        f.pcla.integrateRetainedChildSession(graph, child, childCandidate,
            writeCall = { call, substitutor ->
                call.expression.transformSingle(f.transformer.components.callCompleter.createCompletionResultsWriter(
                    substitutor, completionSystem = f.rootCandidate.system, completionRoot = call.expression,
                    completionCandidate = (call as ConeAtomWithCandidate).candidate), null)
                callWritten = true
                assertFalse(task.isCompleted)
            },
            writeAdditionalResults = { callbacks, lambdas, substitutor ->
                f.transformer.components.callCompleter.createCompletionResultsWriter(substitutor).writePCLAAdditionalResults(callbacks, lambdas)
            })
        assertFalse(childCandidate.onPCLACompletionResultsWritingCallbacks.any { it === task })
        assertTrue(task.isOwnedBy(f.rootCandidate))
        f.writeFull()
        assertTrue(callWritten)
        assertTrue(task.isCompleted)
        assertTrue(cache.getStatus(resource.symbol) is CfirImplicitBodyResolveComputationStatus.Computed)
        assertFalse(f.rootCandidate.postponedPCLACalls.any { it.expression === sourceCall })
    }

    @Test
    fun rawCopyDoesNotCarrySourceCompletionCapabilityOrItsCache() {
        val f = Fixture()
        val raw = f.newResource(f.implementationType, buildFunctionCall {
            calleeReference = buildNamedReference { name = Name.identifier("make") }
        }, "raw", CfirResolvePhase.RAW_CFIR)
        val cache = ImplicitBodyResolveComputationSession()
        raw.resourceSourceCache = cache
        val task = CfirResourceSourceCompletion(raw, f.tryExpression, f.resourceType, cache, null, null)
        raw.resourceSourceCompletion = task
        f.pcla.registerResourceSourceCompletion(task)
        val borrowed = (f.typeDeclarations + f.functionSymbol.cfir).toSet<CfirElement>()
        val symbols = borrowed.filterIsInstance<CfirDeclaration>().mapTo(hashSetOf()) { it.symbol }
        val handles = f.typeDeclarations.mapTo(hashSetOf<Any>()) { it.scopeProvider }.apply { add(f.session.moduleData) }
        val context = CfirDefaultRawCopyContext(CfirRawCopyState(borrowed, symbols), handles)
        val copy = DeepCopyCfirTreeWithSymbols(context).copyRoot(raw)
        assertNotSame(raw, copy)
        assertNull(copy.resourceSourceCompletion)
        assertNull(copy.resourceSourceCache)
        assertTrue(task.isCurrent)
        assertTrue(task.isOwnedBy(f.rootCandidate))
    }

    @Test
    fun sourceCallbackPrecedesLaterObserverInFifoOrder() {
        val f = Fixture(pending = true)
        val source = f.resolveSource()
        var observed = false
        f.rootCandidate.onPCLACompletionResultsWritingCallbacks += {
            assertTrue(source.isCompleted, "FIFO observer ran before the earlier Source callback")
            assertTrue(f.cache().getStatus(f.resource.symbol) is CfirImplicitBodyResolveComputationStatus.Computed)
            observed = true
        }
        f.writeFull()
        assertTrue(observed)
    }

    @Test
    fun canceledSourceCannotRetireFromCapturedFinalBatch() {
        val f = Fixture(retained = true)
        val source = f.resolveSource()
        f.rootCandidate.onPCLACompletionResultsWritingCallbacks.add(0) { source.cancel() }
        f.writeFull()
        assertFalse(source.isCurrent)
        assertFalse(source.isCompleted)
        assertDoesNotThrow { f.newFinalWriter() }
    }

    @Test
    fun twoSourceSlotsCompleteBeforeSharedGraphRetirement() {
        val f = Fixture(retained = true)
        val first = f.resolveSource()
        val extra = f.newResource(f.implementationType, f.resolvedInitializer(), "second")
        f.tryExpression.replaceResources(f.tryExpression.resources + extra)
        val cache = ImplicitBodyResolveComputationSession()
        extra.resourceSourceCache = cache
        val second = CfirResourceSourceCompletion(extra, f.tryExpression, f.resourceType, cache, f.graph, null)
        extra.resourceSourceCompletion = second
        cache.previewResourceSource(extra)
        checkNotNull(f.graph).bindFormalResourceSource(second)
        f.rootCandidate.onPCLACompletionResultsWritingCallbacks += {
            assertTrue(first.isCompleted)
            assertFalse(second.isCompleted)
            // Source1 之后、Source2 之前，图还应允许 PRIVATE 域取得真实 lease。
            val scope = CfirImplicitTypeComputationScope(f.graph, CfirImplicitTypeComputationMode.PRIVATE)
            assertDoesNotThrow { f.graph.captureCompletionLease(scope, f.privateCall, f.rootCandidate.system) }
        }
        f.pcla.registerResourceSourceCompletion(second)
        f.rootCandidate.onPCLACompletionResultsWritingCallbacks += {
            assertTrue(second.isCompleted)
            assertTrue(cache.getStatus(extra.symbol) is CfirImplicitBodyResolveComputationStatus.Computed)
        }
        f.writeFull()
        assertTrue(first.isCompleted && second.isCompleted)
        assertThrows(IllegalStateException::class.java) { f.newFinalWriter() }
    }

    @Test
    fun reinferenceRevokesOldWriterForRestoredSourceCapability() {
        val f = Fixture(pending = true, retained = true)
        val source = f.resolveSource()
        val replay = checkNotNull(f.graph).captureReplayState(checkNotNull(f.scope), f.rootCall)
        f.completeActualSystem()
        val oldWriter = f.newFinalWriter()
        replay.restoreForReinference(f.rootCandidate.system)
        f.rootCall.transformSingle(oldWriter, null)
        assertFalse(source.isCompleted)
        assertTrue(source.isOwnedBy(f.rootCandidate))
        assertTrue(f.cache().getStatus(f.resource.symbol) is CfirImplicitBodyResolveComputationStatus.ComputedType)
        f.rootCall.transformSingle(f.newFinalWriter(), null)
        assertTrue(source.isCompleted)
    }

    @Test
    fun noProxyCacheFallbackForActualSourceStatus() {
        val f = Fixture(retained = true)
        val source = f.resolveSource()
        val actualCache = f.cache()
        val foreignCache = ImplicitBodyResolveComputationSession()
        foreignCache.publishResourceSource(f.resource)
        f.resource.resourceSourceCache = foreignCache
        f.writeFull()
        assertFalse(source.isCompleted)
        assertTrue(actualCache.getStatus(f.resource.symbol) is CfirImplicitBodyResolveComputationStatus.ComputedType)
        assertDoesNotThrow { f.newFinalWriter() }
        f.resource.resourceSourceCache = actualCache
        f.rootCall.transformSingle(f.newFinalWriter(), null)
        assertTrue(source.isCompleted)
    }

    @Test
    fun reinferenceDuringFifoCallbackRevokesRemainingBatchWrites() {
        val f = Fixture(pending = true, retained = true)
        val source = f.resolveSource()
        val replay = checkNotNull(f.graph).captureReplayState(checkNotNull(f.scope), f.rootCall)
        f.rootCandidate.onPCLACompletionResultsWritingCallbacks.add(0) {
            replay.restoreForReinference(f.rootCandidate.system)
        }
        assertDoesNotThrow { f.writeFull() }
        assertFalse(source.isCompleted)
        assertTrue(f.resource.returnTypeRef.coneTypeOrNull is ConeTypeVariableType)
        assertTrue(f.cache().getStatus(f.resource.symbol) is CfirImplicitBodyResolveComputationStatus.ComputedType)
        f.rootCall.transformSingle(f.newFinalWriter(), null)
        assertTrue(source.isCompleted)
    }

    @Test
    fun determinateSourceErrorCannotRetireOutstandingFormalPartialCall() {
        val f = Fixture(retained = true)
        val error = ConeErrorType(ConeSimpleDiagnostic("source rejected"))
        f.resource.replaceReturnTypeRef(error.toCfirResolvedTypeRef())
        f.rootCall.replaceConeTypeOrNull(error)
        assertThrows(IllegalStateException::class.java) {
            checkNotNull(f.graph).retirePrivateResults(null, sourceRejected = f.resource.returnTypeRef.coneTypeOrNull is ConeErrorType)
        }
        assertDoesNotThrow { f.newFinalWriter() }
    }

    @Test
    fun privateThenFormalSourceInSameBatchRetiresCanonicalGraph() {
        val f = Fixture(retained = true)
        val privateSource = f.resolvePrivateSourceInCurrentPcla()
        val formalSource = f.resolveSource()
        assertSame(privateSource.graph, formalSource.graph)
        assertNotSame(privateSource.resource, formalSource.resource)
        assertTrue(privateSource.isOwnedBy(f.rootCandidate))
        assertTrue(formalSource.isOwnedBy(f.rootCandidate))
        assertEquals(listOf(privateSource, formalSource),
            f.rootCandidate.onPCLACompletionResultsWritingCallbacks.filterIsInstance<CfirResourceSourceCompletion>())
        f.rootCandidate.onPCLACompletionResultsWritingCallbacks += {
            assertTrue(privateSource.isCompleted && formalSource.isCompleted)
            assertTrue(privateSource.resource.resourceSourceCache!!.getStatus(privateSource.resource.symbol)
                is CfirImplicitBodyResolveComputationStatus.Computed)
            assertTrue(f.cache().getStatus(f.resource.symbol) is CfirImplicitBodyResolveComputationStatus.Computed)
        }
        f.writeFull()
        assertTrue(privateSource.isCompleted && formalSource.isCompleted)
        // 真正 root writer 已返回；同图 PRIVATE 槽不能吞掉后续 canonical FORMAL 的退休。
        assertThrows(IllegalStateException::class.java) { f.newFinalWriter() }
    }


    @Test
    fun registeredPrivateAdditionalProducerFinishesBeforeSourceAndItsFifoObserver() {
        val f = Fixture(pending = true, retained = true)
        prepareRootLifecycleParent(f)
        val source = f.resolveSource()
        f.completeActualSystem()
        val probe = RootLifecycleProbe(f, checkNotNull(f.graph), f.resource, f.privateCall)
        probe.start()
        val events = mutableListOf<String>()
        installRootAdditionalProducer(f, probe) {
            assertFalse(source.isCompleted)
            assertTrue(f.cache().getStatus(f.resource.symbol) is CfirImplicitBodyResolveComputationStatus.ComputedType)
            assertRootAdditionalVm(f)
            events += "private"
        }
        f.rootCandidate.onPCLACompletionResultsWritingCallbacks += {
            assertTrue(source.isCompleted)
            assertTrue(f.cache().getStatus(f.resource.symbol) is CfirImplicitBodyResolveComputationStatus.Computed)
            probe.assertClosed()
            events += "observer"
        }
        f.writeFull()
        assertEquals(listOf("private", "observer"), events)
        assertEquals(f.implementationType, f.resource.returnTypeRef.coneType)
        probe.assertClosed()
    }

    @Test
    fun provisionalPrivateAdditionalInvocationDoesNotCompleteConsumeOrCloseItsRoot() {
        val f = Fixture(pending = true, retained = true)
        prepareRootLifecycleParent(f)
        val source = f.resolveSource()
        f.completeActualSystem()
        val probe = RootLifecycleProbe(f, checkNotNull(f.graph), f.resource, f.privateCall)
        probe.start()
        var written = 0
        val producer = installRootAdditionalProducer(f, probe) {
            assertFalse(source.isCompleted)
            assertRootAdditionalVm(f)
            written++
        }
        producer(f.substitutor())
        assertEquals(1, written)
        assertFalse(source.isCompleted)
        assertTrue(f.rootCandidate.onPCLACompletionResultsWritingCallbacks.any { it === producer })
        assertThrows(IllegalStateException::class.java) {
            checkNotNull(f.graph).validatePrivateAdditionalResultsCompletion(f.rootCandidate.system)
        }
        probe.withRoot { assertSame(f.resource, probe.builder.currentGraph.declaration) }
        f.writeFull()
        assertEquals(2, written)
        assertTrue(source.isCompleted)
        probe.assertClosed()
    }

    @Test
    fun pendingAdditionalMementoRestoresItsQueueAndRevokesTheHeldDispatcher() {
        val f = Fixture(pending = true, retained = true)
        prepareRootLifecycleParent(f)
        val source = f.resolveSource()
        f.completeActualSystem()
        val probe = RootLifecycleProbe(f, checkNotNull(f.graph), f.resource, f.privateCall)
        probe.start()
        var written = 0
        val held = installRootAdditionalProducer(f, probe) { assertRootAdditionalVm(f); written++ }
        val before = f.rootCandidate.system.captureStorage()
        val beforeCache = f.cache()
        val ast = CfirResolutionSnapshot.capture(f.tryExpression)
        val pcla = f.rootCandidate.capturePCLACompletionState()
        val replay = probe.graph.captureReplayState(probe.scope, f.privateCall)
        val transaction = f.rootCandidate.system.prepareTransaction()
        f.writeFull()
        assertEquals(1, written)
        assertTrue(source.isCompleted)
        probe.assertClosed()
        transaction.rollbackTransaction()
        ast.restoreWithImplicitBodyCache(null, null)
        replay.restoreForRollback()
        // 候选 memento 后恢复也可能重新装入旧 callback；factory 必须续发有效 handle，不能复活它。
        f.rootCandidate.restorePCLACompletionState(pcla)
        assertFalse(source.isCompleted)
        held(f.substitutor())
        assertEquals(1, written)
        assertSame(beforeCache, f.cache())
        assertTrue(f.cache().getStatus(f.resource.symbol) is CfirImplicitBodyResolveComputationStatus.ComputedType)
        assertRootAdditionalVm(f)
        before.allTypeVariables.forEach { (constructor, variable) ->
            assertSame(variable, f.rootCandidate.system.captureStorage().allTypeVariables[constructor])
        }
        f.writeFull()
        assertEquals(2, written)
        assertTrue(source.isCompleted)
        probe.assertClosed()
    }

    @Test
    fun completedAdditionalFactSurvivesSuccessfulMementoWithoutReexecutingItsDispatcher() {
        val f = Fixture(pending = true, retained = true)
        prepareRootLifecycleParent(f)
        val source = f.resolveSource()
        f.completeActualSystem()
        val probe = RootLifecycleProbe(f, checkNotNull(f.graph), f.resource, f.privateCall)
        probe.start()
        var written = 0
        val held = installRootAdditionalProducer(f, probe) { assertRootAdditionalVm(f); written++ }
        held.complete(f.rootCandidate.system, f.substitutor())
        assertEquals(1, written)
        assertFalse(source.isCompleted)
        probe.graph.validatePrivateAdditionalResultsCompletion(f.rootCandidate.system)
        val ast = CfirResolutionSnapshot.capture(f.tryExpression)
        val pcla = f.rootCandidate.capturePCLACompletionState()
        val replay = probe.graph.captureReplayState(probe.scope, f.privateCall)
        val transaction = f.rootCandidate.system.prepareTransaction()
        f.writeFull()
        assertTrue(source.isCompleted)
        probe.assertClosed()
        transaction.rollbackTransaction()
        ast.restoreWithImplicitBodyCache(null, null)
        replay.restoreForRollback()
        f.rootCandidate.restorePCLACompletionState(pcla)
        assertFalse(source.isCompleted)
        held(f.substitutor())
        assertEquals(1, written)
        probe.graph.validatePrivateAdditionalResultsCompletion(f.rootCandidate.system)
        f.writeFull()
        assertEquals(1, written)
        assertTrue(source.isCompleted)
        assertTrue(f.cache().getStatus(f.resource.symbol) is CfirImplicitBodyResolveComputationStatus.Computed)
        probe.assertClosed()
    }

    @Test
    fun latePrivateAdditionalWorkCompletesInTheSameActualFinalDrain() {
        val f = Fixture(pending = true, retained = true)
        prepareRootLifecycleParent(f)
        val source = f.resolveSource()
        f.completeActualSystem()
        val graph = checkNotNull(f.graph)
        val probe = RootLifecycleProbe(f, graph, f.resource, f.privateCall)
        probe.start()
        val events = mutableListOf<String>()
        installRootAdditionalProducer(f, probe) {
            assertFalse(source.isCompleted)
            assertRootAdditionalVm(f)
            events += "first"
            val before = f.rootCandidate.capturePCLACompletionState()
            val variables = f.rootCandidate.system.captureStorage().allTypeVariables.keys.toSet()
            val selected = graph.selectedCandidates().toSet()
            f.rootCandidate.onPCLACompletionResultsWritingCallbacks += {
                assertFalse(source.isCompleted)
                assertRootAdditionalVm(f)
                events += "late"
            }
            graph.capturePrivateAnalysisResults(f.rootCandidate, before, variables, selected)
        }
        f.rootCandidate.onPCLACompletionResultsWritingCallbacks += {
            assertTrue(source.isCompleted)
            events += "observer"
        }
        f.writeFull()
        assertEquals(listOf("first", "late", "observer"), events)
        assertEquals(f.implementationType, f.resource.returnTypeRef.coneType)
        probe.assertClosed()
    }

    /** 实际 PRIVATE root、child PCLA 移交及 final writer；不直接设置 graph 的 completion outcome/owner。 */
    private fun installRootAdditionalProducer(
        f: Fixture,
        probe: RootLifecycleProbe,
        callback: (ConeSubstitutor) -> Unit,
    ): CfirRetainedAdditionalResultsProducer {
        val info = CallInfo(callSite = f.privateCall, callKind = CallKind.Function, name = f.functionSymbol.cfir.name,
            explicitReceiver = null, arguments = emptyList(), isUsedAsGetClassReceiver = false, typeArguments = emptyList(),
            session = f.session, containingFile = probe.context.file, containingDeclarations = probe.context.containers.toList(),
            resolutionMode = ResolutionMode.ContextIndependent)
        val candidate = Candidate(symbol = f.functionSymbol, dispatchReceiver = null, givenExtensionReceiver = null,
            explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            constraintSystemFactory = f.session.inferenceComponents.constraintSystemFactory,
            baseSystem = f.pcla.currentCommonSystem.captureStorage(), callInfo = info, originScope = null,
            bodyResolveContext = probe.context).apply {
            typeArgumentMapping = org.cangnova.cangjie.cfir.resolve.calls.stages.TypeArgumentMapping.NoExplicitArguments
            initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList())
            initializeArgumentMapping(emptyList(), linkedMapOf())
        }
        val child = f.pcla.createChildSession(candidate)
        probe.context.inferenceSession = child
        candidate.onPCLACompletionResultsWritingCallbacks += { substitutor ->
            probe.withRoot { callback(substitutor) }
        }
        child.applyResultsToMainCandidate()
        f.pcla.integrateRetainedChildSession(probe.graph, child, candidate,
            writeCall = probe::writeRetainedCall,
            writeAdditionalResults = probe::writeAdditionalResults)
        assertTrue(f.rootCandidate.retainedChildSessions.any { it === probe.graph })
        return checkNotNull(probe.graph.completionCallback as? CfirRetainedAdditionalResultsProducer)
    }

    /** 非空实际 Resource TV 的 VM/固定解 oracle，不能把空系统或默认类型当作完成。 */
    private fun assertRootAdditionalVm(f: Fixture) {
        val variable = checkNotNull(f.pendingVariable)
        val actual = f.rootCandidate.withConstraintSystemForCompletionResults { it.captureStorage() }
        assertSame(variable, actual.allTypeVariables[variable.typeConstructor])
        assertEquals(f.implementationType, actual.fixedTypeVariables[variable.typeConstructor])
        assertTrue(actual.notFixedTypeVariables.isEmpty())
        assertTrue(actual.constraintsFromAllForkPoints.isEmpty())
    }

    @Test
    fun privateRootSourceFinishThenReinferenceRunsANewActualProducer() {
        val f = Fixture(pending = true, retained = true)
        prepareRootLifecycleParent(f)
        val source = f.resolveSource()
        f.completeActualSystem()
        val system = f.rootCandidate.system
        val variable = checkNotNull(f.pendingVariable)
        val probe = RootLifecycleProbe(f, checkNotNull(f.graph), f.resource, f.privateCall)
        probe.start()
        val ast = CfirResolutionSnapshot.capture(f.tryExpression)
        val replay = probe.graph.captureReplayState(probe.scope, f.privateCall)
        val oldWriter = f.newFinalWriter()
        f.rootCall.transformSingle(oldWriter, null)
        assertTrue(source.isCompleted)
        assertTrue(f.cache().getStatus(f.resource.symbol) is CfirImplicitBodyResolveComputationStatus.Computed)
        probe.assertClosed()

        ast.restoreWithImplicitBodyCache(null, null)
        replay.restoreForReinference(system)
        assertSame(system, f.rootCandidate.system)
        assertSame(variable, system.allTypeVariables[variable.typeConstructor])
        assertEquals(f.implementationType, system.fixedTypeVariables[variable.typeConstructor])
        assertFalse(source.isCompleted)
        val reference = f.rootCall.calleeReference
        f.rootCall.transformSingle(oldWriter, null)
        assertSame(reference, f.rootCall.calleeReference)
        assertFalse(source.isCompleted)

        val next = probe.newCall(f.privateCall)
        probe.produce(next)
        assertSame(next, probe.graph.initializer)
        assertTrue(f.cache().getStatus(f.resource.symbol) is CfirImplicitBodyResolveComputationStatus.ComputedType)
        f.writeFull()
        assertTrue(source.isCompleted)
        assertSame(f.resource, (f.cache().getStatus(f.resource.symbol) as CfirImplicitBodyResolveComputationStatus.Computed).transformedDeclaration)
        probe.assertClosed()
    }

    @Test
    fun privateRootSourceFinishThenRollbackRestoresItsProducerCursor() {
        val f = Fixture(retained = true)
        prepareRootLifecycleParent(f)
        val source = f.resolveSource()
        val probe = RootLifecycleProbe(f, checkNotNull(f.graph), f.resource, f.privateCall)
        probe.start()
        val ast = CfirResolutionSnapshot.capture(f.tryExpression)
        val replay = probe.graph.captureReplayState(probe.scope, f.privateCall)
        f.writeFull()
        assertTrue(source.isCompleted)
        probe.assertClosed()
        ast.restoreWithImplicitBodyCache(null, null)
        replay.restoreForRollback()
        assertFalse(source.isCompleted)
        probe.produce(probe.newCall(f.privateCall))
        f.writeFull()
        assertTrue(source.isCompleted)
        probe.assertClosed()
    }

    @Test
    fun nestedResourceRootRejectsItsAncestorAsTheActiveProducerOwner() {
        val f = Fixture(retained = true)
        prepareRootLifecycleParent(f)
        val probe = RootLifecycleProbe(f, checkNotNull(f.graph), f.resource, f.privateCall)
        probe.start()
        val inner = f.newResource(f.implementationType, f.resolvedInitializer(), resourceName = f.resource.name.asString())
        probe.withRoot {
            val outerGraph = probe.builder.currentGraph
            probe.builder.enterResourceInitializer(inner)
            val innerGraph = probe.builder.currentGraph
            var wrongOwnerRan = false
            assertThrows(IllegalStateException::class.java) {
                probe.builder.withResourceInitializer(f.resource) { wrongOwnerRan = true }
            }
            assertFalse(wrongOwnerRan)
            assertSame(inner, innerGraph.declaration)
            assertNotSame(outerGraph, innerGraph)
            probe.builder.suspendResourceInitializer(inner)
            probe.builder.withResourceInitializer(inner) {
                assertSame(innerGraph, probe.builder.currentGraph)
            }
            probe.builder.finishResourceInitializer(inner)
            assertSame(outerGraph, probe.builder.currentGraph)
        }
        probe.withRoot { assertSame(f.resource, probe.builder.currentGraph.declaration) }
    }

    @Test
    fun sourceFinishAndReinferenceOfOneRootPreserveItsSiblingRootAndVm() {
        val f = Fixture(pending = true, retained = true)
        prepareRootLifecycleParent(f)
        val source = f.resolveSource()
        f.completeActualSystem()
        val system = f.rootCandidate.system
        val variable = checkNotNull(f.pendingVariable)
        val first = RootLifecycleProbe(f, checkNotNull(f.graph), f.resource, f.privateCall)
        first.start()
        val siblingSourceCall = f.resolvedInitializer()
        val siblingPrivateCall = buildFunctionCallCopy(siblingSourceCall) {
            calleeReference = buildNamedReference { name = f.functionSymbol.cfir.name }
        }
        val siblingResource = f.newResource(f.implementationType, siblingSourceCall, resourceName = f.resource.name.asString())
        val siblingGraph = CfirImplicitTypeComputation(siblingResource, siblingPrivateCall,
            mapOf<CfirElement, CfirElement>(siblingSourceCall to siblingPrivateCall), emptyMap(), null,
            first.graph.registry, emptySet())
        assertNull(first.graph.registry.put(siblingResource, siblingGraph))
        val sibling = RootLifecycleProbe(f, siblingGraph, siblingResource, siblingPrivateCall)
        sibling.start()
        var siblingRoot: ControlFlowGraph? = null
        var siblingCursor: org.cangnova.cangjie.cfir.resolve.dfa.cfg.CFGNode<*>? = null
        sibling.withRoot {
            siblingRoot = sibling.builder.currentGraph
            siblingCursor = sibling.builder.lastNode
        }
        val ast = CfirResolutionSnapshot.capture(f.tryExpression)
        val replay = first.graph.captureReplayState(first.scope, f.privateCall)
        f.writeFull()
        assertTrue(source.isCompleted)
        first.assertClosed()
        ast.restoreWithImplicitBodyCache(null, null)
        replay.restoreForReinference(system)
        assertSame(siblingGraph, first.graph.registry[siblingResource])
        sibling.withRoot {
            assertSame(siblingRoot, sibling.builder.currentGraph)
            assertSame(siblingCursor, sibling.builder.lastNode)
        }
        sibling.produce(sibling.newCall(siblingPrivateCall))
        assertSame(variable, system.allTypeVariables[variable.typeConstructor])
        assertEquals(f.implementationType, system.fixedTypeVariables[variable.typeConstructor])
        first.produce(first.newCall(f.privateCall))
        f.writeFull()
        assertTrue(source.isCompleted)
        sibling.withRoot { assertSame(siblingRoot, sibling.builder.currentGraph) }
    }

    @Test
    fun privateRootInheritsPublishedFlowAndDoesNotConsumeFormalResourceOrder() {
        val f = Fixture(retained = true)
        val host = prepareRootLifecycleParent(f)
        val parent = f.context.dataFlowAnalyzerContext.graphBuilder
        val variable = SyntheticVariable(CallResolutionTestFixtures.buildTypedExpression(ConePrimitiveType.INT32))
        val logic = object : LogicSystem(f.session.typeContext) {}
        val mutable = MutableFlow()
        logic.addTypeStatement(mutable, variable typeEq ConePrimitiveType.INT32)
        val flow = mutable.freeze()
        parent.lastNode.flow = flow
        val parentGraph = parent.currentGraph
        val parentCursor = parent.lastNode
        val probe = RootLifecycleProbe(f, checkNotNull(f.graph), f.resource, f.privateCall)
        probe.start()
        probe.withRoot {
            val entry = probe.builder.currentGraph.enterNode
            assertSame(flow, entry.flow)
            assertTrue(entry.flow.knownVariables.contains(variable))
            assertTrue(checkNotNull(entry.flow.getTypeStatement(variable)).upperTypes.contains(ConePrimitiveType.INT32))
            assertSame(host, probe.context.containers.last())
        }
        assertSame(parentGraph, parent.currentGraph)
        assertSame(parentCursor, parent.lastNode)
        assertSame(flow, parent.lastNode.flow)
        // PRIVATE exit 不得把继承的 formal Main cursor 推进；复制入口与原入口都仍能完成首资源。
        probe.dataFlowAnalyzer.enterVariableDeclaration(f.resource)
        probe.dataFlowAnalyzer.exitVariableDeclaration(f.resource)
        f.transformer.dataFlowAnalyzer.enterVariableDeclaration(f.resource)
        f.transformer.dataFlowAnalyzer.exitVariableDeclaration(f.resource)
        // 两个正式 try 必须仍有自己的实参/推迟流帧；PRIVATE call 不能借用并消耗父帧。
        probe.dataFlowAnalyzer.exitTryMainBlock()
        probe.dataFlowAnalyzer.exitTryExpression(callCompleted = true)
        f.transformer.dataFlowAnalyzer.exitTryMainBlock()
        f.transformer.dataFlowAnalyzer.exitTryExpression(callCompleted = true)
    }

    /** 正式父图/词法入口使用真实owner，不把单独的transformer当作隔离证明。 */
    private fun prepareRootLifecycleParent(f: Fixture): CfirNamedFunction {
        // 本组使用真实 LogicSystem；补齐其 session 组件，不改共享 Fixture 或任何 Source origin。
        f.session.register(CfirBuiltinTypes::class, CfirBuiltinTypes())
        val host = CallResolutionTestFixtures.buildFunctionSymbol(f.session, "rootLifecycleHost", ConePrimitiveType.UNIT).cfir
        f.context.containers.addLast(host)
        f.transformer.dataFlowAnalyzer.enterFunction(host)
        f.transformer.dataFlowAnalyzer.enterTryExpression(f.tryExpression)
        return host
    }

    /** 复用已有Source语义fixture；根、PRIVATE producer/FULL writer和Source发布均为生产API。 */
    private class RootLifecycleProbe(
        private val f: Fixture,
        val graph: CfirImplicitTypeComputation,
        private val owner: CfirResourceVariable,
        private val initialCall: CfirFunctionCall,
    ) {
        val scope = CfirImplicitTypeComputationScope(graph, CfirImplicitTypeComputationMode.PRIVATE)
        val context = f.context.createLocalDeclarationContext(ReturnTypeCalculator.Default).also {
            it.implicitTypeComputationScope = scope
        }
        private val transformer = CfirBodyResolveTransformer(f.session, ScopeSession(), outerBodyResolveContext = context)
        val builder get() = context.dataFlowAnalyzerContext.graphBuilder
        val dataFlowAnalyzer get() = transformer.dataFlowAnalyzer

        fun start() {
            dataFlowAnalyzer.enterResourceInitializer(owner)
            graph.registerPrivateInitializerContext(owner, context, dataFlowAnalyzer)
            dataFlowAnalyzer.enterVariableDeclaration(owner)
            produce(initialCall)
            dataFlowAnalyzer.exitVariableDeclaration(owner)
            dataFlowAnalyzer.suspendResourceInitializer(owner)
        }

        fun <T> withRoot(action: () -> T): T = dataFlowAnalyzer.withResourceInitializer(owner, action)

        fun writeRetainedCall(call: org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom, substitutor: ConeSubstitutor) {
            withRoot {
                call.expression.transformSingle(transformer.components.callCompleter.createCompletionResultsWriter(
                    substitutor, completionSystem = checkNotNull(graph.constraintSystemOwner),
                    completionRoot = call.expression, completionCandidate = (call as? ConeAtomWithCandidate)?.candidate,
                ), null)
            }
        }

        fun writeAdditionalResults(callbacks: List<(ConeSubstitutor) -> Unit>, lambdas: List<CfirDeclaration>, substitutor: ConeSubstitutor) =
            withRoot {
                transformer.components.callCompleter.createCompletionResultsWriter(substitutor)
                    .writePCLAAdditionalResults(callbacks, lambdas)
            }

        fun assertClosed() {
            var ran = false
            assertThrows(IllegalStateException::class.java) { withRoot { ran = true } }
            assertFalse(ran)
        }

        fun newCall(previous: CfirFunctionCall): CfirFunctionCall = buildFunctionCallCopy(previous) {
            calleeReference = buildNamedReference { name = f.functionSymbol.cfir.name }
        }.also { scope.registerDerivedElement(previous, it) }

        fun produce(call: CfirFunctionCall) = withRoot {
            assertSame(owner, builder.currentGraph.declaration)
            assertEquals(ControlFlowGraph.Kind.ResourceInitializer, builder.currentGraph.kind)
            graph.initializer = call
            // 对位正式 expression resolver：实参阶段拥有自己的 CFG 和 assignment 帧，之后才进入 callee。
            dataFlowAnalyzer.enterCallArguments(call, call.argumentList.arguments)
            dataFlowAnalyzer.exitCallExplicitReceiver()
            dataFlowAnalyzer.exitCallArguments()
            val parentStorage = f.rootCandidate.system.captureStorage()
            val info = CallInfo(callSite = call, callKind = CallKind.Function, name = f.functionSymbol.cfir.name,
                explicitReceiver = null, arguments = emptyList(), isUsedAsGetClassReceiver = false, typeArguments = emptyList(),
                session = f.session, containingFile = context.file, containingDeclarations = context.containers.toList(),
                resolutionMode = ResolutionMode.ContextIndependent)
            val candidate = Candidate(symbol = f.functionSymbol, dispatchReceiver = null, givenExtensionReceiver = null,
                explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                constraintSystemFactory = f.session.inferenceComponents.constraintSystemFactory,
                baseSystem = parentStorage, callInfo = info, originScope = null,
                bodyResolveContext = context).apply {
                typeArgumentMapping = org.cangnova.cangjie.cfir.resolve.calls.stages.TypeArgumentMapping.NoExplicitArguments
                initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList())
                initializeArgumentMapping(emptyList(), linkedMapOf())
                additionalCompletionVariables += f.rootCandidate.additionalCompletionVariables
            }
            call.replaceCalleeReference(CfirNamedReferenceWithCandidate(call.source, f.functionSymbol.cfir.name, candidate))
            dataFlowAnalyzer.enterFunctionCall(call)
            val actualFull = transformer.components.callCompleter.runFullCompletionForCall(candidate, call)
            val completedStorage = actualFull.system.asReadOnlyStorage()
            assertTrue(completedStorage.notFixedTypeVariables.isEmpty())
            assertTrue(completedStorage.constraintsFromAllForkPoints.isEmpty())
            candidate.additionalCompletionVariables.forEach { constructor ->
                val variable = checkNotNull(parentStorage.allTypeVariables[constructor])
                val binding = checkNotNull(parentStorage.fixedTypeVariables[constructor])
                assertSame(variable, completedStorage.allTypeVariables[constructor])
                assertEquals(binding, completedStorage.fixedTypeVariables[constructor])
            }
            // 保留标准 FULL 的冻结阶段；不可通过省去 asReadOnlyStorage 或解冻来绕过 terminal 读取错误。
            assertThrows(IllegalStateException::class.java) { actualFull.system.getBuilder() }
            val beforeWriting = actualFull.system.captureStorage()
            val substitutor = completedStorage.buildAbstractResultingSubstitutor(f.session.typeContext).asCone()
            call.transformSingle(transformer.components.callCompleter.createCompletionResultsWriterForFullCompletion(actualFull, substitutor), null)
            val afterWriting = actualFull.system.captureStorage()
            assertEquals(beforeWriting.fixedTypeVariables, afterWriting.fixedTypeVariables)
            assertEquals(beforeWriting.allTypeVariables.keys, afterWriting.allTypeVariables.keys)
            assertEquals(beforeWriting.notFixedTypeVariables.keys, afterWriting.notFixedTypeVariables.keys)
            assertEquals(beforeWriting.constraintsFromAllForkPoints, afterWriting.constraintsFromAllForkPoints)
            beforeWriting.allTypeVariables.forEach { (constructor, variable) ->
                assertSame(variable, afterWriting.allTypeVariables[constructor])
            }
            assertThrows(IllegalStateException::class.java) { actualFull.system.getBuilder() }
            dataFlowAnalyzer.exitFunctionCall(call, callCompleted = true)
            assertSame(f.functionSymbol, (call.calleeReference as org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference).resolvedSymbol)
            assertEquals(f.implementationType, call.coneTypeOrNull)
        }
    }

    private class Fixture(pending: Boolean = false, retained: Boolean = false, privateTree: Boolean = false, explicitType: Boolean = false) {
        val session = CallResolutionTestFixtures.newTestSession()
        val resourceType = ConeClassLikeType(ConeClassLikeLookupTagImpl(StdlibClassIds.Resource))
        private val implementationId = ClassId(FqName.ROOT, Name.identifier("R"))
        val implementationType = ConeClassLikeType(ConeClassLikeLookupTagImpl(implementationId))
        val typeDeclarations = listOf<CfirClassLikeDeclaration>(
            ExtendTestFixtures.newInterface(session.moduleData, "Resource", StdlibClassIds.Resource),
            ExtendTestFixtures.newClass(session.moduleData, "R", implementationId,
                superTypeRefs = listOf(resourceType.toCfirResolvedTypeRef())),
        )
        private val source = checkNotNull(CallResolutionTestFixtures.buildTypedExpression(ConePrimitiveType.INT32).source)
        val functionSymbol = CallResolutionTestFixtures.buildFunctionSymbol(session, "make", implementationType)
        val sourceCall = unresolvedInitializer()
        val privateCall = unresolvedInitializer()
        val canonicalResource = newResource(implementationType, sourceCall, typeImplicit = !explicitType)
        private val privateResource = newResource(implementationType, privateCall, typeImplicit = !explicitType)
        var resource = if (privateTree) privateResource else canonicalResource
            private set
        var rootCall = if (privateTree) privateCall else sourceCall
            private set
        private val registry = IdentityHashMap<CfirResourceVariable, CfirImplicitTypeComputation>()
        val graph = if (retained) CfirImplicitTypeComputation(canonicalResource, privateCall,
            mapOf<CfirElement, CfirElement>(canonicalResource to privateResource, sourceCall to privateCall),
            emptyMap(), null, registry, emptySet()).also { registry[canonicalResource] = it } else null
        var scope = graph?.let { CfirImplicitTypeComputationScope(it,
            if (privateTree) CfirImplicitTypeComputationMode.PRIVATE else CfirImplicitTypeComputationMode.FORMAL) }
            private set
        val context = BodyResolveContext(ReturnTypeCalculator.Default, CfirDataFlowAnalyzerContext())
        val transformer: CfirBodyResolveTransformer
        var rootCandidate: Candidate
            private set
        var pcla: CfirPCLAInferenceSession
            private set
        val pendingVariable: ConeTypeVariable?
        val tryExpression = buildTryExpression {
            resources += resource
            tryBlock = buildBlock { coneTypeOrNull = ConePrimitiveType.UNIT }
            coneTypeOrNull = ConePrimitiveType.UNIT
        }

        init {
            session.registerHierarchyProviders(typeDeclarations)
            context.file = CallResolutionTestFixtures.buildCallInfo(session, "make").containingFile
            context.implicitTypeComputationScope = scope
            transformer = CfirBodyResolveTransformer(session, ScopeSession(), outerBodyResolveContext = context)
            rootCandidate = candidate(rootCall)
            pendingVariable = if (pending) ConstraintSystemTestHarness.newVariable(rootCandidate.system, "ResourceType") else null
            if (pendingVariable != null) {
                rootCandidate.additionalCompletionVariables += pendingVariable.typeConstructor
                resource.replaceReturnTypeRef(pendingVariable.defaultType.toCfirResolvedTypeRef(source))
                rootCall.replaceConeTypeOrNull(pendingVariable.defaultType)
            }
            graph?.constraintSystemOwner = rootCandidate.system
            graph?.storeResult(resource.returnTypeRef as CfirResolvedTypeRef)
            pcla = CfirPCLAInferenceSession(rootCandidate, session.inferenceComponents)
            context.inferenceSession = pcla
            rootCall.replaceCalleeReference(CfirNamedReferenceWithCandidate(source, Name.identifier("make"), rootCandidate))
            graph?.recordCallAttempt(checkNotNull(scope), rootCandidate, CfirRetainedCallOutcome.PARTIAL)
        }

        fun resolveSource(): CfirResourceSourceCompletion {
            val calculator = ResourceVariableReturnTypeCalculator(transformer.components, listOf(resource), tryExpression, resourceType)
            calculator.resolveResource(resource) { resource }
            pcla.applyResultsToMainCandidate()
            return checkNotNull(resource.resourceSourceCompletion)
        }

        /** 通过正式 calculator 在同一实际 PCLA 中建立 PRIVATE 槽，随后回到 canonical FORMAL 域。 */
        fun resolvePrivateSourceInCurrentPcla(): CfirResourceSourceCompletion {
            val previous = context.implicitTypeComputationScope
            val privateScope = CfirImplicitTypeComputationScope(checkNotNull(graph), CfirImplicitTypeComputationMode.PRIVATE)
            val privateTry = buildTryExpression {
                resources += privateResource
                tryBlock = buildBlock { coneTypeOrNull = ConePrimitiveType.UNIT }
                coneTypeOrNull = ConePrimitiveType.UNIT
            }
            privateResource.replaceInitializer(resolvedInitializer())
            context.implicitTypeComputationScope = privateScope
            try {
                val calculator = ResourceVariableReturnTypeCalculator(
                    transformer.components, listOf(privateResource), privateTry, resourceType,
                )
                calculator.resolveResource(privateResource) { privateResource }
                pcla.applyResultsToMainCandidate()
                return checkNotNull(privateResource.resourceSourceCompletion)
            } finally {
                context.implicitTypeComputationScope = previous
            }
        }

        fun cache(): ImplicitBodyResolveComputationSession = checkNotNull(resource.resourceSourceCache)
        fun substitutor(): ConeSubstitutor = rootCandidate.system.buildCurrentSubstitutor().asCone()

        fun completeActualSystem(inferredType: ConeCangJieType = implementationType) {
            pendingVariable?.takeIf { it.typeConstructor in rootCandidate.system.notFixedTypeVariables }?.let {
                rootCandidate.system.addSubtypeConstraint(inferredType, it.defaultType, ConeArgumentConstraintPosition(resource))
            }
            ConstraintSystemCompleter(transformer.components).complete(
                rootCandidate.system.asConstraintSystemCompleterContext(), ConstraintSystemCompletionMode.FULL,
                listOf(ConeAtomWithCandidate(rootCall, rootCandidate)), rootCandidate.substitutedReturnType(), transformer.resolutionContext,
            ) { _, _ -> error("Fixture has no postponed lambda") }
            assertTrue(rootCandidate.system.notFixedTypeVariables.isEmpty())
        }

        fun newFinalWriter(): CfirCallCompletionResultsWriterTransformer = transformer.components.callCompleter.createCompletionResultsWriter(
            substitutor(), completionSystem = rootCandidate.system, completionRoot = rootCall, completionCandidate = rootCandidate,
        )
        fun writeFull() { completeActualSystem(); rootCall.transformSingle(newFinalWriter(), null) }

        fun privatePartialAttempt() {
            val previous = context.implicitTypeComputationScope
            val privateScope = CfirImplicitTypeComputationScope(checkNotNull(graph), CfirImplicitTypeComputationMode.PRIVATE)
            context.implicitTypeComputationScope = privateScope
            try { graph.recordCallAttempt(privateScope, candidate(privateCall), CfirRetainedCallOutcome.PARTIAL) }
            finally { context.implicitTypeComputationScope = previous }
        }

        fun useCanonicalSourceOwner() {
            resource = canonicalResource
            rootCall = sourceCall
            scope = CfirImplicitTypeComputationScope(checkNotNull(graph), CfirImplicitTypeComputationMode.FORMAL)
            context.implicitTypeComputationScope = scope
            rootCandidate = candidate(rootCall)
            graph.constraintSystemOwner = rootCandidate.system
            pcla = CfirPCLAInferenceSession(rootCandidate, session.inferenceComponents)
            context.inferenceSession = pcla
            tryExpression.replaceResources(listOf(resource))
            rootCall.replaceCalleeReference(CfirNamedReferenceWithCandidate(source, Name.identifier("make"), rootCandidate))
            graph.recordCallAttempt(checkNotNull(scope), rootCandidate, CfirRetainedCallOutcome.PARTIAL)
        }

        fun newResource(
            type: ConeCangJieType,
            initializer: CfirExpression,
            resourceName: String = "r",
            phase: CfirResolvePhase = CfirResolvePhase.IMPLICIT_TYPES,
            typeImplicit: Boolean = true,
        ): CfirResourceVariable = buildResourceVariable {
            this.source = this@Fixture.source
            moduleData = session.moduleData
            resolvePhase = phase
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            status = CfirDeclarationStatusImpl()
            this.initializer = initializer
            isTypeImplicit = typeImplicit
            isVar = false
            symbol = CfirResourceVariableSymbol(CallableId(Name.identifier(resourceName)))
            returnTypeRef = type.toCfirResolvedTypeRef(source)
            name = Name.identifier(resourceName)
        }

        fun resolvedInitializer(): CfirFunctionCall = buildFunctionCall {
            source = this@Fixture.source
            coneTypeOrNull = implementationType
            calleeReference = buildResolvedNamedReference { name = Name.identifier("make"); resolvedSymbol = functionSymbol }
        }
        private fun unresolvedInitializer(): CfirFunctionCall = buildFunctionCall {
            source = this@Fixture.source
            coneTypeOrNull = implementationType
            calleeReference = buildNamedReference { name = Name.identifier("make") }
        }
        private fun candidate(call: CfirFunctionCall): Candidate {
            val info = CallInfo(callSite = call, callKind = CallKind.Function, name = Name.identifier("make"),
                explicitReceiver = null, arguments = emptyList(), isUsedAsGetClassReceiver = false, typeArguments = emptyList(),
                session = session, containingFile = context.file, containingDeclarations = emptyList(), resolutionMode = ResolutionMode.ContextIndependent)
            return Candidate(symbol = functionSymbol, dispatchReceiver = null, givenExtensionReceiver = null,
                explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                constraintSystemFactory = session.inferenceComponents.constraintSystemFactory,
                baseSystem = session.inferenceComponents.createConstraintSystem().asReadOnlyStorage(), callInfo = info,
                originScope = null, bodyResolveContext = context).apply {
                initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList())
                initializeArgumentMapping(emptyList(), linkedMapOf())
            }
        }
    }
}
