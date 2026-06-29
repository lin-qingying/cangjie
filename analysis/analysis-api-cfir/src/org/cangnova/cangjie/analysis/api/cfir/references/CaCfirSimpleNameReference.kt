package org.cangnova.cangjie.analysis.api.cfir.references

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.impl.base.references.CaBaseSimpleNameReference
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.cfir.expressions.CfirLoopJump
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.psi.CjConstructorCalleeExpression
import org.cangnova.cangjie.psi.CjImportAlias
import org.cangnova.cangjie.psi.CjLabelReferenceExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjTypeReference
import org.cangnova.cangjie.psi.CjUserType
import org.cangnova.cangjie.psi.CjValueArgumentName
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.psiUtil.getAssignmentByLHS
import org.cangnova.cangjie.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.cangnova.cangjie.references.CangJiePsiReferenceProviderContributor

/**
 * 基于 CFIR Analysis API 的简单名引用实现。
 */
@OptIn(CaImplementationDetail::class)
internal class CaCfirSimpleNameReference(
    expression: CjSimpleNameExpression,
    /**
     * 当前引用是否表示读取访问。
     */
    val isRead: Boolean,
) : CaBaseSimpleNameReference(expression), CaCfirReference {
    /**
     * 当前引用使用的 CFIR resolver。
     */
    override val resolver get() = CaCfirReferenceResolver

    /**
     * 当前简单名是否位于注解构造调用位置。
     */
    private val isAnnotationCall: Boolean
        get() {
            val ktUserType = expression.parent as? CjUserType ?: return false
            val ktTypeReference = ktUserType.parent as? CjTypeReference ?: return false
            val ktConstructorCalleeExpression = ktTypeReference.parent as? CjConstructorCalleeExpression ?: return false
            return ktConstructorCalleeExpression.parent is CjAnnotation
        }

    /**
     * 对注解调用解析结果进行构造器修正。
     */
    private fun CaSession.fixUpAnnotationCallResolveToCtor(resultsToFix: Collection<CaSymbol>): Collection<CaSymbol> {
        if (resultsToFix.isEmpty() || !isAnnotationCall) return resultsToFix

        return resultsToFix
    }

    /**
     * 判断当前简单名引用是否指向指定 import alias。
     */
    override fun isReferenceToImportAlias(alias: CjImportAlias): Boolean {
        return super<CaCfirReference>.isReferenceToImportAlias(alias)
    }

    /**
     * 通过 CFIR helper 计算当前简单名引用的公开符号集合。
     */
    override fun CaCfirSession.computeSymbols(): Collection<CaSymbol> {
        val results = CfirReferenceResolveHelper.resolveSimpleNameReference(this@CaCfirSimpleNameReference, this)
        //This fix-up needed to resolve annotation call into annotation constructor (but not into the annotation type)
        return fixUpAnnotationCallResolveToCtor(results)
    }

    /**
     * 解析当前简单名引用对应的 PSI 目标。
     */
    override fun getResolvedToPsi(analysisSession: CaSession): Collection<PsiElement> = with(analysisSession) {
        if (expression is CjLabelReferenceExpression) {
            val fir = expression.getOrBuildCfir((analysisSession as CaCfirSession).resolutionFacade)
            if (fir is CfirLoopJump) {
                return listOfNotNull(fir.target.labeledElement.psi)
            }
        }
        val referenceTargetSymbols = resolveToSymbols()
        val psiOfReferenceTarget = super.getResolvedToPsi(analysisSession, referenceTargetSymbols)
        if (psiOfReferenceTarget.isNotEmpty()) return psiOfReferenceTarget
        referenceTargetSymbols.mapNotNull { symbol -> symbol.psi }
    }

    /**
     * 判断当前引用是否可能指向候选 PSI。
     */
    override fun canBeReferenceTo(candidateTarget: PsiElement): Boolean {
        return true // TODO
    }

    /**
     * 解析当前引用的目标 PSI 集合。
     */
    override fun resolveTargetElements(): Collection<PsiElement> {
        return analyze(expression) { getResolvedToPsi(this) }
    }

    /**
     * 获取当前引用命中的 import alias。
     */
    override fun getImportAlias(): CjImportAlias? {
        val name = element.referencedName
        val file = element.containingCjFile
        return getImportAlias(file.findImportByAlias(name))
    }

    /**
     * 简单名引用 provider。
     */
    class Provider : CangJiePsiReferenceProviderContributor<CjSimpleNameExpression> {
        /**
         * provider 处理的 PSI 元素类型。
         */
        override val elementClass: Class<CjSimpleNameExpression>
            get() = CjSimpleNameExpression::class.java

        /**
         * 根据读写访问类型创建一个或多个简单名引用。
         */
        override val referenceProvider: CangJiePsiReferenceProviderContributor.ReferenceProvider<CjSimpleNameExpression>
            get() = CangJiePsiReferenceProviderContributor.ReferenceProvider { nameReferenceExpression: CjSimpleNameExpression ->
                when (nameReferenceExpression.readWriteAccess(useResolveForReadWrite = true)) {
                    ReferenceAccess.READ -> listOf(CaCfirSimpleNameReference(nameReferenceExpression, isRead = true))
                    ReferenceAccess.WRITE -> listOf(CaCfirSimpleNameReference(nameReferenceExpression, isRead = false))
                    ReferenceAccess.READ_WRITE -> listOf(
                        CaCfirSimpleNameReference(nameReferenceExpression, isRead = true),
                        CaCfirSimpleNameReference(nameReferenceExpression, isRead = false),
                    )
                }
            }
    }
}

/**
 * 简单名引用的读写访问分类。
 */
private enum class ReferenceAccess(
    /**
     * 是否包含读取访问。
     */
    val isRead: Boolean,
    /**
     * 是否包含写入访问。
     */
    val isWrite: Boolean,
) {
    READ(true, false),
    WRITE(false, true),
    READ_WRITE(true, true),
}

/**
 * 根据表达式语法上下文判断简单名的读写访问类型。
 */
private fun CjExpression.readWriteAccess(useResolveForReadWrite: Boolean): ReferenceAccess {
    val expression = unwrapQualifiedOrWrappedExpression()
    val assignment = expression.getAssignmentByLHS()
    if (assignment != null) {
        return when (assignment.operationToken) {
            org.cangnova.cangjie.lexer.CjTokens.EQ -> ReferenceAccess.WRITE
            else -> if (useResolveForReadWrite) ReferenceAccess.READ_WRITE else ReferenceAccess.READ_WRITE
        }
    }

    return when (val parent = expression.parent) {
        is CjValueArgumentName -> ReferenceAccess.WRITE
        is org.cangnova.cangjie.psi.CjUnaryExpression ->
            when (parent.operationToken) {
                org.cangnova.cangjie.lexer.CjTokens.PLUSPLUS,
                org.cangnova.cangjie.lexer.CjTokens.MINUSMINUS,
                    -> ReferenceAccess.READ_WRITE

                else -> ReferenceAccess.READ
            }

        else -> ReferenceAccess.READ
    }
}

/**
 * 解开限定表达式 selector 和括号包装，得到真正参与读写判定的表达式。
 */
private fun CjExpression.unwrapQualifiedOrWrappedExpression(): CjExpression {
    var current = getQualifiedExpressionForSelectorOrThis()
    while (true) {
        current = when (val parent = current.parent) {
            is org.cangnova.cangjie.psi.CjParenthesizedExpression -> parent
            else -> return current
        }
    }
}
