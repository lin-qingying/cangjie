@file:OptIn(
    ExperimentalUnsignedTypes::class,
    org.cangnova.cangjie.cfir.CfirImplementationDetail::class,
    org.cangnova.cangjie.cfir.declarations.ResolveStateAccess::class,
)

package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.*
import com.google.flatbuffers.Table
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysisSettings
import org.cangnova.cangjie.cfir.resolve.providers.CfirLibrarySessionProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirBodyDeserializer
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoSourceContentRoot
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoSourceExpressionSlotKind
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoSourceOwningParent
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializedSourceGraph
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirSourceGraphLoader
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.serialization.provider.CfirExtendProviderComposer
import org.cangnova.cangjie.cfir.session.*
import org.cangnova.cangjie.cfir.source.*
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagWithFixedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirDummyCompilerLazyDeclarationResolver
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.io.TempDir
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

/**
 * 正式 Library context 的 header、显式正文、annotation/default 与完整 SOURCE 共用身份。
 *
 * 依据 build/cjmp-body-reader-20261121/native-reader-contract.md 和 kotlin-mapping.md：
 * Native 14276740ffec1262b81b6b5236b1d840af270b37 的 ExprLoader.cpp:348–380、488–509
 * 以及 ReferenceLoader.cpp:588–589、632–645、732–748；Kotlin
 * 3c7a5041f238102285764d370665f56f587f9dc4 的 IrBodyDeserializer.kt:218–222、667–673。
 * Return/Jump 的 Nothing 类型另由固定 Native TypeCheckExpr/ReturnExpr.cpp、JumpExpr.cpp 确认。
 *
 * 复用 CjoDeclarationCompletionLifecycleTest 的真实 context/provider 及参数完成路径，
 * CjoUnifiedSourceReviewBoundaryTest 的 OFFICIAL_CJMP mixed Block、Library session 和 SDK core。
 * 本文件的包正文是有限手工 schema seeds，不是官方编译器从源码产出的 CJO；
 * String 类型只引用现有 SDK std.core.cjo 的真实 exportId，绝不手造 std.core/String。
 */
class CjoBodyDeserializerIntegrationTest {
    @Test
    fun headerFirstExplicitBodyPreservesFunctionParameterAndReturnTarget(@TempDir root: Path) {
        val fixture = fixture(root, controlSeed())
        val function = fixture.function()
        val frozen = HeaderSnapshot(function)
        assertNull(function.body)
        assertTrue(fixture.context.completedSourceHeaders.getValue(function).bodyAvailable)
        val parameter = function.valueParameters.single()
        assertSame(parameter, fixture.declaration(1))
        assertSame(function.symbol, parameter.containingDeclarationSymbol)
        assertEquals("19", assertIs<CfirLiteralExpression>(parameter.defaultValue).value)

        val body = assertNotNull(fixture.context.createDeclDeserializer().deserializeFunctionBody(0))
        assertSame(body, function.body)
        assertEquals(5, body.statements.size)
        val local = assertIs<CfirFieldVariable>(body.statements[0])
        assertTrue(local.isLocal)
        assertEquals("17", assertIs<CfirLiteralExpression>(local.initializer).value)
        assertReference(body.statements[1] as CfirExpression, local.symbol)
        val returned = assertIs<CfirReturnExpression>(body.statements.last())
        assertTrue(returned.target.isBound)
        assertSame(function, returned.target.labeledElement)
        assertReference(returned.result, local.symbol)
        assertEquals(ConePrimitiveType.NOTHING, returned.coneTypeOrNull)
        // 通过真正的普通 visitor 检查旧 Function 已能看到正文，且一次拥有遍历只出现一次。
        assertEquals(1, ordinaryElements(function).count { it === body })
        repeat(3) {
            assertSame(function, fixture.function())
            assertSame(body, fixture.context.createDeclDeserializer().deserializeFunctionBody(0))
            assertSame(parameter, fixture.declaration(1))
            assertSame(returned.target, assertIs<CfirReturnExpression>(function.body!!.statements.last()).target)
            frozen.assertUnchanged(body)
        }
    }

    /** 坏槽保持非零、拥有闭包仍合法；失败发生于实际 Block kind/info 检查。 */
    @Test
    fun nonzeroMalformedBodyPreservesCompletedHeaderAndRetriesInFreshContexts(@TempDir root: Path) {
        val seed = controlSeed()
        val bytes = CjoPackageWriter.toByteArray(seed)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val packageSlots = WireSlots().at(buffer.getInt(0), buffer)
        val blockSlots = WireSlots().at(packageSlots.expressionPosition(12), buffer)
        buffer.put(blockSlots.expressionInfoTypeSlot(), ExprInfo.ReferenceInfo.toByte())
        val broken = fixture(root, seed, bytes = bytes)
        val oldFunction = broken.function()
        val oldDefaultIdentity = CfirSourceGraphLoader(broken.context).expression(15u).expressionReferences.getValue(15u)
        val frozen = HeaderSnapshot(oldFunction)
        val oldDeclarations = broken.context.declCache
        val oldTypes = broken.context.typeCache
        val oldConstraints = broken.context.genericConstraints
        val oldHeaders = broken.context.completedSourceHeaders
        val oldGraph = broken.context.completedHeaderSourceGraph
        assertFalse(oldDeclarations.containsKey(2), "正文请求前局部声明尚未完成")
        val wireFunction = assertIs<FuncInfo>(broken.context.pkg.allDecls(0)!!.info(FuncInfo()))
        assertEquals(1u, assertNotNull(wireFunction.funcBody).body)
        repeat(2) {
            assertFailsWith<IllegalStateException> {
                broken.context.createDeclDeserializer().deserializeFunctionBody(0)
            }
            assertSame(oldDeclarations, broken.context.declCache)
            assertSame(oldTypes, broken.context.typeCache)
            assertSame(oldConstraints, broken.context.genericConstraints)
            assertSame(oldHeaders, broken.context.completedSourceHeaders)
            assertSame(oldGraph, broken.context.completedHeaderSourceGraph)
            assertNull(broken.context.completedSourceGraph)
            assertFalse(broken.context.declCache.containsKey(2), "失败的新局部声明不得泄露")
            frozen.assertUnchanged()
        }

        // 同一路径换入完整 seed，重建 manager/session/context；旧只读 snapshot 仍保留坏槽。
        val fresh = fixture(root, seed)
        assertNotSame(broken.context, fresh.context)
        assertNotSame(broken.context.moduleData.session, fresh.context.moduleData.session)
        val function = fresh.function()
        assertNotSame(oldFunction, function)
        val parameter = function.valueParameters.single()
        val body = assertNotNull(fresh.context.createDeclDeserializer().deserializeFunctionBody(0))
        val returned = assertIs<CfirReturnExpression>(body.statements.last())
        val freshGraph = fresh.fullSourceGraph()
        assertNotSame(oldDefaultIdentity, freshGraph.expressionReferences.getValue(15u))
        repeat(3) {
            assertSame(function, fresh.function())
            assertSame(body, fresh.context.createDeclDeserializer().deserializeFunctionBody(0))
            assertSame(body, function.body)
            assertSame(parameter, fresh.declaration(1))
            assertSame(function, returned.target.labeledElement)
            assertEquals(1, ordinaryElements(function).count { it === body })
            fresh.context.materialize { assertEquals(listOf(function), fresh.context.bodyState.bodies.keys.toList()) }
        }
        assertFailsWith<IllegalStateException> { broken.context.createDeclDeserializer().deserializeFunctionBody(0) }
        assertSame(oldDeclarations, broken.context.declCache)
        assertSame(oldGraph, broken.context.completedHeaderSourceGraph)
        frozen.assertUnchanged()
    }

    @Test
    fun defaultOwningBlockMaterializesLocalInitializerAndNestedFunctionsForPrimitiveAndFourStringKinds(@TempDir root: Path) =
        checkOwningBlocks(root, annotation = false)

    @Test
    fun annotationOwningBlockMaterializesLocalInitializerAndNestedFunctionsForPrimitiveAndFourStringKinds(@TempDir root: Path) =
        checkOwningBlocks(root, annotation = true)

    /** 先验证 primitive 的原根身份，再用正式 core 引用逐个覆盖四 StringKind。 */
    private fun checkOwningBlocks(root: Path, annotation: Boolean) {
        checkOwningBlock(fixture(root.resolve("primitive"), owningSeed(annotation)), annotation, null)
        val core = officialCore()
        for ((wireKind, semanticKind) in listOf(
            StringKind.Normal to CfirSourceStringKind.NORMAL,
            StringKind.JString to CfirSourceStringKind.JSTRING,
            StringKind.MultiLine to CfirSourceStringKind.MULTILINE,
            StringKind.MultiLineRaw to CfirSourceStringKind.MULTILINE_RAW,
        )) {
            val fixture = fixture(root.resolve(semanticKind.name), owningSeed(annotation, core, wireKind), core)
            checkOwningBlock(fixture, annotation, semanticKind)
        }
    }

