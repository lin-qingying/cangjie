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
    /**
     * 为声明上的显式类型创建 type reference stub。
     *
     * 隐式类型引用表示源码中没有显式类型标注，因此不会在反编译 stub 中创建类型节点。
     */
    fun createDeclaredTypeReferenceStub(
        parent: StubElement<*>,
        typeRef: CfirTypeRef,
    ) {
        if (typeRef is CfirImplicitTypeRef) return
        createTypeReferenceStub(parent, typeRef)
    }

    /**
     * 为 callable 返回类型创建 type reference stub。
     *
     * 函数类型引用会提取其返回类型作为 callable 的声明返回类型，普通类型则直接创建引用。
     */
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

    /**
     * 为 callable 值参数创建参数列表 stub。
     *
     * 可按需创建空参数列表，并可为 primary constructor 参数补齐 modifier list，
     * 以保持与源码 PSI header 层级一致。
     */
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

    /**
     * 创建空的值参数列表 stub。
     */
    fun createEmptyParameterListStub(parent: StubElement<*>) {
        CangJiePlaceHolderStubImpl<CjParameterList>(parent, CjStubElementTypes.VALUE_PARAMETER_LIST)
    }

    /**
     * 根据参数名列表创建简单参数列表 stub。
     *
     * 该入口用于 setter 等反编译合成参数，不携带类型引用。
     */
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

    /**
     * 根据 CFIR 类型引用创建完整 type reference stub。
     *
     * 该入口覆盖 resolved/basic/user/function/tuple/option/varray/implicit 等 CFIR 类型形态。
     */
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

    /**
     * 根据 Cone 类型创建完整 type reference stub。
     *
     * Cone 类型来自已解析类型系统，可能携带 classifier、函数类型、元组、VArray 或原始类型等结构。
     */
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

    /**
     * 创建基础类型引用 stub。
     *
     * 如果输入文本不是仓颉内建基础类型名，则按已渲染用户类型处理，避免丢失复杂类型文本。
     */
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

    /**
     * 为已经渲染好的用户类型文本创建 type reference stub。
     */
    private fun createRenderedUserTypeReferenceStub(
        parent: StubElement<*>,
        renderedType: String,
    ) {
        val typeReferenceStub = CangJiePlaceHolderStubImpl<CjTypeReference>(parent, CjStubElementTypes.TYPE_REFERENCE)
        createRenderedUserTypeStub(typeReferenceStub, renderedType)
    }

    /**
     * 为 Cone classifier 或类型参数创建限定用户类型引用 stub。
     */
    private fun createQualifiedUserTypeReferenceStub(
        parent: StubElement<*>,
        segments: List<Pair<Name, Boolean>>,
        typeArguments: List<ConeTypeProjection>,
    ) {
        if (segments.isEmpty()) return
        val typeReferenceStub = CangJiePlaceHolderStubImpl<CjTypeReference>(parent, CjStubElementTypes.TYPE_REFERENCE)
        createQualifiedUserTypeStub(typeReferenceStub, segments, typeArguments)
    }

    /**
     * 为 CFIR user type 创建限定用户类型引用 stub。
     */
    private fun createQualifiedUserTypeReferenceStubFromCfir(
        parent: StubElement<*>,
        segments: List<Pair<Name, Boolean>>,
        typeArguments: List<CfirTypeRef>,
    ) {
        if (segments.isEmpty()) return
        val typeReferenceStub = CangJiePlaceHolderStubImpl<CjTypeReference>(parent, CjStubElementTypes.TYPE_REFERENCE)
        createQualifiedUserTypeStubFromCfir(typeReferenceStub, segments, typeArguments)
    }

    /**
     * 递归创建 Cone 类型参数驱动的 qualified user type stub。
     *
     * 每个路径段对应一个嵌套 user type stub，末尾段携带类型实参列表。
     */
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

    /**
     * 递归创建 CFIR 类型引用驱动的 qualified user type stub。
     */
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

    /**
     * 将已渲染类型文本拆分为路径段和顶层类型实参，并创建 user type stub。
     */
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

    /**
     * 根据已经拆分出的名称段和类型实参文本递归创建 user type stub。
     */
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

    /**
     * 根据已渲染类型文本创建 type reference stub。
     */
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

    /**
     * 根据 CFIR 函数类型引用创建函数类型 stub。
     */
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

    /**
     * 创建单个值参数 stub，并按需补齐注解列表和修饰符列表占位。
     */
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

    /**
     * 根据 Cone 函数类型创建函数类型 stub。
     */
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

    /**
     * 根据 CFIR tuple 类型引用创建元组类型 stub。
     */
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

    /**
     * 根据 Cone tuple 类型创建元组类型 stub。
     */
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

    /**
     * 根据 CFIR VArray 类型引用创建 VArray 类型 stub。
     */
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

    /**
     * 根据 Cone VArray 类型创建 VArray 类型 stub。
     */
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

    /**
     * 将 CFIR option 类型反编译为 `Option<T>` 用户类型 stub。
     */
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

    /**
     * 返回去掉顶层类型实参后的类型主体文本。
     */
    private fun String.substringBeforeTopLevelTypeArguments(): String {
        val index = indexOfTopLevelLt()
        return if (index < 0) this else substring(0, index).trim()
    }

    /**
     * 解析当前类型文本最外层的类型实参文本列表。
     */
    private fun String.topLevelTypeArguments(): List<String> {
        val ltIndex = indexOfTopLevelLt()
        if (ltIndex < 0 || !endsWith(">")) return emptyList()
        return substring(ltIndex + 1, lastIndex).splitTopLevelTypeArguments()
    }

    /**
     * 查找当前字符串中顶层 `<` 的位置。
     */
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

    /**
     * 按顶层逗号拆分类型实参文本，忽略嵌套泛型内部的逗号。
     */
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
        /**
         * 反编译 stub 可以直接映射为 basic type stub 的仓颉基础类型名集合。
         */
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
