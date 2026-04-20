package org.cangnova.cangjie.analysis.api.types

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.name.Name

sealed interface CaClassTypeQualifier : CaLifetimeOwner {
    val name: Name
    val typeArguments: List<CaTypeProjection>
}
