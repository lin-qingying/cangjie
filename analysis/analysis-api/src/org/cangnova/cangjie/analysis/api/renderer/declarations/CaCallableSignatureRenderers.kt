package org.cangnova.cangjie.analysis.api.renderer.declarations

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFinalizerSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol

internal fun CaCallableSymbol.renderNameText(): String = when (this) {
    is CaConstructorSymbol -> "init"
    is CaFinalizerSymbol -> "~init"
    is CaNamedSymbol -> name.asString()
    else -> "<anonymous>"
}
