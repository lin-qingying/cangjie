package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.checkers.diagnosticFactoryForReturnTypeMismatch
import org.cangnova.cangjie.cfir.analysis.checkers.hasUninferredOmittedLambdaParameterType
import org.cangnova.cangjie.cfir.analysis.checkers.isSubtypeForTypeMismatch
import org.cangnova.cangjie.cfir.analysis.checkers.isFlowExpression
import org.cangnova.cangjie.cfir.analysis.checkers.lambdaExpectedFunctionType
import org.cangnova.cangjie.cfir.analysis.diagnostics.literalConversionDiagnostic
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.analysis.diagnostics.specificTypeMismatchDiagnostic
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.hasImplicitOrInferredReturnType
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirPropertyAccessor
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchExhaustivenessStatus
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirSuperReceiverExpression
import org.cangnova.cangjie.cfir.expressions.CfirThisReceiverExpression
import org.cangnova.cangjie.cfir.expressions.CfirThrowExpression
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.approximateThisTypeForDeclaration
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
        if (containingFunction.hasImplicitOrInferredReturnType()) return

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

        // 对齐官方 `CheckFuncBody`：body 通过普通返回类型检查后，`This` 返回类型还要求
        // 函数体尾表达式在语法上确实回传当前实例（`CheckReturnThisInFuncBody`）。
        // 该规则的诊断锚点是整个函数体 block，与尾表达式自身的类型不匹配诊断区分开。
        if (block.reportThisReturnFormMismatchIfNeeded(expectedType)) return

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

        if (tailExpression != null && checkTargetTypedExpression(tailExpression, expectedType).isHandled) return

        // 目标类型直接检查字面量时，显式 return 与隐式尾表达式都使用同一专用诊断。
        literalConversionDiagnostic(
            source = resultSource,
            expectedType = expectedType,
            expression = tailExpression,
            session = context.session,
        )?.let { diagnostic ->
            reporter.report(diagnostic, context)
            return
        }

        specificTypeMismatchDiagnostic(
            source = resultSource,
            expectedType = expectedType,
            actualType = actualType,
            expression = tailExpression,
            session = context.session,
        )?.let { diagnostic ->
            reporter.report(diagnostic, context)
            return
        }

        if (!isSubtypeForTypeMismatch(context.session, context.session.typeContext, actualType, expectedType)) {
            val diagnosticFactory = when {
                // 官方 ChkFlowExpr 在 flow 节点本身报告通用 mismatched-types；
                // flow 作为隐式尾返回值时不能退化成 RETURN_TYPE_MISMATCH。
                tailExpression?.isFlowExpression() == true -> CfirErrors.TYPE_MISMATCH
                // 函数体尾表达式就是隐式返回值，与显式 `return expr` 共享返回类型语义。
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
 * 报告 `This` 返回类型的函数体尾表达式形态不匹配。
 *
 * 对齐官方 `TypeChecker::TypeCheckerImpl::CheckFuncBody`
 * （`external/cangjie_compiler/src/Sema/TypeChecker.cpp`）中 `Is<ClassThisTy>(fb.retType->ty)` 分支：
 * body 通过普通返回类型检查后，`This` 还额外要求尾表达式在语法上确实回传当前实例，
 * 否则以整个函数体 block 为锚点报告返回类型不匹配。
 *
 * @return 是否已经报告诊断。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun CfirBlock.reportThisReturnFormMismatchIfNeeded(
    expectedType: ConeCangJieType,
): Boolean {
    val expectedThisType = expectedType.fullyExpandedType(context.session) as? ConeClassLikeType ?: return false
    if (!expectedThisType.isThisType) return false
    // 官方要求 `Is<ClassDecl>(fb.parentClassLike)`：只有 class 实例成员函数体才能满足 `This`。
    if (context.findClosestDeclaration<CfirClass>() == null) return false

    val tailStatement = statements.lastOrNull()
    val tailExpression = tailStatement as? CfirExpression ?: return false
    val tailType = tailExpression.coneTypeOrNull
    if (tailType is ConeErrorType) return false
    if (tailExpression.containsReportedErrorDiagnostic()) return false
    // 官方 `isWellTyped` 建模：body 或其中任一 `return` 的结果类型不满足 `This` 时，
    // 该位置已经由普通返回类型不匹配报告，本规则不再叠加第二条诊断。
    if (!tailExpression.satisfiesThisReturnTypeCheck(expectedType)) return false

    if (tailExpression.returnsCurrentInstance(context.session)) return false

    val bodySource = source ?: return false
    reporter.reportOn(
        source = bodySource,
        factory = diagnosticFactoryForReturnTypeMismatch(context.session, expectedType),
        a = expectedType,
        // 官方在报告前先执行 `ReplaceThisTy(fb.body->ty)`，因此实际类型渲染为普通类类型。
        b = (tailType ?: ConePrimitiveType.UNIT).approximateThisTypeForDeclaration(),
        c = false,
    )
    return true
}

/**
 * 判断函数体在普通返回类型检查下是否已经通过。
 *
 * 有显式 `return` 时按官方逐个检查其操作数类型；否则检查尾表达式自身类型。
 */
context(context: CheckerContext)
private fun CfirExpression.satisfiesThisReturnTypeCheck(expectedType: ConeCangJieType): Boolean {
    val returnExpressions = collectReturnExpressions()
    val checkedTypes = if (returnExpressions.isNotEmpty()) {
        returnExpressions.map { it.result.coneTypeOrNull }
    } else {
        listOf(coneTypeOrNull)
    }
    return checkedTypes.all { type ->
        type != null && isSubtypeForTypeMismatch(context.session, context.session.typeContext, type, expectedType)
    }
}

/** 收集表达式子树中的 `return` 表达式（不下钻到嵌套 `return` 的内部）。 */
private fun CfirExpression.collectReturnExpressions(): List<CfirReturnExpression> {
    if (this is CfirReturnExpression) return listOf(this)
    val result = mutableListOf<CfirReturnExpression>()
    acceptChildren(object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            if (element is CfirReturnExpression) {
                result += element
                return
            }
            element.acceptChildren(this, null)
        }
    }, null)
    return result
}

