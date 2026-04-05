package org.cangnova.cangjie.analysis.api.types.pointers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 类型指针。
 */
interface CaTypePointer<out T : CaType> {
    fun restoreType(session: CaSession): T?
}
