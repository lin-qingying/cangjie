package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.cfir.types.ConeClassLikeLookupTag
import org.cangnova.cangjie.name.ClassId

/** 固定 Kotlin 对位：真实 classifier 身份在类型创建时确定，后续 header 填充不改 tag.symbol。 */
class ConeClassLikeLookupTagWithFixedSymbol(
    override val classId: ClassId,
    val symbol: CfirClassLikeSymbol<*>
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

