package org.cangnova.cangjie.analysis.decompiler.stub

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.impl.*

/**
 * `.cjo` 类型 stub 构建器。
 *
 * 显式类型不能只停留在 declaration 的布尔位，必须物化出完整 TYPE_REFERENCE 子树。
 */
internal class TypeCjoStubBuilder {
    fun createDeclaredTypeReferenceStub(
        parent: StubElement<*>,
        typeRef: CfirTypeRef,
    ) {
        if (typeRef is CfirImplicitTypeRef) return
        createTypeReferenceStub(parent, typeRef)
    }

    fun createCallableReturnTypeReferenceStub(
        parent: StubElement<*>,
        typeRef: CfirTypeRef,
    ) {
        if (typeRef is CfirImplicitTypeRef) return
        when (typeRef) {
            is CfirFunctionTypeRef -> createTypeReferenceStub(parent, typeRef.returnTypeRef)
            is CfirResolvedTypeRef -> when (val coneType = typeRef.coneType) {
                is ConeFunctionType -> createTypeReferenceStub(parent, coneType.returnType)
                else -> createTypeReferenceStub(parent, typeRef)
            }
            else -> createTypeReferenceStub(parent, typeRef)
        }
    }

    fun createCallableParameterListStub(
        parent: StubElement<*>,
        valueParameters: List<CfirValueParameter>,
        createEmptyList: Boolean = false,
        includeParameterModifierList: Boolean = false,
    ) {
        if (valueParameters.isEmpty() && !createEmptyList) return
        val parameterListStub = CangJiePlaceHolderStubImpl<CjParameterList>(parent, CjStubElementTypes.VALUE_PARAMETER_LIST)
        valueParameters.forEach { valueParameter ->
            val parameterStub = createParameterStub(
                parent = parameterListStub,
                name = valueParameter.name.asString(),
                includeModifiers = includeParameterModifierList,
            )
            createDeclaredTypeReferenceStub(parameterStub, valueParameter.returnTypeRef)
        }
    }

    fun createEmptyParameterListStub(parent: StubElement<*>) {
        CangJiePlaceHolderStubImpl<CjParameterList>(parent, CjStubElementTypes.VALUE_PARAMETER_LIST)
    }

    fun createSimpleParameterListStub(
        parent: StubElement<*>,
        parameterNames: List<String>,
        includeAnnotations: Boolean = true,
    ) {
        val parameterListStub = CangJiePlaceHolderStubImpl<CjParameterList>(parent, CjStubElementTypes.VALUE_PARAMETER_LIST)
        parameterNames.forEach { parameterName ->
            createParameterStub(parent = parameterListStub, name = parameterName, includeAnnotations = includeAnnotations)
        }
    }

    fun createTypeReferenceStub(
        parent: StubElement<*>,
        typeRef: CfirTypeRef,
    ) {
        when (typeRef) {
            is CfirResolvedTypeRef -> createTypeReferenceStub(parent, typeRef.coneType)
            is CfirBasicTypeRef -> createBasicTypeReferenceStub(parent, typeRef.name.asString())
            is CfirUserTypeRef -> createQualifiedUserTypeReferenceStubFromCfir(
                parent = parent,
                segments = typeRef.qualifier.mapIndexed { index, part -> part.name to (index == typeRef.qualifier.lastIndex) },
                typeArguments = typeRef.qualifier.lastOrNull()?.typeArguments.orEmpty(),
            )
            is CfirFunctionTypeRef -> createFunctionTypeReferenceStub(parent, typeRef.parameterTypeRefs, typeRef.returnTypeRef)
            is CfirTupleTypeRef -> createTupleTypeReferenceStubFromCfir(parent, typeRef.elementTypeRefs)
            is CfirOptionTypeRef -> createOptionTypeReferenceStub(parent, typeRef.componentTypeRef)
            is CfirVArrayTypeRef -> createVArrayTypeReferenceStub(parent, typeRef.elementTypeRef, typeRef.sizeLiteral)
            is CfirImplicitTypeRef -> Unit
            else -> createBasicTypeReferenceStub(parent, renderDecompiledTypeRef(typeRef))
        }
    }

