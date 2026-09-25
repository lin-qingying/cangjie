package org.cangnova.cangjie.analysis.decompiler.stub

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.lexer.CjSingleValueToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.ConstantValueKind
import org.cangnova.cangjie.psi.stubs.elements.CjConstantExpressionElementType.Companion.kindToConstantElementType
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.impl.*

/** 注解参数与声明头共用 compiled stub 协议；不依赖 sidecar 原文或已绑定的源码 PSI。 */
internal fun createAnnotationChildrenStubs(parent: StubElement<*>, annotation: CfirAnnotationCall, sourceName: String) {
    val callee = CangJiePlaceHolderStubImpl<CjConstructorCalleeExpression>(parent, CjStubElementTypes.CONSTRUCTOR_CALLEE)
    val type = CangJiePlaceHolderStubImpl<CjTypeReference>(callee, CjStubElementTypes.TYPE_REFERENCE)
    fun userType(parent: StubElement<*>, segments: List<String>) {
        val stub = CangJieUserTypeStubImpl(parent)
        if (segments.size > 1) userType(stub, segments.dropLast(1))
        CangJieNameReferenceExpressionStubImpl(stub, StringRef.fromString(segments.last()))
    }
    userType(type, sourceName.split('.'))

    val arguments = annotation.argumentList.arguments
    if (arguments.isEmpty()) return
    val list = CangJiePlaceHolderStubImpl<CjValueArgumentList>(parent, CjStubElementTypes.VALUE_ARGUMENT_LIST)
    for (argument in arguments) {
        val stub = CangJieValueArgumentStubImpl(list, CjStubElementTypes.VALUE_ARGUMENT, false)
        val expression = if (argument is CfirNamedArgumentExpression) {
            val name = CangJiePlaceHolderStubImpl<CjValueArgumentName>(stub, CjStubElementTypes.VALUE_ARGUMENT_NAME)
            CangJieNameReferenceExpressionStubImpl(name, StringRef.fromString(argument.argumentName.asString()))
            argument.expression
        } else argument
        createAnnotationExpressionStub(stub, expression)
    }
}

/**
 * 为注解实参表达式创建对应 stub 子树。
 *
 * 支持字面量、二元/比较表达式、命名引用和 collection literal；命名引用缺名或出现尚未有
 * 对等 PSI stub 结构的表达式均视为二进制损坏，直接 error，不生成伪节点。
 */
private fun createAnnotationExpressionStub(parent: StubElement<*>, expression: CfirExpression) {
    when (expression) {
        is CfirLiteralExpression -> createAnnotationLiteralStub(parent, expression)
        is CfirBinaryOp -> createAnnotationBinaryExpressionStub(
            parent,
            expression.left,
            expression.kind.toCjToken(),
            expression.right,
        )
        is CfirComparisonExpression -> createAnnotationBinaryExpressionStub(
            parent,
            expression.left,
            expression.operation.toCjToken(),
            expression.right,
        )
        is CfirArrayLiteral -> {
            val collection = CangJiePlaceHolderStubImpl<CjCollectionLiteralExpression>(
                parent,
                CjStubElementTypes.COLLECTION_LITERAL_EXPRESSION,
            )
            expression.elements.forEach { element ->
                createAnnotationExpressionStub(collection, element)
            }
        }
        is CfirNamedAccessExpression -> {
            val reference = expression.calleeReference as? CfirNamedReference
                ?: error("Annotation reference has no name: ${expression.calleeReference::class.simpleName}")
            val name = StringRef.fromString(reference.name.asString())
            val explicitReceiver = expression.explicitReceiver
            if (explicitReceiver == null) {
                CangJieNameReferenceExpressionStubImpl(parent, name)
            } else {
                val qualified = CangJiePlaceHolderStubImpl<CjDotQualifiedExpression>(
                    parent,
                    CjStubElementTypes.DOT_QUALIFIED_EXPRESSION,
                )
                createAnnotationExpressionStub(qualified, explicitReceiver)
                CangJieNameReferenceExpressionStubImpl(qualified, name)
            }
        }
        else -> error("Unsupported compiled annotation expression: ${expression::class.simpleName}")
    }
}

