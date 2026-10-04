package org.cangnova.cangjie.cfir.analysis.tests

import PackageFormat.DeclKind
import PackageFormat.Package
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.cfir.DependencyListForCliModule
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.diagnostics.CjRegisteredDiagnosticFactoriesStorage
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.entrypoint.configuration.diagnosticFactoriesStorage
import org.cangnova.cangjie.cfir.entrypoint.session.CfirDefaultSessionFactory
import org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir
import org.cangnova.cangjie.cfir.pipeline.CfirSessionConstructionUtils
import org.cangnova.cangjie.cfir.pipeline.CfirSessionProducer
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildPreMacroRawFiles
import org.cangnova.cangjie.cfir.resolve.providers.macro.expandWithDefaultContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.recordExpandedRawFilesOnce
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.addClasspathRoot
import org.cangnova.cangjie.config.cjoOutputDirectory
import org.cangnova.cangjie.config.diagnosticsCollector
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.config.useLightTree
import org.cangnova.cangjie.frontend.pipeline.cfirFrontendPipeline
import org.cangnova.cangjie.frontend.pipeline.ConfigurationPipelineArtifact
import org.cangnova.cangjie.frontend.pipeline.DefaultCfirFrontendPipelineArtifact
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path

/**
 * 通过完整 raw construction 和 provider 注册验证声明所属名义类型的真实符号身份。
 *
 * Kotlin 对位是 ContainingClassUtils.getContainingClassSymbol：从声明自己的 session 查询
 * provider。预期 owner 直接取自实际声明树，不能通过 ClassId 查表反过来构造预期结果。
 */
@OptIn(CompilerConfiguration.Internals::class)
class CfirDeclarationOwnerTest : AbstractCfirAnalysisTestCase() {
    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    @Test
    fun psiMembersKeepTheirDeclarationSiteOwners() {
        assertDeclarationSiteOwners(useLightTree = false)
    }

    @Test
    fun lightTreeMembersKeepTheirDeclarationSiteOwners() {
        assertDeclarationSiteOwners(useLightTree = true)
    }

    @Test
    fun psiSameFileRedeclarationsKeepDistinctOwners() {
        assertSameFileRedeclarationOwners(useLightTree = false)
    }

    @Test
    fun lightTreeSameFileRedeclarationsKeepDistinctOwners() {
        assertSameFileRedeclarationOwners(useLightTree = true)
    }

    @Test
    fun psiCjoMembersKeepTheirDeserializedDeclarationOwners() {
        assertCjoDeclarationOwners(useLightTree = false)
    }

    @Test
    fun lightTreeCjoMembersKeepTheirDeserializedDeclarationOwners() {
        assertCjoDeclarationOwners(useLightTree = true)
    }

    /**
     * 使用真实 frontend 两次编译：第一次由 MetadataProducer/PackageWriter 写出 CJO，
     * 第二次通过普通 classpath 加载。整个断言期间保留两边 session，不构造伪 library session。
     */
    private fun assertCjoDeclarationOwners(useLightTree: Boolean) {
        val disposable = Disposer.newDisposable("declaration-owner-cjo")
        try {
            val source = Files.writeString(tempDir.resolve("library.cj"), CJO_MEMBER_SOURCE, Charsets.UTF_8)
            val outputRoot = tempDir.resolve("library-output")
            val library = compileOwnerSource(source, disposable, useLightTree) {
                cjoOutputDirectory = outputRoot.toString()
            }
            val cjo = outputRoot.resolve(CjoConstants.packageNameToPath("declaration_owner_cjo"))
            assertTrue(Files.isRegularFile(cjo), "the real frontend must write its CJO output")
            val pkg = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(cjo)))
            val variants = (0 until pkg.allDeclsLength).mapNotNull { pkg.allDecls(it) }
                .filter { it.identifier == "Empty" || it.identifier == "Value" }
            assertEquals(DeclKind.VarDecl, variants.single { it.identifier == "Empty" }.kind)
            assertEquals(DeclKind.FuncDecl, variants.single { it.identifier == "Value" }.kind)

