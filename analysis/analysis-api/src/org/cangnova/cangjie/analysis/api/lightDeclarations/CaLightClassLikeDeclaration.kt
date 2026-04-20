package org.cangnova.cangjie.analysis.api.lightDeclarations

import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

interface CaLightClassLikeDeclaration : CaLightDeclaration {
    val classId: ClassId?

    val typeParameters: List<Name>

    val superTypes: List<CaType>

    val members: List<CaLightDeclaration>
}