/** 二元和比较注解实参与源码 PSI 共用“左操作数、运算符、右操作数”的 Stub 结构。 */
private fun createAnnotationBinaryExpressionStub(
    parent: StubElement<*>,
    left: CfirExpression,
    operation: CjSingleValueToken,
    right: CfirExpression,
) {
    val binary = CangJiePlaceHolderStubImpl<CjBinaryExpression>(parent, CjStubElementTypes.BINARY_EXPRESSION)
    createAnnotationExpressionStub(binary, left)
    CangJieOperationReferenceStubImpl(binary, operation)
    createAnnotationExpressionStub(binary, right)
}

private fun CfirBinaryOpKind.toCjToken(): CjSingleValueToken = when (this) {
    CfirBinaryOpKind.AND -> CjTokens.ANDAND
    CfirBinaryOpKind.OR -> CjTokens.OROR
    CfirBinaryOpKind.COALESCING -> CjTokens.COALESCING
    CfirBinaryOpKind.PIPELINE -> CjTokens.PIPELINE
    CfirBinaryOpKind.COMPOSITION -> CjTokens.COMPOSITION
}

private fun CfirComparisonOp.toCjToken(): CjSingleValueToken = when (this) {
    CfirComparisonOp.LT -> CjTokens.LT
    CfirComparisonOp.GT -> CjTokens.GT
    CfirComparisonOp.LE -> CjTokens.LTEQ
    CfirComparisonOp.GE -> CjTokens.GTEQ
    CfirComparisonOp.EQ -> CjTokens.EQEQ
    CfirComparisonOp.NE -> CjTokens.EXCLEQ
}

/** 为字面量实参创建 stub 子树；字符串按字面片段与转义片段逐段重建。 */
private fun createAnnotationLiteralStub(parent: StubElement<*>, literal: CfirLiteralExpression) {
    if (literal.kind == CfirLiteralKind.STRING) {
        val string = CangJiePlaceHolderStubImpl<CjStringTemplateExpression>(parent, CjStubElementTypes.STRING_TEMPLATE)
        // 字符串按字面片段与转义片段建子节点，不能将带转义的整串伪装成 LITERAL entry。
        val pending = StringBuilder()
        fun flushLiteral() {
            if (pending.isEmpty()) return
            CangJiePlaceHolderWithTextStubImpl<CjLiteralStringTemplateEntry>(
                string, CjStubElementTypes.LITERAL_STRING_TEMPLATE_ENTRY, pending.toString(),
            )
            pending.setLength(0)
        }
        for (char in literal.value as String) {
            val escape = when (char) {
                '\\' -> "\\\\"
                '"' -> "\\\""
                '$' -> "\\$"
                '\n' -> "\\n"
                '\r' -> "\\r"
                '\t' -> "\\t"
                else -> if (char.code < 32) "\\u{${char.code.toString(16)}}" else null
            }
            if (escape == null) pending.append(char) else {
                flushLiteral()
                CangJiePlaceHolderWithTextStubImpl<CjEscapeStringTemplateEntry>(
                    string, CjStubElementTypes.ESCAPE_STRING_TEMPLATE_ENTRY, escape,
                )
            }
        }
        flushLiteral()
        return
    }
    val kind = when (literal.kind) {
        CfirLiteralKind.INT -> ConstantValueKind.INTEGER_CONSTANT
        CfirLiteralKind.FLOAT -> ConstantValueKind.FLOAT_CONSTANT
        CfirLiteralKind.BOOLEAN -> ConstantValueKind.BOOLEAN_CONSTANT
        CfirLiteralKind.RUNE -> ConstantValueKind.RUNE_CONSTANT
        CfirLiteralKind.BYTE -> ConstantValueKind.CHARACTER_BYTE_CONSTANT
        CfirLiteralKind.UNIT -> ConstantValueKind.UNIT_CONSTANT
        CfirLiteralKind.STRING -> error("String literal must use a template subtree")
    }
    val token = when (literal.kind) {
        CfirLiteralKind.UNIT -> "()"
        CfirLiteralKind.RUNE -> "r'\\u{${(literal.value as Int).toString(16)}}'"
        CfirLiteralKind.BYTE -> "b'\\u{${(literal.value as java.math.BigInteger).toString(16)}}'"
        else -> literal.value.toString()
    }
    CangJieConstantExpressionStubImpl(parent, kindToConstantElementType(kind), kind, StringRef.fromString(token))
}
