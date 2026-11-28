package org.cangnova.cangjie.parsing

import org.cangnova.cangjie.messages.CjParsingDiagnosticReport
import org.cangnova.cangjie.messages.ParsingDiagnostic
import java.util.Collections

/** 辅助范围具有自己的词元锚点；interface 关键字不能跟随被移动的成员一起平移。 */
internal data class CjStoredParsingHintAnchor(val offset: Int, val token: CjSourceTokenId)

/** 非错误报告绑定真实词元；当前 PSI 移动时仅平移本输入的范围，继承来源保持独立。 */
internal data class CjStoredParsingReport(
    val anchorOffset: Int,
    val anchorToken: CjSourceTokenId,
    val report: CjParsingDiagnosticReport,
    val hintAnchors: List<CjStoredParsingHintAnchor?>,
) {
    init {
        require(hintAnchors.size == report.diagnostic.hints.size)
        require(hintAnchors.zip(report.diagnostic.hints).all { (anchor, hint) ->
            (anchor != null) == (hint.source == null)
        })
    }

    fun atCarrier(offset: Int, token: CjSourceTokenId, tokenOffsets: Map<CjSourceTokenId, Int>): CjParsingDiagnosticReport {
        check(token === anchorToken) { "A parser report must retain its actual lexical carrier" }
        val diagnostic = report.diagnostic
        val shift = offset - anchorOffset
        val inputRange = diagnostic.source == null && diagnostic.origin == ParsingDiagnostic.Origin.INPUT
        return report.copy(diagnostic = diagnostic.copy(
            startOffset = diagnostic.startOffset + if (inputRange) shift else 0,
            endOffset = diagnostic.endOffset + if (inputRange) shift else 0,
            hints = Collections.unmodifiableList(diagnostic.hints.mapIndexed { index, hint ->
                val anchor = hintAnchors[index] ?: return@mapIndexed hint
                val hintShift = checkNotNull(tokenOffsets[anchor.token]) {
                    "A parser hint must retain its own actual lexical carrier"
                } - anchor.offset
                hint.copy(startOffset = hint.startOffset + hintShift, endOffset = hint.endOffset + hintShift)
            }),
            mainRange = null,
            nativeHints = emptyList(),
        ))
    }
}
