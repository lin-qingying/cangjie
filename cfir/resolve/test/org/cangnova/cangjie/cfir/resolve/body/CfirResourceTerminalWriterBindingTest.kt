@file:OptIn(org.cangnova.cangjie.util.PrivateForInline::class)

package org.cangnova.cangjie.cfir.resolve.body

import java.util.IdentityHashMap
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.*
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
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.registerHierarchyProviders
import org.cangnova.cangjie.cfir.resolve.calls.ConeAtomWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.*
import org.cangnova.cangjie.cfir.resolve.inference.*
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeFixVariableConstraintPosition
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirResourceVariableSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * PRIVATE 最终发布与 FORMAL writer 的时间边界。
 *
 * 成功路径使用真实 FULL completion 和生产 writer；拒绝以退休、实际引用、类型和 cache 为 oracle。
 * 不读取 terminal/binding 私有表，不调用 bindFormalCompletionToLease，不伪造完成 outcome。
 * 此组补充协议单测，现有 Source/Replay/Synthetic 与六个完整 RAW forward 入口继续独立验收。
 */
class CfirResourceTerminalWriterBindingTest {
    @Test
    fun resolvedFullCarrierIsActuallyWrittenByTheNextTreeWriter() {
        val f = Fixture()
        f.publishPrivate(f.initialPrivate)
        val formal = f.prepareFormal()
        f.write(formal, f.writer(formal))
        f.assertWritten(formal)
        assertEquals(1, formal.callbacksWritten)
        f.publishPrivate(f.nextPrivate())

        // 不安装 candidate reference，不创建新候选；本次 root 中只有之前真实写回的 resolved call。
        val currentWriter = f.writerForResolvedCall(formal)
        f.assertCannotRetire()
        assertEquals(1, formal.callbacksWritten)
        f.write(formal, currentWriter)
        f.assertWritten(formal)
        assertEquals(2, formal.callbacksWritten)
        f.retire()
    }

    @Test
    fun consumedFormalCarrierCannotBeReplacedByARevokedWriter() {
        val f = Fixture()
        f.publishPrivate(f.initialPrivate)
        val formal = f.prepareFormal()
        f.write(formal, f.writer(formal))
        f.publishPrivate(f.nextPrivate())
        val heldWriter = f.writerForResolvedCall(formal)
        f.write(formal, heldWriter)
        assertEquals(2, formal.callbacksWritten)
        val currentWriter = f.writerForResolvedCall(formal)
        val reference = f.sourceCall.calleeReference
        val arguments = f.sourceCall.argumentList
        val replacement = buildFunctionCall {
            source = f.source
            calleeReference = buildResolvedNamedReference {
                name = f.target.cfir.name
                resolvedSymbol = f.target
            }
        }
        replacement.replaceConeTypeOrNull(f.implementationType)
        val data = ExpectedArgumentType.ExpectedType(f.implementationType, mapOf(f.sourceCall to replacement))

        // 撤权后的旧 writer 必须在 CFG/AST replacement 前退出，不能因为已成功而丢失权限检查。
        assertSame(f.sourceCall, f.sourceCall.transformSingle(heldWriter, data))
        assertSame(reference, f.sourceCall.calleeReference)
        assertSame(arguments, f.sourceCall.argumentList)
        assertEquals(2, formal.callbacksWritten)
        f.write(formal, currentWriter)
        assertEquals(3, formal.callbacksWritten)
        f.retire()
    }

    @Test
    fun successfulResolvedFormalHandoffIsConsumedOnlyOnceByTheSameWriter() {
        val f = Fixture()
        f.publishPrivate(f.initialPrivate)
        val formal = f.prepareFormal()
        f.write(formal, f.writer(formal))
        assertEquals(1, formal.callbacksWritten)
        f.publishPrivate(f.nextPrivate())
        val currentWriter = f.writerForResolvedCall(formal)
        f.assertCannotRetire()

        f.write(formal, currentWriter)
        f.assertWritten(formal)
        assertEquals(2, formal.callbacksWritten)
        val reference = f.sourceCall.calleeReference
        val arguments = f.sourceCall.argumentList
        // 同一 carrier 经 return-expression 与 body 再次访问，不能再执行完整回调。
        f.write(formal, currentWriter)
        assertSame(reference, f.sourceCall.calleeReference)
        assertSame(arguments, f.sourceCall.argumentList)
        assertEquals(2, formal.callbacksWritten)
        f.retire()
    }

