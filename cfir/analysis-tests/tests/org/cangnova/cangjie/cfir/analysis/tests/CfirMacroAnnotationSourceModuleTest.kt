package org.cangnova.cangjie.cfir.analysis.tests

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.annotations.CangjieAnnotationIdentity
import org.cangnova.cangjie.cfir.DependencyListForCliModule
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildFileCopy
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.diagnostics.CjRegisteredDiagnosticFactoriesStorage
import org.cangnova.cangjie.cfir.entrypoint.configuration.diagnosticFactoriesStorage
import org.cangnova.cangjie.cfir.entrypoint.session.CfirDefaultSessionFactory
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationResolveState
import org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir
import org.cangnova.cangjie.cfir.lightTree.LightTreeRawCfirDeclarationBuilder
import org.cangnova.cangjie.cfir.pipeline.CfirSessionConstructionUtils
import org.cangnova.cangjie.cfir.pipeline.CfirSessionProducer
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildPreMacroRawFiles
import org.cangnova.cangjie.cfir.resolve.providers.macro.expandWithDefaultContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.recordExpandedRawFilesOnce
import org.cangnova.cangjie.cfir.resolve.transformers.CfirFileReplacingResolveProcessor
import org.cangnova.cangjie.cfir.resolve.transformers.CfirGlobalResolveProcessor
import org.cangnova.cangjie.cfir.resolve.transformers.CfirTransformerBasedResolveProcessor
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.phaseResolverRegistry
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * 普通宏的新 token 使用空 parser 模块，符号仍属于宿主包。
 * Raw provenance 必须穿过片段收集与 TYPES，不能根据宿主 std 包重新授予 ConstSafe 内置身份。
 */
@OptIn(CompilerConfiguration.Internals::class)
class CfirMacroAnnotationSourceModuleTest : AbstractCfirAnalysisTestCase() {
    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    @Test
    fun psiMacroTokensKeepTheirSourceModuleThroughTypes() {
        assertSourceModuleContract(useLightTree = false)
    }

    @Test
    fun lightTreeMacroTokensKeepTheirSourceModuleThroughTypes() {
        assertSourceModuleContract(useLightTree = true)
    }

    private fun assertSourceModuleContract(useLightTree: Boolean) {
        for (context in SOURCE_CONTEXTS) {
            val session = createResolveSession()
            val sourceText = "${context.packageHeader}\n@ConstSafe\nfunc direct(): Unit {}"
            val sourcePsi = createCjFile("annotationSource", sourceText)
            val hostFile: CfirFile
            val sourceSurfaces: List<MacroSurface>
            val generated: CfirNamedFunction
            val generatedSurfaces: List<MacroSurface>
            if (useLightTree) {
                val built = LightTree2Cfir(session, session.cangjieScopeProvider).buildCfirFileWithSurfaces(
                    parseLightTree(sourceText),
                    CjInMemoryTextSourceFile("annotationSource.cj", null, sourceText),
                    sourceText.toSourceLinesMapping(),
                )
                hostFile = built.first
                sourceSurfaces = built.second
                val generatedTree = parseLightTree(GENERATED_TOKENS)
                val builder = LightTreeRawCfirDeclarationBuilder(
                    session = session,
                    baseScopeProvider = session.cangjieScopeProvider,
                    tree = generatedTree,
                    source = GENERATED_TOKENS,
                )
                generated = checkNotNull(builder.buildDeclarationFragmentInPackage(
                    generatedTree.root, context.packageFqName,
                )) as CfirNamedFunction
                generatedSurfaces = builder.consumeCollectedMacroSurfaces()
            } else {
                val sourceBuilder = PsiRawCfirBuilder(session)
                hostFile = sourceBuilder.buildCfirFile(sourcePsi)
                sourceSurfaces = sourceBuilder.consumeCollectedMacroSurfaces()
                val generatedPsi = CjPsiFactory.forMacroExpansion(project, sourcePsi)
                    .createFile("generated.cj", GENERATED_TOKENS)
                val builder = PsiRawCfirBuilder(session)
                generated = checkNotNull(builder.buildDeclarationFragmentInPackage(
                    generatedPsi, context.packageFqName,
                )) as CfirNamedFunction
                generatedSurfaces = builder.consumeCollectedMacroSurfaces()
            }

            assertEquals(context.packageFqName, hostFile.packageDirective.packageFqName)
            val direct = hostFile.declarations.filterIsInstance<CfirNamedFunction>().single()
            assertEquals("direct", direct.name.asString())
            assertEquals("generated", generated.name.asString())
            assertEquals(context.packageFqName, direct.symbol.callableId.packageName)
            assertEquals(context.packageFqName, generated.symbol.callableId.packageName)
            assertEquals(context.sourceModuleName, annotation(direct).sourceModuleName)
            assertEquals("", annotation(generated).sourceModuleName)
            assertEquals(context.sourceModuleName, sourceSurfaces.single().scopeContext.sourceModuleName)
            assertEquals("", generatedSurfaces.single().scopeContext.sourceModuleName)

            // 先完成 construction，再登记同一个宿主文件，保持 provider 与 resolve 的正常边界。
            val combined = buildFileCopy(hostFile) {
                symbol = CfirFileSymbol()
                declarations.add(generated)
            }
            val pre = buildPreMacroRawFiles(
                session,
                listOf(combined),
                listOf(sourceSurfaces + generatedSurfaces),
            )
            val result = MacroConstructionService.Identity.expandWithDefaultContext(
                pre = pre,
                mode = MacroConstructionService.Mode.STRICT,
            ) as MacroConstructionResult.Success
            recordExpandedRawFilesOnce(
                session.cfirProvider as CfirProviderImpl,
                result.recordableFiles,
                result.registry,
            )
            val resolved = resolveThroughTypes(combined, session)
            val functions = resolved.declarations.filterIsInstance<CfirNamedFunction>().associateBy { it.name.asString() }
            val resolvedDirect = checkNotNull(functions["direct"])
            val resolvedGenerated = checkNotNull(functions["generated"])
            assertEquals(CfirResolvePhase.TYPES, resolvedDirect.resolvePhase)
            assertEquals(CfirResolvePhase.TYPES, resolvedGenerated.resolvePhase)
            assertEquals(context.sourceModuleName, annotation(resolvedDirect).sourceModuleName)
            if (context.sourceModuleName == "std") {
                assertEquals(BuiltInAnnotationKind.CONSTSAFE, annotation(resolvedDirect).annotationKind)
                assertTrue(annotation(resolvedDirect).annotationIdentity is CangjieAnnotationIdentity.LanguageBuiltIn)
            } else {
                assertNull(annotation(resolvedDirect).annotationKind)
                assertFalse(annotation(resolvedDirect).annotationIdentity is CangjieAnnotationIdentity.LanguageBuiltIn)
            }
            val generatedAnnotation = annotation(resolvedGenerated)
            assertEquals("", generatedAnnotation.sourceModuleName)
            assertNull(generatedAnnotation.annotationKind)
            assertFalse(generatedAnnotation.annotationIdentity is CangjieAnnotationIdentity.LanguageBuiltIn)
            assertTrue(generatedAnnotation.annotationResolveState != CfirAnnotationResolveState.UNRESOLVED)
        }
    }

