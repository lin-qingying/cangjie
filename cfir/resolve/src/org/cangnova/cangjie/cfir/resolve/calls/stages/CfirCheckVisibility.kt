package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.diagnostic.HiddenCandidate
import org.cangnova.cangjie.cfir.diagnostic.VisibilityError
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.calls.visibility.visibilityChecker

object CfirCheckVisibility : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    override suspend fun check(candidate: Candidate) {
        val declaration = candidate.symbol.cfir as? CfirMemberDeclaration ?: return

        if (declaration is CfirConstructor) {
            // TODO 仓颉 singleton/enum object 构造可见性细节，后续对齐语言规则。
        }

        val visibilityChecker = candidate.callInfo.session.visibilityChecker
        if (!visibilityChecker.isVisible(declaration, candidate)) {
            sink.reportDiagnostic(VisibilityError(candidate.symbol))
        }
    }
}