    @Test
    fun metadataOnlyFactoryDoesNotLoseTheLastActualResolvedCarrier() {
        val f = Fixture()
        f.publishPrivate(f.initialPrivate)
        val formal = f.prepareFormal()
        f.write(formal, f.writer(formal))
        f.publishPrivate(f.nextPrivate())
        f.writerForResolvedCall(formal)
        f.assertCannotRetire()
        assertEquals(1, formal.callbacksWritten)

        f.write(formal, f.writerForResolvedCall(formal))
        f.assertWritten(formal)
        assertEquals(2, formal.callbacksWritten)
        f.retire()
    }

    @Test
    fun resolvedCarrierHandoffRejectsAnotherDeclarationWithTheSameName() {
        val f = Fixture()
        f.publishPrivate(f.initialPrivate)
        val formal = f.prepareFormal()
        f.write(formal, f.writer(formal))
        f.publishPrivate(f.nextPrivate())
        val different = CallResolutionTestFixtures.buildFunctionSymbol(f.session, "make", f.implementationType)
        assertEquals(f.target.callableId, different.callableId)
        f.sourceCall.replaceCalleeReference(buildResolvedNamedReference {
            source = f.source
            name = different.cfir.name
            resolvedSymbol = different
        })

        assertThrows(IllegalStateException::class.java) { f.writerForResolvedCall(formal) }
        assertSame(different, (f.sourceCall.calleeReference as CfirResolvedNamedReference).resolvedSymbol)
        assertEquals(1, formal.callbacksWritten)
        f.assertCannotRetire()
    }

    @Test
    fun writerCreatedBeforeLateTerminalCannotEndorseThatTerminal() {
        val f = Fixture()
        f.publishPrivate(f.initialPrivate)
        val formal = f.prepareFormal()
        val oldWriter = f.writer(formal)
        val late = f.nextPrivate()
        f.publishPrivate(late)
        f.assertSameVm(f.initialPrivate, late, formal)

        f.write(formal, oldWriter)
        f.assertWritten(formal)
        assertEquals(1, formal.callbacksWritten)
        f.assertCannotRetire()

        // 新 factory 的绑定本身仍不是完成；必须有实际节点写回。
        val current = f.prepareFormal()
        val currentWriter = f.writer(current)
        f.assertCannotRetire()
        f.write(current, currentWriter)
        f.assertWritten(current)
        assertEquals(1, current.callbacksWritten)
        f.retire()
    }

    @Test
    fun completedOldWriterCannotReplayLaterTerminalByBeingInvokedAgain() {
        val f = Fixture()
        f.publishPrivate(f.initialPrivate)
        val formal = f.prepareFormal()
        val oldWriter = f.writer(formal)
        f.write(formal, oldWriter)
        f.assertWritten(formal)
        val late = f.nextPrivate()
        f.publishPrivate(late)
        f.assertSameVm(f.initialPrivate, late, formal)

        f.write(formal, oldWriter)
        f.assertWritten(formal)
        f.assertCannotRetire()
        val current = f.prepareFormal()
        f.write(current, f.writer(current))
        f.assertWritten(current)
        f.retire()
    }

    @Test
    fun sameKeyAndFixedVmWithoutAnotherWriterCannotCompleteLateTerminal() {
        val f = Fixture()
        f.publishPrivate(f.initialPrivate)
        val first = f.prepareFormal()
        f.write(first, f.writer(first))
        val late = f.nextPrivate()
        f.publishPrivate(late)
        f.assertSameVm(first, late)
        assertSame(first.candidate.symbol, late.candidate.symbol)
        assertSame(f.sourceCall, first.candidate.callInfo.callSite)
        assertSame(f.privateCall, late.candidate.callInfo.callSite)
        assertEquals(f.implementationType, late.candidate.system.fixedTypeVariables[f.vm.typeConstructor])
        f.assertCannotRetire()

        val current = f.prepareFormal()
        f.assertSameVm(first, current)
        val writer = f.writer(current)
        assertEquals(0, current.callbacksWritten)
        f.assertCannotRetire()
        f.write(current, writer)
        assertEquals(1, current.callbacksWritten)
        f.retire()
    }

