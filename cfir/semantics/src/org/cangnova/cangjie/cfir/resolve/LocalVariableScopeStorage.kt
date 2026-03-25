package org.cangnova.cangjie.cfir.resolve

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * Caches per-local-variable type scopes so repeated smart-cast lookups reuse the same scope instance.
 */
class LocalVariableScopeStorage private constructor(
    private val map: PersistentMap<CfirCallableSymbol<*>, MutableMap<ConeCangJieType, CfirTypeScope?>>,
) {
    constructor() : this(persistentHashMapOf())

    fun addLocalVariable(symbol: CfirCallableSymbol<*>): LocalVariableScopeStorage =
        LocalVariableScopeStorage(map.put(symbol, mutableMapOf()))

    fun getOrPutScope(
        symbol: CfirCallableSymbol<*>,
        type: ConeCangJieType,
        build: () -> CfirTypeScope?,
    ): CfirTypeScope? {
        val scopes = map[symbol] ?: return build()
        return scopes.getOrPut(type, build)
    }
}
