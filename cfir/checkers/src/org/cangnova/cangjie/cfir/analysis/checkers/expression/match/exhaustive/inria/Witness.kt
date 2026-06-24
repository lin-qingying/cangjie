package org.cangnova.cangjie.cfir.analysis.checkers.expression.match.exhaustive.inria

import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirConstructor
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.analysis.checkers.expression.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeTupleType

/**
 * Maranget usefulness 算法生成的反例 witness。
 *
 * [patterns] 表示一条未覆盖输入形状；算法在递归返回时逐步把子模式重新包回构造器模式。
 */
class Witness(val patterns: MutableList<CfirMatchPattern> = mutableListOf()) {
    /** 返回 witness 模式列表的调试文本。 */
    override fun toString(): String = patterns.toString()

    /** 深拷贝当前 witness 的模式列表，供分支递归独立演化。 */
    fun clone(): Witness = Witness(patterns.toMutableList())

    /**
     * 先为构造器子位置压入 wildcard，再把它们包回当前构造器。
     *
     * 用于某个构造器整体未出现时生成缺失模式。
     */
    fun pushWildConstructor(constructor: CfirConstructor, type: ConeCangJieType): Witness {
        constructor.subTypes(type).forEach { subType -> patterns.add(CfirMatchPattern.wild(subType)) }
        return applyConstructor(constructor, type)
    }

    /**
     * 将 witness 末尾的构造器子模式折叠为一个父模式。
     *
     * tuple、enum 和常量构造器会保留精确模式种类，其他构造器折叠为 wildcard。
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

/** Maranget usefulness 检查的三态结果。 */
sealed class Usefulness {
    /** 有用，并携带可用于非穷尽诊断的 witness 集合。 */
    class UsefulWithWitness(val witnesses: List<Witness>) : Usefulness() {
        companion object {
            /** 只包含空 witness 的默认有用结果。 */
            val Empty: UsefulWithWitness get() = UsefulWithWitness(listOf(Witness()))
        }
    }

    /** 有用但不需要携带 witness。 */
    data object Useful : Usefulness()
    /** 无用，表示当前模式被已有矩阵完全覆盖。 */
    data object Useless : Usefulness()

    /** 当前结果是否为有用模式。 */
    val isUseful: Boolean get() = this !== Useless
}
