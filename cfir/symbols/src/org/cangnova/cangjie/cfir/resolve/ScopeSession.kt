

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.util.PrivateForInline

class ScopeSession {
    private val scopes: HashMap<Any, HashMap<ScopeSessionKey<*, *>, Any>> = hashMapOf()

    @PrivateForInline
    fun scopes(): HashMap<Any, HashMap<ScopeSessionKey<*, *>, Any>> = scopes

    @OptIn(PrivateForInline::class)
    inline fun <reified ID : Any, reified FS : Any> getOrBuild(id: ID, key: ScopeSessionKey<ID, FS>, build: () -> FS): FS {
        return scopes().getOrPut(id) {
            hashMapOf()
        }.getOrPut(key) {
            build()
        } as FS
    }
}

abstract class ScopeSessionKey<ID : Any, FS : Any>