            val consumerSource = Files.writeString(
                tempDir.resolve("consumer.cj"),
                """
                    package declaration_owner_client
                    import declaration_owner_cjo.*

                    public func use(c: ClassOwner, s: StructOwner, e: EnumOwner): Unit {}
                """.trimIndent(),
                Charsets.UTF_8,
            )
            val consumer = compileOwnerSource(consumerSource, disposable, useLightTree) {
                addClasspathRoot(outputRoot.toString())
            }
            val consumerSession = consumer.frontendOutput.outputs.single().session
            val sourceOwners = library.frontendOutput.outputs.single().fir.single()
                .declarations.filterIsInstance<CfirClassLikeDeclaration>()
            assertEquals(setOf("ClassOwner", "StructOwner", "EnumOwner"), sourceOwners.map { it.name.asString() }.toSet())
            for (sourceOwner in sourceOwners) {
                val owner = requireNotNull(
                    consumerSession.symbolProvider.getClassLikeSymbolByClassId(
                        ClassId(FqName("declaration_owner_cjo"), sourceOwner.name),
                    ),
                ).cfir
                assertEquals(CfirDeclarationOrigin.Library, owner.origin)
                assertNotSame(sourceOwner.symbol, owner.symbol)
                assertNotSame(consumerSession, owner.moduleData.session)

                val functions = owner.declarations.filterIsInstance<CfirNamedFunction>()
                assertEquals(setOf("member", "factory"), functions.map { it.name.asString() }.toSet())
                assertFalse(functions.single { it.name.asString() == "member" }.status.isStatic)
                assertTrue(functions.single { it.name.asString() == "factory" }.status.isStatic)
                if (owner is CfirClass || owner is CfirStruct) {
                    assertTrue(owner.declarations.filterIsInstance<CfirConstructor>().isNotEmpty())
                    val fields = owner.declarations.filterIsInstance<CfirFieldVariable>()
                    assertEquals(setOf("field", "staticField"), fields.map { it.name.asString() }.toSet())
                    assertFalse(fields.single { it.name.asString() == "field" }.status.isStatic)
                    assertTrue(fields.single { it.name.asString() == "staticField" }.status.isStatic)
                    val properties = owner.declarations.filterIsInstance<CfirProperty>()
                    assertEquals(setOf("property", "staticProperty"), properties.map { it.name.asString() }.toSet())
                    assertFalse(properties.single { it.name.asString() == "property" }.status.isStatic)
                    assertTrue(properties.single { it.name.asString() == "staticProperty" }.status.isStatic)
                    for (property in properties) {
                        assertDeserializedMemberOwner(owner, requireNotNull(property.getter))
                        assertDeserializedMemberOwner(owner, requireNotNull(property.setter))
                    }
                } else if (owner is CfirEnum) {
                    val constructors = owner.declarations.filterIsInstance<CfirEnumConstructor>()
                    assertEquals(setOf("Empty", "Value"), constructors.map { it.name.asString() }.toSet())
                    assertTrue(constructors.single { it.name.asString() == "Empty" }.valueParameters.isEmpty())
                    assertEquals(1, constructors.single { it.name.asString() == "Value" }.valueParameters.size)
                }
                for (member in owner.declarations.filterIsInstance<CfirCallableDeclaration>()) {
                    assertDeserializedMemberOwner(owner, member)
                }
            }
        } finally {
            Disposer.dispose(disposable)
        }
    }

    /** 预期 parent 来自刚反序列化的 nominal 树，读取方固定使用成员自己的 library session。 */
    private fun assertDeserializedMemberOwner(owner: CfirClassLikeDeclaration, member: CfirCallableDeclaration) {
        assertEquals(CfirDeclarationOrigin.Library, member.origin)
        assertSame(owner.moduleData.session, member.moduleData.session)
        assertSame(
            owner.symbol,
            member.moduleData.session.cfirProvider.getContainingClass(member.symbol),
            "CJO ${owner.name}.${member.symbol.callableId.callableName} must retain its actual declaration owner",
        )
    }

    private fun compileOwnerSource(
        source: Path,
        disposable: Disposable,
        useLightTree: Boolean,
        configure: CompilerConfiguration.() -> Unit,
    ): DefaultCfirFrontendPipelineArtifact {
        val messages = OwnerMessageCollector()
        val diagnostics = DiagnosticsCollectorImpl()
        val configuration = CompilerConfiguration().apply {
            moduleName = "declaration-owner-cjo"
            messageCollector = messages
            diagnosticsCollector = diagnostics
            this.useLightTree = useLightTree
            addCangJieSourceRoot(source.toString())
            configure()
        }
        val result = cfirFrontendPipeline().executePhase(ConfigurationPipelineArtifact(configuration, disposable))
        assertFalse(messages.hasErrors(), messages.entries.joinToString("\n"))
        assertTrue(
            diagnostics.diagnostics.none { it.severity.isError },
            diagnostics.diagnostics.joinToString("\n") { "${it.factoryName}: ${it.renderMessage()}" },
        )
        return requireNotNull(result) { "owner fixture frontend did not complete: ${messages.entries}" }
    }

    private class OwnerMessageCollector : MessageCollector {
        val entries = mutableListOf<String>()
        private var hasError = false

        override fun clear() {
            entries.clear()
            hasError = false
        }

        override fun hasErrors(): Boolean = hasError

        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
            hasError = hasError || severity.isError
            entries += "$severity: $message"
        }
    }

    /** 同名源码分别进入两个独立 session，所有成员都必须返回各自声明现场的 owner。 */
    private fun assertDeclarationSiteOwners(useLightTree: Boolean) {
        val firstFile = buildAndRegisterFile("owners", MEMBER_SOURCE, useLightTree)
        val secondFile = buildAndRegisterFile("owners", MEMBER_SOURCE, useLightTree)
        assertNotSame(firstFile.moduleData.session, secondFile.moduleData.session)

        val firstOwners = firstFile.declarations.filterIsInstance<CfirClassLikeDeclaration>()
        val secondOwners = secondFile.declarations.filterIsInstance<CfirClassLikeDeclaration>()
        assertEquals(4, firstOwners.size)
        assertEquals(4, secondOwners.size)
        for ((firstOwner, secondOwner) in firstOwners.zip(secondOwners)) {
            assertEquals(firstOwner.symbol.classId, secondOwner.symbol.classId)
            assertNotSame(firstOwner.symbol, secondOwner.symbol)
        }

        for (file in listOf(firstFile, secondFile)) {
            val owners = file.declarations.filterIsInstance<CfirClassLikeDeclaration>()
            assertEquals(1, owners.filterIsInstance<CfirClass>().size)
            assertEquals(1, owners.filterIsInstance<CfirStruct>().size)
            assertEquals(1, owners.filterIsInstance<CfirInterface>().size)
            assertEquals(1, owners.filterIsInstance<CfirEnum>().size)
            for (owner in owners) {
                val functions = owner.declarations.filterIsInstance<CfirNamedFunction>()
                assertEquals(setOf("member", "factory"), functions.map { it.name.asString() }.toSet())
                assertFalse(functions.single { it.name.asString() == "member" }.status.isStatic)
                assertTrue(functions.single { it.name.asString() == "factory" }.status.isStatic)
                if (owner is CfirClass || owner is CfirStruct) {
                    assertTrue(owner.declarations.filterIsInstance<CfirConstructor>().any { !it.isPrimary })
                }
                assertMemberOwners(file, owner)
            }

            val variants = owners.filterIsInstance<CfirEnum>().single()
                .declarations.filterIsInstance<CfirEnumConstructor>()
            assertEquals(setOf("Empty", "Value"), variants.map { it.name.asString() }.toSet())
            assertTrue(variants.single { it.name.asString() == "Empty" }.valueParameters.isEmpty())
            assertEquals(1, variants.single { it.name.asString() == "Value" }.valueParameters.size)

            val topLevel = file.declarations.filterIsInstance<CfirNamedFunction>().single()
            assertNull(topLevel.moduleData.session.cfirProvider.getContainingClass(topLevel.symbol))
        }
    }

    /**
     * 两个同文件同 ClassId 的类是语义错误恢复输入，不是合法程序正例。
     * 即使诊断重声明，provider 也必须保留每个成员的真实父声明，不能挑该文件中的第一个类。
     */
    private fun assertSameFileRedeclarationOwners(useLightTree: Boolean) {
        val file = buildAndRegisterFile("redeclarations", REDECLARATION_SOURCE, useLightTree)
        val owners = file.declarations.filterIsInstance<CfirClass>()
        assertEquals(2, owners.size)
        assertEquals(owners[0].symbol.classId, owners[1].symbol.classId)
        assertNotSame(owners[0].symbol, owners[1].symbol)
        for (owner in owners) {
            assertTrue(owner.declarations.filterIsInstance<CfirConstructor>().any { !it.isPrimary })
            val functions = owner.declarations.filterIsInstance<CfirNamedFunction>()
            assertEquals(2, functions.size)
            assertFalse(functions.single { it.name.asString() == "member" }.status.isStatic)
            assertTrue(functions.single { it.name.asString() == "factory" }.status.isStatic)
        }
        // 先查第二个类，直接暴露 ClassId + 文件猜测把两组成员都归到首个声明的问题。
        for (owner in owners.asReversed()) {
            assertMemberOwners(file, owner)
        }
    }

    private fun assertMemberOwners(file: CfirFile, owner: CfirClassLikeDeclaration) {
        assertNull(owner.moduleData.session.cfirProvider.getContainingClass(owner.symbol))
        val members = owner.declarations.filterIsInstance<CfirCallableDeclaration>()
        assertTrue(members.isNotEmpty())
        for (member in members) {
            val provider = member.moduleData.session.cfirProvider
            assertSame(file.moduleData.session, member.moduleData.session)
            assertSame(file, provider.getCfirCallableContainerFile(member.symbol))
            assertSame(
                owner.symbol,
                provider.getContainingClass(member.symbol),
                "${owner.name}.${member.symbol.callableId.callableName} must retain its actual declaration owner",
            )
        }
    }

    /** 完成正式 raw construction 后统一注册，不手工向 provider 的索引注入预期父关系。 */
    private fun buildAndRegisterFile(name: String, text: String, useLightTree: Boolean): CfirFile {
        val session = createResolveSession("$name.cj")
        val psiFile = createCjFile(name, text)
        val syntaxErrors = PsiTreeUtil.findChildrenOfType(psiFile, PsiErrorElement::class.java)
        assertTrue(syntaxErrors.isEmpty(), syntaxErrors.joinToString { it.errorDescription })
        val rawFile: CfirFile
        val surfaces: List<MacroSurface>
        if (useLightTree) {
            val built = LightTree2Cfir(session, session.cangjieScopeProvider).buildCfirFileWithSurfaces(
                parseLightTree(text),
                CjInMemoryTextSourceFile("$name.cj", null, text),
                text.toSourceLinesMapping(),
            )
            rawFile = built.first
            surfaces = built.second
        } else {
            val builder = PsiRawCfirBuilder(session)
            rawFile = builder.buildCfirFile(psiFile)
            surfaces = builder.consumeCollectedMacroSurfaces()
        }
        val pre = buildPreMacroRawFiles(session, listOf(rawFile), listOf(surfaces))
        val result = MacroConstructionService.Identity.expandWithDefaultContext(
            pre = pre,
            mode = MacroConstructionService.Mode.STRICT,
        ) as MacroConstructionResult.Success
        recordExpandedRawFilesOnce(session.cfirProvider as CfirProviderImpl, result.recordableFiles, result.registry)
        return rawFile
    }

    /** 与已有 raw/source-module 测试一致，由正式工厂创建 shared、library 和 source session。 */
    private fun createResolveSession(fileName: String): CfirSession {
        val moduleName = Name.special("<declaration-owner-test>")
        val configuration = CompilerConfiguration().apply {
            diagnosticFactoriesStorage = CjRegisteredDiagnosticFactoriesStorage()
        }
        val dependencies = DependencyListForCliModule.build(moduleName)
        val factory = CfirDefaultSessionFactory()
        return CfirSessionConstructionUtils.prepareSessions(
            files = listOf(fileName),
            configuration = configuration,
            rootModuleName = moduleName,
            dependencyList = dependencies,
            createSharedLibrarySession = {
                factory.createSharedLibrarySession(
                    mainModuleName = moduleName,
                    extensionRegistrars = emptyList(),
                    languageVersionSettings = configuration.languageVersionSettings,
                )
            },
            createLibrarySession = { shared ->
                factory.createLibrarySession(
                    sharedLibrarySession = shared,
                    moduleDataProvider = dependencies.moduleDataProvider,
                    extensionRegistrars = emptyList(),
                    languageVersionSettings = configuration.languageVersionSettings,
                )
            },
            createSourceSession = CfirSessionProducer<String> { _, moduleData, _, configurator ->
                factory.createSourceSession(
                    moduleData = moduleData,
                    extensionRegistrars = emptyList(),
                    configuration = configuration,
                    init = configurator,
                )
            },
        ).single().session
    }

    private fun parseLightTree(source: String): FlyweightCapableTreeStructure<LighterASTNode> {
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        return CangJieLightParser.parse(builder, languageModuleName = "")
    }

    companion object {
        // 可变普通/静态属性的形状来自官方 LLT property/exec_order.cj；
        // struct 的 mut getter/setter 来自 let/mut_1.cj，静态成员与 enum 形状沿用下面的成员矩阵证据。
        private val CJO_MEMBER_SOURCE = """
            package declaration_owner_cjo

            public class ClassOwner {
                public var field: Int64 = 1
                public static var staticField: Int64 = 2
                public init() {}
                public func member(): Int64 { 1 }
                public static func factory(): Int64 { 2 }
                public mut prop property: Int64 {
                    get() { field }
                    set(value) { field = value }
                }
                public mut static prop staticProperty: Int64 {
                    get() { staticField }
                    set(value) { staticField = value }
                }
            }

            public struct StructOwner {
                public var field: Int64 = 1
                public static var staticField: Int64 = 2
                public init() {}
                public func member(): Int64 { 1 }
                public static func factory(): Int64 { 2 }
                public mut prop property: Int64 {
                    get() { field }
                    set(value) { field = value }
                }
                public mut static prop staticProperty: Int64 {
                    get() { staticField }
                    set(value) { staticField = value }
                }
            }

            public enum EnumOwner {
                | Empty | Value(Int64)
                public func member(): Int64 { 1 }
                public static func factory(): Int64 { 2 }
            }
        """.trimIndent()

        // 成员形状来自既有官方 LLT：call/call21.cj（struct/静态函数/init）、
        // class/class11_test1.cj（interface 普通/静态函数）、enum/enum22_test/pkg/pkg.cj（variant/普通/静态函数）；
        // class/struct 的显式 init 也由 cjmpOkBasic.cj 的官方两段式探针覆盖。
        private val MEMBER_SOURCE = """
            package declaration_owner

            public class ClassOwner {
                public init() {}
                public func member(): Int64 { 1 }
                public static func factory(): Int64 { 2 }
            }

            public struct StructOwner {
                public init() {}
                public func member(): Int64 { 1 }
                public static func factory(): Int64 { 2 }
            }

            public interface InterfaceOwner {
                public func member(): Int64
                public static func factory(): Int64 { 2 }
            }

            public enum EnumOwner {
                | Empty | Value(Int64)
                public func member(): Int64 { 1 }
                public static func factory(): Int64 { 2 }
            }

            public func topLevel(): Int64 { 0 }
        """.trimIndent()

        private val REDECLARATION_SOURCE = """
            package declaration_owner

            public class Repeated {
                public init() {}
                public func member(): Int64 { 1 }
                public static func factory(): Int64 { 2 }
            }

            public class Repeated {
                public init() {}
                public func member(): Int64 { 3 }
                public static func factory(): Int64 { 4 }
            }
        """.trimIndent()
    }
}
