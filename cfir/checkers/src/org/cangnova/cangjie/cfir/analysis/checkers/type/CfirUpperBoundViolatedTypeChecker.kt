package org.cangnova.cangjie.cfir.analysis.checkers.type

import org.cangnova.cangjie.cfir.analysis.checkers.checkUpperBoundViolated
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef

/**
 * 检查类型使用处的泛型实参是否满足声明侧 upper bounds。
 *
 * 对齐 Kotlin FIR `FirUpperBoundViolatedTypeChecker`：类型解析阶段只构造类型，
 * 上界违反诊断在 resolved type ref checker 中基于已解析类型和原始 source ref 产生。
 */
object CfirUpperBoundViolatedTypeChecker : CfirResolvedTypeRefChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: CfirResolvedTypeRef) {
        if (context.containingDeclarations.lastOrNull() is CfirTypeAlias) return

        val container = context.containingElements.dropLast(1).lastOrNull()
        checkUpperBoundViolated(
            typeRef = typeRef,
            isIgnoreTypeParameters = container is CfirTypeParameter,
        )
    }
}
