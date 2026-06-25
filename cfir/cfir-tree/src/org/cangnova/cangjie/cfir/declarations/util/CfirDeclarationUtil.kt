package org.cangnova.cangjie.cfir.declarations.util

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.coneTypeSafe
import org.cangnova.cangjie.name.ClassId

/**
 * typealias 展开类型的安全 cone 表示；未解析或错误类型引用返回 `null`。
 */
val CfirTypeAlias.expandedConeType: ConeCangJieType? get() = expandedTypeRef.coneTypeSafe()

/**
 * class-like 声明对应 symbol 的稳定 ClassId。
 */
val CfirClassLikeDeclaration.classId: ClassId
    get() = symbol.classId

/**
 * class 声明父类型引用中可安全读取的 class-like cone 类型列表。
 */
val CfirClass.superConeTypes: List<ConeClassLikeType>
    get() = superTypeRefs.mapNotNull { it.coneTypeSafe() }