    private fun createTypeReferenceStub(
        parent: StubElement<*>,
        coneType: ConeCangJieType,
    ) {
        when (coneType) {
            is ConePrimitiveType -> createBasicTypeReferenceStub(parent, coneType.kind.typeName)
            is ConeTypeParameterType -> createQualifiedUserTypeReferenceStub(
                parent = parent,
                segments = listOf(coneType.lookupTag.name to false),
                typeArguments = emptyList(),
            )
            is ConeClassifierType -> {
                val classId = coneType.lookupTag.classId
                createQualifiedUserTypeReferenceStub(
                    parent = parent,
                    segments = buildList {
                        classId.packageFqName.pathSegments().forEach { packageSegment ->
                            add(packageSegment to false)
                        }
                        add(classId.shortClassName to true)
                    },
                    typeArguments = coneType.typeArguments,
                )
            }
            is ConeFunctionType -> createFunctionTypeReferenceStub(parent, coneType.parameterTypes, coneType.returnType)
            is ConeTupleType -> createTupleTypeReferenceStub(parent, coneType.elementTypes)
            is ConeVArrayType -> createVArrayTypeReferenceStub(parent, coneType.elementType, coneType.size.toString())
            is ConeQuestType -> createBasicTypeReferenceStub(parent, "?")
            else -> createBasicTypeReferenceStub(parent, coneType.toString())
        }
    }

    private fun createBasicTypeReferenceStub(
        parent: StubElement<*>,
        basicTypeName: String,
    ) {
        val normalizedTypeName = normalizeRenderedTypeText(basicTypeName)
            .removePrefix("struct ")
            .replace("/", ".")
            .trim()
        if (normalizedTypeName !in BASIC_TYPE_NAMES) {
            createRenderedUserTypeReferenceStub(parent, normalizedTypeName)
            return
        }

        val typeReferenceStub = CangJiePlaceHolderStubImpl<CjTypeReference>(parent, CjStubElementTypes.TYPE_REFERENCE)
        CangJieBasicTypeStubImpl(typeReferenceStub, normalizedTypeName)
    }

    private fun createRenderedUserTypeReferenceStub(
        parent: StubElement<*>,
        renderedType: String,
    ) {
        val typeReferenceStub = CangJiePlaceHolderStubImpl<CjTypeReference>(parent, CjStubElementTypes.TYPE_REFERENCE)
        createRenderedUserTypeStub(typeReferenceStub, renderedType)
    }

    private fun createQualifiedUserTypeReferenceStub(
        parent: StubElement<*>,
        segments: List<Pair<Name, Boolean>>,
        typeArguments: List<ConeTypeProjection>,
    ) {
        if (segments.isEmpty()) return
        val typeReferenceStub = CangJiePlaceHolderStubImpl<CjTypeReference>(parent, CjStubElementTypes.TYPE_REFERENCE)
        createQualifiedUserTypeStub(typeReferenceStub, segments, typeArguments)
    }

    private fun createQualifiedUserTypeReferenceStubFromCfir(
        parent: StubElement<*>,
        segments: List<Pair<Name, Boolean>>,
        typeArguments: List<CfirTypeRef>,
    ) {
        if (segments.isEmpty()) return
        val typeReferenceStub = CangJiePlaceHolderStubImpl<CjTypeReference>(parent, CjStubElementTypes.TYPE_REFERENCE)
        createQualifiedUserTypeStubFromCfir(typeReferenceStub, segments, typeArguments)
    }

    private fun createQualifiedUserTypeStub(
        parent: StubElement<*>,
        segments: List<Pair<Name, Boolean>>,
        typeArguments: List<ConeTypeProjection>,
    ) {
        if (segments.isEmpty()) return
        val userTypeStub = CangJieUserTypeStubImpl(parent)
        if (segments.size > 1) {
            createQualifiedUserTypeStub(userTypeStub, segments.dropLast(1), emptyList())
        }
        val (segmentName, isClassRef) = segments.last()
        CangJieNameReferenceExpressionStubImpl(userTypeStub, StringRef.fromString(segmentName.asString()), isClassRef)
        if (typeArguments.isNotEmpty()) {
            val typeArgumentListStub = CangJiePlaceHolderStubImpl<CjTypeArgumentList>(userTypeStub, CjStubElementTypes.TYPE_ARGUMENT_LIST)
            typeArguments.forEach { typeArgument ->
                val projectionStub = CangJieTypeProjectionStubImpl(typeArgumentListStub, 0)
                when (typeArgument) {
                    is ConeCangJieType -> createTypeReferenceStub(projectionStub, typeArgument)
                }
            }
        }
    }

