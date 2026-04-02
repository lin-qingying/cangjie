package org.cangnova.cangjie.cfir.builder

import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildBasicTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildFunctionTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
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
 */
internal fun CjTypeReference?.toFirOrImplicitTypeRef(
    toSource: (com.intellij.psi.PsiElement) -> AbstractCjSourceElement,
): CfirTypeRef {
    if (this == null) return buildImplicitTypeRef()
    val typeElement = typeElement
        ?: return buildImplicitTypeRef()
    return typeElement.toFirTypeRef(this, toSource)
}

private fun CjTypeElement.toFirTypeRef(
    ref: CjTypeReference,
    toSource: (com.intellij.psi.PsiElement) -> AbstractCjSourceElement,
): CfirTypeRef = when (this) {
    is CjBasicType -> toFirBasicTypeRef(ref, toSource)
    is CjUserType -> toFirUserTypeRef(ref, toSource)
    is CjFunctionType -> toFirFunctionTypeRef(ref, toSource)
    is CjTupleType -> toFirTupleTypeRef(ref, toSource)
    is CjVArrayType -> toFirVArrayTypeRef(ref, toSource)
    else -> buildErrorTypeRef {
        source = ref.toCjSourceElementOrNull(toSource)
        diagnostic =  ConeSimpleDiagnostic("Unsupported type element: ${javaClass.simpleName}")
    }
}

private fun CjBasicType.toFirBasicTypeRef(
    ref: CjTypeReference,
    toSource: (com.intellij.psi.PsiElement) -> AbstractCjSourceElement,
): CfirBasicTypeRef {
    return buildBasicTypeRef {
        source = ref.toCjSourceElementOrNull(toSource)
        name = Name.identifier(getName())
    }
}

private fun CjUserType.toFirUserTypeRef(
    ref: CjTypeReference,
    toSource: (com.intellij.psi.PsiElement) -> AbstractCjSourceElement,
): CfirUserTypeRef {
    val qualifier = buildQualifierFromUserType(this)
    val typeArguments = typeArguments.map { it.typeReference.toFirOrImplicitTypeRef(toSource) }
    return buildUserTypeRef {
        source = ref.toCjSourceElement(toSource)
        this.qualifier += qualifier
        this.typeArguments += typeArguments
    }
}

private fun buildQualifierFromUserType(userType: CjUserType): List<Name> {
    val segments = mutableListOf<Name>()
    var current: CjUserType? = userType
    while (current != null) {
        val name = current.referencedName
        if (name != null) {
            segments.add(0, Name.identifier(name))
        }
        current = current.qualifier
    }
    return segments
}

private fun CjFunctionType.toFirFunctionTypeRef(
    ref: CjTypeReference,
    toSource: (com.intellij.psi.PsiElement) -> AbstractCjSourceElement,
): CfirFunctionTypeRef {
    val parameterTypes = parameters.map { it.typeReference.toFirOrImplicitTypeRef(toSource) }
    val returnType = returnTypeReference.toFirOrImplicitTypeRef(toSource)
    return buildFunctionTypeRef {
        source = ref.toCjSourceElementOrNull(toSource)
        parameterTypeRefs += parameterTypes
        returnTypeRef = returnType
    }
}

private fun CjTupleType.toFirTupleTypeRef(
    ref: CjTypeReference,
    toSource: (com.intellij.psi.PsiElement) -> AbstractCjSourceElement,
): CfirTupleTypeRef {
    val elementTypes = typeArgumentsAsTypes.map { it.toFirOrImplicitTypeRef(toSource) }
    return buildTupleTypeRef {
        source = ref.toCjSourceElementOrNull(toSource)
        elementTypeRefs += elementTypes
    }
}

private fun CjVArrayType.toFirVArrayTypeRef(
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
        elementTypeRef = elementTypeElement.toFirTypeRef(elementTypeReference, toSource)
        this.sizeLiteral = sizeLiteral
    }
}

private fun CjTypeReference.toCjSourceElementOrNull(
    toSource: (com.intellij.psi.PsiElement) -> AbstractCjSourceElement,
): CjSourceElement? = toSource(this) as? CjSourceElement

private fun CjTypeReference.toCjSourceElement(
    toSource: (com.intellij.psi.PsiElement) -> AbstractCjSourceElement,
): CjSourceElement =
    requireNotNull(toCjSourceElementOrNull(toSource)) { "Expected CjSourceElement for type reference: $text" }
