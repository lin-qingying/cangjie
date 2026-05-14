package org.cangnova.cangjie.analysis.api.types

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaSession

/**
 * 跨 session 的 [CaType] 稳定指针。
 *
 * [CaType] 自身受所属 [CaSession] 生命周期约束,不能跨 session 持有;
 * 当需要把对一个类型的引用穿越 analyze 块或缓存在 IDE 长期数据结构里时,
 * 应通过 [CaType.createPointer] 拿到 [CaTypePointer],再在目标 session 内 restore。
 *
 * - 指针本身可安全保存、传递;
 * - 是否能成功 restore 取决于目标 session 的分析范围,可能返回 `null`(例如声明已被删除);
 * - 推荐通过 `CaSession.restore` 入口使用,而非直接调用 [restore]。
 *
 * 对齐 Kotlin Analysis API 的 `KaTypePointer`。
 *
 * @param T restore 后期望得到的 [CaType] 具体子类型,允许协变。
 */
interface CaTypePointer<out T : CaType> {
    /**
     * 在目标 [session] 内还原指针指向的 [CaType] 实例。
     *
     * - 返回的实例不要求与原对象 `===` 相同,只要求语义等价;
     * - 若指针因 session 范围、源码变更等原因无法再 restore,返回 `null`;
     * - 该方法是实现细节,普通使用方应调用 [CaSession] 上的 restore 入口而非直接使用此方法。
     */
    @CaImplementationDetail
    fun restore(session: CaSession): T?
}
