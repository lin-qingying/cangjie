package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
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

abstract class AbstractCfirAnalyzerFacade {
    abstract val scopeSession: ScopeSession
    abstract val frontendOutput: AllModulesFrontendOutput

    abstract fun runResolution(): List<CfirFile>
}

class CfirAnalyzerFacade(
    val session: CfirSession,
    val configuration: CompilerConfiguration,
    val cjFiles: Collection<CjFile> = emptyList(), // may be empty if light tree mode enabled
    val lightTreeFiles: Collection<CjSourceFile> = emptyList(), // may be empty if light tree mode disabled
    val parser: CfirParser,
    val diagnosticReporterForLightTree: DiagnosticReporter? = null
) : AbstractCfirAnalyzerFacade() {
    private var cfirFiles: List<CfirFile>? = null
    private var _scopeSession: ScopeSession? = null
    private var constructionOutput: SingleModuleFrontendOutput? = null
    override val scopeSession: ScopeSession
        get() = _scopeSession!!

    override val frontendOutput: AllModulesFrontendOutput
        get() = AllModulesFrontendOutput(listOf(SingleModuleFrontendOutput(session, scopeSession, cfirFiles!!)))

    private fun buildAndResolveCfir() {
        if (constructionOutput != null) return
        val pre = when (parser) {
            CfirParser.LightTree -> session.buildPreMacroRawCfirViaLightTree(
                lightTreeFiles,
                diagnosticReporterForLightTree,
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

    override fun runResolution(): List<CfirFile> {
        if (constructionOutput == null) buildAndResolveCfir()
        return cfirFiles!!
    }
}

class CfirPipelineAnalyzerFacade(
    private val output: SingleModuleFrontendOutput,
) : AbstractCfirAnalyzerFacade() {
    override val scopeSession: ScopeSession
        get() = output.scopeSession

    override val frontendOutput: AllModulesFrontendOutput
        get() = AllModulesFrontendOutput(listOf(output))

    override fun runResolution(): List<CfirFile> {
        return output.fir
    }
}
