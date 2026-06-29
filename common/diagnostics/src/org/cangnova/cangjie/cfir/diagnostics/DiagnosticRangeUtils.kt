

package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.openapi.util.TextRange
import java.util.*

/**
 * 诊断文本范围排序与选择工具。
 */
object DiagnosticRangeUtils {
    /**
     * 按起始偏移优先、结束偏移次之排序诊断范围的比较器。
     */
    @JvmField
    val TEXT_RANGE_COMPARATOR: Comparator<TextRange> = Comparator { o1: TextRange, o2: TextRange ->
        if (o1.startOffset != o2.startOffset) {
            return@Comparator o1.startOffset - o2.startOffset
        }
        o1.endOffset - o2.endOffset
    }

    /**
     * 返回范围集合中最靠前的文本范围。
     */
    @JvmStatic
    fun firstRange(ranges: List<TextRange>): TextRange {
        return ranges.minWith(TEXT_RANGE_COMPARATOR)
    }
}
