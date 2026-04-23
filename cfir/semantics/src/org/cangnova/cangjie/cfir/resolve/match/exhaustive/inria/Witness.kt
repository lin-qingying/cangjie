package org.cangnova.cangjie.cfir.resolve.match.exhaustive.inria

import org.cangnova.cangjie.cfir.resolve.match.CfirConstructor
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeTupleType

class Witness(val patterns: MutableList<CfirMatchPattern> = mutableListOf()) {
    override fun toString(): String = patterns.toString()

    fun clone(): Witness = Witness(patterns.toMutableList())

    fun pushWildConstructor(constructor: CfirConstructor, type: ConeCangJieType): Witness {
        constructor.subTypes(type).forEach { subType -> patterns.add(CfirMatchPattern.wild(subType)) }
        return applyConstructor(constructor, type)
    }

    fun applyConstructor(constructor: CfirConstructor, type: ConeCangJieType): Witness {
        val arity = constructor.arity(type)
        val len = patterns.size
        val oldPatterns = patterns.subList(len - arity, len)
        val pats = oldPatterns.reversed().toList()
        oldPatterns.clear()

        val kind = when {
            type is ConeTupleType -> CfirMatchPatternKind.Tuple(pats)
            type is ConeEnumType && constructor is CfirConstructor.Enum ->
                CfirMatchPatternKind.Enum(type.classId, constructor.entryName, pats)
            constructor is CfirConstructor.ConstantValue ->
                CfirMatchPatternKind.Const(constructor.value)
            else -> CfirMatchPatternKind.Wild
        }

        patterns += CfirMatchPattern(type, kind, null)
        return this
    }
}

sealed class Usefulness {
    class UsefulWithWitness(val witnesses: List<Witness>) : Usefulness() {
        companion object {
            val Empty: UsefulWithWitness
                get() = UsefulWithWitness(listOf(Witness()))
        }
    }

    data object Useful : Usefulness()
    data object Useless : Usefulness()

    val isUseful: Boolean
        get() = this !== Useless
}
