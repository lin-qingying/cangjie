package org.cangnova.cangjie.analysis.api.resolution

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol

/**
 * 调用解析结果。
 *
 * Analysis API 需要对外暴露“调用点最终看到了哪些候选、最终选择了哪个候选”，
 * 但不能把底层 CFIR 的候选对象直接泄漏到上层。
 *
 * 因此这里稳定公开两层信息：
 * 1. [successfulCall] 表示无错误的最终选中调用。
 * 2. [calls] 表示当前调用点可观察到的调用视图集合，允许包含带错误的已选候选。
 */
interface CaCallInfo : CaLifetimeOwner {
    /**
     * 最终无错误解析成功的调用。
     */
    val successfulCall: CaCall?

    /**
     * 当前调用点可观察到的调用视图集合。
     */
    val calls: List<CaCall>
}

val CaCallInfo.target: CaCallableSymbol?
    get() = successfulCall?.target

fun CaCallInfo.singleCallOrNull(): CaCall? = calls.singleOrNull()

fun CaCallInfo.successfulFunctionCallOrNull(): CaCall? =
    successfulCall?.takeIf { it.kind == CaCallKind.FUNCTION }
