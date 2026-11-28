package org.cangnova.cangjie.cfir.resolve.substitution

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.resolve.withCombinedAttributesFrom
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.typeContext

/** 声明类型参数的替换以 symbol 身份为键；空映射和未许可的恒等映射复用 Empty。 */
fun substitutorByMap(
    substitution: Map<CfirTypeParameterSymbol, ConeCangJieType>,
    useSiteSession: CfirSession,
    allowIdenticalSubstitution: Boolean = false,
): ConeSubstitutor = ConeSubstitutorByMap.create(substitution, useSiteSession, allowIdenticalSubstitution)

class ConeSubstitutorByMap private constructor(
    val substitution: Map<CfirTypeParameterSymbol, ConeCangJieType>,
    private val useSiteSession: CfirSession,
) : AbstractConeSubstitutor(useSiteSession.typeContext) {
    companion object {
        fun create(
            substitution: Map<CfirTypeParameterSymbol, ConeCangJieType>,
            useSiteSession: CfirSession,
            allowIdenticalSubstitution: Boolean,
        ): ConeSubstitutor {
            if (substitution.isEmpty()) return Empty
            if (!allowIdenticalSubstitution && substitution.all { (parameter, argument) ->
                    (argument as? ConeTypeParameterType)?.lookupTag?.typeParameterSymbol == parameter
                }) return Empty
            return ConeSubstitutorByMap(substitution, useSiteSession)
        }
    }

    private val hashCode: Int by lazy(LazyThreadSafetyMode.PUBLICATION) { substitution.hashCode() }

    override fun substituteType(type: ConeCangJieType): ConeCangJieType? {
        if (type !is ConeTypeParameterType) return null
        val replacement = substitution[type.lookupTag.typeParameterSymbol] ?: return null
        return replacement.withCombinedAttributesFrom(type)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeSubstitutorByMap) return false
        return hashCode == other.hashCode && substitution == other.substitution && useSiteSession == other.useSiteSession
    }

    override fun hashCode(): Int = hashCode

    override fun toString(): String = substitution.entries.joinToString(prefix = "{", postfix = "}", separator = " | ") { (parameter, type) ->
        "${parameter.name} -> $type"
    }
}
