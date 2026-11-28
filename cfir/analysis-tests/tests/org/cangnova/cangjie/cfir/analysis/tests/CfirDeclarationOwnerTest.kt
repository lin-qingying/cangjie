package org.cangnova.cangjie.cfir.analysis.tests

import PackageFormat.DeclKind
import PackageFormat.Package
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.lang.PsiBuilderFactory
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.cfir.DependencyListForCliModule
import org.cangnova.cangjie.cfir.CfirElement
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
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirConstructedClassTypeParameterRef
import org.cangnova.cangjie.cfir.declarations.CfirResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.exportFacts
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.defaultValueMetadata
import org.cangnova.cangjie.cfir.declarations.defaultParameterHelperInfo
import org.cangnova.cangjie.cfir.declarations.defaultAssignmentSyntaxFacts
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysis
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl
import org.cangnova.cangjie.cfir.pipeline.runResolution
import org.cangnova.cangjie.cfir.isMemberParameter
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.patterns.bindingVariables
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
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildPreMacroRawFiles
import org.cangnova.cangjie.cfir.resolve.providers.macro.expandWithDefaultContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.recordExpandedRawFilesOnce
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.addClasspathRoot
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
        val result = CfirFrontendPipelinePhase.executePhase(ConfigurationPipelineArtifact(configuration, disposable))
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
    @Test fun psiDefaultHelperLifecycle() = assertDefaultHelperLifecycle(false, false)
    @Test fun lightTreeDefaultHelperLifecycle() = assertDefaultHelperLifecycle(true, false)
    @Test fun psiDefaultHelperOuterShadow() = assertDefaultHelperLifecycle(false, true)
    @Test fun lightTreeDefaultHelperOuterShadow() = assertDefaultHelperLifecycle(true, true)

    /** 同一正式 construction/provider/resolve 路径验证 helper 的 RAW 到 BODY 产品。 */
    private fun assertDefaultHelperLifecycle(useLightTree: Boolean, shadow: Boolean) {
        val text = if (shadow) "let b: Int64 = 7\nfunc choose(a: Int64, b!: Int64 = b): Int64 { b }"
            else "func choose<T>(a: T, b!: T = a): T { b }"
        val session = createResolveSession("helper-lifecycle.cj")
        val built = if (useLightTree) LightTree2Cfir(session, session.cangjieScopeProvider).buildPreMacroCfirFile(
            parseLightFile(text), CjInMemoryTextSourceFile("helper-lifecycle.cj", null, text), text.toSourceLinesMapping(),
        ) else PsiRawCfirBuilder(session, annotationMetadata = CfirAnnotationMetadataBuilder())
            .buildPreMacroCfirFile(createCjFile("helper-lifecycle", text))
        val result = MacroConstructionService.Identity.expandWithDefaultContext(
            buildPreMacroRawFiles(session, listOf(built)), MacroConstructionService.Mode.STRICT,
        ) as MacroConstructionResult.Success
        val file = result.recordableFiles.files.single()
        val owner = file.declarations.filterIsInstance<CfirNamedFunction>().single()
        val parameter = owner.valueParameters[1]
        val helper = checkNotNull(parameter.defaultValueMetadata?.function)
        val fresh = helper.valueParameters.single()
        assertEquals("b.1", helper.name.asString())
        assertSame(owner, helper.defaultParameterHelperInfo!!.owner)
        assertEquals(CfirResolvePhase.RAW_CFIR, helper.resolvePhase)
        assertFalse(file.declarations.any { it === helper })
        assertNotSame(owner.valueParameters[0].symbol, fresh.symbol)
        assertSame(helper.symbol, fresh.containingDeclarationSymbol)
        assertFalse(fresh.isNamed)
        assertFalse(fresh.isMemberParameter)
        assertNull(fresh.defaultValue)
        assertNull(fresh.defaultValueMetadata)
        val span = checkNotNull(parameter.defaultAssignmentSyntaxFacts?.source)
        assertEquals(span.startOffset, helper.source!!.startOffset)
        assertEquals(span.endOffset, helper.source!!.endOffset)
        recordExpandedRawFilesOnce(session.cfirProvider as CfirProviderImpl, result.recordableFiles, result.registry)
        assertSame(file, session.cfirProvider.getCfirCallableContainerFile(helper.symbol))
        assertNull(session.cfirProvider.getContainingClass(helper.symbol))
        session.runResolution(result.recordableFiles)
        assertEquals(CfirResolvePhase.BODY_RESOLVE, helper.resolvePhase)
        val returned = helper.body!!.statements.single() as CfirReturnExpression
        assertSame(helper, returned.target.labeledElement)
        assertNotSame(parameter.defaultValue, returned.result)
        val binding = ((returned.result as CfirQualifiedAccessExpression).calleeReference as CfirResolvedNamedReference).resolvedSymbol
        if (shadow) {
            val global = file.declarations.filterIsInstance<CfirPatternVariable>().single().pattern.bindingVariables().single()
            assertSame(global.symbol, binding)
        } else {
            assertSame(fresh.symbol, binding)
            val typeParameter = helper.typeParameters.single()
            assertEquals("T\$0", typeParameter.name.asString())
            assertNotSame(owner.typeParameters.single().symbol, typeParameter.symbol)
            assertSame(typeParameter.symbol, (helper.returnTypeRef.coneType as ConeTypeParameterType).lookupTag.typeParameterSymbol)
        }
        val graph = (helper.controlFlowGraphReference as CfirControlFlowGraphReferenceImpl).controlFlowGraph
        assertSame(helper, graph.declaration)
        assertTrue(graph.nodes.any { it.fir === returned })
        assertNotSame(owner.controlFlowGraphReference, helper.controlFlowGraphReference)
    }

    @Test fun psiDefaultHelpersS1PrecedingParameters() = assertS1PrecedingParameters(false)
    @Test fun lightTreeDefaultHelpersS1PrecedingParameters() = assertS1PrecedingParameters(true)
    @Test fun psiDefaultHelpersS1OwnGenericsAndBounds() = assertS1OwnGenericsAndBounds(false)
    @Test fun lightTreeDefaultHelpersS1OwnGenericsAndBounds() = assertS1OwnGenericsAndBounds(true)
    @Test fun psiDefaultHelpersS1ConstructorsAndReceivers() = assertS1ConstructorsAndReceivers(false)
    @Test fun lightTreeDefaultHelpersS1ConstructorsAndReceivers() = assertS1ConstructorsAndReceivers(true)
    @Test fun psiDefaultHelpersS1BorrowedLexicalGenerics() = assertS1BorrowedLexicalGenerics(false)
    @Test fun lightTreeDefaultHelpersS1BorrowedLexicalGenerics() = assertS1BorrowedLexicalGenerics(true)
    @Test fun psiDefaultHelpersS1NestedScopes() = assertS1NestedScopes(false)
    @Test fun lightTreeDefaultHelpersS1NestedScopes() = assertS1NestedScopes(true)

    /**
     * 固定官方 v113 14276740ffec1262b81b6b5236b1d840af270b37：
     * TypeChecker.cpp:2397–2568、Create.cpp:205–216 定义 fresh 参数、泛型及 helper 属主；
     * Collector.cpp:295–339、TypeCheckDecl.cpp:720–767 要求独立绑定和正文检查。
     * 四个专属 fixture 已由已有 SDK 证据指定的官方 cjc 1.1.3 编译确认语法合法。
     */
    private fun assertS1PrecedingParameters(useLightTree: Boolean) = withS1Source("precedingParameters", useLightTree) { session, file, resolve ->
        val owner = file.declarations.filterIsInstance<CfirNamedFunction>().single()
        val helpers = assertRawS1Helpers(session, file, owner, listOf(2, 3, 4))
        assertEquals(listOf("first.2", "second.3", "third.4"), helpers.map { it.name.asString() })
        assertTrue(helpers.all { it.typeParameters.isEmpty() })
        resolve()
        val referencedNames = listOf(listOf("seed"), listOf("seed", "first"), listOf("unused", "seed", "second", "first"))
        for ((helper, names) in helpers.zip(referencedNames)) {
            val returned = assertResolvedS1Helper(session, file, owner, helper)
            assertS1Bindings(returned.result, names.associateWith { name -> helper.valueParameters.single { it.name.asString() == name }.symbol })
            val index = helper.defaultParameterHelperInfo!!.parameterIndex
            assertS1Bindings(owner.valueParameters[index].defaultValue!!, names.associateWith { name -> owner.valueParameters.single { it.name.asString() == name }.symbol })
            val graph = (helper.controlFlowGraphReference as CfirControlFlowGraphReferenceImpl).controlFlowGraph
            assertTrue(s1Elements(returned.result).filterIsInstance<CfirQualifiedAccessExpression>()
                .filter { (it.calleeReference as? CfirNamedReference)?.name?.asString() in names }
                .all { access -> graph.nodes.any { it.fir === access } })
        }
        val thirdGraph = (helpers[2].controlFlowGraphReference as CfirControlFlowGraphReferenceImpl).controlFlowGraph
        assertTrue(thirdGraph.nodes.any { it.followingNodes.size > 1 }, "条件默认值必须建立真实分支 CFG")
    }

    private fun assertS1OwnGenericsAndBounds(useLightTree: Boolean) = withS1Source("ownGenerics", useLightTree) { session, file, resolve ->
        val owner = file.declarations.filterIsInstance<CfirNamedFunction>().single()
        val helpers = assertRawS1Helpers(session, file, owner, listOf(2, 3))
        assertFreshS1TypeParameters(owner, helpers)
        assertFreshS1Bounds(owner, helpers)
        resolve()
        for ((position, helper) in helpers.withIndex()) {
            val returned = assertResolvedS1Helper(session, file, owner, helper)
            val (t, u) = helper.typeParameters
            assertS1Bounds(session, file, t, linkedMapOf("Left" to u.symbol, "Marker" to null))
            assertS1Bounds(session, file, u, mapOf("Right" to t.symbol))
            for ((parameter, typeSymbol) in helper.valueParameters.zip(listOf(t.symbol, u.symbol, t.symbol))) {
                assertS1TypeParameter(parameter.returnTypeRef.coneType, typeSymbol)
            }
            val resultTypeParameter = helper.typeParameters[position]
            assertS1TypeParameter(helper.returnTypeRef.coneType, resultTypeParameter.symbol)
            assertS1TypeParameter(returned.result.coneTypeOrNull!!, resultTypeParameter.symbol)
            val name = if (position == 0) "first" else "second"
            assertS1Bindings(returned.result, mapOf(name to helper.valueParameters[position].symbol))
            assertS1Bindings(owner.valueParameters[position + 2].defaultValue!!, mapOf(name to owner.valueParameters[position].symbol))
            assertS1TypeParameter(owner.valueParameters[position + 2].defaultValue!!.coneTypeOrNull!!, owner.typeParameters[position].symbol)
        }
        assertS1Bounds(session, file, owner.typeParameters[0], linkedMapOf("Left" to owner.typeParameters[1].symbol, "Marker" to null))
        assertS1Bounds(session, file, owner.typeParameters[1], mapOf("Right" to owner.typeParameters[0].symbol))
    }

    private fun assertS1ConstructorsAndReceivers(useLightTree: Boolean) = withS1Source("constructors", useLightTree) { session, file, resolve ->
        val genericOwner = file.declarations.filterIsInstance<CfirClass>().single { it.name.asString() == "GenericOwner" }
        val plainOwner = file.declarations.filterIsInstance<CfirClass>().single { it.name.asString() == "PlainOwner" }
        val genericCtor = genericOwner.declarations.filterIsInstance<CfirConstructor>().single { it.valueParameters.size == 4 }
        val plainCtor = plainOwner.declarations.filterIsInstance<CfirConstructor>().single { it.valueParameters.size == 3 }
        assertFalse(genericCtor.status.isStatic)
        assertEquals(2, genericCtor.typeParameters.size)
        for ((borrowed, declared) in genericCtor.typeParameters.zip(genericOwner.typeParameters)) {
            assertTrue(borrowed is CfirConstructedClassTypeParameterRef)
            assertSame(declared.symbol, borrowed.symbol)
            assertSame(genericOwner.symbol, declared.containingDeclarationSymbol)
        }
        val genericHelpers = assertRawS1Helpers(session, file, genericCtor, listOf(2, 3), genericOwner, true)
        val plainHelper = assertRawS1Helpers(session, file, plainCtor, listOf(2), plainOwner, true).single()
        val genericMember = genericOwner.declarations.filterIsInstance<CfirNamedFunction>().single()
        val genericMemberHelper = assertRawS1Helpers(session, file, genericMember, listOf(1), genericOwner).single()
        val plainMembers = plainOwner.declarations.filterIsInstance<CfirNamedFunction>()
        assertEquals(setOf("member", "factory"), plainMembers.map { it.name.asString() }.toSet())
        val memberHelpers = plainMembers.associateWith { member ->
            assertRawS1Helpers(session, file, member, listOf(1), plainOwner, member.name.asString() == "factory").single()
        }
        assertTrue((genericHelpers + plainHelper + genericMemberHelper + memberHelpers.values).all { it.typeParameters.isEmpty() })
        resolve()
        for ((position, helper) in genericHelpers.withIndex()) {
            val returned = assertResolvedS1Helper(session, file, genericCtor, helper, genericOwner, true)
            assertS1TypeParameter(helper.returnTypeRef.coneType, genericOwner.typeParameters[position].symbol)
            assertS1TypeParameter(returned.result.coneTypeOrNull!!, genericOwner.typeParameters[position].symbol)
            val borrowedTypes = listOf(genericOwner.typeParameters[0].symbol, genericOwner.typeParameters[1].symbol, genericOwner.typeParameters[0].symbol)
            for ((parameter, typeSymbol) in helper.valueParameters.zip(borrowedTypes)) {
                assertS1TypeParameter(parameter.returnTypeRef.coneType, typeSymbol)
            }
            assertS1Bindings(returned.result, mapOf((if (position == 0) "first" else "second") to helper.valueParameters[position].symbol))
            assertNull(helper.dispatchReceiverType, "构造器默认值不能引入 this receiver")
        }
        assertS1Bindings(assertResolvedS1Helper(session, file, plainCtor, plainHelper, plainOwner, true).result,
            mapOf("seed" to plainHelper.valueParameters[0].symbol))
        assertNull(plainHelper.dispatchReceiverType)
        val genericReturned = assertResolvedS1Helper(session, file, genericMember, genericMemberHelper, genericOwner)
        assertS1TypeParameter(genericMemberHelper.returnTypeRef.coneType, genericOwner.typeParameters[0].symbol)
        assertS1Bindings(genericReturned.result, mapOf("seed" to genericMemberHelper.valueParameters[0].symbol))
        assertEquals(genericMember.dispatchReceiverType, genericMemberHelper.dispatchReceiverType)
        assertTrue(genericMemberHelper.dispatchReceiverType != null)
        for ((member, helper) in memberHelpers) {
            val isStatic = member.name.asString() == "factory"
            val returned = assertResolvedS1Helper(session, file, member, helper, plainOwner, isStatic)
            assertS1Bindings(returned.result, mapOf("seed" to helper.valueParameters[0].symbol))
            if (isStatic) assertNull(helper.dispatchReceiverType)
            else {
                assertTrue(helper.dispatchReceiverType != null)
                assertEquals(member.dispatchReceiverType, helper.dispatchReceiverType)
            }
        }
    }

    private fun assertS1BorrowedLexicalGenerics(useLightTree: Boolean) = withS1Source("lexicalScopes", useLightTree) { session, file, resolve ->
        val lexicalOwner = file.declarations.filterIsInstance<CfirClass>().single()
        val member = lexicalOwner.declarations.filterIsInstance<CfirNamedFunction>().single()
        val memberHelpers = assertRawS1Helpers(session, file, member, listOf(3, 4), lexicalOwner)
        assertFreshS1TypeParameters(member, memberHelpers)
        assertFreshS1Bounds(member, memberHelpers)
        val outer = file.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "outer" }
        val local = outer.body!!.statements.filterIsInstance<CfirNamedFunction>().single()
        val localHelpers = assertRawS1Helpers(session, file, local, listOf(2, 3))
        assertTrue(local.typeParameters.isEmpty())
        assertTrue(localHelpers.all { it.typeParameters.isEmpty() && it.isLocal })
        resolve()
        val ambient = lexicalOwner.typeParameters.single().symbol
        for ((position, helper) in memberHelpers.withIndex()) {
            val returned = assertResolvedS1Helper(session, file, member, helper, lexicalOwner)
            val (t, u) = helper.typeParameters
            for ((parameter, typeSymbol) in helper.valueParameters.zip(listOf(ambient, t.symbol, u.symbol, t.symbol))) {
                assertS1TypeParameter(parameter.returnTypeRef.coneType, typeSymbol)
            }
            assertS1Bounds(session, file, t, mapOf("Left" to ambient))
            assertS1Bounds(session, file, u, mapOf("Right" to t.symbol))
            assertS1TypeParameter(helper.returnTypeRef.coneType, helper.typeParameters[position].symbol)
            assertS1Bindings(returned.result, mapOf((if (position == 0) "first" else "second") to helper.valueParameters[position + 1].symbol))
            assertFalse(helper.typeParameters.any { it.symbol === ambient })
        }
        val captured = outer.body!!.statements.filterIsInstance<CfirPatternVariable>().single().pattern.bindingVariables().single()
        assertS1TypeParameter(captured.returnTypeRef.coneType, outer.typeParameters.single().symbol)
        for ((position, helper) in localHelpers.withIndex()) {
            val returned = assertResolvedS1Helper(session, file, local, helper)
            assertS1TypeParameter(helper.returnTypeRef.coneType, outer.typeParameters.single().symbol)
            assertS1TypeParameter(helper.valueParameters[0].returnTypeRef.coneType, outer.typeParameters.single().symbol)
            if (position == 0) assertS1Bindings(returned.result, mapOf("seed" to helper.valueParameters[0].symbol))
            else assertS1Bindings(returned.result, mapOf("captured" to captured.symbol))
        }
        assertSame(lexicalOwner.symbol, lexicalOwner.typeParameters.single().containingDeclarationSymbol)
        assertSame(outer.symbol, outer.typeParameters.single().containingDeclarationSymbol)
    }

    private fun assertS1NestedScopes(useLightTree: Boolean) = withS1Source("lexicalScopes", useLightTree) { session, file, resolve ->
        val owner = file.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "nested" }
        val helper = assertRawS1Helpers(session, file, owner, listOf(2)).single()
        assertFreshS1TypeParameters(owner, listOf(helper))
        val originalLambda = (owner.valueParameters[2].defaultValue as CfirAnonymousFunctionExpression).anonymousFunction
        val copiedLambda = ((helper.body!!.statements.single() as CfirReturnExpression).result as CfirAnonymousFunctionExpression).anonymousFunction
        val originalLocal = originalLambda.body!!.statements.filterIsInstance<CfirNamedFunction>().single()
        val copiedLocal = copiedLambda.body!!.statements.filterIsInstance<CfirNamedFunction>().single()
        assertNotSame(originalLambda, copiedLambda)
        assertNotSame(originalLambda.symbol, copiedLambda.symbol)
        assertNotSame(originalLambda.valueParameters.single().symbol, copiedLambda.valueParameters.single().symbol)
        assertNotSame(originalLocal, copiedLocal)
        assertNotSame(originalLocal.symbol, copiedLocal.symbol)
        assertNotSame(originalLocal.valueParameters.single().symbol, copiedLocal.valueParameters.single().symbol)
        resolve()
        assertResolvedS1Helper(session, file, owner, helper)
        val (t, u) = helper.typeParameters
        val functionType = helper.returnTypeRef.coneType as ConeFunctionType
        assertS1TypeParameter(helper.valueParameters[0].returnTypeRef.coneType, t.symbol)
        assertS1TypeParameter(helper.valueParameters[1].returnTypeRef.coneType, u.symbol)
        assertS1TypeParameter(functionType.parameterTypes.single(), t.symbol)
        assertS1TypeParameter(functionType.returnType, u.symbol)
        assertS1TypeParameter(copiedLambda.valueParameters.single().returnTypeRef.coneType, t.symbol)
        assertS1TypeParameter(copiedLambda.returnTypeRef.coneType, u.symbol)
        assertS1TypeParameter(copiedLocal.valueParameters.single().returnTypeRef.coneType, t.symbol)
        assertS1TypeParameter(copiedLocal.returnTypeRef.coneType, u.symbol)
        assertS1TypeParameter(originalLocal.valueParameters.single().returnTypeRef.coneType, owner.typeParameters[0].symbol)
        assertS1TypeParameter(originalLocal.returnTypeRef.coneType, owner.typeParameters[1].symbol)
        assertTrue(copiedLocal.typeParameters.isEmpty())
        val copiedReturn = copiedLocal.body!!.statements.single() as CfirReturnExpression
        val originalReturn = originalLocal.body!!.statements.single() as CfirReturnExpression
        assertSame(copiedLocal, copiedReturn.target.labeledElement)
        assertSame(originalLocal, originalReturn.target.labeledElement)
        assertS1Bindings(copiedReturn.result, mapOf("second" to helper.valueParameters[1].symbol))
        assertS1Bindings(originalReturn.result, mapOf("second" to owner.valueParameters[1].symbol))
        val copiedCall = s1Elements(copiedLambda.body!!).filterIsInstance<CfirFunctionCall>().single()
        val originalCall = s1Elements(originalLambda.body!!).filterIsInstance<CfirFunctionCall>().single()
        assertSame(copiedLocal.symbol, (copiedCall.calleeReference as CfirResolvedNamedReference).resolvedSymbol)
        assertSame(originalLocal.symbol, (originalCall.calleeReference as CfirResolvedNamedReference).resolvedSymbol)
        assertS1Bindings(copiedCall, mapOf("candidate" to copiedLambda.valueParameters.single().symbol))
        assertS1Bindings(originalCall, mapOf("candidate" to originalLambda.valueParameters.single().symbol))
        for ((original, copied) in listOf(originalLambda to copiedLambda, originalLocal to copiedLocal)) {
            assertEquals(CfirResolvePhase.BODY_RESOLVE, copied.resolvePhase)
            val graph = (copied.controlFlowGraphReference as CfirControlFlowGraphReferenceImpl).controlFlowGraph
            assertSame(copied, graph.declaration)
            assertNotSame(original.controlFlowGraphReference, copied.controlFlowGraphReference)
        }
        val localGraph = (copiedLocal.controlFlowGraphReference as CfirControlFlowGraphReferenceImpl).controlFlowGraph
        assertTrue(localGraph.nodes.any { it.fir === copiedReturn })
        assertTrue(localGraph.nodes.any { it.fir === copiedReturn.result })
        val lambdaGraph = (copiedLambda.controlFlowGraphReference as CfirControlFlowGraphReferenceImpl).controlFlowGraph
        assertTrue(lambdaGraph.nodes.any { it.fir === copiedCall })
    }

    /** 复用正式 construction/provider harness，在同一批真实声明上分开观察 RAW 与 BODY。 */
    private fun withS1Source(name: String, useLightTree: Boolean, assertions: (CfirSession, CfirFile, () -> Unit) -> Unit) {
        val text = Files.readString(Path.of("cfir/analysis-tests/testData/declarationOwner/defaultHelpersS1/$name.cj"), Charsets.UTF_8)
        val (session, result) = constructAndRegisterOwnerFile(name, text, useLightTree)
        assertions(session, result.recordableFiles.files.single()) { session.runResolution(result.recordableFiles) }
    }

    /** 检查全部前序参数及跨 helper 身份隔离，不按表达式实际使用集合裁剪签名。 */
    private fun assertRawS1Helpers(
        session: CfirSession, file: CfirFile, owner: CfirFunction, indices: List<Int>,
        nominalOwner: CfirClassLikeDeclaration? = null, isStatic: Boolean = false,
    ): List<CfirNamedFunction> {
        val helpers = indices.map { index ->
            val parameter = owner.valueParameters[index]
            val metadata = checkNotNull(parameter.defaultValueMetadata)
            assertTrue(metadata.hasInitial)
            val helper = checkNotNull(metadata.function)
            assertSame(owner, helper.defaultParameterHelperInfo!!.owner)
            assertEquals(index, helper.defaultParameterHelperInfo!!.parameterIndex)
            assertEquals("${parameter.name.asString()}.$index", helper.name.asString())
            assertEquals(CfirResolvePhase.RAW_CFIR, helper.resolvePhase)
            assertEquals(CfirDeclarationOrigin.Synthetic.DefaultParameterHelper, helper.origin)
            assertEquals(isStatic, helper.status.isStatic)
            assertEquals(Visibilities.Private, helper.status.visibility)
            assertTrue(helper.exportFacts.isCompilerAdded && helper.exportFacts.hasInitial && helper.exportFacts.isImplicitlyAdded && helper.exportFacts.hasNoReflectInfo)
            assertFalse(file.declarations.any { it === helper })
            if (nominalOwner != null) assertFalse(nominalOwner.declarations.any { it === helper })
            assertSame(file, session.cfirProvider.getCfirCallableContainerFile(helper.symbol))
            assertSame(nominalOwner?.symbol, session.cfirProvider.getContainingClass(helper.symbol))
            assertSame(owner.moduleData, helper.moduleData)
            assertNotSame(owner.symbol, helper.symbol)
            assertSame(helper, helper.symbol.cfir)
            assertNotSame(owner.body, helper.body)
            assertNotSame(parameter.returnTypeRef, helper.returnTypeRef)
            assertEquals(index, helper.valueParameters.size)
            for ((original, fresh) in owner.valueParameters.take(index).zip(helper.valueParameters)) {
                assertEquals(original.name, fresh.name)
                assertNotSame(original, fresh)
                assertNotSame(original.symbol, fresh.symbol)
                assertSame(original, original.symbol.cfir)
                assertSame(fresh, fresh.symbol.cfir)
                assertNotSame(original.attributes, fresh.attributes)
                assertNotSame(original.returnTypeRef, fresh.returnTypeRef)
                assertSame(helper.symbol, fresh.containingDeclarationSymbol)
                assertFalse(fresh.isNamed)
                assertFalse(fresh.isMemberParameter)
                assertNull(fresh.defaultValue)
                assertNull(fresh.defaultValueMetadata)
                assertFalse(fresh.exportFacts.hasInitial)
            }
            val returned = helper.body!!.statements.single() as CfirReturnExpression
            assertSame(helper, returned.target.labeledElement)
            assertNotSame(parameter.defaultValue, returned.result)
            val originalExpressions = s1Elements(parameter.defaultValue!!).filterIsInstance<CfirExpression>()
            val copiedExpressions = s1Elements(returned.result).filterIsInstance<CfirExpression>()
            assertTrue(copiedExpressions.none { copied -> originalExpressions.any { it === copied } }, "默认表达式的 owned 子图必须独立复制")
            val span = checkNotNull(parameter.defaultAssignmentSyntaxFacts?.source)
            assertEquals(span.startOffset, helper.source!!.startOffset)
            assertEquals(span.endOffset, helper.source!!.endOffset)
            helper
        }
        for ((position, helper) in helpers.withIndex()) {
            for (other in helpers.drop(position + 1)) {
                assertNotSame(helper.symbol, other.symbol)
                assertNotSame(helper.body, other.body)
                for ((first, second) in helper.valueParameters.zip(other.valueParameters)) {
                    assertNotSame(first, second)
                    assertNotSame(first.symbol, second.symbol)
                }
            }
        }
        return helpers
    }

    private fun assertResolvedS1Helper(
        session: CfirSession, file: CfirFile, owner: CfirFunction, helper: CfirNamedFunction,
        nominalOwner: CfirClassLikeDeclaration? = null, isStatic: Boolean = false,
    ): CfirReturnExpression {
        assertEquals(CfirResolvePhase.BODY_RESOLVE, helper.resolvePhase)
        assertTrue(helper.status is CfirResolvedDeclarationStatus)
        assertEquals(isStatic, helper.status.isStatic)
        assertEquals(Visibilities.Private, helper.status.visibility)
        assertTrue(helper.exportFacts.isCompilerAdded && helper.exportFacts.hasInitial && helper.exportFacts.isImplicitlyAdded && helper.exportFacts.hasNoReflectInfo)
        assertSame(owner, helper.defaultParameterHelperInfo!!.owner)
        val parameter = owner.valueParameters[helper.defaultParameterHelperInfo!!.parameterIndex]
        assertTrue(parameter.defaultValueMetadata!!.hasInitial)
        assertSame(helper, parameter.defaultValueMetadata!!.function)
        assertSame(file, session.cfirProvider.getCfirCallableContainerFile(helper.symbol))
        assertSame(nominalOwner?.symbol, session.cfirProvider.getContainingClass(helper.symbol))
        val returned = helper.body!!.statements.single() as CfirReturnExpression
        assertSame(helper, returned.target.labeledElement)
        assertTrue(returned.coneTypeOrNull!!.isNothing)
        val expectedType = helper.returnTypeRef.coneType
        val resultType = checkNotNull(returned.result.coneTypeOrNull)
        if (expectedType is ConeFunctionType) {
            // lambda 的闭包属性独立于函数签名，签名内的类型参数身份仍必须完全一致。
            val functionResult = resultType as ConeFunctionType
            assertEquals(expectedType.parameterTypes, functionResult.parameterTypes)
            assertEquals(expectedType.returnType, functionResult.returnType)
        } else {
            assertEquals(expectedType, resultType)
        }
        assertNotSame(parameter.defaultValue, returned.result)
        val graph = (helper.controlFlowGraphReference as CfirControlFlowGraphReferenceImpl).controlFlowGraph
        assertSame(helper, graph.declaration)
        assertTrue(graph.nodes.any { it.fir === returned })
        assertNotSame(owner.controlFlowGraphReference, helper.controlFlowGraphReference)
        assertNotSame((owner.controlFlowGraphReference as CfirControlFlowGraphReferenceImpl).controlFlowGraph, graph)
        return returned
    }

    private fun assertFreshS1TypeParameters(owner: CfirNamedFunction, helpers: List<CfirNamedFunction>) {
        assertEquals(listOf("T", "U"), owner.typeParameters.map { it.name.asString() })
        for (helper in helpers) {
            assertEquals(listOf("T\$0", "U\$1"), helper.typeParameters.map { it.name.asString() })
            for ((original, fresh) in owner.typeParameters.zip(helper.typeParameters)) {
                assertNotSame(original, fresh)
                assertNotSame(original.symbol, fresh.symbol)
                assertSame(original, original.symbol.cfir)
                assertSame(fresh, fresh.symbol.cfir)
                assertSame(owner.symbol, original.containingDeclarationSymbol)
                assertSame(helper.symbol, fresh.containingDeclarationSymbol)
            }
        }
        for ((position, helper) in helpers.withIndex()) {
            for (other in helpers.drop(position + 1)) {
                for ((first, second) in helper.typeParameters.zip(other.typeParameters)) assertNotSame(first.symbol, second.symbol)
            }
        }
    }

    /** 仅对 fixture 明确声明的约束检查复制身份；隐式 Any 上界不是官方源码约束节点。 */
    private fun assertFreshS1Bounds(owner: CfirNamedFunction, helpers: List<CfirNamedFunction>) {
        for (helper in helpers) {
            for ((original, fresh) in owner.typeParameters.zip(helper.typeParameters)) {
                assertEquals(original.bounds.size, fresh.bounds.size)
                for ((originalBound, freshBound) in original.bounds.zip(fresh.bounds)) assertNotSame(originalBound, freshBound)
            }
        }
    }

    private fun assertS1TypeParameter(type: ConeCangJieType, expected: CfirTypeParameterSymbol) {
        assertSame(expected, (type as ConeTypeParameterType).lookupTag.typeParameterSymbol)
    }

    /** 上界的分类器绑定、类型实参身份和多重约束均从实际解析图核验。 */
    private fun assertS1Bounds(session: CfirSession, file: CfirFile, parameter: CfirTypeParameter, expected: Map<String, CfirTypeParameterSymbol?>) {
        val bounds = parameter.bounds.map { it.coneType as ConeClassLikeType }
        assertEquals(expected.keys.toList(), bounds.map { it.lookupTag.classId.shortClassName.asString() })
        for ((name, argument) in expected) {
            val bound = bounds.single { it.lookupTag.classId.shortClassName.asString() == name }
            val declaration = file.declarations.filterIsInstance<CfirInterface>().single { it.name.asString() == name }
            assertSame(declaration.symbol, session.symbolProvider.getClassLikeSymbolByClassId(bound.lookupTag.classId))
            if (argument == null) assertTrue(bound.typeArguments.isEmpty())
            else assertS1TypeParameter(bound.typeArguments.single().type, argument)
        }
    }

    private fun assertS1Bindings(expression: CfirExpression, expected: Map<String, CfirBasedSymbol<*>>) {
        val accesses = s1Elements(expression).filterIsInstance<CfirQualifiedAccessExpression>()
        for ((name, symbol) in expected) {
            val references = accesses.map { it.calleeReference }.filter { (it as? CfirNamedReference)?.name?.asString() == name }
            assertTrue(references.isNotEmpty(), "必须实际解析引用 $name")
            for (reference in references) assertSame(symbol, (reference as CfirResolvedNamedReference).resolvedSymbol, name)
        }
    }

    /** 只读取普通表达式拥有树，避免把 CFG 或 helper metadata 边当成新语法声明。 */
    private fun s1Elements(root: CfirElement): List<CfirElement> {
        val elements = mutableListOf<CfirElement>()
        root.accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                elements += element
                element.acceptChildren(this)
            }
        })
        return elements
    }

    @Test fun psiDefaultHelpersPhaseLateSignatureBodyGraphPhase() = assertLateSignatureBodyGraphPhase(false)
    @Test fun lightTreeDefaultHelpersPhaseLateSignatureBodyGraphPhase() = assertLateSignatureBodyGraphPhase(true)
    @Test fun psiDefaultHelpersPhaseCommittedOblPclaLambdas() = assertCommittedOblPclaLambdas(false)
    @Test fun lightTreeDefaultHelpersPhaseCommittedOblPclaLambdas() = assertCommittedOblPclaLambdas(true)

    /**
     * 官方 v113 14276740ffec1262b81b6b5236b1d840af270b37：LambdaExpr.cpp:260–310
     * 推断无注解参数及返回类型，:314–380 在目标函数类型下重新检查；TypeCheckCall.cpp:
     * 1675–1690/2021–2030 在选定目标后重新推断实参。fixture 已由官方 cjc 1.1.3 验证。
     * CFIR 的完成阶段只在真实 runResolution 后核验，不调用 phase publisher 或注入 CFG。
     */
    private fun assertLateSignatureBodyGraphPhase(useLightTree: Boolean) =
        withPhaseCompletionSource("lateSignatures", useLightTree) { session, file, resolve ->
            val root = file.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "lateSignature" }
            val rawFunctions = assertRawPhaseCompletionTree(root, setOf("lateSignature", "lateLocal", "copy"))
            resolve()
            assertFinalSignatureBodyGraphPhase(root, rawFunctions, mapOf("seed" to ConePrimitiveType.INT64, "value" to ConePrimitiveType.INT64, "extra" to ConePrimitiveType.INT64))
            val functions = phaseFunctionDeclarations(root).filterIsInstance<CfirNamedFunction>()
            val lateLocal = functions.single { it.name.asString() == "lateLocal" }
            val copy = functions.single { it.name.asString() == "copy" }
            val lambdas = phaseFunctionDeclarations(root).filterIsInstance<CfirAnonymousFunction>()
            val outerLambda = lambdas.single { it.valueParameters.single().name.asString() == "value" }
            val innerLambda = lambdas.single { it.valueParameters.single().name.asString() == "extra" }
            val apply = file.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "apply" }
            val inner = phaseLocalBinding(root, "inner")
            assertS1Bindings(root.body!!, mapOf("apply" to apply.symbol, "lateLocal" to lateLocal.symbol, "copy" to copy.symbol, "inner" to inner.symbol,
                "seed" to root.valueParameters.single().symbol, "value" to outerLambda.valueParameters.single().symbol, "extra" to innerLambda.valueParameters.single().symbol))
            assertFinalPhaseLambdaType(inner.returnTypeRef.coneType, ConePrimitiveType.INT64)
            val applyCall = phaseIdentityElements(lateLocal.body!!).filterIsInstance<CfirFunctionCall>()
                .single { (it.calleeReference as CfirNamedReference).name.asString() == "apply" }
            assertSame(apply.symbol, (applyCall.calleeReference as CfirResolvedNamedReference).resolvedSymbol)
            assertEquals(ConePrimitiveType.INT64, applyCall.coneTypeOrNull)
            // Provider 的声明索引不登记普通 local；局部来源由正式语义图的文件归属产品拥有。
            val sourceFacts = CfirExportAnalysis.analyze(listOf(file))
            assertSame(file, sourceFacts.sourceFileOf(lateLocal))
            assertSame(file, sourceFacts.sourceFileOf(copy))
        }

    private fun assertCommittedOblPclaLambdas(useLightTree: Boolean) =
        withPhaseCompletionSource("committedLambdaTargets", useLightTree) { session, file, resolve ->
            val overloadRoot = file.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "overloadCommitted" }
            val pclaRoot = file.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "pclaCommitted" }
            val rawObl = assertRawPhaseCompletionTree(overloadRoot, setOf("overloadCommitted", "lateLocal", "add"))
            val rawPcla = assertRawPhaseCompletionTree(pclaRoot, setOf("pclaCommitted", "read", "add"))
            resolve()
            val numberClass = file.declarations.filterIsInstance<CfirClass>().single { it.name.asString() == "NumberInput" }
            val textClass = file.declarations.filterIsInstance<CfirClass>().single { it.name.asString() == "TextInput" }
            val choices = file.declarations.filterIsInstance<CfirNamedFunction>().filter { it.name.asString() == "choose" }
            assertEquals(2, choices.size)
            val selectedChoice = choices.single { ((it.valueParameters.single().returnTypeRef.coneType as ConeFunctionType).parameterTypes.single() as ConeClassLikeType).lookupTag.classId == numberClass.symbol.classId }
            val rejectedChoice = choices.single { ((it.valueParameters.single().returnTypeRef.coneType as ConeFunctionType).parameterTypes.single() as ConeClassLikeType).lookupTag.classId == textClass.symbol.classId }
            val expectedInput = (selectedChoice.valueParameters.single().returnTypeRef.coneType as ConeFunctionType).parameterTypes.single()
            assertFinalSignatureBodyGraphPhase(overloadRoot, rawObl, mapOf("item" to expectedInput, "extra" to ConePrimitiveType.INT64))
            assertFinalSignatureBodyGraphPhase(pclaRoot, rawPcla, mapOf("value" to ConePrimitiveType.INT64, "extra" to ConePrimitiveType.INT64))
            val chooseCall = phaseIdentityElements(overloadRoot.body!!).filterIsInstance<CfirFunctionCall>()
                .single { (it.calleeReference as CfirNamedReference).name.asString() == "choose" }
            assertSame(selectedChoice.symbol, (chooseCall.calleeReference as CfirResolvedNamedReference).resolvedSymbol)
            assertNotSame(rejectedChoice.symbol, (chooseCall.calleeReference as CfirResolvedNamedReference).resolvedSymbol)
            assertEquals(ConePrimitiveType.INT64, chooseCall.coneTypeOrNull)
            val oblLambdas = phaseFunctionDeclarations(overloadRoot).filterIsInstance<CfirAnonymousFunction>()
            val oblLambda = oblLambdas.single { it.valueParameters.single().name.asString() == "item" }
            val oblInner = oblLambdas.single { it.valueParameters.single().name.asString() == "extra" }
            val valueField = numberClass.declarations.filterIsInstance<CfirFieldVariable>().single { it.name.asString() == "value" }
            val selected = phaseLocalBinding(overloadRoot, "selected")
            val oblFunctions = phaseFunctionDeclarations(overloadRoot).filterIsInstance<CfirNamedFunction>()
            assertS1Bindings(overloadRoot.body!!, mapOf("item" to oblLambda.valueParameters.single().symbol, "value" to valueField.symbol,
                "selected" to selected.symbol, "lateLocal" to oblFunctions.single { it.name.asString() == "lateLocal" }.symbol,
                "add" to oblFunctions.single { it.name.asString() == "add" }.symbol,
                "extra" to oblInner.valueParameters.single().symbol, "inner" to phaseLocalBinding(overloadRoot, "inner").symbol))
            assertSame(numberClass.symbol, session.cfirProvider.getContainingClass(valueField.symbol))

            val infer = file.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "infer" }
            val asNumber = file.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "asNumber" }
            val inferCall = phaseIdentityElements(pclaRoot).filterIsInstance<CfirFunctionCall>()
                .single { (it.calleeReference as CfirNamedReference).name.asString() == "infer" }
            assertSame(infer.symbol, (inferCall.calleeReference as CfirResolvedNamedReference).resolvedSymbol)
            assertFinalPhaseLambdaType(inferCall.coneTypeOrNull!!, ConePrimitiveType.INT64)
            val inferred = phaseLocalBinding(pclaRoot, "inferred")
            assertFinalPhaseLambdaType(inferred.returnTypeRef.coneType, ConePrimitiveType.INT64)
            val pclaLambdas = phaseFunctionDeclarations(pclaRoot).filterIsInstance<CfirAnonymousFunction>()
            val pclaLambda = pclaLambdas.single { it.valueParameters.single().name.asString() == "value" }
            val pclaInner = pclaLambdas.single { it.valueParameters.single().name.asString() == "extra" }
            val pclaFunctions = phaseFunctionDeclarations(pclaRoot).filterIsInstance<CfirNamedFunction>()
            assertS1Bindings(pclaRoot.body!!, mapOf("infer" to infer.symbol, "asNumber" to asNumber.symbol, "inferred" to inferred.symbol,
                "read" to pclaFunctions.single { it.name.asString() == "read" }.symbol,
                "add" to pclaFunctions.single { it.name.asString() == "add" }.symbol,
                "value" to pclaLambda.valueParameters.single().symbol, "extra" to pclaInner.valueParameters.single().symbol,
                "inner" to phaseLocalBinding(pclaRoot, "inner").symbol))
        }

    private fun withPhaseCompletionSource(name: String, useLightTree: Boolean, assertions: (CfirSession, CfirFile, () -> Unit) -> Unit) {
        val path = Path.of("cfir/analysis-tests/testData/declarationOwner/defaultHelpersPhase/$name.cj")
        val (session, result) = constructAndRegisterOwnerFile(name, Files.readString(path, Charsets.UTF_8), useLightTree)
        assertions(session, result.recordableFiles.files.single()) { session.runResolution(result.recordableFiles) }
    }

    /** 明确从未完成的源码签名开始，避免完整签名正例绕过本次晚完成边界。 */
    private fun assertRawPhaseCompletionTree(root: CfirNamedFunction, expectedNames: Set<String>): List<CfirFunction> {
        val functions = phaseFunctionDeclarations(root)
        assertEquals(5, functions.size)
        assertEquals(expectedNames, functions.filterIsInstance<CfirNamedFunction>().map { it.name.asString() }.toSet())
        val lambdas = functions.filterIsInstance<CfirAnonymousFunction>()
        assertEquals(2, lambdas.size)
        for (function in functions) {
            assertEquals(CfirResolvePhase.RAW_CFIR, function.resolvePhase)
            assertNull(function.controlFlowGraphReference)
            if (function is CfirNamedFunction) assertTrue(function.returnTypeRef is CfirImplicitTypeRef)
            else for (parameter in function.valueParameters) assertTrue(parameter.returnTypeRef is CfirImplicitTypeRef)
        }
        return functions
    }

    /** 最终 Signature/Body/Graph/Phase 必须一起完成，且每个 nested 声明都拥有自己的真实图。 */
    private fun assertFinalSignatureBodyGraphPhase(root: CfirNamedFunction, rawFunctions: List<CfirFunction>, expectedInputs: Map<String, ConeCangJieType>) {
        val functions = phaseFunctionDeclarations(root)
        assertEquals(rawFunctions.size, functions.size)
        for (function in functions) {
            assertSame(rawFunctions.single { it.symbol === function.symbol }, function)
            assertSame(function, function.symbol.cfir)
            assertEquals(
                "${function::class.simpleName}:${(function as? CfirNamedFunction)?.name ?: function.valueParameters.map { it.name }} " +
                    "return=${function.returnTypeRef.coneTypeOrNull}, body=${function.body?.coneTypeOrNull}, cfg=${function.controlFlowGraphReference != null}",
                CfirResolvePhase.BODY_RESOLVE, function.resolvePhase,
            )
            assertEquals(ConePrimitiveType.INT64, function.returnTypeRef.coneType)
            for (parameter in function.valueParameters) {
                assertEquals(expectedInputs.getValue(parameter.name.asString()), parameter.returnTypeRef.coneType)
                assertSame(function.symbol, parameter.containingDeclarationSymbol)
                assertSame(parameter, parameter.symbol.cfir)
            }
            val body = checkNotNull(function.body)
            assertTrue(body.coneTypeOrNull != null)
            assertFalse(body.coneTypeOrNull is ConeErrorType, "合法源码的最终正文不能保留错误类型")
            val graph = (function.controlFlowGraphReference as CfirControlFlowGraphReferenceImpl).controlFlowGraph
            assertSame(function, graph.declaration)
            assertTrue(graph.nodes.isNotEmpty())
            val last = body.statements.last() as CfirExpression
            val tail = if (last is CfirReturnExpression) last.result else last
            assertEquals(ConePrimitiveType.INT64, tail.coneTypeOrNull)
            assertTrue(graph.nodes.any { it.fir === tail || it.fir === last }, "最终尾表达式必须来自此声明的真实 CFG")
            val ownedReturns = phaseOwnBodyElements(body).filterIsInstance<CfirReturnExpression>()
            for (returned in ownedReturns) {
                assertSame(function, returned.target.labeledElement)
                assertTrue(returned.coneTypeOrNull!!.isNothing)
                assertEquals(ConePrimitiveType.INT64, returned.result.coneTypeOrNull)
            }
            if (function is CfirAnonymousFunction) {
                assertFinalPhaseLambdaType(function.typeRef.coneType, function.valueParameters.single().returnTypeRef.coneType)
                assertTrue(ownedReturns.isNotEmpty(), "Normal results writer 必须完成 lambda 的末项 Return 写回")
            }
            for (other in functions.filter { it !== function }) assertNotSame(function.controlFlowGraphReference, other.controlFlowGraphReference)
        }
    }

    /** 简单 binding 与语法模式壳共享 initializer；函数声明域按真实对象身份计数，不重复算别名。 */
    private fun phaseFunctionDeclarations(root: CfirElement): List<CfirFunction> {
        return phaseIdentityElements(root).filterIsInstance<CfirFunction>()
    }

    private fun phaseIdentityElements(root: CfirElement): List<CfirElement> {
        val identities = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<CfirElement, Boolean>())
        return s1Elements(root).filter(identities::add)
    }

    private fun assertFinalPhaseLambdaType(type: ConeCangJieType, input: ConeCangJieType) {
        val functionType = type as ConeFunctionType
        assertEquals(listOf(input), functionType.parameterTypes)
        assertEquals(ConePrimitiveType.INT64, functionType.returnType)
    }

    private fun phaseLocalBinding(root: CfirFunction, name: String) =
        phaseIdentityElements(root).filterIsInstance<CfirPatternVariable>().flatMap { it.pattern.bindingVariables() }.single { it.name.asString() == name }

    private fun phaseOwnBodyElements(root: CfirElement): List<CfirElement> {
        val elements = mutableListOf<CfirElement>()
        val identities = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<CfirElement, Boolean>())
        root.accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (element is CfirFunction || !identities.add(element)) return
                elements += element
                element.acceptChildren(this)
            }
        })
        return elements
    }

    private fun buildAndRegisterFile(name: String, text: String, useLightTree: Boolean): CfirFile =
        constructAndRegisterOwnerFile(name, text, useLightTree).second.recordableFiles.files.single()

    private fun constructAndRegisterOwnerFile(name: String, text: String, useLightTree: Boolean): Pair<CfirSession, MacroConstructionResult.Success> {
        val session = createResolveSession("$name.cj")
        val psiFile = createCjFile(name, text)
        val syntaxErrors = PsiTreeUtil.findChildrenOfType(psiFile, PsiErrorElement::class.java)
        assertTrue(syntaxErrors.isEmpty(), syntaxErrors.joinToString { it.errorDescription })
        val built = if (useLightTree) {
            LightTree2Cfir(session, session.cangjieScopeProvider).buildPreMacroCfirFile(
                parseLightFile(text),
                CjInMemoryTextSourceFile("$name.cj", null, text),
                text.toSourceLinesMapping(),
            )
        } else {
            PsiRawCfirBuilder(session, annotationMetadata = CfirAnnotationMetadataBuilder())
                .buildPreMacroCfirFile(psiFile)
        }
        val pre = buildPreMacroRawFiles(session, listOf(built))
        val result = MacroConstructionService.Identity.expandWithDefaultContext(
            pre = pre,
            mode = MacroConstructionService.Mode.STRICT,
        ) as MacroConstructionResult.Success
        recordExpandedRawFilesOnce(session.cfirProvider as CfirProviderImpl, result.recordableFiles, result.registry)
        return session to result
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

    private fun parseLightFile(source: String): org.cangnova.cangjie.parsing.ParsedCangJieLightFile {
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
