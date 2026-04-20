package org.cangnova.cangjie.analysis.api.types

import org.cangnova.cangjie.analysis.api.CaSession

interface CaTypePointer<out T : CaType> {
    fun restoreType(session: CaSession): T?
}