    private fun checkOwningBlock(fixture: Fixture, annotation: Boolean, stringKind: CfirSourceStringKind?) {
        val function = fixture.function()
        val parameter = function.valueParameters.single()
        val annotationCall = if (annotation) assertIs<CfirAnnotationCall>(function.annotations.single()) else null
        val block = assertIs<CfirBlock>(if (annotationCall != null)
            annotationCall.argumentList.arguments.single() else parameter.defaultValue)
        assertEquals(4, block.statements.size)
        assertNull(function.body, "header 的 owning Block 不得顺带读取具名函数正文")
        val local = assertIs<CfirFieldVariable>(block.statements[0])
        assertTrue(local.isLocal)
        assertSame(local, fixture.declaration(3))
        val initializer = assertIs<CfirLiteralExpression>(local.initializer)
        assertEquals(if (stringKind == null) "17" else STRING_TEXT, initializer.value)
        if (stringKind == null) {
            assertEquals(CfirLiteralKind.INT, initializer.kind)
            assertEquals(ConePrimitiveType.INT64, initializer.coneTypeOrNull)
        } else {
            assertEquals(CfirLiteralKind.STRING, initializer.kind)
            assertEquals(stringKind, initializer.stringKind)
            val type = assertIs<ConeStructType>(initializer.coneTypeOrNull)
            assertEquals(StdlibClassIds.String, type.classId)
            val symbol = assertIs<ConeClassLikeLookupTagWithFixedSymbol>(type.lookupTag).symbol
            assertTrue(symbol.isBound)
            assertSame(symbol, assertNotNull(fixture.provider.getClassLikeSymbolByClassId(StdlibClassIds.String)))
            assertTrue(assertNotNull(symbol.cfir.cjoDeclarationOrigin).packagePath.endsWith("std.core.cjo"))
        }
        assertReference(block.statements[1] as CfirExpression, local.symbol)
        assertReference(block.statements.last() as CfirExpression, local.symbol)
        val lambdaExpression = assertIs<CfirAnonymousFunctionExpression>(block.statements[2])
        assertTrue(lambdaExpression.supportMock)
        val lambda = lambdaExpression.anonymousFunction
        assertTrue(lambda.isLambda)
        assertSame(lambda, lambda.symbol.cfir)
        val lambdaParameter = lambda.valueParameters.single()
        assertSame(lambdaParameter, fixture.declaration(4))
        assertSame(lambda.symbol, lambdaParameter.containingDeclarationSymbol)
        val lambdaBody = assertNotNull(lambda.body)
        assertEquals(2, lambdaBody.statements.size)
        val nestedExpression = assertIs<CfirAnonymousFunctionExpression>(lambdaBody.statements[0])
        assertFalse(nestedExpression.supportMock)
        val nested = nestedExpression.anonymousFunction
        assertNotSame(lambda, nested)
        assertSame(nested, nested.symbol.cfir)
        val nestedBody = assertNotNull(nested.body)
        assertEquals(1, nestedBody.statements.size)
        val innerReturn = assertIs<CfirReturnExpression>(nestedBody.statements.single())
        val outerReturn = assertIs<CfirReturnExpression>(lambdaBody.statements.last())
        assertTrue(innerReturn.target.isBound && outerReturn.target.isBound)
        assertSame(nested, innerReturn.target.labeledElement)
        assertSame(lambda, outerReturn.target.labeledElement)
        assertNotSame(innerReturn.target, outerReturn.target)
        assertReference(innerReturn.result, local.symbol)
        assertReference(outerReturn.result, lambdaParameter.symbol)
        assertSame(initializer.coneTypeOrNull, innerReturn.result.coneTypeOrNull)
        assertSame(initializer.coneTypeOrNull, outerReturn.result.coneTypeOrNull)
        val elements = ordinaryElements(block)
        for (owned in listOf(initializer, lambdaBody, nestedBody)) assertEquals(1, elements.count { it === owned })

        CfirSourceGraphLoader(fixture.context).loadHeaders()
        val source = assertNotNull(fixture.context.completedHeaderSourceGraph)
        val graph = source.graph
        val rootIdentity = if (annotation) graph.annotations.getValue(function).single().arguments.single().expression
            else graph.defaults.getValue(parameter)
        assertSame(rootIdentity, graph.expressionOf(block))
        assertSame(source.expressionReferences.getValue(1u), rootIdentity)
        assertSame(if (annotation) function else parameter, graph.declarationOwners.getValue(local))
        assertSame(graph.expressionOf(initializer), graph.initializers.getValue(local))
        assertSame(graph.expressionOf(lambdaBody), graph.functionBodies.getValue(lambda))
        assertSame(graph.expressionOf(nestedBody), graph.functionBodies.getValue(nested))
        assertSame(local, assertIs<CfirSourceReferenceTarget.Declaration>(assertIs<CfirSourceExpressionPayload.Reference>(graph.expression(graph.expressionOf(innerReturn.result)).payload).target).declaration)
        if (stringKind != null) assertEquals(stringKind,
            assertIs<CfirSourceExpressionPayload.Literal>(graph.expression(graph.expressionOf(initializer)).payload).stringKind)
        val header = HeaderSnapshot(function)
        repeat(2) {
            assertSame(function, fixture.function())
            assertSame(local, fixture.declaration(3))
            assertSame(lambdaParameter, fixture.declaration(4))
            assertSame(block, if (annotation) assertIs<CfirAnnotationCall>(function.annotations.single()).argumentList.arguments.single()
                else parameter.defaultValue)
            assertSame(lambdaBody, lambda.body)
            assertSame(nestedBody, nested.body)
            assertSame(rootIdentity, assertNotNull(fixture.context.completedHeaderSourceGraph).graph.expressionOf(block))
            header.assertUnchanged()
        }
    }

    @Test
    fun repeatedOrdinaryAndFullSourceGraphEntriesKeepExpressionPatternOriginsAndControlTargets(@TempDir root: Path) {
        for (sourceFirst in listOf(false, true)) {
            val fixture = fixture(root.resolve(sourceFirst.toString()), controlSeed())
            val function = fixture.function()
            val before = if (sourceFirst) fixture.fullSourceGraph() else null
            val body = assertNotNull(fixture.context.createDeclDeserializer().deserializeFunctionBody(0))
            val source = fixture.fullSourceGraph()
            val graph = source.graph
            val local = assertIs<CfirFieldVariable>(body.statements[0])
            val reference = assertIs<CfirNamedAccessExpression>(body.statements[1])
            val loop = assertIs<CfirLoopExpression>(body.statements[2])
            val jump = assertIs<CfirBreakExpression>(loop.body.statements.single())
            val match = assertIs<CfirNativeMatchExpression>(body.statements[3])
            val case = match.cases.single()
            val pattern: CfirPattern = case.patterns.single()
            val returned = assertIs<CfirReturnExpression>(body.statements.last())
            assertSame(local.initializer, reference.mappedExpression)
            assertEquals(CangjieOverflowStrategy.SATURATING, reference.overflowStrategy)
            assertNull(jump.mappedExpression, "Jump 的 map 槽是 refLoop，不能再成为普通 map child")
            assertTrue(jump.target.isBound)
            assertSame(loop, jump.target.labeledElement)
            assertSame(function, returned.target.labeledElement)
            assertSame(match.selector, pattern.contextExpression)
            assertEquals(ConePrimitiveType.INT64, assertNotNull(pattern.semanticTypeRef).coneType)
            assertTrue(pattern.constantValues.isEmpty()) // seed 的 values 真实为空，不从 selector 猜值。
            val canonicalPattern = assertIs<CfirSourceExpressionPayload.MatchCase>(graph.expression(graph.expressionOf(case)).payload).patterns.single()
            assertSame(graph.expressionOf(match.selector), canonicalPattern.contextExpression)
            assertSame(graph.functionBodies.getValue(function), graph.expressionOf(body))
            assertSame(graph.expressionOf(loop), assertIs<CfirSourceExpressionPayload.Jump>(graph.expression(graph.expressionOf(jump)).payload).loop)
            assertSame(function, assertIs<CfirSourceExpressionPayload.Return>(graph.expression(graph.expressionOf(returned)).payload).function)
            assertSame(graph.expressionOf(local.initializer!!), graph.expression(graph.expressionOf(reference)).mapExpression)
            before?.expressionReferences?.forEach { (wire, identity) -> assertSame(identity, source.expressionReferences.getValue(wire)) }
            if (before != null) assertSame(canonicalPattern.identity,
                assertIs<CfirSourceExpressionPayload.MatchCase>(before.graph.expression(before.expressionReferences.getValue(9u)).payload).patterns.single().identity)

            val ordinary = ordinaryElements(function).filterIsInstance<CfirExpression>()
            assertEquals(15, ordinary.size, "普通 visitor 必须实际访问全部 e；借用 map/target 不重复拥有遍历")
            assertEquals(15, source.expressionReferences.size)
            for (expression in ordinary) {
                val identity = graph.expressionOf(expression)
                val wire = source.expressionReferences.entries.single { it.value === identity }.key
                val imported = assertNotNull(graph.expression(identity).importedPosition)
                val location = assertIs<CjBinarySourceElement>(expression.source).sourceLocation
                assertEquals(imported.begin, location)
                assertEquals(wire.toInt(), assertNotNull(location).line)
            }
            assertEquals(canonicalPattern.importedPosition!!.begin, assertIs<CjBinarySourceElement>(pattern.source).sourceLocation)
            // 直接观察正式 context 池，P 的 table-position token 必须同时连接 canonical 与公共 ordinary P。
            fixture.context.materialize {
                val state = fixture.context.bodyState
                val tablePosition = state.patternIdentities.entries.single { it.value === canonicalPattern.identity }.key
                assertTrue(tablePosition > 0)
                assertSame(canonicalPattern.identity, state.patternRecords.getValue(tablePosition).identity)
                assertSame(pattern, state.patterns.getValue(canonicalPattern.identity))
                source.expressionReferences.forEach { (wire, identity) ->
                    assertSame(identity, state.identities.getValue(wire))
                    assertSame(graph.expressionOf(state.expressions.getValue(identity)), identity)
                }
            }
            repeat(3) {
                assertSame(function, fixture.function())
                assertSame(body, fixture.context.createDeclDeserializer().deserializeFunctionBody(0))
                val repeated = fixture.fullSourceGraph()
                source.expressionReferences.forEach { (wire, identity) -> assertSame(identity, repeated.expressionReferences.getValue(wire)) }
                ordinary.forEach { assertSame(graph.expressionOf(it), repeated.graph.expressionOf(it)) }
                val repeatedPattern = assertIs<CfirSourceExpressionPayload.MatchCase>(repeated.graph.expression(repeated.graph.expressionOf(case)).payload).patterns.single()
                assertSame(canonicalPattern.identity, repeatedPattern.identity)
                assertSame(match.selector, pattern.contextExpression)
                assertSame(jump.target, assertIs<CfirBreakExpression>(loop.body.statements.single()).target)
                assertSame(loop, jump.target.labeledElement)
                assertSame(function, returned.target.labeledElement)
            }
        }
    }

