package org.cangnova.cangjie.analysis.api.cfir.references

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import org.cangnova.cangjie.analysis.api.CaAllowAnalysisFromWriteAction
import org.cangnova.cangjie.analysis.api.CaAllowAnalysisOnEdt
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.permissions.allowAnalysisFromWriteAction
import org.cangnova.cangjie.analysis.api.permissions.allowAnalysisOnEdt
import org.cangnova.cangjie.idea.references.AbstractCjReference
import org.cangnova.cangjie.idea.references.CjReference
import org.cangnova.cangjie.utils.exceptions.buildErrorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry
import org.cangnova.cangjie.utils.rethrowIntellijPlatformExceptionIfNeeded

/**
 * CFIR 引用的 IntelliJ ResolveCache 多结果解析器。
 */
internal object CaCfirReferenceResolver : ResolveCache.PolyVariantResolver<CjReference>{
    /**
     * 引用解析失败时使用的日志入口。
     */
    private val LOG = Logger.getInstance(CaCfirReferenceResolver::class.java)

    /**
     * 仓颉引用解析结果包装。
     */
    class CangJieResolveResult(element: PsiElement) : PsiElementResolveResult(element)

    /**
     * 使用 Analysis API 解析引用并转换为 IntelliJ resolve result。
     */
    @OptIn(CaAllowAnalysisOnEdt::class, CaAllowAnalysisFromWriteAction::class)
    override fun resolve(ref: CjReference, incompleteCode: Boolean): Array<ResolveResult> {
        check(ref is CaCfirReference) { "reference should be CfirCjReference, but was ${ref::class}" }
        check(ref is AbstractCjReference<*>) { "reference should be AbstractCjReference, but was ${ref::class}" }
        return allowAnalysisOnEdt {
            allowAnalysisFromWriteAction {
                val resolveToPsiElements = try {
                    analyze(ref.expression) { ref.getResolvedToPsi(this) }
                } catch (exception: Exception) {
                    rethrowIntellijPlatformExceptionIfNeeded(exception)

                    val wrappedException = buildErrorWithAttachment("Unable to resolve reference ${ref.element::class}", exception) {
                        withPsiEntry("reference", ref.element)
                    }

                    LOG.error(wrappedException)

                    emptyList()
                }

                resolveToPsiElements.map { CangJieResolveResult(it) }.toTypedArray()
            }
        }
    }
}
