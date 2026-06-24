package org.cangnova.cangjie.cfir.declarations


import kotlinx.collections.immutable.PersistentList
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScope

/**
 * 当前局部解析上下文可见的局部作用域栈。
 *
 * 使用持久化列表表达进入/退出作用域时的不可变快照，供隐式 receiver 与局部声明查询共享。
 */
typealias CfirLocalScopes = PersistentList<CfirLocalScope>