    /** 与 frontend 使用同一工厂完整创建 shared/library/source 会话，不逐项拼装解析组件。 */
    private fun createResolveSession(): CfirSession {
        val moduleName = Name.special("<annotation-source-module-test>")
        val configuration = CompilerConfiguration().apply {
            diagnosticFactoriesStorage = CjRegisteredDiagnosticFactoriesStorage()
        }
        val dependencies = DependencyListForCliModule.build(moduleName)
        val factory = CfirDefaultSessionFactory()
        return CfirSessionConstructionUtils.prepareSessions(
            files = listOf("annotationSource.cj"),
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

    /** 使用正式阶段处理器停在 TYPES，防止后续阶段掩盖类型身份解析结果。 */
    private fun resolveThroughTypes(file: CfirFile, session: CfirSession): CfirFile {
        var files = listOf(file)
        for (phase in listOf(CfirResolvePhase.IMPORTS, CfirResolvePhase.SUPER_TYPES, CfirResolvePhase.TYPES)) {
            val processor = checkNotNull(session.phaseResolverRegistry.getProcessor(phase))
            processor.beforePhase()
            try {
                when (processor) {
                    is CfirFileReplacingResolveProcessor -> files = processor.processAndReplace(files)
                    is CfirGlobalResolveProcessor -> processor.process(files)
                    is CfirTransformerBasedResolveProcessor -> files.forEach(processor::processFile)
                }
            } finally {
                processor.afterPhase()
            }
        }
        return files.single()
    }

    private fun annotation(function: CfirNamedFunction): CfirAnnotationCall =
        function.annotations.single() as CfirAnnotationCall

    private fun parseLightTree(source: String): FlyweightCapableTreeStructure<LighterASTNode> {
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        return CangJieLightParser.parse(builder, languageModuleName = "")
    }

    private data class SourceContext(
        val packageHeader: String,
        val packageFqName: FqName,
        val sourceModuleName: String,
    )

    companion object {
        private const val GENERATED_TOKENS = "@ConstSafe\nfunc generated(): Unit {}"
        private val SOURCE_CONTEXTS = listOf(
            SourceContext("package std.foo", FqName("std.foo"), "std"),
            SourceContext("package std::foo", FqName("foo"), "std"),
            SourceContext("package org::std.foo", FqName("std.foo"), "org"),
            SourceContext("package std", FqName("std"), ""),
            SourceContext("", FqName.ROOT, ""),
        )
    }
}
