package org.cangnova.cangjie.cfir.declarations.util

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.coneTypeSafe
import org.cangnova.cangjie.name.ClassId

val CfirTypeAlias.expandedConeType: ConeCangJieType? get() = expandedTypeRef.coneTypeSafe()

val CfirClassLikeDeclaration.classId: ClassId
    get() = symbol.classId

val CfirClass.superConeTypes: List<ConeClassLikeType>
    get() = superTypeRefs.mapNotNull { it.coneTypeSafe() }
