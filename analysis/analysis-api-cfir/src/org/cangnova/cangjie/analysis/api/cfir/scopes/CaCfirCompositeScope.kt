package org.cangnova.cangjie.analysis.api.cfir.scopes

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.name.Name

/**
 * 把多个公开作用域组合成单一名称查询视图。
 *
 * 组合视图按入参顺序查询各子作用域，并按**符号等价性**去重：
 * 同一名称的重载、导入别名与不同来源的成员必须全部保留，
 * 不得按显示名合并，否则会丢掉合法重载与 override/provenance 差异。
 *
 * 该视图不产生新的可见性结论：被组合的各作用域仍各自负责过滤。
 *
 * @param delegates 按查找优先级排列的子作用域。
 * @param isEquivalent 会话内的符号等价判定，用于按身份而非显示名去重。
 */
@OptIn(CaExperimentalApi::class, CaImplementationDetail::class, CaPlatformInterface::class)
internal class CaCfirCompositeScope(
    private val delegates: List<CaScope>,
    private val isEquivalent: (CaSymbol, CaSymbol) -> Boolean,
) : CaScope {

    /**
     * 组合作用域跟随首个子作用域的生命周期令牌。
     */
    override val token get() = delegates.first().token

    /**
     * 当前组合视图可见的 callable，按优先级与符号身份去重。
     */
    override fun callables(names: Collection<Name>): Sequence<CaCallableSymbol> = withValidityAssertion {
        collectNamespaced { delegate -> delegate.callables(names).toList() }
    }

    /**
     * 按名称过滤器查询 callable。
     */
    override fun callables(nameFilter: (Name) -> Boolean): Sequence<CaCallableSymbol> =
        callables(getPossibleCallableNames().filter(nameFilter))

    /**
     * 当前组合视图可见的 classifier，按优先级与符号身份去重。
     */
    override fun classifiers(names: Collection<Name>): Sequence<CaClassifierSymbol> = withValidityAssertion {
        collectNamespaced { delegate -> delegate.classifiers(names).toList() }
    }

    /**
     * 按名称过滤器查询 classifier。
     */
    override fun classifiers(nameFilter: (Name) -> Boolean): Sequence<CaClassifierSymbol> =
        classifiers(getPossibleClassifierNames().filter(nameFilter))

    /**
     * 组合视图合并各子作用域声明的构造器。
     */
    override val constructors: Sequence<CaConstructorSymbol>
        get() = withValidityAssertion {
            collectNamespaced { delegate -> delegate.constructors.toList() }
        }

    /**
     * 组合视图合并各子作用域的包符号。
     */
    @CaExperimentalApi
    override fun getPackageSymbols(nameFilter: (Name) -> Boolean): Sequence<CaPackageSymbol> =
        withValidityAssertion {
            collectNamespaced { delegate -> delegate.getPackageSymbols(nameFilter).toList() }
        }

    /**
     * 组合视图的全部可能名称，等于各子作用域名称的并集。
     */
    override fun getAllPossibleNames(): Set<Name> = withValidityAssertion {
        delegates.flatMapTo(linkedSetOf()) { it.getAllPossibleNames() }
    }

    /**
     * 组合视图可能出现的 callable 名称，等于各子作用域的并集。
     */
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        delegates.flatMapTo(linkedSetOf()) { it.getPossibleCallableNames() }
    }

    /**
     * 组合视图可能出现的 classifier 名称，等于各子作用域的并集。
     */
    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        delegates.flatMapTo(linkedSetOf()) { it.getPossibleClassifierNames() }
    }

    /**
     * 组合视图的类型参数等于各子作用域的并集，按首次出现去重。
     */
    override fun typeParameters(nameFilter: (Name) -> Boolean): Sequence<CaTypeParameterSymbol> =
        withValidityAssertion {
            sequence {
                val seen = mutableSetOf<String>()
                for (delegate in delegates) {
                    for (symbol in delegate.typeParameters(nameFilter)) {
                        val key = symbol.name?.asString() ?: continue
                        if (seen.add(key)) yield(symbol)
                    }
                }
            }
        }

    /**
     * 依次查询各子作用域并按**声明身份**去重，保留首次出现的顺序。
     *
     * 去重键取声明 PSI：同一声明经不同作用域路径到达时 PSI 相同，
     * 不同重载与不同来源的同名声明 PSI 不同，因此都不会被合并。
     *
     * 这里刻意不使用 `CaSymbolRelationProvider.isEquivalentTo`：其缓存键只由
     * `callableId`（包名 + 短名）构成，不含参数列表，会把同名重载判为同一符号。
     * 该缺陷同样影响 `checkCompletionCandidate` 的去重，需在符号身份模型单独修复。
     */
    private fun <T : CaSymbol> collectNamespaced(query: (CaScope) -> List<T>): Sequence<T> {
        val collected = mutableListOf<T>()
        for (delegate in delegates) {
            for (symbol in query(delegate)) {
                if (collected.none { it.isSameDeclaration(symbol, isEquivalent) }) {
                    collected += symbol
                }
            }
        }
        return collected.asSequence()
    }
}

/**
 * 判断两个符号是否指向同一底层声明；无 PSI 的合成符号回退到会话等价判定。
 */
private fun CaSymbol.isSameDeclaration(other: CaSymbol, isEquivalent: (CaSymbol, CaSymbol) -> Boolean): Boolean {
    if (this === other) return true
    val declarationPsi = psi ?: return isEquivalent(this, other)
    return declarationPsi === other.psi
}