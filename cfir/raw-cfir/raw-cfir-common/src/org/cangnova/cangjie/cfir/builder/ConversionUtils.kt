package org.cangnova.cangjie.cfir.builder

import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOpKind
import org.cangnova.cangjie.cfir.expressions.CfirComparisonOp
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name

/**
 * CFIR 构建通用工具函数（对齐 Kotlin 的 ConversionUtils.kt）。
 *
 * 包含运算符映射、表达式构建等工具，由 PSI 和 LightTree 两种构建器共享。
 * 不包含任何 PSI 节点类型引用（CjExpression, CjTypeReference 等）。
 */

// ===== 运算符 → 函数名映射（对齐 Kotlin 的 OperatorConventions） =====

/** 二元运算符 token → 可重载函数名 */
fun IElementType.toBinaryName(): Name? = BINARY_OPERATOR_NAMES[this]

/** 一元前缀运算符 token → 可重载函数名 */
fun IElementType.toPrefixUnaryName(): Name? = PREFIX_UNARY_NAMES[this]

/** 一元后缀运算符 token → 可重载函数名 */
fun IElementType.toPostfixUnaryName(): Name? = POSTFIX_UNARY_NAMES[this]

/** 复合赋值运算符 token → 对应的二元运算函数名 */
fun IElementType.toCompoundAssignName(): Name? = COMPOUND_ASSIGN_NAMES[this]

/** 是否为赋值运算符 */
fun IElementType.isAssignmentToken(): Boolean = this in ASSIGNMENT_TOKENS

/** 二元运算符 → CfirBinaryOpKind（逻辑/空合/管道） */
fun IElementType.toBinaryOpKind(): CfirBinaryOpKind? = BINARY_OP_KINDS[this]

/** 比较运算符 → CfirComparisonOp */
fun IElementType.toComparisonOp(): CfirComparisonOp? = COMPARISON_OPS[this]

// ===== 映射表 =====

private val ASSIGNMENT_TOKENS: Set<IElementType> = setOf(
    CjTokens.EQ, CjTokens.PLUSEQ, CjTokens.MINUSEQ,
    CjTokens.MULTEQ, CjTokens.DIVEQ, CjTokens.PERCEQ,
    CjTokens.ANDANDEQ, CjTokens.OREQ, CjTokens.ANDEQ,
    CjTokens.XOREQ, CjTokens.LTLTEQ, CjTokens.GTGTEQ,
    CjTokens.OROREQ, CjTokens.MULMULEQ,
)

private val COMPOUND_ASSIGN_NAMES: Map<IElementType, Name> = mapOf(
    CjTokens.PLUSEQ to Name.identifier("plus"),
    CjTokens.MINUSEQ to Name.identifier("minus"),
    CjTokens.MULTEQ to Name.identifier("times"),
    CjTokens.DIVEQ to Name.identifier("div"),
    CjTokens.PERCEQ to Name.identifier("rem"),
    CjTokens.ANDANDEQ to Name.identifier("and"),
    CjTokens.OREQ to Name.identifier("or"),
    CjTokens.ANDEQ to Name.identifier("and"),
    CjTokens.XOREQ to Name.identifier("xor"),
    CjTokens.LTLTEQ to Name.identifier("shl"),
    CjTokens.GTGTEQ to Name.identifier("shr"),
    CjTokens.OROREQ to Name.identifier("or"),
    CjTokens.MULMULEQ to Name.identifier("pow"),
)

private val BINARY_OP_KINDS: Map<IElementType, CfirBinaryOpKind> = mapOf(
    CjTokens.ANDAND to CfirBinaryOpKind.AND,
    CjTokens.OROR to CfirBinaryOpKind.OR,
    CjTokens.COALESCING to CfirBinaryOpKind.COALESCING,
    CjTokens.PIPELINE to CfirBinaryOpKind.PIPELINE,
)

private val COMPARISON_OPS: Map<IElementType, CfirComparisonOp> = mapOf(
    CjTokens.LT to CfirComparisonOp.LT,
    CjTokens.GT to CfirComparisonOp.GT,
    CjTokens.LTEQ to CfirComparisonOp.LE,
    CjTokens.GTEQ to CfirComparisonOp.GE,
    CjTokens.EQEQ to CfirComparisonOp.EQ,
    CjTokens.EXCLEQ to CfirComparisonOp.NE,
)

private val BINARY_OPERATOR_NAMES: Map<IElementType, Name> = mapOf(
    CjTokens.PLUS to Name.identifier("plus"),
    CjTokens.MINUS to Name.identifier("minus"),
    CjTokens.MUL to Name.identifier("times"),
    CjTokens.DIV to Name.identifier("div"),
    CjTokens.PERC to Name.identifier("rem"),
    CjTokens.AND to Name.identifier("and"),
    CjTokens.OR to Name.identifier("or"),
    CjTokens.XOR to Name.identifier("xor"),
    CjTokens.LTLT to Name.identifier("shl"),
    CjTokens.GTGT to Name.identifier("shr"),
    CjTokens.MULMUL to Name.identifier("pow"),
)

private val PREFIX_UNARY_NAMES: Map<IElementType, Name> = mapOf(
    CjTokens.MINUS to Name.identifier("unaryMinus"),
    CjTokens.PLUS to Name.identifier("unaryPlus"),
    CjTokens.EXCL to Name.identifier("not"),
    CjTokens.TILDE to Name.identifier("inv"),
    CjTokens.PLUSPLUS to Name.identifier("inc"),
    CjTokens.MINUSMINUS to Name.identifier("dec"),
)

private val POSTFIX_UNARY_NAMES: Map<IElementType, Name> = mapOf(
    CjTokens.PLUSPLUS to Name.identifier("postInc"),
    CjTokens.MINUSMINUS to Name.identifier("postDec"),
)
