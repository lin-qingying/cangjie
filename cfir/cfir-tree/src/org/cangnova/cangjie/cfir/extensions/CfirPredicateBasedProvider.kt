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
    /**
     * 返回匹配指定 lookup 谓词的符号列表。
     */
    abstract fun getSymbolsByPredicate(predicate: LookupPredicate): List<CfirBasedSymbol<*>>

    /**
     * 返回声明的 owner 符号链；无法确定时返回 `null`。
     */
    abstract fun getOwnersOfDeclaration(declaration: CfirDeclaration): List<CfirBasedSymbol<*>>?

    /**
     * 判断文件是否包含插件注册关注的注解。
     */
    abstract fun fileHasPluginAnnotations(file: CfirFile): Boolean

    /**
     * 判断声明是否匹配指定谓词。
     */
    abstract fun matches(predicate: AbstractPredicate<*>, declaration: CfirDeclaration): Boolean

    /**
     * 判断符号绑定声明是否匹配指定谓词。
     */
    fun matches(predicate: AbstractPredicate<*>, declaration: CfirBasedSymbol<*>): Boolean {
        return matches(predicate, declaration.cfir)
    }

    /**
     * 判断声明是否匹配任一谓词。
     */
    fun matches(predicates: List<AbstractPredicate<*>>, declaration: CfirDeclaration): Boolean {
        return predicates.any { matches(it, declaration) }
    }

    /**
     * 判断符号绑定声明是否匹配任一谓词。
     */
    fun matches(predicates: List<AbstractPredicate<*>>, declaration: CfirBasedSymbol<*>): Boolean {
        return matches(predicates, declaration.cfir)
    }

    /**
     * 供插件内部记录已匹配声明使用，普通调用方不应直接依赖。
     */
    @CfirExtensionApiInternals
    open fun registerAnnotatedDeclaration(declaration: CfirDeclaration, owners: PersistentList<CfirDeclaration>) {}
}

/**
 * 空 predicate provider。
 */
@NoMutableState
object CfirEmptyPredicateBasedProvider : CfirPredicateBasedProvider() {
    /**
     * 空实现不返回任何符号。
     */
    override fun getSymbolsByPredicate(predicate: LookupPredicate): List<CfirBasedSymbol<*>> = emptyList()

    /**
     * 空实现没有 owner 信息。
     */
    override fun getOwnersOfDeclaration(declaration: CfirDeclaration): List<CfirBasedSymbol<*>>? = null

    /**
     * 空实现认为文件不含插件注解。
     */
    override fun fileHasPluginAnnotations(file: CfirFile): Boolean = false

    /**
     * 空实现不匹配任何谓词。
     */
    override fun matches(predicate: AbstractPredicate<*>, declaration: CfirDeclaration): Boolean = false
}

/**
 * 当前 session 的 predicate provider。
 */
val CfirSession.predicateBasedProvider: CfirPredicateBasedProvider by CfirSession.sessionComponentAccessor()
