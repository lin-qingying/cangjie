@file:OptIn(ExperimentalUnsignedTypes::class, org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.AnnoKind
import PackageFormat.ConstValue
import PackageFormat.DeclKind
import PackageFormat.ExprKind
import PackageFormat.FuncInfo
import PackageFormat.LitConstKind
import PackageFormat.Package
import PackageFormat.PatternKind
import PackageFormat.Position
import PackageFormat.TypeKind
import PackageFormat.VarInfo
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.evaluatedInitializer
import org.cangnova.cangjie.cfir.declarations.builder.buildFieldVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.expressions.CfirConstantValueExpression
import org.cangnova.cangjie.cfir.CfirEvaluatorResult
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedAccessExpression
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysisSettings
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirLibrarySessionProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.source.CfirSourceGraphProducer
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoConstValueReader
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.serialization.provider.CfirExtendProviderComposer
import org.cangnova.cangjie.cfir.session.*
import org.cangnova.cangjie.cfir.source.*
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirDummyCompilerLazyDeclarationResolver
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.TypeComponents
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.resolve.calls.mpp.CjmpContainerKind
import org.cangnova.cangjie.resolve.calls.mpp.CjmpInitializationDependencies
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMergeContainer
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMergeMember
import org.cangnova.cangjie.source.toCjPsiSourceElement
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
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
 * 统一 SOURCE/imported encoder 的正式消费合同；不调用 decoder，也不手建 imported SOURCE 图。
 *
 * SOURCE 正例依据固定 native 14276740 的 ASTWriter.cpp:770–808、1219–1292、1648–1774
 * 与 ExprWriter.cpp:59–84、689–848。完成产品与统一池的框架依据固定 Kotlin 3c7a504 的
 * IrFileSerializer.kt:344–354、1153–1215 及 IrFileDeserializer.kt:251–278。
 * Position 独立槽、空参数组及非空 v 输入明确属于 schema 消费合同，依据同一 native
 * ModuleFormat.fbs:36–43、69–119、527–534、675 与 supplementary-storage-authority.md。
 * 固定 native 不写 v/Pattern.values；这里的 typed-schema 输入不冒充 cjc SOURCE 产物。
 *
 * common 准入使用真实显式路径及 FROM_COMMON_PART；测试从不清除 IMPORTED。
 * schema 种子经 writer/provider 建立后，也连续经过同一个 producer 两次并由 provider 消费。
 * 正式统一入口为 produce(canonicalSources: Collection<CfirSourceGraph>)。
 */
class CjoUnifiedSourceEncoderTest : CjParsingTestCase("", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition()) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    /** 同一装载包的两个语义 binder 各自拥有真实 d 与正文。 */
    @Test
    fun sharedLoadedPackageRetainsBothDeclarationBinders(@TempDir root: Path) {
        val first = load(root, produceSource(sourceConstant("sharedbinders", "answer", 42)))
        val secondModule = Module(CfirSession.Kind.Library)
        val secondProvider = CfirDeserializedSymbolProvider(
            secondModule.session, first.manager, CfirCangJieScopeProvider(), secondModule,
        )
        secondModule.registerProvider(secondProvider)
        val original = first.variable("answer")
        val second = assertIs<CfirFieldVariable>(
            secondProvider.getTopLevelCallableSymbols(FqName("sharedbinders"), Name.identifier("answer")).single().cfir,
        )
        assertNotSame(original, second)

        val snapshots = CjoExportSourceSnapshots.sourceExportSnapshots(listOf(original, second, original), includeContent = true)
        assertEquals(2, snapshots.size)
        val (originalSnapshot, secondSnapshot) = snapshots
        assertSame(first.snapshot, originalSnapshot.source)
        assertSame(first.snapshot, secondSnapshot.source)
        assertNotSame(originalSnapshot.binderIdentity, secondSnapshot.binderIdentity)
        val originalIndex = kotlin.test.assertNotNull(original.cjoDeclarationOrigin).declarationIndex
        val secondIndex = kotlin.test.assertNotNull(second.cjoDeclarationOrigin).declarationIndex
        assertEquals(originalIndex, secondIndex)
        assertSame(original, originalSnapshot.declarations.getValue(originalIndex))
        assertSame(second, secondSnapshot.declarations.getValue(secondIndex))
        assertNotSame(originalSnapshot.graph, secondSnapshot.graph)
    }

    /** native SOURCE：包只有 global const，正式 SOURCE producer → writer → common provider → producer。 */
    @Test
    fun sourceInitializerWithoutAFunctionSurvivesFormalCommonReexport(@TempDir root: Path) {
        val source = sourceConstant("unifiedconstant", "answer", 42)
        val metadata = produceSource(source)
        assertFalse(metadata.declarations.any { it.kind == DeclKind.FuncDecl })
        assertEquals(0, metadata.values.size)
        val first = load(root.resolve("first"), metadata)
        assertDefinition(first.variable("answer"), first)
        val graph = first.graph()
        assertTrue(graph.functionBodies.isEmpty())
        assertEquals("42", literal(first, "answer"))
        val last = reencode(root.resolve("second"), first)
        assertEquals("42", literal(last, "answer"))
        assertNotSame(first.variable("answer"), last.variable("answer"))
        assertNotSame(graph, last.graph())
        assertEquals(1, last.graph().expressions.size)
        assertCanonical(last)
        assertCanonical(first)
    }

    /** native FullId：ordinary IMPORTED 只借用外包 target，不能把其可读 initializer 认领成本包定义。 */
    @Test
    fun ordinaryImportedTargetRemainsAnExternalBorrowedReference(@TempDir root: Path) {
        val seed = produceSource(sourceConstant("borrowedlibrary", "answer", 7))
        val imported = load(root.resolve("library"), seed, common = false)
        val target = imported.variable("answer")
        assertTrue(target.exportFacts.isImported)
        assertEquals("7", literal(imported, "answer"))
        val source = sourceConstant("borrower", "view", 7, borrowed = target)
        registerCjoSourceFixtureDependencies(source.module.session, imported.provider)
        val input = CfirSourceGraphProducer.produce(listOf(source.file))
        assertSame(target, assertIs<CfirSourceReferenceTarget.Declaration>(assertIs<CfirSourceExpressionPayload.Reference>(
            input.expression(input.initializers.getValue(source.variable)).payload).target).declaration)
        val metadata = CfirCjoPackageMetadataProducer.produce(
            listOf(source.file), loadedPackages = listOf(imported.snapshot), canonicalSources = listOf(input),
        )
        assertEquals(listOf("view"), metadata.declarations.filter { it.isTopLevel }.map { it.identifier })
        assertEquals(1, metadata.expressions.size)
        val reference = kotlin.test.assertNotNull(metadata.expressions.single().reference?.target)
        assertEquals("borrowedlibrary", metadata.imports[reference.pkgId])
        assertEquals(kotlin.test.assertNotNull(imported.wire.allDecls(0)).exportId, reference.decl)
        val consumed = load(root.resolve("consumer"), metadata, common = false,
            searchRoots = listOf(imported.path.parent))
        val graph = consumed.graph()
        val rebound = assertIs<CfirSourceReferenceTarget.Declaration>(assertIs<CfirSourceExpressionPayload.Reference>(
            graph.expression(graph.initializers.getValue(consumed.variable("view"))).payload).target).declaration
        assertSame(rebound, consumed.provider.getTopLevelCallableSymbols(FqName("borrowedlibrary"), Name.identifier("answer")).single().cfir)
        assertTrue(rebound.exportFacts.isImported)
        assertEquals(imported.path.toString(), kotlin.test.assertNotNull(rebound.cjoDeclarationOrigin).packagePath)
        assertFalse(graph.initializers.containsKey(rebound))
        assertCanonical(consumed)
        assertSame(target, imported.variable("answer"))
        assertTrue(target.exportFacts.isImported)
    }

    /** native CUSTOM literal args 合同：真实 annotation constructor F 为 body0，实参仍走同一 e 池。 */
    @Test
    fun annotationOnlyPackageUsesItsRealOwnerAndArgumentIdentity(@TempDir root: Path) {
        // Tag 的具名 rank 实参；annotation class 的 const init 正文没有保存。
        val input = metadata("unifiedannotation", listOf(
            CjoPackageDeclaration("Tag", DeclKind.ClassDecl, type = 2u, attributes = flags(Attribute.PUBLIC),
                info = CjoClassInfo(body = listOf(2u), isAnnotation = true, annotationTargets = 0xffu)),
            CjoPackageDeclaration("init", isTopLevel = false, type = 3u, attributes = flags(Attribute.PUBLIC, Attribute.CONSTRUCTOR),
                info = CjoFunctionInfo(isConst = true, body = CjoFunctionBodyInfo(listOf(listOf(3u)), listOf(listOf(0u)), returnType = 2u))),
            parameter("rank").copy(info = CjoParameterInfo(isNamed = true, isMember = false)),
            CjoPackageDeclaration("Marked", DeclKind.StructDecl, type = 4u, attributes = flags(Attribute.PUBLIC),
                annotations = listOf(CjoAnnotationMetadata(AnnoKind.Custom, "Tag",
                    listOf(CjoAnnotationArgumentMetadata("rank", 1u)),
                    CjoAnnotationTargetMetadata(PackageIndex.CURRENT.value, index = 2u))), info = CjoStructInfo()),
        ), listOf(CjoTypeMetadata(TypeKind.Int64), nominal(1u), functionType(listOf(1u), 2u),
            CjoTypeMetadata(TypeKind.Struct, semanticInfo = CjoCompositeTypeInfoMetadata(4u))), listOf(integer("1")))
        val chain = schemaChain(root, input, officialCore())
        for (fixture in chain) {
            val owner = kotlin.test.assertNotNull(fixture.provider.getClassLikeSymbolByClassId(
                ClassId.topLevel(FqName("unifiedannotation.Marked")))).cfir
            assertSame(owner, fixture.roots.single { it === owner })
            val graph = fixture.graph()
            val constructor = fixture.classifier("Tag").declarations.filterIsInstance<CfirConstructor>().single()
            assertFalse(graph.functionHeaders.getValue(constructor).bodyAvailable)
            assertTrue(graph.functionBodies.isEmpty())
            assertTrue(graph.initializers.isEmpty())
            val annotation = graph.annotations.getValue(owner).single()
            assertSame(constructor, annotation.target)
            val argument = annotation.arguments.single()
            assertEquals("rank", argument.name?.asString())
            val record = graph.expression(argument.expression)
            assertSame(owner, record.context)
            assertEquals("1", assertIs<CfirSourceExpressionPayload.Literal>(record.payload).text)
            assertSame(record, graph.expression(argument.expression))
            assertEquals(1, fixture.metadata.expressions.size)
            assertCanonical(fixture)
        }
    }

    /** schema F：空组、零 helper、retType0 独立于实际 Int64 签名及 body0。 */
    @Test
    fun groupedBodylessHeaderKeepsParameterObjectsAndZeroReturnSlot(@TempDir root: Path) {
        for (fixture in schemaChain(root.resolve("grouped"), headersMetadata())) {
            val function = fixture.function("header")
            val before = kotlin.test.assertNotNull(fixture.provider.sourceHeaderOf(function))
            assertNull(fixture.provider.sourceGraphOf(function))
            val graph = fixture.graph()
            val header = graph.functionHeaders.getValue(function)
            assertEquals(before, header)
            assertEquals(listOf(0, 1), header.parameterLists.map { it.size })
            assertEquals(listOf(0, 1), header.desugaredParameterLists.map { it.size })
            assertNull(header.desugaredParameterLists[1].single())
            assertNull(header.returnType)
            assertFalse(header.bodyAvailable)
            assertSame(function.valueParameters.single(), header.parameterLists[1].single())
            assertSame(function.symbol, header.parameterLists[1].single().containingDeclarationSymbol)
            assertSame(function, graph.declarationOwners.getValue(header.parameterLists[1].single()))
            assertEquals(ConePrimitiveType.INT64, function.returnTypeRef.coneType)
            assertEquals(0u, functionBody(fixture, "header").retType)
            assertEquals(0u, functionBody(fixture, "header").body)
            assertTrue(graph.expressions.isEmpty())
            assertNull(function.body)
            assertCanonical(fixture)
        }
        assertEmptyFunctionHeader(root.resolve("empty"))
    }

    /** schema F 的空向量仍是存在的内嵌 F，不造参数、return node 或空 Block。 */
    private fun assertEmptyFunctionHeader(root: Path) {
        val input = headersMetadata().copy(declarations = listOf(
            headersMetadata().declarations.first().copy(info = CjoFunctionInfo(body = CjoFunctionBodyInfo()))),
            types = listOf(CjoTypeMetadata(TypeKind.Int64), functionType(emptyList(), 1u)))
        for (fixture in schemaChain(root, input)) {
            val function = fixture.function("header")
            val header = kotlin.test.assertNotNull(fixture.provider.sourceHeaderOf(function))
            assertTrue(header.parameterLists.isEmpty())
            assertTrue(header.desugaredParameterLists.isEmpty())
            assertNull(header.returnType)
            assertFalse(header.bodyAvailable)
            assertSame(function, fixture.roots.single())
            assertEquals(0, functionBody(fixture, "header").paramListsLength)
            assertEquals(header, fixture.graph().functionHeaders.getValue(function))
            assertCanonical(fixture)
        }
    }

    /** native default/helper：helper 的拥有者是 parameter，自有 F 不可被 owner F 替代。 */
    @Test
    fun defaultExpressionAndHelperRetainDistinctOwnedSlots(@TempDir root: Path) {
        val input = headersMetadata().copy(declarations = listOf(
            headersMetadata().declarations.first().copy(info = CjoFunctionInfo(body = CjoFunctionBodyInfo(
                listOf(listOf(2u)), listOf(listOf(3u)), returnType = 1u))),
            parameter("value").copy(attributes = flags(Attribute.HAS_INITIAL, Attribute.FROM_COMMON_PART),
                info = CjoParameterInfo(isNamed = true, isMember = false, defaultValue = 1u)),
            CjoPackageDeclaration("default_value", isTopLevel = false, type = 3u,
                attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL, Attribute.FROM_COMMON_PART),
                info = CjoFunctionInfo(body = CjoFunctionBodyInfo(listOf(emptyList()), listOf(emptyList()), returnType = 1u))),
        ), types = listOf(CjoTypeMetadata(TypeKind.Int64), functionType(listOf(1u), 1u), functionType(emptyList(), 1u)),
            expressions = listOf(integer("7")))
        for (fixture in schemaChain(root, input)) {
            val owner = fixture.function("header")
            val parameter = owner.valueParameters.single()
            val graph = fixture.graph()
            val header = graph.functionHeaders.getValue(owner)
            val helper = kotlin.test.assertNotNull(header.desugaredParameterLists.single().single())
            assertSame(parameter, header.parameterLists.single().single())
            assertSame(helper, kotlin.test.assertNotNull(parameter.defaultValueMetadata).function)
            assertSame(parameter, graph.declarationOwners.getValue(helper))
            assertSame(owner, graph.declarationOwners.getValue(parameter))
            assertSame(parameter, graph.expression(graph.defaults.getValue(parameter)).context)
            assertEquals("7", assertIs<CfirSourceExpressionPayload.Literal>(
                graph.expression(graph.defaults.getValue(parameter)).payload).text)
            assertTrue(graph.functionHeaders.getValue(helper).parameterLists.single().isEmpty())
            assertNull(helper.body)
            assertCanonical(fixture)
        }
    }

    /** native SOURCE + 带明确 typed-schema 值的实际 common snapshots：同 d0/e1/t1/v0 各域隔离。 */
    @Test
    fun sourceAndTwoSnapshotsWithEqualRawOrdinalsHaveIndependentIdentities(@TempDir root: Path) {
        val left = load(root.resolve("left"), withTypedValue(produceSource(sourceConstant("unifiedmix", "left", 7)), "LeftNode", "leftValue"))
        val right = load(root.resolve("right"), withTypedValue(produceSource(sourceConstant("unifiedmix", "right", 9)), "RightNode", "rightValue"))
        val local = sourceConstant("unifiedmix", "local", 11)
        val a = left.variable("left")
        val b = right.variable("right")
        assertEquals(0, kotlin.test.assertNotNull(a.cjoDeclarationOrigin).declarationIndex)
        assertEquals(0, kotlin.test.assertNotNull(b.cjoDeclarationOrigin).declarationIndex)
        assertEquals(left.wire.allDecls(0)!!.type, right.wire.allDecls(0)!!.type)
        assertEquals(variableInfo(left, "left").initializer, variableInfo(right, "right").initializer)
        assertNotSame(left.snapshot, right.snapshot)
        assertNotSame(a, b)
        assertNotSame(a.symbol, b.symbol)
        val source = CfirSourceGraphProducer.produce(listOf(local.file))
        val ga = left.graph()
        val gb = right.graph()
        assertNotSame(ga, gb)
        assertNotSame(ga.initializers.getValue(a), gb.initializers.getValue(b))
        assertNotSame(left.value("leftValue"), right.value("rightValue"))
        assertEquals(0u, assertIs<CjoConstValueReader.Value.CompositeReference>(CjoConstValueReader.readRaw(variableInfo(left, "leftValue"))).index)
        assertEquals(0u, assertIs<CjoConstValueReader.Value.CompositeReference>(CjoConstValueReader.readRaw(variableInfo(right, "rightValue"))).index)
        val family = family("unifiedmix", listOf(local.variable) + left.roots + right.roots, local.file.moduleData.session)
        val metadata = CfirCjoPackageMetadataProducer.produce(listOf(local.file), familySnapshot = family,
            loadedPackages = listOf(left.snapshot, right.snapshot),
            exportOptions = CjoExportOptions(serializingCommon = true, sourceEncodingContract = CjoSourceEncodingContract.TYPED_SCHEMA),
            canonicalSources = listOf(source, ga, gb))
        val consumed = load(root.resolve("merged"), metadata)
        assertEquals(setOf("local", "left", "right", "leftValue", "rightValue"),
            consumed.roots.filterIsInstance<CfirVariable>().map { it.symbol.name.asString() }.toSet())
        assertEquals(3, consumed.graph().expressions.size)
        for ((name, text) in listOf("local" to "11", "left" to "7", "right" to "9")) {
            assertEquals(text, literal(consumed, name))
        }
        assertEquals(3, listOf("local", "left", "right").map { consumed.graph().initializers.getValue(consumed.variable(it)) }.toSet().size)
        val leftValue = assertIs<CfirConstantValue.ObjectValue>(consumed.value("leftValue"))
        val rightValue = assertIs<CfirConstantValue.ObjectValue>(consumed.value("rightValue"))
        assertNotSame(leftValue, rightValue)
        assertSame(consumed.classifier("LeftNode").declarations.single().symbol, leftValue.fields.keys.single())
        assertSame(consumed.classifier("RightNode").declarations.single().symbol, rightValue.fields.keys.single())
        assertEquals(2, consumed.wire.allValuesLength)
        assertCanonical(left)
        assertCanonical(right)
        assertCanonical(consumed)
        val final = reencode(root.resolve("final"), consumed)
        assertEquals("7", literal(final, "left"))
        assertEquals("9", literal(final, "right"))
        assertCanonical(final)
    }

    /** 同一完成 source/loaded graph 重复传入只复用原身份，不增加 d/e/file。 */
    @Test
    fun repeatedSourceGraphsDoNotDuplicateDefinitionsOrExpressions(@TempDir root: Path) {
        val source = sourceConstant("unifiedrepeat", "answer", 42)
        val graph = CfirSourceGraphProducer.produce(listOf(source.file))
        val once = CfirCjoPackageMetadataProducer.produce(listOf(source.file), canonicalSources = listOf(graph))
        val repeated = CfirCjoPackageMetadataProducer.produce(listOf(source.file), canonicalSources = listOf(graph, graph, graph))
        assertEquals(once.declarations, repeated.declarations)
        assertEquals(once.expressions, repeated.expressions)
        assertEquals(once.allFiles, repeated.allFiles)
        val first = load(root.resolve("first"), repeated)
        val loaded = first.graph()
        val second = reencode(root.resolve("second"), first, listOf(loaded, loaded, loaded))
        assertEquals(1, second.metadata.declarations.size)
        assertEquals(1, second.graph().expressions.size)
        assertEquals("42", literal(second, "answer"))
        assertCanonical(first)
        assertCanonical(second)
    }

    /** native GetExprIndex：Jump.loop 与普通 map 共用已拥有 loop 身份，回边不再 reserve。 */
    @Test
    fun mapAndLoopReferencesReuseTheOwnedExpressionIdentity(@TempDir root: Path) {
        val input = metadata("unifiedloop", listOf(function("entry", 4u, 1u)),
            listOf(CjoTypeMetadata(TypeKind.Int64), CjoTypeMetadata(TypeKind.Bool), CjoTypeMetadata(TypeKind.Nothing),
                functionType(emptyList(), 1u), CjoTypeMetadata(TypeKind.Unit)), listOf(
                block(1u, listOf(2u, 6u)),
                CjoExpressionMetadata(ExprKind.WhileExpr, type = 5u, operands = listOf(3u, 4u), mapExpression = 4u),
                CjoExpressionMetadata(ExprKind.LitConstExpr, type = 2u, literal = CjoLiteralExpressionMetadata("true", LitConstKind.Bool)),
                block(3u, listOf(5u)),
                CjoExpressionMetadata(ExprKind.JumpExpr, type = 3u, mapExpression = 2u,
                    additionalInfo = CjoAdditionalExpressionInfoMetadata.Jump(true)),
                CjoExpressionMetadata(ExprKind.ReturnExpr, type = 3u, operands = listOf(7u)), integer("42"),
            ))
        for (fixture in schemaChain(root, input)) {
            val function = fixture.function("entry")
            val graph = fixture.graph()
            val body = assertIs<CfirSourceExpressionPayload.Block>(graph.expression(graph.functionBodies.getValue(function)).payload)
            val loopId = assertIs<CfirSourceSlot.Expression>(body.statements[0]).identity
            val loopRecord = graph.expression(loopId)
            val loop = assertIs<CfirSourceExpressionPayload.Loop>(loopRecord.payload)
            assertSame(loop.body, loopRecord.mapExpression)
            val loopBody = assertIs<CfirSourceExpressionPayload.Block>(graph.expression(loop.body).payload)
            val jumpId = assertIs<CfirSourceSlot.Expression>(loopBody.statements.single()).identity
            val jump = graph.expression(jumpId)
            assertSame(loopId, assertIs<CfirSourceExpressionPayload.Jump>(jump.payload).loop)
            assertNull(jump.mapExpression)
            assertEquals(7, graph.expressions.size)
            assertNull(function.body)
            assertCanonical(fixture)
        }
    }

    /** schema Position：缺失、存在无效、file0、ignore=true 的端点各自保存。 */
    @Test
    fun expressionPositionSlotsKeepPresenceInvalidCoordinatesAndIgnore(@TempDir root: Path) {
        val cases = listOf(
            null to position(1u, 0u, 2, 8, true),
            position(1u, 0u, 0, -1, true) to position(1u, 0u, 2, 8),
            position(1u, 0u, 2, 7, true) to null,
            position(0u, 0u, 4, 5, true) to position(0u, 0u, -1, 0),
        )
        cases.forEachIndexed { index, (begin, end) ->
            val input = constantMetadata("unifiedposition", "answer", "7").let {
                it.copy(expressions = listOf(it.expressions.single().copy(begin = begin, end = end)))
            }
            for (fixture in schemaChain(root.resolve(index.toString()), input)) {
                val variable = fixture.variable("answer")
                val record = fixture.graph().expression(fixture.graph().initializers.getValue(variable))
                assertSame(variable, record.context)
                val expression = fixture.wire.allExprs(variableInfo(fixture, "answer").initializer.toInt() - 1)!!
                assertPosition(fixture, begin, expression.begin)
                assertPosition(fixture, end, expression.end)
                if (begin == null || begin.line <= 0 || begin.file == 0u) assertNull(record.importedPosition?.begin)
                assertCanonical(fixture)
            }
        }
    }

    /** native Pattern.values 拒绝边界 + schema 独立 P 端点，selector 仍借用原拥有 e。 */
    @Test
    fun patternPositionsAndSelectorSurviveButNativeNonemptyValuesAreRejected(@TempDir root: Path) {
        val positions = listOf(
            position(1u, 0u, 0, -1, true) to position(1u, 0u, 3, 9, true),
            null to position(1u, 0u, 3, 9, true),
            position(1u, 0u, 3, 8, true) to null,
            position(0u, 0u, 3, 8, true) to position(0u, 0u, -1, 0),
        )
        positions.forEachIndexed { positionIndex, (begin, end) ->
            for (fixture in schemaChain(root.resolve(positionIndex.toString()), matchMetadata(begin, end))) {
                val graph = fixture.graph()
                val owner = fixture.function("matchValue")
                val matchRecord = graph.expressions.values.single { it.payload is CfirSourceExpressionPayload.Match }
                val match = assertIs<CfirSourceExpressionPayload.Match>(matchRecord.payload)
                val case = match.cases.map { assertIs<CfirSourceExpressionPayload.MatchCase>(graph.expression(it).payload) }
                    .single { it.patterns.single().kind == CfirSourcePatternKind.CONSTANT }
                val pattern = case.patterns.single()
                assertSame(match.selector, pattern.contextExpression)
                assertTrue(pattern.constantValues.isEmpty())
                assertEquals(CfirSourcePatternKind.CONSTANT, pattern.kind)
                if (begin == null || begin.line <= 0 || begin.file == 0u) assertNull(pattern.importedPosition?.begin)
                else kotlin.test.assertNotNull(pattern.importedPosition?.begin)
                if (end == null || end.line <= 0 || end.file == 0u) assertNull(pattern.importedPosition?.end)
                else kotlin.test.assertNotNull(pattern.importedPosition?.end)
                assertSame(owner, matchRecord.context)
                val encodedCase = fixture.metadata.expressions.single { expression ->
                    expression.kind == ExprKind.MatchCase && assertIs<CjoAdditionalExpressionInfoMetadata.MatchCase>(
                        expression.additionalInfo).patterns.single().kind == PatternKind.ConstPattern
                }
                val saved = assertIs<CjoAdditionalExpressionInfoMetadata.MatchCase>(encodedCase.additionalInfo).patterns.single()
                val wireCase = fixture.wire.allExprs(fixture.metadata.expressions.indexOfFirst { it === encodedCase })!!
                val wirePattern = assertIs<PackageFormat.MatchCaseInfo>(wireCase.info(PackageFormat.MatchCaseInfo())).patterns(0)!!
                assertPosition(fixture, begin, wirePattern.begin)
                assertPosition(fixture, end, wirePattern.end)
                val invalid = fixture.metadata.copy(sourceEncodingContract = CjoSourceEncodingContract.NATIVE_SOURCE, expressions = fixture.metadata.expressions.map { expression ->
                    if (expression !== encodedCase) expression else expression.copy(
                        additionalInfo = CjoAdditionalExpressionInfoMetadata.MatchCase(listOf(saved.copy(values = listOf(CjoInt64ConstValue(7)))))
                    )
                })
                assertFailsWith<IllegalArgumentException> { CjoPackageWriter.toByteArray(invalid) }
                assertSame(graph, fixture.provider.packageSourceGraphOf(owner))
                assertCanonical(fixture)
            }
        }
    }

    /** schema Position 外部 pkg 一基/file 零基；缺诊断目标包不阻止 imports 名称重定位。 */
    @Test
    fun externalPositionsRelocateWithoutLoadingAMissingDiagnosticPackage(@TempDir root: Path) {
        val begin = position(0u, 2u, 5, 6, true)
        val end = position(1u, 2u, 5, 7)
        val input = constantMetadata("unifiedexternalposition", "answer", "7").let {
            it.copy(imports = listOf("unused.position.pkg", "missing.position.pkg"),
                expressions = listOf(it.expressions.single().copy(begin = begin, end = end)))
        }
        for (fixture in schemaChain(root, input)) {
            val graph = fixture.graph()
            val record = graph.expression(graph.initializers.getValue(fixture.variable("answer")))
            assertNull(record.importedPosition?.begin)
            assertNull(record.importedPosition?.end)
            val expression = fixture.wire.allExprs(variableInfo(fixture, "answer").initializer.toInt() - 1)!!
            assertPosition(fixture, begin, expression.begin, input.imports)
            assertPosition(fixture, end, expression.end, input.imports)
            assertNull(fixture.manager.loadPackageSnapshot("missing.position.pkg"))
            assertCanonical(fixture)
        }
    }

    /** typed-schema v0 有效；同 v 的两根共享，内容相等的另一个 v 仍是不同对象，NONE 保持缺失。 */
    @Test
    fun typedVarInfoValuesKeepZeroSharingAndDistinctEqualObjects(@TempDir root: Path) {
        for (fixture in schemaChain(root, valuesMetadata())) {
            val owner = fixture.classifier("Node")
            val value = assertIs<CfirConstantValue.ObjectValue>(fixture.value("first"))
            val number = owner.declarations.filterIsInstance<CfirFieldVariable>().single { it.name.asString() == "number" }
            assertSame(value, fixture.value("second"))
            assertNotSame(value, fixture.value("equal"))
            assertSame(number.symbol, value.fields.keys.single())
            assertSame(number, value.fields.keys.single().cfir)
            assertEquals(BigInteger.valueOf(42), assertIs<CfirConstantValue.Primitive>(value.fields.getValue(number.symbol)).value)
            assertNull(fixture.variable("absent").evaluatedInitializer)
            assertEquals(ConstValue.NONE, variableInfo(fixture, "absent").valueType)
            val graph = fixture.graph()
            assertSame(value, graph.constantValues.getValue(fixture.variable("first")))
            assertSame(value, graph.constantValues.getValue(fixture.variable("second")))
            val reference = assertIs<CjoConstValueReader.Value.CompositeReference>(CjoConstValueReader.readRaw(variableInfo(fixture, "first")))
            assertEquals(0u, reference.index)
            assertEquals(2, fixture.wire.allValuesLength)
            assertCanonical(fixture)
        }
    }

    /** typed-schema：v 的 self/mutual 回边经正式 context 及统一输出池仍指原 shell。 */
    @Test
    fun typedConstantSelfAndMutualCyclesKeepTheirCanonicalShells(@TempDir root: Path) {
        val input = ringMetadata()
        for (fixture in schemaChain(root, input)) {
            val owner = fixture.classifier("Ring")
            val next = owner.declarations.filterIsInstance<CfirFieldVariable>().single().symbol
            val self = assertIs<CfirConstantValue.ObjectValue>(fixture.value("self"))
            val a = assertIs<CfirConstantValue.ObjectValue>(fixture.value("a"))
            val b = assertIs<CfirConstantValue.ObjectValue>(fixture.value("b"))
            assertSame(self, self.fields.getValue(next))
            assertSame(b, a.fields.getValue(next))
            assertSame(a, b.fields.getValue(next))
            assertNotSame(self, a)
            assertSame(next.cfir, owner.declarations.single())
            assertSame(a, fixture.graph().constantValues.getValue(fixture.variable("a")))
            assertEquals(3, fixture.wire.allValuesLength)
            assertCanonical(fixture)
        }
    }

    /** typed-schema 共 tag 不同语义；binary16 的 ±0/subnormal/Infinity/NaN 原 payload 不经 Double 重造。 */
    @Test
    fun int16AndFloat16KeepTheirActualTypesAndOriginalBits(@TempDir root: Path) {
        val bits = listOf(0x0000, 0x8000, 0x0001, 0x03ff, 0x7c00, 0xfc00, 0x7e01, 0x7fff)
        val input = metadata("unifiedhalf", bits.flatMapIndexed { index, value -> listOf(
            constant("i$index", 1u, CjoInt16ConstValue(value.toShort())),
            constant("f$index", 2u, CjoInt16ConstValue(value.toShort())),
        ) }, listOf(CjoTypeMetadata(TypeKind.Int16), CjoTypeMetadata(TypeKind.Float16)))
        for (fixture in schemaChain(root, input)) {
            bits.forEachIndexed { index, expected ->
                for ((prefix, type, kind) in listOf(Triple("i", ConePrimitiveType.INT16, CfirLiteralKind.INT),
                    Triple("f", ConePrimitiveType.FLOAT16, CfirLiteralKind.FLOAT))) {
                    val name = "$prefix$index"
                    val variable = fixture.variable(name)
                    val value = assertIs<CfirConstantValue.Primitive>(fixture.value(name))
                    assertEquals(type, value.type)
                    assertEquals(kind, value.kind)
                    assertSame(value, fixture.graph().constantValues.getValue(variable))
                    val raw = assertIs<CjoConstValueReader.Value.Scalar>(CjoConstValueReader.readRaw(variableInfo(fixture, name)))
                    assertEquals(ConstValue.Int16Value, raw.tag)
                    assertEquals(expected, (raw.value as Short).toInt() and 0xffff)
                    if (prefix == "i") assertEquals(BigInteger.valueOf(expected.toShort().toLong()), value.value)
                }
            }
            assertCanonical(fixture)
        }
    }

    /** typed-schema：语义 Tuple 相同不授权把 Array union 改成 Composite；后者保留原字段名/type/序。 */
    @Test
    fun arrayAndCompositeTupleRepresentationsRemainDistinct(@TempDir root: Path) {
        val input = metadata("unifiedtuples", listOf(
            constant("arrayTuple", 3u, CjoArrayConstValue(listOf(CjoInt16ConstValue(7), CjoInt64ConstValue(9)))),
            constant("compositeTuple", 3u, CjoCompositeConstValue(0u)),
            constant("compositeAgain", 3u, CjoCompositeConstValue(0u)),
        ), listOf(CjoTypeMetadata(TypeKind.Int16), CjoTypeMetadata(TypeKind.Int64), CjoTypeMetadata(TypeKind.Tuple, listOf(1u, 2u))),
            values = listOf(CjoCompositeValueMetadata(3u, listOf(
                CjoCompositeValueFieldMetadata("saved-left", 1u, CjoInt16ConstValue(7)),
                CjoCompositeValueFieldMetadata("saved-right", 2u, CjoInt64ConstValue(9)),
            ))))
        for (fixture in schemaChain(root, input)) {
            val array = assertIs<CfirConstantValue.Tuple>(fixture.value("arrayTuple"))
            val composite = assertIs<CfirConstantValue.Tuple>(fixture.value("compositeTuple"))
            assertEquals(array.elements.map { (it as CfirConstantValue.Primitive).value },
                composite.elements.map { (it as CfirConstantValue.Primitive).value })
            assertNotSame(array, composite)
            assertSame(composite, fixture.value("compositeAgain"))
            assertEquals(ConstValue.ArrayValue, variableInfo(fixture, "arrayTuple").valueType)
            assertEquals(ConstValue.CompositeValue, variableInfo(fixture, "compositeTuple").valueType)
            val saved = fixture.wire.allValues(0)!!
            assertEquals(listOf("saved-left", "saved-right"), (0 until saved.fieldsLength).map { saved.fields(it)!!.field })
            assertEquals(listOf(TypeKind.Int16, TypeKind.Int64), (0 until saved.fieldsLength).map {
                fixture.wire.allTypes(saved.fields(it)!!.type.toInt() - 1)!!.kind
            })
            assertSame(array, fixture.graph().constantValues.getValue(fixture.variable("arrayTuple")))
            assertEquals(1, fixture.wire.allValuesLength)
            assertCanonical(fixture)
        }
    }

    /** typed-schema：MemberValue.type <: declaredType，动态 composite <: storageType；三者可不同。 */
    @Test
    fun compatibleFieldStorageTypesArePreservedInsteadOfUsingChildOrDeclaredType(@TempDir root: Path) {
        val input = storageMetadata()
        for (fixture in schemaChain(root, input)) {
            val owner = fixture.classifier("Holder")
            val field = owner.declarations.filterIsInstance<CfirFieldVariable>().single()
            val holder = assertIs<CfirConstantValue.ObjectValue>(fixture.value("holder"))
            val child = assertIs<CfirConstantValue.ObjectValue>(holder.fields.getValue(field.symbol))
            assertSame(field.symbol, holder.fields.keys.single())
            assertSame(field, holder.fields.keys.single().cfir)
            assertEquals(fixture.classifier("Base").symbol.classId, assertIs<ConeClassLikeType>(field.returnTypeRef.coneType).classId)
            assertEquals(fixture.classifier("Mid").symbol.classId, assertIs<ConeClassLikeType>(holder.storageTypes.getValue(field.symbol)).classId)
            assertEquals(fixture.classifier("Leaf").symbol.classId, assertIs<ConeClassLikeType>(child.type).classId)
            assertSame(child, fixture.value("leafView"))
            assertSame(child, fixture.value("baseView"))
            val holderIndex = assertIs<CjoConstValueReader.Value.CompositeReference>(CjoConstValueReader.readRaw(variableInfo(fixture, "holder"))).index
            val savedField = fixture.wire.allValues(holderIndex.toInt())!!.fields(0)!!
            val type = fixture.wire.allTypes(savedField.type.toInt() - 1)!!
            val valueType = holder.storageTypes.getValue(field.symbol)
            assertEquals(TypeKind.Class, type.kind)
            assertFalse(valueType == field.returnTypeRef.coneType)
            assertFalse(valueType == child.type)
            val metadataField = fixture.metadata.values[holderIndex.toInt()].fields.single()
            val classifier = assertIs<CjoCompositeTypeInfoMetadata>(fixture.metadata.types[metadataField.type.toInt() - 1].semanticInfo)
            assertEquals("Mid", fixture.metadata.declarations[classifier.declarationIndex.toInt() - 1].identifier)
            assertSame(holder, fixture.graph().constantValues.getValue(fixture.variable("holder")))
            assertCanonical(fixture)
        }
    }

    private data class Source(val module: Module, val file: CfirFile, val variable: CfirFieldVariable)

    /** 完成 typed SOURCE fixture；真实 PSI 范围、状态解析及 construction 登记均沿既有装配入口。 */
    private fun sourceConstant(packageName: String, name: String, number: Long, borrowed: CfirFieldVariable? = null): Source {
        val module = Module(CfirSession.Kind.Source, CfirCjmpMode.COMMON)
        val psi = createPsiFile(name, "package $packageName\npublic const $name: Int64 = ${borrowed?.name?.asString() ?: number}") as CjFile
        val declaration = psi.declarations.filterIsInstance<CjPatternVariable>().single()
        val variable = buildFieldVariable {
            source = declaration.toCjPsiSourceElement()
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            isVar = false
            this.name = Name.identifier(name)
            symbol = CfirFieldVariableSymbol(CallableId(FqName(packageName), this.name))
            status = CfirDeclarationStatusImpl().apply { isConst = true }
            returnTypeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef(null)
            initializer = if (borrowed == null) buildLiteralExpression {
                source = kotlin.test.assertNotNull(declaration.initializer).toCjPsiSourceElement()
                coneTypeOrNull = ConePrimitiveType.INT64
                kind = CfirLiteralKind.INT
                value = number
            } else buildNamedAccessExpression {
                source = kotlin.test.assertNotNull(declaration.initializer).toCjPsiSourceElement()
                coneTypeOrNull = ConePrimitiveType.INT64
                calleeReference = buildResolvedNamedReference { this.name = borrowed.name; resolvedSymbol = borrowed.symbol }
            }
        }
        val file = sourceFile(module, packageName, psi, listOf(variable))
        return Source(module, file, variable)
    }

    private fun sourceFile(module: Module, packageName: String, psi: CjFile, members: List<CfirDeclaration> = emptyList()): CfirFile = buildFile {
        source = psi.toCjPsiSourceElement()
        moduleData = module
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Source
        attributes = CfirDeclarationAttributes.EMPTY
        symbol = CfirFileSymbol()
        name = psi.name
        packageDirective = buildPackageDirective { packageFqName = FqName(packageName); isMacroPackage = false }
        resolveCjoSourceFixtureStatuses(module.session, members)
        declarations += members
    }.also(::registerCjoSourceFixture)

    private fun produceSource(source: Source): CjoPackageMetadata = CfirCjoPackageMetadataProducer.produce(
        listOf(source.file), canonicalSources = listOf(CfirSourceGraphProducer.produce(listOf(source.file))),
    )

    private data class Fixture(
        val metadata: CjoPackageMetadata, val path: Path, val module: Module, val manager: CjoManager,
        val provider: CfirDeserializedSymbolProvider, val snapshot: CjoLoadedPackage,
        val roots: List<CfirDeclaration>,
        val searchRoots: List<Path>,
    ) {
        val wire: Package get() = snapshot.pkg
        fun variable(name: String): CfirFieldVariable = assertIs<CfirFieldVariable>(
            provider.getTopLevelCallableSymbols(FqName(metadata.fullPackageName), Name.identifier(name)).single().cfir)
        fun function(name: String): CfirNamedFunction = assertIs<CfirNamedFunction>(
            provider.getTopLevelCallableSymbols(FqName(metadata.fullPackageName), Name.identifier(name)).single().cfir)
        fun classifier(name: String): CfirClass = assertIs<CfirClass>(kotlin.test.assertNotNull(provider.getClassLikeSymbolByClassId(
            ClassId.topLevel(FqName("${metadata.fullPackageName}.$name")))).cfir)
        fun graph(): CfirSourceGraph = kotlin.test.assertNotNull(provider.packageSourceGraphOf(roots.first()))
        fun value(name: String): CfirConstantValue = assertIs<CfirConstantValueExpression>(
            assertIs<CfirEvaluatorResult.Evaluated>(variable(name).evaluatedInitializer).result).constantValue
    }

    /** 只在测试运行时生成临时 cjo；原 context 经正式 provider 创建并拥有全部 binder。 */
    private fun load(root: Path, input: CjoPackageMetadata, common: Boolean = true, searchRoots: List<Path> = emptyList()): Fixture {
        Files.createDirectories(root)
        val path = root.resolve(input.fullPackageName + ".cjo").toAbsolutePath().normalize()
        Files.write(path, CjoPackageWriter.toByteArray(input))
        val manager = CjoManager(CjoSearchPath(additionalLibrarySearchPaths = (listOf(root) + searchRoots).map { it.toFile() }, envProvider = { null }),
            CjoFormatVersionPolicy.Compilation(LanguageVersionSettingsImpl.DEFAULT),
            explicitCommonPartPaths = if (common) listOf(path) else emptyList())
        val module = Module(CfirSession.Kind.Library)
        val provider = CfirDeserializedSymbolProvider(module.session, manager, CfirCangJieScopeProvider(), module)
        module.registerProvider(provider)
        val snapshot = kotlin.test.assertNotNull(manager.loadPackageSnapshot(input.fullPackageName))
        assertSame(snapshot, manager.loadedPackageSnapshot(path))
        val roots = input.declarations.withIndex().filter { it.value.isTopLevel }.map {
            kotlin.test.assertNotNull(provider.deserializeCjoDeclaration(path, input.fullPackageName, it.index))
        }
        val fixture = Fixture(input, path, module, manager, provider, snapshot, roots, searchRoots)
        roots.forEach { declaration ->
            assertSame(declaration, declaration.symbol.cfir)
            if (common) assertDefinition(declaration, fixture) else assertTrue(declaration.exportFacts.isImported)
        }
        return fixture
    }

    /** 不从 raw 内容重造图；编码输入为原 provider 已发布产品，family 根也是它实际绑定的对象。 */
    private fun reencode(root: Path, input: Fixture, graphs: List<CfirSourceGraph> = listOf(input.graph())): Fixture {
        val module = Module(CfirSession.Kind.Source, CfirCjmpMode.COMMON)
        val psi = createPsiFile("reexport", "package ${input.metadata.fullPackageName}\n") as CjFile
        val file = sourceFile(module, input.metadata.fullPackageName, psi)
        registerCjoSourceFixtureDependencies(module.session, input.provider)
        val family = family(input.metadata.fullPackageName, input.roots, module.session)
        val metadata = CfirCjoPackageMetadataProducer.produce(listOf(file), familySnapshot = family,
            loadedPackages = listOf(input.snapshot), exportOptions = CjoExportOptions(serializingCommon = true,
                sourceEncodingContract = input.metadata.sourceEncodingContract),
            canonicalSources = graphs)
        assertCanonical(input)
        return load(root, metadata, searchRoots = listOf(input.path.parent) + input.searchRoots).also(::assertCanonical)
    }

    /** schema 种子不能由固定 native SOURCE 产生；后续两轮均为实际统一 producer 消费。 */
    private fun schemaChain(root: Path, input: CjoPackageMetadata, core: ByteArray? = null): List<Fixture> {
        if (core != null) {
            Files.createDirectories(root.resolve("seed"))
            Files.write(root.resolve("seed/std.core.cjo"), core)
        }
        val first = load(root.resolve("seed"), input.copy(sourceEncodingContract = CjoSourceEncodingContract.TYPED_SCHEMA))
        assertCanonical(first)
        val second = reencode(root.resolve("producer-one"), first)
        val third = reencode(root.resolve("producer-two"), second)
        return listOf(first, second, third)
    }

    /** 仅装配已加载的真实 family 身份与成员事实；不执行匹配猜测，不改声明 status/IMPORTED。 */
    private fun family(packageName: String, roots: List<CfirDeclaration>, actualSession: CfirSession): CfirCjmpFamilySnapshot {
        val nominals = roots.filterIsInstance<CfirClassLikeDeclaration>()
        val containers: List<CjmpMergeContainer<CfirBasedSymbol<*>, CfirTypeParameterSymbol>> = nominals.map { owner ->
            CjmpMergeContainer(
                owner.symbol, owner.name.asString(), when (owner) {
                    is CfirClass -> CjmpContainerKind.CLASS
                    is CfirStruct -> CjmpContainerKind.STRUCT
                    is CfirInterface -> CjmpContainerKind.INTERFACE
                    is CfirEnum -> CjmpContainerKind.ENUM
                    else -> error("This fixture requires an actual nominal definition")
                }, owner.status.isCommon, owner.status.isSpecific, owner.hasGenericNode,
                owner.typeParameters.filterIsInstance<CfirTypeParameter>().map { it.symbol }, false, false,
                owner.declarations.mapIndexed { index, member ->
                    val status = assertIs<CfirMemberDeclaration>(member).status
                    CjmpMergeMember(member.symbol, when (member) {
                        is CfirVariable -> member.symbol.name.asString()
                        is CfirNamedFunction -> member.name.asString()
                        is CfirConstructor -> member.symbol.name.asString()
                        else -> error("This fixture explicitly supplies only fields and named functions")
                    }, member is CfirFieldVariable && !status.isStatic, false, status.isCommon, status.isSpecific,
                        status.isCommonWithDefault, false, CjmpInitializationDependencies.NotCollected,
                        member.isFromCommonPart, index)
                },
            )
        }
        return CfirCjmpPreparedGroup(FqName(packageName), roots = emptyList(),
            topLevelOrder = roots.map { it.symbol },
            originalChildren = nominals.associate { it.symbol to it.declarations.map { child -> child.symbol } },
            commonDeclarations = roots.filter { it.isFromCommonPart }.map { it.symbol },
            containers = containers, extensions = emptyList(), commonExtendOrder = emptyList(), originalDoNotExport = emptySet(),
            classifierDomain = CfirCjmpClassifierDomain.capture(actualSession, roots, sourceFiles = emptyList())).evaluate()
    }

    /** 每轮断言 provider/context 的实际 d/F/e/value 身份；raw 字段比对仅为附加存储验证。 */
    private fun assertCanonical(fixture: Fixture) {
        val graph = fixture.graph()
        assertSame(fixture.snapshot, fixture.manager.loadedPackageSnapshot(fixture.path))
        for ((index, expected) in fixture.metadata.declarations.withIndex()) {
            if (!expected.isTopLevel) continue
            val declaration = kotlin.test.assertNotNull(fixture.provider.deserializeCjoDeclaration(fixture.path, fixture.metadata.fullPackageName, index))
            assertSame(declaration, fixture.roots.single { it === declaration })
            assertSame(declaration, declaration.symbol.cfir)
            assertSame(graph, fixture.provider.packageSourceGraphOf(declaration))
        }
        graph.declarationOwners.forEach { (declaration, _) ->
            val origin = kotlin.test.assertNotNull(declaration.cjoDeclarationOrigin)
            assertSame(declaration, fixture.provider.deserializeCjoDeclaration(fixture.path, fixture.metadata.fullPackageName, origin.declarationIndex))
            assertSame(graph, fixture.provider.packageSourceGraphOf(declaration))
        }
        graph.functionHeaders.forEach { (owner, header) ->
            if (owner is CfirFunction) assertEquals(header, fixture.provider.sourceHeaderOf(owner))
            header.parameterLists.flatten().forEach { parameter ->
                assertSame(owner.symbol, parameter.containingDeclarationSymbol)
                assertSame(parameter, parameter.symbol.cfir)
            }
        }
        graph.initializers.forEach { (owner, identity) ->
            assertSame(owner, graph.expression(identity).context)
            assertSame(graph.expression(identity), graph.expression(identity))
        }
        graph.constantValues.forEach { (owner, value) ->
            assertSame(value, assertIs<CfirConstantValueExpression>(assertIs<CfirEvaluatorResult.Evaluated>(owner.evaluatedInitializer).result).constantValue)
        }
    }

    private fun assertDefinition(declaration: CfirDeclaration, fixture: Fixture) {
        assertTrue(declaration.isFromCommonPart)
        assertFalse(declaration.exportFacts.isImported)
        assertEquals(fixture.path.toString(), kotlin.test.assertNotNull(declaration.cjoDeclarationOrigin).packagePath)
        assertSame(fixture.module, declaration.moduleData)
    }

    private fun literal(fixture: Fixture, name: String): String = assertIs<CfirSourceExpressionPayload.Literal>(
        fixture.graph().expression(fixture.graph().initializers.getValue(fixture.variable(name))).payload).text

    private fun variableInfo(fixture: Fixture, name: String): VarInfo = assertIs<VarInfo>(
        fixture.wire.allDecls(fixture.metadata.declarations.indexOfFirst { it.identifier == name })!!.info(VarInfo()))

    private fun functionBody(fixture: Fixture, name: String) = kotlin.test.assertNotNull(assertIs<FuncInfo>(
        fixture.wire.allDecls(fixture.metadata.declarations.indexOfFirst { it.identifier == name })!!.info(FuncInfo())).funcBody)

    private fun position(file: UInt, pkg: UInt, line: Int, column: Int, ignore: Boolean = false) =
        CjoPositionMetadata(file, pkg, line, column, ignore)

    private fun assertPosition(fixture: Fixture, expected: CjoPositionMetadata?, actual: Position?, imports: List<String> = emptyList()) {
        if (expected == null) { assertNull(actual); return }
        val saved = kotlin.test.assertNotNull(actual)
        assertEquals(expected.line, saved.line)
        assertEquals(expected.column, saved.column)
        assertEquals(expected.ignore, saved.ignore)
        if (expected.pkgId == 0u) {
            assertEquals(0u, saved.pkgId)
            if (expected.file == 0u) assertEquals(0u, saved.file)
            else assertEquals("unified-schema.cj", fixture.wire.allFiles(saved.file.toInt() - 1))
        } else {
            assertEquals(expected.file, saved.file)
            assertEquals(imports[expected.pkgId.toInt() - 1], fixture.wire.imports(saved.pkgId.toInt() - 1))
        }
    }

    private fun metadata(name: String, declarations: List<CjoPackageDeclaration>, types: List<CjoTypeMetadata>,
                         expressions: List<CjoExpressionMetadata> = emptyList(), values: List<CjoCompositeValueMetadata> = emptyList()): CjoPackageMetadata =
        CjoPackageMetadata(name, name, schemaProfile = CjoSchemaProfile.OFFICIAL_CJMP,
            allFiles = listOf("unified-schema.cj"), declarations = declarations.map {
                it.copy(attributes = addFlag(it.attributes, Attribute.FROM_COMMON_PART))
            }, types = types, expressions = expressions, values = values)

    private fun constantMetadata(pkg: String, name: String, text: String) = metadata(pkg,
        listOf(constant(name, 1u, null).copy(info = CjoVariableInfo(isConst = true, initializer = 1u))),
        listOf(CjoTypeMetadata(TypeKind.Int64)), listOf(integer(text)))

    /** 在正式 producer 的 SOURCE 产物后显式添加 schema 值池；不声称此扩展来自 native writer。 */
    private fun withTypedValue(source: CjoPackageMetadata, ownerName: String, valueName: String): CjoPackageMetadata {
        assertEquals(1, source.declarations.size)
        assertEquals(1, source.types.size)
        assertEquals(TypeKind.Int64, source.types.single().kind)
        assertTrue(source.values.isEmpty())
        return source.copy(declarations = source.declarations + listOf(
            CjoPackageDeclaration(ownerName, DeclKind.ClassDecl, type = 2u,
                attributes = flags(Attribute.PUBLIC, Attribute.FROM_COMMON_PART), info = CjoClassInfo(body = listOf(3u))),
            member("number", 1u).copy(attributes = flags(Attribute.PUBLIC, Attribute.FROM_COMMON_PART)),
            constant(valueName, 2u, CjoCompositeConstValue(0u)),
        ), types = source.types + nominal(2u), values = listOf(CjoCompositeValueMetadata(2u,
            listOf(CjoCompositeValueFieldMetadata("number", 1u, CjoInt64ConstValue(42))))))
    }

    private fun headersMetadata() = metadata("unifiedheaders", listOf(
        CjoPackageDeclaration("header", type = 2u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
            info = CjoFunctionInfo(body = CjoFunctionBodyInfo(listOf(emptyList(), listOf(2u)),
                listOf(emptyList(), listOf(0u)), returnType = 0u))), parameter("value"),
    ), listOf(CjoTypeMetadata(TypeKind.Int64), functionType(listOf(1u), 1u)))

    private fun parameter(name: String) = CjoPackageDeclaration(name, DeclKind.FuncParam, isTopLevel = false, type = 1u,
        attributes = flags(Attribute.FROM_COMMON_PART), info = CjoParameterInfo(isNamed = false, isMember = false))

    private fun function(name: String, type: UInt, body: UInt) = CjoPackageDeclaration(name, type = type,
        attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
        info = CjoFunctionInfo(isConst = true, body = CjoFunctionBodyInfo(returnType = 1u, body = body)))

    private fun functionType(parameters: List<UInt>, result: UInt) = CjoTypeMetadata(TypeKind.Func, parameters,
        semanticInfo = CjoFunctionTypeInfoMetadata(result, false, false))

    private fun integer(text: String) = CjoExpressionMetadata(ExprKind.LitConstExpr, type = 1u,
        literal = CjoLiteralExpressionMetadata(text, LitConstKind.Integer))

    private fun block(type: UInt, operands: List<UInt>) = CjoExpressionMetadata(ExprKind.Block, type = type, operands = operands,
        additionalInfo = CjoAdditionalExpressionInfoMetadata.Block(List(operands.size) { true } + false))

    private fun matchMetadata(begin: CjoPositionMetadata?, end: CjoPositionMetadata?) = metadata("unifiedpattern",
        listOf(function("matchValue", 3u, 1u)), listOf(CjoTypeMetadata(TypeKind.Int64), CjoTypeMetadata(TypeKind.Nothing),
            functionType(emptyList(), 1u)), listOf(
            block(1u, listOf(2u)), CjoExpressionMetadata(ExprKind.ReturnExpr, type = 2u, operands = listOf(3u)),
            CjoExpressionMetadata(ExprKind.MatchExpr, type = 1u, operands = listOf(4u, 5u, 9u),
                additionalInfo = CjoAdditionalExpressionInfoMetadata.Match(true)), integer("7"),
            CjoExpressionMetadata(ExprKind.MatchCase, type = 1u, operands = listOf(0u, 7u),
                additionalInfo = CjoAdditionalExpressionInfoMetadata.MatchCase(listOf(CjoPatternMetadata(
                    PatternKind.ConstPattern, types = listOf(1u), exprs = listOf(6u), begin = begin, end = end)))),
            integer("7"), block(1u, listOf(8u)), integer("9"),
            CjoExpressionMetadata(ExprKind.MatchCase, type = 1u, operands = listOf(0u, 10u),
                additionalInfo = CjoAdditionalExpressionInfoMetadata.MatchCase(listOf(CjoPatternMetadata(
                    PatternKind.WildcardPattern, types = listOf(1u))))),
            block(1u, listOf(11u)), integer("10"),
        ))

    private fun constant(name: String, type: UInt, value: CjoConstValueInfo?) = CjoPackageDeclaration(name, DeclKind.VarDecl,
        type = type, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL, Attribute.FROM_COMMON_PART),
        info = CjoVariableInfo(isConst = true, constValue = value))

    private fun member(name: String, type: UInt) = CjoPackageDeclaration(name, DeclKind.VarDecl, isTopLevel = false,
        type = type, attributes = flags(Attribute.PUBLIC), info = CjoVariableInfo())

    private fun nominal(index: UInt) = CjoTypeMetadata(TypeKind.Class, semanticInfo = CjoCompositeTypeInfoMetadata(index))

    private fun valuesMetadata() = metadata("unifiedvalues", listOf(
        CjoPackageDeclaration("Node", DeclKind.ClassDecl, type = 1u, attributes = flags(Attribute.PUBLIC), info = CjoClassInfo(body = listOf(2u))),
        member("number", 2u), constant("first", 1u, CjoCompositeConstValue(0u)), constant("second", 1u, CjoCompositeConstValue(0u)),
        constant("equal", 1u, CjoCompositeConstValue(1u)), constant("absent", 1u, null),
    ), listOf(nominal(1u), CjoTypeMetadata(TypeKind.Int64)), values = List(2) {
        CjoCompositeValueMetadata(1u, listOf(CjoCompositeValueFieldMetadata("number", 2u, CjoInt64ConstValue(42))))
    })

    private fun ringMetadata() = metadata("unifiedcycles", listOf(
        CjoPackageDeclaration("Ring", DeclKind.ClassDecl, type = 1u, attributes = flags(Attribute.PUBLIC), info = CjoClassInfo(body = listOf(2u))),
        member("next", 1u), constant("self", 1u, CjoCompositeConstValue(0u)),
        constant("a", 1u, CjoCompositeConstValue(1u)), constant("b", 1u, CjoCompositeConstValue(2u)),
    ), listOf(nominal(1u)), values = listOf(0u, 2u, 1u).map { target ->
        CjoCompositeValueMetadata(1u, listOf(CjoCompositeValueFieldMetadata("next", 1u, CjoCompositeConstValue(target))))
    })

    private fun storageMetadata() = metadata("unifiedstorage", listOf(
        CjoPackageDeclaration("Base", DeclKind.ClassDecl, type = 1u, attributes = flags(Attribute.PUBLIC, Attribute.OPEN), info = CjoClassInfo()),
        CjoPackageDeclaration("Mid", DeclKind.ClassDecl, type = 2u, attributes = flags(Attribute.PUBLIC, Attribute.OPEN), info = CjoClassInfo(inheritedTypes = listOf(1u))),
        CjoPackageDeclaration("Leaf", DeclKind.ClassDecl, type = 3u, attributes = flags(Attribute.PUBLIC), info = CjoClassInfo(inheritedTypes = listOf(2u))),
        CjoPackageDeclaration("Holder", DeclKind.ClassDecl, type = 4u, attributes = flags(Attribute.PUBLIC), info = CjoClassInfo(body = listOf(5u))),
        member("payload", 1u), constant("holder", 4u, CjoCompositeConstValue(0u)),
        constant("leafView", 3u, CjoCompositeConstValue(1u)), constant("baseView", 1u, CjoCompositeConstValue(1u)),
    ), listOf(nominal(1u), nominal(2u), nominal(3u), nominal(4u)), values = listOf(
        CjoCompositeValueMetadata(4u, listOf(CjoCompositeValueFieldMetadata("payload", 2u, CjoCompositeConstValue(1u)))),
        CjoCompositeValueMetadata(3u),
    ))

    /** Annotation 标记的实际 std.core 类型绑定来自已存在的官方测试资源。 */
    private fun officialCore(): ByteArray = kotlin.test.assertNotNull(javaClass.classLoader.getResourceAsStream(
        "cjo-sdk/windows_x86_64_cjnative/std/std.core.cjo"), "官方 std.core CJO fixture 必须存在").use { it.readBytes() }

    private fun flags(vararg attributes: Attribute): List<ULong> = attributes.fold(emptyList()) { words, flag -> addFlag(words, flag) }

    private fun addFlag(words: List<ULong>, flag: Attribute): List<ULong> = MutableList(maxOf(words.size, flag.ordinal / 64 + 1)) {
        words.getOrElse(it) { 0uL }
    }.apply { this[flag.ordinal / 64] = this[flag.ordinal / 64] or (1uL shl (flag.ordinal % 64)) }

    /** 正式 Library/Source 组件；source provider 由 construction 登记，库 provider 由 manager 注入。 */
    private class Module(kind: CfirSession.Kind, mode: CfirCjmpMode? = null) : CfirModuleData() {
        override val name = Name.identifier("unified-source-encoder-test")
        override val dependencies: List<CfirModuleData> = emptyList()
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform = CfirPlatform.DEFAULT
        override val isCommon = mode == CfirCjmpMode.COMMON
        override val stableModuleName = "unified-source-encoder-test"
        override val session = object : CfirSession(kind) {}.also {
            it.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
            it.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = mode))
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

        fun registerProvider(provider: CfirSymbolProvider) {
            if (session.kind == CfirSession.Kind.Library) {
                session.register(CfirProvider::class, CfirLibrarySessionProvider(provider))
            }
            session.register(CfirSymbolProvider::class, provider)
            session.register(CfirExtendProvider::class, CfirExtendProviderComposer.fromSymbolProviders(listOf(provider)))
        }
    }
}
