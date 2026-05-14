package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirInoutArgumentExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * Inout 语义检查器。
 *
 * 对齐官方编译器 `ChkFuncArgWithInout` / `ChkInoutFuncArg`：
 * `inout` 只能用于 foreign/CFunc 调用，实参必须是由 `var` 定义的可变左值。
 */
object CfirInoutSemanticsChecker : CfirFunctionCallChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        val inoutArguments = expression.argumentList.arguments.mapNotNull { argument ->
            argument.unwrapArgument() as? CfirInoutArgumentExpression
        }
        if (inoutArguments.isEmpty()) return

        val targetFunction = expression.resolvedFunctionSymbol()?.takeIf { it.isBound }?.cfir as? CfirNamedFunction
        val targetType = (targetFunction?.returnTypeRef as? CfirResolvedTypeRef)?.coneType
        val isCFuncCall = targetFunction?.status?.isForeign == true || (targetType as? ConeFunctionType)?.isCFunc == true

        for (argument in inoutArguments) {
            val argumentExpression = argument.expression
            if (!isCFuncCall) {
                reporter.reportOn(
                    source = argument.source ?: argumentExpression.source ?: expression.source,
                    factory = CfirErrors.INOUT_CAN_ONLY_USED_IN_CFUNC_CALLING,
                )
                continue
            }

            checkInoutTarget(argumentExpression, argument.source)
            checkInoutTypeConstraints(argumentExpression)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkInoutTarget(
        argument: CfirExpression,
        argumentSource: AbstractCjSourceElement?,
    ) {
        val access = argument as? CfirQualifiedAccessExpression
        if (access == null) {
            reporter.reportOn(argument.source ?: argumentSource, CfirErrors.INOUT_MUST_BE_VAR_VARIABLE)
            return
        }

        val variable = access.resolvedVariable()
        if (variable == null) {
            reporter.reportOn(
                source = argumentSource ?: access.source,
                factory = CfirErrors.INOUT_MUST_BE_VAR_VARIABLE,
            )
            return
        }

        checkReceiverChain(access)
        checkVariableAccess(access, variable, access.source ?: argumentSource)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkReceiverChain(access: CfirQualifiedAccessExpression) {
        val receiver = access.explicitReceiver as? CfirQualifiedAccessExpression ?: return
        checkReceiverChain(receiver)
        val variable = receiver.resolvedVariable() ?: return
        checkVariableAccess(receiver, variable, receiver.source)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkVariableAccess(
        access: CfirQualifiedAccessExpression,
        variable: CfirVariable,
        source: AbstractCjSourceElement?,
    ) {
        if (!variable.isVar) {
            reporter.reportOn(
                source = source ?: access.source,
                factory = CfirErrors.INOUT_MUST_BE_VAR_VARIABLE,
            )
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkInoutTypeConstraints(argument: CfirExpression) {
        val argType = argument.coneTypeOrNull ?: return
        if (argType is ConeErrorType) return

        val source = argument.source
        val classId = argType.classIdOrNull()
        if (classId != null && classId.shortClassName.asString() == "CString") {
            reporter.reportOn(
                source = source,
                factory = CfirErrors.INOUT_MODIFY_CSTRING_OR_ZEROSIZED,
                a = argType,
            )
            return
        }

        if (!argType.isCTypeCompatible()) {
            reporter.reportOn(
                source = source,
                factory = CfirErrors.INOUT_MODIFY_NON_CTYPE,
            )
        }

        val receiver = (argument as? CfirQualifiedAccessExpression)?.explicitReceiver ?: return
        val receiverType = receiver.coneTypeOrNull
        if (receiverType is ConeClassLikeType) {
            reporter.reportOn(
                source = receiver.source ?: source,
                factory = CfirErrors.INOUT_MODIFY_HEAP_VARIABLE,
            )
        }
    }

    private fun CfirExpression.unwrapArgument(): CfirExpression {
        return (this as? CfirBlock)?.statements?.singleOrNull() as? CfirExpression ?: this
    }

    private fun CfirQualifiedAccessExpression.resolvedVariable(): CfirVariable? {
        val resolvedSymbol = when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol
            else -> return null
        }
        return (resolvedSymbol as? CfirVariableSymbol<*>)?.takeIf { it.isBound }?.cfir
    }

    private fun CfirFunctionCall.resolvedFunctionSymbol(): CfirFunctionSymbol<*>? {
        return when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirFunctionSymbol<*>
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirFunctionSymbol<*>
            else -> null
        }
    }

    private fun ConeCangJieType.isCTypeCompatible(): Boolean {
        return this is ConePrimitiveType ||
            this is ConeStructType ||
            this is ConeVArrayType ||
            this is ConePointerType
    }

    private fun ConeCangJieType.classIdOrNull(): ClassId? {
        return when (this) {
            is ConeClassLikeType -> classId
            is ConeStructType -> classId
            is ConeEnumType -> classId
            else -> null
        }
    }
}
