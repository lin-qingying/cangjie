package org.cangnova.cangjie.cfir.resolve.match.exhaustive.inria

import org.cangnova.cangjie.cfir.resolve.match.CfirConstructor
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeTupleType

/**
 * Maranget usefulness 算法生成的反例 witness。
 *
 * @property patterns 从内向外逐步构造的缺失模式片段。
 */
class Witness(val patterns: MutableList<CfirMatchPattern> = mutableListOf()) {
    /** witness 调试文本。 */
    override fun toString(): String = patterns.toString()

    /** 创建 witness 深拷贝。 */
    fun clone(): Witness = Witness(patterns.toMutableList())

    /**
     * 为构造器 payload 推入通配子模式，然后应用构造器。
     */
    fun pushWildConstructor(constructor: CfirConstructor, type: ConeCangJieType): Witness {
        constructor.subTypes(type).forEach { subType -> patterns.add(CfirMatchPattern.wild(subType)) }
        return applyConstructor(constructor, type)
    }

    /**
     * 将栈顶 payload 模式组合成一个构造器模式。
     */
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

/**
 * Maranget usefulness 计算结果。
 */
sealed class Usefulness {
    /**
     * useful 且携带 witness。
     *
     * @property witnesses 缺失模式 witness 列表。
     */
    class UsefulWithWitness(val witnesses: List<Witness>) : Usefulness() {
        /** witness 构造工具。 */
        companion object {
            /** 包含单个空 witness 的 useful 结果。 */
            val Empty: UsefulWithWitness
                get() = UsefulWithWitness(listOf(Witness()))
        }
    }

    /** useful 但不携带 witness。 */
    data object Useful : Usefulness()

    /** 不 useful。 */
    data object Useless : Usefulness()

    /** 当前结果是否 useful。 */
    val isUseful: Boolean
        get() = this !== Useless
}
