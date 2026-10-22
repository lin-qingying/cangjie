package org.cangnova.cangjie.cfir.patterns

import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
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
                CfirBindingPatternMutableState(
                    pattern = pattern,
                    typeRef = pattern.typeRef,
                    bindingVariable = pattern.bindingVariable,
                    nestedPattern = pattern.nestedPattern,
                )
            }

            is CfirTypePattern -> {
                CfirTypePatternMutableState(
                    pattern = pattern,
                    typeRef = pattern.typeRef,
                    bindingVariable = pattern.bindingVariable,
                    matchingKind = pattern.matchingKind,
                )
            }

            is CfirVarOrEnumPattern -> {
                CfirVarOrEnumPatternMutableState(
                    pattern = pattern,
                    bindingVariable = pattern.bindingVariable,
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
    private val pattern: CfirBindingPattern,
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
        pattern.replaceTypeRef(typeRef)
        pattern.replaceBindingVariable(bindingVariable)
        pattern.replaceNestedPattern(nestedPattern)
    }
}

/**
 * type pattern 的可恢复状态。
 */
private class CfirTypePatternMutableState(
    /**
     * 被恢复的 type pattern 实现节点。
     */
    private val pattern: CfirTypePattern,
    /**
     * 捕获时的类型引用。
     */
    private val typeRef: CfirTypeRef,
    /**
     * 捕获时的绑定变量。
     */
    private val bindingVariable: CfirPatternBindingVariable?,
    /** 捕获时的类型模式判定形式。 */
    private val matchingKind: CfirTypePatternMatchingKind,
) : CfirPatternMutableState {
    /**
     * 恢复 type pattern 的类型引用、绑定变量和判定形式。
     */
    override fun restore() {
        pattern.replaceTypeRef(typeRef)
        pattern.replaceBindingVariable(bindingVariable)
        pattern.replaceMatchingKind(matchingKind)
    }
}

/**
 * var-or-enum pattern 的可恢复状态。
 */
private class CfirVarOrEnumPatternMutableState(
    /**
     * 被恢复的 var-or-enum pattern 实现节点。
     */
    private val pattern: CfirVarOrEnumPattern,
    /**
     * 捕获时的绑定变量。
     */
    private val bindingVariable: CfirPatternBindingVariable?,
) : CfirPatternMutableState {
    /**
     * 恢复 var-or-enum pattern 的绑定变量。
     */
    override fun restore() {
        pattern.replaceBindingVariable(bindingVariable)
    }
}
