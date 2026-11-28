@file:OptIn(ExperimentalUnsignedTypes::class, org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.test.cjoFixtureModuleDataProvider
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import PackageFormat.*
import com.google.flatbuffers.Table
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.CjPsiSourceFile
import org.cangnova.cangjie.source.CjPsiSourceFileLinesMapping
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.patterns.builder.*
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.providers.macro.*
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.annotations.CangjieAnnotationIdentity
import org.cangnova.cangjie.annotations.CangjieAnnotationOrigin
import org.cangnova.cangjie.cfir.builder.hasCjmpCommonDefault
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolver
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolverImpl
import org.cangnova.cangjie.cfir.resolve.providers.*
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendIndexStore
import org.cangnova.cangjie.cfir.resolve.source.CfirSourceGraphProducer
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.scopes.CfirDefaultImportsProviderHolder
import org.cangnova.cangjie.cfir.resolve.CfirDefaultImportsProvider
import org.cangnova.cangjie.cfir.resolve.services.CfirImportBindingStore
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
import org.cangnova.cangjie.resolve.calls.mpp.CjmpContainerKind
import org.cangnova.cangjie.resolve.calls.mpp.CjmpInitializationDependencies
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMergeContainer
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMergeMember
import org.cangnova.cangjie.source.toCjPsiSourceElement
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

/**
 * R3/R4/R6/R9 的正式 provider → completed graph → producer → fresh CJO 消费边界。
 *
 * 固定 Kotlin 3c7a5041f238102285764d370665f56f587f9dc4 的 IrFileSerializer.kt
 * 共享 statement/default/F helper；固定 Native 14276740ffec1262b81b6b5236b1d840af270b37
 * 的 ASTWriter.cpp:537–581/602–642/1258–1291/1583–1602、
 * ExprWriter.cpp:136–169/302–310/689–829 与 Position.h:26 提供格式合同。
 * 依据 unified-repair-first-gate.md 和本批 Kotlin/Native 权威报告，不改原 fixture/正确 expect。
 *
 * 手写 CJO 是有界 schema 种子，不是固定 Native 生产非空 v 或复杂 annotation/default 的证据。
 * SOURCE 输入只装配已完成类型、状态和依赖产品；不宣称运行了源码求值器或实例化器。
 * 本补丁生成时编译冻结：未合入、未编译、未执行；行为状态由主线程合入后的运行决定。
 */
class CjoUnifiedSourceReviewBoundaryTest :
    CjParsingTestCase("", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition()) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()


    /** R3：default 与 annotation 各自拥有 Block；局部初值不能随 named F.body 门一起关闭。 */
    @Test
    fun headerDefaultBlockKeepsLocalInitializerAndSkipsPackageInitializer(@TempDir root: Path) =
        checkHeaderBlock(root, annotationBlock = false)

    @Test
    fun headerAnnotationBlockKeepsLocalInitializerAndSkipsPackageInitializer(@TempDir root: Path) =
        checkHeaderBlock(root, annotationBlock = true)

    private fun checkHeaderBlock(root: Path, annotationBlock: Boolean) {
        val core = officialCore()
        val annotation = CjoAnnotationMetadata(AnnoKind.Annotation, "Annotation",
            listOf(CjoAnnotationArgumentMetadata(null, 1u)))
        val seed = metadata("reviewheaders", listOf(
            CjoPackageDeclaration("header", type = 2u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
                annotations = if (annotationBlock) listOf(annotation) else emptyList(),
                info = CjoFunctionInfo(body = CjoFunctionBodyInfo(
                    listOf(listOf(2u)), listOf(listOf(3u)), returnType = 1u))),
            CjoPackageDeclaration("value", DeclKind.FuncParam, isTopLevel = false, type = 1u,
                attributes = flags(Attribute.HAS_INITIAL),
                info = CjoParameterInfo(isNamed = true, isMember = false,
                    defaultValue = if (annotationBlock) 8u else 1u)),
            CjoPackageDeclaration("default_value", type = 3u, isTopLevel = false,
                attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
                info = CjoFunctionInfo(body = CjoFunctionBodyInfo(
                    listOf(emptyList()), listOf(emptyList()), returnType = 1u))),
            CjoPackageDeclaration("unrelated", type = 3u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
                info = CjoFunctionInfo(body = CjoFunctionBodyInfo(returnType = 1u, body = 4u))),
            CjoPackageDeclaration("unselectedPackage", DeclKind.VarDecl, type = 1u,
                attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL), info = CjoVariableInfo(initializer = 6u)),
            CjoPackageDeclaration("local", DeclKind.VarDecl, type = 1u, isTopLevel = false,
                annotations = listOf(CjoAnnotationMetadata(AnnoKind.Deprecated, "Deprecated",
                    listOf(CjoAnnotationArgumentMetadata(null, 7u)))),
                info = CjoVariableInfo(initializer = 3u)),
        ), listOf(CjoTypeMetadata(TypeKind.Int64), functionType(listOf(1u), 1u),
            functionType(emptyList(), 1u), stringType(core)), listOf(
            CjoExpressionMetadata(ExprKind.Block, type = 1u, operands = listOf(6u, 2u),
                additionalInfo = CjoAdditionalExpressionInfoMetadata.Block(listOf(false, true, false))),
            integer("19"), integer("17"), block(1u, listOf(5u)), integer("99"), integer("88"), string("中😀", 4u),
            integer("19"), // 参数 default 与注解 Block 的末表达式是两个独立 owning 节点。
        )).copy(imports = listOf("std.core"))
        val bytes = CjoPackageWriter.toByteArray(seed)
        val buffer = ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        val slots = WireSlots().at(buffer.getInt(0), buffer)
        val badBlock = WireSlots().at(slots.expressionPosition(3), buffer)
        // owning index 仍能读取合法 mixed 槽，真正 canonical decoder 会拒绝不匹配的 union tag。
        buffer.put(badBlock.expressionInfoTypeSlot(), ExprInfo.ReferenceInfo.toByte())
        val first = load(root.resolve("seed"), seed, bytes, core)
        val owner = first.function("header")
        val bad = first.function("unrelated")
        assertTrue(kotlin.test.assertNotNull(first.provider.sourceHeaderOf(bad)).bodyAvailable)
        assertTrue(first.variableInfo("unselectedPackage").initializer != 0u)
        val selected = kotlin.test.assertNotNull(first.provider.sourceExportSnapshotOf(owner, false))
        assertSame(first.snapshot, selected.source)
        assertSame(first.module.session, selected.domain)
        val graph = kotlin.test.assertNotNull(selected.graph)
        assertTrue(graph.functionBodies.isEmpty())
        assertTrue(bad in graph.unloadedFunctionBodies)
        assertFalse(first.variable("unselectedPackage") in graph.initializers)
        val selectedIdentity = if (annotationBlock) graph.annotations.getValue(owner).single().arguments.single().expression
            else graph.defaults.getValue(owner.valueParameters.single())
        val local = selectedLocal(graph, selectedIdentity)
        assertSame(if (annotationBlock) owner else owner.valueParameters.single(), graph.declarationOwners.getValue(local))
        assertSame(local, first.declaration(kotlin.test.assertNotNull(local.cjoDeclarationOrigin).declarationIndex))
        assertEquals("17", assertIs<CfirSourceExpressionPayload.Literal>(
            graph.expression(graph.initializers.getValue(local)).payload).text)
        val note = graph.expression(graph.annotations.getValue(local).single().arguments.single().expression)
        assertEquals(StdlibClassIds.String, assertIs<ConeStructType>(note.type).classId)
        assertEquals("中😀", assertIs<CfirSourceExpressionPayload.Literal>(note.payload).text)
        assertFalse(graph.expressions.values.any { (it.payload as? CfirSourceExpressionPayload.Literal)?.text in setOf("88", "99") })
        val second = reexport(root.resolve("fresh"), first, graph, exportContent = false)
        assertEquals(0u, second.body("header").body)
        assertEquals(0u, second.body("unrelated").body)
        assertEquals(0u, second.variableInfo("unselectedPackage").initializer)
        val freshOwner = second.function("header")
        val fresh = second.graph()
        val freshIdentity = if (annotationBlock) fresh.annotations.getValue(freshOwner).single().arguments.single().expression
            else fresh.defaults.getValue(freshOwner.valueParameters.single())
        val freshLocal = selectedLocal(fresh, freshIdentity)
        val localWire = kotlin.test.assertNotNull(second.wire.allDecls(
            kotlin.test.assertNotNull(freshLocal.cjoDeclarationOrigin).declarationIndex))
        assertTrue(assertIs<VarInfo>(localWire.info(VarInfo())).initializer != 0u)
        assertSame(freshLocal, fresh.expression(fresh.initializers.getValue(freshLocal)).context)
        assertEquals("17", assertIs<CfirSourceExpressionPayload.Literal>(
            fresh.expression(fresh.initializers.getValue(freshLocal)).payload).text)
        assertNotSame(local, freshLocal)
        assertFailsWith<IllegalStateException> { first.provider.packageSourceGraphOf(bad) }
        assertSame(local, selectedLocal(graph, selectedIdentity))
        assertSame(owner, first.function("header"))
    }

    private fun selectedLocal(graph: CfirSourceGraph, identity: CfirSourceExpressionIdentity): CfirFieldVariable {
        val block = assertIs<CfirSourceExpressionPayload.Block>(graph.expression(identity).payload)
        return assertIs<CfirFieldVariable>(assertIs<CfirSourceSlot.Declaration>(block.statements.first()).declaration)
    }

    /** R4：loaded 原位属性必须按本次 native outer 投影；INITIALIZED 不移动到其他 bit。 */
    @Test
    fun loadedVarDeclAttributesUseCommonAndSpecificOuterRules(@TempDir root: Path) {
        val first = load(root.resolve("seed"), attributeSeed())
        val original = first.classifier("CommonBox").declarations.filterIsInstance<CfirFieldVariable>()
        assertNull(original.single { it.name.asString() == "absent" }.initializer)
        assertTrue(original.single { it.name.asString() == "absent" }.exportFacts.isInitialized)
        val graph = first.graph()
        val present = original.single { it.name.asString() == "present" }
        assertTrue(present in graph.initializers)
        val before = original.associateWith { it.exportFacts }
        val second = reexport(root.resolve("fresh"), first, graph)
        checkAttributeProjection(second)
        before.forEach { (declaration, facts) -> assertSame(facts, declaration.exportFacts) }
    }

    private fun attributeSeed(): CjoPackageMetadata = metadata("reviewattrs", listOf(
        CjoPackageDeclaration("CommonBox", DeclKind.StructDecl, type = 2u,
            attributes = flags(Attribute.PUBLIC, Attribute.COMMON), info = CjoStructInfo(body = listOf(2u, 3u, 4u))),
        CjoPackageDeclaration("present", DeclKind.VarDecl, isTopLevel = false, type = 1u,
            attributes = flags(Attribute.PUBLIC, Attribute.COMMON, Attribute.SPECIFIC),
            info = CjoVariableInfo(initializer = 1u)),
        CjoPackageDeclaration("absent", DeclKind.VarDecl, isTopLevel = false, type = 1u,
            attributes = flags(Attribute.PUBLIC, Attribute.COMMON, Attribute.INITIALIZED), info = CjoVariableInfo()),
        CjoPackageDeclaration("ready", DeclKind.VarDecl, isTopLevel = false, type = 1u,
            attributes = flags(Attribute.PUBLIC, Attribute.STATIC, Attribute.INITIALIZED), info = CjoVariableInfo()),
        CjoPackageDeclaration("SpecificBox", DeclKind.StructDecl, type = 3u,
            attributes = flags(Attribute.PUBLIC, Attribute.SPECIFIC), info = CjoStructInfo(body = listOf(6u, 7u))),
        CjoPackageDeclaration("clone", DeclKind.VarDecl, isTopLevel = false, type = 1u,
            attributes = flags(Attribute.PUBLIC, Attribute.COMMON, Attribute.SPECIFIC), info = CjoVariableInfo()),
        CjoPackageDeclaration("specificField", DeclKind.VarDecl, isTopLevel = false, type = 1u,
            attributes = flags(Attribute.PUBLIC, Attribute.SPECIFIC), info = CjoVariableInfo()),
    ), listOf(CjoTypeMetadata(TypeKind.Int64),
        CjoTypeMetadata(TypeKind.Struct, semanticInfo = CjoCompositeTypeInfoMetadata(1u)),
        CjoTypeMetadata(TypeKind.Struct, semanticInfo = CjoCompositeTypeInfoMetadata(5u))), listOf(integer("7")))

    private fun checkAttributeProjection(fixture: Fixture) {
        val common = fixture.classifier("CommonBox")
        val specific = fixture.classifier("SpecificBox")
        fun member(owner: CfirClassLikeDeclaration, name: String): PackageFormat.Decl {
            val declaration = owner.declarations.filterIsInstance<CfirFieldVariable>().single { it.name.asString() == name }
            return kotlin.test.assertNotNull(fixture.wire.allDecls(kotlin.test.assertNotNull(declaration.cjoDeclarationOrigin).declarationIndex))
        }
        assertFlag(member(common, "present"), Attribute.INITIALIZED, true)
        assertFlag(member(common, "present"), Attribute.SPECIFIC, false)
        assertFlag(member(common, "absent"), Attribute.INITIALIZED, false)
        assertFlag(member(common, "ready"), Attribute.INITIALIZED, true)
        assertFlag(member(common, "ready"), Attribute.STATIC, true)
        for (name in listOf("clone", "specificField")) {
            val field = member(specific, name)
            assertFlag(field, Attribute.SPECIFIC, false)
            assertFlag(field, Attribute.COMMON, false)
            assertFlag(field, Attribute.FROM_COMMON_PART, false)
        }
        assertTrue(assertIs<VarInfo>(member(common, "present").info(VarInfo())).initializer != 0u)
        assertEquals(0u, assertIs<VarInfo>(member(common, "absent").info(VarInfo())).initializer)
    }

    /**
     * 原 seed 未发布名义 parser default fact，且普通非泛型成员没有初值 SOURCE 导出资格。
     * CommonBox 改为真实泛型 owner，按 PSI 绑定 T；不改普通字段的 const 角色以强制初值输出。
     * 固定 Native 14276740ffec1262b81b6b5236b1d840af270b37：
     * src/Modules/ASTSerialization/ASTWriter.cpp:310–322、src/Parse/ParseCJMPDecl.cpp:143–169；default fact 来自直接成员语法快照。
     */
    @Test
    fun sourceVarDeclAttributesUseCommonAndSpecificOuterRules(@TempDir root: Path) {
        val pkg = "reviewsourceattrs"
        val module = Module(CfirSession.Kind.Source, CfirCjmpMode.COMMON)
        val psi = createPsiFile("sourceattrs", """
            package reviewsourceattrs
            public common struct CommonBox<T> {
                public let present: Int64 = 7
                public let absent: Int64
                public static let ready: Int64
            }
            public specific struct SpecificBox {
                public let clone: Int64
                public let specificField: Int64
            }
        """.trimIndent()) as CjFile
        fun owner(name: String, common: Boolean): CfirStruct {
            val syntax = psi.declarations.filterIsInstance<CjStruct>().single { it.name == name }
            val symbol = CfirStructSymbol(ClassId.topLevel(FqName("$pkg.$name")))
            val parameters = syntax.typeParameters.map { parameterSyntax ->
                buildTypeParameter {
                    source = parameterSyntax.toCjPsiSourceElement()
                    moduleData = module
                    resolvePhase = CfirResolvePhase.BODY_RESOLVE
                    origin = CfirDeclarationOrigin.Source
                    attributes = CfirDeclarationAttributes.EMPTY
                    containingDeclarationSymbol = symbol
                    this.name = Name.identifier(kotlin.test.assertNotNull(parameterSyntax.name))
                    this.symbol = CfirTypeParameterSymbol()
                }
            }
            val type = ConeStructType(symbol.toLookupTag(), parameters.map { it.symbol.constructType() })
            val fields = syntax.variables.map { field ->
                val fieldName = kotlin.test.assertNotNull(field.name)
                val value = if (fieldName == "present") literal(ConePrimitiveType.INT64, CfirLiteralKind.INT, 7L).also {
                    it.replaceSource(kotlin.test.assertNotNull(field.initializer).toCjPsiSourceElement())
                } else null
                sourceVariable(module, pkg, fieldName, ConePrimitiveType.INT64, value, field.toCjPsiSourceElement(),
                    const = false, memberOwner = symbol.classId, receiverType = type).also {
                    it.replaceStatus(CfirDeclarationStatusImpl(visibility = Visibilities.Public).apply {
                        isCommon = common || fieldName == "clone"
                        isSpecific = !common || fieldName == "present"
                        isStatic = fieldName == "ready"
                    })
                    it.isFromCommonPart = !common
                    it.exportFacts = it.exportFacts.copy(isInitialized = fieldName in setOf("absent", "ready"))
                }
            }
            return buildStruct {
                source = syntax.toCjPsiSourceElement()
                moduleData = module
                resolvePhase = CfirResolvePhase.IMPORTS
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes().apply {
                    cjmpSourceNominalHasDefault = kotlin.test.assertNotNull(syntax.cjmpNominalSyntaxFacts)
                        .hasCjmpCommonDefault(module.session.languageVersionSettings)
                    hasGenericNode = syntax.typeParameterList != null
                }
                scopeProvider = CfirCangJieScopeProvider()
                status = CfirDeclarationStatusImpl(visibility = Visibilities.Public).apply {
                    isCommon = common; isSpecific = !common
                }
                this.symbol = symbol
                this.name = Name.identifier(name)
                typeParameters += parameters
                declarations += fields
            }.also {
                it.hasGenericNode = syntax.typeParameterList != null
                assertEquals(kotlin.test.assertNotNull(syntax.cjmpNominalSyntaxFacts)
                    .hasCjmpCommonDefault(module.session.languageVersionSettings), it.cjmpSourceNominalHasDefault)
            }
        }
        val file = sourceFile(module, psi, listOf(owner("CommonBox", true), owner("SpecificBox", false)))
        registerFiles(listOf(file))
        val graph = CfirSourceGraphProducer.produce(listOf(file))
        val metadata = CfirCjoPackageMetadataProducer.produce(listOf(file), canonicalSources = listOf(graph),
            exportOptions = CjoExportOptions(serializingCommon = true))
        val first = load(root.resolve("native"), metadata)
        checkAttributeProjection(first)
        assertTrue(first.metadata.values.isEmpty())
    }

    /** partial 的 UNREACHABLE 同时省略 d.type 与所有 F 槽，两个函数各自持有独立 empty F。 */
    @Test
    fun partialUnreachableOmitsTypeAndDoesNotReserveItsParametersOrTypeSlots(@TempDir root: Path) {
        val core = officialCore()
        val seed = metadata("reviewpartial", listOf(
            CjoPackageDeclaration("partialA", type = 2u,
                attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL, Attribute.GENERIC_INSTANTIATED, Attribute.GENERIC),
                generic = CjoGenericMetadata(emptyList()),
                info = CjoFunctionInfo(body = CjoFunctionBodyInfo(listOf(listOf(2u)), listOf(listOf(0u)), 1u))),
            CjoPackageDeclaration("omittedA", DeclKind.FuncParam, isTopLevel = false, type = 1u,
                info = CjoParameterInfo(isNamed = false, isMember = false)),
            CjoPackageDeclaration("partialB", type = 2u,
                attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL, Attribute.GENERIC_INSTANTIATED, Attribute.GENERIC),
                generic = CjoGenericMetadata(emptyList()),
                info = CjoFunctionInfo(body = CjoFunctionBodyInfo(returnType = 1u))),
        ), listOf(stringType(core), functionType(listOf(1u), 1u))).copy(imports = listOf("std.core"))
        val first = load(root.resolve("seed"), seed, core = core)
        assertEquals(StdlibClassIds.String, assertIs<ConeStructType>(first.function("partialA").returnTypeRef.coneType).classId)
        val second = reexport(root.resolve("fresh"), first, first.graph())
        assertEquals(listOf("partialA", "partialB"), second.metadata.declarations.map { it.identifier })
        assertTrue(second.metadata.types.isEmpty(), "省略的签名不应额外保存 t")
        val bodies = (0 until second.wire.allDeclsLength).map { ordinal ->
            val declaration = kotlin.test.assertNotNull(second.wire.allDecls(ordinal))
            assertFlag(declaration, Attribute.UNREACHABLE, true)
            assertEquals(0u, declaration.type)
            val body = kotlin.test.assertNotNull(assertIs<FuncInfo>(declaration.info(FuncInfo())).funcBody)
            assertEquals(0, body.paramListsLength)
            assertEquals(0u, body.retType)
            assertEquals(0u, body.body)
            assertFalse(body.always)
            assertEquals(0.toUByte(), body.captureKind)
            val function = second.function(kotlin.test.assertNotNull(declaration.identifier))
            val header = kotlin.test.assertNotNull(second.provider.sourceHeaderOf(function))
            assertSame(CfirSourceCallableSignature.Unexported, header.signature)
            assertSame(header.signature, second.graph().functionHeaders.getValue(function).signature)
            assertTrue(function.exportFacts.hasUnexportedSignature)
            body
        }
        // 两个独立内嵌 F 存槽；不以每次 accessor 分配不同包装对象冒充存槽身份。
        val a = assertIs<WireSlots>(second.wire.allDecls(0)!!.info(WireSlots()))
        val b = assertIs<WireSlots>(second.wire.allDecls(1)!!.info(WireSlots()))
        assertNotEquals(a.functionBodyPosition(), b.functionBodyPosition())
        assertEquals(2, bodies.size)
    }

    /** Native ASTWriter.cpp:1261–1265/1579–1602：省略签名只允许 UNREACHABLE 和严格空 F 同时存在。 */
    @Test
    fun unexportedSignatureRejectsMissingUnreachableAndEveryNonemptyFunctionSlot(@TempDir root: Path) {
        val empty = CjoFunctionBodyInfo()
        val omitted = CjoPackageDeclaration("omitted", attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL, Attribute.UNREACHABLE),
            info = CjoFunctionInfo(body = empty))
        val parameter = CjoPackageDeclaration("value", DeclKind.FuncParam, isTopLevel = false, type = 1u,
            info = CjoParameterInfo(isNamed = false, isMember = false))
        val invalidBodies = listOf(
            "parameter" to empty.copy(parameterLists = listOf(listOf(2u)), desugaredParameterLists = listOf(listOf(0u))),
            "returnType" to empty.copy(returnType = 1u),
            "body" to empty.copy(body = 1u),
            "always" to empty.copy(always = true),
            "capture" to empty.copy(captureKind = 1u),
        )
        val seed = metadata("reviewpartialnegative", listOf(omitted, parameter), listOf(CjoTypeMetadata(TypeKind.Int64)),
            listOf(block(1u, listOf(2u)), integer("7")))
        assertFailsWith<IllegalStateException>("type0 没有 UNREACHABLE 不能伪装成省略签名") {
            load(root.resolve("noMarker"), seed.copy(declarations = listOf(
                omitted.copy(attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL)), parameter)))
        }
        assertFailsWith<IllegalStateException>("普通 F 的非零 retType 不能替代缺失的 d.FuncTy") {
            load(root.resolve("returnWithoutFuncTy"), seed.copy(declarations = listOf(omitted.copy(
                attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL), info = CjoFunctionInfo(body = empty.copy(returnType = 1u))), parameter)))
        }
        invalidBodies.forEach { (slot, body) ->
            val invalid = omitted.copy(info = CjoFunctionInfo(body = body))
            assertFailsWith<IllegalStateException>("UNREACHABLE 非空 $slot 必须拒绝") {
                load(root.resolve(slot), seed.copy(declarations = listOf(invalid, parameter)))
            }
        }
    }

    /** R6：真实 enum FuncInfo/F 非零 body 是 typed-schema 事实；None 的 VarInfo 始终没有 F。 */
    @Test
    fun typedEnumNonzeroBodySurvivesTheCompletedGraphPlan(@TempDir root: Path) {
        val seed = metadata("reviewenum", listOf(
            CjoPackageDeclaration("Choice", DeclKind.EnumDecl, type = 2u, attributes = flags(Attribute.PUBLIC),
                info = CjoEnumInfo(body = listOf(2u, 4u), hasArguments = true)),
            CjoPackageDeclaration("Some", DeclKind.FuncDecl, isTopLevel = false, type = 3u,
                attributes = flags(Attribute.PUBLIC, Attribute.ENUM_CONSTRUCTOR),
                info = CjoFunctionInfo(body = CjoFunctionBodyInfo(listOf(listOf(3u)), listOf(listOf(0u)), body = 1u))),
            CjoPackageDeclaration("value", DeclKind.FuncParam, isTopLevel = false, type = 1u,
                info = CjoParameterInfo(isNamed = false, isMember = false)),
            CjoPackageDeclaration("None", DeclKind.VarDecl, isTopLevel = false, type = 2u,
                attributes = flags(Attribute.PUBLIC, Attribute.ENUM_CONSTRUCTOR), info = CjoVariableInfo()),
        ), listOf(CjoTypeMetadata(TypeKind.Int64),
            CjoTypeMetadata(TypeKind.Enum, semanticInfo = CjoCompositeTypeInfoMetadata(1u)),
            functionType(listOf(1u), 2u)), listOf(block(2u, listOf(2u)),
            CjoExpressionMetadata(ExprKind.RefExpr, type = 2u,
                reference = CjoReferenceExpressionMetadata("None", CjoExpressionTargetMetadata(-2, index = 4u)))))
        val first = load(root.resolve("seed"), seed)
        val graph = first.graph()
        val second = reexport(root.resolve("fresh"), first, graph)
        for (fixture in listOf(first, second)) {
            val enum = assertIs<CfirEnum>(fixture.classifier("Choice"))
            val some = assertIs<CfirEnumConstructor>(enum.declarations.single {
                it is CfirEnumConstructor && it.name.asString() == "Some"
            })
            val none = assertIs<CfirEnumConstructor>(enum.declarations.single {
                it is CfirEnumConstructor && it.name.asString() == "None"
            })
            val type = assertIs<ConeEnumType>(some.returnTypeRef.coneType)
            assertSame(enum.symbol, fixture.provider.getClassLikeSymbolByClassId(type.classId))
            val header = kotlin.test.assertNotNull(fixture.provider.sourceHeaderOf(some))
            assertTrue(header.bodyAvailable)
            assertNull(header.returnType, "原 F.retType=0 不能补造")
            assertNull(fixture.provider.sourceHeaderOf(none))
            val completed = fixture.graph()
            assertTrue(some in completed.functionBodies)
            assertFalse(none in completed.functionHeaders)
            val body = assertIs<CfirSourceExpressionPayload.Block>(
                completed.expression(completed.functionBodies.getValue(some)).payload)
            val returned = completed.expression(assertIs<CfirSourceSlot.Expression>(body.statements.single()).identity)
            assertIs<ConeEnumType>(returned.type)
            assertSame(none, assertIs<CfirSourceReferenceTarget.Declaration>(assertIs<CfirSourceExpressionPayload.Reference>(returned.payload).target).declaration)
            val someWire = fixture.wire.allDecls(kotlin.test.assertNotNull(some.cjoDeclarationOrigin).declarationIndex)!!
            val noneWire = fixture.wire.allDecls(kotlin.test.assertNotNull(none.cjoDeclarationOrigin).declarationIndex)!!
            assertEquals(DeclKind.FuncDecl, someWire.kind)
            assertTrue(kotlin.test.assertNotNull(assertIs<FuncInfo>(someWire.info(FuncInfo())).funcBody).body != 0u)
            assertEquals(DeclKind.VarDecl, noneWire.kind)
            assertIs<VarInfo>(noneWire.info(VarInfo()))
            assertSame(some, completed.declarationOwners.getValue(some.valueParameters.single()))
            assertSame(some, some.symbol.cfir)
        }
    }

    /** Member 的 receiver→target→instTys→parentTy；Array 的 args→initFunc，填充不得改号。 */
    @Test
    fun memberAndArraySaveReferenceAndTypeSlotsAfterTheirOwnedChildren(@TempDir root: Path) {
        val seed = metadata("reviewrefs", listOf(
            CjoPackageDeclaration("pre", type = 2u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
                info = CjoFunctionInfo(isConst = true, body = CjoFunctionBodyInfo(returnType = 1u, body = 1u))),
            CjoPackageDeclaration("receiverLocal", DeclKind.VarDecl, isTopLevel = false, type = 3u,
                info = CjoVariableInfo(initializer = 4u)),
            CjoPackageDeclaration("argumentLocal", DeclKind.VarDecl, isTopLevel = false, type = 1u,
                info = CjoVariableInfo(initializer = 8u)),
            CjoPackageDeclaration("target", type = 2u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
                info = CjoFunctionInfo(body = CjoFunctionBodyInfo(returnType = 1u))),
            CjoPackageDeclaration("init", type = 8u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
                info = CjoFunctionInfo(body = CjoFunctionBodyInfo(listOf(listOf(6u)), listOf(listOf(0u)), returnType = 7u))),
            CjoPackageDeclaration("element", DeclKind.FuncParam, isTopLevel = false, type = 1u,
                info = CjoParameterInfo(isNamed = false, isMember = false)),
        ), listOf(CjoTypeMetadata(TypeKind.Int64), functionType(emptyList(), 1u), CjoTypeMetadata(TypeKind.Int32),
            CjoTypeMetadata(TypeKind.UInt8), CjoTypeMetadata(TypeKind.UInt16), CjoTypeMetadata(TypeKind.Invalid),
            CjoTypeMetadata(TypeKind.VArray, listOf(1u), semanticInfo = CjoArrayTypeInfoMetadata(1L)),
            functionType(listOf(1u), 7u)), listOf(
            block(1u, listOf(2u, 5u, 9u, 10u)),
            CjoExpressionMetadata(ExprKind.MemberAccess, type = 1u, operands = listOf(3u),
                reference = CjoReferenceExpressionMetadata("target", CjoExpressionTargetMetadata(-2, index = 4u),
                    instantiatedTypes = listOf(4u), matchedParentType = 5u)),
            CjoExpressionMetadata(ExprKind.Block, type = 1u, operands = listOf(2u, 11u),
                additionalInfo = CjoAdditionalExpressionInfoMetadata.Block(listOf(false, true, false))),
            CjoExpressionMetadata(ExprKind.LitConstExpr, type = 3u, literal = CjoLiteralExpressionMetadata("31", LitConstKind.Integer)),
            CjoExpressionMetadata(ExprKind.ArrayExpr, type = 7u, operands = listOf(6u),
                additionalInfo = CjoAdditionalExpressionInfoMetadata.Array(CjoExpressionTargetMetadata(-2, index = 5u), true)),
            // Native ExprWriter.cpp:667–687 / ReferenceLoader.cpp:657–674：FuncArg 有自己的应用后类型。
            CjoExpressionMetadata(ExprKind.InvalidExpr, type = 1u, operands = listOf(7u),
                functionArgument = CjoFunctionArgumentExpressionMetadata(false, false)),
            CjoExpressionMetadata(ExprKind.Block, type = 1u, operands = listOf(3u, 12u),
                additionalInfo = CjoAdditionalExpressionInfoMetadata.Block(listOf(false, true, false))),
            integer("41"),
            CjoExpressionMetadata(ExprKind.RefExpr, type = 1u,
                reference = CjoReferenceExpressionMetadata("target", CjoExpressionTargetMetadata(-2, index = 4u),
                    matchedParentType = 6u)),
            CjoExpressionMetadata(ExprKind.RefExpr, type = 1u,
                reference = CjoReferenceExpressionMetadata("target", CjoExpressionTargetMetadata(-2, index = 4u))),
            integer("39"), integer("49"),
        ))
        val first = load(root.resolve("seed"), seed)
        val second = reexport(root.resolve("fresh"), first, first.graph())
        val third = reexport(root.resolve("again"), second, second.graph())
        for (fixture in listOf(second, third)) {
            fun d(name: String) = fixture.metadata.declarations.indexOfFirst { it.identifier == name }
            fun t(kind: UShort) = fixture.metadata.types.indexOfFirst { it.kind == kind }
            assertTrue(d("receiverLocal") in 0 until d("target"), "receiver 内拥有 d 必须先于 borrowed CURRENT target")
            assertTrue(d("argumentLocal") in 0 until d("init"), "args 内拥有 d 必须先于 initFunc")
            assertTrue(t(TypeKind.Int32) in 0 until t(TypeKind.UInt8))
            assertTrue(t(TypeKind.UInt8) in 0 until t(TypeKind.UInt16))
            assertTrue(t(TypeKind.UInt16) in 0 until t(TypeKind.Invalid))
            val member = fixture.metadata.expressions.single { it.kind == ExprKind.MemberAccess }
            val reference = kotlin.test.assertNotNull(member.reference)
            assertEquals((d("target") + 1).toUInt(), kotlin.test.assertNotNull(reference.target).index)
            assertEquals(-2, reference.target!!.pkgId)
            assertEquals(listOf((t(TypeKind.UInt8) + 1).toUInt()), reference.instantiatedTypes)
            assertEquals((t(TypeKind.UInt16) + 1).toUInt(), reference.matchedParentType)
            val refs = fixture.metadata.expressions.filter { it.kind == ExprKind.RefExpr }.map { kotlin.test.assertNotNull(it.reference) }
            assertEquals(setOf(0u, (t(TypeKind.Invalid) + 1).toUInt()), refs.map { it.matchedParentType }.toSet())
            val argument = fixture.metadata.expressions.single { it.functionArgument != null }
            assertEquals((t(TypeKind.Int64) + 1).toUInt(), argument.type, "FuncArg 保留实参的完成 Int64 类型")
            val array = assertIs<CjoAdditionalExpressionInfoMetadata.Array>(
                fixture.metadata.expressions.single { it.kind == ExprKind.ArrayExpr }.additionalInfo)
            assertEquals(PackageIndex.CURRENT.value, kotlin.test.assertNotNull(array.initializer).pkgId)
            assertEquals((d("init") + 1).toUInt(), kotlin.test.assertNotNull(array.initializer).index)
            val graph = fixture.graph()
            val payload = assertIs<CfirSourceExpressionPayload.Member>(graph.expressions.values.single {
                it.payload is CfirSourceExpressionPayload.Member
            }.payload)
            assertSame(fixture.function("target"), assertIs<CfirSourceReferenceTarget.Declaration>(payload.reference.target).declaration)
            assertEquals(listOf(ConePrimitiveType.UINT8), payload.reference.instantiatedTypes)
            assertEquals(org.cangnova.cangjie.cfir.references.CfirReferenceParentType.Instantiated(ConePrimitiveType.UINT16),
                payload.reference.matchedParentType)
            val currentArray = assertIs<CfirSourceExpressionPayload.Array>(graph.expressions.values.single {
                it.payload is CfirSourceExpressionPayload.Array
            }.payload)
            assertNull(currentArray.initializer, "固定 native TYPE_VARRAY 不绑定保存的 unused initFunc")
            kotlin.test.assertNotNull(currentArray.savedInitializer, "raw 字段来源和 semantic factory 分开保存")
            assertIs<ConeVArrayType>(graph.expressions.values.single { it.payload is CfirSourceExpressionPayload.Array }.type)
            assertEquals(CfirSourceReferenceKind.SOURCE, currentArray.initializerKind)
            assertEquals(0u, fixture.body("target").body)
            assertEquals(0u, fixture.body("init").body)
        }
        assertEquals(second.metadata.declarations.map { it.identifier }, third.metadata.declarations.map { it.identifier })
        assertEquals(second.metadata.expressions.map { it.kind to it.operands }, third.metadata.expressions.map { it.kind to it.operands })
    }

    /** PRE seed 不因输入 inventory 而获准；suppressed 根与未引用 PRIVATE inline 根均无 PRE 内容。 */
    @Test
    fun preSeedsRespectShouldAlwaysExportAndSuppression(@TempDir root: Path) {
        val seed = metadata("reviewseeds", listOf(
            CjoPackageDeclaration("visible", type = 2u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
                info = CjoFunctionInfo(isConst = true, body = CjoFunctionBodyInfo(returnType = 1u, body = 1u))),
            CjoPackageDeclaration("hidden", type = 2u, attributes = flags(Attribute.PRIVATE, Attribute.GLOBAL),
                info = CjoFunctionInfo(isInline = true, body = CjoFunctionBodyInfo(returnType = 1u, body = 3u))),
            CjoPackageDeclaration("suppressed", type = 2u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
                info = CjoFunctionInfo(isConst = true, body = CjoFunctionBodyInfo(returnType = 1u, body = 5u))),
        ), listOf(CjoTypeMetadata(TypeKind.Int64), functionType(emptyList(), 1u)), listOf(
            block(1u, listOf(2u)), integer("11"), block(1u, listOf(4u)), integer("77"), block(1u, listOf(6u)), integer("88")))
        val first = load(root.resolve("seed"), seed)
        val graph = first.graph()
        val second = reexport(root.resolve("fresh"), first, graph, serializingCommon = false,
            suppressed = setOf(first.function("suppressed").symbol))
        assertFalse(second.metadata.declarations.any { it.identifier == "suppressed" })
        assertFalse(second.metadata.expressions.any { it.literal?.value in setOf("77", "88") })
        assertTrue(second.metadata.expressions.any { it.literal?.value == "11" })
        assertTrue(second.body("visible").body != 0u)
        // FINAL common 根保留合同独立于 PRE：若 hidden 被写出，正文仍不得通过 PRE 偷渡。
        second.metadata.declarations.singleOrNull { it.identifier == "hidden" }?.let {
            assertEquals(0u, assertIs<CjoFunctionInfo>(it.info).body!!.body)
        }
    }

    /** PRE 成员无 parent classifier 类型依赖时先 reserve 自己，parent sidecar 不抢占 d ordinal。 */
    @Test
    fun preMemberReservesBeforeItsOwningNominal(@TempDir root: Path) {
        val seed = metadata("reviewparent", listOf(
            CjoPackageDeclaration("Box", DeclKind.StructDecl, type = 3u, attributes = flags(Attribute.PUBLIC),
                info = CjoStructInfo(body = listOf(2u))),
            CjoPackageDeclaration("inlineMember", isTopLevel = false, type = 2u,
                attributes = flags(Attribute.PUBLIC),
                info = CjoFunctionInfo(isInline = true, body = CjoFunctionBodyInfo(returnType = 1u, body = 1u))),
        ), listOf(CjoTypeMetadata(TypeKind.Int64), functionType(emptyList(), 1u),
            CjoTypeMetadata(TypeKind.Struct, semanticInfo = CjoCompositeTypeInfoMetadata(1u))),
            listOf(block(1u, listOf(2u)), integer("29")))
        val first = load(root.resolve("seed"), seed)
        val second = reexport(root.resolve("fresh"), first, first.graph())
        assertEquals(listOf("inlineMember", "Box"), second.metadata.declarations.map { it.identifier })
        val owner = second.classifier("Box")
        val member = assertIs<CfirNamedFunction>(owner.declarations.single())
        assertSame(owner, second.graph().declarationOwners.getValue(member))
        assertEquals(0, kotlin.test.assertNotNull(member.cjoDeclarationOrigin).declarationIndex)
        assertTrue(kotlin.test.assertNotNull(assertIs<FuncInfo>(second.wire.allDecls(0)!!.info(FuncInfo())).funcBody).body != 0u)
    }

    /**
     * 原 const seed 的初值会参与 PRE，且不能把常量引用的保留形式当作 FINAL DFS 证据。
     * 改用普通 let 的合法 Deprecated[strict: Boolean] header e 见证 FINAL；
     * 初值中的真实 target 引用仍负责发布文件依赖，普通全局 let 初值不要求写出。
     * 固定 Native 14276740ffec1262b81b6b5236b1d840af270b37：
     * src/Modules/ASTSerialization/ASTWriter.cpp:602–642/1220–1225/1626–1633/1661–1667，src/Parse/ParseAnnotations.cpp:287–305。
     */
    @Test
    fun finalFileOrderUsesPublishedDependenciesBeforeTheDependentFile(@TempDir root: Path) {
        val module = Module(CfirSession.Kind.Source, CfirCjmpMode.COMMON)
        val dependentPsi = createPsiFile("dependent", """
            package reviewdfs
            @Deprecated[strict: true]
            public let dependent: Int64 = target
            @Deprecated[strict: false]
            public let dependentTail: Int64 = 22
        """.trimIndent()) as CjFile
        val targetPsi = createPsiFile("target", """
            package reviewdfs
            @Deprecated[strict: true]
            public let target: Int64 = 11
            @Deprecated[strict: false]
            public let targetTail: Int64 = 12
        """.trimIndent()) as CjFile
        fun variables(psi: CjFile, values: List<Long>) = psi.declarations.mapIndexed { index, declaration ->
            val syntax = assertIs<CjPatternVariable>(declaration)
            // 与 PsiRawCfirBuilder.convertCasePattern 一致，从简单绑定读取名字，声明本身没有名字。
            val binding = assertIs<CjBindingPattern>(syntax.pattern)
            sourceVariable(module, "reviewdfs", kotlin.test.assertNotNull(binding.name), ConePrimitiveType.INT64,
                syntax.initializer?.let { initializerSyntax ->
                    literal(ConePrimitiveType.INT64, CfirLiteralKind.INT, values[index]).also {
                        it.replaceSource(initializerSyntax.toCjPsiSourceElement())
                    }
                }, syntax.toCjPsiSourceElement(), const = false).also { variable ->
                val annotationSyntax = PsiTreeUtil.findChildrenOfType(syntax, CjAnnotation::class.java).single()
                val argumentSyntax = annotationSyntax.valueArguments.single()
                val argumentExpression = kotlin.test.assertNotNull(argumentSyntax.getArgumentExpression())
                val strict = literal(ConePrimitiveType.BOOLEAN, CfirLiteralKind.BOOLEAN,
                    argumentExpression.text == "true").also { it.replaceSource(argumentExpression.toCjPsiSourceElement()) }
                assertTrue(argumentExpression.text in setOf("true", "false"))
                variable.replaceAnnotations(listOf(buildAnnotationCall {
                    source = annotationSyntax.toCjPsiSourceElement()
                    typeRef = buildImplicitTypeRef { customRenderer = false }
                    annotationSourceName = "Deprecated"
                    annotationKind = BuiltInAnnotationKind.DEPRECATED
                    annotationIdentity = CangjieAnnotationIdentity.LanguageBuiltIn(BuiltInAnnotationKind.DEPRECATED, "Deprecated")
                    annotationOrigin = CangjieAnnotationOrigin.LANGUAGE_BUILT_IN
                    annotationResolveState = CfirAnnotationResolveState.TYPE_RESOLVED
                    containingDeclarationSymbol = variable.symbol
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
            }
        }
        val dependents = variables(dependentPsi, listOf(21L, 22L))
        val targets = variables(targetPsi, listOf(11L, 12L))
        val dependencySyntax = assertIs<CjPatternVariable>(dependentPsi.declarations.first())
        dependents.first().replaceInitializer(buildNamedAccessExpression {
            source = kotlin.test.assertNotNull(dependencySyntax.initializer).toCjPsiSourceElement()
            coneTypeOrNull = ConePrimitiveType.INT64
            calleeReference = buildResolvedNamedReference {
                name = targets.first().name
                resolvedSymbol = targets.first().symbol
            }
        })
        // 初始化依赖作为完成产品一次发布，writer 只消费；这里的边有真实已解析引用。
        dependents.first().publishInitializationDependencies(listOf(targets.first().symbol))
        targets.first().publishInitializationDependencies(emptyList())
        val dependencyState = dependents.first().initializationDependencies
        val files = listOf(sourceFile(module, dependentPsi, dependents), sourceFile(module, targetPsi, targets))
        registerFiles(files)
        val graph = CfirSourceGraphProducer.produce(files)
        val inputReference = graph.expression(graph.initializers.getValue(dependents.first()))
        assertSame(dependents.first(), inputReference.context)
        assertSame(targets.first(), assertIs<CfirSourceReferenceTarget.Declaration>(assertIs<CfirSourceExpressionPayload.Reference>(inputReference.payload).target).declaration)
        assertSame(ConePrimitiveType.INT64, inputReference.type)
        val metadata = CfirCjoPackageMetadataProducer.produce(files, canonicalSources = listOf(graph),
            exportOptions = CjoExportOptions(serializingCommon = true))
        assertSame(dependencyState, dependents.first().initializationDependencies)
        assertSame(targets.first().symbol, assertIs<CjmpInitializationDependencies.Collected<CfirBasedSymbol<*>>>(dependencyState).declarations.single())
        val consumer = load(root.resolve("native"), metadata)
        assertEquals(listOf("target", "targetTail", "dependent", "dependentTail"), consumer.metadata.declarations.map { it.identifier })
        assertEquals(listOf("true", "false", "true", "false"), consumer.metadata.expressions.mapNotNull { it.literal?.value })
        consumer.metadata.declarations.forEachIndexed { index, declaration ->
            assertEquals(0u, assertIs<CjoVariableInfo>(declaration.info).initializer)
            val argument = declaration.annotations.single().arguments.single()
            assertEquals("strict", argument.name)
            assertEquals((index + 1).toUInt(), argument.expr, "每个 FINAL header 保持自身 e 身份与同文件原序")
        }
        val dependency = consumer.metadata.declarations.single { it.identifier == "dependent" }.dependencies.single()
        assertEquals(1u, dependency.index)
        val completed = consumer.graph()
        assertTrue(completed.initializers.isEmpty())
        val dependent = consumer.variable("dependent")
        val target = consumer.variable("target")
        assertEquals((kotlin.test.assertNotNull(target.cjoDeclarationOrigin).declarationIndex + 1).toUInt(), dependency.index)
        assertCurrent(dependent, consumer)
        assertCurrent(target, consumer)
        consumer.roots.forEach { declaration ->
            assertCurrent(declaration, consumer)
            val argument = completed.annotations.getValue(declaration).single().arguments.single()
            assertSame(declaration, completed.expression(argument.expression).context)
            assertSame(ConePrimitiveType.BOOLEAN, completed.expression(argument.expression).type)
        }
        assertTrue(metadata.values.isEmpty())
    }

    /** R9：真实 tuple/child/Type P 自己的 span；中文和补充平面字符按 UTF-8 字节列。 */
    @Test
    fun sourcePatternsKeepOwnSpansAndGeneratedInnerPatternHasNoSource(@TempDir root: Path) =
        checkSourcePatterns(root, bindingInner = false)

    @Test
    fun generatedInnerVariablePatternAlsoHasNoSource(@TempDir root: Path) =
        checkSourcePatterns(root, bindingInner = true)

    private fun checkSourcePatterns(root: Path, bindingInner: Boolean) {
        // 原 seed 的 MatchBranch 无 completed type；typed binding 的 status 必填布尔字段尚未完整发布。
        // 固定 Native 14276740ffec1262b81b6b5236b1d840af270b37：
        // src/Modules/ASTSerialization/ReferenceLoader.cpp:732–758/745–748，src/Modules/ASTSerialization/ExprWriter.cpp:720–781；P 和 MatchCase 各有自己的 type/source。
        // 这里仅补足真实 PSI 的完成输入，生成 inner P 继续没有 source。
        val module = Module(CfirSession.Kind.Source, CfirCjmpMode.COMMON)
        val psi = createPsiFile("patternpositions", """
            package reviewpositions
            public const func patterns(): Int64 {
                /*中😀*/ let (汉, _) = (7, 9)
                match (1) { case _: Int64 => 9 }
            }
        """.trimIndent().let { if (bindingInner) it.replace("case _: Int64", "case typed: Int64") else it }) as CjFile
        val functionSyntax = assertIs<CjNamedFunction>(psi.declarations.single())
        val bodySyntax = kotlin.test.assertNotNull(functionSyntax.bodyBlockExpression)
        val variableSyntax = assertIs<CjPatternVariable>(bodySyntax.statements.first())
        val tupleSyntax = assertIs<CjTuplePattern>(variableSyntax.pattern)
        assertEquals(2, tupleSyntax.patterns.size)
        val tupleType = ConeTupleType(listOf(ConePrimitiveType.INT64, ConePrimitiveType.INT64))
        val binding = buildPatternBindingVariable {
            source = tupleSyntax.patterns[0].toCjPsiSourceElement()
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            status = buildResolvedDeclarationStatus {
                visibility = Visibilities.Local
                modality = org.cangnova.cangjie.descriptors.Modality.FINAL
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
                isConst = false
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
            }
            name = Name.identifier("汉")
            symbol = CfirPatternBindingSymbol(CallableId(name))
            isVar = false
            returnTypeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef(null)
        }
        val tuplePattern = buildTuplePattern {
            source = tupleSyntax.toCjPsiSourceElement()
            semanticTypeRef = tupleType.toCfirResolvedTypeRef(source)
            elements += buildBindingPattern {
                source = tupleSyntax.patterns[0].toCjPsiSourceElement()
                semanticTypeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef(source)
                name = binding.name
                bindingVariable = binding
            }
            elements += buildWildcardPattern {
                source = tupleSyntax.patterns[1].toCjPsiSourceElement()
                semanticTypeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef(source)
            }
        }
        val variable = buildPatternVariable {
            source = variableSyntax.toCjPsiSourceElement()
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            status = buildResolvedDeclarationStatus {
                visibility = Visibilities.Local
                modality = org.cangnova.cangjie.descriptors.Modality.FINAL
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
                isConst = false
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
            }
            symbol = CfirPatternVariableSymbol(CallableId(Name.special("<pattern-variable>")))
            isVar = false
            returnTypeRef = tupleType.toCfirResolvedTypeRef(null)
            pattern = tuplePattern
            initializer = buildTupleLiteral {
                source = kotlin.test.assertNotNull(variableSyntax.initializer).toCjPsiSourceElement()
                coneTypeOrNull = tupleType
                val tupleInitializer = assertIs<CjTupleExpression>(variableSyntax.initializer)
                elements += listOf(7L, 9L).zip(tupleInitializer.expressions).map { (value, syntax) ->
                    literal(ConePrimitiveType.INT64, CfirLiteralKind.INT, value).also {
                        it.replaceSource(syntax.toCjPsiSourceElement())
                    }
                }
            }
        }
        val matchSyntax = assertIs<CjMatchExpression>(bodySyntax.statements.last())
        val branchSyntax = matchSyntax.entries.single()
        val branchBodySyntax = kotlin.test.assertNotNull(branchSyntax.expression)
        val selectorSyntax = kotlin.test.assertNotNull(matchSyntax.subjectExpression)
        val typeSyntax = PsiTreeUtil.findChildrenOfType(psi, CjTypePattern::class.java).single()
        val typeReferenceSyntax = kotlin.test.assertNotNull(typeSyntax.typeReference)
        val typedPattern = buildTypePattern {
            source = typeSyntax.toCjPsiSourceElement()
            typeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef(typeReferenceSyntax.toCjPsiSourceElement())
            matchingKind = CfirTypePatternMatchingKind.ALWAYS
            semanticTypeRef = typeRef
            matchBeforeRuntime = true
            needRuntimeTypeCheck = false
            if (bindingInner) {
                val variable = buildPatternBindingVariable {
                    source = kotlin.test.assertNotNull(typeSyntax.nameIdentifier).toCjPsiSourceElement()
                    moduleData = module
                    resolvePhase = CfirResolvePhase.BODY_RESOLVE
                    origin = CfirDeclarationOrigin.Source
                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = true
                    status = buildResolvedDeclarationStatus {
                        visibility = Visibilities.Local
                        modality = org.cangnova.cangjie.descriptors.Modality.FINAL
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
                        isConst = false
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
                    }
                    name = Name.identifier("typed")
                    symbol = CfirPatternBindingSymbol(CallableId(name))
                    isVar = false
                    returnTypeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef(typeReferenceSyntax.toCjPsiSourceElement())
                }
                nestedPattern = buildBindingPattern {
                    // 人工生成的无 source P；真实 parser 的 inner P 与 binding d 都保留各自位置。
                    source = null; name = variable.name; bindingVariable = variable
                    semanticTypeRef = this@buildTypePattern.semanticTypeRef
                }
            } else nestedPattern = buildWildcardPattern { semanticTypeRef = this@buildTypePattern.semanticTypeRef }
        }
        val match = buildMatchExpression {
            source = bodySyntax.statements.last().toCjPsiSourceElement()
            coneTypeOrNull = ConePrimitiveType.INT64
            subject = literal(ConePrimitiveType.INT64, CfirLiteralKind.INT, 1L).also {
                it.replaceSource(selectorSyntax.toCjPsiSourceElement())
            }
            branches += buildMatchBranch {
                source = branchSyntax.toCjPsiSourceElement()
                coneTypeOrNull = ConePrimitiveType.INT64
                pattern = typedPattern
                body = buildBlock {
                    source = branchBodySyntax.toCjPsiSourceElement()
                    coneTypeOrNull = ConePrimitiveType.INT64
                    statements += literal(ConePrimitiveType.INT64, CfirLiteralKind.INT, 9L).also {
                        it.replaceSource(branchBodySyntax.statements.single().toCjPsiSourceElement())
                    }
                }
            }
        }
        val function = namedFunction(module, "reviewpositions", "patterns", functionSyntax,
            buildBlock {
                source = bodySyntax.toCjPsiSourceElement()
                coneTypeOrNull = ConePrimitiveType.INT64
                statements += variable
                statements += match
            })
        val file = sourceFile(module, psi, listOf(function))
        registerFiles(listOf(file))
        val graph = CfirSourceGraphProducer.produce(listOf(file))
        val tuple = graph.declarationPatterns.getValue(variable)
        assertSpan(tuple, tupleSyntax.textRange.startOffset, tupleSyntax.textRange.endOffset)
        tuple.patterns.zip(tupleSyntax.patterns).forEach { (record, syntax) ->
            assertSpan(record, syntax.textRange.startOffset, syntax.textRange.endOffset)
        }
        assertNotEquals(tuple.span, graph.expression(graph.initializers.getValue(variable)).span)
        val type = assertIs<CfirSourceExpressionPayload.MatchCase>(
            graph.expressions.values.single { it.payload is CfirSourceExpressionPayload.MatchCase }.payload).patterns.single()
        assertSpan(type, typeSyntax.textRange.startOffset, typeSyntax.textRange.endOffset)
        assertSame(ConePrimitiveType.INT64, type.types.single())
        assertTrue(type.matchBeforeRuntime)
        assertFalse(type.needRuntimeTypeCheck)
        val branch = graph.expressions.values.single { it.payload is CfirSourceExpressionPayload.MatchCase }
        assertSame(ConePrimitiveType.INT64, branch.type)
        assertSame(function, branch.context)
        assertEquals(branchSyntax.textRange.startOffset, kotlin.test.assertNotNull(branch.span).beginOffset)
        assertEquals(branchSyntax.textRange.endOffset, kotlin.test.assertNotNull(branch.span).endOffset)
        assertNull(type.patterns.single().span, "generated inner P 没有 source，不能复制 outer P")
        assertEquals(if (bindingInner) CfirSourcePatternKind.VARIABLE else CfirSourcePatternKind.WILDCARD,
            type.patterns.single().kind)
        val metadata = CfirCjoPackageMetadataProducer.produce(listOf(file), canonicalSources = listOf(graph))
        val first = load(root.resolve("native"), metadata)
        val second = reexport(root.resolve("fresh"), first, first.graph(), contract = CjoSourceEncodingContract.NATIVE_SOURCE)
        for (fixture in listOf(first, second)) {
            val encoded = kotlin.test.assertNotNull(fixture.metadata.declarations.mapNotNull {
                (it.info as? CjoVarWithPatternInfo)?.irrefutablePattern
            }.single())
            assertPosition(encoded.begin, bytePoint(psi.text, tupleSyntax.textRange.startOffset))
            assertPosition(encoded.end, bytePoint(psi.text, tupleSyntax.textRange.endOffset))
            encoded.patterns.zip(tupleSyntax.patterns).forEach { (pattern, syntax) ->
                assertPosition(pattern.begin, bytePoint(psi.text, syntax.textRange.startOffset))
                assertPosition(pattern.end, bytePoint(psi.text, syntax.textRange.endOffset))
            }
            assertEquals(3, encoded.begin!!.line)
            assertEquals(21, encoded.begin!!.column, "4空格 + /*中😀*/ 的 UTF-8 字节 + let 空格")
            assertNotEquals(bytePoint(psi.text, variableSyntax.textRange.startOffset), bytePoint(psi.text, tupleSyntax.textRange.startOffset))
            val matchCase = assertIs<CjoAdditionalExpressionInfoMetadata.MatchCase>(
                fixture.metadata.expressions.single { it.kind == ExprKind.MatchCase }.additionalInfo)
            val outer = matchCase.patterns.single()
            assertPosition(outer.begin, bytePoint(psi.text, typeSyntax.textRange.startOffset))
            assertPosition(outer.end, bytePoint(psi.text, typeSyntax.textRange.endOffset))
            assertTrue(outer.matchBeforeRuntime)
            assertFalse(outer.needRuntimeTypeCheck)
            val inner = outer.patterns.single()
            assertEquals(if (bindingInner) PatternKind.VarPattern else PatternKind.WildcardPattern, inner.kind)
            assertNull(inner.begin)
            assertNull(inner.end)
            // 进一步从 fresh wire 存槽核缺失，而非只看 metadata 对象。
            val ordinal = fixture.metadata.expressions.indexOfFirst { it.kind == ExprKind.MatchCase }
            val wireCase = assertIs<MatchCaseInfo>(fixture.wire.allExprs(ordinal)!!.info(MatchCaseInfo()))
            val wireInner = kotlin.test.assertNotNull(kotlin.test.assertNotNull(wireCase.patterns(0)).patterns(0))
            assertNull(wireInner.begin)
            assertNull(wireInner.end)
        }
    }

    /** imported present-invalid 仍有真实存槽；absent、单端点缺失、ignore/file0 不能合并。 */
    @Test
    fun importedPatternPositionsKeepPresentInvalidSeparateFromAbsent(@TempDir root: Path) {
        val invalid = CjoPositionMetadata(file = 0u, line = 0, column = 0, ignore = true)
        val valid = CjoPositionMetadata(file = 1u, line = 8, column = 13)
        val patterns = listOf(
            CjoPatternMetadata(PatternKind.WildcardPattern, types = listOf(1u), begin = invalid, end = invalid),
            CjoPatternMetadata(PatternKind.WildcardPattern, types = listOf(1u)),
            CjoPatternMetadata(PatternKind.WildcardPattern, types = listOf(1u), begin = valid),
            CjoPatternMetadata(PatternKind.WildcardPattern, types = listOf(1u), end = valid),
        )
        val seed = metadata("reviewimportedpositions", listOf(
            CjoPackageDeclaration("patterns", type = 2u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
                info = CjoFunctionInfo(isConst = true, body = CjoFunctionBodyInfo(returnType = 1u, body = 1u))),
        ), listOf(CjoTypeMetadata(TypeKind.Int64), functionType(emptyList(), 1u)), listOf(
            block(1u, listOf(2u)),
            CjoExpressionMetadata(ExprKind.MatchExpr, type = 1u, operands = listOf(4u, 3u),
                additionalInfo = CjoAdditionalExpressionInfoMetadata.Match(true)),
            CjoExpressionMetadata(ExprKind.MatchCase, type = 1u, operands = listOf(0u, 5u),
                additionalInfo = CjoAdditionalExpressionInfoMetadata.MatchCase(patterns)),
            integer("7"), block(1u, listOf(6u)), integer("9"),
        ))
        val first = load(root.resolve("seed"), seed)
        val graph = first.graph()
        val original = assertIs<CfirSourceExpressionPayload.MatchCase>(graph.expressions.values.single {
            it.payload is CfirSourceExpressionPayload.MatchCase
        }.payload).patterns
        kotlin.test.assertNotNull(kotlin.test.assertNotNull(original[0].importedPosition).beginIdentity)
        assertNull(kotlin.test.assertNotNull(original[0].importedPosition).begin)
        assertNull(kotlin.test.assertNotNull(original[1].importedPosition).beginIdentity)
        assertNull(kotlin.test.assertNotNull(original[1].importedPosition).begin)
        val second = reexport(root.resolve("fresh"), first, graph)
        for (fixture in listOf(first, second)) {
            val ordinal = fixture.metadata.expressions.indexOfFirst { it.kind == ExprKind.MatchCase }
            val wire = assertIs<MatchCaseInfo>(fixture.wire.allExprs(ordinal)!!.info(MatchCaseInfo()))
            val present = kotlin.test.assertNotNull(wire.patterns(0))
            for (position in listOf(kotlin.test.assertNotNull(present.begin), kotlin.test.assertNotNull(present.end))) {
                assertEquals(0u, position.file)
                assertEquals(0u, position.pkgId)
                assertEquals(0, position.line)
                assertEquals(0, position.column)
                assertTrue(position.ignore)
            }
            val absent = kotlin.test.assertNotNull(wire.patterns(1))
            assertNull(absent.begin)
            assertNull(absent.end)
            val beginOnly = kotlin.test.assertNotNull(wire.patterns(2))
            assertNull(beginOnly.end)
            assertEquals(8, kotlin.test.assertNotNull(beginOnly.begin).line)
            assertEquals(13, beginOnly.begin!!.column)
            val endOnly = kotlin.test.assertNotNull(wire.patterns(3))
            assertNull(endOnly.begin)
            assertEquals(8, kotlin.test.assertNotNull(endOnly.end).line)
        }
    }

    private fun assertSpan(pattern: CfirSourcePattern, begin: Int, end: Int) {
        val span = kotlin.test.assertNotNull(pattern.span)
        assertEquals(begin, span.beginOffset)
        assertEquals(end, span.endOffset)
    }

    /** 期望由真实源码 UTF-8 前缀计算，不调用生产坐标投影 helper。 */
    private fun bytePoint(text: String, offset: Int): Pair<Int, Int> {
        val lineStart = text.lastIndexOf('\n', offset - 1) + 1
        return (text.substring(0, offset).count { it == '\n' } + 1) to
            (text.substring(lineStart, offset).toByteArray(Charsets.UTF_8).size + 1)
    }

    private fun assertPosition(position: CjoPositionMetadata?, expected: Pair<Int, Int>) {
        val actual = kotlin.test.assertNotNull(position)
        assertEquals(expected.first, actual.line)
        assertEquals(expected.second, actual.column)
        assertEquals(0u, actual.pkgId)
        assertFalse(actual.ignore)
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
        contract: CjoSourceEncodingContract = input.metadata.sourceEncodingContract,
        serializingCommon: Boolean = true, suppressed: Set<CfirBasedSymbol<*>> = emptySet()): Fixture {
        val module = Module(CfirSession.Kind.Source, CfirCjmpMode.COMMON)
        val psi = createPsiFile("reexport", "package ${input.metadata.fullPackageName}\n") as CjFile
        val file = sourceFile(module, psi, emptyList())
        registerFiles(listOf(file))
        registerCjoSourceFixtureDependencies(module.session, input.provider)
        val metadata = CfirCjoPackageMetadataProducer.produce(listOf(file), familySnapshot = family(input, module.session, suppressed),
            loadedPackages = listOf(input.snapshot), canonicalSources = listOf(graph),
            exportOptions = CjoExportOptions(exportContent = exportContent, serializingCommon = serializingCommon, sourceEncodingContract = contract))
        return load(root, metadata, searchRoots = listOf(input.path.parent) + input.searchRoots)
    }

    /** family 仅使用已绑定对象；enum 的 FuncInfo/VarInfo 不按参数数目改写。 */
    private fun family(input: Fixture, actualSession: CfirSession, suppressed: Set<CfirBasedSymbol<*>> = emptySet()): CfirCjmpFamilySnapshot {
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
            extensions = emptyList(), commonExtendOrder = emptyList(), originalDoNotExport = suppressed,
            classifierDomain = CfirCjmpClassifierDomain.capture(actualSession, input.roots, sourceFiles = emptyList())).evaluate()
    }

    /** ignored factory 保留全部原 FullId 标量；native 空 factory 与 schema missing 是不同 wire。 */
    @Test
    fun valueArraySavedFactorySlotsKeepTheirWireShapeWithoutBinding(@TempDir root: Path) {
        val core = officialCore()
        val slots = listOf(null,
            CjoExpressionTargetMetadata(PackageIndex.INVALID.value),
            CjoExpressionTargetMetadata(PackageIndex.INVALID.value, ""),
            CjoExpressionTargetMetadata(PackageIndex.INVALID.value, "opaque", 9u),
            CjoExpressionTargetMetadata(PackageIndex.PACKAGE_REFERENCE.value, "ignored.package", 3u),
            CjoExpressionTargetMetadata(1, "unused_factory_export_key", 7u),
            CjoExpressionTargetMetadata(PackageIndex.CURRENT.value, index = 2u),
            CjoExpressionTargetMetadata(PackageIndex.CURRENT.value, "", 2u),
            CjoExpressionTargetMetadata(PackageIndex.CURRENT.value, "opaque", 2u))
        for (literal in listOf(false, true)) for (isValueArray in listOf(false, true)) slots.forEachIndexed { ordinal, expected ->
            val kind = if (literal) ExprKind.ArrayLit else ExprKind.ArrayExpr
            val children = if (literal) listOf(integer("17")) else listOf(
                CjoExpressionMetadata(ExprKind.InvalidExpr, type = 1u, operands = listOf(3u),
                    functionArgument = CjoFunctionArgumentExpressionMetadata(false, false)), integer("17"))
            val seed = metadata("reviewarrayslots", listOf(
                CjoPackageDeclaration("arrayValue", DeclKind.VarDecl, type = 2u,
                    attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL), info = CjoVariableInfo(initializer = 1u)),
                CjoPackageDeclaration("init", type = 3u, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL),
                    info = CjoFunctionInfo(body = CjoFunctionBodyInfo(returnType = 1u)))),
                listOf(CjoTypeMetadata(TypeKind.Int64), CjoTypeMetadata(TypeKind.VArray, listOf(1u), semanticInfo = CjoArrayTypeInfoMetadata(1)),
                    functionType(emptyList(), 1u)),
                listOf(CjoExpressionMetadata(kind, type = 2u, operands = listOf(2u),
                    additionalInfo = CjoAdditionalExpressionInfoMetadata.Array(expected, isValueArray))) + children)
                .copy(imports = listOf("unused.peer", "std.core"))
            val directory = root.resolve("$literal-$isValueArray-$ordinal")
            val dependency = directory.resolve("dependency")
            Files.createDirectories(dependency)
            Files.write(dependency.resolve("unused.peer.cjo"), CjoPackageWriter.toByteArray(metadata("unused.peer", emptyList(), emptyList())))
            val first = load(directory.resolve("seed"), seed, core = core, searchRoots = listOf(dependency))
            val firstGraph = first.graph()
            val second = reexport(directory.resolve("second"), first, firstGraph)
            val third = reexport(directory.resolve("third"), second, second.graph())
            for (fixture in listOf(second, third)) {
                val array = assertIs<CjoAdditionalExpressionInfoMetadata.Array>(fixture.metadata.expressions.single { it.kind == kind }.additionalInfo)
                val remapped = when (expected?.pkgId) {
                    PackageIndex.CURRENT.value -> expected.copy(index = (fixture.metadata.declarations.indexOfFirst { it.identifier == "init" } + 1).toUInt())
                    1 -> expected.copy(pkgId = 0)
                    else -> expected
                }
                assertEquals(remapped, array.initializer)
                if (expected?.pkgId == 1) {
                    assertEquals("std.core", fixture.metadata.imports[kotlin.test.assertNotNull(array.initializer).pkgId])
                    assertEquals(listOf("std.core"), fixture.metadata.imports)
                }
                val expressionIndex = fixture.metadata.expressions.indexOfFirst { it.kind == kind }
                val raw = (kotlin.test.assertNotNull(fixture.wire.allExprs(expressionIndex)).info(ArrayInfo()) as ArrayInfo).initFunc
                if (expected == null) assertNull(raw)
                else {
                    kotlin.test.assertNotNull(raw)
                    assertEquals(expected.decl, raw.decl)
                    assertEquals(kotlin.test.assertNotNull(remapped).pkgId, raw.pkgId)
                    assertEquals(remapped.index, raw.index)
                }
                assertEquals(isValueArray, array.isValueArray)
                val graph = fixture.graph()
                val payload = graph.expressions.values.single { it.payload is CfirSourceExpressionPayload.Array || it.payload is CfirSourceExpressionPayload.ArrayLiteral }.payload
                when (payload) {
                    is CfirSourceExpressionPayload.Array -> { assertNull(payload.initializer); kotlin.test.assertNotNull(payload.savedInitializer) }
                    is CfirSourceExpressionPayload.ArrayLiteral -> { assertNull(payload.initializer); kotlin.test.assertNotNull(payload.savedInitializer) }
                    else -> error("Expected actual array payload")
                }
            }
            val native = reexport(directory.resolve("native"), first, firstGraph, contract = CjoSourceEncodingContract.NATIVE_SOURCE)
            val normalized = assertIs<CjoAdditionalExpressionInfoMetadata.Array>(native.metadata.expressions.single { it.kind == kind }.additionalInfo)
            assertEquals(CjoExpressionTargetMetadata(PackageIndex.INVALID.value, ""), normalized.initializer)
            assertEquals(!literal && isValueArray, normalized.isValueArray)
            val nativeGraph = native.graph()
            val nativeRecord = nativeGraph.expressions.values.single { it.payload is CfirSourceExpressionPayload.Array || it.payload is CfirSourceExpressionPayload.ArrayLiteral }
            assertIs<ConeVArrayType>(nativeRecord.type)
            val wire = kotlin.test.assertNotNull(native.wire.allExprs(native.metadata.expressions.indexOfFirst { it.kind == kind }))
            val raw = kotlin.test.assertNotNull((wire.info(ArrayInfo()) as ArrayInfo).initFunc)
            kotlin.test.assertEquals("", raw.decl, "native INVALID 保留存在的空字符串字段")
        }
    }

    private fun assertCurrent(declaration: CfirDeclaration, fixture: Fixture) {
        assertTrue(declaration.isFromCommonPart)
        assertFalse(declaration.exportFacts.isImported)
        assertSame(fixture.module, declaration.moduleData)
        assertSame(declaration, declaration.symbol.cfir)
        assertEquals(fixture.path.toString(), kotlin.test.assertNotNull(declaration.cjoDeclarationOrigin).packagePath)
    }

    private fun sourceFile(module: Module, psi: CjFile, members: List<CfirDeclaration>): CfirFile = buildFile {
        source = psi.toCjPsiSourceElement()
        sourceFile = CjPsiSourceFile(psi)
        sourceFileLinesMapping = CjPsiSourceFileLinesMapping(psi)
        moduleData = module
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Source
        attributes = CfirDeclarationAttributes.EMPTY
        symbol = CfirFileSymbol()
        name = psi.name
        packageDirective = buildPackageDirective { packageFqName = psi.packageFqName; isMacroPackage = false }
        declarations += members
    }

    private fun sourceVariable(module: Module, pkg: String, name: String, type: ConeCangJieType, initializer: CfirExpression?,
        source: org.cangnova.cangjie.source.CjSourceElement?, local: Boolean = false, const: Boolean = true,
        memberOwner: ClassId? = null, receiverType: ConeSimpleCangJieType? = null): CfirFieldVariable = buildFieldVariable {
        this.source = source
        moduleData = module
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Source
        attributes = CfirDeclarationAttributes.EMPTY
        isLocal = local
        isVar = false
        this.name = Name.identifier(name)
        symbol = CfirFieldVariableSymbol(when {
            local -> CallableId(this.name)
            memberOwner != null -> CallableId(memberOwner, this.name)
            else -> CallableId(FqName(pkg), this.name)
        })
        dispatchReceiverType = receiverType
        status = if (local) {
            val modality = org.cangnova.cangjie.descriptors.Modality.FINAL
            CfirDeclarationStatusImpl(Visibilities.Local, modality).apply { isConst = const }
                .resolved(Visibilities.Local, modality)
        } else CfirDeclarationStatusImpl(visibility = Visibilities.Public).apply { isConst = const }
        returnTypeRef = type.toCfirResolvedTypeRef(null)
        this.initializer = initializer
    }

    private fun literal(type: ConeCangJieType, kind: CfirLiteralKind, value: Any): CfirLiteralExpression = buildLiteralExpression {
        coneTypeOrNull = type
        this.kind = kind
        this.value = value
    }

    private fun metadata(pkg: String, declarations: List<CjoPackageDeclaration>, types: List<CjoTypeMetadata>,
        expressions: List<CjoExpressionMetadata> = emptyList(), values: List<CjoCompositeValueMetadata> = emptyList()): CjoPackageMetadata =
        CjoPackageMetadata(pkg, pkg, schemaProfile = CjoSchemaProfile.OFFICIAL_CJMP, allFiles = listOf("lifecycle-schema.cj"),
            declarations = declarations.map { it.copy(attributes = addFlag(it.attributes, Attribute.FROM_COMMON_PART)) },
            types = types, expressions = expressions, values = values, sourceEncodingContract = CjoSourceEncodingContract.TYPED_SCHEMA)

    private fun functionType(parameters: List<UInt>, result: UInt) = CjoTypeMetadata(TypeKind.Func, parameters,
        semanticInfo = CjoFunctionTypeInfoMetadata(result, false, false))
    private fun integer(text: String) = CjoExpressionMetadata(ExprKind.LitConstExpr, type = 1u,
        literal = CjoLiteralExpressionMetadata(text, LitConstKind.Integer))
    private fun string(text: String, type: UInt) = CjoExpressionMetadata(ExprKind.LitConstExpr, type = type,
        literal = CjoLiteralExpressionMetadata(text, LitConstKind.String))
    private fun block(type: UInt, operands: List<UInt>) = CjoExpressionMetadata(ExprKind.Block, type = type, operands = operands,
        additionalInfo = CjoAdditionalExpressionInfoMetadata.Block(List(operands.size) { true } + false))


    /** 多文件 construction 一次发布；复用正式 Source provider，在完整登记后解析成员状态。 */
    private fun registerFiles(files: List<CfirFile>) {
        val session = files.first().moduleData.session
        assertTrue(files.all { it.moduleData.session === session })
        val provider = session.cfirProvider as CfirProviderImpl
        val pre = buildPreMacroRawFiles(session, files.map {
            PreMacroCfirFile(it, annotationMetadata = CfirAnnotationMetadataBuilder())
        })
        val result = MacroConstructionService.successOf(pre, files, MacroExpansionRegistry.EMPTY)
        recordExpandedRawFilesOnce(provider, result.recordableFiles, result.registry)
        session.extendIndexStore.rebuild(files, session.typeResolver)
        files.forEach { completeCjoSourceFixtureClassLikeHeaders(session, it.declarations, useSiteFile = it) }
        files.forEach { resolveCjoSourceFixtureStatuses(session, it.declarations, it) }
    }

    private fun namedFunction(module: Module, pkg: String, name: String,
        syntax: CjNamedFunction, body: CfirBlock): CfirNamedFunction = buildNamedFunction {
        source = syntax.toCjPsiSourceElement()
        moduleData = module
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Source
        attributes = CfirDeclarationAttributes.EMPTY
        isLocal = false
        isMut = false
        this.name = Name.identifier(name)
        symbol = CfirNamedFunctionSymbol(CallableId(FqName(pkg), this.name))
        status = CfirDeclarationStatusImpl(visibility = Visibilities.Public).apply { isConst = true }
        returnTypeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef(null)
        this.body = body
    }

    private fun assertFlag(declaration: PackageFormat.Decl, attribute: Attribute, expected: Boolean) {
        val word = declaration.attributes(attribute.ordinal / 64)
        assertEquals(expected, word and (1uL shl (attribute.ordinal % 64)) != 0uL, attribute.name)
    }

    /** 只读真实存槽，或只改变 schema 种子的坏 Block union tag；不构造 canonical graph。 */
    private class WireSlots : Table() {
        fun at(position: Int, buffer: ByteBuffer): WireSlots = apply { __reset(position, buffer) }
        fun expressionPosition(ordinal: Int): Int = __indirect(__vector(__offset(22)) + ordinal * 4)
        fun expressionInfoTypeSlot(): Int = bb_pos + __offset(18).also { check(it != 0) }
        fun functionBodyPosition(): Int = __indirect(bb_pos + __offset(4).also { check(it != 0) })
    }

    private fun officialCore(): ByteArray = kotlin.test.assertNotNull(javaClass.classLoader.getResourceAsStream(
        "cjo-sdk/windows_x86_64_cjnative/std/std.core.cjo")).use { it.readBytes() }

    private fun stringType(core: ByteArray): CjoTypeMetadata {
        val wire = Package.getRootAsPackage(ByteBuffer.wrap(core))
        val declaration = (0 until wire.allDeclsLength).map { kotlin.test.assertNotNull(wire.allDecls(it)) }
            .single { it.isTopLevel && it.identifier == "String" }
        val type = kotlin.test.assertNotNull(wire.allTypes(declaration.type.toInt() - 1))
        assertEquals(TypeKind.Struct, type.kind)
        return CjoTypeMetadata(type.kind,
            semanticInfo = CjoCompositeTypeInfoMetadata(0u, 0, declarationKey = kotlin.test.assertNotNull(declaration.exportId)))
    }

    private fun flags(vararg attributes: Attribute): List<ULong> =
        attributes.fold(List(2) { 0uL }) { words, flag -> addFlag(words, flag) }

    private fun addFlag(words: List<ULong>, flag: Attribute): List<ULong> = MutableList(maxOf(2, words.size)) {
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
