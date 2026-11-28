package org.cangnova.cangjie.cfir.pipeline

import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.parsingDiagnosticPublication
import org.cangnova.cangjie.cfir.declarations.parsingFileFacts
import org.cangnova.cangjie.cfir.declarations.parsingSourceContext
import org.cangnova.cangjie.cfir.declarations.parserSettings
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.InternalDiagnosticFactoryMethod
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroParserDiagnosticSourceMap
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.session.macroExpansionRegistry
import org.cangnova.cangjie.messages.CjParsingDiagnosticCollector
import org.cangnova.cangjie.messages.CjParsingDiagnosticPublication
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.ParsingDiagnosticSettings
import org.cangnova.cangjie.messages.ParsingWarning
import org.cangnova.cangjie.messages.ParsingDiagnosticHint
import org.cangnova.cangjie.messages.CjParsingDiagnosticReport
import org.cangnova.cangjie.parsing.token
import org.cangnova.cangjie.parsing.CjParserSourceContext
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.source.CjSourceElement
import java.util.IdentityHashMap

private class CfirParsingDiagnosticUnit(
    val publication: CjParsingDiagnosticPublication,
    val owners: IdentityHashMap<ParsingDiagnostic, CfirFile>,
)

/** 完整 source/宏 parser 事务的纯收集入口；驱动可以在发布 WARNING 前判断早期语法失败。 */
fun collectParsingDiagnosticPublication(
    files: Collection<CfirFile>,
    settings: ParsingDiagnosticSettings? = null,
): CjParsingDiagnosticPublication = collectParsingDiagnosticUnit(files, settings).publication

private fun collectParsingDiagnosticUnit(files: Collection<CfirFile>, settings: ParsingDiagnosticSettings?): CfirParsingDiagnosticUnit {
    val inputs = files.distinct().mapNotNull { file ->
        val publication = file.parsingDiagnosticPublication
        check(publication != null || file.parsingFileFacts == null) {
            "A source raw file must retain its complete parser diagnostic product"
        }
        publication?.let { file to it }
    }
    val owners = IdentityHashMap<ParsingDiagnostic, CfirFile>()
    if (inputs.isEmpty()) return CfirParsingDiagnosticUnit(CjParsingDiagnosticPublication.EMPTY, owners)
    val effectiveSettings = settings ?: inputs.map { checkNotNull(it.first.parserSettings).diagnostics }.distinct().single()
    val reports = inputs.flatMapTo(mutableListOf()) { (file, publication) ->
        publication.reports.onEach { owners.putIfAbsent(it.diagnostic, file) }
    }
    val registries = inputs.mapNotNull { it.first.moduleData.session.macroExpansionRegistry }.distinct()
    for (registry in registries) {
        for (record in registry.parsingDiagnosticRecords) {
            val surface = record.originNode.surface
            // 只读取调用前冻结的宿主输入；保留文件原地换代和同一 session 的其它源码不能重新绑定旧宏范围。
            val file = inputs.firstOrNull { (input, publication) -> publication === record.originDiagnosticPublication &&
                    input.parsingSourceContext === record.originSourceContext }?.first ?: continue
            val mapped = mapMacroParsingReports(record.originSourceContext, surface,
                record.diagnosticPublication, record.sourceMap, record.syntaxFailure)
            mapped.forEach { owners[it.diagnostic] = file }
            reports += mapped
        }
    }
    return CfirParsingDiagnosticUnit(CjParsingDiagnosticPublication(
        CjParsingDiagnosticCollector.publishSyntaxDiagnostics(reports, effectiveSettings), reports,
    ), owners)
}

