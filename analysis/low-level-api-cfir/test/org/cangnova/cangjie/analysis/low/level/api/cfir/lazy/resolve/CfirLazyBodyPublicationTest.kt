/*
 * Copyright 2010-2026 Cangnova contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLPartialBodyAnalysisResult
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLPartialBodyAnalysisSnapshot
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLPartialBodyAnalysisState
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.partialBodyAnalysisState
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirModifiablePsiSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.BodyStateKeepers
import org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.PartialBodyAnalysisSuspendedException
import org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.resolveWithKeeper
import org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.stateKeeper
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzerContext
import org.cangnova.cangjie.cfir.resolve.body.CfirTowerDataContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceExpr
import org.cangnova.cangjie.cfir.session.annotationMetadataRegistry
import org.cangnova.cangjie.cfir.session.rawMacroSurfaceUsageStoreOrNull
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 惰性正文采用与 session 索引发布必须属于同一次状态事务。
 * 夹具沿用 MacroRawOwnerSnapshotRawCfirTest 的局部参数注解与表达式宏语法；
 * A/M 故意不声明，测试在 raw 重建后停止，不执行语义解析或诊断检查。
 */
class CfirLazyBodyPublicationTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/lazyBodyPublication",
) {
    override val configurator = analysisApiCfirModifiablePsiSourceTestConfigurator(analyseInDependentSession = false)

    /** 普通失败必须同时恢复正文、局部声明注解索引及文件/PSI 别名的宏使用记录。 */
    @Test
    fun failedBodyResolveRestoresPublishedIndexes(mainFile: CjFile) {
        val file = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val target = file.declarations.filterIsInstance<CfirNamedFunction>().single()
        val session = target.moduleData.session
        val registry = session.annotationMetadataRegistry
        val usages = requireNotNull(session.rawMacroSurfaceUsageStoreOrNull)
        val originalBody = requireNotNull(target.body)
        assertTrue(originalBody is CfirLazyBlock)
        val originalFileUsages = usages.expressionSurfaces(file)
        val originalPsiUsages = usages.expressionSurfacesForSourceFile(mainFile)
        assertSurfaceIdentities(originalFileUsages, originalPsiUsages)

        val designation = CfirDesignation(listOf(file), target)
        val sentinel = BodyResolveSentinel()
        lateinit var captured: CapturedBody
        val thrown = assertThrows(BodyResolveSentinel::class.java) {
            CfirLazyBodyPublications(session).use { publications ->
                resolveWithKeeper(
                    target,
                    designation,
                    publications.withStateKeeper(BodyStateKeepers.FUNCTION),
                    prepareTarget = { CfirLazyBodiesCalculator.calculateBodies(designation, publications) },
                ) {
                    captured = captureBody(target)
                    // 已完成 raw 采用；在任何语义解析之前模拟解析动作失败。
                    throw sentinel
                }
            }
        }
        assertSame(sentinel, thrown)

        // 强持有重建正文及局部 owner，防止弱索引回收掩盖悬空发布。
        assertAll(
            "Failed body resolution must restore the graph and both published indexes",
            { assertSame(originalBody, target.body, "The original lazy body must be restored") },
            { assertNull(registry.snapshot(captured.annotation), "A discarded local annotation must not remain indexed") },
            { assertSurfaceIdentities(originalFileUsages, usages.expressionSurfaces(file)) },
            { assertSurfaceIdentities(originalPsiUsages, usages.expressionSurfacesForSourceFile(mainFile)) },
            {
                assertFalse(usages.expressionSurfaces(file).any { surface ->
                    captured.expressionMacros.any { it === surface.replaceHandle.carrier }
                }, "Discarded expression macro carriers must not remain in the actual file index")
            },
        )
    }

    /** 局部正文分析的正常挂起保留已采用正文及其真实 owner/carrier 的发布记录。 */
    @Test
    fun partialBodySuspensionRetainsPublishedIndexes(mainFile: CjFile) {
        val file = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val target = file.declarations.filterIsInstance<CfirNamedFunction>().single()
        val session = target.moduleData.session
        val registry = session.annotationMetadataRegistry
        val usages = requireNotNull(session.rawMacroSurfaceUsageStoreOrNull)
        val originalBody = requireNotNull(target.body)
        assertTrue(originalBody is CfirLazyBlock)
        val originalFileUsages = usages.expressionSurfaces(file)
        val originalPsiUsages = usages.expressionSurfacesForSourceFile(mainFile)
        assertSurfaceIdentities(originalFileUsages, originalPsiUsages)

        val designation = CfirDesignation(listOf(file), target)
        lateinit var captured: CapturedBody
        val thrown = assertThrows(PartialBodyAnalysisSuspendedException::class.java) {
            CfirLazyBodyPublications(session).use { publications ->
                resolveWithKeeper(
                    target,
                    designation,
                    publications.withStateKeeper(BodyStateKeepers.FUNCTION),
                    prepareTarget = { CfirLazyBodiesCalculator.calculateBodies(designation, publications) },
                ) {
                    captured = captureBody(target)
                    throw PartialBodyAnalysisSuspendedException
                }
            }
        }
        assertSame(PartialBodyAnalysisSuspendedException, thrown)

        assertAll(
            "Successful partial suspension must keep the graph and its published indexes",
            { assertSame(captured.body, target.body) },
            { assertSame(captured.annotationOwner, requireNotNull(registry.snapshot(captured.annotation)).owner) },
            { assertAdoptedMacroUsages(originalFileUsages, captured, usages.expressionSurfaces(file)) },
            { assertAdoptedMacroUsages(originalPsiUsages, captured, usages.expressionSurfacesForSourceFile(mainFile)) },
            { assertSurfaceIdentities(usages.expressionSurfaces(file), usages.expressionSurfacesForSourceFile(mainFile)) },
        )
    }

    /** 根后处理失败发生在 action 前，原图恢复和 OPEN 发布产品清理必须覆盖同一边界。 */
    @Test
    fun rootPostProcessFailureRestoresPublishedIndexes(mainFile: CjFile) {
        val file = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val target = file.declarations.filterIsInstance<CfirNamedFunction>().single()
        val session = target.moduleData.session
        val registry = session.annotationMetadataRegistry
        val usages = requireNotNull(session.rawMacroSurfaceUsageStoreOrNull)
        val originalBody = requireNotNull(target.body)
        assertTrue(originalBody is CfirLazyBlock)
        val originalFileUsages = usages.expressionSurfaces(file)
        val originalPsiUsages = usages.expressionSurfacesForSourceFile(mainFile)
        val designation = CfirDesignation(listOf(file), target)
        val sentinel = BodyResolveSentinel()
        var actionStarted = false
        lateinit var captured: CapturedBody
        val rootKeeper = stateKeeper<CfirNamedFunction, CfirDesignation> { builder, _, context ->
            builder.add(BodyStateKeepers.FUNCTION, context)
            builder.postProcess {
                captured = captureBody(target)
                throw sentinel
            }
        }
        val thrown = assertThrows(BodyResolveSentinel::class.java) {
            CfirLazyBodyPublications(session).use { publications ->
                resolveWithKeeper(
                    target,
                    designation,
                    publications.withStateKeeper(rootKeeper),
                    prepareTarget = { CfirLazyBodiesCalculator.calculateBodies(designation, publications) },
                ) {
                    actionStarted = true
                }
            }
        }
        assertSame(sentinel, thrown)
        assertAll(
            "Root postProcess failure must restore the graph without publishing candidate indexes",
            { assertFalse(actionStarted) },
            { assertSame(originalBody, target.body) },
            { assertNull(registry.snapshot(captured.annotation)) },
            { assertSurfaceIdentities(originalFileUsages, usages.expressionSurfaces(file)) },
            { assertSurfaceIdentities(originalPsiUsages, usages.expressionSurfacesForSourceFile(mainFile)) },
        )
    }

    /** 根后处理复用旧前缀/default；发布选择真实旧 carrier，失败后恢复完整旧记录。 */
    @Test
    fun failedBodyResolveRestoresReusedPrefixAndDefaultFacts(mainFile: CjFile) {
        val file = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val target = file.declarations.filterIsInstance<CfirNamedFunction>().single()
        val designation = CfirDesignation(listOf(file), target)
        val session = target.moduleData.session
        val registry = session.annotationMetadataRegistry
        val usages = requireNotNull(session.rawMacroSurfaceUsageStoreOrNull)
        publishBody(target, designation)
        val original = captureBody(target)
        val originalLocalSlot = requireNotNull(registry.snapshot(original.annotation))
        val parameter = target.valueParameters.single()
        val originalDefault = requireNotNull(parameter.defaultValue)
        val headerAnnotation = parameter.annotations.single() as CfirAnnotationCall
        val originalHeaderSlot = requireNotNull(registry.snapshot(headerAnnotation))
        val originalFileUsages = usages.expressionSurfaces(file)
        val originalPsiUsages = usages.expressionSurfacesForSourceFile(mainFile)
        assertIndexedCarriers(original.expressionMacros + originalDefault, originalFileUsages)
        assertSurfaceIdentities(originalFileUsages, originalPsiUsages)

        // 只建立 keeper 的旧节点复用输入，不执行语义解析；快照强持有真实旧前缀/default。
        val partialState = LLPartialBodyAnalysisState(
            totalPsiStatementCount = 2,
            analyzedPsiStatementCount = 1,
            analyzedCfirStatementCount = 1,
            performedAnalysesCount = 1,
            analysisStateSnapshot = LLPartialBodyAnalysisSnapshot(
                LLPartialBodyAnalysisResult(original.body.statements.take(1), listOf(originalDefault)),
                CfirTowerDataContext(),
                CfirDataFlowAnalyzerContext(),
            ),
        )
        target.partialBodyAnalysisState = partialState
        val retainedCarriers = original.expressionMacros.take(2) + originalDefault
        val retainedRecords = originalFileUsages.filter { surface -> retainedCarriers.any { it === surface.replaceHandle.carrier } }
        val sentinel = BodyResolveSentinel()
        lateinit var discarded: CapturedBody
        lateinit var discardedDefault: CfirExpression
        val thrown = assertThrows(BodyResolveSentinel::class.java) {
            CfirLazyBodyPublications(session).use { publications ->
                resolveWithKeeper(
                    target,
                    designation,
                    publications.withStateKeeper(BodyStateKeepers.FUNCTION),
                    prepareTarget = {
                        CfirLazyBodiesCalculator.calculateBodies(designation, publications)
                        discarded = captureBody(target)
                        discardedDefault = requireNotNull(parameter.defaultValue)
                    },
                ) {
                    val active = captureBody(target)
                    assertSame(original.body.statements.first(), active.body.statements.first())
                    assertSame(originalDefault, parameter.defaultValue)
                    assertSame(originalLocalSlot, registry.snapshot(active.annotation))
                    assertSame(originalHeaderSlot, registry.snapshot(headerAnnotation))
                    assertNull(registry.snapshot(discarded.annotation))
                    val activeCarriers = active.expressionMacros + originalDefault
                    val actualFileUsages = usages.expressionSurfaces(file)
                    assertIndexedCarriers(activeCarriers, actualFileUsages)
                    assertIndexedCarriers(activeCarriers, usages.expressionSurfacesForSourceFile(mainFile))
                    retainedRecords.forEach { record -> assertTrue(actualFileUsages.any { it === record }) }
                    assertFalse(actualFileUsages.any { surface ->
                        surface.replaceHandle.carrier === discardedDefault ||
                                discarded.expressionMacros.take(2).any { it === surface.replaceHandle.carrier }
                    })
                    throw sentinel
                }
            }
        }
        assertSame(sentinel, thrown)
        assertAll(
            "Rollback must retain the actual old prefix, defaults, metadata and usage records",
            { assertSame(original.body, target.body) },
            { assertSame(originalDefault, parameter.defaultValue) },
            { assertSame(partialState, target.partialBodyAnalysisState) },
            { assertSame(originalLocalSlot, registry.snapshot(original.annotation)) },
            { assertSame(originalHeaderSlot, registry.snapshot(headerAnnotation)) },
            { assertNull(registry.snapshot(discarded.annotation)) },
            { assertSurfaceIdentities(originalFileUsages, usages.expressionSurfaces(file)) },
            { assertSurfaceIdentities(originalPsiUsages, usages.expressionSurfacesForSourceFile(mainFile)) },
        )
    }

    /** 同文件 peer 在目标事务内独立提交；目标撤销只恢复自身槽位，保留 peer 和 header。 */
    @Test
    fun failedBodyResolvePreservesSameFilePeerAndHeader(mainFile: CjFile) {
        val file = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val functions = file.declarations.filterIsInstance<CfirNamedFunction>()
        val target = functions.single { it.name.asString() == "outer" }
        val peer = functions.single { it.name.asString() == "peer" }
        val session = target.moduleData.session
        val registry = session.annotationMetadataRegistry
        val usages = requireNotNull(session.rawMacroSurfaceUsageStoreOrNull)
        val originalBody = requireNotNull(target.body)
        assertTrue(originalBody is CfirLazyBlock)
        assertTrue(peer.body is CfirLazyBlock)
        val headerAnnotation = target.annotations.single() as CfirAnnotationCall
        val headerMacro = headerAnnotation.argumentList.arguments.single()
        val originalHeaderSlot = requireNotNull(registry.snapshot(headerAnnotation))
        val parameterHeaders = listOf(target, peer).map { function ->
            function.valueParameters.single().annotations.single() as CfirAnnotationCall
        }
        val parameterHeaderSlots = parameterHeaders.map { requireNotNull(registry.snapshot(it)) }
        val originalFileUsages = usages.expressionSurfaces(file)
        val originalPsiUsages = usages.expressionSurfacesForSourceFile(mainFile)
        assertIndexedCarriers(listOf(headerMacro), originalFileUsages)
        assertSurfaceIdentities(originalFileUsages, originalPsiUsages)
        val designation = CfirDesignation(listOf(file), target)
        val sentinel = BodyResolveSentinel()
        lateinit var discarded: CapturedBody
        lateinit var committedPeer: CapturedBody
        lateinit var committedPeerRecords: List<MacroSurfaceExpr>
        val thrown = assertThrows(BodyResolveSentinel::class.java) {
            CfirLazyBodyPublications(session).use { publications ->
                resolveWithKeeper(
                    target,
                    designation,
                    publications.withStateKeeper(BodyStateKeepers.FUNCTION),
                    prepareTarget = { CfirLazyBodiesCalculator.calculateBodies(designation, publications) },
                ) {
                    discarded = captureBody(target)
                    // 使用另一份操作状态独立提交，不能由 outer 的恢复句柄撤销。
                    publishBody(peer, CfirDesignation(listOf(file), peer))
                    committedPeer = captureBody(peer)
                    committedPeerRecords = usages.expressionSurfaces(file).filter { surface ->
                        committedPeer.expressionMacros.any { it === surface.replaceHandle.carrier }
                    }
                    assertIndexedCarriers(committedPeer.expressionMacros, committedPeerRecords)
                    throw sentinel
                }
            }
        }
        assertSame(sentinel, thrown)
        assertAll(
            "Target rollback must preserve an independently committed peer and original header facts",
            { assertSame(originalBody, target.body) },
            { assertNull(registry.snapshot(discarded.annotation)) },
            { assertSame(committedPeer.body, peer.body) },
            { assertSame(committedPeer.annotationOwner, requireNotNull(registry.snapshot(committedPeer.annotation)).owner) },
            { assertSame(originalHeaderSlot, registry.snapshot(headerAnnotation)) },
            {
                parameterHeaders.zip(parameterHeaderSlots).forEach { (annotation, slot) ->
                    assertSame(slot, registry.snapshot(annotation))
                }
            },
            { assertSurfaceIdentities(originalFileUsages + committedPeerRecords, usages.expressionSurfaces(file)) },
            { assertSurfaceIdentities(originalPsiUsages + committedPeerRecords, usages.expressionSurfacesForSourceFile(mainFile)) },
            { assertIndexedCarriers(listOf(headerMacro) + committedPeer.expressionMacros, usages.expressionSurfaces(file)) },
            { assertIndexedCarriers(listOf(headerMacro) + committedPeer.expressionMacros, usages.expressionSurfacesForSourceFile(mainFile)) },
        )
    }

    /** 通过真实 calculator/keeper 组合提交 raw 正文，供后续复用和隔离测试建立旧事实。 */
    private fun publishBody(target: CfirNamedFunction, designation: CfirDesignation) {
        CfirLazyBodyPublications(target.moduleData.session).use { publications ->
            resolveWithKeeper(
                target,
                designation,
                publications.withStateKeeper(BodyStateKeepers.FUNCTION),
                prepareTarget = { CfirLazyBodiesCalculator.calculateBodies(designation, publications) },
            ) {}
        }
    }

    private fun assertIndexedCarriers(expected: List<CfirExpression>, actual: List<MacroSurfaceExpr>) {
        assertEquals(expected.size, actual.size)
        expected.forEach { carrier ->
            assertEquals(1, actual.count { it.replaceHandle.carrier === carrier }, "Each actual carrier must retain one usage fact")
        }
    }

    private fun captureBody(target: CfirNamedFunction): CapturedBody {
        val body = requireNotNull(target.body)
        assertFalse(body is CfirLazyBlock)
        val local = body.statements.filterIsInstance<CfirNamedFunction>().single()
        val parameter = local.valueParameters.single()
        val annotation = parameter.annotations.single() as CfirAnnotationCall
        return CapturedBody(
            body,
            parameter,
            annotation,
            listOf(
                requireNotNull(parameter.defaultValue),
                requireNotNull(local.body).statements.filterIsInstance<CfirReturnExpression>().single().result,
                body.statements.filterIsInstance<CfirReturnExpression>().single().result,
            ),
        )
    }

    private fun assertSurfaceIdentities(expected: List<MacroSurfaceExpr>, actual: List<MacroSurfaceExpr>) {
        assertEquals(expected.size, actual.size, "The original macro usage records must be restored")
        expected.forEach { original ->
            assertTrue(actual.any { it === original }, "The original usage record identity must be retained")
        }
    }

    private fun assertAdoptedMacroUsages(original: List<MacroSurfaceExpr>, captured: CapturedBody, actual: List<MacroSurfaceExpr>) {
        assertEquals(original.size + captured.expressionMacros.size, actual.size)
        original.forEach { surface -> assertTrue(actual.any { it === surface }) }
        captured.expressionMacros.forEach { expression ->
            assertEquals(1, actual.count { it.replaceHandle.carrier === expression }, "Each adopted macro carrier must be indexed once")
        }
    }

    private class BodyResolveSentinel : RuntimeException("Stop after raw body adoption")

    private class CapturedBody(
        val body: CfirBlock,
        val annotationOwner: CfirValueParameter,
        val annotation: CfirAnnotationCall,
        val expressionMacros: List<CfirExpression>,
    )
}
