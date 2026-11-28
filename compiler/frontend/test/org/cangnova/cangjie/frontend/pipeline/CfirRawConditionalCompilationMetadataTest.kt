package org.cangnova.cangjie.frontend.pipeline

import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.cfir.DependencyListForCliModule
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.builder.toParsingContext
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.diagnostics.CjRegisteredDiagnosticFactoriesStorage
import org.cangnova.cangjie.cfir.entrypoint.configuration.conditionalCompilationSettings
import org.cangnova.cangjie.cfir.entrypoint.configuration.diagnosticFactoriesStorage
import org.cangnova.cangjie.cfir.entrypoint.session.CfirDefaultSessionFactory
import org.cangnova.cangjie.cfir.entrypoint.session.createDefaultCfirSessionFactoryContext
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir
import org.cangnova.cangjie.cfir.pipeline.CfirSessionConstructionUtils
import org.cangnova.cangjie.cfir.pipeline.CfirSessionProducer
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroCfirFile
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildPreMacroRawFiles
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ExplicitCfirConditionalCompilationSettings
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.session.ensureAnnotationMetadataRegistry
import org.cangnova.cangjie.cfir.session.conditionalCompilationSettingsOrNull
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.parsing.collectParsingDiagnostics
import org.cangnova.cangjie.parsing.cjmpParsingContext
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 条件裁剪必须同时维护真实 raw 声明、稳定 surface 及同代 annotation metadata。 */
@OptIn(CompilerConfiguration.Internals::class)
class CfirRawConditionalCompilationMetadataTest {
    @Test
    fun psiPruningKeepsReachableSurfacesAndPublishesOnlyFinalSlots() = assertPruningContract(useLightTree = false)

    @Test
    fun lightTreePruningKeepsReachableSurfacesAndPublishesOnlyFinalSlots() = assertPruningContract(useLightTree = true)

