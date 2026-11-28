/*
 * Copyright 2010-2026 Cangnova contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class, org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure

import com.intellij.openapi.command.WriteCommandAction
import org.cangnova.cangjie.analysis.api.platform.modification.CaElementModificationType
import org.cangnova.cangjie.analysis.api.platform.modification.CaSourceModificationLocality
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.collectDiagnosticsForFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLPartialBodyAnalysisSnapshot
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.partialBodyAnalysisState
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirModifiablePsiSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirControlFlowGraphOwner
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.dfa.FlowPath
import org.cangnova.cangjie.cfir.resolve.dfa.PersistentFlow
import org.cangnova.cangjie.cfir.resolve.dfa.Stack
import org.cangnova.cangjie.cfir.resolve.dfa.controlFlowGraph
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.BlockEnterNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.CFGNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.CFGNodeWithSubgraphs
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraphBuilder
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.Edge
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.EnterValueParameterNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ExitValueParameterNode
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhaseRecursively
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.diagnostic.ConeAmbiguityError
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.parsing.compilationParsingResultOrNull
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.text
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 仓颉的块体可以推断返回类型，只有显式返回类型才能保证函数体修改不改变签名。
 *
 * 官方 v1.1.3 PreCheck.cpp:1784–1802 将空体预设为 Unit、非空隐式返回预设为 Quest；
 * 因此空体与非空体互换必须同时失效声明及调用方的签名，不能只重建函数体。
 * 这里验证返回类型的依赖；CJMP 角色与诊断仍由整文件目标解析决定，显式签名不能证明角色稳定。
 */
class FunctionBodyModificationTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/functionBodyModification",
) {
    override val configurator = analysisApiCfirModifiablePsiSourceTestConfigurator(analyseInDependentSession = false)

    /** 显式返回类型保持签名稳定，修改块体后保留原声明和调用方符号。 */
    @Test
    fun explicitReturnType(mainFile: CjFile) {
        assertBodyReplacement(mainFile, "{ 42 }", ConePrimitiveType.INT64, ConePrimitiveType.INT64, inBlock = true)
    }

    /** 非空隐式返回改为空体时，已推断的 Int64 必须失效并重新推断为 Unit。 */
    @Test
    fun inferredReturnTypeToEmptyBody(mainFile: CjFile) {
        assertBodyReplacement(mainFile, "{}", ConePrimitiveType.INT64, ConePrimitiveType.UNIT, inBlock = false)
    }

    /** 原先预检即可确定的 Unit 不能在空体改为非空体后继续缓存。 */
    @Test
    fun emptyBodyToInferredReturnType(mainFile: CjFile) {
        assertBodyReplacement(mainFile, "{ 42 }", ConePrimitiveType.UNIT, ConePrimitiveType.INT64, inBlock = false)
    }

    /** TYPES 请求没有建立已解析 body，源代次刷新仍必须覆盖其惰性正文。 */
    @Test
    fun typesOnlyBodyEdit(mainFile: CjFile) {
        val function = function(mainFile, "compute")
        val facadeBefore = mainFile.getResolutionFacadeForTest()
        val fileBefore = mainFile.getOrBuildCfirFile(facadeBefore)
        val targetBefore = assertTargetSource(mainFile, fileBefore, facadeBefore)
        val symbolBefore = function.resolveToCfirSymbol(facadeBefore, CfirResolvePhase.TYPES)
        val declarationBefore = symbolBefore.cfir as CfirNamedFunction
        val bodyBefore = requireNotNull(declarationBefore.body)
        assertEquals(CfirResolvePhase.TYPES, declarationBefore.resolvePhase)
        assertTrue(bodyBefore is CfirLazyBlock)
        assertEquals(ConePrimitiveType.INT64, declarationBefore.returnTypeRef.coneTypeOrNull)
        assertElementSource(function, declarationBefore, targetBefore)

        replaceExplicitBodies(mainFile, listOf(function to "{ 42 }"))

        val facadeAfter = mainFile.getResolutionFacadeForTest()
        val fileAfter = mainFile.getOrBuildCfirFile(facadeAfter)
        val targetAfter = assertTargetSource(mainFile, fileAfter, facadeAfter)
        assertSame(fileBefore, fileAfter)
        assertSame(facadeBefore.useSiteCfirSession, facadeAfter.useSiteCfirSession)
        assertNotSame(targetBefore, targetAfter)
        val symbolAfter = function.resolveToCfirSymbol(facadeAfter, CfirResolvePhase.BODY_RESOLVE)
        assertSame(symbolBefore, symbolAfter)
        assertSame(declarationBefore, symbolAfter.cfir)
        assertNotSame(bodyBefore, declarationBefore.body)
        assertResolvedIntegers(declarationBefore, listOf("42"))
        assertElementSource(function, declarationBefore, targetAfter)
        assertSubtreeSourcesCurrent(fileAfter, targetAfter)
    }

    /** 同一写事务的两个块内修改必须一次发布整文件新代，不能逐个保留旧 target。 */
    @Test
    fun twoResolvedBodiesInOneWrite(mainFile: CjFile) {
        val first = function(mainFile, "first")
        val second = function(mainFile, "second")
        val caller = function(mainFile, "use")
        val functions = listOf(first, second, caller)
        val facadeBefore = mainFile.getResolutionFacadeForTest()
        val fileBefore = mainFile.getOrBuildCfirFile(facadeBefore)
        val targetBefore = assertTargetSource(mainFile, fileBefore, facadeBefore)
        val symbolsBefore = functions.map { it.resolveToCfirSymbol(facadeBefore, CfirResolvePhase.BODY_RESOLVE) }
        val declarationsBefore = symbolsBefore.map { it.cfir as CfirNamedFunction }
        val bodiesBefore = declarationsBefore.map { requireNotNull(it.body) }
        assertResolvedIntegers(declarationsBefore[0], listOf("1"))
        assertResolvedIntegers(declarationsBefore[1], listOf("2"))

        replaceExplicitBodies(mainFile, listOf(first to "{ 42 }", second to "{ 84 }"))

        assertTrue(declarationsBefore[0].body is CfirLazyBlock)
        assertTrue(declarationsBefore[1].body is CfirLazyBlock)
        val facadeAfter = mainFile.getResolutionFacadeForTest()
        val fileAfter = mainFile.getOrBuildCfirFile(facadeAfter)
        val targetAfter = assertTargetSource(mainFile, fileAfter, facadeAfter)
        assertSame(fileBefore, fileAfter)
        assertSame(facadeBefore.useSiteCfirSession, facadeAfter.useSiteCfirSession)
        assertNotSame(targetBefore, targetAfter)
        // 在解析任一新正文前，保留子图的所有 source 都已经属于同一个新 target。
        functions.zip(declarationsBefore).forEach { (psi, declaration) ->
            assertElementSource(psi, declaration, targetAfter)
        }
        assertSubtreeSourcesCurrent(fileAfter, targetAfter)
        // 从第二个函数先发起解析，避免只能按写入顺序换源的实现通过。
        for (index in listOf(1, 0, 2)) {
            val after = functions[index].resolveToCfirSymbol(facadeAfter, CfirResolvePhase.BODY_RESOLVE)
            assertSame(symbolsBefore[index], after)
            assertSame(declarationsBefore[index], after.cfir)
        }
        assertNotSame(bodiesBefore[0], declarationsBefore[0].body)
        assertNotSame(bodiesBefore[1], declarationsBefore[1].body)
        assertSame(bodiesBefore[2], declarationsBefore[2].body, "未改动 caller 的正文保持身份")
        assertResolvedIntegers(declarationsBefore[0], listOf("42"))
        assertResolvedIntegers(declarationsBefore[1], listOf("84"))
        assertSubtreeSourcesCurrent(fileAfter, targetAfter)
    }

    /** 先建立 partial mapper，再经历两轮编辑；每轮首句映射和完整 body 都读取本代。 */
    @Test
    fun consecutiveBodyEditsAfterPartialMapping(mainFile: CjFile) {
        val function = function(mainFile, "compute")
        val facadeBefore = mainFile.getResolutionFacadeForTest()
        val fileBefore = mainFile.getOrBuildCfirFile(facadeBefore)
        var previousTarget = assertTargetSource(mainFile, fileBefore, facadeBefore)
        val symbolBefore = function.resolveToCfirSymbol(facadeBefore, CfirResolvePhase.TYPES)
        val declaration = symbolBefore.cfir as CfirNamedFunction
        var previousMapped = assertPartialFirstStatement(function, declaration, facadeBefore, previousTarget, "1")

        for (values in listOf(listOf("42", "43", "44"), listOf("420", "421", "422"))) {
            val previousBody = requireNotNull(declaration.body)
            replaceExplicitBodies(mainFile, listOf(function to values.joinToString("\n", "{\n", "\n}")))
            val facadeAfter = mainFile.getResolutionFacadeForTest()
            val fileAfter = mainFile.getOrBuildCfirFile(facadeAfter)
            val targetAfter = assertTargetSource(mainFile, fileAfter, facadeAfter)
            assertSame(fileBefore, fileAfter)
            assertSame(facadeBefore.useSiteCfirSession, facadeAfter.useSiteCfirSession)
            assertNotSame(previousTarget, targetAfter)
            assertSame(symbolBefore, function.resolveToCfirSymbol(facadeAfter, CfirResolvePhase.TYPES))
            val mapped = assertPartialFirstStatement(function, declaration, facadeAfter, targetAfter, values.first())
            assertNotSame(previousMapped, mapped)
            assertNotSame(previousBody, declaration.body)
            assertSame(symbolBefore, function.resolveToCfirSymbol(facadeAfter, CfirResolvePhase.BODY_RESOLVE))
            assertResolvedIntegers(declaration, values)
            assertSubtreeSourcesCurrent(fileAfter, targetAfter)
            previousTarget = targetAfter
            previousMapped = mapped
        }
    }

    /** body 长度改变后，后继声明及符号保留，source 的内容、对象对应和坐标均迁移到新代。 */
    @Test
    fun followingDeclarationSourceMoves(mainFile: CjFile) {
        val function = function(mainFile, "compute")
        val following = function(mainFile, "following")
        val facadeBefore = mainFile.getResolutionFacadeForTest()
        val fileBefore = mainFile.getOrBuildCfirFile(facadeBefore)
        val targetBefore = assertTargetSource(mainFile, fileBefore, facadeBefore)
        val symbolBefore = following.resolveToCfirSymbol(facadeBefore, CfirResolvePhase.BODY_RESOLVE)
        val declaration = symbolBefore.cfir as CfirNamedFunction
        function.resolveToCfirSymbol(facadeBefore, CfirResolvePhase.BODY_RESOLVE)
        val sourceBefore = requireNotNull(declaration.source)
        val textBefore = sourceBefore.text.toString()
        val startBefore = sourceBefore.startOffset
        val endBefore = sourceBefore.endOffset
        val bodyBefore = requireNotNull(declaration.body)
        val oldBodyLength = requireNotNull(function.bodyExpression).textLength
        assertElementSource(following, declaration, targetBefore)

        val replacement = "{\n    42000\n}"
        replaceExplicitBodies(mainFile, listOf(function to replacement))

        val facadeAfter = mainFile.getResolutionFacadeForTest()
        val fileAfter = mainFile.getOrBuildCfirFile(facadeAfter)
        val targetAfter = assertTargetSource(mainFile, fileAfter, facadeAfter)
        assertSame(fileBefore, fileAfter)
        assertNotSame(targetBefore, targetAfter)
        val symbolAfter = following.resolveToCfirSymbol(facadeAfter, CfirResolvePhase.BODY_RESOLVE)
        assertSame(symbolBefore, symbolAfter)
        assertSame(declaration, symbolAfter.cfir)
        assertSame(bodyBefore, declaration.body)
        val sourceAfter = requireNotNull(declaration.source)
        assertEquals(sourceBefore.kind, sourceAfter.kind)
        assertEquals(textBefore, sourceAfter.text.toString())
        val delta = replacement.length - oldBodyLength
        assertTrue(delta != 0)
        assertEquals(startBefore + delta, sourceAfter.startOffset)
        assertEquals(endBefore + delta, sourceAfter.endOffset)
        assertElementSource(following, declaration, targetAfter)
        assertResolvedIntegers(declaration, listOf("7"))
        assertSubtreeSourcesCurrent(fileAfter, targetAfter)
    }

    /** 未改正文的第二轮 partial 保留旧 BlockEnterNode；换代须迁移只藏在 DFA 中的真实 source。 */
    @Test
    fun retainedFollowingPartialSnapshotSourcesAcrossBodyEdits(mainFile: CjFile) {
        val compute = function(mainFile, "compute")
        val following = function(mainFile, "following")
        val followingPsiBody = requireNotNull(following.bodyExpression)
        val statements = requireNotNull(following.bodyBlockExpression).statements
        assertEquals(5, statements.size)
        val facadeBefore = mainFile.getResolutionFacadeForTest()
        val file = mainFile.getOrBuildCfirFile(facadeBefore)
        var target = assertTargetSource(mainFile, file, facadeBefore)
        val computeSymbol = compute.resolveToCfirSymbol(facadeBefore, CfirResolvePhase.BODY_RESOLVE)
        val followingSymbol = following.resolveToCfirSymbol(facadeBefore, CfirResolvePhase.TYPES)
        val declaration = followingSymbol.cfir as CfirNamedFunction

        val first = requireNotNull(statements[0].getOrBuildCfir(facadeBefore))
        val firstState = requireNotNull(declaration.partialBodyAnalysisState)
        assertEquals(1, firstState.analyzedPsiStatementCount)
        assertEquals(1, firstState.performedAnalysesCount)
        val firstBody = requireNotNull(declaration.body)
        val second = requireNotNull(statements[1].getOrBuildCfir(facadeBefore))
        val secondState = requireNotNull(declaration.partialBodyAnalysisState)
        assertEquals(2, secondState.analyzedPsiStatementCount)
        assertEquals(2, secondState.analyzedCfirStatementCount)
        assertEquals(2, secondState.performedAnalysesCount)
        assertFalse(secondState.isFullyAnalyzed)
        assertNotSame(firstBody, declaration.body)
        val secondSnapshot = requireNotNull(secondState.analysisStateSnapshot)
        val hiddenBlockEnter = snapshotCfgNodes(secondSnapshot).filterIsInstance<BlockEnterNode>()
            .single { it.fir === firstBody }
        assertFalse(hiddenBlockEnter.fir in ordinaryElements(requireNotNull(declaration.body)),
            "必须实际建立只存在于 DFA snapshot 的旧正文入口，不能用当前 body 冒充回归载荷")
        assertSame(first, secondSnapshot.result.statements[0])
        assertSame(second, secondSnapshot.result.statements[1])
        assertSnapshotCfgSourcesCurrent(secondSnapshot, target)

        val edits = listOf(
            "{\n    42000\n}" to listOf("42000"),
            "{\n    8400000\n    8400001\n}" to listOf("8400000", "8400001"),
        )
        for ((editIndex, edit) in edits.withIndex()) {
            val state = requireNotNull(declaration.partialBodyAnalysisState)
            val snapshot = requireNotNull(state.analysisStateSnapshot)
            val prefix = snapshot.result.statements.toList()
            val body = requireNotNull(declaration.body)
            val snapshotNodes = snapshotCfgNodes(snapshot)
            val offsetBefore = requireNotNull(declaration.source).startOffset
            val computeBodyLength = requireNotNull(compute.bodyExpression).textLength

            replaceExplicitBodies(mainFile, listOf(compute to edit.first))

            // 此处尚未查询 following 的任何正文元素；清 state 或全解析均不能满足身份与进度断言。
            assertSame(followingPsiBody, following.bodyExpression, "原 following 正文根没有被平台替换")
            assertSame(body, declaration.body)
            assertSame(state, declaration.partialBodyAnalysisState)
            assertSame(snapshot, requireNotNull(declaration.partialBodyAnalysisState).analysisStateSnapshot)
            assertEquals(5, state.totalPsiStatementCount)
            assertEquals(2 + editIndex, state.analyzedPsiStatementCount)
            assertEquals(2 + editIndex, state.analyzedCfirStatementCount)
            assertEquals(2 + editIndex, state.performedAnalysesCount)
            assertFalse(state.isFullyAnalyzed)
            assertTrue(declaration.resolvePhase < CfirResolvePhase.BODY_RESOLVE)
            prefix.forEachIndexed { index, statement -> assertSame(statement, snapshot.result.statements[index]) }
            val nodesAfterEdit = snapshotCfgNodes(snapshot)
            assertEquals(snapshotNodes.size, nodesAfterEdit.size)
            snapshotNodes.forEach { assertTrue(it in nodesAfterEdit, "换代保留原 DFA 节点身份") }

            val facade = mainFile.getResolutionFacadeForTest()
            assertSame(facadeBefore.useSiteCfirSession, facade.useSiteCfirSession)
            assertSame(file, mainFile.getOrBuildCfirFile(facade))
            val nextTarget = assertTargetSource(mainFile, file, facade)
            assertNotSame(target, nextTarget)
            assertEquals(offsetBefore + edit.first.length - computeBodyLength, requireNotNull(declaration.source).startOffset)
            assertElementSource(following, declaration, nextTarget)
            assertOrdinarySourcesCurrent(body, nextTarget)
            val cachedBlockEnter = nodesAfterEdit.filterIsInstance<BlockEnterNode>().single { it.fir === firstBody }
            assertSame(nextTarget, (requireNotNull(cachedBlockEnter.fir.source) as CjPsiSourceElement).psi.containingFile,
                "保留 snapshot 的旧 BlockEnterNode.fir 不能继续携带上一代 target source")
            // 独立读取私有栈/cache，再按 CFG 前驱/后继 topology 检查实际 source；不调用源迁移 visitor。
            assertSnapshotCfgSourcesCurrent(snapshot, nextTarget)
            assertTrue(nodesAfterEdit.any { it.fir === firstBody }, "旧 BlockEnterNode.fir 必须仍真实存在于保留快照")

            val statementIndex = 2 + editIndex
            val mapped = requireNotNull(statements[statementIndex].getOrBuildCfir(facade)) as CfirLiteralExpression
            assertEquals((13 + editIndex).toString(), mapped.value)
            assertSame(mapped, statements[statementIndex].getOrBuildCfir(facade))
            val resumed = requireNotNull(declaration.partialBodyAnalysisState)
            assertEquals(3 + editIndex, resumed.analyzedPsiStatementCount)
            assertEquals(3 + editIndex, resumed.analyzedCfirStatementCount)
            assertEquals(3 + editIndex, resumed.performedAnalysesCount)
            assertFalse(resumed.isFullyAnalyzed)
            val resumedSnapshot = requireNotNull(resumed.analysisStateSnapshot)
            prefix.forEachIndexed { index, statement -> assertSame(statement, resumedSnapshot.result.statements[index]) }
            assertSnapshotCfgSourcesCurrent(resumedSnapshot, nextTarget)
            assertSame(computeSymbol, compute.resolveToCfirSymbol(facade, CfirResolvePhase.BODY_RESOLVE))
            assertResolvedIntegers(computeSymbol.cfir as CfirNamedFunction, edit.second)
            target = nextTarget
        }

        val facade = mainFile.getResolutionFacadeForTest()
        assertSame(followingSymbol, following.resolveToCfirSymbol(facade, CfirResolvePhase.BODY_RESOLVE))
        val completed = requireNotNull(declaration.partialBodyAnalysisState)
        assertEquals(5, completed.totalPsiStatementCount)
        assertEquals(5, completed.analyzedPsiStatementCount)
        assertEquals(5, completed.analyzedCfirStatementCount)
        assertEquals(5, completed.performedAnalysesCount)
        assertTrue(completed.isFullyAnalyzed)
        assertEquals(null, completed.analysisStateSnapshot)
        assertSame(first, requireNotNull(declaration.body).statements[0])
        assertSame(second, requireNotNull(declaration.body).statements[1])
        assertResolvedIntegers(declaration, listOf("11", "12", "13", "14", "15"))
        assertOrdinarySourcesCurrent(requireNotNull(declaration.body), target)
    }

    /** 局部正文修改属于外层块内失效；新局部函数须绑定自身，外层 return 仍绑定保留的外层声明。 */
    @Test
    fun localFunctionBodyEditRebuildsLocalReturnTargets(mainFile: CjFile) {
        val compute = function(mainFile, "compute")
        val following = function(mainFile, "following")
        val localPsi = requireNotNull(compute.bodyBlockExpression).statements.filterIsInstance<CjNamedFunction>().single()
        val facadeBefore = mainFile.getResolutionFacadeForTest()
        val file = mainFile.getOrBuildCfirFile(facadeBefore)
        val targetBefore = assertTargetSource(mainFile, file, facadeBefore)
        val outerSymbol = compute.resolveToCfirSymbol(facadeBefore, CfirResolvePhase.BODY_RESOLVE)
        val outer = outerSymbol.cfir as CfirNamedFunction
        val originalBody = requireNotNull(outer.body)
        val originalLocal = originalBody.statements.filterIsInstance<CfirNamedFunction>().single()
        val followingSymbol = following.resolveToCfirSymbol(facadeBefore, CfirResolvePhase.BODY_RESOLVE)
        val followingDeclaration = followingSymbol.cfir as CfirNamedFunction
        val followingBody = requireNotNull(followingDeclaration.body)
        assertLocalReturnTargets(outer, originalLocal, "7")

        val service = LLCfirDeclarationModificationService.getInstance(mainFile.project)
        WriteCommandAction.runWriteCommandAction(mainFile.project) {
            val replacement = CjPsiFactory(mainFile.project).createFunction("func replacement(): Int64 { return 42 }")
            val editedBody = requireNotNull(localPsi.bodyExpression).replace(requireNotNull(replacement.bodyExpression))
            val locality = service.detectLocality(editedBody, CaElementModificationType.Unknown)
            assertTrue(locality is CaSourceModificationLocality.InBlock, "局部函数修改必须归属外层正文")
            service.handleInvalidation(editedBody, locality)
            service.flushDeferredModifications()
        }
        assertTrue(outer.body is CfirLazyBlock)
        assertTrue(outer.resolvePhase < CfirResolvePhase.BODY_RESOLVE)
        assertEquals(null, outer.partialBodyAnalysisState)

        val facade = mainFile.getResolutionFacadeForTest()
        assertSame(facadeBefore.useSiteCfirSession, facade.useSiteCfirSession)
        assertSame(file, mainFile.getOrBuildCfirFile(facade))
        val target = assertTargetSource(mainFile, file, facade)
        assertNotSame(targetBefore, target)
        assertSame(followingBody, followingDeclaration.body)
        assertSame(outerSymbol, compute.resolveToCfirSymbol(facade, CfirResolvePhase.BODY_RESOLVE))
        assertSame(outer, outerSymbol.cfir)
        val currentBody = requireNotNull(outer.body)
        val currentLocal = currentBody.statements.filterIsInstance<CfirNamedFunction>().single()
        assertNotSame(originalBody, currentBody)
        assertNotSame(originalLocal, currentLocal)
        assertNotSame(originalLocal.symbol, currentLocal.symbol, "丢弃正文内的局部 symbol 必须重新创建")
        assertSame(currentLocal, currentLocal.symbol.cfir)
        assertLocalReturnTargets(outer, currentLocal, "42")
        assertSame(followingSymbol, following.resolveToCfirSymbol(facade, CfirResolvePhase.BODY_RESOLVE))
        assertSame(followingBody, followingDeclaration.body)
        assertOrdinarySourcesCurrent(currentBody, target)
        assertOrdinarySourcesCurrent(followingDeclaration, target)
    }

    private fun assertLocalReturnTargets(outer: CfirNamedFunction, local: CfirNamedFunction, expected: String) {
        val localReturn = requireNotNull(local.body).statements.filterIsInstance<CfirReturnExpression>().single()
        val outerReturn = requireNotNull(outer.body).statements.filterIsInstance<CfirReturnExpression>().single()
        assertSame(local, localReturn.target.labeledElement)
        assertSame(outer, outerReturn.target.labeledElement)
        assertEquals(expected, (localReturn.result as CfirLiteralExpression).value)
        val call = outerReturn.result as CfirFunctionCall
        assertSame(local.symbol, (call.calleeReference as CfirResolvedNamedReference).resolvedSymbol,
            "外层实际调用必须解析到当前局部 symbol")
        assertEquals(ConePrimitiveType.INT64, local.returnTypeRef.coneTypeOrNull)
        assertEquals(ConePrimitiveType.INT64, outer.returnTypeRef.coneTypeOrNull)
    }

    /** 从文件容器先请求，必须经当前 class/member 重建实际传递 CFG 依赖。 */
    @Test
    fun containerFirstRebuildsTransitiveCfg(mainFile: CjFile) {
        assertTransitiveContainerCfgReplacement(mainFile, memberFirst = false)
    }

    /** 先请求修改成员，随后请求 class/file；容器不能因旧 BODY_RESOLVE phase 跳过重建。 */
    @Test
    fun memberFirstRebuildsTransitiveCfg(mainFile: CjFile) {
        assertTransitiveContainerCfgReplacement(mainFile, memberFirst = true)
    }

    /** 命名默认值是参数自己的正文：file 先请求时保留真实参数图及全部未改成员图。 */
    @Test
    fun containerFirstRetainsNamedDefaultParameterGraph(mainFile: CjFile) {
        assertTransitiveContainerCfgReplacement(mainFile, memberFirst = false, expectNamedDefault = true)
    }

    /** 成员先请求时只重建修改函数正文图，不能重新认领参数默认值图。 */
    @Test
    fun memberFirstRetainsNamedDefaultParameterGraph(mainFile: CjFile) {
        assertTransitiveContainerCfgReplacement(mainFile, memberFirst = true, expectNamedDefault = true)
    }

    private fun assertTransitiveContainerCfgReplacement(
        mainFile: CjFile,
        memberFirst: Boolean,
        expectNamedDefault: Boolean = false,
    ) {
        val klassPsi = mainFile.declarations.filterIsInstance<CjClass>().single()
        val compute = klassPsi.declarations.filterIsInstance<CjNamedFunction>().single { it.name == "compute" }
        val stable = klassPsi.declarations.filterIsInstance<CjNamedFunction>().single { it.name == "stable" }
        val following = function(mainFile, "following")
        val facadeBefore = mainFile.getResolutionFacadeForTest()
        val file = mainFile.getOrBuildCfirFile(facadeBefore)
        val targetBefore = assertTargetSource(mainFile, file, facadeBefore)
        file.lazyResolveToPhaseRecursively(CfirResolvePhase.BODY_RESOLVE)
        val klass = file.declarations.filterIsInstance<CfirClass>().single()
        val computeSymbol = compute.resolveToCfirSymbol(facadeBefore, CfirResolvePhase.BODY_RESOLVE)
        val declaration = computeSymbol.cfir as CfirNamedFunction
        val bodyBefore = requireNotNull(declaration.body)
        val stableSymbol = stable.resolveToCfirSymbol(facadeBefore, CfirResolvePhase.BODY_RESOLVE)
        val stableDeclaration = stableSymbol.cfir as CfirNamedFunction
        val stableBody = requireNotNull(stableDeclaration.body)
        val stableGraph = graphOf(stableDeclaration)
        val followingSymbol = following.resolveToCfirSymbol(facadeBefore, CfirResolvePhase.BODY_RESOLVE)
        val followingDeclaration = followingSymbol.cfir as CfirNamedFunction
        val followingBody = requireNotNull(followingDeclaration.body)
        val followingGraph = graphOf(followingDeclaration)
        val fileGraphBefore = graphOf(file)
        val classGraphBefore = graphOf(klass)
        val memberGraphBefore = graphOf(declaration)
        val parameterBoundariesBefore = memberGraphBefore.nodes
            .filter { it is EnterValueParameterNode || it is ExitValueParameterNode }.map { it::class }
        if (expectNamedDefault) {
            assertEquals(listOf(EnterValueParameterNode::class, ExitValueParameterNode::class), parameterBoundariesBefore)
        }
        val parametersBefore = declaration.valueParameters.toList()
        val parameterSymbolsBefore = parametersBefore.map { it.symbol }
        val parameterTypesBefore = parametersBefore.map { it.returnTypeRef }
        val returnTypeBefore = declaration.returnTypeRef
        val defaultsBefore = parametersBefore.filter { it.defaultValue != null }.map { parameter ->
            val reference = requireNotNull(parameter.controlFlowGraphReference)
            val graph = requireNotNull(reference.controlFlowGraph)
            assertEquals(ControlFlowGraph.Kind.DefaultArgument, graph.kind)
            assertSame(parameter, graph.declaration)
            assertNotSame(memberGraphBefore, graph, "参数默认图必须独立于修改函数的正文图")
            RetainedDefaultParameter(parameter, requireNotNull(parameter.defaultValue), reference, graph)
        }
        if (expectNamedDefault) {
            assertEquals(1, defaultsBefore.size, "命名默认参数回归必须预先得到一个真实参数 CFG")
            assertEquals("value", defaultsBefore.single().parameter.name.asString())
            assertEquals("1", (defaultsBefore.single().defaultValue as CfirLiteralExpression).value)
        } else {
            assertTrue(defaultsBefore.isEmpty(), "原容器回归的 fixture 没有默认参数")
        }
        val defaultStates = captureRetainedGraphStates(defaultsBefore.map { it.graph })
        val defaultGraphs = defaultStates.mapTo(identitySet<ControlFlowGraph>()) { it.graph }
        val retainedStates = captureRetainedGraphStates(listOf(stableGraph, followingGraph)) + defaultStates
        val retainedNodes = retainedStates.flatMap { it.states }
        // 记录真实覆盖数量；未初始化的 flow 或不存在的跨图边不计为对象保留/退休执行证据。
        println("CFG retention coverage: graphs=${retainedStates.size}, nodes=${retainedNodes.size}, " +
            "initializedMainFlows=${retainedNodes.count { it.flowInitialized }}, " +
            "alternateFlows=${retainedNodes.sumOf { it.alternateFlows.size }}, " +
            "crossGraphAdjacencies=${retainedNodes.sumOf { state ->
                state.node.previousNodes.count { it.owner !== state.node.owner } +
                    state.node.followingNodes.count { it.owner !== state.node.owner }
            }}")
        assertEquals(CfirResolvePhase.BODY_RESOLVE, file.resolvePhase)
        assertEquals(CfirResolvePhase.BODY_RESOLVE, klass.resolvePhase)
        assertTrue(classGraphBefore in graphClosure(fileGraphBefore), "必须预先建立实际 file→class CFG")
        assertTrue(memberGraphBefore in graphClosure(classGraphBefore), "必须预先建立实际 class→member CFG")
        val discarded = identitySet<ControlFlowGraph>().apply {
            // 默认参数的独立正文不属于本次退休集合，不能把函数整棵子图无条件判为退休。
            addAll(graphClosure(memberGraphBefore).filter { it !in defaultGraphs })
            add(classGraphBefore)
            add(fileGraphBefore)
        }
        assertTrue(defaultGraphs.none { it in discarded })
        assertTrue(retainedStates.none { it.graph in discarded })

        fun assertRetainedHeaderAndGraphs(stage: String) {
            assertSame(returnTypeBefore, declaration.returnTypeRef, "$stage：显式返回类型的 header 保留")
            assertSameElements(parametersBefore, declaration.valueParameters, "$stage：参数 header")
            parametersBefore.forEachIndexed { index, parameter ->
                assertSame(parameterSymbolsBefore[index], parameter.symbol, "$stage：参数 symbol")
                assertSame(parameterTypesBefore[index], parameter.returnTypeRef, "$stage：参数类型 header")
            }
            defaultsBefore.forEach { state ->
                assertSame(state.defaultValue, state.parameter.defaultValue, "$stage：默认值正文")
                assertSame(state.reference, state.parameter.controlFlowGraphReference, "$stage：参数 CFG reference")
                assertSame(state.graph, graphOf(state.parameter), "$stage：实际参数默认图")
            }
            assertRetainedGraphStates(retainedStates, discarded, stage)
        }

        replaceExplicitBodies(mainFile, listOf(compute to "{\n    42000\n}"))
        assertRetainedHeaderAndGraphs("失效后、重建前")

        assertTrue(declaration.body is CfirLazyBlock)
        assertEquals(null, declaration.controlFlowGraphReference)
        assertEquals(null, klass.controlFlowGraphReference, "修改成员使实际依赖的 class 图失效")
        assertEquals(null, file.controlFlowGraphReference, "file 经 class 传递依赖旧成员图")
        assertTrue(klass.resolvePhase < CfirResolvePhase.BODY_RESOLVE)
        assertTrue(file.resolvePhase < CfirResolvePhase.BODY_RESOLVE)
        assertSame(stableBody, stableDeclaration.body)
        assertSame(stableGraph, graphOf(stableDeclaration))
        assertSame(followingBody, followingDeclaration.body)
        assertSame(followingGraph, graphOf(followingDeclaration))
        val facade = mainFile.getResolutionFacadeForTest()
        assertSame(facadeBefore.useSiteCfirSession, facade.useSiteCfirSession)
        assertSame(file, mainFile.getOrBuildCfirFile(facade))
        val target = assertTargetSource(mainFile, file, facade)
        assertNotSame(targetBefore, target)

        if (memberFirst) {
            assertSame(computeSymbol, compute.resolveToCfirSymbol(facade, CfirResolvePhase.BODY_RESOLVE))
            assertEquals(null, klass.controlFlowGraphReference)
            assertEquals(null, file.controlFlowGraphReference)
            klass.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
            assertRetainedHeaderAndGraphs("修改成员与 class 重建后")
            val classGraph = graphOf(klass)
            assertTrue(graphOf(declaration) in graphClosure(classGraph))
            assertTrue(graphClosure(classGraph).none { it in discarded })
            file.lazyResolveToPhaseRecursively(CfirResolvePhase.BODY_RESOLVE)
        } else {
            // 首次 BODY_RESOLVE 请求明确来自 file 容器，不能预先请求成员掩盖依赖失效缺口。
            file.lazyResolveToPhaseRecursively(CfirResolvePhase.BODY_RESOLVE)
        }

        assertSame(computeSymbol, compute.resolveToCfirSymbol(facade, CfirResolvePhase.BODY_RESOLVE))
        assertSame(declaration, computeSymbol.cfir)
        assertNotSame(bodyBefore, declaration.body)
        assertResolvedIntegers(declaration, listOf("42000"))
        assertEquals(CfirResolvePhase.BODY_RESOLVE, file.resolvePhase)
        assertEquals(CfirResolvePhase.BODY_RESOLVE, klass.resolvePhase)
        val fileGraph = graphOf(file)
        val classGraph = graphOf(klass)
        val memberGraph = graphOf(declaration)
        assertEquals(parameterBoundariesBefore, memberGraph.nodes
            .filter { it is EnterValueParameterNode || it is ExitValueParameterNode }.map { it::class },
            "保留默认图仍须为新函数图建立参数入口和出口")
        assertNotSame(fileGraphBefore, fileGraph)
        assertNotSame(classGraphBefore, classGraph)
        assertNotSame(memberGraphBefore, memberGraph)
        val retainedGraphs = graphClosure(fileGraph)
        assertTrue(classGraph in retainedGraphs)
        assertTrue(memberGraph in graphClosure(classGraph))
        assertTrue(retainedGraphs.none { it in discarded }, "当前完整 CFG 不得保留实际丢弃 graph identity")
        assertSame(stableSymbol, stable.resolveToCfirSymbol(facade, CfirResolvePhase.BODY_RESOLVE))
        assertSame(stableBody, stableDeclaration.body)
        assertSame(stableGraph, graphOf(stableDeclaration))
        assertSame(followingSymbol, following.resolveToCfirSymbol(facade, CfirResolvePhase.BODY_RESOLVE))
        assertSame(followingBody, followingDeclaration.body)
        assertSame(followingGraph, graphOf(followingDeclaration))
        assertTrue(stableGraph in retainedGraphs)
        assertTrue(followingGraph in retainedGraphs)
        defaultsBefore.forEach { assertTrue(it.graph in retainedGraphs, "新函数图仍引用保留的实际参数默认图") }
        assertRetainedHeaderAndGraphs("file/class/member 全部重建后")
        defaultsBefore.forEach { assertOrdinarySourcesCurrent(it.parameter, target) }
        collectCfgNodes(listOf(fileGraph)).forEach { assertOrdinarySourcesCurrent(it.fir, target) }

        // 重复容器请求不得再次替换已完成的当前图，两个外部请求顺序都收敛到同一代。
        file.lazyResolveToPhaseRecursively(CfirResolvePhase.BODY_RESOLVE)
        assertSame(fileGraph, graphOf(file))
        assertSame(classGraph, graphOf(klass))
        assertSame(memberGraph, graphOf(declaration))
        assertRetainedHeaderAndGraphs("重复容器请求后")
    }

    private data class RetainedDefaultParameter(
        val parameter: CfirValueParameter,
        val defaultValue: CfirExpression,
        val reference: CfirControlFlowGraphReference,
        val graph: ControlFlowGraph,
    )

    private data class RetainedCfgNodeState(
        val node: CFGNode<*>,
        val fir: CfirElement,
        val isDead: Boolean,
        val internalPrevious: List<CFGNode<*>>,
        val internalFollowing: List<CFGNode<*>>,
        val incomingEdges: List<Edge>,
        val outgoingEdges: List<Edge>,
        val flowInitialized: Boolean,
        val flow: PersistentFlow?,
        val alternateFlows: Map<FlowPath, PersistentFlow>,
    )

    private data class RetainedCfgGraphState(
        val graph: ControlFlowGraph,
        val nodeCount: Int,
        val nodes: List<CFGNode<*>>,
        val states: List<RetainedCfgNodeState>,
    )

    /** 只读已经完成的真实图及节点，避免双向拓扑遍历把待退休父图收入保留快照。 */
    private fun captureRetainedGraphStates(roots: List<ControlFlowGraph>): List<RetainedCfgGraphState> {
        val graphs = identitySet<ControlFlowGraph>()
        roots.forEach { graphs.addAll(graphClosure(it)) }
        return graphs.map { graph ->
            val nodes = graph.nodes.toList()
            assertTrue(nodes.isNotEmpty(), "保留图必须有真实节点，不能以空集合验证退休边")
            assertTrue(graph.enterNode in nodes && graph.exitNode in nodes)
            val states = nodes.map { node ->
                assertSame(graph, node.owner)
                val previous = node.previousNodes.filter { it.owner === graph }
                val following = node.followingNodes.filter { it.owner === graph }
                RetainedCfgNodeState(
                    node, node.fir, node.isDead, previous, following,
                    previous.map(node::edgeFrom), following.map(node::edgeTo),
                    node.flowInitialized,
                    if (node.flowInitialized) node.flow else null,
                    node.alternateFlowPaths.associateWith { requireNotNull(node.getAlternateFlow(it)) },
                )
            }
            RetainedCfgGraphState(graph, graph.nodeCount, nodes, states)
        }
    }

    /** 内部边与实际 flow 保留；跨图边只检查退休 identity，并允许新父图连接到旧成员。 */
    private fun assertRetainedGraphStates(
        snapshots: List<RetainedCfgGraphState>,
        retired: Set<ControlFlowGraph>,
        stage: String,
    ) {
        assertTrue(snapshots.isNotEmpty(), "$stage：必须检查真实保留图")
        snapshots.forEach { snapshot ->
            val graph = snapshot.graph
            assertFalse(graph in retired, "$stage：保留图不能属于退休集合")
            assertEquals(snapshot.nodeCount, graph.nodeCount, "$stage：保留图节点数量")
            assertSameElements(snapshot.nodes, graph.nodes, "$stage：保留图节点")
            snapshot.states.forEach { state ->
                val node = state.node
                assertSame(graph, node.owner, "$stage：节点 owner")
                assertSame(state.fir, node.fir, "$stage：节点 CFIR 载荷")
                assertEquals(state.isDead, node.isDead, "$stage：节点可达性")
                val previous = node.previousNodes.filter { it.owner === graph }
                val following = node.followingNodes.filter { it.owner === graph }
                assertSameElements(state.internalPrevious, previous, "$stage：内部前驱")
                assertSameElements(state.internalFollowing, following, "$stage：内部后继")
                assertEquals(state.incomingEdges, previous.map(node::edgeFrom), "$stage：内部入边属性")
                assertEquals(state.outgoingEdges, following.map(node::edgeTo), "$stage：内部出边属性")
                node.previousNodes.forEach { predecessor ->
                    assertFalse(predecessor.owner in retired, "$stage：前驱 owner 不得是退休 file/class/member 图")
                    assertEquals(
                        node.previousNodes.count { it === predecessor },
                        predecessor.followingNodes.count { it === node },
                        "$stage：前驱边的两端必须对称",
                    )
                }
                node.followingNodes.forEach { successor ->
                    assertFalse(successor.owner in retired, "$stage：后继 owner 不得是退休 file/class/member 图")
                    assertEquals(
                        node.followingNodes.count { it === successor },
                        successor.previousNodes.count { it === node },
                        "$stage：后继边的两端必须对称",
                    )
                }
                assertEquals(state.flowInitialized, node.flowInitialized, "$stage：flow 初始化状态")
                // 当前未生成 PersistentFlow 的节点不能强读 flow，也不能以 null/null 冒充对象身份证明。
                if (state.flowInitialized) {
                    assertSame(requireNotNull(state.flow), node.flow, "$stage：已有主 flow 对象")
                }
                assertEquals(state.alternateFlows.keys, node.alternateFlowPaths, "$stage：备用 flow 路径")
                state.alternateFlows.forEach { (path, flow) ->
                    assertSame(flow, requireNotNull(node.getAlternateFlow(path)), "$stage：已有备用 flow 对象")
                }
            }
        }
    }

    /** 按位置比较实际对象身份，保留重复边计数和节点顺序。 */
    private fun <T : Any> assertSameElements(expected: List<T>, actual: List<T>, message: String) {
        assertEquals(expected.size, actual.size, message)
        expected.forEachIndexed { index, element -> assertSame(element, actual[index], "$message：$index") }
    }

    private fun graphOf(owner: CfirControlFlowGraphOwner): ControlFlowGraph =
        requireNotNull(requireNotNull(owner.controlFlowGraphReference).controlFlowGraph)

    /** 完整图仅按实际 subGraphs identity 求闭包；不使用语法父子关系或 isSubGraph 猜测依赖。 */
    private fun graphClosure(root: ControlFlowGraph): Set<ControlFlowGraph> {
        val graphs = identitySet<ControlFlowGraph>()
        val pending = java.util.ArrayDeque<ControlFlowGraph>().apply { add(root) }
        while (pending.isNotEmpty()) {
            val graph = pending.removeFirst()
            if (graphs.add(graph)) graph.subGraphs.forEach(pending::addLast)
        }
        return graphs
    }

    /** 纯空白改变不影响值与签名，但不能继续使用旧整文件源、行映射或后继诊断。 */
    @Test
    fun whitespaceEditRefreshesSourcesAndDiagnostics(mainFile: CjFile) {
        assertTriviaEdit(mainFile, "{\n\n    1\n}")
    }

    /** 注释无语义副作用，修改注释仍必须刷新目标产品与诊断位置。 */
    @Test
    fun commentEditRefreshesSourcesAndDiagnostics(mainFile: CjFile) {
        assertTriviaEdit(mainFile, "{ /* current body generation */ 1 }")
    }

    /** cjc1.1.3接受Int8正文中的非尾1000和true；prefix不能继承返回值的窄整数期望类型。 */
    @Test
    fun typedPartialPrefixDoesNotInheritTailType(mainFile: CjFile) {
        val function = function(mainFile, "compute")
        val facade = mainFile.getResolutionFacadeForTest()
        val symbol = function.resolveToCfirSymbol(facade, CfirResolvePhase.TYPES)
        val declaration = symbol.cfir as CfirNamedFunction
        val first = requireNotNull(function.bodyBlockExpression).statements.first()
        val prefix = requireNotNull(first.getOrBuildCfir(facade)) as CfirLiteralExpression
        assertEquals("1000", prefix.value)
        assertEquals(ConePrimitiveType.INT64, prefix.coneTypeOrNull)
        assertEquals(ConePrimitiveType.INT8, declaration.returnTypeRef.coneTypeOrNull)
        assertEquals(1, requireNotNull(declaration.partialBodyAnalysisState).analyzedPsiStatementCount)
        val originalBody = declaration.body
        replaceExplicitBodies(mainFile, listOf(function to "{\n10000\ntrue\n7\n}"))
        val after = mainFile.getResolutionFacadeForTest()
        assertSame(symbol, function.resolveToCfirSymbol(after, CfirResolvePhase.TYPES))
        val newFirst = requireNotNull(function.bodyBlockExpression).statements.first()
        val newPrefix = requireNotNull(newFirst.getOrBuildCfir(after)) as CfirLiteralExpression
        assertEquals("10000", newPrefix.value)
        assertEquals(ConePrimitiveType.INT64, newPrefix.coneTypeOrNull)
        assertNotSame(prefix, newPrefix)
        assertNotSame(originalBody, declaration.body)
        assertSame(symbol, function.resolveToCfirSymbol(after, CfirResolvePhase.BODY_RESOLVE))
        assertEquals(ConePrimitiveType.INT8, declaration.returnTypeRef.coneTypeOrNull)
        assertEquals(emptyList<CjPsiDiagnostic>(), errors(mainFile, after))
    }

    /** 已解析header的诊断载荷也保留源；重新收集不能继续读取其旧target范围。 */
    @Test
    fun typeAmbiguitySourceMovesWithFollowingHeader(mainFile: CjFile) {
        val compute = function(mainFile, "compute")
        val ambiguous = function(mainFile, "ambiguous")
        val before = mainFile.getResolutionFacadeForTest()
        val computeSymbol = compute.resolveToCfirSymbol(before, CfirResolvePhase.BODY_RESOLVE)
        val symbol = ambiguous.resolveToCfirSymbol(before, CfirResolvePhase.TYPES)
        val declaration = symbol.cfir as CfirNamedFunction
        val type = declaration.valueParameters.single().returnTypeRef as CfirErrorTypeRef
        val diagnostic = type.diagnostic as ConeAmbiguityError
        val sourceBefore = requireNotNull(diagnostic.typeUseSource) as CjPsiSourceElement
        val targetBefore = sourceBefore.psi.containingFile
        val offsetBefore = sourceBefore.startOffset
        val bodyLength = requireNotNull(compute.bodyExpression).textLength
        val replacement = "{\n    42\n}"
        replaceExplicitBodies(mainFile, listOf(compute to replacement))
        val after = mainFile.getResolutionFacadeForTest()
        assertSame(computeSymbol, compute.resolveToCfirSymbol(after, CfirResolvePhase.BODY_RESOLVE))
        assertSame(symbol, ambiguous.resolveToCfirSymbol(after, CfirResolvePhase.TYPES))
        assertSame(type, declaration.valueParameters.single().returnTypeRef)
        assertSame(diagnostic, type.diagnostic)
        val sourceAfter = requireNotNull(diagnostic.typeUseSource) as CjPsiSourceElement
        val targetAfter = (requireNotNull(mainFile.getOrBuildCfirFile(after).source) as CjPsiSourceElement).psi
        assertNotSame(targetBefore, targetAfter)
        assertSame(targetAfter, sourceAfter.psi.containingFile)
        assertEquals(offsetBefore + replacement.length - bodyLength, sourceAfter.startOffset)
        assertEquals("Repeated", sourceAfter.text.toString())
    }

    private fun assertBodyReplacement(
        mainFile: CjFile,
        replacementBodyText: String,
        expectedTypeBefore: ConePrimitiveType,
        expectedTypeAfter: ConePrimitiveType,
        inBlock: Boolean,
    ) {
        val functions = mainFile.declarations.filterIsInstance<CjNamedFunction>()
        val function = functions.single { it.name == "compute" }
        val caller = functions.single { it.name == "use" }
        val facadeBefore = mainFile.getResolutionFacadeForTest()
        val fileBefore = mainFile.getOrBuildCfirFile(facadeBefore)
        val targetBefore = assertTargetSource(mainFile, fileBefore, facadeBefore)
        val symbolBefore = function.resolveToCfirSymbol(facadeBefore, CfirResolvePhase.BODY_RESOLVE)
        val callerBefore = caller.resolveToCfirSymbol(facadeBefore, CfirResolvePhase.BODY_RESOLVE)
        val declarationBefore = symbolBefore.cfir as CfirNamedFunction
        val callerDeclarationBefore = callerBefore.cfir as CfirNamedFunction
        assertElementSource(function, declarationBefore, targetBefore)
        assertElementSource(caller, callerDeclarationBefore, targetBefore)
        val bodyBefore = requireNotNull(declarationBefore.body)
        assertFalse(bodyBefore is CfirLazyBlock)
        assertEquals(CfirResolvePhase.BODY_RESOLVE, declarationBefore.resolvePhase)
        assertEquals(expectedTypeBefore, declarationBefore.returnTypeRef.coneTypeOrNull)
        assertEquals(expectedTypeBefore, callerDeclarationBefore.returnTypeRef.coneTypeOrNull)

        val modificationService = LLCfirDeclarationModificationService.getInstance(mainFile.project)
        WriteCommandAction.runWriteCommandAction(mainFile.project) {
            // 用函数解析入口构造块体，避免把 {} 错当成 lambda 表达式。
            val replacement = CjPsiFactory(mainFile.project).createFunction("func replacement() $replacementBodyText")
            val newBody = requireNotNull(function.bodyExpression).replace(requireNotNull(replacement.bodyExpression))
            val locality = modificationService.detectLocality(newBody, CaElementModificationType.Unknown)
            if (inBlock) {
                assertTrue(locality is CaSourceModificationLocality.InBlock, "Explicit return type must keep the change local")
            } else {
                assertTrue(locality is CaSourceModificationLocality.OutOfBlock, "An inferred return type is part of the changed signature")
            }
            modificationService.handleInvalidation(newBody, locality)
            modificationService.flushDeferredModifications()
        }

        if (inBlock) {
            assertTrue(declarationBefore.body is CfirLazyBlock, "Only the body must be invalidated")
            assertTrue(declarationBefore.resolvePhase < CfirResolvePhase.BODY_RESOLVE)
        }

        // 块外事件使旧会话失效，重新从生产入口获取 facade，而不是继续使用修改前的入口。
        val facadeAfter = mainFile.getResolutionFacadeForTest()
        val fileAfter = mainFile.getOrBuildCfirFile(facadeAfter)
        val targetAfter = assertTargetSource(mainFile, fileAfter, facadeAfter)
        assertNotSame(targetBefore, targetAfter, "修改后必须发布新的独立 target PSI")
        val symbolAfter = function.resolveToCfirSymbol(facadeAfter, CfirResolvePhase.BODY_RESOLVE)
        val callerAfter = caller.resolveToCfirSymbol(facadeAfter, CfirResolvePhase.BODY_RESOLVE)
        val declarationAfter = symbolAfter.cfir as CfirNamedFunction
        val callerDeclarationAfter = callerAfter.cfir as CfirNamedFunction
        if (inBlock) {
            assertSame(fileBefore, fileAfter)
            assertSame(facadeBefore.useSiteCfirSession, facadeAfter.useSiteCfirSession)
            assertSame(declarationBefore, declarationAfter)
            assertSame(callerDeclarationBefore, callerDeclarationAfter)
            assertSame(symbolBefore, symbolAfter, "A stable signature must retain its declaration symbol")
            assertSame(callerBefore, callerAfter, "The caller is unaffected by an in-block change")
        } else {
            assertNotSame(fileBefore, fileAfter)
            assertNotSame(facadeBefore.useSiteCfirSession, facadeAfter.useSiteCfirSession)
            assertNotSame(declarationBefore, declarationAfter)
            assertNotSame(callerDeclarationBefore, callerDeclarationAfter)
            assertNotSame(symbolBefore, symbolAfter, "The inferred declaration signature must be rebuilt")
            assertNotSame(callerBefore, callerAfter, "The caller's inferred signature must also be invalidated")
        }
        val bodyAfter = requireNotNull(declarationAfter.body)
        assertNotSame(bodyBefore, bodyAfter)
        assertFalse(bodyAfter is CfirLazyBlock)
        assertEquals(CfirResolvePhase.BODY_RESOLVE, declarationAfter.resolvePhase)
        assertEquals(expectedTypeAfter, declarationAfter.returnTypeRef.coneTypeOrNull)
        assertEquals(expectedTypeAfter, callerDeclarationAfter.returnTypeRef.coneTypeOrNull)
        assertElementSource(function, declarationAfter, targetAfter)
        assertElementSource(caller, callerDeclarationAfter, targetAfter)
        assertSubtreeSourcesCurrent(fileAfter, targetAfter)
        // 相同返回类型不能证明采用了新正文；目标PSI/缓存接入后也必须实际解析修改后的42。
        val integerLiterals = mutableListOf<String>()
        bodyAfter.accept(object : CfirDefaultVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                element.acceptChildren(this)
            }

            override fun visitLiteralExpression(literalExpression: CfirLiteralExpression) {
                if (literalExpression.kind == CfirLiteralKind.INT) {
                    // CFIR整数literal保留源拼写，语义阶段通过coneType确定数值类型。
                    integerLiterals += literalExpression.value as String
                }
            }
        })
        assertEquals(if (replacementBodyText == "{}") emptyList() else listOf("42"), integerLiterals)
    }

    private fun function(mainFile: CjFile, name: String): CjNamedFunction =
        mainFile.declarations.filterIsInstance<CjNamedFunction>().single { it.name == name }

    /** 只走既有 PSI 修改服务；同一 write 的所有事件入队后只 flush 一次。 */
    private fun replaceExplicitBodies(mainFile: CjFile, replacements: List<Pair<CjNamedFunction, String>>) {
        val service = LLCfirDeclarationModificationService.getInstance(mainFile.project)
        WriteCommandAction.runWriteCommandAction(mainFile.project) {
            for ((function, bodyText) in replacements) {
                val replacement = CjPsiFactory(mainFile.project).createFunction("func replacement() $bodyText")
                val body = requireNotNull(function.bodyExpression).replace(requireNotNull(replacement.bodyExpression))
                val locality = service.detectLocality(body, CaElementModificationType.Unknown)
                assertTrue(locality is CaSourceModificationLocality.InBlock)
                service.handleInvalidation(body, locality)
            }
            service.flushDeferredModifications()
        }
    }

    /** 原树只作为来源与编辑入口，CFIR 文件及双向 cache 必须归属真实独立目标产品。 */
    private fun assertTargetSource(mainFile: CjFile, file: CfirFile, facade: LLResolutionFacade): CjFile {
        val source = requireNotNull(file.source) as CjPsiSourceElement
        val target = source.psi as CjFile
        assertNotSame(mainFile, target, "初次 raw CFIR 也必须读取独立 target PSI")
        val product = requireNotNull(target.compilationParsingResultOrNull())
        assertSame(mainFile, product.originFile)
        assertSame(target, product.file)
        product.requireConsistentInput()
        assertEquals(mainFile.text, target.text)
        assertEquals(mainFile.text, source.text.toString())
        assertEquals(mainFile.text, requireNotNull(product.actualInputSeed.sourceContext).snapshot.text)
        assertTrue(product.diagnostics.isEmpty(), "基础语法 fixture 不应产生解析诊断")
        assertSame(file, mainFile.getOrBuildCfirFile(facade), "来源文件反查必须命中同一 CFIR")
        assertSame(file, target.getOrBuildCfirFile(facade), "目标文件反查必须命中同一 CFIR")
        assertSame(product.sourceFile, file.sourceFile, "源码身份和内容必须来自同一完整编译产品")
        assertSame(product.sourceFileLinesMapping, file.sourceFileLinesMapping, "初建与换代都使用产品的冻结行列映射")
        assertEquals(mainFile.textLength, requireNotNull(file.sourceFileLinesMapping).lastOffset)
        assertEquals(mainFile.name, requireNotNull(file.sourceFile).name)
        assertEquals(mainFile.virtualFile?.path, requireNotNull(file.sourceFile).path)
        assertEquals(product.actualInputSeed.sourceKind, requireNotNull(file.sourceFile).sourceKind)
        val displayedText = requireNotNull(file.sourceFile).getContentsAsStream().bufferedReader(Charsets.UTF_8).use {
            it.readText()
        }
        assertEquals(mainFile.text, displayedText, "源展示不能继续读取修改前整文件")
        return target
    }

    /** 对象对应与当前偏移同时成立；名字相同或源码路径相同不能证明源已迁移。 */
    private fun assertElementSource(origin: CjElement, element: CfirElement, target: CjFile) {
        val source = requireNotNull(element.source) as CjPsiSourceElement
        val product = requireNotNull(target.compilationParsingResultOrNull())
        assertSame(target, source.psi.containingFile)
        assertNotSame(origin, source.psi)
        assertSame(source.psi, product.correspondingElementOrNull(origin))
        assertSame(origin, product.originalElementOrNull(source.psi))
        assertEquals(origin.text, source.text.toString())
        assertEquals(origin.textRange.startOffset, source.startOffset)
        assertEquals(origin.textRange.endOffset, source.endOffset)
    }

    /** 保留的 caller、following 与类型引用等子图也不能继续携带上一代 target 的 source。 */
    private fun assertSubtreeSourcesCurrent(root: CfirElement, target: CjFile) {
        val visited = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<CfirElement, Boolean>())
        root.accept(object : CfirDefaultVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (!visited.add(element)) return
                val source = element.source
                if (source is CjPsiSourceElement) {
                    assertSame(target, source.psi.containingFile, "${element::class.simpleName} 的 source 必须同代")
                }
                element.acceptSourceChildren(this)
            }

            // 未请求解析的 lazy 块没有 source 读取协议；检查保留声明与已有子图的来源。
            override fun visitLazyBlock(lazyBlock: CfirLazyBlock) = Unit
        })
    }

    private fun assertResolvedIntegers(function: CfirNamedFunction, expected: List<String>) {
        assertEquals(CfirResolvePhase.BODY_RESOLVE, function.resolvePhase)
        assertEquals(ConePrimitiveType.INT64, function.returnTypeRef.coneTypeOrNull)
        val body = requireNotNull(function.body)
        assertFalse(body is CfirLazyBlock)
        val actual = mutableListOf<String>()
        body.accept(object : CfirDefaultVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                element.acceptChildren(this)
            }

            override fun visitLiteralExpression(literalExpression: CfirLiteralExpression) {
                if (literalExpression.kind == CfirLiteralKind.INT) {
                    actual += literalExpression.value as String
                }
            }
        })
        assertEquals(expected, actual, "必须采用当前正文，不能只靠返回类型或 body 对象变化证明")
    }

    /** 用公开元素查询触发 partial mapper，并检查实际 partial state，避免预先整函数解析。 */
    private fun assertPartialFirstStatement(
        function: CjNamedFunction,
        declaration: CfirNamedFunction,
        facade: LLResolutionFacade,
        target: CjFile,
        expected: String,
    ): CfirElement {
        val statements = requireNotNull(function.bodyBlockExpression).statements
        assertEquals(3, statements.size)
        val first = statements.first()
        val mapped = requireNotNull(first.getOrBuildCfir(facade)) as CfirLiteralExpression
        assertEquals(CfirLiteralKind.INT, mapped.kind)
        assertEquals(expected, mapped.value)
        assertElementSource(first, mapped, target)
        val state = requireNotNull(declaration.partialBodyAnalysisState)
        assertEquals(3, state.totalPsiStatementCount)
        assertEquals(1, state.analyzedPsiStatementCount)
        assertFalse(state.isFullyAnalyzed)
        assertTrue(declaration.resolvePhase < CfirResolvePhase.BODY_RESOLVE)
        assertSame(mapped, first.getOrBuildCfir(facade), "同代重复查询保持 mapper 结果身份")
        return mapped
    }

    /** 保留一条后继未声明名称错误，使无语义变化编辑的诊断刷新可由新坐标区分。 */
    private fun assertTriviaEdit(mainFile: CjFile, replacement: String) {
        val function = function(mainFile, "compute")
        val caller = function(mainFile, "use")
        val following = function(mainFile, "following")
        val facadeBefore = mainFile.getResolutionFacadeForTest()
        val fileBefore = mainFile.getOrBuildCfirFile(facadeBefore)
        val targetBefore = assertTargetSource(mainFile, fileBefore, facadeBefore)
        val symbolBefore = function.resolveToCfirSymbol(facadeBefore, CfirResolvePhase.BODY_RESOLVE)
        val callerBefore = caller.resolveToCfirSymbol(facadeBefore, CfirResolvePhase.BODY_RESOLVE)
        val followingBefore = following.resolveToCfirSymbol(facadeBefore, CfirResolvePhase.BODY_RESOLVE)
        val declaration = symbolBefore.cfir as CfirNamedFunction
        val bodyBefore = requireNotNull(declaration.body)
        assertResolvedIntegers(declaration, listOf("1"))
        val diagnosticsBefore = errors(mainFile, facadeBefore)
        assertEquals(listOf(CfirErrors.UNRESOLVED_REFERENCE.name), diagnosticsBefore.map { it.factoryName })
        val diagnosticBefore = diagnosticsBefore.single()
        assertMissingDiagnostic(mainFile, diagnosticBefore)
        val offsetsBefore = diagnosticBefore.textRanges.map { it.startOffset to it.endOffset }
        val oldBodyLength = requireNotNull(function.bodyExpression).textLength

        replaceExplicitBodies(mainFile, listOf(function to replacement))

        val facadeAfter = mainFile.getResolutionFacadeForTest()
        val fileAfter = mainFile.getOrBuildCfirFile(facadeAfter)
        val targetAfter = assertTargetSource(mainFile, fileAfter, facadeAfter)
        assertSame(fileBefore, fileAfter)
        assertSame(facadeBefore.useSiteCfirSession, facadeAfter.useSiteCfirSession)
        assertNotSame(targetBefore, targetAfter)
        assertSame(symbolBefore, function.resolveToCfirSymbol(facadeAfter, CfirResolvePhase.BODY_RESOLVE))
        assertSame(callerBefore, caller.resolveToCfirSymbol(facadeAfter, CfirResolvePhase.BODY_RESOLVE))
        assertSame(followingBefore, following.resolveToCfirSymbol(facadeAfter, CfirResolvePhase.BODY_RESOLVE))
        assertNotSame(bodyBefore, declaration.body)
        assertResolvedIntegers(declaration, listOf("1"))
        assertElementSource(function, declaration, targetAfter)
        assertElementSource(caller, callerBefore.cfir, targetAfter)
        assertElementSource(following, followingBefore.cfir, targetAfter)
        assertSubtreeSourcesCurrent(fileAfter, targetAfter)
        val diagnosticsAfter = errors(mainFile, facadeAfter)
        assertEquals(listOf(CfirErrors.UNRESOLVED_REFERENCE.name), diagnosticsAfter.map { it.factoryName })
        val diagnosticAfter = diagnosticsAfter.single()
        assertNotSame(diagnosticBefore, diagnosticAfter, "旧诊断不能随保留声明一起缓存")
        assertMissingDiagnostic(mainFile, diagnosticAfter)
        val delta = replacement.length - oldBodyLength
        assertTrue(delta != 0)
        assertEquals(
            offsetsBefore.map { (start, end) -> start + delta to end + delta },
            diagnosticAfter.textRanges.map { it.startOffset to it.endOffset },
        )
        val linesMapping = requireNotNull(fileAfter.sourceFileLinesMapping)
        mainFile.text.withIndex().filter { it.value == '\n' && it.index + 1 < mainFile.textLength }
            .forEachIndexed { line, newline -> assertEquals(newline.index + 1, linesMapping.getLineStartOffset(line + 1)) }
        assertEquals(diagnosticsAfter.map { it.textRanges }, errors(mainFile, facadeAfter).map { it.textRanges })
    }

    private fun <T : Any> identitySet(): MutableSet<T> = Collections.newSetFromMap(IdentityHashMap<T, Boolean>())

    /** 普通语法子树用于证明 DFA 独占载荷，也用于独立检查 source；不复用生产 source-children。 */
    private fun ordinaryElements(root: CfirElement): Set<CfirElement> {
        val elements = identitySet<CfirElement>()
        root.accept(object : CfirDefaultVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (elements.add(element)) element.acceptChildren(this)
            }

            override fun visitLazyBlock(lazyBlock: CfirLazyBlock) = Unit
        })
        return elements
    }

    private fun assertOrdinarySourcesCurrent(root: CfirElement, target: CjFile) {
        ordinaryElements(root).forEach { element ->
            val source = element.source ?: return@forEach
            assertSame(target, (source as CjPsiSourceElement).psi.containingFile,
                "${element::class.simpleName} 的实际 source 必须属于当前 target")
        }
    }

    /** 反射只读既有 builder 的私有存储根；节点按 identity 遍历拓扑，不反射或调用迁移 API。 */
    private fun snapshotCfgNodes(snapshot: LLPartialBodyAnalysisSnapshot): Set<CFGNode<*>> =
        collectCfgNodes(listOf(snapshot.dataFlowAnalyzerContext.graphBuilder))

    private fun collectCfgNodes(roots: Collection<Any>): Set<CFGNode<*>> {
        val nodes = identitySet<CFGNode<*>>()
        val visited = identitySet<Any>()
        val pending = java.util.ArrayDeque<Any>()
        fun enqueue(value: Any?) {
            if (value != null && visited.add(value)) pending.addLast(value)
        }
        fun enqueueStorage(owner: Any) {
            owner.javaClass.declaredFields.filterNot { Modifier.isStatic(it.modifiers) }.forEach { field ->
                field.isAccessible = true
                enqueue(field.get(owner))
            }
        }
        roots.forEach(::enqueue)
        while (pending.isNotEmpty()) {
            when (val value = pending.removeFirst()) {
                is CFGNode<*> -> {
                    nodes += value
                    enqueue(value.owner)
                    value.previousNodes.forEach(::enqueue)
                    value.followingNodes.forEach(::enqueue)
                    if (value is CFGNodeWithSubgraphs<*>) value.subGraphs.forEach(::enqueue)
                }
                is ControlFlowGraph -> {
                    // partial 图没有冻结 nodes 列表；入口、出口及栈/cache 节点共同覆盖双向拓扑。
                    enqueue(value.enterNode)
                    enqueue(value.exitNode)
                }
                is ControlFlowGraphBuilder -> enqueueStorage(value)
                is Stack<*> -> value.all().forEach(::enqueue)
                is Map<*, *> -> value.forEach { (key, entry) ->
                    enqueue(key)
                    enqueue(entry)
                }
                is Map.Entry<*, *> -> {
                    enqueue(value.key)
                    enqueue(value.value)
                }
                is Pair<*, *> -> {
                    enqueue(value.first)
                    enqueue(value.second)
                }
                is Iterable<*> -> value.forEach(::enqueue)
                is Array<*> -> value.forEach(::enqueue)
                is CfirElement, is CfirBasedSymbol<*>, is String, is Number, is Boolean, is Char, is Enum<*> -> Unit
                else -> {
                    check(value.javaClass.enclosingClass == ControlFlowGraphBuilder::class.java) {
                        "Unknown CFG cache payload: ${value.javaClass.name}"
                    }
                    // PostponedLambdas / ListMultimap 仍是 builder 自有缓存，不依赖新 source-owner 接口。
                    enqueueStorage(value)
                }
            }
        }
        return nodes
    }

    private fun assertSnapshotCfgSourcesCurrent(snapshot: LLPartialBodyAnalysisSnapshot, target: CjFile) {
        val nodes = snapshotCfgNodes(snapshot)
        assertTrue(nodes.isNotEmpty(), "必须检查真实存在的 snapshot CFG")
        nodes.forEach { assertOrdinarySourcesCurrent(it.fir, target) }
        snapshot.result.statements.forEach { assertOrdinarySourcesCurrent(it, target) }
        snapshot.result.defaultParameterValues.forEach { assertOrdinarySourcesCurrent(it, target) }
    }

    private fun errors(mainFile: CjFile, facade: LLResolutionFacade): List<CjPsiDiagnostic> =
        mainFile.collectDiagnosticsForFile(facade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
            .filter { it.severity == Severity.ERROR }

    private fun assertMissingDiagnostic(mainFile: CjFile, diagnostic: CjPsiDiagnostic) {
        assertSame(mainFile, diagnostic.psiElement.containingFile, "公开诊断定位回当前原文件")
        assertEquals(listOf("missing"), diagnostic.textRanges.map { mainFile.text.substring(it.startOffset, it.endOffset) })
        assertEquals(listOf(mainFile.text.indexOf("missing")), diagnostic.textRanges.map { it.startOffset })
    }
}
