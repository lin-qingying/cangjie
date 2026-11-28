package org.cangnova.cangjie.cfir.analysis.tests

import PackageFormat.Decl
import PackageFormat.DeclKind
import PackageFormat.Package
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCfirDiagnosticTestSpecBase
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.isFromCommonPart
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpChirOutput
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpMode
import org.cangnova.cangjie.cfir.entrypoint.configuration.configureCjmpCommonPartInputs
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.cjoOutputDirectory
import org.cangnova.cangjie.config.diagnosticsCollector
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.config.useLightTree
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.frontend.pipeline.CfirFrontendPipelinePhase
import org.cangnova.cangjie.frontend.pipeline.ConfigurationPipelineArtifact
import org.cangnova.cangjie.frontend.pipeline.DefaultCfirFrontendPipelineArtifact
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.metadata.model.Attribute
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path

/** C3 声明预检查反例：两个 parser 各自验收 SOURCE 与真实写出、加载的 common CJO。 */
class CfirCjmpDeclarationPreCheckTest : AbstractCfirCjmpDeclarationPreCheckTest(CfirParser.Psi)
class CfirCjmpDeclarationPreCheckLightTreeTest : AbstractCfirCjmpDeclarationPreCheckTest(CfirParser.LightTree)

/**
 * 语义依据为 20261113-c3-declaration-group-probes 与 cjmp-c3-candidate-authority 的官方分支。
 * G1 的独立 NOT_MATCHED witness 只见证 Sema；无 witness 的 exportID ICE 不作为成功期望。
 * 本套件不重建 Names/filter 算法：诊断由正式管线产生，期望只取新增 fixture 的 inline 标记。
 * 独立 nominal/extend/local scope、static 消费 filtered 组和非函数类别尚无本轮 cjc actual，留待补证。
 */
abstract class AbstractCfirCjmpDeclarationPreCheckTest(parser: CfirParser) : AbstractCfirDiagnosticTestSpecBase(parser) {
    private var inspectCommon: (List<CfirFile>, Package) -> Unit = { _, _ -> }
    private var inspectCalls = false

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.firHandlersStep {
            useHandlers({ services: TestServices -> DeclarationPreCheckCjoHandler(services, parser, inspectCommon, inspectCalls) })
        }
    }

    @Test
    fun specificRemovesEveryOwnCommonOverloadWithoutSignatureRestriction() = runCase("g1Baseline.cj")

    @Test
    fun commonDeclarationOrderDoesNotChangeGroupFiltering() = runCase("g1CommonReversed.cj")

    @Test
    fun specificDeclarationOrderDoesNotChangeGroupFiltering() = runCase("g1SpecificReversed.cj")

    @Test
    fun reversingBothDeclarationOrdersDoesNotChangeGroupFiltering() = runCase("g1BothReversed.cj")

    @Test
    fun physicalSourceFileOrderKeepsTheSameGroup() = runCase("g1FilesForward.cj")

    @Test
    fun reversingPhysicalSourceFilesDoesNotChangeGroupFiltering() = runCase("g1FilesReversed.cj")

    @Test
    fun specificOwnerAloneDoesNotTriggerFunctionGroupDeletion() = runCase("g1NoSpecificFunction.cj")

    @Test
    fun physicalCommonOwnerDoesNotMakeItsUnmarkedMemberCommon() {
        inspectCommon = { files, pkg ->
            val owner = files.flatMap { it.declarations }.filterIsInstance<CfirClass>().single { it.name.asString() == "Box" }
            assertTrue(owner.status.isCommon)
            assertFalse(owner.declarations.filterIsInstance<CfirNamedFunction>().single().status.isCommon)
            val declarations = (0 until pkg.allDeclsLength).map { checkNotNull(pkg.allDecls(it)) }
            assertTrue(declarations.single { it.kind == DeclKind.ClassDecl && it.identifier == "Box" }.has(Attribute.COMMON))
            val member = declarations.single { it.kind == DeclKind.FuncDecl && it.identifier == "f" }
            assertFalse(member.has(Attribute.COMMON), "G2: COMMON=false 来自真实 common 源码")
            assertTrue(member.has(Attribute.FROM_COMMON_PART), "G2: 物理来源位不得触发 COMMON 删除")
        }
        runCase("g2ExplicitSpecific.cj")
    }

    @Test
    fun unmarkedSpecificMemberStillConflictsWithUnmarkedCommonPartMember() = runCase("g2UnmarkedSpecific.cj")

    @Test
    fun declarationFilteringDoesNotRemoveTheCommonOnlyCallCounterpart() {
        inspectCalls = true
        runCase("callCommonOnlyOverload.cj")
    }

    private fun runCase(file: String) = runTest(ROOT + file)

    private companion object {
        const val ROOT = "cfir/analysis-tests/testData/diagnostics2/common-specific/declaration-precheck/"
    }
}

