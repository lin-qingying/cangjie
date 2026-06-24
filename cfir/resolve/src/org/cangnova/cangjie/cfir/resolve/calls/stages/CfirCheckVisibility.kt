package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.diagnostic.HiddenCandidate
import org.cangnova.cangjie.cfir.diagnostic.VisibilityError
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.calls.visibility.visibilityChecker

/** 候选可见性检查阶段。 */
object CfirCheckVisibility : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    /** 检查候选声明在当前调用站点是否可见，并报告隐藏候选或可见性错误。 */
    override suspend fun check(candidate: Candidate) {
        if (candidate.callInfo.origin == CfirFunctionCallOrigin.CompilerCoreIntrinsic) return

        val declaration = candidate.symbol.cfir as? CfirMemberDeclaration ?: return

        if (declaration is CfirConstructor) {
            // TODO 仓颉 singleton/enum object 构造可见性细节，后续对齐语言规则。
        }

        val visibilityChecker = candidate.callInfo.session.visibilityChecker
        if (!visibilityChecker.isVisible(declaration, candidate)) {
            val diagnostic = if (declaration.isHiddenFunctionCandidate(candidate.callInfo.callKind)) {
                HiddenCandidate()
            } else {
                VisibilityError(candidate.symbol)
            }
            sink.reportDiagnostic(diagnostic)
        }
    }

    /**
     * 官方成员/名字查找会把不可访问函数候选从可调用集合里滤掉，
     * 之后由普通调用解析报告 no-match；属性/变量访问仍保留可见性诊断。
     */
    private fun CfirMemberDeclaration.isHiddenFunctionCandidate(callKind: CallKind): Boolean {
        if (this !is CfirFunction) return false
        return when (callKind) {
            CallKind.Function,
            CallKind.DelegatingConstructorCall,
            CallKind.EnumConstructorCall,
            -> true

            CallKind.NamedValueAccess -> false
        }
    }
}
