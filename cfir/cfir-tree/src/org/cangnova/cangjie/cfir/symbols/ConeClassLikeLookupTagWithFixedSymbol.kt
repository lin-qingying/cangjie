package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.cfir.types.ConeClassLikeLookupTag
import org.cangnova.cangjie.name.ClassId

class ConeClassLikeLookupTagWithFixedSymbol(
    override val classId: ClassId,
    val symbol: CfirClassifierSymbolWithClassId<*>
) : ConeClassLikeLookupTag() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConeClassLikeLookupTagWithFixedSymbol

        if (symbol != other.symbol) return false

        return true
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }
}
