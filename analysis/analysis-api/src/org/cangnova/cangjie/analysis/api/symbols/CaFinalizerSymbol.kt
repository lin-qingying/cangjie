package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.name.ClassId

abstract class CaFinalizerSymbol : CaFunctionSymbol() {
    abstract val containingClassId: ClassId?
}
