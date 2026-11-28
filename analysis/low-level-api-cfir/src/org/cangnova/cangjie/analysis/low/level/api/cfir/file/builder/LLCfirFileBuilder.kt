
@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class, org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder

import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.publishParsingDiagnostics
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.materializePrimaryConstructorParameterFields
import org.cangnova.cangjie.cfir.session.ensureAnnotationMetadataRegistry
import org.cangnova.cangjie.cfir.session.recordRawExpressionMacroSurfaces
import org.cangnova.cangjie.cfir.session.rawMacroSurfaceUsageStoreOrNull
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
    fun buildRawCfirFileWithCaching(cjFile: CjFile): CfirFile = moduleComponents.cache.fileCached(cjFile) {
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
            bodyBuildingMode = BodyBuildingMode.LAZY_BODIES,
            annotationMetadata = CfirAnnotationMetadataBuilder(),
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
        materializePrimaryConstructorParameterFields(cfirFile, builder.annotationMetadata)
        org.cangnova.cangjie.cfir.resolve.providers.macro.prepareDefaultParameterFunctions(
            cfirFile, builder.annotationMetadata, product.actualInputSeed.settings.backend,
        )
        val generation = LLCfirFileSourceGeneration(product, cfirFile, moduleComponents.session.languageVersionSettings)
        generation.requireConsistentInput(moduleComponents.session)
        // 封闭当前 raw 代次的元数据，不授予 macro construction 完成身份。
        // 后续 LL construction 必须在此 seal 之前完成，不能改写已缓存图。
        val metadata = builder.annotationMetadata.seal(listOf(cfirFile))
        moduleComponents.session.ensureAnnotationMetadataRegistry().publish(metadata)
        moduleComponents.session.recordRawExpressionMacroSurfaces(
            file = cfirFile,
            sourceFileIdentity = product.file,
            surfaces = builder.consumeCollectedMacroSurfaces(),
        )
        checkNotNull(moduleComponents.session.rawMacroSurfaceUsageStoreOrNull).bindSourceFileIdentity(cfirFile, product.originFile)
        generation
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
