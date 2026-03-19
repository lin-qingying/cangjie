package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScopeImpl

/**
 * scope 塔中的单个数据元素。
 */
class CfirTowerDataElement(
    /** 该层级对应的 scope。 */
    val scope: CfirScope,
    /** 是否为局部 scope。 */
    val isLocal: Boolean,
)

/**
 * scope 塔上下文，保存当前解析点可见的完整 scope 栈。
 */
data class CfirTowerDataContext private constructor(
    /** 所有 scope 元素，从外到内排列。 */
    val towerDataElements: List<CfirTowerDataElement>,
    /** 局部 scope 列表。 */
    val localScopes: List<CfirLocalScopeImpl>,
    /** 非局部 scope 元素列表。 */
    val nonLocalTowerDataElements: List<CfirTowerDataElement>,
) {

    constructor() : this(
        towerDataElements = emptyList(),
        localScopes = emptyList(),
        nonLocalTowerDataElements = emptyList(),
    )

    /** 添加局部 scope。 */
    fun addLocalScope(localScope: CfirLocalScopeImpl): CfirTowerDataContext {
        val element = CfirTowerDataElement(localScope, isLocal = true)
        return copy(
            towerDataElements = towerDataElements + element,
            localScopes = localScopes + localScope,
        )
    }

    /** 添加非局部 scope。 */
    fun addNonLocalScope(scope: CfirScope): CfirTowerDataContext {
        val element = CfirTowerDataElement(scope, isLocal = false)
        return copy(
            towerDataElements = towerDataElements + element,
            nonLocalTowerDataElements = nonLocalTowerDataElements + element,
        )
    }

    /** 批量添加非局部 scope。 */
    fun addNonLocalScopes(scopes: List<CfirScope>): CfirTowerDataContext {
        if (scopes.isEmpty()) return this
        var ctx = this
        for (scope in scopes) {
            ctx = ctx.addNonLocalScope(scope)
        }
        return ctx
    }

    /** 获取从内到外的全部 scope，用于名称查找。 */
    fun allScopesReversed(): List<CfirScope> {
        return towerDataElements.asReversed().map { it.scope }
    }
}

