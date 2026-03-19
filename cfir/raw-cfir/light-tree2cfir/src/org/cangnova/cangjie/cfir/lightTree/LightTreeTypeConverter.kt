package org.cangnova.cangjie.cfir.lightTree

import com.intellij.lang.LighterASTNode
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.builder.*
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjNodeTypes

/**
 * LightTree 类型引用转换（对齐 PSI 版 PsiConversionUtils.kt）。
 *
 * 将 LightTree 中的类型节点转换为未解析的 [CfirTypeRef]。
 */

/**
 * 将 TYPE_REFERENCE 节点转换为 CfirTypeRef，若为 null 返回隐式类型。
 */
fun convertTypeReference(
    typeRefNode: LighterASTNode?,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    source: CharSequence,
    toSource: (LighterASTNode) -> AbstractCjSourceElement,
): CfirTypeRef {
    if (typeRefNode == null) return buildImplicitTypeRef()
    // TYPE_REFERENCE 内部包含一个具体的类型元素子节点
    var typeElement: LighterASTNode? = null
    tree.forEachChildren(typeRefNode) { child ->
        val tt = child.tokenType
        if (tt == CjNodeTypes.BASIC_TYPE || tt == CjNodeTypes.USER_TYPE
            || tt == CjNodeTypes.FUNCTION_TYPE || tt == CjNodeTypes.TUPLE_TYPE
            || tt == CjNodeTypes.VARRAY_TYPE || tt == CjNodeTypes.OPTIONAL_TYPE
            || tt == CjNodeTypes.THIS_TYPE || tt == CjNodeTypes.PARENTHESIZED_TYPE
        ) {
            typeElement = child
        }
    }
    val element = typeElement ?: return buildImplicitTypeRef()
    return convertTypeElement(element, typeRefNode, tree, source, toSource)
}

/**
 * 根据类型元素节点的 tokenType 分发转换。
 */
private fun convertTypeElement(
    typeElement: LighterASTNode,
    typeRefNode: LighterASTNode,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    source: CharSequence,
    toSource: (LighterASTNode) -> AbstractCjSourceElement,
): CfirTypeRef = when (typeElement.tokenType) {
    CjNodeTypes.BASIC_TYPE -> convertBasicType(typeElement, typeRefNode, source, toSource)
    CjNodeTypes.USER_TYPE -> convertUserType(typeElement, typeRefNode, tree, source, toSource)
    CjNodeTypes.FUNCTION_TYPE -> convertFunctionType(typeElement, typeRefNode, tree, source, toSource)
    CjNodeTypes.TUPLE_TYPE -> convertTupleType(typeElement, typeRefNode, tree, source, toSource)
    CjNodeTypes.VARRAY_TYPE -> convertVArrayType(typeElement, typeRefNode, tree, source, toSource)
    CjNodeTypes.PARENTHESIZED_TYPE -> {
        // 括号内嵌套一个 TYPE_REFERENCE
        val innerTypeRef = tree.findChildByType(typeElement, CjNodeTypes.TYPE_REFERENCE)
        convertTypeReference(innerTypeRef, tree, source, toSource)
    }
    else -> buildErrorTypeRef {
        this.source = toSource(typeRefNode) as? CjSourceElement
        reason = "Unsupported type element: ${typeElement.tokenType}"
    }
}

// ===== 各类型转换 =====

/**
 * BASIC_TYPE → CfirBasicTypeRef
 *
 * BASIC_TYPE 子节点为一个基本类型关键字 Token（如 Int32, Bool 等）。
 */
private fun convertBasicType(
    typeElement: LighterASTNode,
    typeRefNode: LighterASTNode,
    source: CharSequence,
    toSource: (LighterASTNode) -> AbstractCjSourceElement,
): CfirTypeRef {
    val text = getNodeText(typeElement, source)
    return buildBasicTypeRef {
        this.source = toSource(typeRefNode) as? CjSourceElement
        name = Name.identifier(text)
    }
}

/**
 * USER_TYPE → CfirUserTypeRef
 *
 * USER_TYPE 结构:
 * - 嵌套 USER_TYPE（限定符，如 `a.b.Foo` → USER_TYPE(qualifier=USER_TYPE(a), ref=b) + outer USER_TYPE(ref=Foo)）
 * - REFERENCE_EXPRESSION（名称）
 * - TYPE_ARGUMENT_LIST → TYPE_PROJECTION → TYPE_REFERENCE（类型参数）
 */
private fun convertUserType(
    typeElement: LighterASTNode,
    typeRefNode: LighterASTNode,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    source: CharSequence,
    toSource: (LighterASTNode) -> AbstractCjSourceElement,
): CfirTypeRef {
    val qualifier = buildQualifierFromUserType(typeElement, tree, source)
    val typeArguments = collectTypeArguments(typeElement, tree, source, toSource)
    return buildUserTypeRef {
        this.source = toSource(typeRefNode) as? CjSourceElement
        this.qualifier += qualifier
        this.typeArguments += typeArguments
    }
}

/**
 * 从 USER_TYPE 递归提取限定名。
 * USER_TYPE 可能嵌套 USER_TYPE 作为 qualifier。
 */
private fun buildQualifierFromUserType(
    userTypeNode: LighterASTNode,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    source: CharSequence,
): List<Name> {
    val segments = mutableListOf<Name>()

    // 递归处理嵌套的 qualifier
    val nestedUserType = tree.findChildByType(userTypeNode, CjNodeTypes.USER_TYPE)
    if (nestedUserType != null) {
        segments.addAll(buildQualifierFromUserType(nestedUserType, tree, source))
    }

    // 提取当前节点的 REFERENCE_EXPRESSION
    val refExpr = tree.findChildByType(userTypeNode, CjNodeTypes.REFERENCE_EXPRESSION)
    if (refExpr != null) {
        val name = getNodeText(refExpr, source)
        if (name.isNotEmpty()) {
            segments.add(Name.identifier(name))
        }
    }

    return segments
}

