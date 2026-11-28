package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.scopes.CfirStoredFunctionMember
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 原生返回相容性只消费完成的存储类型与上界；不在归并期间读取 symbol 或 alias。
 */
internal fun areStoredFunctionReturnTypesCompatible(
    parent: CfirStoredFunctionMember,
    child: CfirStoredFunctionMember,
    session: CfirSession,
): Boolean {
    if (child.returnType is ConeTypeParameterType && !AbstractTypeChecker.equalTypes(session.typeContext, parent.returnType, child.returnType)) {
        fun hasConcreteClass(type: ConeCangJieType): Boolean = when (type) {
            is ConeIntersectionType -> type.intersectedTypes.any(::hasConcreteClass)
            is ConeClassLikeType -> !type.isInterface
            else -> false
        }
        if (child.returnUpperBounds.none(::hasConcreteClass)) return false
    }
    return AbstractTypeChecker.isSubtypeOfWithoutImplicitBoxing(session.typeContext, child.returnType, parent.returnType)
}
