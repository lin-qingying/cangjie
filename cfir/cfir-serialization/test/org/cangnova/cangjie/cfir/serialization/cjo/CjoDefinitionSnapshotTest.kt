@file:OptIn(ExperimentalUnsignedTypes::class, org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeserializationMode
import org.cangnova.cangjie.test.cjoFixtureModuleDataProvider
import PackageFormat.ClassInfo
import PackageFormat.DeclKind
import PackageFormat.ExprInfo
import PackageFormat.ExprKind
import PackageFormat.FuncInfo
import PackageFormat.LitConstKind
import PackageFormat.Package
import PackageFormat.PatternKind
import PackageFormat.Position
import PackageFormat.TypeKind
import PackageFormat.VarWithPatternInfo
import com.google.flatbuffers.Table
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildClassCopy
import org.cangnova.cangjie.cfir.declarations.cjoDeclarationOrigin
import org.cangnova.cangjie.cfir.resolve.providers.CfirLibrarySessionProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirSourceGraphLoader
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoSourceBindingSite
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoSourceBindingSlot
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoSourceContentRoot
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoSourceTable
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.registerTypeAwareSupertypeComponents
import org.cangnova.cangjie.cfir.source.CfirSourceExpressionPayload
import org.cangnova.cangjie.cfir.source.CfirSourceReferenceTarget
import org.cangnova.cangjie.cfir.source.CfirSourceSlot
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirDummyCompilerLazyDeclarationResolver
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver
import org.cangnova.cangjie.cfir.types.TypeComponents
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * R42 definition scope 的真实 provider 合同；依据 integration-mapping、selection-authority
 * 和 core-file-binding-authority。固定 K 的 body policy 与 N LoadFuncBody 将 header/body 分开。
 * 资源 R 不是运行 SDK H：所有 d/e/F/P 身份从实际 source 和原 canonical 对象取得。
 * 本测试只证明限定选择、原正文/位置与原子发布，不证明 Core output-use 或 FILE 投影闭合。
 */
class CjoDefinitionSnapshotTest {
    @Test
    fun sdkObjectAnyHeadersSelectOnlyTheirOwningClosure(@TempDir root: Path) {
        val core = sdk(root)
        val result = complete(core.fixture, core.request())
        assertSdkHeaderScope(core, result)
        assertNull(core.constructor.body)
        assertSame(result.selection.identity, passive(core.fixture, result.selection.request).selection.identity)
    }

    @Test
    fun explicitConstConstructorBodyKeepsRealSourceAndOldHeaderScopeUnselected(@TempDir root: Path) {
        val core = sdk(root)
        val headerRequest = core.request()
        val header = complete(core.fixture, headerRequest)
        val headerGraph = assertNotNull(header.snapshot.graph)
        val bodyRequest = core.request(listOf(CjoDefinitionContentRequirement(core.constructor, CjoDefinitionContentSlot.FUNCTION_BODY)))
        // 主动完成后统一被动捕获；同 binder 的两个不同 request 不能被去重吞掉。
        val results = CjoExportSourceSnapshots.definitionExportSnapshots(listOf(headerRequest, bodyRequest))
        val currentHeader = results[0]
        val body = results[1]
        assertSame(header.selection.identity, currentHeader.selection.identity)
        assertNotSame(header.snapshot.publicationIdentity, body.snapshot.publicationIdentity)
        assertSame(currentHeader.snapshot.publicationIdentity, body.snapshot.publicationIdentity)
        assertSdkHeaderScope(core, header)
        assertSdkHeaderScope(core, currentHeader)
        assertSame(headerGraph, header.snapshot.graph)
        assertSame(headerGraph, currentHeader.snapshot.graph)
        assertFalse(header.snapshot.graphPublications.containsKey(body.snapshot.graph))
        assertSame(headerGraph, body.snapshot.finalPublishedGraph(headerGraph))
        assertRealConstructorBody(core, body)
        val bodyGraph = assertNotNull(body.snapshot.graph)
        val unit = bodyGraph.expressions.values.single { it.payload is CfirSourceExpressionPayload.Literal }
        val position = body.snapshot.positions.getValue(assertNotNull(assertNotNull(unit.importedPosition).beginIdentity))
        val bodyFile = CjoSavedFileReference(position.pkgId, position.file)
        assertFalse(bodyFile in header.selection.positionFiles, "旧 header scope 不取得 body FILE 资格")
        assertFalse(bodyFile in currentHeader.selection.positionFiles)
        assertFalse(bodyFile in header.snapshot.positionFileBindings, "旧完成代不能被新 FILE 绑定回写")
        assertSame(body.selection.identity, passive(core.fixture, bodyRequest).selection.identity)
    }

    @Test
    fun passiveScopeReadsStayUnderOriginalFenceAndDoNotCompleteANewRequest(@TempDir root: Path) {
        val core = sdk(root)
        val request = core.request()
        val completed = complete(core.fixture, request)
        val owner = assertIs<CjoSourceGraphPublicationOwner>(core.objectDeclaration.contentSource)
        val unpublishedBody = core.request(listOf(CjoDefinitionContentRequirement(core.constructor, CjoDefinitionContentSlot.FUNCTION_BODY)))
        owner.withReadOnlyContent {
            repeat(3) {
                val next = CjoExportSourceSnapshots.completedDefinitionExportSnapshots(listOf(request)).single()
                assertSame(completed.selection.identity, next.selection.identity)
                assertSame(completed.snapshot.publicationIdentity, next.snapshot.publicationIdentity)
                assertSame(completed.snapshot.graph, next.snapshot.graph)
                assertSame(completed.snapshot.graphPublications, next.snapshot.graphPublications)
                assertIs<CjoDefinitionSnapshotResult.Failure>(core.fixture.provider.completedDefinitionExportSnapshotOf(unpublishedBody))
                owner.requirePublicationUnchanged(completed.snapshot.publicationIdentity)
            }
            // 同一真实 owner 的正/负控：被动捕获允许，未完成 scope 的生产入口在 materialize 前拒绝。
            val failure = assertFailsWith<IllegalStateException> {
                core.fixture.provider.definitionExportSnapshotOf(unpublishedBody)
            }
            assertEquals("Imported content materialization is forbidden in a read-only input region", failure.message)
            assertTrue(failure.stackTrace.any { it.methodName.startsWith("materialize") })
            assertSame(completed.snapshot.publicationIdentity, owner.currentPublicationIdentity())
        }
        assertUnchanged(core.fixture, completed)
        assertNull(core.constructor.body)
    }

    @Test
    fun wrongSourceBinderDomainAndCanonicalCopyAreRejected(@TempDir root: Path) {
        val core = sdk(root.resolve("original"))
        val other = sdk(root.resolve("other"))
        val completed = complete(core.fixture, core.request())
        val source = completed.snapshot
        val requests = listOf(
            CjoDefinitionSnapshotRequest(other.fixture.source, source.binderIdentity, source.domain, listOf(core.objectDeclaration), emptyList()),
            CjoDefinitionSnapshotRequest(source.source, Any(), source.domain, listOf(core.objectDeclaration), emptyList()),
            CjoDefinitionSnapshotRequest(source.source, source.binderIdentity, other.fixture.module.session, listOf(core.objectDeclaration), emptyList()),
        )
        requests.forEach { request ->
            assertSame(CjoDefinitionSnapshotResult.NotOwner, core.fixture.provider.definitionExportSnapshotOf(request))
            assertSame(CjoDefinitionSnapshotResult.NotOwner, core.fixture.provider.completedDefinitionExportSnapshotOf(request))
        }
        val copy = buildClassCopy(core.objectDeclaration) { symbol = CfirClassSymbol(core.objectDeclaration.symbol.classId) }
        assertSame(copy, copy.symbol.cfir)
        assertSame(core.objectDeclaration, core.objectDeclaration.symbol.cfir)
        val copyRequest = CjoDefinitionSnapshotRequest(source.source, source.binderIdentity, source.domain, listOf(copy), emptyList())
        assertIs<CjoDefinitionSnapshotResult.Failure>(core.fixture.provider.definitionExportSnapshotOf(copyRequest))
        assertIs<CjoDefinitionSnapshotResult.Failure>(core.fixture.provider.completedDefinitionExportSnapshotOf(copyRequest))
        assertUnchanged(core.fixture, completed)
    }

    @Test
    fun bodyOutsideOwningHeaderClosureFailsWithoutPublishingRequest(@TempDir root: Path) {
        val core = sdk(root)
        val completed = complete(core.fixture, core.request())
        // Any 是真实同 source/binder 的 canonical seed；Object ctor 确实不属于其 owning 闭包。
        val request = core.fixture.request(listOf(core.anyDeclaration), listOf(
            CjoDefinitionContentRequirement(core.constructor, CjoDefinitionContentSlot.FUNCTION_BODY),
        ))
        repeat(2) {
            val failure = assertFailsWith<IllegalStateException> { core.fixture.provider.definitionExportSnapshotOf(request) }
            assertEquals("A definition content request must belong to its actual owning header closure", failure.message)
            assertIs<CjoDefinitionSnapshotResult.Failure>(core.fixture.provider.completedDefinitionExportSnapshotOf(request))
            assertUnchanged(core.fixture, completed)
        }
        assertNull(core.constructor.body)
    }

    @Test
    fun malformedSelectedSdkBodyDoesNotPublishScopeOrChangePriorSuccess(@TempDir root: Path) {
        val bytes = officialCoreBytes()
        val wire = Package.getRootAsPackage(ByteBuffer.wrap(bytes))
        val objectIndex = (0 until wire.allDeclsLength).single {
            val declaration = assertNotNull(wire.allDecls(it))
            declaration.isTopLevel && declaration.kind == DeclKind.ClassDecl && declaration.identifier == "Object"
        }
        val objectInfo = assertIs<ClassInfo>(assertNotNull(wire.allDecls(objectIndex)).info(ClassInfo()))
        val constructorWire = assertNotNull(wire.allDecls(objectInfo.body(0).toInt() - 1))
        val functionInfo = assertIs<FuncInfo>(constructorWire.info(FuncInfo()))
        assertTrue(functionInfo.isConst)
        val bodyReference = assertNotNull(functionInfo.funcBody).body
        assertTrue(bodyReference != 0u)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val packageSlots = CorruptionSlots().at(buffer.getInt(0), buffer)
        val bodySlots = CorruptionSlots().at(packageSlots.expressionPosition(bodyReference.toInt() - 1), buffer)
        // 保留合法 body、owning operands 和类型，只破坏被选 Block 的 union tag。
        buffer.put(bodySlots.infoTagSlot(), ExprInfo.ReferenceInfo.toByte())
        assertEquals(ExprInfo.ReferenceInfo, assertNotNull(wire.allExprs(bodyReference.toInt() - 1)).infoType)
        val core = sdk(root, bytes)
        val header = complete(core.fixture, core.request())
        assertSdkHeaderScope(core, header)
        val request = core.request(listOf(CjoDefinitionContentRequirement(core.constructor, CjoDefinitionContentSlot.FUNCTION_BODY)))
        repeat(2) {
            assertFailsWith<IllegalStateException> { core.fixture.provider.definitionExportSnapshotOf(request) }
            assertIs<CjoDefinitionSnapshotResult.Failure>(core.fixture.provider.completedDefinitionExportSnapshotOf(request))
            assertUnchanged(core.fixture, header)
        }
        assertNull(core.constructor.body)
    }

    @Test
    fun twoActualBindersOfOneLoadedSourceKeepSeparateScopes(@TempDir root: Path) {
        val first = sdk(root)
        val second = sdkDeclarations(Fixture(first.fixture.manager, first.fixture.source))
        assertSame(first.fixture.source, second.fixture.source)
        assertNotSame(first.objectDeclaration, second.objectDeclaration)
        assertNotSame(first.fixture.module.session, second.fixture.module.session)
        val firstRequest = first.request()
        val secondRequest = second.request()
        val results = CjoExportSourceSnapshots.definitionExportSnapshots(listOf(firstRequest, secondRequest))
        val left = results[0]
        val right = results[1]
        assertNotSame(left.snapshot.binderIdentity, right.snapshot.binderIdentity)
        assertNotSame(left.selection.identity, right.selection.identity)
        assertSdkHeaderScope(first, left)
        assertSdkHeaderScope(second, right)
        assertSame(CjoDefinitionSnapshotResult.NotOwner, first.fixture.provider.definitionExportSnapshotOf(secondRequest))
        assertSame(CjoDefinitionSnapshotResult.NotOwner, second.fixture.provider.completedDefinitionExportSnapshotOf(firstRequest))
        val crossed = CjoDefinitionSnapshotRequest(left.snapshot.source, left.snapshot.binderIdentity, left.snapshot.domain,
            listOf(second.objectDeclaration), emptyList())
        assertIs<CjoDefinitionSnapshotResult.Failure>(first.fixture.provider.definitionExportSnapshotOf(crossed))
        val body = complete(first.fixture, first.request(listOf(
            CjoDefinitionContentRequirement(first.constructor, CjoDefinitionContentSlot.FUNCTION_BODY),
        )))
        assertRealConstructorBody(first, body)
        assertUnchanged(second.fixture, right)
        assertNull(second.constructor.body)
    }

    @Test
    fun legalRawFixtureKeepsFunctionPatternBindingSitesAndExplicitInitializer(@TempDir root: Path) {
        val metadata = rawMetadata()
        val path = CjoPackageWriter.write(root.resolve("scope.raw.cjo"), metadata)
        val fixture = fixture(root, path, metadata.fullPackageName)
        val entry = assertIs<CfirFunction>(fixture.declaration(0))
        val value = assertIs<CfirVariable>(fixture.declaration(3))
        // 固定 N 的 VarWithPattern 必须由真实 F.body 拥有，并保存非零 initializer。
        // 此时只查询 entry/value 的 canonical header，不冷查询尚未完成的 local d/P。
        val entryHeaderRequest = fixture.request(listOf(entry, value))
        val entryHeader = complete(fixture, entryHeaderRequest)
        assertEquals(setOf(0, 3), entryHeader.selection.definitions.keys)
        val entryHeaderGraph = assertNotNull(entryHeader.snapshot.graph)
        assertTrue(entryHeader.selection.patterns.isEmpty())
        assertTrue(entryHeaderGraph.functionBodies.isEmpty())
        assertTrue(entryHeaderGraph.initializers.isEmpty())
        val functionPosition = assertIs<CjoSourceTable>(assertNotNull(fixture.source.pkg.allDecls(0)).info(CjoSourceTable())).tablePosition(4)
        assertSame(entry, entryHeader.selection.functions.getValue(functionPosition))
        val request = fixture.request(listOf(entry, value), listOf(
            CjoDefinitionContentRequirement(entry, CjoDefinitionContentSlot.FUNCTION_BODY),
            CjoDefinitionContentRequirement(value, CjoDefinitionContentSlot.VARIABLE_INITIALIZER),
        ))
        val result = complete(fixture, request)
        val graph = assertNotNull(result.snapshot.graph)
        assertEquals(setOf(0, 1, 2, 3), result.selection.definitions.keys)
        val patternOwner = assertIs<CfirPatternVariable>(result.snapshot.declarations.getValue(1))
        assertSame(patternOwner, result.selection.definitions.getValue(1))
        assertTrue(patternOwner.isLocal)
        val patternWire = assertNotNull(fixture.source.pkg.allDecls(index(patternOwner)))
        assertFalse(patternWire.isTopLevel)
        val patternInfo = assertIs<VarWithPatternInfo>(patternWire.info(VarWithPatternInfo()))
        assertTrue(patternInfo.initializer != 0u)
        val patternPosition = assertIs<CjoSourceTable>(patternWire.info(CjoSourceTable())).tablePosition(8)
        val patternIdentity = result.selection.patterns.getValue(patternPosition)
        val completedPattern = graph.declarationPatterns.getValue(patternOwner)
        assertSame(patternIdentity, completedPattern.identity)
        assertSame(completedPattern, graph.pattern(patternIdentity))
        assertTrue(result.selection.borrowedPatterns.isEmpty())
        assertTrue(graph.borrowedDeclarationPatterns.isEmpty())
        assertSame(entry, result.selection.owners.getValue(patternOwner))
        assertSame(patternOwner, result.selection.owners.getValue(result.selection.definitions.getValue(2)))
        assertSame(result.selection.definitions.getValue(2),
            assertIs<CfirSourceSlot.Declaration>(completedPattern.slots.single()).declaration)
        assertEquals("11", assertIs<CfirSourceExpressionPayload.Literal>(
            graph.expression(graph.initializers.getValue(patternOwner)).payload).text)
        val body = assertIs<CfirSourceExpressionPayload.Block>(graph.expression(graph.functionBodies.getValue(entry)).payload)
        assertSame(patternOwner, assertIs<CfirSourceSlot.Declaration>(body.statements.first()).declaration)
        val borrowed = result.snapshot.declarations.getValue(4)
        assertSame(borrowed, result.selection.borrowedHeaders.getValue(4))
        assertFalse(result.selection.definitions.values.any { it === borrowed })
        assertSame(entry, result.selection.functions.getValue(functionPosition))
        val bodyRoot = result.selection.roots.filterIsInstance<CjoSourceContentRoot.FunctionBody>().single()
        assertEquals(functionPosition, bodyRoot.function.tablePosition)
        assertEquals(4u, result.selection.roots.filterIsInstance<CjoSourceContentRoot.Initializer>().single().declaration)
        val referenceRecord = graph.expressions.values.single { it.payload is CfirSourceExpressionPayload.Reference }
        val reference = assertIs<CfirSourceExpressionPayload.Reference>(referenceRecord.payload)
        val rawBinding = result.selection.declarationBindings.single {
            it.site is CjoSourceBindingSite.Expression && it.site.slot == CjoSourceBindingSlot.TARGET
        }
        assertEquals(PackageIndex.CURRENT.value, rawBinding.target.pkgId)
        assertEquals(5u, rawBinding.target.index)
        assertSame(entry, referenceRecord.context)
        assertSame(borrowed, assertIs<CfirSourceReferenceTarget.Declaration>(reference.target).declaration)
        val typeBinding = result.selection.typeBindings.single {
            it.site == (rawBinding.site as CjoSourceBindingSite.Expression).copy(slot = CjoSourceBindingSlot.TYPE)
        }
        assertEquals(1u, typeBinding.originalReference)
        assertSame(referenceRecord.type, typeBinding.type)
        assertEquals("7", assertIs<CfirSourceExpressionPayload.Literal>(graph.expression(graph.initializers.getValue(value)).payload).text)
        assertTrue(entryHeaderGraph.functionBodies.isEmpty())
        assertTrue(entryHeaderGraph.initializers.isEmpty())
        assertUnchangedSelection(entryHeader, passive(fixture, entryHeaderRequest))

        // 独立 header scope 只以已发表的 local canonical d 为 seed，显式借用原 CompleteP。
        val headerRequest = fixture.request(listOf(patternOwner))
        val header = complete(fixture, headerRequest)
        val headerGraph = assertNotNull(header.snapshot.graph)
        assertEquals(setOf(index(patternOwner), index(result.selection.definitions.getValue(2))), header.selection.definitions.keys)
        assertSame(patternOwner, header.selection.definitions.getValue(index(patternOwner)))
        assertSame(entry, header.selection.borrowedHeaders.getValue(index(entry)))
        assertFalse(index(entry) in header.selection.definitions)
        assertSame(entry, header.selection.owners.getValue(patternOwner))
        assertSame(patternOwner, header.selection.owners.getValue(result.selection.definitions.getValue(2)))
        assertEquals(setOf(patternPosition), header.selection.patterns.keys)
        assertEquals(setOf(patternPosition), header.selection.borrowedPatterns)
        assertEquals(setOf(patternOwner), headerGraph.borrowedDeclarationPatterns)
        assertSame(patternIdentity, header.selection.patterns.getValue(patternPosition))
        assertSame(completedPattern, headerGraph.pattern(patternIdentity))
        assertSame(completedPattern, headerGraph.declarationPatterns.getValue(patternOwner))
        assertEquals(listOf(CjoSourceContentRoot.DeclarationPatternHeader((index(patternOwner) + 1).toUInt())),
            header.selection.roots.filterIsInstance<CjoSourceContentRoot.DeclarationPatternHeader>())
        assertFalse(header.selection.roots.any { it is CjoSourceContentRoot.FunctionBody || it is CjoSourceContentRoot.Initializer })
        assertTrue(header.selection.functions.isEmpty(), "借用词法 F header 不授原 F 定义/body 选择")
        assertTrue(headerGraph.functionBodies.isEmpty())
        assertTrue(headerGraph.initializers.isEmpty(), "原 local initializer 已完成也不能进入独立 header scope")
        assertTrue(headerGraph.expressions.isEmpty())
        assertTrue(headerGraph.borrowedExpressions.isEmpty(), "此合法 VarPattern 只有 binding d，没有需要借用的 e 槽")
        assertTrue(headerRequest.contents.isEmpty())
        assertSame(graph, result.snapshot.graph)
        assertUnchangedSelection(header, passive(fixture, headerRequest))
    }

    @Test
    fun standalonePatternHeaderContentBorrowsCompletedLocalPatternWithoutOtherRoots(@TempDir root: Path) {
        val context = standalonePatternContext(root)
        val loader = CfirSourceGraphLoader(context)
        val entry = assertIs<CfirFunction>(loader.headerDeclaration(0))
        val functionIdentity = context.sourceFunctionIdentity(entry)
        val bodyRoots = listOf(CjoSourceContentRoot.FunctionBody(functionIdentity))
        val owning = loader.content(bodyRoots)
        val owningGraph = owning.graph
        // local canonical seed 必须来自原 F.body 的成功完成产品；不冷读裸 local d/P。
        val body = assertIs<CfirSourceExpressionPayload.Block>(
            owningGraph.expression(owningGraph.functionBodies.getValue(entry)).payload)
        val local = assertIs<CfirPatternVariable>(assertIs<CfirSourceSlot.Declaration>(body.statements.first()).declaration)
        assertTrue(local.isLocal)
        val reference = (index(local) + 1).toUInt()
        assertSame(local, context.declCache.getValue(index(local)))
        val wire = assertNotNull(context.pkg.allDecls(index(local)))
        assertFalse(wire.isTopLevel)
        val info = assertIs<VarWithPatternInfo>(wire.info(VarWithPatternInfo()))
        assertTrue(info.initializer != 0u)
        val position = assertIs<CjoSourceTable>(wire.info(CjoSourceTable())).tablePosition(8)
        val originalPattern = owningGraph.declarationPatterns.getValue(local)
        val binding = assertIs<CfirSourceSlot.Declaration>(originalPattern.slots.single()).declaration
        assertSame(binding, context.declCache.getValue(index(binding)))
        assertSame(local, owningGraph.declarationOwners.getValue(binding))
        assertSame(entry, owningGraph.declarationOwners.getValue(local))
        val initializer = owningGraph.initializers.getValue(local)
        assertEquals("11", assertIs<CfirSourceExpressionPayload.Literal>(owningGraph.expression(initializer).payload).text)

        // D2 独立入口只传一个 P header 根；不得先加 DeclarationAnnotations 掩盖完成集合缺口。
        val roots = listOf(CjoSourceContentRoot.DeclarationPatternHeader(reference))
        val header = loader.content(roots)
        val graph = header.graph
        assertEquals(roots, header.selection.roots)
        assertEquals(setOf(reference), header.selection.selectedPatternHeaders)
        assertTrue(header.selection.selectedAnnotations.isEmpty())
        assertTrue(header.selection.selectedDefaults.isEmpty())
        assertTrue(header.selection.selectedInitializers.isEmpty())
        assertTrue(header.selection.selectedBodies.isEmpty())
        assertTrue(header.selection.ownedContent.declarations.isEmpty())
        assertTrue(header.selection.ownedContent.functions.isEmpty())
        assertTrue(header.selection.ownedContent.patterns.isEmpty())
        assertTrue(header.selection.ownedContent.expressions.isEmpty())
        assertSame(local, graph.declarationPatterns.keys.single())
        assertSame(originalPattern, graph.declarationPatterns.getValue(local))
        assertSame(originalPattern, graph.pattern(originalPattern.identity))
        assertSame(binding, assertIs<CfirSourceSlot.Declaration>(graph.pattern(originalPattern.identity).slots.single()).declaration)
        assertSame(originalPattern, owningGraph.pattern(originalPattern.identity))
        assertEquals(setOf(local), graph.borrowedDeclarationPatterns)
        assertFalse(local in owningGraph.borrowedDeclarationPatterns)
        assertTrue(position in context.sourceHeaderRegistry.patterns)
        val typeBinding = header.selection.typeBindings.single {
            it.site == CjoSourceBindingSite.Declaration(reference, CjoSourceBindingSlot.TYPE)
        }
        assertEquals(wire.type, typeBinding.reference)
        assertSame(originalPattern.types.single(), context.typeCache.getValue(typeBinding.reference.toInt() - 1))
        assertTrue(graph.functionBodies.isEmpty())
        assertTrue(graph.initializers.isEmpty())
        assertTrue(graph.expressions.isEmpty())
        assertTrue(graph.borrowedExpressions.isEmpty(), "此原 VarPattern 只有 binding d，不借原 initializer 或 body e")
        assertFalse(initializer in graph.expressions)
        assertSame(owningGraph, assertNotNull(context.selectedSourceGraph(bodyRoots)).graph)
        assertSame(graph, assertNotNull(context.selectedSourceGraph(roots)).graph)
        assertSame(originalPattern, assertNotNull(context.selectedSourceGraph(roots)).graph.pattern(originalPattern.identity))
    }

    @Test
    fun standalonePatternHeaderContentRejectsUncompletedLocalPattern(@TempDir root: Path) {
        val context = standalonePatternContext(root)
        val loader = CfirSourceGraphLoader(context)
        val entry = assertIs<CfirFunction>(loader.headerDeclaration(0))
        val bodyRoots = listOf(CjoSourceContentRoot.FunctionBody(context.sourceFunctionIdentity(entry)))
        // 只登记真实 F.body 的 owning metadata，不完成 body/initializer/P。
        loader.discoverFunctionHeaders(entry)
        val bodyWire = assertNotNull(context.pkg.allExprs(assertNotNull(
            assertIs<FuncInfo>(assertNotNull(context.pkg.allDecls(index(entry))).info(FuncInfo())).funcBody).body.toInt() - 1))
        val blockInfo = assertIs<PackageFormat.BlockInfo>(bodyWire.info(PackageFormat.BlockInfo()))
        assertFalse(blockInfo.isExpr(0))
        val localIndex = bodyWire.operands(0).toInt() - 1
        val local = assertIs<CfirPatternVariable>(loader.headerDeclaration(localIndex))
        assertTrue(local.isLocal)
        assertSame(local, context.declCache.getValue(localIndex))
        val wire = assertNotNull(context.pkg.allDecls(localIndex))
        val info = assertIs<VarWithPatternInfo>(wire.info(VarWithPatternInfo()))
        assertTrue(info.initializer != 0u)
        val position = assertIs<CjoSourceTable>(wire.info(CjoSourceTable())).tablePosition(8)
        assertTrue(position in context.sourceHeaderRegistry.patterns, "metadata P 存在不能冒充 CompleteP")
        assertFalse(position in context.bodyState.patternSources)
        assertNull(context.selectedSourceGraph(bodyRoots))
        val roots = listOf(CjoSourceContentRoot.DeclarationPatternHeader((index(local) + 1).toUInt()))
        val owner = assertIs<CjoSourceGraphPublicationOwner>(local.contentSource)
        val publication = owner.currentPublicationIdentity()
        val declarations = context.declCache
        val types = context.typeCache
        repeat(2) {
            val failure = assertFailsWith<IllegalStateException> { loader.content(roots) }
            assertEquals("A scoped P header requires its explicitly completed original owning pattern product", failure.message)
            assertNull(context.selectedSourceGraph(roots))
            assertNull(context.selectedSourceGraph(bodyRoots))
            assertSame(declarations, context.declCache)
            assertSame(types, context.typeCache)
            owner.requirePublicationUnchanged(publication)
        }
    }

    @Test
    fun foreignPositionBindingKeepsActuallySelectedDifferentSource(@TempDir root: Path) {
        val core = sdk(root)
        val objectPosition = assertNotNull(assertNotNull(core.fixture.source.pkg.allDecls(index(core.objectDeclaration))).begin)
        assertEquals(0u, objectPosition.pkgId)
        assertTrue(objectPosition.file != 0u)
        // 外包 file 是目标原 allFiles 的零基 ordinal，取实际来源位置，不写死资源槽。
        val ordinal = objectPosition.file.toInt() - 1
        val position = CjoPositionMetadata(file = ordinal.toUInt(), pkgId = 1u, line = 0, column = 0, ignore = false)
        val original = rawMetadata()
        val metadata = original.copy(imports = listOf(core.fixture.source.header.fullPkgName), expressions =
            original.expressions.mapIndexed { index, expression ->
                if (index == 3) expression.copy(begin = position, end = position) else expression
            })
        val path = CjoPackageWriter.write(root.resolve("scope.raw.cjo"), metadata)
        val fixture = fixture(root, path, metadata.fullPackageName)
        val selectedForeign = assertNotNull(fixture.manager.loadPackageSnapshot(core.fixture.source.header.fullPkgName))
        assertNotSame(fixture.source, selectedForeign)
        val value = assertIs<CfirVariable>(fixture.declaration(3))
        val headerRequest = fixture.request(listOf(value))
        val header = complete(fixture, headerRequest)
        assertTrue(header.selection.positionFiles.isEmpty())
        val result = complete(fixture, fixture.request(listOf(value), listOf(
            CjoDefinitionContentRequirement(value, CjoDefinitionContentSlot.VARIABLE_INITIALIZER),
        )))
        val reference = CjoSavedFileReference(position.pkgId, position.file)
        assertEquals(setOf(reference), result.selection.positionFiles.keys)
        val binding = assertLoadedFileBinding(fixture, result, reference, selectedForeign, ordinal)
        assertFalse(binding.belongsToOriginalContext, "另一个 actual source 必须保留 foreign 身份")
        val graph = assertNotNull(result.snapshot.graph)
        val importedPosition = assertNotNull(graph.expression(graph.initializers.getValue(value)).importedPosition)
        for (identity in listOf(assertNotNull(importedPosition.beginIdentity), assertNotNull(importedPosition.endIdentity))) {
            assertEquals(CjoSavedSourcePosition(position.file, position.pkgId, position.line, position.column, position.ignore),
                result.snapshot.positions.getValue(identity))
        }
        assertTrue(header.selection.positionFiles.isEmpty())
        assertTrue(header.snapshot.positionFileBindings.isEmpty())
        assertUnchangedSelection(header, passive(fixture, headerRequest))
    }

    private fun assertSdkHeaderScope(core: SdkFixture, result: CjoDefinitionSnapshotResult.Completed) {
        assertSame(core.fixture.source, result.snapshot.source)
        assertSame(core.fixture.module.session, result.snapshot.domain)
        assertSame(result.selection.request.source, result.snapshot.source)
        assertSame(result.selection.request.binderIdentity, result.snapshot.binderIdentity)
        assertEquals(setOf(index(core.objectDeclaration), index(core.anyDeclaration), index(core.constructor)), result.selection.definitions.keys)
        for (declaration in listOf(core.objectDeclaration, core.anyDeclaration, core.constructor)) {
            assertSame(declaration, result.selection.definitions.getValue(index(declaration)))
            assertSame(declaration, result.snapshot.declarations.getValue(index(declaration)))
        }
        assertSame(core.objectDeclaration, result.selection.owners.getValue(core.constructor))
        assertEquals(setOf(core.functionPosition), result.selection.functions.keys)
        assertSame(core.constructor, result.selection.functions.getValue(core.functionPosition))
        assertTrue(result.selection.patterns.isEmpty())
        assertTrue(result.selection.definitions.keys.intersect(result.selection.borrowedHeaders.keys).isEmpty())
        assertFalse(result.selection.roots.any { it is CjoSourceContentRoot.FunctionBody || it is CjoSourceContentRoot.Initializer })
        val graph = assertNotNull(result.snapshot.graph)
        assertTrue(graph.expressions.isEmpty(), "限定 header scope 不能装载 whole SDK body")
        assertTrue(graph.functionBodies.isEmpty())
        assertTrue(graph.functionHeaders.getValue(core.constructor).bodyAvailable, "未选择正文不等于原 body=0")
        assertTrue(core.constructor in graph.unloadedFunctionBodies)
        assertEquals(sdkHeaderFiles(core), result.selection.positionFiles.keys)
        result.selection.positionFiles.keys.forEach { reference ->
            assertEquals(0u, reference.packageIndex)
            if (reference.fileIndex == 0u) {
                val binding = assertIs<CjoPositionFileBinding.NoFile>(result.selection.positionFiles.getValue(reference))
                assertSame(core.fixture.source, binding.originalSource)
                assertSame<Any>(core.objectDeclaration.contentSource, binding.originalOwner)
                assertSame(result.snapshot.binderIdentity, binding.originalBinder)
                assertSame(result.snapshot.domain, binding.originalDomain)
                assertSame(binding, result.snapshot.positionFileBindings.getValue(reference))
            } else {
                val binding = assertLoadedFileBinding(core.fixture, result, reference, core.fixture.source, reference.fileIndex.toInt() - 1)
                assertTrue(binding.belongsToOriginalContext)
            }
        }
        result.selection.typeBindings.filter { it.originalReference != 0u }.forEach { binding ->
            assertSame(result.snapshot.types.getValue(binding.originalReference.toInt() - 1), binding.type)
        }
        assertTrue(result.selection.declarationBindings.any {
            it.target.pkgId == PackageIndex.CURRENT.value && it.target.index == (index(core.anyDeclaration) + 1).toUInt()
        }, "原 Object inherited type 必须保留真实 Any binding")
    }

    /** 对原 Block → Return → Unit 的真实产品逐槽核验；FILE 只比较已保存标量，不伪造投影证书。 */
    private fun assertRealConstructorBody(core: SdkFixture, result: CjoDefinitionSnapshotResult.Completed) {
        assertSame(core.fixture.source, result.snapshot.source)
        val graph = assertNotNull(result.snapshot.graph)
        val root = result.selection.roots.filterIsInstance<CjoSourceContentRoot.FunctionBody>().single()
        assertEquals(core.functionPosition, root.function.tablePosition)
        val identity = graph.functionBodies.getValue(core.constructor)
        val blockRecord = graph.expression(identity)
        assertSame(core.constructor, blockRecord.context)
        val block = assertIs<CfirSourceExpressionPayload.Block>(blockRecord.payload)
        val returned = graph.expression(assertIs<CfirSourceSlot.Expression>(block.statements.single()).identity)
        val returnPayload = assertIs<CfirSourceExpressionPayload.Return>(returned.payload)
        assertSame(core.constructor, returnPayload.function)
        val unit = graph.expression(assertNotNull(returnPayload.value))
        assertEquals("()", assertIs<CfirSourceExpressionPayload.Literal>(unit.payload).text)
        assertEquals(3, graph.expressions.size)
        assertEquals(setOf(core.constructor), graph.functionBodies.keys)
        val blockWire = assertNotNull(core.fixture.source.pkg.allExprs(core.bodyReference.toInt() - 1))
        val returnWire = assertNotNull(core.fixture.source.pkg.allExprs(blockWire.operands(0).toInt() - 1))
        val unitWire = assertNotNull(core.fixture.source.pkg.allExprs(returnWire.operands(0).toInt() - 1))
        for ((record, wire) in listOf(blockRecord to blockWire, returned to returnWire, unit to unitWire)) {
            val position = assertNotNull(record.importedPosition)
            assertEquals(saved(assertNotNull(wire.begin)), result.snapshot.positions.getValue(assertNotNull(position.beginIdentity)))
            assertEquals(saved(assertNotNull(wire.end)), result.snapshot.positions.getValue(assertNotNull(position.endIdentity)))
            assertSame(core.constructor, record.context)
        }
        val savedUnit = result.snapshot.positions.getValue(assertNotNull(assertNotNull(unit.importedPosition).beginIdentity))
        assertTrue(savedUnit.pkgId > 0u)
        assertEquals(0, savedUnit.line)
        assertEquals(0, savedUnit.column)
        assertFalse(savedUnit.ignore)
        val references = graph.expressions.values.flatMap { record ->
            val position = assertNotNull(record.importedPosition)
            listOf(assertNotNull(position.beginIdentity), assertNotNull(position.endIdentity)).map { identity ->
                result.snapshot.positions.getValue(identity).let { CjoSavedFileReference(it.pkgId, it.file) }
            }
        }.toSet()
        assertEquals(sdkHeaderFiles(core) + references, result.selection.positionFiles.keys, "scope 只选择真实 header/body 位置槽")
        val unitReference = CjoSavedFileReference(savedUnit.pkgId, savedUnit.file)
        val unitBinding = assertLoadedFileBinding(core.fixture, result, unitReference, core.fixture.source, savedUnit.file.toInt())
        assertTrue(unitBinding.belongsToOriginalContext)
        assertEquals(core.fixture.source.pkg.allFiles(savedUnit.file.toInt()), unitBinding.file.snapshot.pkg.allFiles(unitBinding.file.rawIndex))
        assertSame(graph, result.snapshot.finalPublishedGraph(graph))
    }

    private fun sdkHeaderFiles(core: SdkFixture): Set<CjoSavedFileReference> =
        listOf(core.objectDeclaration, core.anyDeclaration, core.constructor).flatMap { declaration ->
            val wire = assertNotNull(core.fixture.source.pkg.allDecls(index(declaration)))
            listOfNotNull(wire.begin, wire.end, wire.identifierPos).map { CjoSavedFileReference(it.pkgId, it.file) }
        }.toSet()

    private fun assertLoadedFileBinding(fixture: Fixture, result: CjoDefinitionSnapshotResult.Completed,
        reference: CjoSavedFileReference, target: CjoLoadedPackage, ordinal: Int): CjoPositionFileBinding.Loaded {
        val binding = assertIs<CjoPositionFileBinding.Loaded>(result.selection.positionFiles.getValue(reference))
        assertSame(binding, result.snapshot.positionFileBindings.getValue(reference))
        assertSame(fixture.source, binding.originalSource)
        assertSame<Any>(result.selection.request.seeds.first().contentSource, binding.originalOwner)
        assertSame(result.snapshot.binderIdentity, binding.originalBinder)
        assertSame(result.snapshot.domain, binding.originalDomain)
        assertEquals(reference, binding.reference)
        assertSame(target, binding.file.snapshot)
        assertEquals(CjoSnapshotTableDomain.FILE, binding.file.domain)
        assertEquals(ordinal, binding.file.rawIndex)
        assertTrue(ordinal in 0 until target.pkg.allFilesLength)
        assertNotNull(target.pkg.allFiles(ordinal))
        return binding
    }

    private fun saved(position: Position): CjoSavedSourcePosition =
        CjoSavedSourcePosition(position.file, position.pkgId, position.line, position.column, position.ignore)

    private fun assertUnchanged(fixture: Fixture, before: CjoDefinitionSnapshotResult.Completed) {
        val after = passive(fixture, before.selection.request)
        assertSame(before.snapshot.publicationIdentity, after.snapshot.publicationIdentity)
        assertSame(before.snapshot.graphPublications, after.snapshot.graphPublications)
        assertSame(before.snapshot.positionFileBindings, after.snapshot.positionFileBindings)
        assertUnchangedSelection(before, after)
    }

    private fun assertUnchangedSelection(before: CjoDefinitionSnapshotResult.Completed, after: CjoDefinitionSnapshotResult.Completed) {
        assertSame(before.selection.request, after.selection.request)
        assertSame(before.selection.identity, after.selection.identity)
        assertSame(before.snapshot.graph, after.snapshot.graph)
        assertEquals(before.selection.definitions, after.selection.definitions)
        assertEquals(before.selection.borrowedHeaders, after.selection.borrowedHeaders)
        assertEquals(before.selection.roots, after.selection.roots)
        assertEquals(before.selection.functions, after.selection.functions)
        assertEquals(before.selection.patterns, after.selection.patterns)
        assertEquals(before.selection.borrowedPatterns, after.selection.borrowedPatterns)
        assertEquals(before.selection.positionFiles.keys, after.selection.positionFiles.keys)
        before.selection.positionFiles.forEach { (reference, binding) ->
            assertSame(binding, after.selection.positionFiles.getValue(reference))
        }
    }

    private fun complete(fixture: Fixture, request: CjoDefinitionSnapshotRequest): CjoDefinitionSnapshotResult.Completed =
        assertIs<CjoDefinitionSnapshotResult.Completed>(fixture.provider.definitionExportSnapshotOf(request))

    private fun passive(fixture: Fixture, request: CjoDefinitionSnapshotRequest): CjoDefinitionSnapshotResult.Completed =
        assertIs<CjoDefinitionSnapshotResult.Completed>(fixture.provider.completedDefinitionExportSnapshotOf(request))

    private fun index(declaration: CfirDeclaration): Int = assertNotNull(declaration.cjoDeclarationOrigin).declarationIndex

    private fun sdk(root: Path, bytes: ByteArray = officialCoreBytes()): SdkFixture {
        Files.createDirectories(root)
        val path = root.resolve("std.core.cjo")
        Files.write(path, bytes)
        return sdkDeclarations(fixture(root, path, "std.core"))
    }

    private fun officialCoreBytes(): ByteArray = assertNotNull(javaClass.classLoader.getResourceAsStream(
        "cjo-sdk/windows_x86_64_cjnative/std/std.core.cjo",
    )).use { it.readBytes() }

    private fun sdkDeclarations(fixture: Fixture): SdkFixture {
        val objectDeclaration = assertIs<CfirClass>(assertNotNull(fixture.provider.getClassLikeSymbolByClassId(
            ClassId.topLevel(FqName("std.core.Object")),
        )).cfir)
        val anyDeclaration = assertIs<CfirInterface>(assertNotNull(fixture.provider.getClassLikeSymbolByClassId(
            ClassId.topLevel(FqName("std.core.Any")),
        )).cfir)
        val objectWire = assertNotNull(fixture.source.pkg.allDecls(index(objectDeclaration)))
        val objectInfo = assertIs<ClassInfo>(objectWire.info(ClassInfo()))
        assertEquals(1, objectInfo.bodyLength)
        val constructor = assertIs<CfirFunction>(fixture.declaration(objectInfo.body(0).toInt() - 1))
        val constructorWire = assertNotNull(fixture.source.pkg.allDecls(index(constructor)))
        val info = assertIs<FuncInfo>(constructorWire.info(FuncInfo()))
        assertTrue(info.isConst)
        val bodyReference = assertNotNull(info.funcBody).body
        assertTrue(bodyReference != 0u, "实际 SDK const ctor 必须保留非零 body")
        val functionPosition = assertIs<CjoSourceTable>(constructorWire.info(CjoSourceTable())).tablePosition(4)
        return SdkFixture(fixture, objectDeclaration, anyDeclaration, constructor, functionPosition, bodyReference)
    }

    private data class SdkFixture(val fixture: Fixture, val objectDeclaration: CfirClass, val anyDeclaration: CfirInterface,
        val constructor: CfirFunction, val functionPosition: Int, val bodyReference: UInt) {
        fun request(contents: List<CjoDefinitionContentRequirement> = emptyList()): CjoDefinitionSnapshotRequest =
            fixture.request(listOf(objectDeclaration, anyDeclaration), contents)
    }

    private fun fixture(root: Path, path: Path, packageName: String): Fixture {
        val manager = CjoManager(CjoSearchPath(additionalLibrarySearchPaths = listOf(root.toFile()), envProvider = { null }),
            CjoFormatVersionPolicy.Compilation(LanguageVersionSettingsImpl.DEFAULT))
        val source = assertNotNull(manager.loadPackageSnapshot(packageName))
        assertEquals(path.toAbsolutePath().normalize(), source.sourcePath)
        return Fixture(manager, source)
    }

    /** 每个 Fixture 都使用真正的 Library provider/context；共享 Manager 不共享 semantic binder。 */
    private class Fixture(val manager: CjoManager, val source: CjoLoadedPackage) {
        val module = Module()
        val provider = CfirDeserializedSymbolProvider(
            module.session, manager, CfirCangJieScopeProvider(), module,
            moduleDataProvider = cjoFixtureModuleDataProvider(module, setOf(source.sourcePath.parent)),
        )
        init {
            module.session.register(CfirSymbolProvider::class, provider)
            module.session.register(CfirProvider::class, CfirLibrarySessionProvider(provider))
        }
        fun declaration(index: Int): CfirDeclaration = assertNotNull(provider.deserializeCjoDeclaration(
            source.sourcePath, source.header.fullPkgName, index,
        ))
        fun request(seeds: List<CfirDeclaration>, contents: List<CjoDefinitionContentRequirement> = emptyList()): CjoDefinitionSnapshotRequest {
            // 原 canonical d 的正式 content owner 提供 binder；整包 snapshot 要求 package graph，
            // 不能为构造限定 request 去调用 loadHeaders/load 或伪造 completed membership。
            val owner = assertIs<CjoSourceGraphPublicationOwner>(seeds.first().contentSource)
            val domain = seeds.first().moduleData.session
            assertSame(module.session, domain)
            return CjoDefinitionSnapshotRequest(source, owner.binderIdentity, domain, seeds, contents)
        }
    }

    private class Module : CfirModuleData() {
        override val name = Name.identifier("definition-snapshot-test")
        override val stableModuleName = "definition-snapshot-test"
        override val dependencies: List<CfirModuleData> = emptyList()
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform = CfirPlatform.DEFAULT
        override val isCommon = false
        override val session = object : CfirSession(CfirSession.Kind.Library) {}.also {
            it.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
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

    /** standalone content 控制使用实际 Manager snapshot 和正式 context，不制造 P 或图 membership。 */
    private fun standalonePatternContext(root: Path): CfirDeserializationContext {
        val metadata = rawMetadata()
        val path = CjoPackageWriter.write(root.resolve("scope.raw.cjo"), metadata)
        val fixture = fixture(root, path, metadata.fullPackageName)
        return fixture.provider.deserializationBinding.contextForSnapshot(fixture.source, CjoDeserializationMode.FULL)
    }

    /** 固定 N 合法 raw fixture：F.body 拥有 local P/binding 和非零 initializer，另有全局变量。 */
    private fun rawMetadata(): CjoPackageMetadata {
        val publicGlobal = List(maxOf(Attribute.PUBLIC.ordinal, Attribute.GLOBAL.ordinal) / 64 + 1) { word ->
            listOf(Attribute.PUBLIC, Attribute.GLOBAL).filter { it.ordinal / 64 == word }
                .fold(0uL) { bits, attribute -> bits or (1uL shl (attribute.ordinal % 64)) }
        }
        return CjoPackageMetadata("scope.raw", "scope-raw", schemaProfile = CjoSchemaProfile.OFFICIAL_V1_1_3,
            allFiles = listOf("scope.cj"), fileImports = listOf(CjoPackageFileImports(emptyList())),
            types = listOf(CjoTypeMetadata(TypeKind.Int64), CjoTypeMetadata(TypeKind.Nothing),
                CjoTypeMetadata(TypeKind.Func, semanticInfo = CjoFunctionTypeInfoMetadata(1u, false, false))),
            declarations = listOf(
                CjoPackageDeclaration("entry", type = 3u, attributes = publicGlobal,
                    info = CjoFunctionInfo(body = CjoFunctionBodyInfo(parameterLists = listOf(emptyList()),
                        desugaredParameterLists = listOf(emptyList()), returnType = 1u, body = 3u, always = true))),
                CjoPackageDeclaration("", DeclKind.VarWithPatternDecl, isTopLevel = false, type = 1u,
                    info = CjoVarWithPatternInfo(irrefutablePattern = CjoPatternMetadata(PatternKind.VarPattern,
                        types = listOf(1u), exprs = listOf(3u)), initializer = 5u)),
                CjoPackageDeclaration("bound", DeclKind.VarDecl, isTopLevel = false, type = 1u, info = CjoVariableInfo()),
                CjoPackageDeclaration("value", DeclKind.VarDecl, type = 1u, attributes = publicGlobal, info = CjoVariableInfo(initializer = 4u)),
                CjoPackageDeclaration("borrowed", DeclKind.VarDecl, type = 1u, attributes = publicGlobal, info = CjoVariableInfo()),
            ), expressions = listOf(
                CjoExpressionMetadata(ExprKind.RefExpr, type = 1u, reference = CjoReferenceExpressionMetadata("borrowed",
                    CjoExpressionTargetMetadata(pkgId = PackageIndex.CURRENT.value, index = 5u))),
                CjoExpressionMetadata(ExprKind.ReturnExpr, type = 2u, operands = listOf(1u)),
                CjoExpressionMetadata(ExprKind.Block, type = 2u, operands = listOf(2u, 2u),
                    additionalInfo = CjoAdditionalExpressionInfoMetadata.Block(listOf(false, true, false))),
                CjoExpressionMetadata(ExprKind.LitConstExpr, type = 1u, literal = CjoLiteralExpressionMetadata("7", LitConstKind.Integer)),
                CjoExpressionMetadata(ExprKind.LitConstExpr, type = 1u, literal = CjoLiteralExpressionMetadata("11", LitConstKind.Integer)),
            ))
    }

    /** 仅用于在原资源中制造明确坏 union；不制造 canonical SOURCE 或 membership 产品。 */
    private class CorruptionSlots : Table() {
        fun at(position: Int, buffer: ByteBuffer): CorruptionSlots = apply { __reset(position, buffer) }
        fun expressionPosition(ordinal: Int): Int = __indirect(__vector(__offset(22)) + ordinal * Int.SIZE_BYTES)
        fun infoTagSlot(): Int = bb_pos + __offset(18).also { check(it != 0) }
    }
}
