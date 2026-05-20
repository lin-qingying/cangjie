package org.cangnova.cangjie.codeinsight.folding

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

/**
 * 仓颉共享折叠区域。
 *
 * `element` 是 IDE 侧生成 `FoldingDescriptor` 的 PSI 锚点；LSP 侧只消费文本范围和折叠类型。
 */
data class CangJieFoldingRegion(
    val element: PsiElement,
    val range: TextRange,
    val kind: CangJieFoldingKind,
    val placeholderText: String?,
    val canBeRemovedWhenCollapsed: Boolean,
)
