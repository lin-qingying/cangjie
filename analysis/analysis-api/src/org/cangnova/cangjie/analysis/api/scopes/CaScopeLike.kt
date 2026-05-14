package org.cangnova.cangjie.analysis.api.scopes

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.name.Name

/**
 * 作用域的轻量"名称视图"。
 *
 * - 仅暴露名字层面的可达性,不要求物化具体符号;
 * - 名称集合允许出现假阳性(可能名字),但不允许漏报;
 *   即"返回 `true` 不代表一定存在,返回 `false` 一定不存在"。
 *
 * 对齐 Kotlin Analysis API 的 `KaScopeLike`。
 */
@CaExperimentalApi
@SubclassOptInRequired(CaImplementationDetail::class)
interface CaScopeLike : CaLifetimeOwner {
    /**
     * 返回作用域中"可能存在"的所有顶级声明名称集合(callable + classifier 合并)。
     *
     * 结果可能含假阳性,即名字实际并不对应该作用域内的声明。
     */
    fun getAllPossibleNames(): Set<Name> = withValidityAssertion {
        getPossibleCallableNames() + getPossibleClassifierNames()
    }

    /**
     * 返回作用域中"可能存在"的顶级 callable 名称集合。
     *
     * 结果可能含假阳性。
     */
    fun getPossibleCallableNames(): Set<Name>

    /**
     * 返回作用域中"可能存在"的顶级 classifier 名称集合。
     *
     * 结果可能含假阳性。
     */
    fun getPossibleClassifierNames(): Set<Name>

    /**
     * 判断当前作用域是否"可能"包含名为 [name] 的声明。
     *
     * 由于 [getPossibleCallableNames] 与 [getPossibleClassifierNames] 允许假阳性,
     * 返回 `true` 时仍可能找不到对应声明;但返回 `false` 时则一定不存在。
     */
    fun mayContainName(name: Name): Boolean = withValidityAssertion {
        name in getPossibleCallableNames() || name in getPossibleClassifierNames()
    }
}
