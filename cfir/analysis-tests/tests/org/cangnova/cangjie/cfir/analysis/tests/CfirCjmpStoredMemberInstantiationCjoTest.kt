package org.cangnova.cangjie.cfir.analysis.tests

import PackageFormat.ClassInfo
import PackageFormat.CompositeTyInfo
import PackageFormat.Decl
import PackageFormat.DeclKind
import PackageFormat.ExprKind
import PackageFormat.FuncInfo
import PackageFormat.InterfaceInfo
import PackageFormat.Package
import PackageFormat.Position
import PackageFormat.ReferenceInfo
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCfirDiagnosticTestSpecBase
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticRelatedSourceLocation
import org.cangnova.cangjie.cfir.diagnostics.SourceElementPositioningStrategies
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpChirOutput
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpMode
import org.cangnova.cangjie.cfir.entrypoint.configuration.configureCjmpCommonPartInputs
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.serialization.cjo.CjoExportSourceSnapshots
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.dependenciesSymbolProvider
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeCangJieTypeProjection
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.addClasspathRoot
import org.cangnova.cangjie.config.classpathRoots
import org.cangnova.cangjie.config.cjoOutputDirectory
import org.cangnova.cangjie.config.diagnosticsCollector
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.config.useLightTree
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.frontend.pipeline.CfirFrontendPipelinePhase
import org.cangnova.cangjie.frontend.pipeline.ConfigurationPipelineArtifact
import org.cangnova.cangjie.frontend.pipeline.DefaultCfirFrontendPipelineArtifact
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.text
import org.cangnova.cangjie.test.CfirParser
import org.cangnova.cangjie.test.builders.TestConfigurationBuilder
import org.cangnova.cangjie.test.config.firHandlersStep
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.frontend.CfirAnalysisHandler
import org.cangnova.cangjie.test.frontend.CfirOutputArtifact
import org.cangnova.cangjie.test.frontend.CfirOutputPartForDependsOnModule
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.compilerConfigurationProvider
import org.cangnova.cangjie.test.services.globalMetadataInfoHandler
import org.cangnova.cangjie.test.services.sourceFileProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path

/** PSI 入口逐项验收原 12 个 SOURCE fixture 及其正式 CJO 写入、加载与实例化检查。 */
class CfirCjmpStoredMemberInstantiationCjoTest : AbstractCfirCjmpStoredMemberInstantiationCjoTest(CfirParser.Psi)

/** LightTree 与 PSI 使用相同输入、官方期望和 CJO handler。 */
class CfirCjmpStoredMemberInstantiationCjoLightTreeTest : AbstractCfirCjmpStoredMemberInstantiationCjoTest(CfirParser.LightTree)

/**
 * 语义来源为 20261113-c3-generic-member-identity-probes 的 official 1.1.3 actual。
 * 两个普通 private 库的跨包 CJO 对照另见 20261113-c3-roundtrip-writer 下 int64/int32 的 result.json。
 * SOURCE 仍由规格测试框架验收原 inline 标记；本类不修改 fixture，也不构造成员表或 CJO 属性。
 */