    /** 已由公开正文入口完成的 loop 是借用目标；裸 break 只选择自己的一条 owning e。 */
    @Test
    fun warmStandaloneBreak(@TempDir root: Path) {
        val fixture = fixture(root, controlSeed())
        val function = fixture.function()
        val body = assertNotNull(fixture.context.createDeclDeserializer().deserializeFunctionBody(0))
        val loop = assertIs<CfirLoopExpression>(body.statements[2])
        val jump = assertIs<CfirBreakExpression>(loop.body.statements.single())
        val target = jump.target
        assertTrue(target.isBound)
        assertSame(loop, target.labeledElement)
        val header = HeaderSnapshot(function)
        lateinit var loopIdentity: CfirSourceExpressionIdentity
        lateinit var loopRecord: CfirSourceExpressionRecord
        lateinit var jumpRecord: CfirSourceExpressionRecord
        fixture.context.materialize {
            val state = fixture.context.bodyState
            loopIdentity = state.identities.getValue(4u)
            loopRecord = state.records.getValue(4u)
            jumpRecord = state.records.getValue(7u)
            assertSame(loop, state.expressions.getValue(loopIdentity))
            assertSame(target, state.loops.getValue(loopIdentity))
        }

        val selected = CfirSourceGraphLoader(fixture.context).expression(7u)
        assertEquals(setOf(7u), selected.expressionReferences.keys)
        assertEquals(1, selected.expressionReferences.size)
        assertTrue(selected.graph.functionBodies.isEmpty())
        assertTrue(selected.graph.initializers.isEmpty())
        val jumpIdentity = selected.expressionReferences.getValue(7u)
        val record = selected.graph.expression(jumpIdentity)
        assertSame(jumpRecord, record)
        val payload = assertIs<CfirSourceExpressionPayload.Jump>(record.payload)
        assertTrue(payload.isBreak)
        assertSame(loopIdentity, payload.loop)
        assertNull(record.mapExpression)
        assertSame(jumpIdentity, selected.graph.expressionOf(jump))
        assertNull(jump.mappedExpression)
        fixture.context.materialize {
            val state = fixture.context.bodyState
            assertSame(jump, state.expressions.getValue(jumpIdentity))
            assertSame(loop, state.expressions.getValue(loopIdentity))
            assertSame(target, state.loops.getValue(loopIdentity))
            assertSame(loopRecord, state.records.getValue(4u))
            assertSame(record, state.records.getValue(7u))
            assertSame(body, state.bodies.getValue(function))
        }
        assertSame(target, jump.target)
        assertSame(loop, target.labeledElement)
        header.assertUnchanged(body)
    }

    /** map 只借用原完成 initializer；单独 reference 不取得该 initializer 的拥有权。 */
    @Test
    fun warmStandaloneMappedReference(@TempDir root: Path) {
        val fixture = fixture(root, controlSeed())
        val function = fixture.function()
        val body = assertNotNull(fixture.context.createDeclDeserializer().deserializeFunctionBody(0))
        val local = assertIs<CfirFieldVariable>(body.statements[0])
        val initializer = assertNotNull(local.initializer)
        val reference = assertIs<CfirNamedAccessExpression>(body.statements[1])
        assertSame(initializer, reference.mappedExpression)
        assertReference(reference, local.symbol)
        val header = HeaderSnapshot(function)
        lateinit var initializerIdentity: CfirSourceExpressionIdentity
        lateinit var initializerRecord: CfirSourceExpressionRecord
        lateinit var referenceRecord: CfirSourceExpressionRecord
        fixture.context.materialize {
            val state = fixture.context.bodyState
            initializerIdentity = state.identities.getValue(2u)
            initializerRecord = state.records.getValue(2u)
            referenceRecord = state.records.getValue(3u)
            assertSame(initializer, state.expressions.getValue(initializerIdentity))
            assertSame(initializer, state.initializers.getValue(local))
        }

        val selected = CfirSourceGraphLoader(fixture.context).expression(3u)
        assertEquals(setOf(3u), selected.expressionReferences.keys)
        assertEquals(1, selected.expressionReferences.size)
        assertTrue(selected.graph.functionBodies.isEmpty())
        assertTrue(selected.graph.initializers.isEmpty())
        assertSame(function, selected.graph.declarationOwners.getValue(local))
        val referenceIdentity = selected.expressionReferences.getValue(3u)
        val record = selected.graph.expression(referenceIdentity)
        assertSame(referenceRecord, record)
        assertSame(initializerIdentity, record.mapExpression)
        assertSame(local, assertIs<CfirSourceReferenceTarget.Declaration>(assertIs<CfirSourceExpressionPayload.Reference>(record.payload).target).declaration)
        assertEquals(CangjieOverflowStrategy.SATURATING, record.overflowStrategy)
        assertSame(referenceIdentity, selected.graph.expressionOf(reference))
        fixture.context.materialize {
            val state = fixture.context.bodyState
            assertSame(reference, state.expressions.getValue(referenceIdentity))
            assertSame(initializer, state.expressions.getValue(initializerIdentity))
            assertSame(initializer, state.initializers.getValue(local))
            assertSame(initializerRecord, state.records.getValue(2u))
            assertSame(record, state.records.getValue(3u))
            assertSame(body, state.bodies.getValue(function))
        }
        assertSame(initializer, reference.mappedExpression)
        header.assertUnchanged(body)
    }

