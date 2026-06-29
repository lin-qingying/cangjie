package org.cangnova.cangjie.codeinsight.folding

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

/**
 * 仓颉共享折叠区域。
 *
 * `element` 是 IDE 侧生成 `FoldingDescriptor` 的 PSI 锚点；LSP 侧只消费文本范围和折叠类型。
 */
data class CangJieFoldingRegion(
    /** 折叠区域对应的 PSI 锚点。 */
    val element: PsiElement,
    /** 可折叠文本范围。 */
    val range: TextRange,
    /** 折叠区域的协议级类别。 */
    val kind: CangJieFoldingKind,
    /** 折叠后展示的占位文本，允许由宿主使用默认值。 */
    val placeholderText: String?,
    /** 折叠状态下是否允许宿主删除该区域。 */
    val canBeRemovedWhenCollapsed: Boolean,
)