/**
 * 判断表达式在语法上是否确实回传当前实例。
 *
 * 对齐官方 `TypeChecker::TypeCheckerImpl::CheckReturnThisInFuncBody` 中的 `checkExprType`：
 * 只接受 `this` 本身、以 `this`/`super`/无限定形式调用且被调函数返回 `This` 的调用，
 * 其余表达式退化为“在子树中查找 `return`，并对其返回值套用同一判定”。
 */
private fun CfirExpression.returnsCurrentInstance(session: CfirSession): Boolean = when (this) {
    is CfirThisReceiverExpression -> true
    is CfirFunctionCall -> isCurrentInstanceCallReturningThisType(session)
    else -> lastNestedReturnExpression()?.result?.returnsCurrentInstance(session) ?: false
}

/**
 * 调用是否以 `this`/`super`/无限定形式发出，且被调函数自身返回 `This`。
 */
private fun CfirFunctionCall.isCurrentInstanceCallReturningThisType(session: CfirSession): Boolean {
    val receiver = explicitReceiver
    val isCurrentInstanceCall = receiver == null ||
            receiver is CfirThisReceiverExpression ||
            receiver is CfirSuperReceiverExpression
    if (!isCurrentInstanceCall) return false
    val calleeSymbol = (calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol ?: return false
    val callee = calleeSymbol.cfir as? CfirCallableDeclaration ?: return false
    val calleeReturnType = (callee.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return false
    return (calleeReturnType.fullyExpandedType(session) as? ConeClassLikeType)?.isThisType == true
}

/**
 * 在子树中查找最后一个 `return` 表达式。
 *
 * 官方 walker 对每个 `return` 覆写判定结果并跳过其子节点，因此遍历顺序上最后一个 `return` 生效。
 * 这条回退路径使 `try { return this } finally {}` 这类包裹结构仍能满足 `This` 返回类型。
 */
private fun CfirExpression.lastNestedReturnExpression(): CfirReturnExpression? {
    var result: CfirReturnExpression? = null
    val visitor = object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            if (element is CfirReturnExpression) {
                result = element
                return
            }
            element.acceptChildren(this, null)
        }
    }
    if (this is CfirReturnExpression) return this
    acceptChildren(visitor, null)
    return result
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