abstract class AbstractCfirCjmpStoredMemberInstantiationCjoTest(parser: CfirParser) : AbstractCfirDiagnosticTestSpecBase(parser) {
    private lateinit var fixture: StoredMemberCjoFixture

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.firHandlersStep {
            useHandlers({ services: TestServices -> StoredMemberInstantiationCjoHandler(services, parser, fixture) })
        }
    }

    @Test
    fun nominalStringCollision() = runCase(StoredMemberCjoFixture.NOMINAL_STRING)

    @Test
    fun nominalInt64NoCollision() = runCase(StoredMemberCjoFixture.NOMINAL_INT64)

    @Test
    fun extendStringCollision() = runCase(StoredMemberCjoFixture.EXTEND_STRING)

    @Test
    fun extendInt64NoCollision() = runCase(StoredMemberCjoFixture.EXTEND_INT64)

    @Test
    fun constructorMarkerCollision() = runCase(StoredMemberCjoFixture.CONSTRUCTOR_MARKER)

    @Test
    fun constructorExactCounterpartNoCollision() = runCase(StoredMemberCjoFixture.CONSTRUCTOR_EXACT)

    @Test
    fun constructorInt64NoCollision() = runCase(StoredMemberCjoFixture.CONSTRUCTOR_INT64)

    @Test
    fun inheritedCounterpartMarkerCollisionAndDefaultBit() = runCase(StoredMemberCjoFixture.INHERITED_MARKER)

    @Test
    fun privateNominalSourceInt64CollisionAndBinaryImportNoCollision() = runCase(StoredMemberCjoFixture.PRIVATE_INT64)

    @Test
    fun privateNominalSourceInt32AndBinaryImportNoCollision() = runCase(StoredMemberCjoFixture.PRIVATE_INT32)

    @Test
    fun unmatchedNominalStringCollision() = runCase(StoredMemberCjoFixture.UNMATCHED_NOMINAL)

    @Test
    fun unmatchedExtendStringCollision() = runCase(StoredMemberCjoFixture.UNMATCHED_EXTEND)

    @Test
    fun referencedPrivateDefaultHelperIsCollectedOutsideClassBody() = runExportGraphCase(StoredMemberCjoFixture.DEFAULT_HELPER_GRAPH)

    @Test
    fun publicInterfaceDefaultBodyPromotesReferencedPrivateFunction() = runExportGraphCase(StoredMemberCjoFixture.DEFAULT_FUNCTION_GRAPH)

    @Test
    fun privateInstanceLayoutFieldRequiresItsPrivateTypeGraph() = runExportGraphCase(StoredMemberCjoFixture.LAYOUT_GRAPH)

    private fun runCase(case: StoredMemberCjoFixture) {
        fixture = case
        runTest("cfir/analysis-tests/testData/diagnostics2/common-specific/stored-member-instantiation/${case.fileName}")
    }

    /** 新输入逐字取自已成功运行的 official probe；临时 fixture 只接入原有 SOURCE 规格管线。 */
    private fun runExportGraphCase(case: StoredMemberCjoFixture) {
        fixture = case
        val graph = checkNotNull(case.exportGraph)
        val root = Files.createTempDirectory("cjmp-export-graph-fixture-")
        try {
            val file = root.resolve(case.fileName)
            val directives = """
                // LANGUAGE_VERSION: 1.1.3
                // RUN_PIPELINE_TILL: FRONTEND
                // MODULE: producer
                // CJMP_MODE: NONE
                // MAIN_MODULE
                // FILE: producer.cj
            """.trimIndent()
            Files.writeString(file, directives + "\n" + graph.producerSource + "\n", Charsets.UTF_8)
            runTest(file.toAbsolutePath().toString())
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}

/** 只描述已由 official cjc 执行的输入和候选身份，不参与生产匹配或实例化算法。 */
private enum class StoredMemberCjoFixture(
    val fileName: String,
    val candidateOwner: String? = null,
    val candidateName: String? = null,
    val commonCandidateUsesTypeParameter: Boolean = false,
    val ordinaryArgument: PrimitiveTypeKind? = null,
    val exportGraph: StoredMemberExportGraph? = null,
) {
    NOMINAL_STRING("nominalStringCollision.cj", "NominalOverloadBox", "f"),
    NOMINAL_INT64("nominalInt64NoCollision.cj"),
    EXTEND_STRING("extendStringCollision.cj", "extend", "g"),
    EXTEND_INT64("extendInt64NoCollision.cj"),
    CONSTRUCTOR_MARKER("constructorMarkerCollision.cj", "ConstructorBox", "init"),
    CONSTRUCTOR_EXACT("constructorExactCounterpartNoCollision.cj"),
    CONSTRUCTOR_INT64("constructorInt64NoCollision.cj"),
    INHERITED_MARKER("inheritedCounterpartMarkerCollision.cj", "Parent", "f"),
    PRIVATE_INT64("privateNominalInt64Collision.cj", ordinaryArgument = PrimitiveTypeKind.INT64),
    PRIVATE_INT32("privateNominalInt32NoCollision.cj", ordinaryArgument = PrimitiveTypeKind.INT32),
    UNMATCHED_NOMINAL("unmatchedNominalStringCollision.cj", "NominalBox", "f", true),
    UNMATCHED_EXTEND("unmatchedExtendStringCollision.cj", "extend", "g", true),
    DEFAULT_HELPER_GRAPH("privateDefaultHelperGraph.cj", exportGraph = StoredMemberExportGraph.DEFAULT_HELPER),
    DEFAULT_FUNCTION_GRAPH("promotedPrivateFunctionGraph.cj", exportGraph = StoredMemberExportGraph.DEFAULT_FUNCTION),
    LAYOUT_GRAPH("privateLayoutTypeGraph.cj", exportGraph = StoredMemberExportGraph.LAYOUT_TYPE),
}

/** 三个 graph 与 consumer 文本均有 test-authority 下的 cjc/flatc actual，不扩大其他 C3 语义。 */
private enum class StoredMemberExportGraph(val packageName: String, val ownerName: String) {
    DEFAULT_HELPER("c3_export_private_default", "Api"),
    DEFAULT_FUNCTION("c3_export_promoted_function", "Api"),
    LAYOUT_TYPE("c3_export_private_layout", "Layout");

    val producerSource: String
        get() = when (this) {
            DEFAULT_HELPER -> """
                package c3_export_private_default

                public class Api {
                    public init() {}
                    public func value(x!: Int64 = 7): Int64 { x }
                }
            """.trimIndent()
            DEFAULT_FUNCTION -> """
                package c3_export_promoted_function

                private func helper(): Int64 { 7 }

                public interface Api {
                    func value(): Int64 { helper() }
                }
            """.trimIndent()
            LAYOUT_TYPE -> """
                package c3_export_private_layout

                private class Hidden {
                    public init() {}
                }

                public class Layout {
                    private var hidden: Hidden = Hidden()
                    public init() {}
                }
            """.trimIndent()
        }

    val consumerSource: String
        get() = when (this) {
            DEFAULT_HELPER -> """
                package c3_export_private_default_client

                import c3_export_private_default.Api

                public func use(value: Api): Int64 { value.value() }
            """.trimIndent()
            DEFAULT_FUNCTION -> """
                package c3_export_promoted_function_client

                import c3_export_promoted_function.Api

                public class Impl <: Api {}
                public func use(value: Api): Int64 { value.value() }
            """.trimIndent()
            LAYOUT_TYPE -> """
                package c3_export_private_layout_client

                import c3_export_private_layout.Layout

                public func use(value: Layout): Unit {}
            """.trimIndent()
        }
}

/**
 * 编译骨架原样沿用 DeclarationPreCheckCjoHandler 的新配置、UTF-8 源根和 executePhase 路径。
 * 原 handler 是私有类型，因此此独立文件不修改其可见性；这里只扩展期望归属与普通库 transport。
 */
@OptIn(CompilerConfiguration.Internals::class)
private class StoredMemberInstantiationCjoHandler(
    testServices: TestServices,
    private val parser: CfirParser,
    private val fixture: StoredMemberCjoFixture,
) : CfirAnalysisHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer> = emptyList()
    private var checkedModules = 0

    override fun processModule(module: TestModule, info: CfirOutputArtifact) {
        val mode = module.directives[CfirDiagnosticsDirectives.CJMP_MODE].single()
        if (mode == CfirCjmpMode.COMMON) return
        assertEquals(0, checkedModules, "每个 fixture 必须只完成一次实际 binary 对照")
        val part = info.partsForDependsOnModules.single { it.module === module }
        val root = Files.createTempDirectory("cjmp-stored-member-cjo-")
        val disposable = Disposer.newDisposable("cjmp-stored-member-cjo")
        try {
            if (fixture.exportGraph != null) {
                assertEquals(CfirCjmpMode.NONE, mode)
                checkExportGraph(part, root, disposable, checkNotNull(fixture.exportGraph))
            } else if (fixture.ordinaryArgument != null) {
                assertEquals(CfirCjmpMode.NONE, mode)
                checkOrdinaryPrivateLibrary(part, root, disposable)
            } else {
                assertEquals(CfirCjmpMode.SPECIFIC, mode)
                val common = info.partsForDependsOnModules.single {
                    it.module.directives[CfirDiagnosticsDirectives.CJMP_MODE].single() == CfirCjmpMode.COMMON
                }
                checkCommonPart(common, part, root, disposable)
            }
            checkedModules++
        } finally {
            Disposer.dispose(disposable)
            root.toFile().deleteRecursively()
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        assertEquals(1, checkedModules, "原 12 fixture 都必须执行 SOURCE 与真实 CJO，不跳过 NONE/private 用例")
    }

    private fun checkCommonPart(
        common: CfirOutputPartForDependsOnModule,
        specific: CfirOutputPartForDependsOnModule,
        root: Path,
        disposable: Disposable,
    ) {
        assertNotSame(common.session, specific.session)
        val commonResult = compile(common, root.resolve("common"), disposable) {
            cjmpChirOutput = true
            cjoOutputDirectory = root.resolve("out").toString()
        }
        assertDiagnostics(commonResult, expectedDiagnostics(common))
        val produced = checkNotNull(commonResult.artifact) { "common 必须完成正式 frontend 并写出 CJO" }
        assertFalse(commonResult.messages.hasErrors(), commonResult.messages.entries.toString())
        val producedFiles = produced.frontendOutput.outputs.single().fir
        val packageName = producedFiles.map { it.packageDirective.packageFqName }.distinct().single()
        val cjo = root.resolve("out").resolve(CjoConstants.packageNameToPath(packageName.asString()))
        assertTrue(Files.isRegularFile(cjo), "必须由正式 MetadataProducer/PackageWriter 写出")
        val bytes = Files.readAllBytes(cjo)
        val wire = WrittenCjo(Package.getRootAsPackage(ByteBuffer.wrap(bytes)), commonResult.sources.single())

        val specificResult = compile(specific, root.resolve("specific"), disposable) {
            configureCjmpCommonPartInputs(listOf(cjo.toString()), emptyList())
        }
        assertDiagnostics(specificResult, expectedDiagnostics(specific))
        // 本组错误都锚在消费端 SOURCE；直接 frontend phase 应保留产物，加载/driver 早退不是反例成功。
        val consumed = checkNotNull(specificResult.artifact) { "CJO 消费不能因 parser、driver 或 binary 错误提前退出" }
        val consumerOutput = consumed.frontendOutput.outputs.single()
        assertNotSame(produced.frontendOutput.outputs.single().session, consumerOutput.session)
        assertEquals(specificResult.sources.size, consumed.sourceFiles.size, "消费配置只能加入 specific 源根")
        assertLoadedCommonDeclarations(producedFiles, consumerOutput.session, cjo, wire)
        if (fixture == StoredMemberCjoFixture.INHERITED_MARKER) {
            val parent = consumerOutput.fir.flatMap { it.declarations }.filterIsInstance<CfirInterface>().single()
            assertTrue(parent.declarations.filterIsInstance<CfirNamedFunction>().single().status.isDefault)
        }
        if (fixture.candidateOwner != null) {
            assertCandidateIdentity(producedFiles, consumerOutput.fir, specificResult, wire)
        }
        assertArrayEquals(bytes, Files.readAllBytes(cjo), "消费和诊断不得改写原 CJO")
    }

    private fun checkOrdinaryPrivateLibrary(
        part: CfirOutputPartForDependsOnModule,
        root: Path,
        disposable: Disposable,
    ) {
        val producer = compileSources(part, listOf(InputSource("source.cj", PRIVATE_LIBRARY_SOURCE)), root.resolve("library"), disposable) {
            cjoOutputDirectory = root.resolve("out").toString()
        }
        assertDiagnostics(producer, emptyList())
        val produced = checkNotNull(producer.artifact)
        val sourceOwner = produced.frontendOutput.outputs.single().fir.single()
            .declarations.filterIsInstance<CfirClass>().single()
        val sourceSelects = sourceOwner.declarations.filterIsInstance<CfirNamedFunction>()
            .filter { it.name.asString() == "select" }
        assertEquals(2, sourceSelects.size, "live CFIR 必须保留原来的两个 private select，不能在 SOURCE 侧删成员")
        assertTrue(sourceSelects.all { it.status.visibility == Visibilities.Private })
        val sourceParameter = sourceOwner.typeParameters.single().symbol
        assertEquals("T", sourceParameter.name.asString())
        val genericSelect = sourceSelects.single {
            it.valueParameters.single().returnTypeRef.coneTypeOrNull is ConeTypeParameterType
        }
        assertSame(
            sourceParameter,
            (genericSelect.valueParameters.single().returnTypeRef.coneTypeOrNull as ConeTypeParameterType)
                .lookupTag.typeParameterSymbol,
        )
        val fixedSelect = sourceSelects.single { it !== genericSelect }
        assertEquals(
            PrimitiveTypeKind.INT64,
            (fixedSelect.valueParameters.single().returnTypeRef.coneTypeOrNull as ConePrimitiveType).kind,
        )
        val cjo = root.resolve("out").resolve(CjoConstants.packageNameToPath("c3_member_identity"))
        assertTrue(Files.isRegularFile(cjo))
        val bytes = Files.readAllBytes(cjo)
        assertOrdinaryPrivateWire(Package.getRootAsPackage(ByteBuffer.wrap(bytes)))
        val consumerSource = when (checkNotNull(fixture.ordinaryArgument)) {
            PrimitiveTypeKind.INT64 -> PRIVATE_INT64_CONSUMER
            PrimitiveTypeKind.INT32 -> PRIVATE_INT32_CONSUMER
            else -> error("没有 official actual 的普通 private binary 参数")
        }
        val consumer = compileSources(part, listOf(InputSource("source.cj", consumerSource)), root.resolve("consumer"), disposable) {
            addClasspathRoot(root.resolve("out").toString())
        }
        // official 1.1.3：SOURCE Int64 有 ambiguity，跨包 ordinary CJO import 的两个 consumer 均成功。
        assertDiagnostics(consumer, emptyList())
        assertFalse(consumer.messages.hasErrors(), consumer.messages.entries.toString())
        val output = checkNotNull(consumer.artifact).frontendOutput.outputs.single()
        val classId = ClassId(FqName("c3_member_identity"), Name.identifier("PrivateGeneric"))
        val loaded = checkNotNull(output.session.dependenciesSymbolProvider.getClassLikeSymbolByClassId(classId)).cfir
        assertLibraryOrigin(sourceOwner, loaded, cjo)
        assertEquals(1, loaded.typeParameters.size)
        val function = output.fir.single().declarations.filterIsInstance<CfirNamedFunction>().single()
        val parameterType = function.valueParameters.single().returnTypeRef.coneTypeOrNull as ConeClassLikeType
        assertEquals(classId, parameterType.lookupTag.classId, "必须实际消费刚加载的 PrivateGeneric")
        val argument = (parameterType.typeArguments.single() as ConeCangJieTypeProjection).type as ConePrimitiveType
        assertEquals(fixture.ordinaryArgument, argument.kind)
        assertLoadedSnapshot(output.session, cjo)
        assertArrayEquals(bytes, Files.readAllBytes(cjo))
    }

    /** native actual 的 allDecls 与 ClassInfo.body 分别验收，consumer 过滤不能掩盖 producer 错误。 */
    private fun assertOrdinaryPrivateWire(pkg: Package) {
        val declarations = (0 until pkg.allDeclsLength).map { checkNotNull(pkg.allDecls(it)) }
        assertEquals(3, declarations.size, "native ordinary CJO 只收录 PrivateGeneric、T 和 init")
        assertEquals(
            setOf(DeclKind.ClassDecl to "PrivateGeneric", DeclKind.GenericParamDecl to "T", DeclKind.FuncDecl to "init"),
            declarations.map { it.kind to checkNotNull(it.identifier) }.toSet(),
        )
        val owner = declarations.single { it.kind == DeclKind.ClassDecl }
        val generic = checkNotNull(owner.generic)
        assertEquals(1, generic.typeParametersLength)
        val parameterReference = generic.typeParameters(0)
        check(parameterReference > 0u && parameterReference.toLong() <= declarations.size)
        val parameter = declarations[parameterReference.toInt() - 1]
        assertEquals(DeclKind.GenericParamDecl, parameter.kind)
        assertEquals("T", parameter.identifier)
        val classInfo = checkNotNull(owner.info(ClassInfo())) as ClassInfo
        assertEquals(1, classInfo.bodyLength, "native ClassInfo.body 只有 init，两个 private select 不属于 body")
        val memberReference = classInfo.body(0)
        check(memberReference > 0u && memberReference.toLong() <= declarations.size)
        val member = declarations[memberReference.toInt() - 1]
        assertEquals(DeclKind.FuncDecl, member.kind)
        assertEquals("init", member.identifier)
    }

    /** collection、body、类型/表达式引用分别验收；全部来自正式 SOURCE→writer→loader 管线。 */
    private fun checkExportGraph(
        part: CfirOutputPartForDependsOnModule,
        root: Path,
        disposable: Disposable,
        graph: StoredMemberExportGraph,
    ) {
        val producer = compile(part, root.resolve("producer"), disposable) {
            cjoOutputDirectory = root.resolve("out").toString()
        }
        assertDiagnostics(producer, expectedDiagnostics(part))
        val producedFiles = checkNotNull(producer.artifact).frontendOutput.outputs.single().fir
        val sourceDeclarations = producedFiles.single().declarations
        val sourceOwner = sourceDeclarations.filterIsInstance<CfirClassLikeDeclaration>()
            .single { it.name.asString() == graph.ownerName }
        val cjo = root.resolve("out").resolve(CjoConstants.packageNameToPath(graph.packageName))
        assertTrue(Files.isRegularFile(cjo), "结构断言必须读取正式 writer 的实际 CJO")
        val bytes = Files.readAllBytes(cjo)
        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(bytes))
        when (graph) {
            StoredMemberExportGraph.DEFAULT_HELPER -> assertPrivateDefaultHelperGraph(pkg, sourceOwner)
            StoredMemberExportGraph.DEFAULT_FUNCTION -> assertPromotedPrivateFunctionGraph(pkg, sourceDeclarations, sourceOwner)
            StoredMemberExportGraph.LAYOUT_TYPE -> assertPrivateLayoutGraph(pkg, sourceDeclarations, sourceOwner)
        }
        val consumer = compileSources(
            part,
            listOf(InputSource("consumer.cj", graph.consumerSource)),
            root.resolve("consumer"),
            disposable,
        ) { addClasspathRoot(root.resolve("out").toString()) }
        assertDiagnostics(consumer, emptyList())
        assertFalse(consumer.messages.hasErrors(), consumer.messages.entries.toString())
        val output = checkNotNull(consumer.artifact).frontendOutput.outputs.single()
        val ownerId = ClassId(FqName(graph.packageName), Name.identifier(graph.ownerName))
        val loaded = checkNotNull(output.session.dependenciesSymbolProvider.getClassLikeSymbolByClassId(ownerId)).cfir
        assertLibraryOrigin(sourceOwner, loaded, cjo)
        if (graph == StoredMemberExportGraph.DEFAULT_FUNCTION) {
            assertTrue(loaded.declarations.filterIsInstance<CfirNamedFunction>().single().status.isDefault)
        }
        assertLoadedSnapshot(output.session, cjo)
        assertArrayEquals(bytes, Files.readAllBytes(cjo), "consumer 不能回写 producer 的 graph")
    }

    /** 参数表 desugars 是真实引用边，private/HAS_INITIAL helper 必须入表但不得混入 ClassInfo.body。 */
    private fun assertPrivateDefaultHelperGraph(pkg: Package, sourceOwner: CfirClassLikeDeclaration) {
        val sourceFunction = sourceOwner.declarations.filterIsInstance<CfirNamedFunction>().single()
        assertFalse(sourceFunction.status.isDefault)
        assertNotNull(sourceFunction.valueParameters.single().defaultValue)
        val declarations = wireDeclarations(pkg)
        assertEquals(5, declarations.size)
        assertEquals(
            setOf("Api", "init", "value", "x", "x.0"),
            declarations.map { checkNotNull(it.identifier) }.toSet(),
        )
        val owner = declarations.single { it.kind == DeclKind.ClassDecl && it.identifier == "Api" }
        val body = classBody(pkg, owner)
        assertEquals(listOf("init", "value"), body.map { checkNotNull(it.identifier) })
        val function = body.single { it.kind == DeclKind.FuncDecl && it.identifier == "value" }
        assertFalse(function.has(Attribute.DEFAULT))
        val parameters = checkNotNull((checkNotNull(function.info(FuncInfo())) as FuncInfo).funcBody)
        assertEquals(1, parameters.paramListsLength)
        val list = checkNotNull(parameters.paramLists(0))
        assertEquals(1, list.paramsLength)
        assertEquals(1, list.desugarsLength)
        val parameter = wireDeclaration(pkg, list.params(0))
        assertEquals(DeclKind.FuncParam, parameter.kind)
        assertEquals("x", parameter.identifier)
        val helperReference = list.desugars(0)
        val helper = wireDeclaration(pkg, helperReference)
        assertEquals(DeclKind.FuncDecl, helper.kind)
        assertEquals("x.0", helper.identifier)
        assertTrue(helper.has(Attribute.PRIVATE))
        assertTrue(helper.has(Attribute.HAS_INITIAL))
        assertFalse(helper.isTopLevel)
        val ownerInfo = checkNotNull(owner.info(ClassInfo())) as ClassInfo
        assertFalse((0 until ownerInfo.bodyLength).any { ownerInfo.body(it) == helperReference })
        assertEquals(0u, parameters.body)
        assertEquals(0u, checkNotNull((checkNotNull(helper.info(FuncInfo())) as FuncInfo).funcBody).body)
        assertEquals(1, pkg.allExprsLength, "literal default assignment 必须独立保存")
    }

    /** DEFAULT 的实际 body 引用使普通 private helper 入图，不能改 PRIVATE 或额外保存其普通 body。 */
    private fun assertPromotedPrivateFunctionGraph(
        pkg: Package,
        sourceDeclarations: List<CfirDeclaration>,
        sourceOwner: CfirClassLikeDeclaration,
    ) {
        val sourceHelper = sourceDeclarations.filterIsInstance<CfirNamedFunction>().single()
        assertEquals(Visibilities.Private, sourceHelper.status.visibility)
        assertFalse(sourceHelper.status.isDefault)
        assertTrue(sourceOwner.declarations.filterIsInstance<CfirNamedFunction>().single().status.isDefault)
        val declarations = wireDeclarations(pkg)
        assertEquals(3, declarations.size)
        assertEquals(setOf("Api", "value", "helper"), declarations.map { checkNotNull(it.identifier) }.toSet())
        val owner = declarations.single { it.kind == DeclKind.InterfaceDecl }
        val info = checkNotNull(owner.info(InterfaceInfo())) as InterfaceInfo
        assertEquals(1, info.bodyLength)
        val function = wireDeclaration(pkg, info.body(0))
        assertEquals("value", function.identifier)
        assertTrue(function.has(Attribute.DEFAULT))
        val functionBody = checkNotNull((checkNotNull(function.info(FuncInfo())) as FuncInfo).funcBody).body
        assertTrue(functionBody > 0u)
        val helper = declarations.single { it.kind == DeclKind.FuncDecl && it.identifier == "helper" }
        assertTrue(helper.has(Attribute.PRIVATE))
        assertFalse(helper.has(Attribute.DEFAULT))
        assertTrue(helper.isTopLevel)
        assertEquals(0u, checkNotNull((checkNotNull(helper.info(FuncInfo())) as FuncInfo).funcBody).body)
        assertEquals(4, pkg.allExprsLength)
        val reference = (0 until pkg.allExprsLength).map { checkNotNull(pkg.allExprs(it)) }
            .single { it.kind == ExprKind.RefExpr }
        val target = checkNotNull((checkNotNull(reference.info(ReferenceInfo())) as ReferenceInfo).target)
        assertEquals(-2, target.pkgId, "FullId 的当前包不是 Position.pkgId 的零")
        assertEquals(declarations.indexOf(helper).toUInt() + 1u, target.index)
    }

    /** 外部可见 nominal 的 private 实例布局字段，必须保留字段、真实 private type 与相应类型引用。 */
    private fun assertPrivateLayoutGraph(
        pkg: Package,
        sourceDeclarations: List<CfirDeclaration>,
        sourceOwner: CfirClassLikeDeclaration,
    ) {
        val sourceHidden = sourceDeclarations.filterIsInstance<CfirClass>().single { it.name.asString() == "Hidden" }
        assertEquals(Visibilities.Private, sourceHidden.status.visibility)
        val sourceField = sourceOwner.declarations.filterIsInstance<CfirFieldVariable>().single()
        assertEquals(Visibilities.Private, sourceField.status.visibility)
        assertFalse(sourceField.status.isStatic)
        val sourceType = sourceField.returnTypeRef.coneTypeOrNull as ConeClassifierType
        assertEquals(sourceHidden.symbol.classId, sourceType.lookupTag.classId)
        val declarations = wireDeclarations(pkg)
        assertEquals(5, declarations.size)
        val hidden = declarations.single { it.kind == DeclKind.ClassDecl && it.identifier == "Hidden" }
        val layout = declarations.single { it.kind == DeclKind.ClassDecl && it.identifier == "Layout" }
        assertTrue(hidden.has(Attribute.PRIVATE))
        val hiddenBody = classBody(pkg, hidden)
        val layoutBody = classBody(pkg, layout)
        assertEquals(listOf("init"), hiddenBody.map { checkNotNull(it.identifier) })
        assertEquals(listOf("hidden", "init"), layoutBody.map { checkNotNull(it.identifier) })
        val field = layoutBody.single { it.kind == DeclKind.VarDecl }
        assertTrue(field.has(Attribute.PRIVATE))
        assertFalse(field.has(Attribute.STATIC))
        val type = checkNotNull(pkg.allTypes(field.type.toInt() - 1))
        val target = checkNotNull((checkNotNull(type.info(CompositeTyInfo())) as CompositeTyInfo).declPtr)
        assertEquals(-2, target.pkgId)
        assertEquals(declarations.indexOf(hidden).toUInt() + 1u, target.index)
        assertEquals(hidden.type, field.type)
        assertEquals(0, pkg.allExprsLength)
    }

    private fun wireDeclarations(pkg: Package): List<Decl> =
        (0 until pkg.allDeclsLength).map { checkNotNull(pkg.allDecls(it)) }

    private fun wireDeclaration(pkg: Package, reference: UInt): Decl {
        check(reference > 0u && reference.toLong() <= pkg.allDeclsLength)
        return checkNotNull(pkg.allDecls(reference.toInt() - 1))
    }

    private fun classBody(pkg: Package, owner: Decl): List<Decl> {
        val info = checkNotNull(owner.info(ClassInfo())) as ClassInfo
        return (0 until info.bodyLength).map { wireDeclaration(pkg, info.body(it)) }
    }

    /** 只观察 writer/loader 已产出的物理树、属性和类型参数符号；不构造继承或实例化成员表。 */
    private fun assertLoadedCommonDeclarations(files: List<CfirFile>, consumer: CfirSession, cjo: Path, wire: WrittenCjo) {
        for (source in files.flatMap { it.declarations }) {
            when (source) {
                is CfirClassLikeDeclaration -> {
                    val loaded = checkNotNull(consumer.dependenciesSymbolProvider.getClassLikeSymbolByClassId(source.symbol.classId)).cfir
                    assertLibraryOrigin(source, loaded, cjo)
                    assertEquals(source.typeParameters.size, loaded.typeParameters.size)
                    val serializedOwner = wire.declaration(source)
                    if (source.typeParameters.isEmpty()) {
                        assertFalse(serializedOwner.has(Attribute.GENERIC))
                    } else {
                        assertEquals(source.typeParameters.size, checkNotNull(serializedOwner.generic).typeParametersLength)
                    }
                    assertEquals(source.status.isCommon, serializedOwner.has(Attribute.COMMON))
                    assertTrue(serializedOwner.has(Attribute.FROM_COMMON_PART))
                    assertTrue(loaded.isFromCommonPart)
                    assertPhysicalFunctions(source.declarations, loaded.declarations, source.typeParameters, loaded.typeParameters, wire, cjo) { member ->
                        assertSame(loaded.symbol, member.moduleData.session.cfirProvider.getContainingClass(member.symbol))
                    }
                }
                is CfirExtend -> {
                    val index = wire.index(source)
                    val loaded = consumer.extendProvider.getExtendsInPackage(files.single().packageDirective.packageFqName).single {
                        it.cjoDeclarationOrigin == CfirCjoDeclarationOrigin(cjo.toAbsolutePath().normalize().toString(), index,
                            isTopLevel = files.single().declarations.any { declaration -> declaration === source })
                    }
                    assertLibraryOrigin(source, loaded, cjo)
                    assertEquals(source.typeParameters.size, loaded.typeParameters.size)
                    assertPhysicalFunctions(source.declarations, loaded.declarations, source.typeParameters, loaded.typeParameters, wire, cjo) { member ->
                        assertSame(loaded.symbol, checkNotNull(member.moduleData.session.extendProvider.getContainingExtend(member.symbol)).symbol)
                    }
                }
                else -> error("本轮 common fixture 的物理根必须是 nominal 或 extend")
            }
        }
        assertLoadedSnapshot(consumer, cjo)
    }

    private fun assertPhysicalFunctions(
        sourceMembers: List<CfirDeclaration>,
        loadedMembers: List<CfirDeclaration>,
        sourceParameters: List<CfirTypeParameterRef>,
        loadedParameters: List<CfirTypeParameterRef>,
        wire: WrittenCjo,
        cjo: Path,
        inspectOwner: (CfirFunction) -> Unit,
    ) {
        for (source in sourceMembers.filterIsInstance<CfirFunction>().filter { it.origin == CfirDeclarationOrigin.Source }) {
            val index = wire.index(source)
            val loaded = loadedMembers.filterIsInstance<CfirFunction>().single { it.cjoDeclarationOrigin?.declarationIndex == index }
            assertLibraryOrigin(source, loaded, cjo)
            inspectOwner(loaded)
            val serialized = wire.declaration(source)
            assertEquals(source.status.isCommon, serialized.has(Attribute.COMMON))
            assertEquals(source.status.isDefault, serialized.has(Attribute.DEFAULT), "DEFAULT 必须由真实 writer 原样保存")
            assertEquals(serialized.has(Attribute.DEFAULT), loaded.status.isDefault, "DEFAULT 必须由真实 loader 恢复")
            assertEquals(source.status.isCommon, loaded.status.isCommon)
            assertEquals(source.status.isStatic, loaded.status.isStatic)
            assertTrue(loaded.isFromCommonPart)
            if (source is CfirNamedFunction) {
                val expectedDefault = fixture == StoredMemberCjoFixture.INHERITED_MARKER
                assertEquals(expectedDefault, source.status.isDefault, "DEFAULT 属于 interface 成员，独立于 common 函数体")
                assertEquals(expectedDefault, serialized.has(Attribute.DEFAULT))
                assertEquals(expectedDefault, loaded.status.isDefault)
            }
            assertEquals(source.valueParameters.size, loaded.valueParameters.size)
            source.valueParameters.zip(loaded.valueParameters).forEach { (original, restored) ->
                assertEquals(original.name, restored.name)
                assertEquals(original.isNamed, restored.isNamed)
                assertParameterType(
                    checkNotNull(original.returnTypeRef.coneTypeOrNull),
                    checkNotNull(restored.returnTypeRef.coneTypeOrNull),
                    sourceParameters,
                    loadedParameters,
                )
            }
        }
    }

    private fun assertParameterType(
        original: ConeCangJieType,
        restored: ConeCangJieType,
        originalParameters: List<CfirTypeParameterRef>,
        restoredParameters: List<CfirTypeParameterRef>,
    ) {
        when (original) {
            is ConeTypeParameterType -> {
                val index = originalParameters.indexOfFirst { it.symbol === original.lookupTag.typeParameterSymbol }
                check(index >= 0) { "fixture 参数必须来自实际物理 generic owner" }
                assertTrue(restored is ConeTypeParameterType)
                assertSame(restoredParameters[index].symbol, (restored as ConeTypeParameterType).lookupTag.typeParameterSymbol)
            }
            is ConePrimitiveType -> assertEquals(original.kind, (restored as ConePrimitiveType).kind)
            is ConeClassifierType -> {
                assertTrue(original.typeArguments.isEmpty(), "固定 String/Marker 参数没有额外实例化参数")
                assertEquals(original::class, restored::class, "String 的 struct 与 Marker 的 class 类型载体必须原样往返")
                assertEquals(original.lookupTag.classId, (restored as ConeClassifierType).lookupTag.classId)
                assertTrue(restored.typeArguments.isEmpty())
            }
            else -> error("fixture 出现了没有 official actual 的参数类型：$original")
        }
    }

    private fun assertLibraryOrigin(source: CfirDeclaration, loaded: CfirDeclaration, cjo: Path) {
        assertEquals(CfirDeclarationOrigin.Library, loaded.origin)
        assertNotSame(source.symbol, loaded.symbol)
        assertNotSame(source.moduleData.session, loaded.moduleData.session)
        assertEquals(cjo.toAbsolutePath().normalize(), Path.of(checkNotNull(loaded.cjoDeclarationOrigin).packagePath).toAbsolutePath().normalize())
    }

    private fun assertLoadedSnapshot(consumer: CfirSession, cjo: Path) {
        val snapshot = checkNotNull(CjoExportSourceSnapshots.loadedPackage(consumer.dependenciesSymbolProvider, cjo))
        assertEquals(cjo.toAbsolutePath().normalize(), snapshot.sourcePath.toAbsolutePath().normalize())
        assertTrue(consumer.dependenciesSymbolProvider.getCjmpLoadDiagnostics(FqName(snapshot.header.fullPkgName), consumer).isEmpty())
    }

    /** native actual 的两个 peer 分别是固定/泛型 common 原声明与真实 specific 原声明。 */
    private fun assertCandidateIdentity(commonFiles: List<CfirFile>, specificFiles: List<CfirFile>, result: Compilation, wire: WrittenCjo) {
        fun functions(files: List<CfirFile>): List<CfirFunction> {
            val owner = files.flatMap { it.declarations }.single {
                when (it) {
                    is CfirExtend -> fixture.candidateOwner == "extend"
                    is CfirClassLikeDeclaration -> it.name.asString() == fixture.candidateOwner
                    else -> false
                }
            }
            val members = when (owner) {
                is CfirExtend -> owner.declarations
                is CfirClassLikeDeclaration -> owner.declarations
                else -> error("候选必须属于已声明的 physical owner")
            }
            return members.filterIsInstance<CfirFunction>().filter {
                if (fixture.candidateName == "init") it is CfirConstructor
                else it is CfirNamedFunction && it.name.asString() == fixture.candidateName
            }
        }
        val original = functions(commonFiles).single {
            (it.valueParameters.single().returnTypeRef.coneTypeOrNull is ConeTypeParameterType) == fixture.commonCandidateUsesTypeParameter
        }
        val specific = functions(specificFiles).single()
        val diagnostic = result.diagnostics.diagnostics.single { it.factoryName == "CFIR_GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS" }
        assertEquals(2, diagnostic.relatedInformation.size, "native actual 保留两个不同的 declaration peer")
        assertTrue(diagnostic.relatedInformation.all { it.message == "found candidate" })
        val commonNote = diagnostic.relatedInformation.single { it.element is CfirCjoDeclarationSourceElement }
        val actualLocation = checkNotNull(commonNote.sourceLocation)
        val expectedLocation = wire.identifier(original)
        // wire 与公开诊断采用各自的路径显示分隔符；文件身份及 UTF-8 行列分别精确核对。
        assertEquals(Path.of(expectedLocation.filePath).toAbsolutePath().normalize(), Path.of(actualLocation.filePath).toAbsolutePath().normalize())
        assertEquals(expectedLocation.line, actualLocation.line, "common peer 必须定位到真实 wire identifierPos 行")
        assertEquals(expectedLocation.column, actualLocation.column, "common peer 必须定位到真实 wire identifierPos UTF-8 列")
        val commonSource = commonNote.element as CfirCjoDeclarationSourceElement
        val declarationPosition = checkNotNull(commonSource.declarationPosition)
        assertEquals(wire.begin(original).line, checkNotNull(declarationPosition.declarationLocation).line)
        assertEquals(wire.begin(original).column, checkNotNull(declarationPosition.declarationLocation).column)
        val specificNote = diagnostic.relatedInformation.single { it.element !is CfirCjoDeclarationSourceElement }
        assertNull(specificNote.sourceLocation)
        val range = checkNotNull(SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME.markOrNull(checkNotNull(specific.source))).single()
        val nameSource = checkNotNull(specificNote.element) as CjSourceElement
        assertEquals(range.startOffset, nameSource.startOffset)
        assertEquals(range.endOffset, nameSource.endOffset)
        assertEquals(fixture.candidateName, nameSource.text.toString())
    }

    private fun expectedDiagnostics(part: CfirOutputPartForDependsOnModule): List<ExpectedDiagnostic> {
        val file = part.firFilesByTestFile.keys.filter { !it.isAdditional }.single()
        return testServices.globalMetadataInfoHandler.getExistingMetaInfosForFile(file).map { ExpectedDiagnostic(it.tag, it.start, it.end) }
    }

    private fun assertDiagnostics(result: Compilation, expected: List<ExpectedDiagnostic>) {
        val actual = result.diagnostics.diagnostics.map { ExpectedDiagnostic(it.factoryName.removePrefix("CFIR_"), it.firstRange.startOffset, it.firstRange.endOffset) }
        val order = compareBy<ExpectedDiagnostic>({ it.tag }, { it.start }, { it.end })
        assertEquals(expected.sortedWith(order), actual.sortedWith(order), "binary 的类型范围、完整声明、public token 期望必须保持原样")
        assertTrue(
            result.messages.entries.filter { it.first.isError }.all { (_, text) ->
                result.diagnostics.diagnostics.any { it.renderMessage() == text }
            },
            "driver/加载/写出错误不能冒充语义反例：${result.messages.entries}",
        )
    }

    private fun compile(
        part: CfirOutputPartForDependsOnModule,
        sourceRoot: Path,
        disposable: Disposable,
        configure: CompilerConfiguration.() -> Unit,
    ): Compilation {
        val inputs = part.firFilesByTestFile.keys.filter { !it.isAdditional }.map {
            InputSource(it.relativePath, testServices.sourceFileProvider.getContentOfSourceFile(it))
        }
        return compileSources(part, inputs, sourceRoot, disposable, configure)
    }

    private fun compileSources(
        part: CfirOutputPartForDependsOnModule,
        inputs: List<InputSource>,
        sourceRoot: Path,
        disposable: Disposable,
        configure: CompilerConfiguration.() -> Unit,
    ): Compilation {
        val diagnostics = DiagnosticsCollectorImpl()
        val messages = RoundTripMessageCollector()
        val sources = mutableListOf<WrittenSource>()
        val moduleConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(part.module)
        val configuration = CompilerConfiguration().apply {
            moduleName = part.module.name
            languageVersionSettings = moduleConfiguration.languageVersionSettings
            useLightTree = parser == CfirParser.LightTree
            cjmpMode = part.module.directives[CfirDiagnosticsDirectives.CJMP_MODE].single()
            diagnosticsCollector = diagnostics
            messageCollector = messages
            // 正式环境配置器已按模块依赖、WITH_STDLIB 和 import 指令解析二进制根。
            // 这里只接入这些实际依赖；源根仍由本次输入独立写出，不复用 SOURCE provider。
            moduleConfiguration.classpathRoots.forEach { addClasspathRoot(it.path) }
            for (input in inputs) {
                val path = sourceRoot.resolve(input.relativePath).toAbsolutePath().normalize()
                check(path.startsWith(sourceRoot.toAbsolutePath().normalize()))
                Files.createDirectories(path.parent)
                Files.writeString(path, input.content, Charsets.UTF_8)
                sources += WrittenSource(path, input.content)
                addCangJieSourceRoot(path.toString())
            }
            configure()
        }
        return Compilation(
            CfirFrontendPipelinePhase.executePhase(ConfigurationPipelineArtifact(configuration, disposable)),
            diagnostics,
            messages,
            sources,
        )
    }

    private data class ExpectedDiagnostic(val tag: String, val start: Int, val end: Int)
    private data class InputSource(val relativePath: String, val content: String)
    private data class WrittenSource(val path: Path, val content: String)
    private data class Compilation(
        val artifact: DefaultCfirFrontendPipelineArtifact?,
        val diagnostics: DiagnosticsCollectorImpl,
        val messages: RoundTripMessageCollector,
        val sources: List<WrittenSource>,
    )

    /** 以原声明的格式 kind、原生 identifier 和完整真实源范围唯一定位 wire 身份。 */
    private class WrittenCjo(private val pkg: Package, private val source: WrittenSource) {
        fun index(declaration: CfirDeclaration): Int {
            check(declaration.origin == CfirDeclarationOrigin.Source)
            check(declaration.symbol.cfir === declaration) { "wire 索引必须使用符号实际绑定的原声明" }
            val declarationSource = checkNotNull(declaration.source)
            val begin = sourcePosition(declarationSource.startOffset)
            val end = sourcePosition(declarationSource.endOffset)
            val kind = declarationKind(declaration)
            val name = nativeWireIdentifier(declaration)
            val identifierRange = SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME
                .markOrNull(declarationSource)?.single()
            val identifier = identifierRange?.let { sourcePosition(it.startOffset) }
            return (0 until pkg.allDeclsLength).single { index ->
                val candidate = checkNotNull(pkg.allDecls(index))
                // Marker class 与其隐式 init 可以共享 begin；仍须同时匹配 kind、原生名及全部位置。
                candidate.kind == kind && candidate.identifier == name &&
                        matchesPosition(candidate.begin, begin) && matchesPosition(candidate.end, end) &&
                        matchesPosition(candidate.identifierPos, identifier)
            }
        }

        /** 原样使用正式 producer 的 DeclKind 投影；它只区分 wire 记录类别。 */
        private fun declarationKind(declaration: CfirDeclaration): UShort = when (declaration) {
            is CfirBuiltInDeclaration -> DeclKind.BuiltInDecl
            is CfirClass -> DeclKind.ClassDecl
            is CfirInterface -> DeclKind.InterfaceDecl
            is CfirStruct -> DeclKind.StructDecl
            is CfirEnum -> DeclKind.EnumDecl
            is CfirTypeAlias -> DeclKind.TypeAliasDecl
            is CfirProperty -> DeclKind.PropDecl
            is CfirEnumConstructor -> if (declaration.valueParameters.isEmpty()) DeclKind.VarDecl else DeclKind.FuncDecl
            is CfirFunction -> DeclKind.FuncDecl
            is CfirValueParameter -> DeclKind.FuncParam
            is CfirTypeParameter -> DeclKind.GenericParamDecl
            is CfirVariable -> DeclKind.VarDecl
            is CfirExtend -> DeclKind.ExtendDecl
            else -> error("Unsupported CJO declaration ${declaration::class.simpleName}")
        }

        /**
         * ASTWriter::SaveDecl 写入 Decl.identifier，而非 CFIR 内部特殊符号名。
         * ParseConstructor/ParseFinalizer 与 AddStaticInitForTypeDecl 定义构造、析构及静态初始化原生名；
         * accessor 与正式 producer 一样取真实 get/set 名称范围，unnamed extend 保持空 identifier。
         */
        private fun nativeWireIdentifier(declaration: CfirDeclaration): String = when (declaration) {
            is CfirClassLikeDeclaration -> declaration.name.asString()
            is CfirExtend -> ""
            is CfirConstructor -> if (declaration.status.isStatic) "static.init" else "init"
            is CfirFinalizer -> "~init"
            is CfirPropertyAccessor -> {
                val accessorSource = checkNotNull(declaration.source)
                val range = checkNotNull(SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME.markOrNull(accessorSource))
                    .single()
                source.content.substring(range.startOffset, range.endOffset)
            }
            is CfirCallableDeclaration -> declaration.symbol.name.asString()
            is CfirTypeParameter -> declaration.name.asString()
            else -> error("Unsupported CJO declaration ${declaration::class.simpleName}")
        }

        private fun sourcePosition(offset: Int): CjDiagnosticRelatedSourceLocation {
            val prefix = source.content.substring(0, offset)
            return CjDiagnosticRelatedSourceLocation(
                source.path.toString(),
                prefix.count { it == '\n' } + 1,
                offset - prefix.lastIndexOf('\n'),
            )
        }

        private fun matchesPosition(position: Position?, expected: CjDiagnosticRelatedSourceLocation?): Boolean {
            if (expected == null) return position == null
            if (position == null || position.pkgId != 0u) return false
            return position.line == expected.line && position.column == expected.column &&
                    filePath(position) == Path.of(expected.filePath).toAbsolutePath().normalize()
        }

        fun declaration(member: CfirDeclaration): Decl = checkNotNull(pkg.allDecls(index(member)))
        fun begin(member: CfirDeclaration): Position = checkNotNull(declaration(member).begin)
        fun identifier(member: CfirDeclaration): CjDiagnosticRelatedSourceLocation {
            val position = checkNotNull(declaration(member).identifierPos)
            return CjDiagnosticRelatedSourceLocation(filePath(position).toString(), position.line, position.column)
        }

        private fun filePath(position: Position): Path {
            assertEquals(0u, position.pkgId)
            check(position.file > 0u && position.file.toLong() <= pkg.allFilesLength)
            return Path.of(checkNotNull(pkg.allFiles(position.file.toInt() - 1))).toAbsolutePath().normalize()
        }
    }

    private companion object {
        // 逐字取自本侧车已执行的 official-private-library/source.cj；只分离 transport，不改 private 语义。
        val PRIVATE_LIBRARY_SOURCE = """
            package c3_member_identity

            public class PrivateGeneric<T> {
                private func select(value: T): Unit { }
                private func select(value: Int64): Unit { }
            }
        """.trimIndent()

        val PRIVATE_INT64_CONSUMER = """
            package c3_roundtrip_consumer_int64

            import c3_member_identity.PrivateGeneric

            public func instantiate(value: PrivateGeneric<Int64>): Unit { }
        """.trimIndent()

        val PRIVATE_INT32_CONSUMER = """
            package c3_roundtrip_consumer_int32

            import c3_member_identity.PrivateGeneric

            public func instantiate(value: PrivateGeneric<Int32>): Unit { }
        """.trimIndent()
    }
}

/** 读取官方 schema 的属性 ordinal；没有造出或补写 CJO 属性。 */
private fun Decl.has(attribute: Attribute): Boolean {
    val word = attribute.ordinal / 64
    check(word < attributesLength)
    return attributes(word) and (1uL shl (attribute.ordinal % 64)) != 0uL
}

private class RoundTripMessageCollector : MessageCollector {
    val entries = mutableListOf<Pair<CompilerMessageSeverity, String>>()
    override fun clear() = entries.clear()
    override fun hasErrors(): Boolean = entries.any { it.first.isError }
    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        entries += severity to message
    }
}
