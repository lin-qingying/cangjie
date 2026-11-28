@file:OptIn(ExperimentalUnsignedTypes::class, org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeserializationMode
import org.cangnova.cangjie.test.bindCjoFixture
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeclarationMaterialization
import org.cangnova.cangjie.cfir.serialization.provider.CjoPackageContextProvider
import org.cangnova.cangjie.test.cjoFixtureModuleDataProvider
import PackageFormat.AnnoKind
import PackageFormat.DeclKind
import PackageFormat.ExprKind
import PackageFormat.LitConstKind
import PackageFormat.Package
import PackageFormat.ParamInfo
import PackageFormat.TypeKind
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.annotationInfo
import org.cangnova.cangjie.cfir.declarations.cjoDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.exportFacts
import org.cangnova.cangjie.cfir.declarations.initializationDependencies
import org.cangnova.cangjie.cfir.declarations.ownedGraphAnnotationsUnselected
import org.cangnova.cangjie.cfir.declarations.builder.buildClassCopy
import org.cangnova.cangjie.cfir.declarations.builder.buildConstructorCopy
import org.cangnova.cangjie.cfir.declarations.builder.buildInterfaceCopy
import org.cangnova.cangjie.cfir.declarations.builder.buildTypeParameterCopy
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameterCopy
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationArgumentStatus
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirNamedArgumentExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildAnnotationCallCopy
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpressionCopy
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirDeclarationExportAnalysis
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysis
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysisInputPreparation
import org.cangnova.cangjie.cfir.resolve.providers.CfirLibrarySessionProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirSourceGraphLoader
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirTypeDeserializer
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.source.CfirImportedDeclarationHeaderSource
import org.cangnova.cangjie.cfir.source.CfirSourceGraph
import org.cangnova.cangjie.cfir.source.CfirSourceGraphProvenance
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagWithFixedSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.resolve.calls.mpp.CjmpInitializationDependencies
import org.junit.jupiter.api.io.TempDir
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * 声明/类型登记之后的 annotation、default 和依赖产品必须在首次根请求内共同完成。
 *
 * 固定证据：Kotlin 3c7a5041f238102285764d370665f56f587f9dc4
 * IrDeclarationDeserializer.kt:241–262 先构造声明，后读取 annotations；
 * Native 14276740ffec1262b81b6b5236b1d840af270b37 的 ASTLoaderImpl.h:146–159
 * 登记真实 d/e，ASTLoader.cpp:573–588 建立 TP/outerDecl，ReferenceLoader.cpp:306–330、
 * 508–534 恢复类型上界、依赖和 annotation 引用；ExprLoader.cpp:141–153 恢复合法 literal。
 *
 * 复用 CjoTypeGraphDeserializationTest 的 library session、CjoUnifiedSourceEncoderTest 的
 * constructor FullId/Block 和 CfirInitializationDependencySerializationTest 的 common 依赖格式。
 * 下列是有界 schema seeds，不是 Native 生产 std.core 或完整 SOURCE33 reader 通过的证据。
 * String 保留真实 std.core.String 身份；Marker/Relation 仅提供合法的接口及泛型引用图。
 * 所有函数保存完整 FuncTy 和真实 Block，公开声明查询按自身合同不读取 named F 正文。
 */
class CjoDeclarationCompletionLifecycleTest {
    /** 首次 normal annotation 请求完成 Tag 的普通内容；空 dependencies 不能等下次 Public 查询补写。 */
    @Test
    fun annotationDiscoveredOwnerPublishesCollectedEmptyDependenciesOnItsFirstCompletion(@TempDir root: Path) {
        for (typeFirst in listOf(false, true)) {
            val fixture = fixture(root.resolve(typeFirst.toString()), Message.EXPLICIT_INTEGER)
            val context = fixture.context
            // 两个分支在首次 type/normal 请求之前均为冷准备。
            assertFalse(context.declCache.containsKey(TAG))
            val marked = loadMarked(context, typeFirst, Message.EXPLICIT_INTEGER)
            val tag = assertIs<CfirClass>(context.declCache[TAG])
            val stamp = assertIs<CfirFieldVariable>(context.declCache[STAMP])
            assertSame(stamp, tag.declarations.filterIsInstance<CfirFieldVariable>().single())
            assertEquals(0, assertNotNull(context.pkg.allDecls(STAMP)).dependenciesLength)
            assertCollectedEmpty(stamp)
            assertAnnotation(context, marked, Message.EXPLICIT_INTEGER)
            val frozen = FieldSnapshot(stamp)
            val call = assertIs<CfirAnnotationCall>(marked.annotations.single())
            val view = assertNotNull(call.argumentView)

            // 切换 reader，并直接查询 owner、字段和类型；每次都只能取得同一已完成产品。
            repeat(2) {
                assertSame(tag, context.createDeclDeserializer().deserializeDecl(TAG))
                assertSame(stamp, context.createDeclDeserializer().deserializeDecl(STAMP))
                assertSame(context.typeCache[4], CfirTypeDeserializer(context).deserializeType(4))
                assertSame(marked, context.createDeclDeserializer().deserializeDecl(MARKED))
                assertSame(call, marked.annotations.single())
                assertSame(view, call.argumentView)
                frozen.assertUnchanged()
            }
        }
    }

    /** Any 形参的合法 String 默认值首次引入 String、Relation.T；默认 view 借用真实 parameter/default。 */
    @Test
    fun defaultLiteralCompletesNewClassifierAndGenericHeadersBeforeTheArgumentView(@TempDir root: Path) {
        for (typeFirst in listOf(false, true)) {
            val fixture = fixture(root.resolve(typeFirst.toString()), Message.DEFAULT_STRING)
            val context = fixture.context
            assertSame(context.pkg, context.sourceHeaderRegistry.ownerPackage)
            assertFalse(context.declCache.containsKey(MARKED))
            assertFalse(context.declCache.containsKey(TAG))
            assertFalse(context.declCache.containsKey(STRING))
            var observed = false
            var observedMarked: CfirClass? = null
            var observerRoot = RootSnapshot(context)
            // 在任何可能绑定 Marker 的 metadata 请求之前安装真实 resolver observer。
            fixture.beforeMarkerHeader = {
                observed = true
                observerRoot.assertUnchanged()
                val selection = assertNotNull(context.currentSourceSelection)
                assertSame(context.pkg, selection.ownedContent.ownerPackage)
                assertSame(context.pkg, selection.registeredHeaders.ownerPackage)
                // 工作 header 只核对内部身份，不读取其 Published 内容。
                val marked = assertIs<CfirClass>(context.declarations[MARKED])
                assertTrue(marked.symbol.isBound)
                assertSame(marked, marked.symbol.cfir)
                observedMarked = marked
            }
            if (typeFirst) {
                // type-first 是独立 metadata Root；先提交合法 header，再保存 normal Root 的旧完成代。
                val type = CfirTypeDeserializer(context).deserializeType(2)
                assertSame(type, context.typeCache[2])
                val marked = assertIs<CfirClass>(context.declCache[MARKED])
                assertSame(context.declCache[1], marked.declarations.filterIsInstance<CfirConstructor>().single())
                assertSame(org.cangnova.cangjie.cfir.CfirPublishedAnnotations.Unselected,
                    context.publishedContentSource.snapshot().annotations(marked))
                assertTrue(marked.annotations.isEmpty())
                assertNull(marked.annotationInfo)
                assertIs<CfirClass>(context.declCache[TAG])
            }
            assertEquals(typeFirst, context.declCache.containsKey(MARKED))
            assertEquals(typeFirst, context.declCache.containsKey(1))
            assertEquals(typeFirst, context.declCache.containsKey(TAG))
            assertFalse(context.declCache.containsKey(STRING))
            // metadata 成功代是 normal Root 自己的旧代，不能沿用冷 metadata Root 的快照。
            val normalRoot = RootSnapshot(context)
            observerRoot = normalRoot
            val marked = loadMarked(context, typeFirst = false, message = Message.DEFAULT_STRING)
            fixture.beforeMarkerHeader = null
            if (typeFirst) {
                assertSame(normalRoot.declarations[MARKED], marked)
                assertSame(normalRoot.declarations[1], marked.declarations.filterIsInstance<CfirConstructor>().single())
                // cached type 请求必须原样复用完成容器，不能再提交一个空 Root。
                val completedDeclarations = context.declCache
                val completedTypes = context.typeCache
                val completedConstraints = context.genericConstraints
                val completedSnapshot = context.publishedContentSource.snapshot()
                assertSame(completedTypes[2], CfirTypeDeserializer(context).deserializeType(2))
                assertSame(completedDeclarations, context.declCache)
                assertSame(completedTypes, context.typeCache)
                assertSame(completedConstraints, context.genericConstraints)
                assertSame(completedSnapshot, context.publishedContentSource.snapshot())
            }
            assertTrue(observed, "合法 default literal 必须实际触发新的跨包 header 引用")
            assertSame(assertNotNull(observedMarked), marked)
            assertAnnotation(context, marked, Message.DEFAULT_STRING)
            assertStringHeaders(fixture)
            val stamp = FieldSnapshot(assertIs<CfirFieldVariable>(context.declCache[STAMP]))
            val length = FieldSnapshot(assertIs<CfirFieldVariable>(context.declCache[LENGTH]))
            assertCollectedEmpty(stamp.field)
            assertCollectedEmpty(length.field)
            assertSame(marked, context.createDeclDeserializer().deserializeDecl(MARKED))
            assertSame(length.field, context.createDeclDeserializer().deserializeDecl(LENGTH))
            assertSame(stamp.field, context.createDeclDeserializer().deserializeDecl(STAMP))
            stamp.assertUnchanged()
            length.assertUnchanged()
        }
    }

    /** 已完成的真实 target 不产生新声明工作；最后一项 annotation literal 仍必须先完成新类型 header。 */
    @Test
    fun lastAnnotationLiteralDrainsTypeHeadersBeforeBindingExplicitAndDefaultArguments(@TempDir root: Path) {
        val fixture = fixture(root, Message.EXPLICIT_STRING)
        val context = fixture.context
        assertSame(context.pkg, context.sourceHeaderRegistry.ownerPackage)
        val tag = context.materialize {
            val reader = context.createDeclDeserializer()
            val tag = assertIs<CfirClass>(reader.deserializeDecl(TAG))
            reader.deserializeVariableInitializer(STAMP)
            tag
        }
        val stamp = FieldSnapshot(assertIs<CfirFieldVariable>(context.declCache[STAMP]))
        assertCollectedEmpty(stamp.field)
        assertFalse(context.declCache.containsKey(STRING))
        var observed = false
        var observedMarked: CfirClass? = null
        var observerRoot = RootSnapshot(context)
        val oldSnapshot = context.publishedContentSource.snapshot()
        val oldTagAnnotations = oldSnapshot.annotations(tag)
        val oldTagAnnotationInfo = tag.annotationInfo
        val oldTagDeclarations = tag.declarations
        val constructor = oldTagDeclarations.filterIsInstance<CfirConstructor>().single()
        val parameters = constructor.valueParameters.toList()
        val defaults = parameters.map { oldSnapshot.defaultValue(it) }
        fixture.beforeMarkerHeader = {
            observed = true
            // 新 explicit literal 只在工作集中；旧 Tag/default/stamp 产品不得被替换或补写。
            observerRoot.assertUnchanged()
            val selection = assertNotNull(context.currentSourceSelection)
            assertSame(context.pkg, selection.ownedContent.ownerPackage)
            assertSame(context.pkg, selection.registeredHeaders.ownerPackage)
            assertSame(tag, context.declCache[TAG])
            assertSame(oldTagAnnotations, context.publishedContentSource.snapshot().annotations(tag))
            // MutableOrEmptyList是value class，包装boxing地址不是内容身份合同。
            assertEquals(oldTagAnnotations.values, tag.annotations)
            oldTagAnnotations.values.forEachIndexed { index, annotation -> assertSame(annotation, tag.annotations[index]) }
            assertSame(oldTagAnnotationInfo, tag.annotationInfo)
            assertSame(oldTagDeclarations, tag.declarations)
            assertSame(constructor, context.declCache[TAG_INIT])
            assertSame(constructor, tag.declarations.filterIsInstance<CfirConstructor>().single())
            parameters.forEachIndexed { index, parameter ->
                assertSame(parameter, constructor.valueParameters[index])
                assertSame(constructor.symbol, parameter.containingDeclarationSymbol)
                assertSame(defaults[index], context.publishedContentSource.snapshot().defaultValue(parameter))
                assertSame(defaults[index], parameter.defaultValue)
            }
            val rank = assertIs<CfirLiteralExpression>(defaults[1])
            assertEquals("19", rank.value)
            assertEquals(ConePrimitiveType.INT64, rank.coneTypeOrNull)
            assertSame(stamp.field, context.declCache[STAMP])
            assertSame(oldSnapshot.initializer(stamp.field), stamp.field.initializer)
            stamp.assertUnchanged()

            // metadata 工作 header 只核对内部身份，不读取其公开内容。
            val marked = assertIs<CfirClass>(context.declarations[MARKED])
            assertTrue(marked.symbol.isBound)
            assertSame(marked, marked.symbol.cfir)
            observedMarked = marked
        }
        // observer 已安装；metadata Root 成功之后才保存 normal Root 的旧完成代。
        val type = CfirTypeDeserializer(context).deserializeType(2)
        assertSame(type, context.typeCache[2])
        val metadataMarked = assertIs<CfirClass>(context.declCache[MARKED])
        val markedConstructor = metadataMarked.declarations.filterIsInstance<CfirConstructor>().single()
        assertSame(markedConstructor, context.declCache[1])
        assertSame(org.cangnova.cangjie.cfir.CfirPublishedAnnotations.Unselected,
            context.publishedContentSource.snapshot().annotations(metadataMarked))
        assertTrue(metadataMarked.annotations.isEmpty())
        assertNull(metadataMarked.annotationInfo)
        val normalRoot = RootSnapshot(context)
        observerRoot = normalRoot
        val marked = loadMarked(context, typeFirst = false, message = Message.EXPLICIT_STRING)
        fixture.beforeMarkerHeader = null
        assertSame(metadataMarked, marked)
        assertSame(markedConstructor, marked.declarations.filterIsInstance<CfirConstructor>().single())
        // 即使首次 Marker lookup 发生在 metadata Root，normal Root 也必须保留旧 Tag 的完整产品。
        assertSame(oldTagAnnotations, context.publishedContentSource.snapshot().annotations(tag))
        assertSame(oldTagAnnotationInfo, tag.annotationInfo)
        assertSame(oldTagDeclarations, tag.declarations)
        assertSame(constructor, context.declCache[TAG_INIT])
        parameters.forEachIndexed { index, parameter ->
            assertSame(parameter, constructor.valueParameters[index])
            assertSame(defaults[index], context.publishedContentSource.snapshot().defaultValue(parameter))
            assertSame(defaults[index], parameter.defaultValue)
        }
        // cached type 请求只读既有完成代，保留四个公开完成容器的身份。
        val completedDeclarations = context.declCache
        val completedTypes = context.typeCache
        val completedConstraints = context.genericConstraints
        val completedSnapshot = context.publishedContentSource.snapshot()
        assertSame(type, CfirTypeDeserializer(context).deserializeType(2))
        assertSame(completedDeclarations, context.declCache)
        assertSame(completedTypes, context.typeCache)
        assertSame(completedConstraints, context.genericConstraints)
        assertSame(completedSnapshot, context.publishedContentSource.snapshot())
        assertTrue(observed, "String 的新 header 必须经过实际 FullId resolver")
        assertSame(assertNotNull(observedMarked), marked)
        assertAnnotation(context, marked, Message.EXPLICIT_STRING)
        assertStringHeaders(fixture)
        assertSame(tag, context.createDeclDeserializer().deserializeDecl(TAG))
        assertSame(stamp.field, context.createDeclDeserializer().deserializeDecl(STAMP))
        stamp.assertUnchanged()
    }

    /** 真实 default 槽损坏在后置读取时失败；换 reader 重试和修复后成功均不能改写旧缓存/字段。 */
    @Test
    fun failingDefaultCompletionAndRetryPreservePreviouslyCompletedCacheAndFields(@TempDir root: Path) {
        val fixture = fixture(root, Message.DEFAULT_STRING)
        val context = fixture.context
        assertSame(context.pkg, context.sourceHeaderRegistry.ownerPackage)
        CfirTypeDeserializer(context).deserializeType(6) // 先完成 String 的 metadata，Tag 尚未请求。
        context.createDeclDeserializer().deserializeVariableInitializer(LENGTH)
        assertStringHeaders(fixture)
        assertFalse(context.declCache.containsKey(TAG))
        val parameterInfo = assertIs<ParamInfo>(assertNotNull(context.pkg.allDecls(MESSAGE)).info(ParamInfo()))
        assertEquals(1u, parameterInfo.defaultVal)
        // 独立 metadata Root 先成功；后续 normal 失败不能撤销这些合法 header/type。
        val type = CfirTypeDeserializer(context).deserializeType(2)
        assertSame(type, context.typeCache[2])
        val metadataMarked = assertIs<CfirClass>(context.declCache[MARKED])
        val metadataTag = assertIs<CfirClass>(context.declCache[TAG])
        val metadataConstructor = metadataTag.declarations.filterIsInstance<CfirConstructor>().single()
        assertSame(metadataConstructor, context.declCache[TAG_INIT])
        metadataConstructor.valueParameters.forEachIndexed { index, parameter ->
            assertSame(parameter, context.declCache[if (index == 0) MESSAGE else RANK])
            assertNull(context.publishedContentSource.snapshot().defaultValue(parameter))
        }
        assertSame(org.cangnova.cangjie.cfir.CfirPublishedAnnotations.Unselected,
            context.publishedContentSource.snapshot().annotations(metadataMarked))
        assertSame(org.cangnova.cangjie.cfir.CfirPublishedAnnotations.Unselected,
            context.publishedContentSource.snapshot().annotations(metadataTag))
        val normalRoot = RootSnapshot(context)
        val oldDeclarations = normalRoot.declarations
        val oldTypes = normalRoot.types
        val length = FieldSnapshot(assertIs<CfirFieldVariable>(context.declCache[LENGTH]))
        assertCollectedEmpty(length.field)
        repeat(2) {
            assertSame(type, CfirTypeDeserializer(context).deserializeType(2))
            normalRoot.assertUnchanged()
            length.assertUnchanged()
        }
        assertTrue(parameterInfo.mutateDefaultVal(context.pkg.allExprsLength.toUInt() + 1u))

        // 失败来自实际 required-expression 读取；不注入回调异常、不改合法 Block 的正确期望。
        repeat(2) {
            assertFailsWith<IllegalStateException> {
                loadMarked(context, typeFirst = false, message = Message.DEFAULT_STRING)
            }
            normalRoot.assertUnchanged()
            assertSame(metadataMarked, context.declCache[MARKED])
            assertSame(metadataTag, context.declCache[TAG])
            assertSame(org.cangnova.cangjie.cfir.CfirPublishedAnnotations.Unselected,
                context.publishedContentSource.snapshot().annotations(metadataMarked))
            assertSame(org.cangnova.cangjie.cfir.CfirPublishedAnnotations.Unselected,
                context.publishedContentSource.snapshot().annotations(metadataTag))
            metadataConstructor.valueParameters.forEach { parameter ->
                assertNull(context.publishedContentSource.snapshot().defaultValue(parameter))
            }
            length.assertUnchanged()
        }
        assertTrue(parameterInfo.mutateDefaultVal(1u))
        val marked = loadMarked(context, typeFirst = false, message = Message.DEFAULT_STRING)
        assertSame(metadataMarked, marked)
        assertAnnotation(context, marked, Message.DEFAULT_STRING)
        assertCollectedEmpty(assertIs<CfirFieldVariable>(context.declCache[STAMP]))
        oldDeclarations.forEach { (index, declaration) -> assertSame(declaration, context.declCache[index]) }
        oldTypes.forEach { (index, type) -> assertSame(type, context.typeCache[index]) }
        assertSame(length.field, context.createDeclDeserializer().deserializeDecl(LENGTH))
        assertSame(marked, context.createDeclDeserializer().deserializeDecl(MARKED))
        length.assertUnchanged()
    }

    /** 冷 metadata identity 经正式 provider 准备注解/default；borrowed 正文和初值仍保持未选。 */
    @Test
    fun exportPreparationCompletesNormalHeaderBeforeReadOnlyCapture(@TempDir root: Path) {
        val (provider, path) = exportPreparationProvider(root)
        val marked = assertIs<CfirClass>(provider.deserializeCjoDeclaration(path, "r38.header", 0))
        val session = marked.moduleData.session
        val source = assertIs<org.cangnova.cangjie.cfir.CfirContentSource.Published>(marked.contentSource)
        val metadata = source.snapshot()
        assertSame(org.cangnova.cangjie.cfir.CfirPublishedAnnotations.Unselected, metadata.annotations(marked))
        assertTrue(marked.annotations.isEmpty())
        val markedConstructor = marked.declarations.filterIsInstance<CfirConstructor>().single()
        assertNull(markedConstructor.body)

        val guard = R38ExportPreparationGuard(session, provider)
        guard.proveGuards(marked)
        val roots = listOf<org.cangnova.cangjie.cfir.declarations.CfirDeclaration>(marked)
        val prepared = org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysisInputPreparation.prepare(
            emptyList(), roots, ::r38PhysicalMembers)
        guard.observeOwners(prepared.declarations)
        guard.assertHeaderReady(marked)
        assertSame(marked, provider.deserializeCjoDeclaration(path, "r38.header", 0))
        assertSame(metadata.annotations(marked), org.cangnova.cangjie.cfir.CfirPublishedAnnotations.Unselected)
        prepared.declarations.forEach { declaration ->
            val published = assertIs<org.cangnova.cangjie.cfir.CfirContentSource.Published>(declaration.contentSource)
            assertIs<org.cangnova.cangjie.cfir.CfirPublishedAnnotations.Complete>(published.snapshot().annotations(declaration))
        }
        val complete = assertIs<org.cangnova.cangjie.cfir.CfirPublishedAnnotations.Complete>(source.snapshot().annotations(marked))
        val annotation = assertIs<CfirAnnotationCall>(complete.values.single())
        val tagConstructor = assertIs<CfirConstructor>(assertIs<CfirResolvedNamedReference>(annotation.calleeReference).resolvedSymbol.cfir)
        val tag = assertIs<CfirClass>(provider.deserializeCjoDeclaration(path, "r38.header", 2))
        assertSame(tagConstructor, tag.declarations.filterIsInstance<CfirConstructor>().single())
        guard.assertHeaderReady(tag)
        guard.assertHeaderReady(tagConstructor)
        val parameter = tagConstructor.valueParameters.single()
        val default = assertIs<CfirLiteralExpression>(parameter.defaultValue)
        val entry = assertNotNull(annotation.argumentView).entries.single()
        assertSame(parameter, entry.resolvedParameter)
        assertSame(default, entry.constantExpression)
        assertTrue(entry.isDefaultOrigin)
        assertEquals("19", default.value)
        assertTrue(assertNotNull(tag.annotationInfo).isAnnotation)
        val stamp = tag.declarations.filterIsInstance<CfirFieldVariable>().single()
        assertNull(stamp.initializer)
        assertNull(tagConstructor.body)
        assertNull(markedConstructor.body)
        assertTrue(assertNotNull(provider.sourceHeaderOf(tagConstructor)).bodyAvailable)

        val snapshot = guard.readOnly {
            org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysis.analyze(
                emptyList(), roots, ::r38PhysicalMembers, preparedInput = prepared).also { it.validateSourceUnchanged() }
        }
        assertSame(parameter, snapshot.valueParametersOf(tagConstructor).single())
        assertSame(default, parameter.defaultValue)
        assertNull(stamp.initializer)
        assertNull(tagConstructor.body)
        assertFalse(snapshot.hasCanonicalSourceOf(tag))
        assertFalse(snapshot.hasCanonicalSourceOf(stamp))
        repeat(2) {
            val next = org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysisInputPreparation.prepare(
                emptyList(), roots, ::r38PhysicalMembers)
            assertSame(complete, source.snapshot().annotations(marked))
            assertSame(parameter, tagConstructor.valueParameters.single())
            assertSame(default, parameter.defaultValue)
            guard.readOnly {
                snapshot.validateSourceUnchanged()
                org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysis.analyze(
                    emptyList(), roots, ::r38PhysicalMembers, preparedInput = next).validateSourceUnchanged()
            }
        }
        // 显式正文选择是独立授权；它必须使此前 header-only 的冻结输入失效。
        assertNotNull(provider.sourceBodyOf(tagConstructor))
        assertSame(tagConstructor, provider.deserializeCjoDeclaration(path, "r38.header", 3))
        guard.readOnly { assertFailsWith<IllegalStateException> { snapshot.validateSourceUnchanged() } }
    }

    /** Owned 实际字段来自已完成 header；新参数/TP 绑定新 owner，未选注解不能伪装为空完成。 */
    @Test
    fun ownedHeaderReadinessRejectsUnselectedAnnotationsOnTheActualDeclaration(@TempDir root: Path) {
        val fixture = fixture(root, Message.EXPLICIT_STRING)
        val marked = loadMarked(fixture.context, typeFirst = false, message = Message.EXPLICIT_STRING)
        val markedSymbol = CfirClassSymbol(marked.symbol.classId)
        val originalAnnotation = assertIs<CfirAnnotationCall>(marked.annotations.single())
        val ownedAnnotation = buildAnnotationCallCopy(originalAnnotation) {
            containingDeclarationSymbol = markedSymbol
        }
        val ownedMarked = buildClassCopy(marked) {
            symbol = markedSymbol
            annotations.clear()
            annotations += ownedAnnotation
        }
        val original = assertIs<CfirConstructor>(fixture.context.declCache[TAG_INIT])
        val ownedConstructorSymbol = CfirConstructorSymbol(original.symbol.callableId)
        val owned = buildConstructorCopy(original) {
            symbol = ownedConstructorSymbol
            valueParameters.clear()
            original.valueParameters.forEach { parameter ->
                valueParameters += buildValueParameterCopy(parameter) {
                    this.symbol = CfirValueParameterSymbol(parameter.symbol.callableId)
                    containingDeclarationSymbol = ownedConstructorSymbol
                }
            }
        }
        val relation = assertIs<CfirInterface>(fixture.context.declCache[RELATION])
        val relationSymbol = CfirInterfaceSymbol(relation.symbol.classId)
        val originalTypeParameter = assertIs<CfirTypeParameter>(relation.typeParameters.single())
        val ownedTypeParameter = buildTypeParameterCopy(originalTypeParameter) {
            this.symbol = CfirTypeParameterSymbol()
            containingDeclarationSymbol = relationSymbol
        }
        val ownedRelation = buildInterfaceCopy(relation) {
            this.symbol = relationSymbol
            typeParameters.clear()
            typeParameters += ownedTypeParameter
        }
        val guard = R38ExportPreparationGuard(original.moduleData.session, fixture.provider)
        assertSame(org.cangnova.cangjie.cfir.CfirContentSource.Owned, owned.contentSource)
        assertSame(org.cangnova.cangjie.cfir.CfirContentSource.Owned, ownedTypeParameter.contentSource)
        guard.assertHeaderReady(ownedMarked)
        guard.assertHeaderReady(owned)
        guard.assertHeaderReady(ownedRelation)
        assertSame(ownedMarked, markedSymbol.cfir)
        assertSame(ownedAnnotation, ownedMarked.annotations.single())
        assertSame(markedSymbol, ownedAnnotation.containingDeclarationSymbol)
        assertSame(originalAnnotation.typeRef, ownedAnnotation.typeRef)
        assertSame(originalAnnotation.calleeReference, ownedAnnotation.calleeReference)
        assertSame(originalAnnotation.argumentView, ownedAnnotation.argumentView)
        assertSame(owned, ownedConstructorSymbol.cfir)
        assertSame(original.returnTypeRef, owned.returnTypeRef)
        assertTrue(owned.annotations.isEmpty())
        assertTrue(owned.typeParameters.isEmpty())
        owned.valueParameters.forEachIndexed { index, parameter ->
            assertSame(parameter, parameter.symbol.cfir)
            assertSame(ownedConstructorSymbol, parameter.containingDeclarationSymbol)
            assertSame(original.valueParameters[index].returnTypeRef, parameter.returnTypeRef)
            assertSame(original.valueParameters[index].defaultValue, parameter.defaultValue)
            assertTrue(parameter.annotations.isEmpty())
        }
        assertNull(owned.valueParameters[0].defaultValue)
        val rank = owned.valueParameters[1]
        val default = assertIs<CfirLiteralExpression>(rank.defaultValue)
        assertEquals("19", default.value)
        assertSame(ownedTypeParameter, ownedRelation.typeParameters.single())
        assertSame(ownedTypeParameter, ownedTypeParameter.symbol.cfir)
        assertSame(relationSymbol, ownedTypeParameter.containingDeclarationSymbol)
        assertSame(originalTypeParameter.bounds.single(), ownedTypeParameter.bounds.single())

        rank.ownedGraphAnnotationsUnselected = true
        val failure = assertFailsWith<IllegalStateException> { guard.assertHeaderReady(owned) }
        assertEquals("Owned analysis requires explicitly materialized annotation content", failure.message)
        assertSame(default, rank.defaultValue)
        assertTrue(rank.annotations.isEmpty())
        assertFalse(original.valueParameters[1].ownedGraphAnnotationsUnselected == true)
        rank.ownedGraphAnnotationsUnselected = null
        guard.assertHeaderReady(owned)
        assertSame(default, rank.defaultValue)
    }

    /** public API 自己保护 capture 的第一次成员域验证；不借用 testguard 的外层只读 fence。 */
    @Test
    fun captureDomainValidationRejectsOriginalOwnerHeaderCompletionBeforeMaterialization(@TempDir root: Path) {
        val (provider, path) = exportPreparationProvider(root)
        val owner = assertIs<CfirClass>(provider.deserializeCjoDeclaration(path, "r38.header", 0))
        val source = assertIs<CfirImportedDeclarationHeaderSource>(owner.contentSource)
        owner.moduleData.session.register(CfirProvider::class, CfirLibrarySessionProvider(provider))
        assertTrue(owner.symbol.isBound)
        assertSame(owner, owner.symbol.cfir)
        val roots = listOf<CfirDeclaration>(owner)
        val originalMembers = java.util.IdentityHashMap<CfirDeclaration, List<CfirDeclaration>>()
        var completeDuringCapture = false
        var completionAttempts = 0
        var returnedAfterCompletion = false
        val members: (CfirDeclaration) -> List<CfirDeclaration> = { declaration ->
            val input = originalMembers.getOrPut(declaration) { r38PhysicalMembers(declaration) }
            if (completeDuringCapture && declaration === owner) {
                completionAttempts++
                source.completeHeader(owner)
                returnedAfterCompletion = true
            }
            input
        }
        val prepared = CfirExportAnalysisInputPreparation.prepare(emptyList(), roots, members)
        assertEquals(0, completionAttempts)
        assertSame(owner.declarations, originalMembers.getValue(owner))
        val publications = java.util.IdentityHashMap<CfirImportedDeclarationHeaderSource,
            org.cangnova.cangjie.cfir.CfirPublishedContentSnapshot>()
        prepared.declarations.forEach { declaration ->
            val contentOwner = assertIs<CfirImportedDeclarationHeaderSource>(declaration.contentSource)
            publications[contentOwner] = contentOwner.snapshot()
            assertIs<org.cangnova.cangjie.cfir.CfirPublishedAnnotations.Complete>(contentOwner.snapshot().annotations(declaration))
        }
        // 同一准备产品先正常 capture，证明负控输入本身已完备且真实 owner 可被消费。
        val normal = CfirExportAnalysis.analyze(emptyList(), roots, members, preparedInput = prepared)
        normal.validateSourceUnchanged()
        prepared.declarations.forEach { declaration -> assertTrue(normal.capturedDeclarations().any { it === declaration }) }

        completeDuringCapture = true
        var failedCapture: CfirDeclarationExportAnalysis? = null
        val failure = assertFailsWith<IllegalStateException> {
            failedCapture = CfirExportAnalysis.analyze(emptyList(), roots, members, preparedInput = prepared)
        }
        assertEquals(1, completionAttempts)
        assertFalse(returnedAfterCompletion)
        assertNull(failedCapture)
        // 精确拒绝原因来自原 materialize 的事务入口，排除 wrongContext/域漂移造成的伪负控。
        assertEquals("Imported content materialization is forbidden in a read-only input region", failure.message)
        // internal 成员在实际 JVM ABI 中带模块后缀，栈帧须按真实入口名核验。
        assertTrue(failure.stackTrace.any {
            it.className == CfirDeserializationContext::class.java.name &&
                it.methodName == "materialize" + '$' + "cfir_serialization"
        })
        assertTrue(failure.stackTrace.any {
            it.className.endsWith("CfirExportAnalysisInputPreparation" + '$' + "Domain") && it.methodName == "validate"
        })
        publications.forEach { (contentOwner, publication) -> assertSame(publication, contentOwner.snapshot()) }
        originalMembers.forEach { (declaration, input) -> assertSame(input, r38PhysicalMembers(declaration)) }
        assertSame(source, owner.contentSource)
        assertSame(owner, owner.symbol.cfir)

        // 拒绝没有材料化/发布新代，也没有损坏原输入；关闭负控后仍使用同一个 prepared。
        completeDuringCapture = false
        normal.validateSourceUnchanged()
        val next = CfirExportAnalysis.analyze(emptyList(), roots, members, preparedInput = prepared)
        next.validateSourceUnchanged()
        assertEquals(1, completionAttempts)
        publications.forEach { (contentOwner, publication) -> assertSame(publication, contentOwner.snapshot()) }
    }

    /** initializer 授权与 body 授权分开；不允许一次变化后重抓输入接受新一代。 */
    @Test
    fun borrowedInitializerCompletionInvalidatesTheOriginalPreparedCapture(@TempDir root: Path) {
        val (provider, path) = exportPreparationProvider(root)
        val tag = assertIs<CfirClass>(provider.deserializeCjoDeclaration(path, "r38.header", 2))
        val guard = R38ExportPreparationGuard(tag.moduleData.session, provider)
        val roots = listOf<org.cangnova.cangjie.cfir.declarations.CfirDeclaration>(tag)
        val prepared = org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysisInputPreparation.prepare(
            emptyList(), roots, ::r38PhysicalMembers)
        guard.observeOwners(prepared.declarations)
        val stamp = tag.declarations.filterIsInstance<CfirFieldVariable>().single()
        val constructor = tag.declarations.filterIsInstance<CfirConstructor>().single()
        assertNull(stamp.initializer)
        val snapshot = guard.readOnly {
            org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysis.analyze(
                emptyList(), roots, ::r38PhysicalMembers, preparedInput = prepared)
        }
        val initializer = assertNotNull(provider.sourceInitializerOf(stamp))
        assertSame(initializer, stamp.initializer)
        assertSame(stamp, provider.deserializeCjoDeclaration(path, "r38.header", 5))
        assertNull(constructor.body)
        // 公开 SOURCE 内容接口仍拒绝 borrowed owner；旧冻结输入必须报告显式内容升级。
        assertFailsWith<IllegalStateException> { snapshot.initializerOf(stamp) }
        guard.readOnly { assertFailsWith<IllegalStateException> { snapshot.validateSourceUnchanged() } }
    }

    /** 原 provider 的 canonical SOURCE、normal default、显式 initializer 产生三代真实 alias 产品。 */
    @Test
    fun successfulSourceGraphHistoryRetainsEveryGenerationAndItsOriginalProducts(@TempDir root: Path) {
        val (provider, path) = exportPreparationProvider(root)
        val marked = assertIs<CfirClass>(provider.deserializeCjoDeclaration(path, "r38.header", 0))
        val first = assertNotNull(provider.sourceExportSnapshotOf(marked, includeContent = true))
        val g0 = assertNotNull(first.graph)
        val owner = assertIs<CjoSourceGraphPublicationOwner>(assertIs<CfirSourceGraphProvenance.Published>(g0.provenance).owner)
        assertSame<Any>(marked.contentSource, owner)
        assertSame(owner.binderIdentity, first.binderIdentity)
        val tag = assertIs<CfirClass>(first.declarations.getValue(2))
        val constructor = tag.declarations.filterIsInstance<CfirConstructor>().single()
        val rank = constructor.valueParameters.single()
        val stamp = tag.declarations.filterIsInstance<CfirFieldVariable>().single()
        assertNull(rank.defaultValue)
        assertNull(stamp.initializer)
        val rankSlot = g0.defaults.getValue(rank)
        val stampSlot = g0.initializers.getValue(stamp)
        assertTrue(rankSlot !== stampSlot)
        val savedFirst = SourcePublicationSnapshot(first)

        assertIs<CfirImportedDeclarationHeaderSource>(tag.contentSource).completeHeader(tag)
        val default = assertIs<CfirLiteralExpression>(rank.defaultValue)
        assertEquals("19", default.value)
        val second = assertNotNull(provider.completedSourceExportSnapshotOf(marked, includeContent = true))
        val g1 = assertNotNull(second.graph)
        assertTrue(g0 !== g1, "真实 default 普通节点必须增加 aliases，不能用 token 变化冒充 graph 升级")
        assertTrue(first.publicationIdentity !== second.publicationIdentity)
        assertFalse(g0.containsOrigin(default))
        assertSame(rankSlot, g1.expressionOf(default))
        assertSame(g1, second.finalPublishedGraph(g0))
        assertSame(g1, second.finalPublishedGraph(g1))
        assertAliasProductsUnchanged(g0, g1)
        savedFirst.assertUnchanged()
        val savedSecond = SourcePublicationSnapshot(second)
        val completedDefault = assertIs<org.cangnova.cangjie.cfir.CfirContentSource.Published>(rank.contentSource)
            .snapshot().defaultValue(rank)

        val initializer = assertIs<CfirLiteralExpression>(provider.sourceInitializerOf(stamp))
        assertSame(initializer, stamp.initializer)
        assertEquals("19", initializer.value)
        val third = assertNotNull(provider.completedSourceExportSnapshotOf(marked, includeContent = true))
        val g2 = assertNotNull(third.graph)
        assertTrue(g1 !== g2, "显式 initializer 的普通节点必须产生第二次真实 graph 升级")
        assertTrue(second.publicationIdentity !== third.publicationIdentity)
        assertFalse(g0.containsOrigin(initializer))
        assertFalse(g1.containsOrigin(initializer))
        assertSame(stampSlot, g2.expressionOf(initializer))
        assertSame(rankSlot, g2.expressionOf(default))
        listOf(g0, g1, g2).forEach { assertSame(g2, third.finalPublishedGraph(it)) }
        assertAliasProductsUnchanged(g0, g2)
        assertAliasProductsUnchanged(g1, g2)
        assertSame(default, rank.defaultValue)
        assertSame(completedDefault, assertIs<org.cangnova.cangjie.cfir.CfirContentSource.Published>(rank.contentSource)
            .snapshot().defaultValue(rank))
        assertNull(constructor.body)
        savedFirst.assertUnchanged()
        savedSecond.assertUnchanged()
        assertFalse(first.graphPublications.containsKey(g1))
        assertFalse(first.graphPublications.containsKey(g2))
        assertFalse(second.graphPublications.containsKey(g2))
        val inputs = owner.withReadOnlyContent {
            CjoExportSourceSnapshots.completedInputs(emptyList(), true, listOf(g0, g1, g2), listOf(marked, tag))
        }
        assertSame(g2, inputs.graphs.single())
        assertSame(third.publicationIdentity, inputs.snapshots.single().publicationIdentity)
        inputs.validateUnchanged()
    }

    /** fence 正控证明被动 API 不进入 materialize；主动 API 负控必须被同一原 owner 拒绝。 */
    @Test
    fun completedSourceSnapshotReadsUnderOriginalFenceWithoutMaterializationOrTokenRefresh(@TempDir root: Path) {
        val (provider, path) = exportPreparationProvider(root)
        val marked = assertIs<CfirClass>(provider.deserializeCjoDeclaration(path, "r38.header", 0))
        val old = assertNotNull(provider.sourceExportSnapshotOf(marked, includeContent = false))
        val graph = assertNotNull(old.graph)
        val owner = assertIs<CjoSourceGraphPublicationOwner>(marked.contentSource)
        val content = assertIs<CfirImportedDeclarationHeaderSource>(marked.contentSource).snapshot()
        val saved = SourcePublicationSnapshot(old)
        owner.withReadOnlyContent {
            repeat(3) {
                val next = assertNotNull(provider.completedSourceExportSnapshotOf(marked, includeContent = false))
                assertSame(old.publicationIdentity, next.publicationIdentity)
                assertSame(old.graphPublications, next.graphPublications)
                assertSame(graph, next.graph)
                assertSame(old.publicationIdentity, owner.completedSnapshotOf(graph).publicationIdentity)
                owner.requirePublicationUnchanged(old.publicationIdentity)
                assertSame(content, assertIs<CfirImportedDeclarationHeaderSource>(marked.contentSource).snapshot())
            }
            val failure = assertFailsWith<IllegalStateException> {
                provider.sourceExportSnapshotOf(marked, includeContent = false)
            }
            assertEquals("Imported content materialization is forbidden in a read-only input region", failure.message)
            assertSame(old.publicationIdentity, owner.currentPublicationIdentity())
        }
        saved.assertUnchanged()
        val tag = assertIs<CfirClass>(old.declarations.getValue(2))
        assertIs<CfirImportedDeclarationHeaderSource>(tag.contentSource).completeHeader(tag)
        assertTrue(old.publicationIdentity !== owner.currentPublicationIdentity())
        assertFailsWith<IllegalStateException> { owner.requirePublicationUnchanged(old.publicationIdentity) }
        saved.assertUnchanged()
    }

    /** 空包没有声明 content owner 可供枚举；独立 Work/Analysis 必须从真实 Published graph 建立 fence。 */
    @Test
    fun emptyPublishedSourceGraphProtectsOriginalContextInIndependentWorkAndAnalysisConsumers(@TempDir root: Path) {
        val path = CjoPackageWriter.write(root.resolve("r41.empty.cjo"), CjoPackageMetadata(
            "r41.empty", "r41-empty", schemaProfile = CjoSchemaProfile.OFFICIAL_CJMP,
            allFiles = listOf("empty.cj"),
        )).toAbsolutePath().normalize()
        val manager = CjoManager(CjoSearchPath(additionalLibrarySearchPaths = listOf(root.toFile()), envProvider = { null }),
            CjoFormatVersionPolicy.Compilation(LanguageVersionSettingsImpl.DEFAULT))
        val loaded = assertNotNull(manager.loadPackageSnapshot("r41.empty"))
        assertSame(loaded, manager.loadedPackageSnapshot(path))
        assertEquals(path, loaded.sourcePath)
        assertEquals(0, loaded.pkg.allDeclsLength)
        assertEquals(0, loaded.pkg.allTypesLength)
        assertEquals(0, loaded.pkg.allExprsLength)
        val module = Module()
        val binding = bindCjoFixture(module, manager, setOf(root))
        val context = binding.contextForSnapshot(loaded, CjoDeserializationMode.FULL)
        // 只由原 loader 的成功根事务生产图和 membership，不构造 Published Builder 或替代 owner。
        val graph = CfirSourceGraphLoader(context).load().graph
        val owner = assertIs<CjoSourceGraphPublicationOwner>(assertIs<CfirSourceGraphProvenance.Published>(graph.provenance).owner)
        assertSame<Any>(context.publishedContentSource, owner)
        assertSame(graph, assertNotNull(context.completedSourceGraph).graph)
        assertTrue(context.declCache.isEmpty())
        assertTrue(context.typeCache.isEmpty())
        assertTrue(graph.expressions.isEmpty())
        assertTrue(graph.borrowedExpressions.isEmpty())
        assertTrue(graph.declarationOwners.isEmpty())
        assertTrue(graph.generatedDeclarations.isEmpty())
        assertTrue(graph.functionContexts.isEmpty())
        assertTrue(graph.functionHeaders.isEmpty())
        assertTrue(graph.functionBodies.isEmpty())
        assertTrue(graph.initializers.isEmpty())
        assertTrue(graph.defaults.isEmpty())
        assertTrue(graph.annotations.isEmpty())
        val snapshot = context.sourceExportSnapshot(loaded, includeContent = true)
        assertSame(loaded, snapshot.source)
        assertSame(graph, snapshot.finalPublishedGraph(graph))
        assertSame(snapshot.publicationIdentity, owner.completedSnapshotOf(graph).publicationIdentity)
        val saved = SourcePublicationSnapshot(snapshot)
        val content = context.publishedContentSource.snapshot()
        val graphs = listOf(graph)
        val work = CfirExportAnalysisInputPreparation.Work(emptyList(), emptyList(), ::r38PhysicalMembers)
        work.complete(canonicalSources = graphs)
        assertTrue(work.declarations.isEmpty(), "空 graph 的 fence 不能依赖声明 inventory")
        var attempts = 0
        var materializationActions = 0
        val workFailure = assertFailsWith<IllegalStateException> {
            work.withReadOnlyInput {
                attempts++
                context.materialize { materializationActions++ }
            }
        }
        assertEquals("Imported content materialization is forbidden in a read-only input region", workFailure.message)
        assertEquals(1, attempts)
        assertEquals(0, materializationActions)
        owner.requirePublicationUnchanged(snapshot.publicationIdentity)
        val prepared = work.freeze()
        assertTrue(prepared.declarations.isEmpty())
        val analysis = CfirExportAnalysis.analyze(emptyList(), emptyList(), ::r38PhysicalMembers,
            canonicalSources = graphs, preparedInput = prepared)
        assertTrue(analysis.capturedDeclarations().isEmpty())
        assertTrue(analysis.hasCanonicalSource)
        assertSame(graph, analysis.canonicalSourceGraphs().single())
        // 唯一外层是公开 consumer 能力；没有 producer 或 test 的 owner.withReadOnlyContent。
        repeat(2) {
            val failure = assertFailsWith<IllegalStateException> {
                analysis.withReadOnlyInput {
                    assertSame(snapshot.publicationIdentity, owner.completedSnapshotOf(graph).publicationIdentity)
                    attempts++
                    context.materialize { materializationActions++ }
                }
            }
            assertEquals("Imported content materialization is forbidden in a read-only input region", failure.message)
            assertTrue(failure.stackTrace.any {
                it.className == CfirDeserializationContext::class.java.name && it.methodName.startsWith("materialize")
            })
            assertEquals(0, materializationActions, "必须在原 materialize 入口拒绝，不能执行 action 后再报告漂移")
            owner.requirePublicationUnchanged(snapshot.publicationIdentity)
            assertSame(content, context.publishedContentSource.snapshot())
            assertSame(snapshot.graphPublications, owner.completedSnapshotOf(graph).graphPublications)
            saved.assertUnchanged()
            analysis.validateSourceUnchanged()
        }
        assertEquals(3, attempts)
        assertSame(snapshot.publicationIdentity, owner.currentPublicationIdentity())
        assertSame(graph, analysis.canonicalSourceGraphs().single())
    }

    /** 公共变体沿用 Published 身份和全部记录，却没有原 context 的成功发布资格。 */
    @Test
    fun externalOriginAliasVariantKeepsPublishedMarkerButCannotEnterSuccessfulHistory(@TempDir root: Path) {
        val (provider, path) = exportPreparationProvider(root)
        val marked = assertIs<CfirClass>(provider.deserializeCjoDeclaration(path, "r38.header", 0))
        val snapshot = assertNotNull(provider.sourceExportSnapshotOf(marked, includeContent = false))
        val graph = assertNotNull(snapshot.graph)
        val owner = assertIs<CjoSourceGraphPublicationOwner>(marked.contentSource)
        val parameter = graph.defaults.keys.single()
        val identity = graph.defaults.getValue(parameter)
        // 实际已绑定声明是合法 CfirElement；仅作为外部新增 origin，不冒充普通节点生产。
        assertFalse(graph.containsOrigin(marked))
        val external = graph.withOriginAliases(mapOf(marked to identity))
        assertTrue(external !== graph)
        assertSame(graph.provenance, external.provenance)
        assertSame(owner, assertIs<CfirSourceGraphProvenance.Published>(external.provenance).owner)
        assertAliasProductsUnchanged(graph, external)
        assertFalse(snapshot.graphPublications.containsKey(external))
        owner.withReadOnlyContent {
            assertSame(graph, owner.completedSnapshotOf(graph).graph)
            val failure = assertFailsWith<IllegalStateException> { owner.completedSnapshotOf(external) }
            assertEquals("SOURCE graph was not successfully published by its actual context", failure.message)
            assertFailsWith<IllegalStateException> { snapshot.finalPublishedGraph(external) }
            assertFailsWith<IllegalStateException> {
                CjoExportSourceSnapshots.completedInputs(emptyList(), false, listOf(external), emptyList())
            }
            owner.requirePublicationUnchanged(snapshot.publicationIdentity)
        }
        assertFalse(graph.containsOrigin(marked))
        assertSame(identity, external.expressionOf(marked))
        assertSame(snapshot.graphPublications, owner.completedSnapshotOf(graph).graphPublications)
    }

    /** 同名同索引的另一真实 binder 和新 symbol copy 都不能被原 provider 被动认领。 */
    @Test
    fun completedSourceSnapshotRejectsForeignOwnerAndDeclarationCopy(@TempDir root: Path) {
        val (provider, path) = exportPreparationProvider(root.resolve("original"))
        val (otherProvider, otherPath) = exportPreparationProvider(root.resolve("other"))
        val marked = assertIs<CfirClass>(provider.deserializeCjoDeclaration(path, "r38.header", 0))
        val other = assertIs<CfirClass>(otherProvider.deserializeCjoDeclaration(otherPath, "r38.header", 0))
        val snapshot = assertNotNull(provider.sourceExportSnapshotOf(marked, includeContent = false))
        val otherSnapshot = assertNotNull(otherProvider.sourceExportSnapshotOf(other, includeContent = false))
        val owner = assertIs<CjoSourceGraphPublicationOwner>(marked.contentSource)
        val otherOwner = assertIs<CjoSourceGraphPublicationOwner>(other.contentSource)
        val graph = assertNotNull(snapshot.graph)
        assertTrue(owner !== otherOwner)
        assertTrue(snapshot.binderIdentity !== otherSnapshot.binderIdentity)
        val copy = buildClassCopy(marked) { symbol = CfirClassSymbol(marked.symbol.classId) }
        assertSame(copy, copy.symbol.cfir)
        assertSame(marked, marked.symbol.cfir)
        owner.withReadOnlyContent {
            otherOwner.withReadOnlyContent {
                assertNull(provider.completedSourceExportSnapshotOf(other, includeContent = false))
                assertNull(otherProvider.completedSourceExportSnapshotOf(marked, includeContent = false))
                assertNull(provider.completedSourceExportSnapshotOf(copy, includeContent = false))
                val failure = assertFailsWith<IllegalStateException> { otherOwner.completedSnapshotOf(graph) }
                assertEquals("SOURCE graph belongs to a different publication owner", failure.message)
                assertFailsWith<IllegalStateException> { otherSnapshot.finalPublishedGraph(graph) }
                owner.requirePublicationUnchanged(snapshot.publicationIdentity)
                otherOwner.requirePublicationUnchanged(otherSnapshot.publicationIdentity)
            }
        }
        assertSame(graph, owner.completedSnapshotOf(graph).graph)
    }

    /** 本地非空 Owned default 图按自身身份保留，不被同包 Published history 替换或认领。 */
    @Test
    fun completedPublicationAssemblyPreservesIndependentOwnedDefaultGraph(@TempDir root: Path) {
        val (provider, path) = exportPreparationProvider(root)
        val tag = assertIs<CfirClass>(provider.deserializeCjoDeclaration(path, "r38.header", 2))
        val snapshot = assertNotNull(provider.sourceExportSnapshotOf(tag, includeContent = false))
        assertIs<CfirImportedDeclarationHeaderSource>(tag.contentSource).completeHeader(tag)
        val constructor = tag.declarations.filterIsInstance<CfirConstructor>().single()
        val ownedConstructorSymbol = CfirConstructorSymbol(constructor.symbol.callableId)
        val parameter = constructor.valueParameters.single()
        val literal = buildLiteralExpressionCopy(assertIs<CfirLiteralExpression>(parameter.defaultValue)) {}
        val owned = buildConstructorCopy(constructor) {
            symbol = ownedConstructorSymbol
            valueParameters.clear()
            valueParameters += buildValueParameterCopy(parameter) {
                symbol = CfirValueParameterSymbol(parameter.symbol.callableId)
                containingDeclarationSymbol = ownedConstructorSymbol
                defaultValue = literal
            }
        }
        val ownedParameter = owned.valueParameters.single()
        assertSame(org.cangnova.cangjie.cfir.CfirContentSource.Owned, ownedParameter.contentSource)
        val builder = CfirSourceGraph.Builder()
        val identity = builder.reserve(literal)
        val originalGraph = assertNotNull(snapshot.graph)
        builder.fill(identity, originalGraph.expression(originalGraph.defaults.getValue(parameter)).copy(context = ownedParameter))
        builder.default(ownedParameter, identity)
        val ownedGraph = builder.finish()
        val owner = assertIs<CjoSourceGraphPublicationOwner>(tag.contentSource)
        val current = owner.completedSnapshotOf(originalGraph)
        val inputs = owner.withReadOnlyContent {
            CjoExportSourceSnapshots.completedInputs(emptyList(), false, listOf(ownedGraph, originalGraph), listOf(tag))
        }
        assertEquals(2, inputs.graphs.size)
        assertSame(ownedGraph, inputs.graphs[0])
        assertSame(current.graph, inputs.graphs[1])
        assertSame(CfirSourceGraphProvenance.Owned, ownedGraph.provenance)
        assertSame(identity, ownedGraph.defaults.getValue(ownedParameter))
        assertSame(identity, ownedGraph.expressionOf(literal))
        assertFalse(current.graphPublications.containsKey(ownedGraph))
        assertFailsWith<IllegalStateException> { owner.completedSnapshotOf(ownedGraph) }
        inputs.validateUnchanged()
    }

    /** 损坏 literal 在实际 Reader 中失败；已完成 header 图和 history 不登记失败 initializer 图。 */
    @Test
    fun failingOriginalProviderInitializerRootDoesNotPublishGraphOrChangeOldPublication(@TempDir root: Path) {
        val (provider, path) = exportPreparationProvider(root, invalidInitializerLiteral = true)
        val tag = assertIs<CfirClass>(provider.deserializeCjoDeclaration(path, "r38.header", 2))
        val snapshot = assertNotNull(provider.sourceExportSnapshotOf(tag, includeContent = false))
        val graph = assertNotNull(snapshot.graph)
        val owner = assertIs<CjoSourceGraphPublicationOwner>(tag.contentSource)
        val contentOwner = assertIs<CfirImportedDeclarationHeaderSource>(tag.contentSource)
        val content = contentOwner.snapshot()
        val stamp = tag.declarations.filterIsInstance<CfirFieldVariable>().single()
        val field = FieldSnapshot(stamp)
        val saved = SourcePublicationSnapshot(snapshot)
        assertNull(stamp.initializer)
        assertFalse(graph.initializers.containsKey(stamp))
        repeat(2) {
            val failure = assertFailsWith<IllegalArgumentException> { provider.sourceInitializerOf(stamp) }
            assertTrue(failure.stackTrace.any { it.className.contains("CfirBodyDeserializer") && it.methodName == "expression" })
            assertTrue(failure.stackTrace.any { it.className == CfirDeserializationContext::class.java.name && it.methodName.startsWith("materialize") })
            val next = assertNotNull(provider.completedSourceExportSnapshotOf(tag, includeContent = false))
            assertSame(snapshot.publicationIdentity, next.publicationIdentity)
            assertSame(snapshot.graphPublications, next.graphPublications)
            assertSame(snapshot.declarations, next.declarations)
            assertSame(snapshot.types, next.types)
            assertSame(snapshot.headers, next.headers)
            assertSame(snapshot.positions, next.positions)
            assertSame(snapshot.constants, next.constants)
            assertSame(snapshot.constantRoots, next.constantRoots)
            assertSame(graph, next.graph)
            assertSame(content, contentOwner.snapshot())
            owner.requirePublicationUnchanged(snapshot.publicationIdentity)
            field.assertUnchanged()
            saved.assertUnchanged()
        }
    }

    /** aliases 只增加 origin；全部保存槽和 record 容器必须是原完成对象。 */
    private fun assertAliasProductsUnchanged(original: CfirSourceGraph, successor: CfirSourceGraph) {
        assertSame(original.provenance, successor.provenance)
        assertSame(original.expressions, successor.expressions)
        assertSame(original.borrowedExpressions, successor.borrowedExpressions)
        assertSame(original.functionBodies, successor.functionBodies)
        assertSame(original.functionHeaders, successor.functionHeaders)
        assertSame(original.initializers, successor.initializers)
        assertSame(original.defaults, successor.defaults)
        assertSame(original.declarationPatterns, successor.declarationPatterns)
        assertSame(original.generatedDeclarations, successor.generatedDeclarations)
        assertSame(original.annotationPlans, successor.annotationPlans)
        assertSame(original.inheritedTypeReferences, successor.inheritedTypeReferences)
        assertSame(original.declarationOwners, successor.declarationOwners)
        assertSame(original.functionContexts, successor.functionContexts)
        assertSame(original.annotations, successor.annotations)
        assertSame(original.constantValues, successor.constantValues)
        assertSame(original.unloadedFunctionBodies, successor.unloadedFunctionBodies)
        original.expressions.forEach { (identity, record) -> assertSame(record, successor.expression(identity)) }
        original.borrowedExpressions.forEach { (identity, record) -> assertSame(record, successor.expression(identity)) }
    }

    /** 记录旧捕获目录的 entries，防止后续根事务原地重定向旧 history 或保存事实。 */
    private class SourcePublicationSnapshot(private val snapshot: CjoSourceExportSnapshot) {
        private val token = snapshot.publicationIdentity
        private val graph = assertNotNull(snapshot.graph)
        private val history = snapshot.graphPublications.entries.map { it.key to it.value }
        private val declarations = snapshot.declarations.entries.map { it.key to it.value }
        private val types = snapshot.types.entries.map { it.key to it.value }
        private val headers = snapshot.headers.entries.map { it.key to it.value }
        private val positions = snapshot.positions.entries.map { it.key to it.value }
        private val constants = snapshot.constants.entries.map { it.key to it.value }
        private val roots = snapshot.constantRoots.entries.map { it.key to it.value }

        fun assertUnchanged() {
            assertSame(token, snapshot.publicationIdentity)
            assertSame(graph, snapshot.graph)
            assertEquals(history.size, snapshot.graphPublications.size)
            history.forEach { (original, final) -> assertSame(final, snapshot.finalPublishedGraph(original)) }
            assertEntriesUnchanged(declarations, snapshot.declarations)
            assertEntriesUnchanged(types, snapshot.types)
            assertEntriesUnchanged(headers, snapshot.headers)
            assertEntriesUnchanged(positions, snapshot.positions)
            assertEntriesUnchanged(constants, snapshot.constants)
            assertEntriesUnchanged(roots, snapshot.constantRoots)
        }

        private fun <K, V> assertEntriesUnchanged(entries: List<Pair<K, V>>, actual: Map<K, V>) {
            assertEquals(entries.size, actual.size)
            entries.forEach { (key, value) -> assertSame(value, actual.getValue(key)) }
        }
    }

    /** 复用本类合法 const ctor/default/Block 格式；使用本包 Tag 和 Int64，不构造 std.core/Any。 */
    private fun exportPreparationProvider(root: Path, invalidInitializerLiteral: Boolean = false): Pair<CfirDeserializedSymbolProvider, Path> {
        val seed = CjoPackageMetadata(
            "r38.header", "r38-header", schemaProfile = CjoSchemaProfile.OFFICIAL_CJMP,
            allFiles = listOf("header.cj"),
            declarations = listOf(
                CjoPackageDeclaration("Marked", DeclKind.ClassDecl, type = 2u, attributes = flags(Attribute.PUBLIC),
                    info = CjoClassInfo(body = listOf(2u)), annotations = listOf(CjoAnnotationMetadata(
                        AnnoKind.Custom, "Tag", target = CjoAnnotationTargetMetadata(PackageIndex.CURRENT.value, index = 4u)))),
                constructor(3u, 2u, 2u),
                CjoPackageDeclaration("Tag", DeclKind.ClassDecl, type = 4u,
                    attributes = flags(Attribute.PUBLIC, Attribute.FROM_COMMON_PART),
                    info = CjoClassInfo(body = listOf(4u, 6u), isAnnotation = true, annotationTargets = 0xffu)),
                constructor(5u, 4u, 3u, listOf(5u), isConst = true),
                parameter("rank", 1u, 1u),
                field("stamp", 4u),
            ),
            types = listOf(CjoTypeMetadata(TypeKind.Int64), nominal(TypeKind.Class, 1u),
                functionType(emptyList(), 2u), nominal(TypeKind.Class, 3u),
                functionType(listOf(1u), 4u), CjoTypeMetadata(TypeKind.Unit)),
            expressions = listOf(integer(),
                CjoExpressionMetadata(ExprKind.Block, type = 6u,
                    additionalInfo = CjoAdditionalExpressionInfoMetadata.Block(listOf(false))),
                CjoExpressionMetadata(ExprKind.Block, type = 6u,
                    additionalInfo = CjoAdditionalExpressionInfoMetadata.Block(listOf(false))),
                integer()),
        )
        // 独立负控只损坏 Bool 文本；保留非零 initializer 和所有正确 ctor/default/Block 槽。
        val input = if (invalidInitializerLiteral) seed.copy(
            declarations = seed.declarations.mapIndexed { index, declaration ->
                if (index == 5) declaration.copy(type = 7u) else declaration
            },
            types = seed.types + CjoTypeMetadata(TypeKind.Bool),
            expressions = seed.expressions.dropLast(1) + CjoExpressionMetadata(ExprKind.LitConstExpr, type = 7u,
                literal = CjoLiteralExpressionMetadata("invalid-bool", LitConstKind.Bool)),
        ) else seed
        val path = CjoPackageWriter.write(root.resolve("r38.header.cjo"), input).toAbsolutePath().normalize()
        val manager = CjoManager(CjoSearchPath(additionalLibrarySearchPaths = listOf(root.toFile()), envProvider = { null }),
            CjoFormatVersionPolicy.Compilation(LanguageVersionSettingsImpl.DEFAULT))
        val module = Module()
        module.session.register(org.cangnova.cangjie.cfir.types.TypeComponents::class,
            org.cangnova.cangjie.cfir.types.TypeComponents(module.session))
        val provider = CfirDeserializedSymbolProvider(
            module.session, manager, CfirCangJieScopeProvider(), module,
            moduleDataProvider = cjoFixtureModuleDataProvider(module, setOf(root)),
        )
        module.session.register(CfirSymbolProvider::class, provider)
        module.session.register(org.cangnova.cangjie.cfir.source.CfirImportedSourceGraphProvider::class, provider)
        return provider to path
    }

    /** metadata 与 normal 各自提交；normal 根显式消费原 ctor/default 和本 case 的 initializer 槽。 */
    private fun loadMarked(context: CfirDeserializationContext, typeFirst: Boolean, message: Message): CfirClass {
        val first = if (typeFirst) CfirTypeDeserializer(context).deserializeType(2) else null
        val marked = context.materialize {
            val reader = context.createDeclDeserializer()
            val marked = assertIs<CfirClass>(reader.deserializeDecl(MARKED))
            reader.deserializeDecl(TAG)
            reader.deserializeParameterDefaults(listOf(MESSAGE, RANK))
            reader.deserializeVariableInitializer(STAMP)
            if (message != Message.EXPLICIT_INTEGER) reader.deserializeVariableInitializer(LENGTH)
            marked
        }
        if (typeFirst) assertSame(first, context.typeCache[2])
        assertSame(marked, context.declCache[MARKED])
        return marked
    }

    private fun assertAnnotation(context: CfirDeserializationContext, marked: CfirClass, message: Message) {
        val tag = assertIs<CfirClass>(context.declCache[TAG])
        val constructor = tag.declarations.filterIsInstance<CfirConstructor>().single()
        assertSame(constructor, context.declCache[TAG_INIT])
        assertEquals(tag.symbol.classId, constructor.symbol.callableId.classId)
        assertEquals(tag.symbol.classId, assertIs<ConeClassLikeType>(constructor.returnTypeRef.coneType).classId)
        val call = assertIs<CfirAnnotationCall>(marked.annotations.single())
        assertSame(marked.symbol, call.containingDeclarationSymbol)
        assertSame(constructor.symbol, assertIs<CfirResolvedNamedReference>(call.calleeReference).resolvedSymbol)
        assertEquals(tag.symbol.classId, call.annotationClassId)
        assertNotNull(marked.annotationInfo)
        assertTrue(assertNotNull(tag.annotationInfo).isAnnotation)
        val view = assertNotNull(call.argumentView)
        assertEquals(2, view.entries.size)
        for ((index, name) in listOf("message", "rank").withIndex()) {
            val parameter = constructor.valueParameters[index]
            assertSame(parameter, context.declCache[if (index == 0) MESSAGE else RANK])
            assertSame(constructor.symbol, parameter.containingDeclarationSymbol)
            val parameterOrigin = assertNotNull(parameter.cjoDeclarationOrigin)
            assertEquals(if (index == 0) MESSAGE else RANK, parameterOrigin.declarationIndex)
            assertEquals(assertNotNull(constructor.cjoDeclarationOrigin).packagePath, parameterOrigin.packagePath)
            val entry = view.entries.single { it.resolvedParameter === parameter }
            assertEquals(CfirAnnotationArgumentStatus.RESOLVED, entry.status)
            val fromDefault = index == 1 || message == Message.DEFAULT_STRING
            assertEquals(fromDefault, entry.isDefaultOrigin)
            val literal = assertIs<CfirLiteralExpression>(entry.constantExpression)
            if (fromDefault) {
                assertNull(entry.argument)
                assertSame(parameter.defaultValue, literal)
                assertEquals(Name.identifier(name), entry.explicitName)
            } else {
                assertSame(literal, call.argumentMapping.mapping[Name.identifier(name)])
            }
            if (index == 1 || message == Message.EXPLICIT_INTEGER) {
                assertEquals("19", literal.value)
                assertEquals(ConePrimitiveType.INT64, literal.coneTypeOrNull)
            } else {
                assertEquals("ready中😀", literal.value)
                val type = assertIs<ConeStructType>(literal.coneTypeOrNull)
                assertEquals(StdlibClassIds.String, type.classId)
                val symbol = assertIs<ConeClassLikeLookupTagWithFixedSymbol>(type.lookupTag).symbol
                assertTrue(symbol.isBound, "default/view 中的类型必须已有完整的真实 classifier")
                assertSame(context.declCache[STRING], symbol.cfir)
                assertSame(context.typeCache[6], type)
            }
        }
    }

    private fun assertStringHeaders(fixture: Fixture) {
        val context = fixture.context
        val string = assertIs<CfirStruct>(context.declCache[STRING])
        assertEquals(StdlibClassIds.String, string.symbol.classId)
        val length = assertIs<CfirFieldVariable>(context.declCache[LENGTH])
        assertSame(length, string.declarations.filterIsInstance<CfirFieldVariable>().single())
        assertEquals(string.symbol.classId, length.symbol.callableId.classId)
        assertEquals(LENGTH, assertNotNull(length.cjoDeclarationOrigin).declarationIndex)
        val relation = assertIs<CfirInterface>(context.declCache[RELATION])
        val parameter = assertIs<CfirTypeParameter>(relation.typeParameters.single())
        assertSame(parameter, context.declCache[TYPE_PARAMETER])
        assertSame(relation.symbol, parameter.containingDeclarationSymbol)
        val self = assertIs<ConeClassLikeType>(context.typeCache[11])
        assertSame(parameter.symbol, assertIs<ConeTypeParameterType>(self.typeArguments.single().type).lookupTag.typeParameterSymbol)
        val bound = assertIs<ConeClassLikeType>(parameter.bounds.single().coneType)
        val marker = assertIs<CfirClassLikeDeclaration>(assertNotNull(fixture.markerProvider.getClassLikeSymbolByClassId(bound.classId)).cfir)
        assertSame(marker.symbol, assertIs<ConeClassLikeLookupTagWithFixedSymbol>(bound.lookupTag).symbol)
        val constraint = assertNotNull(context.genericConstraints[relation.symbol]).single()
        assertSame(parameter.symbol, assertIs<ConeTypeParameterType>(constraint.subject).lookupTag.typeParameterSymbol)
        assertSame(bound, constraint.upperBounds.single().coneType)
        val applied = assertIs<ConeClassLikeType>(string.superTypeRefs.first().coneType)
        assertSame(relation.symbol, assertIs<ConeClassLikeLookupTagWithFixedSymbol>(applied.lookupTag).symbol)
        assertSame(context.typeCache[6], applied.typeArguments.single().type)
    }

    private fun assertCollectedEmpty(field: CfirFieldVariable) {
        val dependencies = assertIs<CjmpInitializationDependencies.Collected<*>>(field.initializationDependencies)
        assertTrue(dependencies.declarations.isEmpty(), "present-empty dependencies 不能保留 NotCollected")
    }

    /** 每个活动 Root 分别保存上一公开完成代；observer 与失败回滚均比较四个容器的身份。 */
    private class RootSnapshot(private val context: CfirDeserializationContext) {
        val declarations = context.declCache
        val types = context.typeCache
        private val constraints = context.genericConstraints
        private val content = context.publishedContentSource.snapshot()

        fun assertUnchanged() {
            assertSame(declarations, context.declCache)
            assertSame(types, context.typeCache)
            assertSame(constraints, context.genericConstraints)
            assertSame(content, context.publishedContentSource.snapshot())
        }
    }

    /** 记录消费者已取得的字段产品；后续公开请求和失败事务均只能复用这些对象。 */
    private class FieldSnapshot(val field: CfirFieldVariable) {
        private val symbol = field.symbol
        private val status = field.status
        private val typeRef = field.returnTypeRef
        private val initializer = field.initializer
        private val dependencies = field.initializationDependencies
        private val facts = field.exportFacts
        private val annotationInfo = field.annotationInfo

        fun assertUnchanged() {
            assertSame(symbol, field.symbol)
            assertSame(status, field.status)
            assertSame(typeRef, field.returnTypeRef)
            assertSame(initializer, field.initializer)
            assertSame(dependencies, field.initializationDependencies)
            assertSame(facts, field.exportFacts)
            assertSame(annotationInfo, field.annotationInfo)
        }
    }

    /** observer 位于真实 imported FullId binder；不替代 loader，也不查询/驱动 completion queue。 */
    private class Fixture(
        val context: CfirDeserializationContext,
        val provider: CfirDeserializedSymbolProvider,
        val markerProvider: CfirDeserializedSymbolProvider,
    ) {
        var beforeMarkerHeader: (() -> Unit)? = null
    }

    private fun fixture(root: Path, message: Message): Fixture {
        val marker = CjoPackageMetadata("lifecycle.marker", "lifecycle-marker",
            schemaProfile = CjoSchemaProfile.OFFICIAL_CJMP, allFiles = listOf("marker.cj"),
            declarations = listOf(CjoPackageDeclaration("Marker", DeclKind.InterfaceDecl, type = 1u,
                attributes = flags(Attribute.PUBLIC), exportId = "lifecycle.marker::Marker", info = CjoInterfaceInfo())),
            types = listOf(nominal(TypeKind.Interface, 1u)))
        val coreRoot = Files.createDirectories(root.resolve("core"))
        val markerRoot = Files.createDirectories(root.resolve("marker"))
        CjoPackageWriter.write(markerRoot.resolve("lifecycle.marker.cjo"), marker)
        CjoPackageWriter.write(coreRoot.resolve("std.core.cjo"), seed(message))
        val materialization = CjoDeclarationMaterialization()
        val policy = CjoFormatVersionPolicy.Compilation(LanguageVersionSettingsImpl.DEFAULT)
        val markerManager = CjoManager(
            CjoSearchPath(additionalLibrarySearchPaths = listOf(markerRoot.toFile()), envProvider = { null }),
            policy,
            declarationMaterialization = materialization,
        )
        val markerModule = Module()
        val markerProvider = CfirDeserializedSymbolProvider(
            markerModule.session, markerManager, CfirCangJieScopeProvider(), markerModule,
            moduleDataProvider = cjoFixtureModuleDataProvider(markerModule, setOf(markerRoot)),
        )
        markerModule.session.register(CfirSymbolProvider::class, markerProvider)
        val manager = CjoManager(
            CjoSearchPath(additionalLibrarySearchPaths = listOf(coreRoot.toFile()), envProvider = { null }),
            policy,
            declarationMaterialization = materialization,
        )
        val module = Module()
        lateinit var fixture: Fixture
        val provider = CfirDeserializedSymbolProvider(
            module.session, manager, CfirCangJieScopeProvider(), module,
            moduleDataProvider = cjoFixtureModuleDataProvider(module, setOf(coreRoot)),
            importedPackageContextProvider = CjoPackageContextProvider { packageName, mode ->
                assertEquals(marker.fullPackageName, packageName)
                fixture.beforeMarkerHeader?.invoke()
                markerProvider.getOwnPackageContext(packageName, mode)
            },
        )
        module.session.register(CfirSymbolProvider::class, provider)
        val context = assertNotNull(provider.getOwnPackageContext("std.core", CjoDeserializationMode.FULL))
        fixture = Fixture(context, provider, markerProvider)
        return fixture
    }

    /**
     * @Tag[message: 19] / @Tag[message: "ready中😀"] / @Tag[]，真实目标为 const init，
     * rank!: Int64 = 19；default 分支另有 message!: Any = "ready中😀"。
     * Tag.stamp 和 String.length 都有合法 literal 初值及已知空依赖。
     * 类型引用图为 String <: Relation<String> & Marker，Relation<T> where T <: Marker。
     */
    private fun seed(message: Message): CjoPackageMetadata = CjoPackageMetadata(
        fullPackageName = "std.core", moduleName = "declaration-completion-lifecycle",
        schemaProfile = CjoSchemaProfile.OFFICIAL_CJMP, allFiles = listOf("completion.cj"),
        imports = listOf("lifecycle.marker"),
        declarations = listOf(
            CjoPackageDeclaration("Marked", DeclKind.ClassDecl, type = 3u, attributes = flags(Attribute.PUBLIC),
                info = CjoClassInfo(body = listOf(2u)), annotations = listOf(CjoAnnotationMetadata(AnnoKind.Custom, "Tag",
                    arguments = when (message) {
                        Message.DEFAULT_STRING -> emptyList()
                        Message.EXPLICIT_STRING, Message.EXPLICIT_INTEGER -> listOf(CjoAnnotationArgumentMetadata("message", 8u))
                    }, target = CjoAnnotationTargetMetadata(PackageIndex.CURRENT.value, index = 4u)))),
            constructor(4u, 3u, body = 3u),
            CjoPackageDeclaration("Tag", DeclKind.ClassDecl, type = 5u,
                attributes = flags(Attribute.PUBLIC, Attribute.FROM_COMMON_PART),
                info = CjoClassInfo(body = listOf(4u, 7u), isAnnotation = true, annotationTargets = 0xffu)),
            constructor(6u, 5u, body = 4u, parameters = listOf(5u, 6u), isConst = true),
            parameter("message", 2u, if (message == Message.DEFAULT_STRING) 1u else 0u),
            parameter("rank", 1u, 2u),
            field("stamp", initializer = 5u),
            CjoPackageDeclaration("Any", DeclKind.InterfaceDecl, type = 2u,
                attributes = flags(Attribute.PUBLIC), info = CjoInterfaceInfo()),
            CjoPackageDeclaration("String", DeclKind.StructDecl, type = 7u,
                attributes = flags(Attribute.PUBLIC, Attribute.FROM_COMMON_PART),
                info = CjoStructInfo(inheritedTypes = listOf(10u, 8u), body = listOf(10u, 11u))),
            field("length", initializer = 6u),
            constructor(11u, 7u, body = 7u, isConst = true),
            CjoPackageDeclaration("Relation", DeclKind.InterfaceDecl, type = 12u,
                attributes = flags(Attribute.PUBLIC), info = CjoInterfaceInfo(),
                generic = CjoGenericMetadata(listOf(13u), listOf(CjoConstraintMetadata(9u, listOf(8u))))),
            CjoPackageDeclaration("T", DeclKind.GenericParamDecl, isTopLevel = false, type = 9u),
        ),
        types = listOf(
            CjoTypeMetadata(TypeKind.Int64), nominal(TypeKind.Interface, 8u),
            nominal(TypeKind.Class, 1u), functionType(emptyList(), 3u),
            nominal(TypeKind.Class, 3u), functionType(listOf(2u, 1u), 5u),
            nominal(TypeKind.Struct, 9u),
            CjoTypeMetadata(TypeKind.Interface, semanticInfo = CjoCompositeTypeInfoMetadata(
                declarationIndex = 0u, packageId = 0, declarationKey = "lifecycle.marker::Marker")),
            CjoTypeMetadata(TypeKind.Generic, semanticInfo = CjoGenericTypeInfoMetadata(13u, listOf(8u))),
            nominal(TypeKind.Interface, 12u, listOf(7u)), functionType(emptyList(), 7u),
            nominal(TypeKind.Interface, 12u, listOf(9u)), CjoTypeMetadata(TypeKind.Unit),
        ),
        expressions = listOf(
            CjoExpressionMetadata(ExprKind.LitConstExpr, type = 7u,
                literal = CjoLiteralExpressionMetadata("ready中😀", LitConstKind.String)),
            CjoExpressionMetadata(ExprKind.LitConstExpr, type = 1u,
                literal = CjoLiteralExpressionMetadata("19", LitConstKind.Integer)),
            emptyConstructorBlock(), // Marked.init 正文，独立 e 身份。
            emptyConstructorBlock(), // Tag.const init 正文，独立 e 身份。
            integer(), // Tag.stamp 初值，不借用 rank.default。
            integer(), // String.length 初值，不借用 Tag.stamp。
            emptyConstructorBlock(), // String.const init 正文，独立 e 身份。
            if (message == Message.EXPLICIT_INTEGER) integer() else CjoExpressionMetadata(
                ExprKind.LitConstExpr, type = 7u, literal = CjoLiteralExpressionMetadata("ready中😀", LitConstKind.String)),
        ),
    )

    private fun constructor(type: UInt, result: UInt, body: UInt, parameters: List<UInt> = emptyList(), isConst: Boolean = false) =
        CjoPackageDeclaration("init", isTopLevel = false, type = type,
            attributes = flags(Attribute.PUBLIC, Attribute.CONSTRUCTOR),
            info = CjoFunctionInfo(isConst = isConst, body = CjoFunctionBodyInfo(
                parameterLists = listOf(parameters), desugaredParameterLists = listOf(parameters.map { 0u }),
                returnType = result, body = body)))

    /** 空 Block 的最后一个 false 是 native isExpr 的终止槽；Unit 是真实 t13，不是 wire0。 */
    private fun emptyConstructorBlock() = CjoExpressionMetadata(ExprKind.Block, type = 13u,
        additionalInfo = CjoAdditionalExpressionInfoMetadata.Block(listOf(false)))

    private fun integer() = CjoExpressionMetadata(ExprKind.LitConstExpr, type = 1u,
        literal = CjoLiteralExpressionMetadata("19", LitConstKind.Integer))

    private fun parameter(name: String, type: UInt, default: UInt) = CjoPackageDeclaration(name, DeclKind.FuncParam,
        isTopLevel = false, type = type,
        attributes = if (default == 0u) emptyList() else flags(Attribute.HAS_INITIAL),
        info = CjoParameterInfo(isNamed = true, isMember = false, defaultValue = default))

    private fun field(name: String, initializer: UInt) = CjoPackageDeclaration(name, DeclKind.VarDecl, isTopLevel = false, type = 1u,
        attributes = flags(Attribute.PUBLIC, Attribute.FROM_COMMON_PART, Attribute.INITIALIZED),
        info = CjoVariableInfo(isVar = false, initializer = initializer))

    private fun nominal(kind: UShort, declaration: UInt, arguments: List<UInt> = emptyList()) =
        CjoTypeMetadata(kind, arguments, semanticInfo = CjoCompositeTypeInfoMetadata(declaration))

    private fun functionType(parameters: List<UInt>, result: UInt) =
        CjoTypeMetadata(TypeKind.Func, parameters, semanticInfo = CjoFunctionTypeInfoMetadata(result, false, false))

    private fun flags(vararg attributes: Attribute): List<ULong> =
        MutableList(attributes.maxOf { it.ordinal / 64 } + 1) { 0uL }.apply {
            attributes.forEach { this[it.ordinal / 64] = this[it.ordinal / 64] or (1uL shl (it.ordinal % 64)) }
        }

    private enum class Message { EXPLICIT_INTEGER, EXPLICIT_STRING, DEFAULT_STRING }

    /** 与现有 CJO tests 使用同一 library session 组件，不引入替代 reader/binder。 */
    private class Module : CfirModuleData() {
        override val name = Name.identifier("declaration-completion-lifecycle")
        override val dependencies = emptyList<CfirModuleData>()
        override val refinementDependencies = emptyList<CfirModuleData>()
        override val allRefinementDependencies = emptyList<CfirModuleData>()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform = CfirPlatform.DEFAULT
        override val isCommon = false
        override val stableModuleName = "declaration-completion-lifecycle"
        override val session = object : CfirSession(Kind.Library) {}.also {
            it.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
            it.register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
        }
        init { bindSession(session) }
    }

    private companion object {
        const val MARKED = 0
        const val TAG = 2
        const val TAG_INIT = 3
        const val MESSAGE = 4
        const val RANK = 5
        const val STAMP = 6
        const val STRING = 8
        const val LENGTH = 9
        const val RELATION = 11
        const val TYPE_PARAMETER = 12
    }
}
