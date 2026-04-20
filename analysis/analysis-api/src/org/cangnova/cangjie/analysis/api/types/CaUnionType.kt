package org.cangnova.cangjie.analysis.api.types

interface CaUnionType : CaType {
    val alternatives: List<CaType>
}
