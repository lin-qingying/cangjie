package org.cangnova.cangjie.analysis.api.types

interface CaTupleType : CaType {
    val elementTypes: List<CaType>
}
