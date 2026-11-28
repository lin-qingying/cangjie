
@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class, org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder

import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.MacroSurfaceCollectionMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.publishParsingDiagnostics
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroCfirFile
import org.cangnova.cangjie.cfir.resolve.providers.macro.materializePrimaryConstructorParameterFields
import org.cangnova.cangjie.cfir.resolve.providers.macro.prepareDefaultParameterFunctions
import org.cangnova.cangjie.cfir.session.ensureAnnotationMetadataRegistry
import org.cangnova.cangjie.cfir.session.ensureRawMacroSurfaceUsageStore
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.builder.toParsingContext
import org.cangnova.cangjie.parsing.CangJieParser
import org.cangnova.cangjie.parsing.compilationParsingResultOrNull
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.utils.ThreadSafe
import org.cangnova.cangjie.utils.exceptions.checkWithAttachment

/**
 * Responsble for building [CfirFile] by [CjFile]
 */
@ThreadSafe
internal class LLCfirFileBuilder(val moduleComponents: LLCfirModuleResolveComponents) {
    /**
     * 用于校验待构建文件实际所属模块的项目结构 provider。
     */
    private val projectStructureProvider by lazy { CangJieProjectStructureProvider.getInstance(moduleComponents.session.project) }

    /**
     * 构建或复用指定 PSI 文件的 raw CFIR 文件，并校验 contextual module 与实际文件模块一致。
     */
    fun buildRawCfirFileWithCaching(cjFile: CjFile): CfirFile {
        LLCfirSourcePsiGeneration.capture(listOf(cjFile), compareModificationStamps = false).requireCurrent()
        return moduleComponents.cache.fileCached(cjFile) {
            val session = moduleComponents.session
            val collectionMode = if (session.macroSemanticSnapshot != null) {
                MacroSurfaceCollectionMode.EXPRESSION_ONLY
            } else {
                MacroSurfaceCollectionMode.CONSTRUCTION
            }
            val input = buildRawFileInput(cjFile, BodyBuildingMode.LAZY_BODIES, collectionMode)
            val generation = input.generation
            val file = generation.cfirFile
            val product = generation.product
            materializePrimaryConstructorParameterFields(file, input.annotationMetadata)
            prepareDefaultParameterFunctions(file, input.annotationMetadata, product.actualInputSeed.settings.backend)
            generation.requireConsistentInput(session)
            val productMetadata = input.annotationMetadata.seal(listOf(file))
            synchronized(session) {
                val publication = session.ensureAnnotationMetadataRegistry().preparePublication(listOf(productMetadata))
                try {
                    publication.commit()
                    session.ensureRawMacroSurfaceUsageStore().recordSurfaces(
                        file, listOf(product.file, product.originFile), input.surfaces,
                    )
                } catch (failure: Throwable) {
                    publication.restore()
                    throw failure
                }
            }
            generation
        }
    }

    /** 解析入口先选择实际发布的语义文件；普通 raw 入口仍独立管理各自的来源代次。 */
    fun getOrBuildFileForResolution(cjFile: CjFile): CfirFile {
        val snapshot = moduleComponents.session.macroSemanticSnapshot
        return snapshot?.fileForSource(cjFile) ?: snapshot?.fileForReparsedPsi(cjFile)
            ?: buildRawCfirFileWithCaching(cjFile)
    }

    /** 宏构造独占完整正文、同代 parser 报告和仍打开的 annotation writer。 */
    fun buildPreMacroRawFileUncached(cjFile: CjFile): PreMacroCfirFile {
        val input = buildRawFileInput(cjFile, BodyBuildingMode.NORMAL, MacroSurfaceCollectionMode.CONSTRUCTION)
        val product = input.generation.product
        return PreMacroCfirFile(
            cfirFile = input.generation.cfirFile,
            surfaces = input.surfaces,
            annotationMetadata = input.annotationMetadata,
            parsingDiagnosticPublication = product.diagnosticPublication,
            parserSettings = product.actualInputSeed.settings,
            sourceFileIdentity = cjFile,
        )
    }

    /** 独立构造树不会在此入口取得普通 LL 文件缓存或 source provider 的归属。 */
    fun buildRawCfirFileUncached(cjFile: CjFile): CfirFile = buildPreMacroRawFileUncached(cjFile).cfirFile

    private class RawFileInput(
        val generation: LLCfirFileSourceGeneration,
        val annotationMetadata: CfirAnnotationMetadataBuilder,
        val surfaces: List<MacroSurface>,
    )

    private fun buildRawFileInput(
        cjFile: CjFile,
        bodyBuildingMode: BodyBuildingMode,
        macroSurfaceCollectionMode: MacroSurfaceCollectionMode,
    ): RawFileInput {
        LLCfirSourcePsiGeneration.capture(listOf(cjFile), compareModificationStamps = false).requireCurrent()
        val contextualModule = moduleComponents.module
        val actualFileModule = projectStructureProvider.getModule(cjFile, contextualModule)

        checkWithAttachment(actualFileModule == contextualModule, { "Modules are inconsistent" }) {
            withEntry("file", cjFile.name)
            withEntry("file module", actualFileModule) {
                it.toString()
            }
            withEntry("components module", contextualModule) {
                it.toString()
            }
        }

        val builder = PsiRawCfirBuilder(
            moduleComponents.session,
            moduleComponents.scopeProvider,
            bodyBuildingMode = bodyBuildingMode,
            annotationMetadata = CfirAnnotationMetadataBuilder(),
            macroSurfaceCollectionMode = macroSurfaceCollectionMode,
        )
        val product = cjFile.compilationParsingResultOrNull() ?: parseFile(cjFile)
        check(product.actualInputSeed.cjmpContext ==
                moduleComponents.session.cjmpSettings.toParsingContext(moduleComponents.session.languageVersionSettings)) {
            "A raw source build must consume its actual session parsing configuration before publication."
        }
        val cfirFile = product.withTargetFile(builder::buildCfirFile)
        cfirFile.replaceSourceFile(product.sourceFile)
        cfirFile.replaceSourceFileLinesMapping(product.sourceFileLinesMapping)
        cfirFile.publishParsingDiagnostics(product.diagnosticPublication, product.actualInputSeed.settings)
        val generation = LLCfirFileSourceGeneration(product, cfirFile, moduleComponents.session.languageVersionSettings)
        generation.requireConsistentInput(moduleComponents.session)
        return RawFileInput(generation, builder.annotationMetadata, builder.consumeCollectedMacroSurfaces())
    }

    /** 文件产品由当前 session 语境完整解析；原入口、目标树与缓存身份始终共同保存。 */
    fun parseFile(cjFile: CjFile): org.cangnova.cangjie.parsing.ParsedCangJiePsiFile {
        val context = moduleComponents.session.cjmpSettings.toParsingContext(moduleComponents.session.languageVersionSettings)
        return if (cjFile is CjCodeFragment) {
            CangJieParser.parseCompilationFragment(cjFile, cjFile.fragmentParserSeed.copy(cjmpContext = context), cjFile.parserCreationContext)
        } else {
            CangJieParser.parseCompilationFile(cjFile, context)
        }
    }
}
