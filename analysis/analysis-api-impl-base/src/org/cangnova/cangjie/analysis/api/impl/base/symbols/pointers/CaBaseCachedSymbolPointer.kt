package org.cangnova.cangjie.analysis.api.impl.base.symbols.pointers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import java.lang.ref.WeakReference

/**
 * 对齐 Kotlin `KaBaseCachedSymbolPointer` 的通用缓存基座。
 *
 * 这里统一约束 pointer 的缓存协议：
 * 1. 只有缓存符号仍绑定当前 analysis session token 时才可直接复用；
 * 2. 否则必须重新走具体 pointer 的恢复链；
 * 3. 只有具备稳定非局部身份的符号才允许进入缓存，避免局部声明在同 token 修改下复用旧值。
 */
abstract class CaBaseCachedSymbolPointer<out S : CaSymbol>(
    originalSymbol: S?,
) : CaSymbolPointer<S> {
    private var cachedSymbol: Any? = null

    init {
        originalSymbol?.let(::cacheWithIsCacheableCheck)
    }

    final override fun restoreSymbol(session: CaSession): S? = when (val cachedSymbol = cachedSymbol) {
        is WeakReference<*> -> {
            val value = cachedSymbol.get()
            @Suppress("UNCHECKED_CAST")
            if (value != null && (value as S).token == session.token) {
                value
            } else {
                restoreIfNotCached(session).also {
                    this.cachedSymbol = WeakReference(it)
                }
            }
        }

        NOT_CACHED -> restoreIfNotCached(session)
        null -> restoreIfNotCached(session)?.also(::cacheWithIsCacheableCheck)
        else -> error("Unexpected cached symbol holder: ${cachedSymbol::class.simpleName}")
    }

    private fun cacheWithIsCacheableCheck(symbol: S) {
        cachedSymbol = if (symbol.isCacheable) WeakReference(symbol) else NOT_CACHED
    }

    protected abstract fun restoreIfNotCached(session: CaSession): S?

    private companion object {
        private val NOT_CACHED = Any()

        private val CaSymbol.isCacheable: Boolean
            get() = when (this) {
                is CaConstructorSymbol -> this.containingClassId != null
                is CaCallableSymbol -> this.callableId != null
                is CaClassLikeSymbol -> this.classId != null
                is CaPackageSymbol, is CaFileSymbol -> true
                else -> false
            }
    }
}
