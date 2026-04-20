package org.cangnova.cangjie.analysis.api.evaluation

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner

sealed interface CaCompileTimeValue : CaLifetimeOwner {
    val renderedText: String
}
