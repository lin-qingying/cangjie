package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.render.ConeTypeRenderer
import org.cangnova.cangjie.cfir.types.*

/**
 * raw CFIR dump 阶段必须按 type-ref 结构渲染，
 * 不能把未 resolve 的类型引用强行降级成 coneType 读取。
 */
internal fun renderTypeRefForDebug(typeRef: CfirTypeRef?, typeRenderer: ConeTypeRenderer): String {
    return when (typeRef) {
        null -> ""
        is CfirErrorTypeRef -> "R|<ERROR:${typeRef.diagnostic.reason}>|"

        is CfirImplicitTypeRef -> "<implicit>"
        is CfirResolvedTypeRef -> "R|${typeRenderer.render(typeRef.coneType)}|"
        is CfirBasicTypeRef -> "R|${typeRef.name.asString()}|"
        is CfirUserTypeRef -> buildString {
            append("R|")
            typeRef.qualifier.forEachIndexed { index, qualifier ->
                if (index > 0) append(".")
                append(qualifier.name.asString())
                if (qualifier.typeArguments.isNotEmpty()) {
                    append("<")
                    qualifier.typeArguments.forEachIndexed { argumentIndex, argument ->
                        if (argumentIndex > 0) append(", ")
                        append(renderTypeRefForDebug(argument, typeRenderer))
                    }
                    append(">")
                }
            }
            append("|")
        }
        is CfirFunctionTypeRef -> buildString {
            append("R|(")
            typeRef.parameterTypeRefs.forEachIndexed { index, parameterTypeRef ->
                if (index > 0) append(", ")
                append(renderTypeRefForDebug(parameterTypeRef, typeRenderer))
            }
            append(") -> ")
            append(renderTypeRefForDebug(typeRef.returnTypeRef, typeRenderer))
            append("|")
        }
        is CfirOptionTypeRef -> "R|Option<${renderTypeRefForDebug(typeRef.componentTypeRef, typeRenderer)}>|"
        is CfirTupleTypeRef -> buildString {
            append("R|(")
            typeRef.elementTypeRefs.forEachIndexed { index, elementTypeRef ->
                if (index > 0) append(", ")
                append(renderTypeRefForDebug(elementTypeRef, typeRenderer))
            }
            append(")|")
        }
        is CfirVArrayTypeRef ->
            "R|VArray<${renderTypeRefForDebug(typeRef.elementTypeRef, typeRenderer)}, ${typeRef.sizeLiteral}>|"
    }
}
