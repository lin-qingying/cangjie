package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.pipeline.AllModulesFrontendOutput
import org.cangnova.cangjie.cfir.pipeline.SingleModuleFrontendOutput
import org.cangnova.cangjie.cfir.pipeline.buildPreMacroRawCfirFromCjFiles
import org.cangnova.cangjie.cfir.pipeline.buildPreMacroRawCfirViaLightTree
import org.cangnova.cangjie.cfir.pipeline.resolveAndCheckCfirAfterConstruction
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDemandClassification
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFailurePolicy
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ensureAnnotationMetadataRegistry
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.diagnosticsCollector
import org.cangnova.cangjie.frontend.pipeline.FrontendMacroConstructionService
import org.cangnova.cangjie.frontend.pipeline.installDefaultMacroFragmentParserFactory
import org.cangnova.cangjie.frontend.pipeline.macroConstructionMode
import org.cangnova.cangjie.frontend.pipeline.prepareMacroArtifactDefinitionsForExpansion
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.CfirParser

/**
 * 表示 `AbstractCfirAnalyzerFacade`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
 */
abstract class AbstractCfirAnalyzerFacade {
    /**
     * 保存 `scopeSession`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    abstract val scopeSession: ScopeSession
    /**
     * 保存 `frontendOutput`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    abstract val frontendOutput: AllModulesFrontendOutput

    /**
     * 提供 `runResolution` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    abstract fun runResolution(): List<CfirFile>
}

/**
 * 表示 `CfirAnalyzerFacade`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
 */
class CfirAnalyzerFacade(
    /**
     * 保存 `session`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val session: CfirSession,
    /**
     * 保存 `configuration`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val configuration: CompilerConfiguration,
    /**
     * 保存 `cjFiles`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val cjFiles: Collection<CjFile> = emptyList(), // may be empty if light tree mode enabled
    /**
     * 保存 `lightTreeFiles`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val lightTreeFiles: Collection<CjSourceFile> = emptyList(), // may be empty if light tree mode disabled
    /**
     * 保存 `parser`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val parser: CfirParser,
) : AbstractCfirAnalyzerFacade() {
    /**
     * 维护 `cfirFiles`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    private var cfirFiles: List<CfirFile>? = null
    /**
     * 维护 `_scopeSession`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    private var _scopeSession: ScopeSession? = null
    /**
     * 维护 `constructionOutput`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    private var constructionOutput: SingleModuleFrontendOutput? = null
    /**
     * 保存 `scopeSession`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    override val scopeSession: ScopeSession
        get() = _scopeSession!!

    /**
     * 保存 `frontendOutput`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    override val frontendOutput: AllModulesFrontendOutput
        get() = AllModulesFrontendOutput(listOf(SingleModuleFrontendOutput(session, scopeSession, cfirFiles!!)))

    /**
     * 提供 `buildAndResolveCfir` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    private fun buildAndResolveCfir() {
        if (constructionOutput != null) return
        val pre = when (parser) {
            CfirParser.LightTree -> session.buildPreMacroRawCfirViaLightTree(
                lightTreeFiles,
                reportFilesAndLines = null,
            )
            CfirParser.Psi -> session.buildPreMacroRawCfirFromCjFiles(cjFiles)
        }
        session.ensureAnnotationMetadataRegistry()
        val classification = MacroDemandClassification.create(pre).also {
            session.register(MacroDemandClassification::class, it)
        }
        val artifactPreparation = prepareMacroArtifactDefinitionsForExpansion(configuration, listOf(classification))
        classification.freezeFinal(
            macroArtifactDefinitions = artifactPreparation.definitions,
            failurePolicy = when (configuration.macroConstructionMode) {
                MacroConstructionService.Mode.STRICT -> MacroFailurePolicy.STRICT
                MacroConstructionService.Mode.DEGRADED -> MacroFailurePolicy.DEGRADED
            },
        )
        configuration.installDefaultMacroFragmentParserFactory(cjFiles.firstOrNull()?.project)
        val (constructionResult, output) = resolveAndCheckCfirAfterConstruction(
            session = session,
            pre = pre,
            classification = classification,
            constructionService = FrontendMacroConstructionService(configuration),
            constructionMode = configuration.macroConstructionMode,
            diagnosticsCollector = configuration.diagnosticsCollector,
            macroArtifactDefinitions = artifactPreparation.definitions,
            preConstructionDiagnostics = artifactPreparation.diagnostics,
        )
        val resolvedOutput = output
            ?: error(buildString {
                append("Macro construction failed before CFIR test frontend output was produced.")
                val diagnostics = constructionResult.registry.diagnostics
                if (diagnostics.isEmpty()) {
                    append(" No macro construction diagnostics were recorded.")
                    return@buildString
                }
                appendLine()
                appendLine("Macro construction diagnostics:")
                diagnostics.forEach { diagnostic ->
                    append(" - ")
                    append(diagnostic.severity)
                    append(' ')
                    append(diagnostic.kind)
                    append(" [")
                    append(diagnostic.diagnosticOrigin)
                    append("]: ")
                    append(diagnostic.message)
                    diagnostic.artifactPackage?.let {
                        append(" package=")
                        append(it)
                    }
                    diagnostic.artifactPath?.let {
                        append(" artifact=")
                        append(it)
                    }
                    diagnostic.macroLibraryPath?.let {
                        append(" lib=")
                        append(it)
                    }
                    diagnostic.originSurfaceId?.let {
                        append(" surfaceId=")
                        append(it)
                    }
                    appendLine()
                }
            })
        constructionOutput = resolvedOutput
        cfirFiles = resolvedOutput.fir
        _scopeSession = resolvedOutput.scopeSession
    }

    /**
     * 执行 `runResolution` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    override fun runResolution(): List<CfirFile> {
        if (constructionOutput == null) buildAndResolveCfir()
        return cfirFiles!!
    }
}

/**
 * 表示 `CfirPipelineAnalyzerFacade`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
 */
class CfirPipelineAnalyzerFacade(
    /**
     * 保存 `output`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    private val output: SingleModuleFrontendOutput,
) : AbstractCfirAnalyzerFacade() {
    /**
     * 保存 `scopeSession`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    override val scopeSession: ScopeSession
        get() = output.scopeSession

    /**
     * 保存 `frontendOutput`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    override val frontendOutput: AllModulesFrontendOutput
        get() = AllModulesFrontendOutput(listOf(output))

    /**
     * 执行 `runResolution` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    override fun runResolution(): List<CfirFile> {
        return output.fir
    }
}