    @Test
    fun repeatPublicationBySamePrivateWriterStillNeedsANewFormalWriter() {
        val f = Fixture()
        val privateWriter = f.writer(f.initialPrivate)
        f.write(f.initialPrivate, privateWriter)
        val formal = f.prepareFormal()
        f.write(formal, f.writer(formal))

        // 不换 candidate、不 reinfer、不创建新的 VM，重复真正的 PRIVATE final 写回。
        f.write(f.initialPrivate, privateWriter)
        f.assertWritten(f.initialPrivate)
        f.assertSameVm(f.initialPrivate, formal)
        f.assertCannotRetire()
        val current = f.prepareFormal()
        f.write(current, f.writer(current))
        f.retire()
    }

    @Test
    fun completionGrantCapturedBeforeCarrierCannotInventItsWriterBinding() {
        val f = Fixture()
        f.publishPrivate(f.initialPrivate)
        val owner = f.initialPrivate.candidate.system
        val grant = f.graph.captureCompletionLease(f.formalScope, f.sourceCall, owner)
        val formal = f.prepareFormal()
        f.sourceCall.replaceCalleeReference(buildResolvedNamedReference {
            source = f.source
            name = f.target.cfir.name
            resolvedSymbol = f.target
        })
        // 有合法 source grant、实际目标、具体结果与同一个 VM，故意没有 writer 的 carrier handshake。
        assertTrue(f.graph.isCompletionLeaseActive(f.formalScope, formal.candidate, grant))
        assertThrows(IllegalStateException::class.java) {
            f.graph.recordCompletedCall(f.formalScope, formal.candidate, f.sourceCall, owner,
                owner.buildCurrentSubstitutor().asCone(), grant)
        }
        assertEquals(0, formal.callbacksWritten)
        f.assertCannotRetire()
        f.install(formal)
        f.write(formal, f.writer(formal))
        f.assertWritten(formal)
        f.retire()
    }

    @Test
    fun sameNamedDifferentResolvedDeclarationCannotConsumeTerminal() {
        val f = Fixture()
        f.publishPrivate(f.initialPrivate)
        val formal = f.prepareFormal()
        val writer = f.writer(formal)
        val different = CallResolutionTestFixtures.buildFunctionSymbol(f.session, "make", f.implementationType)
        assertEquals(f.target.callableId, different.callableId)
        assertNotSame(f.target, different)
        f.sourceCall.replaceCalleeReference(buildResolvedNamedReference {
            source = f.source
            name = different.cfir.name
            resolvedSymbol = different
        })
        assertThrows(IllegalStateException::class.java) { f.write(formal, writer) }
        f.assertCannotRetire()
        f.install(formal)
        f.write(formal, f.writer(formal))
        f.assertWritten(formal)
        f.retire()
    }

    @Test
    fun unrelatedSystemWithSameNamedFixedVmCannotCompleteRetainedCall() {
        val f = Fixture()
        f.publishPrivate(f.initialPrivate)
        val formal = f.prepareFormal()
        val foreign = ConstraintSystemTestHarness.newSystem(f.session)
        val replacement = ConstraintSystemTestHarness.newVariable(foreign, "ResourceResult")
        foreign.fixVariable(replacement, f.implementationType, ConeFixVariableConstraintPosition(replacement))
        assertNotSame(f.vm, replacement)
        assertNotSame(f.vm.typeConstructor, replacement.typeConstructor)
        assertNull(foreign.allTypeVariables[f.vm.typeConstructor])
        assertEquals(f.implementationType, foreign.fixedTypeVariables[replacement.typeConstructor])

        assertThrows(IllegalStateException::class.java) {
            f.write(formal, f.writer(formal, foreign))
        }
        f.assertCannotRetire()
        f.install(formal)
        f.write(formal, f.writer(formal))
        f.assertWritten(formal)
        f.retire()
    }

