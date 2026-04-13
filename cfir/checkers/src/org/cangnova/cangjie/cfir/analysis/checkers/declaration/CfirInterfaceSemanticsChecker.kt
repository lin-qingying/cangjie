package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn

/**
 * 接口语义检查器（Interface 分组）
 *
 * 对齐 C++ TypeCheckClassLike.cpp:
 * - 接口中 static 函数/属性必须有实现体
 *
 * 注册为 classLikeCheckers
 */
object CfirInterfaceSemanticsChecker : CfirClassLikeChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        if (declaration !is CfirInterface) return
        checkUnimplementedStaticMembers(declaration)
    }

    /**
     * 检查接口中 static 成员是否有实现。
     *
     * 对齐 C++ DiagKind::sema_interface_call_with_unimplemented_call
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkUnimplementedStaticMembers(interfaceDecl: CfirInterface) {
        for (member in interfaceDecl.declarations) {
            when (member) {
                is CfirNamedFunction -> {
                    if (member.status.isStatic && member.body == null && !member.status.isAbstract) {
                        reporter.reportOn(
                            source = member.source,
                            factory = CfirErrors.INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL,
                            a = "static function",
                            b = member.name,
                        )
                    }
                }
                else -> Unit
            }
        }
    }
}
