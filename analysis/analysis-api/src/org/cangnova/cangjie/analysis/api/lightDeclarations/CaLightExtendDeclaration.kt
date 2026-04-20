package org.cangnova.cangjie.analysis.api.lightDeclarations

import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

interface CaLightExtendDeclaration : CaLightDeclaration {
    val extendId: String

    val targetClassId: ClassId?

    val extendedType: CaType

    val typeParameters: List<Name>

    val superTypes: List<CaType>

    val members: List<CaLightDeclaration>
}
