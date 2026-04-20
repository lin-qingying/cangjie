package org.cangnova.cangjie.analysis.api.types

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotated
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner

interface CaType : CaLifetimeOwner, CaAnnotated {
    val presentation: String

    val abbreviation: CaUsualClassType?

    fun createPointer(): CaTypePointer<CaType>
}
