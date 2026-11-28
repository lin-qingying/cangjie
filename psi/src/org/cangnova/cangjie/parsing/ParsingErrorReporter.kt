package org.cangnova.cangjie.parsing

import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.CjParsingDiagnosticCollector
import org.cangnova.cangjie.messages.CjParsingDiagnosticRange
import org.cangnova.cangjie.messages.CjParsingDiagnosticPublication
import org.cangnova.cangjie.messages.CjParsingDiagnosticReport
import org.cangnova.cangjie.messages.CompilerMessageLocationWithRange
import org.cangnova.cangjie.messages.CompilerMessageRelatedLocation
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.ParsingDiagnosticSettings
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjFileRoleErrorElement
import org.cangnova.cangjie.psi.CjSourceRangeErrorElement
import org.cangnova.cangjie.psi.CjTokenRangeErrorElement
import org.cangnova.cangjie.psi.CjOrderedParsingErrorElement

/** 只发布根 collector 已完成的规范范围；adapter 不再分类、去重、重排或重建来源。 */
fun MessageCollector.toCangJieParsingErrorListener(): CangJieLightParser.LightTreeParsingErrorListener =
    CangJieLightParser.LightTreeParsingErrorListener { diagnostic ->
        val range = checkNotNull(diagnostic.mainRange) { "A parser diagnostic must finish its root collector before publication." }
        val related = diagnostic.nativeHints.mapNotNull { hint ->
            val location = sourceRangeLocation(hint.range) ?: return@mapNotNull null
            CompilerMessageRelatedLocation(location, hint.label)
        }
        report(if (diagnostic.isError) CompilerMessageSeverity.ERROR else CompilerMessageSeverity.WARNING,
            diagnostic.message, sourceRangeLocation(range, related, diagnostic))
    }

private fun sourceRangeLocation(
    range: CjParsingDiagnosticRange,
    relatedLocations: List<CompilerMessageRelatedLocation> = emptyList(),
    diagnostic: ParsingDiagnostic? = null,
): CompilerMessageLocationWithRange? = CompilerMessageLocationWithRange.createWithRelatedLocations(
    range.source.filePath,
    range.begin.first,
    range.begin.second,
    range.end.first,
    range.end.second,
    range.source.lineContent(range.begin.first),
    relatedLocations,
    diagnostic?.mainHint,
    diagnostic?.notes.orEmpty(),
    diagnostic?.warning?.nativeKind,
    diagnostic?.category,
)

/** PSI 与 LightTree 共用同一完成结果；源树遍历始终位于一次平台读事务中。 */
fun MessageCollector.reportSyntaxErrors(file: CjFile): Boolean {
    val diagnostics = file.collectParsingDiagnostics()
    val listener = toCangJieParsingErrorListener()
    diagnostics.forEach(listener::onError)
    return diagnostics.any { it.isError }
}

/** 收集当前真实 PSI 代次的诊断；历史根输入不是局部提交后的当前坐标视图。 */
fun CjFile.collectParsingDiagnostics(
    settings: ParsingDiagnosticSettings? = null,
): List<ParsingDiagnostic> = collectParsingDiagnosticPublication(settings).diagnostics

/** 完整规范化报告保留 producer 身份，后续宏来源映射不得以位置排序后的输出重新选择首报。 */
fun CjFile.collectParsingDiagnosticPublication(
    settings: ParsingDiagnosticSettings? = null,
): CjParsingDiagnosticPublication =
    ReadAction.compute<CjParsingDiagnosticPublication, RuntimeException> {
        val view = currentPsiSourceView()
        val context = view.sourceContext
        val rootSettings = checkNotNull(getCopyableUserData(ROOT_PARSER_SEED_KEY)) {
            "A source parser must retain its invocation input"
        }.settings.diagnostics
        val collector = CjParsingDiagnosticCollector(context, settings ?: rootSettings)
        val visitor = CangJieParsingDiagnosticVisitor(collector::restoreReport)
        accept(visitor)
        visitor.restoreNonErrorReports()
        view.requireCurrent()
        collector.finish()
        collector.publication
    }

/** PSI 与 LightTree 共用的默认态未知字符诊断构造。 */
internal fun lexicalDiagnosticForToken(
    tokenType: IElementType,
    tokenText: String,
    startOffset: Int,
    endOffset: Int,
): ParsingDiagnostic? {
    if (tokenType !== CjTokens.UNKNOWN_TOKEN_START) return null
    require(tokenText.isNotEmpty()) { "An unknown-token diagnostic must have a source character." }
    return ParsingDiagnostic(
        startOffset,
        startOffset,
        CangJieParsingBundle.message("parsing.error.unknown.token.start", nativeCharacterName(tokenText)),
        category = ParsingDiagnostic.Category.LEX,
        rangeKind = ParsingDiagnostic.RangeKind.POINT,
    )
}