    @Test
    fun snapshotBeforeBindingDoesNotResurrectRevokedWriterForExistingCandidate() {
        val f = Fixture()
        f.publishPrivate(f.initialPrivate)
        val formal = f.prepareFormal()
        val ast = CfirResolutionSnapshot.capture(f.sourceCall)
        val before = f.graph.captureReplayState(f.formalScope, f.sourceCall)
        val oldWriter = f.writer(formal)
        val late = f.nextPrivate()
        f.publishPrivate(late)
        ast.restoreWithImplicitBodyCache(null, null)
        before.restoreForRollback()
        val reference = f.sourceCall.calleeReference
        val arguments = f.sourceCall.argumentList

        f.write(formal, oldWriter)
        assertSame(reference, f.sourceCall.calleeReference)
        assertSame(arguments, f.sourceCall.argumentList)
        assertEquals(0, formal.callbacksWritten)
        f.assertSameVm(formal, f.initialPrivate)
        f.assertCannotRetire()
        f.write(formal, f.writer(formal))
        assertEquals(1, formal.callbacksWritten)
        f.retire()
    }

    @Test
    fun successfulMementoRestoresItsBindingAndRevokesLaterWriter() {
        val f = Fixture()
        f.publishPrivate(f.initialPrivate)
        val first = f.prepareFormal()
        val firstWriter = f.writer(first)
        f.write(first, firstWriter)
        val ast = CfirResolutionSnapshot.capture(f.sourceCall)
        val selected = f.graph.captureReplayState(f.formalScope, f.sourceCall)
        val late = f.nextPrivate()
        f.publishPrivate(late)
        val later = f.prepareFormal()
        val laterWriter = f.writer(later)
        f.write(later, laterWriter)
        ast.restoreWithImplicitBodyCache(null, null)
        selected.restoreForRollback()
        val reference = f.sourceCall.calleeReference
        val arguments = f.sourceCall.argumentList

        f.write(later, laterWriter)
        assertSame(reference, f.sourceCall.calleeReference)
        assertSame(arguments, f.sourceCall.argumentList)
        assertEquals(1, later.callbacksWritten)
        // 只恢复成功快照本来持有的 W0/T0；W1 不能借旧 AST/同 VM 复活。
        f.write(first, firstWriter)
        f.assertWritten(first)
        f.retire()
    }

    @Test
    fun reinferenceKeepsVmButRevokesOldFormalWriterUntilFreshCompletion() {
        val f = Fixture()
        f.publishPrivate(f.initialPrivate)
        val formal = f.prepareFormal()
        val before = f.graph.captureReplayState(f.formalScope, f.sourceCall)
        val oldWriter = f.writer(formal)
        before.restoreForReinference(formal.candidate.system)
        val reference = f.sourceCall.calleeReference
        f.write(formal, oldWriter)
        assertSame(reference, f.sourceCall.calleeReference)
        assertEquals(0, formal.callbacksWritten)
        assertSame(f.vm, formal.candidate.system.allTypeVariables[f.vm.typeConstructor])

        val next = f.nextPrivate()
        f.publishPrivate(next)
        f.assertSameVm(formal, next)
        f.assertCannotRetire()
        val current = f.prepareFormal()
        f.write(current, f.writer(current))
        f.assertWritten(current)
        f.retire()
    }

    @Test
    fun retiredGraphRejectsNewWriterAndHeldWritersCannotWriteAgain() {
        val f = Fixture()
        val privateWriter = f.writer(f.initialPrivate)
        f.write(f.initialPrivate, privateWriter)
        val formal = f.prepareFormal()
        val formalWriter = f.writer(formal)
        f.write(formal, formalWriter)
        f.retire()
        val privateReference = f.privateCall.calleeReference
        val formalReference = f.sourceCall.calleeReference
        val privateArguments = f.privateCall.argumentList
        val formalArguments = f.sourceCall.argumentList

        f.write(f.initialPrivate, privateWriter)
        f.write(formal, formalWriter)
        assertSame(privateReference, f.privateCall.calleeReference)
        assertSame(formalReference, f.sourceCall.calleeReference)
        assertSame(privateArguments, f.privateCall.argumentList)
        assertSame(formalArguments, f.sourceCall.argumentList)
        assertThrows(IllegalStateException::class.java) { f.writer(formal) }
        assertThrows(IllegalStateException::class.java) { f.writer(f.initialPrivate) }
    }