    private fun assertPruningContract(useLightTree: Boolean) {
        val disposable = Disposer.newDisposable("conditional-compilation-metadata")
        try {
            val project = CangJieCoreEnvironment.create(disposable, CangJieCoreEnvironmentMode.Production)
                .projectEnvironment.project
            val session = createSourceSession()
            val raw = buildRawFile(project, session, useLightTree)
            val file = raw.cfirFile
            val metadata = raw.annotationMetadata
            val originalSlots = metadata.snapshots
            val functions = file.declarations.filterIsInstance<CfirNamedFunction>().associateBy { it.name.asString() }
            val retained = functions.getValue("retained")
            val removed = functions.getValue("removed")
            val plainParameter = retained.valueParameters.single { it.name.asString() == "plain" }
            val retainedParameter = retained.valueParameters.single { it.name.asString() == "retainedParameter" }
            val removedParameter = retained.valueParameters.single { it.name.asString() == "removedParameter" }
            val removedOwnerParameter = removed.valueParameters.single()
            val declarationWhen = annotation(raw, retained, "When")
            val declarationCustom = annotation(raw, retained, "APILevel")
            val parameterWhen = annotation(raw, retainedParameter, "When")
            val parameterCustom = annotation(raw, retainedParameter, "APILevel")
            val plainCustom = annotation(raw, plainParameter, "APILevel")
            val declarationSurface = surface(raw, declarationCustom)
            val parameterSurface = surface(raw, parameterCustom)
            val plainSurface = surface(raw, plainCustom)
            val declarationWhenSurface = surface(raw, declarationWhen)
            val parameterWhenSurface = surface(raw, parameterWhen)
            val declarationCarrier = checkNotNull(declarationSurface.replaceHandle.annotationCarrier)
            val parameterCarrier = checkNotNull(parameterSurface.replaceHandle.annotationCarrier)
            val originalDeclarationSlot = checkNotNull(metadata.snapshot(declarationCustom))
            val originalParameterSlot = checkNotNull(metadata.snapshot(parameterCustom))

            assertEquals(FqName("pruner.metadata"), file.packageDirective.packageFqName)
            assertEquals(1, originalDeclarationSlot.annotationIndex)
            assertEquals(1, originalParameterSlot.annotationIndex)
            assertSame(plainParameter, plainSurface.replaceHandle.carrier)
            val removedOwners = listOf(removed, removedParameter, removedOwnerParameter)
            val removedSurfaces = raw.surfaces.filter { candidate ->
                removedOwners.any { it === candidate.replaceHandle.carrier }
            }
            for (owner in removedOwners) {
                assertTrue(removedSurfaces.any { it.replaceHandle.carrier === owner }, owner.toString())
            }
            val registry = session.ensureAnnotationMetadataRegistry()
            originalSlots.forEach { assertNull(registry.snapshot(it.originalAnnotation)) }

            val pruned = buildPreMacroRawFiles(session, listOf(raw)).pruneConditionalCompilation().files.single()

            assertSame(file, pruned.cfirFile)
            assertSame(metadata, pruned.annotationMetadata)
            assertEquals(listOf(retained), file.declarations.filterIsInstance<CfirNamedFunction>())
            assertEquals(listOf(plainParameter, retainedParameter), retained.valueParameters)
            assertEquals(listOf(declarationCustom), retained.annotations)
            assertEquals(listOf(parameterCustom), retainedParameter.annotations)
            assertTrue(file.conditionalCompilationFailures.isEmpty())
            for (keptSurface in listOf(declarationSurface, parameterSurface, plainSurface)) {
                assertTrue(pruned.surfaces.any { it === keptSurface })
            }
            for (consumedSurface in listOf(declarationWhenSurface, parameterWhenSurface)) {
                assertFalse(pruned.surfaces.any { it === consumedSurface })
                assertNull(metadata.currentSlot(checkNotNull(consumedSurface.replaceHandle.annotationCarrier)))
            }
            for (removedSurface in removedSurfaces) {
                assertFalse(pruned.surfaces.any { it === removedSurface })
            }
            assertEquals(0, checkNotNull(metadata.currentSlot(declarationCarrier)).annotationIndex)
            assertEquals(0, checkNotNull(metadata.currentSlot(parameterCarrier)).annotationIndex)
            val finalDeclarationSlot = checkNotNull(metadata.snapshot(declarationCarrier))
            val finalParameterSlot = checkNotNull(metadata.snapshot(parameterCarrier))
            assertEquals(0, finalDeclarationSlot.annotationIndex)
            assertEquals(0, finalParameterSlot.annotationIndex)
            assertSame(declarationCustom, finalDeclarationSlot.originalAnnotation)
            assertSame(parameterCustom, finalParameterSlot.originalAnnotation)
            assertEquals(originalDeclarationSlot.rawSyntax, finalDeclarationSlot.rawSyntax)
            assertSame(originalDeclarationSlot.annotationSource, finalDeclarationSlot.annotationSource)
            assertEquals(originalParameterSlot.rawSyntax, finalParameterSlot.rawSyntax)
            assertSame(originalParameterSlot.annotationSource, finalParameterSlot.annotationSource)

            // 只封闭裁剪后的真实图；禁用 owner 的旧 metadata 不能随 writer 一起发布。
            val publishedAnnotations = listOf(
                file.declarations.filterIsInstance<CfirClass>().single().annotations.single() as CfirAnnotationCall,
                declarationCustom,
                plainCustom,
                parameterCustom,
            )
            val product = metadata.seal(listOf(file))
            originalSlots.forEach { assertNull(registry.snapshot(it.originalAnnotation)) }
            registry.publish(product)
            for (original in originalSlots) {
                val published = registry.snapshot(original.originalAnnotation)
                if (publishedAnnotations.any { it === original.originalAnnotation }) {
                    val slot = checkNotNull(published)
                    assertSame(original.owner, slot.owner)
                    assertSame(original.originalAnnotation, slot.originalAnnotation)
                    assertEquals(slot.owner.annotations.indexOfFirst { it === slot.originalAnnotation }, slot.annotationIndex)
                } else {
                    assertNull(published, original.rawSyntax)
                }
            }
            assertSame(finalDeclarationSlot, registry.snapshot(declarationCustom))
            assertSame(finalParameterSlot, registry.snapshot(parameterCustom))
        } finally {
            Disposer.dispose(disposable)
        }
    }

    /** 注解和 surface 必须来自本次 parser/raw 构建，不为期望结果制造 carrier。 */
    private fun annotation(raw: PreMacroCfirFile, owner: CfirDeclaration, name: String): CfirAnnotationCall =
        owner.annotations.filterIsInstance<CfirAnnotationCall>().single {
            raw.annotationMetadata.snapshot(it)?.qualifiedName?.shortName()?.asString() == name
        }

