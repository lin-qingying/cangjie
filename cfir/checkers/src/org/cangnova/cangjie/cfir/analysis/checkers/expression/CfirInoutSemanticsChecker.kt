package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirInoutArgumentExpression
import org.cangnova.cangjie.cfir.expressions.CfirNamedArgumentExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirThisReceiverExpression
import org.cangnova.cangjie.cfir.expressions.CfirSuperReceiverExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.declarations.interopInfo
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.providers.getContainingClass
import org.cangnova.cangjie.cfir.types.CfirCTypeSemantics
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeCStringType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * Inout 语义检查器。
 *
 * 对齐官方编译器 `ChkFuncArgWithInout` / `ChkInoutFuncArg`：
 * `inout` 只能用于 foreign/CFunc 调用，实参必须是由 `var` 定义的可变左值。
 */
object CfirInoutSemanticsChecker : CfirFunctionCallChecker() {
    /**
     * 检查函数调用中的所有 `inout` 实参。
     *
     * 入口先确认被调函数是否为 foreign/CFunc，再逐个检查实参左值可变性和 C 类型兼容性。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        val inoutArguments = expression.argumentList.arguments.mapNotNull { argument ->
            argument.unwrapArgument() as? CfirInoutArgumentExpression
        }
        if (inoutArguments.isEmpty()) return

        val isCFuncCall = expression.isCFuncCall(context.session)

        for (argument in inoutArguments) {
            val argumentExpression = argument.expression
            if (!isCFuncCall) {
                reporter.reportOn(
                    source = argument.source ?: argumentExpression.source ?: expression.source,
                    factory = CfirErrors.INOUT_CAN_ONLY_USED_IN_CFUNC_CALLING,
                )
                continue
            }

            // 官方 SynFuncArg 先确认可变左值；无效目标的中间类型不能再触发 CType 级联诊断。
            if (argumentExpression.coneTypeOrNull !is ConeErrorType &&
                checkInoutTarget(argumentExpression, argument.source)
            ) {
                checkInoutTypeConstraints(argumentExpression)
            }
        }
    }

    /**
     * 检查 `inout` 实参是否是可解析的变量访问。
     *
     * 非 qualified access 或无法解析为变量的表达式都不满足 inout 的可变左值要求。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkInoutTarget(
        argument: CfirExpression,
        argumentSource: AbstractCjSourceElement?,
        isBase: Boolean = false,
    ): Boolean {
        // this/super 只允许作为成员访问的基址，不能直接作为 inout 实参。
        if (isBase && (argument is CfirThisReceiverExpression || argument is CfirSuperReceiverExpression)) return true
        val access = argument as? CfirNamedAccessExpression
        if (access == null) {
            reporter.reportOn(argumentSource ?: argument.source, CfirErrors.INOUT_MUST_BE_VAR_VARIABLE)
            return false
        }

        val variable = access.resolvedVariable()
        if (variable == null) {
            reporter.reportOn(
                source = argumentSource ?: access.source,
                factory = CfirErrors.INOUT_MUST_BE_VAR_VARIABLE,
            )
            return false
        }

        val variableIsValid = checkVariableAccess(access, variable, access.source ?: argumentSource)
        if (variable.status.isStatic) return variableIsValid

        val nominalOwner = if (variable is CfirValueParameter || variable.isLocal) {
            null
        } else {
            variable.symbol.getContainingClass()?.cfir
        }
        val receiver = access.explicitReceiver
        if (receiver == null) {
            if (!variableIsValid) return false
            // 裸字段访问可以没有 dispatchReceiver；声明 owner 才能区分局部变量与实例字段。
            if (nominalOwner != null && nominalOwner !is CfirStruct) {
                reporter.reportOn(access.source ?: argumentSource, CfirErrors.INOUT_MODIFY_HEAP_VARIABLE)
                return false
            }
            return true
        }
        if (receiver.coneTypeOrNull?.fullyExpandedType(context.session) is ConeClassLikeType) {
            reporter.reportOn(receiver.source ?: argumentSource, CfirErrors.INOUT_MODIFY_HEAP_VARIABLE)
            return false
        }
        val receiverIsValid = checkInoutTarget(receiver, receiver.source, isBase = true)
        return receiverIsValid && variableIsValid
    }

    /**
     * 检查变量访问是否来自 `var` 声明。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkVariableAccess(
        access: CfirQualifiedAccessExpression,
        variable: CfirVariable,
        source: AbstractCjSourceElement?,
    ): Boolean {
        if (!variable.isVar) {
            reporter.reportOn(
                source = source ?: access.source,
                factory = CfirErrors.INOUT_MUST_BE_VAR_VARIABLE,
            )
            return false
        }
        return true
    }

    /**
     * 检查 inout 实参类型是否满足 C 互操作修改约束。
     *
     * CString、非 C 类型和堆对象接收者分别对应不同官方诊断。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkInoutTypeConstraints(argument: CfirExpression) {
        val argType = argument.coneTypeOrNull?.fullyExpandedType(context.session) ?: return
        if (argType is ConeErrorType) return

        val source = argument.source
        if (argType is ConeCStringType) {
            reporter.reportOn(
                source = source,
                factory = CfirErrors.INOUT_MODIFY_CSTRING_OR_ZEROSIZED,
                a = argType,
            )
            return
        }

        if (!CfirCTypeSemantics.isMetCType(context.session, argType)) {
            reporter.reportOn(
                source = source,
                factory = CfirErrors.INOUT_MODIFY_NON_CTYPE,
            )
            return
        }

        if (CfirCTypeSemantics.isZeroSized(context.session, argType)) {
            reporter.reportOn(
                source = source,
                factory = CfirErrors.INOUT_MODIFY_CSTRING_OR_ZEROSIZED,
                a = argType,
            )
            return
        }

    }

    /** 去掉命名实参包装，取得真实的 inout/value 表达式。 */
    private tailrec fun CfirExpression.unwrapArgument(): CfirExpression = when (this) {
        is CfirNamedArgumentExpression -> expression.unwrapArgument()
        else -> this
    }

    /**
     * 从 qualified access 中解析变量声明。
     */
    private fun CfirQualifiedAccessExpression.resolvedVariable(): CfirVariable? {
        val resolvedSymbol = when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol
            else -> return null
        }
        return (resolvedSymbol as? CfirVariableSymbol<*>)?.takeIf { it.isBound }?.cfir
    }

}