    @Test
    fun sourceCannotSealWhileActualPrivateProducerRemainsOnStack() {
        val f = Fixture()
        f.publishPrivate(f.initialPrivate)
        val formal = f.prepareFormal()
        val source = f.resolveSource(formal)
        val writer = f.writer(formal)
        assertThrows(IllegalStateException::class.java) {
            f.graph.withPrivateOrigins { f.write(formal, writer) }
        }
        assertFalse(source.isCompleted)
        assertTrue(f.cache().getStatus(f.resource.symbol) is CfirImplicitBodyResolveComputationStatus.ComputedType)
        assertEquals(CfirResolvePhase.IMPLICIT_TYPES, f.resource.resolvePhase)
        f.write(formal, f.writer(formal))
        f.assertSealed(source)
    }

    /** 候选与真实 call 一起保存；计数来自实际 ordinary completion callback。 */
    private class ActualCall(val candidate: Candidate, val call: CfirFunctionCall) {
        var callbacksWritten = 0
    }

    /**
     * 沿现有 SourceCompletion/Replay 单测的 session、Resource 层次、CS、calculator 与 writer seam 建图。
     * 唯一 retained VM 在实际系统固定；每次后到 PRIVATE 发布共享它而不制造正式 receipt。
     */
    private class Fixture {
        val session = CallResolutionTestFixtures.newTestSession()
        val source = checkNotNull(CallResolutionTestFixtures.buildTypedExpression(ConePrimitiveType.INT32).source)
        val resourceType = ConeClassLikeType(ConeClassLikeLookupTagImpl(StdlibClassIds.Resource))
        private val implementationId = ClassId(FqName.ROOT, Name.identifier("R"))
        val implementationType = ConeClassLikeType(ConeClassLikeLookupTagImpl(implementationId))
        val target = CallResolutionTestFixtures.buildFunctionSymbol(session, "make", implementationType)
        val sourceCall = call()
        val privateCall = call()
        val resource = newResource(sourceCall, "resource")
        private val privateResource = newResource(privateCall, "resource")
        private val registry = IdentityHashMap<CfirResourceVariable, CfirImplicitTypeComputation>()
        val graph = CfirImplicitTypeComputation(resource, privateCall,
            mapOf<CfirElement, CfirElement>(resource to privateResource, sourceCall to privateCall),
            emptyMap(), null, registry, emptySet()).also { registry[resource] = it }
        val privateScope = CfirImplicitTypeComputationScope(graph, CfirImplicitTypeComputationMode.PRIVATE)
        val formalScope = CfirImplicitTypeComputationScope(graph, CfirImplicitTypeComputationMode.FORMAL)
        val context = BodyResolveContext(ReturnTypeCalculator.Default, CfirDataFlowAnalyzerContext())
        val transformer: CfirBodyResolveTransformer
        val initialPrivate: ActualCall
        val vm: ConeTypeVariable
        private var storageOwner: ConstraintSystemImpl
        var pcla: CfirPCLAInferenceSession? = null
            private set
        val tryExpression = buildTryExpression {
            resources += resource
            tryBlock = buildBlock { coneTypeOrNull = ConePrimitiveType.UNIT }
            coneTypeOrNull = ConePrimitiveType.UNIT
        }

