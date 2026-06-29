package org.cangnova.cangjie.analysis.api.renderer.types

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 类型近似化策略。
 *
 * 把内部类型(如反向投影、不可表达类型)替换成"在源码中可写"的近似类型,
 * 用于在 IDE 显示时屏蔽内部实现细节。
 *
 * 对齐 Kotlin Analysis API 的 `KaRendererTypeApproximator`。
 */
fun interface CaRendererTypeApproximator {
    /** 返回 [type] 经近似化后的类型, 同一类型可原样返回。 */
    fun approximateType(analysisSession: CaSession, type: CaType): CaType

    /**
     * 预设: 不做任何近似化, 直接返回原类型。
     *
     * 适用于内部诊断/debug 等需要看见真实类型的场景。
     */
    object NO_APPROXIMATION : CaRendererTypeApproximator {
        /**
         * 直接返回输入类型，不执行任何近似化。
         */
        override fun approximateType(analysisSession: CaSession, type: CaType): CaType {
            return type
        }
    }
}
