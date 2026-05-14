package org.cangnova.cangjie.analysis.api.types

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion

/**
 * 公开类型替换器。
 *
 * 表示 “把类型表达式中的类型参数替换为另一组具体类型” 这一能力的稳定公开接口。
 *
 * 典型来源:
 * - 由 `buildSubstitutor` 等构造器在 session 内手动构造;
 * - 在调用解析的结果上拿到(例如把方法签名中的形参类型按实参代换)。
 *
 * 该接口对齐 Kotlin Analysis API 的 `KaSubstitutor`,只暴露 substitute 行为本身,
 * 不向公开层泄露底层映射表、缓存键或后端实现细节。
 *
 * 替换示例:
 * ```
 * substitutor = { T -> Int64, S -> String }
 * substitute(HashMap<T, S>, substitutor) = HashMap<Int64, String>
 * ```
 */
interface CaSubstitutor : CaLifetimeOwner {
    /**
     * 对 [type] 执行类型替换。
     *
     * 若当前替换器对该类型未产生任何替换,则直接返回原 [type] 对象,避免无意义的对象重建。
     */
    fun substitute(type: CaType): CaType = withValidityAssertion {
        substituteOrNull(type) ?: type
    }

    /**
     * 对 [type] 执行类型替换。
     *
     * 与 [substitute] 的区别:若当前替换器没有产生任何替换,这里返回 `null`,
     * 方便调用方区分 “未替换” 与 “被替换成同结构类型” 两种情况。
     */
    fun substituteOrNull(type: CaType): CaType?

    /**
     * 空替换器。
     *
     * 持有一个空的类型参数映射,不会对任何输入类型产生改变。
     * 用作默认值/占位,显式表达 “当前没有可应用的类型参数映射” 这一公开语义。
     */
    class Empty(
        override val token: CaLifetimeToken,
    ) : CaSubstitutor {
        override fun substitute(type: CaType): CaType = withValidityAssertion { type }

        override fun substituteOrNull(type: CaType): CaType? = withValidityAssertion { null }
    }
}
