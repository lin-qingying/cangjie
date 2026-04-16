package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjValueArgument
import org.cangnova.cangjie.psi.CjValueArgumentList
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.source.toCjPsiSourceElement

/**
 * Inout 语义检查器
 *
 * 对齐 C++ FFI/CFFICheck.cpp 中 CheckLegalityOfUnsafeAndInout:
 * - inout 只能在 CFunc（foreign 函数）调用中使用
 * - inout 修饰的表达式必须是 var 变量的可变左值（RefExpr / MemberAccess）
 * - 同一参数不能重复标记 inout
 *
 * 由于 CFIR 树的 CfirArgumentList 当前不携带 inout 标记，
 * 此 checker 通过 PSI 回溯从 CfirFunctionCall.source 获取 CjCallExpression，
 * 再从 CjValueArgument.isInout 读取 inout 标记。
 * 这与 CfirBuiltInAnnotationSemanticsChecker 使用 PSI 回溯的模式一致。
 */
object CfirInoutSemanticsChecker : CfirFunctionCallChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        val callPsi = expression.source?.psi as? CjCallExpression ?: return
        val argumentList = callPsi.valueArgumentList ?: return
        val inoutArgs = argumentList.arguments.filterIsInstance<CjValueArgument>().filter { it.isInout }
        if (inoutArgs.isEmpty()) return

        // 检查调用目标是否是 foreign 函数
        val targetSymbol = expression.resolvedFunctionSymbol()
        val targetFunction = targetSymbol?.takeIf { it.isBound }?.cfir as? CfirNamedFunction
        val isForeignCall = targetFunction?.status?.isForeign == true

        for (arg in inoutArgs) {
            val argSource = arg.getInoutKeyword()?.toCjPsiSourceElement()
                ?: expression.source

            // inout 只能在 CFunc 调用中使用
            if (!isForeignCall) {
                reporter.reportOn(
                    source = argSource,
                    factory = CfirErrors.INOUT_CAN_ONLY_USED_IN_CFUNC_CALLING,
                )
                continue
            }

            // inout 参数必须是可变左值（变量引用）
            val argExpr = arg.getArgumentExpression()
            if (argExpr == null || !isValidInoutTarget(argExpr)) {
                reporter.reportOn(
                    source = argSource,
                    factory = CfirErrors.INVALID_INOUT_ARGUMENT,
                )
                continue
            }

            // inout 参数必须是 var 变量（此检查需要 resolve 信息）
            // 通过 CFIR 表达式对应的 resolve 结果检查
            val cfirArgIndex = argumentList.arguments.indexOf(arg)
            if (cfirArgIndex >= 0 && cfirArgIndex < expression.argumentList.arguments.size) {
                val cfirArg = expression.argumentList.arguments[cfirArgIndex]
                checkInoutMustBeVar(cfirArg, argSource)
                checkInoutTypeConstraints(cfirArg, argSource)
            }
        }

        // 检查是否有重复 inout 标记（同一参数位置出现两次 inout）
        val seenPositions = mutableSetOf<Int>()
        for (arg in argumentList.arguments.filterIsInstance<CjValueArgument>()) {
            if (!arg.isInout) continue
            val position = argumentList.arguments.indexOf(arg)
            if (!seenPositions.add(position)) {
                val argSource = arg.getInoutKeyword()?.toCjPsiSourceElement()
                    ?: expression.source
                reporter.reportOn(
                    source = argSource,
                    factory = CfirErrors.DUPLICATE_INOUT_ARGUMENT,
                )
            }
        }
    }

    /**
     * 检查 PSI 表达式是否是有效的 inout 目标（可变左值）。
     *
     * 对齐 C++ CFFICheck.cpp: inout 表达式必须是 RefExpr 或 MemberAccess。
     */
    private fun isValidInoutTarget(expr: org.cangnova.cangjie.psi.CjExpression): Boolean {
        return expr is CjSimpleNameExpression ||
            expr is org.cangnova.cangjie.psi.CjDotQualifiedExpression
    }

    /**
     * 检查 CFIR 参数表达式是否引用 var 变量。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkInoutMustBeVar(
        cfirArg: org.cangnova.cangjie.cfir.expressions.CfirExpression,
        argSource: org.cangnova.cangjie.source.AbstractCjSourceElement?,
    ) {
        val qualifiedAccess = cfirArg as? org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression ?: return
        val reference = qualifiedAccess.calleeReference
        val resolvedSymbol = when (reference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol
            else -> return
        }
        val variable = (resolvedSymbol as? org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol<*>)
            ?.takeIf { it.isBound }?.cfir as? CfirVariable ?: return
        if (!variable.isVar) {
            reporter.reportOn(
                source = argSource,
                factory = CfirErrors.INOUT_MUST_BE_VAR_VARIABLE,
            )
        }
    }

    /**
     * 检查 inout 参数的类型约束。
     *
     * 对齐 C++ CFFICheck.cpp:
     * - inout 表达式类型必须满足 CType 约束
     * - inout 表达式不能是 CString 或零大小类型
     * - inout 变量不能来自堆（class 实例字段）
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkInoutTypeConstraints(
        cfirArg: org.cangnova.cangjie.cfir.expressions.CfirExpression,
        argSource: org.cangnova.cangjie.source.AbstractCjSourceElement?,
    ) {
        val argType = cfirArg.coneTypeOrNull ?: return
        if (argType is org.cangnova.cangjie.cfir.types.ConeErrorType) return

        // CString 和零大小类型不允许
        val classId = argType.classIdOrNull()
        if (classId != null && classId.shortClassName.asString() == "CString") {
            reporter.reportOn(
                source = argSource,
                factory = CfirErrors.INOUT_MODIFY_CSTRING_OR_ZEROSIZED,
                a = argType,
            )
            return
        }

        // 必须满足 CType 约束（基本类型和 @C struct 是 CType）
        if (!isCTypeCompatible(argType)) {
            reporter.reportOn(
                source = argSource,
                factory = CfirErrors.INOUT_MODIFY_NON_CTYPE,
            )
        }

        // 检查是否来自 class 实例字段（堆变量）
        val qualifiedAccess = cfirArg as? org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression ?: return
        val receiver = qualifiedAccess.explicitReceiver
        if (receiver != null) {
            val receiverType = receiver.coneTypeOrNull
            if (receiverType is org.cangnova.cangjie.cfir.types.ConeClassLikeType) {
                reporter.reportOn(
                    source = argSource,
                    factory = CfirErrors.INOUT_MODIFY_HEAP_VARIABLE,
                )
            }
        }
    }

    /**
     * 判断类型是否满足 CType 约束。
     * CType 包括：基本类型、@C struct、VArray、CPointer 等。
     */
    private fun isCTypeCompatible(type: org.cangnova.cangjie.cfir.types.ConeCangJieType): Boolean {
        if (type is org.cangnova.cangjie.cfir.types.ConePrimitiveType) return true
        if (type is org.cangnova.cangjie.cfir.types.ConeStructType) return true
        if (type is org.cangnova.cangjie.cfir.types.ConeVArrayType) return true
        if (type is org.cangnova.cangjie.cfir.types.ConePointerType) return true
        return false
    }

    private fun org.cangnova.cangjie.cfir.types.ConeCangJieType.classIdOrNull(): org.cangnova.cangjie.name.ClassId? {
        return when (this) {
            is org.cangnova.cangjie.cfir.types.ConeClassLikeType -> classId
            is org.cangnova.cangjie.cfir.types.ConeStructType -> classId
            is org.cangnova.cangjie.cfir.types.ConeEnumType -> classId
            else -> null
        }
    }

    private fun CfirFunctionCall.resolvedFunctionSymbol(): CfirFunctionSymbol<*>? {
        return when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirFunctionSymbol<*>
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirFunctionSymbol<*>
            else -> null
        }
    }
}
