package org.cangnova.cangjie.cfir.resolve

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * 局部变量 smart cast 后类型作用域的缓存。
 *
 * @property map 按局部变量符号和 smart cast 类型索引的作用域缓存。
 */
class LocalVariableScopeStorage private constructor(
    private val map: PersistentMap<CfirCallableSymbol<*>, MutableMap<ConeCangJieType, CfirTypeScope?>>,
) {
    constructor() : this(persistentHashMapOf())

    /**
     * 为局部变量创建独立作用域缓存桶。
     *
     * @return 包含该变量缓存桶的新存储实例。
     */
    fun addLocalVariable(symbol: CfirCallableSymbol<*>): LocalVariableScopeStorage =
        LocalVariableScopeStorage(map.put(symbol, mutableMapOf()))

    /**
     * 获取或创建局部变量在指定类型下的作用域。
     *
     * 如果变量尚未注册缓存桶，直接调用 [build] 返回结果，避免为非局部变量写入缓存。
     */
    fun getOrPutScope(
        symbol: CfirCallableSymbol<*>,
        type: ConeCangJieType,
        build: () -> CfirTypeScope?,
    ): CfirTypeScope? {
        val scopes = map[symbol] ?: return build()
        return scopes.getOrPut(type, build)
    }
}
