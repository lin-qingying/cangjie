package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.pipeline.AllModulesFrontendOutput
import org.cangnova.cangjie.cfir.pipeline.SingleModuleFrontendOutput
import org.cangnova.cangjie.cfir.pipeline.buildRecordableCfirFromCjFiles
import org.cangnova.cangjie.cfir.pipeline.buildRecordableCfirViaLightTree
import org.cangnova.cangjie.cfir.pipeline.runResolution
import org.cangnova.cangjie.cfir.resolve.providers.macro.RecordableRawCfirFiles
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.CfirParser

abstract class AbstractCfirAnalyzerFacade {
    abstract val scopeSession: ScopeSession
    abstract val frontendOutput: AllModulesFrontendOutput

    abstract fun runResolution(): List<CfirFile>
}

class CfirAnalyzerFacade(
    val session: CfirSession,
    val cjFiles: Collection<CjFile> = emptyList(), // may be empty if light tree mode enabled
    val lightTreeFiles: Collection<CjSourceFile> = emptyList(), // may be empty if light tree mode disabled
    val parser: CfirParser,
    val diagnosticReporterForLightTree: DiagnosticReporter? = null
) : AbstractCfirAnalyzerFacade() {
    private var recordableFiles: RecordableRawCfirFiles? = null
    private var cfirFiles: List<CfirFile>? = null
    private var _scopeSession: ScopeSession? = null
    override val scopeSession: ScopeSession
        get() = _scopeSession!!

    override val frontendOutput: AllModulesFrontendOutput
        get() = AllModulesFrontendOutput(listOf(SingleModuleFrontendOutput(session, scopeSession, cfirFiles!!)))

    private fun buildRawCfir() {
        if (recordableFiles != null) return
        recordableFiles = when (parser) {
            CfirParser.LightTree -> session.buildRecordableCfirViaLightTree(
                lightTreeFiles,
                diagnosticReporterForLightTree,
                reportFilesAndLines = null,
            )
            CfirParser.Psi -> session.buildRecordableCfirFromCjFiles(cjFiles)
        }
        cfirFiles = recordableFiles!!.files
    }

    override fun runResolution(): List<CfirFile> {
        if (cfirFiles == null) buildRawCfir()
        if (_scopeSession != null) return cfirFiles!!
        val (scopeSession, resolvedFiles) = session.runResolution(recordableFiles!!)
        _scopeSession = scopeSession
        cfirFiles = resolvedFiles
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