/**
 * 收集 TYPE_ARGUMENT_LIST 中的类型参数。
 */
private fun collectTypeArguments(
    typeElement: LighterASTNode,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    source: CharSequence,
    toSource: (LighterASTNode) -> AbstractCjSourceElement,
): List<CfirTypeRef> {
    val typeArgList = tree.findChildByType(typeElement, CjNodeTypes.TYPE_ARGUMENT_LIST) ?: return emptyList()
    val projections = tree.getChildrenByType(typeArgList, CjNodeTypes.TYPE_PROJECTION)
    return projections.map { projection ->
        val typeRef = tree.findChildByType(projection, CjNodeTypes.TYPE_REFERENCE)
        convertTypeReference(typeRef, tree, source, toSource)
    }
}

/**
 * FUNCTION_TYPE → CfirFunctionTypeRef
 *
 * FUNCTION_TYPE 结构:
 * - VALUE_PARAMETER_LIST → VALUE_PARAMETER → TYPE_REFERENCE（参数类型）
 * - ARROW
 * - TYPE_REFERENCE（返回类型）
 */
private fun convertFunctionType(
    typeElement: LighterASTNode,
    typeRefNode: LighterASTNode,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    source: CharSequence,
    toSource: (LighterASTNode) -> AbstractCjSourceElement,
): CfirTypeRef {
    val parameterTypes = mutableListOf<CfirTypeRef>()
    var returnType: CfirTypeRef = buildImplicitTypeRef()

    val paramList = tree.findChildByType(typeElement, CjNodeTypes.VALUE_PARAMETER_LIST)
    if (paramList != null) {
        val params = tree.getChildrenByType(paramList, CjNodeTypes.VALUE_PARAMETER)
        for (param in params) {
            val typeRef = tree.findChildByType(param, CjNodeTypes.TYPE_REFERENCE)
            parameterTypes.add(convertTypeReference(typeRef, tree, source, toSource))
        }
    }

    // 返回类型是 ARROW 之后的最后一个 TYPE_REFERENCE
    var afterArrow = false
    tree.forEachChildren(typeElement) { child ->
        if (child.tokenType == CjTokens.ARROW) {
            afterArrow = true
        } else if (afterArrow && child.tokenType == CjNodeTypes.TYPE_REFERENCE) {
            returnType = convertTypeReference(child, tree, source, toSource)
        }
    }

    return buildFunctionTypeRef {
        this.source = toSource(typeRefNode) as? CjSourceElement
        parameterTypeRefs += parameterTypes
        returnTypeRef = returnType
    }
}

/**
 * TUPLE_TYPE → CfirTupleTypeRef
 *
 * TUPLE_TYPE 子节点包含多个 TYPE_REFERENCE。
 */
private fun convertTupleType(
    typeElement: LighterASTNode,
    typeRefNode: LighterASTNode,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    source: CharSequence,
    toSource: (LighterASTNode) -> AbstractCjSourceElement,
): CfirTypeRef {
    val elementTypes = tree.getChildrenByType(typeElement, CjNodeTypes.TYPE_REFERENCE).map {
        convertTypeReference(it, tree, source, toSource)
    }
    return buildTupleTypeRef {
        this.source = toSource(typeRefNode) as? CjSourceElement
        elementTypeRefs += elementTypes
    }
}

/**
 * VARRAY_TYPE → CfirVArrayTypeRef
 *
 * VARRAY_TYPE 结构:
 * - TYPE_ARGUMENT_LIST → TYPE_PROJECTION → TYPE_REFERENCE（元素类型）
 * - INTEGER_LITERAL（大小，如 $4）
 */
private fun convertVArrayType(
    typeElement: LighterASTNode,
    typeRefNode: LighterASTNode,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    source: CharSequence,
    toSource: (LighterASTNode) -> AbstractCjSourceElement,
): CfirTypeRef {
    // 从 TYPE_ARGUMENT_LIST → TYPE_PROJECTION → TYPE_REFERENCE 提取元素类型
    val typeArgList = tree.findChildByType(typeElement, CjNodeTypes.TYPE_ARGUMENT_LIST)
    val firstProjection = typeArgList?.let { tree.findChildByType(it, CjNodeTypes.TYPE_PROJECTION) }
    val innerTypeRef = firstProjection?.let { tree.findChildByType(it, CjNodeTypes.TYPE_REFERENCE) }

    if (innerTypeRef == null) {
        return buildErrorTypeRef {
            this.source = toSource(typeRefNode) as? CjSourceElement
            reason = "Malformed VArray type: missing element type"
        }
    }
    val elementType = convertTypeReference(innerTypeRef, tree, source, toSource)

    // 查找大小字面量（INTEGER_LITERAL token，如 $4）
    val sizeLiteralNode = tree.findChildByType(typeElement, CjTokens.INTEGER_LITERAL)
    val sizeLiteral = if (sizeLiteralNode != null) getNodeText(sizeLiteralNode, source) else null
    if (sizeLiteral == null) {
        return buildErrorTypeRef {
            this.source = toSource(typeRefNode) as? CjSourceElement
            reason = "Malformed VArray type: missing size literal"
        }
    }

    return buildVArrayTypeRef {
        this.source = toSource(typeRefNode) as? CjSourceElement
        elementTypeRef = elementType
        this.sizeLiteral = sizeLiteral
    }
}
