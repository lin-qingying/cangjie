package org.cangnova.cangjie.cfir.render

import org.cangnova.cangjie.cfir.types.*

/**
 * 固定 native Ty::String 的完整现有 Cone 节点策略。递归、ID 与属性策略复用 Cone renderer；
 * 此入口只接收完成存储类型，内部推断/error 节点不伪装成 native 正常类型。
 */
class ConeTypeRendererForNativeTypeString : ConeTypeRenderer(ConeAttributeRenderer.None) {
    override fun renderType(type: ConeCangJieType) {
        when (type) {
            ConeInvalidType -> builder.append("Invalid")
            is ConeQuestType -> builder.append("UnknownType")
            is ConePrimitiveType -> builder.append(when (type.kind) {
                PrimitiveTypeKind.IDEAL_INT -> "Int"
                PrimitiveTypeKind.IDEAL_FLOAT -> "Float"
                else -> type.kind.typeName
            })
            is ConeIdealIntLiteralType -> builder.append("Int")
            is ConeIdealFloatLiteralType -> builder.append("Float")
            is ConeErrorType, is ConeStubType, is ConeTypeVariableType, is ConePlaceholderType ->
                error("Native stored type strings require a completed semantic type: ${type::class.simpleName}")
            else -> super.renderType(type)
        }
    }

    override fun renderSimpleType(type: ConeSimpleCangJieType) {
        val tag = type.getConstructor()
        check(tag is ConeClassifierLookupTag && tag !is ConeClassLikeLookupTag)
        builder.append("Generics-").append(tag.name.asString())
    }

    override fun renderClassLikeType(type: ConeClassLikeType) {
        if (type.isThisType) {
            builder.append("This")
            return
        }
        if (type.classId == StdlibClassIds.RawArray) {
            val layout = checkNotNull(type.rawArrayLayout)
            for (index in 0u until layout.dimensions) builder.append("Array<")
            renderType(layout.element)
            for (index in 0u until layout.dimensions) builder.append('>')
            return
        }
        builder.append(if (type.isInterface) "Interface-" else "Class-")
        idRenderer.renderClassId(type.classId)
        renderNativeTypeArguments(type.typeArguments)
    }

    override fun renderStructType(type: ConeStructType) {
        builder.append("Struct-")
        idRenderer.renderClassId(type.classId)
        renderNativeTypeArguments(type.typeArguments)
    }

    override fun renderEnumType(type: ConeEnumType) {
        builder.append("Enum-")
        idRenderer.renderClassId(type.classId)
        renderNativeTypeArguments(type.typeArguments)
        if (type.isRefEnum) builder.append("(asRef)")
    }

    override fun renderTypeAliasType(type: ConeTypeAliasType) {
        builder.append("TypeAlias-")
        idRenderer.renderClassId(type.classId)
        renderNativeTypeArguments(type.typeArguments)
    }

    /** 与 K renderer 同样把类型实参格式保留为策略自身的私有 helper。 */
    private fun renderNativeTypeArguments(arguments: List<ConeTypeProjection>) {
        if (arguments.isEmpty()) return
        builder.append('<')
        arguments.forEachIndexed { index, argument ->
            if (index > 0) builder.append(", ")
            renderType(argument.type)
        }
        builder.append('>')
    }

    override fun renderFunctionType(type: ConeFunctionType) {
        if (type.isCFunc) builder.append("CFunc<")
        builder.append('(')
        type.parameterTypes.forEachIndexed { index, parameter ->
            if (index > 0) builder.append(", ")
            renderType(parameter)
        }
        builder.append(") -> ")
        renderType(type.returnType)
        if (type.isCFunc) builder.append('>')
    }

    override fun renderTupleType(type: ConeTupleType) {
        builder.append("Tuple<")
        type.elementTypes.forEachIndexed { index, element ->
            if (index > 0) builder.append(", ")
            renderType(element)
        }
        builder.append('>')
    }

    override fun renderVArrayType(type: ConeVArrayType) {
        builder.append("VArray<")
        renderType(type.elementType)
        builder.append(", $").append(type.size).append('>')
    }

    override fun renderIntersectionType(type: ConeIntersectionType) {
        builder.append(type.intersectedTypes.map(::renderNativeTypeString).sorted().joinToString(" & "))
    }

    override fun renderUnionType(type: ConeUnionType) {
        builder.append(type.unionTypes.map(::renderNativeTypeString).sorted().joinToString(" | "))
    }
}

/** 每次渲染使用独立状态；不保存 provider、source 回调或可变全局 renderer。 */
fun renderNativeTypeString(type: ConeCangJieType): String {
    val buffer = StringBuilder()
    val renderer = ConeTypeRendererForNativeTypeString().apply {
        builder = buffer
        idRenderer = ConeShortIdRenderer().apply { builder = buffer }
    }
    renderer.render(type)
    return buffer.toString()
}
