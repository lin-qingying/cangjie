package org.cangnova.cangjie.analysis.api.types

interface CaIntersectionType : CaType {
    val conjuncts: List<CaType>
}
