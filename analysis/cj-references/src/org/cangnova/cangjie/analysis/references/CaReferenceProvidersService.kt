package org.cangnova.cangjie.analysis.references

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CangJieReferenceProvidersService
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjSimpleNameExpression

/**
 * 基于 Analysis API 的引用提供器。
 *
 * 当前先把 simple-name 的基础解析接入 Analysis API，解析顺序保持稳定：
 * 1. 优先解析当前文件中可直接定位的声明；
 * 2. 再解析同包下的 class-like 符号；
 * 3. 最后解析同包下的顶层 callable。
 *
 * 这里保留统一入口，后续成员解析、导入解析和跨模块可见性解析都应在此基础上扩展，
 * 而不是在各个 PSI 节点上各自分叉。
 */
class CaReferenceProvidersService : CangJieReferenceProvidersService() {
    override fun getReferences(psiElement: PsiElement): Array<PsiReference> {
        val simpleNameExpression = psiElement as? CjSimpleNameExpression ?: return PsiReference.EMPTY_ARRAY
        return arrayOf(CaSimpleNameReference(simpleNameExpression))
    }
}

/**
 * 由 Analysis API 驱动的 simple-name 引用。
 */
private class CaSimpleNameReference(
    expression: CjSimpleNameExpression,
) : PsiReferenceBase<CjSimpleNameExpression>(
    expression,
    expression.referencedNameElement.textRangeInParent,
    false,
) {
    override fun resolve(): PsiElement? {
        val containingFile = element.containingFile as? CjFile ?: return null
        val referencedName = element.referencedName
        val referencedNameAsName = element.referencedNameAsName

        containingFile.declarations
            .filterIsInstance<CjNamedDeclaration>()
            .firstOrNull { declaration -> declaration.name == referencedName }
            ?.let { return it }

        return analyze(element) {
            containingFile.getFileScope()
                .getSymbols(referencedNameAsName)
                .asSequence()
                .mapNotNull { symbol -> symbol.getOriginalPsi() }
                .firstOrNull()
        }
    }

    override fun getVariants(): Array<Any> {
        val containingFile = element.containingFile as? CjFile ?: return emptyArray()

        return analyze(element) {
            buildSet {
                addAll(
                    containingFile.declarations
                        .filterIsInstance<CjNamedDeclaration>()
                        .mapNotNull { declaration -> declaration.name },
                )

                val fileScope = containingFile.getFileScope()
                addAll(fileScope.availableNames.map(Name::asString))
            }.toTypedArray()
        }
    }
}
