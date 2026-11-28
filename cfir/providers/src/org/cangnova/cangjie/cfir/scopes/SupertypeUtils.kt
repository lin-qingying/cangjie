package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.ScopeSessionKey

/** 创建强类型缓存 key；具体语义输入与 owned generation 由实际 scope provider 提供。 */
inline fun <reified ID : Any, reified FS : Any> scopeSessionKey(): ScopeSessionKey<ID, FS> =
    object : ScopeSessionKey<ID, FS>() {}
