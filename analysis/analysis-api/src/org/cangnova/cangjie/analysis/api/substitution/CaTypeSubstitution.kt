package org.cangnova.cangjie.analysis.api.substitution

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.Name

interface CaTypeSubstitutor : CaLifetimeOwner {
    val substitutions: Map<Name, CaType>

    fun substitute(type: CaType): CaType
}
