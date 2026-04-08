package org.cangnova.cangjie.psi

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.psiUtil.containingTypeStatement

/**
 * 仓颉声明级 use-scope 统一策略。
 *
 * Kotlin analysis 的 `find usages`、`references search`、导航入口都依赖同一套声明作用域语义。
 * 仓颉这里不能把这套规则散落在不同 PSI 类里各自维护，否则：
 * 1. `CjNamedDeclaration`、模式变量、模式绑定会出现搜索边界不一致；
 * 2. `cj-references` 无法稳定复用 `useScope` 作为正式搜索入口；
 * 3. 后续 generated usages 测试很难把作用域语义收敛成主真相。
 *
 * 因此这里将“局部声明 / 私有声明 / 成员声明”的 use-scope 规则抽成统一策略，
 * 由具名声明与模式变量共同复用。
 */
internal fun computeCangJieDeclarationUseScope(
    declaration: CjDeclaration,
    defaultScope: SearchScope,
): SearchScope {
    val enclosingBlock = CjPsiUtil.getEnclosingElementForLocalDeclaration(declaration, false)
    if (enclosingBlock != null) {
        return LocalSearchScope(enclosingBlock)
    }

    val modifierOwner = declaration as? CjModifierListOwner
    if (modifierOwner?.hasModifier(CjTokens.PRIVATE_KEYWORD) == true) {
        val containingClass = PsiTreeUtil.getParentOfType(
            declaration,
            CjTypeStatement::class.java,
        )

        if (containingClass != null) {
            return LocalSearchScope(containingClass)
        }

        val file = declaration.getContainingCjFile()
        if (declaration is CjTypeStatement) {
            val searchScope = GlobalSearchScope.getScopeRestrictedByFileTypes(
                GlobalSearchScope.allScope(declaration.project),
                CangJieFileType.INSTANCE,
            )

            val fileScope: SearchScope = GlobalSearchScope.fileScope(file)
            val nonCangJieScope: SearchScope = GlobalSearchScope.notScope(searchScope)
            return fileScope.union(nonCangJieScope)
        }

        return LocalSearchScope(file)
    }

    var scope = defaultScope
    declaration.containingTypeStatement?.let { containingType ->
        scope = scope.intersectWith(containingType.useScope)
    }

    return scope
}
