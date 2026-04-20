package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.types.CaType

interface CaTypeRelationChecker : CaLifetimeOwner {
    fun CaType.isSubTypeOf(superType: CaType): Boolean

    fun CaType.semanticallyEquals(other: CaType): Boolean
}
