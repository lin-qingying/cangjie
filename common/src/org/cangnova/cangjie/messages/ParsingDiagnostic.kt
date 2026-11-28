package org.cangnova.cangjie.messages

import org.cangnova.cangjie.parsing.CjParsingSourceSnapshot
import org.cangnova.cangjie.parsing.CjParsingSourceRange

/**
 * 解析器产生的原始词法／语法诊断。没有显式 source 时偏移属于本次输入；继承来源引用其独立快照。
 * 此载体没有 CFIR 诊断工厂；宏构造等调用方只能转交解析结果，不能重建语法规则。
 */
data class ParsingDiagnostic(
    val startOffset: Int,
    val endOffset: Int,
    val message: String,
    val origin: Origin = Origin.INPUT,
    val hints: List<ParsingDiagnosticHint> = emptyList(),
    val source: CjParsingSourceSnapshot? = null,
    val category: Category = Category.PARSE,
    val rangeKind: RangeKind = RangeKind.RANGE,
    /** 原始报告尚未经过根 collector 时缺失；发布入口必须读取完成后的规范范围。 */
    val mainRange: CjParsingDiagnosticRange? = null,
    val nativeHints: List<CjCanonicalParsingDiagnosticHint> = emptyList(),
    /** 原生严重级别；解析警告不会制造 ERROR_ELEMENT 或改变语法成功状态。 */
    val severity: Severity = Severity.ERROR,
    /** WARNING 在真实 producer 处保存其原生身份及格式参数，不能从 message 反推。 */
    val warning: ParsingWarning? = null,
    val mainHint: String? = null,
    val notes: List<String> = emptyList(),
) {
    /** 位置归属由文法报告点确定，消费者不能根据消息或偏移猜测文件级错误。 */
    enum class Origin { INPUT, ORIGINAL_FILE }

    /** 类别来自真实词法／文法 producer，与 severity 独立。 */
    enum class Category { LEX, PARSE }

    enum class Severity { ERROR, WARNING }

    val isError: Boolean get() = severity == Severity.ERROR

    /** POINT 保存真实位置，原生主提示宽一列；EOF 与换行处不能按真实字符范围裁剪。 */
    enum class RangeKind { RANGE, POINT, TOKEN }

    init {
        require(startOffset in 0..endOffset)
        require(rangeKind != RangeKind.POINT || startOffset == endOffset)
        require((severity == Severity.WARNING) == (warning != null)) {
            "A parser warning must retain its native kind and format arguments"
        }
    }
}

/** 根 collector 绑定的规范主/辅助范围；POINT 的结束列可以位于 EOF 或空行之后。 */
data class CjParsingDiagnosticRange(
    val source: CjParsingSourceSnapshot,
    val begin: Pair<Int, Int>,
    val end: Pair<Int, Int>,
    /** 原始字符范围与原生绘制列分别保留；POINT/TOKEN 的结束列不用于反推 UTF-16 偏移。 */
    val sourceRange: CjParsingSourceRange? = null,
) {
    init {
        require(begin.first > 0 && begin.second > 0 && end.first > 0 && end.second > 0)
        require(begin.first < end.first || begin.first == end.first && begin.second <= end.second)
        require(sourceRange == null || sourceRange.source === source)
    }

    /** 原生 range identity 只包含 source/begin/end，原始字符范围是定位证据。 */
    override fun equals(other: Any?): Boolean = other is CjParsingDiagnosticRange &&
        source === other.source && begin == other.begin && end == other.end

    override fun hashCode(): Int = 31 * (31 * source.hashCode() + begin.hashCode()) + end.hashCode()
}

/** 完成的提示范围与显示文字独立；范围已经绑定真实来源，不由 adapter 再分类。 */
data class CjCanonicalParsingDiagnosticHint(val range: CjParsingDiagnosticRange, val label: String?)

/** Parser 明确附加的辅助源码范围；label 是显示文本，null 表示官方诊断未附加文字。 */
data class ParsingDiagnosticHint(
    val startOffset: Int,
    val endOffset: Int,
    val label: String? = null,
    val source: CjParsingSourceSnapshot? = null,
    val rangeKind: ParsingDiagnostic.RangeKind = ParsingDiagnostic.RangeKind.RANGE,
) {
    init {
        require(startOffset in 0..endOffset)
        require(source == null || endOffset <= source.text.length)
        require(rangeKind != ParsingDiagnostic.RangeKind.POINT || startOffset == endOffset)
    }
}
