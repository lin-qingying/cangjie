package org.cangnova.cangjie.macro

import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjSourceFileLinesMapping

/**
 * 将 CFIR/PSI 源码元素的起始偏移转换为宏协议使用的源码位置。
 */
internal fun CjSourceElement?.toSourcePosition(linesMapping: CjSourceFileLinesMapping?): SourcePosition {
    if (this == null || linesMapping == null) return SourcePosition()
    val (line, column) = linesMapping.getLineAndColumnByOffset(startOffset)
    return SourcePosition(
        line = line,
        column = column,
    )
}

/**
 * 将 CFIR/PSI 源码元素的结束偏移转换为宏协议使用的闭区间尾部位置。
 */
internal fun CjSourceElement?.toEndSourcePosition(linesMapping: CjSourceFileLinesMapping?): SourcePosition {
    if (this == null || linesMapping == null) return SourcePosition()
    val safeOffset = (endOffset - 1).coerceAtLeast(startOffset)
    val (line, column) = linesMapping.getLineAndColumnByOffset(safeOffset)
    return SourcePosition(
        line = line,
        column = column + 1,
    )
}

/**
 * 将可选文本包装成单 token 列表，供缺少词法 token 的宏参数兜底描述使用。
 */
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
