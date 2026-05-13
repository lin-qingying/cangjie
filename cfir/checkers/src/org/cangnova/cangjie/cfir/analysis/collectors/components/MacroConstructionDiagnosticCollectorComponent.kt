package org.cangnova.cangjie.cfir.analysis.collectors.components

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.macroExpansionRegistry
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * 将 macro construction 阶段记录在 [MacroExpansionRegistry] 中的诊断
 * 转发到 ordinary diagnostic collector。
 *
 * ordinary checker 仍只遍历 final CFIR；本组件只在文件入口读取 session
 * 上的 construction registry，并用 `originSurfaceId -> MacroSurface` 把诊断
 * 定位回原始 macro 调用位点。
 */
class MacroConstructionDiagnosticCollectorComponent(
    session: CfirSession,
    reporter: PendingDiagnosticReporter,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    private var reported = false

    override fun visitFile(file: CfirFile, data: CheckerContext) {
        if (reported) return
        reported = true

        val registry = session.macroExpansionRegistry ?: return
        for (diagnostic in registry.diagnostics) {
            if (!diagnostic.shouldReportToOrdinaryDiagnostics()) continue
            val surface = diagnostic.originSurfaceId?.let { registry.originSurfaceById[it] }
            val source = surface?.sourceRange?.source
                ?: diagnostic.originSource
                ?: file.source as? AbstractCjSourceElement
                ?: continue
            reportConstructionDiagnostic(diagnostic, surface, source, data)
            reporter.checkAndCommitReportsOn(source, data, commitEverything = true)
        }
    }

    private fun reportConstructionDiagnostic(
        diagnostic: MacroConstructionDiagnostic,
        surface: MacroSurface?,
        source: AbstractCjSourceElement,
        context: CheckerContext,
    ) {
        val macroName = surface?.qualifiedName?.shortName()?.asString()
            ?: diagnostic.extractBacktickedName()
            ?: "<macro>"
        if (diagnostic.diagnosticOrigin == MacroConstructionDiagnostic.Origin.DIAG_REPORT) {
            val hint = diagnostic.hint.orEmpty()
            when (diagnostic.severity) {
                MacroConstructionDiagnostic.Severity.ERROR ->
                    reporter.reportOn(source, CfirErrors.MACRO_DIAG_REPORT_ERROR, diagnostic.message, hint, context)
                MacroConstructionDiagnostic.Severity.WARNING ->
                    reporter.reportOn(source, CfirErrors.MACRO_DIAG_REPORT_WARNING, diagnostic.message, hint, context)
                MacroConstructionDiagnostic.Severity.INFO -> return
            }
            return
        }
        when (diagnostic.kind) {
            MacroConstructionDiagnostic.Kind.MACRO_NOT_EXPANDED -> {
                reporter.reportOn(source, CfirErrors.MACRO_NOT_EXPANDED, macroName, context)
            }
            MacroConstructionDiagnostic.Kind.MACRO_EXPANSION_FAILED,
            MacroConstructionDiagnostic.Kind.GENERIC -> {
                reporter.reportOn(source, CfirErrors.MACRO_EXPANSION_FAILED, macroName, diagnostic.message, context)
            }
            MacroConstructionDiagnostic.Kind.MACRO_UNDEFINED_PACKAGE -> {
                reporter.reportOn(
                    source,
                    CfirErrors.MACRO_UNDEFINED_PACKAGE,
                    diagnostic.artifactPackage?.asString() ?: macroName,
                    diagnostic.message,
                    context,
                )
            }
            MacroConstructionDiagnostic.Kind.MACRO_UNDECLARED_IDENTIFIER -> {
                reporter.reportOn(
                    source,
                    CfirErrors.MACRO_UNDECLARED_IDENTIFIER,
                    diagnostic.relatedName ?: Name.identifier(macroName),
                    diagnostic.message,
                    context,
                )
            }
            MacroConstructionDiagnostic.Kind.MACRO_EXPECT_MACRO_DEFINITION -> {
                reporter.reportOn(
                    source,
                    CfirErrors.MACRO_EXPECT_MACRO_DEFINITION,
                    diagnostic.artifactPackage?.asString() ?: macroName,
                    diagnostic.message,
                    context,
                )
            }
            MacroConstructionDiagnostic.Kind.MACRO_DEPENDENCY_COMPILE_FAILED -> {
                reporter.reportOn(
                    source,
                    CfirErrors.MACRO_DEPENDENCY_COMPILE_FAILED,
                    diagnostic.artifactPackage?.asString() ?: macroName,
                    diagnostic.message,
                    diagnostic.sourceDiagnosticsRef.orEmpty(),
                    context,
                )
            }
            MacroConstructionDiagnostic.Kind.MACRO_AMBIGUOUS_MATCH -> {
                reporter.reportOn(
                    source,
                    CfirErrors.MACRO_AMBIGUOUS_MATCH,
                    macroName,
                    diagnostic.relatedTargets,
                    context,
                )
            }
            MacroConstructionDiagnostic.Kind.MACRO_CANNOT_FIND_DEPENDENCY_BCHIR -> {
                reporter.reportOn(
                    source,
                    CfirErrors.MACRO_CANNOT_FIND_DEPENDENCY_BCHIR,
                    diagnostic.artifactPackage?.asString() ?: macroName,
                    diagnostic.artifactPath.orEmpty(),
                    context,
                )
            }
            MacroConstructionDiagnostic.Kind.MACRO_EXPECT_PLAIN_MACRO -> {
                reporter.reportOn(source, CfirErrors.MACRO_EXPECT_PLAIN_MACRO, macroName, diagnostic.message, context)
            }
            MacroConstructionDiagnostic.Kind.MACRO_EXPECT_ATTRIBUTED_MACRO -> {
                reporter.reportOn(source, CfirErrors.MACRO_EXPECT_ATTRIBUTED_MACRO, macroName, diagnostic.message, context)
            }
            MacroConstructionDiagnostic.Kind.MACRO_EXPAND_ATEXCL -> {
                reporter.reportOn(source, CfirErrors.MACRO_EXPAND_ATEXCL, macroName, diagnostic.message, context)
            }
            MacroConstructionDiagnostic.Kind.MACRO_INVALID_ATTR_TOKENS -> {
                reporter.reportOn(source, CfirErrors.MACRO_INVALID_ATTR_TOKENS, macroName, diagnostic.message, context)
            }
            MacroConstructionDiagnostic.Kind.MACRO_INVALID_INPUT_TOKENS -> {
                reporter.reportOn(source, CfirErrors.MACRO_INVALID_INPUT_TOKENS, macroName, diagnostic.message, context)
            }
            MacroConstructionDiagnostic.Kind.MACRO_INVALID_ESCAPE -> {
                reporter.reportOn(source, CfirErrors.MACRO_INVALID_ESCAPE, macroName, diagnostic.message, context)
            }
            MacroConstructionDiagnostic.Kind.MACRO_SAME_PACKAGE_DEF_CALL -> {
                reporter.reportOn(
                    source,
                    CfirErrors.MACRO_SAME_PACKAGE_DEF_CALL,
                    macroName,
                    surface?.scopeContext?.packageFqName ?: FqName.ROOT,
                    context,
                )
            }
            MacroConstructionDiagnostic.Kind.MACRO_ALIAS_CONFLICT -> {
                reporter.reportOn(
                    source,
                    CfirErrors.MACRO_ALIAS_CONFLICT,
                    diagnostic.relatedName ?: Name.identifier(macroName.removePrefix("@").ifEmpty { "macroAlias" }),
                    diagnostic.relatedTargets,
                    context,
                )
            }
            MacroConstructionDiagnostic.Kind.MACRO_EXECUTOR_UNAVAILABLE -> {
                reporter.reportOn(source, CfirErrors.MACRO_EXECUTOR_UNAVAILABLE, diagnostic.message, context)
            }
            MacroConstructionDiagnostic.Kind.MACRO_CANNOT_OPEN_LIB -> {
                reporter.reportOn(
                    source,
                    CfirErrors.MACRO_CANNOT_OPEN_LIB,
                    diagnostic.macroLibraryPath ?: macroName,
                    diagnostic.message,
                    context,
                )
            }
            MacroConstructionDiagnostic.Kind.MACRO_CANNOT_FIND_METHOD -> {
                reporter.reportOn(source, CfirErrors.MACRO_CANNOT_FIND_METHOD, macroName, diagnostic.message, context)
            }
            MacroConstructionDiagnostic.Kind.MACRO_EVALUATE_FAILED -> {
                reporter.reportOn(source, CfirErrors.MACRO_EVALUATE_FAILED, macroName, diagnostic.message, context)
            }
            MacroConstructionDiagnostic.Kind.MACRO_EXPAND_FAILED -> {
                reporter.reportOn(source, CfirErrors.MACRO_EXPAND_FAILED, macroName, diagnostic.message, context)
            }
            MacroConstructionDiagnostic.Kind.MACRO_EXPAND_CODE_SHOULD_NOT_HAVE_MACROCALL -> {
                reporter.reportOn(
                    source,
                    CfirErrors.MACRO_EXPAND_CODE_SHOULD_NOT_HAVE_MACROCALL,
                    macroName,
                    diagnostic.message,
                    context,
                )
            }
            MacroConstructionDiagnostic.Kind.MACRO_CALL_SAVE_FILE_FAILED -> {
                reporter.reportOn(source, CfirErrors.MACRO_CALL_SAVE_FILE_FAILED, macroName, diagnostic.message, context)
            }
            MacroConstructionDiagnostic.Kind.MACRO_EXECUTOR_PROTOCOL_ERROR -> {
                reporter.reportOn(source, CfirErrors.MACRO_EXECUTOR_PROTOCOL_ERROR, diagnostic.message, context)
            }
            MacroConstructionDiagnostic.Kind.MACRO_EXECUTOR_SERVER_DISCONNECTED -> {
                reporter.reportOn(source, CfirErrors.MACRO_EXECUTOR_SERVER_DISCONNECTED, diagnostic.message, context)
            }
            MacroConstructionDiagnostic.Kind.MACRO_EXECUTOR_TIMEOUT -> {
                reporter.reportOn(source, CfirErrors.MACRO_EXECUTOR_TIMEOUT, diagnostic.message, context)
            }
            MacroConstructionDiagnostic.Kind.MACRO_EXECUTOR_SERVER_CRASH -> {
                reporter.reportOn(source, CfirErrors.MACRO_EXECUTOR_SERVER_CRASH, diagnostic.message, context)
            }
            MacroConstructionDiagnostic.Kind.MACRO_REEVALUATION_FAILED -> {
                reporter.reportOn(source, CfirErrors.MACRO_REEVALUATION_FAILED, macroName, diagnostic.message, context)
            }
            MacroConstructionDiagnostic.Kind.MACRO_UNRESOLVED -> {
                reporter.reportOn(source, CfirErrors.MACRO_UNRESOLVED, Name.identifier(macroName), context)
            }
            MacroConstructionDiagnostic.Kind.MACRO_CYCLE -> {
                reporter.reportOn(source, CfirErrors.MACRO_CYCLE, macroName, listOf(diagnostic.message), context)
            }
        }
    }

    private fun MacroConstructionDiagnostic.shouldReportToOrdinaryDiagnostics(): Boolean {
        if (severity == MacroConstructionDiagnostic.Severity.ERROR) return true
        return severity == MacroConstructionDiagnostic.Severity.WARNING &&
            diagnosticOrigin == MacroConstructionDiagnostic.Origin.DIAG_REPORT
    }

    private fun MacroConstructionDiagnostic.extractBacktickedName(): String? {
        val first = message.indexOf('`')
        if (first < 0) return null
        val second = message.indexOf('`', startIndex = first + 1)
        if (second <= first + 1) return null
        return message.substring(first + 1, second).removePrefix("@")
    }
}
