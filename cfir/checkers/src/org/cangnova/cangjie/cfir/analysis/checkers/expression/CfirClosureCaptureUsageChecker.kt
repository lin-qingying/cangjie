package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedErrorReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import java.util.IdentityHashMap

/**
 * 检查捕获可变局部变量的函数/闭包是否仅被直接调用。
 *
 * 对齐官方 `SetCaptureKind`、`SetFuncBodyCaptureKind` 和 `CheckLegalUseOfClosure`：
 * 直接捕获可变变量的闭包作为值使用时报 `USE_FUNC_CAPTURE_VAR_ALONE`；仅通过调用其他闭包
 * 形成的传递捕获按赋值、返回、参数或普通表达式分别报告专用诊断。
 */
object CfirClosureCaptureUsageChecker : CfirBasicExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirStatement) {
        when (expression) {
            is CfirAnonymousFunctionExpression -> checkAnonymousFunctionExpression(expression)
            is CfirQualifiedAccessExpression -> checkFunctionReference(expression)
        }
    }

    /** 检查具名局部函数作为值使用；普通 `g()` 调用由 function-call 节点表示，直接放行。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkFunctionReference(expression: CfirQualifiedAccessExpression) {
        if (expression is CfirFunctionCall) return
        val function = expression.resolvedFunctionOrNull() ?: return
        if (System.getenv("CFIR_CAPTURE_TRACE") == "1") {
            println(
                "CFIR_CAPTURE_TRACE ref=${function.symbol.callableId.callableName} local=${function.isLocal} " +
                        "source=${expression.source}"
            )
        }
        reportIllegalClosureValueUse(
            expression = expression,
            function = function,
            description = "function",
            subjectName = function.symbol.callableId.callableName.asString(),
        )
    }

    /** 检查 lambda 作为值使用；作为 synthetic invoke receiver 的立即调用 lambda 合法。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkAnonymousFunctionExpression(expression: CfirAnonymousFunctionExpression) {
        if (context.callsOrAssignments.asReversed()
                .filterIsInstance<CfirFunctionCall>()
                .any { call -> call.explicitReceiver.containsElement(expression) || call.dispatchReceiver.containsElement(expression) }
        ) {
            return
        }
        reportIllegalClosureValueUse(
            expression = expression,
            function = expression.anonymousFunction,
            description = "lambda",
            subjectName = "lambda",
        )
    }

    /** 根据直接/传递捕获以及当前值使用位置选择官方诊断。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportIllegalClosureValueUse(
        expression: CfirExpression,
        function: CfirFunction,
        description: String,
        subjectName: String,
    ) {
        val captureInfo = ClosureCaptureAnalyzer().captureInfo(function)
        if (System.getenv("CFIR_CAPTURE_TRACE") == "1") {
            println(
                "CFIR_CAPTURE_TRACE info=${function.symbol.callableId.callableName} kind=${captureInfo.kind} " +
                        "vars=${captureInfo.mutableVariables.map { it.symbol.callableId.callableName }}"
            )
        }
        when (captureInfo.kind) {
            ClosureCaptureKind.NONE -> return
            ClosureCaptureKind.DIRECT_MUTABLE -> reporter.reportOn(
                source = expression.source,
                factory = CfirErrors.USE_FUNC_CAPTURE_VAR_ALONE,
                a = description,
            )
            ClosureCaptureKind.TRANSITIVE -> {
                val captureKind = "transitively"
                when (expression.valueUsage(context)) {
                    ClosureValueUsage.ASSIGN -> reporter.reportOn(
                        expression.source,
                        CfirErrors.FUNC_CAPTURE_VAR_CANNOT_ASSIGN,
                        subjectName,
                        captureKind,
                        subjectName,
                    )
                    ClosureValueUsage.RETURN -> reporter.reportOn(
                        expression.source,
                        CfirErrors.FUNC_CAPTURE_VAR_CANNOT_RETURN,
                        subjectName,
                        captureKind,
                        subjectName,
                    )
                    ClosureValueUsage.PARAMETER -> reporter.reportOn(
                        expression.source,
                        CfirErrors.FUNC_CAPTURE_VAR_CANNOT_PARAM,
                        subjectName,
                        captureKind,
                        subjectName,
                    )
                    ClosureValueUsage.EXPRESSION -> reporter.reportOn(
                        expression.source,
                        CfirErrors.FUNC_CAPTURE_VAR_CANNOT_EXPR,
                        subjectName,
                        captureKind,
                        subjectName,
                    )
                }
            }
        }
    }
}

/** 闭包捕获种类，对齐官方 `CaptureKind`。 */
private enum class ClosureCaptureKind {
    NONE,
    DIRECT_MUTABLE,
    TRANSITIVE,
}

/** 闭包捕获分析结果。 */
private data class ClosureCaptureInfo(
    val kind: ClosureCaptureKind,
    val mutableVariables: Set<CfirVariable>,
)

/**
 * 以函数为单位计算直接和传递可变捕获。
 *
 * 嵌套函数声明本身不并入当前函数的直接捕获；只有实际调用该闭包时，才把其中仍位于当前函数
 * 外部的捕获变量传播为传递捕获，与官方 `SetFuncBodyCaptureKind` 一致。
 */
private class ClosureCaptureAnalyzer {
    private val cache = IdentityHashMap<CfirFunction, ClosureCaptureInfo>()
    private val visiting = java.util.Collections.newSetFromMap(IdentityHashMap<CfirFunction, Boolean>())