    /** 裸 MatchCase 首次完成即连接原 owning Match 的 selector；以后只复用完成 record/P。 */
    @Test
    fun coldStandaloneMatchCase(@TempDir root: Path) {
        val fixture = fixture(root, controlSeed())
        val function = fixture.function()
        assertNull(function.body)
        assertTrue(fixture.context.completedSourceHeaders.getValue(function).bodyAvailable)
        val header = HeaderSnapshot(function)
        val loader = CfirSourceGraphLoader(fixture.context)
        prepareCaseSelector(fixture, loader, function)
        val selected = loader.expression(9u)
        assertEquals(setOf(9u, 13u, 14u), selected.expressionReferences.keys)
        assertEquals(3, selected.expressionReferences.size)
        assertTrue(selected.graph.functionBodies.isEmpty())
        assertNull(function.body)
        val caseIdentity = selected.expressionReferences.getValue(9u)
        val record = selected.graph.expression(caseIdentity)
        val payload = assertIs<CfirSourceExpressionPayload.MatchCase>(record.payload)
        val pattern = payload.patterns.single()
        val selectorIdentity = assertNotNull(pattern.contextExpression)
        assertSame(ConePrimitiveType.INT64, pattern.types.single())
        assertTrue(pattern.constantValues.isEmpty())
        assertNull(payload.guard)
        assertSame(selected.expressionReferences.getValue(13u), payload.body)
        var patternPosition = -1
        fixture.context.materialize {
            val state = fixture.context.bodyState
            assertSame(selectorIdentity, state.identities.getValue(10u))
            patternPosition = state.patternIdentities.entries.single { it.value === pattern.identity }.key
            assertTrue(patternPosition > 0)
            assertSame(record, state.records.getValue(9u))
            assertSame(pattern, state.patternRecords.getValue(patternPosition))
            assertFalse(state.bodies.containsKey(function))
        }
        header.assertUnchanged()

        val full = fixture.fullSourceGraph()
        val fullRecord = full.graph.expression(full.expressionReferences.getValue(9u))
        assertSame(record, fullRecord)
        assertSame(pattern, assertIs<CfirSourceExpressionPayload.MatchCase>(fullRecord.payload).patterns.single())
        val match = assertIs<CfirSourceExpressionPayload.Match>(
            full.graph.expression(full.expressionReferences.getValue(8u)).payload)
        assertSame(caseIdentity, match.cases.single())
        assertSame(selectorIdentity, match.selector)
        assertSame(selectorIdentity, full.expressionReferences.getValue(10u))
        fixture.context.materialize {
            val state = fixture.context.bodyState
            assertSame(record, state.records.getValue(9u))
            assertSame(pattern, state.patternRecords.getValue(patternPosition))
        }
        assertSame(selectorIdentity, pattern.contextExpression)
        header.assertUnchanged()
    }

    /** 所有视图与池必须共享真正冻结的完成 record/P；仅复用 token 不能满足此合同。 */
    @Test
    fun repeatedPartialHeaderFullCompletedRecordsAndPatterns(@TempDir root: Path) {
        val fixture = fixture(root, controlSeed())
        val function = fixture.function()
        val loader = CfirSourceGraphLoader(fixture.context)
        loader.loadHeaders()
        val firstHeader = assertNotNull(fixture.context.completedHeaderSourceGraph)
        val defaultRecord = firstHeader.graph.expression(firstHeader.expressionReferences.getValue(15u))
        prepareCaseSelector(fixture, loader, function)
        val firstPartial = loader.expression(9u)
        val firstCase = firstPartial.graph.expression(firstPartial.expressionReferences.getValue(9u))
        val firstPattern = assertIs<CfirSourceExpressionPayload.MatchCase>(firstCase.payload).patterns.single()
        assertNotNull(firstPattern.contextExpression)
        val firstFull = fixture.fullSourceGraph()
        val records = firstFull.expressionReferences.mapValues { (_, identity) -> firstFull.graph.expression(identity) }
        assertEquals(15, records.size)
        assertSame(defaultRecord, records.getValue(15u))
        assertSame(firstCase, records.getValue(9u))
        assertSame(firstPattern, assertIs<CfirSourceExpressionPayload.MatchCase>(records.getValue(9u).payload).patterns.single())
        val body = assertNotNull(fixture.context.createDeclDeserializer().deserializeFunctionBody(0))
        val match = assertIs<CfirNativeMatchExpression>(body.statements[3])
        val ordinaryPattern = match.cases.single().patterns.single()
        val header = HeaderSnapshot(function)

        repeat(3) {
            val partial = loader.expression(9u)
            val defaultPartial = loader.expression(15u)
            loader.loadHeaders()
            val currentHeader = assertNotNull(fixture.context.completedHeaderSourceGraph)
            val currentFull = fixture.fullSourceGraph()
            assertEquals(setOf(9u, 13u, 14u), partial.expressionReferences.keys)
            assertEquals(3, partial.expressionReferences.size)
            assertEquals(setOf(15u), defaultPartial.expressionReferences.keys)
            assertEquals(1, defaultPartial.expressionReferences.size)
            val views = listOf(firstHeader, firstPartial, firstFull, partial, defaultPartial, currentHeader, currentFull)
            views.forEach { view ->
                view.expressionReferences.forEach { (wire, identity) ->
                    assertSame(firstFull.expressionReferences.getValue(wire), identity)
                    assertSame(records.getValue(wire), view.graph.expression(identity))
                }
            }
            for (view in listOf(firstPartial, firstFull, partial, currentFull)) {
                val record = view.graph.expression(view.expressionReferences.getValue(9u))
                assertSame(firstCase, record)
                assertSame(firstPattern, assertIs<CfirSourceExpressionPayload.MatchCase>(record.payload).patterns.single())
            }
            assertSame(firstFull.expressionReferences.getValue(10u), firstPattern.contextExpression)
            assertSame(match.selector, ordinaryPattern.contextExpression)
            assertSame(ordinaryPattern, match.cases.single().patterns.single())
            fixture.context.materialize {
                val state = fixture.context.bodyState
                firstFull.expressionReferences.forEach { (wire, identity) ->
                    assertSame(identity, state.identities.getValue(wire))
                    assertSame(records.getValue(wire), state.records.getValue(wire))
                }
                val position = state.patternIdentities.entries.single { it.value === firstPattern.identity }.key
                assertTrue(position > 0)
                assertSame(firstPattern, state.patternRecords.getValue(position))
                assertSame(ordinaryPattern, state.patterns.getValue(firstPattern.identity))
                assertSame(match.selector, state.expressions.getValue(firstFull.expressionReferences.getValue(10u)))
                assertSame(body, state.bodies.getValue(function))
            }
            header.assertUnchanged(body)
        }
    }

