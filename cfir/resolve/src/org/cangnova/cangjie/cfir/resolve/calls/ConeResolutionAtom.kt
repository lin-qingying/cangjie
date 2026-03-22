package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirResolutionAtom
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * 调用解析原子协议层的仓颉版本入口。
 *
 * 新架构下，真正用于参数映射与候选绑定的叶子原子已经收敛为 [CfirResolutionAtom]；
 * 这里保留旧文件路径，但内容改成更薄、更仓颉化的协议层，避免继续承载旧 Kotlin
 * postponed/PCLA 体系的实现细节。
 */
sealed class ConeResolutionAtom {
    abstract val expression: CfirExpression
}

class ConeSimpleLeafResolutionAtom(
    override val expression: CfirExpression,
) : ConeResolutionAtom()

class ConeAtomWithCandidate(
    override val expression: CfirExpression,
    val candidate: CfirCandidate,
) : ConeResolutionAtom()

sealed class ConePostponedResolvedAtom : ConeResolutionAtom(), PostponedResolvedAtomMarker {
    abstract override val inputTypes: Collection<ConeCangJieType>
    abstract override val outputType: ConeCangJieType?
    abstract override val expectedType: ConeCangJieType?
    override var analyzed: Boolean = false
}

class ConeResolutionAtomWithPostponedChild(
    override val expression: CfirExpression,
    val fallbackSubAtom: ConeResolutionAtom? = null,
) : ConeResolutionAtom() {
    var subAtom: ConeResolutionAtom? = null
        private set

    fun setPostponedSubAtom(atom: ConePostponedResolvedAtom) {
        require(subAtom == null) { "subAtom already initialized" }
        subAtom = atom
    }

    fun useFallbackSubAtom() {
        subAtom = fallbackSubAtom
    }
}

object ConeResolutionAtomFactory {
    fun create(expression: CfirExpression): ConeResolutionAtom {
        return ConeSimpleLeafResolutionAtom(expression)
    }

    fun createWithCandidate(expression: CfirExpression, candidate: CfirCandidate): ConeResolutionAtom {
        return ConeAtomWithCandidate(expression, candidate)
    }
}
