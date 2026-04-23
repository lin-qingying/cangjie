package org.cangnova.cangjie.analysis.api.cfir.utils

import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.utils.errors.requireIsInstance
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol

internal val CaSymbol.cfirSymbol: CfirBasedSymbol<*>
    get() {
        requireIsInstance<CaCfirSymbol<*>>(this)
        return this.cfirSymbol
    }