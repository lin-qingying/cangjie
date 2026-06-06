

package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.openapi.util.TextRange
import java.util.*

object DiagnosticRangeUtils {
    @JvmField
    val TEXT_RANGE_COMPARATOR: Comparator<TextRange> = Comparator { o1: TextRange, o2: TextRange ->
        if (o1.startOffset != o2.startOffset) {
            return@Comparator o1.startOffset - o2.startOffset
        }
        o1.endOffset - o2.endOffset
    }

    @JvmStatic
    fun firstRange(ranges: List<TextRange>): TextRange {
        return ranges.minWith(TEXT_RANGE_COMPARATOR)
    }
}

