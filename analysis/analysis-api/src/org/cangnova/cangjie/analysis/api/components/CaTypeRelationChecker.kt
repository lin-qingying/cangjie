package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 类型关系判定协议。
 *
 * 设计要点/职责:
 * - 暴露类型层面的子类型与语义等价判定,
 *   结果只反映稳定的类型系统关系,不涉及具体推断引擎细节。
 * - 与基于结构(`equals`)的比较区分:语义等价会展开 type alias、
 *   折叠等价类型形态后再比较。
 *
 * 对齐 Kotlin Analysis API 的 `KaTypeRelationChecker`。
 */
interface CaTypeRelationChecker : CaLifetimeOwner {
    /**
     * 判定当前类型是否为 [superType] 的子类型(非严格,任一类型都是自身的子类型)。
     */
    fun CaType.isSubTypeOf(superType: CaType): Boolean

    /**
     * 判定当前类型与 [other] 是否在语义上相等。
     */
    fun CaType.semanticallyEquals(other: CaType): Boolean
}
