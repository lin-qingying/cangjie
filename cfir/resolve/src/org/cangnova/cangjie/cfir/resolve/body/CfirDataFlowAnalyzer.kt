package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid

/**
 * Minimal body-resolve data-flow facade required by the current call-completion chain.
 *
 * The broader CFG/smart-cast responsibilities remain for later tasks; for now we only
 * provide the direct anonymous-function return collection already consumed by
 * `CfirCallCompleter`.
 */
class CfirDataFlowAnalyzer(
    private val sessionHolder: SessionHolder,
    @Suppress("UNUSED_PARAMETER") private val context: BodyResolveContext,
) : SessionHolder by sessionHolder {
    fun returnExpressionsOfAnonymousFunction(function: CfirAnonymousFunction): Collection<CfirAnonymousFunctionReturnExpressionInfo> {
        val body = function.body ?: return emptyList()
        val collector = ReturnExpressionCollector(function)
        body.acceptChildren(collector)
        return collector.results
    }

    data class CfirAnonymousFunctionReturnExpressionInfo(
        val expression: CfirExpression,
    )

    private class ReturnExpressionCollector(
        private val owner: CfirAnonymousFunction,
    ) : CfirDefaultVisitorVoid() {
        private val _results = mutableListOf<CfirAnonymousFunctionReturnExpressionInfo>()
        val results: List<CfirAnonymousFunctionReturnExpressionInfo>
            get() = _results

        override fun visitElement(element: org.cangnova.cangjie.cfir.CfirElement) {
            element.acceptChildren(this)
        }

        override fun visitAnonymousFunction(anonymousFunction: CfirAnonymousFunction) {
            if (anonymousFunction === owner) {
                anonymousFunction.acceptChildren(this)
            }
        }

        override fun visitReturnExpression(returnExpression: CfirReturnExpression) {
            returnExpression.result?.let { expression ->
                _results += CfirAnonymousFunctionReturnExpressionInfo(expression)
            }
            returnExpression.acceptChildren(this)
        }
    }
}