        init {
            session.registerHierarchyProviders(listOf<CfirClassLikeDeclaration>(
                ExtendTestFixtures.newInterface(session.moduleData, "Resource", StdlibClassIds.Resource),
                ExtendTestFixtures.newClass(session.moduleData, "R", implementationId,
                    superTypeRefs = listOf(resourceType.toCfirResolvedTypeRef())),
            ))
            context.file = CallResolutionTestFixtures.buildCallInfo(session, "make").containingFile
            transformer = CfirBodyResolveTransformer(session, ScopeSession(), outerBodyResolveContext = context)
            context.implicitTypeComputationScope = privateScope
            val actual = candidate(privateCall, target, context, ConstraintSystemTestHarness.newSystem(session).captureStorage())
            graph.recordCallAttempt(privateScope, actual, CfirRetainedCallOutcome.PARTIAL)
            vm = graph.variable(privateScope, actual, privateCall, CfirInferenceVariableRole.INVOKE_RETURN, 0) {
                ConeTypeVariable("ResourceResult")
            }
            actual.system.registerVariable(vm)
            actual.system.fixVariable(vm, implementationType, ConeFixVariableConstraintPosition(vm))
            initialPrivate = wrap(actual, privateCall)
            storageOwner = actual.system
            graph.constraintSystemOwner = storageOwner
            graph.storeResult(resource.returnTypeRef as CfirResolvedTypeRef)
            install(initialPrivate)
            completeSystem(actual)
        }

        fun candidate(call: CfirFunctionCall, symbol: CfirNamedFunctionSymbol,
            ownerContext: BodyResolveContext?, storage: ConstraintStorage): Candidate {
            val owner = ownerContext ?: BodyResolveContext(ReturnTypeCalculator.Default, CfirDataFlowAnalyzerContext()).apply {
                file = context.file
            }
            val info = CallInfo(callSite = call, callKind = CallKind.Function, name = symbol.cfir.name,
                explicitReceiver = null, arguments = emptyList(), isUsedAsGetClassReceiver = false,
                typeArguments = emptyList(), session = session, containingFile = context.file,
                containingDeclarations = emptyList(), resolutionMode = ResolutionMode.ContextIndependent)
            return Candidate(symbol = symbol, dispatchReceiver = null, givenExtensionReceiver = null,
                explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                constraintSystemFactory = session.inferenceComponents.constraintSystemFactory,
                baseSystem = storage, callInfo = info, originScope = null, bodyResolveContext = owner).apply {
                initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList())
                initializeArgumentMapping(emptyList(), linkedMapOf())
            }
        }

        private fun wrap(candidate: Candidate, call: CfirFunctionCall): ActualCall =
            ActualCall(candidate, call).also { actual ->
                candidate.onPCLACompletionResultsWritingCallbacks += { actual.callbacksWritten++ }
            }

        private fun prepare(scope: CfirImplicitTypeComputationScope, call: CfirFunctionCall): ActualCall {
            context.implicitTypeComputationScope = scope
            val candidate = candidate(call, target, context, storageOwner.captureStorage())
            graph.recordCallAttempt(scope, candidate, CfirRetainedCallOutcome.PARTIAL)
            val retained = graph.variable<ConeTypeVariable>(scope, candidate, call, CfirInferenceVariableRole.INVOKE_RETURN, 0) {
                error("The same retained call allocated another VM")
            }
            assertSame(vm, retained)
            val actual = wrap(candidate, call)
            install(actual)
            completeSystem(candidate)
            return actual
        }

        fun nextPrivate(): ActualCall = prepare(privateScope, privateCall)
        fun prepareFormal(): ActualCall = prepare(formalScope, sourceCall)
        fun publishPrivate(actual: ActualCall) {
            write(actual, writer(actual))
            assertWritten(actual)
            storageOwner = actual.candidate.system
            graph.constraintSystemOwner = storageOwner
        }

        fun install(actual: ActualCall) {
            actual.call.replaceConeTypeOrNull(implementationType)
            actual.call.replaceCalleeReference(CfirNamedReferenceWithCandidate(source, actual.candidate.callInfo.name, actual.candidate))
        }

        fun completeSystem(candidate: Candidate) {
            ConstraintSystemCompleter(transformer.components).complete(
                candidate.system.asConstraintSystemCompleterContext(), ConstraintSystemCompletionMode.FULL,
                listOf(ConeAtomWithCandidate(candidate.callInfo.callSite as CfirExpression, candidate)),
                candidate.substitutedReturnType(), transformer.resolutionContext,
            ) { _, _ -> error("This fixture has no postponed argument") }
            assertTrue(candidate.system.notFixedTypeVariables.isEmpty())
        }