    /** 冷 header 后只选 ref e3；literal e2 通过正式借用准备完成，不认领 initializer 的拥有权。 */
    @Test
    fun coldStandaloneMappedReference(@TempDir root: Path) {
        val fixture = fixture(root, controlSeed())
        val loader = CfirSourceGraphLoader(fixture.context)
        val function = assertIs<CfirNamedFunction>(loader.headerDeclaration(0))
        val functionIdentity = fixture.context.sourceHeaderRegistry.functionByDeclaration.getValue(1u)
        assertEquals(1u, fixture.context.sourceHeaderRegistry.functions.getValue(functionIdentity).body.body)
        loader.content(listOf(CjoSourceContentRoot.DeclarationHeader(1u)))
        assertNull(function.body)
        assertTrue(fixture.context.completedSourceHeaders.getValue(function).bodyAvailable)
        val header = HeaderSnapshot(function)
        fixture.context.materialize {
            assertFalse(fixture.context.bodyState.records.containsKey(2u))
            assertFalse(fixture.context.bodyState.bodies.containsKey(function))
        }

        // 显式 canonical F.body discovery 只登记保存槽与 record，不发布普通 F.body。
        val discovery = loader.content(listOf(CjoSourceContentRoot.FunctionBody(functionIdentity)))
        assertTrue(fixture.context.isPublishedDeclaration(function))
        assertSame(discovery.expressionReferences.getValue(1u), discovery.graph.functionBodies.getValue(function))
        assertNull(function.body)
        fixture.context.materialize {
            assertFalse(fixture.context.bodyState.bodies.containsKey(function))
        }
        header.assertUnchanged()
        val registry = fixture.context.sourceHeaderRegistry
        val initializerOccurrence = registry.expressionContexts.getValue(2u).single()
        assertEquals(CjoSourceOwningParent.Declaration(3u), initializerOccurrence.parent)
        assertEquals(CjoSourceExpressionSlotKind.INITIALIZER, initializerOccurrence.slot.kind)
        assertEquals(functionIdentity, initializerOccurrence.scope.returnFunction)
        val initializerRoot = CjoSourceContentRoot.Initializer(3u)
        val selectedOccurrence = registry.expressionContexts.getValue(3u).single()
        assertEquals(CjoSourceOwningParent.Expression(1u), selectedOccurrence.parent)
        assertEquals(CjoSourceExpressionSlotKind.OPERAND, selectedOccurrence.slot.kind)
        assertEquals(1, selectedOccurrence.slot.ordinal)
        assertEquals(functionIdentity, selectedOccurrence.scope.returnFunction)
        val initializerSource = loader.content(listOf(initializerRoot))
        assertEquals(setOf(2u), initializerSource.expressionReferences.keys)
        assertTrue(initializerSource.graph.functionBodies.isEmpty())
        assertIs<CfirLiteralExpression>(fixture.context.createDeclDeserializer().deserializeVariableInitializer(2))
        assertNull(function.body)
        fixture.context.materialize {
            assertFalse(fixture.context.bodyState.bodies.containsKey(function))
        }
        header.assertUnchanged()
        val selectedRoot = CjoSourceContentRoot.SelectedExpression(selectedOccurrence)
        val selected = loader.content(listOf(selectedRoot))
        CfirBodyDeserializer(fixture.context, fixture.context.createDeclDeserializer())
            .deserializeExpression(3u, selectedRoot)
        assertEquals(setOf(3u), selected.expressionReferences.keys)
        assertEquals(1, selected.expressionReferences.size)
        assertTrue(selected.graph.functionBodies.isEmpty())
        assertTrue(selected.graph.initializers.isEmpty())
        assertTrue(selected.graph.declarationOwners.containsKey(function))
        assertNull(selected.graph.declarationOwners.getValue(function))
        assertNull(selected.graph.functionContexts.getValue(function).declarationContext)
        assertNull(selected.graph.functionContexts.getValue(function).outerFunction)
        assertTrue(function in selected.graph.unloadedFunctionBodies)
        assertSame(selected.functionHeaders.getValue(function), selected.graph.functionHeaders.getValue(function))
        val referenceIdentity = selected.expressionReferences.getValue(3u)
        val record = selected.graph.expression(referenceIdentity)
        val initializerIdentity = assertNotNull(record.mapExpression)
        val local = assertIs<CfirFieldVariable>(assertIs<CfirSourceReferenceTarget.Declaration>(assertIs<CfirSourceExpressionPayload.Reference>(record.payload).target).declaration)
        assertTrue(local.isLocal)
        assertSame(function, selected.graph.declarationOwners.getValue(local))
        assertSame(function, record.context)
        assertEquals(CangjieOverflowStrategy.SATURATING, record.overflowStrategy)
        lateinit var initializer: CfirLiteralExpression
        lateinit var initializerRecord: CfirSourceExpressionRecord
        fixture.context.materialize {
            val state = fixture.context.bodyState
            assertSame(initializerIdentity, state.identities.getValue(2u))
            initializerRecord = state.records.getValue(2u)
            assertSame(local, initializerRecord.context)
            initializer = assertIs<CfirLiteralExpression>(state.expressions.getValue(initializerIdentity))
            assertEquals("17", initializer.value)
            assertSame(ConePrimitiveType.INT64, initializer.coneTypeOrNull)
            assertSame(initializerRecord, state.expressionSources.getValue(2u).expression(initializerIdentity))
            assertSame(record, state.records.getValue(3u))
            assertFalse(state.bodies.containsKey(function))
        }
        assertSame(function, loader.headerDeclaration(0))
        header.assertUnchanged()

        val full = fixture.fullSourceGraph()
        assertSame(referenceIdentity, full.expressionReferences.getValue(3u))
        assertSame(initializerIdentity, full.expressionReferences.getValue(2u))
        assertSame(record, full.graph.expression(referenceIdentity))
        assertSame(initializerRecord, full.graph.expression(initializerIdentity))
        assertSame(initializerIdentity, full.graph.initializers.getValue(local))
        fixture.context.materialize {
            val state = fixture.context.bodyState
            assertSame(initializer, state.expressions.getValue(initializerIdentity))
            assertSame(record, state.records.getValue(3u))
            assertSame(initializerRecord, state.records.getValue(2u))
        }
    }

    /** 冷 break 的 loop 准备必须是真实完整产品；不能以取得外层 F.body 代替 loop 绑定准备。 */
    @Test
    fun coldStandaloneBreak(@TempDir root: Path) {
        val fixture = fixture(root, controlSeed())
        val loader = CfirSourceGraphLoader(fixture.context)
        val function = assertIs<CfirNamedFunction>(loader.headerDeclaration(0))
        val functionIdentity = fixture.context.sourceHeaderRegistry.functionByDeclaration.getValue(1u)
        assertEquals(1u, fixture.context.sourceHeaderRegistry.functions.getValue(functionIdentity).body.body)
        loader.content(listOf(CjoSourceContentRoot.DeclarationHeader(1u)))
        assertNull(function.body)
        assertTrue(fixture.context.completedSourceHeaders.getValue(function).bodyAvailable)
        val header = HeaderSnapshot(function)
        fixture.context.materialize {
            assertFalse(fixture.context.bodyState.records.containsKey(4u))
            assertFalse(fixture.context.bodyState.bodies.containsKey(function))
        }

        // 显式 canonical F.body discovery 只登记保存槽与 record，不发布普通 F.body。
        val discovery = loader.content(listOf(CjoSourceContentRoot.FunctionBody(functionIdentity)))
        assertTrue(fixture.context.isPublishedDeclaration(function))
        assertSame(discovery.expressionReferences.getValue(1u), discovery.graph.functionBodies.getValue(function))
        assertNull(function.body)
        fixture.context.materialize {
            assertFalse(fixture.context.bodyState.bodies.containsKey(function))
        }
        header.assertUnchanged()
        val registry = fixture.context.sourceHeaderRegistry
        val loopOccurrence = registry.expressionContexts.getValue(4u).single()
        assertEquals(CjoSourceOwningParent.Expression(1u), loopOccurrence.parent)
        assertEquals(CjoSourceExpressionSlotKind.OPERAND, loopOccurrence.slot.kind)
        assertEquals(2, loopOccurrence.slot.ordinal)
        assertEquals(functionIdentity, loopOccurrence.scope.returnFunction)
        val loopRoot = CjoSourceContentRoot.SelectedExpression(loopOccurrence)
        val loopSource = loader.content(listOf(loopRoot))
        assertEquals(setOf(4u, 5u, 6u, 7u), loopSource.expressionReferences.keys)
        assertTrue(loopSource.graph.functionBodies.isEmpty())
        assertIs<CfirLoopExpression>(CfirBodyDeserializer(fixture.context, fixture.context.createDeclDeserializer())
            .deserializeExpression(4u, loopRoot))
        val selectedOccurrence = registry.expressionContexts.getValue(7u).single()
        assertEquals(CjoSourceOwningParent.Expression(6u), selectedOccurrence.parent)
        assertEquals(CjoSourceExpressionSlotKind.OPERAND, selectedOccurrence.slot.kind)
        assertEquals(0, selectedOccurrence.slot.ordinal)
        assertEquals(functionIdentity, selectedOccurrence.scope.returnFunction)
        assertNull(function.body)
        fixture.context.materialize {
            assertFalse(fixture.context.bodyState.bodies.containsKey(function))
        }
        header.assertUnchanged()
        val selectedRoot = CjoSourceContentRoot.SelectedExpression(selectedOccurrence)
        val selected = loader.content(listOf(selectedRoot))
        CfirBodyDeserializer(fixture.context, fixture.context.createDeclDeserializer())
            .deserializeExpression(7u, selectedRoot)
        assertEquals(setOf(7u), selected.expressionReferences.keys)
        assertEquals(1, selected.expressionReferences.size)
        assertTrue(selected.graph.functionBodies.isEmpty())
        assertTrue(selected.graph.declarationOwners.containsKey(function))
        assertNull(selected.graph.declarationOwners.getValue(function))
        assertNull(selected.graph.functionContexts.getValue(function).declarationContext)
        assertNull(selected.graph.functionContexts.getValue(function).outerFunction)
        assertTrue(function in selected.graph.unloadedFunctionBodies)
        assertSame(selected.functionHeaders.getValue(function), selected.graph.functionHeaders.getValue(function))
        val jumpIdentity = selected.expressionReferences.getValue(7u)
        val record = selected.graph.expression(jumpIdentity)
        assertSame(function, record.context)
        val payload = assertIs<CfirSourceExpressionPayload.Jump>(record.payload)
        assertTrue(payload.isBreak)
        assertNull(record.mapExpression)
        val loopIdentity = payload.loop
        lateinit var loop: CfirLoopExpression
        lateinit var jump: CfirBreakExpression
        lateinit var loopRecord: CfirSourceExpressionRecord
        fixture.context.materialize {
            val state = fixture.context.bodyState
            assertSame(loopIdentity, state.identities.getValue(4u))
            loopRecord = state.records.getValue(4u)
            assertSame(function, loopRecord.context)
            val target = state.loops.getValue(loopIdentity)
            assertTrue(target.isBound)
            loop = assertIs<CfirLoopExpression>(target.labeledElement)
            assertSame(loop, state.expressions.getValue(loopIdentity))
            assertFalse(loop.isDoWhile)
            assertEquals(false, assertIs<CfirLiteralExpression>(loop.condition).value)
            assertSame(ConePrimitiveType.UNIT, loop.coneTypeOrNull)
            jump = assertIs<CfirBreakExpression>(loop.body.statements.single())
            assertSame(jump, state.expressions.getValue(jumpIdentity))
            assertSame(target, jump.target)
            assertNull(jump.mappedExpression)
            assertSame(loopRecord, state.expressionSources.getValue(4u).expression(loopIdentity))
            assertSame(record, state.records.getValue(7u))
            assertFalse(state.bodies.containsKey(function))
        }
        val target = jump.target
        assertSame(function, loader.headerDeclaration(0))
        header.assertUnchanged()

        val body = assertNotNull(fixture.context.createDeclDeserializer().deserializeFunctionBody(0))
        assertSame(loop, assertIs<CfirLoopExpression>(body.statements[2]))
        assertSame(jump, assertIs<CfirBreakExpression>(loop.body.statements.single()))
        assertSame(target, jump.target)
        assertSame(loop, target.labeledElement)
        fixture.context.materialize {
            val state = fixture.context.bodyState
            assertSame(record, state.records.getValue(7u))
            assertSame(loopRecord, state.records.getValue(4u))
            assertSame(target, state.loops.getValue(loopIdentity))
            assertSame(body, state.bodies.getValue(function))
        }
        header.assertUnchanged(body)
        val full = fixture.fullSourceGraph()
        assertSame(record, full.graph.expression(full.expressionReferences.getValue(7u)))
        assertSame(loopRecord, full.graph.expression(full.expressionReferences.getValue(4u)))
    }

