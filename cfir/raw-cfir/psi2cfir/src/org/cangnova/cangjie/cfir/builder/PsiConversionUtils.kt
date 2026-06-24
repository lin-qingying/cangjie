package org.cangnova.cangjie.cfir.builder

import org.cangnova.cangjie.cfir.CfirQualifierPart
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.builder.buildQualifierPart
import org.cangnova.cangjie.cfir.types.builder.buildBasicTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildFunctionTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildOptionTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildTupleTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildUserTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildVArrayTypeRef
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*

/**
 * PSI 特有的类型转换工具（对齐 Kotlin 的 PsiConversionUtils.kt）。
 *
 * 将 CjTypeReference PSI 节点转换为未解析的 CfirTypeRef。
 * 仅被 PsiRawCfirBuilder 使用。
 */

/**
 * 将 PSI 类型引用转换为未解析的 CFIR 类型引用。
 *
 * @receiver 待转换的 PSI 类型引用；为 null 时生成 implicit type ref。
 * @param toSource PSI 元素到 CFIR source element 的映射函数。
 * @return raw 阶段未解析的 [CfirTypeRef]。
 */
internal fun CjTypeReference?.toCfirOrImplicitTypeRef(
    toSource: (com.intellij.psi.PsiElement) -> AbstractCjSourceElement,
): CfirTypeRef {
    if (this == null) return buildImplicitTypeRef {}
    val typeElement = typeElement
        ?: return buildImplicitTypeRef {}
    return typeElement.toCfirTypeRef(this, toSource)
}

/** 按具体 PSI 类型元素分派到对应的 CFIR type ref 构建函数。 */
private fun CjTypeElement.toCfirTypeRef(
    ref: CjTypeReference,
    toSource: (com.intellij.psi.PsiElement) -> AbstractCjSourceElement,
): CfirTypeRef = when (this) {
    is CjBasicType -> toCfirBasicTypeRef(ref, toSource)
    is CjUserType -> toCfirUserTypeRef(ref, toSource)
    is CjFunctionType -> toCfirFunctionTypeRef(ref, toSource)
    is CjOptionType -> toCfirOptionTypeRef(ref, toSource)
    is CjTupleType -> toCfirTupleTypeRef(ref, toSource)
    is CjVArrayType -> toCfirVArrayTypeRef(ref, toSource)
    is CjThisType -> toCfirThisTypeRef(ref, toSource)
    else -> buildErrorTypeRef {
        source = ref.toCjSourceElementOrNull(toSource)
        diagnostic =  ConeSimpleDiagnostic("Unsupported type element: ${javaClass.simpleName}")
    }
}

/** 将可选类型 PSI 转换为 [CfirTypeRef]，缺少内部类型时生成 error type ref。 */
private fun CjOptionType.toCfirOptionTypeRef(
    ref: CjTypeReference,
    toSource: (com.intellij.psi.PsiElement) -> AbstractCjSourceElement,
): CfirTypeRef {
    val innerType = getInnerType()
        ?: return buildErrorTypeRef {
            source = ref.toCjSourceElementOrNull(toSource)
            diagnostic = ConeSimpleDiagnostic("Malformed option type: missing component type")
        }

    return buildOptionTypeRef {
        source = ref.toCjSourceElement(toSource)
        componentTypeRef = innerType.toCfirTypeRef(ref, toSource)
    }
}

/** 将基础类型 PSI 转换为 [CfirBasicTypeRef]。 */
private fun CjBasicType.toCfirBasicTypeRef(
    ref: CjTypeReference,
    toSource: (com.intellij.psi.PsiElement) -> AbstractCjSourceElement,
): CfirBasicTypeRef {
    return buildBasicTypeRef {
        source = ref.toCjSourceElementOrNull(toSource)
        name = Name.identifier(getName())
    }
}

/** 将用户类型 PSI 转换为 [CfirUserTypeRef]，保留限定名与类型实参链。 */
private fun CjUserType.toCfirUserTypeRef(
    ref: CjTypeReference,
    toSource: (com.intellij.psi.PsiElement) -> AbstractCjSourceElement,
): CfirUserTypeRef {
    val qualifier = buildQualifierFromUserType(this, toSource)
    return buildUserTypeRef {
        source = ref.toCjSourceElement(toSource)
        this.qualifier += qualifier
    }
}