/** 对齐 v1.1.3 ConvertChar：保留 Unicode 字符，控制字符使用四位大写码点。 */
private fun nativeCharacterName(tokenText: String): String {
    val codePoint = Character.codePointAt(tokenText, 0)
    return when {
        codePoint == '\t'.code -> "tab character"
        codePoint == '\n'.code -> "new line character"
        codePoint < 0x20 || codePoint == 0x7f -> "\\u{${codePoint.toString(16).uppercase().padStart(4, '0')}}"
        else -> String(Character.toChars(codePoint))
    }
}

/** 统一遍历 PSI 错误节点和 lexer 产生的未知字符叶；SDK lazy 节点在子遍历时按真实协议展开。 */
private class CangJieParsingDiagnosticVisitor(
    private val onDiagnostic: (CjParsingDiagnosticReport) -> Unit,
) : PsiRecursiveElementWalkingVisitor() {
    private val tokenOffsets = linkedMapOf<CjSourceTokenId, Int>()
    private val nonErrorReports = mutableListOf<Pair<Int, CjStoredParsingReport>>()
    var hasErrors: Boolean = false
        private set

    override fun visitErrorElement(element: PsiErrorElement) {
        val producer = checkNotNull(element as? CjOrderedParsingErrorElement) {
            "A parser error node must retain its actual producer report identity."
        }
        report(CjParsingDiagnosticReport(element.toParsingDiagnostic(), producer.diagnosticOrder))
        super.visitErrorElement(element)
    }

    override fun visitElement(element: com.intellij.psi.PsiElement) {
        element.node.parsedSourceTokenOrNull()?.let { token ->
            check(tokenOffsets.put(token, element.textRange.startOffset) == null) {
                "A lexical token cannot have two current PSI carriers"
            }
        }
        element.node.getCopyableUserData(PARSER_NON_ERROR_REPORTS_KEY)?.let { reports ->
            val token = checkNotNull(element.node.parsedSourceTokenOrNull()) { "A parser warning requires its actual lexical carrier" }
            reports.forEach {
                check(it.anchorToken === token) { "A parser warning requires its original lexical carrier" }
                nonErrorReports += element.textRange.startOffset to it
            }
        }
        val tokenType = element.node.elementType
        if (tokenType === CjTokens.UNKNOWN_TOKEN_START) {
            val token = checkNotNull(element.node.parsedSourceTokenOrNull()) {
                "An unknown token must retain its actual lexical producer identity."
            }
            val origin = checkNotNull(element.node.getCopyableUserData(PARSER_LEXICAL_DIAGNOSTIC_KEY)) {
                "An unknown leaf must retain its actual frozen lexical report."
            }
            check(token === origin.token.id && origin.token.type === tokenType &&
                element.text == origin.source.text.substring(origin.token.startOffset, origin.token.endOffset)) {
                "A lexical report must remain bound to its actual PSI token carrier."
            }
            val point = element.textRange.startOffset
            report(origin.report.copy(diagnostic = origin.report.diagnostic.copy(
                startOffset = point, endOffset = point, source = null,
            )))
        }
        super.visitElement(element)
    }

    private fun report(diagnostic: CjParsingDiagnosticReport) {
        if (diagnostic.diagnostic.isError) hasErrors = true
        onDiagnostic(diagnostic)
    }

    /** 先完整展开实际 PSI，再按每个辅助词元的当前坐标恢复报告；访问次序不决定 producer 次序。 */
    fun restoreNonErrorReports() {
        nonErrorReports.forEach { (offset, stored) -> report(stored.atCarrier(offset, stored.anchorToken, tokenOffsets)) }
    }
}

/** PSI 和 LightTree 根据同一文法节点类型发布位置归属。 */
private fun PsiErrorElement.toParsingDiagnostic(): ParsingDiagnostic {
    val range = when (this) {
        is CjTokenRangeErrorElement -> reportedRange
        is CjSourceRangeErrorElement -> reportedRange
        else -> textRange
    }
    val hints = (this as CjOrderedParsingErrorElement).reportedHints
    return ParsingDiagnostic(
        startOffset = range.startOffset,
        endOffset = range.endOffset,
        message = errorDescription,
        origin = if (this is CjFileRoleErrorElement) {
            ParsingDiagnostic.Origin.ORIGINAL_FILE
        } else {
            ParsingDiagnostic.Origin.INPUT
        },
        hints = hints,
        source = (this as? CjSourceRangeErrorElement)?.reportedSource,
        category = when (this) {
            is CjSourceRangeErrorElement -> category
            is CjTokenRangeErrorElement -> category
            else -> ParsingDiagnostic.Category.PARSE
        },
        rangeKind = when (this) {
            is CjSourceRangeErrorElement -> rangeKind
            is CjTokenRangeErrorElement -> ParsingDiagnostic.RangeKind.TOKEN
            else -> ParsingDiagnostic.RangeKind.RANGE
        },
    )
}
