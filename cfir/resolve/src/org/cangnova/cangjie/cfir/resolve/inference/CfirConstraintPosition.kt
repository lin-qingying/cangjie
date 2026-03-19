package org.cangnova.cangjie.cfir.resolve.inference

/**
 * 约束来源位置，用于记录约束产生的上下文。
 * 它主要服务于诊断信息，帮助定位推断失败的原因。
 * 对齐 K2 `ConstraintPosition`，但做了简化。
 */
sealed class CfirConstraintPosition {

    /** 来自函数参数位置的约束。 */
    data class ArgumentPosition(val index: Int) : CfirConstraintPosition() {
        override fun toString(): String = "argument[$index]"
    }

    /** 来自期望类型的约束。 */
    data object ExpectedType : CfirConstraintPosition() {
        override fun toString(): String = "expected type"
    }

    /** 来自函数返回类型的约束。 */
    data object ReturnType : CfirConstraintPosition() {
        override fun toString(): String = "return type"
    }

    /** 来自类型参数上界的约束。 */
    data object UpperBound : CfirConstraintPosition() {
        override fun toString(): String = "upper bound"
    }

    /** 来自类型变量固定阶段的约束。 */
    data class FixVariable(val variableName: String) : CfirConstraintPosition() {
        override fun toString(): String = "fix variable[$variableName]"
    }
}

