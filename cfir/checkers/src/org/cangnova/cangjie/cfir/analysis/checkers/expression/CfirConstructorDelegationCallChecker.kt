package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin

/**
 * `this(...)` / `super(...)` 在仓颉里是构造器 delegation 调用，
 * 即使语法上表现为 call expression，也不能出现在普通函数、lambda 或类体其他位置。
 *
 * declaration 侧的 `CfirConstructorDelegationChecker` 负责“在构造器里是否合法”；
 * 这里负责更外层的入口约束：如果最近的函数级声明不是 constructor，就直接报非法位置。
 */
object CfirConstructorDelegationCallChecker : CfirFunctionCallChecker() {
    /** 检查 `this(...)`/`super(...)` 构造器委托调用是否位于构造器声明内部。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        val delegationName = expression.origin.constructorDelegationKeyword() ?: return
        val source = expression.calleeReference.source ?: expression.source
        if (context.findClosestDeclaration<CfirClassLikeDeclaration>() == null) {
            reporter.reportOn(
                source = source,
                factory = CfirErrors.THIS_SUPER_USE_ERROR_OUTSIDE_CLASS,
                a = delegationName,
            )
            return
        }

        val closestFunction = context.closestFunctionLikeDeclaration()
        if (closestFunction is CfirConstructor) return

        reporter.reportOn(
            source = source,
            factory = CfirErrors.INVALID_THIS_CALL_OUTSIDE_CTOR,
            a = delegationName,
        )
    }
}

/** 将函数调用 origin 转换为构造器委托关键字；非委托调用返回空。 */
private fun CfirFunctionCallOrigin.constructorDelegationKeyword(): String? {
    return when (this) {
        CfirFunctionCallOrigin.ConstructorDelegationThis -> "this"
        CfirFunctionCallOrigin.ConstructorDelegationSuper -> "super"
        else -> null
    }
}

/** 查找当前上下文中最近的函数级声明。 */
private fun CheckerContext.closestFunctionLikeDeclaration(): CfirFunction? {
    return containingDeclarations
        .asReversed()
        .firstOrNull { declaration -> declaration is CfirFunction } as? CfirFunction
}
