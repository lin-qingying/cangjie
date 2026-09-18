package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.lambdaExpectedFunctionType
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirSuperReceiverExpression
import org.cangnova.cangjie.cfir.expressions.CfirThisReceiverExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid

/**
 * CFunc lambda 的捕获规则检查器。
 *
 * 官方把 CFunc lambda 的捕获分成两条语义：局部变量/局部闭包不能跨函数体捕获，
 * 实例状态（this、super、非 static 成员字段或属性）不能被隐式带入 CFunc。这里保留
 * 两条诊断的来源和范围，并让 this、super、named access、function call 分别挂在真实
 * CFIR visitor seam 上，避免把所有引用压到一个基于源码文本的 checker 中。
 */
object CfirCFuncCaptureThisChecker : CfirBasicExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirStatement) {
        val thisExpression = expression as? CfirThisReceiverExpression ?: return
        if (!context.isInsideCFuncLambda(thisExpression)) return

        reporter.reportOn(
            source = thisExpression.source,
            factory = CfirErrors.CFUNC_CANNOT_CAPTURE_THIS,
            a = "this",
        )
    }
}

/** CFunc lambda 中的 `super` receiver 不能被捕获。 */
object CfirCFuncCaptureSuperChecker : CfirSuperReceiverExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirSuperReceiverExpression) {
        if (!context.isInsideCFuncLambda(expression)) return

        reporter.reportOn(
            source = expression.source,
            factory = CfirErrors.CFUNC_CANNOT_CAPTURE_THIS,
            a = "super",
        )
    }
}

/**
 * CFunc lambda 中无显式 receiver 的 named access 捕获检查。
 *
 * 显式 `this`/`super` 已经由对应 receiver checker 报告，显式普通对象 receiver
 * 则由其自身访问节点检查局部变量捕获；因此这里只处理官方的隐式成员访问形状。
 */
object CfirCFuncCaptureNamedAccessChecker : CfirQualifiedAccessChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        if (expression is CfirFunctionCall) return
        if (expression.explicitReceiver != null || expression.dispatchReceiver != null) return
        val target = expression.resolvedTargetDeclarationOrNull() ?: return
        if (!context.isInsideCFuncLambda(expression)) return
        if (context.reportCFuncCapturedLocal(
                target,
                expression.source ?: expression.calleeReference.source,
                report = { captured, source ->
                    reporter.reportOn(
                        source = source,
                        factory = CfirErrors.CFUNC_CANNOT_CAPTURE_VAR,
                        a = captured.captureName(),
                    )
                },
            )
        ) return
        if ((target as? CfirCallableDeclaration)?.isNonStaticInstanceMember() != true) return

        reporter.reportOn(
            source = expression.calleeReference.source ?: expression.source,
            factory = CfirErrors.CFUNC_CANNOT_CAPTURE_THIS,
            a = "this",
        )
    }
}

/** CFunc lambda 中局部变量/局部闭包作为 call callee 的捕获检查。 */
object CfirCFuncCaptureCallChecker : CfirFunctionCallChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        if (!context.isInsideCFuncLambda(expression)) return
        val target = expression.resolvedTargetDeclarationOrNull() ?: return
        context.reportCFuncCapturedLocal(
            target,
            expression.calleeReference.source ?: expression.source,
            report = { captured, source ->
                reporter.reportOn(
                    source = source,
                    factory = CfirErrors.CFUNC_CANNOT_CAPTURE_VAR,
                    a = captured.captureName(),
                )
            },
        )
    }
}

/** 判断当前 checker 路径是否处于某个 CFunc lambda 的函数体中。 */
private fun CheckerContext.isInsideCFuncLambda(expression: CfirExpression): Boolean {
    val source = expression.source ?: return false
    return containingElements.asReversed()
        .filterIsInstance<CfirAnonymousFunctionExpression>()
        .any { lambdaExpression ->
            val lambdaSource = lambdaExpression.source ?: return@any false
            lambdaExpression.anonymousFunction.isCFuncLambda(this) &&
                lambdaSource.startOffset <= source.startOffset &&
                source.endOffset <= lambdaSource.endOffset
        }
}

