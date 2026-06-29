package org.cangnova.cangjie.analysis.api.cfir.scopes

import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.cfir.utils.cached
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope
import org.cangnova.cangjie.name.Name

/**
 * 直接委托底层 CFIR names-aware scope 的公开作用域实现。
 */
internal open class CaCfirDelegatingNamesAwareScope(
    cfirScope: CfirContainingNamesAwareScope,
    builder: CaSymbolByCfirBuilder,
) : CaCfirBasedScope<CfirContainingNamesAwareScope>(cfirScope, builder) {
    /**
     * callable 与 classifier 名称的合并缓存。
     */
    private val allNamesCached by cached {
        getPossibleCallableNames() + getPossibleClassifierNames()
    }

    /**
     * 返回当前作用域可能出现的全部名称。
     */
    override fun getAllPossibleNames(): Set<Name> = withValidityAssertion { allNamesCached }

    /**
     * 返回底层 CFIR 作用域声明的 callable 名称集合。
     */
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        cfirScope.getCallableNames()
    }

    /**
     * 返回底层 CFIR 作用域声明的 classifier 名称集合。
     */
    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        cfirScope.getClassifierNames()
    }

    /**
     * 判断当前作用域是否可能包含指定名称。
     */
    override fun mayContainName(name: Name): Boolean = withValidityAssertion {
        name in getAllPossibleNames()
    }
}