    fun captureInfo(function: CfirFunction): ClosureCaptureInfo {
        cache[function]?.let { return it }
        if (!visiting.add(function)) return ClosureCaptureInfo(ClosureCaptureKind.NONE, emptySet())

        val directCaptures = linkedSetOf<CfirVariable>()
        val transitiveCaptures = linkedSetOf<CfirVariable>()
        function.body?.accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                element.acceptChildren(this, null)
            }

            override fun visitFunction(nestedFunction: CfirFunction) = Unit

            override fun visitNamedFunction(namedFunction: CfirNamedFunction) = Unit

            override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: CfirAnonymousFunctionExpression) = Unit

            override fun visitQualifiedAccessExpression(qualifiedAccessExpression: CfirQualifiedAccessExpression) {
                processVariableAccess(qualifiedAccessExpression)
            }

            override fun visitNamedAccessExpression(namedAccessExpression: CfirNamedAccessExpression) {
                processVariableAccess(namedAccessExpression)
            }

            private fun processVariableAccess(qualifiedAccessExpression: CfirQualifiedAccessExpression) {
                val variable = qualifiedAccessExpression.resolvedVariableOrNull()
                if (variable != null && variable.isMutableLocalCapturedBy(function)) {
                    directCaptures += variable
                }
                qualifiedAccessExpression.explicitReceiver?.accept(this, null)
            }

            override fun visitFunctionCall(functionCall: CfirFunctionCall) {
                val calledFunction = functionCall.resolvedFunctionOrNull()
                if (calledFunction != null && calledFunction !== function) {
                    for (capturedVariable in captureInfo(calledFunction).mutableVariables) {
                        if (!function.containsDeclarationInOwnScope(capturedVariable)) {
                            transitiveCaptures += capturedVariable
                        }
                    }
                }
                functionCall.explicitReceiver?.accept(this, null)
                for (argument in functionCall.argumentList.arguments) {
                    argument.accept(this, null)
                }
            }
        }, null)

        visiting.remove(function)
        val result = when {
            directCaptures.isNotEmpty() -> ClosureCaptureInfo(ClosureCaptureKind.DIRECT_MUTABLE, directCaptures + transitiveCaptures)
            transitiveCaptures.isNotEmpty() -> ClosureCaptureInfo(ClosureCaptureKind.TRANSITIVE, transitiveCaptures)
            else -> ClosureCaptureInfo(ClosureCaptureKind.NONE, emptySet())
        }
        cache[function] = result
        return result
    }
}

/** 当前函数是否直接捕获该可变局部变量。 */
private fun CfirVariable.isMutableLocalCapturedBy(function: CfirFunction): Boolean =
    isLocal && isVar && this !is CfirValueParameter && !function.containsDeclarationInOwnScope(this)

/** 判断声明是否属于函数自己的参数或函数体作用域，不进入嵌套函数。 */
private fun CfirFunction.containsDeclarationInOwnScope(target: CfirVariable): Boolean {
    if (valueParameters.any { it === target }) return true
    var found = false
    body?.accept(object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            if (found) return
            if (element === target) {
                found = true
                return
            }
            element.acceptChildren(this, null)
        }

        override fun visitFunction(function: CfirFunction) = Unit
        override fun visitNamedFunction(namedFunction: CfirNamedFunction) = Unit
        override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: CfirAnonymousFunctionExpression) = Unit
    }, null)
    return found
}

/** 从最终引用恢复具名函数目标。 */
private fun CfirQualifiedAccessExpression.resolvedFunctionOrNull(): CfirFunction? =
    when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol.cfir as? CfirFunction
        is CfirResolvedErrorReference -> reference.resolvedSymbol.cfir as? CfirFunction
        is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol?.cfir as? CfirFunction
        else -> null
    }

/** 从最终引用恢复变量目标。 */
private fun CfirQualifiedAccessExpression.resolvedVariableOrNull(): CfirVariable? =
    when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol.cfir as? CfirVariable
        is CfirResolvedErrorReference -> reference.resolvedSymbol.cfir as? CfirVariable
        is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol?.cfir as? CfirVariable
        else -> null
    }

/** 表达式值使用位置。 */
private enum class ClosureValueUsage { ASSIGN, RETURN, PARAMETER, EXPRESSION }

/** 根据 checker 上下文恢复闭包值所处的赋值、返回、参数或普通表达式位置。 */
private fun CfirExpression.valueUsage(context: CheckerContext): ClosureValueUsage {
    if (context.containingDeclarations.asReversed()
            .filterIsInstance<CfirVariable>()
            .any { variable -> variable.initializer.containsElement(this) }
    ) {
        return ClosureValueUsage.ASSIGN
    }
    if (context.containingElements.asReversed().any { it is CfirReturnExpression }) {
        return ClosureValueUsage.RETURN
    }
    if (context.callsOrAssignments.asReversed()
            .filterIsInstance<CfirFunctionCall>()
            .any { call -> call.argumentList.arguments.any { argument -> argument.containsElement(this) } }
    ) {
        return ClosureValueUsage.PARAMETER
    }
    return ClosureValueUsage.EXPRESSION
}

/** 以 identity 判断表达式树是否包含目标节点，不进入其他匿名函数体。 */
private fun CfirExpression?.containsElement(target: CfirElement): Boolean {
    if (this == null) return false
    if (this === target) return true
    var found = false
    accept(object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            if (found) return
            if (element === target) {
                found = true
                return
            }
            element.acceptChildren(this, null)
        }
    }, null)
    return found
}