    private fun createQualifiedUserTypeStubFromCfir(
        parent: StubElement<*>,
        segments: List<Pair<Name, Boolean>>,
        typeArguments: List<CfirTypeRef>,
    ) {
        if (segments.isEmpty()) return
        val userTypeStub = CangJieUserTypeStubImpl(parent)
        if (segments.size > 1) {
            createQualifiedUserTypeStubFromCfir(userTypeStub, segments.dropLast(1), emptyList())
        }
        val (segmentName, isClassRef) = segments.last()
        CangJieNameReferenceExpressionStubImpl(userTypeStub, StringRef.fromString(segmentName.asString()), isClassRef)
        if (typeArguments.isNotEmpty()) {
            val typeArgumentListStub = CangJiePlaceHolderStubImpl<CjTypeArgumentList>(userTypeStub, CjStubElementTypes.TYPE_ARGUMENT_LIST)
            typeArguments.forEach { typeArgument ->
                val projectionStub = CangJieTypeProjectionStubImpl(typeArgumentListStub, 0)
                createTypeReferenceStub(projectionStub, typeArgument)
            }
        }
    }

    private fun createRenderedUserTypeStub(
        parent: StubElement<*>,
        renderedType: String,
    ) {
        val baseType = renderedType.substringBeforeTopLevelTypeArguments()
        val segments = baseType.split('.')
            .filter(String::isNotBlank)
            .map(Name::identifier)
        if (segments.isEmpty()) return
        createRenderedUserTypeStub(parent, segments, renderedType.topLevelTypeArguments())
    }

    private fun createRenderedUserTypeStub(
        parent: StubElement<*>,
        segments: List<Name>,
        typeArguments: List<String>,
    ) {
        val userTypeStub = CangJieUserTypeStubImpl(parent)
        if (segments.size > 1) {
            createRenderedUserTypeStub(userTypeStub, segments.dropLast(1), emptyList())
        }
        CangJieNameReferenceExpressionStubImpl(userTypeStub, StringRef.fromString(segments.last().asString()), true)
        if (typeArguments.isNotEmpty()) {
            val typeArgumentListStub = CangJiePlaceHolderStubImpl<CjTypeArgumentList>(userTypeStub, CjStubElementTypes.TYPE_ARGUMENT_LIST)
            typeArguments.forEach { typeArgument ->
                val projectionStub = CangJieTypeProjectionStubImpl(typeArgumentListStub, 0)
                createTypeReferenceStubFromRenderedText(projectionStub, typeArgument)
            }
        }
    }

    private fun createTypeReferenceStubFromRenderedText(
        parent: StubElement<*>,
        renderedType: String,
    ) {
        val normalizedType = normalizeRenderedTypeText(renderedType)
            .removePrefix("struct ")
            .replace("/", ".")
            .trim()
        if (normalizedType in BASIC_TYPE_NAMES) {
            createBasicTypeReferenceStub(parent, normalizedType)
        } else {
            createRenderedUserTypeReferenceStub(parent, normalizedType)
        }
    }

