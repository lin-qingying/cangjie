package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.checkers.diagnosticFactoryForReturnTypeMismatch
import org.cangnova.cangjie.cfir.analysis.checkers.hasUninferredOmittedLambdaParameterType
import org.cangnova.cangjie.cfir.analysis.checkers.isSubtypeForTypeMismatch
import org.cangnova.cangjie.cfir.analysis.checkers.lambdaExpectedFunctionType
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.analysis.diagnostics.specificTypeMismatchDiagnostic
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirPropertyAccessor
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchExhaustivenessStatus
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirThrowExpression
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid

/**
 * 函数体尾表达式返回类型检查器。
 *
 * 对齐官方 `TypeChecker::CheckFuncBody`：显式非 Unit 返回类型才将最外层
 * body block 按返回值检查；显式 Unit 返回类型只综合分析函数体，后续插入
 * `return ()`，不把普通尾表达式强制当作 Unit 返回值。
 */
object CfirFunctionBodyTypeMismatchChecker : CfirBasicExpressionChecker() {
    /** 检查函数体 block 尾表达式是否满足显式返回类型。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirStatement) {
        val block = expression as? CfirBlock ?: return
        val containingFunction = context.findClosestDeclaration<CfirFunction> { it.body === block } ?: return
        if (
            containingFunction is CfirAnonymousFunction &&
            containingFunction.hasUninferredOmittedLambdaParameterType() &&
            !containingFunction.hasLambdaShapeDiagnosticForBodyTypeCheck(context)
        ) {
            return
        }
        if (containingFunction.returnTypeRef is CfirImplicitTypeRef) return

        if (block.statements.dropLast(1).any { it is CfirReturnExpression }) return
        val tailStatement = block.statements.lastOrNull()
        val tailExpression = tailStatement as? CfirExpression
        if (tailExpression?.isTerminatingFunctionBodyTail() == true) return

        // 空函数体或以声明结尾的函数体会正常流出 Unit，仍然必须参与显式返回类型检查。
        // 属性 getter 尤其依赖这一规则：`get() {}` 不能因为没有尾表达式而绕过属性类型约束。
        val actualType = when (tailExpression) {
            null -> ConePrimitiveType.UNIT
            else -> tailExpression.coneTypeOrNull ?: return
        }
        if (actualType is ConeErrorType) return
        if (tailExpression?.containsReportedErrorDiagnostic() == true) return
        // 声明不是 block 的结果表达式，但官方仍把“声明导致 Unit 流出”的类型错误
        // 定位在该尾声明本身；只有真正的空 body 才定位到整个 block。
        val resultSource = tailStatement?.source ?: block.source ?: return

        val expectedType = when (containingFunction) {
            is CfirAnonymousFunction ->
                containingFunction.lambdaExpectedFunctionType(context)?.returnType
                    ?: (containingFunction.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                    ?: return
            is CfirConstructor -> ConePrimitiveType.UNIT
            else -> (containingFunction.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
        }
        if (expectedType is ConeErrorType) return
        if (expectedType.isUnit) return

        if (tailExpression != null && checkTargetTypedExpression(tailExpression, expectedType).isHandled) return

        specificTypeMismatchDiagnostic(
            source = resultSource,
            expectedType = expectedType,
            actualType = actualType,
            session = context.session,
        )?.let { diagnostic ->
            reporter.report(diagnostic, context)
            return
        }

        if (!isSubtypeForTypeMismatch(context.session, context.session.typeContext, actualType, expectedType)) {
            val diagnosticFactory = when {
                // 普通函数的 body 尾表达式是隐式返回值，必须和显式 `return expr`
                // 使用同一套返回类型不匹配诊断；尾部声明不是返回表达式，只作为 Unit 流出。
                tailExpression != null -> diagnosticFactoryForReturnTypeMismatch(context.session, expectedType)
                tailStatement != null -> CfirErrors.TYPE_MISMATCH
                containingFunction is CfirAnonymousFunction && containingFunction.isLambda -> CfirErrors.TYPE_MISMATCH
                containingFunction is CfirPropertyAccessor && containingFunction.isGetter -> CfirErrors.TYPE_MISMATCH
                else -> diagnosticFactoryForReturnTypeMismatch(context.session, expectedType)
            }
            reporter.reportOn(
                source = resultSource,
                factory = diagnosticFactory,
                a = expectedType,
                b = actualType,
                c = false,
            )
        }
    }
}

/**
 * Lambda 参数头部已经有更具体形状错误时，返回类型检查仍应继续执行。
 */
private fun CfirAnonymousFunction.hasLambdaShapeDiagnosticForBodyTypeCheck(context: CheckerContext): Boolean {
    if (context.hasLambdaParameterShapeDiagnostic(this)) return true
    val expectedFunctionType = lambdaExpectedFunctionType(context)
        ?: return false
    return valueParameters.size != expectedFunctionType.parameterTypes.size
}

/**
 * 函数体尾位置若被显式控制流终止，不存在需要与函数返回类型比较的隐式返回值。
 */
private fun CfirExpression.isTerminatingFunctionBodyTail(): Boolean {
    return when (this) {
        is CfirReturnExpression,
        is CfirThrowExpression,
        -> true

        is CfirBlock -> {
            val tailExpression = statements.lastOrNull() as? CfirExpression ?: return false
            tailExpression.isTerminatingFunctionBodyTail()
        }

        is CfirIfExpression -> {
            val elseBranch = elseBranch ?: return false
            thenBranch.isTerminatingFunctionBodyTail() && elseBranch.isTerminatingFunctionBodyTail()
        }

        is CfirMatchExpression -> {
            exhaustiveness is CfirMatchExhaustivenessStatus.Exhaustive &&
                    branches.isNotEmpty() &&
                    branches.all { it.body.isTerminatingFunctionBodyTail() }
        }

        is CfirTryExpression -> {
            val finallyTerminates = finallyBlock?.isTerminatingFunctionBodyTail() == true
            finallyTerminates ||
                    (tryBlock.isTerminatingFunctionBodyTail() &&
                            catches.all { it.body.isTerminatingFunctionBodyTail() } &&
                            handlers.all { it.body.isTerminatingFunctionBodyTail() })
        }

        else -> false
    }
}

/**
 * 尾表达式子树已经携带解析/约束诊断时，不再从函数体返回类型检查追加级联 mismatch。
 */
private fun CfirExpression.containsReportedErrorDiagnostic(): Boolean {
    if (this is CfirDiagnosticHolder) return true
    if (this is CfirResolvable && calleeReference is CfirDiagnosticHolder) return true

    var hasErrorDiagnostic = false
    acceptChildren(object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            if (hasErrorDiagnostic) return
            when {
                element is CfirDiagnosticHolder -> hasErrorDiagnostic = true
                element is CfirResolvable && element.calleeReference is CfirDiagnosticHolder -> hasErrorDiagnostic = true
                element is CfirExpression && element.coneTypeOrNull is ConeErrorType -> hasErrorDiagnostic = true
                else -> element.acceptChildren(this, null)
            }
        }
    }, null)
    return hasErrorDiagnostic
}
