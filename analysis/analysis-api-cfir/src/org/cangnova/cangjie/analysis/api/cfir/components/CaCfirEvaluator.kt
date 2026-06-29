package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaEvaluator
import org.cangnova.cangjie.analysis.api.evaluation.CaCollectionCompileTimeValue
import org.cangnova.cangjie.analysis.api.evaluation.CaCompileTimeValue
import org.cangnova.cangjie.analysis.api.evaluation.CaScalarCompileTimeValue
import org.cangnova.cangjie.analysis.api.evaluation.CaScalarValueKind
import org.cangnova.cangjie.analysis.api.evaluation.CaTupleCompileTimeValue
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.psi.CjCollectionLiteralExpression
import org.cangnova.cangjie.psi.CjConstantExpression
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjParenthesizedExpression
import org.cangnova.cangjie.psi.CjPsiUtil
import org.cangnova.cangjie.psi.CjStatementExpression
import org.cangnova.cangjie.psi.CjStringTemplateExpression
import org.cangnova.cangjie.psi.CjTupleExpression
import org.cangnova.cangjie.psi.psiUtil.isPlain

/**
 * 编译期求值组件。
 *
 * 对齐 Kotlin 的 `KaFirEvaluator` 落位，去掉单独的 `EvaluationProtocol` 文件。
 * 公开组件与内部求值 helper 放在同一文件中，职责边界更稳定。
 */
internal class CaCfirEvaluator(
    /**
     * 延迟取得当前 CFIR Analysis session，保证求值入口在有效会话内执行。
     */
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaEvaluator {
    /**
     * 对表达式执行 Analysis API 层面的编译期值求值。
     */
    override fun CjExpression.evaluate(): CaCompileTimeValue? = withValidityAssertion {
        analysisSession.evaluateCompileTimeValue(this@evaluate)
    }
}

/**
 * 编译期值与表达式结构信息的 CFIR 实现。
 *
 * 这部分能力不直接暴露底层 CFIR 细节，而是围绕公开 PSI 形态建立稳定语义：
 * - 语句形态
 * - 编译期常量可求值性
 * - 结构化编译期值
 */
internal class CaCfirScalarCompileTimeValueImpl(
    /**
     * 标量值的公开分类。
     */
    override val kind: CaScalarValueKind,
    /**
     * 可稳定展示给调用方的源码文本或规范化文本。
     */
    override val renderedText: String,
    /**
     * 约束标量值对象生命周期的会话 token。
     */
    override val token: CaLifetimeToken,
) : CaScalarCompileTimeValue

/**
 * 元组字面量编译期值的 CFIR 实现。
 */
internal class CaCfirTupleCompileTimeValueImpl(
    /**
     * 元组中按源码顺序排列的子编译期值。
     */
    override val elements: List<CaCompileTimeValue>,
    /**
     * 约束元组值对象生命周期的会话 token。
     */
    override val token: CaLifetimeToken,
) : CaTupleCompileTimeValue {
    /**
     * 按元组字面量形态渲染的编译期值文本。
     */
    override val renderedText: String
        get() = elements.joinToString(prefix = "(", postfix = ")") { it.renderedText }
}

/**
 * 集合字面量编译期值的 CFIR 实现。
 */
internal class CaCfirCollectionCompileTimeValueImpl(
    /**
     * 集合中按源码顺序排列的子编译期值。
     */
    override val elements: List<CaCompileTimeValue>,
    /**
     * 约束集合值对象生命周期的会话 token。
     */
    override val token: CaLifetimeToken,
) : CaCollectionCompileTimeValue {
    /**
     * 按集合字面量形态渲染的编译期值文本。
     */
    override val renderedText: String
        get() = elements.joinToString(prefix = "[", postfix = "]") { it.renderedText }
}

/**
 * 判断表达式当前是否以语句形态参与控制流。
 */
internal fun CjExpression.isStatementLikeExpression(): Boolean {
    return this is CjStatementExpression || CjPsiUtil.isStatement(this)
}

/**
 * 结构化求值当前表达式的编译期值。
 *
 * 当前只覆盖仓颉 Analysis API 已经能够稳定公开的几类编译期值：
 * - 常量字面量
 * - 无插值字符串模板
 * - 元组字面量
 * - 集合字面量
 * - 单层括号包裹的上述表达式
 */
internal fun CaCfirSession.evaluateCompileTimeValue(expression: CjExpression): CaCompileTimeValue? {
    return when (expression) {
        is CjParenthesizedExpression -> expression.expression?.let(::evaluateCompileTimeValue)
        is CjConstantExpression -> expression.asCompileTimeScalar(token)
        is CjStringTemplateExpression -> expression.asCompileTimeString(token)
        is CjTupleExpression -> expression.asCompileTimeTuple(this, token)
        is CjCollectionLiteralExpression -> expression.asCompileTimeCollection(this, token)
        else -> null
    }
}

/**
 * 将常量表达式包装为公开标量编译期值。
 */
private fun CjConstantExpression.asCompileTimeScalar(token: CaLifetimeToken): CaScalarCompileTimeValue {
    val renderedText = text
    return CaCfirScalarCompileTimeValueImpl(
        kind = detectScalarKind(renderedText),
        renderedText = renderedText,
        token = token,
    )
}

/**
 * 将无插值的普通字符串模板转换为字符串编译期值。
 */
private fun CjStringTemplateExpression.asCompileTimeString(token: CaLifetimeToken): CaScalarCompileTimeValue? {
    if (hasInterpolation() || !isPlain()) return null
    return CaCfirScalarCompileTimeValueImpl(
        kind = CaScalarValueKind.STRING,
        renderedText = stringContent,
        token = token,
    )
}

/**
 * 递归求值元组字面量中的每个元素。
 */
private fun CjTupleExpression.asCompileTimeTuple(
    session: CaCfirSession,
    token: CaLifetimeToken,
): CaTupleCompileTimeValue? {
    val evaluatedElements = expressions.map { element ->
        session.evaluateCompileTimeValue(element) ?: return null
    }
    return CaCfirTupleCompileTimeValueImpl(
        elements = evaluatedElements,
        token = token,
    )
}

/**
 * 递归求值集合字面量中的每个元素。
 */
private fun CjCollectionLiteralExpression.asCompileTimeCollection(
    session: CaCfirSession,
    token: CaLifetimeToken,
): CaCollectionCompileTimeValue? {
    val evaluatedElements = innerExpressions.map { element ->
        session.evaluateCompileTimeValue(element) ?: return null
    }
    return CaCfirCollectionCompileTimeValueImpl(
        elements = evaluatedElements,
        token = token,
    )
}

/**
 * 根据字面量文本推断标量编译期值分类。
 */
private fun detectScalarKind(renderedText: String): CaScalarValueKind {
    return when {
        renderedText == "true" || renderedText == "false" -> CaScalarValueKind.BOOLEAN
        renderedText == "()" -> CaScalarValueKind.UNIT
        renderedText.startsWith("\"") || renderedText.startsWith("'") -> CaScalarValueKind.RUNE
        renderedText.contains('.') || renderedText.contains('e', ignoreCase = true) -> CaScalarValueKind.FLOAT
        renderedText.any(Char::isDigit) -> CaScalarValueKind.INTEGER
        else -> CaScalarValueKind.UNKNOWN
    }
}