    private fun createFunctionTypeReferenceStub(
        parent: StubElement<*>,
        parameterTypes: List<CfirTypeRef>,
        returnType: CfirTypeRef,
    ) {
        val typeReferenceStub = CangJiePlaceHolderStubImpl<CjTypeReference>(parent, CjStubElementTypes.TYPE_REFERENCE)
        val functionTypeStub = CangJiePlaceHolderStubImpl<CjFunctionType>(typeReferenceStub, CjStubElementTypes.FUNCTION_TYPE)
        val parameterListStub = CangJiePlaceHolderStubImpl<CjParameterList>(functionTypeStub, CjStubElementTypes.VALUE_PARAMETER_LIST)
        parameterTypes.forEach { parameterType ->
            val parameterStub = CangJieParameterStubImpl(
                parent = parameterListStub,
                fqName = null,
                name = null,
                isMutable = false,
                hasLetOrVar = false,
                hasDefaultValue = false,
                isNamed = false,
                functionTypeParameterName = null,
            )
            createTypeReferenceStub(parameterStub, parameterType)
        }
        createTypeReferenceStub(functionTypeStub, returnType)
    }

    private fun createParameterStub(
        parent: StubElement<*>,
        name: String?,
        includeAnnotations: Boolean = true,
        includeModifiers: Boolean = false,
    ): CangJieParameterStubImpl {
        val parameterStub = CangJieParameterStubImpl(
            parent = parent,
            fqName = null,
            name = StringRef.fromString(name),
            isMutable = false,
            hasLetOrVar = false,
            hasDefaultValue = false,
            isNamed = false,
            functionTypeParameterName = null,
        )
        if (includeAnnotations) {
            CangJiePlaceHolderStubImpl<CjAnnotations>(parameterStub, CjStubElementTypes.ANNOTATIONS)
        }
        if (includeModifiers) {
            CangJieModifierListStubImpl(parameterStub, 0, CjStubElementTypes.MODIFIER_LIST)
        }
        return parameterStub
    }

    private fun createFunctionTypeReferenceStub(
        parent: StubElement<*>,
        parameterTypes: List<ConeCangJieType>,
        returnType: ConeCangJieType,
    ) {
        val typeReferenceStub = CangJiePlaceHolderStubImpl<CjTypeReference>(parent, CjStubElementTypes.TYPE_REFERENCE)
        val functionTypeStub = CangJiePlaceHolderStubImpl<CjFunctionType>(typeReferenceStub, CjStubElementTypes.FUNCTION_TYPE)
        val parameterListStub = CangJiePlaceHolderStubImpl<CjParameterList>(functionTypeStub, CjStubElementTypes.VALUE_PARAMETER_LIST)
        parameterTypes.forEach { parameterType ->
            val parameterStub = CangJieParameterStubImpl(
                parent = parameterListStub,
                fqName = null,
                name = null,
                isMutable = false,
                hasLetOrVar = false,
                hasDefaultValue = false,
                isNamed = false,
                functionTypeParameterName = null,
            )
            createTypeReferenceStub(parameterStub, parameterType)
        }
        createTypeReferenceStub(functionTypeStub, returnType)
    }

    private fun createTupleTypeReferenceStubFromCfir(
        parent: StubElement<*>,
        elementTypes: List<CfirTypeRef>,
    ) {
        val typeReferenceStub = CangJiePlaceHolderStubImpl<CjTypeReference>(parent, CjStubElementTypes.TYPE_REFERENCE)
        val tupleTypeStub = CangJiePlaceHolderStubImpl<CjTupleType>(typeReferenceStub, CjStubElementTypes.TUPLE_TYPE)
        elementTypes.forEach { elementType ->
            createTypeReferenceStub(tupleTypeStub, elementType)
        }
    }

    private fun createTupleTypeReferenceStub(
        parent: StubElement<*>,
        elementTypes: List<ConeCangJieType>,
    ) {
        val typeReferenceStub = CangJiePlaceHolderStubImpl<CjTypeReference>(parent, CjStubElementTypes.TYPE_REFERENCE)
        val tupleTypeStub = CangJiePlaceHolderStubImpl<CjTupleType>(typeReferenceStub, CjStubElementTypes.TUPLE_TYPE)
        elementTypes.forEach { elementType ->
            createTypeReferenceStub(tupleTypeStub, elementType)
        }
    }

