package org.cangnova.cangjie.codeinsight.hints

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.CaMacroExpansionMapping
import org.cangnova.cangjie.psi.CjErrorElement
import org.cangnova.cangjie.psi.CjFile

/**
 * 各收集器共用的候选过滤。
 *
 * 这些判定与「哪一类提示」无关：位置是否在请求范围内、是否处在语法错误里、
 * 是否落在宏展开区间内。集中一处，避免各收集器各写一遍后规则漂移。
 */
internal object CjInlayHintFilters {
    /**
     * 位置是否落在请求范围内。
     *
     * 两端都算命中：提示恰好落在 range 末尾（视图底行行尾）时不该被裁掉。
     */
    fun isInRange(offset: Int, range: TextRange?): Boolean =
        range == null || (offset >= range.startOffset && offset <= range.endOffset)

    /**
     * 祖先链上是否存在错误节点。
     */
    fun hasErrorAncestor(element: PsiElement): Boolean {
        var current: PsiElement? = element
        while (current != null && current !is CjFile) {
            if (current is CjErrorElement) return true
            current = current.parent
        }
        return false
    }

    /**
     * 子树内是否存在错误节点。
     */
    fun containsErrorElement(element: PsiElement): Boolean =
        PsiTreeUtil.findChildOfType(element, CjErrorElement::class.java) != null

    /**
     * 位置是否落在宏展开区间内。
     *
     * 宏调用区间内的 PSI 由展开产物决定，源码视角的提示会指向用户没写的代码。
     */
    fun isInsideMacroExpansion(
        file: CjFile,
        offset: Int,
        session: CaSession,
    ): Boolean = with(session) {
        macroExpansionMappingAt(file, offset) !is CaMacroExpansionMapping.None
    }
}
