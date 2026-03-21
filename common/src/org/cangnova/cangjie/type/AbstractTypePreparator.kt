package org.cangnova.cangjie.type

import org.cangnova.cangjie.type.model.CangJieTypeMarker

abstract class AbstractTypePreparator {
    abstract fun prepareType(type: CangJieTypeMarker): CangJieTypeMarker

    object Default : AbstractTypePreparator() {
        override fun prepareType(type: CangJieTypeMarker): CangJieTypeMarker = type
    }
}
