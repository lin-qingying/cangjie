package org.cangnova.cangjie.analysis.api.types

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken

class CaTypeProjection(
    public val type: CaType?,
    override val token: CaLifetimeToken,
) : CaLifetimeOwner {

}
