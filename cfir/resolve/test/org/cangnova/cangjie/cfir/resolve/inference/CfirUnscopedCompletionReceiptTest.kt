@file:OptIn(org.cangnova.cangjie.util.PrivateForInline::class)

package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.builder.*
import org.cangnova.cangjie.cfir.resolve.CfirResolutionSnapshot
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.body.CfirBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzerContext
import org.cangnova.cangjie.cfir.resolve.body.ImplicitBodyResolveComputationSession
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.resolve.calls.ConeAtomWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.stages.TypeArgumentMapping
import org.cangnova.cangjie.cfir.resolve.calls.candidate.*
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeFixVariableConstraintPosition
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityChecker
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.substitution.asCone
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeParameterBasedTypeVariable
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.type.model.TypeVariableInferenceScope
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * unscoped queued call 的真实 FULL 写回与 owner 隔离。
 * VM 来自真实声明类型参数并参与返回类型；oracle 是实际 callback、target、AST 身份与队列消耗。
 * 不调用 receipt/pass 的内部 factory，不读其表，不把相同 symbol/type/VM 视为完成。
 */
class CfirUnscopedCompletionReceiptTest {
    @Test
    fun mutableSourceCompletionUsesTheActualBodyFrame() {
        val f = Fixture(fixed = false)
        val child = f.child()
        // Source 候选已经完成正常 CS 初始化；准入查询本身不得承担这一步。
        assertSame(f.variable, child.candidate.system.captureStorage().allTypeVariables[f.variable.typeConstructor])
        assertTrue(f.transformer.components.callCompleter.tryCompletePendingSourceCall(
            child.call, ResolutionMode.WithExpectedType(ConePrimitiveType.INT32.toCfirResolvedTypeRef()),
        ))
        assertEquals(ConePrimitiveType.INT32, child.call.coneTypeOrNull)
        assertSame(child.candidate.symbol, (child.call.calleeReference as CfirResolvedNamedReference).resolvedSymbol)
    }

    @Test
    fun fixedFrozenSourceCandidateWithoutReceiptCannotReenterCompletion() {
        val f = Fixture()
        val child = f.child()
        val system = child.candidate.system
        system.asReadOnlyStorage()
        val reference = child.call.calleeReference
        val before = system.captureStorage()
        assertFalse(f.transformer.components.callCompleter.tryCompletePendingSourceCall(
            child.call, ResolutionMode.ContextIndependent,
        ))
        assertSame(reference, child.call.calleeReference)
        val after = system.captureStorage()
        assertSame(before.allTypeVariables[f.variable.typeConstructor], after.allTypeVariables[f.variable.typeConstructor])
        assertEquals(before.fixedTypeVariables, after.fixedTypeVariables)
    }

    @Test
    fun anotherBodyFrameCannotCompleteTheSourceCandidate() {
        val f = Fixture(fixed = false)
        val other = Fixture(fixed = false)
        val child = f.child()
        val reference = child.call.calleeReference
        assertFalse(other.transformer.components.callCompleter.tryCompletePendingSourceCall(
            child.call, ResolutionMode.ContextIndependent,
        ))
        assertSame(reference, child.call.calleeReference)
        assertTrue(child.candidate.system.captureStorage().notFixedTypeVariables.containsKey(f.variable.typeConstructor))
    }

    @Test
    fun actualFullSourceReceiptIsReadWithoutNewCompletion() {
        val f = Fixture()
        val child = f.child()
        f.owner.postponedPCLACalls += child.atom
        f.full()
        assertEquals(1, child.writes)
        f.context.inferenceSession = CfirPCLAInferenceSession(f.owner, f.session.inferenceComponents)
        val reference = child.call.calleeReference
        val before = f.system.captureStorage()
        // FULL 写回先发布 receipt；只读查询不得替正式消费入口删除原 atom。
        assertTrue(f.owner.postponedPCLACalls.any { it === child.atom })
        assertTrue(f.transformer.components.callCompleter.tryCompletePendingSourceCall(
            child.call, ResolutionMode.ContextIndependent,
        ))
        assertEquals(1, child.writes)
        assertSame(reference, child.call.calleeReference)
        assertEquals(before.fixedTypeVariables, f.system.captureStorage().fixedTypeVariables)
        assertTrue(f.owner.postponedPCLACalls.any { it === child.atom })

        // 再由既有 FULL writer 消费 exact atom，之后查询仍只读取原完成事实。
        f.full()
        assertEquals(1, child.writes)
        assertTrue(f.owner.postponedPCLACalls.isEmpty())
        assertTrue(f.transformer.components.callCompleter.tryCompletePendingSourceCall(
            child.call, ResolutionMode.ContextIndependent,
        ))
        assertEquals(1, child.writes)
        assertSame(reference, child.call.calleeReference)
        assertEquals(before.fixedTypeVariables, f.system.captureStorage().fixedTypeVariables)
        assertTrue(f.owner.postponedPCLACalls.isEmpty())

        // 同一 candidate/carrier 的新 actual atom 不能继承上一 atom 的已消费事实。
        val later = ConeAtomWithCandidate(child.call, child.candidate)
        f.owner.postponedPCLACalls += later
        assertFalse(f.transformer.components.callCompleter.tryCompletePendingSourceCall(
            child.call, ResolutionMode.ContextIndependent,
        ))
        assertTrue(f.owner.postponedPCLACalls.single() === later)
        assertSame(reference, child.call.calleeReference)
        assertEquals(1, child.writes)
        assertEquals(before.fixedTypeVariables, f.system.captureStorage().fixedTypeVariables)
    }

    @Test
    fun deletingProcessedMiddleBoundaryOrTailPreservesTheRemainingStatementConstraint() {
        for (deletedPosition in listOf("middle", "boundary", "tail")) {
            val f = Fixture()
            val probe = StatementParameter(f)
            f.owner.postponedPCLACalls += f.child().atom
            val excludedBoolean = probe.enqueue(ConePrimitiveType.BOOLEAN)
            val inference = probe.start()
            val boundary = probe.enqueue(ConePrimitiveType.INT32)
            // 中段删除只留一个未扫描槽，错误的 min(size) 不能从另一个尾槽偶然得到相同解。
            val tail = if (deletedPosition == "middle") boundary else probe.enqueue(ConePrimitiveType.INT32)
            val removed = when (deletedPosition) {
                "middle" -> excludedBoolean
                "boundary" -> boundary
                "tail" -> tail
                else -> error("Unknown queue position")
            }
            assertTrue(f.owner.removePostponedCall(removed))
            assertTrue(probe.parameter.returnTypeRef.coneTypeOrNull is ConeTypeVariableType)
            inference.onStatementResolved(tail.expression)
            // 正确扫描只读未处理的 Int32 尾部；错纳旧 Bool 前缀或漏掉尾部均不能满足此类型。
            assertEquals(ConePrimitiveType.INT32, probe.parameter.returnTypeRef.coneTypeOrNull, deletedPosition)
        }
    }

