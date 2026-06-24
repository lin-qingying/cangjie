package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeVArrayType

/**
 * VArray 语义检查器
 *
 * 对齐 C++ TypeCheckType.cpp:
 * - VARRAY_IN_CFUNC: VArray 作为 CFunc 返回类型不允许
 *
 * 注册为 callableDeclarationCheckers
 */
object CfirVArrayExtraChecker : CfirCallableDeclarationChecker() {
    /**
     * 分发 callable 上的 VArray 附加语义检查。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirCallableDeclaration) {
        checkVArrayReturnInCFunc(declaration)
    }

    /**
     * VArray 作为 CFunc 返回类型不允许。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkVArrayReturnInCFunc(declaration: CfirCallableDeclaration) {
        if (declaration !is CfirFunction) return
        if (!declaration.status.isForeign) return

        val returnType = (declaration.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
        if (returnType is ConeVArrayType) {
            reporter.reportOn(
                source = declaration.returnTypeRef.source ?: declaration.source,
                factory = CfirErrors.VARRAY_IN_CFUNC,
            )
        }
    }
}
