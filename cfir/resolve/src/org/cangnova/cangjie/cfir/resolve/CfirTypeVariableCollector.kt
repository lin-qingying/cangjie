package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.types.arrayElementType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType

import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.type

/**
 * 递归收集类型中出现的类型变量名称。
 *
 * 该工具用于诊断和候选比较等只需要名称级信息的路径；它会展开 class-like、函数、tuple、
 * varray、pointer、typealias、交叉/联合类型以及数组元素类型。
 */
internal fun ConeCangJieType.collectTypeVariableNames(result: MutableSet<String>) {
    when (this) {
        is ConeTypeParameterType -> result += lookupTag.name.asString()
        is ConeClassLikeType -> typeArguments.forEach { it.collectTypeVariableNames(result) }
        is ConeStructType -> typeArguments.forEach { it.collectTypeVariableNames(result) }
        is ConeEnumType -> typeArguments.forEach { it.collectTypeVariableNames(result) }
        is ConeFunctionType -> {
            parameterTypes.forEach { it.collectTypeVariableNames(result) }
            returnType.collectTypeVariableNames(result)
        }
        is ConeTupleType -> elementTypes.forEach { it.collectTypeVariableNames(result) }
        is ConeVArrayType -> elementType.collectTypeVariableNames(result)
        is ConePointerType -> pointeeType.collectTypeVariableNames(result)
        is ConeTypeAliasType -> {
            expandedType?.collectTypeVariableNames(result)
            typeArguments.forEach { it.collectTypeVariableNames(result) }
        }
        is ConeIntersectionType -> intersectedTypes.forEach { it.collectTypeVariableNames(result) }
        is ConeUnionType -> unionTypes.forEach { it.collectTypeVariableNames(result) }
        else -> arrayElementType?.collectTypeVariableNames(result)
    }
}

/** 从类型投影中继续收集底层类型的类型变量名称。 */
private fun ConeTypeProjection.collectTypeVariableNames(result: MutableSet<String>) {
    type.collectTypeVariableNames(result)
}
