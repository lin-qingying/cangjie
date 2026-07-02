package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.cfir.render.ConeTypeRendererForDebugInfo
import org.cangnova.cangjie.type.model.*
import org.cangnova.cangjie.types.AbstractTypeApproximator
import org.cangnova.cangjie.types.TypeApproximatorConfiguration

/**
 * 类型近似器（Type Approximator）
 *
 * 作用：
 * 在类型推断 / 泛型处理过程中，把“复杂类型”近似为：
 *  - 更宽的类型（super type，向上近似）
 *  - 更窄的类型（sub type，向下近似）
 *
 * 典型场景：
 *  - captured type → Any?
 *  - intersection type → 公共父类型
 *  - 字面量类型（ILT）→ 对应基础类型
 */
class ConeTypeApproximator(
    inferenceContext: ConeInferenceContext,
    languageVersionSettings: LanguageVersionSettings
) : AbstractTypeApproximator(inferenceContext, languageVersionSettings) {

    /**
     * 向“父类型”方向做近似（变得更宽）
     *
     * 示例：
     *   List<out Int>  →  List<Any?>
     */
    fun approximateToSuperType(
        type: ConeCangJieType,
        conf: TypeApproximatorConfiguration
    ): ConeCangJieType? {
        // 快速路径：如果确定不需要近似，直接返回 null（表示不变）
        if (type.fastPathSkipApproximation(conf)) return null

        // 调用父类实现（核心算法在 AbstractTypeApproximator）
        // caches = null 表示不使用缓存
        return super.approximateToSuperType(type, conf, caches = null)?.asCone()
    }

    /**
     * 向“子类型”方向做近似（变得更具体）
     */
    fun approximateToSubType(
        type: ConeCangJieType,
        conf: TypeApproximatorConfiguration
    ): ConeCangJieType? {
        if (type.fastPathSkipApproximation(conf)) return null

        return super.approximateToSubType(type, conf, caches = null)?.asCone()
    }

    /**
     * Debug 输出：把类型渲染成字符串
     */
    override fun CangJieTypeMarker.renderForDebugInfo(): String = buildString {
        ConeTypeRendererForDebugInfo(this, renderCapturedDetails = true)
            .render(this@renderForDebugInfo.asCone())
    }

    /**
     * 快速路径：判断是否可以“跳过近似”
     *
     * 目的：性能优化 + 避免不必要的语义变化
     */
    private fun ConeCangJieType.fastPathSkipApproximation(
        conf: TypeApproximatorConfiguration
    ): Boolean {

        /**
         * Case 1：简单类型直接跳过
         *
         * 条件：
         *  - 是类类型（不是 captured / intersection 等复杂类型）
         *  - 没有泛型参数
         *  - 不是 anonymous type
         *
         * 说明：
         * 这种类型已经是“稳定类型”，不需要近似
         */
        if (this is ConeClassLikeType && this.typeArguments.isEmpty() &&
            this.lookupTag.let { !it.isAnonymous() }
        ) {
            return true
        }

        /**
         * =========================
         * Case 2：K2 的 captured type 优化
         * =========================
         *
         * 背景：
         * 在 K2 中：
         *  - captured type 理论上在“完成推断后”不应该再近似
         *  - 但实际中，内部可能嵌套 ILT（Ideal Literal Type）
         *
         * 问题：
         * 深层泛型结构（例如嵌套泛型 + 自引用）会：
         *  - 触发递归近似
         *  - 命中深度限制（depth == 4）
         *  - 被错误近似成 out Any?
         *
         * 结果：
         *  - 类型被“误认为改变了”
         *  - 导致 false-positive
         *  - 性能变差（非常慢）
         *
         * 解决：
         * 加一个 fast-path，避免不必要的近似
         */

//        // 如果语言特性未开启 → 不做优化
//        if (!languageVersionSettings.supportsFeature(
//                LanguageFeature.AvoidApproximationOfRecursiveCapturedTypesWithNoReason
//            )
//        ) return false


        // 该 fast-path 只适用于纯 ILT 近似配置。公开声明、类型变量等配置还可能
        // 近似其他结构，不能因为当前类型树不含 ILT 就直接短路。
        if (conf !is TypeApproximatorConfiguration.AbstractILTApproximation) return false

        /**
         * 核心逻辑：
         *
         * 如果整个类型结构中：
         *   ❌ 不包含需要近似的类型（ILT / captured）
         *
         * 那么：
         *   ✅ 可以安全跳过近似
         */
        return !contains { mightNeedApproximation(it.asCone(), conf) }
    }

    /**
     * 判断某个类型”是否可能需要近似”
     */
    private fun mightNeedApproximation(
        type: ConeCangJieType,
        conf: TypeApproximatorConfiguration
    ): Boolean = when (type) {
        // ILT（Ideal Literal Type，比如整数常量类型）必须近似
        is ConeIdealLiteralType -> true
        // 公共类型系统同时把 primitive 形式的 IdealInt/IdealFloat 视为 ILT 构造器。
        is ConePrimitiveType -> type.kind.isIdeal
        // 仓颉无 CapturedType，其他类型默认不需要近似
        else -> false
    }
}
