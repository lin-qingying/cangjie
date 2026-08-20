package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.diagnostic.VisibilityError
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.accessibilityResult
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.calls.candidate.yieldDiagnostic
import org.cangnova.cangjie.cfir.resolve.calls.visibility.visibilityChecker
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityResult
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupDisposition

/** 候选可见性检查阶段。 */
object CfirCheckVisibility : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    /** 检查候选声明在当前调用站点是否可见，并报告隐藏候选或可见性错误。 */
    override suspend fun check(candidate: Candidate) {
        if (candidate.callInfo.origin == CfirFunctionCallOrigin.CompilerCoreIntrinsic) return

        val declaration = candidate.symbol.cfir as? CfirMemberDeclaration ?: return

        val visibilityChecker = candidate.callInfo.session.visibilityChecker
        when (val result = candidate.accessibilityResult(visibilityChecker, declaration)) {
            CfirAccessibilityResult.Accessible -> Unit
            is CfirAccessibilityResult.Inaccessible -> {
                when (result.disposition) {
                    CfirLookupDisposition.NOT_DISCOVERABLE,
                    CfirLookupDisposition.EXCLUDE_CALLABLE,
                    -> error(
                        "${result.disposition} candidate `${candidate.symbol}` reached CfirCheckVisibility; " +
                            "tower discovery must handle it before Candidate creation",
                    )

                    CfirLookupDisposition.REPORT_ACCESS_ERROR ->
                        sink.yieldDiagnostic(VisibilityError(result.reportingOwner))
                }
            }
        }
    }
}
