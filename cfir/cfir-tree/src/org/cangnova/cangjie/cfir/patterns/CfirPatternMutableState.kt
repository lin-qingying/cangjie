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
    /**
     * 恢复捕获时记录的 pattern 可变字段。
     */
    fun restore()

    /**
     * pattern 可变状态捕获工厂。
     */
    companion object {
        /**
         * 捕获指定 pattern 当前可恢复状态。
         */
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

/**
 * binding pattern 的可恢复状态。
 */
private class CfirBindingPatternMutableState(
    /**
     * 被恢复的 binding pattern 实现节点。
     */
    private val pattern: CfirBindingPatternImpl,
    /**
     * 捕获时的类型引用。
     */
    private val typeRef: CfirTypeRef?,
    /**
     * 捕获时的绑定变量。
     */
    private val bindingVariable: CfirPatternBindingVariable?,
    /**
     * 捕获时的嵌套 pattern。
     */
    private val nestedPattern: CfirPattern?,
) : CfirPatternMutableState {
    /**
     * 恢复 binding pattern 的类型引用、绑定变量和嵌套 pattern。
     */
    override fun restore() {
        pattern.typeRef = typeRef
        pattern.bindingVariable = bindingVariable
        pattern.nestedPattern = nestedPattern
    }
}

/**
 * type pattern 的可恢复状态。
 */
private class CfirTypePatternMutableState(
    /**
     * 被恢复的 type pattern 实现节点。
     */
    private val pattern: CfirTypePatternImpl,
    /**
     * 捕获时的类型引用。
     */
    private val typeRef: CfirTypeRef,
    /**
     * 捕获时的绑定变量。
     */
    private val bindingVariable: CfirPatternBindingVariable?,
) : CfirPatternMutableState {
    /**
     * 恢复 type pattern 的类型引用和绑定变量。
     */
    override fun restore() {
        pattern.typeRef = typeRef
        pattern.bindingVariable = bindingVariable
    }
}

/**
 * var-or-enum pattern 的可恢复状态。
 */
private class CfirVarOrEnumPatternMutableState(
    /**
     * 被恢复的 var-or-enum pattern 实现节点。
     */
    private val pattern: CfirVarOrEnumPatternImpl,
    /**
     * 捕获时的绑定变量。
     */
    private val bindingVariable: CfirPatternBindingVariable?,
) : CfirPatternMutableState {
    /**
     * 恢复 var-or-enum pattern 的绑定变量。
     */
    override fun restore() {
        pattern.bindingVariable = bindingVariable
    }
}
