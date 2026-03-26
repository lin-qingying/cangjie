package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeIdealLiteralType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer

/**
 * Minimal local approximation transformer for the current call-completion seam.
 *
 * The upstream transformer also handles wrapped integer operators and more receiver shapes.
 * The local tree currently reaches only expression-result rewriting plus explicit receivers,
 * so we keep the collaborator narrow and only normalize ideal literal result types here.
 */
class IntegerLiteralAndOperatorApproximationTransformer(
    override val session: CfirSession,
    override val scopeSession: ScopeSession,
) : CfirTransformer<ConeCangJieType?>(), SessionAndScopeSessionHolder {
    override fun <E : CfirElement> transformElement(element: E, data: ConeCangJieType?): E = element

    override fun transformExpression(expression: CfirExpression, data: ConeCangJieType?): CfirExpression {
        expression.transformChildren(this, data)
        val approximatedType = expression.coneTypeOrNull?.approximateIntegerLiteralType(data)
        if (approximatedType != expression.coneTypeOrNull) {
            expression.replaceConeTypeOrNull(approximatedType)
        }
        return expression
    }

    fun approximateType(type: ConeCangJieType?, expectedType: ConeCangJieType? = null): ConeCangJieType? {
        return type?.approximateIntegerLiteralType(expectedType)
    }

    private fun ConeCangJieType.approximateIntegerLiteralType(expectedType: ConeCangJieType?): ConeCangJieType {
        return when (this) {
            is ConeIdealLiteralType -> expectedType ?: getApproximatedType()
            else -> this
        }
    }
}
