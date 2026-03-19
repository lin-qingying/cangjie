package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.session.diagnosticCollector
import org.cangnova.cangjie.test.services.TestService
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.toLightTreeShortName

private typealias CfirDiagnosticsMap = Map<CfirFile, List<CjDiagnostic>>

open class CfirDiagnosticCollectorService(
    @Suppress("UNUSED_PARAMETER") val testServices: TestServices,
) : TestService {
    open val reporterForLTSyntaxErrors: DiagnosticReporter = DiagnosticsCollectorImpl()

    private val cache: MutableMap<CfirOutputArtifact, CfirDiagnosticsMap> = mutableMapOf()

    open fun getFrontendDiagnosticsForModule(info: CfirOutputArtifact): CfirDiagnosticsMap {
        return cache.getOrPut(info) { computeDiagnostics(info) }
    }

    val containsErrorDiagnostics: Boolean
        get() = cache.values.any { perFile ->
            perFile.values.flatten().any { it.severity == Severity.ERROR }
        }

    fun containsErrors(info: CfirOutputArtifact): Boolean {
        return getFrontendDiagnosticsForModule(info).values.flatten().any { it.severity == Severity.ERROR }
    }

    private fun computeDiagnostics(info: CfirOutputArtifact): CfirDiagnosticsMap {
        val allFiles = info.partsForDependsOnModules.flatMap { it.firFilesByTestFile.values }
        val diagnosticsByFile = linkedMapOf<CfirFile, MutableList<CjDiagnostic>>()
        allFiles.forEach { diagnosticsByFile[it] = mutableListOf() }

        for (part in info.partsForDependsOnModules) {
            val collector = runCatching { part.session.diagnosticCollector }.getOrNull()
            if (collector != null) {
                appendSessionDiagnostics(collector.rawDiagnostics, diagnosticsByFile)
            }
            appendLightTreeSyntaxDiagnostics(part, diagnosticsByFile)
        }

        return diagnosticsByFile.mapValues { (_, value) -> value.toList() }
    }

    private fun appendSessionDiagnostics(
        diagnostics: List<CjDiagnostic>,
        destination: MutableMap<CfirFile, MutableList<CjDiagnostic>>,
    ) {
        if (diagnostics.isEmpty()) return

        for (diagnostic in diagnostics) {
            val filePath = (diagnostic.context as? DiagnosticContext)?.containingFilePath?.normalizePath()
            if (filePath == null) {
                destination.values.firstOrNull()?.add(diagnostic)
                continue
            }

            val file = destination.keys.firstOrNull { firFile ->
                firFile.sourceFile?.path?.normalizePath() == filePath
            } ?: continue
            destination.getValue(file).add(diagnostic)
        }
    }

    private fun appendLightTreeSyntaxDiagnostics(
        part: CfirOutputPartForDependsOnModule,
        destination: MutableMap<CfirFile, MutableList<CjDiagnostic>>,
    ) {
        val lightTreeReporter = reporterForLTSyntaxErrors as? DiagnosticsCollectorImpl ?: return
        val diagnosticsByPath = lightTreeReporter.diagnosticsByFilePath

        for ((testFile, firFile) in part.firFilesByTestFile) {
            val path = "/${testFile.toLightTreeShortName()}"
            val diagnostics = diagnosticsByPath[path].orEmpty()
            if (diagnostics.isEmpty()) continue
            destination.getOrPut(firFile) { mutableListOf() }.addAll(diagnostics)
        }
    }
}

private fun String.normalizePath(): String = replace('\\', '/')

val TestServices.cfirDiagnosticCollectorService: CfirDiagnosticCollectorService by TestServices.testServiceAccessor()
