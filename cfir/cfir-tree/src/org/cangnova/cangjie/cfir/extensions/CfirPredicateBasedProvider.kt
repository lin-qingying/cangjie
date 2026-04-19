package org.cangnova.cangjie.cfir.extensions

import kotlinx.collections.immutable.PersistentList
import org.cangnova.cangjie.cfir.NoMutableState
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.extensions.predicate.AbstractPredicate
import org.cangnova.cangjie.cfir.extensions.predicate.LookupPredicate
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol

/**
 * `CfirPredicateBasedProvider` 对位 Kotlin `FirPredicateBasedProvider`。
 */
abstract class CfirPredicateBasedProvider : CfirSessionComponent {
    abstract fun getSymbolsByPredicate(predicate: LookupPredicate): List<CfirBasedSymbol<*>>

    abstract fun getOwnersOfDeclaration(declaration: CfirDeclaration): List<CfirBasedSymbol<*>>?

    abstract fun fileHasPluginAnnotations(file: CfirFile): Boolean

    abstract fun matches(predicate: AbstractPredicate<*>, declaration: CfirDeclaration): Boolean

    fun matches(predicate: AbstractPredicate<*>, declaration: CfirBasedSymbol<*>): Boolean {
        return matches(predicate, declaration.cfir)
    }

    fun matches(predicates: List<AbstractPredicate<*>>, declaration: CfirDeclaration): Boolean {
        return predicates.any { matches(it, declaration) }
    }

    fun matches(predicates: List<AbstractPredicate<*>>, declaration: CfirBasedSymbol<*>): Boolean {
        return matches(predicates, declaration.cfir)
    }

    /**
     * 供插件内部记录已匹配声明使用，普通调用方不应直接依赖。
     */
    @CfirExtensionApiInternals
    open fun registerAnnotatedDeclaration(declaration: CfirDeclaration, owners: PersistentList<CfirDeclaration>) {}
}

@NoMutableState
object CfirEmptyPredicateBasedProvider : CfirPredicateBasedProvider() {
    override fun getSymbolsByPredicate(predicate: LookupPredicate): List<CfirBasedSymbol<*>> = emptyList()

    override fun getOwnersOfDeclaration(declaration: CfirDeclaration): List<CfirBasedSymbol<*>>? = null

    override fun fileHasPluginAnnotations(file: CfirFile): Boolean = false

    override fun matches(predicate: AbstractPredicate<*>, declaration: CfirDeclaration): Boolean = false
}

val CfirSession.predicateBasedProvider: CfirPredicateBasedProvider by CfirSession.sessionComponentAccessor()