/** 读取 lambda 的已解析函数签名，不从源码名称推断 CFunc 身份。 */
private fun CfirAnonymousFunction.isCFuncLambda(context: CheckerContext): Boolean =
    lambdaExpectedFunctionType(context)?.isCFunc == true

/**
 * 按官方 `IsCapturedInCFuncLambda` 的函数体边界判断局部声明是否跨过 CFunc 捕获。
 * 当前函数体或 CFunc lambda 自身声明的局部变量不是捕获；越过 CFunc 后才是非法捕获。
 */
private fun CheckerContext.isCapturedInCFuncLambda(target: CfirDeclaration): Boolean {
    if ((target as? CfirCallableDeclaration)?.isLocal != true) return false
    val functions = containingDeclarations.asReversed()
        .mapNotNull { symbol -> symbol.cfir as? CfirFunction }
    if (functions.isEmpty()) return false

    val current = functions.first()
    val targetOwnerIndex = functions.indexOfFirst { function -> function.ownsDeclaration(target) }
    if (targetOwnerIndex == 0) return false
    if ((current as? CfirAnonymousFunction)?.isCFuncLambda(this) == true) return true

    for (index in 1 until functions.size) {
        val function = functions[index]
        // 与官方实现相同：先判断 target body，再判断该层是否为 CFunc。
        if (index == targetOwnerIndex) return false
        if ((function as? CfirAnonymousFunction)?.isCFuncLambda(this) == true) return true
    }
    return false
}

/**
 * 判断 [target] 是否由当前函数直接拥有。
 *
 * target 是函数形参，或出现在函数 body 子树中；递归遇到嵌套函数即停止，
 * 嵌套函数体内的声明不归外层函数拥有。
 */
private fun CfirFunction.ownsDeclaration(target: CfirDeclaration): Boolean {
    if (valueParameters.any { parameter -> parameter === target }) return true

    var found = false
    body?.accept(object : CfirVisitorVoid() {
        override fun visitElement(element: org.cangnova.cangjie.cfir.CfirElement) {
            if (found) return
            if (element === target) {
                found = true
                return
            }
            if (element is org.cangnova.cangjie.cfir.declarations.CfirFunction) return
            element.acceptChildren(this, null)
        }

        override fun visitFunction(function: org.cangnova.cangjie.cfir.declarations.CfirFunction) {
            if (function === target) found = true
        }
    }, null)
    return found
}

/** 将捕获诊断限定为已经解析的 declaration，不使用短名或源码文本。 */
private fun CheckerContext.reportCFuncCapturedLocal(
    target: CfirDeclaration,
    source: org.cangnova.cangjie.source.CjSourceElement?,
    report: (CfirDeclaration, org.cangnova.cangjie.source.CjSourceElement?) -> Unit,
): Boolean {
    if (!isCapturedInCFuncLambda(target)) return false
    report(target, source)
    return true
}

/** 读取限定访问表达式解析到的声明；引用未解析或 symbol 未绑定时返回 `null`。 */
private fun CfirQualifiedAccessExpression.resolvedTargetDeclarationOrNull(): CfirDeclaration? {
    val symbol: CfirBasedSymbol<*> = when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol
        is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol
        else -> return null
    }
    return symbol.takeIf { it.isBound }?.cfir
}

/** 判断 callable 是否为非静态的实例成员（有 dispatch receiver 且未标记 static）。 */
private fun CfirCallableDeclaration.isNonStaticInstanceMember(): Boolean =
    this !is CfirConstructor && this !is CfirEnumConstructor &&
        dispatchReceiverType != null && !status.isStatic

/** 局部 callable 的稳定诊断名来自已解析 symbol，而非源码文本。 */
private fun CfirDeclaration.captureName(): Name =
    (symbol as? CfirCallableSymbol<*>)?.name ?: Name.identifier("<anonymous>")
