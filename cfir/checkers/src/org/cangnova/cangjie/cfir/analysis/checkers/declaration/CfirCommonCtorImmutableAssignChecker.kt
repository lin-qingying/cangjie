package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid

/**
 * common class/struct 构造器中禁止对 common 的 let 字段赋值。
 *
 * 对齐 C++ DiagKind::sema_common_assign_to_common_immutable_in_ctor (Diags.cpp:380):
 * common 声明中 let 字段虽然在平台侧可被 specific 构造器初始化,
 * 但 common 自己的构造器不能赋值这类字段。
 */
object CfirCommonCtorImmutableAssignChecker : CfirClassLikeChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        if (declaration !is CfirClass && declaration !is CfirStruct) return
        if (!declaration.status.isCommon) return

        for (member in declaration.declarations) {
            if (member !is CfirConstructor) continue
            val body = member.body ?: continue
            body.acceptChildren(object : CfirVisitorVoid() {
                override fun visitElement(element: CfirElement) {
                    if (element is CfirAssignment) {
                        val ref = (element.lValue as? org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression)
                            ?.calleeReference as? CfirResolvedNamedReference
                        val target = ref?.resolvedSymbol as? CfirFieldVariableSymbol
                        val field = target?.cfir
                        if (field is CfirFieldVariable
                            && !field.isVar
                            && field.status.isCommon) {
                            reporter.reportOn(
                                source = element.source,
                                factory = CfirErrors.COMMON_ASSIGN_TO_COMMON_IMMUTABLE_IN_CTOR,
                                a = field.name,
                            )
                        }
                    }
                    element.acceptChildren(this, null)
                }
            }, null)
        }
    }
}
