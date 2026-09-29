package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.render.ConeTypeRenderer
import org.cangnova.cangjie.cfir.types.*

/**
 * raw CFIR dump 阶段必须按 type-ref 结构渲染，
 * 不能把未 resolve 的类型引用强行降级成 coneType 读取。
 */
internal fun renderTypeRefForDebug(typeRef: CfirTypeRef?, typeRenderer: ConeTypeRenderer): String {
    val builder = typeRenderer.builder
    val initialLength = builder.length
    return try {
        appendTypeRefForDebug(typeRef, typeRenderer, builder)
        builder.substring(initialLength)
    } finally {
        builder.setLength(initialLength)
    }
}

/** 按 Kotlin FIR 的共享 builder 方式写入 type ref 与已解析 Cone 类型。 */
internal fun appendTypeRefForDebug(
    typeRef: CfirTypeRef?,
    typeRenderer: ConeTypeRenderer,
    builder: StringBuilder,
) {
    check(typeRenderer.builder === builder) {
        "Type-reference and Cone renderers must write to the same buffer."
    }
    when (typeRef) {
        null -> Unit
        is CfirErrorTypeRef -> builder.append("R|<ERROR:${typeRef.diagnostic.reason}>|")
        is CfirImplicitTypeRef -> builder.append("<implicit>")
        is CfirResolvedTypeRef -> {
            builder.append("R|")
            typeRenderer.render(typeRef.coneType)
            builder.append("|")
        }
        is CfirBasicTypeRef -> builder.append("R|${typeRef.name.asString()}|")
        is CfirUserTypeRef -> {
            builder.append("R|")
            typeRef.qualifier.forEachIndexed { index, qualifier ->
                if (index > 0) builder.append(".")
                builder.append(qualifier.name.asString())
                if (qualifier.typeArguments.isNotEmpty()) {
                    builder.append("<")
                    qualifier.typeArguments.forEachIndexed { argumentIndex, argument ->
                        if (argumentIndex > 0) builder.append(", ")
                        appendTypeRefForDebug(argument, typeRenderer, builder)
                    }
                    builder.append(">")
                }
            }
            builder.append("|")
        }
        is CfirFunctionTypeRef -> {
            builder.append("R|(")
            typeRef.parameterTypeRefs.forEachIndexed { index, parameterTypeRef ->
                if (index > 0) builder.append(", ")
                appendTypeRefForDebug(parameterTypeRef, typeRenderer, builder)
            }
            builder.append(") -> ")
            appendTypeRefForDebug(typeRef.returnTypeRef, typeRenderer, builder)
            builder.append("|")
        }
        is CfirOptionTypeRef -> {
            builder.append("R|Option<")
            appendTypeRefForDebug(typeRef.componentTypeRef, typeRenderer, builder)
            builder.append(">|")
        }
        is CfirTupleTypeRef -> {
            builder.append("R|(")
            typeRef.elementTypeRefs.forEachIndexed { index, elementTypeRef ->
                if (index > 0) builder.append(", ")
                appendTypeRefForDebug(elementTypeRef, typeRenderer, builder)
            }
            builder.append(")|")
        }
        is CfirVArrayTypeRef -> {
            builder.append("R|VArray<")
            appendTypeRefForDebug(typeRef.elementTypeRef, typeRenderer, builder)
            builder.append(", ${typeRef.sizeLiteral}>|")
        }
    }
}
