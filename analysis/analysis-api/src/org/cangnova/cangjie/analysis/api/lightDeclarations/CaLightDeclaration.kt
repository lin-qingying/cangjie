package org.cangnova.cangjie.analysis.api.lightDeclarations

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

interface CaLightDeclaration : CaLifetimeOwner {
    val kind: CaLightDeclarationKind

    val name: String?

    val module: CaModule?

    val annotations: List<CaAnnotation>

    val origin: CaLightDeclarationOrigin
}
