@file:OptIn(ExperimentalUnsignedTypes::class, org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.analysis.tests

import PackageFormat.TypeKind
import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCfirDiagnosticTestBase
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostic.ConeDiagnosticWithCandidates
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpMode
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.symbols.CfirErrorCallableSymbol
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.providers.dependenciesSymbolProvider
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.serialization.cjo.*
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.config.*
import org.cangnova.cangjie.frontend.pipeline.CfirFrontendPipelinePhase
import org.cangnova.cangjie.frontend.pipeline.ConfigurationPipelineArtifact
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.test.CfirParser
import org.cangnova.cangjie.test.builders.TestConfigurationBuilder
import org.cangnova.cangjie.test.config.TestPhase
import org.cangnova.cangjie.test.config.TestPhaseDirectives.RUN_PIPELINE_TILL
import org.cangnova.cangjie.test.config.firHandlersStep
import org.cangnova.cangjie.test.directives.CangjieTestDirectives.WITH_STDLIB
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives.CJMP_MODE
import org.cangnova.cangjie.test.directives.LanguageSettingsDirectives.LANGUAGE_VERSION
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.frontend.CfirAnalysisHandler
import org.cangnova.cangjie.test.frontend.CfirOutputArtifact
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.compilerConfigurationProvider
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.util.Collections
import java.util.IdentityHashMap
import org.junit.jupiter.api.Assertions.*

class CfirSourceExpressionExportUnexportedSignatureTest : AbstractUnexportedSignatureConsumerTest(CfirParser.Psi)
class CfirSourceExpressionExportUnexportedSignatureLightTreeTest : AbstractUnexportedSignatureConsumerTest(CfirParser.LightTree)

/** 复用 CfirSourceExpressionExportTest 的标准库配置、handler 和正式 frontend 工厂，不替换 lookup/provider。 */
abstract class AbstractUnexportedSignatureConsumerTest(parser: CfirParser) : AbstractCfirDiagnosticTestBase(parser) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            LANGUAGE_VERSION with "1.1.3"
            CJMP_MODE with CfirCjmpMode.NONE
            RUN_PIPELINE_TILL with TestPhase.FRONTEND
            +WITH_STDLIB
        }
        builder.firHandlersStep { useHandlers({ services: TestServices -> UnexportedSignatureConsumerHandler(services, parser) }) }
    }

    @Test
    fun libraryCallsAndFunctionValueReferencesExcludeUnexportedSignatures() {
        val root = Files.createTempDirectory("partial-signature-host-")
        try {
            val host = root.resolve("host.cj")
            Files.writeString(host, "package partial_host\nfunc host(): Int64 { return 7 }\n", Charsets.UTF_8)
            runTest(host.toAbsolutePath().toString())
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}

/**
 * 固定 Native 14276740ffec1262b81b6b5236b1d840af270b37：ASTWriter.cpp:1261–1265/1571–1602
 * 保存 partial 的真实空 F；TypeCheckCall.cpp:1839–1863/1512–1515 分别排除调用与函数值引用。
 * Kotlin 3c7a5041f238102285764d370665f56f587f9dc4 的 TowerLevelHandler.consumeCandidate 归属保持不变。
 * 库种子是此 Native 存储合同的有界 schema 输入；consumer 使用实际 .cjo、SDK、import 与完整 resolve。
 */
@OptIn(CompilerConfiguration.Internals::class)
private class UnexportedSignatureConsumerHandler(testServices: TestServices, private val parser: CfirParser) :
    CfirAnalysisHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer> = emptyList()
    private var checkedModules = 0

    override fun processModule(module: TestModule, info: CfirOutputArtifact) {
        assertEquals(0, checkedModules)
        val root = Files.createTempDirectory("partial-signature-consumer-")
        val disposable = Disposer.newDisposable("partial-signature-consumer")
        try {
            val library = root.resolve(CjoConstants.packageNameToPath("partial_library"))
            Files.createDirectories(library.parent)
            val bytes = CjoPackageWriter.toByteArray(librarySeed())
            Files.write(library, bytes)
            val source = root.resolve("consumer.cj")
            Files.writeString(source, """
                package partial_consumer
                import partial_library.*
                func rejectedCall(): Int64 { return omitted() }
                func rejectedReference(): () -> Int64 { return omitted }
                func acceptedCall(): Int64 { return available() }
                func acceptedReference(): () -> Int64 { return available }
            """.trimIndent(), Charsets.UTF_8)
            val moduleConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
            val diagnostics = DiagnosticsCollectorImpl()
            val configuration = CompilerConfiguration().apply {
                moduleName = "partial-consumer"
                languageVersionSettings = moduleConfiguration.languageVersionSettings
                useLightTree = parser == CfirParser.LightTree
                cjmpMode = CfirCjmpMode.NONE
                diagnosticsCollector = diagnostics
                messageCollector = PartialConsumerMessages()
                moduleConfiguration.classpathRoots.forEach { addClasspathRoot(it.path) }
                addClasspathRoot(root.toString())
                addCangJieSourceRoot(source.toString())
            }
            val output = checkNotNull(CfirFrontendPipelinePhase.executePhase(
                ConfigurationPipelineArtifact(configuration, disposable))).frontendOutput.outputs.single()
            assertNotSame(info.partsForDependsOnModules.single { it.module === module }.session, output.session)
            val provider = output.session.dependenciesSymbolProvider
            val omitted = provider.getTopLevelFunctionSymbols(FqName("partial_library"), Name.identifier("omitted")).single().cfir
            val overloads = provider.getTopLevelFunctionSymbols(FqName("partial_library"), Name.identifier("available")).map { it.cfir }
            assertEquals(2, overloads.size, "身份查询保留真实 partial d；不能以删除库声明代替候选过滤")
            val available = overloads.single { !it.exportFacts.hasUnexportedSignature }
            assertTrue(omitted.exportFacts.hasUnexportedSignature)
            assertEquals(1, overloads.count { it.exportFacts.hasUnexportedSignature })
            (overloads + omitted).forEach {
                assertEquals(CfirSession.Kind.Library, it.moduleData.session.kind)
                assertEquals(library.toAbsolutePath().normalize().toString(), checkNotNull(it.cjoDeclarationOrigin).packagePath)
            }
            val functions = output.fir.single().declarations.filterIsInstance<CfirNamedFunction>()
            assertEquals(4, functions.size)
            functions.forEach { function ->
                val rejected = function.name.asString().startsWith("rejected")
                val reference = references(function).single { it.name.asString() == if (rejected) "omitted" else "available" }
                if (rejected) {
                    val error = assertInstanceOf(CfirDiagnosticHolder::class.java, reference)
                    // 正常 unresolved 路径持有合成错误候选；被省略签名的真实库声明不能进入推断。
                    if (reference is CfirNamedReferenceWithCandidateBase) {
                        assertInstanceOf(CfirErrorCallableSymbol::class.java, reference.candidateSymbol)
                        assertNotSame(omitted.symbol, reference.candidateSymbol)
                    }
                    assertFalse(error.diagnostic is ConeDiagnosticWithCandidates, "省略签名不得创建隐藏/不适用的推断候选")
                } else {
                    assertSame(available.symbol, assertInstanceOf(CfirResolvedNamedReference::class.java, reference).resolvedSymbol,
                        "call 与 function-value reference 均选择真实完整签名 overload")
                }
            }
            assertTrue(diagnostics.diagnostics.isNotEmpty(), "两个省略签名使用必须由正式诊断流水线拒绝")
            assertArrayEquals(bytes, Files.readAllBytes(library))
            checkedModules++
        } finally {
            Disposer.dispose(disposable)
            root.toFile().deleteRecursively()
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) = assertEquals(1, checkedModules)

    private fun references(function: CfirNamedFunction): Set<CfirNamedReference> {
        val references = Collections.newSetFromMap(IdentityHashMap<CfirNamedReference, Boolean>())
        function.accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (element is CfirNamedReference) references += element
                element.acceptChildren(this)
            }
        })
        return references
    }

    private fun librarySeed(): CjoPackageMetadata {
        val empty = CjoFunctionInfo(body = CjoFunctionBodyInfo())
        val attrs = flags(Attribute.PUBLIC, Attribute.GLOBAL, Attribute.GENERIC_INSTANTIATED, Attribute.GENERIC, Attribute.UNREACHABLE)
        return CjoPackageMetadata("partial_library", "partial_library", schemaProfile = CjoSchemaProfile.OFFICIAL_CJMP,
            allFiles = listOf("partial.cj"), declarations = listOf(
                CjoPackageDeclaration("omitted", exportId = "partial_omitted", attributes = attrs,
                    generic = CjoGenericMetadata(emptyList()), info = empty),
                CjoPackageDeclaration("available", exportId = "partial_available_unexported", attributes = attrs,
                    generic = CjoGenericMetadata(emptyList()), info = empty),
                CjoPackageDeclaration("available", exportId = "partial_available_resolved", type = 1u,
                    attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL), info = CjoFunctionInfo(body = CjoFunctionBodyInfo(
                        parameterLists = listOf(emptyList()), desugaredParameterLists = listOf(emptyList()), returnType = 2u))),
            ), types = listOf(CjoTypeMetadata(TypeKind.Func, semanticInfo = CjoFunctionTypeInfoMetadata(2u, false, false)),
                CjoTypeMetadata(TypeKind.Int64)))
    }

    private fun flags(vararg attributes: Attribute): List<ULong> = List(2) { word ->
        attributes.filter { it.ordinal / 64 == word }.fold(0uL) { bits, attribute -> bits or (1uL shl (attribute.ordinal % 64)) }
    }
}

private class PartialConsumerMessages : MessageCollector {
    private val severities = mutableListOf<CompilerMessageSeverity>()
    override fun clear() = severities.clear()
    override fun hasErrors(): Boolean = severities.any { it.isError }
    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        severities += severity
    }
}
