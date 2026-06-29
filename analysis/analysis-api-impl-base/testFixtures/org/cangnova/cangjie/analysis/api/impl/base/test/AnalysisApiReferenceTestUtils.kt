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
    /**
     * 在指定文件中查找用于 Analysis API 引用测试的真实使用点 simple-name。
     *
     * 查找过程会排除声明名和其它不应作为引用使用点的 PSI 位置，并在多个同名候选中选择
     * 文本顺序最后一个使用点，以匹配当前 testData 对目标引用的标记约定。
     */
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

    /**
     * 判断 simple-name 是否可以作为 Analysis API 测试中的引用使用点。
     *
     * 该判断排除声明名和 package directive 中的名字，确保 resolver、reference behavior、symbol restore
     * 等测试不会把非引用位置误交给公开引用 API。
     */
    fun CjSimpleNameExpression.isUsageSimpleNameForAnalysisApiTest(): Boolean {
        if (isDeclarationNameLike()) return false
        if (getStrictParentOfType<CjPackageDirective>() != null) return false

        return true
    }

    /**
     * 判断命名函数是否是 extend 声明中的成员函数。
     *
     * 该工具向测试基类提供稳定的 extend 成员识别逻辑，避免每个测试族重复依赖具体 PSI 父节点形状。
     */
    fun CjNamedFunction.isExtendMemberDeclaration(): Boolean {
        return getStrictParentOfType<CjExtend>() != null
    }

    /**
     * 判断 simple-name 是否处在声明名语义位置。
     *
     * 该私有工具集中处理 binding pattern、变量/枚举 pattern 和 `PsiNameIdentifierOwner` 名称标识符，
     * 用于从测试候选集合中过滤掉不能作为引用使用点的名字。
     */
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