    @Test
    fun activeStatementSessionUsesTheWatermarkRestoredWithItsSuccessfulState() {
        val f = Fixture()
        val probe = StatementParameter(f)
        f.owner.postponedPCLACalls += f.child().atom
        val excludedBoolean = probe.enqueue(ConePrimitiveType.BOOLEAN)
        val inference = probe.start()
        val tail = probe.enqueue(ConePrimitiveType.INT32)
        val ast = CfirResolutionSnapshot.capture(probe.lambda)
        val cache = ast.captureImplicitBodyCache(f.implicitCache)
        val commonStorage = inference.currentCommonSystem.fork().captureStorage()
        val state = f.owner.capturePCLACompletionState()
        inference.onStatementResolved(tail.expression)
        assertEquals(ConePrimitiveType.INT32, probe.parameter.returnTypeRef.coneTypeOrNull)
        f.owner.removePostponedCall(excludedBoolean)
        val later = probe.enqueue(ConePrimitiveType.BOOLEAN)
        // 让活跃 session 的水位确实不同于成功快照；漏恢复数字状态不能碰巧从原位置扫描。
        inference.onStatementResolved(later.expression)

        ast.restoreWithImplicitBodyCache(cache, f.implicitCache)
        inference.currentCommonSystem.replaceContentWith(commonStorage)
        f.owner.restorePCLACompletionState(state)
        assertTrue(probe.parameter.returnTypeRef.coneTypeOrNull is ConeTypeVariableType)
        // 不创建另一个 session；恢复后的同一 hook 必须从原水位读取 Int32 尾部。
        inference.onStatementResolved(tail.expression)
        assertEquals(ConePrimitiveType.INT32, probe.parameter.returnTypeRef.coneTypeOrNull)
    }

