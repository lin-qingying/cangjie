package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.pipeline.runCheckers
import org.cangnova.cangjie.cfir.session.lazyDeclarationResolver
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

        val platformPart = info.partsForDependsOnModules.last()
        val lazyDeclarationResolver = platformPart.session.lazyDeclarationResolver

        lazyDeclarationResolver.disableLazyResolveContractChecksInside {
            for (part in info.partsForDependsOnModules) {
                val diagnosticsCollector = DiagnosticsCollectorImpl()
                val diagnostics = part.session.runCheckers(
                    scopeSession = part.scopeSession,
                    firFiles = part.firFilesByTestFile.values,
                    diagnosticsCollector = diagnosticsCollector,
                )
                appendComputedDiagnostics(diagnostics, diagnosticsByFile)
                appendLightTreeSyntaxDiagnostics(part, diagnosticsByFile)
            }
        }

        return diagnosticsByFile.mapValues { (_, value) -> value.toList() }
    }

    private fun appendComputedDiagnostics(
        diagnostics: CfirDiagnosticsMap,
        destination: MutableMap<CfirFile, MutableList<CjDiagnostic>>,
    ) {
        for ((file, fileDiagnostics) in diagnostics) {
            if (fileDiagnostics.isEmpty()) continue
            destination.getOrPut(file) { mutableListOf() }.addAll(fileDiagnostics)
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
