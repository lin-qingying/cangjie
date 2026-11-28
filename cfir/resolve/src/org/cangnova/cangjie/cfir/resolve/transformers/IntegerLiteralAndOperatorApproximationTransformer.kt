package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeIdealLiteralType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeStubType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.contains
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.IdealTypeResolver
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer

/**
 * 整数字面量与基础运算符类型近似 transformer。
 *
 * 上游 Kotlin transformer 还处理包装整数运算符和更多 receiver 形态；当前 CFIR 路径只需要表达式结果类型重写
 * 与显式 receiver 场景，因此此处只归一化 ideal literal 结果类型。
 */
class IntegerLiteralAndOperatorApproximationTransformer(
    /** 当前 CFIR session。 */
    override val session: CfirSession,
    /** 当前 scope session。 */
    override val scopeSession: ScopeSession,
) : CfirTransformer<ConeCangJieType?>(), SessionAndScopeSessionHolder {
    /** 默认不递归未知元素，保持该 transformer 只处理表达式节点。 */
    override fun <E : CfirElement> transformElement(element: E, data: ConeCangJieType?): E = element

    /**
     * 对位 Kotlin 的具体字面量入口；generated visitor 不经 transformExpression 转发。
     * 只收敛当前字面量的 ideal 类型，保留显式后缀、已固定类型和仍未完成的目标。
     */
    override fun transformLiteralExpression(literalExpression: CfirLiteralExpression, data: ConeCangJieType?): CfirExpression {
        if (data?.contains { it is ConeTypeVariableType || it is ConeStubType || it is ConeErrorType } == true) {
            return literalExpression
        }
        val actualType = literalExpression.coneTypeOrNull ?: return literalExpression
        val approximatedType = actualType.approximateIntegerLiteralType(data)
        if (approximatedType != actualType) literalExpression.replaceConeTypeOrNull(approximatedType)
        return literalExpression
    }

    /** 递归处理表达式子节点，并按期望类型近似当前表达式结果类型。 */
    override fun transformExpression(expression: CfirExpression, data: ConeCangJieType?): CfirExpression {
        expression.transformChildren(this, data)
        val approximatedType = expression.coneTypeOrNull?.approximateIntegerLiteralType(data)
        if (approximatedType != expression.coneTypeOrNull) {
            expression.replaceConeTypeOrNull(approximatedType)
        }
        return expression
    }

    /** 对外提供单个类型的 ideal literal 近似入口。 */
    fun approximateType(type: ConeCangJieType?, expectedType: ConeCangJieType? = null): ConeCangJieType? {
        return type?.approximateIntegerLiteralType(expectedType)
    }

    /** 根据期望类型把 ideal literal 或 primitive ideal 类型近似为具体类型。 */
    private fun ConeCangJieType.approximateIntegerLiteralType(expectedType: ConeCangJieType?): ConeCangJieType {
        val expandedExpectedType = expectedType?.fullyExpandedType(session)
        return when (this) {
            is ConeIdealLiteralType -> getApproximatedType(expandedExpectedType)
            is ConePrimitiveType -> IdealTypeResolver.resolveIfIdeal(this, expandedExpectedType)
            else -> this
        }
    }
}