    /** 有限 schema 的 map 借用环；不声称官方源码或 writer 产出过此 cycle。 */
    @Test
    fun cyclicSchemaMapBindingsKeepTheirOwningSelection(@TempDir root: Path) {
        val original = controlSeed()
        assertEquals(0u, original.expressions[1].mapExpression)
        assertEquals(2u, original.expressions[2].mapExpression)
        assertEquals(1u, original.expressions[1].type)
        assertEquals(1u, original.expressions[2].type)
        val seed = original.copy(expressions = original.expressions.mapIndexed { index, expression ->
            if (index == 1) expression.copy(mapExpression = 3u) else expression
        })
        val fixture = fixture(root, seed)
        val loader = CfirSourceGraphLoader(fixture.context)
        val function = assertIs<CfirNamedFunction>(loader.headerDeclaration(0))
        val functionIdentity = fixture.context.sourceHeaderRegistry.functionByDeclaration.getValue(1u)
        assertEquals(1u, fixture.context.sourceHeaderRegistry.functions.getValue(functionIdentity).body.body)
        loader.content(listOf(CjoSourceContentRoot.DeclarationHeader(1u)))
        assertNull(function.body)
        assertTrue(fixture.context.completedSourceHeaders.getValue(function).bodyAvailable)
        val header = HeaderSnapshot(function)
        fixture.context.materialize {
            assertFalse(fixture.context.bodyState.records.containsKey(2u))
            assertFalse(fixture.context.bodyState.records.containsKey(3u))
            assertFalse(fixture.context.bodyState.bodies.containsKey(function))
        }

        // 合法借用环直接完成；StackOverflow/递归重入异常均使本正例失败，不捕获或改写预期。
        // 显式 canonical F.body discovery 只登记保存槽与 record，不发布普通 F.body。
        val discovery = loader.content(listOf(CjoSourceContentRoot.FunctionBody(functionIdentity)))
        assertTrue(fixture.context.isPublishedDeclaration(function))
        assertSame(discovery.expressionReferences.getValue(1u), discovery.graph.functionBodies.getValue(function))
        assertNull(function.body)
        fixture.context.materialize {
            assertFalse(fixture.context.bodyState.bodies.containsKey(function))
        }
        header.assertUnchanged()
        val registry = fixture.context.sourceHeaderRegistry
        val initializerOccurrence = registry.expressionContexts.getValue(2u).single()
        assertEquals(CjoSourceOwningParent.Declaration(3u), initializerOccurrence.parent)
        assertEquals(CjoSourceExpressionSlotKind.INITIALIZER, initializerOccurrence.slot.kind)
        assertEquals(functionIdentity, initializerOccurrence.scope.returnFunction)
        val initializerRoot = CjoSourceContentRoot.Initializer(3u)
        val selectedOccurrence = registry.expressionContexts.getValue(3u).single()
        assertEquals(CjoSourceOwningParent.Expression(1u), selectedOccurrence.parent)
        assertEquals(CjoSourceExpressionSlotKind.OPERAND, selectedOccurrence.slot.kind)
        assertEquals(1, selectedOccurrence.slot.ordinal)
        assertEquals(functionIdentity, selectedOccurrence.scope.returnFunction)
        // B2 前置：ordinary roots-list 入口消费这两个显式 owning 槽；不得自动补借用产品。
        val mapRoot = CjoSourceContentRoot.SelectedExpression(selectedOccurrence)
        val mapRoots = listOf(initializerRoot, mapRoot)
        val mapSource = loader.content(mapRoots)
        assertEquals(setOf(2u, 3u), mapSource.expressionReferences.keys)
        assertTrue(mapSource.graph.functionBodies.isEmpty())
        assertIs<CfirLiteralExpression>(CfirBodyDeserializer(fixture.context, fixture.context.createDeclDeserializer())
            .deserializeExpressions(mapRoots).getValue(2u))
        assertNull(function.body)
        fixture.context.materialize {
            assertFalse(fixture.context.bodyState.bodies.containsKey(function))
        }
        header.assertUnchanged()
        val selectedRoot = CjoSourceContentRoot.SelectedExpression(selectedOccurrence)
        val selected = loader.content(listOf(selectedRoot))
        CfirBodyDeserializer(fixture.context, fixture.context.createDeclDeserializer())
            .deserializeExpression(3u, selectedRoot)
        assertEquals(setOf(3u), selected.expressionReferences.keys)
        assertEquals(1, selected.expressionReferences.size)
        assertTrue(selected.graph.functionBodies.isEmpty())
        assertTrue(selected.graph.initializers.isEmpty())
        assertTrue(selected.graph.declarationOwners.containsKey(function))
        assertNull(selected.graph.declarationOwners.getValue(function))
        assertNull(selected.graph.functionContexts.getValue(function).declarationContext)
        assertNull(selected.graph.functionContexts.getValue(function).outerFunction)
        assertTrue(function in selected.graph.unloadedFunctionBodies)
        assertSame(selected.functionHeaders.getValue(function), selected.graph.functionHeaders.getValue(function))
        val referenceIdentity = selected.expressionReferences.getValue(3u)
        val referenceRecord = selected.graph.expression(referenceIdentity)
        val literalIdentity = assertNotNull(referenceRecord.mapExpression)
        val local = assertIs<CfirFieldVariable>(assertIs<CfirSourceReferenceTarget.Declaration>(assertIs<CfirSourceExpressionPayload.Reference>(referenceRecord.payload).target).declaration)
        assertSame(function, selected.graph.declarationOwners.getValue(local))
        assertSame(function, referenceRecord.context)
        lateinit var literalRecord: CfirSourceExpressionRecord
        lateinit var literal: CfirLiteralExpression
        lateinit var reference: CfirNamedAccessExpression
        fixture.context.materialize {
            val state = fixture.context.bodyState
            assertSame(referenceIdentity, state.identities.getValue(3u))
            assertSame(literalIdentity, state.identities.getValue(2u))
            literalRecord = state.records.getValue(2u)
            assertSame(local, literalRecord.context)
            assertSame(referenceIdentity, literalRecord.mapExpression)
            assertSame(referenceRecord, state.records.getValue(3u))
            assertSame(literalRecord, state.expressionSources.getValue(2u).expression(literalIdentity))
            assertSame(referenceRecord, state.expressionSources.getValue(3u).expression(referenceIdentity))
            literal = assertIs<CfirLiteralExpression>(state.expressions.getValue(literalIdentity))
            reference = assertIs<CfirNamedAccessExpression>(state.expressions.getValue(referenceIdentity))
            assertEquals("17", literal.value)
            assertSame(ConePrimitiveType.INT64, literal.coneTypeOrNull)
            assertSame(ConePrimitiveType.INT64, reference.coneTypeOrNull)
            assertSame(reference, literal.mappedExpression)
            assertSame(literal, reference.mappedExpression)
            assertReference(reference, local.symbol)
            assertFalse(state.bodies.containsKey(function))
        }
        assertSame(function, loader.headerDeclaration(0))
        header.assertUnchanged()
        repeat(2) {
            val repeated = loader.content(listOf(selectedRoot))
            assertEquals(setOf(3u), repeated.expressionReferences.keys)
            assertSame(referenceIdentity, repeated.expressionReferences.getValue(3u))
            assertSame(referenceRecord, repeated.graph.expression(referenceIdentity))
            header.assertUnchanged()
        }

        val full = fixture.fullSourceGraph()
        assertSame(referenceIdentity, full.expressionReferences.getValue(3u))
        assertSame(literalIdentity, full.expressionReferences.getValue(2u))
        assertSame(referenceRecord, full.graph.expression(referenceIdentity))
        assertSame(literalRecord, full.graph.expression(literalIdentity))
        assertSame(referenceIdentity, literalRecord.mapExpression)
        assertSame(literalIdentity, referenceRecord.mapExpression)
        fixture.context.materialize {
            val state = fixture.context.bodyState
            assertSame(literal, state.expressions.getValue(literalIdentity))
            assertSame(reference, state.expressions.getValue(referenceIdentity))
            assertSame(reference, literal.mappedExpression)
            assertSame(literal, reference.mappedExpression)
            assertSame(literalRecord, state.records.getValue(2u))
            assertSame(referenceRecord, state.records.getValue(3u))
        }
    }

