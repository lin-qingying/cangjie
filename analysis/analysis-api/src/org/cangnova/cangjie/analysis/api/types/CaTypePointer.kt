package org.cangnova.cangjie.analysis.api.types

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaSession

interface CaTypePointer<out T : CaType> {
    /**
     * Returns the restored [CaType] (possibly a new type instance) if the pointer is still valid, or `null` otherwise.
     *
     * Do not use this function directly, as it is an implementation detail.
     * Use [CaSession.restore][org.jetbrains.kotlin.analysis.api.CaSession.restore] instead.
     */
    @CaImplementationDetail
    public fun restore(session: CaSession): T?
}
