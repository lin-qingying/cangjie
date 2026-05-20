package org.cangnova.cangjie.analysis.api.impl.base.test

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjNamedPattern
import org.cangnova.cangjie.psi.CjPackageDirective
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjVarOrEnumPattern
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType

/**
 * analysis generated 测试里“按名字找使用点 / 找目标声明”的统一工具。
 *
 * 这些工具的职责不是简化断言，而是把 PSI 形态差异统一收敛掉：
 * 1. 声明名位置本身不应被当成真正的引用使用点；
 * 2. extend 成员不应依赖“直接 parent 恰好是 CjExtend”这类脆弱条件；
 * 3. 各测试族共享同一套目标抽取协议，避免出现一族修好、另一族继续漂移。
 */
internal object AnalysisApiReferenceTestUtils {
    fun findUsageSimpleName(
        file: CjFile,
        referencedName: String,
    ): CjSimpleNameExpression {
        return PsiTreeUtil.findChildrenOfType(file, CjSimpleNameExpression::class.java)
            .asSequence()
            .filter { expression -> expression.referencedName == referencedName }
            .filterNot { expression -> expression.isDeclarationNameLike() }
            .sortedBy { expression -> expression.getTextOffset() }
            .lastOrNull()
            ?: error("Cannot locate usage simple-name `$referencedName` in `${file.name}`")
    }

    fun CjSimpleNameExpression.isUsageSimpleNameForAnalysisApiTest(): Boolean {
        if (isDeclarationNameLike()) return false
        if (getStrictParentOfType<CjPackageDirective>() != null) return false

        return true
    }

    fun CjNamedFunction.isExtendMemberDeclaration(): Boolean {
        return getStrictParentOfType<CjExtend>() != null
    }

    private fun CjSimpleNameExpression.isDeclarationNameLike(): Boolean {
        if (this is CjBindingPattern) return true
        if (parent is CjNamedPattern || parent is CjVarOrEnumPattern) return true

        return generateSequence(this as PsiElement?) { current -> current.parent }
            .filterIsInstance<PsiNameIdentifierOwner>()
            .any { owner ->
                val nameIdentifier = owner.nameIdentifier ?: return@any false
                nameIdentifier == this || PsiTreeUtil.isAncestor(nameIdentifier, this, false)
            }
    }
}
