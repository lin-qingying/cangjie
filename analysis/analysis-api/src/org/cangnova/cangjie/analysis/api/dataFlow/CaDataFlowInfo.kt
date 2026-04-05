package org.cangnova.cangjie.analysis.api.dataFlow

import org.cangnova.cangjie.analysis.api.evaluation.CaCompileTimeValue
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 表达式在当前 use-site session 下的数据流稳定性分类。
 *
 * 该分类不试图泄漏底层约束系统或 smart cast 内部状态，
 * 而是向上层稳定表达“这个表达式当前更像稳定值、可变值还是计算结果”。
 */
enum class CaDataFlowStability {
    STABLE_VALUE,
    MUTABLE_VALUE,
    COMPUTED_VALUE,
    UNKNOWN,
}

/**
 * Analysis API 暴露的数据流快照。
 *
 * 当前阶段聚焦表达式层的稳定性、编译期值和已知语义类型，
 * 作为引用分析、常量折叠、文档提示和工具层规划的统一入口。
 */
interface CaDataFlowInfo : CaLifetimeOwner {
    /**
     * 当前表达式的语义类型。
     */
    val expressionType: CaType?

    /**
     * 当前表达式可恢复的编译期值。
     */
    val compileTimeValue: CaCompileTimeValue?

    /**
     * 当前表达式是否表现为纯引用读取，而不是一次新的计算或调用。
     */
    val isPureReference: Boolean

    /**
     * 当前表达式在当前数据流上下文中的稳定性分类。
     */
    val stability: CaDataFlowStability
}
