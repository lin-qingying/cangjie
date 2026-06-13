/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.builder

import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOpKind
import org.cangnova.cangjie.cfir.expressions.CfirComparisonOp
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions

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
    CjTokens.PLUSEQ to OperatorNameConventions.PLUS,
    CjTokens.MINUSEQ to OperatorNameConventions.MINUS,
    CjTokens.MULTEQ to OperatorNameConventions.TIMES,
    CjTokens.DIVEQ to OperatorNameConventions.DIV,
    CjTokens.PERCEQ to OperatorNameConventions.REM,
    CjTokens.ANDANDEQ to OperatorNameConventions.ANDAND,
    CjTokens.OROREQ to OperatorNameConventions.OROR,
    CjTokens.ANDEQ to OperatorNameConventions.AND,
    CjTokens.OREQ to OperatorNameConventions.OR,
    CjTokens.XOREQ to OperatorNameConventions.XOR,
    CjTokens.LTLTEQ to OperatorNameConventions.LEFT_SHIFT,
    CjTokens.GTGTEQ to OperatorNameConventions.RIGHT_SHIFT,
    CjTokens.MULMULEQ to OperatorNameConventions.EXPONENTIATION,
)

private val BINARY_OP_KINDS: Map<IElementType, CfirBinaryOpKind> = mapOf(
    CjTokens.ANDAND to CfirBinaryOpKind.AND,
    CjTokens.OROR to CfirBinaryOpKind.OR,
    CjTokens.COALESCING to CfirBinaryOpKind.COALESCING,
    CjTokens.PIPELINE to CfirBinaryOpKind.PIPELINE,
    CjTokens.COMPOSITION to CfirBinaryOpKind.COMPOSITION,
)

private val COMPARISON_OPS: Map<IElementType, CfirComparisonOp> = mapOf(
    CjTokens.LT to CfirComparisonOp.LT,
    CjTokens.OPERATION_COMPARE_LT to CfirComparisonOp.LT,
    CjTokens.GT to CfirComparisonOp.GT,
    CjTokens.OPERATION_COMPARE_GT to CfirComparisonOp.GT,
    CjTokens.LTEQ to CfirComparisonOp.LE,
    CjTokens.OPERATION_COMPARE_LTEQ to CfirComparisonOp.LE,
    CjTokens.GTEQ to CfirComparisonOp.GE,
    CjTokens.OPERATION_COMPARE_GTEQ to CfirComparisonOp.GE,
    CjTokens.EQEQ to CfirComparisonOp.EQ,
    CjTokens.OPERATION_EQUALS to CfirComparisonOp.EQ,
    CjTokens.EXCLEQ to CfirComparisonOp.NE,
    CjTokens.OPERATION_NOT_EQUALS to CfirComparisonOp.NE,
)

private val BINARY_OPERATOR_NAMES: Map<IElementType, Name> = mapOf(
    CjTokens.PLUS to OperatorNameConventions.PLUS,
    CjTokens.OPERATION_PLUS to OperatorNameConventions.PLUS,
    CjTokens.MINUS to OperatorNameConventions.MINUS,
    CjTokens.OPERATION_MINUS to OperatorNameConventions.MINUS,
    CjTokens.MUL to OperatorNameConventions.TIMES,
    CjTokens.OPERATION_TIMES to OperatorNameConventions.TIMES,
    CjTokens.DIV to OperatorNameConventions.DIV,
    CjTokens.OPERATION_DIV to OperatorNameConventions.DIV,
    CjTokens.PERC to OperatorNameConventions.REM,
    CjTokens.OPERATION_REM to OperatorNameConventions.REM,
    CjTokens.AND to OperatorNameConventions.AND,
    CjTokens.OPERATION_AND to OperatorNameConventions.AND,
    CjTokens.OR to OperatorNameConventions.OR,
    CjTokens.OPERATION_OR to OperatorNameConventions.OR,
    CjTokens.XOR to OperatorNameConventions.XOR,
    CjTokens.OPERATION_XOR to OperatorNameConventions.XOR,
    CjTokens.LTLT to OperatorNameConventions.LEFT_SHIFT,
    CjTokens.OPERATION_LEFT_SHIFT to OperatorNameConventions.LEFT_SHIFT,
    CjTokens.GTGT to OperatorNameConventions.RIGHT_SHIFT,
    CjTokens.OPERATION_RIGHT_SHIFT to OperatorNameConventions.RIGHT_SHIFT,
    CjTokens.MULMUL to OperatorNameConventions.EXPONENTIATION,
    CjTokens.OPERATION_EXPONENTIATION to OperatorNameConventions.EXPONENTIATION,
)

private val PREFIX_UNARY_NAMES: Map<IElementType, Name> = mapOf(
    CjTokens.MINUS to OperatorNameConventions.UNARY_MINUS,
    CjTokens.OPERATION_MINUS to OperatorNameConventions.UNARY_MINUS,
    CjTokens.PLUS to OperatorNameConventions.UNARY_PLUS,
    CjTokens.OPERATION_PLUS to OperatorNameConventions.UNARY_PLUS,
    CjTokens.EXCL to OperatorNameConventions.NOT,
    CjTokens.OPERATION_NOT to OperatorNameConventions.NOT,
    CjTokens.PLUSPLUS to OperatorNameConventions.INC,
    CjTokens.MINUSMINUS to OperatorNameConventions.DEC,
)

private val POSTFIX_UNARY_NAMES: Map<IElementType, Name> = mapOf(
    CjTokens.PLUSPLUS to OperatorNameConventions.INC,
    CjTokens.MINUSMINUS to OperatorNameConventions.DEC,
)
