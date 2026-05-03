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

@OptIn(CaImplementationDetail::class)
internal class CaCfirSimpleNameReference(
    expression: CjSimpleNameExpression,
    val isRead: Boolean,
) : CaBaseSimpleNameReference(expression), CaCfirReference {
    override val resolver get() = CaCfirReferenceResolver

    private val isAnnotationCall: Boolean
        get() {
            val ktUserType = expression.parent as? CjUserType ?: return false
            val ktTypeReference = ktUserType.parent as? CjTypeReference ?: return false
            val ktConstructorCalleeExpression = ktTypeReference.parent as? CjConstructorCalleeExpression ?: return false
            return ktConstructorCalleeExpression.parent is CjAnnotation
        }

    private fun CaSession.fixUpAnnotationCallResolveToCtor(resultsToFix: Collection<CaSymbol>): Collection<CaSymbol> {
        if (resultsToFix.isEmpty() || !isAnnotationCall) return resultsToFix

        return resultsToFix
    }

    override fun isReferenceToImportAlias(alias: CjImportAlias): Boolean {
        return super<CaCfirReference>.isReferenceToImportAlias(alias)
    }

    override fun CaCfirSession.computeSymbols(): Collection<CaSymbol> {
        val results = CfirReferenceResolveHelper.resolveSimpleNameReference(this@CaCfirSimpleNameReference, this)
        //This fix-up needed to resolve annotation call into annotation constructor (but not into the annotation type)
        return fixUpAnnotationCallResolveToCtor(results)
    }

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

    override fun canBeReferenceTo(candidateTarget: PsiElement): Boolean {
        return true // TODO
    }

    override fun resolveTargetElements(): Collection<PsiElement> {
        return analyze(expression) { getResolvedToPsi(this) }
    }

    override fun getImportAlias(): CjImportAlias? {
        val name = element.referencedName
        val file = element.containingCjFile
        return getImportAlias(file.findImportByAlias(name))
    }

    class Provider : CangJiePsiReferenceProviderContributor<CjSimpleNameExpression> {
        override val elementClass: Class<CjSimpleNameExpression>
            get() = CjSimpleNameExpression::class.java

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

private enum class ReferenceAccess(
    val isRead: Boolean,
    val isWrite: Boolean,
) {
    READ(true, false),
    WRITE(false, true),
    READ_WRITE(true, true),
}

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

private fun CjExpression.unwrapQualifiedOrWrappedExpression(): CjExpression {
    var current = getQualifiedExpressionForSelectorOrThis()
    while (true) {
        current = when (val parent = current.parent) {
            is org.cangnova.cangjie.psi.CjParenthesizedExpression -> parent
            else -> return current
        }
    }
}