    /** controlSeed的Match selector是原e10，纯F discovery不绑定或读取case正文。 */
    private fun prepareCaseSelector(fixture: Fixture, loader: CfirSourceGraphLoader, function: CfirNamedFunction) {
        loader.discoverFunctionHeaders(function)
        val occurrence = fixture.context.sourceHeaderRegistry.expressionContexts.getValue(10u).single()
        val root = CjoSourceContentRoot.SelectedExpression(occurrence)
        val selector = assertIs<CfirNamedAccessExpression>(CfirBodyDeserializer(fixture.context, fixture.context.createDeclDeserializer()).deserializeExpressions(listOf(root)).getValue(10u))
        assertReference(selector, function.valueParameters.single().symbol)
        assertNull(function.body)
    }

    private fun assertReference(expression: CfirExpression, symbol: CfirBasedSymbol<*>) {
        val access = assertIs<CfirNamedAccessExpression>(expression)
        assertSame(symbol, assertIs<CfirResolvedNamedReference>(access.calleeReference).resolvedSymbol)
        assertSame(symbol.cfir, assertIs<CfirResolvedNamedReference>(access.calleeReference).resolvedSymbol.cfir)
    }

    private fun ordinaryElements(root: CfirElement): List<CfirElement> = buildList {
        root.accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                add(element)
                element.acceptChildren(this)
            }
        })
    }

    /** 不只比较值；失败请求不得改写旧 header、参数/default、来源或分析状态对象。 */
    private class HeaderSnapshot(private val function: CfirNamedFunction) {
        private val symbol = function.symbol
        private val status = function.status
        private val type = function.returnTypeRef
        private val parameters = function.valueParameters
        private val defaults = parameters.map { it.defaultValue }
        private val parameterTypes = parameters.map { it.returnTypeRef }
        private val annotations = function.annotations.toList()
        private val annotationStorage = annotationStorage(function.annotations)
        private val facts = function.exportFacts
        private val origin = function.cjoDeclarationOrigin
        private val resolveState = function.resolveState
        private val cfg = function.controlFlowGraphReference
        private val body = function.body

        fun assertUnchanged(expectedBody: CfirBlock? = body) {
            assertSame(symbol, function.symbol)
            assertSame(function, symbol.cfir)
            assertSame(status, function.status)
            assertSame(type, function.returnTypeRef)
            assertSame(parameters, function.valueParameters)
            parameters.forEachIndexed { index, parameter ->
                assertSame(defaults[index], parameter.defaultValue)
                assertSame(parameterTypes[index], parameter.returnTypeRef)
                assertSame(symbol, parameter.containingDeclarationSymbol)
                assertSame(parameter, parameter.symbol.cfir)
            }
            val currentAnnotations = function.annotations
            assertSame(annotationStorage, annotationStorage(currentAnnotations))
            assertEquals(annotations.size, currentAnnotations.size)
            annotations.forEachIndexed { index, annotation ->
                assertSame(annotation, currentAnnotations[index])
            }
            assertSame(facts, function.exportFacts)
            assertSame(origin, function.cjoDeclarationOrigin)
            assertSame(resolveState, function.resolveState)
            assertSame(cfg, function.controlFlowGraphReference)
            assertSame(expectedBody, function.body)
        }

        // List getter 会装箱 value class；通过已有公开 JVM 解包方法读取真实 List 存储。
        private fun annotationStorage(annotations: List<CfirAnnotation>): List<*>? =
            MutableOrEmptyList::class.java.getMethod("unbox-impl")
                .invoke(assertIs<MutableOrEmptyList<CfirAnnotation>>(annotations)) as List<*>?
    }

    private class Fixture(val context: CfirDeserializationContext, val provider: CfirDeserializedSymbolProvider) {
        fun declaration(index: Int): CfirDeclaration = assertNotNull(context.createDeclDeserializer().deserializeDecl(index))
        fun function(): CfirNamedFunction = assertIs<CfirNamedFunction>(declaration(0))
        fun fullSourceGraph(): CfirDeserializedSourceGraph = CfirSourceGraphLoader(context).load()
    }

    /** 使用正式 Manager snapshot；每次调用重建库图，所有跨包类型仍交给正式 provider。 */
    private fun fixture(root: Path, seed: CjoPackageMetadata, core: ByteArray? = null,
        bytes: ByteArray = CjoPackageWriter.toByteArray(seed)): Fixture {
        Files.createDirectories(root)
        if (core != null) Files.write(root.resolve("std.core.cjo"), core)
        val path = root.resolve(seed.fullPackageName + ".cjo").toAbsolutePath().normalize()
        Files.write(path, bytes)
        val manager = CjoManager(CjoSearchPath(additionalLibrarySearchPaths = listOf(root.toFile()), envProvider = { null }),
            CjoFormatVersionPolicy.Compilation(LanguageVersionSettingsImpl.DEFAULT), explicitCommonPartPaths = listOf(path))
        val module = Module()
        val provider = CfirDeserializedSymbolProvider(module.session, manager, CfirCangJieScopeProvider(), module)
        module.session.register(CfirProvider::class, CfirLibrarySessionProvider(provider))
        module.session.register(CfirSymbolProvider::class, provider)
        module.session.register(CfirExtendProvider::class, CfirExtendProviderComposer.fromSymbolProviders(listOf(provider)))
        val snapshot = assertNotNull(manager.loadPackageSnapshot(seed.fullPackageName))
        assertEquals(path, snapshot.sourcePath)
        val context = CfirDeserializationContext(snapshot.pkg, snapshot.header, module, manager, sourcePath = path,
            declarationResolver = { sourcePath, packageName, index -> provider.deserializeCjoDeclaration(sourcePath, packageName, index) },
            dependencySnapshotResolver = { provider.selectImportedSnapshot(module, it) }, loadedPackage = snapshot)
        return Fixture(context, provider)
    }

    /** e1 的混合 Block 拥有 local、loop、Match/P 和 Return；e15 只属于参数默认值。 */
    private fun controlSeed(): CjoPackageMetadata = metadata("body.control", listOf(
        function("select", 5u, 1u, 1u, parameters = listOf(2u), desugars = listOf(4u)),
        parameter("value", 1u, 15u),
        CjoPackageDeclaration("local", DeclKind.VarDecl, isTopLevel = false, type = 1u, info = CjoVariableInfo(initializer = 2u)),
        function("default_value", 6u, 1u, 0u, topLevel = false),
    ), listOf(CjoTypeMetadata(TypeKind.Int64), CjoTypeMetadata(TypeKind.Bool), CjoTypeMetadata(TypeKind.Unit),
        CjoTypeMetadata(TypeKind.Nothing), functionType(listOf(1u), 1u), functionType(emptyList(), 1u)), listOf(
        mixedBlock(4u, listOf(3u, 3u, 4u, 8u, 11u), listOf(false, true, true, true, true)), // e1
        integer("17"), // e2: local.initializer
        reference("local", 3u).copy(mapExpression = 2u, overflowPolicy = OverflowPolicy.Saturating), // e3
        CjoExpressionMetadata(ExprKind.WhileExpr, type = 3u, operands = listOf(5u, 6u)), // e4
        CjoExpressionMetadata(ExprKind.LitConstExpr, type = 2u, literal = CjoLiteralExpressionMetadata("false", LitConstKind.Bool)),
        block(4u, listOf(7u)), // e6: 尾 Jump 的 Nothing，不从外层 loop 的 Unit 猜类型
        CjoExpressionMetadata(ExprKind.JumpExpr, type = 4u, mapExpression = 4u, additionalInfo = CjoAdditionalExpressionInfoMetadata.Jump(true)),
        CjoExpressionMetadata(ExprKind.MatchExpr, type = 1u, operands = listOf(10u, 9u), additionalInfo = CjoAdditionalExpressionInfoMetadata.Match(true)),
        CjoExpressionMetadata(ExprKind.MatchCase, type = 1u, operands = listOf(0u, 13u),
            additionalInfo = CjoAdditionalExpressionInfoMetadata.MatchCase(listOf(CjoPatternMetadata(PatternKind.WildcardPattern,
                types = listOf(1u), begin = point(9), end = point(9, 3))))),
        reference("value", 2u), // e10
        CjoExpressionMetadata(ExprKind.ReturnExpr, type = 4u, operands = listOf(12u)),
        reference("local", 3u), // e12
        block(1u, listOf(14u)), // e13: 坏正文 case 的真实非零 Block
        integer("41"), integer("19"),
    ))

    /** owning e1 与被 gate 掉的 named e14 分域；两层 F 的 Return 各自借用最近真实 F。 */
    private fun owningSeed(annotation: Boolean, core: ByteArray? = null, stringKind: UByte = StringKind.Normal): CjoPackageMetadata {
        val literal = if (core == null) integer("17") else CjoExpressionMetadata(ExprKind.LitConstExpr, type = 1u,
            literal = CjoLiteralExpressionMetadata(STRING_TEXT, LitConstKind.String, stringKind))
        return metadata("body.owning", listOf(
            function("header", 2u, 1u, 14u, parameters = listOf(2u), desugars = listOf(3u)).copy(
                annotations = if (annotation) listOf(CjoAnnotationMetadata(AnnoKind.Annotation, "Annotation",
                    listOf(CjoAnnotationArgumentMetadata(null, 1u)))) else emptyList()),
            parameter("value", 1u, if (annotation) 11u else 1u),
            function("default_value", 3u, 1u, 0u, topLevel = false),
            CjoPackageDeclaration("local", DeclKind.VarDecl, isTopLevel = false, type = 1u, info = CjoVariableInfo(initializer = 2u)),
            CjoPackageDeclaration("inner", DeclKind.FuncParam, isTopLevel = false, type = 1u,
                info = CjoParameterInfo(isNamed = false, isMember = false)),
        ), listOf(if (core == null) CjoTypeMetadata(TypeKind.Int64) else stringType(core),
            functionType(listOf(1u), 1u), functionType(emptyList(), 1u), CjoTypeMetadata(TypeKind.Nothing)), listOf(
            mixedBlock(1u, listOf(4u, 3u, 4u, 10u), listOf(false, true, true, true)), // e1
            literal, reference("local", 4u), // e2/e3
            CjoExpressionMetadata(ExprKind.LambdaExpr, type = 2u, additionalInfo = CjoAdditionalExpressionInfoMetadata.Lambda(
                CjoFunctionBodyInfo(listOf(listOf(5u)), listOf(listOf(0u)), returnType = 1u, body = 5u), supportMock = true)),
            block(4u, listOf(6u, 9u)), // e5: outer lambda body
            CjoExpressionMetadata(ExprKind.LambdaExpr, type = 3u, additionalInfo = CjoAdditionalExpressionInfoMetadata.Lambda(
                CjoFunctionBodyInfo(listOf(emptyList()), listOf(emptyList()), returnType = 1u, body = 7u), supportMock = false)),
            block(4u, listOf(8u)), // e7: nested lambda body
            CjoExpressionMetadata(ExprKind.ReturnExpr, type = 4u, operands = listOf(12u)),
            CjoExpressionMetadata(ExprKind.ReturnExpr, type = 4u, operands = listOf(13u)),
            reference("local", 4u), literal.copy(), reference("local", 4u), reference("inner", 5u),
            block(4u, listOf(15u)), // e14: named body 不因 annotation/default 顺带读取
            CjoExpressionMetadata(ExprKind.ReturnExpr, type = 4u, operands = listOf(16u)), reference("value", 2u),
        )).copy(imports = if (core == null) emptyList() else listOf("std.core"))
    }

    private fun metadata(pkg: String, declarations: List<CjoPackageDeclaration>, types: List<CjoTypeMetadata>,
        expressions: List<CjoExpressionMetadata>): CjoPackageMetadata = CjoPackageMetadata(pkg, pkg,
        schemaProfile = CjoSchemaProfile.OFFICIAL_CJMP, allFiles = listOf("body-schema-seed.cj"),
        declarations = declarations.map { it.copy(attributes = addFlag(it.attributes, Attribute.FROM_COMMON_PART)) },
        types = types, expressions = expressions.mapIndexed { index, expression ->
            expression.copy(begin = point(index + 1), end = point(index + 1, 3))
        }, sourceEncodingContract = CjoSourceEncodingContract.TYPED_SCHEMA)

    private fun function(name: String, type: UInt, result: UInt, body: UInt, parameters: List<UInt> = emptyList(),
        desugars: List<UInt> = emptyList(), topLevel: Boolean = true) = CjoPackageDeclaration(name, type = type,
        isTopLevel = topLevel, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
        info = CjoFunctionInfo(body = CjoFunctionBodyInfo(listOf(parameters), listOf(desugars), returnType = result, body = body)))

    private fun parameter(name: String, type: UInt, default: UInt) = CjoPackageDeclaration(name, DeclKind.FuncParam,
        isTopLevel = false, type = type, attributes = flags(Attribute.HAS_INITIAL),
        info = CjoParameterInfo(isNamed = true, isMember = false, defaultValue = default))

    private fun functionType(parameters: List<UInt>, result: UInt) = CjoTypeMetadata(TypeKind.Func, parameters,
        semanticInfo = CjoFunctionTypeInfoMetadata(result, false, false))

    private fun integer(text: String) = CjoExpressionMetadata(ExprKind.LitConstExpr, type = 1u,
        literal = CjoLiteralExpressionMetadata(text, LitConstKind.Integer))

    private fun reference(name: String, declaration: UInt) = CjoExpressionMetadata(ExprKind.RefExpr, type = 1u,
        reference = CjoReferenceExpressionMetadata(name, CjoExpressionTargetMetadata(PackageIndex.CURRENT.value, index = declaration)))

    private fun block(type: UInt, expressions: List<UInt>) = mixedBlock(type, expressions, List(expressions.size) { true })
    private fun mixedBlock(type: UInt, slots: List<UInt>, isExpr: List<Boolean>) = CjoExpressionMetadata(ExprKind.Block,
        type = type, operands = slots, additionalInfo = CjoAdditionalExpressionInfoMetadata.Block(isExpr + false))
    private fun point(line: Int, column: Int = 2) = CjoPositionMetadata(file = 1u, line = line, column = column)

    private fun officialCore(): ByteArray = assertNotNull(javaClass.classLoader.getResourceAsStream(
        "cjo-sdk/windows_x86_64_cjnative/std/std.core.cjo")).use { it.readBytes() }

    private fun stringType(core: ByteArray): CjoTypeMetadata {
        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(core))
        val declaration = (0 until pkg.allDeclsLength).map { assertNotNull(pkg.allDecls(it)) }
            .single { it.isTopLevel && it.identifier == "String" }
        val type = assertNotNull(pkg.allTypes(declaration.type.toInt() - 1))
        assertEquals(TypeKind.Struct, type.kind)
        return CjoTypeMetadata(type.kind, semanticInfo = CjoCompositeTypeInfoMetadata(0u, 0,
            declarationKey = assertNotNull(declaration.exportId)))
    }

    private fun flags(vararg attributes: Attribute): List<ULong> = attributes.fold(List(2) { 0uL }, ::addFlag)
    private fun addFlag(words: List<ULong>, flag: Attribute): List<ULong> = MutableList(maxOf(2, words.size)) {
        words.getOrElse(it) { 0uL }
    }.apply { this[flag.ordinal / 64] = this[flag.ordinal / 64] or (1uL shl (flag.ordinal % 64)) }

    /** 与现有 Library fixture 相同的语言/type/supertype 组件，所有 symbol 由正式 reader 绑定。 */
    private class Module : CfirModuleData() {
        override val name = Name.identifier("body-deserializer-integration")
        override val dependencies = emptyList<CfirModuleData>()
        override val refinementDependencies = emptyList<CfirModuleData>()
        override val allRefinementDependencies = emptyList<CfirModuleData>()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform = CfirPlatform.DEFAULT
        override val isCommon = false
        override val stableModuleName = "body-deserializer-integration"
        override val session = object : CfirSession(Kind.Library) {}.also {
            it.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
            it.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent())
            it.register(CfirExportAnalysisSettings::class, CfirExportAnalysisSettings())
            it.register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
            it.register(TypeComponents::class, TypeComponents(it))
            it.register(CfirLazyDeclarationResolver::class, CfirDummyCompilerLazyDeclarationResolver)
            it.registerTypeAwareSupertypeComponents()
        }
        init {
            bindSession(session)
            session.register(CfirModuleData::class, this)
        }
    }

    /** 只破坏本测试有限 seed 的 union tag；不替代生产 decoder 或构造语义图。 */
    private class WireSlots : Table() {
        fun at(position: Int, buffer: ByteBuffer): WireSlots = apply { __reset(position, buffer) }
        fun expressionPosition(ordinal: Int): Int = __indirect(__vector(__offset(22)) + ordinal * 4)
        fun expressionInfoTypeSlot(): Int = bb_pos + __offset(18).also { check(it != 0) }
    }

    private companion object { const val STRING_TEXT = "ready中😀\nraw\\value" }
}
