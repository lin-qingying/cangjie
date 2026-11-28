package org.cangnova.cangjie.cfir.builder

import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.cfir.declarations.CfirDefaultAssignmentSyntaxFacts
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjNodeTypes

/**
 * 参数 default assignment 在语法归一化前的共享分类。
 * 原生 ParseUnaryOpExpr 将直接负数形成 literal，括号中的负数仍是 UnaryExpr；
 * ParenExpr 只传播 literal 的 const，不把复合运算、const 引用或 const 调用改成 literal。
 */
fun <N : Any> captureDefaultAssignmentSyntaxFacts(
    root: N,
    typeOf: (N) -> IElementType,
    childrenOf: (N) -> List<N>,
    isExpression: (N) -> Boolean,
): CfirDefaultAssignmentSyntaxFacts {
    fun classify(node: N): CfirDefaultAssignmentSyntaxFacts = when (typeOf(node)) {
        CjNodeTypes.INTEGER_CONSTANT, CjNodeTypes.FLOAT_CONSTANT, CjNodeTypes.BOOLEAN_CONSTANT,
        CjNodeTypes.RUNE_CONSTANT, CjNodeTypes.CHARACTER_BYTE_CONSTANT, CjNodeTypes.UNIT_CONSTANT,
        -> CfirDefaultAssignmentSyntaxFacts(isLiteral = true, literalConst = true)
        CjNodeTypes.STRING_TEMPLATE -> CfirDefaultAssignmentSyntaxFacts(
            isLiteral = true,
            literalConst = childrenOf(node).none {
                typeOf(it) == CjNodeTypes.LONG_STRING_TEMPLATE_ENTRY || typeOf(it) == CjNodeTypes.SHORT_STRING_TEMPLATE_ENTRY
            },
        )
        CjNodeTypes.PARENTHESIZED -> CfirDefaultAssignmentSyntaxFacts(
            isLiteral = false,
            literalConst = childrenOf(node).singleOrNull(isExpression)?.let { classify(it).literalConst } == true,
        )
        CjNodeTypes.PREFIX_EXPRESSION -> {
            val children = childrenOf(node)
            val operand = children.singleOrNull { typeOf(it) != CjNodeTypes.OPERATION_REFERENCE && isExpression(it) }
            val minus = children.firstOrNull { typeOf(it) == CjNodeTypes.OPERATION_REFERENCE }
                ?.let(childrenOf)?.any { typeOf(it) == CjTokens.MINUS } == true
            val isNegativeLiteral = minus && operand != null &&
                (typeOf(operand) == CjNodeTypes.INTEGER_CONSTANT || typeOf(operand) == CjNodeTypes.FLOAT_CONSTANT)
            CfirDefaultAssignmentSyntaxFacts(isLiteral = isNegativeLiteral, literalConst = isNegativeLiteral)
        }
        else -> CfirDefaultAssignmentSyntaxFacts(isLiteral = false, literalConst = false)
    }
    return classify(root)
}
