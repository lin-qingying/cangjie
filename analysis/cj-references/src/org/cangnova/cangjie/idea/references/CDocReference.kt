package org.cangnova.cangjie.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocName
import org.cangnova.cangjie.name.Name

/**
 * CDoc 名称节点的仓颉引用基类。
 *
 * CDoc 引用使用多目标解析结果，并按项目服务选择传统或实验性解析结果收敛策略。
 */
abstract class CDocReference(element: CDocName) : CjMultiReference<CDocName>(element) {
    /**
     * 返回 CDoc 名称文本在元素内部的引用范围。
     */
    override fun getRangeInElement(): TextRange = element.getNameTextRange()

    /**
     * CDoc 名称引用允许通过 rename 修改名称文本。
     */
    override fun canRename(): Boolean = true

    /**
     * 解析 CDoc 引用的单目标结果。
     */
    override fun resolve(): PsiElement? = multiResolve(incompleteCode = false).let { resolvedResults ->
        if (CangJieCDocResolutionStrategyProviderService.getService(element.project)?.shouldUseExperimentalStrategy() == true) {

            resolvedResults.singleOrNull()
        } else {
            resolvedResults.firstOrNull()
        }
    }?.element

    /**
     * 返回 CDoc 引用的规范文本。
     */
    override fun getCanonicalText(): String = element.getNameText()

    /**
     * 返回该 CDoc 引用可能解析到的名称集合。
     */
    override val resolvesByNames: Collection<Name>
        get() {
            val element = element
            val name = element.getNameText()

            // Text check is required to distinguish between '`this`'/'`super`' and 'this'/'super' cases
            if (name in FORBIDDEN_NAMES && element.textMatches(name)) {
                // According to the KDoc, `this`/`super` cannot be properly expressed in terms of this API
                return emptyList()
            }

            return listOfNotNull(
                Name.identifier(name),
                // A property might resolve into a getter function
            )
        }
}

/**
 * CDoc 引用名称过滤中不应作为普通名称参与搜索的关键字列表。
 */
private val FORBIDDEN_NAMES = listOf(CjTokens.THIS_KEYWORD.value, CjTokens.SUPER_KEYWORD.value)
