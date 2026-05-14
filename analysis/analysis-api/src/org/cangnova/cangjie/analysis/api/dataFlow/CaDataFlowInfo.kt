package org.cangnova.cangjie.analysis.api.dataFlow

import org.cangnova.cangjie.analysis.api.evaluation.CaCompileTimeValue
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 表达式的数据流分析结果。
 *
 * - 把类型推断、常量折叠、纯引用判定、稳定性等数据流相关结果集中暴露;
 * - IDE 据此驱动 smart cast、补全 narrow、悬浮提示等功能;
 * - 受 [CaLifetimeOwner] 约束,只在 session 内有效。
 *
 * 对齐 Kotlin Analysis API 的 `KaDataFlowInfoProvider` 返回值。
 */
interface CaDataFlowInfo : CaLifetimeOwner {
    /** 表达式经过数据流分析后的精炼类型,无法确定时为 `null`。 */
    val expressionType: CaType?

    /** 表达式的常量求值结果;若非编译期常量则为 `null`。 */
    val compileTimeValue: CaCompileTimeValue?

    /**
     * 表达式是否为"纯引用",即不依赖副作用、可在 smart cast 等场景安全重复读取。
     */
    val isPureReference: Boolean

    /** 引用值的稳定性等级,见 [CaDataFlowStability]。 */
    val stability: CaDataFlowStability
}