        fun writer(actual: ActualCall, system: ConstraintSystemImpl = actual.candidate.system): CfirCallCompletionResultsWriterTransformer {
            context.implicitTypeComputationScope = actual.candidate.implicitTypeComputationScope
            return transformer.components.callCompleter.createCompletionResultsWriter(system.buildCurrentSubstitutor().asCone(),
                completionSystem = system, completionRoot = actual.call, completionCandidate = actual.candidate)
        }

        /** 模拟外层树 writer；root 没有 candidate reference，也不传入该已完成调用的 candidate。 */
        fun writerForResolvedCall(actual: ActualCall): CfirCallCompletionResultsWriterTransformer {
            context.implicitTypeComputationScope = formalScope
            val system = actual.candidate.system
            return transformer.components.callCompleter.createCompletionResultsWriter(system.buildCurrentSubstitutor().asCone(),
                completionSystem = system, completionRoot = sourceCall)
        }

        fun write(actual: ActualCall, writer: CfirCallCompletionResultsWriterTransformer) {
            context.implicitTypeComputationScope = actual.candidate.implicitTypeComputationScope
            actual.call.transformSingle(writer, null)
        }

        fun assertWritten(actual: ActualCall) {
            assertSame(actual.candidate.symbol, (actual.call.calleeReference as CfirResolvedNamedReference).resolvedSymbol)
            assertEquals(implementationType, actual.call.coneTypeOrNull)
        }

        fun assertSameVm(vararg calls: ActualCall) {
            calls.forEach { actual ->
                assertSame(vm, actual.candidate.system.allTypeVariables[vm.typeConstructor])
                assertEquals(implementationType, actual.candidate.system.fixedTypeVariables[vm.typeConstructor])
            }
        }

        fun assertCannotRetire() {
            assertThrows(IllegalStateException::class.java) { graph.retirePrivateResults(null, sourceRejected = false) }
        }
        fun retire() {
            assertDoesNotThrow { graph.retirePrivateResults(null, sourceRejected = false) }
        }

        fun resolveSource(actual: ActualCall): CfirResourceSourceCompletion {
            context.implicitTypeComputationScope = formalScope
            pcla = CfirPCLAInferenceSession(actual.candidate, session.inferenceComponents)
            context.inferenceSession = checkNotNull(pcla)
            ResourceVariableReturnTypeCalculator(transformer.components, listOf(resource), tryExpression, resourceType)
                .resolveResource(resource) { resource }
            pcla!!.applyResultsToMainCandidate()
            return checkNotNull(resource.resourceSourceCompletion)
        }

        fun cache(): ImplicitBodyResolveComputationSession = checkNotNull(resource.resourceSourceCache)
        fun assertSealed(source: CfirResourceSourceCompletion) {
            assertTrue(source.isCompleted)
            val status = cache().getStatus(resource.symbol) as CfirImplicitBodyResolveComputationStatus.Computed
            assertSame(resource, status.transformedDeclaration)
            assertSame(resource.returnTypeRef, status.resolvedTypeRef)
            assertEquals(implementationType, resource.returnTypeRef.coneTypeOrNull)
            assertEquals(CfirResolvePhase.BODY_RESOLVE, resource.resolvePhase)
            assertThrows(IllegalStateException::class.java) {
                graph.captureCompletionLease(formalScope, sourceCall, initialPrivate.candidate.system)
            }
        }

        fun call(): CfirFunctionCall = buildFunctionCall {
            source = this@Fixture.source
            coneTypeOrNull = implementationType
            calleeReference = buildNamedReference { source = this@Fixture.source; name = Name.identifier("make") }
        }

        fun newResource(initializer: CfirFunctionCall, resourceName: String): CfirResourceVariable = buildResourceVariable {
            source = this@Fixture.source
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.IMPLICIT_TYPES
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            status = CfirDeclarationStatusImpl()
            isVar = false
            isTypeImplicit = true
            symbol = CfirResourceVariableSymbol(CallableId(Name.identifier(resourceName)))
            name = Name.identifier(resourceName)
            returnTypeRef = implementationType.toCfirResolvedTypeRef(source)
            this.initializer = initializer
        }
    }
}