    private fun createVArrayTypeReferenceStub(
        parent: StubElement<*>,
        elementType: CfirTypeRef,
        sizeLiteral: String,
    ) {
        val typeReferenceStub = CangJiePlaceHolderStubImpl<CjTypeReference>(parent, CjStubElementTypes.TYPE_REFERENCE)
        val varrayTypeStub = CangJiePlaceHolderStubImpl<CjVArrayType>(typeReferenceStub, CjStubElementTypes.VARRAY_TYPE)
        val typeArgumentListStub = CangJiePlaceHolderStubImpl<CjTypeArgumentList>(varrayTypeStub, CjStubElementTypes.TYPE_ARGUMENT_LIST)
        val projectionStub = CangJieTypeProjectionStubImpl(typeArgumentListStub, 0)
        createTypeReferenceStub(projectionStub, elementType)
        CangJieNameReferenceExpressionStubImpl(varrayTypeStub, StringRef.fromString(sizeLiteral), false)
    }

    private fun createVArrayTypeReferenceStub(
        parent: StubElement<*>,
        elementType: ConeCangJieType,
        sizeLiteral: String,
    ) {
        val typeReferenceStub = CangJiePlaceHolderStubImpl<CjTypeReference>(parent, CjStubElementTypes.TYPE_REFERENCE)
        val varrayTypeStub = CangJiePlaceHolderStubImpl<CjVArrayType>(typeReferenceStub, CjStubElementTypes.VARRAY_TYPE)
        val typeArgumentListStub = CangJiePlaceHolderStubImpl<CjTypeArgumentList>(varrayTypeStub, CjStubElementTypes.TYPE_ARGUMENT_LIST)
        val projectionStub = CangJieTypeProjectionStubImpl(typeArgumentListStub, 0)
        createTypeReferenceStub(projectionStub, elementType)
        CangJieNameReferenceExpressionStubImpl(varrayTypeStub, StringRef.fromString(sizeLiteral), false)
    }

    private fun createOptionTypeReferenceStub(
        parent: StubElement<*>,
        componentType: CfirTypeRef,
    ) {
        val typeReferenceStub = CangJiePlaceHolderStubImpl<CjTypeReference>(parent, CjStubElementTypes.TYPE_REFERENCE)
        val userTypeStub = CangJieUserTypeStubImpl(typeReferenceStub)
        CangJieNameReferenceExpressionStubImpl(userTypeStub, StringRef.fromString("Option"), true)
        val typeArgumentListStub = CangJiePlaceHolderStubImpl<CjTypeArgumentList>(userTypeStub, CjStubElementTypes.TYPE_ARGUMENT_LIST)
        val projectionStub = CangJieTypeProjectionStubImpl(typeArgumentListStub, 0)
        createTypeReferenceStub(projectionStub, componentType)
    }

    private fun String.substringBeforeTopLevelTypeArguments(): String {
        val index = indexOfTopLevelLt()
        return if (index < 0) this else substring(0, index).trim()
    }

    private fun String.topLevelTypeArguments(): List<String> {
        val ltIndex = indexOfTopLevelLt()
        if (ltIndex < 0 || !endsWith(">")) return emptyList()
        return substring(ltIndex + 1, lastIndex).splitTopLevelTypeArguments()
    }

    private fun String.indexOfTopLevelLt(): Int {
        var depth = 0
        forEachIndexed { index, char ->
            when (char) {
                '<' -> {
                    if (depth == 0) return index
                    depth++
                }
                '>' -> depth--
            }
        }
        return -1
    }

    private fun String.splitTopLevelTypeArguments(): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var start = 0
        forEachIndexed { index, char ->
            when (char) {
                '<' -> depth++
                '>' -> depth--
                ',' -> if (depth == 0) {
                    result += substring(start, index).trim()
                    start = index + 1
                }
            }
        }
        result += substring(start).trim()
        return result.filter(String::isNotBlank)
    }

    private companion object {
        val BASIC_TYPE_NAMES = setOf(
            "Bool",
            "IntNative",
            "Int8",
            "Int16",
            "Int32",
            "Int64",
            "UIntNative",
            "UInt8",
            "UInt16",
            "UInt32",
            "UInt64",
            "Float16",
            "Float32",
            "Float64",
            "Nothing",
            "Rune",
            "Unit",
        )
    }
}
