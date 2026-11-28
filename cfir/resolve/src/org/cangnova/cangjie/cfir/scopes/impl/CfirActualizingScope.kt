package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.cjmpMatchResultForActualization
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.scopes.*
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.isCjmpSpecificCompilationEnabled
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.name.Name

/**
 * body tower 的非成员作用域实际化，对位 Kotlin FirActualizingScope。
 * 在适用性、隐藏性及重载优先级判断之前排除已对应的 common，并保留原始来源路径。
 * 类型成员的继承归并与缓存边界由成员作用域负责，不能在这里包装 CfirTypeScope。
 */
class CfirActualizingScope(private val delegate: CfirScope, private val session: CfirSession) :
    CfirScope(), CfirCallableLookupProvenanceScope {
    init { require(delegate !is CfirTypeScope) }

    override val scopeOwnerLookupNames: List<String> get() = delegate.scopeOwnerLookupNames
    override fun mayContainName(name: Name): Boolean = delegate.mayContainName(name)
    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) =
        delegate.processClassifiersByName(name, processor)
    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (CfirClassifierSymbol<*>, ConeSubstitutor) -> Unit) =
        delegate.processClassifiersByNameWithSubstitution(name, processor)
    override fun processDeclaredConstructors(processor: (CfirConstructorSymbol) -> Unit) =
        delegate.processDeclaredConstructors(processor)
    override fun processOwnNonTypeBindingsByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) =
        delegate.processOwnNonTypeBindingsByName(name, processor)

    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        val symbols = mutableListOf<CfirNamedFunctionSymbol>()
        delegate.processFunctionsByName(name) { symbols += it }
        actualizeCjmpCandidates(symbols, session) { it }.forEach(processor)
    }

    override fun processVariablesByName(name: Name, processor: (CfirVariableSymbol<*>) -> Unit) {
        val symbols = mutableListOf<CfirVariableSymbol<*>>()
        delegate.processVariablesByName(name) { symbols += it }
        actualizeCjmpCandidates(symbols, session) { it }.forEach(processor)
    }

    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        val symbols = mutableListOf<CfirPropertySymbol>()
        delegate.processPropertiesByName(name) { symbols += it }
        actualizeCjmpCandidates(symbols, session) { it }.forEach(processor)
    }

    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        val symbols = mutableListOf<CfirCallableSymbol<*>>()
        delegate.processCallablesByName(name) { symbols += it }
        actualizeCjmpCandidates(symbols, session) { it }.forEach(processor)
    }

    override fun processCallablesByNameWithLookupProvenance(name: Name, processor: (CfirCallableWithLookupProvenance) -> Unit) {
        val candidates = mutableListOf<CfirCallableWithLookupProvenance>()
        delegate.processCallablesByNameWithLookupProvenance(name) { candidates += it }
        actualizeCjmpCandidates(candidates, session) { it.symbol }.forEach(processor)
    }

    override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirScope? =
        delegate.withReplacedSessionOrNull(newSession, newScopeSession)?.withCjmpActualization(newSession)
}

/**
 * 同一名称的候选必须收集完整后再正向剔除 common，结果保留原候选对象及其 lookup provenance。
 * 非成员包装、成员 tower 和直接 body 查询共用此入口；结构 scope 的构建和继承归并不得调用它。
 */
internal fun <T> actualizeCjmpCandidates(
    candidates: List<T>,
    session: CfirSession,
    symbol: (T) -> CfirCallableSymbol<*>,
): List<T> {
    if (!session.isCjmpSpecificCompilationEnabled()) return candidates
    val hiddenCommon = mutableSetOf<CfirBasedSymbol<*>>()
    for (candidate in candidates) {
        hiddenCommon += symbol(candidate).cjmpMatchResultForActualization()?.counterparts.orEmpty().map { it.commonSymbol }
    }
    return candidates.filter { candidate ->
        val declaration = symbol(candidate).cfir.unwrapSubstitutionOverrides()
        !declaration.status.isCommon || declaration.symbol !in hiddenCommon
    }.distinct()
}

/** 只在 body 查询建立实际化包装，不污染 STATUS/TYPES 已缓存的结构作用域。 */
fun CfirScope.withCjmpActualization(session: CfirSession): CfirScope =
    if (session.isCjmpSpecificCompilationEnabled() && this !is CfirTypeScope && this !is CfirActualizingScope) {
        CfirActualizingScope(this, session)
    } else {
        this
    }
