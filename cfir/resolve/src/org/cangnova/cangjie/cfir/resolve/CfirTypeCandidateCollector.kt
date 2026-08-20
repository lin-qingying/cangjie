package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.declarations.declarationAvailabilityProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessContext
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessKind
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityResult
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOrigin
import org.cangnova.cangjie.cfir.resolve.providers.lookupOriginForAccessibility
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.accessibilityChecker
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassifierSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

/**
 * 类型与构造目标 classifier 的统一候选收集器。
 *
 * scope 只提供结构符号、替换器与查找来源；本收集器在每个候选进入解析器时，使用完整
 * [CfirAccessContext] 调用 session 级可访问性服务。类型解析、tower classifier、限定符
 * 成员和构造调用因此不会再各自回放 package/import/provider 查找。
 */
internal class CfirTypeCandidateCollector(
    private val session: CfirSession,
    private val context: CfirAccessContext,
) {
    /** 单个 classifier 候选及其 use-site 结构元数据。 */
    data class TypeCandidate(
        val symbol: CfirClassifierSymbol<*>,
        val substitutor: ConeSubstitutor?,
        val lookupOrigin: CfirLookupOrigin,
    )

    /**
     * 返回第一个含可访问候选的 scope 层，并保留该层全部候选。
     *
     * 不可发现的 classifier 不会阻断低优先级 scope；同层多个可访问候选由上层解析器
     * 继续执行歧义处理，不能在 collector 内任意选择一个符号。
     */
    fun firstVisibleScopeCandidates(
        scopes: Iterable<CfirScope>,
        name: Name,
    ): List<TypeCandidate> {
        for (scope in scopes) {
            val candidates = candidatesFromScope(scope, name)
            if (candidates.isNotEmpty()) return candidates
        }
        return emptyList()
    }

    /** 返回第一个可访问 classifier 候选。 */
    fun firstVisibleScopeCandidate(
        scopes: Iterable<CfirScope>,
        name: Name,
    ): TypeCandidate? = firstVisibleScopeCandidates(scopes, name).firstOrNull()

    /** 从一个确定 scope 中收集可访问 classifier，并保留 use-site substitutor。 */
    fun candidatesFromScope(
        scope: CfirScope,
        name: Name,
    ): List<TypeCandidate> {
        val lookupOrigin = scope.lookupOriginForAccessibility()
        val result = mutableListOf<TypeCandidate>()
        scope.processClassifiersByNameWithSubstitution(name) { symbol, substitutor ->
            if (isAccessible(symbol, lookupOrigin)) {
                result += TypeCandidate(symbol, substitutor, lookupOrigin)
            }
        }
        return result.distinctBy(TypeCandidate::symbol)
    }

    /**
     * 聚合已确定 [classId] 的完整重声明候选。
     *
     * [preferredCandidate] 保留首次 scope lookup 的真实 origin/substitutor；provider 冲突
     * 候选使用 [lookupOrigin]，但仍逐个经过同一 checker，不能因已知 ClassId 绕过访问控制。
     */
    fun collectClassIdCandidates(
        classId: ClassId,
        preferredCandidate: TypeCandidate?,
        lookupOrigin: CfirLookupOrigin,
    ): List<TypeCandidate> = buildList {
        preferredCandidate?.let(::add)
        session.declarationAvailabilityProvider.classLikeCandidates(classId).forEach { symbol ->
            add(
                TypeCandidate(
                    symbol = symbol,
                    substitutor = null,
                    lookupOrigin = lookupOrigin,
                )
            )
        }
    }.distinctBy(TypeCandidate::symbol).filter { candidate ->
        isAccessible(candidate.symbol, candidate.lookupOrigin)
    }

    /** classifier 以其真实查找来源进入唯一可访问性服务。 */
    private fun isAccessible(
        symbol: CfirClassifierSymbol<*>,
        lookupOrigin: CfirLookupOrigin,
    ): Boolean {
        val classLike = symbol as? CfirClassLikeSymbol<*> ?: return true
        val result = session.accessibilityChecker.checkClassLike(
            classLike,
            context.copy(
                lookupOrigin = lookupOrigin,
                kind = CfirAccessKind.TYPE,
            ),
        )
        return result is CfirAccessibilityResult.Accessible
    }
}
