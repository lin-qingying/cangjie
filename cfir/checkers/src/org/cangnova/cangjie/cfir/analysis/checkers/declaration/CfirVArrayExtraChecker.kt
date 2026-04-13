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
 * - VArray 作为 CFunc 返回类型是不允许的
 *
 * 注册为 callableDeclarationCheckers（覆盖函数和属性声明）
 */
object CfirVArrayExtraChecker : CfirCallableDeclarationChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirCallableDeclaration) {
        if (declaration !is CfirFunction) return
        if (!declaration.status.isForeign) return

        // VArray 作为 CFunc 返回类型不允许
        val returnType = (declaration.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
        if (returnType is ConeVArrayType) {
            reporter.reportOn(
                source = declaration.returnTypeRef.source ?: declaration.source,
                factory = CfirErrors.VARRAY_IN_CFUNC,
            )
        }
    }
}
