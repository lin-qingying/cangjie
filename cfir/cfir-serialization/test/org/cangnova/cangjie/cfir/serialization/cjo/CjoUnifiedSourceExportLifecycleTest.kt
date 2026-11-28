@file:OptIn(ExperimentalUnsignedTypes::class, org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.test.cjoFixtureModuleDataProvider
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import PackageFormat.AnnoKind
import PackageFormat.ConstValue
import PackageFormat.DeclKind
import PackageFormat.ExprInfo
import PackageFormat.ExprKind
import PackageFormat.FuncInfo
import PackageFormat.LitConstKind
import PackageFormat.MatchCaseInfo
import PackageFormat.Package
import PackageFormat.PatternKind
import PackageFormat.TypeKind
import PackageFormat.VarInfo
import com.google.flatbuffers.Table
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.annotations.CangjieAnnotationIdentity
import org.cangnova.cangjie.annotations.CangjieAnnotationOrigin
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.CfirEvaluatorResult
import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.builder.hasCjmpCommonDefault
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.utils.evaluatedInitializer
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolver
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolverImpl
import org.cangnova.cangjie.cfir.resolve.providers.*
import org.cangnova.cangjie.cfir.resolve.providers.macro.*
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendIndexStore
import org.cangnova.cangjie.cfir.resolve.source.CfirSourceGraphProducer
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.scopes.CfirDefaultImportsProviderHolder
import org.cangnova.cangjie.cfir.resolve.CfirDefaultImportsProvider
import org.cangnova.cangjie.cfir.resolve.services.CfirImportBindingStore
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoConstValueReader
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.serialization.provider.CfirExtendProviderComposer
import org.cangnova.cangjie.cfir.session.*
import org.cangnova.cangjie.cfir.source.*
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.psi.CjBlockExpression
import org.cangnova.cangjie.psi.CjDoWhileExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjInterface
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjReturnExpression
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
import kotlin.test.*

/**
 * provider/context → 完成 canonical 产品 → 统一 producer → fresh CJO 的生命周期合同。
 *
 * Native 固定为 14276740ffec1262b81b6b5236b1d840af270b37：ASTWriter.cpp 的参数/default/
 * annotation、PRE/FINAL 保存门及 ExprWriter.cpp 的 mixed Block、DoWhile 保存次序。
 * Kotlin 固定为 3c7a5041f238102285764d370665f56f587f9dc4：IrFileSerializer.kt 的
 * serializeStatement、serializeIrVariable、serializeIrFunctionBase 共用 container/helper。
 * 语义边界见 unified-repair-native-authority.md 与 unified-repair-kotlin-map.md。
 *
 * 非空 v、共享 String/Array union payload 与 Pattern.values 是合法 ModuleFormat schema
 * 种子，明确不是该固定 Native 的源码产物。测试不手建 canonical 图，也不造 imported body。
 */
class CjoUnifiedSourceExportLifecycleTest : CjParsingTestCase("", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition()) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    /** exportContent=false 关闭 F.body；真实 default/helper/各层 annotation 仍进入统一拥有闭包。 */
    @Test
    fun headerOnlyExportsDefaultsHelpersAndAnnotationsWithoutDecodingAnUnrelatedBadBlock(@TempDir root: Path) {
        val core = officialCore()
        val seed = headerSeed(core)
        val bytes = CjoPackageWriter.toByteArray(seed)
        val wire = Package.getRootAsPackage(ByteBuffer.wrap(bytes))
        // 保留实际 Block、合法 statement 引用和类型，只损坏其 union tag；owning index 可读，decoder 必须拒绝。
        val buffer = ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        val packageSlots = UnionSlots().at(buffer.getInt(buffer.position()) + buffer.position(), buffer)
        val expressionSlots = UnionSlots().at(packageSlots.expressionPosition(4), buffer)
        buffer.put(expressionSlots.expressionInfoTypeSlot(), ExprInfo.ReferenceInfo.toByte())
        assertEquals(ExprInfo.ReferenceInfo, kotlin.test.assertNotNull(wire.allExprs(4)).infoType)
        val first = load(root.resolve("seed"), seed, bytes, core = core)
        val owner = first.function("header")
        val before = kotlin.test.assertNotNull(first.provider.sourceHeaderOf(owner))
        val bad = first.function("unrelated")
        assertTrue(kotlin.test.assertNotNull(first.provider.sourceHeaderOf(bad)).bodyAvailable)
        val selected = kotlin.test.assertNotNull(first.provider.sourceExportSnapshotOf(owner, includeContent = false))
        val graph = kotlin.test.assertNotNull(selected.graph, "header-only 完成产品必须包含所选 default/annotation 的 canonical 子图")
        assertSame(first.snapshot, selected.source)
        assertSame(first.module.session, selected.domain)
        assertEquals(before, graph.functionHeaders.getValue(owner))
        assertTrue(graph.functionBodies.isEmpty())
        assertFalse(graph.expressions.values.any { it.payload is CfirSourceExpressionPayload.Block })
        assertEquals(4, graph.expressions.size)
        assertHeaderDependencies(first, graph)
        val second = reexport(root.resolve("fresh"), first, graph, exportContent = false,
            contract = CjoSourceEncodingContract.NATIVE_SOURCE)
        assertEquals(0u, second.body("header").body)
        assertEquals(0u, second.body("unrelated").body)
        assertHeaderDependencies(second, second.graph())
        assertTrue(second.graph().functionBodies.isEmpty())
        assertNotSame(owner, second.function("header"))
        assertNotSame(graph.defaults.getValue(owner.valueParameters.single()),
            second.graph().defaults.getValue(second.function("header").valueParameters.single()))
        assertTrue(second.metadata.values.isEmpty())
        // 完整读取确实会触发坏 Block；失败不能撤销此前完成的 header/default 身份。
        assertFailsWith<IllegalStateException> { first.provider.packageSourceGraphOf(bad) }
        assertSame(owner, first.function("header"))
        assertEquals(before, first.provider.sourceHeaderOf(owner))
        assertHeaderDependencies(first, graph)
    }

    private fun assertHeaderDependencies(fixture: Fixture, graph: CfirSourceGraph) {
        val owner = fixture.function("header")
        val header = graph.functionHeaders.getValue(owner)
        val parameter = header.parameterLists.single().single()
        val helper = kotlin.test.assertNotNull(header.desugaredParameterLists.single().single())
        assertSame(owner.valueParameters.single(), parameter)
        assertSame(owner.symbol, parameter.containingDeclarationSymbol)
        assertSame(helper, kotlin.test.assertNotNull(parameter.defaultValueMetadata).function)
        assertSame(owner, graph.declarationOwners.getValue(parameter))
        assertSame(parameter, graph.declarationOwners.getValue(helper))
        assertEquals(ConePrimitiveType.INT64, parameter.returnTypeRef.coneType)
        assertEquals(ConePrimitiveType.INT64, header.returnType)
        assertFalse(header.bodyAvailable)
        assertFalse(graph.functionHeaders.getValue(helper).bodyAvailable)
        assertNull(owner.body)
        assertNull(helper.body)
        val default = graph.expression(graph.defaults.getValue(parameter))
        assertSame(parameter, default.context)
        assertEquals(ConePrimitiveType.INT64, default.type)
        assertEquals("7", assertIs<CfirSourceExpressionPayload.Literal>(default.payload).text)
        for ((declaration, text) in listOf(owner to "11", parameter to "12", helper to "13")) {
            assertCurrent(declaration, fixture)
            val annotation = graph.annotations.getValue(declaration).single()
            assertEquals("Deprecated", annotation.identifier)
            val argument = graph.expression(annotation.arguments.single().expression)
            assertSame(declaration, argument.context)
            assertEquals(StdlibClassIds.String, assertIs<ConeStructType>(argument.type).classId)
            assertEquals(text, assertIs<CfirSourceExpressionPayload.Literal>(argument.payload).text)
            assertSame(argument, graph.expression(annotation.arguments.single().expression))
        }
        val helperIndex = kotlin.test.assertNotNull(helper.cjoDeclarationOrigin).declarationIndex
        assertSame(helper, fixture.declaration(helperIndex))
        val helperWire = kotlin.test.assertNotNull(fixture.wire.allDecls(helperIndex))
        val globalBit = Attribute.GLOBAL.ordinal
        assertTrue(helperWire.attributes(globalBit / 64) and (1uL shl (globalBit % 64)) != 0uL)
        val body = fixture.body("header")
        assertTrue(kotlin.test.assertNotNull(body.paramLists(0)).desugars(0) != 0u)
        val info = assertIs<PackageFormat.ParamInfo>(fixture.wire.allDecls(
            kotlin.test.assertNotNull(parameter.cjoDeclarationOrigin).declarationIndex)!!.info(PackageFormat.ParamInfo()))
        assertTrue(info.defaultVal != 0u)
    }

    /** 两个真实 VarInfo 宿主复用同一 payload；inline 根和 inline 子值按宿主 occurrence 区分。 */
    @Test
    fun sharedStringAndArrayPayloadsKeepSeparateInlineRootsWhileCompositeZeroStaysShared(@TempDir root: Path) {
        val core = officialCore()
        val seed = metadata("lifecyclevalues", listOf(
            constant("s0", 2u, CjoStringConstValue("same")), constant("s1", 2u, CjoStringConstValue("same")),
            constant("a0", 3u, CjoArrayConstValue(listOf(CjoInt64ConstValue(7), CjoInt64ConstValue(9)))),
            constant("a1", 3u, CjoArrayConstValue(listOf(CjoInt64ConstValue(7), CjoInt64ConstValue(9)))),
            CjoPackageDeclaration("Node", DeclKind.ClassDecl, type = 4u, attributes = flags(Attribute.PUBLIC),
                info = CjoClassInfo(body = listOf(6u))),
            constant("number", 1u, null).copy(isTopLevel = false, info = CjoVariableInfo()),
            constant("v0a", 4u, CjoCompositeConstValue(0u)), constant("v0b", 4u, CjoCompositeConstValue(0u)),
        ), listOf(CjoTypeMetadata(TypeKind.Int64), stringType(core),
            CjoTypeMetadata(TypeKind.VArray, listOf(1u), semanticInfo = CjoArrayTypeInfoMetadata(2L)),
            CjoTypeMetadata(TypeKind.Class, semanticInfo = CjoCompositeTypeInfoMetadata(5u))), values = listOf(
            CjoCompositeValueMetadata(4u, listOf(CjoCompositeValueFieldMetadata("number", 1u, CjoInt64ConstValue(42))))
        )).copy(imports = listOf("std.core"))
        val bytes = CjoPackageWriter.toByteArray(seed)
        val wire = Package.getRootAsPackage(ByteBuffer.wrap(bytes))
        for ((left, right) in listOf(0 to 1, 2 to 3)) {
            val a = assertIs<UnionSlots>(wire.allDecls(left)!!.info(UnionSlots()))
            val b = assertIs<UnionSlots>(wire.allDecls(right)!!.info(UnionSlots()))
            shareUnionPayload(bytes, a.variableValueSlot(), b.variableValueSlot())
        }
        val first = load(root.resolve("seed"), seed, bytes, core = core)
        val firstRoots = listOf("s0", "s1", "a0", "a1", "v0a", "v0b").associateWith(first::value)
        val firstGraph = first.graph()
        firstRoots.forEach { (name, value) -> assertSame(value, firstGraph.constantValues.getValue(first.variable(name))) }
        val second = reexport(root.resolve("fresh"), first, firstGraph)
        for (fixture in listOf(first, second)) {
            val graph = fixture.graph()
            for ((left, right) in listOf("s0" to "s1", "a0" to "a1")) {
                val a = fixture.value(left)
                val b = fixture.value(right)
                assertNotSame(a, b)
                assertEquals(a, b)
                assertSame(a, fixture.value(left))
                assertSame(b, fixture.value(right))
                for ((name, value) in listOf(left to a, right to b)) {
                    val host = fixture.variable(name)
                    assertCurrent(host, fixture)
                    assertSame(value, graph.constantValues.getValue(host))
                    assertSame(host.returnTypeRef.coneType, value.type)
                    assertSame(host, fixture.declaration(kotlin.test.assertNotNull(host.cjoDeclarationOrigin).declarationIndex))
                }
                if (left == "a0") {
                    val aa = assertIs<CfirConstantValue.VArray>(a)
                    val bb = assertIs<CfirConstantValue.VArray>(b)
                    assertEquals(2L, aa.type.size)
                    aa.elements.zip(bb.elements).forEach { (x, y) ->
                        assertNotSame(x, y)
                        assertSame(ConePrimitiveType.INT64, x.type)
                        assertSame(ConePrimitiveType.INT64, y.type)
                    }
                }
            }
            val shared = assertIs<CfirConstantValue.ObjectValue>(fixture.value("v0a"))
            assertSame(shared, fixture.value("v0b"))
            assertSame(shared, graph.constantValues.getValue(fixture.variable("v0a")))
            assertSame(fixture.classifier("Node").declarations.single().symbol, shared.fields.keys.single())
            assertSame(ConePrimitiveType.INT64, shared.storageTypes.values.single())
            assertEquals(1, fixture.wire.allValuesLength)
            assertEquals(0u, assertIs<CjoConstValueReader.Value.CompositeReference>(
                CjoConstValueReader.readRaw(fixture.variableInfo("v0a"))).index)
            val snapshot = kotlin.test.assertNotNull(fixture.provider.sourceExportSnapshotOf(fixture.variable("a0"), true))
            assertSame(fixture.module.session, snapshot.domain)
            assertIs<CjoSavedConstantStorage.Array>(snapshot.constants.getValue(fixture.value("a0")))
            assertIs<CjoSavedConstantStorage.Scalar>(snapshot.constants.getValue(fixture.value("s0")))
            assertCanonical(fixture, graph)
        }
        assertNotSame(first.value("v0a"), second.value("v0a"))
        assertNotSame(first.value("a0"), second.value("a0"))
    }

    /** 同一 P 的两个 values ordinal 指向同一 struct payload，仍发布两个独立 typed inline 根。 */
    @Test
    fun patternValueOrdinalsKeepIndependentOccurrencesAndTheirSelectorUse(@TempDir root: Path) {
        val seed = patternSeed()
        val bytes = CjoPackageWriter.toByteArray(seed)
        val wire = Package.getRootAsPackage(ByteBuffer.wrap(bytes))
        val slots = assertIs<UnionSlots>(wire.allExprs(2)!!.info(UnionSlots()))
        shareUnionPayload(bytes, slots.patternValueSlot(0, 0), slots.patternValueSlot(0, 1))
        val first = load(root.resolve("seed"), seed, bytes)
        val second = reexport(root.resolve("fresh"), first, first.graph())
        for (fixture in listOf(first, second)) {
            val graph = fixture.graph()
            val owner = fixture.function("patterns")
            val matchRecord = graph.expressions.values.single { it.payload is CfirSourceExpressionPayload.Match }
            val match = assertIs<CfirSourceExpressionPayload.Match>(matchRecord.payload)
            val case = assertIs<CfirSourceExpressionPayload.MatchCase>(graph.expression(match.cases.first()).payload)
            val pattern = case.patterns.single()
            assertSame(owner, matchRecord.context)
            assertSame(match.selector, pattern.contextExpression)
            assertSame(ConePrimitiveType.INT64, pattern.types.single())
            val literalIdentity = assertIs<CfirSourceSlot.Expression>(pattern.slots.single()).identity
            assertNotSame(match.selector, literalIdentity)
            assertSame(owner, graph.expression(literalIdentity).context)
            assertSame(ConePrimitiveType.INT64, graph.expression(literalIdentity).type)
            val values = pattern.constantValues
            assertEquals(2, values.size)
            assertNotSame(values[0], values[1])
            assertEquals(values[0], values[1])
            values.forEach { value ->
                assertSame(ConePrimitiveType.INT64, value.type)
                assertEquals(BigInteger.valueOf(7), assertIs<CfirConstantValue.Primitive>(value).value)
            }
            val repeated = assertIs<CfirSourceExpressionPayload.MatchCase>(
                fixture.graph().expression(match.cases.first()).payload).patterns.single()
            assertSame(pattern, repeated)
            assertSame(values[0], repeated.constantValues[0])
            assertSame(values[1], repeated.constantValues[1])
            assertCurrent(owner, fixture)
            assertNull(owner.body)
            val outputCase = fixture.metadata.expressions.single { it.kind == ExprKind.MatchCase }
            val encoded = assertIs<CjoAdditionalExpressionInfoMetadata.MatchCase>(outputCase.additionalInfo).patterns.single()
            assertEquals(listOf(CjoInt64ConstValue(7), CjoInt64ConstValue(7)), encoded.values)
            assertTrue(fixture.metadata.values.isEmpty())
            assertCanonical(fixture, graph)
        }
    }

    /** 实际 FuncInfo/F 的 enum constructor 走 callable API；实际 VarInfo case 不获得替身 F。 */
    @Test
    fun enumCallableHeaderSurvivesReencodingWithoutInventingAHeaderForTheVarInfoCase(@TempDir root: Path) {
        val first = load(root.resolve("seed"), enumSeed())
        val second = reexport(root.resolve("fresh"), first, first.graph())
        for (fixture in listOf(first, second)) {
            val enum = assertIs<CfirEnum>(fixture.classifier("Choice"))
            val graph = fixture.graph()
            val some = assertIs<CfirEnumConstructor>(enum.declarations.single {
                it is CfirEnumConstructor && it.name.asString() == "Some"
            })
            val none = assertIs<CfirEnumConstructor>(enum.declarations.single {
                it is CfirEnumConstructor && it.name.asString() == "None"
            })
            assertSame(some, fixture.provider.getTopLevelCallableSymbols(FqName(fixture.metadata.fullPackageName),
                Name.identifier("Some")).single().cfir)
            val header = kotlin.test.assertNotNull(fixture.provider.sourceHeaderOf(some))
            assertSame(header, fixture.provider.sourceHeaderOf(some))
            assertEquals(header, graph.functionHeaders.getValue(some))
            assertSame(some.valueParameters.single(), header.parameterLists.single().single())
            assertSame(some.symbol, some.valueParameters.single().containingDeclarationSymbol)
            assertSame(some, graph.declarationOwners.getValue(some.valueParameters.single()))
            assertSame(enum, graph.declarationOwners.getValue(some))
            assertNull(header.desugaredParameterLists.single().single())
            assertNull(header.returnType, "原 F.retType=0 不能从 enum 返回签名补造")
            assertFalse(header.bodyAvailable)
            assertEquals(ConePrimitiveType.INT64, some.valueParameters.single().returnTypeRef.coneType)
            val resultType = assertIs<ConeEnumType>(some.returnTypeRef.coneType)
            assertSame(enum.symbol, fixture.provider.getClassLikeSymbolByClassId(resultType.classId))
            assertNull(fixture.provider.sourceHeaderOf(none))
            assertFalse(graph.functionHeaders.containsKey(none))
            // Empty() 的实际 FuncInfo 不因零参数降为 None 的 VarInfo。
            val empty = assertIs<CfirEnumConstructor>(enum.declarations.single {
                it is CfirEnumConstructor && it.name.asString() == "Empty"
            })
            val emptyHeader = kotlin.test.assertNotNull(fixture.provider.sourceHeaderOf(empty))
            assertEquals(listOf(0), emptyHeader.parameterLists.map { it.size })
            assertEquals(listOf(0), emptyHeader.desugaredParameterLists.map { it.size })
            assertFalse(emptyHeader.bodyAvailable)
            assertNull(emptyHeader.returnType)
            assertEquals(emptyHeader, graph.functionHeaders.getValue(empty))
            assertEquals(DeclKind.FuncDecl, fixture.wire.allDecls(kotlin.test.assertNotNull(empty.cjoDeclarationOrigin).declarationIndex)!!.kind)
            val someWire = fixture.wire.allDecls(kotlin.test.assertNotNull(some.cjoDeclarationOrigin).declarationIndex)!!
            val noneWire = fixture.wire.allDecls(kotlin.test.assertNotNull(none.cjoDeclarationOrigin).declarationIndex)!!
            assertEquals(DeclKind.FuncDecl, someWire.kind)
            assertEquals(DeclKind.VarDecl, noneWire.kind)
            val body = kotlin.test.assertNotNull(assertIs<FuncInfo>(someWire.info(FuncInfo())).funcBody)
            assertEquals(0u, body.retType)
            assertEquals(0u, body.body)
            assertEquals(1, body.paramListsLength)
            assertIs<VarInfo>(noneWire.info(VarInfo()))
            listOf(enum, some, none, empty, some.valueParameters.single()).forEach { assertCurrent(it, fixture) }
            assertCanonical(fixture, graph)
        }
    }

    /** SOURCE 的完成值在 graph 生产前发布到 Evaluated 生命周期；不直接注入 canonical 常量结果。 */
    @Test
    fun completedSourceScalarAndVArrayValuesUseTheExplicitTypedSchemaContract(@TempDir root: Path) {
        val module = Module(CfirSession.Kind.Source, CfirCjmpMode.COMMON)
        val psi = createPsiFile("completed", """
            package lifecyclecompleted
            public const signed: Int64 = 42
            public const unsigned: UInt64 = 18446744073709551615u64
            public const floating: Float64 = -0.0
            public const vector: VArray<UInt64, $2> = [0u64, 18446744073709551615u64]
        """.trimIndent()) as CjFile
        val unsigned = BigInteger("18446744073709551615")
        val vectorType = ConeVArrayType(ConePrimitiveType.UINT64, 2L)
        val values = listOf(
            CfirConstantValue.Primitive(ConePrimitiveType.INT64, CfirLiteralKind.INT, BigInteger.valueOf(42)),
            CfirConstantValue.Primitive(ConePrimitiveType.UINT64, CfirLiteralKind.INT, unsigned),
            CfirConstantValue.Primitive(ConePrimitiveType.FLOAT64, CfirLiteralKind.FLOAT, -0.0),
            CfirConstantValue.VArray(vectorType, listOf(
                CfirConstantValue.Primitive(ConePrimitiveType.UINT64, CfirLiteralKind.INT, BigInteger.ZERO),
                CfirConstantValue.Primitive(ConePrimitiveType.UINT64, CfirLiteralKind.INT, unsigned))),
        )
        val names = listOf("signed", "unsigned", "floating", "vector")
        val declarations = values.mapIndexed { index, value ->
            val initializer = if (value is CfirConstantValue.VArray) buildArrayLiteral {
                coneTypeOrNull = value.type
                elements += value.elements.map { literal(ConePrimitiveType.UINT64, CfirLiteralKind.INT,
                    assertIs<CfirConstantValue.Primitive>(it).value) }
            } else literal(value.type, assertIs<CfirConstantValue.Primitive>(value).kind, value.value)
            val syntax = assertIs<CjPatternVariable>(psi.declarations[index])
            initializer.replaceSource(kotlin.test.assertNotNull(syntax.initializer).toCjPsiSourceElement())
            sourceVariable(module, "lifecyclecompleted", names[index], value.type, initializer,
                syntax.toCjPsiSourceElement()).also {
                // 输入 fixture 明确处于已完成值阶段；本用例考察保存消费，不声称执行了源码求值器。
                it.evaluatedInitializer = CfirEvaluatorResult.Evaluated(CfirConstantValueExpression(initializer.source, value))
            }
        }
        val file = sourceFile(module, psi, declarations)
        val graph = CfirSourceGraphProducer.produce(listOf(file))
        declarations.zip(values).forEach { (declaration, value) ->
            assertTrue(declaration.origin.fromSource)
            assertFalse(declaration.exportFacts.isImported)
            assertSame(module, declaration.moduleData)
            assertSame(declaration, declaration.symbol.cfir)
            assertSame(value, graph.constantValues.getValue(declaration))
            assertSame(declaration, graph.expression(graph.initializers.getValue(declaration)).context)
            assertSame(value.type, declaration.returnTypeRef.coneType)
        }
        val typed = CfirCjoPackageMetadataProducer.produce(listOf(file), canonicalSources = listOf(graph),
            exportOptions = CjoExportOptions(serializingCommon = true, sourceEncodingContract = CjoSourceEncodingContract.TYPED_SCHEMA))
        val first = load(root.resolve("typed"), typed)
        val second = reexport(root.resolve("fresh"), first, first.graph())
        for (fixture in listOf(first, second)) {
            assertEquals(BigInteger.valueOf(42), assertIs<CfirConstantValue.Primitive>(fixture.value("signed")).value)
            assertEquals(unsigned, assertIs<CfirConstantValue.Primitive>(fixture.value("unsigned")).value)
            assertEquals(Long.MIN_VALUE, assertIs<Double>(assertIs<CfirConstantValue.Primitive>(fixture.value("floating")).value).toRawBits())
            val vector = assertIs<CfirConstantValue.VArray>(fixture.value("vector"))
            assertEquals(2L, vector.type.size)
            assertSame(ConePrimitiveType.UINT64, vector.type.elementType)
            assertEquals(listOf(BigInteger.ZERO, unsigned), vector.elements.map { assertIs<CfirConstantValue.Primitive>(it).value })
            for ((name, tag) in listOf("signed" to ConstValue.Int64Value, "unsigned" to ConstValue.UInt64Value,
                "floating" to ConstValue.Float64Value, "vector" to ConstValue.ArrayValue)) {
                assertEquals(tag, fixture.variableInfo(name).valueType)
                assertCurrent(fixture.variable(name), fixture)
                assertSame(fixture.variable(name).returnTypeRef.coneType, fixture.value(name).type)
            }
            assertTrue(fixture.metadata.values.isEmpty(), "inline ArrayValue 不分配 composite v 槽")
            assertCanonical(fixture, fixture.graph())
        }
        val native = CfirCjoPackageMetadataProducer.produce(listOf(file), canonicalSources = listOf(graph),
            exportOptions = CjoExportOptions(serializingCommon = true, sourceEncodingContract = CjoSourceEncodingContract.NATIVE_SOURCE))
        assertTrue(native.values.isEmpty())
        native.declarations.filter { it.kind == DeclKind.VarDecl }.forEach {
            assertNull(assertIs<CjoVariableInfo>(it.info).constValue)
        }
        assertTrue(native.expressions.isNotEmpty())
        val nativeConsumer = load(root.resolve("native"), native)
        assertEquals(0, nativeConsumer.wire.allValuesLength)
        names.forEach { assertEquals(ConstValue.NONE, nativeConsumer.variableInfo(it).valueType) }
        assertTrue(nativeConsumer.graph().constantValues.isEmpty())
    }

    /** tuple 的完成类型/元素序不证明新的 CJO storage 布局，不能猜字段名或改用 ArrayValue。 */
    @Test
    fun sourceTupleStorageIsRejectedEvenWhenItsSourceAndCompletedValueAreValid(@TempDir root: Path) {
        val module = Module(CfirSession.Kind.Source, CfirCjmpMode.COMMON)
        val psi = createPsiFile("unknownTuple", "package lifecycleunknowntuple\npublic const pair: (Int64, Int64) = (7, 9)") as CjFile
        val syntax = assertIs<CjPatternVariable>(psi.declarations.single())
        val type = ConeTupleType(listOf(ConePrimitiveType.INT64, ConePrimitiveType.INT64))
        val value = CfirConstantValue.Tuple(type, listOf(
            CfirConstantValue.Primitive(ConePrimitiveType.INT64, CfirLiteralKind.INT, BigInteger.valueOf(7)),
            CfirConstantValue.Primitive(ConePrimitiveType.INT64, CfirLiteralKind.INT, BigInteger.valueOf(9)),
        ))
        val initializer = buildTupleLiteral {
            source = kotlin.test.assertNotNull(syntax.initializer).toCjPsiSourceElement()
            coneTypeOrNull = type
            elements += listOf(literal(ConePrimitiveType.INT64, CfirLiteralKind.INT, 7L),
                literal(ConePrimitiveType.INT64, CfirLiteralKind.INT, 9L))
        }
        val variable = sourceVariable(module, "lifecycleunknowntuple", "pair", type, initializer, syntax.toCjPsiSourceElement())
        variable.evaluatedInitializer = CfirEvaluatorResult.Evaluated(CfirConstantValueExpression(initializer.source, value))
        val file = sourceFile(module, psi, listOf(variable))
        val graph = CfirSourceGraphProducer.produce(listOf(file))
        assertSame(value, graph.constantValues.getValue(variable))
        val record = graph.expression(graph.initializers.getValue(variable))
        assertSame(variable, record.context)
        assertSame(type, record.type)
        assertEquals(2, assertIs<CfirSourceExpressionPayload.Tuple>(record.payload).elements.size)
        val error = assertFailsWith<IllegalStateException> {
            val metadata = CfirCjoPackageMetadataProducer.produce(listOf(file), canonicalSources = listOf(graph),
                exportOptions = CjoExportOptions(serializingCommon = true, sourceEncodingContract = CjoSourceEncodingContract.TYPED_SCHEMA))
            CjoPackageWriter.write(root.resolve("unsupported.cjo"), metadata)
        }
        assertTrue(kotlin.test.assertNotNull(error.message).contains("tuple", ignoreCase = true), "拒绝必须指出缺失的 tuple 表示能力")
        assertFalse(Files.exists(root.resolve("unsupported.cjo")))
        assertSame(value, assertIs<CfirConstantValueExpression>(assertIs<CfirEvaluatorResult.Evaluated>(variable.evaluatedInitializer).result).constantValue)
    }

    /**
     * 原 seed 的 public const global 符合 PRE 资格，会抢占 e#1，不能作为 FINAL 见证。
     * 普通 let 的 @Deprecated[strict: true] header 实参见证 FINAL；非 const interface default 正文见证 PRE。
     * 固定 Native 14276740ffec1262b81b6b5236b1d840af270b37：
     * src/Modules/ASTSerialization/ASTWriter.cpp:310–341/552–556/574–580/1220–1225/1626–1629/1661–1667；
     * src/Parse/ParseAnnotations.cpp:115–118/287–305 确认方括号及 Boolean 实参；AST/Utils.h:174–177 确认 DEFAULT 的 PRE 资格。
     * 固定 Kotlin 3c7a5041f238102285764d370665f56f587f9dc4：
     * compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/serialization/IrFileSerializer.kt:344–353/1098–1112/1298–1303；保留真实 mixed owning 保存顺序。
     */
    @Test
    fun preContentAndFinalUseTheRealMixedStatementAndDoWhileSaveOrder(@TempDir root: Path) {
        val module = Module(CfirSession.Kind.Source, CfirCjmpMode.COMMON)
        val psi = createPsiFile("order", """
            package lifecycleorder
            @Deprecated[strict: true]
            public let finalValue: Int64 = 91
            public interface Api {
                func pre(): Int64 {
                    let local: Int64 = 21
                    22
                    do { 31 } while (false)
                    return 41
                }
            }
        """.trimIndent()) as CjFile
        val finalSyntax = assertIs<CjPatternVariable>(psi.declarations[0])
        val interfaceSyntax = assertIs<CjInterface>(psi.declarations[1])
        val preSyntax = assertIs<CjNamedFunction>(interfaceSyntax.declarations.single())
        val interfaceSymbol = CfirInterfaceSymbol(ClassId.topLevel(FqName("lifecycleorder.Api")))
        val interfaceType = assertIs<ConeClassLikeType>(interfaceSymbol.constructType())
        val functionTarget = CfirFunctionTarget(null, false)
        val bodySyntax = kotlin.test.assertNotNull(preSyntax.bodyBlockExpression)
        assertEquals(4, bodySyntax.statements.size)
        val localSyntax = assertIs<CjPatternVariable>(bodySyntax.statements[0])
        val loopSyntax = assertIs<CjDoWhileExpression>(bodySyntax.statements[2])
        val loopBodySyntax = assertIs<CjBlockExpression>(loopSyntax.body)
        val final = sourceVariable(module, "lifecycleorder", "finalValue", ConePrimitiveType.INT64,
            literal(ConePrimitiveType.INT64, CfirLiteralKind.INT, 91L).also {
                it.replaceSource(kotlin.test.assertNotNull(finalSyntax.initializer).toCjPsiSourceElement())
            }, finalSyntax.toCjPsiSourceElement(), const = false)
        val annotationSyntax = PsiTreeUtil.findChildrenOfType(finalSyntax, CjAnnotation::class.java).single()
        val argumentSyntax = annotationSyntax.valueArguments.single()
        val argumentSource = kotlin.test.assertNotNull(argumentSyntax.getArgumentExpression()).toCjPsiSourceElement()
        val strict = literal(ConePrimitiveType.BOOLEAN, CfirLiteralKind.BOOLEAN, true).also {
            it.replaceSource(argumentSource)
        }
        final.replaceAnnotations(listOf(buildAnnotationCall {
            source = annotationSyntax.toCjPsiSourceElement()
            typeRef = buildImplicitTypeRef { customRenderer = false }
            annotationSourceName = "Deprecated"
            annotationKind = BuiltInAnnotationKind.DEPRECATED
            annotationIdentity = CangjieAnnotationIdentity.LanguageBuiltIn(BuiltInAnnotationKind.DEPRECATED, "Deprecated")
            annotationOrigin = CangjieAnnotationOrigin.LANGUAGE_BUILT_IN
            annotationResolveState = CfirAnnotationResolveState.TYPE_RESOLVED
            containingDeclarationSymbol = final.symbol
            calleeReference = buildNamedReference { name = Name.identifier("Deprecated") }
            argumentList = buildArgumentList {
                        source = annotationSyntax.valueArgumentList?.toCjPsiSourceElement()
                arguments += buildNamedArgumentExpression {
                    source = argumentSyntax.asElement().toCjPsiSourceElement()
                    coneTypeOrNull = ConePrimitiveType.BOOLEAN
                    argumentName = Name.identifier("strict")
                    expression = strict
                }
            }
        }))
        val local = sourceVariable(module, "lifecycleorder", "local", ConePrimitiveType.INT64,
            literal(ConePrimitiveType.INT64, CfirLiteralKind.INT, 21L).also {
                it.replaceSource(kotlin.test.assertNotNull(localSyntax.initializer).toCjPsiSourceElement())
            }, localSyntax.toCjPsiSourceElement(), local = true, const = false)
        val loop = buildLoopExpression {
            source = loopSyntax.toCjPsiSourceElement()
            coneTypeOrNull = ConePrimitiveType.UNIT
            isDoWhile = true
            body = buildBlock {
                source = loopBodySyntax.toCjPsiSourceElement()
                coneTypeOrNull = ConePrimitiveType.UNIT
                statements += literal(ConePrimitiveType.INT64, CfirLiteralKind.INT, 31L).also {
                    it.replaceSource(loopBodySyntax.statements.single().toCjPsiSourceElement())
                }
            }
            condition = literal(ConePrimitiveType.BOOLEAN, CfirLiteralKind.BOOLEAN, false).also {
                it.replaceSource(kotlin.test.assertNotNull(loopSyntax.condition).toCjPsiSourceElement())
            }
        }
        val pre = buildNamedFunction {
            source = preSyntax.toCjPsiSourceElement()
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            isMut = false
            name = Name.identifier("pre")
            symbol = CfirNamedFunctionSymbol(CallableId(interfaceSymbol.classId, name))
            dispatchReceiverType = interfaceType
            status = CfirDeclarationStatusImpl(visibility = Visibilities.Public).apply { isDefault = true }
            returnTypeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef(null)
            body = buildBlock {
                source = bodySyntax.toCjPsiSourceElement()
                coneTypeOrNull = ConePrimitiveType.NOTHING
                statements += listOf(local, literal(ConePrimitiveType.INT64, CfirLiteralKind.INT, 22L).also {
                    it.replaceSource(bodySyntax.statements[1].toCjPsiSourceElement())
                }, loop, buildReturnExpression {
                    val returnSyntax = assertIs<CjReturnExpression>(bodySyntax.statements[3])
                    source = returnSyntax.toCjPsiSourceElement()
                    coneTypeOrNull = ConePrimitiveType.NOTHING
                    target = functionTarget
                    result = literal(ConePrimitiveType.INT64, CfirLiteralKind.INT, 41L).also {
                        it.replaceSource(kotlin.test.assertNotNull(returnSyntax.returnedExpression).toCjPsiSourceElement())
                    }
                })
            }
        }
        functionTarget.bind(pre)
        val api = buildInterface {
            source = interfaceSyntax.toCjPsiSourceElement()
            moduleData = module
            resolvePhase = CfirResolvePhase.IMPORTS
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes().apply {
                cjmpSourceNominalHasDefault = kotlin.test.assertNotNull(interfaceSyntax.cjmpNominalSyntaxFacts)
                    .hasCjmpCommonDefault(module.session.languageVersionSettings)
            }
            scopeProvider = CfirCangJieScopeProvider()
            status = CfirDeclarationStatusImpl(visibility = Visibilities.Public)
            symbol = interfaceSymbol
            name = Name.identifier("Api")
            declarations += pre
        }
        assertFalse(pre.status.isConst)
        assertTrue(pre.status.isDefault)
        val file = sourceFile(module, psi, listOf(final, api))
        val graph = CfirSourceGraphProducer.produce(listOf(file))
        val metadata = CfirCjoPackageMetadataProducer.produce(listOf(file), canonicalSources = listOf(graph))
        val first = load(root.resolve("native"), metadata)
        val second = reexport(root.resolve("fresh"), first, first.graph(), contract = CjoSourceEncodingContract.NATIVE_SOURCE)
        for (fixture in listOf(first, second)) {
            fun index(text: String): Int = fixture.metadata.expressions.indexOfFirst { it.literal?.value == text }
            listOf("21", "22", "31", "false", "41", "true").forEach { text ->
                assertTrue(index(text) >= 0, "保存表必须包含实际 $text 表达式")
            }
            val owner = assertIs<CfirNamedFunction>(fixture.classifier("Api").declarations.single())
            val preBody = kotlin.test.assertNotNull(assertIs<FuncInfo>(fixture.wire.allDecls(
                kotlin.test.assertNotNull(owner.cjoDeclarationOrigin).declarationIndex)!!.info(FuncInfo())).funcBody)
            assertEquals(1u, preBody.body, "PRE 的 F.body 是本次首次 reserve 的 e 根")
            assertTrue(index("21") < index("22"), "Block d initializer 必须先于后一个 e")
            assertTrue(index("31") < index("false"), "DoWhile 保存 body 再保存 condition")
            assertTrue(index("41") < index("true"), "PRE 真正保存内容先于 FINAL header 实参")
            assertEquals(fixture.metadata.expressions.lastIndex, index("true"))
            assertEquals(0u, fixture.variableInfo("finalValue").initializer, "普通全局 let 的初值不满足 Native SOURCE 资格")
            assertEquals((index("true") + 1).toUInt(),
                fixture.metadata.declarations.single { it.identifier == "finalValue" }.annotations.single().arguments.single().expr)
            val outputGraph = fixture.graph()
            val body = assertIs<CfirSourceExpressionPayload.Block>(outputGraph.expression(outputGraph.functionBodies.getValue(owner)).payload)
            val variable = assertIs<CfirFieldVariable>(assertIs<CfirSourceSlot.Declaration>(body.statements.first()).declaration)
            assertSame(owner, outputGraph.declarationOwners.getValue(variable))
            assertSame(variable, outputGraph.expression(outputGraph.initializers.getValue(variable)).context)
            assertSame(ConePrimitiveType.INT64, variable.returnTypeRef.coneType)
            assertCurrent(variable, fixture)
            val savedLoop = outputGraph.expressions.values.single { it.payload is CfirSourceExpressionPayload.Loop }
            val payload = assertIs<CfirSourceExpressionPayload.Loop>(savedLoop.payload)
            assertTrue(payload.isDoWhile)
            assertSame(owner, savedLoop.context)
            val wireLoop = fixture.metadata.expressions.single { it.kind == ExprKind.DoWhileExpr }
            assertEquals(ExprKind.Block, fixture.metadata.expressions[wireLoop.operands[0].toInt() - 1].kind)
            assertEquals("false", fixture.metadata.expressions[wireLoop.operands[1].toInt() - 1].literal?.value)
            assertNull(owner.body)
            assertCanonical(fixture, outputGraph)
        }
    }

    /** Native ASTWriter.cpp:1261–1265 的 partial F 跨 fresh provider 仍是省略签名，而非可调用空参数签名。 */
    @Test
    fun partialFreshHeadersKeepTheUnexportedSignature(@TempDir root: Path) {
        val seed = metadata("lifecyclepartial", listOf(CjoPackageDeclaration("omitted",
            attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL, Attribute.GENERIC_INSTANTIATED,
                Attribute.GENERIC, Attribute.UNREACHABLE), generic = CjoGenericMetadata(emptyList()),
            info = CjoFunctionInfo(body = CjoFunctionBodyInfo()))), emptyList())
        val first = load(root.resolve("seed"), seed)
        val fresh = reexport(root.resolve("fresh"), first, first.graph())
        for (fixture in listOf(first, fresh)) {
            val function = fixture.function("omitted")
            val header = kotlin.test.assertNotNull(fixture.provider.sourceHeaderOf(function))
            assertSame(CfirSourceCallableSignature.Unexported, header.signature)
            assertSame(header.signature, fixture.graph().functionHeaders.getValue(function).signature)
            assertTrue(function.exportFacts.hasUnexportedSignature)
            assertTrue(header.parameterLists.isEmpty())
            assertNull(header.returnType)
            assertFalse(header.bodyAvailable)
            assertFalse(header.always)
            assertEquals(0, header.captureKind)
            assertEquals(0u, fixture.metadata.declarations.single().type)
            assertTrue(fixture.metadata.types.isEmpty())
        }
    }

    private data class Fixture(val metadata: CjoPackageMetadata, val path: Path, val module: Module,
        val manager: CjoManager, val provider: CfirDeserializedSymbolProvider, val snapshot: CjoLoadedPackage,
        val roots: List<CfirDeclaration>, val searchRoots: List<Path>) {
        val wire: Package get() = snapshot.pkg
        fun declaration(index: Int): CfirDeclaration = kotlin.test.assertNotNull(provider.deserializeCjoDeclaration(path, metadata.fullPackageName, index))
        fun function(name: String): CfirNamedFunction = assertIs<CfirNamedFunction>(callable(name))
        fun binding(name: String): CfirPatternBindingVariable = provider.packageVariableBinding(metadata.fullPackageName, name)
        fun variable(name: String): CfirPatternVariable = assertIs<CfirPatternVariable>(binding(name).patternTreeOwner)
        private fun callable(name: String): CfirDeclaration = provider.getTopLevelCallableSymbols(
            FqName(metadata.fullPackageName), Name.identifier(name)).single().cfir
        fun classifier(name: String): CfirClassLikeDeclaration = kotlin.test.assertNotNull(provider.getClassLikeSymbolByClassId(
            ClassId.topLevel(FqName("${metadata.fullPackageName}.$name")))).cfir
        fun graph(): CfirSourceGraph = kotlin.test.assertNotNull(provider.packageSourceGraphOf(roots.first()))
        fun value(name: String): CfirConstantValue = assertIs<CfirConstantValueExpression>(
            assertIs<CfirEvaluatorResult.Evaluated>(binding(name).evaluatedInitializer).result).constantValue
        fun variableInfo(name: String): VarInfo = assertIs<VarInfo>(wire.allDecls(
            kotlin.test.assertNotNull(variable(name).cjoDeclarationOrigin).declarationIndex)!!.info(VarInfo()))
        fun body(name: String): PackageFormat.FuncBody = kotlin.test.assertNotNull(assertIs<FuncInfo>(wire.allDecls(
            kotlin.test.assertNotNull(function(name).cjoDeclarationOrigin).declarationIndex)!!.info(FuncInfo())).funcBody)
    }

    /** 复用现有 fixture 的正式 Manager/Library provider 装配，显式 common 路径保留 CURRENT 角色。 */
    private fun load(root: Path, metadata: CjoPackageMetadata, bytes: ByteArray = CjoPackageWriter.toByteArray(metadata),
        core: ByteArray? = null, searchRoots: List<Path> = emptyList()): Fixture {
        Files.createDirectories(root)
        if (core != null) Files.write(root.resolve("std.core.cjo"), core)
        val path = root.resolve(metadata.fullPackageName + ".cjo").toAbsolutePath().normalize()
        Files.write(path, bytes)
        val manager = CjoManager(CjoSearchPath(additionalLibrarySearchPaths = (listOf(root) + searchRoots).map { it.toFile() },
            envProvider = { null }), CjoFormatVersionPolicy.Compilation(LanguageVersionSettingsImpl.DEFAULT),
            explicitCommonPartPaths = listOf(path))
        val module = Module(CfirSession.Kind.Library)
        val provider = CfirDeserializedSymbolProvider(
            module.session, manager, CfirCangJieScopeProvider(), module,
            moduleDataProvider = cjoFixtureModuleDataProvider(module, (listOf(root) + searchRoots).toSet()),
        )
        module.registerProvider(provider)
        val snapshot = kotlin.test.assertNotNull(manager.loadPackageSnapshot(metadata.fullPackageName))
        val roots = metadata.declarations.withIndex().filter { it.value.isTopLevel }.map {
            kotlin.test.assertNotNull(provider.deserializeCjoDeclaration(path, metadata.fullPackageName, it.index))
        }
        return Fixture(metadata, path, module, manager, provider, snapshot, roots, searchRoots).also { fixture ->
            roots.forEach { assertCurrent(it, fixture) }
            assertSame(snapshot, manager.loadedPackageSnapshot(path))
        }
    }

    private fun reexport(root: Path, input: Fixture, graph: CfirSourceGraph, exportContent: Boolean = true,
        contract: CjoSourceEncodingContract = input.metadata.sourceEncodingContract): Fixture {
        val module = Module(CfirSession.Kind.Source, CfirCjmpMode.COMMON)
        val psi = createPsiFile("reexport", "package ${input.metadata.fullPackageName}\n") as CjFile
        val file = sourceFile(module, psi, emptyList())
        registerCjoSourceFixtureDependencies(module.session, input.provider)
        val metadata = CfirCjoPackageMetadataProducer.produce(listOf(file), familySnapshot = family(input, module.session),
            loadedPackages = listOf(input.snapshot), canonicalSources = listOf(graph),
            exportOptions = CjoExportOptions(exportContent = exportContent, serializingCommon = true, sourceEncodingContract = contract))
        return load(root, metadata, searchRoots = listOf(input.path.parent) + input.searchRoots)
    }

    /** family 仅使用已绑定对象；enum 的 FuncInfo/VarInfo 不按参数数目改写。 */
    private fun family(input: Fixture, actualSession: CfirSession): CfirCjmpFamilySnapshot {
        val nominals = input.roots.filterIsInstance<CfirClassLikeDeclaration>()
        val containers: List<CjmpMergeContainer<CfirBasedSymbol<*>, CfirTypeParameterSymbol>> = nominals.map { owner ->
            CjmpMergeContainer(owner.symbol, owner.name.asString(), when (owner) {
                is CfirClass -> CjmpContainerKind.CLASS
                is CfirStruct -> CjmpContainerKind.STRUCT
                is CfirInterface -> CjmpContainerKind.INTERFACE
                is CfirEnum -> CjmpContainerKind.ENUM
                else -> error("This fixture requires a nominal definition")
            }, owner.status.isCommon, owner.status.isSpecific, owner.hasGenericNode,
                owner.typeParameters.filterIsInstance<CfirTypeParameter>().map { it.symbol }, false, false,
                owner.declarations.mapIndexed { index, member ->
                    val status = assertIs<CfirMemberDeclaration>(member).status
                    val name = when (member) {
                        is CfirEnumConstructor -> member.name.asString()
                        is CfirNamedFunction -> member.name.asString()
                        is CfirVariable -> member.symbol.name.asString()
                        else -> error("This fixture requires a named callable member")
                    }
                    CjmpMergeMember(member.symbol, name, member is CfirFieldVariable && !status.isStatic, false,
                        status.isCommon, status.isSpecific, status.isCommonWithDefault, false,
                        CjmpInitializationDependencies.NotCollected, member.isFromCommonPart, index)
                })
        }
        return CfirCjmpPreparedGroup(FqName(input.metadata.fullPackageName), roots = emptyList(),
            topLevelOrder = input.roots.map { it.symbol },
            originalChildren = nominals.associate { it.symbol to it.declarations.map { child -> child.symbol } },
            commonDeclarations = input.roots.filter { it.isFromCommonPart }.map { it.symbol }, containers = containers,
            extensions = emptyList(), commonExtendOrder = emptyList(), originalDoNotExport = emptySet(),
            classifierDomain = CfirCjmpClassifierDomain.capture(actualSession, input.roots, sourceFiles = emptyList())).evaluate()
    }

    private fun assertCurrent(declaration: CfirDeclaration, fixture: Fixture) {
        assertTrue(declaration.isFromCommonPart)
        assertFalse(declaration.exportFacts.isImported)
        assertSame(fixture.module, declaration.moduleData)
        assertSame(declaration, declaration.symbol.cfir)
        assertEquals(fixture.path.toString(), kotlin.test.assertNotNull(declaration.cjoDeclarationOrigin).packagePath)
    }

    private fun assertCanonical(fixture: Fixture, graph: CfirSourceGraph) {
        assertSame(graph, fixture.graph())
        graph.declarationOwners.forEach { (declaration, _) ->
            assertCurrent(declaration, fixture)
            assertSame(declaration, fixture.declaration(kotlin.test.assertNotNull(declaration.cjoDeclarationOrigin).declarationIndex))
        }
        graph.functionHeaders.forEach { (callable, header) ->
            assertEquals(header, fixture.provider.sourceHeaderOf(callable))
            header.parameterLists.flatten().forEach { parameter ->
                assertSame(callable.symbol, parameter.containingDeclarationSymbol)
                assertSame(parameter, parameter.symbol.cfir)
            }
        }
        graph.constantValues.forEach { (host, value) ->
            assertSame(value, assertIs<CfirConstantValueExpression>(assertIs<CfirEvaluatorResult.Evaluated>(
                if (host is CfirPatternVariable) host.pattern.bindingVariables().single().evaluatedInitializer else host.evaluatedInitializer,
            ).result).constantValue)
            assertSame(host.returnTypeRef.coneType, value.type)
        }
    }

    /** 原文件先 construction 登记，再完成原 Source 父图/RHS，最后解析成员状态。 */
    private fun registerFiles(files: List<CfirFile>) {
        val session = files.first().moduleData.session
        assertTrue(files.all { it.moduleData.session === session })
        val originalHeaders = files.flatMap { it.declarations }.filterIsInstance<CfirClassLikeDeclaration>()
            .map { Triple(it, it.symbol, it.source) }
        val provider = session.cfirProvider as CfirProviderImpl
        val pre = buildPreMacroRawFiles(session, files.map {
            PreMacroCfirFile(it, annotationMetadata = CfirAnnotationMetadataBuilder())
        })
        val result = MacroConstructionService.successOf(pre, files, MacroExpansionRegistry.EMPTY)
        recordExpandedRawFilesOnce(provider, result.recordableFiles, result.registry)
        session.extendIndexStore.rebuild(files, session.typeResolver)
        val scopeSession = ScopeSession()
        files.forEach { completeCjoSourceFixtureClassLikeHeaders(session, it.declarations, scopeSession, it) }
        // Api 的合法空父投影也必须来自正规阶段，且仍是原 Source interface/header。
        if (originalHeaders.isNotEmpty()) {
            val supertypeProvider = kotlin.test.assertNotNull(session.directSupertypeProviderOrNull)
            for ((declaration, symbol, source) in originalHeaders) {
                val product = kotlin.test.assertNotNull(supertypeProvider.getCompletedSupertypeProduct(symbol))
                assertSame(symbol, product.ownerSymbol)
                assertSame(declaration, symbol.cfir)
                assertSame(symbol, declaration.symbol)
                assertSame(source, declaration.source)
                assertTrue(declaration.origin.fromSource)
                assertSame(session, declaration.moduleData.session)
                assertFalse(product.isInReferenceCycle)
                assertEquals(declaration.superTypeRefs, product.superTypeRefs)
                assertSame(declaration, provider.getCfirClassifierByFqName(symbol.classId))
            }
        }
        files.forEach { resolveCjoSourceFixtureStatuses(session, it.declarations, it) }
    }

    private fun sourceFile(module: Module, psi: CjFile, members: List<CfirDeclaration>): CfirFile = buildFile {
        source = psi.toCjPsiSourceElement()
        moduleData = module
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Source
        attributes = CfirDeclarationAttributes.EMPTY
        symbol = CfirFileSymbol()
        name = psi.name
        packageDirective = buildPackageDirective { packageFqName = psi.packageFqName; isMacroPackage = false }
        declarations += members
    }.also {
        registerFiles(listOf(it))
    }

    private fun sourceVariable(module: Module, pkg: String, name: String, type: ConeCangJieType, initializer: CfirExpression,
        source: org.cangnova.cangjie.source.CjSourceElement?, local: Boolean = false, const: Boolean = true): CfirFieldVariable = buildFieldVariable {
        this.source = source
        moduleData = module
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Source
        attributes = CfirDeclarationAttributes.EMPTY
        isLocal = local
        isVar = false
        this.name = Name.identifier(name)
        symbol = CfirFieldVariableSymbol(if (local) CallableId(this.name) else CallableId(FqName(pkg), this.name))
        status = if (local) buildResolvedDeclarationStatus {
            visibility = Visibilities.Local
            modality = org.cangnova.cangjie.descriptors.Modality.FINAL
            isConst = const
            visibilityExplicitness = CfirModifierExplicitness.IMPLICIT
            modalityExplicitness = CfirModifierExplicitness.IMPLICIT
            bodyPresence = CfirBodyPresence.UNKNOWN
            isDeclarationFile = false
            isFromCommonPart = false
            isVisibilityExplicit = false
            isModalityExplicit = false
            isAbstractExplicit = false
            isOverride = false
            isOperator = false
            isStatic = false
            isMut = false
            isUnsafe = false
            isForeign = false
            isC = false
            isCommon = false
            isSpecific = false
            isCommonWithDefault = false
            isRedef = false
            isDefault = false
            isAbstract = false
            isOpen = false
            isSealed = false
        } else CfirDeclarationStatusImpl(visibility = Visibilities.Public).apply { isConst = const }
        returnTypeRef = type.toCfirResolvedTypeRef(null)
        this.initializer = initializer
    }

    private fun literal(type: ConeCangJieType, kind: CfirLiteralKind, value: Any): CfirLiteralExpression = buildLiteralExpression {
        coneTypeOrNull = type
        this.kind = kind
        this.value = value
    }

    private fun headerSeed(core: ByteArray): CjoPackageMetadata = metadata("lifecycleheaders", listOf(
        CjoPackageDeclaration("header", type = 2u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
            annotations = listOf(annotation(2u)), info = CjoFunctionInfo(body = CjoFunctionBodyInfo(
                listOf(listOf(2u)), listOf(listOf(3u)), returnType = 1u))),
        CjoPackageDeclaration("value", DeclKind.FuncParam, isTopLevel = false, type = 1u,
            attributes = flags(Attribute.HAS_INITIAL), annotations = listOf(annotation(3u)),
            info = CjoParameterInfo(isNamed = true, isMember = false, defaultValue = 1u)),
        CjoPackageDeclaration("default_value", isTopLevel = false, type = 3u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
            annotations = listOf(annotation(4u)), info = CjoFunctionInfo(body = CjoFunctionBodyInfo(
                listOf(emptyList()), listOf(emptyList()), returnType = 1u))),
        CjoPackageDeclaration("unrelated", type = 3u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
            info = CjoFunctionInfo(body = CjoFunctionBodyInfo(returnType = 1u, body = 5u))),
    ), listOf(CjoTypeMetadata(TypeKind.Int64), functionType(listOf(1u), 1u), functionType(emptyList(), 1u), stringType(core)),
        listOf(integer("7"), string("11", 4u), string("12", 4u), string("13", 4u), block(1u, listOf(6u)), integer("99")))
        .copy(imports = listOf("std.core"))

    private fun patternSeed(): CjoPackageMetadata = metadata("lifecyclepatterns", listOf(
        CjoPackageDeclaration("patterns", type = 2u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
            info = CjoFunctionInfo(isConst = true, body = CjoFunctionBodyInfo(returnType = 1u, body = 1u))),
    ), listOf(CjoTypeMetadata(TypeKind.Int64), functionType(emptyList(), 1u)), listOf(
        block(1u, listOf(2u)), CjoExpressionMetadata(ExprKind.MatchExpr, type = 1u, operands = listOf(4u, 3u),
            additionalInfo = CjoAdditionalExpressionInfoMetadata.Match(true)),
        CjoExpressionMetadata(ExprKind.MatchCase, type = 1u, operands = listOf(0u, 6u),
            additionalInfo = CjoAdditionalExpressionInfoMetadata.MatchCase(listOf(CjoPatternMetadata(
                PatternKind.ConstPattern, types = listOf(1u), exprs = listOf(5u), values = listOf(CjoInt64ConstValue(7), CjoInt64ConstValue(7)))))),
        integer("7"), integer("7"), block(1u, listOf(7u)), integer("9")))

    private fun enumSeed(): CjoPackageMetadata = metadata("lifecycleenum", listOf(
        CjoPackageDeclaration("Choice", DeclKind.EnumDecl, type = 2u, attributes = flags(Attribute.PUBLIC),
            info = CjoEnumInfo(body = listOf(2u, 4u, 5u), hasArguments = true)),
        CjoPackageDeclaration("Some", DeclKind.FuncDecl, isTopLevel = false, type = 3u,
            attributes = flags(Attribute.PUBLIC, Attribute.ENUM_CONSTRUCTOR),
            info = CjoFunctionInfo(body = CjoFunctionBodyInfo(listOf(listOf(3u)), listOf(listOf(0u))))),
        CjoPackageDeclaration("value", DeclKind.FuncParam, isTopLevel = false, type = 1u,
            info = CjoParameterInfo(isNamed = false, isMember = false)),
        CjoPackageDeclaration("None", DeclKind.VarDecl, isTopLevel = false, type = 2u,
            attributes = flags(Attribute.PUBLIC, Attribute.ENUM_CONSTRUCTOR), info = CjoVariableInfo()),
        CjoPackageDeclaration("Empty", DeclKind.FuncDecl, isTopLevel = false, type = 4u,
            attributes = flags(Attribute.PUBLIC, Attribute.ENUM_CONSTRUCTOR), info = CjoFunctionInfo(
                body = CjoFunctionBodyInfo(listOf(emptyList()), listOf(emptyList())))),
    ), listOf(CjoTypeMetadata(TypeKind.Int64), CjoTypeMetadata(TypeKind.Enum,
        semanticInfo = CjoCompositeTypeInfoMetadata(1u)), functionType(listOf(1u), 2u), functionType(emptyList(), 2u)))

    private fun metadata(pkg: String, declarations: List<CjoPackageDeclaration>, types: List<CjoTypeMetadata>,
        expressions: List<CjoExpressionMetadata> = emptyList(), values: List<CjoCompositeValueMetadata> = emptyList()): CjoPackageMetadata =
        CjoPackageMetadata(pkg, pkg, schemaProfile = CjoSchemaProfile.OFFICIAL_CJMP, allFiles = listOf("lifecycle-schema.cj"),
            declarations = declarations.map { it.copy(attributes = addFlag(it.attributes, Attribute.FROM_COMMON_PART)) },
            types = types, expressions = expressions, values = values, sourceEncodingContract = CjoSourceEncodingContract.TYPED_SCHEMA)

    private fun annotation(index: UInt) = CjoAnnotationMetadata(AnnoKind.Deprecated, "Deprecated",
        listOf(CjoAnnotationArgumentMetadata(null, index)))
    private fun constant(name: String, type: UInt, value: CjoConstValueInfo?) = CjoPackageDeclaration(name, DeclKind.VarDecl,
        type = type, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL), info = CjoVariableInfo(isConst = true, constValue = value))
    private fun functionType(parameters: List<UInt>, result: UInt) = CjoTypeMetadata(TypeKind.Func, parameters,
        semanticInfo = CjoFunctionTypeInfoMetadata(result, false, false))
    private fun integer(text: String) = CjoExpressionMetadata(ExprKind.LitConstExpr, type = 1u,
        literal = CjoLiteralExpressionMetadata(text, LitConstKind.Integer))
    private fun string(text: String, type: UInt) = CjoExpressionMetadata(ExprKind.LitConstExpr, type = type,
        literal = CjoLiteralExpressionMetadata(text, LitConstKind.String))
    private fun block(type: UInt, operands: List<UInt>) = CjoExpressionMetadata(ExprKind.Block, type = type, operands = operands,
        additionalInfo = CjoAdditionalExpressionInfoMetadata.Block(List(operands.size) { true } + false))

    /** 仅操作 schema 种子的 uoffset：VarInfo.value=vt14；MatchCase.patterns=vt4；Pattern.values=vt18。 */
    private class UnionSlots : Table() {
        fun at(position: Int, buffer: ByteBuffer): UnionSlots = apply { __reset(position, buffer) }
        fun expressionPosition(ordinal: Int): Int = __indirect(__vector(__offset(22)) + ordinal * 4)
        fun expressionInfoTypeSlot(): Int = bb_pos + __offset(18).also { check(it != 0) }
        fun variableValueSlot(): Int = bb_pos + __offset(14).also { check(it != 0) }
        fun patternValueSlot(patternOrdinal: Int, valueOrdinal: Int): Int {
            val buffer = bb
            val position = __indirect(__vector(__offset(4)) + patternOrdinal * 4)
            val pattern = UnionSlots().apply { __reset(position, buffer) }
            return pattern.__vector(pattern.__offset(18)) + valueOrdinal * 4
        }
    }

    /** 共享 forward union 目标是 FlatBuffers 合法布局，不改变 tag、宿主 table 或语义绑定。 */
    private fun shareUnionPayload(bytes: ByteArray, first: Int, second: Int) {
        val buffer = ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        val target = maxOf(first + buffer.getInt(first), second + buffer.getInt(second))
        check(target > first && target > second && target < bytes.size)
        buffer.putInt(first, target - first)
        buffer.putInt(second, target - second)
        assertEquals(first + buffer.getInt(first), second + buffer.getInt(second))
    }

    private fun officialCore(): ByteArray = kotlin.test.assertNotNull(javaClass.classLoader.getResourceAsStream(
        "cjo-sdk/windows_x86_64_cjnative/std/std.core.cjo")).use { it.readBytes() }
    private fun stringType(core: ByteArray): CjoTypeMetadata {
        val wire = Package.getRootAsPackage(ByteBuffer.wrap(core))
        val declaration = (0 until wire.allDeclsLength).map { kotlin.test.assertNotNull(wire.allDecls(it)) }
            .single { it.isTopLevel && it.identifier == "String" }
        val type = kotlin.test.assertNotNull(wire.allTypes(declaration.type.toInt() - 1))
        assertTrue(type.kind == TypeKind.Struct || type.kind == TypeKind.Class)
        return CjoTypeMetadata(type.kind,
            semanticInfo = CjoCompositeTypeInfoMetadata(0u, 0, declarationKey = kotlin.test.assertNotNull(declaration.exportId)))
    }
    private fun flags(vararg attributes: Attribute): List<ULong> = attributes.fold(emptyList()) { words, flag -> addFlag(words, flag) }
    private fun addFlag(words: List<ULong>, flag: Attribute): List<ULong> = MutableList(maxOf(words.size, flag.ordinal / 64 + 1)) {
        words.getOrElse(it) { 0uL }
    }.apply { this[flag.ordinal / 64] = this[flag.ordinal / 64] or (1uL shl (flag.ordinal % 64)) }

    /** 正式 source/library session；名称、类型、scope 和 construction 归属与现有 fixture 相同。 */
    private class Module(kind: CfirSession.Kind, mode: CfirCjmpMode? = null) : CfirModuleData() {
        override val name = Name.identifier("source-export-lifecycle-test")
        override val dependencies: List<CfirModuleData> = emptyList()
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform = CfirPlatform.DEFAULT
        override val isCommon = mode == CfirCjmpMode.COMMON
        override val stableModuleName = "source-export-lifecycle-test"
        override val session = object : CfirSession(kind) {}.also {
            it.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
            it.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = mode))
            it.register(CfirExportAnalysisSettings::class, CfirExportAnalysisSettings())
            it.register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
            it.register(TypeComponents::class, TypeComponents(it))
            it.register(CfirLazyDeclarationResolver::class, CfirDummyCompilerLazyDeclarationResolver)
            it.registerTypeAwareSupertypeComponents()
            if (kind == CfirSession.Kind.Source) {
                it.register(CfirImportBindingStore::class, CfirImportBindingStore())
                it.register(CfirDefaultImportsProviderHolder::class, CfirDefaultImportsProviderHolder.of(CfirDefaultImportsProvider))
            }
        }
        init {
            bindSession(session)
            session.register(CfirModuleData::class, this)
            if (kind == CfirSession.Kind.Source) {
                // 对位 CfirAbstractSessionFactory.createSourceSession：own extend 来自真实源码索引。
                val provider = CfirProviderImpl(session, session.cangjieScopeProvider)
                session.register(CfirProvider::class, provider)
                session.register(CfirSymbolProvider::class, provider.symbolProvider)
                session.register(CfirTypeResolver::class, CfirTypeResolverImpl(session))
                val indexStore = CfirExtendIndexStore(session)
                session.register(CfirExtendIndexStore::class, indexStore)
                val ownExtends = CfirSessionExtendProvider(session, indexStore)
                session.register(OWN_EXTEND_PROVIDER_QUALIFIED_KEY, ownExtends)
                session.register(CfirExtendProvider::class, CfirExtendProviderComposer.combine(
                    ownProvider = ownExtends, dependencyProviders = emptyList(),
                ))
            }
        }
        fun registerProvider(provider: CfirSymbolProvider) {
            session.register(CfirProvider::class, CfirLibrarySessionProvider(provider))
            session.register(CfirSymbolProvider::class, provider)
            session.register(CfirExtendProvider::class, CfirExtendProviderComposer.fromSymbolProviders(listOf(provider)))
        }
    }
}