/** 将 `This` 类型 PSI 转换为普通 user type ref 形式。 */
private fun CjThisType.toCfirThisTypeRef(
    ref: CjTypeReference,
    toSource: (com.intellij.psi.PsiElement) -> AbstractCjSourceElement,
): CfirUserTypeRef {
    return buildUserTypeRef {
        source = ref.toCjSourceElement(toSource)
        qualifier += buildQualifierPart {
            source = toSource(this@toCfirThisTypeRef) as? CjSourceElement
            name = Name.identifier("This")
        }
    }
}

/** 从最内层 [CjUserType] 反向收集限定名片段并构造 CFIR qualifier。 */
private fun buildQualifierFromUserType(
    userType: CjUserType,
    toSource: (com.intellij.psi.PsiElement) -> AbstractCjSourceElement,
): List<CfirQualifierPart> {
    val segments = mutableListOf<CfirQualifierPart>()
    var current: CjUserType? = userType
    while (current != null) {
        val name = current.referencedName
        if (name != null) {
            segments.add(
                0,
                buildQualifierPart {
                    source = current.referenceExpression?.let(toSource) as? CjSourceElement
                    this.name = Name.identifier(name)
                    typeArguments += current.typeArguments.map { it.typeReference.toCfirOrImplicitTypeRef(toSource) }
                }
            )
        }
        current = current.qualifier
    }
    return segments
}

/** 将函数类型 PSI 转换为 [CfirFunctionTypeRef]。 */
private fun CjFunctionType.toCfirFunctionTypeRef(
    ref: CjTypeReference,
    toSource: (com.intellij.psi.PsiElement) -> AbstractCjSourceElement,
): CfirFunctionTypeRef {
    val parameterTypes = parameters.map { it.typeReference.toCfirOrImplicitTypeRef(toSource) }
    val returnType = returnTypeReference.toCfirOrImplicitTypeRef(toSource)
    return buildFunctionTypeRef {
        source = ref.toCjSourceElementOrNull(toSource)
        parameterTypeRefs += parameterTypes
        returnTypeRef = returnType
    }
}

/** 将 tuple 类型 PSI 转换为 [CfirTupleTypeRef]。 */
private fun CjTupleType.toCfirTupleTypeRef(
    ref: CjTypeReference,
    toSource: (com.intellij.psi.PsiElement) -> AbstractCjSourceElement,
): CfirTupleTypeRef {
    val elementTypes = typeArgumentsAsTypes.map { it.toCfirOrImplicitTypeRef(toSource) }
    return buildTupleTypeRef {
        source = ref.toCjSourceElementOrNull(toSource)
        elementTypeRefs += elementTypes
    }
}

/** 将 VArray 类型 PSI 转换为 [CfirTypeRef]，缺少元素类型或大小时生成 error type ref。 */
private fun CjVArrayType.toCfirVArrayTypeRef(
    ref: CjTypeReference,
    toSource: (com.intellij.psi.PsiElement) -> AbstractCjSourceElement,
): CfirTypeRef {
    val elementTypeReference = typeReference
        ?: return buildErrorTypeRef {
            source = ref.toCjSourceElementOrNull(toSource)
            diagnostic = ConeSimpleDiagnostic("Malformed VArray type: missing element type")
        }
    val elementTypeElement = elementTypeReference.typeElement
        ?: return buildErrorTypeRef {
            source = ref.toCjSourceElementOrNull(toSource)
            diagnostic =ConeSimpleDiagnostic("Malformed VArray type: missing element type")
        }
    val sizeLiteral = literal?.text
        ?: return buildErrorTypeRef {
            source = ref.toCjSourceElementOrNull(toSource)
            diagnostic = ConeSimpleDiagnostic("Malformed VArray type: missing size literal")
        }
    return buildVArrayTypeRef {
        source = ref.toCjSourceElementOrNull(toSource)
        elementTypeRef = elementTypeElement.toCfirTypeRef(elementTypeReference, toSource)
        this.sizeLiteral = sizeLiteral
    }
}

/** 尝试把当前类型引用映射为 [CjSourceElement]，失败时返回 null。 */
private fun CjTypeReference.toCjSourceElementOrNull(
    toSource: (com.intellij.psi.PsiElement) -> AbstractCjSourceElement,
): CjSourceElement? = toSource(this) as? CjSourceElement

/** 把当前类型引用映射为 [CjSourceElement]，无法映射时说明 raw builder source 管线损坏。 */
private fun CjTypeReference.toCjSourceElement(
    toSource: (com.intellij.psi.PsiElement) -> AbstractCjSourceElement,
): CjSourceElement =
    requireNotNull(toCjSourceElementOrNull(toSource)) { "Expected CjSourceElement for type reference: $text" }
