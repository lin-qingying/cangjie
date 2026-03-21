package org.cangnova.cangjie.type

import org.cangnova.cangjie.type.model.CangJieTypeMarker

abstract class AbstractTypeRefiner {
    abstract fun refineType(type: CangJieTypeMarker): CangJieTypeMarker

    object Default : AbstractTypeRefiner() {
        override fun refineType(type: CangJieTypeMarker): CangJieTypeMarker = type
    }
}