/**
 * 复用 module/source/inline metadata 服务与 CJMP 两段式 frontend 入口。
 * 模式、refinement 身份和源文件顺序来自 fixture；不按声明名字或测试名字切换编译行为。
 */
@OptIn(CompilerConfiguration.Internals::class)
private class DeclarationPreCheckCjoHandler(
    testServices: TestServices,
    private val parser: CfirParser,
    private val inspectCommon: (List<CfirFile>, Package) -> Unit,
    private val inspectCalls: Boolean,
) : CfirAnalysisHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer> = emptyList()
    private var checkedSpecificModules = 0

    override fun processModule(module: TestModule, info: CfirOutputArtifact) {
        if (module.directives[CfirDiagnosticsDirectives.CJMP_MODE].single() != CfirCjmpMode.SPECIFIC) return
        val specific = info.partsForDependsOnModules.single { it.module === module }
        val common = info.partsForDependsOnModules.single {
            it.module.directives[CfirDiagnosticsDirectives.CJMP_MODE].single() == CfirCjmpMode.COMMON
        }
        assertNotSame(common.session, specific.session)
        val commonFiles = common.firFilesByTestFile.filterKeys { !it.isAdditional }.values.toList()
        val specificFiles = specific.firFilesByTestFile.filterKeys { !it.isAdditional }.values.toList()
        assertTrue(commonFiles.first().moduleData in specificFiles.first().moduleData.refinementDependencies)
        val expected = info.allFirFilesByTestFile.keys.filter { !it.isAdditional }.flatMap { file ->
            testServices.globalMetadataInfoHandler.getExistingMetaInfosForFile(file).map { it.tag }
        }.sorted()
        val root = Files.createTempDirectory("cjmp-declaration-precheck-")
        val disposable = Disposer.newDisposable("cjmp-declaration-precheck")
        try {
            val commonResult = compile(common, root.resolve("common"), disposable) {
                cjmpChirOutput = true
                cjoOutputDirectory = root.resolve("out").toString()
            }
            assertEquals(emptyList<String>(), commonResult.diagnostics.diagnostics.map { it.factoryName }, "common 必须真实编译成功")
            assertFalse(commonResult.messages.hasErrors(), commonResult.messages.entries.toString())
            checkNotNull(commonResult.artifact) { "common frontend 未完成" }
            val packageName = commonFiles.map { it.packageDirective.packageFqName }.distinct().single().asString()
            val cjo = root.resolve("out").resolve(CjoConstants.packageNameToPath(packageName))
            assertTrue(Files.isRegularFile(cjo), "必须由正式 MetadataProducer/PackageWriter 写出 CJO")
            inspectCommon(commonFiles, Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(cjo))))
            val specificResult = compile(specific, root.resolve("specific"), disposable) {
                configureCjmpCommonPartInputs(listOf(cjo.toString()), emptyList())
            }
            assertEquals(
                expected,
                specificResult.diagnostics.diagnostics.map { it.factoryName.removePrefix("CFIR_") }.sorted(),
                "CJO 必须与官方 fixture 的诊断多重集相同，不能从 SOURCE actual 生成期望",
            )
            for (diagnostic in specificResult.diagnostics.diagnostics.filter { it.factoryName == "CFIR_CONFLICTING_OVERLOADS" }) {
                val note = diagnostic.relatedInformation.single()
                assertEquals("conflict with the declaration", note.message)
                val location = checkNotNull(note.sourceLocation) { "G1/G2 冲突必须保留真实 common CJO 的 peer 位置" }
                assertTrue(Path.of(location.filePath).normalize().startsWith(root.resolve("common")))
            }
            assertTrue(
                specificResult.messages.entries.filter { it.first.isError }.all { (_, message) ->
                    specificResult.diagnostics.diagnostics.any { it.renderMessage() == message }
                },
                "不得把 parser/driver/加载失败计为反例成功：${specificResult.messages.entries}",
            )
            if (expected.isEmpty()) {
                checkNotNull(specificResult.artifact) { "零诊断用例必须完成真实 CJO frontend" }
            }
            if (inspectCalls) {
                assertCommonOnlyCall(specificFiles, fromCjo = false)
                val output = checkNotNull(specificResult.artifact).frontendOutput.outputs.single()
                assertCommonOnlyCall(output.fir, fromCjo = true)
            }
            checkedSpecificModules++
        } finally {
            Disposer.dispose(disposable)
            root.toFile().deleteRecursively()
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        assertEquals(1, checkedSpecificModules, "必须验收一次 SOURCE refinement 图和真实 CJO 输入")
    }

    private fun compile(
        part: CfirOutputPartForDependsOnModule,
        sourceRoot: Path,
        disposable: Disposable,
        configure: CompilerConfiguration.() -> Unit,
    ): Compilation {
        val diagnostics = DiagnosticsCollectorImpl()
        val messages = PreCheckMessageCollector()
        val configuration = CompilerConfiguration().apply {
            moduleName = part.module.name
            languageVersionSettings = testServices.compilerConfigurationProvider.getCompilerConfiguration(part.module).languageVersionSettings
            useLightTree = parser == CfirParser.LightTree
            cjmpMode = part.module.directives[CfirDiagnosticsDirectives.CJMP_MODE].single()
            diagnosticsCollector = diagnostics
            messageCollector = messages
            for (file in part.firFilesByTestFile.keys.filter { !it.isAdditional }) {
                val path = sourceRoot.resolve(file.relativePath)
                Files.createDirectories(path.parent)
                Files.writeString(path, testServices.sourceFileProvider.getContentOfSourceFile(file), Charsets.UTF_8)
                addCangJieSourceRoot(path.toString())
            }
            configure()
        }
        return Compilation(CfirFrontendPipelinePhase.executePhase(ConfigurationPipelineArtifact(configuration, disposable)), diagnostics, messages)
    }

    /** 调用必须解析到原 common-only Int64 函数，不能仅凭零诊断认定成功。 */
    private fun assertCommonOnlyCall(files: List<CfirFile>, fromCjo: Boolean) {
        val calls = mutableListOf<CfirFunctionCall>()
        val visitor = object : CfirDefaultVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (element is CfirFunctionCall && (element.calleeReference as? CfirNamedReference)?.name?.asString() == "f") {
                    calls += element
                }
                element.acceptChildren(this)
            }
        }
        files.forEach { it.accept(visitor) }
        val call = calls.single()
        val target = (call.calleeReference as CfirResolvedNamedReference).resolvedSymbol.cfir as CfirNamedFunction
        assertTrue(target.status.isCommon)
        assertFalse(target.status.isSpecific)
        assertEquals(PrimitiveTypeKind.INT64, (target.valueParameters.single().returnTypeRef.coneTypeOrNull as ConePrimitiveType).kind)
        assertEquals(fromCjo, target.isFromCommonPart)
        assertNotSame(files.single().moduleData.session, target.moduleData.session)
    }

    private data class Compilation(
        val artifact: DefaultCfirFrontendPipelineArtifact?,
        val diagnostics: DiagnosticsCollectorImpl,
        val messages: PreCheckMessageCollector,
    )
}

/** CJO 属性位由官方 schema 的 ordinal 解码，只检查来源和自身角色，不实现候选删除。 */
private fun Decl.has(attribute: Attribute): Boolean {
    val word = attribute.ordinal / 64
    check(word < attributesLength) { "真实 CJMP CJO 缺失角色属性字" }
    return attributes(word) and (1uL shl (attribute.ordinal % 64)) != 0uL
}

private class PreCheckMessageCollector : MessageCollector {
    val entries = mutableListOf<Pair<CompilerMessageSeverity, String>>()
    override fun clear() = entries.clear()
    override fun hasErrors(): Boolean = entries.any { it.first.isError }
    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        entries += severity to message
    }
}
