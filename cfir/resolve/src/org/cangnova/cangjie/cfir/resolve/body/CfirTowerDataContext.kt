package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScopeImpl

/**
 * Scope 塔中的单个数据元素。
 *
 * 每个元素封装一个 scope 及其元数据（是否为局部 scope）。
 * Phase 2 仅支持 scope 类型的元素，后续将扩展为支持隐式接收者。
 *
 * 参考 K2 FirTowerDataElement。
 */
class CfirTowerDataElement(
    /** 该层级对应的 scope（非空） */
    val scope: CfirScope,
    /** 是否为局部 scope（块/函数体内的局部变量 scope） */
    val isLocal: Boolean,
)

/**
 * Scope 塔上下文，持有当前解析点的完整 scope 栈。
 *
 * 使用 copy-on-write 语义（每次变更返回新 List），
 * 进入/退出声明上下文时通过新建 context 实例管理 scope 变化。
 *
 * 参考 K2 FirTowerDataContext。
 */
data class CfirTowerDataContext private constructor(
    /** 所有 scope 元素，从外到内排列 */
    val towerDataElements: List<CfirTowerDataElement>,
    /** 局部 scope 列表（从外到内，是 towerDataElements 中 isLocal=true 的子集） */
    val localScopes: List<CfirLocalScopeImpl>,
    /** 非局部 scope 元素列表 */
    val nonLocalTowerDataElements: List<CfirTowerDataElement>,
) {

    constructor() : this(
        towerDataElements = emptyList(),
        localScopes = emptyList(),
        nonLocalTowerDataElements = emptyList(),
    )

    /** 添加局部 scope（函数体/块内的变量 scope） */
    fun addLocalScope(localScope: CfirLocalScopeImpl): CfirTowerDataContext {
        val element = CfirTowerDataElement(localScope, isLocal = true)
        return copy(
            towerDataElements = towerDataElements + element,
            localScopes = localScopes + localScope,
        )
    }

    /** 添加非局部 scope（包、导入、类成员、类型参数等） */
    fun addNonLocalScope(scope: CfirScope): CfirTowerDataContext {
        val element = CfirTowerDataElement(scope, isLocal = false)
        return copy(
            towerDataElements = towerDataElements + element,
            nonLocalTowerDataElements = nonLocalTowerDataElements + element,
        )
    }

    /** 批量添加非局部 scope */
    fun addNonLocalScopes(scopes: List<CfirScope>): CfirTowerDataContext {
        if (scopes.isEmpty()) return this
        var ctx = this
        for (scope in scopes) {
            ctx = ctx.addNonLocalScope(scope)
        }
        return ctx
    }

    /** 获取所有 scope（从内到外），用于名称查找 */
    fun allScopesReversed(): List<CfirScope> {
        return towerDataElements.asReversed().map { it.scope }
    }
}
