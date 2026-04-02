package org.cangnova.cangjie.macro

import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjSourceFileLinesMapping

internal fun CjSourceElement?.toSourcePosition(linesMapping: CjSourceFileLinesMapping?): SourcePosition {
    if (this == null || linesMapping == null) return SourcePosition()
    val (line, column) = linesMapping.getLineAndColumnByOffset(startOffset)
    return SourcePosition(
        line = line,
        column = column,
    )
}

internal fun CjSourceElement?.toEndSourcePosition(linesMapping: CjSourceFileLinesMapping?): SourcePosition {
    if (this == null || linesMapping == null) return SourcePosition()
    val safeOffset = (endOffset - 1).coerceAtLeast(startOffset)
    val (line, column) = linesMapping.getLineAndColumnByOffset(safeOffset)
    return SourcePosition(
        line = line,
        column = column + 1,
    )
}

internal fun String?.asSingleTokenList(
    begin: SourcePosition,
    end: SourcePosition,
): List<TokenInfo> {
    if (this.isNullOrEmpty()) return emptyList()
    return listOf(
        TokenInfo(
            kind = 0u,
            value = this,
            begin = begin,
            end = end,
        )
    )
}
