package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

class CfirPrimitiveTypeSymbol(
    override val classId: ClassId,
    val kind: PrimitiveTypeKind,
) : CfirClassLikeSymbol<CfirPrimitiveTypeDeclaration>(classId) {
    override val name: Name
        get() = if (isBound) cfir.name else super.name

    override fun toString(): String =
        if (isBound) "CfirPrimitiveTypeSymbol(${cfir.name})" else "CfirPrimitiveTypeSymbol(${kind.typeName})"
}