    @Test
    fun completedAtomConsumptionPreservesNextStatementParameterInference() {
        val f = Fixture()
        val completed = f.child()
        f.owner.postponedPCLACalls += completed.atom
        f.full()
        assertEquals(1, completed.writes)

        val nextVariable = ConeTypeVariable("NextStatementParameter")
        val lambdaSymbol = CfirAnonymousFunctionSymbol()
        val parameter = CallResolutionTestFixtures.buildValueParameter(f.session, "next", nextVariable.defaultType,
            containingDeclarationSymbol = lambdaSymbol)
        val lambda = buildAnonymousFunction {
            source = f.source
            moduleData = f.session.moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            status = CfirDeclarationStatusImpl()
            symbol = lambdaSymbol
            valueParameters += parameter
            returnTypeRef = ConePrimitiveType.UNIT.toCfirResolvedTypeRef()
            typeRef = ConeFunctionType(listOf(nextVariable.defaultType), ConePrimitiveType.UNIT).toCfirResolvedTypeRef()
            body = buildBlock { coneTypeOrNull = ConePrimitiveType.UNIT }
            hasExplicitParameterList = true
            isLambda = true
        }
        val pcla = CfirPCLAInferenceSession(f.owner, f.session.inferenceComponents, lambda)
        f.context.inferenceSession = pcla
        // 本会话把旧队列视为已处理；实际 FULL 消费旧 atom 必须同步这个水位。
        f.full()
        assertTrue(f.owner.postponedPCLACalls.isEmpty())
        assertEquals(1, completed.writes)
        pcla.currentCommonSystem.registerVariable(nextVariable)
        val argument = buildNamedAccessExpression {
            source = f.source
            coneTypeOrNull = nextVariable.defaultType
            calleeReference = buildResolvedNamedReference {
                name = parameter.name
                resolvedSymbol = parameter.symbol
            }
        }
        val target = CallResolutionTestFixtures.buildFunctionSymbol(f.session, "consumeNext", ConePrimitiveType.UNIT,
            parameterTypes = listOf(ConePrimitiveType.INT32))
        val call = buildFunctionCall {
            source = f.source
            coneTypeOrNull = ConePrimitiveType.UNIT
            calleeReference = buildNamedReference { name = target.cfir.name }
            argumentList = buildArgumentList { arguments += argument }
        }
        val info = CallInfo(callSite = call, callKind = CallKind.Function, name = target.cfir.name,
            explicitReceiver = null, arguments = listOf(argument), isUsedAsGetClassReceiver = false,
            typeArguments = emptyList(), session = f.session, containingFile = f.context.file,
            containingDeclarations = emptyList(), resolutionMode = ResolutionMode.ContextIndependent)
        val candidate = Candidate(symbol = target, dispatchReceiver = null, givenExtensionReceiver = null,
            explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            constraintSystemFactory = f.session.inferenceComponents.constraintSystemFactory,
            baseSystem = pcla.currentCommonSystem.captureStorage(), callInfo = info, originScope = null,
            bodyResolveContext = f.context).apply {
            initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList())
            val argumentAtom = ConeResolutionAtom.createRawAtom(argument)
            initializeArgumentMapping(listOf(argumentAtom), linkedMapOf(argumentAtom to target.cfir.valueParameters.single()))
        }
        call.replaceCalleeReference(CfirNamedReferenceWithCandidate(f.source, target.cfir.name, candidate))
        f.owner.postponedPCLACalls += ConeAtomWithCandidate(call, candidate)
        pcla.onStatementResolved(call)
        // 观察正式形参类型；不读水位私表，不给变量预先加可解约束。
        assertEquals(ConePrimitiveType.INT32, parameter.returnTypeRef.coneTypeOrNull)
    }

    @Test
    fun twoLiveUnscopedBindingsRemainFactoryConflictInEitherOrder() {
        for (newFirst in listOf(false, true)) {
            val f = Fixture()
            val first = f.child()
            val second = f.child(first.call)
            f.install(second)
            f.owner.postponedPCLACalls += if (newFirst) listOf(second.atom, first.atom) else listOf(first.atom, second.atom)
            f.assertSameVm(first, second)
            val error = assertThrows(IllegalStateException::class.java) { f.writer() }
            assertEquals("Completion carrier has conflicting actual candidates", error.message)
            assertEquals(0, first.writes)
            assertEquals(0, second.writes)
        }
    }

    @Test
    fun twoLiveUnscopedBindingsAfterFactoryStillFailOriginalDispatchGuard() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        val writer = f.writer()
        val second = f.child(first.call)
        f.install(second)
        f.owner.postponedPCLACalls.clear()
        f.owner.postponedPCLACalls += second.atom
        val reference = first.call.calleeReference
        val error = assertThrows(IllegalStateException::class.java) { f.write(writer) }
        assertEquals("Queued completion carrier changed its actual candidate", error.message)
        assertSame(reference, first.call.calleeReference)
        assertEquals(0, first.writes)
        assertEquals(0, second.writes)
    }

    @Test
    fun actualFullResultConsumesItsExactAtomWithoutAnotherBodyWrite() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        f.full()
        f.assertWritten(first)
        assertEquals(1, first.writes)
        val reference = first.call.calleeReference
        val arguments = first.call.argumentList
        f.full()
        assertFalse(f.owner.postponedPCLACalls.any { it === first.atom })
        assertSame(reference, first.call.calleeReference)
        assertSame(arguments, first.call.argumentList)
        assertEquals(1, first.writes)
    }

    @Test
    fun completedOldAtomAllowsActualNewCandidateInEitherQueueOrder() {
        for (newFirst in listOf(false, true)) {
            val f = Fixture()
            val first = f.child()
            f.owner.postponedPCLACalls += first.atom
            f.full()
            val second = f.child(first.call)
            f.install(second)
            f.owner.postponedPCLACalls.clear()
            f.owner.postponedPCLACalls += if (newFirst) listOf(second.atom, first.atom) else listOf(first.atom, second.atom)
            f.assertSameVm(first, second)
            f.full()
            assertFalse(f.owner.postponedPCLACalls.any { it === first.atom })
            assertTrue(f.owner.postponedPCLACalls.any { it === second.atom })
            f.assertWritten(second)
            assertEquals(1, first.writes)
            assertEquals(1, second.writes)
        }
    }

    @Test
    fun sameWriterConsumesRealOldCompletionWhenCallbackPublishesNewAst() {
        val f = Fixture()
        val first = f.child()
        val publisher = f.publisher()
        f.owner.postponedPCLACalls += listOf(first.atom, publisher.atom)
        val writer = f.writer()
        val second = f.child(first.call)
        f.owner.postponedPCLACalls += second.atom
        publisher.candidate.onPCLACompletionResultsWritingCallbacks += {
            assertEquals(1, first.writes)
            assertEquals(0, second.writes)
            f.install(second)
        }
        f.write(writer)
        assertFalse(f.owner.postponedPCLACalls.any { it === first.atom })
        f.assertWritten(second)
        assertEquals(1, first.writes)
        assertEquals(1, publisher.writes)
        assertEquals(1, second.writes)
    }

    @Test
    fun currentResolvedReceiptCannotHideADifferentLiveAtomWithoutAstSelection() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        f.full()
        val second = f.child(first.call)
        // 没有发布 second 的真实 AST 引用；相同 target/type/VM 不等于选择。
        f.owner.postponedPCLACalls += second.atom
        val error = assertThrows(IllegalStateException::class.java) { f.writer() }
        assertEquals("Unscoped queued replacement has no actual candidate reference", error.message)
        assertEquals(0, second.writes)
        assertEquals(1, first.writes)
    }

    @Test
    fun provisionalResolvedResultWithoutSystemCannotAuthorizeReplacement() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        f.write(f.ordinaryWriter(system = null))
        f.assertWritten(first)
        val second = f.child(first.call)
        f.install(second)
        f.owner.postponedPCLACalls += second.atom
        assertThrows(IllegalStateException::class.java) { f.writer() }
        assertEquals(0, second.writes)
    }

    @Test
    fun finalSystemWithoutExplicitFullEntryDoesNotCreateReceipt() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        f.write(f.ordinaryWriter())
        f.assertWritten(first)
        assertTrue(f.system.notFixedTypeVariables.isEmpty())
        val second = f.child(first.call)
        f.install(second)
        f.owner.postponedPCLACalls += second.atom
        assertThrows(IllegalStateException::class.java) { f.writer() }
        assertEquals(0, second.writes)
    }

    @Test
    fun factoryAndFixedBindingsAloneDoNotCompleteQueuedAtom() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        val writer = f.writer()
        assertEquals(ConePrimitiveType.INT32, f.system.fixedTypeVariables[f.variable.typeConstructor])
        val second = f.child(first.call)
        f.install(second)
        f.owner.postponedPCLACalls.clear()
        f.owner.postponedPCLACalls += second.atom
        assertThrows(IllegalStateException::class.java) { f.write(writer) }
        assertEquals(0, first.writes)
        assertEquals(0, second.writes)
    }

    @Test
    fun concreteFalseSubstitutorCannotStandInForActualFixedBinding() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        val falseSubstitutor = f.substitutor(ConePrimitiveType.BOOLEAN)
        f.write(f.ordinaryWriter(substitutor = falseSubstitutor))
        assertTrue(first.call.coneTypeOrNull is ConePrimitiveType)
        assertEquals(ConePrimitiveType.INT32, f.system.fixedTypeVariables[f.variable.typeConstructor])
        val second = f.child(first.call)
        f.install(second)
        f.owner.postponedPCLACalls += second.atom
        assertThrows(IllegalStateException::class.java) { f.writer() }
        assertEquals(0, second.writes)
    }

    @Test
    fun concreteTypeWithUnfixedActualSystemCannotCreateFullReceipt() {
        val f = Fixture(fixed = false)
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        f.write(f.ordinaryWriter(substitutor = f.substitutor(ConePrimitiveType.INT32)))
        assertTrue(f.variable.typeConstructor in f.system.notFixedTypeVariables)
        assertEquals(ConePrimitiveType.INT32, first.call.coneTypeOrNull)
        val second = f.child(first.call)
        f.install(second)
        f.owner.postponedPCLACalls += second.atom
        assertThrows(IllegalStateException::class.java) { f.writer() }
    }

    @Test
    fun failedFullCallbackCannotLeaveSuccessfulReceipt() {
        val f = Fixture()
        val first = f.child()
        first.candidate.onPCLACompletionResultsWritingCallbacks += { error("actual body completion failed") }
        f.owner.postponedPCLACalls += first.atom
        val failure = assertThrows(IllegalStateException::class.java) { f.full() }
        assertEquals("actual body completion failed", failure.message)
        assertTrue(first.call.calleeReference is CfirResolvedNamedReference)
        val second = f.child(first.call)
        f.install(second)
        f.owner.postponedPCLACalls += second.atom
        assertThrows(IllegalStateException::class.java) { f.writer() }
        assertEquals(0, second.writes)
    }

    @Test
    fun sameNamedWrongActualTargetCannotBeRepairedIntoReceipt() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        f.full()
        val different = CallResolutionTestFixtures.buildFunctionSymbol(f.session, "work",
            ConeTypeParameterTypeImpl(f.parameter.symbol.toLookupTag()), typeParameters = listOf(f.parameter))
        assertEquals(f.work.callableId, different.callableId)
        assertNotSame(f.work, different)
        val reference = buildResolvedNamedReference { name = different.cfir.name; resolvedSymbol = different }
        first.call.replaceCalleeReference(reference)
        val failure = assertThrows(IllegalStateException::class.java) { f.full() }
        assertEquals("PCLA completion atom differs from the actual resolved target", failure.message)
        assertSame(reference, first.call.calleeReference)
        assertEquals(1, first.writes)
    }

    @Test
    fun changedActualBindingInvalidatesOldReceiptDespiteSameVmAndTarget() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        f.full()
        // 用公开 actual fixed-binding map 注入过期解；不伪造 substitutor 或 receipt。
        f.system.fixedTypeVariables[f.variable.typeConstructor] = ConePrimitiveType.BOOLEAN
        assertSame(f.variable, f.system.allTypeVariables[f.variable.typeConstructor])
        val second = f.child(first.call)
        f.install(second)
        f.owner.postponedPCLACalls += second.atom
        assertThrows(IllegalStateException::class.java) { f.writer() }
        assertEquals(0, second.writes)
    }

    @Test
    fun foreignSystemWithSameNamedVmCannotConsumeOwnedReceipt() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        f.full()
        val second = f.child(first.call)
        f.install(second)
        f.owner.postponedPCLACalls += second.atom
        val foreign = ConstraintSystemTestHarness.newSystem(f.session)
        val variable = ConeTypeParameterBasedTypeVariable(f.parameter.symbol, TypeVariableInferenceScope())
        foreign.registerVariable(variable)
        foreign.fixVariable(variable, ConePrimitiveType.INT32, ConeFixVariableConstraintPosition(variable))
        assertNotSame(f.variable, variable)
        assertNotSame(f.variable.typeConstructor, variable.typeConstructor)
        assertEquals(f.variable.typeConstructor.originalTypeParameter, variable.typeConstructor.originalTypeParameter)
        assertThrows(IllegalStateException::class.java) { f.ordinaryWriter(system = foreign) }
        assertEquals(0, second.writes)
    }

    @Test
    fun restoredPreWriterStateRevokesHeldWriterAndItsCompletion() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        val before = f.owner.capturePCLACompletionState()
        val ast = CfirResolutionSnapshot.capture(first.call)
        val writer = f.writer()
        f.write(writer)
        assertEquals(1, first.writes)
        ast.restoreWithImplicitBodyCache(null, null)
        f.owner.restorePCLACompletionState(before)
        val reference = first.call.calleeReference
        val arguments = first.call.argumentList
        f.write(writer)
        assertSame(reference, first.call.calleeReference)
        assertSame(arguments, first.call.argumentList)
        assertEquals(1, first.writes)

        val second = f.child(first.call)
        f.install(second)
        f.owner.postponedPCLACalls += second.atom
        assertThrows(IllegalStateException::class.java) { f.writer() }
        assertEquals(0, second.writes)
    }

    @Test
    fun successfulSnapshotRestoresOnlyItsReceiptAndRevokesLaterPass() {
        val f = Fixture()
        val first = f.child()
        val emptyCache = f.implicitCache.capture(setOf(f.work))
        f.implicitCache.compute(f.work) { f.work.cfir }
        f.owner.postponedPCLACalls += first.atom
        val completedWriter = f.writer()
        f.write(completedWriter)
        val selectedCacheStatus = f.implicitCache.getStatus(f.work)
        val restoreSelected = f.captureSuccessfulState(first)
        val selectedReference = first.call.calleeReference
        val selectedArguments = first.call.argumentList
        val second = f.child(first.call)
        f.install(second)
        f.owner.postponedPCLACalls += second.atom
        val laterWriter = f.writer()
        // FULL 已正常返回后才开启事务；恢复真实 CS 入口，再配对恢复成功 AST/cache/PCLA。
        val parentTransaction = f.system.prepareTransaction()
        val childTransaction = first.candidate.system.prepareTransaction()
        f.system.fixedTypeVariables[f.variable.typeConstructor] = ConePrimitiveType.BOOLEAN
        first.candidate.system.fixedTypeVariables[f.variable.typeConstructor] = ConePrimitiveType.BOOLEAN
        f.implicitCache.restore(emptyCache, setOf(f.work))
        f.implicitCache.computeType(f.work) { ConePrimitiveType.BOOLEAN.toCfirResolvedTypeRef() }
        first.call.replaceConeTypeOrNull(ConePrimitiveType.BOOLEAN)
        childTransaction.rollbackTransaction()
        parentTransaction.rollbackTransaction()
        restoreSelected()
        assertSame(selectedReference, first.call.calleeReference)
        assertSame(selectedArguments, first.call.argumentList)
        assertSame(selectedCacheStatus, f.implicitCache.getStatus(f.work))
        assertEquals(ConePrimitiveType.INT32, f.system.fixedTypeVariables[f.variable.typeConstructor])
        assertEquals(ConePrimitiveType.INT32, first.candidate.system.fixedTypeVariables[f.variable.typeConstructor])
        val rootReference = f.rootCall.calleeReference
        f.write(completedWriter)
        f.write(laterWriter)
        assertSame(rootReference, f.rootCall.calleeReference)
        assertSame(selectedReference, first.call.calleeReference)
        assertEquals(1, first.writes)
        assertEquals(0, second.writes)
        // 恢复原成功事实，只让 fresh 实际 FULL writer 消费 exact atom，不重复 body callback。
        f.full()
        assertFalse(f.owner.postponedPCLACalls.any { it === first.atom })
        assertSame(selectedReference, first.call.calleeReference)
        assertSame(selectedArguments, first.call.argumentList)
        assertEquals(1, first.writes)
        assertEquals(0, second.writes)
    }

    @Test
    fun copiedOwnerStateCannotTransferReceiptOrMutateSiblingState() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        f.full()
        val selected = f.owner.capturePCLACompletionState()
        val ast = CfirResolutionSnapshot.capture(first.call)
        val siblingCall = f.call("root")
        val sibling = f.candidate(siblingCall, f.rootSymbol, f.system.captureStorage())
        sibling.initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList())
        siblingCall.replaceCalleeReference(CfirNamedReferenceWithCandidate(f.source, Name.identifier("root"), sibling))
        sibling.restorePCLACompletionState(selected.copy())
        val second = f.child(first.call)
        f.install(second)
        sibling.postponedPCLACalls += second.atom
        assertThrows(IllegalStateException::class.java) { f.writer(sibling, siblingCall) }
        assertNotSame(f.owner.postponedPCLACalls, sibling.postponedPCLACalls)
        ast.restoreWithImplicitBodyCache(null, null)
        f.full()
        assertEquals(1, first.writes)
        assertTrue(f.owner.postponedPCLACalls.isEmpty())
        assertTrue(sibling.postponedPCLACalls.any { it === first.atom })
    }

    @Test
    fun copiedCarrierAndNewAtomRequireTheirOwnActualFullWrite() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        f.full()
        val copiedCall = buildFunctionCallCopy(first.call) {}
        val copiedAtom = ConeAtomWithCandidate(copiedCall, first.candidate)
        f.owner.postponedPCLACalls.clear()
        f.owner.postponedPCLACalls += copiedAtom
        f.full()
        assertEquals(2, first.writes, "copied carrier inherited an unexecuted receipt")
        assertNotSame(first.call, copiedCall)
        assertTrue(copiedCall.calleeReference is CfirResolvedNamedReference)
    }

    @Test
    fun newAtomForSameCandidateAndCarrierCannotInheritOldAtomReceipt() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        f.full()
        val replacementAtom = ConeAtomWithCandidate(first.call, first.candidate)
        f.owner.postponedPCLACalls.clear()
        f.owner.postponedPCLACalls += replacementAtom
        f.full()
        assertEquals(2, first.writes)
        assertNotSame(first.atom, replacementAtom)
    }

    @Test
    fun anotherFactoryRevokesHeldWriterWithoutRevokingCompletedFact() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        val oldWriter = f.writer()
        f.write(oldWriter)
        val currentWriter = f.writer()
        val rootReference = f.rootCall.calleeReference
        val callReference = first.call.calleeReference
        f.write(oldWriter)
        assertSame(rootReference, f.rootCall.calleeReference)
        assertSame(callReference, first.call.calleeReference)
        assertEquals(1, first.writes)
        assertTrue(f.owner.postponedPCLACalls.any { it === first.atom })
        f.write(currentWriter)
        assertTrue(f.owner.postponedPCLACalls.isEmpty())
        assertEquals(1, first.writes)
    }

    @Test
    fun realFullPreparationCannotUseItsOldSubstitutorAfterActualBindingChanges() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        // 这一步调用生产 FULL completer，随后通过公开 mutable binding API 注入同 VM 的实际错解。
        val writer = f.writer()
        // 用公开 actual fixed-binding map 注入过期解；不伪造 substitutor 或 receipt。
        f.system.fixedTypeVariables[f.variable.typeConstructor] = ConePrimitiveType.BOOLEAN
        f.write(writer)
        assertEquals(1, first.writes)
        assertEquals(ConePrimitiveType.INT32, first.call.coneTypeOrNull)
        assertEquals(ConePrimitiveType.BOOLEAN, f.system.fixedTypeVariables[f.variable.typeConstructor])
        val second = f.child(first.call)
        f.install(second)
        f.owner.postponedPCLACalls += second.atom
        assertThrows(IllegalStateException::class.java) { f.writer() }
        assertEquals(0, second.writes)
    }

    @Test
    fun standardCompleteCallPreservesFrozenStorageAndWritesQueuedCall() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        val result = f.transformer.components.callCompleter.completeCall(f.rootCall, ResolutionMode.ContextIndependent)
        assertSame(f.rootCall, result)
        f.assertWritten(first)
        assertEquals(1, first.writes)
        val storage = f.system.captureStorage()
        assertSame(f.variable, storage.allTypeVariables[f.variable.typeConstructor])
        assertEquals(ConePrimitiveType.INT32, storage.fixedTypeVariables[f.variable.typeConstructor])
        // 标准 FULL 分支必须仍然冻结实际 CS；不能为了 receipt 改成可继续补全的系统。
        assertThrows(IllegalStateException::class.java) { f.system.asConstraintSystemCompleterContext() }
    }

    /** 写回借用结束后，下一次语义访问仍导入真实 parent 新增的 VM/约束，不能永久跳过同步。 */
    @Test
    fun fullWriteReturnsTheChildSystemToNormalParentSynchronization() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        val originalChildSystem = first.candidate.system
        val writer = f.writer()
        val laterVariable = ConeTypeVariable("LaterSemanticConstraint")
        f.system.registerVariable(laterVariable)
        f.system.addEqualityConstraint(laterVariable.defaultType, ConePrimitiveType.BOOLEAN,
            SimpleConstraintSystemConstraintPosition)

        f.write(writer)

        f.assertWritten(first)
        assertEquals(1, first.writes)
        val current = first.candidate.system
        assertSame(originalChildSystem, current)
        assertSame(laterVariable, current.allTypeVariables[laterVariable.typeConstructor])
        assertTrue(laterVariable.typeConstructor in current.notFixedTypeVariables)
        assertTrue(current.notFixedTypeVariables.getValue(laterVariable.typeConstructor).constraints.any {
            it.kind == org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind.EQUALITY &&
                it.type == ConePrimitiveType.BOOLEAN
        })
        assertEquals(ConePrimitiveType.INT32, current.fixedTypeVariables[f.variable.typeConstructor])
    }

    /** ordinary callback 异常必须原样传播，且 entry 借用边界在异常后仍还原正常 rebase。 */
    @Test
    fun fullWriteCallbackFailureReleasesTheChildSystemBorrow() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        val writer = f.writer()
        val failure = IllegalStateException("actual unscoped callback failure")
        first.candidate.onPCLACompletionResultsWritingCallbacks += { throw failure }
        f.system.fixedTypeVariables[f.variable.typeConstructor] = ConePrimitiveType.BOOLEAN

        assertSame(failure, assertThrows(IllegalStateException::class.java) { f.write(writer) })
        assertEquals(1, first.writes)
        // 正常 getter 继续执行原来的 ancestor fixed-binding 检查，未被异常路径永久挂起。
        assertThrows(IllegalStateException::class.java) { first.candidate.system }
        assertEquals(ConePrimitiveType.BOOLEAN, f.system.fixedTypeVariables[f.variable.typeConstructor])
    }

    /** 标准 FULL 冻结后，所有已有可变上下文入口都须拒绝，结果及原 VM 不受拒绝操作影响。 */
    @Test
    fun standardFullFreezesBuilderAnalyzerAndTransactionEntryPoints() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        f.transformer.components.callCompleter.completeCall(f.rootCall, ResolutionMode.ContextIndependent)

        assertThrows(IllegalStateException::class.java) { f.system.getBuilder() }
        assertThrows(IllegalStateException::class.java) { f.system.asPostponedArgumentsAnalyzerContext() }
        assertThrows(IllegalStateException::class.java) { f.system.prepareTransaction() }
        assertThrows(IllegalStateException::class.java) { f.system.registerVariable(ConeTypeVariable("RejectedFrozenMutation")) }
        val storage = f.system.captureStorage()
        assertSame(f.variable, storage.allTypeVariables[f.variable.typeConstructor])
        assertEquals(ConePrimitiveType.INT32, storage.fixedTypeVariables[f.variable.typeConstructor])
        f.assertWritten(first)
        assertEquals(1, first.writes)
    }

    @Test
    fun realFullProducerSolvesUnfixedDeclarationVmBeforeMintingReceipt() {
        val f = Fixture(fixed = false)
        f.system.addEqualityConstraint(f.variable.defaultType, ConePrimitiveType.INT32, SimpleConstraintSystemConstraintPosition)
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        assertTrue(f.variable.typeConstructor in f.system.notFixedTypeVariables)
        val writer = f.writer()
        assertEquals(ConePrimitiveType.INT32, f.system.fixedTypeVariables[f.variable.typeConstructor])
        assertEquals(0, first.writes)
        f.write(writer)
        f.assertWritten(first)
        f.full()
        assertTrue(f.owner.postponedPCLACalls.isEmpty())
        assertEquals(1, first.writes)
    }

    @Test
    fun snapshotTakenAfterFactoryCannotReactivateTheSameHeldPass() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        val held = f.writer()
        val afterFactory = f.owner.capturePCLACompletionState()
        f.owner.restorePCLACompletionState(afterFactory)
        val reference = first.call.calleeReference
        val rootReference = f.rootCall.calleeReference
        val arguments = first.call.argumentList
        f.write(held)
        assertSame(reference, first.call.calleeReference)
        assertSame(rootReference, f.rootCall.calleeReference)
        assertSame(arguments, first.call.argumentList)
        assertEquals(0, first.writes)
        f.full()
        assertEquals(1, first.writes)
    }

    @Test
    fun childSuccessfulRestoreRevokesHeldWriterWithoutDiscardingMatchingParentFact() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        f.full()
        val selectedChild = first.candidate.capturePCLACompletionState()
        val held = f.writer()
        first.candidate.restorePCLACompletionState(selectedChild)
        val rootReference = f.rootCall.calleeReference
        val reference = first.call.calleeReference
        f.write(held)
        assertSame(rootReference, f.rootCall.calleeReference)
        assertSame(reference, first.call.calleeReference)
        assertEquals(1, first.writes)
        f.full()
        assertSame(reference, first.call.calleeReference)
        assertTrue(f.owner.postponedPCLACalls.isEmpty())
        assertEquals(1, first.writes)
    }

    @Test
    fun changedChildFixedBindingInvalidatesReceiptWithUnchangedParentSolution() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        f.full()
        // 单独改变 child 实际解，parent 系统、VM、target 和 carrier 保持相同。
        first.candidate.system.fixedTypeVariables[f.variable.typeConstructor] = ConePrimitiveType.BOOLEAN
        assertEquals(ConePrimitiveType.INT32, f.system.fixedTypeVariables[f.variable.typeConstructor])
        assertEquals(ConePrimitiveType.BOOLEAN, first.candidate.system.fixedTypeVariables[f.variable.typeConstructor])
        val second = f.child(first.call)
        f.assertSameVm(first, second)
        f.install(second)
        f.owner.postponedPCLACalls += second.atom
        val failure = assertThrows(IllegalStateException::class.java) { f.writer() }
        assertEquals("Completion carrier has conflicting actual candidates", failure.message)
        assertEquals(0, second.writes)
    }

    @Test
    fun sameNamedFreshVmCannotReplaceReceiptVmDespiteEqualFixedTypes() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        f.full()
        val replacement = ConeTypeParameterBasedTypeVariable(f.parameter.symbol, TypeVariableInferenceScope())
        first.candidate.system.registerVariable(replacement)
        first.candidate.system.fixVariable(replacement, ConePrimitiveType.INT32, ConeFixVariableConstraintPosition(replacement))
        first.candidate.initializeSubstitutorAndVariables(
            CfirTypeSubstitutorByMap(mapOf(f.parameter.symbol.toLookupTag() to replacement.defaultType)), listOf(replacement))
        assertNotSame(f.variable, replacement)
        val second = f.child(first.call)
        f.install(second)
        f.owner.postponedPCLACalls += second.atom
        val failure = assertThrows(IllegalStateException::class.java) { f.writer() }
        assertEquals("Completion carrier has conflicting actual candidates", failure.message)
        assertEquals(1, first.writes)
        assertEquals(0, second.writes)
    }

    @Test
    fun additionalOwnedVmIntroducedAfterCompletionInvalidatesReceipt() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        f.full()
        val extra = ConeTypeParameterBasedTypeVariable(f.parameter.symbol, TypeVariableInferenceScope())
        first.candidate.system.registerVariable(extra)
        first.candidate.system.fixVariable(extra, ConePrimitiveType.INT32, ConeFixVariableConstraintPosition(extra))
        first.candidate.additionalCompletionVariables += extra.typeConstructor
        val second = f.child(first.call)
        f.install(second)
        f.owner.postponedPCLACalls += second.atom
        val failure = assertThrows(IllegalStateException::class.java) { f.writer() }
        assertEquals("Completion carrier has conflicting actual candidates", failure.message)
        assertEquals(0, second.writes)
    }

    @Test
    fun sameResolvedResultCannotHideFactoryLateUnselectedLiveAtom() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        f.full()
        val held = f.writer()
        val second = f.child(first.call)
        f.owner.postponedPCLACalls += second.atom
        val reference = first.call.calleeReference
        val arguments = first.call.argumentList
        val failure = assertThrows(IllegalStateException::class.java) { f.write(held) }
        assertEquals("Queued completion carrier changed its actual candidate", failure.message)
        assertSame(reference, first.call.calleeReference)
        assertSame(arguments, first.call.argumentList)
        assertEquals(1, first.writes)
        assertEquals(0, second.writes)
    }

    @Test
    fun ordinaryFinalSystemCannotConsumeAnExistingRealFullReceipt() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        f.full()
        val second = f.child(first.call)
        f.install(second)
        f.owner.postponedPCLACalls += second.atom
        val failure = assertThrows(IllegalStateException::class.java) { f.ordinaryWriter() }
        assertEquals("Completion carrier has conflicting actual candidates", failure.message)
        assertEquals(1, first.writes)
        assertEquals(0, second.writes)
        f.full()
        assertEquals(1, second.writes)
        assertFalse(f.owner.postponedPCLACalls.any { it === first.atom })
    }

    @Test
    fun unscopedFullRootNeedsItsExactCandidateReference() {
        val f = Fixture()
        f.full()
        val reference = f.rootCall.calleeReference
        assertTrue(reference is CfirResolvedNamedReference)
        val failure = assertThrows(IllegalStateException::class.java) {
            f.transformer.components.callCompleter.runFullCompletionForCall(f.owner, f.rootCall)
        }
        assertEquals("FULL receipt preparation changed its actual root candidate", failure.message)
        assertSame(reference, f.rootCall.calleeReference)
    }

    @Test
    fun currentResultWitnessRejectsChangedArgumentsReceiverAndTypeArguments() {
        for (change in listOf("arguments", "receiver", "typeArguments")) {
            val f = Fixture()
            val first = f.child()
            f.owner.postponedPCLACalls += first.atom
            f.full()
            when (change) {
                "arguments" -> first.call.replaceArgumentList(buildArgumentList {
                    arguments += CallResolutionTestFixtures.buildTypedExpression(ConePrimitiveType.INT32)
                })
                "receiver" -> first.call.replaceDispatchReceiver(CallResolutionTestFixtures.buildTypedExpression(ConePrimitiveType.INT32))
                "typeArguments" -> first.call.replaceTypeArguments(listOf(ConePrimitiveType.BOOLEAN.toCfirResolvedTypeRef()))
            }
            f.full()
            assertEquals(2, first.writes, "changed $change inherited completion evidence")
            assertTrue(f.owner.postponedPCLACalls.any { it === first.atom })
        }
    }

    @Test
    fun parentOnlyRestoreCannotReviveReceiptForResetGeneratedUnscopedChild() {
        val f = Fixture()
        val child = f.generatedUnscopedLambda()
        f.full()
        assertEquals(1, child.writes)
        val parentState = f.owner.capturePCLACompletionState()
        val held = f.writer()
        child.candidate.resetLambdaBodyResolutionResults()
        f.owner.restorePCLACompletionState(parentState)
        val rootReference = f.rootCall.calleeReference
        val reference = child.call.calleeReference
        f.write(held)
        assertSame(rootReference, f.rootCall.calleeReference)
        assertSame(reference, child.call.calleeReference)
        assertEquals(1, child.writes)
        val replacement = f.child(child.call)
        f.install(replacement)
        f.owner.postponedPCLACalls += replacement.atom
        val failure = assertThrows(IllegalStateException::class.java) { f.writer() }
        assertEquals("Completion carrier has conflicting actual candidates", failure.message)
        assertEquals(0, replacement.writes)
    }

    @Test
    fun fullSuccessfulSnapshotRestoresGeneratedChildFactAfterActualReset() {
        val f = Fixture()
        val child = f.generatedUnscopedLambda()
        f.full()
        assertEquals(1, child.writes)
        val lambda = child.candidate.callInfo.callSite as CfirAnonymousFunctionExpression
        f.implicitCache.compute(lambda.anonymousFunction.symbol) { lambda.anonymousFunction }
        val status = f.implicitCache.getStatus(lambda.anonymousFunction.symbol)
        val restoreSelected = f.captureSuccessfulState(child)
        val selectedReference = child.call.calleeReference
        val selectedArguments = child.call.argumentList
        val held = f.writer()
        child.candidate.resetLambdaBodyResolutionResults()
        restoreSelected()
        f.write(held)
        assertSame(selectedReference, child.call.calleeReference)
        assertEquals(1, child.writes)
        assertSame(status, f.implicitCache.getStatus(lambda.anonymousFunction.symbol))
        f.full()
        assertFalse(f.owner.postponedPCLACalls.any { it === child.atom })
        assertSame(selectedReference, child.call.calleeReference)
        assertSame(selectedArguments, child.call.argumentList)
        assertEquals(1, child.writes)
    }

    @Test
    fun restoredSuccessCannotReactivateHeldFullProducerCapability() {
        val f = Fixture()
        val first = f.child()
        f.owner.postponedPCLACalls += first.atom
        val completer = f.transformer.components.callCompleter
        val actualFull = completer.runFullCompletionForCall(f.owner, f.rootCall)
        val substitutor = actualFull.system.captureStorage().buildAbstractResultingSubstitutor(f.session.typeContext).asCone()
        val writer = completer.createCompletionResultsWriterForFullCompletion(actualFull, substitutor)
        f.write(writer)
        val selected = f.owner.capturePCLACompletionState()
        f.owner.restorePCLACompletionState(selected)
        val reference = f.rootCall.calleeReference
        val failure = assertThrows(IllegalStateException::class.java) {
            completer.createCompletionResultsWriterForFullCompletion(actualFull, substitutor)
        }
        assertEquals("FULL producer authority was revoked before writer creation", failure.message)
        assertSame(reference, f.rootCall.calleeReference)
        assertEquals(1, first.writes)
        f.full()
        assertTrue(f.owner.postponedPCLACalls.isEmpty())
        assertEquals(1, first.writes)
    }


    /** 普通语义快照不能复活已被成功重查替换的 source producer，包括重新创建 FULL/pass。 */
    @Test
    fun withdrawnSourceCannotMintFreshFullOrPassAfterSemanticRestore() {
        val f = Fixture()
        val lambdaOwner = f.generatedUnscopedLambda().candidate
        val previous = f.child()
        f.owner.postponedPCLACalls += previous.atom
        val before = previous.candidate.capturePCLACompletionState()
        val callbacks = previous.candidate.onPCLACompletionResultsWritingCallbacks
        val calls = previous.candidate.postponedPCLACalls

        lambdaOwner.resetLambdaBodyResolutionResults(listOf(previous.candidate))
        previous.candidate.restorePCLACompletionState(before.copy())
        assertSame(callbacks, previous.candidate.onPCLACompletionResultsWritingCallbacks)
        assertSame(calls, previous.candidate.postponedPCLACalls)
        assertTrue(previous.candidate.isSourceCompletionWithdrawn)

        val completer = f.transformer.components.callCompleter
        val authority = completer.runFullCompletionForCall(f.owner, f.rootCall)
        val substitutor = authority.system.captureStorage().buildAbstractResultingSubstitutor(f.session.typeContext).asCone()
        val reference = previous.call.calleeReference
        val rootReference = f.rootCall.calleeReference
        val fullFailure = assertThrows(IllegalStateException::class.java) {
            completer.runFullCompletionForCall(previous.candidate, previous.call)
        }
        assertEquals("Withdrawn source producer cannot run FULL completion", fullFailure.message)
        val passFailure = assertThrows(IllegalStateException::class.java) {
            previous.candidate.captureUnscopedCompletionPass(authority)
        }
        assertEquals("Withdrawn source producer cannot create a completion pass", passFailure.message)
        val factoryFailure = assertThrows(IllegalStateException::class.java) {
            completer.createCompletionResultsWriter(substitutor, completionSystem = authority.system,
                completionRoot = previous.call, completionCandidate = previous.candidate)
        }
        assertEquals("Final writer source producer was withdrawn", factoryFailure.message)
        assertSame(reference, previous.call.calleeReference)
        assertSame(rootReference, f.rootCall.calleeReference)
        assertEquals(0, previous.writes)
        val storage = previous.candidate.withConstraintSystemForCompletionResults { it.captureStorage() }
        assertSame(f.variable, storage.allTypeVariables[f.variable.typeConstructor])
        assertEquals(ConePrimitiveType.INT32, storage.fixedTypeVariables[f.variable.typeConstructor])
    }

    /** 已恢复的旧队列/回调不能覆盖当前 source candidate，两个真实 atom 的顺序都不能决定 winner。 */
    @Test
    fun freshCollectorAfterRestoreSkipsWithdrawnSourceAndWritesActualSuccessor() {
        for (newFirst in listOf(false, true)) {
            val f = Fixture()
            val lambdaOwner = f.generatedUnscopedLambda().candidate
            val previous = f.child()
            f.owner.postponedPCLACalls += previous.atom
            f.full()
            assertEquals(1, previous.writes)
            val childState = previous.candidate.capturePCLACompletionState()
            val parentState = f.owner.capturePCLACompletionState()

            lambdaOwner.resetLambdaBodyResolutionResults(listOf(previous.candidate))
            previous.candidate.restorePCLACompletionState(childState.copy())
            f.owner.restorePCLACompletionState(parentState)
            val current = f.child(previous.call)
            f.install(current)
            f.owner.postponedPCLACalls.clear()
            f.owner.postponedPCLACalls += if (newFirst) listOf(current.atom, previous.atom)
                else listOf(previous.atom, current.atom)

            f.full()
            f.assertWritten(current)
            assertFalse(f.owner.postponedPCLACalls.any { it === previous.atom })
            assertEquals(1, previous.writes)
            assertEquals(1, current.writes)
            for (candidate in listOf(previous.candidate, current.candidate)) {
                val storage = candidate.withConstraintSystemForCompletionResults { it.captureStorage() }
                assertSame(f.variable, storage.allTypeVariables[f.variable.typeConstructor])
                assertEquals(ConePrimitiveType.INT32, storage.fixedTypeVariables[f.variable.typeConstructor])
            }
        }
    }

    /** 有真实形参、实参 mapping 与 PCLA hook 的队列边界夹具，不读取私有数字水位。 */
    private class StatementParameter(private val fixture: Fixture) {
        private val variable = ConeTypeVariable("StatementParameter")
        private val symbol = CfirAnonymousFunctionSymbol()
        val parameter = CallResolutionTestFixtures.buildValueParameter(fixture.session, "input", variable.defaultType,
            containingDeclarationSymbol = symbol)
        val lambda = buildAnonymousFunction {
            source = fixture.source
            moduleData = fixture.session.moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            status = CfirDeclarationStatusImpl()
            this.symbol = this@StatementParameter.symbol
            valueParameters += parameter
            returnTypeRef = ConePrimitiveType.UNIT.toCfirResolvedTypeRef()
            typeRef = ConeFunctionType(listOf(variable.defaultType), ConePrimitiveType.UNIT).toCfirResolvedTypeRef()
            body = buildBlock { coneTypeOrNull = ConePrimitiveType.UNIT }
            hasExplicitParameterList = true
            isLambda = true
        }

        fun start(): CfirPCLAInferenceSession = CfirPCLAInferenceSession(fixture.owner,
            fixture.session.inferenceComponents, lambda).also {
            fixture.context.inferenceSession = it
            it.currentCommonSystem.registerVariable(variable)
        }

        fun enqueue(expected: ConeCangJieType): ConeAtomWithCandidate {
            val argument = buildNamedAccessExpression {
                source = fixture.source
                coneTypeOrNull = variable.defaultType
                calleeReference = buildResolvedNamedReference { name = parameter.name; resolvedSymbol = parameter.symbol }
            }
            val target = CallResolutionTestFixtures.buildFunctionSymbol(fixture.session, "consume", ConePrimitiveType.UNIT,
                parameterTypes = listOf(expected))
            val call = buildFunctionCall {
                source = fixture.source
                coneTypeOrNull = ConePrimitiveType.UNIT
                calleeReference = buildNamedReference { name = target.cfir.name }
                argumentList = buildArgumentList { arguments += argument }
            }
            val info = CallInfo(callSite = call, callKind = CallKind.Function, name = target.cfir.name,
                explicitReceiver = null, arguments = listOf(argument), isUsedAsGetClassReceiver = false,
                typeArguments = emptyList(), session = fixture.session, containingFile = fixture.context.file,
                containingDeclarations = emptyList(), resolutionMode = ResolutionMode.ContextIndependent)
            val candidate = Candidate(symbol = target, dispatchReceiver = null, givenExtensionReceiver = null,
                explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                constraintSystemFactory = fixture.session.inferenceComponents.constraintSystemFactory,
                baseSystem = fixture.system.captureStorage(), callInfo = info, originScope = null,
                bodyResolveContext = fixture.context).apply {
                initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList())
                val argumentAtom = ConeResolutionAtom.createRawAtom(argument)
                initializeArgumentMapping(listOf(argumentAtom), linkedMapOf(argumentAtom to target.cfir.valueParameters.single()))
            }
            call.replaceCalleeReference(CfirNamedReferenceWithCandidate(fixture.source, target.cfir.name, candidate))
            return ConeAtomWithCandidate(call, candidate).also { fixture.owner.postponedPCLACalls += it }
        }
    }

    /** 记录实际 ordinary callback 次数，不记录或读取 receipt 的内部状态。 */
    private class Call(
        val call: CfirFunctionCall,
        val candidate: Candidate,
        val atom: ConeAtomWithCandidate = ConeAtomWithCandidate(call, candidate),
    ) {
        var writes = 0
        init { candidate.onPCLACompletionResultsWritingCallbacks += { writes++ } }
    }

    /**
     * 真正 generic work<T> 的 fresh VM 参与返回签名，所有调用共享实际 parent CS 的同一 VM。
     * 类型参数、Candidate、mapping、CS 与 FULL writer 沿现有测试基础设施构造。
     */
    private class Fixture(fixed: Boolean = true) {
        val session = CallResolutionTestFixtures.newTestSession().also {
            it.register(CfirBuiltinTypes::class, CfirBuiltinTypes())
            it.register(CfirAccessibilityChecker::class, CfirAccessibilityChecker(it))
        }
        val source = checkNotNull(CallResolutionTestFixtures.buildTypedExpression(ConePrimitiveType.INT32).source)
        val context = BodyResolveContext(ReturnTypeCalculator.Default, CfirDataFlowAnalyzerContext()).apply {
            file = CallResolutionTestFixtures.buildCallInfo(session, "root").containingFile
        }
        val implicitCache = ImplicitBodyResolveComputationSession()
        val transformer = object : CfirBodyResolveTransformer(session, ScopeSession(), outerBodyResolveContext = context) {
            override val implicitBodyResolveComputationSession: ImplicitBodyResolveComputationSession = implicitCache
        }
        val rootSymbol = CallResolutionTestFixtures.buildFunctionSymbol(session, "root", ConePrimitiveType.INT32)
        val rootCall = call("root")
        val owner = candidate(rootCall, rootSymbol, ConstraintSystemTestHarness.newSystem(session).captureStorage()).apply {
            initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList())
        }
        val system: ConstraintSystemImpl get() = owner.system
        val parameter = ExtendTestFixtures.newTypeParameter(session.moduleData, "T")
        val work = CallResolutionTestFixtures.buildFunctionSymbol(session, "work",
            ConeTypeParameterTypeImpl(parameter.symbol.toLookupTag()), typeParameters = listOf(parameter))
        val variable = ConeTypeParameterBasedTypeVariable(parameter.symbol, TypeVariableInferenceScope())

        init {
            rootCall.replaceCalleeReference(CfirNamedReferenceWithCandidate(source, Name.identifier("root"), owner))
            system.registerVariable(variable)
            owner.additionalCompletionVariables += variable.typeConstructor
            if (fixed) fix(ConePrimitiveType.INT32)
        }

        fun fix(type: ConeCangJieType) = system.fixVariable(variable, type, ConeFixVariableConstraintPosition(variable))

        fun call(name: String = "work") = buildFunctionCall {
            source = this@Fixture.source
            coneTypeOrNull = ConePrimitiveType.INT32
            calleeReference = buildNamedReference { this.name = Name.identifier(name) }
        }

        fun candidate(call: CfirFunctionCall, symbol: CfirNamedFunctionSymbol, storage: ConstraintStorage): Candidate {
            val info = CallInfo(callSite = call, callKind = CallKind.Function, name = symbol.cfir.name, explicitReceiver = null,
                arguments = emptyList(), isUsedAsGetClassReceiver = false, typeArguments = emptyList(), session = session,
                containingFile = context.file, containingDeclarations = emptyList(), resolutionMode = ResolutionMode.ContextIndependent)
            return Candidate(symbol = symbol, dispatchReceiver = null, givenExtensionReceiver = null,
                explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                constraintSystemFactory = session.inferenceComponents.constraintSystemFactory, baseSystem = storage,
                callInfo = info, originScope = null, bodyResolveContext = context).apply {
                // 与本 fixture 的空源码类型实参一致；共享 VM 仍由实际声明参数参与 FULL 求解。
                typeArgumentMapping = TypeArgumentMapping.NoExplicitArguments
                initializeArgumentMapping(emptyList(), linkedMapOf())
            }
        }

        fun child(carrier: CfirFunctionCall? = null): Call {
            val call = carrier ?: call()
            val candidate = candidate(call, work, system.captureStorage())
            candidate.initializeSubstitutorAndVariables(
                CfirTypeSubstitutorByMap(mapOf(parameter.symbol.toLookupTag() to variable.defaultType)), listOf(variable))
            assertNull(candidate.implicitTypeComputationScope)
            return Call(call, candidate).also { if (carrier == null) install(it) }
        }

        fun install(call: Call) {
            call.call.replaceCalleeReference(CfirNamedReferenceWithCandidate(source, call.candidate.callInfo.name, call.candidate))
        }

        /** 真实 SyntheticGenerator + PCLA 路径，保留 generator 入队的原 atom 和原 candidate。 */
        fun generatedUnscopedLambda(): Call {
            val functionType = ConeFunctionType(emptyList(), ConePrimitiveType.UNIT)
            val lambda = buildAnonymousFunctionExpression {
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
            val pcla = CfirPCLAInferenceSession(owner, session.inferenceComponents)
            context.inferenceSession = pcla
            transformer.components.syntheticCallGenerator.resolveAnonymousFunctionExpressionWithSyntheticOuterCall(
                lambda, ResolutionMode.WithExpectedType(functionType.toCfirResolvedTypeRef()), transformer.resolutionContext)
            pcla.applyResultsToMainCandidate()
            val atom = owner.postponedPCLACalls.filterIsInstance<ConeAtomWithCandidate>().single {
                it.candidate.callInfo.callSite === lambda
            }
            assertNull(atom.candidate.implicitTypeComputationScope)
            return Call(atom.expression as CfirFunctionCall, atom.candidate, atom)
        }

        fun publisher(): Call {
            val symbol = CallResolutionTestFixtures.buildFunctionSymbol(session, "publish", ConePrimitiveType.UNIT)
            val call = call("publish")
            call.replaceConeTypeOrNull(ConePrimitiveType.UNIT)
            val candidate = candidate(call, symbol, system.captureStorage()).apply {
                initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList())
            }
            call.replaceCalleeReference(CfirNamedReferenceWithCandidate(source, symbol.cfir.name, candidate))
            return Call(call, candidate)
        }

        fun assertSameVm(vararg calls: Call) {
            calls.forEach { call ->
                assertSame(variable, call.candidate.freshVariables.single())
                assertSame(variable, call.candidate.system.allTypeVariables[variable.typeConstructor])
                assertSame(work, call.candidate.symbol)
            }
        }

        /** 与 SuccessfulCandidateAnalysis 相同边界：实际 AST/cache + 两个实际 CS + 各自 PCLA 状态。 */
        fun captureSuccessfulState(call: Call): () -> Unit {
            val tree = buildBlock {
                coneTypeOrNull = ConePrimitiveType.UNIT
                statements += work.cfir
                statements += rootSymbol.cfir
                statements += rootCall
                statements += call.call
            }
            val ast = CfirResolutionSnapshot.capture(tree)
            val cache = ast.captureImplicitBodyCache(implicitCache)
            val ownerStorage = system.fork().captureStorage()
            val childStorage = call.candidate.system.fork().captureStorage()
            val ownerState = owner.capturePCLACompletionState()
            val childState = call.candidate.capturePCLACompletionState()
            return {
                ast.restoreWithImplicitBodyCache(cache, implicitCache)
                system.replaceContentWith(ownerStorage)
                call.candidate.system.replaceContentWith(childStorage)
                owner.restorePCLACompletionState(ownerState)
                call.candidate.restorePCLACompletionState(childState)
            }
        }

        /** 直接调用标准 completeCall 使用的 actual FULL producer 和 factory，不存在测试 mint helper。 */
        fun writer(candidate: Candidate = owner, root: CfirFunctionCall = rootCall): CfirCallCompletionResultsWriterTransformer {
            // 下一次正式入口显式提交测试持有的 exact root candidate，再由生产 completer 执行 FULL。
            root.replaceCalleeReference(CfirNamedReferenceWithCandidate(source, candidate.callInfo.name, candidate))
            val completer = transformer.components.callCompleter
            val actualFull = completer.runFullCompletionForCall(candidate, root)
            val substitutor = actualFull.system.captureStorage().buildAbstractResultingSubstitutor(session.typeContext).asCone()
            return completer.createCompletionResultsWriterForFullCompletion(actualFull, substitutor)
        }

        fun ordinaryWriter(
            system: ConstraintSystemImpl? = this.system,
            substitutor: ConeSubstitutor = system?.buildCurrentSubstitutor()?.asCone() ?: ConeSubstitutor.Empty,
        ) = transformer.components.callCompleter.createCompletionResultsWriter(substitutor, completionSystem = system,
            completionRoot = rootCall, completionCandidate = owner)

        fun write(writer: CfirCallCompletionResultsWriterTransformer) { rootCall.transformSingle(writer, null) }
        fun full() { write(writer()) }

        fun assertWritten(call: Call) {
            assertSame(call.candidate.symbol, (call.call.calleeReference as CfirResolvedNamedReference).resolvedSymbol)
            assertEquals(ConePrimitiveType.INT32, call.call.coneTypeOrNull)
        }

        fun substitutor(type: ConeCangJieType): ConeSubstitutor {
            val actual = system.buildCurrentSubstitutor().asCone()
            return object : ConeSubstitutor() {
                override fun substituteOrNull(value: ConeCangJieType): ConeCangJieType? =
                    if (value == variable.defaultType) type else actual.substituteOrNull(value)
                override fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection? =
                    actual.substituteArgument(projection, index)
            }
        }
    }
}
