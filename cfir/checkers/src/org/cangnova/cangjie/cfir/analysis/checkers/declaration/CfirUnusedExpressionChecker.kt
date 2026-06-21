package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
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
                if (!branch.body.isPureUnitBranchResult()) {
                    branch.body.accept(this, data)
                }
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
                is CfirTupleLiteral -> elements.any { it.hasSideEffect() }

                is CfirFunctionCall -> true

                is CfirQualifiedAccessExpression -> {
                    /*
                     * 官方 unused 诊断来自 CHIR DCE：LOAD/FIELD/GET_ELEMENT_REF 这类取值
                     * 表达式在结果无用户时可被报告。CFIR 中非调用的 qualified access
                     * 对应这类取值；只有 receiver 求值本身有副作用时才阻止报告。
                     */
                    hasAccessReceiverSideEffect()
                }

                else -> true
            }
        }

        private fun CfirQualifiedAccessExpression.hasAccessReceiverSideEffect(): Boolean {
            if (calleeReference is CfirDiagnosticHolder) return true
            if (!isValueLikeAccess()) return true
            if (explicitReceiver?.hasSideEffect() == true) return true
            return dispatchReceiver !== explicitReceiver && dispatchReceiver?.hasSideEffect() == true
        }

        private fun CfirQualifiedAccessExpression.isValueLikeAccess(): Boolean {
            val symbol = (calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol ?: return false
            return symbol is CfirVariableSymbol<*> ||
                    symbol is CfirPropertySymbol ||
                    symbol is CfirEnumConstructorSymbol
        }

        /**
         * 对齐 Kotlin `FirUnusedCheckerBase.isUnitBlock`：match/when 分支中的纯 `Unit` 结果
         * 是分支占位结果，不作为被丢弃的普通表达式报告。
         */
        private fun CfirExpression.isPureUnitBranchResult(): Boolean {
            val singleResult = when (this) {
                is CfirBlock -> statements.singleOrNull() as? CfirExpression
                else -> this
            }
            return singleResult is CfirLiteralExpression && singleResult.coneTypeOrNull == ConePrimitiveType.UNIT
        }
    }
}
