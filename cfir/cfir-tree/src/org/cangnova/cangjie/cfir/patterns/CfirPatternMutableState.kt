package org.cangnova.cangjie.cfir.patterns

import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.patterns.impl.CfirBindingPatternImpl
import org.cangnova.cangjie.cfir.patterns.impl.CfirTypePatternImpl
import org.cangnova.cangjie.cfir.patterns.impl.CfirVarOrEnumPatternImpl
import org.cangnova.cangjie.cfir.types.CfirTypeRef

/**
 * 模式树在 resolve 阶段会原地替换类型引用和绑定变量。
 *
 * speculative body resolve 需要像 Kotlin FIR 的候选事务一样，把试跑期间写入
 * pattern 节点的可变字段恢复到进入候选前的状态；否则下一次候选分析或最终提交
 * 会丢失原始 pattern binding。
 */
interface CfirPatternMutableState {
    fun restore()

    companion object {
        fun capture(pattern: CfirPattern): CfirPatternMutableState? = when (pattern) {
            is CfirBindingPattern -> {
                val impl = pattern as? CfirBindingPatternImpl
                    ?: error("CfirBindingPattern must be backed by generated implementation")
                CfirBindingPatternMutableState(
                    pattern = impl,
                    typeRef = impl.typeRef,
                    bindingVariable = impl.bindingVariable,
                    nestedPattern = impl.nestedPattern,
                )
            }

            is CfirTypePattern -> {
                val impl = pattern as? CfirTypePatternImpl
                    ?: error("CfirTypePattern must be backed by generated implementation")
                CfirTypePatternMutableState(
                    pattern = impl,
                    typeRef = impl.typeRef,
                    bindingVariable = impl.bindingVariable,
                )
            }

            is CfirVarOrEnumPattern -> {
                val impl = pattern as? CfirVarOrEnumPatternImpl
                    ?: error("CfirVarOrEnumPattern must be backed by generated implementation")
                CfirVarOrEnumPatternMutableState(
                    pattern = impl,
                    bindingVariable = impl.bindingVariable,
                )
            }

            else -> null
        }
    }
}

private class CfirBindingPatternMutableState(
    private val pattern: CfirBindingPatternImpl,
    private val typeRef: CfirTypeRef?,
    private val bindingVariable: CfirPatternBindingVariable?,
    private val nestedPattern: CfirPattern?,
) : CfirPatternMutableState {
    override fun restore() {
        pattern.typeRef = typeRef
        pattern.bindingVariable = bindingVariable
        pattern.nestedPattern = nestedPattern
    }
}

private class CfirTypePatternMutableState(
    private val pattern: CfirTypePatternImpl,
    private val typeRef: CfirTypeRef,
    private val bindingVariable: CfirPatternBindingVariable?,
) : CfirPatternMutableState {
    override fun restore() {
        pattern.typeRef = typeRef
        pattern.bindingVariable = bindingVariable
    }
}

private class CfirVarOrEnumPatternMutableState(
    private val pattern: CfirVarOrEnumPatternImpl,
    private val bindingVariable: CfirPatternBindingVariable?,
) : CfirPatternMutableState {
    override fun restore() {
        pattern.bindingVariable = bindingVariable
    }
}