    private fun surface(raw: PreMacroCfirFile, annotation: CfirAnnotationCall): MacroSurface =
        raw.surfaces.single { it.replaceHandle.annotationCarrier?.originalAnnotation === annotation }

    private fun buildRawFile(project: Project, session: CfirSession, useLightTree: Boolean): PreMacroCfirFile {
        val parsingContext = session.cjmpSettings.toParsingContext(session.languageVersionSettings)
        return if (useLightTree) {
            val parserBuilder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), SOURCE)
            val diagnostics = mutableListOf<ParsingDiagnostic>()
            val parsed = CangJieLightParser.parse(
                parserBuilder, errorListener = { diagnostics += it }, cjmpContext = parsingContext,
            )
            assertEquals(emptyList<ParsingDiagnostic>(), diagnostics)
            LightTree2Cfir(session, session.cangjieScopeProvider).buildPreMacroCfirFile(
                parsed,
                CjInMemoryTextSourceFile(FILE_NAME, null, SOURCE),
                SOURCE.toSourceLinesMapping(),
            )
        } else {
            val psiFile = CjPsiFactory(project, markGenerated = false).createFile(FILE_NAME, SOURCE)
            psiFile.cjmpParsingContext = parsingContext
            assertEquals(emptyList<ParsingDiagnostic>(), psiFile.collectParsingDiagnostics())
            PsiRawCfirBuilder(session, annotationMetadata = CfirAnnotationMetadataBuilder()).buildPreMacroCfirFile(psiFile)
        }
    }

    /** 使用正式 shared/library/source 会话工厂，条件值显式来自本测试的 compiler invocation。 */
    private fun createSourceSession(): CfirSession {
        val moduleName = Name.special("<conditional-compilation-metadata-test>")
        val configuration = CompilerConfiguration().apply {
            diagnosticFactoriesStorage = CjRegisteredDiagnosticFactoriesStorage()
            conditionalCompilationSettings = ExplicitCfirConditionalCompilationSettings(
                backend = "cjnative", arch = "x86_64", os = "Windows", cjcVersion = "1.1.3", debug = false, test = false,
            )
        }
        val dependencies = DependencyListForCliModule.build(moduleName)
        val factory = CfirDefaultSessionFactory()
        val context = createDefaultCfirSessionFactoryContext(configuration)
        val session = CfirSessionConstructionUtils.prepareSessions(
            files = listOf(FILE_NAME),
            configuration = configuration,
            rootModuleName = moduleName,
            dependencyList = dependencies,
            createSharedLibrarySession = {
                factory.createSharedLibrarySession(
                    mainModuleName = moduleName,
                    extensionRegistrars = emptyList(),
                    languageVersionSettings = configuration.languageVersionSettings,
                    context = context,
                )
            },
            createLibrarySession = { shared ->
                factory.createLibrarySession(
                    sharedLibrarySession = shared,
                    moduleDataProvider = dependencies.moduleDataProvider,
                    extensionRegistrars = emptyList(),
                    languageVersionSettings = configuration.languageVersionSettings,
                    context = context,
                )
            },
            createSourceSession = CfirSessionProducer<String> { _, moduleData, _, configurator ->
                factory.createSourceSession(
                    moduleData = moduleData,
                    extensionRegistrars = emptyList(),
                    configuration = configuration,
                    context = context,
                    init = configurator,
                )
            },
        ).single().session
        assertSame(configuration.conditionalCompilationSettings, session.conditionalCompilationSettingsOrNull)
        return session
    }

    companion object {
        private const val FILE_NAME = "conditionalMetadata.cj"

        // @When 语法来自 llt/conditional-compilation/basic.cj；自定义注解声明及参数用法
        // 来自 macro/llt/annotation/globals/allkinds.cj。本测试只裁剪 raw，不执行宏或 resolve。
        private val SOURCE = """
            package pruner.metadata

            @Annotation
            public class APILevel {
                let a: UInt8
                const init(v: UInt8) { a = v }
            }

            @When[!debug]
            @APILevel[11]
            func retained(
                @APILevel[12] plain: Int64,
                @When[!debug] @APILevel[13] retainedParameter: Int64,
                @When[debug] @APILevel[14] removedParameter: Int64,
            ): Unit {}

            @When[debug]
            @APILevel[21]
            func removed(@APILevel[22] removedOwnerParameter: Int64): Unit {}
        """.trimIndent()
    }
}
