package org.cangnova.cangjie.idea.search

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.tree.TokenSet
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjImportAlias
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjTypePattern
import org.cangnova.cangjie.psi.CjTypeParameter
import org.cangnova.cangjie.psi.CjTypeStatement

/**
 * 为仓颉语言提供 IntelliJ Find Usages 入口。
 *
 * 引用解析、`useScope` 和导航已经由 analysis/psi 主线负责，这里只补齐
 * IntelliJ 需要的语言级桥接，避免“能 resolve 但没有正式查找用法入口”的断层。
 */
class CangJieFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner {
        return DefaultWordsScanner(
            CangJieLexer(),
            TokenSet.create(CjTokens.IDENTIFIER, CjTokens.FIELD_IDENTIFIER),
            CjTokens.COMMENTS,
            TokenSet.EMPTY,
        )
    }

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean {
        return psiElement.language == CangJieLanguage && psiElement is PsiNamedElement && !psiElement.name.isNullOrBlank()
    }

    override fun getHelpId(psiElement: PsiElement): String? = null

    override fun getType(element: PsiElement): String = when (element) {
        is CjNamedFunction -> "function"
        is CjProperty -> "property"
        is CjExtend -> "extend"
        is CjTypeStatement -> "type"
        is CjParameter -> "parameter"
        is CjTypeParameter -> "type parameter"
        is CjBindingPattern -> "pattern binding"
        is CjTypePattern -> "typed pattern"
        is CjImportAlias -> "import alias"
        is CjNamedDeclaration -> "declaration"
        else -> "symbol"
    }

    override fun getDescriptiveName(element: PsiElement): String {
        return (element as? PsiNamedElement)?.name ?: element.text
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        return when {
            element is CjNamedDeclaration && !element.name.isNullOrBlank() -> element.name!!
            element is PsiNamedElement && !element.name.isNullOrBlank() -> element.name!!
            else -> element.text
        }
    }
}