/** 宏的临时输入坐标仅经其已保存的 source map 回到真实宿主，不按 package 或消息重建来源。 */
private fun mapMacroParsingReports(
    context: CjParserSourceContext,
    surface: MacroSurface,
    publication: CjParsingDiagnosticPublication,
    sourceMap: MacroParserDiagnosticSourceMap?,
    syntaxFailure: Boolean,
): List<CjParsingDiagnosticReport> {
    if (publication.reports.isEmpty()) return emptyList()
    val collector = CjParsingDiagnosticCollector(context)
    for (report in publication.reports) {
        val diagnostic = report.diagnostic
        val originalFile = diagnostic.origin == ParsingDiagnostic.Origin.ORIGINAL_FILE
        val mappedRange = when {
            diagnostic.source != null || originalFile -> null
            syntaxFailure && diagnostic.isError -> checkNotNull(surface.sourceRange).let { com.intellij.openapi.util.TextRange(it.startOffset, it.endOffset) }
            else -> checkNotNull(checkNotNull(sourceMap).mapRange(diagnostic.startOffset, diagnostic.endOffset)).let {
                com.intellij.openapi.util.TextRange(it.startOffset, it.endOffset)
            }
        }
        val hints = diagnostic.hints.map { hint ->
            if (hint.source != null || originalFile) hint else {
                val range = checkNotNull(checkNotNull(sourceMap).mapRange(hint.startOffset, hint.endOffset)) {
                    "A macro parser hint must map to its actual host source"
                }
                ParsingDiagnosticHint(range.startOffset, range.endOffset, hint.label,
                    rangeKind = if (range.startOffset == range.endOffset) hint.rangeKind else ParsingDiagnostic.RangeKind.RANGE)
            }
        }
        collector.restoreReport(report.copy(diagnostic = diagnostic.copy(
            startOffset = when { diagnostic.source != null -> diagnostic.startOffset; originalFile -> 0; else -> checkNotNull(mappedRange).startOffset },
            endOffset = when { diagnostic.source != null -> diagnostic.endOffset; originalFile -> 0; else -> checkNotNull(mappedRange).endOffset },
            hints = hints,
            rangeKind = when {
                diagnostic.source != null -> diagnostic.rangeKind
                originalFile -> ParsingDiagnostic.RangeKind.POINT
                checkNotNull(mappedRange).startOffset == mappedRange.endOffset -> diagnostic.rangeKind
                else -> ParsingDiagnostic.RangeKind.RANGE
            },
            mainRange = null, nativeHints = emptyList(),
        )))
    }
    collector.finish()
    return collector.collectedReports
}

/**
 * 正规语法报告进入 typed CFIR collector 的共享边界。producer、suppression、两层去重和 LEX 门
 * 均属于 parser publication；这里只按已保存的 warning 身份选择已有语言工厂，不重跑修饰符规则。
 */
@OptIn(InternalDiagnosticFactoryMethod::class)
fun DiagnosticReporter.reportParsingDiagnostics(
    files: Collection<CfirFile>,
    settings: ParsingDiagnosticSettings? = null,
): CjParsingDiagnosticPublication {
    val unit = collectParsingDiagnosticUnit(files, settings)
    for (parsing in unit.publication.diagnostics) {
        val warning = parsing.warning ?: continue
        val file = checkNotNull(unit.owners[parsing]) { "Publication must retain its original report owner" }
        val range = checkNotNull(checkNotNull(parsing.mainRange).sourceRange) {
            "A typed parser report requires its producer's exact character range"
        }
        val source = CjOffsetsOnlySourceElement(range.startOffset, range.endOffset,
            (file.source as? CjSourceElement)?.takeIf { file.parsingSourceContext?.snapshot === range.source })
        val context = object : DiagnosticContext {
            override val languageVersionSettings = file.moduleData.session.languageVersionSettings
            override val containingFilePath = range.source.filePath ?: range.source.fileName
            override val parsingDiagnostic: ParsingDiagnostic = parsing
            override fun isDiagnosticSuppressed(diagnostic: CjDiagnostic): Boolean = false
        }
        val typed = when (warning) {
            is ParsingWarning.RedundantModifier -> when (val reason = warning.reason) {
                is ParsingWarning.Redundancy.Explicit -> CfirErrors.REDUNDANT_MODIFIER.on(
                    source, warning.modifier.token(), reason.higherModifier.token(), null, context,
                )
                ParsingWarning.Redundancy.InterfaceMember -> CfirErrors.REDUNDANT_MODIFIER_FOR_TARGET.on(
                    source, warning.modifier.token(), "interface member", null, context,
                )
            }
            is ParsingWarning.UnsafeIgnored -> CfirErrors.UNSAFE_WILL_BE_IGNORED.on(source, warning.backend, null, context)
            is ParsingWarning.ConfusingLineTerminator -> CfirErrors.CONFUSING_LINE_TERMINATOR.on(source, null, context)
            is ParsingWarning.EmptyAttribute -> CfirErrors.EMPTY_ANNOTATION_ATTRIBUTE.on(source, warning.annotationName, null, context)
            is ParsingWarning.DuplicatedAttributeValue -> CfirErrors.DUPLICATED_ANNOTATION_ATTRIBUTE_VALUE.on(source, warning.value, null, context)
            ParsingWarning.JavaMirrorPropertyDeprecated -> CfirErrors.JAVA_MIRROR_PROPERTY_DEPRECATED.on(source, null, context)
            is ParsingWarning.MacroReport -> CfirErrors.MACRO_DIAG_REPORT_WARNING.on(source, warning.message, warning.hint, null, context)
        }
        checkNotNull(typed) { "Parser invocation suppression must precede typed factory construction" }
        check(typed.renderMessage() == parsing.message) { "A parser factory must render the native main message exactly" }
        report(typed, context)
    }
    return unit.publication
}
