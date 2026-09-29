package org.cangnova.cangjie.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.stubs.ImportPathStubData
import org.cangnova.cangjie.psi.stubs.elements.CjTokenSets

/** 导入路径的源码所有者；组织引用与包路径分离，绝不返回组合后的虚拟 PSI。 */
interface CjImportPathOwner : CjElement {
    val organizationReference: CjSimpleNameExpression?
    val importedReference: CjExpression?
}

internal fun CjElementImplStub<*>.importOrganizationReference(data: ImportPathStubData?): CjSimpleNameExpression? {
    if (data != null) {
        if (!data.hasOrganizationReference) return null
        return getImportPathExpressions()[0] as CjSimpleNameExpression
    }
    val separator = node.findChildByType(CjTokens.DOUBLE_COLON) ?: return null
    return children.filterIsInstance<CjSimpleNameExpression>().firstOrNull { it.textRange.endOffset <= separator.startOffset }
}

internal fun CjElementImplStub<*>.getImportPathExpressions(): List<CjExpression> =
    getStubOrPsiChildren(CjTokenSets.INSIDE_DIRECTIVE_EXPRESSIONS, CjExpression.ARRAY_FACTORY).filterNotNull()

internal fun CjElementImplStub<*>.importLocalReference(data: ImportPathStubData?): CjExpression? {
    if (data != null) {
        if (!data.hasPathReference) return null
        return getImportPathExpressions()[if (data.hasOrganizationReference) 1 else 0]
    }
    val separator = node.findChildByType(CjTokens.DOUBLE_COLON)
    return getImportPathExpressions().firstOrNull { separator == null || it.textRange.startOffset > separator.startOffset }
}

/** 严格按节点组成限定名；不完整 selector 不得退化为 receiver。 */
internal fun importFqName(expression: CjExpression?): FqName? = when (expression) {
    is CjSimpleNameExpression -> FqName.topLevel(expression.referencedNameAsName)
    is CjDotQualifiedExpression -> {
        val receiver = importFqName(expression.receiverExpression)
        val selector = expression.selectorExpression as? CjSimpleNameExpression
        if (receiver == null || selector == null) null else receiver.child(selector.referencedNameAsName)
    }
    else -> null
}

/** 只检查当前容器的语法，叶项和分组各自记录自己的错误。 */
internal fun hasImportContainerErrors(element: PsiElement): Boolean = element.children.any {
    it is PsiErrorElement || (it !is CjImportItem && it !is CjImportGroup && hasImportContainerErrors(it))
}
