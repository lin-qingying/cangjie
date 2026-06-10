package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitor

/**
 * 对齐 Kotlin `FirUnusedExpressionChecker/FirUnusedCheckerBase` 的声明级 visitor 入口。
 *
 * 当前 analysis-tests 只注册 `CommonDeclarationCheckers`，因此这里直接挂在 common
 * declaration checker 流里，让 `try/finally` 中“结果被丢弃”的纯表达式走统一的 unused
 * 诊断路径，而不是在具体 expression checker 里做特判。
 */
object CfirUnusedExpressionChecker : CfirBasicDeclarationChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        val visitor = UsageVisitor(context, reporter, declaration)
        when (declaration) {
            is CfirCodeFragment -> declaration.block.accept(visitor, UsageState.Used)
            is CfirAnonymousFunction -> Unit
            is CfirFunction -> declaration.body?.accept(visitor, declaration.bodyUsageState())
            is CfirVariable -> declaration.initializer?.accept(visitor, UsageState.Used)
            else -> Unit
        }
    }

    private fun CfirFunction.bodyUsageState(): UsageState {
        val returnType = returnTypeRef.coneTypeOrNull
        return if (returnType == ConePrimitiveType.UNIT) UsageState.UnusedUnitReturn else UsageState.Used
    }

    private enum class UsageState {
        Used,
        Unused,
        UnusedUnitReturn,
        ;

        fun isUnused(): Boolean = this == Unused || this == UnusedUnitReturn
    }

    private class UsageVisitor(
        private val context: CheckerContext,
        private val reporter: DiagnosticReporter,
        private val declaration: CfirDeclaration,
    ) : CfirDefaultVisitor<Unit, UsageState>() {
        override fun visitDeclaration(declaration: CfirDeclaration, data: UsageState) {
            // 嵌套声明由诊断收集器单独驱动对应的 declaration checker，不在这里重复扫描。
        }

        override fun visitElement(element: CfirElement, data: UsageState) {
            if (element is CfirExpression && element.source != null) {
                checkExpression(element, data)
            }
            element.acceptChildren(this, UsageState.Used)
        }

        override fun visitAnonymousFunctionExpression(
            anonymousFunctionExpression: CfirAnonymousFunctionExpression,
            data: UsageState,
        ) {
            checkExpression(anonymousFunctionExpression, data)
            val bodyUsage = if (anonymousFunctionExpression.anonymousFunction.isLambda) {
                UsageState.Used
            } else {
                UsageState.Unused
            }
            anonymousFunctionExpression.anonymousFunction.body?.accept(this, bodyUsage)
        }

        override fun visitReturnExpression(returnExpression: CfirReturnExpression, data: UsageState) {
            returnExpression.result.accept(this, UsageState.Used)
        }

        override fun visitIfExpression(ifExpression: CfirIfExpression, data: UsageState) {
            checkExpression(ifExpression, data)
            ifExpression.condition.accept(this, UsageState.Used)
            ifExpression.thenBranch.accept(this, data)
            ifExpression.elseBranch?.accept(this, data)
        }

        override fun visitMatchExpression(matchExpression: CfirMatchExpression, data: UsageState) {
            checkExpression(matchExpression, data)
            matchExpression.subject?.accept(this, UsageState.Used)
            matchExpression.branches.forEach { branch ->
                branch.guard?.accept(this, UsageState.Used)
                branch.body.accept(this, data)
            }
        }

        override fun visitTryExpression(tryExpression: CfirTryExpression, data: UsageState) {
            checkExpression(tryExpression, data)
            tryExpression.tryBlock.accept(this, data)
            tryExpression.catches.forEach { catchClause ->
                catchClause.body.accept(this, data)
            }
            tryExpression.finallyBlock?.accept(this, UsageState.Unused)
        }

        override fun visitLoopExpression(loopExpression: CfirLoopExpression, data: UsageState) {
            checkExpression(loopExpression, data)
            loopExpression.condition.accept(this, UsageState.Used)
            loopExpression.body.accept(this, UsageState.Unused)
        }

        override fun visitBlock(block: CfirBlock, data: UsageState) {
            checkExpression(block, data)
            val lastIndex = block.statements.lastIndex
            for (index in block.statements.indices) {
                val usage = if (index == lastIndex) data else UsageState.Unused
                block.statements[index].accept(this, usage)
            }
        }

        private fun checkExpression(expression: CfirExpression, data: UsageState) {
            if (!data.isUnused()) return
            // 官方 cjc 只在 Unit 返回位置的非 Unit 表达式上报 unused expression，尾部 `()` 是有效返回值。
            if (data == UsageState.UnusedUnitReturn && expression.coneTypeOrNull == ConePrimitiveType.UNIT) return
            if (expression.hasSideEffect()) return
            if (expression is CfirAnonymousFunctionExpression) return
            if (expression is CfirThisReceiverExpression && declaration is CfirFinalizer) return
            with(context) {
                reporter.reportOn(expression.source, CfirErrors.UNUSED_EXPRESSION)
            }
        }

        private fun CfirExpression.hasSideEffect(): Boolean {
            return when (this) {
                is CfirLiteralExpression,
                is CfirThisReceiverExpression,
                is CfirAnonymousFunctionExpression,
                    -> false

                is CfirWrappedExpression -> expression.hasSideEffect()
                is CfirOptionalExpression -> expression.hasSideEffect()
                is CfirSmartCastExpression -> originalExpression.hasSideEffect()

                is CfirFunctionCall -> true

                is CfirQualifiedAccessExpression -> {
                    val dispatchHasSideEffect = dispatchReceiver?.hasSideEffect() == true
                    val explicitHasSideEffect = explicitReceiver?.hasSideEffect() == true
                    if (dispatchHasSideEffect || explicitHasSideEffect) {
                        true
                    } else {
                        when (val symbol = (calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol) {
                            is CfirValueParameterSymbol,
                            is CfirPatternVariableSymbol,
                            is CfirPatternBindingSymbol,
                                -> false

                            is CfirFieldVariableSymbol -> symbol.isBound && symbol.cfir.isLocal
                            is CfirPropertySymbol -> {
                                symbol.isBound &&
                                        symbol.cfir.isLocal &&
                                        symbol.getterSymbol == null &&
                                        symbol.setterSymbol == null
                            }

                            else -> true
                        }
                    }
                }

                else -> true
            }
        }
    }
}
