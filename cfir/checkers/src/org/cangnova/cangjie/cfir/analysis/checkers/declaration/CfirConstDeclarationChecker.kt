package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn

/**
 * const 声明语义检查器（ConstDeclaration 分组）
 *
 * 对齐 C++ ConstEvaluationChecker.cpp:
 * - 没有 const 构造器时不能定义 const 成员函数
 * - 有 var 成员变量时不能定义 const 构造器
 *
 * 注册为 classLikeCheckers
 */
object CfirConstDeclarationChecker : CfirClassLikeChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        if (declaration !is CfirClass && declaration !is CfirStruct) return

        val hasConstConstructor = declaration.declarations.any { member ->
            member is CfirConstructor && member.status.isConst
        }
        val hasConstMemberFunction = declaration.declarations.any { member ->
            member is CfirNamedFunction && member.status.isConst && !member.status.isStatic
        }
        val hasVarField = declaration.declarations.any { member ->
            member is CfirFieldVariable && member.isVar
        }

        // 有 const 成员函数但没有 const 构造器
        if (hasConstMemberFunction && !hasConstConstructor) {
            val constFunc = declaration.declarations.first { member ->
                member is CfirNamedFunction && member.status.isConst && !member.status.isStatic
            }
            reporter.reportOn(
                source = constFunc.source,
                factory = CfirErrors.NO_CONST_INIT,
            )
        }

        // 有 var 成员变量但定义了 const 构造器
        if (hasVarField && hasConstConstructor) {
            val constCtor = declaration.declarations.first { member ->
                member is CfirConstructor && member.status.isConst
            }
            reporter.reportOn(
                source = constCtor.source,
                factory = CfirErrors.CLASS_CONST_INIT_WITH_VAR,
            )
        }
    }
}

/**
 * const 函数内 var 变量限制检查器
 *
 * 对齐 C++ DiagKind::sema_cannot_define_var_in_const_function:
 * const 函数体内不能定义 var 变量。
 *
 * 注册为 functionCheckers
 */
object CfirConstFunctionVarChecker : CfirFunctionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFunction) {
        if (!declaration.status.isConst) return
        val body = declaration.body ?: return

        for (statement in body.statements) {
            checkVarInConstContext(statement)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkVarInConstContext(element: org.cangnova.cangjie.cfir.CfirElement) {
        when (element) {
            is org.cangnova.cangjie.cfir.declarations.CfirVariable -> {
                if (element.isVar) {
                    reporter.reportOn(
                        source = element.source,
                        factory = CfirErrors.CANNOT_DEFINE_VAR_IN_CONST_FUNCTION,
                    )
                }
            }
            // 不递归进入嵌套函数/lambda，它们有自己的 const 约束
            is CfirFunction -> return
            else -> Unit
        }
    }
}
